/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit;

import gedit.editor.ColorManager;
import gedit.model.Document;
import gedit.model.DocumentAnalyzer;
import gedit.model.IProblemRequestor;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.osgi.framework.BundleContext;

public class GrammarEditorPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static GrammarEditorPlugin fPlugin;
	//Resource bundle.
	private ResourceBundle fResourceBundle;
	// Images
	private Map fImages;
	private Map fModels;
	private ColorManager fColorManager;
	private IPreferenceStore fCombinedPreferenceStore;
	
	/**
	 * The constructor.
	 */
	public GrammarEditorPlugin() {
		super();
		fPlugin = this;
		fColorManager = new ColorManager();
		try {
			fResourceBundle = ResourceBundle.getBundle("gedit.GrammarEditorResources");
		} catch (MissingResourceException x) {
			fResourceBundle = null;
		}
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		fColorManager.dispose();
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static GrammarEditorPlugin getDefault() {
		return fPlugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return fResourceBundle;
	}
	
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(getDefault().getBundle().getSymbolicName(), path);
	}
	
	private static Image doGetImage(Object key) {
		Map images = getDefault().fImages;
		if (images == null)
			getDefault().fImages = images = new HashMap();
		Image image = (Image) images.get(key);
		if (image == null) {
			ImageDescriptor descriptor = null;
			if (key instanceof ImageDescriptor)
				descriptor = (ImageDescriptor) key;
			else if (key instanceof String)
				descriptor = getImageDescriptor((String) key);
			else
				descriptor = ImageDescriptor.getMissingImageDescriptor();
			images.put(key, image = descriptor.createImage());
		}
		return image;
	}

	public static Image getImage(ImageDescriptor descriptor) {
		return doGetImage(descriptor);
	}

	public static Image getImage(String path) {
		return doGetImage(path);
	}
	
	public static ColorManager getColorManager() {
		return getDefault().fColorManager;
	}

	public static Document getDocumentModel(IDocument document, IProblemRequestor probemRequestor, boolean reconcile) {
		Map models = getDefault().fModels;
		if (models == null)
			getDefault().fModels = models = new WeakHashMap();
		Document doc = null;
		synchronized (document) {
			doc = (Document) models.get(document);
			if (doc == null || reconcile) {
				models.put(document, doc = new DocumentAnalyzer(document.get(), probemRequestor).analyze(doc));
				if (getDefault().isDebugging())
					System.out.println(models.size() + " documents cached");
			}
		}
		return doc;
	}

	public static void logError(String message, Throwable exception) {
		if (exception != null)
			exception.printStackTrace();
		if (message == null)
			message = "No message provided";
		getDefault().getLog().log(new Status(IStatus.ERROR, getDefault().
				getBundle().getSymbolicName(), 0, message, exception));
	}

	public IPreferenceStore getCombinedPreferenceStore() {
		if (fCombinedPreferenceStore == null) {
			IPreferenceStore generalTextStore = EditorsUI.getPreferenceStore(); 
			fCombinedPreferenceStore = new ChainedPreferenceStore(new IPreferenceStore[] {
					getPreferenceStore(), generalTextStore });
		}
		return fCombinedPreferenceStore;
	}
}