/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

public class PreferenceUtils {
	private ColorManager fColorManager;
	private IPreferenceStore fPreferenceStore;

	public PreferenceUtils(ColorManager colorManager, IPreferenceStore preferenceStore) {
		fColorManager = colorManager;
		fPreferenceStore = preferenceStore;
	}

	public TextAttribute createTextAttribute(String preferenceKey) {
		return new TextAttribute(getColor(preferenceKey), null, getStyle(preferenceKey));
	}

	public Color getColor(String preferenceKey) {
		if (preferenceKey == null)
			return null;
		return fColorManager.getColor(PreferenceConverter.getColor(fPreferenceStore, preferenceKey));
	}

	public int getStyle(String preferenceKey) {
		if (preferenceKey == null)
			return 0;
		int bold = fPreferenceStore.getBoolean(preferenceKey + PreferenceConstants.EDITOR_BOLD_SUFFIX) ? SWT.BOLD : 0;
		int italic = fPreferenceStore.getBoolean(preferenceKey + PreferenceConstants.EDITOR_ITALIC_SUFFIX) ? SWT.ITALIC : 0;
		int strikethrough = fPreferenceStore.getBoolean(preferenceKey + PreferenceConstants.EDITOR_STRIKETHROUGH_SUFFIX) ? TextAttribute.STRIKETHROUGH : 0;
		int underline = fPreferenceStore.getBoolean(preferenceKey + PreferenceConstants.EDITOR_UNDERLINE_SUFFIX) ? TextAttribute.UNDERLINE : 0;
		return bold | italic | strikethrough | underline;
	}

}
