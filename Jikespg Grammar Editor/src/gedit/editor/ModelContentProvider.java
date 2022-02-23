/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.model.BaseWorkbenchContentProvider;

import gedit.model.ModelBase;

public class ModelContentProvider extends BaseWorkbenchContentProvider {

	@Override
	public ModelBase[] getChildren(Object element) {
		if (!(element instanceof ModelBase))
			return new ModelBase[0];
		ModelBase modelBase = (ModelBase) element;

		ModelBase[] children = modelBase.getChildren();
		List<ModelBase> filtered = null;

		for (int i = 0; i < children.length; i++) {
			ModelBase child = children[i];
			if (child != null && child.isVisible()) {
				if (filtered != null)
					filtered.add(child);
			} else if (filtered == null) {
				filtered = new ArrayList<>();
				for (int j = 0; j < i; j++) {
					filtered.add(children[j]);
				}
			}
		}
		if (filtered == null)
			return children;
		return filtered.toArray(new ModelBase[filtered.size()]);
	}

	@Override
	public ModelBase getParent(Object element) {
		if (!(element instanceof ModelBase))
			return null;
		ModelBase modelBase = (ModelBase) element;

		return modelBase.getParent();
	}
}
