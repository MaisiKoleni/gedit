/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.text.Position;

import gedit.GrammarEditorPlugin;
import gedit.NonUISafeRunnable;

public class Document extends ModelBase implements IAdaptable {
	public interface IModelListener {
		void modelChanged(Document model);
	}

	private Map<ModelType, Section> sections = new HashMap<>();
	private List<Document> includes;
	private Map<Node, ModelBase> nodeToElements = new HashMap<>();
	private List<Problem> problems;
	private ListenerList<IModelListener> listeners;

	private DocumentOptions options = new DocumentOptions();

	private Map<String, ModelBase> elementCache = new WeakHashMap<>();

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
		return nodeToElements.get(node);
	}

	public Node getRoot() {
		return node;
	}

	@Override
	public ModelType getType() {
		return ModelType.DOCUMENT;
	}

	@Override
	public Section[] getChildren() {
		return getSections();
	}

	public Section[] getSections() {
		return sections.values().toArray(Section[]::new);
	}

	public Section getSection(ModelType childType) {
		return sections.get(childType);
	}

	public void setSection(ModelType childType, Section section) {
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
				|| modelType == ModelType.SECTION && ((Section) model).getChildType() != ModelType.OPTION) {
			offset = model.getRangeOffset();
			length = model.getRangeLength();
		}
		return getProblems(offset, length);
	}

	public Problem[] getProblems(int offset, int length) {
		Position position = new Position(offset, length);
		Map<Integer, Problem> result = new TreeMap<>(Collections.reverseOrder());
		for (int i = 0; problems != null && i < problems.size(); i++) {
			Problem problem = problems.get(i);
			if (problem.getOffset() == offset)
				result.put(2 * problem.getType(), problem);
			else if (position.overlapsWith(problem.getOffset(), problem.getLength()))
				result.put(problem.getType(), problem);
		}
		return result.values().toArray(new Problem[result.size()]);
	}

	public void addProblem(Problem problem) {
		if (problems == null)
			problems = new ArrayList<>();
		problems.add(problem);
	}

	protected void addChildren(ModelType childType, List<?> children, boolean visible, boolean create) {
		Class<?> elementType = childType.getModelClass();
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
		for (ModelBase element : array) {
			if (!element.visible)
				continue;
			childrenVisible = true;
			break;
		}
		section.visible = visible & childrenVisible & section.node != null;
		section.setChildren(array);
	}

	protected void addInclude(Document include) {
		if (includes == null)
			includes = new ArrayList<>();
		includes.add(include);
	}

	public Document getInclude(String name) {
		if (includes == null)
			return null;
		Object file = FileProzessor.getFileForName(this, name);
		for (Object include : includes) {
			Document document = (Document) include;
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

		for (ModelBase child : section.children) {
			if (child instanceof Rule) {
				rules[size] = (Rule) child;
				size++;
			}
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
		Set<ModelType> filter = EnumSet.of( //
				ModelType.ALIAS, //
				ModelType.TERMINAL, //
				ModelType.RULE, //
				ModelType.EXPORT, //
				ModelType.DEFINITION, //
				ModelType.ERROR_TOK);
		return getElementById(id, filter);
	}

	public ModelBase getElementById(String id, Set<ModelType> filter) {
		String key = id + filter;
		if (elementCache.containsKey(key))
			return elementCache.get(key);
		ModelBase model = internalGetElementById(id, filter);
		elementCache.put(key, model);
		return model;
	}

	private ModelBase internalGetElementById(String id, Set<ModelType> filter) {
		for (Section section : new ArrayList<>(sections.values())) {
			if (!section.getChildType().matches(filter))
				continue;
			ModelBase element = section.getElementById(id, filter);
			if (element != null)
				return element;
		}
		if (includes == null)
			return null;
		for (Object include : includes) {
			ModelBase element = ((Document) include).getElementById(id, filter);
			if (element != null)
				return element;
		}
		return null;
	}

	protected void notifyModelChanged() {
		if (listeners == null)
			return;
		Object[] objects = listeners.getListeners();
		for (Object object : objects) {
			final IModelListener listener = (IModelListener) object;
			SafeRunner.run(new NonUISafeRunnable() {
				@Override
				public void run() throws Exception {
					listener.modelChanged(Document.this);
				}
			});
		}
	}

	public void addModelListener(IModelListener listener) {
		if (listeners == null)
			listeners = new ListenerList<>();
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
