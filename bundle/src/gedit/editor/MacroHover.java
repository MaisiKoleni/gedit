/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.rules.RuleBasedScanner;

import gedit.model.Definition;
import gedit.model.ModelBase;
import gedit.model.ModelUtils;

public class MacroHover implements ITextHover, ITextHoverExtension {
	private RuleBasedScanner fMacroScanner;
	private ModelBase fModel;

	public MacroHover(RuleBasedScanner macroScanner) {
		fMacroScanner = macroScanner;
	}

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		if (!(fModel instanceof Definition))
			return null;
		String label = ((Definition) fModel).getName();
		String text = ((Definition) fModel).getValue();

		StringBuilder sb = new StringBuilder();
		sb.append(SimpleTextPresenter.BOLD);
		sb.append(label);
		sb.append(SimpleTextPresenter.BOLD);
		sb.append(SimpleTextPresenter.CR);
		sb.append(text);

		return sb.toString();
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		if (!(textViewer instanceof GrammarSourceViewer))
			return null;
		fModel = ModelUtils.lookupMacro(offset, (GrammarSourceViewer) textViewer, fMacroScanner, null);
		return fModel != null ? new Region(offset, 0) : null;
	}

	@Override
	public IInformationControlCreator getHoverControlCreator() {
		return parent -> new GrammarInformationControl(parent, new SimpleTextPresenter(),
				fModel != null ? fModel.getDocument() : null);
	}

}
