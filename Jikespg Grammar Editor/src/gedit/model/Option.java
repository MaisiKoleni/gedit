/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

public class Option extends ModelBase {
	protected String value;
	protected Reference[] fileReferences;

	public Option(ModelBase parent, String key, String value) {
		super(parent, key);
		this.value = value;
	}
	
	protected void addFileReference(Reference fileReference, Node node) {
		if (fileReferences == null)
			fileReferences = new Reference[] { fileReference };
		else {
			int length = fileReferences.length;
			System.arraycopy(fileReferences, 0, fileReferences = new Reference[length + 1], 0, length);
			fileReferences[length] = fileReference;
		}
		fileReferences[fileReferences.length - 1].node = node;
	}
	
	public ModelType getType() {
		return ModelType.OPTION;
	}

	public String getValue() {
		return value;
	}
	
	public Reference[] getFileReferences() {
		return fileReferences;
	}
}
