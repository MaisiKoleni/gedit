/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

public class GrammarTextViewerCreator implements IViewerCreator {

	public Viewer createViewer(Composite parent, CompareConfiguration config) {
		return new GrammarTextViewer(parent);
	}

}
