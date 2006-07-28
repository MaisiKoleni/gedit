/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import java.util.BitSet;

import gedit.model.Alias;
import gedit.model.ModelBase;
import gedit.model.ModelType;
import gedit.model.Reference;
import gedit.model.Rhs;
import gedit.model.Rule;

import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.widgets.Shell;

public class ModelContentHover implements ITextHover, ITextHoverExtension {
	private ModelBase fModel;
	private BitSet fFilter = ModelType.REFERENCE.or(ModelType.ALIAS.or(ModelType.RULE));

	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {

		String label = null;
		if (fModel instanceof Reference)
			fModel = ((Reference) fModel).getReferer();
		if (fModel instanceof Alias)
			label = ((Alias) fModel).getRhs().getValue();
		if (label == null && !(fModel instanceof Rule))
			return null;
		if (label == null)
			label = fModel.getLabel();
			
		StringBuffer sb = new StringBuffer();
		sb.append(SimpleTextPresenter.BOLD);
		sb.append(label);
		sb.append(SimpleTextPresenter.BOLD);
		sb.append(SimpleTextPresenter.CR);
		if (fModel instanceof Rule) {
			Rhs[] rhses = ((Rule) fModel).getRhs();
			for (int i = 0; i < rhses.length; i++) {
				if (i > 0)
					sb.append(SimpleTextPresenter.CR);
				appendRhsText(rhses[i], sb);
			}
		}
		return sb.toString();
	}

	private void appendRhsText(Rhs rhs, StringBuffer sb) {
		sb.append(SimpleTextPresenter.INDENT);
		Reference[] references = rhs.getParts();
		for (int j = 0; j < references.length; j++) {
			sb.append(' ').append(references[j].getValue());
		}
	}

	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		if (!(textViewer instanceof GrammarSourceViewer))
			return null;
		GrammarSourceViewer viewer = (GrammarSourceViewer) textViewer;
		ModelBase model = viewer.getModel(false).getElementAt(offset);
		fModel = model != null && model.getType().matches(fFilter) ? model : null;
		
		return fModel != null ? new Region(offset, 0) : null;
	}

	public IInformationControlCreator getHoverControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new GrammarInformationControl(parent, new SimpleTextPresenter(),
						fModel != null ? fModel.getDocument() : null);
			}
		};
	}

}
