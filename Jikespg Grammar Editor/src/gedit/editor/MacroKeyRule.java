/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class MacroKeyRule implements IPredicateRule {
	protected static final int UNDEFINED = -1;
	private MacroKeyDetector fMacroKeyDetector;
	protected IToken fDefaultToken;
	protected int fColumn = UNDEFINED;
	private StringBuffer fBuffer = new StringBuffer();

	public MacroKeyRule(MacroKeyDetector macroKeyDetector, IToken token) {
		fDefaultToken = token;
		fMacroKeyDetector = macroKeyDetector;
	}

	public IToken evaluate(ICharacterScanner scanner) {
		int c = scanner.read();
		if (fMacroKeyDetector.isWordStart((char) c)) {
			if (fColumn == UNDEFINED || (fColumn == scanner.getColumn() - 1)) {

				fBuffer.setLength(0);
				c = scanner.read();
				for (;;) { 
					int result = fMacroKeyDetector.isWordPart((char) c, scanner, fBuffer);
					switch (result) {
					case MacroKeyDetector.NO_MATCH:
						scanner.unread();
						scanner.unread();
						return Token.UNDEFINED;
					case MacroKeyDetector.MATCH:
						scanner.unread();
						return fDefaultToken;
					}
					fBuffer.append((char) c);
					c = scanner.read();
				}
			}
		}

		scanner.unread();
		return Token.UNDEFINED;
	}

	public IToken evaluate(ICharacterScanner scanner, boolean resume) {
		return evaluate(scanner);
	}

	public IToken getSuccessToken() {
		return fDefaultToken;
	}

	public MacroKeyDetector getMacroKeyDetector() {
		return fMacroKeyDetector;
	}

}
