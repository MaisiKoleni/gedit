/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

public class Alias extends ModelBase {
	private Reference rhs;

	public Alias(ModelBase parent, String lhs, Reference rhs) {
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

	@Override
	public ModelType getType() {
		return ModelType.ALIAS;
	}

	@Override
	public String toString() {
		return super.toString() + "->" + rhs;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Alias))
			return false;
		return super.equals(o) && rhs.equals(((Alias) o).rhs);
	}
}
