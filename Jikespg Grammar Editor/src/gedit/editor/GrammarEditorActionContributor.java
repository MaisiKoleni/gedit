/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.editor.actions.ToggleMarkOccurrencesAction;

import java.util.ResourceBundle;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.ui.texteditor.TextEditorAction;

public class GrammarEditorActionContributor extends TextEditorActionContributor {
	private class GotoAnnotationAction extends TextEditorAction {
		private boolean fForward;

		public GotoAnnotationAction(ResourceBundle bundle, String prefix, boolean forward) {
			super(bundle, prefix, null);
			fForward = forward;
		}

		public void run() {
			GrammarEditor editor = (GrammarEditor) getTextEditor();
			editor.gotoAnnotation(fForward);
		}

		public void setEditor(ITextEditor editor) {
			super.setEditor(editor);
			update();
		}

		public void update() {
			setEnabled(getTextEditor() instanceof GrammarEditor);
		}
	};

	private RetargetTextEditorAction fShowOutlineAction;
	private RetargetTextEditorAction fContentAssistProposal;
	private RetargetTextEditorAction fShowDeclarationAction;
	private RetargetTextEditorAction fToggleCommentAction;
	private GotoAnnotationAction fPreviousAnnotationAction;
	private GotoAnnotationAction fNextAnnotationAction;
	private RetargetTextEditorAction fFindOccurrencesAction;
	private ToggleMarkOccurrencesAction fToggleMarkOccurrencesAction;
	
	public GrammarEditorActionContributor() {
		ResourceBundle bundle = GrammarEditorPlugin.getDefault().getResourceBundle();

		fShowOutlineAction = new RetargetTextEditorAction(bundle, "showOutline."); //$NON-NLS-1$
		fShowOutlineAction.setActionDefinitionId(IGrammarEditorActionDefinitionIds.SHOW_OUTLINE);
		fContentAssistProposal = new RetargetTextEditorAction(bundle, "contentassist."); //$NON-NLS-1$
		fContentAssistProposal.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		fShowDeclarationAction = new RetargetTextEditorAction(bundle, "showDeclaration."); //$NON-NLS-1$
		fShowDeclarationAction.setActionDefinitionId(IGrammarEditorActionDefinitionIds.SHOW_DECLARATION);
		fToggleCommentAction = new RetargetTextEditorAction(bundle, "toggleComment."); //$NON-NLS-1$
		fToggleCommentAction.setActionDefinitionId(IGrammarEditorActionDefinitionIds.TOGGLE_COMMENT);
		fPreviousAnnotationAction = new GotoAnnotationAction(bundle, "previousAnnotation.", false); //$NON-NLS-1$
		fPreviousAnnotationAction.setActionDefinitionId("org.eclipse.ui.navigate.previous"); //$NON-NLS-1$
		fNextAnnotationAction = new GotoAnnotationAction(bundle, "nextAnnotation.", true); //$NON-NLS-1$
		fNextAnnotationAction.setActionDefinitionId("org.eclipse.ui.navigate.next"); //$NON-NLS-1$
		fFindOccurrencesAction = new RetargetTextEditorAction(bundle, "findOccurrences."); //$NON-NLS-1$
		fFindOccurrencesAction.setActionDefinitionId(IGrammarEditorActionDefinitionIds.FIND_OCCURRENCES);
		fToggleMarkOccurrencesAction = new ToggleMarkOccurrencesAction();
		fToggleMarkOccurrencesAction.setActionDefinitionId(IGrammarEditorActionDefinitionIds.TOGGLE_MARK_OCCURRENCES);
	}
	
	public void init(IActionBars bars) {
		super.init(bars);
		bars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_NEXT_ANNOTATION, fNextAnnotationAction);
		bars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_PREVIOUS_ANNOTATION, fPreviousAnnotationAction);
		bars.setGlobalActionHandler(ActionFactory.NEXT.getId(), fNextAnnotationAction);
		bars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), fPreviousAnnotationAction);
		bars.setGlobalActionHandler(IGrammarEditorActionDefinitionIds.SHOW_DECLARATION, fShowDeclarationAction);
		bars.setGlobalActionHandler(IGrammarEditorActionDefinitionIds.TOGGLE_COMMENT, fToggleCommentAction);
		bars.setGlobalActionHandler(IGrammarEditorActionDefinitionIds.FIND_OCCURRENCES, fFindOccurrencesAction);
		bars.setGlobalActionHandler(IGrammarEditorActionDefinitionIds.TOGGLE_MARK_OCCURRENCES, fToggleMarkOccurrencesAction);
	}
	
	public void dispose() {
		fPreviousAnnotationAction.setEditor(null);
		fNextAnnotationAction.setEditor(null);
		fToggleMarkOccurrencesAction.setEditor(null);
		super.dispose();
	}

	public void contributeToMenu(IMenuManager menu) {
		super.contributeToMenu(menu);

		IMenuManager navMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE);
		navMenu.appendToGroup(IWorkbenchActionConstants.SHOW_EXT, fShowOutlineAction);

		IMenuManager editMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			editMenu.add(fToggleCommentAction);
			editMenu.add(fShowDeclarationAction);
			editMenu.add(fContentAssistProposal);
			editMenu.add(new Separator());
		}

		IMenuManager searchMenu = menu.findMenuUsingPath("org.eclipse.search.menu");
		if (searchMenu != null) {
			searchMenu.add(new Separator());
			searchMenu.add(fFindOccurrencesAction);
		}
	}

	public void setActiveEditor(IEditorPart part) {
		if (part instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) part;
			fShowOutlineAction.setAction(getAction(editor, GrammarEditor.ACTION_SHOW_OUTLINE));
			fContentAssistProposal.setAction(getAction(editor, GrammarEditor.ACTION_CONTENT_ASSIST));
			fShowDeclarationAction.setAction(getAction(editor, GrammarEditor.ACTION_SHOW_DECLARATION));
			fToggleCommentAction.setAction(getAction(editor, GrammarEditor.ACTION_TOGGLE_COMMENT));
			fPreviousAnnotationAction.setEditor(editor);
			fNextAnnotationAction.setEditor(editor);
			fFindOccurrencesAction.setAction(getAction(editor, GrammarEditor.ACTION_FIND_OCCURRENCES));
			fToggleMarkOccurrencesAction.setEditor(editor);
		}
		super.setActiveEditor(part);
	}
}
