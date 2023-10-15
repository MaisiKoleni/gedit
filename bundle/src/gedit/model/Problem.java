/*
 * (c) Copyright 2002 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

public class Problem {
	private int type;
	private int offset;
	private int length;
	private String message;

	public final static int ERROR = 1;
	public final static int WARNING = 2;

	public Problem(int type, String message, int offset, int length) {
		this.type = type;
		this.offset = offset;
		this.length = length;
		this.message = message;
	}

	public int getType() {
		return type;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "[" + offset + ", " + length + "] " + message;
	}
}
