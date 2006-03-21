/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;

public class GrammarMergeViewer extends TextMergeViewer {
	private class DocumentPartitioner implements IDocumentPartitioner { 
		private IDocumentPartitioner fPartitioner;
		public DocumentPartitioner(IDocumentPartitioner partitioner) {
			fPartitioner = partitioner;
		}

		public ITypedRegion[] computePartitioning(int offset, int length) {
			return fPartitioner.computePartitioning(offset, length);
		}
		
		public void connect(IDocument document) {
			if (document instanceof IDocumentExtension3)
				((IDocumentExtension3) document).setDocumentPartitioner(GrammarDocumentSetupParticipant.GRAMMAR_PARTITION, this);
			fPartitioner.connect(document);
		}
		
		public void disconnect() {
			fPartitioner.disconnect();
		}

		public void documentAboutToBeChanged(DocumentEvent event) {
			fPartitioner.documentAboutToBeChanged(event);
		}

		public boolean documentChanged(DocumentEvent event) {
			return fPartitioner.documentChanged(event);
		}

		public String[] getLegalContentTypes() {
			return fPartitioner.getLegalContentTypes();
		}

		public String getContentType(int offset) {
			return fPartitioner.getContentType(offset);
		}

		public ITypedRegion getPartition(int offset) {
			return fPartitioner.getPartition(offset);
		}
	};
	
	public GrammarMergeViewer(Composite parent, int style, CompareConfiguration configuration) {
		super(parent, style, configuration);
	}

	protected void configureTextViewer(TextViewer textViewer) {
		if (textViewer instanceof ISourceViewer) {
			((ISourceViewer) textViewer).configure(new GrammarSourceViewerConfiguration());
		}
	}

	protected IDocumentPartitioner getDocumentPartitioner() {
		IDocumentPartitioner partitioner = new DocumentPartitioner(GrammarDocumentSetupParticipant.createDocumentPartitioner());
		return partitioner;
	}
}
