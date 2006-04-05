/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.model.Document;
import gedit.model.Document.IModelListener;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public class GrammarOutlinePage extends ContentOutlinePage {
	private class LinkAction extends Action  {
		public LinkAction() {
			super("Link with Editor");
			setToolTipText("Link with the active editor");
			setImageDescriptor(GrammarEditorPlugin.getImageDescriptor("icons/synced.gif"));
			setChecked(GrammarEditorPlugin.getDefault().getPreferenceStore().getBoolean(PROPERTY_LINKED));
		}
		
		public void run() {
			boolean checked = isChecked();
			GrammarEditorPlugin.getDefault().getPreferenceStore().setValue(PROPERTY_LINKED, checked);
		}
	};
	private class SortAction extends Action {
		public SortAction() {
			super("Sort");
			setToolTipText("Sort the elements");
			setImageDescriptor(GrammarEditorPlugin.getImageDescriptor("icons/alphab_sort_co.gif"));
			setChecked(GrammarEditorPlugin.getDefault().getPreferenceStore().getBoolean(PROPERTY_SORTER));
		}
		
		public void run() {
			boolean checked = isChecked();
			GrammarEditorPlugin.getDefault().getPreferenceStore().setValue(PROPERTY_SORTER, checked);
			((Sorter) getTreeViewer().getSorter()).enable(checked);
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
	
	private class Sorter extends ViewerSorter {
		private boolean enabled = GrammarEditorPlugin.getDefault().getPreferenceStore().getBoolean(PROPERTY_SORTER);

		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!enabled)
				return 0;
			if (e1 instanceof Comparable)
				return ((Comparable) e1).compareTo(e2);
			return super.compare(viewer, e1, e2);
		}

		public void enable(boolean enabled) {
			this.enabled = enabled;
		}
	};

	private boolean fSuppressSelectionChangePropagation;
    protected ListenerList selectionChangedListeners = new ListenerList();

	private final static String PROPERTY_SORTER = "outline_sorted";
	private final static String PROPERTY_LINKED = "outline_linked";

	public void createControl(Composite parent) {
		super.createControl(parent);
		getTreeViewer().setContentProvider(new BaseWorkbenchContentProvider());
		getTreeViewer().setLabelProvider(new ModelLabelProvider());
		getTreeViewer().setSorter(new Sorter());
	}
	
	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager,
			IStatusLineManager statusLineManager) {
		toolBarManager.add(new CollapseAllAction());
		toolBarManager.add(new SortAction());
		toolBarManager.add(new LinkAction());
	}

	public void setInput(Document model) {
		getTreeViewer().setInput(model);
		model.addModelListener(new IModelListener() {
			public void modelChanged() {
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
		return GrammarEditorPlugin.getDefault().getPreferenceStore().getBoolean(PROPERTY_LINKED);
	}
}
