/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

import gedit.editor.PreferenceConstants;
import gedit.model.ModelType;

public class GrammarEditorPluginPreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {

		IPreferenceStore store = GrammarEditorPlugin.getDefault().getPreferenceStore();

		PreferenceConverter.setDefault(store, PreferenceConstants.GRAMMAR_COLORING_COMMENT, new RGB(128, 128, 128));

		PreferenceConverter.setDefault(store, PreferenceConstants.GRAMMAR_COLORING_OPTION, new RGB(30, 90, 90));
		store.setDefault(PreferenceConstants.GRAMMAR_COLORING_OPTION + PreferenceConstants.EDITOR_ITALIC_SUFFIX, true);

		PreferenceConverter.setDefault(store, PreferenceConstants.GRAMMAR_COLORING_MACRO, new RGB(128, 0, 0));

		PreferenceConverter.setDefault(store, PreferenceConstants.GRAMMAR_COLORING_MACRO_KEY, new RGB(70, 70, 70));
		store.setDefault(PreferenceConstants.GRAMMAR_COLORING_MACRO_KEY + PreferenceConstants.EDITOR_BOLD_SUFFIX, true);

		PreferenceConverter.setDefault(store, PreferenceConstants.GRAMMAR_COLORING_LINK, new RGB(0, 0, 255));

		PreferenceConverter.setDefault(store, PreferenceConstants.GRAMMAR_COLORING_OPERATOR, new RGB(200, 0, 0));
		store.setDefault(PreferenceConstants.GRAMMAR_COLORING_OPERATOR + PreferenceConstants.EDITOR_BOLD_SUFFIX, true);

		PreferenceConverter.setDefault(store, PreferenceConstants.GRAMMAR_COLORING_ALIAS, new RGB(0, 128, 0));

		PreferenceConverter.setDefault(store, PreferenceConstants.GRAMMAR_COLORING_TERMINAL, new RGB(0, 0, 0));

		PreferenceConverter.setDefault(store, PreferenceConstants.GRAMMAR_COLORING_NON_TERMINAL, new RGB(0, 0, 128));


		store.setDefault(PreferenceConstants.EDITOR_MAXIMUM_PROBLEMS_REPORTED, 100);

		store.setDefault(PreferenceConstants.SECTION_ORDERING, createDefaultSectionOrdering());
	}

	private String createDefaultSectionOrdering() {
		ModelType[] types = {
				ModelType.OPTION,
				ModelType.INCLUDE,
				ModelType.NOTICE,
				ModelType.DEFINITION,
				ModelType.TERMINAL,
				ModelType.EXPORT,
				ModelType.IMPORT,
				ModelType.KEYWORD,
				ModelType.EOF_TOK,
				ModelType.EOL_TOK,
				ModelType.ERROR_TOK,
				ModelType.RECOVER,
				ModelType.IDENTIFIER,
				ModelType.START_TOK,
				ModelType.ALIAS,
				ModelType.NAME,
				ModelType.HEADER,
				ModelType.AST,
				ModelType.GLOBAL,
				ModelType.TRAILER,
				ModelType.RULE,
				ModelType.TYPE,
		};
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < types.length; i++) {
			if (i > 0)
				sb.append(PreferenceConstants.SECTION_ORDERING_SEPARATOR);
			sb.append(types[i].getBitPosition());
		}
		return sb.toString();
	}

}
