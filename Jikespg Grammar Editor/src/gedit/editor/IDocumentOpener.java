/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.IRegion;

import gedit.model.Document;

public interface IDocumentOpener {

	void open(Document document, Document parentDocument, IRegion selectedRegion);
}
