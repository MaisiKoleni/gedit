/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlCreatorExtension;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import gedit.GrammarEditorPlugin;
import gedit.model.Document;

public class GrammarCompletionProposal implements ICompletionProposal, ICompletionProposalExtension,
		ICompletionProposalExtension2, ICompletionProposalExtension3, Comparable {
	private String fReplacement;
	private String fDisplayString;
	private int fReplacementOffset;
	private int fReplacementLength;
	private int fCursorPosition;
	private StyleRange fRememberedStyleRange;
	private Image fImage;
	private String fAdditionalProposalInfo;
	private Document fParentDocument;
	private String fEscapeToValidate;

	public GrammarCompletionProposal(String replacement, String displayString,
			Image image, int offset, int length, int cursorPosition,
			String additionalProposalInfo, Document parentDocument,
			char escapeToValidate) {

		fReplacement = replacement;
		fDisplayString = displayString;
		fImage = image;
		fReplacementOffset = offset;
		fReplacementLength = length;
		fCursorPosition = cursorPosition;
		fAdditionalProposalInfo = additionalProposalInfo;
		fParentDocument = parentDocument;
		if (escapeToValidate != 0)
			fEscapeToValidate = String.valueOf(escapeToValidate);
	}

	@Override
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		IDocument document= viewer.getDocument();
		boolean fToggleEating = (stateMask & SWT.MOD1) != 0;
		if (!fToggleEating && offset >= fReplacementOffset)
			fReplacementLength = offset - fReplacementOffset;

		apply(document, trigger, offset);
		fToggleEating = false;
	}

	@Override
	public void apply(IDocument document, char trigger, int offset) {
		try {
			document.replace(fReplacementOffset, fReplacementLength, fReplacement);
		} catch (BadLocationException x) {
		}
	}

	@Override
	public int getContextInformationPosition() {
		return 0;
	}

	@Override
	public char[] getTriggerCharacters() {
		return null;
	}

	@Override
	public boolean isValidFor(IDocument document, int offset) {
		return validate(document, offset, null);
	}

	@Override
	public void selected(ITextViewer viewer, boolean smartToggle) {
		if (smartToggle)
			updateStyle(viewer);
		else {
			repairPresentation(viewer);
			fRememberedStyleRange = null;
		}
	}

	@Override
	public void unselected(ITextViewer viewer) {
		repairPresentation(viewer);
		fRememberedStyleRange= null;
	}

	private void updateStyle(ITextViewer viewer) {

		StyledText text = viewer.getTextWidget();
		if (text == null || text.isDisposed())
			return;

		int widgetCaret = text.getCaretOffset();

		int modelCaret = 0;
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
			modelCaret = extension.widgetOffset2ModelOffset(widgetCaret);
		} else {
			IRegion visibleRegion = viewer.getVisibleRegion();
			modelCaret = widgetCaret + visibleRegion.getOffset();
		}

		int len = fReplacementLength;
		if (modelCaret >= fReplacementOffset + len) {
			repairPresentation(viewer);
			return;
		}
//		if (len == 0) {
//			repairPresentation(viewer);
//			return;
//		}

		int offset = widgetCaret;
		int length = fReplacementOffset + len - modelCaret;

		Color background = viewer.getTextWidget().getDisplay().getSystemColor(SWT.COLOR_YELLOW);

		repairPresentation(viewer);
		fRememberedStyleRange = new StyleRange(offset, length, null, background);

		// http://dev.eclipse.org/bugs/show_bug.cgi?id=34754
		try {
			text.setStyleRange(fRememberedStyleRange);
		} catch (IllegalArgumentException x) {
			// catching exception as offset + length might be outside of the text widget
			fRememberedStyleRange = null;
		}
	}

	private void repairPresentation(ITextViewer viewer) {
		if (fRememberedStyleRange != null) {
			 if (viewer instanceof ITextViewerExtension2) {
			 	// attempts to reduce the redraw area
			 	ITextViewerExtension2 viewer2= (ITextViewerExtension2) viewer;

			 	if (viewer instanceof ITextViewerExtension5) {

			 		ITextViewerExtension5 extension= (ITextViewerExtension5) viewer;
			 		IRegion widgetRange= extension.widgetRange2ModelRange(new Region(fRememberedStyleRange.start, fRememberedStyleRange.length));
			 		if (widgetRange != null)
			 			viewer2.invalidateTextPresentation(widgetRange.getOffset(), widgetRange.getLength());
		 			viewer2.invalidateTextPresentation(fRememberedStyleRange.start, fRememberedStyleRange.length);
			 	} else {
					viewer2.invalidateTextPresentation(fRememberedStyleRange.start + viewer.getVisibleRegion().getOffset(), fRememberedStyleRange.length);
			 	}

			} else
				viewer.invalidateTextPresentation();
		}
	}

	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event) {

		if (offset < fReplacementOffset)
			return false;

		/*
		 * See http://dev.eclipse.org/bugs/show_bug.cgi?id=17667
		String word= fReplacementString;
		 */
		int length= offset - fReplacementOffset;
		String text = "";
		try {
			text = document.get(fReplacementOffset, length);
		} catch (BadLocationException e) {
			GrammarEditorPlugin.logError("Cannot get the text for the completion replacement", e);
		}
		int pointIndex = text.indexOf('.');
		if (pointIndex != -1)
			text = text.substring(pointIndex + 1);

		boolean validated= startsWith(document, offset, fReplacement);
		if (!validated && fEscapeToValidate != null && fReplacement.startsWith(fEscapeToValidate))
			validated = startsWith(document, offset, fReplacement.substring(1));

		if (validated && event != null) {
			// adapt replacement range to document change
			int delta= (event.fText == null ? 0 : event.fText.length()) - event.fLength;
			fReplacementLength += delta;
		}

		return validated;
	}

	protected boolean startsWith(IDocument document, int offset, String word) {
		int wordLength= word == null ? 0 : word.length();
		if (offset >  fReplacementOffset + wordLength)
			return false;

		try {
			int length= offset - fReplacementOffset;
			String start= document.get(fReplacementOffset, length);
			return word.substring(0, length).equalsIgnoreCase(start);
		} catch (BadLocationException x) {
		}

		return false;
	}

	@Override
	public void apply(IDocument document) {
		try {
			document.replace(fCursorPosition, fReplacementLength, fReplacement);
		} catch (BadLocationException e) {
			GrammarEditorPlugin.logError("Cannot apply the completion text", e);
		}
	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(fReplacementOffset + fCursorPosition, 0);
	}

	@Override
	public String getAdditionalProposalInfo() {
		return fAdditionalProposalInfo;
	}

	@Override
	public String getDisplayString() {
		return fDisplayString;
	}

	@Override
	public Image getImage() {
		return fImage;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public IInformationControlCreator getInformationControlCreator() {
		class InformationControlCreator implements IInformationControlCreator, IInformationControlCreatorExtension {
			@Override
			public IInformationControl createInformationControl(Shell parent) {
				return new GrammarInformationControl(parent, SWT.NONE, SWT.NONE, new SimpleTextPresenter(), fParentDocument);
			}

			@Override
			public boolean canReuse(IInformationControl control) {
				return control != null;
			}

			@Override
			public boolean canReplace(IInformationControlCreator creator) {
				return true;
			}
		}
		return new InformationControlCreator();
	}

	@Override
	public int getPrefixCompletionStart(IDocument document, int completionOffset) {
		return completionOffset;
	}

	@Override
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return null;
	}

	@Override
	public int compareTo(Object o) {
		return o instanceof GrammarCompletionProposal ? fDisplayString.compareToIgnoreCase(((GrammarCompletionProposal) o).fDisplayString) : 0;
	}

}
