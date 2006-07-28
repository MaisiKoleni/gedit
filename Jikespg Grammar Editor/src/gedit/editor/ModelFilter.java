/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.model.ModelBase;
import gedit.model.ModelType;
import gedit.model.Section;

import java.util.BitSet;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class ModelFilter extends ViewerFilter {
	private BitSet fTypedFilter;
	private boolean fFilterMacros;

	public ModelFilter(BitSet set, boolean filterMacros) {
		fTypedFilter = set;
		fFilterMacros = filterMacros;
	}
	
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof Section)
			return !((Section) element).getChildType().matches(fTypedFilter);
		if (element instanceof ModelBase)
			return !(fFilterMacros && ((ModelBase) element).getType() == ModelType.MACRO_BLOCK);
		return true;
	}
	
	public void setFilter(BitSet set) {
		fTypedFilter = set;
	}
	
	public void setFilterMacros(boolean filterMacros) {
		fFilterMacros = filterMacros;
	}
}
