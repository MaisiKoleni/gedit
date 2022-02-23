/*
 * (c) Copyright 2006 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;

import gedit.model.Document;

public class GrammarFileEditorInput implements IPathEditorInput, ILocationProvider {
	private static class WorkbenchAdapter implements IWorkbenchAdapter {
		@Override
		public Object[] getChildren(Object o) {
			return null;
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object object) {
			return null;
		}

		@Override
		public String getLabel(Object o) {
			return ((GrammarFileEditorInput)o).getName();
		}

		@Override
		public Object getParent(Object o) {
			return null;
		}
	}

	private File fFile;
	private Document fParentDocument;
	private WorkbenchAdapter fWorkbenchAdapter = new WorkbenchAdapter();

	public GrammarFileEditorInput(File file, Document parentDocument) {
		fFile= file;
		fParentDocument = parentDocument;
		fWorkbenchAdapter = new WorkbenchAdapter();
	}

	@Override
	public boolean exists() {
		return fFile.exists();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return fFile.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return fFile.getAbsolutePath();
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (ILocationProvider.class.equals(adapter))
			return this;
		if (IWorkbenchAdapter.class.equals(adapter))
			return fWorkbenchAdapter;
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	@Override
	public IPath getPath(Object element) {
		if (element instanceof GrammarFileEditorInput) {
			GrammarFileEditorInput input = (GrammarFileEditorInput) element;
			return Path.fromOSString(input.fFile.getAbsolutePath());
		}
		return null;
	}

    @Override
	public IPath getPath() {
        return Path.fromOSString(fFile.getAbsolutePath());
    }

    public Document getParentDocument() {
		return fParentDocument;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (o instanceof GrammarFileEditorInput) {
			GrammarFileEditorInput input = (GrammarFileEditorInput) o;
			return fFile.equals(input.fFile);
		}

        if (o instanceof IPathEditorInput) {
            IPathEditorInput input= (IPathEditorInput)o;
            return getPath().equals(input.getPath());
        }

		return false;
	}

	@Override
	public int hashCode() {
		return fFile.hashCode();
	}
}
