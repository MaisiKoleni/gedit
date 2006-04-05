/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.model.Document;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;

public class GrammarPartitionScanner extends RuleBasedPartitionScanner {
	private class BlockRule extends MultiLineRule {
		public BlockRule(String startSequence, String endSequence, IToken token) {
			super(startSequence, endSequence, token, (char) 0, true);
		}
		
		public void setSequences(String start, String end) {
			fStartSequence = start.toCharArray();
			fEndSequence = end.toCharArray();
		}
	};
	
	private class MacroRule extends WordRule implements IPredicateRule {
		public MacroRule(IWordDetector detector, IToken defaultToken) {
			super(detector, defaultToken);
		}

		public IToken getSuccessToken() {
			return fDefaultToken;
		}

		public IToken evaluate(ICharacterScanner scanner, boolean resume) {
			return evaluate(scanner);
		}
		
	};
	
	private MacroKeyDetector fMacroKeyDetector;
	private BlockRule fBlockRule;
	private BlockRule fHBlockRule;

	public final static String GRAMMAR_COMMENT = "__grammar_comment";
	public final static String GRAMMAR_OPTION = "__grammar_option";
	public final static String GRAMMAR_MACRO = "__grammar_macro";
	public final static String GRAMMAR_OPERATOR = "__grammar_operator";
	
	public GrammarPartitionScanner() {

		IToken comment = new Token(GRAMMAR_COMMENT);
		IToken option = new Token(GRAMMAR_OPTION);
		IToken macro = new Token(GRAMMAR_MACRO);
		IToken operator = new Token(GRAMMAR_OPERATOR);

		IPredicateRule[] rules = {
			new SingleLineRule("--", null, comment, (char) 0, true),
			new CaseInsensitiveSingleLineRule("%Options", null, option, (char) 0, true),
			fBlockRule = new BlockRule(Document.DEFAULT_BLOCKB, Document.DEFAULT_BLOCKE, macro),
			fHBlockRule = new BlockRule(Document.DEFAULT_HBLOCKB, Document.DEFAULT_HBLOCKE, macro),
			new MacroRule(fMacroKeyDetector = new MacroKeyDetector(), macro),
			new OperatorRule(operator),
		};

		setPredicateRules(rules);
	}
	
	public void setPartialRange(IDocument document, int offset, int length, String contentType, int partitionOffset) {
		super.setPartialRange(document, offset, length, contentType, partitionOffset);
		if (offset == 0 && length == 0)
			return;
		Document model = GrammarEditorPlugin.getDocumentModel(document, null, false);
		if (model == null)
			model = GrammarEditorPlugin.getDocumentModel(document, null, true);
		fMacroKeyDetector.setEscape(model.getEsape());
		String[] beginnings = model.getBlockBeginnings();
		String[] ends = model.getBlockEnds();
		fBlockRule.setSequences(beginnings[0], ends[0]);
		fHBlockRule.setSequences(beginnings[1], ends[1]);
	}
}
