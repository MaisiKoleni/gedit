/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor.actions;

import java.util.ResourceBundle;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.editors.text.IFoldingCommandIds;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextOperationAction;

import gedit.GrammarEditorPlugin;
import gedit.editor.GrammarEditor;

public class FoldingActionGroup extends ActionGroup {
	private ProjectionViewer fViewer;
	private TextOperationAction fToggle;
	private TextOperationAction fExpand;
	private TextOperationAction fCollapse;
	private TextOperationAction fExpandAll;

	private IProjectionListener fProjectionListener;

	public FoldingActionGroup(ITextEditor editor, ITextViewer viewer) {
		if (!(viewer instanceof ProjectionViewer))
			return;
		fViewer = (ProjectionViewer) viewer;

		fProjectionListener = new IProjectionListener() {

			@Override
			public void projectionEnabled() {
				update();
			}

			@Override
			public void projectionDisabled() {
				update();
			}
		};

		fViewer.addProjectionListener(fProjectionListener);

		ResourceBundle bundle = GrammarEditorPlugin.getDefault().getResourceBundle();

		fToggle = new TextOperationAction(bundle, "FoldingToggle.", editor, ProjectionViewer.TOGGLE, true); //$NON-NLS-1$
		fToggle.setChecked(true);
		fToggle.setActionDefinitionId(IFoldingCommandIds.FOLDING_TOGGLE);
		editor.setAction(GrammarEditor.ACTION_FOLDING_TOGGLE, fToggle);

		fExpandAll = new TextOperationAction(bundle, "FoldingExpandAll.", editor, ProjectionViewer.EXPAND_ALL, true); //$NON-NLS-1$
		fExpandAll.setActionDefinitionId(IFoldingCommandIds.FOLDING_EXPAND_ALL);
		editor.setAction(GrammarEditor.ACTION_FOLDING_EXPAND_ALL, fExpandAll);

		fExpand = new TextOperationAction(bundle, "FoldingExpand.", editor, ProjectionViewer.EXPAND, true); //$NON-NLS-1$
		fExpand.setActionDefinitionId(IFoldingCommandIds.FOLDING_EXPAND);
		editor.setAction("FoldingExpand", fExpand); //$NON-NLS-1$

		fCollapse = new TextOperationAction(bundle, "FoldingCollapse.", editor, ProjectionViewer.COLLAPSE, true); //$NON-NLS-1$
		fCollapse.setActionDefinitionId(IFoldingCommandIds.FOLDING_COLLAPSE);
		editor.setAction("FoldingCollapse", fCollapse); //$NON-NLS-1$
	}

	private boolean isEnabled() {
		return fViewer != null;
	}

	@Override
	public void dispose() {
		if (isEnabled()) {
			fViewer.removeProjectionListener(fProjectionListener);
			fViewer= null;
		}
		super.dispose();
	}

	public void update() {
		if (isEnabled()) {
			fToggle.update();
			fToggle.setChecked(fViewer.getProjectionAnnotationModel() != null);
			fExpandAll.update();
			fExpand.update();
			fCollapse.update();
		}
	}

	@Override
	public void updateActionBars() {
		update();
	}

}
