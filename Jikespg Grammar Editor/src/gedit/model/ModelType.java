/*
 * (c) Copyright 2002, 2005 Uwe Voigt All Rights Reserved.
 */
package gedit.model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class ModelType implements Comparable<ModelType> {
	private final static List<ModelType> ALL_TYPES = new ArrayList<>();

	public final static ModelType DOCUMENT = new ModelType("Document", Document.class, false, false);
	public final static ModelType SECTION = new ModelType("Section", Section.class, false, false);
	public final static ModelType OPTION = new ModelType("Option", Option.class, true, true);
	public final static ModelType DEFINITION = new ModelType("Definition", Definition.class, true, true);
	public final static ModelType TERMINAL = new ModelType("Terminal", GenericModel.class, true, true);
	public final static ModelType ALIAS = new ModelType("Alias", Alias.class, true, true);
	public final static ModelType START_TOK = new ModelType("StartToken", Reference.class, true, true);
	public final static ModelType RULE = new ModelType("Rule", ModelBase.class, true, true);
	public final static ModelType RHS = new ModelType("Rhs", Rhs.class, false, false);
	public final static ModelType REFERENCE = new ModelType("Reference", Reference.class, false, false);
	public final static ModelType NAME = new ModelType("Name", GenericModel.class, true, true);
	public final static ModelType END_TOK = new ModelType("EndToken", null, false, true);
	public final static ModelType EMPTY_TOK = new ModelType("EmptyToken", null, false, true);
	public final static ModelType EOL_TOK = new ModelType("EOLToken", GenericModel.class, true, true);
	public final static ModelType EOF_TOK = new ModelType("EOFToken", GenericModel.class, true, true);
	public final static ModelType ERROR_TOK = new ModelType("ErrorToken", GenericModel.class, true, true);
	public final static ModelType COMMENT = new ModelType("Comment", GenericModel.class, false, false);

	public final static ModelType INCLUDE = new ModelType("Include", GenericModel.class, true, true);
	public final static ModelType NOTICE = new ModelType("Notice", GenericModel.class, true, true);
	public final static ModelType EXPORT = new ModelType("Export", GenericModel.class, true, true);
	public final static ModelType IMPORT = new ModelType("Import", ModelBase.class, true, true);
	public final static ModelType HEADER = new ModelType("Header", GenericModel.class, true, true);
	public final static ModelType TRAILER = new ModelType("Trailer", GenericModel.class, true, true);
	public final static ModelType GLOBAL = new ModelType("Global", GenericModel.class, true, true);
	public final static ModelType KEYWORD = new ModelType("Keyword", GenericModel.class, true, true);
	public final static ModelType IDENTIFIER = new ModelType("Identifier", GenericModel.class, true, true);
	public final static ModelType TYPE = new ModelType("Type", Rule.class, true, true);
	public final static ModelType RECOVER = new ModelType("Recover", GenericModel.class, true, true);
	public final static ModelType DROP_ACTION = new ModelType("DropActions", null, false, true);
	public final static ModelType DROP_SYMBOL = new ModelType("DropSymbols", null, false, true);
	public final static ModelType DROP_RULE = new ModelType("DropRules", null, false, true);
	public final static ModelType MACRO_BLOCK = new ModelType("MacroBlock", GenericModel.class, false, false);
	public final static ModelType AST = new ModelType("Ast", GenericModel.class, true, true);

	private static int NUMBER;

	public static ModelType[] getAllTypes() {
		return ALL_TYPES.toArray(new ModelType[ALL_TYPES.size()]);
	}

	public static ModelType getByBitPosition(int position) {
		for (Object element : ALL_TYPES) {
			ModelType modelType = (ModelType) element;
			if (modelType.bitSet.get(position))
				return modelType;
		}
		return null;
	}

	private BitSet bitSet;
	private String string;
	private boolean sectionType;
	private boolean keyword;
	private Class<?> modelClass;

	private ModelType(String string, Class<?> modelClass, boolean sectionType, boolean keyword) {
		this.bitSet = createBitSet();
		this.string = string;
		this.sectionType = sectionType;
		this.keyword = keyword;
		this.modelClass = modelClass;
		ALL_TYPES.add(this);
	}

	private BitSet createBitSet() {
		BitSet set = new BitSet();
		set.set(NUMBER++);
		return set;
	}

	public BitSet or(ModelType type) {
		BitSet set = new BitSet();
		if (type != null)
			set.or(type.bitSet);
		return or(set);
	}

	public BitSet or(BitSet set) {
		if (set == null)
			set = new BitSet();
		set.or(bitSet);
		return set;
	}

	public boolean matches(BitSet filter) {
		BitSet set = new BitSet();
		set.set(bitSet.nextSetBit(0));
		set.and(filter);
		return !set.isEmpty();
	}

	public int getBitPosition() {
		return or((BitSet) null).nextSetBit(0);
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
	public int compareTo(ModelType o) {
		return string.compareToIgnoreCase(o.string);
	}

	@Override
	public String toString() {
		return string;
	}
}
