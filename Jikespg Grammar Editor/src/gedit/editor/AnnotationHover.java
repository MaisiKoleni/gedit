/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class AnnotationHover implements IAnnotationHover, ITextHover {
	private List fAnnotations = new ArrayList(1);
	
	protected final static Map SHOW_IN_OVERVIEW;
	protected final static Map SHOW_IN_TEXT;
	
	static {
		Map map = new HashMap();
		map.put(GrammarDocumentProvider.ANNOTATION_BOOKMARK, null);
		map.put(GrammarDocumentProvider.ANNOTATION_ERROR, null);
		map.put(GrammarDocumentProvider.ANNOTATION_TASK, null);
		map.put(GrammarDocumentProvider.ANNOTATION_WARNING, null);
		SHOW_IN_TEXT = Collections.unmodifiableMap(new HashMap(map));
		map.put(GrammarDocumentProvider.ANNOTATION_SEARCH_RESULT, null);
		map.put(GrammarDocumentProvider.ANNOTATION_OCCURRENCE, null);
		SHOW_IN_OVERVIEW = Collections.unmodifiableMap(map);
	}

	protected void findAnnotations(IAnnotationModel model, IRegion region) {
		fAnnotations.clear();
		if (model == null) {
			IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
			if (part instanceof ITextEditor) {
				ITextEditor editor = (ITextEditor) part;
				model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
			}
		}
		if (model == null)
			return;
		for (Iterator it = model.getAnnotationIterator(); it.hasNext();) {
			Annotation annotation = (Annotation) it.next();
			Position position = model.getPosition(annotation);
			if (position == null)
				continue;
			if (position.overlapsWith(region.getOffset(), region.getLength()))
				fAnnotations.add(annotation);
		}
	}

	protected String getHoverText(IAnnotationModel model, IRegion region, Map shownTypes) {

		if (fAnnotations.size() == 0)
			findAnnotations(model, region);

		StringBuffer text = null;
		Annotation previousAnnotation = null;
		Map posMessages = new HashMap();
		for (int i = 0; i < fAnnotations.size(); i++) {
			Annotation annotation = (Annotation) fAnnotations.get(i);
			if (!shownTypes.containsKey(annotation.getType()))
				continue;
			String msg = annotation.getText();
			if (msg == null)
				continue;
			Position position = model.getPosition(annotation);
			if (isDuplicateAt(position, msg, posMessages))
				continue;
			if (text == null) {
				text = new StringBuffer(msg);
				previousAnnotation = annotation;				
			} else {
				if (previousAnnotation != null) {
					String prev = text.toString();
					text.replace(0, text.length(), "More than one marker at the line");
					appendMultiline(text, prev, previousAnnotation.getType());
					previousAnnotation = null;
				}
				appendMultiline(text, msg, annotation.getType());
			}
		}
		fAnnotations.clear();
		return text != null ? text.toString() : null;
	}

	private void appendMultiline(StringBuffer text, String msg, String type) {
		text.append(SimpleTextPresenter.CR);
		text.append(SimpleTextPresenter.INDENT).append(SimpleTextPresenter.BULLET);
		text.append(" [").append(mapType(type)).append("] ").append(msg);
	}

	private String mapType(String type) {
		if (GrammarDocumentProvider.ANNOTATION_ERROR.equals(type))
			return "Error";
		if (GrammarDocumentProvider.ANNOTATION_WARNING.equals(type))
			return "Warning";
		if (GrammarDocumentProvider.ANNOTATION_BOOKMARK.equals(type))
			return "Bookmark";
		if (GrammarDocumentProvider.ANNOTATION_TASK.equals(type))
			return "Task";
		if (GrammarDocumentProvider.ANNOTATION_SEARCH_RESULT.equals(type))
			return "Search";
		if (GrammarDocumentProvider.ANNOTATION_OCCURRENCE.equals(type))
			return "Occurrence";
		return null;
	}

	private boolean isDuplicateAt(Position position, String text, Map annotations) {
		if (annotations.containsKey(position)) {
			Object object = annotations.get(position);
			if (text.equals(object))
				return true;
			if (object instanceof List) {
				List list = (List) object;
				if (list.contains(text))
					return true;
				else
					list.add(text);
			} else {
				List list = new ArrayList();
				list.add(object);
				list.add(text);
				annotations.put(position, list);
			}
		} else {
			annotations.put(position, text);
		}
		return false;
	}

	public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
		IDocument document = sourceViewer.getDocument();
		IRegion region = null;
		try {
			region = document.getLineInformation(lineNumber);
		} catch (BadLocationException ignore) {
			return null;
		}
		return getHoverText(sourceViewer.getAnnotationModel(), region, SHOW_IN_OVERVIEW);
	}

	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		IAnnotationModel model = textViewer instanceof ISourceViewer ? ((ISourceViewer) textViewer).getAnnotationModel() : null;
		return getHoverText(model, hoverRegion, SHOW_IN_TEXT); 
	}

	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		IAnnotationModel model = textViewer instanceof ISourceViewer ? ((ISourceViewer) textViewer).
				getAnnotationModel() : null;
		findAnnotations(model, new Region(offset, 0));
		for (int i = 0; i < fAnnotations.size(); i++) {
			Annotation annotation = (Annotation) fAnnotations.get(i);
			if (!SHOW_IN_TEXT.containsKey(annotation.getType()))
				continue;
			Position position = model.getPosition(annotation);
			if (position.includes(offset))
				return new Region(offset, 0);
		}
		return null;
	}

}
