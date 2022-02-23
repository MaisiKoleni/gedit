/*
 * (c) Copyright 2002 Uwe Voigt
 * All Rights Reserved.
 */
package gedit;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;

public class StringUtils {
	public static class QuoteDetector {
		private char[] fQuoteChars;
		private char fInQuotes;

		public final static char[] DEFAULT_QUOTE_CHARS = { '\'', '\'', '"', '"' };

		public QuoteDetector() {
			this(DEFAULT_QUOTE_CHARS);
		}

		public QuoteDetector(char[] quoteChars) {
			setQuoteChars(quoteChars);
		}

		public boolean detect(int c) {
			for (int i = fInQuotes == 0 ? 0 : 1; i < fQuoteChars.length; i += 2) {
				char quoteChar = fQuoteChars[i];
				if (c != quoteChar)
					continue;
				if (fInQuotes == quoteChar)
					fInQuotes = 0;
				else if (fInQuotes == 0)
					fInQuotes = getCorrespondingChar(i);
			}
			return isInQuotes();
		}

		private char getCorrespondingChar(int index) {
			return fQuoteChars[index % 2 == 0 ? index + 1 : index - 1];
		}

		public boolean isInQuotes() {
			return fInQuotes != 0;
		}

		public void reset() {
			fInQuotes = 0;
		}

		public void setQuoteChars(char[] quoteChars) {
			Assert.isNotNull(quoteChars);
			fQuoteChars = quoteChars;
			reset();
		}
	}

	public static String[] split(String value, String separator) {
		if (value == null)
			return new String[0];
		StringTokenizer st = new StringTokenizer(value, separator);
		String[] result = new String[st.countTokens()];
		for (int i = 0; i < result.length; i++) {
			result[i] = st.nextToken();
		}
		return result;
	}

	public static String trimQuotes(String text) {
		return trimQuotes(trimQuotes(text, '\''), '"');
	}

	public static String trimQuotes(String text, char quote) {
		return text != null && text.length() > 1 && text.charAt(0) == quote && text.charAt(text.length() -1) == quote
			? text.substring(1, text.length() - 1) : text;
	}

}
