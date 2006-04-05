/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.editor.actions.FindOccurrencesInFileAction;
import gedit.editor.actions.FoldingActionGroup;
import gedit.editor.actions.RenameInFileAction;
import gedit.editor.actions.ToggleCommentAction;
import gedit.model.Document;
import gedit.model.ModelBase;
import gedit.model.Node;
import gedit.model.Problem;
import gedit.model.Reference;
import gedit.model.ElementFinder.IdFinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension4;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class GrammarEditor extends TextEditor implements IProjectionListener, IReconcilingListener {
	private final class Listener implements MouseListener, MouseMoveListener,
			KeyListener, ITextPresentationListener, PaintListener {
		private boolean fActive;
		private IRegion fActiveRegion;
		private Cursor fCursor;

		public void mouseDoubleClick(MouseEvent e) {
		}
		public void mouseDown(MouseEvent event) {
			
			if (!fActive)
				return;
				
			if (event.stateMask != SWT.CONTROL) {
				deactivate();
				return;	
			}
			
			if (event.button != 1) {
				deactivate();
				return;	
			}			
		}

		public void mouseUp(MouseEvent e) {

			if (!fActive)
				return;
				
			if (e.button != 1) {
				deactivate();
				return;
			}
			
			boolean wasActive = fCursor != null;
			
			IRegion region = fActiveRegion;
				
			deactivate();

			if (wasActive) {
				getSourceViewer().setSelectedRange(region.getOffset(), region.getLength());
				getGrammarSourceViewer().selectWord(region);
			}
		}

		public void paintControl(PaintEvent event) {	
			if (fActiveRegion == null)
				return;
	
			ISourceViewer viewer = getSourceViewer();
			if (viewer == null)
				return;
				
			StyledText text= viewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;
				
				
			int offset = 0;
			int length = 0;

			if (viewer instanceof ITextViewerExtension5) {

				ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
				IRegion widgetRange = extension.modelRange2WidgetRange(fActiveRegion);
				if (widgetRange == null)
					return;

				offset = widgetRange.getOffset();
				length = widgetRange.getLength();

			} else {

				IRegion region = viewer.getVisibleRegion();
				if (!includes(region, fActiveRegion))
					return;

				offset = fActiveRegion.getOffset() - region.getOffset();
				length = fActiveRegion.getLength();
			}
			
			// support for bidi
			Point minLocation = getMinimumLocation(text, offset, length);
			Point maxLocation = getMaximumLocation(text, offset, length);

			int x1 = minLocation.x;
			int x2 = minLocation.x + maxLocation.x - minLocation.x - 1;
			int y = minLocation.y + text.getLineHeight() - 1;

			GC gc = event.gc;
			gc.setForeground(fColorManager.getColor(PreferenceConverter.getColor(getPreferenceStore(), PREFERENCE_COLOR_LINK)));
			gc.drawLine(x1, y, x2, y);
		}

		public void applyTextPresentation(TextPresentation textPresentation) {
			if (fActiveRegion == null)
				return;
			IRegion region = textPresentation.getExtent();
			if (fActiveRegion.getOffset() + fActiveRegion.getLength() >= region.getOffset() && region.getOffset() + region.getLength() > fActiveRegion.getOffset())
				textPresentation.mergeStyleRange(new StyleRange(fActiveRegion.getOffset(), fActiveRegion.getLength(), fColorManager.getColor(PreferenceConverter.getColor(getPreferenceStore(), PREFERENCE_COLOR_LINK)), null));
		}
		
		public void keyPressed(KeyEvent e) {
		}
		
		public void keyReleased(KeyEvent event) {
			
			if (!fActive)
				return;

			deactivate();				
		}

		public void mouseMove(MouseEvent event) {
			if (event.widget instanceof Control && !((Control) event.widget).isFocusControl()) {
				deactivate();
				return;
			}
			
			if (!fActive) {
				if (event.stateMask != SWT.CONTROL)
					return;
				// modifier was already pressed
				fActive = true;
			}

			ISourceViewer viewer = getSourceViewer();
			if (viewer == null) {
				deactivate();
				return;
			}
				
			StyledText text = viewer.getTextWidget();
			if (text == null || text.isDisposed()) {
				deactivate();
				return;
			}
				
			if ((event.stateMask & SWT.BUTTON1) != 0 && text.getSelectionCount() != 0) {
				deactivate();
				return;
			}
		
			IRegion region = getCurrentTextRegion(viewer);
			if (region == null || region.getLength() == 0) {
				repairRepresentation();
				return;
			}

			highlightRegion(viewer, region);
			activateCursor();												
		}

		private void deactivate() {
			deactivate(false);
		}
		
		public void dispose() {
			if (fCursor != null)
				fCursor.dispose();
		}

		private void deactivate(boolean redrawAll) {
			if (!fActive)
				return;

			repairRepresentation(redrawAll);			
			fActive = false;
		}

		private void activateCursor() {
			ISourceViewer viewer = getSourceViewer();
			StyledText text = viewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;
			Display display = text.getDisplay();
			if (fCursor == null)
				fCursor = new Cursor(display, SWT.CURSOR_HAND);
			text.setCursor(fCursor);
		}

		private void resetCursor(ISourceViewer viewer) {
			StyledText text = viewer.getTextWidget();
			if (text != null && !text.isDisposed())
				text.setCursor(null);

			if (fCursor != null) {
				fCursor.dispose();
				fCursor = null;
			}
		}

		private void repairRepresentation() {			
			repairRepresentation(false);
		}

		private void repairRepresentation(boolean redrawAll) {			

			if (fActiveRegion == null)
				return;
			
			int offset = fActiveRegion.getOffset();
			int length = fActiveRegion.getLength();
			fActiveRegion = null;

			ISourceViewer viewer = getSourceViewer();
			if (viewer != null) {
				
				resetCursor(viewer);
				
				// Invalidate ==> remove applied text presentation
				if (!redrawAll && viewer instanceof ITextViewerExtension2)
					((ITextViewerExtension2) viewer).invalidateTextPresentation(offset, length);
				else
					viewer.invalidateTextPresentation();
				
				// Remove underline
				if (viewer instanceof ITextViewerExtension5) {
					ITextViewerExtension5 extension= (ITextViewerExtension5) viewer;
					offset= extension.modelOffset2WidgetOffset(offset);
				} else {
					offset -= viewer.getVisibleRegion().getOffset();
				}
				try {
					StyledText text= viewer.getTextWidget();

					text.redrawRange(offset, length, false);
				} catch (IllegalArgumentException e) {
					GrammarEditorPlugin.logError("Cannot repair the presentation", e);
				}
			}
		}

		private void highlightRegion(ISourceViewer viewer, IRegion region) {

			if (region.equals(fActiveRegion))
				return;

			repairRepresentation();
			
			StyledText text = viewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;

			
			// Underline
			int offset = 0;
			int length = 0;
			if (viewer instanceof ITextViewerExtension5) {
				ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
				IRegion widgetRange = extension.modelRange2WidgetRange(region);
				if (widgetRange == null)
					return;

				offset = widgetRange.getOffset();
				length = widgetRange.getLength();

			} else {
				offset = region.getOffset() - viewer.getVisibleRegion().getOffset();
				length = region.getLength();
			}
			text.redrawRange(offset, length, false);
			
			// Invalidate region ==> apply text presentation
			fActiveRegion = region;
			if (viewer instanceof ITextViewerExtension2)
				((ITextViewerExtension2) viewer).invalidateTextPresentation(region.getOffset(), region.getLength());
			else
				viewer.invalidateTextPresentation();
		}

		private IRegion getCurrentTextRegion(ISourceViewer viewer) {

			int offset= getCurrentTextOffset(viewer);				
			if (offset == -1)
				return null;
			return getGrammarSourceViewer().getSelectedWord(viewer.getDocument(), offset);
		}

		private int getCurrentTextOffset(ISourceViewer viewer) {

			try {
				StyledText text = viewer.getTextWidget();
				if (text == null || text.isDisposed())
					return -1;

				Display display = text.getDisplay();
				Point absolutePosition = display.getCursorLocation();
				Point relativePosition = text.toControl(absolutePosition);

				int widgetOffset = text.getOffsetAtLocation(relativePosition);
				if (viewer instanceof ITextViewerExtension5) {
					ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
					return extension.widgetOffset2ModelOffset(widgetOffset);
				} else {
					return widgetOffset + viewer.getVisibleRegion().getOffset();
				}

			} catch (IllegalArgumentException e) {
				return -1;
			}
		}

		private boolean includes(IRegion region, IRegion position) {
			return position.getOffset() >= region.getOffset()
					&& position.getOffset() + position.getLength() <= region.getOffset()
							+ region.getLength();
		}

		private Point getMinimumLocation(StyledText text, int offset, int length) {
			Point minLocation = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);

			for (int i = 0; i <= length; i++) {
				Point location = text.getLocationAtOffset(offset + i);

				if (location.x < minLocation.x)
					minLocation.x = location.x;
				if (location.y < minLocation.y)
					minLocation.y = location.y;
			}

			return minLocation;
		}

		private Point getMaximumLocation(StyledText text, int offset, int length) {
			Point maxLocation = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

			for (int i = 0; i <= length; i++) {
				Point location = text.getLocationAtOffset(offset + i);

				if (location.x > maxLocation.x)
					maxLocation.x = location.x;
				if (location.y > maxLocation.y)
					maxLocation.y = location.y;
			}

			return maxLocation;
		}
	};

	private class EditorSelectionChangeListener implements ISelectionChangedListener {
		public void selectionChanged(SelectionChangedEvent event) {
			handleEditorSelectionChanged();
		}

		public void install() {
			ISelectionProvider selectionProvider = getSelectionProvider();
			if (selectionProvider instanceof IPostSelectionProvider)
				((IPostSelectionProvider) selectionProvider).addPostSelectionChangedListener(this);
			else
				selectionProvider.addSelectionChangedListener(this);
		}

		public void uninstall() {
			ISelectionProvider selectionProvider = getSelectionProvider();
			if (selectionProvider instanceof IPostSelectionProvider)
				((IPostSelectionProvider) selectionProvider).removePostSelectionChangedListener(this);
			else
				selectionProvider.removeSelectionChangedListener(this);
		}
	};
	
	private class OccurrencesFinder extends IdFinder {
		private List fResult = new ArrayList();
		public OccurrencesFinder(Document document, String id, int filter) {
			super(document, id, filter);
		}
		
		protected boolean doVisit(Node node, ModelBase element) {
			if (!node.spansMultipleNodes() && matches(element.getLabel(element)))
				fResult.add(node);
			return true;
		}
		
		public Node[] findOccurrences() {
			document.getRoot().accept(this);
			return (Node[]) fResult.toArray(new Node[fResult.size()]);
		}
	};

	private GrammarOutlinePage fOutlinePage;
	private ISelectionChangedListener fOutlineSelectionChangedListener;
	private EditorSelectionChangeListener fEditorSelectionChangedListener;
	private Listener fListener;
	private ProjectionSupport fProjectionSupport;
	private GrammarFoldingStructureProvider fFoldingStructureProvider;
    private FoldingActionGroup fFoldingGroup;
	private ColorManager fColorManager;
	private boolean fMarkOccurrenceAnnotations;
	private ModelBase fLastSelectedElement;

	public final static String ACTION_SHOW_OUTLINE = "showOutline";
	public final static String ACTION_CONTENT_ASSIST = "contentAssist";
	public final static String ACTION_GOTO_DECLARATION = "gotoDeclaration";
	public final static String ACTION_SHOW_DECLARATION = "showDeclaraion";
	public final static String ACTION_TOGGLE_COMMENT = "toggleComment";
	public final static String ACTION_FIND_OCCURRENCES = "findOccurrences";
	public final static String ACTION_RENAME_IN_FILE = "renameInFile";
	public final static String ACTION_FOLDING_TOGGLE = "foldingToggle";
	public final static String ACTION_FOLDING_EXPAND_ALL = "foldingExpandAll";
	
	private final static String PREFERENCE_COLOR_LINK = PreferenceConstants.GRAMMAR_COLORING_LINK;

	public GrammarEditor() {
		super();
		fColorManager = GrammarEditorPlugin.getColorManager();
		setDocumentProvider(new GrammarDocumentProvider());
	}
	
	protected void createActions() {
		super.createActions();
		ResourceBundle bundle = GrammarEditorPlugin.getDefault().getResourceBundle();

		IAction action = new TextOperationAction(bundle, "showOutline.", this,
				GrammarSourceViewer.SHOW_OUTLINE);
		action.setActionDefinitionId(IGrammarEditorActionDefinitionIds.SHOW_OUTLINE);
		setAction(ACTION_SHOW_OUTLINE, action);
		action = new TextOperationAction(bundle, "gotoDeclaration.", this,
				GrammarSourceViewer.GOTO_DECLARATION);
		action.setActionDefinitionId(IGrammarEditorActionDefinitionIds.GOTO_DECLARATION);
		setAction(ACTION_GOTO_DECLARATION, action);
		action = new ContentAssistAction(bundle, "contentassist.", this);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction(ACTION_CONTENT_ASSIST, action);
		action = new TextOperationAction(bundle, "showDeclaration.", this,
				ISourceViewer.INFORMATION, true);
		action.setActionDefinitionId(IGrammarEditorActionDefinitionIds.SHOW_DECLARATION);
		setAction(ACTION_SHOW_DECLARATION, action);
		action = new ToggleCommentAction(bundle, "toggleComment.", this);
		action.setActionDefinitionId(IGrammarEditorActionDefinitionIds.TOGGLE_COMMENT);
		setAction(ACTION_TOGGLE_COMMENT, action);
		markAsStateDependentAction(ACTION_TOGGLE_COMMENT, true);
		configureToggleCommentAction();
		action = new FindOccurrencesInFileAction(bundle, "findOccurrences.", this);
		action.setActionDefinitionId(IGrammarEditorActionDefinitionIds.FIND_OCCURRENCES);
		setAction(ACTION_FIND_OCCURRENCES, action);
		action = new RenameInFileAction(this);
		action.setActionDefinitionId(IGrammarEditorActionDefinitionIds.RENAME_IN_FILE);
		setAction(ACTION_RENAME_IN_FILE, action);
		
		fFoldingGroup = new FoldingActionGroup(this, getSourceViewer());
	}

	private void configureToggleCommentAction() {
		IAction action= getAction(ACTION_TOGGLE_COMMENT);
		if (action instanceof ToggleCommentAction) {
			ISourceViewer sourceViewer= getSourceViewer();
			SourceViewerConfiguration configuration= getSourceViewerConfiguration();
			((ToggleCommentAction)action).configure(sourceViewer, configuration);
		}
	}

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		if (getSourceViewer() instanceof ProjectionViewer) {
			ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer(); 
	        createFoldingSupport(projectionViewer);

			if (isFoldingEnabled())
		        projectionViewer.doOperation(ProjectionViewer.TOGGLE);
		}
		fListener = new Listener();
		getSourceViewer().getTextWidget().addMouseMoveListener(fListener);
		getSourceViewer().getTextWidget().addMouseListener(fListener);
		getSourceViewer().getTextWidget().addKeyListener(fListener);
		getSourceViewer().getTextWidget().addPaintListener(fListener);
		((ITextViewerExtension4) getSourceViewer()).addTextPresentationListener(fListener);
		fEditorSelectionChangedListener = new EditorSelectionChangeListener();
		fEditorSelectionChangedListener.install();
	}
	
	protected void initializeEditor() {
		super.initializeEditor();
		IPreferenceStore store = GrammarEditorPlugin.getDefault().getCombinedPreferenceStore();
		setPreferenceStore(store);
		fMarkOccurrenceAnnotations = store.getBoolean(PreferenceConstants.EDITOR_MARK_OCCURRENCES);
		updateOccurrencesAnnotations();
	}
	
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "gedit.context" });  //$NON-NLS-1$
	}
	
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		menu.appendToGroup(ITextEditorActionConstants.GROUP_SAVE, new Separator("group.open"));
		IAction action = getAction(ACTION_GOTO_DECLARATION);
		menu.appendToGroup("group.open", action);
		menu.appendToGroup(ITextEditorActionConstants.GROUP_COPY, new Separator("group.search"));
		action = getAction(ACTION_FIND_OCCURRENCES);
		menu.appendToGroup("group.search", action);
		action = getAction(ACTION_RENAME_IN_FILE);
		menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, action);
	}
	
	protected String[] collectContextMenuPreferencePages() {
		String[] ids = super.collectContextMenuPreferencePages();
		List more = new ArrayList();
		more.add("gedit.editorPreferencePage"); //$NON-NLS-1$
		more.add("gedit.foldingPreferencePage"); //$NON-NLS-1$
		more.addAll(Arrays.asList(ids));
		return (String[]) more.toArray(new String[more.size()]);
	}

	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		fAnnotationAccess = createAnnotationAccess();
		fOverviewRuler = createOverviewRuler(getSharedColors());
		GrammarSourceViewer viewer = new GrammarSourceViewer(parent, ruler, getOverviewRuler(),
				isOverviewRulerVisible(), styles, getPreferenceStore());
		// ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);
		setSourceViewerConfiguration(new GrammarSourceViewerConfiguration(fColorManager, getPreferenceStore(), viewer));
		viewer.addReconcilingListener(this);
		return viewer;
	}
	
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		return ((GrammarSourceViewerConfiguration) getSourceViewerConfiguration()).affectsTextPresentation(event) || super.affectsTextPresentation(event);
	}

	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		try {
			if (getSourceViewer()== null)
				return;
			((GrammarSourceViewerConfiguration) getSourceViewerConfiguration()).adaptToPreferenceChange(event);

			if (PreferenceConstants.EDITOR_MARK_OCCURRENCES.equals(event.getProperty())) {
				boolean newValue = ((Boolean) event.getNewValue()).booleanValue();
				if (newValue != fMarkOccurrenceAnnotations) {
					fMarkOccurrenceAnnotations = newValue;
					fLastSelectedElement = null;
					updateOccurrencesAnnotations();
				}
				return;
			}
		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}

	protected void createFoldingSupport(ProjectionViewer projectionViewer) {
		fProjectionSupport = new ProjectionSupport(projectionViewer, getAnnotationAccess(), getSharedColors());
    	fProjectionSupport.setHoverControlCreator(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell shell) {
				return new GrammarInformationControl(shell);
			}
		});
        fProjectionSupport.install();
		((ProjectionViewer) getSourceViewer()).addProjectionListener(this);
	}
	
	protected void rulerContextMenuAboutToShow(IMenuManager menu) {
		super.rulerContextMenuAboutToShow(menu);

		IMenuManager foldingMenu = new MenuManager(GrammarEditorPlugin.getResourceString("Folding.menu")); //$NON-NLS-1$
		if (menu.find(ITextEditorActionConstants.GROUP_RULERS) != null)
			menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, foldingMenu);
		else {
			menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
			menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, foldingMenu);
		}
		IAction action = getAction(ACTION_FOLDING_TOGGLE);
		foldingMenu.add(action);
		foldingMenu.add(new Separator());
		action = getAction(ACTION_FOLDING_EXPAND_ALL);
		foldingMenu.add(action);
		fFoldingGroup.update();
	}

	public void dispose() {
		getGrammarSourceViewer().removeReconcilingListener(this);
		if (fOutlinePage != null) {
			fOutlinePage.removeSelectionChangedListener(fOutlineSelectionChangedListener);
			fOutlinePage.dispose();
		}
		fEditorSelectionChangedListener.uninstall();
		((ITextViewerExtension4) getSourceViewer()).removeTextPresentationListener(fListener);
		fListener.dispose();
		fFoldingStructureProvider = null;
		((GrammarSourceViewerConfiguration) getSourceViewerConfiguration()).dispose();
		super.dispose();
	}
	
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		if (fOutlinePage != null)
			fOutlinePage.setInput(getGrammarSourceViewer().getModel(false));
		configureToggleCommentAction();
	}
	
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
			if (fOutlinePage == null) {
				fOutlinePage = new GrammarOutlinePage();
				if (fOutlineSelectionChangedListener == null)
					fOutlineSelectionChangedListener = new ISelectionChangedListener() {
						public void selectionChanged(SelectionChangedEvent event) {
							handleOutlineSelectionChanged();
						}
				};
				fOutlinePage.addSelectionChangedListener(fOutlineSelectionChangedListener);
				getEditorSite().getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						fOutlinePage.setInput(getGrammarSourceViewer().getModel(false));
					}
				});
			}
			return fOutlinePage;
		}
        if (fProjectionSupport != null) { 
        	Object object = fProjectionSupport.getAdapter(getSourceViewer(), adapter);
        	if (object != null)
        		return object;
        }			
		return super.getAdapter(adapter);
	}
	
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
		getGrammarSourceViewer().getModel(true);
	}
	
	private GrammarSourceViewer getGrammarSourceViewer() {
		return (GrammarSourceViewer) getSourceViewer();
	}
	
	public ISourceViewer getViewer() {
		return getSourceViewer();
	}
	
	public ModelBase getSelectedElement() {
		GrammarSourceViewer viewer = getGrammarSourceViewer();
		if (viewer == null)
			return null;
		return viewer.getSelectedElement();
	}
	
	private void handleOutlineSelectionChanged() {
		IStructuredSelection selection = (IStructuredSelection) fOutlinePage.getSelection();
		if (selection.size() != 1)
			return;
		ModelBase model = (ModelBase) selection.getFirstElement();
		getGrammarSourceViewer().selectElement(model);
	}
	
	protected void handleEditorSelectionChanged() {
		GrammarSourceViewer viewer = getGrammarSourceViewer();
		if (viewer == null)
			return;
		ModelBase element = getSelectedElement();
		if (element != null)
			viewer.setRangeIndication(element, false);
		if (element instanceof Reference)
			element = (ModelBase) ((Reference) element).getParent(element);
		updateProblemStatus(viewer);
		synchronizeOutlineSelection(element);
		if (fMarkOccurrenceAnnotations)
			updateOccurrencesAnnotations();
	}

	protected boolean isActivePart() {
		IWorkbenchPart activePart = getSite().getWorkbenchWindow().getActivePage().getActivePart();
		return activePart != null && activePart.equals(this);
	}
	
	private void updateProblemStatus(GrammarSourceViewer viewer) {
		Problem[] problems = viewer.getSelectedProblems();
		setStatusMessage(null, IMessageProvider.ERROR);
		if (problems.length == 0)
			setStatusMessage(null, IMessageProvider.WARNING);
		for (int i = 0; i < problems.length; i++) {
			setStatusMessage(problems[i].getMessage(), IMessageProvider.WARNING);
		}
	}

	protected void synchronizeOutlineSelection(ModelBase element) {
		if (element == null || fOutlinePage == null || !fOutlinePage.isLinkingEnabled())
			return;
		fOutlinePage.removeSelectionChangedListener(fOutlineSelectionChangedListener);
		fOutlinePage.setSelection(new StructuredSelection(element));
		fOutlinePage.addSelectionChangedListener(fOutlineSelectionChangedListener);
	}

	protected void setStatusMessage(String msg, int type) {
		switch (type) {
			case IMessageProvider.ERROR:
				getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(msg);
			default :
				getEditorSite().getActionBars().getStatusLineManager().setMessage(msg);
		}
	}

	public void gotoError(boolean forward) {
		ISelectionProvider provider = getSelectionProvider();

		ITextSelection s = (ITextSelection) provider.getSelection();
		Position errorPosition = new Position(0, 0);
		Annotation nextError = getNextAnnotation(s.getOffset(), forward, errorPosition);
		if (nextError != null) {

			IMarker marker = null;
			if (nextError instanceof MarkerAnnotation)
				marker = ((MarkerAnnotation) nextError).getMarker();

			if (marker != null) {
				IWorkbenchPage page = getSite().getPage();
				IViewPart view = page.findView("org.eclipse.ui.views.TaskList"); //$NON-NLS-1$
				if (view != null) {
					try {
						view.getClass().getMethod("setSelection", new Class[] { ISelection.class, boolean.class }).
								invoke(view, new Object[] {new StructuredSelection(marker), new Boolean(true)});
					} catch (Exception ignore) {
					}
				}
			}

			selectAndReveal(errorPosition.getOffset(), errorPosition.getLength());
			setStatusMessage(nextError.getText(), IMessageProvider.WARNING);

		} else {
			setStatusMessage(null, IMessageProvider.WARNING);
		}
	}

	private Annotation getNextAnnotation(int offset, boolean forward, Position errorPosition) {
		Annotation nextError = null;
		Position nextErrorPosition = null;

		IDocument document = getDocumentProvider().getDocument(getEditorInput());
		int endOfDocument = document.getLength();
		int distance = 0;

		IAnnotationModel model = getDocumentProvider().getAnnotationModel(getEditorInput());
		for (Iterator it = model.getAnnotationIterator(); it.hasNext(); ) {
			Annotation annotation = (Annotation) it.next();
			if (!isNavigationTarget(annotation))
				continue;
			Position p = model.getPosition(annotation);
			if (!p.includes(offset)) {
				int currentDistance = 0;
				if (forward) {
					currentDistance = p.getOffset() - offset;
					if (currentDistance < 0)
						currentDistance = endOfDocument - offset + p.getOffset();
				} else {
					currentDistance = offset - p.getOffset();
					if (currentDistance < 0)
						currentDistance = offset + endOfDocument - p.getOffset();
				}

				if (nextError == null || currentDistance < distance) {
					distance = currentDistance;
					nextError = annotation;
					nextErrorPosition = p;
				}
			}
		}

		if (nextErrorPosition != null) {
			errorPosition.setOffset(nextErrorPosition.getOffset());
			errorPosition.setLength(nextErrorPosition.getLength());
		}

		return nextError;
	}

	protected boolean isNavigationTarget(Annotation annotation) {
		Preferences preferences = EditorsUI.getPluginPreferences();
		AnnotationPreference preference = getAnnotationPreferenceLookup().getAnnotationPreference(annotation);
		String key = preference == null ? null : preference.getIsGoToNextNavigationTargetKey();
		return (key != null && preferences.getBoolean(key));
	}

	protected boolean isFoldingEnabled() {
		return GrammarEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED);
	}
	
	public void projectionEnabled() {
    	if (getSourceViewer().getDocument() == null)
    		return;
    	fFoldingStructureProvider = new GrammarFoldingStructureProvider(this);
		fFoldingStructureProvider.setDocument(getDocumentProvider().getDocument(getEditorInput()));
		getSourceViewer().getTextWidget().getDisplay().asyncExec(new Runnable() {
			public void run() {
				GrammarSourceViewer viewer = getGrammarSourceViewer();
				if (viewer != null)
					fFoldingStructureProvider.updateFoldingRegions(viewer.getModel(false), true);
			}
		});
	}

	public void projectionDisabled() {
    	fFoldingStructureProvider = null;
	}

	public void reconciled() {
		GrammarSourceViewer viewer = getGrammarSourceViewer();
		if (viewer == null)
			return;
    	if (fFoldingStructureProvider != null) {
    		fFoldingStructureProvider.updateFoldingRegions(viewer.getModel(false), false);
    	}
    	getSite().getShell().getDisplay().asyncExec(new Runnable() {
    		public void run() {
    	    	handleEditorSelectionChanged();
    		}
    	});
	}

	public boolean isMarkingOccurrences() {
		return fMarkOccurrenceAnnotations;
	}
	
	public Node[] findOccurrences(String id) {
		OccurrencesFinder occurrencesFinder = new OccurrencesFinder(getGrammarSourceViewer().
				getModel(true), id, -1);
		return occurrencesFinder.findOccurrences();
	}

	private void updateOccurrencesAnnotations() {
		ModelBase element = getSelectedElement();
		if (fMarkOccurrenceAnnotations && (element == null || element == fLastSelectedElement))
			return;
		fLastSelectedElement = element;
		Node[] occurrences = null;
		if (fMarkOccurrenceAnnotations)
			occurrences = findOccurrences(element.getLabel(element));
		if (getDocumentProvider() == null)
			return;
		IAnnotationModel annotationModel = getDocumentProvider().getAnnotationModel(getEditorInput());
		for (Iterator it = annotationModel.getAnnotationIterator(); it.hasNext(); ) {
			Annotation annotation = (Annotation) it.next();
			if (GrammarDocumentProvider.ANNOTATION_OCCURRENCE.equals(annotation.getType()))
				annotationModel.removeAnnotation(annotation);
		}
		if (occurrences == null)
			return;
		for (int i = 0; i < occurrences.length; i++) {
			Node occurrence = occurrences[i];
			annotationModel.addAnnotation(new Annotation(GrammarDocumentProvider.ANNOTATION_OCCURRENCE,
					false, element.getLabel(element)), new Position(occurrence.getOffset(), occurrence.getLength()));
		}
	}
}
