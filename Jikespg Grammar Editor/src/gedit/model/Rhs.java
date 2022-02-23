/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.util.ArrayList;
import java.util.List;

public class Rhs extends ModelBase {
	protected List parts = new ArrayList(1);
	private String string;

	public Rhs(ModelBase parent, String label) {
		super(parent, label);
	}

	protected void addPart(Reference reference) {
		parts.add(reference);
		reference.parent = this;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (Rule.class.equals(adapter))
			if (parent instanceof Rule)
				return parent;
		return super.getAdapter(adapter);
	}

	public Reference[] getParts() {
		return (Reference[]) parts.toArray(new Reference[parts.size()]);
	}

	@Override
	public ModelType getType() {
		return ModelType.RHS;
	}

	@Override
	public String getLabel() {
		if (string == null)
			string = computeString(false);
		return string;
	}

	private String computeString(boolean withAnnotation) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0)
				sb.append(' ');
			sb.append(withAnnotation ? (Object) parts.get(i) : ((Reference) parts.get(i)).label);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return computeString(true);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Rhs))
			return false;
		Rhs rhs = (Rhs) o;
		Rule thisParent = (Rule) getAdapter(Rule.class);
		Rule otherParent = (Rule) rhs.getAdapter(Rule.class);
		return super.equals(rhs) && thisParent.getLhs().equals(otherParent.getLhs())
				&& parts.equals(rhs.parts);
	}

}
