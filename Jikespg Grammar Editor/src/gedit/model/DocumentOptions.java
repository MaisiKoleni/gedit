/*
 * (c) Copyright 2002, 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DocumentOptions {
	private char escape;
	private char orMarker;
	private String language;
	private String[] blockBeginnings; 
	private String[] blockEnds;
	private String[] includeDirs;
	private boolean global;
	
	public final static char DEFAULT_ESCAPE = '%';
	public final static char DEFAULT_OR_MARKER = '|';
	public final static String DEFAULT_BLOCKB = "/.";
	public final static String DEFAULT_BLOCKE = "./";
	// compatibility
	public final static String DEFAULT_HBLOCKB = "/:";
	public final static String DEFAULT_HBLOCKE = ":/";

	public DocumentOptions() {
		escape = DEFAULT_ESCAPE;
		orMarker = DEFAULT_OR_MARKER;
		blockBeginnings = new String[] { DEFAULT_BLOCKB, DEFAULT_HBLOCKB };
		blockEnds = new String[] { DEFAULT_BLOCKE, DEFAULT_HBLOCKE };
	}
	
	public DocumentOptions(boolean global) {
		this();
		this.global = global;
	}

	public char getEsape() {
		return escape;
	}
	
	public char getOrMarker() {
		return orMarker;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public String[] getBlockBeginnings() {
		return blockBeginnings;
	}

	public String[] getBlockEnds() {
		return blockEnds;
	}
	
	public String[] getIncludeDirs() {
		return includeDirs;
	}
	
	protected void addBlockPair(String beginning, String end) {
		if (beginning == null || end == null || beginning.length() != end.length() || beginning.length() == 0)
			return;
		for (int i = 0; i < blockBeginnings.length; i++) {
			if (beginning.equals(blockBeginnings[i]) && end.equals(blockEnds[i]))
				return;
		}
		String[] tmp = new String[blockBeginnings.length + 1];
		System.arraycopy(blockBeginnings, 0, tmp, 0, blockBeginnings.length);
		tmp[blockBeginnings.length] = beginning;
		blockBeginnings = tmp;
		tmp = new String[blockEnds.length + 1];
		System.arraycopy(blockEnds, 0, tmp, 0, blockEnds.length);
		tmp[blockEnds.length] = end;
		blockEnds = tmp;
	}

	protected void setEscape(char c) {
		escape = c;
	}

	protected void setOrMarker(char orMarker) {
		this.orMarker = orMarker;
	}

	protected void setLanguage(String language) {
		this.language = language;
	}

	public void setIncludeDirs(String[] includeDirs) {
		this.includeDirs = includeDirs;
	}

	public void resetTo(DocumentOptions globalOptions) {
		escape = globalOptions.getEsape();
		orMarker = globalOptions.getOrMarker();
		language = globalOptions.getLanguage();
		blockBeginnings = globalOptions.getBlockBeginnings();
		blockEnds = globalOptions.getBlockEnds();
		if (globalOptions.global)
			includeDirs = globalOptions.getIncludeDirs();
		else {
			List dirs = globalOptions.includeDirs != null ? new ArrayList(Arrays.asList(globalOptions.includeDirs)) : new ArrayList();
			if (includeDirs != null) {
				for (int i = 0; i < includeDirs.length; i++) {
					int index = dirs.indexOf(includeDirs[i]);
					if (index != -1)
						dirs.remove(index);
				}
			}
			
			int newLength = dirs.size() + (includeDirs != null ? includeDirs.length : 0);
			String[] tmp = new String[newLength];
			if (includeDirs != null)
				System.arraycopy(includeDirs, 0, tmp, 0, includeDirs.length);
			System.arraycopy(dirs.toArray(new String[dirs.size()]), 0, tmp, includeDirs != null ? includeDirs.length : 0, dirs.size());
			includeDirs = tmp;
		}
	}
}