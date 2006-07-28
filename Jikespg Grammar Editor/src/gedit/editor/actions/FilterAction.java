/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor.actions;

import gedit.GrammarEditorPlugin;
import gedit.StringUtils;
import gedit.editor.ModelFilter;
import gedit.editor.ModelLabelProvider;
import gedit.editor.ModelSorter;
import gedit.editor.PreferenceConstants;
import gedit.model.ModelType;
import gedit.model.ModelUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
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

public class FilterAction extends Action {
	private class FilterDialog extends Dialog {
		private BitSet fSelectedFilterTypes;
		private boolean fFilterMacros;
		private TableViewer fTypeTable;
		private Button fFilterMacrosButton;

		protected FilterDialog(Shell parentShell, BitSet selectedFilterTypes, boolean filterMacros) {
			super(parentShell);
			fSelectedFilterTypes = selectedFilterTypes;
			fFilterMacros = filterMacros;
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}
		
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Section Type Filters");
		}
		
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
			ModelType[] allTypes = ModelType.getAllTypes();
			List input = new ArrayList();
			for (int i = 0; i < allTypes.length; i++) {
				ModelType type = allTypes[i];
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
			for (int i = 0; i < items.length; i++) {
				TableItem item = items[i];
				ModelType type = (ModelType) item.getData();
				if (fSelectedFilterTypes.get(type.getBitPosition()))
					item.setChecked(true);
			}
		}

		protected void okPressed() {
			TableItem[] items = fTypeTable.getTable().getItems();
			BitSet selectedTypes = new BitSet();
			for (int i = 0; i < items.length; i++) {
				TableItem item = items[i];
				ModelType type = (ModelType) item.getData();
				if (item.getChecked())
					selectedTypes.set(type.getBitPosition());
			}
			fSelectedFilterTypes = selectedTypes;
			fFilterMacros = fFilterMacrosButton.getSelection();
			super.okPressed();
		}

		public BitSet getSelectedFilterTypes() {
			return fSelectedFilterTypes;
		}
		
		public boolean isFilterMacros() {
			return fFilterMacros;
		}
		
	};
	
	private static int MAX_RECENTLY_USED_FILTERS = 5;
	
	private BitSet fPresetFilter;
	private BitSet fFilter;
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

	private FilterAction(ContentViewer viewer, ModelFilter sectionFilter, String text, BitSet filter, String preferenceKey, String preferenceKeyRecentlyUsed, String preferenceKeyMacros) {
		this(text, viewer, sectionFilter, preferenceKey, preferenceKeyRecentlyUsed, preferenceKeyMacros);
		setChecked(ModelUtils.createBitSetFromString(fStore.getString(preferenceKey), PreferenceConstants.SECTION_FILTERS_SEPARATOR).get(filter.nextSetBit(0)));
		fPresetFilter = filter;
	}

	public void run() {
		BitSet previousFilter = (BitSet) (fFilter = ModelUtils.createBitSetFromString(fStore
				.getString(fPreferenceKey), PreferenceConstants.SECTION_FILTERS_SEPARATOR)).clone();
		boolean previousFilterMacros = fStore.getBoolean(fPreferenceKeyMacros);
		boolean newFilterMacros = previousFilterMacros;
		if (fPresetFilter != null) {
			fFilter.xor(fPresetFilter);
			setChecked(fFilter.get(fPresetFilter.nextSetBit(0)));
		} else {
			fDialog = new FilterDialog(fViewer.getControl().getShell(), previousFilter, previousFilterMacros);
			fDialog.open();
			fFilter = fDialog.getSelectedFilterTypes();
			newFilterMacros = fDialog.isFilterMacros();
			fDialog = null;
		}
		if (!previousFilter.equals(fFilter)) {
			String filterString = ModelUtils.createStringFromBitSet(fFilter, PreferenceConstants.SECTION_FILTERS_SEPARATOR);
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
		List recentlyUsedFilters = new ArrayList(Arrays.asList(StringUtils.split(fStore.getString(fPreferenceKeyRecentlyUsed), PreferenceConstants.SECTION_FILTERS_SEPARATOR)));
		String[] newFilters = StringUtils.split(newFiltersString, PreferenceConstants.SECTION_FILTERS_SEPARATOR);
		for (int i = 0; i < newFilters.length; i++) {
			String newFilter = newFilters[i];
			if (!recentlyUsedFilters.contains(newFilter))
				recentlyUsedFilters.add(0, newFilter);
		}
		if (recentlyUsedFilters.size() > MAX_RECENTLY_USED_FILTERS)
			recentlyUsedFilters = recentlyUsedFilters.subList(0, MAX_RECENTLY_USED_FILTERS);
		fStore.setValue(fPreferenceKeyRecentlyUsed, StringUtils.join((String[]) recentlyUsedFilters.toArray(new String[recentlyUsedFilters.size()]), PreferenceConstants.SECTION_FILTERS_SEPARATOR));
	}

	public IContributionItem getMostRecentlyUsedContributionItem() {
		return new CompoundContributionItem() {
			protected IContributionItem[] getContributionItems() {
				IAction[] actions = getMostRecentlyUsedFilters(fViewer);
				ActionContributionItem[] items = new ActionContributionItem[actions.length];
				for (int i = 0; i < items.length; i++) {
					items[i] = new ActionContributionItem(actions[i]);
				}
				return items;
			}
		};
	}

	private IAction[] getMostRecentlyUsedFilters(ContentViewer viewer) {
		BitSet filters = ModelUtils.createBitSetFromString(fStore.getString(fPreferenceKeyRecentlyUsed), PreferenceConstants.SECTION_FILTERS_SEPARATOR);
		IAction[] actions = new IAction[filters.cardinality()];
		ILabelProvider labelProvider = (ILabelProvider) viewer.getLabelProvider();
		for (int i = 0, lastBitPos = 0; i < actions.length; i++, lastBitPos++) {
			ModelType type = ModelType.getByBitPosition(lastBitPos = filters.nextSetBit(lastBitPos));
			BitSet filter = fPresetFilter != null ? fFilter : type.or((BitSet) null);
			actions[i] = new FilterAction(viewer, fSectionFilter, (i + 1) + " " + labelProvider.getText(type), filter, fPreferenceKey, fPreferenceKeyRecentlyUsed, fPreferenceKeyMacros);
		}
		return actions;
	}
	
	public boolean isDialogActive() {
		return fDialog != null;
	}

}
