/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.actions.CompoundContributionItem;

import gedit.GrammarEditorPlugin;
import gedit.StringUtils;
import gedit.editor.ModelFilter;
import gedit.editor.ModelLabelProvider;
import gedit.editor.ModelSorter;
import gedit.editor.PreferenceConstants;
import gedit.model.ModelType;
import gedit.model.ModelUtils;

public class FilterAction extends Action {
	private class FilterDialog extends Dialog {
		private EnumSet<ModelType> fSelectedFilterTypes;
		private boolean fFilterMacros;
		private TableViewer fTypeTable;
		private Button fFilterMacrosButton;

		protected FilterDialog(Shell parentShell, EnumSet<ModelType> selectedFilterTypes, boolean filterMacros) {
			super(parentShell);
			fSelectedFilterTypes = selectedFilterTypes;
			fFilterMacros = filterMacros;
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Section Type Filters");
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);

			fFilterMacrosButton = new Button(composite, SWT.CHECK);
			fFilterMacrosButton.setText("&Filter macro blocks");
			fFilterMacrosButton.setData(new GridData(GridData.FILL_HORIZONTAL));
			fFilterMacrosButton.setSelection(fFilterMacros);

			Label label = new Label(composite, SWT.NONE);
			label.setText("&Select the sections to be excluded from the view");
			label.setData(new GridData(GridData.FILL_HORIZONTAL));

			fTypeTable = new TableViewer(composite, SWT.BORDER | SWT.CHECK);
			GridData data = new GridData(GridData.FILL_BOTH);
			data.heightHint = convertHeightInCharsToPixels(15);
			data.widthHint = convertWidthInCharsToPixels(10);
			fTypeTable.getTable().setLayoutData(data);
			new TableColumn(fTypeTable.getTable(), SWT.LEFT);
			TableLayout layout = new TableLayout();
			layout.addColumnData(new ColumnWeightData(100));
			fTypeTable.getTable().setLayout(layout);

			fTypeTable.setContentProvider(new ArrayContentProvider());
			fTypeTable.setLabelProvider(new ModelLabelProvider());
			fTypeTable.setSorter(new ModelSorter());
			ModelType[] allTypes = ModelType.values();
			List<ModelType> input = new ArrayList<>();
			for (ModelType type : allTypes) {
				if (!type.isSectionType())
					continue;
				input.add(type);
			}
			fTypeTable.setInput(input);
			initTypeTable();

			applyDialogFont(composite);
			return composite;
		}

		private void initTypeTable() {
			TableItem[] items = fTypeTable.getTable().getItems();
			for (TableItem item : items) {
				ModelType type = (ModelType) item.getData();
				if (fSelectedFilterTypes.contains(type))
					item.setChecked(true);
			}
		}

		@Override
		protected void okPressed() {
			TableItem[] items = fTypeTable.getTable().getItems();
			EnumSet<ModelType> selectedTypes = EnumSet.noneOf(ModelType.class);
			for (TableItem item : items) {
				ModelType type = (ModelType) item.getData();
				if (item.getChecked())
					selectedTypes.add(type);
			}
			fSelectedFilterTypes = selectedTypes;
			fFilterMacros = fFilterMacrosButton.getSelection();
			super.okPressed();
		}

		public EnumSet<ModelType> getSelectedFilterTypes() {
			return fSelectedFilterTypes;
		}

		public boolean isFilterMacros() {
			return fFilterMacros;
		}

	}

	private static int MAX_RECENTLY_USED_FILTERS = 5;

	private EnumSet<ModelType> fPresetFilter;
	private EnumSet<ModelType> fFilter;
	private ContentViewer fViewer;
	private ModelFilter fSectionFilter;
	private String fPreferenceKey;
	private String fPreferenceKeyRecentlyUsed;
	private String fPreferenceKeyMacros;
	private IPreferenceStore fStore;

	private FilterDialog fDialog;

	private FilterAction(String text, ContentViewer viewer, ModelFilter filter, String preferenceKey, String preferenceKeyRecentlyUsed, String preferenceKeyMacros) {
		super(text);
		fViewer = viewer;
		fSectionFilter = filter;
		fPreferenceKey = preferenceKey;
		fPreferenceKeyRecentlyUsed = preferenceKeyRecentlyUsed;
		fPreferenceKeyMacros = preferenceKeyMacros;
		fStore = GrammarEditorPlugin.getDefault().getPreferenceStore();
	}

	public FilterAction(ContentViewer viewer, ModelFilter sectionFilter, String preferenceKey, String preferenceKeyRecentlyUsed, String preferenceKeyMacros) {
		this("&Filters...", viewer, sectionFilter, preferenceKey, preferenceKeyRecentlyUsed, preferenceKeyMacros);
		setToolTipText("Choose the filter to apply");
		setImageDescriptor(GrammarEditorPlugin.getImageDescriptor("icons/filter.gif"));
	}

	private FilterAction(ContentViewer viewer, ModelFilter sectionFilter, String text, EnumSet<ModelType> filter, String preferenceKey, String preferenceKeyRecentlyUsed, String preferenceKeyMacros) {
		this(text, viewer, sectionFilter, preferenceKey, preferenceKeyRecentlyUsed, preferenceKeyMacros);
		setChecked(ModelUtils
				.createModelTypeSetFromString(fStore.getString(preferenceKey),
						PreferenceConstants.SECTION_FILTERS_SEPARATOR)
				.contains(ModelUtils.getFirstOrElse(fFilter, null)));
		fPresetFilter = filter;
	}

	@Override
	public void run() {
		EnumSet<ModelType> previousFilter = (fFilter = ModelUtils.createModelTypeSetFromString(fStore
				.getString(fPreferenceKey), PreferenceConstants.SECTION_FILTERS_SEPARATOR)).clone();
		boolean previousFilterMacros = fStore.getBoolean(fPreferenceKeyMacros);
		boolean newFilterMacros = previousFilterMacros;
		if (fPresetFilter != null) {
			fFilter = ModelUtils.disjoint(fFilter, fPresetFilter);
			setChecked(fFilter.contains(ModelUtils.getFirstOrElse(fPresetFilter, null)));
		} else {
			fDialog = new FilterDialog(fViewer.getControl().getShell(), previousFilter, previousFilterMacros);
			fDialog.open();
			fFilter = fDialog.getSelectedFilterTypes();
			newFilterMacros = fDialog.isFilterMacros();
			fDialog = null;
		}
		if (!previousFilter.equals(fFilter)) {
			String filterString = ModelUtils.createStringFromModelTypeSet(fFilter, PreferenceConstants.SECTION_FILTERS_SEPARATOR);
			fStore.setValue(fPreferenceKey, filterString);
			addToRecentlyUsedFilters(filterString);
			fSectionFilter.setFilter(fFilter);
		}
		if (newFilterMacros != previousFilterMacros) {
			fStore.setValue(fPreferenceKeyMacros, newFilterMacros);
			fSectionFilter.setFilterMacros(newFilterMacros);
		}

		if (previousFilter.equals(fFilter) && newFilterMacros == previousFilterMacros)
			return;
		fViewer.getControl().setRedraw(false);
		fViewer.refresh();
		fViewer.getControl().setRedraw(true);
	}

	private void addToRecentlyUsedFilters(String newFiltersString) {
		List<String> recentlyUsedFilters = new ArrayList<>(Arrays.asList(StringUtils.split(fStore.getString(fPreferenceKeyRecentlyUsed), PreferenceConstants.SECTION_FILTERS_SEPARATOR)));
		String[] newFilters = StringUtils.split(newFiltersString, PreferenceConstants.SECTION_FILTERS_SEPARATOR);
		for (String newFilter : newFilters) {
			if (!recentlyUsedFilters.contains(newFilter))
				recentlyUsedFilters.add(0, newFilter);
		}
		if (recentlyUsedFilters.size() > MAX_RECENTLY_USED_FILTERS)
			recentlyUsedFilters = recentlyUsedFilters.subList(0, MAX_RECENTLY_USED_FILTERS);
		fStore.setValue(fPreferenceKeyRecentlyUsed, String.join(PreferenceConstants.SECTION_FILTERS_SEPARATOR, recentlyUsedFilters));
	}

	public IContributionItem getMostRecentlyUsedContributionItem() {
		return new CompoundContributionItem() {
			@Override
			protected IContributionItem[] getContributionItems() {
				return getMostRecentlyUsedFilters(fViewer).stream().map(ActionContributionItem::new)
						.toArray(IContributionItem[]::new);
			}
		};
	}

	private List<IAction> getMostRecentlyUsedFilters(ContentViewer viewer) {
		EnumSet<ModelType> filters = ModelUtils.createModelTypeSetFromString(
				fStore.getString(fPreferenceKeyRecentlyUsed), PreferenceConstants.SECTION_FILTERS_SEPARATOR);
		List<IAction> actions = new ArrayList<>();
		ILabelProvider labelProvider = (ILabelProvider) viewer.getLabelProvider();
		int index = 1;
		for (ModelType type : filters) {
			EnumSet<ModelType> filter = fPresetFilter != null ? fFilter : EnumSet.of(type);
			actions.add(new FilterAction(viewer, fSectionFilter, index + " " + labelProvider.getText(type), filter,
					fPreferenceKey, fPreferenceKeyRecentlyUsed, fPreferenceKeyMacros));
			index++;
		}
		return actions;
	}

	public boolean isDialogActive() {
		return fDialog != null;
	}

}
