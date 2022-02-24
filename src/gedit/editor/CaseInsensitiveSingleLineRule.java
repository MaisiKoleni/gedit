/*
 * (c) Copyright 2002 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

import gedit.StringUtils.QuoteDetector;

public class CaseInsensitiveSingleLineRule extends SingleLineRule {
	private QuoteDetector fQuoteDetector;
	private boolean fEndSequenceIncluded;
	private boolean fEolDetected;
	private char[][] fLineDelimiters;
	private char[][] fSortedLineDelimiters;
	private Comparator<char[]> fLineDelimiterComparator = (o1, o2) -> o2.length - o1.length;
	public CaseInsensitiveSingleLineRule(String startSequence, String endSequence,
			IToken token, char escapeCharacter, boolean breaksOnEOF, boolean endSequenceIncluded) {
		super(startSequence, endSequence, token, escapeCharacter, breaksOnEOF);
		fQuoteDetector = new QuoteDetector();
		fEndSequenceIncluded = endSequenceIncluded;
		for (int i = 0; i < fStartSequence.length; i++)
			fStartSequence[i] = Character.toLowerCase(fStartSequence[i]);
	}

	@Override
	protected IToken doEvaluate(ICharacterScanner scanner, boolean resume) {

		if (resume) {

			if (endSequenceDetected(scanner))
				return fToken;

		} else {

			fQuoteDetector.reset();

			int c= scanner.read();
			fQuoteDetector.detect(c);
			if (c == fStartSequence[0] && sequenceDetected(scanner, fStartSequence, true, true) && endSequenceDetected(scanner)) {
				if (!fEolDetected && !fEndSequenceIncluded && fEndSequence != null)
					for (int i = 0; i < fEndSequence.length; i++)
						scanner.unread();
				return fToken;
			}
		}

		scanner.unread();
		return Token.UNDEFINED;
	}

	@Override
	protected boolean endSequenceDetected(ICharacterScanner scanner) {

		char[][] originalDelimiters= scanner.getLegalLineDelimiters();
		int count= originalDelimiters.length;
		if (fLineDelimiters == null || originalDelimiters.length != count) {
			fSortedLineDelimiters= new char[count][];
		} else {
			while (count > 0 && fLineDelimiters[count-1] == originalDelimiters[count-1])
				count--;
		}
		if (count != 0) {
			fLineDelimiters= originalDelimiters;
			System.arraycopy(fLineDelimiters, 0, fSortedLineDelimiters, 0, fLineDelimiters.length);
			Arrays.sort(fSortedLineDelimiters, fLineDelimiterComparator);
		}

		int c;
		while ((c= scanner.read()) != ICharacterScanner.EOF) {
			fQuoteDetector.detect(c);
			if (c == fEscapeCharacter) {
				// Skip escaped character(s)
				if (fEscapeContinuesLine) {
					c= scanner.read();
					fQuoteDetector.detect(c);
					for (char[] fSortedLineDelimiter : fSortedLineDelimiters) {
						if (c == fSortedLineDelimiter[0] && sequenceDetected(scanner, fSortedLineDelimiter, true, false))
							break;
					}
				} else
					fQuoteDetector.detect(scanner.read());

			} else if (fEndSequence.length > 0 && c == fEndSequence[0]) {
				// Check if the specified end sequence has been found.
				if (sequenceDetected(scanner, fEndSequence, true, true))
					return true;
			} else if (fBreaksOnEOL) {
				// Check for end of line since it can be used to terminate the pattern.
				fEolDetected = false;
				for (char[] fSortedLineDelimiter : fSortedLineDelimiters) {
					if (c == fSortedLineDelimiter[0] && sequenceDetected(scanner, fSortedLineDelimiter, true, false)) {
						fEolDetected = true;
						return true;
					}
				}
			}
		}
		if (fBreaksOnEOF) return true;
		scanner.unread();
		return false;
	}

	protected boolean sequenceDetected(ICharacterScanner scanner, char[] sequence, boolean eofAllowed, boolean checkQuotes) {
		for (int i= 1; i < sequence.length; i++) {
			int c= scanner.read();
			if (c == ICharacterScanner.EOF && eofAllowed)
				return true;

			if (checkQuotes && fQuoteDetector.isInQuotes() || Character.toLowerCase((char) c) != sequence[i]) {
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