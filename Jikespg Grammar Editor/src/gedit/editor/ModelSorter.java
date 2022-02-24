/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import gedit.GrammarEditorPlugin;
import gedit.StringUtils;
import gedit.model.ModelBase;
import gedit.model.ModelType;
import gedit.model.Section;

public class ModelSorter extends ViewerSorter {
	private List<String> fSectionOrder;
	private boolean fEnabled;
	private ILabelProvider fLabelProvider;

	public ModelSorter() {
		fSectionOrder = new ArrayList<>();
		initSectionOrder(GrammarEditorPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.SECTION_ORDERING));
		fEnabled = true;
	}

	public ModelSorter(String sortPreferenceKey) {
		this();
		fEnabled = GrammarEditorPlugin.getDefault().getPreferenceStore().getBoolean(sortPreferenceKey);
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		ModelType type1 = getModelType(e1);
		ModelType type2 = getModelType(e2);
		if (type1 == ModelType.SECTION && type2 == ModelType.SECTION)
			return getSectionOrder((Section) e1, (Section) e2);

		if (!fEnabled)
			return 0;
		if (e1 instanceof ModelType && e2 instanceof ModelType)
			return getLabelProvider().getText(e1).compareToIgnoreCase(getLabelProvider().getText(e2));

		return super.compare(viewer, e1, e2);
	}

	private ILabelProvider getLabelProvider() {
		return fLabelProvider != null ? fLabelProvider : (fLabelProvider = new ModelLabelProvider());
	}

	public void setEnabled(boolean enabled) {
		fEnabled = enabled;
	}

	private void initSectionOrder(String ordering) {
		fSectionOrder.clear();
		fSectionOrder.addAll(Arrays.asList(StringUtils.split(ordering, PreferenceConstants.SECTION_ORDERING_SEPARATOR)));
	}

	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (PreferenceConstants.SECTION_ORDERING.equals(property)) {
			initSectionOrder((String) event.getNewValue());
		}
	}

	protected int getSectionOrder(Section section1, Section section2) {
		if (fEnabled)
			return fSectionOrder.indexOf(Integer.toString(section1.getChildType().getBitPosition()))
				- fSectionOrder.indexOf(Integer.toString(section2.getChildType().getBitPosition()));
		return section1.getOffset() - section2.getOffset();
	}

	protected ModelBase getModel(Object element) {
		return element instanceof ModelBase ? (ModelBase) element : null;
	}

	protected ModelType getModelType(Object element) {
		ModelBase model = getModel(element);
		return model != null ? model.getType() : null;
	}
}
