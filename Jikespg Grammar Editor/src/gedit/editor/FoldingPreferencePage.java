/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.model.Document;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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

public class FoldingPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private class SectionTableEntry {
		private String fSection;
		private boolean fState;

		public SectionTableEntry(String section, boolean state) {
			fSection = section;
			fState = state;
		}
		
		public String toString() {
			return fSection;
		}
	};
	
	private Button fEnable;
	private Button fFoldSubElements;
	private Button fFoldSelectedSection;
	private TableViewer fSectionsViewer;
	
	public FoldingPreferencePage() {
		setPreferenceStore(GrammarEditorPlugin.getDefault().getPreferenceStore());
	}

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
			public void widgetSelected(SelectionEvent e) {
				handleFoldSectionSelected();
			}
		});
		fFoldSubElements = new Button(contents, SWT.CHECK);
		fFoldSubElements.setText("Initially fold &productions");
	}

	private TableViewer createSectionsViewer(Group group) {
		TableViewer viewer = new TableViewer(group, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		GridData data = new GridData(SWT.BEGINNING, SWT.FILL, false, true);
		data.heightHint = convertHeightInCharsToPixels(6);
		viewer.getControl().setLayoutData(data);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSectionsViewerSelectionChanged();
			}
		});
		return viewer;
	}

	public void init(IWorkbench workbench) {
	}
	
	private void initialize() {
		IPreferenceStore store = getPreferenceStore();
		
		fEnable.setSelection(store.getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED));
		fFoldSubElements.setSelection(store.getBoolean(PreferenceConstants.EDITOR_FOLD_RULES));
		initializeSections(store, false);
		updateButtonEnablement();
	}

	private void initializeSections(IPreferenceStore store, boolean defaultValues) {
		String[] sections = Document.getAvailableSectionLabels();
		SectionTableEntry[] entries = new SectionTableEntry[sections.length];
		for (int i = 0; i < entries.length; i++) {
			String section = sections[i];
			boolean value = defaultValues ? store.getDefaultBoolean(PreferenceConstants.EDITOR_FOLD_SECTIONS + section)
					: store.getBoolean(PreferenceConstants.EDITOR_FOLD_SECTIONS + section);
			entries[i] = new SectionTableEntry(section, value);
		}
		fSectionsViewer.setInput(entries);
	}

	private void updateButtonEnablement() {
		boolean enable = fEnable.getSelection();
		fFoldSubElements.setEnabled(enable);
		fFoldSelectedSection.setEnabled(enable);
		fSectionsViewer.getControl().setEnabled(enable);
	}
	
	private void updateSectionButton(SectionTableEntry entry) {
		fFoldSelectedSection.setEnabled(entry != null);
		if (entry != null)
			fFoldSelectedSection.setSelection(entry.fState);
	}

	private void handleSectionsViewerSelectionChanged() {
		IStructuredSelection selection = (IStructuredSelection) fSectionsViewer.getSelection();
		SectionTableEntry entry = (SectionTableEntry) selection.getFirstElement();
		updateSectionButton(entry);
	}

	private void handleFoldSectionSelected() {
		IStructuredSelection selection = (IStructuredSelection) fSectionsViewer.getSelection();
		SectionTableEntry entry = (SectionTableEntry) selection.getFirstElement();
		if (entry != null)
			entry.fState = fFoldSelectedSection.getSelection();
	}

	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		
		store.setValue(PreferenceConstants.EDITOR_FOLDING_ENABLED, fEnable.getSelection());
		store.setValue(PreferenceConstants.EDITOR_FOLD_RULES, fFoldSubElements.getSelection());
		
		SectionTableEntry[] entries = (SectionTableEntry[]) fSectionsViewer.getInput();
		for (int i = 0; i < entries.length; i++) {
			SectionTableEntry entry = entries[i];
			store.setValue(PreferenceConstants.EDITOR_FOLD_SECTIONS + entry.fSection, entry.fState);
		}

		return super.performOk();
	}
	
	protected void performDefaults() {
		IPreferenceStore store = getPreferenceStore();
		
		fEnable.setSelection(store.getDefaultBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED));
		fFoldSubElements.setSelection(store.getDefaultBoolean(PreferenceConstants.EDITOR_FOLD_RULES));

		initializeSections(store, true);

		super.performDefaults();
	}

}
