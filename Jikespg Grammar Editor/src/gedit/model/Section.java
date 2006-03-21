/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import gedit.GrammarEditorPlugin;

import org.eclipse.jface.resource.ImageDescriptor;

public class Section extends ModelBase {
	private ModelType childType;
	private ImageDescriptor imageDescriptor;
	protected ModelBase[] children;

	public Section(ModelType childType, Object parent, String label, String imagePath) {
		super(parent, label);
		this.childType = childType;
		if (imagePath != null)
			this.imageDescriptor = GrammarEditorPlugin.getImageDescriptor(imagePath);
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

	public ImageDescriptor getImageDescriptor(Object o) {
		return imageDescriptor;
	}
	
	public Object[] getChildren(Object o) {
		return children != null ? children : new ModelBase[0];
	}
	
	public void setChildren(ModelBase[] children) {
		this.children = children;
	}

	protected ModelBase getElementById(String id, int filter) {
		return ElementFinder.findElement(this, id, filter);
	}
	
}