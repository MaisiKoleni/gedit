/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;

public class SimpleTextPresenter implements IInformationPresenter {

	public final static char BOLD = SWT.BOLD;
	public final static char CR = 2;
	public final static char INDENT = 3;
	public final static char BULLET = 4;

	@Override
	public String updatePresentation(Display display, String hoverInfo, TextPresentation presentation, int maxWidth, int maxHeight) {
		StringBuffer sb = new StringBuffer(hoverInfo);
		applyBold(sb, presentation);
		applyItalic(sb, presentation);
		applyCr(sb, presentation);
		applyIndent(sb, presentation);
		applyBullet(sb, presentation);

		return sb.toString();
	}

	private void applyBold(StringBuffer text, TextPresentation presentation) {
		applyFontStyle(text, presentation, SWT.BOLD);
	}

	private void applyItalic(StringBuffer text, TextPresentation presentation) {
		applyFontStyle(text, presentation, SWT.ITALIC);
	}

	private void applyFontStyle(StringBuffer text, TextPresentation presentation, int fontStyle) {
		String s = text.toString();
		int index1 = s.indexOf(fontStyle);
		if (index1 == -1)
			return;
		int index2 = s.indexOf(fontStyle, index1 + 1);
		if (index2 == -1)
			index2 = text.length();
		else
			text.deleteCharAt(index2);
		StyleRange range = new StyleRange(index1, index2 - index1 - 1, null, null, fontStyle);
		presentation.addStyleRange(range);
		text.deleteCharAt(index1);
		applyFontStyle(text, presentation, fontStyle);
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
