/*
 * (c) Copyright 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Parser extends jpgdcl implements jpgsym {
	private int state_stack_top,
        stack[],
    	tok_stack_top,
    	parse_stack_top;
	private Token[] tok_stack;
	private Node[] parse_stack;

	private Scanner scanner;
	private Document document;
	private IProblemRequestor problemRequestor;
	private List definitions, terminals, aliases, startTokens, rules, names;
	private Rule currentRule;
	private Rhs currentRuleRhs;
	private Token currentRuleToken;
	private Map rulesLookup;

	private void pushAct(int act) {
		if (++state_stack_top >= stack.length) {
			int[] tmp = new int[stack.length + 50];
			System.arraycopy(stack, 0, tmp, 0, stack.length);
			stack = tmp;
		}
		stack[state_stack_top] = act;
	}

	private Node pushAst(Node node) {
		if (++parse_stack_top >= parse_stack.length) {
			Node[] tmp = new Node[parse_stack.length + 15];
			System.arraycopy(parse_stack, 0, tmp, 0, parse_stack.length);
			parse_stack = tmp;
		}
		return parse_stack[parse_stack_top] = node;
	}

	private Node popAst() {
		return parse_stack[parse_stack_top--];
	}

	private Node peekAst() {
		return parse_stack[parse_stack_top];
	}

	private void pushToken(Token token) {
		if (++tok_stack_top >= tok_stack.length) {
			Token[] tmp = new Token[tok_stack.length + 32];
			System.arraycopy(tok_stack, 0, tmp, 0, tok_stack.length);
			tok_stack = tmp;
		}
		tok_stack[tok_stack_top] = token;
	}

    private Token popToken() {
		return tok_stack[tok_stack_top--];
	}
    
    private void token_action(Token token) {
    	pushToken(token);
    }

	public Parser(Document document, String text, IProblemRequestor problemRequestor) {
        this.document = document;
        this.problemRequestor = problemRequestor;
        scanner = new Scanner(document, text, null);
	}

	public Parser(Document document, Reader in) throws IOException {
        this.document = document;
        scanner = new Scanner(document, in, null);
    }
 
	public Node parse() {
    	Token token = scanner.scanToken();

        int kind = token.kind,
            act = START_STATE;
        
        stack = new int[80];
        tok_stack = new Token[40];
        parse_stack = new Node[20];

		definitions = new ArrayList();
		terminals = new ArrayList();
		aliases = new ArrayList();
		startTokens = new ArrayList(1);
		rules = new ArrayList();
		names = new ArrayList();
		rulesLookup = new HashMap();

		
    /*****************************************************************/
    /* Start parsing.                                                */
    /*****************************************************************/
        state_stack_top = tok_stack_top = parse_stack_top -1;
        
        Node root = pushAst(new Node(null, 0, 0));

        ProcessTerminals: for (;;) {
            pushAct(act);
            
            act = t_action(act, kind);
 
            if (act <= NUM_RULES)
                state_stack_top--; // make reduction look like a shift-reduce
            else if (act == ERROR_ACTION) {
            	if (scanner.errorMessage != null) {
	    			createProblem(scanner.errorMessage, token);
            	}
    			if (token.kind != TK_EOF) {
					token = scanner.scanToken();
					state_stack_top = -1;
					act = START_STATE;
   					continue ProcessTerminals;
    			}
    			break ProcessTerminals;
    			
            } else if (act > ERROR_ACTION) {
                token_action(token);
                token = scanner.scanToken();
                kind = token.kind;
                act -= ERROR_ACTION;

            } else if (act < ACCEPT_ACTION) {
            	token_action(token);
                token = scanner.scanToken();
                kind = token.kind;

                continue ProcessTerminals;
            } else
            	break ProcessTerminals;

            ProcessNonTerminals: do {
            	consume_rule(act);
                state_stack_top -= (rhs[act] - 1);
                act = nt_action(stack[state_stack_top], lhs[act]);
            } while(act <= NUM_RULES);
        }

        setChildren(ModelType.DEFINITION, definitions, Definition.class);
        setChildren(ModelType.ALIAS, aliases, Alias.class);
        setChildren(ModelType.TERMINAL, terminals, Terminal.class);
        setChildren(ModelType.RULE, rules, Rule.class);
        setChildren(ModelType.NAME, names, Name.class);
        document.startTokens = startTokens;
        
        root.length = scanner.currentTokenOffset;
        return root;
    }
	
	private void setChildren(ModelType type, List children, Class elementType) {
        Section section = document.getSection(type);
        if (section != null) {
        	Object array = Array.newInstance(elementType, children.size());
        	for (int i = 0; i < children.size(); i++) {
				Array.set(array, i, children.get(i));
			}
        	section.setChildren((ModelBase[]) array);
        }
	}
    
	protected void mapElementToNode(Node node, ModelBase element, boolean setToElement) {
		document.register(node, element);
		if (setToElement && element != null)
			element.node = node;
	}
	
	private Node createNodeFromToken(Node parent, Token token) {
		return new Node(parent, token.offset, token.length);
	}

	private Node createNodeFromTokens(Node parent, Token token1, Token token2) {
		return new Node(parent, token1.offset, token2.offset + token2.length - token1.offset);
	}
	
	private Node expandNodeBy(Node node, Node child) {
		node.length = child.offset + child.length - node.offset;
		return node;
	}

	private void consume_enter_define_block() {
		Token tok = popToken();
		pushAst(createNodeFromToken(peekAst(), tok));
//TODO move to model content provider, modelbase should not be a workbench adapter
		Section section = new Section(ModelType.DEFINITION, document, tok.name, "icons/definitions.gif");
		document.setSection(ModelType.DEFINITION, section);
		Node labelNode = createNodeFromToken(peekAst(), tok);
		mapElementToNode(labelNode, section, true);
	}
	private void consume_macro_list() {
		consume_add_macro_definition();
	}
	private void consume_add_macro_definition() {
		Token defToken = popToken();
		Token nameToken = popToken();
		Node node = createNodeFromTokens(peekAst(), nameToken, defToken);
		Node nameNode = createNodeFromToken(node, nameToken); 
		Node defNode = createNodeFromToken(node, defToken); 
		Definition definition = new Definition(document.getSection(ModelType.DEFINITION), nameToken.name, defToken.name);
		mapElementToNode(node, definition, true);
		mapElementToNode(nameNode, definition, true);
		mapElementToNode(defNode, definition, false);
		definitions.add(definition);
		expandNodeBy(peekAst(), node);
	}
	private void consume_define_block() {
		mapElementToNode(popAst(), document.getSection(ModelType.DEFINITION), false);
	}
	private void consume_enter_terminals_block() {
		Token tok = popToken();
		pushAst(createNodeFromToken(peekAst(), tok));
//		TODO see above todo
		Section section = new Section(ModelType.TERMINAL, document, tok.name, "icons/terminals.gif");
		document.setSection(ModelType.TERMINAL, section);
		Node labelNode = createNodeFromToken(peekAst(), tok);
		mapElementToNode(labelNode, section, true);
	}
	private void consume_terminal() {
		Token termToken = popToken();
		Node node = createNodeFromToken(peekAst(), termToken);
		Terminal terminal = new Terminal(document.getSection(ModelType.TERMINAL), termToken.name);
		mapElementToNode(node, terminal, true);
		terminals.add(terminal);
		expandNodeBy(peekAst(), node);
	}
	private void consume_terminals_block() {
		mapElementToNode(popAst(), document.getSection(ModelType.TERMINAL), false);
	}
	private void consume_enter_alias_block() {
		Token tok = popToken();
		pushAst(createNodeFromToken(peekAst(), tok));
//		TODO see above todo
		Section section = new Section(ModelType.ALIAS, document, tok.name, "icons/aliases.gif");
		document.setSection(ModelType.ALIAS, section);
		Node labelNode = createNodeFromToken(peekAst(), tok);
		mapElementToNode(labelNode, section, true);
	}
	private void consume_alias_definition() {
		Token rhs = popToken();
		popToken();
		Token lhs = popToken();
		Node node = createNodeFromTokens(peekAst(), lhs, rhs);
		Node rhsNode = createNodeFromToken(node, rhs);
		Node lhsNode = createNodeFromToken(node, lhs);
		Reference reference = new Reference(null, rhs.name);
		Alias alias = new Alias(document.getSection(ModelType.ALIAS), lhs.name, reference);
		mapElementToNode(node, alias, true);
		mapElementToNode(rhsNode, reference, true);
		mapElementToNode(lhsNode, alias, true);
		aliases.add(alias);
		expandNodeBy(peekAst(), node);
	}
	private void consume_alias_block() {
		mapElementToNode(popAst(), document.getSection(ModelType.ALIAS), false);
	}
	private void consume_enter_start_block() {
		popToken();
	}
	private void consume_start_symbol() {
		Token symToken = popToken();
		startTokens.add(symToken.name);
	}
	private void consume_enter_rules_block() {
		Token tok = popToken();
		pushAst(createNodeFromToken(peekAst(), tok));
//		TODO see above todo
		Section section = new Section(ModelType.RULE, document, tok.name, "icons/productions.gif");
		document.setSection(ModelType.RULE, section);
		Node labelNode = createNodeFromToken(peekAst(), tok);
		mapElementToNode(labelNode, section, true);
	}
	private void consume_rule_lhs() {
		popToken(); // produces
		addRule();
		currentRuleToken = popToken();
		currentRule = (Rule) rulesLookup.get(currentRuleToken.name);
		if (currentRule == null)
			rulesLookup.put(currentRuleToken.name, currentRule = new Rule(document.getSection(ModelType.RULE), currentRuleToken.name));
		pushAst(createNodeFromToken(peekAst(), currentRuleToken));
		Node ruleLabelNode = createNodeFromToken(peekAst(), currentRuleToken);
		mapElementToNode(ruleLabelNode, currentRule, true);
	}
	private void addRule() {
		if (currentRule == null)
			return;
		if (currentRuleRhs != null) {
			Node rhsNode = popAst();
			mapElementToNode(rhsNode, currentRuleRhs, true);
		}
		Node ruleNode = popAst();
		mapElementToNode(ruleNode, currentRule, false);
		if (currentRule.getUserData(null) == null) {
			rules.add(currentRule);
			currentRule.setUserData(null, new Object());
		}
		currentRule = null;
		currentRuleRhs = null;
		expandNodeBy(peekAst(), ruleNode);
	}
	private void consume_rule_and() {
		popToken(); // '|'
		if (currentRuleRhs != null) {
			if (currentRuleRhs.parts.size() > 0) {
				Node rhsNode = popAst();
				Node ruleNode = peekAst();
				ruleNode.length = rhsNode.offset + rhsNode.length - ruleNode.offset;
				mapElementToNode(rhsNode, currentRuleRhs, true);
			}
			currentRuleRhs = null;
		}
	}
	private void consume_rule_symbol() {
		Token symToken = popToken();
		if (currentRule != null) {
			if (currentRuleRhs == null) {
				currentRule.rhs.add(currentRuleRhs = new Rhs(currentRule, symToken.name));
				Node ruleNode = peekAst();
				ruleNode.length = symToken.offset + symToken.length - ruleNode.offset;
				pushAst(createNodeFromToken(ruleNode, symToken));
			}
			Reference reference = new Reference(currentRuleRhs, symToken.name);
			currentRuleRhs.addPart(reference);
			Node rhsNode = peekAst();
			rhsNode.length = symToken.offset + symToken.length - rhsNode.offset;
			Node refNode = createNodeFromToken(rhsNode, symToken);
			mapElementToNode(refNode, reference, true);
		}
	}
	private void consume_rule_action_block() {
		// nothing to be done
	}
	private void consume_rules_block() {
		addRule();
		mapElementToNode(popAst(), document.getSection(ModelType.RULE), false);
	}
	private void consume_add_block_definition() {
		popToken();
	}
	private void consume_enter_names_block() {
		Token tok = popToken();
		pushAst(createNodeFromToken(peekAst(), tok));
//		TODO see above todo
		Section section = new Section(ModelType.NAME, document, tok.name, "icons/names.gif");
		document.setSection(ModelType.NAME, section);
		Node labelNode = createNodeFromToken(peekAst(), tok);
		mapElementToNode(labelNode, section, true);
	}
	private void consume_names_definition() {
		Token nameToken = popToken();
		popToken(); // produces
		Token symToken = popToken();
		Node node = createNodeFromTokens(peekAst(), symToken, nameToken);
		Node nameNode = createNodeFromToken(node, nameToken); 
		Node symNode = createNodeFromToken(node, symToken); 
		Reference reference = new Reference(null, nameToken.name);
		Name name = new Name(document.getSection(ModelType.NAME), symToken.name, reference);
		mapElementToNode(node, name, true);
		mapElementToNode(nameNode, reference, true);
		mapElementToNode(symNode, name, true);
		names.add(name);
		expandNodeBy(peekAst(), node);
	}
	private void consume_names_block() {
		mapElementToNode(popAst(), document.getSection(ModelType.NAME), false);
	}
	private void consume_end_key() {
		popToken();
	}
	protected void handle_error(String messageKey) {
		Token token = tok_stack[tok_stack_top];
		createProblem(SyntaxMessages.getError(messageKey, token.name), token);
    }

	protected void consume_rule(int action) {
		switch (action) {
     	case 3 :  // System.out.println("bad_symbol ::= EQUIVALENCE"); //$NON-NLS-1$
		    handle_error("bad_first_symbol");  
			break ;

     	case 4 :  // System.out.println("bad_symbol ::= ARROW"); //$NON-NLS-1$
		    handle_error("bad_first_symbol");  
			break ;

     	case 5 :  // System.out.println("bad_symbol ::= OR"); //$NON-NLS-1$
		    handle_error("bad_first_symbol");  
			break ;

     	case 6 :  // System.out.println("bad_symbol ::= EMPTY_SYMBOL"); //$NON-NLS-1$
		    handle_error("bad_first_symbol");  
			break ;

     	case 7 :  // System.out.println("bad_symbol ::= ERROR_SYMBOL"); //$NON-NLS-1$
		    handle_error("bad_first_symbol");  
			break ;

     	case 8 :  // System.out.println("bad_symbol ::= MACRO_NAME"); //$NON-NLS-1$
		    handle_error("bad_first_symbol");  
			break ;

     	case 9 :  // System.out.println("bad_symbol ::= SYMBOL"); //$NON-NLS-1$
		    handle_error("bad_first_symbol");  
			break ;

     	case 10 :  // System.out.println("bad_symbol ::= BLOCK"); //$NON-NLS-1$
		    handle_error("bad_first_block");  
			break ;

     	case 13 :  // System.out.println("enter_define ::="); //$NON-NLS-1$
		    consume_enter_define_block();  
			break ;
 
    	case 14 :  // System.out.println("macro_list ::= macro_name_symbol macro_block"); //$NON-NLS-1$
		    consume_macro_list();  
			break ;
 
     	case 15 :  // System.out.println("macro_list ::= macro_list macro_name_symbol macro_block"); //$NON-NLS-1$
		    consume_add_macro_definition();  
			break ;
 
     	case 17 :  // System.out.println("macro_name_symbol ::= SYMBOL"); //$NON-NLS-1$
		    handle_error("macro_without_escape");  
			break ;

     	case 18 :  // System.out.println("macro_name_symbol ::= OR"); //$NON-NLS-1$
		    handle_error("bad_macro_name");  
			break ;

     	case 19 :  // System.out.println("macro_name_symbol ::= EMPTY_SYMBOL"); //$NON-NLS-1$
		    handle_error("bad_macro_name");  
			break ;

     	case 20 :  // System.out.println("macro_name_symbol ::= ERROR_SYMBOL"); //$NON-NLS-1$
		    handle_error("bad_macro_name");  
			break ;

     	case 21 :  // System.out.println("macro_name_symbol ::= produces"); //$NON-NLS-1$
		    handle_error("bad_macro_name");  
			break ;

     	case 22 :  // System.out.println("macro_name_symbol ::= BLOCK"); //$NON-NLS-1$
		    handle_error("no_macro_name");  
			break ;

     	case 23 :  // System.out.println("macro_name_symbol ::= DEFINE_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_macro_keyword");  
			break ;

     	case 25 :  // System.out.println("macro_block ::= OR"); //$NON-NLS-1$
		    handle_error("definition_expected");  
			break ;

     	case 26 :  // System.out.println("macro_block ::= EMPTY_SYMBOL"); //$NON-NLS-1$
		    handle_error("definition_expected");  
			break ;

     	case 27 :  // System.out.println("macro_block ::= ERROR_SYMBOL"); //$NON-NLS-1$
		    handle_error("definition_expected");  
			break ;

     	case 28 :  // System.out.println("macro_block ::= produces"); //$NON-NLS-1$
		    handle_error("definition_expected");  
			break ;

     	case 29 :  // System.out.println("macro_block ::= SYMBOL"); //$NON-NLS-1$
		    handle_error("definition_expected");  
			break ;

     	case 30 :  // System.out.println("macro_block ::= keyword"); //$NON-NLS-1$
		    handle_error("definition_expected");  
			break ;

     	case 31 :  // System.out.println("macro_block ::= END_KEY"); //$NON-NLS-1$
		    handle_error("definition_expected");  
			break ;

     	case 33 :  // System.out.println("enter_terminals ::="); //$NON-NLS-1$
		    consume_enter_terminals_block();  
			break ;
 
     	case 34 :  // System.out.println("terminal_symbol ::= SYMBOL"); //$NON-NLS-1$
		    consume_terminal();  
			break ;
 
     	case 37 :  // System.out.println("terminal_symbol ::= DEFINE_KEY"); //$NON-NLS-1$
		    handle_error("bad_terminal");  
			break ;
 
     	case 38 :  // System.out.println("terminal_symbol ::= TERMINALS_KEY"); //$NON-NLS-1$
		    handle_error("bad_terminal");  
			break ;
 
     	case 39 :  // System.out.println("terminal_symbol ::= BLOCK"); //$NON-NLS-1$
		    handle_error("misplaced_block_terminal");  
			break ;

     	case 41 :  // System.out.println("enter_alias ::="); //$NON-NLS-1$
		    consume_enter_alias_block();  
			break ;
 
     	case 42 :  // System.out.println("alias_definition ::= alias_lhs produces alias_rhs"); //$NON-NLS-1$
		    consume_alias_definition();  
			break ;
 
     	case 57 :  // System.out.println("bad_alias_rhs ::= DEFINE_KEY"); //$NON-NLS-1$
		    handle_error("bad_alias_rhs");  
			break ;

     	case 58 :  // System.out.println("bad_alias_rhs ::= TERMINALS_KEY"); //$NON-NLS-1$
		    handle_error("bad_alias_rhs");  
			break ;

     	case 59 :  // System.out.println("bad_alias_rhs ::= ALIAS_KEY"); //$NON-NLS-1$
		    handle_error("bad_alias_rhs");  
			break ;

     	case 60 :  // System.out.println("bad_alias_rhs ::= BLOCK"); //$NON-NLS-1$
		    handle_error("misplaced_block_alias");  
			break ;

     	case 62 :  // System.out.println("bad_alias_lhs ::= EMPTY_SYMBOL"); //$NON-NLS-1$
		    handle_error("empty_symbol");  
			break ;

     	case 63 :  // System.out.println("bad_alias_lhs ::= produces"); //$NON-NLS-1$
		    handle_error("missing_quote");  
			break ;
 
     	case 64 :  // System.out.println("bad_alias_lhs ::= OR"); //$NON-NLS-1$
		    handle_error("missing_quote");  
			break ;
 
     	case 66 :  // System.out.println("enter_start ::="); //$NON-NLS-1$
		    consume_enter_start_block();  
			break ;
 
     	case 67 :  // System.out.println("start_symbol ::= SYMBOL"); //$NON-NLS-1$
		    consume_start_symbol();  
			break ;
  
     	case 68 :  // System.out.println("start_symbol ::= OR"); //$NON-NLS-1$
		    handle_error("bad_start_symbol");  
			break ;

     	case 69 :  // System.out.println("start_symbol ::= EMPTY_SYMBOL"); //$NON-NLS-1$
		    handle_error("bad_start_symbol");  
			break ;

     	case 70 :  // System.out.println("start_symbol ::= ERROR_SYMBOL"); //$NON-NLS-1$
		    handle_error("bad_start_symbol");  
			break ;

     	case 71 :  // System.out.println("start_symbol ::= produces"); //$NON-NLS-1$
		    handle_error("bad_start_symbol");  
			break ;

     	case 72 :  // System.out.println("start_symbol ::= BLOCK"); //$NON-NLS-1$
		    handle_error("misplaced_block_start");  
			break ;
 
     	case 73 :  // System.out.println("start_symbol ::= DEFINE_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_in_START") ;  
			break ;

     	case 74 :  // System.out.println("start_symbol ::= TERMINALS_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_in_START") ;  
			break ;

     	case 75 :  // System.out.println("start_symbol ::= ALIAS_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_in_START") ;  
			break ;

     	case 76 :  // System.out.println("start_symbol ::= START_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_in_START") ;  
			break ;

     	case 78 :  // System.out.println("rules_block ::= RULES_KEY enter_rules rule_list"); //$NON-NLS-1$
		    consume_rules_block();  
			break ;
 
     	case 79 :  // System.out.println("enter_rules ::="); //$NON-NLS-1$
		    consume_enter_rules_block();  
			break ;
 
    	case 82 :  // System.out.println("rule_list ::= {action_block} SYMBOL produces"); //$NON-NLS-1$
		    consume_rule_lhs();  
			break ;
 
     	case 83 :  // System.out.println("rule_list ::= rule_list OR"); //$NON-NLS-1$
		    consume_rule_and();  
			break ;
 
     	case 84 :  // System.out.println("rule_list ::= rule_list SYMBOL produces"); //$NON-NLS-1$
		    consume_rule_lhs();  
			break ;
 
     	case 85 :  // System.out.println("rule_list ::= rule_list EMPTY_SYMBOL"); //$NON-NLS-1$
		    consume_rule_symbol();  
			break ;
 
     	case 86 :  // System.out.println("rule_list ::= rule_list action_block"); //$NON-NLS-1$
		    consume_rule_action_block();  
			break ;
 
     	case 87 :  // System.out.println("rule_list ::= rule_list ERROR_SYMBOL"); //$NON-NLS-1$
		    consume_rule_symbol();  
			break ;
 
     	case 88 :  // System.out.println("rule_list ::= rule_list SYMBOL"); //$NON-NLS-1$
		    consume_rule_symbol();  
			break ;
 
     	case 89 :  // System.out.println("rule_list ::= OR"); //$NON-NLS-1$
		    handle_error("bad_first_symbol_in_RULES_section");  
			break ;
 
     	case 90 :  // System.out.println("rule_list ::= EMPTY_SYMBOL"); //$NON-NLS-1$
		    handle_error("bad_first_symbol_in_RULES_section");  
			break ;
 
     	case 91 :  // System.out.println("rule_list ::= ERROR_SYMBOL"); //$NON-NLS-1$
		    handle_error("bad_first_symbol_in_RULES_section");  
			break ;
 
     	case 92 :  // System.out.println("rule_list ::= keyword"); //$NON-NLS-1$
		    handle_error("bad_first_symbol_in_RULES_section");  
			break ;
 
     	case 93 :  // System.out.println("rule_list ::= rule_list OR produces"); //$NON-NLS-1$
		    handle_error("rule_without_left_hand_side");  
			break ;

     	case 94 :  // System.out.println("rule_list ::= rule_list action_block produces"); //$NON-NLS-1$
		    handle_error("rule_without_left_hand_side");  
			break ;

     	case 95 :  // System.out.println("rule_list ::= rule_list EMPTY_SYMBOL produces"); //$NON-NLS-1$
		    handle_error("rule_without_left_hand_side");  
			break ;

     	case 96 :  // System.out.println("rule_list ::= rule_list keyword produces"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_in_rules");  
			break ;
 
     	case 97 :  // System.out.println("action_block ::= BLOCK"); //$NON-NLS-1$
		    consume_add_block_definition();  
			break ;
 
     	case 98 :  // System.out.println("action_block ::= HBLOCK"); //$NON-NLS-1$
		    consume_add_block_definition();  
			break ;
 
     	case 99 :  // System.out.println("keyword ::= DEFINE_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_in_rules");  
			break ;
 
     	case 100 :  // System.out.println("keyword ::= TERMINALS_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_in_rules");  
			break ;
 
     	case 101 :  // System.out.println("keyword ::= ALIAS_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_in_rules");  
			break ;
 
     	case 102 :  // System.out.println("keyword ::= START_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_in_rules");  
			break ;
 
     	case 103 :  // System.out.println("keyword ::= RULES_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_in_rules");  
			break ;
 
     	case 105 :  // System.out.println("enter_names ::="); //$NON-NLS-1$
		    consume_enter_names_block();  
			break ;
 
     	case 106 :  // System.out.println("names_definition ::= name produces name"); //$NON-NLS-1$
		    consume_names_definition();  
			break ;
 
     	case 116 :  // System.out.println("bad_name ::= DEFINE_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_found_in_NAMES_section");  
			break ;
 
     	case 117 :  // System.out.println("bad_name ::= TERMINALS_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_found_in_NAMES_section");  
			break ;
 
     	case 118 :  // System.out.println("bad_name ::= ALIAS_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_found_in_NAMES_section");  
			break ;
 
     	case 119 :  // System.out.println("bad_name ::= START_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_found_in_NAMES_section");  
			break ;
 
     	case 120 :  // System.out.println("bad_name ::= RULES_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_found_in_NAMES_section");  
			break ;
 
     	case 121 :  // System.out.println("bad_name ::= NAMES_KEY"); //$NON-NLS-1$
		    handle_error("misplaced_keyword_found_in_NAMES_section");  
			break ;
 
     	case 122 :  // System.out.println("bad_name ::= BLOCK"); //$NON-NLS-1$
		    handle_error("misplaced_block_names");  
			break ;

     	case 123 :  // System.out.println("bad_name ::= MACRO_NAME"); //$NON-NLS-1$
		    handle_error("misplaced_macro_in_names");  
			break ;

     	case 125 :  // System.out.println("[define_block] ::= define_block"); //$NON-NLS-1$
		    consume_define_block();  
			break ;
 
    	case 127 :  // System.out.println("[terminals_block] ::= terminals_block"); //$NON-NLS-1$
		    consume_terminals_block();  
			break ;
 
     	case 129 :  // System.out.println("[alias_block] ::= alias_block"); //$NON-NLS-1$
		    consume_alias_block(); 
			break ;

     	case 135 :  // System.out.println("[names_block] ::= names_block"); //$NON-NLS-1$
		    consume_names_block();  
			break ;
 
     	case 137 :  // System.out.println("[%END] ::= END_KEY"); //$NON-NLS-1$
		    consume_end_key(); 
			break ;

		}
	}

	private final static int original_state(int state) { return -base_check(state); }
	private final static int asi(int state) { return asb[original_state(state)]; }
	
	private final static int nt_action(int state, int sym) {
		return base_action[state + sym];
	}
	
	private final static int t_action(int state, int sym) {
		return term_action[term_check[base_action[state] + sym] == sym ? base_action[state]
				+ sym
				: base_action[state]];
	}

	private void createProblem(String message, Token token) {
		Problem problem = new Problem(Problem.ERROR, message, token.offset, token.length);
		if (problemRequestor != null && token != null)
			problemRequestor.accept(problem);
		document.addProblem(problem);
	}

	public String toString() {
		final String leftMarker = "--->"; //$NON-NLS-1$
		final String rightMarker = "<---"; //$NON-NLS-1$
		StringBuffer sb = new StringBuffer();
		sb.append("TOKEN_STACK:"); //$NON-NLS-1$
		if (tok_stack_top == -1) {
			sb.append(leftMarker);
			sb.append(rightMarker);
		}
		for (int i = 0, n = tok_stack_top + 5; i < tok_stack.length && i < n; i++) {
			if (i > 0) sb.append(", "); //$NON-NLS-1$
			if (i == tok_stack_top) sb.append(leftMarker);
			sb.append(tok_stack[i]);
			if (i == tok_stack_top) sb.append(rightMarker);
		}
		sb.append("\nPARSE_STACK:"); //$NON-NLS-1$
		if (parse_stack_top == -1) {
			sb.append(leftMarker);
			sb.append(rightMarker);
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
					sb.append(name[terminal_index[indexes[i]]]);
				}
			}
		}
		return sb.toString();
	}
}
