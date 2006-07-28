/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.model.IProblemRequestor;
import gedit.model.Problem;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.editors.text.ForwardingDocumentProvider;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

public class GrammarDocumentProvider extends TextFileDocumentProvider {
	private class AnnotationModel extends ResourceMarkerAnnotationModel implements IProblemRequestor {
		private int fMaximumProblemsReported;
		public AnnotationModel(IResource resource) {
			super(resource);
		}
		
		public void beginReporting() {
			Map annotations = getAnnotationMap();
			for (Iterator it = annotations.keySet().iterator(); it.hasNext();) {
				Annotation annotation = (Annotation) it.next();
				if (annotation instanceof ProblemAnnotation)
					it.remove();
			}
			fMaximumProblemsReported = GrammarEditorPlugin.getDefault().getPreferenceStore().getInt(
					PreferenceConstants.EDITOR_MAXIMUM_PROBLEMS_REPORTED);
		}

		public void accept(Problem problem) {
			if (getAnnotationMap().size() >= fMaximumProblemsReported)
				return;
			Annotation annotation = new ProblemAnnotation(problem);
			try {
				addAnnotation(annotation, new Position(problem.getOffset(), problem.getLength()), false);
			} catch (BadLocationException e) {
				addAnnotation(annotation, new Position(problem.getOffset(), problem.getLength()));
			}
		}

		public void endReporting() {
			fireModelChanged(new AnnotationModelEvent(this));
		}
	};
	
	public class ProblemAnnotation extends Annotation {
		private Problem fProblem;
		public ProblemAnnotation(Problem problem) {
			fProblem = problem;
			switch (problem.getType()) {
			default:
				setType(TYPE_UNKNOWN);
				break;
			case Problem.WARNING:
				setType(ANNOTATION_WARNING);
				break;
			case Problem.ERROR:
				setType(ANNOTATION_ERROR);
				break;
			}
			setText(problem.getMessage());
		}
		
		public Problem getProblem() {
			return fProblem;
		}
	};
	
	public static final String ANNOTATION_WARNING = "org.eclipse.ui.workbench.texteditor.warning";
	public static final String ANNOTATION_ERROR = "org.eclipse.ui.workbench.texteditor.error";
	public static final String ANNOTATION_TASK = "org.eclipse.ui.workbench.texteditor.task";
	public static final String ANNOTATION_BOOKMARK = "org.eclipse.ui.workbench.texteditor.bookmark";
	public static final String ANNOTATION_SEARCH_RESULT = "org.eclipse.search.results";
	public static final String ANNOTATION_OCCURRENCE = "org.eclipse.jdt.ui.occurrences";
	
	public GrammarDocumentProvider() {
		IDocumentProvider provider = new TextFileDocumentProvider();
		provider = new ForwardingDocumentProvider(GrammarDocumentSetupParticipant.GRAMMAR_PARTITION,
				new GrammarDocumentSetupParticipant(), provider);
		setParentDocumentProvider(provider);
	}
	
	protected IAnnotationModel createAnnotationModel(IFile file) {
		return new AnnotationModel(file);
	}
	
	public void connect(Object element) throws CoreException {
		super.connect(element);
		if (!(element instanceof GrammarFileEditorInput))
			return;
		IDocument document = getDocument(element);
		if (!(document instanceof GrammarDocument))
			return;
		((GrammarDocument) document).setParentDocument(((GrammarFileEditorInput) element).getParentDocument());
	}
}