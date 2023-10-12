/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProvider;

public class GrammarInformationProvider implements IInformationProvider {
	private ITextHover[] fHovers;

	public GrammarInformationProvider(ITextHover[] hovers) {
		fHovers = hovers;
	}

	@Override
	public IRegion getSubject(ITextViewer textViewer, int offset) {
		for (ITextHover fHover : fHovers) {
			IRegion region = fHover.getHoverRegion(textViewer, offset);
			if (region != null)
				return region;
		}
		return null;
	}

	@Override
	public String getInformation(ITextViewer textViewer, IRegion subject) {
		for (ITextHover fHover : fHovers) {
			String information = fHover.getHoverInfo(textViewer, subject);
			if (information != null)
				return information;
		}
		return null;
	}

}
