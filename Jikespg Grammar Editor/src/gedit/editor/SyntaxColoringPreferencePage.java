/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

public class SyntaxColoringPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private static class SourcePreviewerUpdater {

		SourcePreviewerUpdater(final SourceViewer viewer, final GrammarSourceViewerConfiguration configuration, final IPreferenceStore preferenceStore) {
			Assert.isNotNull(viewer);
			Assert.isNotNull(configuration);
			Assert.isNotNull(preferenceStore);
			final IPropertyChangeListener fontChangeListener= new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(PreferenceConstants.GRAMMAR_EDITOR_TEXT_FONT)) {
						Font font = JFaceResources.getFont(PreferenceConstants.GRAMMAR_EDITOR_TEXT_FONT);
						viewer.getTextWidget().setFont(font);
					}
				}
			};
			final IPropertyChangeListener propertyChangeListener= new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					if (configuration.affectsTextPresentation(event)) {
						configuration.adaptToPreferenceChange(event);
						viewer.invalidateTextPresentation();
					}
				}
			};
			viewer.getTextWidget().addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					preferenceStore.removePropertyChangeListener(propertyChangeListener);
					JFaceResources.getFontRegistry().removeListener(fontChangeListener);
				}
			});
			JFaceResources.getFontRegistry().addListener(fontChangeListener);
			preferenceStore.addPropertyChangeListener(propertyChangeListener);
		}
	};

	private static class HighlightingColorListItem {
		private String fDisplayName;
		private String fColorKey;
		private String fBoldKey;
		private String fItalicKey;
		private String fStrikethroughKey;
		private String fUnderlineKey;
		private Color fItemColor;

		public HighlightingColorListItem(String displayName, String colorKey, String boldKey, String italicKey, String strikethroughKey, String underlineKey, Color itemColor) {
			fDisplayName = displayName;
			fColorKey = colorKey;
			fBoldKey = boldKey;
			fItalicKey = italicKey;
			fStrikethroughKey = strikethroughKey;
			fUnderlineKey = underlineKey;
			fItemColor = itemColor;
		}

		public String getBoldKey() {
			return fBoldKey;
		}

		public String getItalicKey() {
			return fItalicKey;
		}

		public String getStrikethroughKey() {
			return fStrikethroughKey;
		}

		public String getUnderlineKey() {
			return fUnderlineKey;
		}

		public String getColorKey() {
			return fColorKey;
		}

		public String getDisplayName() {
			return fDisplayName;
		}

		public Color getItemColor() {
			return fItemColor;
		}
	};

	private class ColorListLabelProvider extends LabelProvider implements IColorProvider {

		public String getText(Object element) {
			return ((HighlightingColorListItem) element).getDisplayName();
		}

		public Color getForeground(Object element) {
			return ((HighlightingColorListItem) element).getItemColor();
		}

		public Color getBackground(Object element) {
			return null;
		}
	};

	private static final String BOLD = PreferenceConstants.EDITOR_BOLD_SUFFIX;
	private static final String ITALIC = PreferenceConstants.EDITOR_ITALIC_SUFFIX;
	private static final String STRIKETHROUGH = PreferenceConstants.EDITOR_STRIKETHROUGH_SUFFIX;
	private static final String UNDERLINE = PreferenceConstants.EDITOR_UNDERLINE_SUFFIX;

	private final String[][] fSyntaxColorListModel = new String[][] {
		{ "Terminal", PreferenceConstants.GRAMMAR_COLORING_TERMINAL },
		{ "Non-terminal", PreferenceConstants.GRAMMAR_COLORING_NON_TERMINAL },
		{ "Alias", PreferenceConstants.GRAMMAR_COLORING_ALIAS },
		{ "Operator", PreferenceConstants.GRAMMAR_COLORING_OPERATOR },
		{ "Comment", PreferenceConstants.GRAMMAR_COLORING_COMMENT },
		{ "Macro", PreferenceConstants.GRAMMAR_COLORING_MACRO },
		{ "Macro key", PreferenceConstants.GRAMMAR_COLORING_MACRO_KEY },
		{ "Option", PreferenceConstants.GRAMMAR_COLORING_OPTION },
		{ "Link", PreferenceConstants.GRAMMAR_COLORING_LINK },
	};

	private OverlayPreferenceStore fOverlayStore;

	private ColorSelector fSyntaxForegroundColorEditor;
	private Button fBoldCheckBox;

	private Button fItalicCheckBox;
	private Button fStrikethroughCheckBox;
	private Button fUnderlineCheckBox;
	private SourceViewer fPreviewViewer;
	private ArrayList fMasterSlaveListeners = new ArrayList();
	private final List fHighlightingColorList = new ArrayList();
	private TableViewer fHighlightingColorListViewer;
	private ColorManager fColorManager;

	public SyntaxColoringPreferencePage() {
		setPreferenceStore(GrammarEditorPlugin.getDefault().getPreferenceStore());
		fOverlayStore = new OverlayPreferenceStore(getPreferenceStore(), createOverlayStoreKeys());
	}

	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {

		ArrayList overlayKeys = new ArrayList();

		for (int i = 0; i < fSyntaxColorListModel.length; i++) {
			String colorKey = fSyntaxColorListModel[i][1];
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, colorKey));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, colorKey + BOLD));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, colorKey + ITALIC));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, colorKey + STRIKETHROUGH));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, colorKey + UNDERLINE));
		}

		OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return keys;
	}

	public void init(IWorkbench workbench) {
	}

	private void handleSyntaxColorListSelection() {
		HighlightingColorListItem item = getHighlightingColorListItem();
		RGB rgb = PreferenceConverter.getColor(fOverlayStore, item.getColorKey());
		fSyntaxForegroundColorEditor.setColorValue(rgb);
		fBoldCheckBox.setSelection(fOverlayStore.getBoolean(item.getBoldKey()));
		fItalicCheckBox.setSelection(fOverlayStore.getBoolean(item.getItalicKey()));
		fStrikethroughCheckBox.setSelection(fOverlayStore.getBoolean(item.getStrikethroughKey()));
		fUnderlineCheckBox.setSelection(fOverlayStore.getBoolean(item.getUnderlineKey()));

		fSyntaxForegroundColorEditor.getButton().setEnabled(true);
		fBoldCheckBox.setEnabled(true);
		fItalicCheckBox.setEnabled(true);
		fStrikethroughCheckBox.setEnabled(true);
		fUnderlineCheckBox.setEnabled(true);
	}

	private Control createSyntaxPage(Composite parent) {

		Label label = new Label(parent, SWT.LEFT);
		label.setText("Fo&reground");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite editorComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		editorComposite.setLayout(layout);
		GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		editorComposite.setLayoutData(data);

		fHighlightingColorListViewer = new TableViewer(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		fHighlightingColorListViewer.setLabelProvider(new ColorListLabelProvider());
		fHighlightingColorListViewer.setContentProvider(new ArrayContentProvider());
		data = new GridData(SWT.BEGINNING, SWT.FILL, false, true);
		data.heightHint = convertHeightInCharsToPixels(5);
		fHighlightingColorListViewer.getControl().setLayoutData(data);

		Composite stylesComposite = new Composite(editorComposite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalAlignment = GridData.BEGINNING;
		data.horizontalSpan = 2;

		label = new Label(stylesComposite, SWT.LEFT);
		label.setText("Co&lor");
		data= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		data.horizontalIndent = 20;
		label.setLayoutData(data);

		fSyntaxForegroundColorEditor = new ColorSelector(stylesComposite);
		Button foregroundColorButton = fSyntaxForegroundColorEditor.getButton();
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		foregroundColorButton.setLayoutData(data);

		fBoldCheckBox = new Button(stylesComposite, SWT.CHECK);
		fBoldCheckBox.setText("&Bold");
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		data.horizontalIndent = 20;
		data.horizontalSpan = 2;
		fBoldCheckBox.setLayoutData(data);

		fItalicCheckBox = new Button(stylesComposite, SWT.CHECK);
		fItalicCheckBox.setText("&Italic");
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		data.horizontalIndent = 20;
		data.horizontalSpan = 2;
		fItalicCheckBox.setLayoutData(data);

		fStrikethroughCheckBox = new Button(stylesComposite, SWT.CHECK);
		fStrikethroughCheckBox.setText("&Strikethrough");
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		data.horizontalIndent = 20;
		data.horizontalSpan = 2;
		fStrikethroughCheckBox.setLayoutData(data);

		fUnderlineCheckBox = new Button(stylesComposite, SWT.CHECK);
		fUnderlineCheckBox.setText("&Underline");
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		data.horizontalIndent = 20;
		data.horizontalSpan = 2;
		fUnderlineCheckBox.setLayoutData(data);

		label = new Label(parent, SWT.LEFT);
		label.setText("Previe&w");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control previewer = createPreviewer(parent);
		data = new GridData(GridData.FILL_BOTH);
		data.widthHint = convertWidthInCharsToPixels(20);
		data.heightHint = convertHeightInCharsToPixels(5);
		previewer.setLayoutData(data);


		fHighlightingColorListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSyntaxColorListSelection();
			}
		});

		foregroundColorButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				PreferenceConverter.setValue(fOverlayStore, item.getColorKey(), fSyntaxForegroundColorEditor.getColorValue());
			}
		});

		fBoldCheckBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				fOverlayStore.setValue(item.getBoldKey(), fBoldCheckBox.getSelection());
			}
		});

		fItalicCheckBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				fOverlayStore.setValue(item.getItalicKey(), fItalicCheckBox.getSelection());
			}
		});

		fStrikethroughCheckBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				fOverlayStore.setValue(item.getStrikethroughKey(), fStrikethroughCheckBox.getSelection());
			}
		});

		fUnderlineCheckBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				fOverlayStore.setValue(item.getUnderlineKey(), fUnderlineCheckBox.getSelection());
			}
		});

		parent.layout();

		return parent;
	}

	private Control createPreviewer(Composite parent) {

		IPreferenceStore store = new ChainedPreferenceStore(new IPreferenceStore[] { fOverlayStore, GrammarEditorPlugin.getDefault().getCombinedPreferenceStore()});
		fPreviewViewer = new GrammarSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER, store);
		fColorManager = GrammarEditorPlugin.getColorManager();
		GrammarSourceViewerConfiguration configuration = new GrammarSourceViewerConfiguration(fColorManager, store, null);
		fPreviewViewer.configure(configuration);
		Font font = JFaceResources.getFont(PreferenceConstants.GRAMMAR_EDITOR_TEXT_FONT);
		fPreviewViewer.getTextWidget().setFont(font);
		new SourcePreviewerUpdater(fPreviewViewer, configuration, store);
		fPreviewViewer.setEditable(false);

		String content = loadPreviewContentFromFile("GrammarEditorColorSettingPreviewCode.txt"); //$NON-NLS-1$
		IDocument document = new Document(content);
		GrammarDocumentSetupParticipant.setupDocument(document);
		fPreviewViewer.setDocument(document);

		return fPreviewViewer.getControl();
	}

	protected Control createContents(Composite parent) {
		fOverlayStore.load();
		fOverlayStore.start();

		Composite contents = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		contents.setLayout(layout);
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));

		createHeader(contents);
		createSyntaxPage(contents);
		initialize();

		Dialog.applyDialogFont(contents);
		return contents;
	}

	private void createHeader(Composite contents) {
		String text = "Grammar editor syntax coloring preferences. Note that some properties may be set on the <a>Text Editors</a> preferenc page.";
		Link link = new Link(contents, SWT.NONE);
		link.setText(text);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(getShell(), "org.eclipse.ui.preferencePages.GeneralTextEditor", null, null); //$NON-NLS-1$
			}
		});
		link.setToolTipText("Show the shared editor text preferences");

		GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gridData.widthHint = 150; // only expand further if anyone else requires it
		link.setLayoutData(gridData);
	}

	private void initialize() {

		initializeFields();
		for (int i = 0, n = fSyntaxColorListModel.length; i < n; i++)
			fHighlightingColorList.add(new HighlightingColorListItem(fSyntaxColorListModel[i][0], fSyntaxColorListModel[i][1], fSyntaxColorListModel[i][1] + BOLD, fSyntaxColorListModel[i][1] + ITALIC, fSyntaxColorListModel[i][1] + STRIKETHROUGH, fSyntaxColorListModel[i][1] + UNDERLINE, null));

		fHighlightingColorListViewer.setInput(fHighlightingColorList);
		fHighlightingColorListViewer.setSelection(new StructuredSelection(fHighlightingColorListViewer.getElementAt(0)));
	}

	private void initializeFields() {
        // Update slaves
        for (Iterator iter= fMasterSlaveListeners.iterator(); iter.hasNext(); ) {
            SelectionListener listener = (SelectionListener)iter.next();
            listener.widgetSelected(null);
        }
	}

	public boolean performOk() {
		fOverlayStore.propagate();
		GrammarEditorPlugin.getDefault().savePluginPreferences();
		return true;
	}

	protected void performDefaults() {

		fOverlayStore.loadDefaults();

		initializeFields();
		handleSyntaxColorListSelection();

		super.performDefaults();

		fPreviewViewer.invalidateTextPresentation();
	}

	public void dispose() {

		if (fOverlayStore != null) {
			fOverlayStore.stop();
			fOverlayStore = null;
		}

		super.dispose();
	}

	private String loadPreviewContentFromFile(String filename) {
		String line;
		String separator = System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer(512);
		BufferedReader reader = null;
		try {
			InputStream in = getClass().getResourceAsStream(filename);
			if (in == null)
				throw new IllegalStateException("Missing resource: " + filename);
			reader = new BufferedReader(new InputStreamReader(in));
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
				buffer.append(separator);
			}
		} catch (Exception e) {
			GrammarEditorPlugin.logError("Cannot load the grammar syntax preview content", e);
		} finally {
			if (reader != null) {
				try { reader.close(); } catch (IOException e) {}
			}
		}
		return buffer.toString();
	}

	private HighlightingColorListItem getHighlightingColorListItem() {
		IStructuredSelection selection = (IStructuredSelection) fHighlightingColorListViewer.getSelection();
		return (HighlightingColorListItem) selection.getFirstElement();
	}
}
