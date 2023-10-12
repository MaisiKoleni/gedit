/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit;

import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.widgets.Display;

public abstract class NonUISafeRunnable extends SafeRunnable {

	@Override
	public void handleException(final Throwable e) {
		if (Thread.currentThread() != Display.getDefault().getSyncThread()) {
			Display.getDefault().syncExec(() -> NonUISafeRunnable.super.handleException(e));
		} else {
			super.handleException(e);
		}
	}

}
