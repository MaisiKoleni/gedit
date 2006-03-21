/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;

import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class GrammarTextViewer extends Viewer {
	private SourceViewer fSourceViewer;
	private Object fInput;

	GrammarTextViewer(Composite parent) {
		fSourceViewer = new SourceViewer(parent, null, SWT.H_SCROLL + SWT.V_SCROLL);
		fSourceViewer.configure(new GrammarSourceViewerConfiguration());
		fSourceViewer.setEditable(false);

		String symbolicFontName = GrammarMergeViewer.class.getName();
		Font font = JFaceResources.getFont(symbolicFontName);
		if (font != null)
			fSourceViewer.getTextWidget().setFont(font);
	}
		
	public Control getControl() {
		return fSourceViewer.getControl();
	}
	
	public void setInput(Object input) {
		if (input instanceof IStreamContentAccessor) {
			Document document = new Document(getString(input));
			GrammarDocumentSetupParticipant.setupDocument(document);
			fSourceViewer.setDocument(document);
		}
		fInput = input;
	}
	
	public Object getInput() {
		return fInput;
	}
	
	public ISelection getSelection() {
		return null;
	}
	
	public void setSelection(ISelection s, boolean reveal) {
	}
	
	public void refresh() {
	}
	
	private static String getString(Object input) {
		
		if (input instanceof IStreamContentAccessor) {
			IStreamContentAccessor accessor = (IStreamContentAccessor) input;
			StringBuffer sb = new StringBuffer();
			try {
				Reader reader = new InputStreamReader(accessor.getContents());
				char[] buf = new char[1024];
				for (int count = reader.read(buf); count != -1; count = reader.read(buf)) {
					sb.append(buf, 0, count);
				}
				reader.close();
				return sb.toString();
			} catch (Exception e) {
				GrammarEditorPlugin.logError("Cannot read content", e); //$NON-NLS-1$
			}
		}
		return ""; //$NON-NLS-1$
	}
}
