/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import gedit.GrammarEditorPlugin;

import org.eclipse.jface.resource.ImageDescriptor;

public class Alias extends ModelBase {
	private Reference rhs;

	public Alias(Object parent, String lhs, Reference rhs) {
		super(parent, lhs);
		this.rhs = rhs;
		rhs.parent = this;
	}
	
	public String getLhs() {
		return label;
	}

	public Reference getRhs() {
		return rhs;
	}
	
	public ModelType getType() {
		return ModelType.ALIAS;
	}
	
	public ImageDescriptor getImageDescriptor(Object o) {
		return GrammarEditorPlugin.getImageDescriptor("icons/alias.gif"); //$NON-NLS-1$
	}
	
	public String toString() {
		return super.toString() + "->" + rhs;
	}
	
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Alias))
			return false;
		return super.equals(o) && rhs.equals(((Alias) o).rhs);
	}
}
