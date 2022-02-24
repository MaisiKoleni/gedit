/*
 * (c) Copyright 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.util.Set;

public abstract class NodeVisitor implements INodeVisitor {
	protected Document document;
	protected Set<ModelType> filter;

	protected NodeVisitor(Document document) {
		this(document, null);
	}

	protected NodeVisitor(Document document, Set<ModelType> filter) {
		this.document = document;
		this.filter = filter;
	}

	@Override
	public boolean visit(Node node) {
		if (filter == null)
			return true;
		ModelBase element = getElement(node);
		if (element != null && element.getType().matches(filter))
			return doVisit(node, element);
		return true;
	}

	@SuppressWarnings("unused")
	protected boolean doVisit(Node node, ModelBase element) {
		return true;
	}

	protected ModelBase getElement(Node node) {
		return document.getElementForNode(node);
	}
}
