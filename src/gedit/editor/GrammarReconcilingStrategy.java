/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

import gedit.GrammarEditorPlugin;
import gedit.model.IProblemRequestor;

public class GrammarReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {
	private IDocument fDocument;
	private ISourceViewer fSourceViewer;
	private IReconcilingListener fReconcilingListener;

	public GrammarReconcilingStrategy(ISourceViewer sourceViewer, IReconcilingListener reconcilingListener) {
		fSourceViewer = sourceViewer;
		fReconcilingListener = reconcilingListener;
	}

	@Override
	public void setDocument(IDocument document) {
		fDocument = document;
	}

	@Override
	public void initialReconcile() {
		reconcile();
	}

	@Override
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		reconcile();
	}

	@Override
	public void reconcile(IRegion partition) {
		reconcile();
	}

	@Override
	public void setProgressMonitor(IProgressMonitor monitor) {
	}

	private void reconcile() {
		IAnnotationModel annotationModel = fSourceViewer.getAnnotationModel();
		IProblemRequestor probemRequestor = annotationModel instanceof IProblemRequestor ? (IProblemRequestor) annotationModel : null;
		GrammarEditorPlugin.getDocumentModel(fDocument, probemRequestor, true);
		if (fReconcilingListener != null)
			fReconcilingListener.reconciled();
	}

}
