/*
 * (c) Copyright 2002, 2005 Uwe Voigt All Rights Reserved.
 */
package gedit.model;

public class ModelType {

	public final static ModelType DOCUMENT = new ModelType(0x0001, "Document");
	public final static ModelType SECTION = new ModelType(0x0002, "Section");
	public final static ModelType OPTION = new ModelType(0x0004, "Option");
	public final static ModelType DEFINITION = new ModelType(0x0008, "Definition");
	public final static ModelType TERMINAL = new ModelType(0x0010, "Terminal");
	public final static ModelType ALIAS = new ModelType(0x0020, "Alias");
	public final static ModelType START_TOK = new ModelType(0x0040, "StartToken");
	public final static ModelType RULE = new ModelType(0x0080, "Rule");
	public final static ModelType RHS = new ModelType(0x0100, "Rhs");
	public final static ModelType REFERENCE = new ModelType(0x0200, "Reference");
	public final static ModelType NAME = new ModelType(0x0400, "Name");
	public final static ModelType END_TOK = new ModelType(0x0800, "EndToken");
	public final static ModelType EMPTY_TOK = new ModelType(0x1000, "EmptyToken");
	public final static ModelType EOL_TOK = new ModelType(0x2000, "EOLToken");
	public final static ModelType EOF_TOK = new ModelType(0x4000, "EOFToken");
	public final static ModelType COMMENT = new ModelType(0x8000, "Comment");

	private int type;
	private String string;

	private ModelType(int type, String string) {
		this.type = type;
		this.string = string;
	}
	
	public int getType() {
		return type;
	}
	
	public String toString() {
		return string;
	}
}
