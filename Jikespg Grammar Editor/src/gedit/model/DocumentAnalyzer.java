/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import gedit.GrammarEditorPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DocumentAnalyzer {
	private String text;
	private IProblemRequestor probemRequestor;
	private String[] eol;
	private Map elementToNode = new HashMap();

	protected static String trimQuotes(String text) {
		return text.startsWith("'") && text.endsWith("'") && text.length() > 1 ?
				text.substring(1, text.length() - 1) : text;
	}

	public DocumentAnalyzer(String text, IProblemRequestor probemRequestor) {
		this(text, probemRequestor, null);
	}

	public DocumentAnalyzer(String text, IProblemRequestor probemRequestor, String eol) {
		this.text = text;
		this.probemRequestor = probemRequestor;
		String defaultEol = System.getProperty("line.separator", Character.toString((char) 10));
		if (eol == null)
			eol = defaultEol;
		this.eol = new String[eol.length()];
		for (int i = 0; i < this.eol.length; i++) {
			this.eol[i] = Character.toString(eol.charAt(i));
		}
	}
	
	public Document analyze(Document document) {
		if (document == null)
			document = new Document();
		document.reset();
		if (probemRequestor != null)
			probemRequestor.beginReporting();

		long time = System.currentTimeMillis();
		
		Parser parser = new Parser(document, text, probemRequestor);
		try {
			parser.mapElementToNode(parser.parse(), document, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		checkConsistency(document);

		if (probemRequestor != null)
			probemRequestor.endReporting();

		document.notifyModelChanged();
	
		if (GrammarEditorPlugin.getDefault().isDebugging())
			System.out.println("analyze document in: " + (System.currentTimeMillis() - time) + "ms");
		return document;
	}
	
	private void checkConsistency(Document document) {
		Section rulesSection = document.getSection(ModelType.RULE);
		if (rulesSection == null)
			return;
		Rule[] rules = document.getRules();
		Map rulesLookup = new HashMap();
		for (int i = 0; rules != null && i < rules.length; i++) {
			rulesLookup.put(rules[i].label, rules[i]);
		}
		Terminal[] terminals = document.getTerminals();
		Map terminalsLookup = new HashMap();
		for (int i = 0; terminals != null && i < terminals.length; i++) {
			terminalsLookup.put(terminals[i].label, terminals[i]);
		}
		Alias[] aliases = document.getAliases();
		Map aliasLookup = new HashMap();
		for (int i = 0; aliases != null && i < aliases.length; i++) {
			Alias alias = aliases[i];
			if (aliasLookup.put(trimQuotes(alias.getLhs()), alias) != null)
				createProblem(document, alias, Problem.WARNING, "Alias " + alias.label + " has already been defined.");
		}
		
		for (int i = 0; aliases != null && i < aliases.length; i++) {
			Alias alias = aliases[i];
			String rhs = alias.getRhs().label;
			if (terminalsLookup.containsKey(rhs))
				continue;
			if (rulesLookup.containsKey(rhs))
				continue;
			createProblem(document, alias.getRhs(), Problem.WARNING, "Alias reference " + rhs + " is not defined");
		}
		
		for (int i = 0; rules != null && i < rules.length; i++) {
			Rule rule = rules[i];
			if (lookup(rule.label, terminalsLookup, aliasLookup) != null) {
				createProblem(document, rule, Problem.ERROR, "Terminal " + rule.label + " cannot be used as left hand side");
				markAsReferenced(rule);
			}
			Rhs[] rhses = rule.getRhs();
			for (int j = 0; j < rhses.length; j++) {
				Rhs rhs = rhses[j];
				for (int k = j + 1; k < rhses.length; k++) {
					Rhs other = rhses[k];
					if (equals(other.getParts(), rhs.getParts()))
						createProblem(document, other, Problem.ERROR, other.getLabel(other) + " does already exist in " + rule.label);
				}
				Reference[] parts = rhs.getParts();
				for (int k = 0; k < parts.length; k++) {
					Reference ref = parts[k];
					String label = ref.label;
					if (label.equalsIgnoreCase(document.escape + (String) Document.getKeywords().get(ModelType.EMPTY_TOK)))
						continue;
					label = trimQuotes(label);
					ModelBase referred = lookup(label, rulesLookup, aliasLookup);
					if (referred != null) {
						markAsReferenced(referred);
						continue;
					}
					referred = lookup(label, terminalsLookup, aliasLookup);
					if (referred != null) {
						markAsReferenced(referred);
						continue;
					}
					createProblem(document, ref, Problem.ERROR, "Element " + ref.label + " is not defined");
				}
			}
		}
		for (Iterator it = rulesLookup.values().iterator(); it.hasNext(); ) {
			ModelBase model = (ModelBase) it.next();
			if (hasBeenReferenced(model))
				continue;
			String lhs = model.label;
			if (document.startTokens.contains(lhs))
				continue;
			createProblem(document, model, Problem.WARNING, "The production " + lhs + " is never used");
		}

		for (Iterator it = terminalsLookup.values().iterator(); it.hasNext(); ) {
			ModelBase model = (ModelBase) it.next();
			if (hasBeenReferenced(model))
				continue;
			if (aliasLookup.containsKey(((Terminal) model).label))
				continue;
			createProblem(document, model, Problem.WARNING, "The terminal " + ((Terminal) model).label + " is never used");
		}
	}

	private void markAsReferenced(ModelBase model) {
		model.setUserData("ref", new Object());
	}
	
	private boolean hasBeenReferenced(ModelBase model) {
		return model.getUserData("ref") != null;
	}

	private ModelBase lookup(String value, Map originLookup, Map aliasLookup) {
		ModelBase model = (ModelBase) originLookup.get(value);
		if (model != null)
			return model;
		Alias alias = (Alias) aliasLookup.get(value);
		if (alias == null) {
			String trimmedValue = trimQuotes(value);
			if (trimmedValue.length() == value.length())
				return null;
			alias = (Alias) aliasLookup.get(trimmedValue);
			if (alias == null)
				return (ModelBase) originLookup.get(trimmedValue);
		}
		return (ModelBase) originLookup.get(alias.getRhs().label);
	}

	private boolean equals(ModelBase[] base1, ModelBase[] base2) {
        if (base1 == base2)
        	return true;
        if (base1 == null || base2 == null)
            return false;
        if (base1.length != base2.length)
            return false;
        for (int i = 0; i < base1.length; i++) {
            if (!base1[i].label.equals(base2[i].label))
                return false;
        }
		return true;
	}

	private void createProblem(Document document, ModelBase element, int type, String message) {
		Node node = (Node) elementToNode.get(element);
		if (node == null)
			node = element.node;
		if (node != null)
			createProblem(document, node.offset, node.length, type, message);
	}
	
	private void createProblem(Document document, int offset, int length, int type, String message) {
		Problem problem = new Problem(type, message, offset, length);
		document.addProblem(problem);
		if (probemRequestor != null)
			probemRequestor.accept(problem);
	}
	
	protected static Definition[] readPredefinedDefinitions() {
		final String fileName = "predefined_macros.txt";
		InputStream in = DocumentAnalyzer.class.getResourceAsStream(fileName);
		if (in == null) {
			GrammarEditorPlugin.logError("Missing resource: " + fileName, null);
			return new Definition[0];
		}
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		List result = new ArrayList();
		try {
			for (String line = r.readLine(); line != null; line = r.readLine()) {
				result.add(new Definition(null, line.trim(), ""));
			}
			in.close();
		} catch (IOException e) {
			GrammarEditorPlugin.logError("Cannot read: " + fileName, e);
		}
		return (Definition[]) result.toArray(new Definition[result.size()]);
	}

}
