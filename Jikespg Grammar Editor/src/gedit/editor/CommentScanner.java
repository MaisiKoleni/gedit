/*
 * (c) Copyright 2002, 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.util.Assert;

public class CommentScanner extends RuleBasedScanner {
	private class TaskRule implements IRule {
		private IToken token;
		public TaskRule(IToken token) {
			this.token = token;
		}
		
		public IToken evaluate(ICharacterScanner scanner) {
			for (int c = scanner.read(), i = 0, j = 0; c != '\r' && c != '\n'; c = scanner.read(), j++) {
				if (c == ICharacterScanner.EOF) {
					if (j == 0)
						return Token.EOF;
					scanner.unread();
					break;
				}
				if (c == TASK[i]) {
					if (j > 0 && i == 0) {
						scanner.unread();
						break;
					}
					if (++i >= TASK.length)
						return token;
				} else
					i = 0;
			}
			return fDefaultReturnToken;
		}
	};
	
	private final static char[] TASK = "TODO".toCharArray();

	private PreferenceUtils fUtils;

	public CommentScanner(PreferenceUtils utils) {
		Assert.isNotNull(utils);
		fUtils = utils;
		Token comment = new Token(fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_COMMENT));
		Token task = new Token(fUtils.createTextAttribute(PreferenceConstants.GRAMMAR_COLORING_TASK));

		setRules(new IRule[] { new TaskRule(task) });
		
		setDefaultReturnToken(comment);
	}
}
