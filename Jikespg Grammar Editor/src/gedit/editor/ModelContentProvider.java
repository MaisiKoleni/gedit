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
	public Object[] getChildren(Object element) {
		if (!(element instanceof ModelBase))
			return new Object[0];
		ModelBase modelBase = (ModelBase) element;

		Object[] children = modelBase.getChildren();
		List filtered = null;

		for (int i = 0; i < children.length; i++) {
			Object child = children[i];
			if (child instanceof ModelBase && ((ModelBase) child).isVisible()) {
				if (filtered != null)
					filtered.add(child);
			} else if (filtered == null) {
				filtered = new ArrayList();
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
	public Object getParent(Object element) {
		if (!(element instanceof ModelBase))
			return null;
		ModelBase modelBase = (ModelBase) element;

		return modelBase.getParent();
	}
}
