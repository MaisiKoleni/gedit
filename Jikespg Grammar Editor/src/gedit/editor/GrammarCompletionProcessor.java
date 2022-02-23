/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import java.io.File;
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
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import gedit.GrammarEditorPlugin;
import gedit.model.Definition;
import gedit.model.Document;
import gedit.model.DocumentOptions;
import gedit.model.GenericModel;
import gedit.model.ModelType;
import gedit.model.ModelUtils;
import gedit.model.ModelUtils.OptionAmbigousException;
import gedit.model.ModelUtils.OptionProposal;
import gedit.model.Rule;

public class GrammarCompletionProcessor implements IContentAssistProcessor {
	private GrammarSourceViewer fViewer;
	private int fOffset;
	private String fErrorMessage;

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {

		if (!(viewer instanceof GrammarSourceViewer))
			return null;
		fViewer = (GrammarSourceViewer) viewer;
		fOffset = offset;
		String text = viewer.getDocument().get();
		String word = findWordBegin(text, offset, (char) 0, false);
		int end = findWordEnd(text, offset, (char) 0);
		String contentType = fViewer.getContentType(viewer.getDocument(), offset);

		List<ICompletionProposal> proposals = new ArrayList<>();
		Document model = fViewer.getModel(true);

		if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
			computeRulesProposals(model, word, end, proposals);
			computeTerminalProposals(model, word, end, proposals);
		}
		ILabelProvider labelProvider = new ModelLabelProvider();
		if (GrammarPartitionScanner.GRAMMAR_MACRO.equals(contentType))
			computeMacroProposals(model, word, end, labelProvider, proposals);
		if (GrammarPartitionScanner.GRAMMAR_OPTION.equals(contentType)) {
			word = findWordBegin(text, offset, ',', false);
			String previousWord = findWordBegin(text, offset - word.length(), ',', true);
			end = findWordEnd(text, offset, ',');
			computeOptionProposals(model, word, previousWord.trim(), end, proposals);
		}
		if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType))
			computeKeywordProposals(model, word, end, labelProvider, proposals);

		setErrorMessage(proposals.size() > 0 ? null : "No proposals available");

		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private void computeRulesProposals(Document model, String word, int endOffset, List<ICompletionProposal> proposals) {

		List<Rule> list = new ArrayList<>(Arrays.asList(model.getRules()));
		Collections.sort(list);
		Set<String> duplicates = new HashSet<>();
		Rule[] rules = list.toArray(new Rule[list.size()]);
		for (Rule rule : rules) {
			String name = rule.getLhs();
			if (duplicates.contains(name))
				continue;
			duplicates.add(name);
			if (!startsWithIgnoreCase(name, word))
				continue;
			int length = endOffset - fOffset;
			String replacement = name.substring(word.length());
			proposals.add(new GrammarCompletionProposal(replacement, name,
					GrammarEditorPlugin.getImage("icons/production.gif"), //$NON-NLS-1$
					fOffset, length, replacement.length(), null, model, (char) 0));
		}
	}

	private void computeTerminalProposals(Document model, String word, int endOffset, List<ICompletionProposal> proposals) {
		List<GenericModel> list = new ArrayList<>(Arrays.asList(model.getTerminals()));
		Collections.sort(list);
		GenericModel[] terminals = list.toArray(new GenericModel[list.size()]);
		for (GenericModel terminal : terminals) {
			String name = terminal.getLabel();
			if (!startsWithIgnoreCase(name, word))
				continue;
			int length = endOffset - fOffset;
			String replacement = name.substring(Math.min(word.length(), name.length()));
			proposals.add(new GrammarCompletionProposal(replacement, name,
					GrammarEditorPlugin.getImage("icons/terminal.gif"), //$NON-NLS-1$
					fOffset, length, replacement.length(), null, model, (char) 0));
		}
	}

	private void computeMacroProposals(Document model, String word, int endOffset, ILabelProvider labelProvider, List<ICompletionProposal> proposals) {
		Definition[] definitions = model.getAllMakros();
		List<Definition> list = new ArrayList<>(Arrays.asList(definitions));
		list.add(new Definition(null, labelProvider.getText(ModelType.EMPTY_TOK), ""));
		Collections.sort(list);
		definitions = list.toArray(new Definition[list.size()]);
		char escape = model.getOptions().getEsape();
		String[] blockb = model.getOptions().getBlockBeginnings();
		boolean secondRun = false;
		do {
			for (Definition definition : definitions) {
				String name = definition.getName();
				name = escape + name;
				for (String element : blockb) {
					if (!word.startsWith(element))
						continue;
					word = word.substring(element.length());
				}
				boolean withEscape = startsWithIgnoreCase(name, word);
				boolean withoutEscape = startsWithIgnoreCase(name, escape + word);
				if (!withEscape && !withoutEscape && !secondRun)
					continue;

				int offset = withEscape ? fOffset : fOffset - word.length();
				int length = endOffset - offset;
				String replacement = withEscape ? name.substring(word.length()) : name;
				String additionalInfo = definition.getValue();
				if ("".equals(additionalInfo))
					additionalInfo = "predefined macro, no lookup available";
				proposals.add(new GrammarCompletionProposal(replacement, name,
						GrammarEditorPlugin.getImage("icons/definition.gif"), //$NON-NLS-1$
						offset, length, replacement.length(), additionalInfo, model, escape));
			}
			secondRun = true;
			fOffset += word.length();
		} while (proposals.isEmpty());
	}

	private void computeKeywordProposals(Document model, String word, int endOffset, ILabelProvider labelProvider, List<ICompletionProposal> proposals) {
		ModelType[] allTypes = ModelType.getAllTypes();
		List<GrammarCompletionProposal> sortedProposals = new ArrayList<>();
		for (ModelType type : allTypes) {
			if (!type.isKeyword())
				continue;
			String keyword = labelProvider.getText(type);
			char escape = type == ModelType.OPTION ? DocumentOptions.DEFAULT_ESCAPE : model.getOptions().getEsape();
			boolean withEscape = startsWithIgnoreCase(escape + keyword, word);
			boolean withoutEscape = startsWithIgnoreCase(keyword, word);
			if (!withEscape && !withoutEscape)
				continue;
			int offset = withEscape ? fOffset : fOffset - word.length();
			int length = endOffset - offset;
			keyword = escape + keyword;
			String replacement = withEscape ? keyword.substring(word.length()) : keyword;
			sortedProposals.add(new GrammarCompletionProposal(replacement, keyword,
					GrammarEditorPlugin.getImage("icons/escape.gif"), //$NON-NLS-1$
					offset, length, replacement.length(), null, model, escape));
		}
		Collections.sort(sortedProposals);
		proposals.addAll(sortedProposals);
	}

	private OptionProposal findOptionProposal(String word) {
		try {
			return ModelUtils.findOptionProposal(word);
		} catch (OptionAmbigousException e) {
			return null;
		}
	}

	private void computeOptionProposals(Document model, String word, String previousWord, int endOffset, List<ICompletionProposal> proposals) {
		Map<?, ?> options = ModelUtils.getAllOptions();
		int assignmentIndex = word.indexOf('=');
		if ("=".equals(previousWord) || assignmentIndex != -1) {
			String key = assignmentIndex != -1 ? word.substring(0, assignmentIndex) : previousWord;
			if (assignmentIndex != -1)
				word = word.substring(assignmentIndex + 1);
			OptionProposal option = findOptionProposal(key);
			if (option != null)
				computeOptionValueProposals(model, word, endOffset, proposals, option);
			return;
		}

		OptionProposal previousOption = findOptionProposal(previousWord);
		if (previousOption != null && previousOption.hasValue()) {
			proposals.add(new GrammarCompletionProposal("=", "=",
					GrammarEditorPlugin.getImage("icons/option.gif"), //$NON-NLS-1$
					fOffset, 1, 1, null, model, (char) 0));
			return;
		}

		OptionProposal optionByWord = findOptionProposal(word);
		for (Iterator<?> it = options.values().iterator(); it.hasNext(); ) {
			OptionProposal proposal = (OptionProposal) it.next();
			String name = proposal.getKey();
			previousOption = optionByWord;
			boolean noprefixed = word.length() > 1 && startsWithIgnoreCase("no" + name, word);
			boolean unprefixed = startsWithIgnoreCase(name, word);
			if (!noprefixed && !unprefixed && previousOption == null)
				continue;
			if (name.equalsIgnoreCase(word))
				previousOption = (OptionProposal) options.get(name);
			if (previousOption != null && optionByWord != null && previousOption.hasValue() && !noprefixed) {
				proposals.add(new GrammarCompletionProposal("=", "=",
						GrammarEditorPlugin.getImage("icons/option.gif"), //$NON-NLS-1$
						fOffset, 1, 1, null, model, (char) 0));
				optionByWord = null;
				continue;
			}
			if (noprefixed && proposal.hasValue())
				continue;
			if (noprefixed)
				name = "no" + name;
			int length = endOffset - fOffset;
			String replacement = name.substring(Math.min(word.length(), name.length()));
			String additionalInfo = "This option is valid for " + getOptionProposalVersionInfo(proposal.getVersion());
			proposals.add(new GrammarCompletionProposal(replacement, name,
					GrammarEditorPlugin.getImage("icons/option.gif"), //$NON-NLS-1$
					fOffset, length, replacement.length(), additionalInfo, model, (char) 0));
		}
	}

	private String getOptionProposalVersionInfo(int version) {
		switch (version) {
		default:
			return null;
		case OptionProposal.JIKESPG:
			return "JikesPG";
		case OptionProposal.LPG:
			return "LPG";
		case OptionProposal.JIKESPG | OptionProposal.LPG:
			return "JikesPG and LPG";
		}
	}

	private void computeOptionValueProposals(Document model, String word, int endOffset, List<ICompletionProposal> proposals, OptionProposal option) {
		String[] choices = option.getChoice();
		if (choices != null) {
			for (String choice : choices) {
				if (!startsWithIgnoreCase(choice, word))
					continue;
				String replacement = choice.substring(Math.min(word.length(), choice.length()));
				int length = endOffset - fOffset;
				proposals.add(new GrammarCompletionProposal(replacement, choice, null,
						fOffset, length, replacement.length(), null, model, (char) 0));
			}
		} else if (option.isFile()) {
			computeFileSystemProposals(model, word, endOffset, proposals, false);
		} else if (option.isDirectory()) {
			computeFileSystemProposals(model, word, endOffset, proposals, true);
		}
	}

	private void computeFileSystemProposals(Document model, String word, int endOffset, List<ICompletionProposal> proposals, boolean suppressFiles) {
		int separatorIndex = word.lastIndexOf(';');
		if (separatorIndex != -1)
			word = word.substring(separatorIndex + 1);
		String[] dirs = model.getOptions().getIncludeDirs();
		if (dirs != null) {
			for (String dir : dirs) {
				File root = new File(dir);
				doComputeFileSystemProposals(model, word, endOffset, proposals, root, suppressFiles, false);
			}
		}
		File root = new File(word);
		if (root.isAbsolute()) {
			doComputeFileSystemProposals(model, word, endOffset, proposals, root, suppressFiles, true);
		}
	}

	private void doComputeFileSystemProposals(Document model, String word, int endOffset, List<ICompletionProposal> proposals, File root, boolean suppressFiles, boolean absolute) {
		word = word.replace('/', File.separatorChar);
		if (absolute && !word.endsWith(File.separator) && root.exists() && root.isDirectory())
			root = root.getParentFile();
		while (!root.exists() && root.getParentFile() != null)
			root = root.getParentFile();
		File[] files = root.listFiles();
		if (files == null)
			return;
		int separatorIndex = word.lastIndexOf(File.separatorChar);
		if (separatorIndex != -1) {
			File file = new File(root, word.substring(0, separatorIndex + 1));
			if (file.exists()) {
				root = file;
				files = root.listFiles();
				if (files == null)
					return;
				word = word.substring(separatorIndex + 1);
			}
		}
		String prefix = new File(word).getName();
		if (word.endsWith(File.separator))
			prefix = "";
		boolean first = true;
		for (int pass = 0; pass < 2; pass++) {
			for (File file : files) {
				if (suppressFiles && file.isFile())
					continue;
				if (pass == 0 && !file.isDirectory() || pass == 1 && file.isDirectory())
					continue;
				String path = absolute ? file.getAbsolutePath() : file.getName();
				if (!startsWithIgnoreCase(path, word))
					continue;
				String display = file.getName();
				String replacement = display.substring(prefix.length());
				int length = endOffset - fOffset;
				if (proposals.size() > 0 && first)
					proposals.add(new CompletionProposal("", fOffset, 0, 0, null, "----------- " + root.getAbsolutePath(), null, null));
				proposals.add(new GrammarCompletionProposal(replacement, display,
						PlatformUI.getWorkbench().getSharedImages().getImage(file.isFile() ? ISharedImages.IMG_OBJ_FILE : file.isDirectory() ? ISharedImages.IMG_OBJ_FOLDER : ISharedImages.IMG_OBJ_ELEMENT),
						fOffset, length, replacement.length(), null, model, (char) 0));
				first = false;
			}
		}
	}

	private String findWordBegin(String text, int offset, char separator, boolean includeWhitespace) {
		if (offset > text.length())
			return new String();
		int i = offset - 1;
		if (includeWhitespace)
			while (i > 0 && Character.isWhitespace(text.charAt(i)))
				i--;
		for ( ; i >= 0; i--) {
			char c = text.charAt(i);
			if (!Character.isWhitespace(c) && c != separator)
				continue;
			if (i < offset)
				i++;
			return text.substring(i, offset);
		}
		return text.substring(i + 1, offset);
	}

	private int findWordEnd(String text, int offset, char separator) {
		if (offset >= text.length())
			return offset;
		for (int i = offset; i < text.length(); i++) {
			char c = text.charAt(i);
			if (!Character.isWhitespace(c) && c != separator)
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

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	private void setErrorMessage(String string) {
		fErrorMessage = string;
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor()
				.getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(fErrorMessage);
	}

	@Override
	public String getErrorMessage() {
		return fErrorMessage;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}
