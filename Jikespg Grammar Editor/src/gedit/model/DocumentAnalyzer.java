/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gedit.GrammarEditorPlugin;
import gedit.StringUtils;

public class DocumentAnalyzer {
	private IProblemRequestor problemRequestor;
	private FileProzessor fileProzessor;
	private DocumentOptions globalOptions;
	private Document parentDocument;
	private Map<String, Node> elementToNode = new HashMap<>(); // FIXME This is pointless, we never insert

	public DocumentAnalyzer(IProblemRequestor probemRequestor, FileProzessor fileProzessor, Document parentDocument) {
		this(probemRequestor, fileProzessor, parentDocument != null ? parentDocument.getOptions() : null);
		this.parentDocument = parentDocument;
	}

	public DocumentAnalyzer(IProblemRequestor probemRequestor, FileProzessor fileProzessor, DocumentOptions globalOptions) {
		this.problemRequestor = probemRequestor;
		this.fileProzessor = fileProzessor;
		this.globalOptions = globalOptions;
	}

	public Document analyze(Document document, String text) {
		if (document == null)
			document = new Document();
		document.reset(globalOptions);
		if (problemRequestor != null)
			problemRequestor.beginReporting();

		long time = System.currentTimeMillis();

		Parser parser = new Parser(document, text, problemRequestor, fileProzessor);
		try {
			parser.mapElementToNode(parser.parse(), document, true);
		} catch (Exception e) {
			GrammarEditorPlugin.logError("Cannot parse the document", e);
		}

		if (parentDocument != null)
			mergeMacros(parentDocument, document);

		checkConsistency(document);

		if (problemRequestor != null)
			problemRequestor.endReporting();

		document.notifyModelChanged();

		if (GrammarEditorPlugin.DEBUG_PARSER_LEVEL > 0)
			System.out.println("Analyzed " + document + ": " + (System.currentTimeMillis() - time) + "ms");
		return document;
	}

	private void mergeMacros(Document source, Document target) {
		Section sourceSection = source.getSection(ModelType.DEFINITION);
		if (sourceSection == null || sourceSection.children == null)
			return;
		List<ModelBase> clonedMacros = new ArrayList<>();
		for (ModelBase child : sourceSection.children) {
			ModelBase clone = (ModelBase) child.clone();
			clone.visible = false;
			clonedMacros.add(clone);
		}
		target.addChildren(ModelType.DEFINITION, clonedMacros, false, true);
	}

	private void checkConsistency(Document document) {

		Rule[] rules = document.getRules();
		Map<String, ModelBase> rulesLookup = new HashMap<>();
		for (Rule rule : rules) {
			rulesLookup.put(rule.label, rule);
			String strippedLabel = stripEscape(document, rule.label);
			if (strippedLabel != null)
				rulesLookup.put(strippedLabel, rule);
		}
		GenericModel[] terminals = document.getTerminals();
		Map<String, ModelBase> terminalsLookup = new HashMap<>();
		for (GenericModel terminal : terminals) {
			terminalsLookup.put(terminal.label, terminal);
		}
		Alias[] aliases = document.getAliases();
		Map<String, Alias> aliasLookup = new HashMap<>();
		for (Alias alias : aliases) {
			if (aliasLookup.put(StringUtils.trimQuotes(alias.label, '\''), alias) != null)
				createProblem(document, alias, Problem.WARNING, "Alias " + alias.label + " has already been defined.");
		}

		for (Alias alias : aliases) {
			String rhs = alias.getRhs().label;
			if (terminalsLookup.containsKey(rhs) || rulesLookup.containsKey(rhs))
				continue;
			createProblem(document, alias.getRhs(), Problem.WARNING, "Alias reference " + rhs + " is not defined, assuming it is a terminal");
			createUndeclaredTerminal(document, alias.getRhs(), terminalsLookup);
		}

		for (Rule rule : rules) {
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
						createProblem(document, other, Problem.ERROR, other.getLabel() + " does already exist in " + rule.label);
				}
				Reference[] parts = rhs.getParts();
				for (Reference ref : parts) {
					String label = ref.label;
					if ((document.getOptions().getEsape() + ModelType.EMPTY_TOK.getString()).equalsIgnoreCase(label))
						continue;
					ModelBase referred = lookup(label, rulesLookup, aliasLookup);
					if (referred != null) {
						markAsReferenced(referred);
						if (parts.length == 1 && referred.equals(rule))
							createProblem(document, ref, Problem.WARNING, "The production " + rule.label + " -> " + label + " may lead to a recursion");
						continue;
					}
					referred = lookup(label, terminalsLookup, aliasLookup);
					if (referred != null) {
						markAsReferenced(referred);
						continue;
					}

					ModelBase includedRef = document.getElementById(label);
					String strippedLabel = stripEscape(document, label);
					ModelBase strippedIncludeRef = strippedLabel != null ? document.getElementById(strippedLabel) : null;
					if (includedRef != null && includedRef.getDocument() != document && !isExportingType(((Section) includedRef.parent).getChildType())) {
						createProblem(document, ref, Problem.WARNING, "Element " + label + " is declared in " + new File(includedRef.getDocument().getFilePath()).getName() + " but not exported there");
					} else if (includedRef == null && strippedIncludeRef == null && !jpgprs.name[1].substring(1).equalsIgnoreCase(label.substring(1))) {
						createProblem(document, ref, Problem.WARNING, "Element " + label + " is not defined, assuming it is a terminal");
						GenericModel terminal = createUndeclaredTerminal(document, ref, terminalsLookup);
						markAsReferenced(terminal);
					}
				}
			}
		}

		Section section = document.getSection(ModelType.START_TOK);
		if (section != null) {
			ModelBase[] startTokens = section.getChildren();
			for (ModelBase startToken2 : startTokens) {
				Reference startToken = (Reference) startToken2;
				ModelBase referrer = startToken.getReferer();
				if (referrer != null) {
					markAsReferenced(referrer);
				} else {
					createProblem(document, startToken, Problem.WARNING, "The start token " + startToken.label + " is not defined, assuming it is a terminal");
					createUndeclaredTerminal(document, startToken, terminalsLookup);
				}
			}
		}

		for (ModelBase model : rulesLookup.values()) {
			if (hasBeenReferenced(model))
				continue;
			String strippedLabel = stripEscape(document, model.label);
			ModelBase strippedModel = rulesLookup.get(strippedLabel);
			if (strippedModel != null && hasBeenReferenced(strippedModel))
				continue;
			createProblem(document, model, Problem.WARNING, "The production " + model.label + " is never used");
		}

		for (ModelBase model : terminalsLookup.values()) {
			if (hasBeenReferenced(model) || aliasLookup.containsKey(((GenericModel) model).label))
				continue;
			createProblem(document, model, Problem.WARNING, "The terminal " + ((GenericModel) model).label + " is never used");
		}
	}

	private GenericModel createUndeclaredTerminal(Document document, ModelBase model, Map<String, ModelBase> terminalsLookup) {
		ModelBase section = model.parent;
		GenericModel terminal = new GenericModel(section, model.getLabel(), ModelType.TERMINAL);
		terminal.visible = false;
		if (section instanceof Section)
			((Section) section).addChild(terminal).node = section.node;
		terminal.node = new Node(section.node, model.getOffset(), model.getLength());
		document.register(terminal.node, terminal);

		terminalsLookup.put(terminal.label, terminal);
		return terminal;
	}

	private String stripEscape(Document document, String name) {
		int escapeIndex = name.indexOf(document.getOptions().getEsape());
		if (escapeIndex == -1 || "empty".equals(name.substring(1).toLowerCase()) ||
				name.charAt(0) == '\'' && name.charAt(name.length() - 1) == '\'')
			return null;
		return name.substring(0, escapeIndex);
	}

	private boolean isExportingType(ModelType type) {
		return type == ModelType.EXPORT || type == ModelType.ERROR_TOK || type == ModelType.EOF_TOK || type == ModelType.EOL_TOK;
	}

	private void markAsReferenced(ModelBase model) {
		model.setUserData(UserData.REFERENCED, Boolean.TRUE);
	}

	private boolean hasBeenReferenced(ModelBase model) {
		return model.getUserData(UserData.REFERENCED) != null;
	}

	private ModelBase lookup(String value, Map<String, ModelBase> originLookup, Map<String, Alias> aliasLookup) {
		ModelBase model = originLookup.get(value);
		if (model != null)
			return model;
		Alias alias = aliasLookup.get(value);
		if (alias == null) {
			String trimmedValue = StringUtils.trimQuotes(value, '\'');
			if (trimmedValue.length() == value.length())
				return null;
			alias = aliasLookup.get(trimmedValue);
			if (alias == null)
				return originLookup.get(trimmedValue);
		}
		return originLookup.get(alias.getRhs().label);
	}

	private boolean equals(ModelBase[] base1, ModelBase[] base2) {
        if (base1 == base2)
        	return true;
        if (base1 == null || base2 == null || base1.length != base2.length)
            return false;
        for (int i = 0; i < base1.length; i++) {
            if (!base1[i].label.equals(base2[i].label))
                return false;
        }
		return true;
	}

	private void createProblem(Document document, ModelBase element, int type, String message) {
		if (element.getDocument() != document) // ignore problems from included documents
			return;
		Node node = elementToNode.get(element); // FIXME see declaration
		if (node == null)
			node = element.node;
		if (node != null)
			createProblem(document, node.offset, node.length, type, message);
	}

	private void createProblem(Document document, int offset, int length, int type, String message) {
		Problem problem = new Problem(type, message, offset, length);
		document.addProblem(problem);
		if (problemRequestor != null)
			problemRequestor.accept(problem);
	}

}
