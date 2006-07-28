/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.editor.actions.FilterAction;
import gedit.model.ModelBase;
import gedit.model.ModelUtils;
import gedit.model.Reference;
import gedit.model.Rhs;
import gedit.model.Section;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.IInformationControlExtension3;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class OutlineInformationControl implements IInformationControl,
		IInformationControlExtension2, IInformationControlExtension3 {

	private class PatternFilter extends ViewerFilter {
		private Map fCache = new HashMap();

		public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
			if (fMatcher == null)
				return elements;

			Object[] filtered = (Object[]) fCache.get(parent);
			if (filtered == null) {
				filtered = super.filter(viewer, parent, elements);
				fCache.put(parent, filtered);
			}
			return filtered;
		}

		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (element instanceof Section) {
				Object[] children = ((ITreeContentProvider) ((AbstractTreeViewer) viewer)
						.getContentProvider()).getChildren(element);
				if (children.length > 0)
					return filter(viewer, element, children).length > 0;
			}

			String labelText = ((ILabelProvider) ((StructuredViewer) viewer)
					.getLabelProvider()).getText(element);
			if (labelText == null)
				return false;
			return match(labelText);
		}

		public void setPattern(String patternString) {
			fCache.clear();
			if (patternString == null || patternString.equals("")) //$NON-NLS-1$
				fMatcher = null;
			else
				fMatcher = new StringMatcher(patternString + "*", true, false); //$NON-NLS-1$
		}

		protected boolean match(String string) {
			return fMatcher.match(string);
		}
	};
	
	private class SortAction extends Action {
		public SortAction() {
			super("Sort");
			setToolTipText("Sort the elements");
			setImageDescriptor(GrammarEditorPlugin.getImageDescriptor("icons/alphab_sort_co.gif"));
			setChecked(GrammarEditorPlugin.getDefault().getPreferenceStore().getBoolean(PREFERENCE_SORTER));
		}
		
		public void run() {
			boolean checked = isChecked();
			GrammarEditorPlugin.getDefault().getPreferenceStore().setValue(PREFERENCE_SORTER, checked);
			((ModelSorter) fTreeViewer.getSorter()).setEnabled(checked);
			fTreeViewer.refresh();
		}
	};

	private Shell fShell;
	private TreeViewer fTreeViewer;
	private PatternFilter fPatternFilter = new PatternFilter();
	private ModelSectionFilter fSectionFilter;
	private Text fText;
	private ToolBar fToolBar;
	private StringMatcher fMatcher;
	private GrammarSourceViewer fGrammarViewer;
	private MenuManager fMenuManager;
	
	private final static String PREFERENCE_SORTER = "quick_outline_sorted";
	private final static String PREFERENCE_FILTERS = PreferenceConstants.SECTION_FILTERS + "_quick_outline";
	private final static String PREFERENCE_FILTERS_RECENTLY_USED = PreferenceConstants.SECTION_FILTERS_RECENTLY_USED + "_quick_outline";
	
	public OutlineInformationControl(GrammarSourceViewer viewer, Shell parent) {
		fGrammarViewer = viewer;
		fShell = createShell(parent);
		fText = createTextControl(fShell);
		new Label(fShell, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fTreeViewer = createTreeViewer(fShell);
		new Label(fShell, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setForegroundColor(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		setBackgroundColor(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		
		fillMenuManager(fMenuManager);
	}

	private Shell createShell(Shell parent) {
		Shell shell = new Shell(parent, SWT.RESIZE | SWT.BORDER | SWT.NO_FOCUS);
		GridLayout layout = new GridLayout();
		shell.setLayout(layout);
		shell.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				deactivate();
			}
		});
		return shell;
	}

	private Text createTextControl(Shell parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Text text = new Text(composite, SWT.NONE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				handleFilterFieldKeyReleased(e);
			}
		});
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				handleFilterFieldModified();
			}
		});
		text.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
				if (!selection.isEmpty()) {
					entrySelected((ModelBase) selection.getFirstElement());
					deactivate();
				}
			}
		});

		createMenuButton(composite, fMenuManager = new MenuManager());
		return text;
	}

	protected void fillMenuManager(MenuManager menuManager) {
		FilterAction filterAction = new FilterAction(fTreeViewer, fSectionFilter, PREFERENCE_FILTERS, PREFERENCE_FILTERS_RECENTLY_USED);
		menuManager.add(filterAction.getMostRecentlyUsedContributionItem());
		menuManager.add(filterAction);
		menuManager.add(new Separator());
		menuManager.add(new SortAction());
	}

	protected void createMenuButton(Composite parent, final MenuManager menuManager) {
		if (menuManager == null)
			return;
		fToolBar = new ToolBar(parent, SWT.FLAT);
		fToolBar.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		ToolItem item = new ToolItem(fToolBar, SWT.PUSH, 0);
		GridData data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fToolBar.setLayoutData(data);
		item.setImage(GrammarEditorPlugin.getImage("icons/arrow_down.gif")); //$NON-NLS-1$
		item.setToolTipText("Menu");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showViewMenu(fToolBar, menuManager);
			}
		});
	}

	protected void showViewMenu(ToolBar toolBar, MenuManager menuManager) {
		Menu menu = menuManager.createContextMenu(fShell);
		Rectangle bounds = toolBar.getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = fShell.toDisplay(topLeft);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
	}

	private TreeViewer createTreeViewer(Shell shell) {
		TreeViewer viewer = new TreeViewer(shell, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setContentProvider(new ModelContentProvider());
		viewer.setLabelProvider(new ModelLabelProvider());
		viewer.setSorter(new ModelSorter(PREFERENCE_SORTER));
		
		viewer.getTree().addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				entrySelected((ModelBase) e.item.getData());
				deactivate();
			}
		});
		viewer.getControl().addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				handleTreeKeyReleased(e);
			}
		});
		viewer.addFilter(fPatternFilter);
		viewer.addFilter(fSectionFilter = new ModelSectionFilter(ModelUtils.
				createBitSetFromString(GrammarEditorPlugin.getDefault().getPreferenceStore().
						getString(PREFERENCE_FILTERS), PreferenceConstants.SECTION_FILTERS_SEPARATOR)));
		return viewer;
	}

	protected void handleTreeKeyReleased(KeyEvent e) {
		if (e.keyCode == SWT.ARROW_UP) {
			TreeItem[] selection = fTreeViewer.getTree().getSelection();
			int itemCount = fTreeViewer.getTree().getItemCount();
			if (selection.length > 0 && itemCount > 0 && fTreeViewer.getTree().getItems()[0].equals(
					selection[0]))
				fText.setFocus();
		}
	}

	protected void handleFilterFieldKeyReleased(KeyEvent e) {
		if (e.keyCode == SWT.ARROW_DOWN)
			fTreeViewer.getControl().setFocus();
	}
	
	protected void handleFilterFieldModified() {
		fPatternFilter.setPattern(fText.getText());
		fTreeViewer.getTree().setRedraw(false);
		fTreeViewer.refresh();
		if (fText.getText().length() > 0) {
			fTreeViewer.expandAll();
			selectFirstMatch();
		} else {
			fTreeViewer.collapseAll();
		}
		fTreeViewer.getTree().setRedraw(true);
	}
	
	protected void selectFirstMatch() {
		Tree tree = fTreeViewer.getTree();
		Object element = findElement(tree.getItems());
		if (element != null)
			fTreeViewer.setSelection(new StructuredSelection(element), true);
		else
			fTreeViewer.setSelection(StructuredSelection.EMPTY);
	}

	private ModelBase findElement(TreeItem[] items) {
		ILabelProvider labelProvider = (ILabelProvider) fTreeViewer.getLabelProvider();
		for (int i = 0; i < items.length; i++) {
			ModelBase element = (ModelBase) items[i].getData();
			if (fMatcher == null)
				return element;

			if (element != null) {
				String label = labelProvider.getText(element);
				if (fMatcher.match(label))
					return element;
			}

			element = findElement(items[i].getItems());
			if (element != null)
				return element;
		}
		return null;
	}

	protected void entrySelected(ModelBase model) {
		if (fTreeViewer.getData("init") != null)
			return;
		fTreeViewer.setData("selection", "true");
		fGrammarViewer.selectElement(model);
		fTreeViewer.setData("selection", null);
	}

	public void setInformation(String information) {
	}

	public void setSizeConstraints(int maxWidth, int maxHeight) {
	}

	public Point computeSizeHint() {
		return new Point(10, 10);
	}

	public void setVisible(boolean visible) {
		ModelBase element = fGrammarViewer.getSelectedElement();
		if (element instanceof Reference && element.getParent() instanceof Rhs)
			element = element.getParent();
		
		if (fTreeViewer.getData("selection") == null) {
			fShell.setVisible(visible);
			fText.setText("");
			if (visible && element != null) {
				fTreeViewer.expandToLevel(element, 0);
				fTreeViewer.setSelection(new StructuredSelection(element), true);
			}
		}
	}

	public void setSize(int width, int height) {
		fShell.setSize(width, height);
	}

	public void setLocation(Point location) {
		fShell.setLocation(location);
	}
	
	protected void deactivate() {
		setVisible(false);
	}

	public void dispose() {
		fShell.dispose();
		fShell = null;
		fTreeViewer = null;
	}

	public void addDisposeListener(DisposeListener listener) {
		fTreeViewer.getControl().addDisposeListener(listener);
	}

	public void removeDisposeListener(DisposeListener listener) {
		fTreeViewer.getControl().removeDisposeListener(listener);
	}

	public void setForegroundColor(Color foreground) {
		fShell.setForeground(foreground);
		fText.setForeground(foreground);
		fText.getParent().setForeground(foreground);
		fTreeViewer.getControl().setForeground(foreground);
	}

	public void setBackgroundColor(Color background) {
		fShell.setBackground(background);
		fText.setBackground(background);
		fText.getParent().setBackground(background);
		fTreeViewer.getControl().setBackground(background);
	}

	public boolean isFocusControl() {
		return fTreeViewer.getControl().isFocusControl() ||
				fText.isFocusControl();
	}

	public void setFocus() {
		fText.setFocus();
	}

	public void addFocusListener(FocusListener listener) {
		fTreeViewer.getControl().addFocusListener(listener);
	}

	public void removeFocusListener(FocusListener listener) {
		fTreeViewer.getControl().removeFocusListener(listener);
	}

	public Rectangle getBounds() {
		return fShell.getBounds();
	}

	public Rectangle computeTrim() {
		return fShell.computeTrim(-1, -1, 20, 20);
	}

	public boolean restoresSize() {
		return true;
	}

	public boolean restoresLocation() {
		return true;
	}

	public void setInput(Object input) {
		fTreeViewer.setData("init", "true");
		fTreeViewer.setInput(input);
		fShell.getDisplay().asyncExec(new Runnable() {
			public void run() {
				fTreeViewer.setData("init", null);
			}
		});
	}

}
