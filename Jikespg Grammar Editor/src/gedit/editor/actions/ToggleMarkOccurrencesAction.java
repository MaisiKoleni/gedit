/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor.actions;

import gedit.GrammarEditorPlugin;
import gedit.editor.GrammarEditor;
import gedit.editor.PreferenceConstants;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public class ToggleMarkOccurrencesAction extends TextEditorAction implements IPropertyChangeListener {
	private IPreferenceStore fStore;

	public ToggleMarkOccurrencesAction() {
		super(GrammarEditorPlugin.getDefault().getResourceBundle(), "toggleMarkOccurrences.", null); //$NON-NLS-1$
		setImageDescriptor(GrammarEditorPlugin.getImageDescriptor("icons/mark_occurrences.gif")); //$NON-NLS-1$
	}

	public void run() {
		fStore.setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, isChecked());
	}

	public void update() {
		ITextEditor editor = getTextEditor();

		boolean checked = false;
		if (editor instanceof GrammarEditor)
			checked = ((GrammarEditor) editor).isMarkingOccurrences();

		setChecked(checked);
		setEnabled(editor != null);
	}

	public void setEditor(ITextEditor editor) {
		super.setEditor(editor);
		if (editor != null) {

			if (fStore == null) {
				fStore = GrammarEditorPlugin.getDefault().getPreferenceStore();
				fStore.addPropertyChangeListener(this);
			}

		} else if (fStore != null) {
			fStore.removePropertyChangeListener(this);
			fStore = null;
		}

		update();
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(PreferenceConstants.EDITOR_MARK_OCCURRENCES))
			setChecked(Boolean.valueOf(event.getNewValue().toString()).booleanValue());
	}
}
