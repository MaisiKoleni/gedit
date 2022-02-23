/*
 * (c) Copyright 2002, 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import gedit.StringUtils.QuoteDetector;

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
	private QuoteDetector quoteDetector;

	private static final char EOF = (char) -1;
	private static final char EOL = (char) -2;

	public Scanner(Document document, String text, boolean tokenizeComments) {
		this.document = document;
		this.tokenizeComments = tokenizeComments;
		buf = text.toCharArray();
		quoteDetector = new QuoteDetector();
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

    public Token peekToken() {
    	return doScan();
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
				switch (peekChar(peekPos + 2)) {
				case '?':
					if (Character.isWhitespace(peekChar(peekPos + 3))) {
			    		token.kind = TK_PRIORITY_ARROW;
			    		peekPos += 3;
					}
		    		break;
		    	default:
					if (Character.isWhitespace(peekChar(peekPos + 2))) {
			    		token.kind = TK_ARROW;
			    		peekPos += 2;
					}
		    		break;
				}
				break;
			case '-':
				token.kind = scanComment();
				if (!tokenizeComments)
					return doScan();
			}
			break;
		case ':':
			if (peekChar(peekPos + 1) == ':' && peekChar(peekPos + 2) == '=') {
				switch (peekChar(peekPos + 3)) {
				case '?':
					if (Character.isWhitespace(peekChar(peekPos + 4))) {
						token.kind = TK_PRIORITY_EQUIVALENCE;
			    		peekPos += 4;
					}
		    		break;
		    	default:
					if (Character.isWhitespace(peekChar(peekPos + 3))) {
						token.kind = TK_EQUIVALENCE;
						peekPos += 3;
					}
		    		break;
				}
			}
			break;
		default:
			if (c == DocumentOptions.DEFAULT_ESCAPE && matches("options", 1)) { //$NON-NLS-1$
				peekPos += 8;
	    		token.kind = processOptionLine();
			} else if (c == document.getOptions().getEsape()) {
				token.kind = scanKeyword();
			} else if (c == document.getOptions().getOrMarker()) {
				if (Character.isWhitespace(peekChar(peekPos + 1))) {
					peekPos++;
					token.kind = TK_OR;
				}
	        } else {
	        	String[] blockb = document.getOptions().getBlockBeginnings();
	        	String[] blocke = document.getOptions().getBlockEnds();
	        	for (int i = 0; i < blockb.length; i++) {
		        	if (blockb[i].length() > 0 && matches(blockb[i], 0)) {
			        	token.kind = processMacro(c, blockb[i].length(), blocke[i], TK_BLOCK);
		        	}
				}
	        }
        }
        if (token.kind == 0) {
        	while (c != EOF && (quoteDetector.detect(c) || !Character.isWhitespace(c)))
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
    		switch (Character.toLowerCase(peekChar(peekPos + 2))) {
			case 'e':
	    		if (matches("fine", 3) && isTokenSeparatorChar(peekChar(peekPos + 7))) { peekPos += 7; return TK_DEFINE_KEY; }
				break;
			case 'r':
	    		switch (Character.toLowerCase(peekChar(peekPos + 3))) {
	    		case 'o':
		    		switch (Character.toLowerCase(peekChar(peekPos + 4))) {
		    		case 'p':
			    		switch (Character.toLowerCase(peekChar(peekPos + 5))) {
			    		case 's':
				    		if (matches("ymbols", 6) && isTokenSeparatorChar(peekChar(peekPos + 12))) { peekPos += 12; return TK_DROPSYMBOLS_KEY; }
				    		break;
			    		case 'a':
				    		if (matches("ctions", 6) && isTokenSeparatorChar(peekChar(peekPos + 12))) { peekPos += 12; return TK_DROPACTIONS_KEY; }
				    		break;
			    		case 'r':
				    		if (matches("ules", 6) && isTokenSeparatorChar(peekChar(peekPos + 10))) { peekPos += 10; return TK_DROPRULES_KEY; }
				    		break;
			    		}
			    		break;
		    		}
		    		break;
	    		}
	    		break;
			}
			break;
    	case 't':
    		switch (Character.toLowerCase(peekChar(peekPos + 2))) {
    		case 'e':
        		if (matches("rminals", 3) && isTokenSeparatorChar(peekChar(peekPos + 10))) { peekPos += 10; return TK_TERMINALS_KEY; }
        		break;
    		case 'r':
        		if (matches("ailers", 3) && isTokenSeparatorChar(peekChar(peekPos + 9))) { peekPos += 9; return TK_TRAILERS_KEY; }
        		break;
    		case 'y':
        		if (matches("pes", 3) && isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_TYPES_KEY; }
        		break;
    		}
    		break;
    	case 'a':
    		switch (Character.toLowerCase(peekChar(peekPos + 2))) {
			case 'l':
	    		if (matches("ias", 3) && isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_ALIAS_KEY; }
				break;
			case 's':
	    		if (matches("t", 3) && isTokenSeparatorChar(peekChar(peekPos + 4))) { peekPos += 4; return TK_AST_KEY; }
				break;
			}
    		break;
    	case 'r':
    		switch (Character.toLowerCase(peekChar(peekPos + 2))) {
    		case 'e':
        		if (matches("cover", 3) && isTokenSeparatorChar(peekChar(peekPos + 8))) { peekPos += 8; return TK_RECOVER_KEY; }
        		break;
    		case 'u':
        		if (matches("les", 3) && isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_RULES_KEY; }
        		break;
    		}
    		break;
    	case 's':
    		if (matches("tart", 2) && isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_START_KEY; }
    		break;
    	case 'n':
    		switch (Character.toLowerCase(peekChar(peekPos + 2))) {
    		case 'a':
    			if (matches("mes", 3) && isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_NAMES_KEY; }
    			break;
    		case 'o':
    			if (matches("tice", 3) && isTokenSeparatorChar(peekChar(peekPos + 7))) { peekPos += 7; return TK_NOTICE_KEY; }
    			break;
    		}
    		break;
    	case 'e':
    		switch(Character.toLowerCase(peekChar(peekPos + 2))) {
    		case 'm':
        		if (matches("pty", 3) && isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_EMPTY_KEY; }
        		break;
    		case 'n':
    			if (matches("d", 3) && isTokenSeparatorChar(peekChar(peekPos + 4))) { peekPos += 4; return TK_END_KEY; }
    			break;
    		case 'o':
        		switch(Character.toLowerCase(peekChar(peekPos + 3))) {
        		case 'l':
        			if (isTokenSeparatorChar(peekChar(peekPos + 4))) { peekPos += 4; return TK_EOL_KEY; }
        			break;
        		case 'f':
        			if (isTokenSeparatorChar(peekChar(peekPos + 4))) { peekPos += 4; return TK_EOF_KEY; }
        			break;
        		}
        		break;
    		case 'r':
    			if (matches("ror", 3) && isTokenSeparatorChar(peekChar(peekPos + 6))) { peekPos += 6; return TK_ERROR_KEY; }
    			break;
    		case 'x':
    			if (matches("port", 3) && isTokenSeparatorChar(peekChar(peekPos + 7))) { peekPos += 7; return TK_EXPORT_KEY; }
    			break;
    		}
    		break;
    	case 'i':
    		switch(Character.toLowerCase(peekChar(peekPos + 2))) {
    		case 'd':
        		if (matches("entifier", 3) && isTokenSeparatorChar(peekChar(peekPos + 11))) { peekPos += 11; return TK_IDENTIFIER_KEY; }
        		break;
    		case 'n':
        		if (matches("clude", 3) && isTokenSeparatorChar(peekChar(peekPos + 8))) { peekPos += 8; return TK_INCLUDE_KEY; }
        		break;
    		case 'm':
	    		if (matches("port", 3) && isTokenSeparatorChar(peekChar(peekPos + 7))) { peekPos += 7; return TK_IMPORT_KEY; }
	    		break;
    		}
    		break;
    	case 'h':
    		if (matches("eaders", 2) && isTokenSeparatorChar(peekChar(peekPos + 8))) { peekPos += 8; return TK_HEADERS_KEY; }
    		break;
    	case 'k':
    		if (matches("eywords", 2) && isTokenSeparatorChar(peekChar(peekPos + 9))) { peekPos += 9; return TK_KEYWORDS_KEY; }
    		break;
    	case 'g':
    		if (matches("lobals", 2) && isTokenSeparatorChar(peekChar(peekPos + 8))) { peekPos += 8; return TK_GLOBALS_KEY; }
    		break;
    	}
		while ((c = peekChar(++peekPos)) != EOF && !Character.isWhitespace(c)) ;
		return TK_MACRO_NAME;
	}

	private int processMacro(char c, int offset, String endSequence, int successToken) {
		while ((c = peekChar(offset + peekPos)) != EOF && !matchesSequence(c, offset, endSequence))
			offset++;
		if (c != EOF) {
			peekPos += offset + endSequence.length();
			return successToken;
		}
		errorMessage = "Unterminated macro";
		peekPos = buf.length;
		return TK_ERROR;
	}

	private boolean matchesSequence(char c, int offset, String endSequence) {
		for (int i = 0; i < endSequence.length(); i++) {
			if (c == EOF || c != endSequence.charAt(i))
				return false;
			offset++;
			c = peekChar(offset + peekPos);
		}
		return true;
	}

	private int processOptionLine() {
		char c = peekChar(peekPos);
		while (c != EOF && isEol(c, peekPos) == 0)
			c = peekChar(++peekPos);
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

	@Override
	public String toString() {
		if (buf == null)
			return "empty";
		StringBuilder sb = new StringBuilder();
		if (peekPos >= 0)
			sb.append(new String(buf, 0, peekPos < buf.length ? peekPos + 1 : peekPos ));
		sb.append(">>>>>>>>>>>>>>>>next char: '");
		if (peekPos + 1 < buf.length)
			sb.append(buf[peekPos + 1]);
		else
			sb.append("EOF");
		sb.append("'<<<<<<<<<<<<<<<<");
		if (peekPos + 2 < buf.length)
			sb.append(new String(buf, peekPos + 2, buf.length - (peekPos + 2)));
		return sb.toString();
	}

}
