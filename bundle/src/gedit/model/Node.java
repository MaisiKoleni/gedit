/*
 * (c) Copyright 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Node {
	protected int offset;
	protected int length;
	protected Node parent;
	private List<Node> children;

	public Node(Node parent, int offset, int length) {
		this.offset = offset;
		this.length = length;
		this.parent = parent;
		if (parent != null)
			parent.addChild(this);
	}

	private void addChild(Node node) {
		if (children == null)
			children = new ArrayList<>(8); // average for Java grammar is 4.1, so 8 seems like a good choice
		children.add(node);
	}

	public void accept(INodeVisitor visitor) {
		if (!visitor.visit(this))
			return;
		acceptMultiple(getChildren(), visitor);
	}

	protected static void acceptMultiple(List<Node> nodes, INodeVisitor visitor) {
		if (nodes == null)
			return;
		for (Node node : nodes) {
			node.accept(visitor);
		}
	}

	public boolean spansMultipleNodes() {
		return children != null && !children.isEmpty();
	}

	protected List<Node> getChildren() {
		return Objects.requireNonNullElse(children, List.of());
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

	@Override
	public String toString() {
		return "node@" + hashCode() + "(" + offset + "," + length + ")";
	}
}
