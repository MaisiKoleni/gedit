/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

import gedit.StringUtils.QuoteDetector;

public class OperatorRule implements IPredicateRule {
	private IToken fToken;
	private QuoteDetector fQuoteDetector;
	private char fOrMarker;

	public OperatorRule(IToken token) {
		fToken = token;
		fQuoteDetector = new QuoteDetector();
	}

	private IToken unread(ICharacterScanner scanner, int num) {
		while (num-- > 0)
			scanner.unread();
		return Token.UNDEFINED;
	}

	@Override
	public IToken evaluate(ICharacterScanner scanner, boolean resume) {
		return evaluate(scanner);
	}

	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		int c = scanner.read();
		fQuoteDetector.detect(c);
		if (!fQuoteDetector.isInQuotes()) {
			IToken token = doEvaluate(c, scanner);
			if (token != null)
				return token;
		}
		return unread(scanner, 1);
	}

	private IToken doEvaluate(int c, ICharacterScanner scanner) {
		switch (c) {
		default:
			if (c == fOrMarker) {
				if (!Character.isWhitespace((char) scanner.read()))
					return unread(scanner, 2);
				scanner.unread();
				return fToken;
			} else
				return null;
		case ':':
			if (scanner.read() != ':')
				return unread(scanner, 2);
			if (scanner.read() != '=')
				return unread(scanner, 3);
			c = scanner.read();
			if (c == '?') {
				if (!Character.isWhitespace((char) scanner.read()))
					return unread(scanner, 5);
				scanner.unread();
				return fToken;
			}
			if (Character.isWhitespace((char) c)) {
				scanner.unread();
				return fToken;
			}
			return unread(scanner, 4);
		case '-':
			if (scanner.read() != '>')
				return unread(scanner, 2);
			c = scanner.read();
			if (c == '?') {
				if (!Character.isWhitespace((char) scanner.read()))
					return unread(scanner, 4);
				scanner.unread();
				return fToken;
			}
			if (Character.isWhitespace((char) c)) {
				scanner.unread();
				return fToken;
			}
			return unread(scanner, 3);
		}
	}

	@Override
	public IToken getSuccessToken() {
		return fToken;
	}

	public void reset() {
		fQuoteDetector.reset();
	}

	public void setOrMarker(char orMarker) {
		fOrMarker = orMarker;
	}

}