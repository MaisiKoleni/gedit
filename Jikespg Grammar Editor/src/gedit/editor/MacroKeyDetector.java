/*
 * (c) Copyright 2002, 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.StringUtils.QuoteDetector;
import gedit.model.Definition;
import gedit.model.DocumentOptions;
import gedit.model.ModelType;

import java.util.Iterator;
import java.util.TreeMap;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.viewers.ILabelProvider;

public class MacroKeyDetector {
	private TreeMap fMacros = new TreeMap();
	private TreeMap fCurrentMacros;
	private String fCurrentKey;
	private int fCurrentIndex;
	private Definition[] fMacrosCopy;
	private char fEscape = DocumentOptions.DEFAULT_ESCAPE;
	private QuoteDetector fQuoteDetector = new QuoteDetector();
	private ILabelProvider fLabelProvider = new ModelLabelProvider();

	private final static String DEF = " "; //$NON-NLS-1$
	
	public final static int NO_MATCH = 1;
	public final static int PROBABLE_MATCH = 2;
	public final static int MATCH = 3;
	
	public MacroKeyDetector() {
	}
	
	public MacroKeyDetector(char escape, Definition[] makros) {
		setEscape(escape);
		setMakros(makros);
	}
	
	public void setQuoteChars(char[] quoteCharPairs) {
		fQuoteDetector.setQuoteChars(quoteCharPairs);
	}
	
	public boolean isWordStart(char c) {
		fQuoteDetector.detect(c);
		if (c == fEscape && !fQuoteDetector.isInQuotes()) {
			fCurrentMacros = fMacros;
			fCurrentKey = null;
			fCurrentIndex = 0;
			return true;
		}
		return false;
	}

	public int isWordPart(char c, ICharacterScanner scanner, StringBuffer buffer) {
		if (fCurrentKey != null) {
			if (fCurrentIndex < fCurrentKey.length() && Character.toLowerCase(c) == fCurrentKey.charAt(fCurrentIndex)) {
				fCurrentIndex++;
				return PROBABLE_MATCH;
			} else if (fCurrentIndex == fCurrentKey.length()) {
				fCurrentKey = null;
				fCurrentIndex = 0;
				return MATCH;
			} else {
				return rewind(scanner, buffer);
			}
		}
		Object value = fCurrentMacros.get(String.valueOf(Character.toLowerCase(c)));
		if (value == null) {
			if (reachedWordEnd(buffer, (String) fCurrentMacros.get(DEF)))
				return MATCH;
			return rewind(scanner, buffer);
		}
		if (value instanceof TreeMap)
			fCurrentMacros = (TreeMap) value;
		else if (value instanceof String) {
			fCurrentKey = (String) value;
			if (reachedWordEnd(buffer, fCurrentKey))
				return MATCH;
		}
		fCurrentIndex++;
		return PROBABLE_MATCH;
	}
	
	private boolean reachedWordEnd(StringBuffer buffer, String word) {
		return buffer.toString().toLowerCase().equals(word);
	}

	private int rewind(ICharacterScanner scanner, StringBuffer buffer) {
		unreadBuffer(scanner, buffer);
		return NO_MATCH;
	}

	private void unreadBuffer(ICharacterScanner scanner, StringBuffer buffer) {
		for (int i = buffer.length() - 1; i >= 0; i--)
			scanner.unread();
		buffer.setLength(0);
	}

	public void setEscape(char escape) {
		fEscape = escape;
		fQuoteDetector.reset();
	}

	public synchronized void setMakros(Definition[] makros) {
		if (sameAsOnLastInvocation(makros))
			return;
		clear(fMacros);
		for (int i = 0; i < makros.length; i++) {
			insert(fMacros, makros[i].getLabel(), 0);
		}
		ModelType[] allTypes = ModelType.getAllTypes();
		for (int i = 0; i < allTypes.length; i++) {
			insert(fMacros, fLabelProvider.getText(allTypes[i]), 0);
		}
		fMacrosCopy = makros;
	}
	
	private boolean sameAsOnLastInvocation(Definition[] makros) {
		if (fMacrosCopy == null)
			return false;
		if (fMacrosCopy.length != makros.length)
			return false;
		for (int i = 0; i < fMacrosCopy.length; i++) {
			if (!fMacrosCopy[i].equals(makros[i]))
				return false;
		}
		return true;
	}

	private void clear(TreeMap map) {
		synchronized (map) {
			for (Iterator it = map.keySet().iterator(); it.hasNext();) {
				Object key = it.next();
				Object value = map.get(key);
				if (value instanceof TreeMap)
					clear((TreeMap) value);
				it.remove();
			}
		}
	}

	private void insert(TreeMap m, String name, int index) {
		if (name == null || name.length() == 0) // don't want error default
			return;
		name = name.toLowerCase();
		String key = name.length() <= index ? DEF : String.valueOf(name.charAt(index));
		Object val = m.get(key);
		if (val instanceof TreeMap) {
			insert((TreeMap) val, name, index + 1);
		} else if (val instanceof String) {
			String prev = (String) val;
			if (prev.equals(name)) { // special case for doubled adding of keywords
				if (GrammarEditorPlugin.DEBUG)
					System.out.println("Doubled added macro, ignored: " + name);
				return;
			}
			TreeMap p = new TreeMap();
			TreeMap p1 = p;
			for (int keyIndex = index + 1, n = Math.max(name.length(), prev.length()); keyIndex < n; keyIndex++) {
				String sKey1 = prev.length() <= keyIndex ? DEF : String.valueOf(prev.charAt(keyIndex));
				String sKey2 = name.length() <= keyIndex ? DEF : String.valueOf(name.charAt(keyIndex));
				if (sKey1.equals(sKey2)) {
					p1.put(sKey1, p1 = new TreeMap()); 
					continue;
				}
				p1.put(sKey1, prev);
				p1.put(sKey2, name);
				break;
			}
			val = p;
		} else {
			val = name;
		}
		m.put(key, val);
	}
}