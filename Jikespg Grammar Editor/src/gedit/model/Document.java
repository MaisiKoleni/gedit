/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import gedit.GrammarEditorPlugin;
import gedit.NonUISafeRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.util.ListenerList;

public class Document extends ModelBase implements IAdaptable {
	public interface IModelListener {
		public void modelChanged();
	}

	private static Comparator sectionComparator = new Comparator() {
		public int compare(Object o1, Object o2) {
			return indexOf(o1, SECTION_ORDER) - indexOf(o2, SECTION_ORDER);
		}
	};

	private SortedMap sections = new TreeMap(sectionComparator);
	private Map nodeToElements = new HashMap();
	private List problems;
	private ListenerList listeners;
	protected List startTokens;
	
	protected char escape = DEFAULT_ESCAPE;
	protected String blockb = DEFAULT_BLOCKB;
	protected String blocke = DEFAULT_BLOCKE;
	protected String hblockb = DEFAULT_HBLOCKB;
	protected String hblocke = DEFAULT_HBLOCKE;

	public final static char DEFAULT_ESCAPE = '%';
	public final static String DEFAULT_BLOCKB = "/.";
	public final static String DEFAULT_BLOCKE = "./";
	public final static String DEFAULT_HBLOCKB = "/:";
	public final static String DEFAULT_HBLOCKE = ":/";

	private static Map KEYWORDS;
	private static Definition[] PREDEFINED_DEFINITIONS;
	
	public static String getSectionLabel(Object type) {
		return (String) KEYWORDS.get(type);
	}

	public final static String[] getAvailableSectionLabels() {
		String[] visibleLabels = new String[SECTION_ORDER.length];
		for (int i = 0; i < visibleLabels.length; i++) {
			visibleLabels[i] = getSectionLabel(SECTION_ORDER[i]);
		}
		return visibleLabels;
	}

	private static int indexOf(Object object, Object[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == object)
				return i;
		}
		return -1;
	}

	static {
		Map keywords = new HashMap();
		keywords.put(ModelType.OPTION, "Options");
		keywords.put(ModelType.DEFINITION, "Define");
		keywords.put(ModelType.TERMINAL, "Terminals");
		keywords.put(ModelType.ALIAS, "Alias");
		keywords.put(ModelType.RULE, "Rules");
		keywords.put(ModelType.NAME, "Names");
		keywords.put(ModelType.START_TOK, "Start");
		keywords.put(ModelType.END_TOK, "End");
		keywords.put(ModelType.EMPTY_TOK, "empty");
		KEYWORDS = Collections.unmodifiableMap(keywords);
	}
	
	private final static Object[] SECTION_ORDER = {
		ModelType.OPTION, ModelType.DEFINITION, ModelType.TERMINAL, ModelType.ALIAS,
		ModelType.RULE, ModelType.NAME,	
	};
	
	public static Map getKeywords() {
		return KEYWORDS;
	}
	
	public Document() {
		super(null, "Root");
	}
	
	protected void reset() {
		if (problems != null)
			problems.clear();
		problems = null;
		sections.clear();
		nodeToElements.clear();
		node = null;
		escape = DEFAULT_ESCAPE;
		blockb = DEFAULT_BLOCKB;
		blocke = DEFAULT_BLOCKE;
		hblockb = DEFAULT_HBLOCKB;
		hblocke = DEFAULT_HBLOCKE;
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

	public Object[] getChildren(Object o) {
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
	
	public String[] getBlockBeginnings() {
		return new String[] { blockb, hblockb };
	}

	public String[] getBlockEnds() {
		return new String[] { blocke, hblocke };
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
		SortedMap result = new TreeMap(Collections.reverseOrder());
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

	public Rule[] getRules() {
		Section section = getSection(ModelType.RULE);
		return section != null ? (Rule[]) section.children : new Rule[0];
	}
	
	public Terminal[] getTerminals() {
		Section section = getSection(ModelType.TERMINAL);
		return section != null ? (Terminal[]) section.children : new Terminal[0];
	}

	public Alias[] getAliases() {
		Section section = getSection(ModelType.ALIAS);
		return section != null ? (Alias[]) section.children : new Alias[0];
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
			PREDEFINED_DEFINITIONS = DocumentAnalyzer.readPredefinedDefinitions();
		}
		return PREDEFINED_DEFINITIONS;
	}
	
	public char getEsape() {
		return escape;
	}

	public ModelBase getElementAt(int offset) {
		return getElementAt(offset, true);
	}

	public ModelBase getElementAt(int offset, boolean restrictToLeafs) {
		ModelBase element = ElementFinder.findElementAt(this, offset, restrictToLeafs);
		if (GrammarEditorPlugin.getDefault().isDebugging())
			System.out.println("Element at: " + offset + ": " + element + (element instanceof Reference ? " referrer: " + ((Reference) element).getReferer() : ""));
		return element;
	}

	public ModelBase getElementById(String id) {
		ModelBase element = doGet(getSection(ModelType.ALIAS), id, ModelType.ALIAS.getType());
		if (element instanceof Alias)
			id = ((Alias) element).getRhs().label;
		if (element != null)
			return element;
		element = doGet(getSection(ModelType.TERMINAL), id, ModelType.TERMINAL.getType());
		if (element != null)
			return element;
		return doGet(getSection(ModelType.RULE), id, ModelType.RULE.getType());
	}
	
	private ModelBase doGet(Section section, String id, int filter) {
		return section != null ? section.getElementById(id, filter) : null;
	}

	protected void notifyModelChanged() {
		if (listeners == null)
			return;
		Object[] objects = listeners.getListeners();
		for (int i = 0; i < objects.length; i++) {
			final IModelListener listener = (IModelListener) objects[i];
			Platform.run(new NonUISafeRunnable() {
				public void run() throws Exception {
					listener.modelChanged();
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

}
