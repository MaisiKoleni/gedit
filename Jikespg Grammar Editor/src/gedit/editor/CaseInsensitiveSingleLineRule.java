/*
 * (c) Copyright 2002 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.StringUtils.QuoteDetector;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

public class CaseInsensitiveSingleLineRule extends SingleLineRule {
	private QuoteDetector fQuoteDetector;

	public CaseInsensitiveSingleLineRule(String startSequence, String endSequence, IToken token, char escapeCharacter, boolean breaksOnEOF) {
		super(startSequence, endSequence, token, escapeCharacter, breaksOnEOF);
		fQuoteDetector = new QuoteDetector();
		for (int i = 0; i < fStartSequence.length; i++)
			fStartSequence[i] = Character.toLowerCase(fStartSequence[i]);
	}

	protected IToken doEvaluate(ICharacterScanner scanner, boolean resume) {

		if (resume) {

			if (endSequenceDetected(scanner))
				return fToken;

		} else {

			int c= scanner.read();
			fQuoteDetector.detect(c);
			if (c == fStartSequence[0]) {
				if (sequenceDetected(scanner, fStartSequence, false)) {
					if (endSequenceDetected(scanner))
						return fToken;
				}
			}
		}

		scanner.unread();
		return Token.UNDEFINED;
	}

	protected boolean sequenceDetected(ICharacterScanner scanner, char[] sequence, boolean eofAllowed) {
		for (int i= 1; i < sequence.length; i++) {
			int c= scanner.read();
			if (c == ICharacterScanner.EOF && eofAllowed)
				return true;

			if (fQuoteDetector.isInQuotes() || Character.toLowerCase((char) c) != sequence[i]) {
				// Non-matching character detected, rewind the scanner back to the start.
				// Do not unread the first character.
				scanner.unread();
				for (int j= i-1; j > 0; j--)
					scanner.unread();
				return false;
			}
		}

		return true;
	}
	
	public void reset() {
		fQuoteDetector.reset();
	}
}