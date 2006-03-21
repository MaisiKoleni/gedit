/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;

public class SimpleTextPresenter implements IInformationPresenter {

	public final static char BOLD = 1;
	public final static char CR = 2;
	public final static char INDENT = 3;
	public final static char BULLET = 4;

	public String updatePresentation(Display display, String hoverInfo, TextPresentation presentation, int maxWidth, int maxHeight) {
		StringBuffer sb = new StringBuffer(hoverInfo);
		applyBold(sb, presentation);
		applyCr(sb, presentation);
		applyIndent(sb, presentation);
		applyBullet(sb, presentation);
		
		return sb.toString();
	}
	
	private void applyBold(StringBuffer text, TextPresentation presentation) {
		String s = text.toString();
		int index1 = s.indexOf(BOLD);
		if (index1 == -1)
			return;
		int index2 = s.indexOf(BOLD, index1 + 1);
		if (index2 == -1)
			index2 = text.length();
		else
			text.deleteCharAt(index2);
		StyleRange range = new StyleRange(index1, index2, null, null, SWT.BOLD);
		presentation.addStyleRange(range);
		text.deleteCharAt(index1);
		applyBold(text, presentation);
	}

	private void applyCr(StringBuffer text, TextPresentation presentation) {
		String s = text.toString();
		int index = s.indexOf(CR);
		if (index == -1)
			return;
		text.replace(index, index + 1, "\r\n");
		applyCr(text, presentation);
	}

	private void applyIndent(StringBuffer text, TextPresentation presentation) {
		String s = text.toString();
		int index = s.indexOf(INDENT);
		if (index == -1)
			return;
		text.replace(index, index + 1, "    ");
		applyIndent(text, presentation);
	}

	private void applyBullet(StringBuffer text, TextPresentation presentation) {
		String s = text.toString();
		int index = s.indexOf(BULLET);
		if (index == -1)
			return;
		StyleRange range = new StyleRange(index, 1, null, null, SWT.BOLD);
		presentation.addStyleRange(range);
		text.replace(index, index + 1, String.valueOf((char) 9679));
		applyBullet(text, presentation);
	}

}
