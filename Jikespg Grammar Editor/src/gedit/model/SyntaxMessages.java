/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class SyntaxMessages {

	private final static ResourceBundle BUNDLE = ResourceBundle.getBundle("gedit.model.SyntaxMessages");


	public static String getError(String key) {
		try {
			return BUNDLE.getString(key);
		} catch (Exception e) {
			return '!' + key + '!';
		}
	}

	public static String getError(String key, Object argument) {
		try {
			String message = BUNDLE.getString(key);
			return MessageFormat.format(message, argument instanceof Object[] ?
					(Object[]) argument : new Object[] { argument });
		} catch (Exception e) {
			return '!' + key + '!';
		}
	}
}
