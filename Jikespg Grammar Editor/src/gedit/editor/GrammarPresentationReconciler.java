/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.PresentationReconciler;

public class GrammarPresentationReconciler extends PresentationReconciler {

	protected TextPresentation createPresentation(IRegion damage, IDocument document) {
		return super.createPresentation(damage, document);
	}
}
