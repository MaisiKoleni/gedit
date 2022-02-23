/*
 * (c) Copyright 2003 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

public interface IProblemRequestor {

	void beginReporting();
	void accept(Problem problem);
	void endReporting();
}
