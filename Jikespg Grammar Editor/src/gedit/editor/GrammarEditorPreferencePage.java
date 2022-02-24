/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import gedit.GrammarEditorPlugin;
import gedit.StringUtils;
import gedit.model.ModelType;

public class GrammarEditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, Listener {
	private Text fDirectoryText;
	private List fDirectoryList;
	private Table fSectionTable;
	private Button fBrowseDirButton, fAddDirButton, fRemoveDirButton, fEditDirButton, fDirUpButton, fDirDownButton;
	private Button fSectionUpButton, fSectionDownButton, fSectionAlphabeticalButton;

	public GrammarEditorPreferencePage() {
		setPreferenceStore(GrammarEditorPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	private Control createPage(Composite parent) {

		createIncludeDirectoryGroup(parent);
		createSectionOrderingGroup(parent);

		return parent;
	}

	private Control createIncludeDirectoryGroup(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(group, SWT.LEFT);
		label.setText("&Choose directory:");
		GridData data = new GridData();
		data.horizontalSpan = 2;
		label.setLayoutData(data);

		fDirectoryText = new Text(group, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertWidthInCharsToPixels(30);
		fDirectoryText.setLayoutData(data);
		fDirectoryText.addListener(SWT.Modify, this);

		fBrowseDirButton = new Button(group, SWT.PUSH);
		fBrowseDirButton.setText("&Browse");
		setButtonLayoutData(fBrowseDirButton);
		fBrowseDirButton.addListener(SWT.Selection, this);

		label = new Label(group, SWT.LEFT);
		label.setText("&Include directories:");
		data = new GridData();
		data.horizontalSpan = 2;
		label.setLayoutData(data);

		fDirectoryList = new List(group, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_BOTH);
		data.widthHint = convertWidthInCharsToPixels(35);
		data.heightHint = convertHeightInCharsToPixels(7);
		fDirectoryList.setLayoutData(data);
		fDirectoryList.addListener(SWT.Selection, this);

		Composite buttons = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = layout.marginWidth = 0;
		buttons.setLayout(layout);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

		fAddDirButton = new Button(buttons, SWT.PUSH);
		fAddDirButton.setText("Add");
		setButtonLayoutData(fAddDirButton);
		fAddDirButton.addListener(SWT.Selection, this);

		fRemoveDirButton = new Button(buttons, SWT.PUSH);
		fRemoveDirButton.setText("&Remove");
		setButtonLayoutData(fRemoveDirButton);
		fRemoveDirButton.addListener(SWT.Selection, this);

		fEditDirButton = new Button(buttons, SWT.PUSH);
		fEditDirButton.setText("&Edit");
		setButtonLayoutData(fEditDirButton);
		fEditDirButton.addListener(SWT.Selection, this);

		fDirUpButton = new Button(buttons, SWT.PUSH);
		fDirUpButton.setText("&Up");
		setButtonLayoutData(fDirUpButton);
		fDirUpButton.addListener(SWT.Selection, this);

		fDirDownButton = new Button(buttons, SWT.PUSH);
		fDirDownButton.setText("D&own");
		setButtonLayoutData(fDirDownButton);
		fDirDownButton.addListener(SWT.Selection, this);

		updateDirectoryButtons();
		initializeDirectoryGroup(false);
		return group;
	}

	private Control createSectionOrderingGroup(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		group.setText("&Section ordering:");
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_BOTH));

		fSectionTable = new Table(group, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = convertWidthInCharsToPixels(35);
		data.heightHint = convertHeightInCharsToPixels(7);
		fSectionTable.setLayoutData(data);
		fSectionTable.addListener(SWT.Selection, this);
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100));
		fSectionTable.setLayout(layout);
		new TableColumn(fSectionTable, SWT.LEFT);

		Composite buttons = new Composite(group, SWT.NONE);
		GridLayout gridLayout = new GridLayout(1, true);
		gridLayout.marginHeight = gridLayout.marginWidth = 0;
		buttons.setLayout(gridLayout);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

		fSectionUpButton = new Button(buttons, SWT.PUSH);
		fSectionUpButton.setText("U&p");
		setButtonLayoutData(fSectionUpButton);
		fSectionUpButton.addListener(SWT.Selection, this);

		fSectionDownButton = new Button(buttons, SWT.PUSH);
		fSectionDownButton.setText("Do&wn");
		setButtonLayoutData(fSectionDownButton);
		fSectionDownButton.addListener(SWT.Selection, this);

		Label separator = new Label(buttons, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fSectionAlphabeticalButton = new Button(buttons, SWT.PUSH);
		fSectionAlphabeticalButton.setText("Alp&habetical");
		setButtonLayoutData(fSectionAlphabeticalButton);
		fSectionAlphabeticalButton.addListener(SWT.Selection, this);

		updateSectionButtons();
		initializeSectionGroup(false);
		return group;
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.type) {
		case SWT.Selection:
			if (event.widget == fDirectoryList)
				updateDirectoryButtons();
			else if (event.widget == fSectionTable)
				updateSectionButtons();
			else if (event.widget == fBrowseDirButton)
				handleDirectoryBrowse(fDirectoryText);
			else if (event.widget == fAddDirButton)
				handleDirectoryAdd();
			else if (event.widget == fRemoveDirButton)
				handleDirectoryRemove();
			else if (event.widget == fEditDirButton)
				handleDirectoryEdit();
			else if (event.widget == fDirUpButton)
				handleDirectoryUp();
			else if (event.widget == fDirDownButton)
				handleDirectoryDown();
			else if (event.widget == fSectionUpButton)
				handleSectionUp();
			else if (event.widget == fSectionDownButton)
				handleSectionDown();
			else if (event.widget == fSectionAlphabeticalButton)
				handleSectionAlphabetical();
			break;
		case SWT.Modify:
			updateDirectoryButtons();
			break;
		}
	}

	private void handleDirectoryAdd() {
		String text = fDirectoryText.getText();
		if (text.trim().length() == 0)
			return;
		fDirectoryList.add(text);
		fDirectoryText.setText("");
		fDirectoryList.setSelection(fDirectoryList.getItemCount() - 1);
		updateDirectoryButtons();
	}

	private void handleDirectoryRemove() {
		if (fDirectoryList.getSelectionCount() == 1) {
			fDirectoryText.setText(fDirectoryList.getSelection()[0]);
			fDirectoryList.remove(fDirectoryList.getSelectionIndex());
		} else if (fDirectoryList.getSelectionCount() > 1) {
			fDirectoryList.remove(fDirectoryList.getSelectionIndices());
		}
		updateDirectoryButtons();
	}

	private void handleDirectoryEdit() {
		int index = fDirectoryList.getSelectionIndex();
		if (index == -1)
			return;
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(fDirectoryList.getItem(index));
		String result = dialog.open();
		if (result != null)
			fDirectoryList.setItem(index, result);
	}

	private void handleDirectoryUp() {
		int[] selected = fDirectoryList.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			int index = selected[i];
			if (index < 1)
				continue;
			String entry = fDirectoryList.getItem(index);
			fDirectoryList.remove(index);
			fDirectoryList.add(entry, selected[i] = index - 1);
		}
		fDirectoryList.setSelection(selected);
		updateDirectoryButtons();
	}

	private void handleDirectoryDown() {
		int[] selected = fDirectoryList.getSelectionIndices();
		int maxIndex = fDirectoryList.getItemCount() - 1;
		for (int i = selected.length - 1; i >= 0 ; i--) {
			int index = selected[i];
			if (index >= maxIndex)
				continue;
			String entry = fDirectoryList.getItem(index);
			fDirectoryList.remove(index);
			fDirectoryList.add(entry, selected[i] = index + 1);
		}
		fDirectoryList.setSelection(selected);
		updateDirectoryButtons();
	}

	private void handleDirectoryBrowse(Text text) {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(text.getText());
		String result = dialog.open();
		if (result != null)
			text.setText(result);
	}

	private void handleSectionUp() {
		int[] selected = fSectionTable.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			int index = selected[i];
			if (index < 1)
				continue;
			swapItems(fSectionTable.getItem(index), fSectionTable.getItem(selected[i] = index - 1));
		}
		fSectionTable.setSelection(selected);
		updateSectionButtons();
	}

	private void handleSectionDown() {
		int[] selected = fSectionTable.getSelectionIndices();
		int maxIndex = fSectionTable.getItemCount() - 1;
		for (int i = selected.length - 1; i >= 0 ; i--) {
			int index = selected[i];
			if (index >= maxIndex)
				continue;
			swapItems(fSectionTable.getItem(index), fSectionTable.getItem(selected[i] = index + 1));
		}
		fSectionTable.setSelection(selected);
		updateSectionButtons();
	}

	private void swapItems(TableItem item1, TableItem item2) {
		String text = item1.getText();
		Image image = item1.getImage();
		Object data = item1.getData();
		item1.setText(item2.getText());
		item1.setImage(item2.getImage());
		item1.setData(item2.getData());
		item2.setText(text);
		item2.setImage(image);
		item2.setData(data);
	}

	private void handleSectionAlphabetical() {
		int reverse = fSectionAlphabeticalButton.getData() != null ? -1 : 1;
		TableItem[] items = fSectionTable.getItems();
		for (int i = 0; i < items.length; i++) {
			int min = i;
			for (int j = i + 1; j < items.length; j++) {
				if (items[j].getText().compareToIgnoreCase(items[min].getText()) * reverse < 0)
					min = j;
			}
			if (min != i)
				swapItems(items[min], items[i]);
		}
		fSectionAlphabeticalButton.setData(reverse == -1 ? null : new Object());
	}

	private void updateDirectoryButtons() {
		fAddDirButton.setEnabled(fDirectoryText.getText().trim().length() > 0);
		boolean atLeastOneSelected = fDirectoryList.getSelectionCount() > 0;
		fRemoveDirButton.setEnabled(atLeastOneSelected);
		fEditDirButton.setEnabled(fDirectoryList.getSelectionCount() == 1);
		int[] selected = fDirectoryList.getSelectionIndices();
		fDirUpButton.setEnabled(atLeastOneSelected && selected[0] > 0);
		fDirDownButton.setEnabled(atLeastOneSelected && selected[selected.length - 1] < fDirectoryList.getItemCount() - 1);
	}

	private void updateSectionButtons() {
		boolean atLeastOneSelected = fSectionTable.getSelectionCount() > 0;
		int[] selected = fSectionTable.getSelectionIndices();
		fSectionUpButton.setEnabled(atLeastOneSelected && selected[0] > 0);
		fSectionDownButton.setEnabled(atLeastOneSelected && selected[selected.length - 1] < fSectionTable.getItemCount() - 1);
	}

	@Override
	protected Control createContents(Composite parent) {

		Composite contents = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		contents.setLayout(layout);
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));

		createPage(contents);

		Dialog.applyDialogFont(contents);
		return contents;
	}

	private void initializeDirectoryGroup(boolean useDefault) {
		fDirectoryList.removeAll();
		String[] values = StringUtils.split(useDefault ? getPreferenceStore().getDefaultString(PreferenceConstants.GRAMMAR_INCLUDE_DIRECTORIES) : getPreferenceStore().getString(PreferenceConstants.GRAMMAR_INCLUDE_DIRECTORIES),
				PreferenceConstants.INCLUDE_DIRECTORIES_SEPARATOR);
		for (String value : values) {
			fDirectoryList.add(value);
		}
	}

	private void initializeSectionGroup(boolean useDefault) {
		fSectionTable.removeAll();
		ModelType[] allTypes = ModelType.values();
		String[] values = StringUtils.split(useDefault ? getPreferenceStore().getDefaultString(PreferenceConstants.SECTION_ORDERING) : getPreferenceStore().getString(PreferenceConstants.SECTION_ORDERING),
				PreferenceConstants.SECTION_ORDERING_SEPARATOR);
		ModelLabelProvider labelProvider = new ModelLabelProvider();
		for (String value : values) {
			for (ModelType type : allTypes) {
				if (!type.isSectionType() || !value.equals(Integer.toString(type.ordinal())))
					continue;
				TableItem item = new TableItem(fSectionTable, SWT.NONE);
				item.setText(labelProvider.getText(type));
				item.setImage(labelProvider.getImage(type));
				item.setData(Integer.toString(type.ordinal()));
				break;
			}
		}
	}

	@Override
	public boolean performOk() {
		getPreferenceStore().setValue(PreferenceConstants.GRAMMAR_INCLUDE_DIRECTORIES, String.join(
				PreferenceConstants.INCLUDE_DIRECTORIES_SEPARATOR, fDirectoryList.getItems()));
		StringBuilder sb = new StringBuilder();
		TableItem[] items = fSectionTable.getItems();
		for (int i = 0; i < items.length; i++) {
			if (i > 0)
				sb.append(PreferenceConstants.SECTION_ORDERING_SEPARATOR);
			sb.append(items[i].getData());
		}
		getPreferenceStore().setValue(PreferenceConstants.SECTION_ORDERING, sb.toString());
		GrammarEditorPlugin.getDefault().savePluginPreferences();
		return true;
	}

	@Override
	protected void performDefaults() {

		initializeDirectoryGroup(true);
		initializeSectionGroup(true);

		super.performDefaults();

	}

}
