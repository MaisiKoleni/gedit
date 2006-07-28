/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor.actions;

import gedit.GrammarEditorPlugin;
import gedit.StringUtils;
import gedit.editor.ModelLabelProvider;
import gedit.editor.ModelSectionFilter;
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
		private TableViewer fTypeTable;

		protected FilterDialog(Shell parentShell, BitSet selectedFilterTypes) {
			super(parentShell);
			fSelectedFilterTypes = selectedFilterTypes;
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}
		
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Section Type Filters");
		}
		
		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);
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
			super.okPressed();
		}

		public BitSet getSelectedFilterTypes() {
			return fSelectedFilterTypes;
		}
		
	};
	
	private static int MAX_RECENTLY_USED_FILTERS = 5;
	
	private BitSet fPresetFilter;
	private BitSet fFilter;
	private ContentViewer fViewer;
	private ModelSectionFilter fSectionFilter;
	private String fPreferenceKey;
	private String fPreferenceKeyRecentlyUsed;
	private IPreferenceStore fStore;

	
	private FilterAction(String text, ContentViewer viewer, ModelSectionFilter filter, String preferenceKey, String preferenceKeyRecentlyUsed) {
		super(text);
		fViewer = viewer;
		fSectionFilter = filter;
		fPreferenceKey = preferenceKey;
		fPreferenceKeyRecentlyUsed = preferenceKeyRecentlyUsed;
		fStore = GrammarEditorPlugin.getDefault().getPreferenceStore();
	}
	
	public FilterAction(ContentViewer viewer, ModelSectionFilter sectionFilter, String preferenceKey, String preferenceKeyRecentlyUsed) {
		this("&Filters...", viewer, sectionFilter, preferenceKey, preferenceKeyRecentlyUsed);
		setToolTipText("Choose the filter to apply");
		setImageDescriptor(GrammarEditorPlugin.getImageDescriptor("icons/filter.gif"));
	}

	private FilterAction(ContentViewer viewer, ModelSectionFilter sectionFilter, String text, BitSet filter, String preferenceKey, String preferenceKeyRecentlyUsed) {
		this(text, viewer, sectionFilter, preferenceKey, preferenceKeyRecentlyUsed);
		setChecked(ModelUtils.createBitSetFromString(fStore.getString(preferenceKey), PreferenceConstants.SECTION_FILTERS_SEPARATOR).get(filter.nextSetBit(0)));
		fPresetFilter = filter;
	}

	public void run() {
		BitSet previousFilter = (BitSet) (fFilter = ModelUtils.createBitSetFromString(fStore
				.getString(fPreferenceKey), PreferenceConstants.SECTION_FILTERS_SEPARATOR)).clone();
		if (fPresetFilter != null) {
			fFilter.xor(fPresetFilter);
			setChecked(fFilter.get(fPresetFilter.nextSetBit(0)));
		} else
			fFilter = openFilterDialog(previousFilter);
		if (previousFilter.equals(fFilter))
			return;
		String filterString = ModelUtils.createStringFromBitSet(fFilter, PreferenceConstants.SECTION_FILTERS_SEPARATOR);
		fStore.setValue(fPreferenceKey, filterString);
		addToRecentlyUsedFilters(filterString);
		
		fSectionFilter.setFilter(fFilter);
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

	private BitSet openFilterDialog(BitSet filter) {
		FilterDialog dialog = new FilterDialog(fViewer.getControl().getShell(), filter);
		dialog.open();
		return dialog.getSelectedFilterTypes();
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
			actions[i] = new FilterAction(viewer, fSectionFilter, (i + 1) + " " + labelProvider.getText(type), filter, fPreferenceKey, fPreferenceKeyRecentlyUsed);
		}
		return actions;
	}

}
