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
	private static class DocumentPartitioner implements IDocumentPartitioner {
		private IDocumentPartitioner fPartitioner;
		public DocumentPartitioner(IDocumentPartitioner partitioner) {
			fPartitioner = partitioner;
		}

		@Override
		public ITypedRegion[] computePartitioning(int offset, int length) {
			return fPartitioner.computePartitioning(offset, length);
		}

		@Override
		public void connect(IDocument document) {
			if (document instanceof IDocumentExtension3)
				((IDocumentExtension3) document).setDocumentPartitioner(GrammarDocumentSetupParticipant.GRAMMAR_PARTITION, this);
			fPartitioner.connect(document);
		}

		@Override
		public void disconnect() {
			fPartitioner.disconnect();
		}

		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			fPartitioner.documentAboutToBeChanged(event);
		}

		@Override
		public boolean documentChanged(DocumentEvent event) {
			return fPartitioner.documentChanged(event);
		}

		@Override
		public String[] getLegalContentTypes() {
			return fPartitioner.getLegalContentTypes();
		}

		@Override
		public String getContentType(int offset) {
			return fPartitioner.getContentType(offset);
		}

		@Override
		public ITypedRegion getPartition(int offset) {
			return fPartitioner.getPartition(offset);
		}
	}

	public GrammarMergeViewer(Composite parent, int style, CompareConfiguration configuration) {
		super(parent, style, configuration);
	}

	@Override
	protected void configureTextViewer(TextViewer textViewer) {
		if (textViewer instanceof ISourceViewer) {
			((ISourceViewer) textViewer).configure(new GrammarSourceViewerConfiguration());
		}
	}

	@Override
	protected IDocumentPartitioner getDocumentPartitioner() {
		return new DocumentPartitioner(GrammarDocumentSetupParticipant.createDocumentPartitioner());
	}
}
