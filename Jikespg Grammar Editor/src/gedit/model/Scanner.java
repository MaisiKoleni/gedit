/*
 * (c) Copyright 2002, 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;
import java.util.ArrayList;
import java.util.List;

class Scanner implements jpgsym {
	protected Token current;
	protected int currentTokenOffset;
	protected String errorMessage;
	protected int scanPos;
	protected boolean tokenizeLbr;
	private boolean tokenizeComments;
	private int peekPos;
	private char[] buf;
	private Document document;

	private static final char EOF = (char) -1;
	private static final char EOL = (char) -2;
	
	public final static int TK_EOL = 9998;
	public final static int TK_COMMENT = 9999;

	public Scanner(Document document, String text, boolean tokenizeComments) {
		this.document = document;
		this.tokenizeComments = tokenizeComments;
		buf = text.toCharArray();
	}

    public Token scanToken() {
    	peekPos = scanPos;
    	errorMessage = null;
    	current = doScan();
    	
    	scanPos = peekPos;
    	
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
		case EOL:
			if (!tokenizeLbr)
				return doScan();
			token.kind = TK_EOL;
			break;
		case '-':
			switch (peekChar(peekPos + 1)) {
			case '>':
				if (!Character.isWhitespace(peekChar(peekPos + 2)))
					errorMessage = "Whitespace required after arrow";
	    		token.kind = TK_ARROW;
	    		peekPos += 2;
	    		break;
			case '-':
				token.kind = scanComment();
				if (!tokenizeComments)
					return doScan();
			}
			break;
		case ':':
			if (peekChar(peekPos + 1) == ':' && peekChar(peekPos + 2) == '=') {
				if (!Character.isWhitespace(peekChar(peekPos + 3)))
					errorMessage = "Whitespace required after equivalence";
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
	    		token.kind = processOptionLine();
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

    private int scanComment() {
    	char c = peekChar(peekPos);
    	while (c != EOF && c != '\n')
			c = peekChar(++peekPos);
		return TK_COMMENT;
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

	private int processOptionLine() {
		List options = new ArrayList();
		char c;
		int eolOffset = 0;
		do {
			c = peekChar(peekPos);
			while (c != EOF && c == ' ')
				c = peekChar(++peekPos);
			int start = peekPos;
			while (c != EOF && (eolOffset = isEol(c, peekPos)) == 0 && c != ' ' && c != ',' && c != '=')
				c = peekChar(++peekPos);
			String option = new String(buf, start, peekPos - start);
			if (c == EOF)
				return TK_OPTION_LINE;
			if (option.length() == 0 || "=".equals(option) || ",".equals(option)) {
				if (eolOffset == 0)
					peekPos++;
			} else
				options.add(option.toLowerCase());
		} while (eolOffset == 0);
		for (int i = 0; i < options.size(); i++) {
			String option = (String) options.get(i);
			String value = i + 1 < options.size() ? (String) options.get(i + 1) : null;
			if (option.startsWith("es")) {
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
		return TK_OPTION_LINE;
	}
	
    private char handleInitialWhitespace() {
		char c = peekChar(peekPos);
		while (Character.isWhitespace(c)) {
			if (tokenizeLbr) {
				int pos = isEol(c, peekPos);
				peekPos += pos;
				if (pos > 0)
					return EOL;
			}
			c = peekChar(++peekPos);
		}
		return c;
    }
    
    private int isEol(char c, int pos) {
    	switch (c) {
    	default:
    		return 0;
    	case '\r':
    		return peekChar(pos + 1) == '\n' ? 2 : 1;
    	case '\n':
    		return 1;
    	}
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
