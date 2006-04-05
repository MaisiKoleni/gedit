/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.model.Definition;
import gedit.model.Document;
import gedit.model.ModelType;
import gedit.model.Rule;
import gedit.model.Terminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.PlatformUI;

public class GrammarCompletionProcessor implements IContentAssistProcessor {
	private GrammarSourceViewer fViewer;
	private int fOffset;
	private String fErrorMessage;
	
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		
		if (!(viewer instanceof GrammarSourceViewer))
			return null;
		fViewer = (GrammarSourceViewer) viewer;
		fOffset = offset;
		String text = viewer.getDocument().get();
		String word = findWordBegin(text, offset);
		int end = findWordEnd(text, offset);
		String contentType = fViewer.getContentType(viewer.getDocument(), offset);
		
		List proposals = new ArrayList();
		Document model = fViewer.getModel(true);

		if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
			computeRulesProposals(model, word, end, proposals);
			computeTerminalProposals(model, word, end, proposals);
		}
		if (GrammarPartitionScanner.GRAMMAR_MACRO.equals(contentType))
			computeMacroProposals(model, word, end, proposals);
		if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType))
			computeKeywordProposals(model, word, end, proposals);
		
		setErrorMessage(proposals.size() > 0 ? null : "No proposals available");

		return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private void computeRulesProposals(Document model, String word, int endOffset, List proposals) {
		
		List list = new ArrayList(Arrays.asList(model.getRules()));
		Collections.sort(list);
		Set duplicates = new HashSet();
		Rule[] rules = (Rule[]) list.toArray(new Rule[list.size()]);
		for (int i = 0; i < rules.length; i++) {
			String name = rules[i].getLhs();
			if (duplicates.contains(name))
				continue;
			duplicates.add(name);
			if (!startsWithIgnoreCase(name, word))
				continue;
			int length = (endOffset - fOffset);
			String replacement = name.substring(word.length());
			proposals.add(new GrammarCompletionProposal(replacement, name,
					GrammarEditorPlugin.getImage("icons/production.gif"), //$NON-NLS-1$
					fOffset, length, replacement.length()));
		}
	}

	private void computeTerminalProposals(Document model, String word, int endOffset, List proposals) {
		List list = new ArrayList(Arrays.asList(model.getTerminals()));
		Collections.sort(list);
		Terminal[] terminals = (Terminal[]) list.toArray(new Terminal[list.size()]);
		for (int i = 0; i < terminals.length; i++) {
			String name = terminals[i].getValue();
			if (!startsWithIgnoreCase(name, word))
				continue;
			int length = (endOffset - fOffset);
			String replacement = name.substring(Math.min(word.length(), name.length()));
			proposals.add(new GrammarCompletionProposal(replacement, name,
					GrammarEditorPlugin.getImage("icons/terminal.gif"), //$NON-NLS-1$
					fOffset, length, replacement.length()));
		}
	}

	private void computeMacroProposals(Document model, String word, int endOffset, List proposals) {
		Definition[] definitions = model.getAllMakros();
		List list = new ArrayList(Arrays.asList(definitions));
		Collections.sort(list);
		definitions = (Definition[]) list.toArray(new Definition[list.size()]);
		char escape = model.getEsape();
		String[] blockb = model.getBlockBeginnings();
		for (int i = 0; i < definitions.length; i++) {
			String name = definitions[i].getName();
			name = escape + name.substring(1);
			for (int j = 0; j < blockb.length; j++) {
				if (!word.startsWith(blockb[j]))
					continue;
				word = word.substring(blockb[j].length());
			}
			boolean withEscape = startsWithIgnoreCase(name, word);
			boolean withoutEscape = startsWithIgnoreCase(name, escape + word);
			if (!withEscape && !withoutEscape)
				continue;
			
			int offset = withEscape ? fOffset : fOffset - word.length();
			int length = (endOffset - offset);
			String replacement = withEscape ? name.substring(word.length()) : name;
			proposals.add(new GrammarCompletionProposal(replacement, name,
					GrammarEditorPlugin.getImage("icons/definition.gif"), //$NON-NLS-1$
					offset, length, replacement.length()));
		}
	}

	private void computeKeywordProposals(Document model, String word, int endOffset, List proposals) {
		Map keywords = Document.getKeywords();
		for (Iterator it = keywords.keySet().iterator(); it.hasNext(); ) {
			Object type = it.next();
			String keyword = (String) keywords.get(type);
			char escape = type == ModelType.OPTION ? Document.DEFAULT_ESCAPE : model.getEsape();
			if (!startsWithIgnoreCase(keyword, word) && !startsWithIgnoreCase(escape + keyword, word))
				continue;
			keyword = escape + keyword;
			if (model.getSection(type) != null)
				continue;
			int length = (endOffset - fOffset);
			String replacement = keyword.substring(word.length());
			proposals.add(new GrammarCompletionProposal(replacement, keyword,
					GrammarEditorPlugin.getImage("icons/keyword.gif"), //$NON-NLS-1$
					fOffset, length, replacement.length()));
		}
	}

	private String findWordBegin(String text, int offset) {
		if (offset > text.length())
			return new String();
		int i = offset - 1;
		for ( ; i >= 0; i--) {
			char c = text.charAt(i);
			if (!Character.isWhitespace(c))
				continue;
			if (i < offset)
				i++;
			return text.substring(i, offset);
		}
		return text.substring(i + 1, offset);
	}

	private int findWordEnd(String text, int offset) {
		if (offset >= text.length())
			return offset;
		for (int i = offset; i < text.length(); i++) {
			char c = text.charAt(i);
			if (!Character.isWhitespace(c))
				continue;
			return i;
		}
		return text.length();
	}
	
	private boolean startsWithIgnoreCase(String string, String prefix) {
		if (prefix.length() > string.length())
			return false;
		for (int i = 0; i < prefix.length(); i++) {
			if (Character.toLowerCase(string.charAt(i)) != Character.toLowerCase(prefix.charAt(i)))
				return false;
		}
		return true;
	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	private void setErrorMessage(String string) {
		fErrorMessage = string;
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor()
				.getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(fErrorMessage);
	}

	public String getErrorMessage() {
		return fErrorMessage;
	}

	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}
