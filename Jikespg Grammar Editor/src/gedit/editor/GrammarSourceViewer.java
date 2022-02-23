/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.NonUISafeRunnable;
import gedit.model.Definition;
import gedit.model.Document;
import gedit.model.FileProzessor;
import gedit.model.IProblemRequestor;
import gedit.model.ModelBase;
import gedit.model.ModelType;
import gedit.model.ModelUtils;
import gedit.model.Problem;
import gedit.model.Reference;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class GrammarSourceViewer extends ProjectionViewer implements IPropertyChangeListener, IReconcilingListener {
	private IInformationPresenter fOutlinePresenter;
	private SemanticHighLighter fSemanticHighLighter;
	private IPreferenceStore fPreferenceStore;
	private ListenerList fReconcilingListeners;
	private RuleBasedScanner fMacroScanner;
	private Color fForegroundColor;
	private Color fBackgroundColor;
	private Color fSelectionForegroundColor;
	private Color fSelectionBackgroundColor;
	
	public final static int SHOW_OUTLINE = 103;
	public final static int GOTO_DECLARATION = 104;

	public GrammarSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler,
			boolean showAnnotationsOverview, int styles, IPreferenceStore store) {
		super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles);
		fPreferenceStore = store;
	}
	
	public void configure(SourceViewerConfiguration configuration) {
		if (configuration instanceof GrammarSourceViewerConfiguration) {
			GrammarSourceViewerConfiguration grammarSourceViewerConfiguration = (GrammarSourceViewerConfiguration) configuration;
			fOutlinePresenter = grammarSourceViewerConfiguration.getOutlinePresenter(this);
			fOutlinePresenter.install(this);
			
			fSemanticHighLighter = grammarSourceViewerConfiguration.getSemanticHighlighter(this);
			addReconcilingListener(fSemanticHighLighter);
			addTextInputListener(fSemanticHighLighter);
			if (!grammarSourceViewerConfiguration.isUseReconciling())
				addTextPresentationListener(fSemanticHighLighter);
			fMacroScanner = grammarSourceViewerConfiguration.getMacroScanner();
		}
		if (fPreferenceStore != null) {
			fPreferenceStore.addPropertyChangeListener(this);
			initializeViewerColors();
		}
		super.configure(configuration);
	}

	protected void initializeViewerColors() {
		if (fPreferenceStore == null)
			return;

		StyledText styledText = getTextWidget();

		// ----------- foreground color --------------------
		Color color = fPreferenceStore.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT)
		? null
		: createColor(fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND, styledText.getDisplay());
		styledText.setForeground(color);

		if (fForegroundColor != null)
			fForegroundColor.dispose();

		fForegroundColor = color;

		// ---------- background color ----------------------
		color = fPreferenceStore.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)
		? null
		: createColor(fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND, styledText.getDisplay());
		styledText.setBackground(color);

		if (fBackgroundColor != null)
			fBackgroundColor.dispose();

		fBackgroundColor = color;

		// ----------- selection foreground color --------------------
		color = fPreferenceStore.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR)
			? null
			: createColor(fPreferenceStore, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR, styledText.getDisplay());
		styledText.setSelectionForeground(color);

		if (fSelectionForegroundColor != null)
			fSelectionForegroundColor.dispose();

		fSelectionForegroundColor = color;

		// ---------- selection background color ----------------------
		color = fPreferenceStore.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR)
			? null
			: createColor(fPreferenceStore, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR, styledText.getDisplay());
		styledText.setSelectionBackground(color);

		if (fSelectionBackgroundColor != null)
			fSelectionBackgroundColor.dispose();

		fSelectionBackgroundColor = color;
    }

    private Color createColor(IPreferenceStore store, String key, Display display) {
        RGB rgb = null;

        if (store.contains(key)) {
            if (store.isDefault(key))
                rgb = PreferenceConverter.getDefaultColor(store, key);
            else
                rgb = PreferenceConverter.getColor(store, key);

            if (rgb != null)
                return new Color(display, rgb);
        }
        return null;
    }

    public void unconfigure() {
		if (fOutlinePresenter != null)
			fOutlinePresenter.uninstall();
		removeReconcilingListener(fSemanticHighLighter);
		removeTextInputListener(fSemanticHighLighter);
		removeTextPresentationListener(fSemanticHighLighter);
		if (fPreferenceStore != null)
			fPreferenceStore.removePropertyChangeListener(this);
		disposeColor(fForegroundColor);
		disposeColor(fBackgroundColor);
		disposeColor(fSelectionForegroundColor);
		disposeColor(fSelectionBackgroundColor);
		super.unconfigure();
	}
    
	private void disposeColor(Color color) {
		if (color != null)
			color.dispose();
	}

	public void resetVisibleRegion() {
		super.resetVisibleRegion();
		if (fPreferenceStore != null && fPreferenceStore.getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED) && !isProjectionMode())
			enableProjection();
	}

	public boolean canDoOperation(int operation) {
		switch (operation) {
		case SHOW_OUTLINE:
			return fOutlinePresenter != null;
		case GOTO_DECLARATION:
			return true;
		}
		return super.canDoOperation(operation);
	}

	public void doOperation(int operation) {
		switch (operation) {
		case SHOW_OUTLINE:
			getModel(true);
			fOutlinePresenter.showInformation();
			return;
		case GOTO_DECLARATION:
			selectReferringWord(getSelectedWord(getDocument(), widgetOffset2ModelOffset(getTextWidget().getCaretOffset() - 1)));
			break;
		}
		super.doOperation(operation);
	}
	
	public void addReconcilingListener(IReconcilingListener listener) {
		if (fReconcilingListeners == null)
			fReconcilingListeners = new ListenerList();
		fReconcilingListeners.add(listener);
	}

	public void removeReconcilingListener(IReconcilingListener listener) {
		if (fReconcilingListeners == null)
			return;
		fReconcilingListeners.remove(listener);
		if (fReconcilingListeners.isEmpty())
			fReconcilingListeners = null;
	}

	protected void notifyReconcilingListeners() {
		if (fReconcilingListeners == null)
			return;
		Object[] listeners = fReconcilingListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			final IReconcilingListener listener = (IReconcilingListener) listeners[i];
			Platform.run(new NonUISafeRunnable() {
				public void run() throws Exception {
					listener.reconciled();
				}
			});
		}
	}
	
	protected GrammarPresentationReconciler getPresentationReconciler() {
		return (GrammarPresentationReconciler) fPresentationReconciler;
	}

	public Document getModel(boolean reconcile) {
		IAnnotationModel annotationModel = getAnnotationModel();
		IProblemRequestor probemRequestor = annotationModel instanceof IProblemRequestor ? (IProblemRequestor) annotationModel : null;
		return GrammarEditorPlugin.getDocumentModel(getDocument(), probemRequestor, reconcile);
	}

	public void selectElement(ModelBase model) {
		if (model == null || model.getOffset() < 0)
			return;
		getTextWidget().setRedraw(false);
		setRangeIndication(model, true);
		revealRange(model.getOffset(), model.getLength());
		setSelectedRange(model.getOffset(), model.getLength());
		getTextWidget().setRedraw(true);
	}

	public void setRangeIndication(ModelBase model, boolean moveCursor) {
		ModelBase element = model;
		while (element != null && element.getType() != ModelType.RULE)
			element = element.getParent();
		if (element == null) {
			element = model;
			while (element != null && element.getType() != ModelType.SECTION)
				element = element.getParent();
		}
		if (element == null)
			element = model.getParent() != null ? (ModelBase) model.getParent() : model;
		if (element != null)
			setRangeIndication(element.getRangeOffset(), element.getRangeLength(), moveCursor);
	}

	public void selectReferringWord(IRegion region) {
		if (region == null)
			return;
		try {
			markInNavigationHistory();
			selectReferringElement(getDocument().get(region.getOffset(), region.getLength()));
			markInNavigationHistory();
		} catch (BadLocationException e) {
			GrammarEditorPlugin.logError("Cannot select the word", e);
		}
	}

	private void markInNavigationHistory() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IWorkbenchPart part = page.getActivePart();
		if (part instanceof IEditorPart)
			page.getNavigationHistory().markLocation((IEditorPart) part);
	}

	private void selectReferringElement(String word) {
		Document document = getModel(false);
		ModelBase model = document.getElementById(word);
		Document include = document.getInclude(word);
		if (model != null && model.getDocument() != document) {
			openUsingOpener(model.getDocument(), document, new Region(model.getOffset(), model.getLength()));
		} else if (include != null && FileProzessor.getFileForName(document, word).exists()) {
			openUsingOpener(include, document, null);
		} else {
			selectElement(model);
		}
	}
	
	private void openUsingOpener(Document document, Document parentDocument, IRegion selection) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IDocumentOpener opener = (IDocumentOpener) page.getActivePart().getAdapter(IDocumentOpener.class);
		opener.open(document, parentDocument, selection);
	}
	
	protected String getContentType(IDocument document, int offset) {
		try {
			if (document instanceof IDocumentExtension3)
				return ((IDocumentExtension3) document).getContentType(GrammarDocumentSetupParticipant.GRAMMAR_PARTITION, offset, false);
			else
				return document.getContentType(offset);
		} catch (Exception e) {
			System.err.println("Cannot get the content type for position: " + offset);
			return null;
		}
	}
	
	public IRegion getSelectedWord(IDocument document, int anchor) {
		
		try {
			String contentType = getContentType(document, anchor);
			Document model = getModel(false);

			if (GrammarPartitionScanner.GRAMMAR_MACRO.equals(contentType)) {
				Position position = new Position(anchor);
				Definition definition = ModelUtils.lookupMacro(anchor, this, fMacroScanner, position);
				if (definition != null)
					return new Region(position.getOffset(), position.getLength());
			} else if (GrammarPartitionScanner.GRAMMAR_OPTION.equals(contentType)) {
				ModelBase element = model.getElementAt(anchor);
				if (!(element instanceof Reference))
					return null;
				return new Region(element.getOffset(), element.getLength());
			}

			if (!IDocument.DEFAULT_CONTENT_TYPE.equals(contentType))
				return null;

			ModelBase element = model.getElementAt(anchor);
			if (element != null && element.getOffset() <= anchor && element.getOffset() + element.getLength() >= anchor)
				return new Region(element.getOffset(), element.getLength());

			int offset = anchor;
			char c;

			while (offset >= 0) {
				c = document.getChar(offset);
				if (Character.isWhitespace(c))
					break;
				--offset;
			}

			int start = offset + 1;

			offset = anchor;
			int length = document.getLength();

			while (offset < length) {
				c = document.getChar(offset);
				if (Character.isWhitespace(c))
					break;
				++offset;
			}
			
			int end = offset;
			
			String selected = document.get(start, end - start);
			if ("::=".equals(selected) || "::=?".equals(selected) || "->".equals(selected)
					|| "->?".equals(selected) || String.valueOf(model.getOptions().getOrMarker()).equals(selected))
				return null;
			
			if (start >= end)
				return new Region(start, 0);
			else
				return new Region(start, end - start);
			
		} catch (BadLocationException x) {
			return null;
		}
	}
	
	public ModelBase getSelectedElement() {
		return getModel(false).getElementAt(widgetOffset2ModelOffset(getTextWidget().getCaretOffset()));
	}
	
	public Problem[] getSelectedProblems() {
		Point selection = widgetSelection2ModelSelection(getTextWidget().getSelectionRange());
		return getModel(false).getProblems(selection.x, selection.y);
	}
	
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND.equals(property)
				|| AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT.equals(property)
				|| AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND.equals(property)
				|| AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT.equals(property)
				|| AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR.equals(property)
				|| AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR.equals(property)
				|| AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR.equals(property)
				|| AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR.equals(property))
		{
			initializeViewerColors();
		}
	}
	
	public void reconciled() {
		notifyReconcilingListeners();
	}

}
