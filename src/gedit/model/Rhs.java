/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.util.ArrayList;
import java.util.List;

public class Rhs extends ModelBase {
	protected List<Reference> parts = new ArrayList<>(1);
	private String string;

	public Rhs(ModelBase parent, String label) {
		super(parent, label);
	}

	protected void addPart(Reference reference) {
		parts.add(reference);
		reference.parent = this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (Rule.class.equals(adapter))
			if (parent instanceof Rule)
				return (T) parent;
		return super.getAdapter(adapter);
	}

	public Reference[] getParts() {
		return parts.toArray(Reference[]::new);
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
			sb.append(withAnnotation ? (Object) parts.get(i) : parts.get(i).label);
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
		Rule thisParent = getAdapter(Rule.class);
		Rule otherParent = rhs.getAdapter(Rule.class);
		return super.equals(rhs) && thisParent.getLhs().equals(otherParent.getLhs())
				&& parts.equals(rhs.parts);
	}

}
