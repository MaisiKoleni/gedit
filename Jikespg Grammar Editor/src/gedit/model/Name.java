/*
 * (c) Copyright 2002, 2005 Uwe Voigt All Rights Reserved.
 */
package gedit.model;

import gedit.GrammarEditorPlugin;

import org.eclipse.jface.resource.ImageDescriptor;

public class Name extends ModelBase {
	private Reference rhs;

	public Name(Object parent, String lhs, Reference rhs) {
		super(parent, lhs);
		this.rhs = rhs;
		rhs.parent = this;
	}
	
	public String getLhs() {
		return label;
	}

	public Reference getARhs() {
		return rhs;
	}
	
	public ModelType getType() {
		return ModelType.NAME;
	}

	public ImageDescriptor getImageDescriptor(Object o) {
		return GrammarEditorPlugin.getImageDescriptor("icons/name.gif"); //$NON-NLS-1$
	}

	public String toString() {
		return super.toString() + "->" + rhs;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Name))
			return false;
		return super.equals(o) && rhs.equals(((Name) o).rhs);
	}
}
