/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import java.util.ArrayList;
import java.util.EnumSet;
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

import gedit.GrammarEditorPlugin;
import gedit.model.Document;
import gedit.model.ModelBase;
import gedit.model.ModelType;
import gedit.model.ModelUtils;
import gedit.model.Node;
import gedit.model.NodeVisitor;
import gedit.model.Section;
import gedit.model.UserData;

public class GrammarFoldingStructureProvider {
	private static class RegionFinder extends NodeVisitor {
		private List<ModelBase> fResult = new ArrayList<>();

		protected RegionFinder(Document document, Set<ModelType> filter) {
			super(document, filter);
		}

		@Override
		protected boolean doVisit(Node node, ModelBase element) {
			if (!node.spansMultipleNodes())
				return false;
			fResult.add(element);
			element.setUserData(UserData.NODE, node);
			return true;
		}

		public List<ModelBase> getElements(Document document) {
			document.getRoot().accept(this);
			return fResult;
		}

	}

	private static class FoldingOptions {
		FoldingOptions(IPreferenceStore store) {
			ModelType[] allTypes = ModelType.values();
			fFoldSections = new HashMap<>();
			Set<ModelType> values = ModelUtils.createModelTypeSetFromString(store.getString(PreferenceConstants.EDITOR_FOLD_SECTIONS), PreferenceConstants.EDITOR_FOLDING_SEPARATOR);
			for (ModelType type : allTypes) {
				String section = type.getString();
				fFoldSections.put(section.toLowerCase(), values.contains(type)
						? new Object() : null);
			}
			fFoldRules = store.getBoolean(PreferenceConstants.EDITOR_FOLD_RULES);
			fFoldMacros = store.getBoolean(PreferenceConstants.EDITOR_FOLD_MACROS);
			fFoldComments = store.getBoolean(PreferenceConstants.EDITOR_FOLD_COMMENTS);
		}
		boolean isEnabledFor(ModelBase node) {
			if (!(node instanceof Section))
				return false;
			String label = ((Section) node).getChildType().getString().toLowerCase();
			return  fFoldSections.get(label) != null;
		}
		Map<String, Object> fFoldSections;
		boolean fFoldRules;
		boolean fFoldMacros;
		boolean fFoldComments;
	}

	private IDocument fDocument;
	private Map<Position, ModelBase> fPositionToElement = new HashMap<>();
	private GrammarEditor fEditor;

	public GrammarFoldingStructureProvider(GrammarEditor editor) {
		fEditor = editor;
	}

	public void setDocument(IDocument document) {
		fDocument = document;
	}

	private void updateFoldingRegions(ProjectionAnnotationModel model,
			Set<Position> currentRegions, boolean initial, FoldingOptions options) {
		Annotation[] deletions = computeDifferences(model, currentRegions);

		Map<ProjectionAnnotation, Position> additionsMap = new HashMap<>();
		for (Position position : currentRegions) {
			ModelBase node = fPositionToElement.get(position);
			ModelType type = node.getType();
			boolean collapseRules = initial && type == ModelType.RULE && options.fFoldRules;
			boolean collapseMacros = initial && type == ModelType.MACRO_BLOCK && options.fFoldMacros;
			boolean collapseComments = initial && type == ModelType.COMMENT && options.fFoldComments;
			boolean collapseSections = initial && options.isEnabledFor(node);
			boolean collapse = collapseRules || collapseMacros || collapseComments || collapseSections;
			additionsMap.put(new ProjectionAnnotation(collapse), position);
		}

		if (deletions.length != 0 || additionsMap.size() != 0) {
			model.modifyAnnotations(deletions, additionsMap, new Annotation[] {});
		}
	}

	private Annotation[] computeDifferences(ProjectionAnnotationModel model, Set<Position> additions) {
		List<Object> deletions = new ArrayList<>();
		for (Iterator<?> it = model.getAnnotationIterator(); it.hasNext(); ) {
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
		return deletions.toArray(new Annotation[deletions.size()]);
	}

	public void updateFoldingRegions(Document document, boolean initial) {
		FoldingOptions options = new FoldingOptions(GrammarEditorPlugin.getDefault().getPreferenceStore());
		fPositionToElement = new HashMap<>();
		try {
			ProjectionAnnotationModel model = fEditor
					.getAdapter(ProjectionAnnotationModel.class);
			if (model == null)
				return;

			Set<ModelType> filter = EnumSet.of(ModelType.SECTION, ModelType.RULE,
					ModelType.COMMENT, ModelType.MACRO_BLOCK);

			RegionFinder finder = new RegionFinder(document, filter);
			List<ModelBase> elements = finder.getElements(document);

			Set<Position> currentRegions = new HashSet<>();
			addFoldingRegions(currentRegions, elements);
			updateFoldingRegions(model, currentRegions, initial, options);
		} catch (BadLocationException ignore) {
		}
	}

	private void addFoldingRegions(Set<Position> regions, List<ModelBase> children) throws BadLocationException {

		for (ModelBase element : children) {

			Node node = (Node) element.getUserData(UserData.NODE);
			if (node == null)
				continue;
			int startLine = fDocument.getLineOfOffset(node.getOffset());
			int endLine = fDocument.getLineOfOffset(node.getOffset() + node.getLength());
			if (startLine < endLine) {
				int start = fDocument.getLineOffset(startLine);
				int end = fDocument.getLineOffset(endLine) + fDocument.getLineLength(endLine);
				Position position = new Position(start, end - start);
				regions.add(position);
				fPositionToElement.put(position, element);
			}
		}
	}

}
