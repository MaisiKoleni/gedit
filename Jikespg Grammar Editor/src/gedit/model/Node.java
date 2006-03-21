/*
 * (c) Copyright 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.util.ArrayList;
import java.util.List;

public class Node {
	protected int offset;
	protected int length;
	protected Node parent;
	private List children;

	public Node(Node parent, int offset, int length) {
		this.offset = offset;
		this.length = length;
		this.parent = parent;
		if (parent != null) 
			parent.addChild(this);
	}
	
	private void addChild(Node node) {
		if (children == null)
			children = new ArrayList(3);
		children.add(node);
	}

	public void accept(INodeVisitor visitor) {
		if (!visitor.visit(this))
			return;
		acceptArray(getChildren(), visitor);
	}

	protected void acceptArray(Node[] nodes, INodeVisitor visitor) {
		if (nodes == null)
			return;
		for (int i = 0; i < nodes.length; i++) {
			nodes[i].accept(visitor);
		}
	}
	
	public boolean spansMultipleNodes() {
		return children != null && children.size() > 0;
	}
	
	protected Node[] getChildren() {
		return children != null ? (Node[]) children.toArray(new Node[children.size()]) : null;
	}

	public int getOffset() {
		return offset;
	}
	
	public int getLength() {
		return length;
	}
	
	public String toString() {
		return "node@" + hashCode() + "(" + offset + "," + length + ")";
	}
}
