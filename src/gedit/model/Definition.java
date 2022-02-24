/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

public class Definition extends ModelBase {
	private String value;

	public Definition(ModelBase parent, String name, String value) {
		super(parent, name);
		this.value = value;
	}

	public String getName() {
		return label;
	}

	public String getValue() {
		return value;
	}

	@Override
	public ModelType getType() {
		return ModelType.DEFINITION;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Definition))
			return false;
		return super.equals(o) && value.equals(((Definition) o).value);
	}
}
