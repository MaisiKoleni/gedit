/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.model.Document;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class GrammarScanner extends RuleBasedScanner {
	private class WhitespaceDetector implements IWhitespaceDetector {

		public boolean isWhitespace(char c) {
			return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
		}
	};

	private String fPreferenceKey;
	private PreferenceUtils fUtils;
	private MacroKeyRule fMacroKeyRule;
	private boolean fDetectOperators;
	private boolean fDetectQuotes;
	
	public GrammarScanner(String preferenceKey, PreferenceUtils utils, boolean detectOperators, boolean detectQuotes) {
		fPreferenceKey = preferenceKey;
		fUtils = utils;
		fDetectOperators = detectOperators;
		fDetectQuotes = detectQuotes;

		setRules();
	}
	
	private void setRules() {
		IToken macroKeyToken = new Token(fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_MACRO_KEY));
		IToken operator = new Token(fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_OPERATOR));

		IRule[] rules = new IRule[fDetectOperators ? 3 : 2];
		rules[0] = fMacroKeyRule = new MacroKeyRule(new MacroKeyDetector(), macroKeyToken);
		rules[1] = new WhitespaceRule(new WhitespaceDetector());
		if (fDetectOperators)
			rules[2] = new OperatorRule(operator);
		if (!fDetectQuotes)
			fMacroKeyRule.getMacroKeyDetector().setQuoteChars(new char[0]);

		setRules(rules);

		IToken defaultToken = new Token(fUtils.createTextAttribute(fPreferenceKey));
		setDefaultReturnToken(defaultToken);
	}
	
	public void setRange(IDocument document, int offset, int length) {
		super.setRange(document, offset, length);
		Document model = GrammarEditorPlugin.getDocumentModel(document, null, false);
		fMacroKeyRule.getMacroKeyDetector().setEscape(model.getOptions().getEsape());
		fMacroKeyRule.getMacroKeyDetector().setMakros(model.getAllMakros());
		if (fDetectOperators) {
			OperatorRule operatorRule = (OperatorRule) fRules[2];
			operatorRule.reset();
			operatorRule.setOrMarker(model.getOptions().getOrMarker());
		}
	}
	
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (affectsBehavior(event))
			setRules();
	}

	public boolean affectsBehavior(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (fPreferenceKey == null)
			return AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND.equals(property) ||
					AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT.equals(property) ||
					property != null && (property.startsWith(PreferenceConstants.GRAMMAR_COLORING_MACRO_KEY) ||
							property.startsWith(PreferenceConstants.GRAMMAR_COLORING_OPERATOR));
		return property != null && (
				property.startsWith(PreferenceConstants.GRAMMAR_COLORING_MACRO_KEY) ||
				property.startsWith(fPreferenceKey) ||
				property.startsWith(PreferenceConstants.GRAMMAR_COLORING_OPERATOR));
	}
	
}
