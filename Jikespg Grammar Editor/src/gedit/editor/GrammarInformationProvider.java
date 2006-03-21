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

	public IRegion getSubject(ITextViewer textViewer, int offset) {
		for (int i = 0; i < fHovers.length; i++) {
			IRegion region = fHovers[i].getHoverRegion(textViewer, offset);
			if (region != null)
				return region;
		}
		return null;
	}

	public String getInformation(ITextViewer textViewer, IRegion subject) {
		for (int i = 0; i < fHovers.length; i++) {
			String information = fHovers[i].getHoverInfo(textViewer, subject);
			if (information != null)
				return information;
		}
		return null;
	}

}
