/*
 * (c) Copyright 2002, 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

class Scanner implements jpgsym
{
	protected Token current;
	protected int currentTokenOffset;
	protected int currentLine = 1;
	protected String errorMessage;
	protected int scanPos;
	private int peekLine;
	private int peekPos;
	private String[] eol;
	private char[] buf;
	private Document document;

	private static final char EOF = (char) -1;

	public Scanner(Document document, String text, String eol) {
		this.document = document;
		buf = text.toCharArray();
		String defaultEol = System.getProperty("line.separator", Character.toString((char) 10));
		if (eol == null)
			eol = defaultEol;
		this.eol = new String[eol.length()];
		for (int i = 0; i < this.eol.length; i++) {
			this.eol[i] = Character.toString(eol.charAt(i));
		}
	}

	public Scanner(Document document, Reader in, String eol) throws IOException {
		this(document, readComplete(in), eol);
    }

	private static String readComplete(Reader in) throws IOException {
		char[] chars = new char[4000];
		StringBuffer sb = new StringBuffer();
		for (;;) {
			int count = in.read(chars);
			if (count == -1)
				break;
			sb.append(chars, 0, count);
		} 
		return sb.toString();
	}

    public Token scanToken()
    {
    	peekPos = scanPos;
    	peekLine = currentLine;
    	current = doScan();
    	
    	scanPos = peekPos;
    	currentLine = peekLine;
    	
    	current.offset = currentTokenOffset;
    	current.length = peekPos - currentTokenOffset;
    	
    	return current;
    }

	private Token doScan() {
    	char c = handleInitialWhitespace();
    	currentTokenOffset = peekPos;
		int start = peekPos;

    	Token token = new Token();
        switch (c) {
		case EOF:
	        token.kind = TK_EOF;
			token.name = "";
			break;
		case '-':
			switch (peekChar(peekPos + 1)) {
			case '>':
	    		token.kind = TK_ARROW;
	    		peekPos += 2;
	    		break;
			case '-':
				return scanComment();
			}
			break;
		case ':':
			if (peekChar(peekPos + 1) == ':' && peekChar(peekPos + 2) == '=') {
	    		token.kind = TK_EQUIVALENCE;
	    		peekPos += 3;
			}
			break;
		case '|':
			peekPos++;
			token.kind = TK_OR;
			break;
		default:
			if (c == Document.DEFAULT_ESCAPE && matches("options", 1)) { //$NON-NLS-1$
				peekPos += 8;
	    		processOptionLine();
				return doScan();
			} else if (c == document.getEsape()) {
				token.kind = scanKeyword();
	        } else if (matches(document.hblockb, 0)) {
	        	token.kind = processMacro(c, document.hblockb.length(), document.hblocke, TK_HBLOCK);
	        } else if (matches(document.blockb, 0)) {
	        	token.kind = processMacro(c, document.blockb.length(), document.blocke, TK_BLOCK);
	    	}
        }
        if (token.kind == 0) {
        	while (c != EOF && !Character.isWhitespace(c))
    			c = peekChar(++peekPos);
            token.kind = TK_SYMBOL;
        }
    	token.name = new String(buf, start, peekPos - start);
        return token;
	}

    private Token scanComment() {
    	char c = peekChar(peekPos);
    	while (c != EOF && c != '\n')
			c = peekChar(++peekPos);
		return doScan();
	}

	private boolean isTokenSeparatorChar(char c) {
		return c == (char) -1
			|| Character.isWhitespace(c)
			|| isTokenSeparatorNotWhitespaceChar(c);
    }
    
	private boolean isTokenSeparatorNotWhitespaceChar(char c) {
		return c != c;
	}

	private boolean matches(String string, int offset) {
		for (int i = 0, n = string.length(); i < n; i++, offset++) {
			if (Character.toLowerCase(peekChar(peekPos + offset)) != string.charAt(i))
				return false;
		}
		return true;
	}

	private int scanKeyword() {
		char c = Character.toLowerCase(peekChar(peekPos + 1));
    	switch (c) {
		case EOF:
	        return TK_EOF;
    	case 'd':
    		if (matches("efine", 2)) //$NON-NLS-1$
    			if (isTokenSeparatorChar(peekChar(peekPos + 7))) { peekPos += 7; return TK_DEFINE_KEY; }
			break;
    	case 't':
    		if (matches("erminals", 2)) //$NON-NLS-1$
    			if (isTokenSeparatorChar(peekChar(peekPos + 10))) { peekPos += 10; return TK_TERMINALS_KEY; }
    		break;
    	case 'a':
    		if (matches("lias", 2)) //$NON-NLS-1$
    			if (isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_ALIAS_KEY; }
    		break;
    	case 'r':
    		if (matches("ules", 2)) //$NON-NLS-1$
    			if (isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_RULES_KEY; }
    		break;
    	case 's':
    		if (matches("tart", 2)) //$NON-NLS-1$
    			if (isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_START_KEY; }
    		break;
    	case 'n':
    		if (matches("ames", 2)) //$NON-NLS-1$
    			if (isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_NAMES_KEY; }
    		break;
    	case 'e':
    		switch(Character.toLowerCase(peekChar(peekPos + 2))) {
    		case 'm':
        		if (matches("pty", 3)) //$NON-NLS-1$
        			if (isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_EMPTY_SYMBOL; }
        		break;
    		case 'n':
    			if (matches("d", 3)) //$NON-NLS-1$
    				if (isTokenSeparatorChar(peekChar(peekPos + 4))) { peekPos += 4; return TK_END_KEY; }
    			break;
    		case 'o':
        		switch(Character.toLowerCase(peekChar(peekPos + 3))) {
        		case 'l':
        			peekPos += 5; return TK_EOL_SYMBOL;
        		case 'f':
        			peekPos += 5; return TK_EOF_SYMBOL;
        		}
        		break;
    		case 'r':
    			if (matches("ror", 3)) //$NON-NLS-1$
    				if (isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_ERROR_SYMBOL; }
    			break;
    		}
    		break;
    	}
		while ((c = peekChar(++peekPos)) != EOF && !Character.isWhitespace(c)) ;
		return TK_MACRO_NAME;
	}

	private int processMacro(char c, int offset, String endSequence, int successToken) {
		while (c != EOF && !matchesSequence(c, offset, endSequence))
			c = peekChar(++offset + peekPos);
		if (c != EOF) {
			peekPos += offset + endSequence.length();
			return successToken;
		}
		errorMessage = "Unterminated macro";
		peekPos = buf.length;
		return TK_$error;
	}

	private boolean matchesSequence(char c, int offset, String endSequence) {
		for (int i = 0; i < endSequence.length() && c != EOF; i++) {
			if (c != endSequence.charAt(i))
				return false;
			c = peekChar(++offset + peekPos);
		}
		return true;
	}

	private void processOptionLine() {
		List options = new ArrayList();
		char c;
		do {
			c = peekChar(peekPos);
			while (c != EOF && c == ' ')
				c = peekChar(++peekPos);
			int start = peekPos;
			while (c != EOF && c != 13 && c != '\n' && c != ' ' && c != ',' && c != '=')
				c = peekChar(++peekPos);
			String option = new String(buf, start, peekPos - start);
			if (c == EOF)
				return;
			if (option.length() == 0 || "=".equals(option) || ",".equals(option))
				peekPos++;
			else
				options.add(option);
		} while (c != EOF && !isLineBreakChar(c));
		for (int i = 0; i < options.size(); i++) {
			String option = (String) options.get(i);
			String value = i + 1 < options.size() ? (String) options.get(i + 1) : null;
			if (option.toLowerCase().startsWith("es")) {
				if (value != null && value.length() > 0) {
					document.escape = value.charAt(0);
					i++;
				}
			} else if (option.equals("hblockb")) {
				if (value != null && value.length() > 0) {
					document.hblockb = value;
					i++;
				}
			} else if (option.equals("hblocke")) {
				if (value != null && value.length() > 0) {
					document.hblocke = value;
					i++;
				}
			} else if (option.equals("blockb")) {
				if (value != null && value.length() > 0) {
					document.blockb = value;
					i++;
				}
			} else if (option.equals("blocke")) {
				if (value != null && value.length() > 0) {
					document.blocke = value;
					i++;
				}
			}
		}
	}
	
    private char handleInitialWhitespace() {
		char c = peekChar(peekPos);
		while ((c == '\n' && ++peekLine > 0) || Character.isWhitespace(c))
			c = peekChar(++peekPos);
		return c;
    }

    private boolean isLineBreakChar(char c) {
    	return (c == EOF) || (c == '\n' && ++peekLine > 0);
    }

    private char peekChar(int p) {
		return p < buf.length ? (char) buf[p] : EOF;
	}

	protected String get(int offset, int length) {
		return new String(buf, offset, offset + length);
	}

	public String toString() {
		if (buf == null)
			return "empty";
		StringBuffer sb = new StringBuffer();
		if (peekPos >= 0)
			sb.append(new String(buf, 0, peekPos < buf.length ? peekPos + 1 : peekPos ));
		sb.append(">>>>>>>>>>>>>>>>next char: '");
		if (peekPos + 1 < buf.length)
			sb.append(buf[peekPos + 1]);
		else
			sb.append("EOF");
		sb.append("'<<<<<<<<<<<<<<<<");
		if (peekPos + 2 < buf.length)
			sb.append(new String(buf, peekPos + 2, buf.length - (peekPos + 2 + 1)));
		return sb.toString();
	}

}
