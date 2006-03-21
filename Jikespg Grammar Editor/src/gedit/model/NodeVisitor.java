/*
 * (c) Copyright 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

public abstract class NodeVisitor implements INodeVisitor {
	protected Document document;
	protected int filter;
	
	protected NodeVisitor(Document document) {
		this(document, 0);
	}

	protected NodeVisitor(Document document, int filter) {
		this.document = document;
		this.filter = filter;
	}
	
	public boolean visit(Node node) {
		if (filter == 0)
			return true;
		ModelBase element = getElement(node);
		if (element != null && (element.getType().getType() & filter) > 0)
			return doVisit(node, element);
		return true;
	}
	
	protected boolean doVisit(Node node, ModelBase element) {
		return true;
	}
	
	protected ModelBase getElement(Node node) {
		return document.getElementForNode(node);
	}
}
