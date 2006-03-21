/*
 * (c) Copyright 2002, 2005 Uwe Voigt All Rights Reserved.
 */
package gedit.model;

import gedit.GrammarEditorPlugin;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;

public class Rule extends ModelBase {
	protected List rhs = new ArrayList(1);
	
	public Rule(Object parent, String lhs) {
		super(parent, lhs);
	}
	
	protected void addRhs(Rhs rhs) {
		this.rhs.add(rhs);
		rhs.parent = this;
	}
	
	public Rhs[] getRhs() {
		return (Rhs[]) rhs.toArray(new Rhs[rhs.size()]);
	}
	
	public Object[] getChildren(Object o) {
		return getRhs();
	}

	public String getLhs() {
		return label;
	}
	
	public ModelType getType() {
		return ModelType.RULE;
	}

	public ImageDescriptor getImageDescriptor(Object o) {
		return GrammarEditorPlugin.getImageDescriptor("icons/production.gif"); //$NON-NLS-1$
	}

	public String toString() {
		return super.toString() + "->" + rhs;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Rule))
			return false;
		return super.equals(o) && rhs.equals(((Rule) o).rhs);
	}
}