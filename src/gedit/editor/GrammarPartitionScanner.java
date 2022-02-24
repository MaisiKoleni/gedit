/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

import gedit.GrammarEditorPlugin;
import gedit.model.Document;

public class GrammarPartitionScanner extends RuleBasedPartitionScanner {
	private class BlockRule extends MultiLineRule {
		public BlockRule(String startSequence, String endSequence, IToken token) {
			super(startSequence, endSequence, token, (char) 0, true);
		}

		public void setSequences(String start, String end) {
			fStartSequence = start.toCharArray();
			fEndSequence = end.toCharArray();
		}
	}

	private BlockRule[] fBlockRules;
	private MacroKeyRule fMacroRule;
	private int fStaticRuleNumber;

	public final static String GRAMMAR_COMMENT = "__grammar_comment";
	public final static String GRAMMAR_OPTION = "__grammar_option";
	public final static String GRAMMAR_MACRO = "__grammar_macro";

	public GrammarPartitionScanner() {

		IToken comment = new Token(GRAMMAR_COMMENT);
		IToken option = new Token(GRAMMAR_OPTION);
		IToken macro = new Token(GRAMMAR_MACRO);

		IPredicateRule[] rules = {
			new CaseInsensitiveSingleLineRule("--", null, comment, (char) 0, true, true),
			new CaseInsensitiveSingleLineRule("%Options", null, option, (char) 0, true, true),
			fMacroRule = new MacroKeyRule(new MacroKeyDetector(), macro),
		};
		fStaticRuleNumber = rules.length;

		setPredicateRules(rules);
	}

	@Override
	public void setPartialRange(IDocument document, int offset, int length, String contentType, int partitionOffset) {
		super.setPartialRange(document, offset, length, contentType, partitionOffset);
		if (offset == 0 && length == 0)
			return;
		((CaseInsensitiveSingleLineRule) fRules[0]).reset();
		((CaseInsensitiveSingleLineRule) fRules[1]).reset();
		Document model = GrammarEditorPlugin.getDocumentModel(document, null, offset == 0);
		fMacroRule.getMacroKeyDetector().setEscape(model.getOptions().getEsape());
		fMacroRule.getMacroKeyDetector().setMakros(model.getAllMakros());
		String[] beginnings = model.getOptions().getBlockBeginnings();
		String[] ends = model.getOptions().getBlockEnds();
		if (fBlockRules == null || fBlockRules.length != beginnings.length) {
			fBlockRules = new BlockRule[beginnings.length];
			for (int i = 0; i < beginnings.length; i++) {
				fBlockRules[i] = new BlockRule(beginnings[i], ends[i], fMacroRule.getSuccessToken());
			}
			addBlockRules();
		} else {
			for (int i = 0; i < beginnings.length; i++) {
				fBlockRules[i].setSequences(beginnings[i], ends[i]);
			}
		}
	}

	private void addBlockRules() {
		IRule[] rules = new IRule[fStaticRuleNumber + fBlockRules.length];
		System.arraycopy(fRules, 0, rules, 0, fStaticRuleNumber);
		System.arraycopy(fBlockRules, 0, rules, fStaticRuleNumber, fBlockRules.length);
		fRules = rules;
	}
}
