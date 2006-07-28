/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor.actions;

import gedit.editor.GrammarDocumentProvider;
import gedit.editor.GrammarEditor;
import gedit.model.ModelBase;
import gedit.model.Node;

import java.util.Iterator;
import java.util.ResourceBundle;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public class FindOccurrencesInFileAction extends TextEditorAction {
	public FindOccurrencesInFileAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
	}

	public final void run() {
		ITextEditor textEditor = getTextEditor();
		if (!(textEditor instanceof GrammarEditor))
			return;
		GrammarEditor editor = (GrammarEditor) textEditor;
		ModelBase element = editor.getSelectedElement();
		Node[] occurrences = null;
		if (element != null)
			occurrences = editor.findOccurrences(element.getLabel());
		IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		if (annotationModel == null)
			return;
		for (Iterator it = annotationModel.getAnnotationIterator(); it.hasNext(); ) {
			Annotation annotation = (Annotation) it.next();
			if (GrammarDocumentProvider.ANNOTATION_SEARCH_RESULT.equals(annotation.getType()))
				annotationModel.removeAnnotation(annotation);
		}
		if (occurrences == null)
			return;
		for (int i = 0; i < occurrences.length; i++) {
			Node occurrence = occurrences[i];
			annotationModel.addAnnotation(new Annotation(GrammarDocumentProvider.ANNOTATION_SEARCH_RESULT,
					false, element.getLabel()), new Position(occurrence.getOffset(), occurrence.getLength()));
		}
	}

}
