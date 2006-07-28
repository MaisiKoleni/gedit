/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.model.Document;
import gedit.model.ModelBase;
import gedit.model.ModelType;
import gedit.model.Node;
import gedit.model.NodeVisitor;
import gedit.model.Reference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

public class SemanticHighLighter implements IReconcilingListener, ITextPresentationListener, IDocumentListener, ITextInputListener {
	private class SemanticPosition extends Position {
		private TextAttribute fTextAttribute;
		public SemanticPosition(TextAttribute attribute, int offset, int length) {
			super(offset, length);
			fTextAttribute = attribute;
		}
		public String toString() {
			return offset + ", " + length + "[" + fTextAttribute.getForeground() + "/" + fTextAttribute.getStyle() + "]";
		}
	};
	
	private class Styler extends NodeVisitor implements Comparator {
		private TextPresentation fPresentation;
		private List fPositions = new ArrayList();
		public Styler(Document document) {
			super(document, ModelType.ALIAS.or(
					ModelType.TERMINAL.or(ModelType.RULE.or(
					ModelType.REFERENCE))));
			fPresentation = new TextPresentation();
		}
		
		protected boolean doVisit(Node node, ModelBase element) {
			if (fCanceled)
				return false;
			if (element == null || node.spansMultipleNodes())
				return true;
			ModelType elementType = element.getType();
			if (elementType == ModelType.TERMINAL) {
				applyAttribute(fPresentation, fTerminalTextAttribute, node, element);
			} else if (elementType == ModelType.ALIAS) {
				applyAttribute(fPresentation, fAliasTextAttribute, node, element);
			} else if (elementType == ModelType.RULE) {
				applyAttribute(fPresentation, fNonTerminalTextAttribute, node, element);
			} else if (elementType == ModelType.REFERENCE) {
				ModelBase referrer = ((Reference) element).getReferer();
				doVisit(node, referrer);
			}
			return true;
		}

		private void applyAttribute(TextPresentation presentation, TextAttribute attribute, Node node, ModelBase element) {
			int offset = node.getOffset();
			int length = node.getLength();
			if (GrammarEditorPlugin.DEBUG) {
				for (int i = 0; i < fPositions.size(); i++) {
					Position position = (Position) fPositions.get(i);
					if (!position.overlapsWith(offset, length))
						continue;
					System.out.println("new position for: " + element + " has already been added");
				}
			}
			presentation.addStyleRange(createStyleRange(attribute, offset, length));
			Position position = new SemanticPosition(attribute, offset, length);
			fPositions.add(position);
		}

		public List legLos() {
			if (document.getRoot() != null)
				document.getRoot().accept(this);
			Collections.sort(fPositions, this);
			return fPositions;
		}
		
		public int compare(Object arg0, Object arg1) {
			Position s1 = (Position) arg0;
			Position s2 = (Position) arg1;
			return s1.offset - s2.offset;
		}
	};
	
	private class PresentationJob extends Job {
		private TextPresentation fPresentation;
		public PresentationJob() {
			super("UpdateSemanticPresentation");
			setSystem(true);
		}

		protected IStatus run(IProgressMonitor monitor) {
			updatePresentation(fPresentation);
			return Status.OK_STATUS;
		}
	};

	private GrammarSourceViewer fViewer;
	private PreferenceUtils fUtils;
	private TextAttribute fTerminalTextAttribute;
	private TextAttribute fNonTerminalTextAttribute;
	private TextAttribute fAliasTextAttribute;
	private List fPositions = new ArrayList();
	private boolean fCanceled;
	private PresentationJob fPresentationJob = new PresentationJob();
	
	public SemanticHighLighter(GrammarSourceViewer viewer, PreferenceUtils utils) {
		fViewer = viewer;
		fUtils = utils;
		fTerminalTextAttribute = fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_TERMINAL);
		fNonTerminalTextAttribute = fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_NON_TERMINAL);
		fAliasTextAttribute = fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_ALIAS);
	}
	
	private void setupDocument(IDocument document) {
		if (document != null)
			document.addDocumentListener(this);
	}
	
	private void releaseDocument(IDocument document) {
		if (document != null)
			document.removeDocumentListener(this);
	}
	
	public void reconciled() {
		fCanceled = false;
		Job job = new Job("SemanticReconciler") {	{ setSystem(true); }
			protected IStatus run(IProgressMonitor monitor) {
				fPresentationJob.fPresentation = createMergedStyles();
				fPresentationJob.schedule();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
	
	private void updatePresentation(final TextPresentation presentation) {
		if (fCanceled || presentation == null ||
				fViewer.getTextWidget() == null || fViewer.getTextWidget().isDisposed())
			return;
		fViewer.getTextWidget().getDisplay().asyncExec(new Runnable() {
			public void run() {
				fViewer.removeTextPresentationListener(SemanticHighLighter.this);
				try {
					if (!fCanceled)
						fViewer.changeTextPresentation(presentation, false);
				} catch (Exception e) {
					// several situations lead to IndexOutOfBounds if positions have changed
				}
				fViewer.addTextPresentationListener(SemanticHighLighter.this);
			}
		});
	}
	
	protected TextPresentation createMergedStyles() {
		if (fCanceled || fViewer.getDocument() == null)
			return null;
		TextPresentation presentation = createSemanticStyles();
		if (fCanceled || fViewer.getDocument() == null)
			return presentation;
		TextPresentation original = fViewer.getPresentationReconciler().createPresentation(
				new Region(0, fViewer.getDocument().getLength()), fViewer.getDocument());
		for (Iterator it = presentation.getAllStyleRangeIterator(); it.hasNext(); ) {
			original.mergeStyleRange((StyleRange) it.next());
		}
		return original;
	}
	
	protected TextPresentation createSemanticStyles() {
		long time = System.currentTimeMillis();
		Styler finder = new Styler(fViewer.getModel(false));
		List positions = finder.legLos();

		if (GrammarEditorPlugin.DEBUG)
			System.out.println("semantic styles creation: " + (System.currentTimeMillis() - time) + "ms");

		synchronized (fPositions) {
			fPositions.clear();
			fPositions.addAll(positions);
		}
		return finder.fPresentation;
	}
	
	public void applyTextPresentation(TextPresentation textPresentation) {
		if (fPositions.size() == 0)
			createMergedStyles();

		IRegion region = textPresentation.getExtent();
		IDocument document = fViewer.getDocument();
		
		int i = computeIndexAtOffset(fPositions, region.getOffset()), n = computeIndexAtOffset(fPositions, region.getOffset() + region.getLength());
		if (n - i > 2) {
			List ranges = new ArrayList(n - i);
			for (; i < n; i++) {
				SemanticPosition position = (SemanticPosition) fPositions.get(i);
				if (!IDocument.DEFAULT_CONTENT_TYPE.equals(fViewer.getContentType(document, position.offset)))
					continue;
				if (!position.isDeleted())
					ranges.add(createStyleRange(position.fTextAttribute, position.offset, position.length));
			}
			StyleRange[] array = new StyleRange[ranges.size()];
			array = (StyleRange[]) ranges.toArray(array);
			textPresentation.mergeStyleRanges(array);
		} else {
			for (; i < n; i++) {
				SemanticPosition position = (SemanticPosition) fPositions.get(i);
				if (!IDocument.DEFAULT_CONTENT_TYPE.equals(fViewer.getContentType(document, position.offset)))
					continue;
				if (!position.isDeleted())
					textPresentation.mergeStyleRange(createStyleRange(position.fTextAttribute, position.offset, position.length));
			}
		}
	}
	
	private int computeIndexAtOffset(List positions, int offset) {
		int i = -1;
		int j = positions.size();
		while (j - i > 1) {
			int k = (i + j) >> 1;
			Position position = (Position) positions.get(k);
			if (position.getOffset() >= offset)
				j = k;
			else
				i = k;
		}
		return j;
	}

	private StyleRange createStyleRange(TextAttribute attribute, int offset, int length) {
		int fontStyle = attribute.getStyle() & (SWT.NORMAL | SWT.BOLD | SWT.ITALIC);
		StyleRange range = new StyleRange(offset, length, attribute.getForeground(), attribute.getBackground(), fontStyle);
		range.strikeout = (attribute.getStyle() & TextAttribute.STRIKETHROUGH) > 0;
		range.underline = (attribute.getStyle() & TextAttribute.UNDERLINE) > 0;
		return range;
	}

	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		String property = event.getProperty();
		return property != null && (property.startsWith(
				PreferenceConstants.GRAMMAR_COLORING_TERMINAL) || property.startsWith(
				PreferenceConstants.GRAMMAR_COLORING_NON_TERMINAL) || property.startsWith(
				PreferenceConstants.GRAMMAR_COLORING_ALIAS));
	}

	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (property == null)
			return;
		if (property.startsWith(PreferenceConstants.GRAMMAR_COLORING_TERMINAL)) {
			TextAttribute attribute = fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_TERMINAL); 
			adaptPositions(fTerminalTextAttribute, attribute);
			fTerminalTextAttribute = attribute;
		} else if (property.startsWith(PreferenceConstants.GRAMMAR_COLORING_NON_TERMINAL)) {
			TextAttribute attribute = fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_NON_TERMINAL);
			adaptPositions(fNonTerminalTextAttribute, attribute);
			fNonTerminalTextAttribute = attribute;
		} else if (property.startsWith(PreferenceConstants.GRAMMAR_COLORING_ALIAS)) {
			TextAttribute attribute = fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_ALIAS);
			adaptPositions(fAliasTextAttribute, attribute);
			fAliasTextAttribute = attribute;
		}
	}
	
	private void adaptPositions(TextAttribute origin, TextAttribute newAttribute) {
		for (int i = 0; i < fPositions.size(); i++) {
			SemanticPosition position = (SemanticPosition) fPositions.get(i);
			if (position.fTextAttribute == origin)
				position.fTextAttribute = newAttribute;
		}
	}
	
	public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
		releaseDocument(oldInput);
	}
	
	public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
		setupDocument(newInput);
	}

	public void documentAboutToBeChanged(DocumentEvent event) {
		int i = computeIndexAtOffset(fPositions, event.getOffset());
		if (i < 0 || i >= fPositions.size())
			return;
		boolean insidePosition = true;
		SemanticPosition position = (SemanticPosition) fPositions.get(i);
		if (!position.overlapsWith(event.fOffset, event.fLength)) {
			if (i > 0) {
				SemanticPosition pos = (SemanticPosition) fPositions.get(--i);
				if (!pos.overlapsWith(event.fOffset, event.fLength))
					insidePosition = false;
				else
					position = pos;
			}
		}
		int eventEnd = event.fOffset + event.fLength;
		int offset = event.fText != null ? event.fText.length() - event.fLength : 0;
		if (offset < 0 && position.offset >= event.fOffset && position.offset + position.length <= eventEnd)
			position.delete();
		if (insidePosition && !position.isDeleted())
			position.length += offset;
		for (; ++i < fPositions.size(); ) {
			position = (SemanticPosition) fPositions.get(i);
			if (offset < 0 && position.offset >= event.fOffset && position.offset + position.length <= eventEnd)
				position.delete();
			if (!position.isDeleted())
				position.offset += offset;
		}
	}

	public void documentChanged(DocumentEvent event) {
		fCanceled = true;
	}

}
