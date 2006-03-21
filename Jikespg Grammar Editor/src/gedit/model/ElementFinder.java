/*
 * (c) Copyright 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.util.SortedMap;
import java.util.TreeMap;

public class ElementFinder {
	static class OffsetFinder extends NodeVisitor {
		private SortedMap result;
		private int offset;
		public OffsetFinder(Document document, int offset) {
			super(document);
			this.offset = offset;
			result = new TreeMap();
		}
		
		public boolean visit(Node node) {
			ModelBase element = element = getElement(node);
			if (offset >= node.offset && offset <= node.offset + node.length)
				result.put(new Integer(node.length), element);

			return super.visit(node);
		}

		public ModelBase getNearest() {
			return !result.isEmpty() ? (ModelBase) result.get(result.firstKey()) : null;
		}
	};

	static class IdFinder extends NodeVisitor {
		private String id;
		private String trimmedId;
		private ModelBase firstHit;
		public IdFinder(Document document, String id, int filter) {
			super(document, filter);
			this.id = id;
			String s = DocumentAnalyzer.trimQuotes(id);
			if (!s.equals(id))
				trimmedId = s;
		}
		
		public ModelBase getFirstHit() {
			return firstHit;
		}

		protected boolean doVisit(Node node, ModelBase element) {
			if (firstHit != null)
				return false;
			if (matches(element.label)) {
				firstHit = element;
				return false;
			}
			return true;
		}
		
		private boolean matches(String text) {
			return text.equals(id) || text.equals(trimmedId)
				|| DocumentAnalyzer.trimQuotes(text).equals(trimmedId != null ? trimmedId : id);
		}

	};

	public static ModelBase findElementAt(ModelBase start, int offset) {
		Document document = start.getDocument();
		OffsetFinder finder = new OffsetFinder(document, offset);
		if (start.node != null)
			start.node.accept(finder);
		return finder.getNearest();
	}

	public static ModelBase findElement(ModelBase start, String id, int filter) {
		Document document = start.getDocument();
		IdFinder finder = new IdFinder(document, id, filter);
		Node node = start.node != null ? start.node : null;
		if (node.parent != null)
			node = node.parent;
		node.accept(finder);
		return finder.getFirstHit();
	}

}
