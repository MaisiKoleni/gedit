/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.core.filebuffers.IDocumentFactory;
import org.eclipse.jface.text.IDocument;

public class GrammarDocumentFactory implements IDocumentFactory {

	public IDocument createDocument() {
		return new GrammarDocument();
	}

}
