/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import gedit.GrammarEditorPlugin;
import gedit.NonUISafeRunnable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.Position;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;

public class Document extends ModelBase implements IAdaptable {
	public interface IModelListener {
		public void modelChanged(Document model);
	}

	private Map sections = new HashMap();
	private List includes;
	private Map nodeToElements = new HashMap();
	private List problems;
	private ListenerList listeners;

	private DocumentOptions options = new DocumentOptions();

	private Map elementCache = new WeakHashMap();

	private static Definition[] PREDEFINED_DEFINITIONS;

	public Document() {
		super(null, "Root");
	}

	public Document(String pathRef) {
		super(null, pathRef);
	}

	protected void reset(DocumentOptions globalOptions) {
		if (problems != null)
			problems.clear();
		problems = null;
		sections.clear();
		elementCache.clear();
		if (includes != null)
			includes.clear();
		nodeToElements.clear();
		node = null;
		options.resetTo(globalOptions);
	}

	protected void register(Node node, ModelBase element) {
		nodeToElements.put(node, element);
	}

	protected ModelBase getElementForNode(Node node) {
		return (ModelBase) nodeToElements.get(node);
	}

	public Node getRoot() {
		return node;
	}

	public ModelType getType() {
		return ModelType.DOCUMENT;
	}

	public Object[] getChildren() {
		return getSections();
	}

	public Section[] getSections() {
		return (Section[]) sections.values().toArray(new Section[sections.values().size()]);
	}

	public Section getSection(Object childType) {
		return (Section) sections.get(childType);
	}

	public void setSection(Object childType, Section section) {
		if (section != null)
			sections.put(childType, section);
		else
			sections.remove(childType);
	}

	public Problem[] getProblems(ModelBase model) {
		if (model.node == null)
			return new Problem[0];
		int offset = model.getOffset();
		int length = model.getLength();
		ModelType modelType = model.getType();
		if (modelType == ModelType.RULE || modelType == ModelType.ALIAS || modelType == ModelType.NAME
				|| (modelType == ModelType.SECTION && ((Section) model).getChildType() != ModelType.OPTION)) {
			offset = model.getRangeOffset();
			length = model.getRangeLength();
		}
		return getProblems(offset, length);
	}

	public Problem[] getProblems(int offset, int length) {
		Position position = new Position(offset, length);
		Map result = new TreeMap(Collections.reverseOrder());
		for (int i = 0; problems != null && i < problems.size(); i++) {
			Problem problem = (Problem) problems.get(i);
			if (problem.getOffset() == offset)
				result.put(new Integer(2 * problem.getType()), problem);
			else if (position.overlapsWith(problem.getOffset(), problem.getLength()))
				result.put(new Integer(problem.getType()), problem);
		}
		return (Problem[]) result.values().toArray(new Problem[result.size()]);
	}

	public void addProblem(Problem problem) {
		if (problems == null)
			problems = new ArrayList();
		problems.add(problem);
	}

	protected void addChildren(ModelType childType, List children, boolean visible, boolean create) {
		Class elementType = childType.getModelClass();
		if (elementType == null)
			return;
		Assert.isTrue(ModelBase.class.isAssignableFrom(elementType));
        Section section = getSection(childType);
		if (section == null) {
			if (!create)
				return;
			setSection(childType, section = new Section(childType, this));
		} else
			visible = section.visible;

		int sectionChildrenSize = section.children != null ? section.children.length : 0;
		for (int i = 0; i < children.size(); i++) {
			ModelBase child = (ModelBase) children.get(i);
			for (int j = 0; j < sectionChildrenSize; j++) {
				if (!section.children[j].label.equals(child.label))
					continue;
				children.remove(i);
				break;
			}
		}
		int size = children.size() + sectionChildrenSize;
    	ModelBase[] array = (ModelBase[]) Array.newInstance(elementType, size);
    	children.toArray(array);
		if (sectionChildrenSize > 0)
			System.arraycopy(section.children, 0, array, children.size(), sectionChildrenSize);
		boolean childrenVisible = false;
		for (int i = 0; i < array.length; i++) {
			if (!array[i].visible)
				continue;
			childrenVisible = true;
			break;
		}
		section.visible = visible & childrenVisible & section.node != null;
		section.setChildren(array);
	}

	protected void addInclude(Document include) {
		if (includes == null)
			includes = new ArrayList();
		includes.add(include);
	}

	public Document getInclude(String name) {
		if (includes == null)
			return null;
		Object file = FileProzessor.getFileForName(this, name);
		for (int i = 0; i < includes.size(); i++) {
			Document document = (Document) includes.get(i);
			if (FileProzessor.getFileForName(this, document.label).equals(file))
				return document;
		}
		return null;
	}

	public Rule[] getRules() {
		Section section = getSection(ModelType.RULE);
		if (section == null)
			return new Rule[0];
		Rule[] rules = new Rule[section.children.length];
		int size = 0;

		for (int i = 0; i < section.children.length; i++) {
			ModelBase child = section.children[i];
			if (child instanceof Rule)
				rules[size++] = (Rule) child;
		}
		if (size != rules.length)
			System.arraycopy(rules, 0, rules = new Rule[size], 0, size);
		return rules;
	}

	public GenericModel[] getTerminals() {
		Section section = getSection(ModelType.TERMINAL);
		return section != null ? (GenericModel[]) section.getChildren() : new GenericModel[0];
	}

	public Alias[] getAliases() {
		Section section = getSection(ModelType.ALIAS);
		return section != null ? (Alias[]) section.getChildren() : new Alias[0];
	}

	public GenericModel[] getExports() {
		Section section = getSection(ModelType.EXPORT);
		return section != null ? (GenericModel[]) section.getChildren() : new GenericModel[0];
	}

	public Definition[] getMakros() {
		Section section = getSection(ModelType.DEFINITION);
		return section != null ? (Definition[]) section.getChildren() : new Definition[0];
	}

	public Definition[] getAllMakros() {
		Section section = getSection(ModelType.DEFINITION);
		Definition[] predefinedDefinitions = getPredefinedDefinitions();
		int size = predefinedDefinitions.length;
		if (section != null && section.children != null)
			size += section.children.length;
		Definition[] result = new Definition[size];
		System.arraycopy(predefinedDefinitions, 0, result, 0, predefinedDefinitions.length);
		if (size > predefinedDefinitions.length)
			System.arraycopy(section.children, 0, result, predefinedDefinitions.length, section.children.length);
		return result;
	}

	private Definition[] getPredefinedDefinitions() {
		if (PREDEFINED_DEFINITIONS == null) {
			PREDEFINED_DEFINITIONS = ModelUtils.readPredefinedDefinitions();
		}
		return PREDEFINED_DEFINITIONS;
	}

	public DocumentOptions getOptions() {
		return options;
	}

	public ModelBase getElementAt(int offset) {
		return getElementAt(offset, true);
	}

	public ModelBase getElementAt(int offset, boolean restrictToLeafs) {
		ModelBase element = ElementFinder.findElementAt(this, offset, restrictToLeafs);
		if (GrammarEditorPlugin.DEBUG)
			System.out.println("Element at: " + offset + ": " + element + (element instanceof Reference ? " referrer: " + ((Reference) element).getReferer() : ""));
		return element;
	}

	public ModelBase getElementById(String id) {
		BitSet filter = ModelType.ALIAS.or(
			ModelType.TERMINAL.or(
			ModelType.RULE.or(
			ModelType.EXPORT.or(
			ModelType.DEFINITION.or(
			ModelType.ERROR_TOK)))));
		return getElementById(id, filter);
	}

	public ModelBase getElementById(String id, BitSet filter) {
		String key = id + filter;
		ModelBase model = (ModelBase) elementCache.get(key);
		if (model != null)
			return model;
		model = internalGetElementById(id, filter);
		if (model != null)
			elementCache.put(key, model);
		return model;
	}

	private ModelBase internalGetElementById(String id, BitSet filter) {
		for (Iterator it = new ArrayList(sections.values()).iterator(); it.hasNext(); ) {
			Section section = (Section) it.next();
			if (!section.getChildType().matches(filter))
				continue;
			ModelBase element = section.getElementById(id, filter);
			if (element != null)
				return element;
		}
		if (includes == null)
			return null;
		for (int i = 0; i < includes.size(); i++) {
			ModelBase element = ((Document) includes.get(i)).getElementById(id, filter);
			if (element != null)
				return element;
		}
		return null;
	}

	protected void notifyModelChanged() {
		if (listeners == null)
			return;
		Object[] objects = listeners.getListeners();
		for (int i = 0; i < objects.length; i++) {
			final IModelListener listener = (IModelListener) objects[i];
			Platform.run(new NonUISafeRunnable() {
				public void run() throws Exception {
					listener.modelChanged(Document.this);
				}
			});
		}
	}

	public void addModelListener(IModelListener listener) {
		if (listeners == null)
			listeners = new ListenerList();
		listeners.add(listener);
	}

	public void removeModelListener(IModelListener listener) {
		if (listeners == null)
			return;
		listeners.remove(listener);
	}

	public String getFilePath() {
		return label;
	}

}
