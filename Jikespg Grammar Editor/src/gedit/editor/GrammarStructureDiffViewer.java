/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.model.Document;
import gedit.model.ModelBase;
import gedit.model.ModelType;

import java.io.UnsupportedEncodingException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.IStructureCreator;
import org.eclipse.compare.structuremergeviewer.StructureDiffViewer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

public class GrammarStructureDiffViewer extends StructureDiffViewer {
	private class GrammarStructureCreator implements IStructureCreator {
		public String getName() {
			return "Grammar Structure Compare";
		}

		public IStructureComparator getStructure(Object input) {
			IDocument document = CompareUI.getDocument(input);
			if (document == null) {
				if (input instanceof IStreamContentAccessor) {
					IStreamContentAccessor sca = (IStreamContentAccessor) input;			
					String contents = GrammarTextViewer.getString(sca);
					if (contents != null) {
						int n = contents.length();
						char[] buffer = new char[n];
						contents.getChars(0, n, buffer, 0);
						
						document = new org.eclipse.jface.text.Document(contents);
						GrammarDocumentSetupParticipant.setupDocument(document);
					}
				}
			}
			Document model = GrammarEditorPlugin.getDocumentModel(document, null, true);
			return new GrammarNode(ModelType.DOCUMENT.getBitPosition(), document, model);
		}

		public IStructureComparator locate(Object path, Object input) {
			return null;
		}

		public String getContents(Object node, boolean ignoreWhitespace) {
			if (!(node instanceof IStreamContentAccessor))
				return null;
				
			IStreamContentAccessor sca = (IStreamContentAccessor) node;
			return GrammarTextViewer.getString(sca);
		}

		public void save(IStructureComparator node, Object input) {
			if (node instanceof GrammarNode && input instanceof IEditableContent) {
				IDocument document = ((GrammarNode) node).getDocument();
				IEditableContent bca = (IEditableContent) input;
				String contents = document.get();
				String charSet = null;
				if (input instanceof IEncodedStreamContentAccessor) {
					try {
						charSet = ((IEncodedStreamContentAccessor) input).getCharset();
					} catch (CoreException ignore) {
					}
				}
				if (charSet == null)
					charSet= ResourcesPlugin.getEncoding();
				byte[] bytes;				
				try {
					bytes = contents.getBytes(charSet);
				} catch (UnsupportedEncodingException e) {
					bytes = contents.getBytes();	
				}
				bca.setContent(bytes);
			}
		}

	};
	
	private class GrammarNode extends DocumentRangeNode implements ITypedElement {
		private ModelBase fModel;

		public GrammarNode(int typeCode, IDocument document, ModelBase model) {
			super(typeCode, typeCode + model.getLabel(), document, model.getOffset(), model.getLength());
			fModel = model;
			adaptRange();
		}
		
		private void adaptRange() {
			Position range = getRange();
			ModelType type = fModel.getType();
			if (type == ModelType.SECTION || type == ModelType.RULE) {
				range.offset = fModel.getRangeOffset();
				range.length = fModel.getRangeLength();
			}
		}
		
		public Object[] getChildren() {
			Object[] children = fModel.getChildren();
			if (children == null)
				return null;
			GrammarNode[] nodes = new GrammarNode[children.length];
			for (int i = 0; i < nodes.length; i++) {
				ModelBase model = (ModelBase) children[i];
				nodes[i] = new GrammarNode(model.getType().getBitPosition(), getDocument(), model);
			}
			return nodes;
		}

		public String getName() {
			return fModel.getLabel();
		}

		public Image getImage() {
			return fLabelProvider.getImage(fModel);
		}

		public String getType() {
			return "g";
		}
	};

	private ILabelProvider fLabelProvider = new ModelLabelProvider();
	public GrammarStructureDiffViewer(Composite parent, CompareConfiguration configuration) {
		super(parent, configuration);
		setStructureCreator(new GrammarStructureCreator());
	}

}
