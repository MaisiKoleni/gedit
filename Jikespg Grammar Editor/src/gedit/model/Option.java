/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import gedit.GrammarEditorPlugin;

import org.eclipse.jface.resource.ImageDescriptor;

public class Option extends ModelBase {
	public Option(Object parent, String text) {
		super(parent, text);
	}
	
	public ModelType getType() {
		return ModelType.OPTION;
	}

	public ImageDescriptor getImageDescriptor(Object o) {
		return GrammarEditorPlugin.getImageDescriptor("icons/option.gif"); //$NON-NLS-1$
	}

}
