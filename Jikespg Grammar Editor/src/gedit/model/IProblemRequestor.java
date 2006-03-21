/*
 * (c) Copyright 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

public interface IProblemRequestor {

	public void beginReporting();
	public void accept(Problem problem);
	public void endReporting();
}
