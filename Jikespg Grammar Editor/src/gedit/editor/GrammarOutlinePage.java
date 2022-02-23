/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.editor.actions.FilterAction;
import gedit.model.Document;
import gedit.model.ModelUtils;
import gedit.model.Document.IModelListener;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public class GrammarOutlinePage extends ContentOutlinePage {
	private class LinkAction extends Action  {
		public LinkAction() {
			super("Link with Editor");
			setToolTipText("Link with the active editor");
			setImageDescriptor(GrammarEditorPlugin.getImageDescriptor("icons/synced.gif"));
			setChecked(GrammarEditorPlugin.getDefault().getPreferenceStore().getBoolean(PREFERENCE_LINKED));
		}

		public void run() {
			boolean checked = isChecked();
			GrammarEditorPlugin.getDefault().getPreferenceStore().setValue(PREFERENCE_LINKED, checked);
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
			((ModelSorter) getTreeViewer().getSorter()).setEnabled(checked);
			refreshWithoutSelectionChangePropagation();
		}
	};
	private class CollapseAllAction extends Action {
		public CollapseAllAction() {
			super("Collapse All");
			setToolTipText("Collapse all elements");
			setImageDescriptor(GrammarEditorPlugin.getImageDescriptor("icons/collapseall.gif"));
		}

		public void run() {
			fSuppressSelectionChangePropagation = true;
			getTreeViewer().collapseAll();
			fSuppressSelectionChangePropagation = false;
		}
	};

	private boolean fSuppressSelectionChangePropagation;
    private ListenerList selectionChangedListeners = new ListenerList();
    private ModelFilter fFilter;

	private final static String PREFERENCE_SORTER = "outline_sorted";
	private final static String PREFERENCE_LINKED = "outline_linked";
	private final static String PREFERENCE_SECTION_FILTERS_RECENTLY_USED = PreferenceConstants.SECTION_FILTERS_RECENTLY_USED + "_outline_page";
	private static final String PREFERENCE_SECTION_FILTERS= PreferenceConstants.SECTION_FILTERS + "_outline_page";
	private static final String PREFERENCE_FILTER_MACROS = PreferenceConstants.FILTER_MACROS + "_outline_page";

	public void createControl(Composite parent) {
		super.createControl(parent);
		getTreeViewer().setContentProvider(new ModelContentProvider());
		getTreeViewer().setLabelProvider(new ModelLabelProvider());
		getTreeViewer().setSorter(new ModelSorter(PREFERENCE_SORTER));
		IPreferenceStore store = GrammarEditorPlugin.getDefault().getPreferenceStore();
		getTreeViewer().addFilter(fFilter = new ModelFilter(ModelUtils.
				createBitSetFromString(store.getString(PREFERENCE_SECTION_FILTERS),
						PreferenceConstants.SECTION_FILTERS_SEPARATOR),
				store.getBoolean(PREFERENCE_FILTER_MACROS)));
	}

	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager,
			IStatusLineManager statusLineManager) {
		toolBarManager.add(new CollapseAllAction());
		toolBarManager.add(new SortAction());
		toolBarManager.add(new LinkAction());

		FilterAction filterAction = new FilterAction(getTreeViewer(), fFilter,
				PREFERENCE_SECTION_FILTERS, PREFERENCE_SECTION_FILTERS_RECENTLY_USED,
				PREFERENCE_FILTER_MACROS);
		menuManager.add(filterAction.getMostRecentlyUsedContributionItem());
		menuManager.add(filterAction);
	}

	public void setInput(Document model) {
		getTreeViewer().setInput(model);
		model.addModelListener(new IModelListener() {
			public void modelChanged(Document model) {
				refreshWithoutSelectionChangePropagation();
			}
		});
	}

	protected void refreshWithoutSelectionChangePropagation() {
		if (getTreeViewer().getControl().isDisposed())
			return;
		getTreeViewer().getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				fSuppressSelectionChangePropagation = true;
				if (getTreeViewer().getControl().isDisposed())
					return;
				getTreeViewer().refresh(true);
				fSuppressSelectionChangePropagation = false;
			}
		});
	}

    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListeners.add(listener);
    }

    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListeners.remove(listener);
    }

    protected void fireSelectionChanged(ISelection selection) {
    	if (fSuppressSelectionChangePropagation)
    		return;
        final SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
        Object[] listeners = selectionChangedListeners.getListeners();
        for (int i = 0; i < listeners.length; ++i) {
            final ISelectionChangedListener l = (ISelectionChangedListener) listeners[i];
            Platform.run(new SafeRunnable() {
                public void run() {
                    l.selectionChanged(event);
                }
            });
        }
    }

	public boolean isLinkingEnabled() {
		return GrammarEditorPlugin.getDefault().getPreferenceStore().getBoolean(PREFERENCE_LINKED);
	}

	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (PREFERENCE_SORTER.equals(event.getProperty()))
				((ModelSorter) getTreeViewer().getSorter()).adaptToPreferenceChange(event);
		getTreeViewer().refresh();
	}
}
