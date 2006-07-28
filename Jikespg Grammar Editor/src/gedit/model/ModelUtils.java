/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import gedit.GrammarEditorPlugin;
import gedit.StringUtils;
import gedit.editor.GrammarSourceViewer;
import gedit.editor.MacroKeyDetector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.RuleBasedScanner;

public class ModelUtils {
	public static class OptionProposal implements Comparable {
		private String key;
		private boolean hasValue;
		private String[] choice;
		private boolean isFile;
		private boolean isDirectory;
		private int version;
		
		public final static int JIKESPG = 1;
		public final static int LPG = 2;
		
		static OptionProposal createFromLine(String line, int fromVersion) {
			OptionProposal option = new OptionProposal();
			option.version = fromVersion;
			int assignmentIndex = line.indexOf('=');
			if (assignmentIndex == -1) {
				option.key = line;
				return option;
			}
			option.key = line.substring(0, assignmentIndex);
			option.hasValue = true;
			String values = line.substring(assignmentIndex + 1);
			if (values.length() == 0)
				return option;
			option.isFile = values.equals("file");
			option.isDirectory = values.equals("directory");
			if (values.charAt(0) == '<' && values.charAt(values.length() - 1) == '>')
				option.choice = StringUtils.split(values.substring(1, values.length() - 1), "|");
			return option;
		}
		
		public boolean equals(Object obj) {
			return obj instanceof OptionProposal ? key.equals(((OptionProposal) obj).key) : false;
		}
		
		public int compareTo(Object o) {
			return o instanceof OptionProposal ? key.compareToIgnoreCase(((OptionProposal) o).key) : 0;
		}
		
		public String getKey() {
			return key;
		}

		public String[] getChoice() {
			return choice;
		}
		
		public int getVersion() {
			return version;
		}

		public boolean hasValue() {
			return hasValue;
		}

		public boolean isDirectory() {
			return isDirectory;
		}

		public boolean isFile() {
			return isFile;
		}
	}

	private static Map OPTIONS;
	private static BitSet ALL_FILTER;;

	public static Definition lookupMacro(int offset, GrammarSourceViewer viewer, RuleBasedScanner macroScanner, Position found) {
		char c = 0;
		int start = offset, end = offset;
		IDocument document = viewer.getDocument();
		Document model = viewer.getModel(false);
		char escape = model.getOptions().getEsape();

		try {
			while (start >= 0) {
				c = document.getChar(start);
				if (escape == c || Character.isWhitespace(c))
					break;
				--start;
			}
			start++;
			end = start + document.getLineLength(document.getLineOfOffset(start));
		} catch (BadLocationException ignore) {
		}
		
		MacroKeyDetector macroKeyDetector = new MacroKeyDetector(escape, model.getAllMakros());
		if (!macroKeyDetector.isWordStart(c))
			return null;

		StringBuffer buffer = new StringBuffer();
		macroScanner.setRange(document, start, end - start);
		Scan: while ((c = (char) macroScanner.read()) != ICharacterScanner.EOF) {
			switch (macroKeyDetector.isWordPart(c, macroScanner, buffer)) {
			case MacroKeyDetector.NO_MATCH:
				return null;
			case MacroKeyDetector.PROBABLE_MATCH:
				buffer.append(c);
				break;
			case MacroKeyDetector.MATCH:
				break Scan;
			}
		}
		Definition definition = (Definition) model.getElementById(buffer.toString(), ModelType.DEFINITION.or((BitSet) null));
		if (definition != null && found != null) {
			found.offset = start;
			found.length = buffer.length();
		}
		return definition;
	}

	public static BitSet createBitSetFromString(String string, String separator) {
		String[] filters = StringUtils.split(string, separator);
		BitSet set = new BitSet();
		for (int i = 0; i < filters.length; i++) {
			try {
				set.set(Integer.parseInt(filters[i].trim()));
			} catch (Exception ignore) {
			}
		}
		return set;
	}
	
	public static String createStringFromBitSet(BitSet set, String separator) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0, lastIndex = 0, n = set.cardinality(); i < n; i++, lastIndex++) {
			if (i > 0)
				sb.append(separator);
			sb.append(lastIndex = set.nextSetBit(lastIndex));
		}
		return sb.toString();
	}

	protected static Definition[] readPredefinedDefinitions() {
		String[] lines = readResource("predefined_macros.txt", false);
		List result = new ArrayList();
		for (int i = 0; i < lines.length; i++) {
			result.add(new Definition(null, lines[i], ""));
		}
		return (Definition[]) result.toArray(new Definition[result.size()]);
	}

	protected static OptionProposal[] readOptions() {
		String[] lines = readResource("jikespg_options.txt", false);
		List result = new ArrayList();
		for (int i = 0; i < lines.length; i++) {
			result.add(OptionProposal.createFromLine(lines[i], OptionProposal.JIKESPG));
		}
		lines = readResource("lpg_options.txt", false);
		for (int i = 0; i < lines.length; i++) {
			OptionProposal proposal = OptionProposal.createFromLine(lines[i], OptionProposal.LPG);
			if (result.contains(proposal))
				proposal.version |= OptionProposal.JIKESPG;
			result.add(proposal);
		}
		Collections.sort(result);
		return (OptionProposal[]) result.toArray(new OptionProposal[result.size()]);
	}

	protected static String[] readResource(final String fileName, boolean sort) {
		InputStream in = ModelUtils.class.getResourceAsStream(fileName);
		if (in == null) {
			GrammarEditorPlugin.logError("Missing resource: " + fileName, null);
			return new String[0];
		}
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		List result = new ArrayList();
		try {
			for (String line = r.readLine(); line != null; line = r.readLine()) {
				line = line.trim();
				if (!line.startsWith("#"))
					result.add(line.trim());
			}
			in.close();
		} catch (IOException e) {
			GrammarEditorPlugin.logError("Cannot read: " + fileName, e);
		}
		if (sort)
			Collections.sort(result);
		return (String[]) result.toArray(new String[result.size()]);
	}

	public static BitSet getAllFilter() {
		if (ALL_FILTER == null) {
			BitSet set = new BitSet();
			ModelType[] allTypes = ModelType.getAllTypes();
			for (int i = 0; i < allTypes.length; i++) {
				set.set(allTypes[i].getBitPosition());
			}
			ALL_FILTER = set;
		}
		return ALL_FILTER;
	}
	
	public static Map getAllOptions() {
		if (OPTIONS == null) {
			OptionProposal[] proposals = ModelUtils.readOptions();
			Map map = new TreeMap();
			for (int i = 0; i < proposals.length; i++) {
				map.put(proposals[i].getKey(), proposals[i]);
			}
			OPTIONS = Collections.unmodifiableMap(map);
		}
		return OPTIONS;
	}

	public static OptionProposal findOptionProposal(String word) {
		if (word == null)
			return null;
		Map proposals = getAllOptions();
		OptionProposal proposal = (OptionProposal) proposals.get(word);
		if (proposal == null)
			proposal = (OptionProposal) proposals.get(word.replace('_', '-'));
		if (proposal != null)
			return proposal;
		for (Iterator it = proposals.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			if (key.startsWith(word)) {
				boolean isAmbigous = false;
				for (Iterator it1 = proposals.keySet().iterator(); it1.hasNext();) {
					String cmp = (String) it1.next();
					if (cmp.equals(key) || !cmp.startsWith(word))
						continue;
					OptionProposal found1 = (OptionProposal) proposals.get(key);
					OptionProposal found2 = (OptionProposal) proposals.get(cmp);
					if (isAmbigous = (found1.version & found2.version) > 0)
						break;
				}
				if (!isAmbigous)
					return (OptionProposal) proposals.get(key);
			}
			int sepIndex = key.indexOf('_');
				if (sepIndex == -1)
					sepIndex = key.indexOf('-');
			if (sepIndex != -1 && sepIndex < key.length()) {
				String abbrev = key.charAt(0) + String.valueOf(key.charAt(sepIndex + 1));
				if (abbrev.equalsIgnoreCase(word))
					return (OptionProposal) proposals.get(key);
			}
		}
		return null;
	}

}
