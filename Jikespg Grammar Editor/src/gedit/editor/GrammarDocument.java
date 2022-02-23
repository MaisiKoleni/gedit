/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.model.Document;

public class GrammarDocument extends org.eclipse.jface.text.Document {
	private Document fParentDocument;

	public GrammarDocument() {
	}

	public GrammarDocument(String initialContent) {
		super(initialContent);
	}

	public GrammarDocument(String initialContent, Document parentDocument) {
		this(initialContent);
		fParentDocument = parentDocument;
	}

	public Document getParentDocument() {
		return fParentDocument;
	}

	public void setParentDocument(Document parentDocument) {
		fParentDocument = parentDocument;
	}
}
