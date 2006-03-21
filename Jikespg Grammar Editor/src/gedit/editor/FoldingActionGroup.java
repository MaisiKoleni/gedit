/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;

import java.util.ResourceBundle;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.editors.text.IFoldingCommandIds;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextOperationAction;

public class FoldingActionGroup extends ActionGroup {
	private ProjectionViewer fViewer;
	private TextOperationAction fToggle;
	private TextOperationAction fExpandAll;

	private IProjectionListener fProjectionListener;
	
	public FoldingActionGroup(ITextEditor editor, ITextViewer viewer) {
		if (!(viewer instanceof ProjectionViewer))
			return;
		fViewer = (ProjectionViewer) viewer;
		
		fProjectionListener = new IProjectionListener() {

			public void projectionEnabled() {
				update();
			}

			public void projectionDisabled() {
				update();
			}
		};
		
		fViewer.addProjectionListener(fProjectionListener);
		
		ResourceBundle bundle = GrammarEditorPlugin.getDefault().getResourceBundle();
		
		fToggle = new TextOperationAction(bundle, "FoldingToggle.", editor, ProjectionViewer.TOGGLE, true); //$NON-NLS-1$
		fToggle.setChecked(true);
		fToggle.setActionDefinitionId(IFoldingCommandIds.FOLDING_TOGGLE);
		editor.setAction("FoldingToggle", fToggle); //$NON-NLS-1$
		
		fExpandAll = new TextOperationAction(bundle, "FoldingExpandAll.", editor, ProjectionViewer.EXPAND_ALL, true); //$NON-NLS-1$
		fExpandAll.setActionDefinitionId(IFoldingCommandIds.FOLDING_EXPAND_ALL);
		editor.setAction("FoldingExpandAll", fExpandAll); //$NON-NLS-1$
	}
	
	private boolean isEnabled() {
		return fViewer != null;
	}
	
	public void dispose() {
		if (isEnabled()) {
			fViewer.removeProjectionListener(fProjectionListener);
			fViewer= null;
		}
		super.dispose();
	}
	
	protected void update() {
		if (isEnabled()) {
			fToggle.update();
			fToggle.setChecked(fViewer.getProjectionAnnotationModel() != null);
			fExpandAll.update();
		}
	}
	
	public void updateActionBars() {
		update();
	}

}
