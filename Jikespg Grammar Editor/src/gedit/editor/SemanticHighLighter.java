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
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

public class SemanticHighLighter implements IReconcilingListener, ITextPresentationListener {
	private class SemanticPosition extends Position {
		private TextAttribute fTextAttribute;
		public SemanticPosition(TextAttribute attribute, int offset, int length) {
			super(offset, length);
			fTextAttribute = attribute;
		}
	};
	
	private class Styler
	extends NodeVisitor {
		private TextPresentation fPresentation;
		public Styler(Document document) {
			super(document, ModelType.ALIAS.getType() |
					ModelType.TERMINAL.getType() | ModelType.RULE.getType() |
					ModelType.REFERENCE.getType());
			fPresentation = new TextPresentation();
		}
		
		protected boolean doVisit(Node node, ModelBase element) {
			if (element == null || node.spansMultipleNodes())
				return true;
			int elementType = element.getType().getType();
			if (elementType == ModelType.TERMINAL.getType()) {
				applyAttribute(fPresentation, fTerminalTextAttribute, node, element);
			} else if (elementType == ModelType.ALIAS.getType()) {
				applyAttribute(fPresentation, fAliasTextAttribute, node, element);
			} else if (elementType == ModelType.RULE.getType()) {
				applyAttribute(fPresentation, fNonTerminalTextAttribute, node, element);
			} else if (elementType == ModelType.REFERENCE.getType()) {
				ModelBase referrer = ((Reference) element).getReferer();
				doVisit(node, referrer);
			}
			return true;
		}

		public TextPresentation legLos() {
			if (document.getRoot() != null)
				document.getRoot().accept(this);
			return fPresentation;
		}
	};
	
	private GrammarSourceViewer fViewer;
	private PreferenceUtils fUtils;
	private TextAttribute fTerminalTextAttribute;
	private TextAttribute fNonTerminalTextAttribute;
	private TextAttribute fAliasTextAttribute;
	private List fPositions = new ArrayList();
	private List fRemovedPositions = new ArrayList();
	private boolean fInstalled;
	
	public SemanticHighLighter(GrammarSourceViewer viewer, PreferenceUtils utils) {
		fViewer = viewer;
		fUtils = utils;
		fTerminalTextAttribute = fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_TERMINAL);
		fNonTerminalTextAttribute = fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_NON_TERMINAL);
		fAliasTextAttribute = fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_ALIAS);
	}
	
	private void install() {
		fViewer.getDocument().addPositionCategory(toString());
		fInstalled = true;
	}
	
	public void reconciled() {
		if (fViewer.getTextWidget() == null || fViewer.getTextWidget().isDisposed())
			return;
		final TextPresentation presentation = createMergedStyles();
		fViewer.getTextWidget().getDisplay().asyncExec(new Runnable() {
			public void run() {
				fViewer.removeTextPresentationListener(SemanticHighLighter.this);
				try {
					fViewer.changeTextPresentation(presentation, false);
				} catch (Exception e) {
					// several situations lead to IndexOutOfBounds if positions have changed
				}
				fViewer.addTextPresentationListener(SemanticHighLighter.this);
			}
		});
	}
	
	protected TextPresentation createMergedStyles() {
		TextPresentation presentation = createSemanticStyles();
		if (fViewer.getDocument() == null)
			return presentation;
		if (!fInstalled)
			install();
		TextPresentation original = fViewer.getPresentationReconciler().createPresentation(
				new Region(0, fViewer.getDocument().getLength()), fViewer.getDocument());
		for (Iterator it = presentation.getAllStyleRangeIterator(); it.hasNext(); ) {
			original.mergeStyleRange((StyleRange) it.next());
		}
		return original;
	}
	
	protected TextPresentation createSemanticStyles() {
		long time = System.currentTimeMillis();
		fRemovedPositions.addAll(fPositions);
		fPositions.clear();
		Styler finder = new Styler(fViewer.getModel(false));
		TextPresentation presentation = finder.legLos();
		
		if (GrammarEditorPlugin.getDefault().isDebugging())
			System.out.println("semantic styles creation: " + (System.currentTimeMillis() - time) + "ms");
		return presentation;
	}
	
	public void applyTextPresentation(TextPresentation textPresentation) {
		if (fPositions.size() == 0)
			reconciled();

		IRegion region = textPresentation.getExtent();
		IDocument document = fViewer.getDocument();
		for (int i = 0; i < fPositions.size(); i++) {
			SemanticPosition position = (SemanticPosition) fPositions.get(i);
			if (position.offset >= region.getOffset() && position.offset + position.length <= region.getOffset() + region.getLength())
				textPresentation.replaceStyleRange(createStyleRange(position.fTextAttribute,
						position.offset, position.length));
			if (position.isDeleted || fRemovedPositions.contains(position)) {
				try {
					document.removePosition(toString(), position);
					fRemovedPositions.remove(position);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				addPosition(position);
			}
		}
		fRemovedPositions.addAll(fPositions);
		fPositions.clear();
	}
	
	private void addPosition(Position position) {
		IDocument document = fViewer.getDocument();
		if (document == null)
			return;
		try {
			document.removePosition(toString(), position);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void applyAttribute(TextPresentation presentation, TextAttribute attribute, Node node, ModelBase element) {
		int offset = node.getOffset();
		int length = node.getLength();
		if (GrammarEditorPlugin.getDefault().isDebugging()) {
			for (int i = 0; i < fPositions.size(); i++) {
				Position position = (Position) fPositions.get(i);
				if (!position.overlapsWith(offset, length))
					continue;
				System.err.println("new position for: " + element + " has already been added");
			}
		}
		presentation.addStyleRange(createStyleRange(attribute, offset, length));
		fPositions.add(new SemanticPosition(attribute, offset, length));
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

}
