/*
 * (c) Copyright 2002, 2005 Uwe Voigt All Rights Reserved.
 */
package gedit.model;

import java.util.Set;

public enum ModelType implements Comparable<ModelType> {

	DOCUMENT("Document", Document.class, false, false),
	SECTION("Section", Section.class, false, false),
	OPTION("Option", Option.class, true, true),
	DEFINITION("Definition", Definition.class, true, true),
	TERMINAL("Terminal", GenericModel.class, true, true),
	ALIAS("Alias", Alias.class, true, true),
	START_TOKEN("StartToken", Reference.class, true, true),
	RULE("Rule", ModelBase.class, true, true),
	RHS("Rhs", Rhs.class, false, false),
	REFERENCE("Reference", Reference.class, false, false),
	NAME("Name", GenericModel.class, true, true),
	END_TOKEN("EndToken", null, false, true),
	EMPTY_TOKEN("EmptyToken", null, false, true),
	EOL_TOK("EOLToken", GenericModel.class, true, true),
	EOF_TOK("EOFToken", GenericModel.class, true, true),
	ERROR_TOK("ErrorToken", GenericModel.class, true, true),
	COMMENT("Comment", GenericModel.class, false, false),

	INCLUDE("Include", GenericModel.class, true, true),
	NOTICE("Notice", GenericModel.class, true, true),
	EXPORT("Export", GenericModel.class, true, true),
	IMPORT("Import", ModelBase.class, true, true),
	HEADER("Header", GenericModel.class, true, true),
	TRAILER("Trailer", GenericModel.class, true, true),
	GLOBAL("Global", GenericModel.class, true, true),
	KEYWORD("Keyword", GenericModel.class, true, true),
	IDENTIFIER("Identifier", GenericModel.class, true, true),
	TYPE("Type", Rule.class, true, true),
	RECOVER("Recover", GenericModel.class, true, true),
	DROP_ACTION("DropActions", null, false, true),
	DROP_SYMBOL("DropSymbols", null, false, true),
	DROP_RULE("DropRules", null, false, true),
	MACRO_BLOCK("MacroBlock", GenericModel.class, false, false),
	AST("Ast", GenericModel.class, true, true);

	private String string;
	private boolean sectionType;
	private boolean keyword;
	private Class<?> modelClass;

	ModelType(String string, Class<?> modelClass, boolean sectionType, boolean keyword) {
		this.string = string;
		this.sectionType = sectionType;
		this.keyword = keyword;
		this.modelClass = modelClass;
	}

	public boolean matches(Set<ModelType> filter) {
		return filter.contains(this);
	}

	public String getString() {
		return string;
	}

	public boolean isSectionType() {
		return sectionType;
	}

	public boolean isKeyword() {
		return keyword;
	}

	public Class<?> getModelClass() {
		return modelClass;
	}

	@Override
	public String toString() {
		return string;
	}
}
