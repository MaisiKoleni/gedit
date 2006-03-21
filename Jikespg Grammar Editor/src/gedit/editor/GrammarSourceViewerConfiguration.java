/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

public class GrammarSourceViewerConfiguration extends TextSourceViewerConfiguration {
	private class ProductionProvider implements IInformationProvider, IInformationProviderExtension {
		public IRegion getSubject(ITextViewer textViewer, int offset) {
			return new Region(offset, 0);
		}

		public String getInformation(ITextViewer textViewer, IRegion subject) {
			return null;
		}

		public Object getInformation2(ITextViewer textViewer, IRegion subject) {
			return ((GrammarSourceViewer) textViewer).getModel(false);
		}
	};

	private IReconcilingListener fReconcilingListener;
	private GrammarScanner fDefaultScanner;
	private GrammarScanner fMacroScanner;
	private NonRuleBasedDamagerRepairer fCommentDr;
	private NonRuleBasedDamagerRepairer fOptionDr;
	private NonRuleBasedDamagerRepairer fOperatorDr;
	private SemanticHighLighter fSemanticHighlighter;
	private PreferenceUtils fUtils;
	private ModelContentHover fModelContentHover;
	private AnnotationHover fAnnotationHover;
	private boolean fUseReconciling;

	public GrammarSourceViewerConfiguration() {
		this(GrammarEditorPlugin.getColorManager(), GrammarEditorPlugin.getDefault().getCombinedPreferenceStore(), null);
	}

	public GrammarSourceViewerConfiguration(ColorManager colorManager, IPreferenceStore store, IReconcilingListener reconcilingListener) {
		super(store);
		fReconcilingListener = reconcilingListener;
		fUtils = new PreferenceUtils(colorManager, store);
		if (reconcilingListener != null)
			fUseReconciling = true;
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			GrammarPartitionScanner.GRAMMAR_COMMENT,
			GrammarPartitionScanner.GRAMMAR_OPTION,
			GrammarPartitionScanner.GRAMMAR_MACRO,
			GrammarPartitionScanner.GRAMMAR_OPERATOR,
			GrammarPartitionScanner.GRAMMAR_STRING,
		};
	}
	
	public IInformationPresenter getOutlinePresenter(final GrammarSourceViewer viewer) {
		InformationPresenter presenter = new InformationPresenter(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new OutlineInformationControl(viewer, parent);
			}
		});
		ProductionProvider provider = new ProductionProvider();
		presenter.setDocumentPartitioning(GrammarDocumentSetupParticipant.GRAMMAR_PARTITION);
		presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
		presenter.setInformationProvider(provider, GrammarPartitionScanner.GRAMMAR_COMMENT);
		presenter.setInformationProvider(provider, GrammarPartitionScanner.GRAMMAR_MACRO);
		presenter.setInformationProvider(provider, GrammarPartitionScanner.GRAMMAR_OPTION);
		presenter.setInformationProvider(provider, GrammarPartitionScanner.GRAMMAR_OPERATOR);
		presenter.setInformationProvider(provider, GrammarPartitionScanner.GRAMMAR_STRING);
		presenter.setSizeConstraints(40, 20, true, false);
		presenter.setRestoreInformationControlBounds(getSettingsSection("outlinePresenterBounds"), true, true);
		return presenter;
	}
	
	private IDialogSettings getSettingsSection(String sectionName) {
		IDialogSettings settings = GrammarEditorPlugin.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(sectionName);
		return section != null ? section : settings.addNewSection(sectionName);
	}
	
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant fContentAssistant = new ContentAssistant();
		GrammarCompletionProcessor processor = new GrammarCompletionProcessor();
		fContentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		fContentAssistant.setContentAssistProcessor(processor, GrammarPartitionScanner.GRAMMAR_STRING);
		fContentAssistant.setContentAssistProcessor(processor, GrammarPartitionScanner.GRAMMAR_MACRO);
		fContentAssistant.setDocumentPartitioning(GrammarDocumentSetupParticipant.GRAMMAR_PARTITION);
		fContentAssistant.setAutoActivationDelay(500);
		fContentAssistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
		fContentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);

		fContentAssistant.setInformationControlCreator(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent);
			}
		});
		return fContentAssistant;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new GrammarPresentationReconciler();
		reconciler.setDocumentPartitioning(GrammarDocumentSetupParticipant.GRAMMAR_PARTITION);
		
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getDefaultScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		fCommentDr = new NonRuleBasedDamagerRepairer(PreferenceConstants.GRAMMAR_COLORING_COMMENT, fUtils);
		reconciler.setDamager(fCommentDr, GrammarPartitionScanner.GRAMMAR_COMMENT);
		reconciler.setRepairer(fCommentDr, GrammarPartitionScanner.GRAMMAR_COMMENT);

		fOptionDr = new NonRuleBasedDamagerRepairer(PreferenceConstants.GRAMMAR_COLORING_OPTION, fUtils);
		reconciler.setDamager(fOptionDr, GrammarPartitionScanner.GRAMMAR_OPTION);
		reconciler.setRepairer(fOptionDr, GrammarPartitionScanner.GRAMMAR_OPTION);

		dr = new DefaultDamagerRepairer(getMacroScanner());
		reconciler.setDamager(dr, GrammarPartitionScanner.GRAMMAR_MACRO);
		reconciler.setRepairer(dr, GrammarPartitionScanner.GRAMMAR_MACRO);

		fOperatorDr = new NonRuleBasedDamagerRepairer(PreferenceConstants.GRAMMAR_COLORING_OPERATOR, fUtils);
		reconciler.setDamager(fOperatorDr, GrammarPartitionScanner.GRAMMAR_OPERATOR);
		reconciler.setRepairer(fOperatorDr, GrammarPartitionScanner.GRAMMAR_OPERATOR);

		return reconciler;
	}
	
	public SemanticHighLighter getSemanticHighlighter(GrammarSourceViewer viewer) {
		if (fSemanticHighlighter == null) {
			fSemanticHighlighter = new SemanticHighLighter(viewer, fUtils);
			if (!fUseReconciling)
				viewer.addTextPresentationListener(fSemanticHighlighter);
		}

		return fSemanticHighlighter;
	}
	
	public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] { "--", "" }; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public int[] getConfiguredTextHoverStateMasks(ISourceViewer sourceViewer, String contentType) {
		return new int[] { SWT.CTRL, 0 };
	}
	
	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, new SimpleTextPresenter());
			}
		};
	}
	
	public IInformationPresenter getInformationPresenter(final ISourceViewer sourceViewer) {
		InformationPresenter presenter = new InformationPresenter(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new GrammarInformationControl(parent, SWT.TOOL | SWT.RESIZE,
						SWT.V_SCROLL | SWT.H_SCROLL, new SimpleTextPresenter());
			}
		});
		presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		IInformationProvider provider = new GrammarInformationProvider(new ITextHover[] {
				getAnnotationHover(), getModelContentHover(), 	
		});
		presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
		presenter.setInformationProvider(provider, GrammarPartitionScanner.GRAMMAR_STRING);
		presenter.setSizeConstraints(60, 10, true, true);
		return presenter;
		
	}
	
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return getAnnotationHover();
	}
	
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
		if ((SWT.CTRL & stateMask) > 0) {
			return getModelContentHover();
		}
		return getAnnotationHover();
	}
	
	private ModelContentHover getModelContentHover() {
		if (fModelContentHover == null)
			fModelContentHover = new ModelContentHover();
		return fModelContentHover;
	}

	private AnnotationHover getAnnotationHover() {
		if (fAnnotationHover == null) {
			fAnnotationHover = new AnnotationHover();
		}
		return fAnnotationHover;
	}

	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		if (!fUseReconciling)
			return null;
		IReconcilingListener reconcilingListener = fReconcilingListener;
		if (reconcilingListener == null && sourceViewer instanceof IReconcilingListener)
			reconcilingListener = (IReconcilingListener) sourceViewer;
		return new MonoReconciler(new GrammarReconcilingStrategy(sourceViewer, reconcilingListener), false);
	}
	
	public void setUseReconciling(boolean useReconciling) {
		fUseReconciling = useReconciling;
	}
	
	private ITokenScanner getDefaultScanner() {
		if (fDefaultScanner == null)
			fDefaultScanner = new GrammarScanner(null, fUtils);
		return fDefaultScanner;
	}

	private ITokenScanner getMacroScanner() {
		if (fMacroScanner == null)
			fMacroScanner = new GrammarScanner(PreferenceConstants.GRAMMAR_COLORING_MACRO, fUtils);
		return fMacroScanner;
	}

	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return GrammarDocumentSetupParticipant.GRAMMAR_PARTITION;
	}
	
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (fDefaultScanner == null)
			return;
		fDefaultScanner.adaptToPreferenceChange(event);
		fMacroScanner.adaptToPreferenceChange(event);
		fCommentDr.adaptToPreferenceChange(event);
		fOptionDr.adaptToPreferenceChange(event);
		fOperatorDr.adaptToPreferenceChange(event);
		fSemanticHighlighter.adaptToPreferenceChange(event);
	}

	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		return fMacroScanner.affectsBehavior(event)
				|| fDefaultScanner.affectsBehavior(event)
				|| fCommentDr.affectsTextPresentation(event)
				|| fOperatorDr.affectsTextPresentation(event)
				|| fOptionDr.affectsTextPresentation(event)
				|| fSemanticHighlighter.affectsTextPresentation(event);
	}

}