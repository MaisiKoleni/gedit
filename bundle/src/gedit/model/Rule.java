/*
 * (c) Copyright 2002, 2005 Uwe Voigt All Rights Reserved.
 */
package gedit.model;

import java.util.ArrayList;
import java.util.List;

public class Rule extends ModelBase {
	protected List<Rhs> rhs = new ArrayList<>(1);

	public Rule(ModelBase parent, String lhs) {
		super(parent, lhs);
	}

	protected void addRhs(Rhs rhs) {
		this.rhs.add(rhs);
		rhs.parent = this;
	}

	public Rhs[] getRhs() {
		return rhs.toArray(Rhs[]::new);
	}

	@Override
	public Rhs[] getChildren() {
		return getRhs();
	}

	public String getLhs() {
		return label;
	}

	@Override
	public ModelType getType() {
		return ModelType.RULE;
	}

	@Override
	public String toString() {
		return super.toString() + "->" + rhs;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Rule))
			return false;
		return super.equals(o) && rhs.equals(((Rule) o).rhs);
	}
}