/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;

public abstract class ModelBase extends PlatformObject implements Comparable, Cloneable {
	protected ModelBase parent;
	protected String label;
	protected Node node;
	protected boolean visible = true;
	private Map userData;

	public ModelBase(ModelBase parent, String label) {
		Assert.isNotNull(label);
		this.parent = parent;
		this.label = label;
	}

	public Object[] getChildren() {
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

	@Override
	public Object getAdapter(Class adapter) {
		if (Document.class.equals(adapter)) {
			if (parent instanceof Document)
				return parent;
			if (parent != null)
				return parent.getAdapter(adapter);
		}
		return super.getAdapter(adapter);
	}

	public String getLabel() {
		return label;
	}

	public ModelBase getParent() {
		return parent;
	}

	public boolean isVisible() {
		return visible;
	}

	@Override
	protected Object clone() {
		ModelBase clone = null;
		try {
			clone = (ModelBase) super.clone();
		} catch (CloneNotSupportedException ignore) {
		}
		return clone;
	}

	@Override
	public int compareTo(Object o) {
		return o instanceof ModelBase ? label.compareToIgnoreCase(((ModelBase) o).label) : 0;
	}

	@Override
	public String toString() {
		return getType()  + ": " + label;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ModelBase))
			return false;
		ModelBase base = (ModelBase) o;
		return label.equals(base.label) && node.offset == base.node.offset;
	}

}
