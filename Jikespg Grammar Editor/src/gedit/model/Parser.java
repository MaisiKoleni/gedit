/*
 * (c) Copyright 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gedit.GrammarEditorPlugin;
import gedit.StringUtils;
import gedit.StringUtils.QuoteDetector;
import gedit.model.ModelUtils.OptionAmbigousException;
import gedit.model.ModelUtils.OptionProposal;

class Parser extends jpgprs {

	private int state_stack_top,
        stack[],
    	tok_stack_top,
    	parse_stack_top,
    	model_stack_top;
	private Token[] tok_stack;
	private Node[] parse_stack;
	private ModelBase[] model_stack;

	private Scanner scanner;
	private Document document;
	private IProblemRequestor problemRequestor;
	private FileProzessor fileProzessor;
	private Map elements;
	private Rule currentRule;
	private Rhs currentRuleRhs;
	private int ruleSymbolNumber;
	private Map rulesLookup;
	private boolean optionsProcessed;

	public final int t_action(int act, int sym) {
        act = tAction(act, sym);
        if (act > LA_STATE_OFFSET)
        {
			Token la = scanner.peekToken();
			while (la.kind == TK_COMMENT | la.kind == TK_EOL)
				la = scanner.peekToken();
			act = lookAhead(act - LA_STATE_OFFSET, la.kind);
            while(act > LA_STATE_OFFSET)
            {
                la = nextToken();
                act = lookAhead(act - LA_STATE_OFFSET, la.kind);
            }
    		if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 2)
    			System.out.println("  lookahead: " + la.name + " (" + orderedTerminalSymbols[la.kind] + ")");
        }
        return act;
    }

    private void pushAct(int act) {
		if (++state_stack_top >= stack.length) {
			int[] tmp = new int[stack.length + 50];
			System.arraycopy(stack, 0, tmp, 0, stack.length);
			stack = tmp;
		}
		stack[state_stack_top] = act;
	}

	private Node pushAst(Node node) {
		int length = parse_stack.length;
		if (++parse_stack_top >= length)
			System.arraycopy(parse_stack, 0, parse_stack = new Node[length + 15], 0, length);
		return parse_stack[parse_stack_top] = node;
	}

	private Node popAst() {
		return parse_stack[parse_stack_top--];
	}

	private Node peekAst() {
		return parse_stack[parse_stack_top];
	}

	private ModelBase pushModel(ModelBase model) {
		int length = model_stack.length;
		if (++model_stack_top >= length)
			System.arraycopy(model_stack, 0, model_stack = new ModelBase[length + 15], 0, length);
		return model_stack[model_stack_top] = model;
	}

	private ModelBase popModel() {
		return model_stack[model_stack_top--];
	}

	private ModelBase peekModel() {
		return model_stack[model_stack_top];
	}

	private void pushToken(Token token) {
		int length = tok_stack.length;
		if (++tok_stack_top >= length)
			System.arraycopy(tok_stack, 0, tok_stack = new Token[length + 32], 0, length);
		tok_stack[tok_stack_top] = token;
	}

    private Token popToken() {
		return tok_stack[tok_stack_top--];
	}

    private void token_action(Token token) {
    	pushToken(token);
    }

	public Parser(Document document, String text, IProblemRequestor problemRequestor, FileProzessor fileProzessor) {
        this.document = document;
        this.problemRequestor = problemRequestor;
        this.fileProzessor = fileProzessor;
        scanner = new Scanner(document, text, true);
	}

	private Token nextToken() {
		Token token, prevToken = token = scanner.scanToken();
		List comments = null;
		while (token.kind == TK_COMMENT | token.kind == TK_EOL) {
			if (comments == null)
				comments = new ArrayList();
			if (token.kind == TK_COMMENT)
				comments.add(token);
			scanner.tokenizeLbr = true;
			token = scanner.scanToken();
			if (token.kind == TK_EOL && prevToken.kind == TK_EOL) {
				consume_comments((Token[]) comments.toArray(new Token[comments.size()]));
				comments.clear();
			}
			prevToken = token;
		}
		scanner.tokenizeLbr = false;
		if (comments != null)
			consume_comments((Token[]) comments.toArray(new Token[comments.size()]));

		if (token.kind == TK_OPTION_LINE) {
			if (optionsProcessed)
				createProblem("No options definition allowed at this place.", token, Problem.ERROR);
			else
				consume_option_line(token);
			return nextToken();
		}

		if (!optionsProcessed) {
			optionsProcessed = true;
			if (parse_stack_top > 0) {
				Node optionsSection = popAst();
				expandNodeBy(peekAst(), optionsSection);
			}
		}

    	if (scanner.errorMessage != null)
			createProblem(scanner.errorMessage, token, Problem.ERROR);

		if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 2)
			System.out.println("next token: " + token.name + " (" + orderedTerminalSymbols[token.kind] + ")");
    	return token;
	}

	public Node parse() {

        stack = new int[80];
        tok_stack = new Token[40];
        parse_stack = new Node[20];
        model_stack = new ModelBase[20];

        elements = new HashMap();
		rulesLookup = new HashMap();

    /*****************************************************************/
    /* Start parsing.                                                */
    /*****************************************************************/
        state_stack_top = tok_stack_top = parse_stack_top = model_stack_top = -1;

        Node root = pushAst(new Node(null, 0, 0));

		Token token = nextToken();

        int kind = token.kind,
            act = START_STATE;

        ProcessTerminals: for (;;) {
            pushAct(act);

            act = t_action(act, kind);

            if (act <= NUM_RULES)
                state_stack_top--; // make reduction look like a shift-reduce
            else if (act == ERROR_ACTION) {
    			if (token.kind != TK_EOF) {
    				createProblem("Syntax error on " + token.name, token, Problem.ERROR);
					token = nextToken();
					kind = token.kind;
					state_stack_top = -1;
					act = START_STATE;
   					continue ProcessTerminals;
    			}
    			break ProcessTerminals;

            } else if (act > ERROR_ACTION) {
                token_action(token);
                token = nextToken();
                kind = token.kind;
                act -= ERROR_ACTION;

            } else if (act < ACCEPT_ACTION) {
            	token_action(token);
                token = nextToken();
                kind = token.kind;

                continue ProcessTerminals;
            } else
            	break ProcessTerminals;

            ProcessNonTerminals: do {
            	consume_rule(act);
                state_stack_top -= rhs[act] - 1;
                act = ntAction(stack[state_stack_top], lhs[act]);
            } while(act <= NUM_RULES);
        }

        ModelType[] allTypes = ModelType.getAllTypes();
        for (ModelType type : allTypes) {
        	if (type.getModelClass() != null)
        		document.addChildren(type, getElements(type), true, false);
        }

        root.length = scanner.currentTokenOffset;
        return root;
    }

	private List getElements(ModelType elementType) {
		List list = (List) elements.get(elementType);
		if (list == null)
			elements.put(elementType, list = new ArrayList());
		return list;
	}

	protected Node mapElementToNode(Node node, ModelBase element, boolean setToElement) {
		document.register(node, element);
		if (setToElement && element != null)
			element.node = node;
		return node;
	}

	private Section createSectionNode(ModelType childType) {
		Token tok = popToken();
		pushAst(createNodeFromToken(peekAst(), tok));
		return createSectionNode(childType, tok, true);
	}

	private Section createSectionNode(ModelType childType, Token token, boolean visible) {
		Section section = document.getSection(childType); // section may have been created in advance
		if (section == null) {
			document.setSection(childType, section = new Section(childType, document));
			section.visible = visible;
		} else if (section.visible != visible) {
			section.visible = visible;
			section.node = null;
		}
		if (section.node == null) {
			Node labelNode = createNodeFromToken(peekAst(), token);
			mapElementToNode(labelNode, section, true);
		}
		return section;
	}

	private Node createNodeFromToken(Node parent, Token token) {
		return new Node(parent, token.offset, token.length);
	}

	private Node createNodeFromTokens(Node parent, Token token1, Token token2) {
		return new Node(parent, token1.offset, token2.offset + token2.length - token1.offset);
	}

	private void expandNodeBy(Node node, Node child) {
		node.length = child.offset + child.length - node.offset;
	}

	private void consume_comments(Token[] tokens) {
		if (tokens.length < 2)
			return;
		Node multi = createNodeFromToken(peekAst(), tokens[0]);
		for (Token token : tokens) {
			expandNodeBy(multi, createNodeFromToken(multi, token));
		}
		ModelBase parent = document.getElementForNode(peekAst());
		mapElementToNode(multi, new GenericModel(parent, "#", ModelType.COMMENT), true);
	}
	private void consume_option_line(Token optionLineToken) {
		Section section = document.getSection(ModelType.OPTION);
		if (section == null) {
			pushToken(optionLineToken);
			section = createSectionNode(ModelType.OPTION);
			int index = optionLineToken.name.indexOf(' ');
			section.node.length = index != -1 ? index : optionLineToken.name.length();
			mapElementToNode(peekAst(), section, false);
		}
		QuoteDetector detector = new QuoteDetector(new char[] { '\'', '\'', '"', '"', '(', ')' });
		Token key = new Token();
		Token value = new Token();
		Map compatMap = new HashMap();
		DocumentOptions documentOptions = document.getOptions();
		String line = optionLineToken.name;
		int start = line.indexOf(DocumentOptions.DEFAULT_ESCAPE) + 8;
		while (start < line.length() && Character.isWhitespace(line.charAt(start)))
			start++;
		boolean elementStarted = false;
		for (int i = start, n = line.length(); i < n; i++) {
			char c = line.charAt(i);
			if (detector.detect(c))
				continue;
			if (c == ',' || c == ' ' || i + 1 >= n) {
				if (c == ' ' && !elementStarted) {
					start++;
					continue;
				}
				Token token = key.name == null ? key : value;
				token.name = line.substring(start, c == ',' ? i : i + 1).trim();
				token.offset = optionLineToken.offset + start;
				token.length = token.name.length();

				if (token.name.trim().length() > 0) {
					Option option = new Option(section, key.name, value.name);
					getElements(ModelType.OPTION).add(option);
					mapElementToNode(createNodeFromToken(peekAst(), key), option, true);
					expandNodeBy(peekAst(), mapElementToNode(value.name != null ? createNodeFromTokens(peekAst(), key, value) : createNodeFromToken(peekAst(), key), option, false));

					processOption(option, key, value, documentOptions, compatMap);
				}
				start = i + 1;
				key.name = value.name = null;
				elementStarted = false;
			} else if (c == '=') {
				key.name = line.substring(start, i).trim();
				key.offset = optionLineToken.offset + start;
				key.length = key.name.length();
				start = i + 1;
				elementStarted = false;
			} else
				elementStarted = true;
		}
		documentOptions.addBlockPair((String) compatMap.get("hblockb"), (String) compatMap.get("hblocke"));
		documentOptions.addBlockPair((String) compatMap.get("blockb"), (String) compatMap.get("blocke"));
	}
	private void processOption(Option option, Token keyToken, Token valueToken, DocumentOptions documentOptions, Map compatMap) {
		String key = keyToken.name.toLowerCase();
		String value = valueToken.name;
		if (key.startsWith("es")) {
			if (value != null && value.length() > 0) {
				documentOptions.setEscape(StringUtils.trimQuotes(value).charAt(0));
			}
		} else if ("om".equals(key) || key.startsWith("o")) {
			if (key.startsWith("ou")|| "os".equals(key)) // compatibility
				return;
			if (value != null && value.length() > 0) {
				documentOptions.setOrMarker(StringUtils.trimQuotes(value).charAt(0));
			}
			// compatibility options
		} else if ("hblockb".equals(key) || "hblocke".equals(key) || "blockb".equals(key) || "blocke".equals(key)) {
			if (value != null && value.length() > 0) {
				compatMap.put(key, value);
			}
		} else if ("action".equals(key)) {
			if (value != null) {
				String[] actionCommand = StringUtils.split(value.substring(1, value.length() - 1), ",");
				if (actionCommand.length > 2)
					documentOptions.addBlockPair(StringUtils.trimQuotes(actionCommand[1].trim()), StringUtils.trimQuotes(actionCommand[2].trim()));
			}
		} else if ("pl".equals(key) || key.startsWith("pro")) {
			if (value != null) {
				if (documentOptions.getEsape() == DocumentOptions.DEFAULT_ESCAPE &&
						("java".equals(value) || "c".equals(value) || "c++".equals(value)
								|| "cpp".equals(value))) {
					documentOptions.setEscape('$');
					documentOptions.setLanguage(value);
				}
			}
		} else if ("id".equals(key) || key.startsWith("in")) {
			if (value != null) {
				documentOptions.setIncludeDirs(StringUtils.split(value, ";"));
			}
		} else if (key.startsWith("te")) {
			if (value != null) {
				String[] names = StringUtils.split(value, ";");
				for (int i = 0, offset = valueToken.offset; i < names.length; offset += names[i].length(), i++) {
					Document template = processFileOptionValue(option, offset, names[i]);
					if (template == null)
						return;
					documentOptions.resetTo(template.getOptions());
					Definition[] makros = template.getMakros();
					for (int j = 0; j < makros.length; j++) {
						makros[j].visible = false;
					}
					document.addChildren(ModelType.DEFINITION, Arrays.asList(makros), true, true);
				}
			}
		} else if ("it".equals(key) || key.startsWith("impo")) {
			if (value != null) {
				String[] names = StringUtils.split(value, ";");
				for (int i = 0, offset = valueToken.offset; i < names.length; offset += names[i].length() + 1, i++) {
					Document importedTerminals = processFileOptionValue(option, offset, names[i]);
					if (importedTerminals == null)
						return;
					GenericModel[] terminals = importedTerminals.getExports();
					for (int j = 0; j < terminals.length; j++) {
						terminals[j].visible = false;
					}
					document.addChildren(ModelType.TERMINAL, Arrays.asList(terminals), true, true);
				}
			}
		} else if (key.startsWith("filt") && value != null) {
			processFileOptionValue(option, valueToken.offset, value);
		}

		try {
			if (ModelUtils.findOptionProposal(key) == null)
				createProblem(key + " is not a know option.", keyToken, Problem.WARNING);
		} catch (OptionAmbigousException e) {
			OptionProposal[] ambigous = e.getAmbigous();
			StringBuffer sb = new StringBuffer("The option ").append(key).append(" is ambigous between ");
			for (int i = 0; i < ambigous.length; i++) {
				if (i == ambigous.length - 1) sb.append(" and "); else if (i > 0) sb.append(", ");
				sb.append(ambigous[i].getKey());
			}
			createProblem(sb.toString(), keyToken, Problem.WARNING);
		}
	}
	private Document processFileOptionValue(Option option, int offset, String name) {
		Node refNode = new Node(option.node, offset, name.length());
		Reference fileRef = new Reference(option, name);
		option.addFileReference(fileRef, refNode);
		mapElementToNode(refNode, fileRef, true);
		try {
			return fileProzessor.process(document, name);
		} catch (Exception e) {
			createProblem(e.getMessage(), offset, name.length(), Problem.ERROR);
			return null;
		}
	}
	private void consume_enter_segment(ModelType childType) {
		pushModel(createSectionNode(childType));
	}
	private void consume_macro_definition() {
		Token defToken = popToken();
		Token nameToken = popToken();
		Node node = createNodeFromTokens(peekAst(), nameToken, defToken);
		Node nameNode = createNodeFromToken(node, nameToken);
		Node defNode = createNodeFromToken(node, defToken);
		String name = nameToken.name.charAt(0) == document.getOptions().getEsape() ? nameToken.name.substring(1) : nameToken.name;
		Definition definition = new Definition(document.getSection(ModelType.DEFINITION), name, defToken.name);
		mapElementToNode(node, definition, true);
		mapElementToNode(nameNode, definition, true);
		mapElementToNode(defNode, definition, false);
		getElements(ModelType.DEFINITION).add(definition);
		expandNodeBy(peekAst(), node);
	}
	private void consume_terminal(ModelType sectionType) {
		consume_element(sectionType, ModelType.TERMINAL);
	}
	private void consume_element(ModelType sectionType, ModelType elementType) {
		Token elementToken = popToken();
		GenericModel element = new GenericModel(document.getSection(sectionType),
				elementToken.name, elementType);
		getElements(sectionType).add(element);
		expandNodeBy(peekAst(), mapElementToNode(createNodeFromToken(peekAst(), elementToken), element, true));
	}
	private void consume_terminal_produces_name(ModelType sectionType) {
		Token termToken = tok_stack[tok_stack_top - 2];
		create_alias_definition(document.getSection(sectionType), popToken(), termToken, false, true);
		popToken(); // produces
		int length = peekAst().length;
		consume_terminal(sectionType);
		peekAst().length = length; // take the node expansion back
	}
	private void consume_alias_definition() {
		Token rhs = popToken();
		popToken();
		Token lhs = popToken();
		create_alias_definition(document.getSection(ModelType.ALIAS), lhs, rhs, true, false);
	}
	private void create_alias_definition(ModelBase parent, Token aliasToken, Token termToken, boolean visible, boolean swapNodes) {
		Node node = createNodeFromTokens(peekAst(), swapNodes ? termToken : aliasToken, swapNodes ? aliasToken : termToken);
		Node termNode = createNodeFromToken(node, termToken);
		Node aliasNode = createNodeFromToken(node, aliasToken);
		Reference reference = new Reference(null, termToken.name);
		Alias alias = new Alias(parent, aliasToken.name, reference);
		alias.visible = visible;
		mapElementToNode(node, alias, false);
		mapElementToNode(termNode, reference, true);
		mapElementToNode(aliasNode, alias, true);
		getElements(ModelType.ALIAS).add(alias);
		expandNodeBy(peekAst(), node);
	}
	private void consume_start_symbol() {
		Token symToken = popToken();
		Reference startToken = new Reference(peekModel(), symToken.name);
		getElements(ModelType.START_TOK).add(startToken);
		expandNodeBy(peekAst(), mapElementToNode(createNodeFromToken(peekAst(), symToken), startToken, true));
	}
	private void consume_lhs_produces_rhs() {
		Token currentRuleToken = tok_stack[tok_stack_top - ruleSymbolNumber - 1];
		String stripped = stripEscape(currentRuleToken.name);
		currentRule = (Rule) rulesLookup.get(currentRuleToken.name);
		boolean firstRule = currentRule == null;
		if (firstRule) {
			rulesLookup.put(currentRuleToken.name, currentRule = new Rule(peekModel(), currentRuleToken.name));
			if (stripped != null) {
				Rule rule = new Rule(peekModel(), stripped);
				rulesLookup.put(stripped, rule);
				rule.visible = false;
				Node ruleLabelNode = createNodeFromToken(peekAst(), currentRuleToken);
				mapElementToNode(ruleLabelNode, rule, true);
			}
			pushAst(createNodeFromToken(peekAst(), currentRuleToken));
		} else {
			pushAst(currentRule.node.parent);
		}
		Node ruleLabelNode = createNodeFromToken(peekAst(), currentRuleToken);
		mapElementToNode(ruleLabelNode, currentRule, firstRule);

		addRhs();
		popToken(); // produces
		popToken(); // rule symbol
		addRule();
	}
	private void addRhs() {
		for (int i = ruleSymbolNumber - 1; i >= 0; i--) {
			Token symToken = tok_stack[tok_stack_top - i];
			if (currentRuleRhs == null) {
				currentRule.rhs.add(currentRuleRhs = new Rhs(currentRule, symToken.name));
				Node rhsNode = createNodeFromToken(currentRule.node.parent, symToken);
				expandNodeBy(currentRule.node.parent, rhsNode);
				pushAst(rhsNode);
			}
			Reference reference = new Reference(currentRuleRhs, symToken.name);
			currentRuleRhs.addPart(reference);
			Node rhsNode = peekAst();
			Node refNode = createNodeFromToken(rhsNode, symToken);
			expandNodeBy(rhsNode, refNode);
			mapElementToNode(refNode, reference, true);
			String stripped = stripEscape(symToken.name);
			if (stripped != null) {
				Reference strippedRef = new Reference(currentRuleRhs, stripped);
				currentRuleRhs.addPart(strippedRef);
				strippedRef.visible = false;
				mapElementToNode(createNodeFromToken(rhsNode, symToken), strippedRef, true);
			}
		}
		while (ruleSymbolNumber > 0) {
			ruleSymbolNumber--;
			popToken();
		}
	}
	private void addRule() {
		if (currentRule == null)
			return;
		Node rhsNode = null;
		if (currentRuleRhs != null) {
			rhsNode = popAst();
			mapElementToNode(rhsNode, currentRuleRhs, true);
			currentRuleRhs = null;
		}
		Node ruleNode = popAst();
		if (rhsNode != null)
			expandNodeBy(ruleNode, rhsNode);
		mapElementToNode(ruleNode, currentRule, false);
		if (currentRule.getUserData(null) == null) {
			if (currentRule.parent instanceof Section)
				getElements(((Section) currentRule.parent).getChildType()).add(currentRule);
			currentRule.setUserData(null, new Object());
		}
		expandNodeBy(peekAst(), ruleNode);
	}
	private void consume_or_rhs() {
		addRhs();
		popToken(); // '|'
		if (currentRuleRhs != null) {
			if (currentRuleRhs.parts.size() > 0) {
				Node rhsNode = popAst();
				expandNodeBy(currentRule.node.parent, rhsNode);
				mapElementToNode(rhsNode, currentRuleRhs, true);
			}
			currentRuleRhs = null;
		}
	}
	private void consume_rhs_symbol() {
		ruleSymbolNumber++;
	}
	private void consume_action_block() {
		Token blockToken = popToken();
		GenericModel block = new GenericModel(peekModel(), blockToken.name, ModelType.MACRO_BLOCK);
		expandNodeBy(peekAst(), mapElementToNode(createNodeFromToken(peekAst(), blockToken), block, true));
		ModelType parentType = block.parent instanceof Section ? ((Section) block.parent).getChildType() : block.parent.getType();
		getElements(parentType).add(block);
		if (blockToken.name.indexOf('\r') != -1 || blockToken.name.indexOf('\n') != -1)
			new Node(block.node, 0, 0); // just to give it a child
	}
	private void consume_names_definition() {
		Token nameToken = popToken();
		popToken(); // produces
		Token symToken = popToken();
		Node node = createNodeFromTokens(peekAst(), symToken, nameToken);
		Node nameNode = createNodeFromToken(node, nameToken);
		Node symNode = createNodeFromToken(node, symToken);
		GenericModel name = new GenericModel(document.getSection(ModelType.NAME), symToken.name, ModelType.NAME);
		Reference reference = new Reference(name, nameToken.name);
		mapElementToNode(node, name, true);
		mapElementToNode(nameNode, reference, true);
		mapElementToNode(symNode, name, true);
		getElements(ModelType.NAME).add(name);
		expandNodeBy(peekAst(), node);
	}
	private void consume_end_key(boolean releaseToken) {
		Node sectionNode = popAst();
		if (releaseToken)
			expandNodeBy(sectionNode, createNodeFromToken(sectionNode, popToken()));
		expandNodeBy(peekAst(), sectionNode);
		mapElementToNode(sectionNode, popModel(), false);
	}
	private void consume_include_segment() {
		Token symToken = tok_stack[tok_stack_top];
		consume_element(ModelType.INCLUDE, ModelType.INCLUDE);
		try {
			fileProzessor.process(document, symToken.name);
		} catch (Exception e) {
			createProblem(e.getMessage(), symToken, Problem.ERROR);
		}
	}
	private String stripEscape(String name) {
		int escapeIndex = name.indexOf(document.getOptions().getEsape());
		if (escapeIndex == -1 || "empty".equals(name.substring(1).toLowerCase()) ||
				name.charAt(0) == '\'' && name.charAt(name.length() - 1) == '\'')
			return null;
		return name.substring(0, escapeIndex);
	}
	protected void report_problem(String messageKey) {
		report_problem(messageKey, Problem.ERROR);
	}
	protected void report_problem(String messageKey, int problemType) {
		Token token = tok_stack[tok_stack_top];
		createProblem(SyntaxMessages.getError(messageKey, token.name), token, problemType);
    }

	protected void consume_rule(int action) {
		switch (action) {
     	case 25 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("bad_symbol ::= EQUIVALENCE");
		    report_problem("bad_first_symbol");
			break;

     	case 26 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("bad_symbol ::= ARROW");
		    report_problem("bad_first_symbol");
			break;

     	case 27 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("bad_symbol ::= OR");
		    report_problem("bad_first_symbol");
			break;

     	case 28 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("bad_symbol ::= EMPTY_KEY");
		    report_problem("bad_first_symbol");
			break;

     	case 29 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("bad_symbol ::= ERROR_KEY");
		    report_problem("bad_first_symbol");
			break;

     	case 30 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("bad_symbol ::= MACRO_NAME");
		    report_problem("bad_first_symbol");
			break;

     	case 31 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("bad_symbol ::= SYMBOL");
		    report_problem("bad_first_symbol");
			break;

     	case 32 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("bad_symbol ::= BLOCK");
		    report_problem("bad_first_block");
			break;

     	case 34 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("include_segment ::= enter_include_segment SYMBOL");
		    consume_include_segment();
			break;

     	case 35 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("include_segment ::= enter_include_segment SYMBOL SYMBOL");
		    report_problem("only_one_include_allowed", Problem.ERROR);
			break;

     	case 36 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("enter_include_segment ::= INCLUDE_KEY");
		    consume_enter_segment(ModelType.INCLUDE);
			break;

     	case 37 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("notice_segment ::= NOTICE_KEY");
		    consume_enter_segment(ModelType.NOTICE);
			break;

     	case 39 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("define_segment ::= DEFINE_KEY");
		    consume_enter_segment(ModelType.DEFINITION);
			break;

    	case 40 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("define_segment ::= define_segment macro_name_symbol macro_segment");
		    consume_macro_definition();
			break;

     	case 42 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("macro_name_symbol ::= SYMBOL");
		    report_problem("macro_without_escape", Problem.WARNING);
			break;

     	case 44 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("terminals_segment ::= TERMINALS_KEY");
		    consume_enter_segment(ModelType.TERMINAL);
			break;

     	case 45 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("terminals_segment ::= terminals_segment terminal_symbol");
		    consume_terminal(ModelType.TERMINAL);
			break;

     	case 46 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("terminals_segment ::= terminals_segment terminal_symbol produces name");
		    consume_terminal_produces_name(ModelType.TERMINAL);
			break;

     	case 47 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("export_segment ::= EXPORT_KEY");
		    consume_enter_segment(ModelType.EXPORT);
			break;

     	case 48 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("export_segment ::= export_segment terminal_symbol");
		    consume_terminal(ModelType.EXPORT);
			break;

     	case 50 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("import_segment ::= enter_import_segment SYMBOL {drop_command}");
		    popToken();
			break;

     	case 51 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("enter_import_segment ::= IMPORT_KEY");
		    consume_enter_segment(ModelType.IMPORT);
			break;

     	case 54 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("drop_command ::= DROPACTIONS_KEY");
		    popToken();
			break;

     	case 55 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("drop_symbols ::= DROPSYMBOLS_KEY");
		    popToken();
			break;

     	case 56 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("drop_symbols ::= drop_symbols SYMBOL");
		    consume_terminal(ModelType.IMPORT);
			break;

     	case 57 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("drop_rules ::= DROPRULES_KEY");
		    popToken();
			break;

    	case 59 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("drop_rule ::= drop_rule_symbol produces rhs");
		    consume_lhs_produces_rhs();
			break;

     	case 62 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("drop_rule ::= drop_rule | rhs");
		    consume_or_rhs();
			break;

     	case 65 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("keywords_segment ::= KEYWORDS_KEY");
		    consume_enter_segment(ModelType.KEYWORD);
			break;

     	case 66 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("keywords_segment ::= keywords_segment terminal_symbol");
		    consume_terminal(ModelType.KEYWORD);
			break;

     	case 67 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("keywords_segment ::= keywords_segment terminal_symbol produces name");
		    consume_terminal_produces_name(ModelType.KEYWORD);
			break;

     	case 69 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("error_segment ::= enter_error_segment terminal_symbol");
		    consume_terminal(ModelType.ERROR_TOK);
			break;

     	case 70 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("enter_error_segment ::= ERROR_KEY");
		    consume_enter_segment(ModelType.ERROR_TOK);
			break;

     	case 71 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("recover_segment ::= RECOVER_KEY");
		    consume_enter_segment(ModelType.RECOVER);
			break;

     	case 73 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("recover_segment ::= recover_segment recover_terminal BLOCK");
		    consume_action_block();
			break;

     	case 74 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("recover_terminal ::= terminal_symbol");
		    consume_terminal(ModelType.RECOVER);
			break;

     	case 76 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("identifier_segment ::= enter_identifier_segment terminal_symbol");
		    consume_terminal(ModelType.IDENTIFIER);
			break;

     	case 77 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("enter_identifier_segment ::= IDENTIFIER_KEY");
		    consume_enter_segment(ModelType.IDENTIFIER);
			break;

     	case 79 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("eol_segment ::= enter_eol_segment terminal_symbol");
		    consume_terminal(ModelType.EOL_TOK);
			break;

     	case 80 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("enter_eol_segment ::= EOL_KEY");
		    consume_enter_segment(ModelType.EOL_TOK);
			break;

     	case 82 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("eof_segment ::= enter_eof_segment terminal_symbol");
		    consume_terminal(ModelType.EOF_TOK);
			break;

     	case 83 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("enter_eof_segment ::= EOF_KEY");
		    consume_enter_segment(ModelType.EOF_TOK);
			break;

     	case 85 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("terminal_symbol ::= MACRO_NAME");
		    report_problem("bad_symbol_prefix", Problem.WARNING);
			break;

     	case 86 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("alias_segment ::= ALIAS_KEY");
		    consume_enter_segment(ModelType.ALIAS);
			break;

     	case 87 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("alias_segment ::= alias_segment alias_lhs produces alias_rhs");
		    consume_alias_definition();
			break;

     	case 93 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("alias_lhs ::= MACRO_NAME");
		    report_problem("bad_symbol_prefix", Problem.WARNING);
			break;

     	case 95 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("alias_rhs ::= MACRO_NAME");
		    report_problem("bad_symbol_prefix", Problem.WARNING);
			break;

     	case 102 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("enter_start_segment ::= START_KEY");
		    consume_enter_segment(ModelType.START_TOK);
			break;

     	case 103 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("headers_segment ::= HEADERS_KEY");
		    consume_enter_segment(ModelType.HEADER);
			break;

     	case 107 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("enter_ast_segment ::= AST_KEY");
		    consume_enter_segment(ModelType.AST);
			break;

     	case 108 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("globals_segment ::= GLOBALS_KEY");
		    consume_enter_segment(ModelType.GLOBAL);
			break;

     	case 110 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("trailers_segment ::= TRAILERS_KEY");
		    consume_enter_segment(ModelType.TRAILER);
			break;

     	case 112 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("start_symbol ::= SYMBOL");
		    consume_start_symbol();
			break;

     	case 113 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("start_symbol ::= MACRO_NAME");
		    consume_start_symbol();
			break;

     	case 116 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("enter_rules_segment ::= RULES_KEY");
		    consume_enter_segment(ModelType.RULE);
			break;

    	case 117 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("rules ::= rule_symbol produces rhs");
		    consume_lhs_produces_rhs();
			break;

     	case 118 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("rules ::= rules | rhs");
		    consume_or_rhs();
			break;

     	case 130 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("rhs ::= rhs rhs_symbol");
		    consume_rhs_symbol();
			break;

     	case 132 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("action_segment ::= BLOCK");
		    consume_action_block();
			break;

     	case 133 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("types_segment ::= TYPES_KEY");
		    consume_enter_segment(ModelType.TYPE);
			break;

     	case 135 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("type_declarations ::= SYMBOL produces type_rhs");
		    consume_lhs_produces_rhs();
			break;

     	case 136 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("type_declarations ::= type_declarations | type_rhs");
		    consume_or_rhs();
			break;

     	case 137 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("type_rhs ::= SYMBOL");
		    consume_rhs_symbol();
			break;

     	case 138 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("names_segment ::= NAMES_KEY");
		    consume_enter_segment(ModelType.NAME);
			break;

     	case 139 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("names_segment ::= names_segment name produces name");
		    consume_names_definition();
			break;

     	case 141 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("name ::= MACRO_NAME");
		    report_problem("bad_symbol_prefix", Problem.WARNING);
			break;

     	case 146 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("[END_KEY] ::= $Empty");
		    consume_end_key(false);
			break;

     	case 147 :  //if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 1) System.out.println("[END_KEY] ::= END_KEY");
		    consume_end_key(true);
			break;
		}
	}

	private void createProblem(String message, int offset, int length, int problemType) {
		Problem problem = new Problem(problemType, message, offset, length);
		if (problemRequestor != null)
			problemRequestor.accept(problem);
		document.addProblem(problem);
	}

	private void createProblem(String message, Token token, int problemType) {
		createProblem(message, token.offset, token.length, problemType);
	}

	@Override
	public String toString() {
		final String leftMarker = "--->"; //$NON-NLS-1$
		final String rightMarker = "<---"; //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		sb.append("TOKEN_STACK:"); //$NON-NLS-1$
		if (tok_stack_top == -1) {
			sb.append(leftMarker);
			sb.append(rightMarker);
		}
		for (int i = 0, n = Math.min(tok_stack_top + 5, tok_stack.length); i < n; i++) {
			if (i > 0) sb.append(", "); //$NON-NLS-1$
			if (i == tok_stack_top) sb.append(leftMarker);
			if (tok_stack[i] != null) sb.append(tok_stack[i]);
			if (i == tok_stack_top) sb.append(rightMarker);
		}
		sb.append("\nPARSE_STACK:"); //$NON-NLS-1$
		if (parse_stack_top == -1) {
			sb.append(leftMarker);
			sb.append(rightMarker);
		}
		for (int i = 0, n = Math.min(parse_stack_top + 5, parse_stack.length); i < n; i++) {
			if (i > 0) sb.append(", "); //$NON-NLS-1$
			if (i == parse_stack_top) sb.append(leftMarker);
			if (parse_stack[i] != null) sb.append(parse_stack[i].offset).append(',').append(parse_stack[i].length);
			if (i == parse_stack_top) sb.append(rightMarker);
		}
		sb.append("\nSCAN_POS:"); //$NON-NLS-1$
		sb.append(scanner);
		if (state_stack_top >= 0) {
			sb.append("\nEXPECTINGS:"); //$NON-NLS-1$
			int start, end = start = asi(stack[state_stack_top]);
			while (asr[end] != 0)
				end++;
			int length = end - start;
			if (length != 0) {
				byte[] indexes = new byte[length];
				System.arraycopy(asr, start, indexes, 0, length);
				for (int i = 0; i < length; i++) {
					if (i > 0) sb.append(", "); //$NON-NLS-1$
					sb.append(name[terminalIndex(indexes[i])]);
				}
			}
		}
		return sb.toString();
	}
}
