/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class OperatorRule implements IPredicateRule {
	private IToken fToken;

	public OperatorRule(IToken token) {
		fToken = token;
	}

	private IToken unread(ICharacterScanner scanner, int num) {
		while (num-- > 0)
			scanner.unread();
		return Token.UNDEFINED;
	}

	public IToken evaluate(ICharacterScanner scanner, boolean resume) {
		return evaluate(scanner);
	}

	public IToken evaluate(ICharacterScanner scanner) {
		switch (scanner.read()) {
		case ':':
			if (scanner.read() != ':')
				return unread(scanner, 2);
			if (scanner.read() == '=')
				return fToken;
			return unread(scanner, 3);
		case '-':
			if (scanner.read() == '>')
				return fToken;
			return unread(scanner, 2);
		}
		return unread(scanner, 1);
	}
	
	public IToken getSuccessToken() {
		return fToken;
	}
	
}