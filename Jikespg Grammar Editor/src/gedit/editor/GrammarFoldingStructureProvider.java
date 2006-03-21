/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.model.Document;
import gedit.model.ModelBase;
import gedit.model.ModelType;
import gedit.model.Node;
import gedit.model.NodeVisitor;
import gedit.model.Rule;
import gedit.model.Section;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

public class GrammarFoldingStructureProvider {
	private class RegionFinder extends NodeVisitor {
		private List fResult = new ArrayList();

		protected RegionFinder(Document document, int filter) {
			super(document, filter);
		}
		
		protected boolean doVisit(Node node, ModelBase element) {
			if (!node.spansMultipleNodes())
				return false;
			fResult.add(element);
			element.setUserData("node", node);
			return true;
		}

		public List getSections(Document document) {
			document.getRoot().accept(this);
			return fResult;
		}
		
	};
	
	private class FoldingOptions {
		FoldingOptions(IPreferenceStore store) {
			String[] sections = Document.getAvailableSectionLabels();
			fFoldSections = new HashMap();
			for (int i = 0; i < sections.length; i++) {
				String section = sections[i];
				fFoldSections.put(section.toLowerCase(), store.getBoolean(PreferenceConstants.EDITOR_FOLD_SECTIONS + section)
						? new Object() : null); 
			}
			fFoldRules = store.getBoolean(PreferenceConstants.EDITOR_FOLD_RULES);
		}
		boolean isEnabledFor(ModelBase node) {
			if (!(node instanceof Section))
				return false;
			String label = Document.getSectionLabel(((Section) node).getChildType()).toLowerCase();
			return  fFoldSections.get(label) != null;
		}
		Map fFoldSections;
		boolean fFoldRules;
	};

	private IDocument fDocument;
	private Map fPositionToElement = new HashMap();
	private GrammarEditor fEditor;

	public GrammarFoldingStructureProvider(GrammarEditor editor) {
		fEditor = editor;
	}

	public void setDocument(IDocument document) {
		fDocument = document;		
	}

	private void updateFoldingRegions(ProjectionAnnotationModel model,
			Set currentRegions, boolean initial, FoldingOptions options) {
		Annotation[] deletions = computeDifferences(model, currentRegions);

		Map additionsMap = new HashMap();
		for (Iterator it = currentRegions.iterator(); it.hasNext(); ) {
			Object position = it.next();
			ModelBase node = (ModelBase) fPositionToElement.get(position);
			boolean collapseRules = initial && node instanceof Rule && options.fFoldRules;
			boolean collapseSections = initial && options.isEnabledFor(node);
			boolean collapse = collapseRules || collapseSections;
			additionsMap.put(new ProjectionAnnotation(collapse), position);
		}

		if ((deletions.length != 0 || additionsMap.size() != 0)) {
			model.modifyAnnotations(deletions, additionsMap, new Annotation[] {});
		}
	}

	private Annotation[] computeDifferences(ProjectionAnnotationModel model, Set additions) {
		List deletions = new ArrayList();
		for (Iterator it = model.getAnnotationIterator(); it.hasNext(); ) {
			Object annotation = it.next();
			if (annotation instanceof ProjectionAnnotation) {
				Position position = model.getPosition((Annotation) annotation);
				if (additions.contains(position)) {
					additions.remove(position);
				} else {
					deletions.add(annotation);
				}
			}
		}
		return (Annotation[]) deletions.toArray(new Annotation[deletions.size()]);
	}

	public void updateFoldingRegions(Document document, boolean initial) {
		FoldingOptions options = new FoldingOptions(GrammarEditorPlugin.getDefault().getPreferenceStore());
		fPositionToElement = new HashMap();
		try {
			ProjectionAnnotationModel model = (ProjectionAnnotationModel) fEditor
					.getAdapter(ProjectionAnnotationModel.class);
			if (model == null)
				return;
			
			RegionFinder finder = new RegionFinder(document, ModelType.SECTION.getType() |
					ModelType.RULE.getType());
			List sections = finder.getSections(document);

			Set currentRegions = new HashSet();
			addFoldingRegions(currentRegions, sections);//Arrays.asList(document.getSections()));
			updateFoldingRegions(model, currentRegions, initial, options);
		} catch (BadLocationException ignore) {
		}
	}

	private void addFoldingRegions(Set regions, /*List children*/List sections) throws BadLocationException {

//		for (Iterator it = children.iterator(); it.hasNext(); ) {
		for (int i = 0; i < sections.size(); i++) {
			
//			ModelBase element = (ModelBase) it.next();
			ModelBase element = (ModelBase) sections.get(i);
			Node node = (Node) element.getUserData("node");
			int startLine = fDocument.getLineOfOffset(node.getOffset());//element.getOffset());
			int endLine = fDocument.getLineOfOffset(node.getOffset()//element.getOffset()
					+ node.getLength());//element.getLengthWithChildren());
			if (startLine < endLine) {
				int start = fDocument.getLineOffset(startLine);
				int end = fDocument.getLineOffset(endLine)
						+ fDocument.getLineLength(endLine);
				Position position = new Position(start, end - start);
				regions.add(position);
				fPositionToElement.put(position, element);
			}

//			addFoldingRegions(regions, new ArrayList(Arrays.asList(element.getChildren(element))));
		}
	}

}
