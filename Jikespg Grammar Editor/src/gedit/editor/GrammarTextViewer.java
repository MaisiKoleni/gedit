/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.compare.IEncodedStreamContentAccessor;
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

import gedit.GrammarEditorPlugin;

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

	@Override
	public Control getControl() {
		return fSourceViewer.getControl();
	}

	@Override
	public void setInput(Object input) {
		if (input instanceof IStreamContentAccessor) {
			Document document = new Document(getString(input));
			GrammarDocumentSetupParticipant.setupDocument(document);
			fSourceViewer.setDocument(document);
		}
		fInput = input;
	}

	@Override
	public Object getInput() {
		return fInput;
	}

	@Override
	public ISelection getSelection() {
		return null;
	}

	@Override
	public void setSelection(ISelection s, boolean reveal) {
	}

	@Override
	public void refresh() {
	}

	static String getString(Object input) {

		if (input instanceof IStreamContentAccessor) {
			String charSet = null;
			IStreamContentAccessor accessor = (IStreamContentAccessor) input;
			if (accessor instanceof IEncodedStreamContentAccessor) {
				try {
					charSet = ((IEncodedStreamContentAccessor) accessor).getCharset();
				} catch (Exception e) {
				}
			}
			StringBuilder sb = new StringBuilder();
			try {
				Reader reader = charSet == null ? new InputStreamReader(accessor.getContents())
						: new InputStreamReader(accessor.getContents(), charSet);
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
