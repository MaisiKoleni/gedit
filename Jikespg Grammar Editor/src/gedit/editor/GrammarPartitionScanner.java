/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

public class GrammarPartitionScanner extends RuleBasedPartitionScanner {
	public final static String GRAMMAR_COMMENT = "__grammar_comment";
	public final static String GRAMMAR_OPTION = "__grammar_option";
	public final static String GRAMMAR_MACRO = "__grammar_macro";
	public final static String GRAMMAR_STRING = "__grammar_string";
	public final static String GRAMMAR_OPERATOR = "__grammar_operator";

	public GrammarPartitionScanner() {

		IToken comment = new Token(GRAMMAR_COMMENT);
		IToken option = new Token(GRAMMAR_OPTION);
		IToken macro = new Token(GRAMMAR_MACRO);
		IToken string = new Token(GRAMMAR_STRING);
		IToken operator = new Token(GRAMMAR_OPERATOR);

		IPredicateRule[] rules = {
			new SingleLineRule("--", null, comment, (char) 0, true),
			new MultiLineRule("/.", "./", macro, (char) 0, true),
			new MultiLineRule("/:", ":/", macro, (char) 0, true),
			new SingleLineRule("'", "'", string, (char) 0, true),
			new CaseInsensitiveSingleLineRule("%Options", null, option, (char) 0, true),
			new OperatorRule(operator),
		};

		setPredicateRules(rules);
	}
}
