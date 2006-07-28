/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.lang.reflect.Array;
import java.util.BitSet;

public class Section extends ModelBase {
	private ModelType childType;
	protected ModelBase[] children;

	public Section(ModelType childType, ModelBase parent) {
		super(parent, childType.getString());
		this.childType = childType;
	}
	
	public ModelType getChildType() {
		return childType;
	}

	public ModelType getType() {
		return ModelType.SECTION;
	}

	public int compareTo(Object o) {
		return 0;
	}

	public Object[] getChildren() {
		return children != null ? children : createChildrenArray(0);
	}
	
	public void setChildren(ModelBase[] newChildren) {
		int length = newChildren != null ? newChildren.length : 0;
		if (newChildren.getClass().getComponentType() != getChildModelClass()) {
			children = createChildrenArray(length);
			if (length > 0)
				System.arraycopy(newChildren, 0, children, 0, newChildren.length);
		} else
			children = newChildren;
	}

	protected ModelBase getElementById(String id, BitSet filter) {
		return ElementFinder.findElement(this, id, filter);
	}
	
	private Class getChildModelClass() {
		return childType != null && childType.getModelClass() != null ?
				childType.getModelClass() : ModelBase.class;
	}
	
	private ModelBase[] createChildrenArray(int size) {
		return (ModelBase[]) Array.newInstance(getChildModelClass(), size);
	}

	public GenericModel addChild(GenericModel terminal) {
		int length = children != null ? children.length : 0;
		ModelBase[] array = createChildrenArray(length + 1);
		if (length > 0)
			System.arraycopy(children, 0, array, 0, length);
		array[length] = terminal;
		children = array;
		return terminal;
	}
	
}