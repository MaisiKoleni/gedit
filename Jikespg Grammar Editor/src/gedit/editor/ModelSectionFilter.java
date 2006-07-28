/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.model.Section;

import java.util.BitSet;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class ModelSectionFilter extends ViewerFilter {
	private BitSet fTypedFilter;
	public ModelSectionFilter(BitSet set) {
		setFilter(set);
	}
	
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (!(element instanceof Section))
			return true;
		return !((Section) element).getChildType().matches(fTypedFilter);
	}
	
	public void setFilter(BitSet set) {
		fTypedFilter = set;
	}
}
