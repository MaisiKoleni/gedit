/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.model.Document;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class GrammarScanner extends RuleBasedScanner {
	private class WhitespaceDetector implements IWhitespaceDetector {

		public boolean isWhitespace(char c) {
			return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
		}
	};

	private class MacroKeyDetector implements IWordDetector {
		private char fEscape = Document.DEFAULT_ESCAPE;
		public boolean isWordStart(char c) {
			return c == fEscape || c == Document.DEFAULT_ESCAPE;
		}
		public boolean isWordPart(char c) {
			return c != (char) ICharacterScanner.EOF && !Character.isWhitespace(c);
		}
	};
	
	private String fPreferenceKey;
	private PreferenceUtils fUtils;
	private MacroKeyDetector fMacroKeyDetector;
	
	public GrammarScanner(String preferenceKey, PreferenceUtils utils) {
		fPreferenceKey = preferenceKey;
		fUtils = utils;

		setRules();
	}
	
	private void setRules() {
		IToken macroKeyToken = new Token(fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_MACRO_KEY));

		IRule[] rules = {
				new WordRule(fMacroKeyDetector = new MacroKeyDetector(), macroKeyToken),
				new WhitespaceRule(new WhitespaceDetector()),
		};

		setRules(rules);

		IToken defaultToken = new Token(fUtils.createTextAttribute(fPreferenceKey));
		setDefaultReturnToken(defaultToken);
	}
	
	public void setRange(IDocument document, int offset, int length) {
		super.setRange(document, offset, length);
		fMacroKeyDetector.fEscape = GrammarEditorPlugin.getDocumentModel(document, null, false).getEsape();
	}
	
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (affectsBehavior(event))
			setRules();
	}

	public boolean affectsBehavior(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (fPreferenceKey == null)
			return AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND.equals(property) ||
					AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT.equals(property);
		return property != null && (
				property.startsWith(PreferenceConstants.GRAMMAR_COLORING_MACRO_KEY) ||
				property.startsWith(fPreferenceKey));
	}
	
}
