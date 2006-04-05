/*
 * (c) Copyright 2002, 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.model.Document;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IWordDetector;

public class MacroKeyDetector implements IWordDetector {
	private char fEscape = Document.DEFAULT_ESCAPE;
	private boolean fInQuotes;

	public boolean isWordStart(char c) {
		if (c == '\'')
			fInQuotes = !fInQuotes;
		return c == fEscape && !fInQuotes;
	}

	public boolean isWordPart(char c) {
		return c != (char) ICharacterScanner.EOF && !Character.isWhitespace(c);
	}
	
	public void setEscape(char escape) {
		fEscape = escape;
	}
}