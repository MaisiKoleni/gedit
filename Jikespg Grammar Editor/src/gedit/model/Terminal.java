/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import gedit.GrammarEditorPlugin;

import org.eclipse.jface.resource.ImageDescriptor;

public class Terminal extends ModelBase {
	public Terminal(Object parent, String value) {
		super(parent, value);
	}
	
	public String getValue() {
		return label;
	}
	
	public ModelType getType() {
		return ModelType.TERMINAL;
	}
	
	public ImageDescriptor getImageDescriptor(Object o) {
		return GrammarEditorPlugin.getImageDescriptor("icons/terminal.gif"); //$NON-NLS-1$
	}
	
}
