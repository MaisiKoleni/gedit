/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import gedit.GrammarEditorPlugin;

import org.eclipse.jface.resource.ImageDescriptor;

public class Definition extends ModelBase {
	private String value;

	public Definition(Object parent, String name, String value) {
		super(parent, name);
		this.value = value;
	}
	
	public String getName() {
		return label;
	}
	
	public String getValue() {
		return label;
	}

	public ModelType getType() {
		return ModelType.DEFINITION;
	}

	public ImageDescriptor getImageDescriptor(Object o) {
		return GrammarEditorPlugin.getImageDescriptor("icons/definition.gif"); //$NON-NLS-1$
	}
	
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Definition))
			return false;
		return super.equals(o) && value.equals(((Definition) o).value);
	}
}
