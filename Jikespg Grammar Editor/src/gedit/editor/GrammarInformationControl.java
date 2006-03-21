/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class GrammarInformationControl implements IInformationControl, IInformationControlExtension, DisposeListener {
	private Shell fShell;
	private SourceViewer fViewer;
	private StyledText fText;
	private IInformationPresenter fPresenter;
	private TextPresentation fPresentation = new TextPresentation();

	private static final int BORDER = 1;

	public GrammarInformationControl(Shell parent) {
		this(parent, null);
	}

	public GrammarInformationControl(Shell parent, IInformationPresenter informationPresenter) {
		this(parent, SWT.TOOL, SWT.NONE, informationPresenter);
	}

	public GrammarInformationControl(Shell parent, int shellStyle, int style, IInformationPresenter informationPresenter) {
		fPresenter = informationPresenter;
		fShell = new Shell(parent, SWT.NO_FOCUS | SWT.ON_TOP | shellStyle);
		Display display = fShell.getDisplay();
		fShell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));

		Composite composite = fShell;
		GridLayout layout = new GridLayout(1, false);
		int border = ((shellStyle & SWT.NO_TRIM) == 0) ? 0 : BORDER;
		layout.marginHeight = border;
		layout.marginWidth = border;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(data);
		fViewer = createViewer(composite, style);

		fText = fViewer.getTextWidget();
		data = new GridData(GridData.BEGINNING | GridData.FILL_BOTH);
		data.horizontalIndent = 0;
		data.verticalIndent = 0;
		fText.setLayoutData(data);
		fText.setForeground(parent.getDisplay().getSystemColor(
				SWT.COLOR_INFO_FOREGROUND));
		fText.setBackground(parent.getDisplay().getSystemColor(
				SWT.COLOR_INFO_BACKGROUND));

		fText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == 0x1B) // ESC
					fShell.dispose();
			}
		});
	}

	private SourceViewer createViewer(Composite parent, int style) {
		GrammarSourceViewer viewer = new GrammarSourceViewer(parent, null, null, false, style, null);

		SourceViewerConfiguration configuration = new GrammarSourceViewerConfiguration();
		viewer.configure(configuration);
		viewer.setEditable(false);

		return viewer;
	}

	public void setInformation(String content) {
		if (fPresenter != null) {
			fPresentation.clear();
			content = fPresenter.updatePresentation(fShell.getDisplay(), content, fPresentation, -1, -1);
		}
		if (content == null) {
			fViewer.setInput(null);
			return;
		}

		IDocument document = new Document(content);
		new GrammarDocumentSetupParticipant().setup(document);
		fViewer.setDocument(document);
		fPresentation.mergeStyleRanges(fText.getStyleRanges());
		TextPresentation.applyTextPresentation(fPresentation, fText);
	}

	public void setSizeConstraints(int maxWidth, int maxHeight) {
	}

	public Point computeSizeHint() {
		return fShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	}

	public void setVisible(boolean visible) {
		fShell.setVisible(visible);
	}

	public void setSize(int width, int height) {
		fShell.setSize(width, height);
	}

	public void setLocation(Point location) {
		Rectangle trim = fShell.computeTrim(0, 0, 0, 0);
		Point textLocation = fText.getLocation();
		location.x += trim.x - textLocation.x;
		location.y += trim.y - textLocation.y;
		fShell.setLocation(location);
	}

	public void dispose() {
		if (fShell != null && !fShell.isDisposed()) {
			fShell.dispose();
		} else {
			widgetDisposed(null);
		}

	}

	public void addDisposeListener(DisposeListener listener) {
		fShell.addDisposeListener(listener);

	}

	public void removeDisposeListener(DisposeListener listener) {
		fShell.removeDisposeListener(listener);
	}

	public void setForegroundColor(Color foreground) {
		fText.setForeground(foreground);
	}

	public void setBackgroundColor(Color background) {
		fText.setBackground(background);
	}

	public boolean isFocusControl() {
		return fText.isFocusControl();
	}

	public void setFocus() {
		fShell.forceFocus();
		fText.setFocus();

	}

	public void addFocusListener(FocusListener listener) {
		fText.addFocusListener(listener);
	}

	public void removeFocusListener(FocusListener listener) {
		fText.removeFocusListener(listener);

	}

	public boolean hasContents() {
		return fText.getCharCount() > 0;
	}

	public void widgetDisposed(DisposeEvent e) {
		fShell = null;
		fText = null;
	}
}
