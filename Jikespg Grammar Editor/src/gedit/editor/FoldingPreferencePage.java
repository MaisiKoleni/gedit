/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import gedit.GrammarEditorPlugin;
import gedit.model.ModelType;
import gedit.model.ModelUtils;

public class FoldingPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private Button fEnable;
	private Button fFoldProductions;
	private Button fFoldComments;
	private Button fFoldMacros;
	private Button fFoldSelectedSection;
	private TableViewer fSectionsViewer;
	private Set<ModelType> fEnablementState;

	public FoldingPreferencePage() {
		setPreferenceStore(GrammarEditorPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite contents = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		contents.setLayout(layout);
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));

		fEnable = new Button(contents, SWT.CHECK);
		fEnable.setText("&Enable folding");
		fEnable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateButtonEnablement();
			}
		});

		createElementsGroup(contents);

		initialize();
		Dialog.applyDialogFont(contents);

		return contents;
	}

	private void createElementsGroup(Composite contents) {
		Group group = new Group(contents, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData());
		group.setText("&Initially fold the sections");

		fSectionsViewer = createSectionsViewer(group);

		fFoldSelectedSection = new Button(group, SWT.CHECK);
		fFoldSelectedSection.setText("&Fold");
		fFoldSelectedSection.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		fFoldSelectedSection.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleFoldSectionSelected();
			}
		});
		fFoldProductions = new Button(contents, SWT.CHECK);
		fFoldProductions.setText("Initially fold &productions (rules)");
		fFoldComments = new Button(contents, SWT.CHECK);
		fFoldComments.setText("Initially fold &comments");
		fFoldMacros = new Button(contents, SWT.CHECK);
		fFoldMacros.setText("Initially fold &macro blocks");
	}

	private TableViewer createSectionsViewer(Group group) {
		TableViewer viewer = new TableViewer(group, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new ModelLabelProvider());
		GridData data = new GridData(SWT.BEGINNING, SWT.FILL, false, true);
		data.heightHint = convertHeightInCharsToPixels(6);
		data.widthHint = convertWidthInCharsToPixels(30);
		viewer.getControl().setLayoutData(data);
		viewer.addSelectionChangedListener(event -> handleSectionsViewerSelectionChanged());
		return viewer;
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	private void initialize() {
		IPreferenceStore store = getPreferenceStore();

		fEnable.setSelection(store.getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED));
		fFoldProductions.setSelection(store.getBoolean(PreferenceConstants.EDITOR_FOLD_RULES));
		fFoldComments.setSelection(store.getBoolean(PreferenceConstants.EDITOR_FOLD_COMMENTS));
		fFoldMacros.setSelection(store.getBoolean(PreferenceConstants.EDITOR_FOLD_MACROS));
		initializeSections(store, false);
		updateButtonEnablement();
	}

	private void initializeSections(IPreferenceStore store, boolean defaultValues) {
		ModelType[] allTypes = ModelType.values();
		List<ModelType> entries = new ArrayList<>();
		fEnablementState = ModelUtils.createModelTypeSetFromString(defaultValues ? store.getDefaultString(PreferenceConstants.EDITOR_FOLD_SECTIONS)
				: store.getString(PreferenceConstants.EDITOR_FOLD_SECTIONS), PreferenceConstants.EDITOR_FOLDING_SEPARATOR);
		for (ModelType type : allTypes) {
			if (!type.isSectionType())
				continue;
			entries.add(type);
		}
		Collections.sort(entries);
		fSectionsViewer.setInput(entries);
	}

	private void updateButtonEnablement() {
		boolean enable = fEnable.getSelection();
		fFoldProductions.setEnabled(enable);
		fFoldComments.setEnabled(enable);
		fFoldMacros.setEnabled(enable);
		fFoldSelectedSection.setEnabled(enable && !fSectionsViewer.getSelection().isEmpty());
		fSectionsViewer.getControl().setEnabled(enable);
	}

	private void updateSectionButton(ModelType entry) {
		fFoldSelectedSection.setEnabled(entry != null);
		if (entry != null)
			fFoldSelectedSection.setSelection(fEnablementState.contains(entry));
	}

	private void handleSectionsViewerSelectionChanged() {
		IStructuredSelection selection = (IStructuredSelection) fSectionsViewer.getSelection();
		ModelType entry = (ModelType) selection.getFirstElement();
		updateSectionButton(entry);
	}

	private void handleFoldSectionSelected() {
		IStructuredSelection selection = (IStructuredSelection) fSectionsViewer.getSelection();
		ModelType entry = (ModelType) selection.getFirstElement();
		if (entry == null)
			return;
		if (fFoldSelectedSection.getSelection())
			fEnablementState.add(entry);
		else
			fEnablementState.remove(entry);
	}

	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();

		store.setValue(PreferenceConstants.EDITOR_FOLDING_ENABLED, fEnable.getSelection());
		store.setValue(PreferenceConstants.EDITOR_FOLD_RULES, fFoldProductions.getSelection());
		store.setValue(PreferenceConstants.EDITOR_FOLD_COMMENTS, fFoldComments.getSelection());
		store.setValue(PreferenceConstants.EDITOR_FOLD_MACROS, fFoldMacros.getSelection());

		store.setValue(PreferenceConstants.EDITOR_FOLD_SECTIONS, ModelUtils.createStringFromModelTypeSet(fEnablementState, PreferenceConstants.EDITOR_FOLDING_SEPARATOR));

		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		IPreferenceStore store = getPreferenceStore();

		fEnable.setSelection(store.getDefaultBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED));
		fFoldProductions.setSelection(store.getDefaultBoolean(PreferenceConstants.EDITOR_FOLD_RULES));
		fFoldComments.setSelection(store.getDefaultBoolean(PreferenceConstants.EDITOR_FOLD_COMMENTS));
		fFoldMacros.setSelection(store.getDefaultBoolean(PreferenceConstants.EDITOR_FOLD_MACROS));

		initializeSections(store, true);

		super.performDefaults();
	}

}
