/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.jface.text.TextPresentation;
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

import gedit.model.Document;

public class GrammarInformationControl implements IInformationControl, IInformationControlExtension, DisposeListener {
	private Shell fShell;
	private Document fParentDocument;
	private SourceViewer fViewer;
	private StyledText fText;
	private IInformationPresenter fPresenter;
	private TextPresentation fPresentation = new TextPresentation();

	private static final int BORDER = 1;

	public GrammarInformationControl(Shell parent, Document parentDocument) {
		this(parent, null, parentDocument);
	}

	public GrammarInformationControl(Shell parent, IInformationPresenter informationPresenter, Document parentDocument) {
		this(parent, SWT.TOOL, SWT.NONE, informationPresenter, parentDocument);
	}

	public GrammarInformationControl(Shell parent, int shellStyle, int style, IInformationPresenter informationPresenter, Document parentDocument) {
		fPresenter = informationPresenter;
		fParentDocument = parentDocument;
		fShell = new Shell(parent, SWT.NO_FOCUS | SWT.ON_TOP | shellStyle);
		Display display = fShell.getDisplay();
		fShell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));

		Composite composite = fShell;
		GridLayout layout = new GridLayout(1, false);
		int border = (shellStyle & SWT.NO_TRIM) == 0 ? 0 : BORDER;
		layout.marginHeight = border;
		layout.marginWidth = border;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(data);
		fViewer = createViewer(composite, style);

		fText = fViewer.getTextWidget();
		data = new GridData(GridData.BEGINNING | GridData.FILL_BOTH);
		fText.setLayoutData(data);
		fText.setForeground(parent.getDisplay().getSystemColor(
				SWT.COLOR_INFO_FOREGROUND));
		fText.setBackground(parent.getDisplay().getSystemColor(
				SWT.COLOR_INFO_BACKGROUND));

		fText.addKeyListener(new KeyAdapter() {
			@Override
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

	@Override
	public void setInformation(String content) {
		if (fPresenter != null) {
			fPresentation.clear();
			content = fPresenter.updatePresentation(fShell.getDisplay(), content, fPresentation, -1, -1);
		}
		if (content == null) {
			fViewer.setInput(null);
			return;
		}

		IDocument document = new GrammarDocument(content, fParentDocument);
		new GrammarDocumentSetupParticipant().setup(document);
		fViewer.setDocument(document);
		fPresentation.mergeStyleRanges(fText.getStyleRanges());
		TextPresentation.applyTextPresentation(fPresentation, fText);
	}

	@Override
	public void setSizeConstraints(int maxWidth, int maxHeight) {
	}

	@Override
	public Point computeSizeHint() {
		return fShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	}

	@Override
	public void setVisible(boolean visible) {
		fShell.setVisible(visible);
	}

	@Override
	public void setSize(int width, int height) {
		fShell.setSize(width, height);
	}

	@Override
	public void setLocation(Point location) {
		Rectangle trim = fShell.computeTrim(0, 0, 0, 0);
		Point textLocation = fText.getLocation();
		location.x += trim.x - textLocation.x;
		location.y += trim.y - textLocation.y;
		fShell.setLocation(location);
	}

	@Override
	public void dispose() {
		if (fShell != null && !fShell.isDisposed()) {
			fShell.dispose();
		} else {
			widgetDisposed(null);
		}

	}

	@Override
	public void addDisposeListener(DisposeListener listener) {
		fShell.addDisposeListener(listener);

	}

	@Override
	public void removeDisposeListener(DisposeListener listener) {
		fShell.removeDisposeListener(listener);
	}

	@Override
	public void setForegroundColor(Color foreground) {
		fText.setForeground(foreground);
	}

	@Override
	public void setBackgroundColor(Color background) {
		fText.setBackground(background);
	}

	@Override
	public boolean isFocusControl() {
		return fText.isFocusControl();
	}

	@Override
	public void setFocus() {
		fShell.forceFocus();
		fText.setFocus();

	}

	@Override
	public void addFocusListener(FocusListener listener) {
		fText.addFocusListener(listener);
	}

	@Override
	public void removeFocusListener(FocusListener listener) {
		fText.removeFocusListener(listener);

	}

	@Override
	public boolean hasContents() {
		return fText.getCharCount() > 0;
	}

	@Override
	public void widgetDisposed(DisposeEvent e) {
		fShell = null;
		fText = null;
	}
}
