/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;
import org.eclipse.ui.model.WorkbenchAdapter;

public abstract class ModelBase extends WorkbenchAdapter implements Comparable, IAdaptable {
	protected Object parent;
	protected String label;
	protected Node node;
	private Map userData;
	
	public ModelBase(Object parent, String label) {
		Assert.isNotNull(label);
		this.parent = parent;
		this.label = label;
	}

	public Object[] getChildren(Object o) {
		return new ModelBase[0];
	}
	
	public abstract ModelType getType();
	
	public int getOffset() {
		return node.offset;
	}

	public int getLength() {
		return node.length;
	}
	
	public int getRangeOffset() {
		return node.parent != null ? node.parent.offset : node.offset;
	}

	public int getRangeLength() {
		return node.parent != null ? node.parent.length : node.length;
	}
	
	public Problem[] getProblems() {
		Document document = (Document) getAdapter(Document.class);
		if (document == null)
			return null;
		return document.getProblems(this);
	}
	
	public void setUserData(Object key, Object value) {
		if (userData == null)
			userData = new HashMap();
		userData.put(key, value);
	}
	
	public Object getUserData(Object key) {
		return userData != null ? userData.get(key) : null;
	}

	public Document getDocument() {
		return (Document) getAdapter(Document.class);
	}
	
	public Object getAdapter(Class adapter) {
		if (Document.class.equals(adapter)) {
			if (parent instanceof Document)
				return parent;
			if (parent instanceof IAdaptable)
				return ((IAdaptable) parent).getAdapter(adapter);
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
	
	public String getLabel(Object o) {
		return label;
	}
	
	public ImageDescriptor getImageDescriptor(Object o) {
		return ImageDescriptor.getMissingImageDescriptor();
	}

	public Object getParent(Object o) {
		return parent;
	}

	public int compareTo(Object o) {
		return o instanceof ModelBase ? label.compareToIgnoreCase(((ModelBase) o).label) : 0;
	}

	public String toString() {
		return getType()  + ": " + label;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ModelBase))
			return false;
		ModelBase base = (ModelBase) o;
		return label.equals(base.label) && node.offset == base.node.offset;
	}

}
