/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

public class Reference extends ModelBase {
	private ModelBase referrer;

	public Reference(Object parent, String value) {
		super(parent, value);
	}
	
	public String getValue() {
		return label;
	}
	
	public ModelType getType() {
		return ModelType.REFERENCE;
	}

	public ModelBase getReferer() {
		if (referrer == null)
			referrer = findReferrer();
		return referrer;
	}

	private ModelBase findReferrer() {
		Document document = (Document) getAdapter(Document.class);
		return document.getElementById(label);
	}

}
