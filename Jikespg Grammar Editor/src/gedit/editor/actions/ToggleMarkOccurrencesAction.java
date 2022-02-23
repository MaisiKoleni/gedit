/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor.actions;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

import gedit.GrammarEditorPlugin;
import gedit.editor.GrammarEditor;
import gedit.editor.PreferenceConstants;

public class ToggleMarkOccurrencesAction extends TextEditorAction implements IPropertyChangeListener {
	private IPreferenceStore fStore;

	public ToggleMarkOccurrencesAction() {
		super(GrammarEditorPlugin.getDefault().getResourceBundle(), "toggleMarkOccurrences.", null); //$NON-NLS-1$
		setImageDescriptor(GrammarEditorPlugin.getImageDescriptor("icons/mark_occurrences.gif")); //$NON-NLS-1$
	}

	@Override
	public void run() {
		fStore.setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, isChecked());
	}

	@Override
	public void update() {
		ITextEditor editor = getTextEditor();

		boolean checked = false;
		if (editor instanceof GrammarEditor)
			checked = ((GrammarEditor) editor).isMarkingOccurrences();

		setChecked(checked);
		setEnabled(editor != null);
	}

	@Override
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

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (PreferenceConstants.EDITOR_MARK_OCCURRENCES.equals(event.getProperty()))
			setChecked(Boolean.parseBoolean(event.getNewValue().toString()));
	}
}
