/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

public class GenericModel extends ModelBase {
	private ModelType modelType;

	public GenericModel(ModelBase parent, String label, ModelType type) {
		super(parent, label);
		this.modelType = type;
	}

	@Override
	public ModelType getType() {
		return modelType;
	}

}
