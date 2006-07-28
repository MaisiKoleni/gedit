/*
 * (c) Copyright 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit;

import gedit.editor.ColorManager;
import gedit.editor.GrammarDocument;
import gedit.editor.PreferenceConstants;
import gedit.model.Document;
import gedit.model.DocumentAnalyzer;
import gedit.model.DocumentOptions;
import gedit.model.FileProzessor;
import gedit.model.IProblemRequestor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.osgi.framework.BundleContext;

public class GrammarEditorPlugin extends AbstractUIPlugin implements IPropertyChangeListener {
	//The shared instance.
	private static GrammarEditorPlugin fPlugin;
	//Resource bundle.
	private ResourceBundle fResourceBundle;
	// Images
	private Map fImages;
	private Map fModels;
	private FileProzessor fFileProzessor;
	private ColorManager fColorManager;
	private IPreferenceStore fCombinedPreferenceStore;
	private DocumentOptions fGlobalDocumentOptions;
	
	public final static boolean DEBUG = Boolean.valueOf(Platform.getDebugOption("gedit/debug")).booleanValue();
	public final static int DEBUG_PARSER_LEVEL = getIntDebugOption("gedit/parser/level");
	
	private static int getIntDebugOption(String name) {
		try {
			return Integer.valueOf(Platform.getDebugOption(name)).intValue();
		} catch (Exception ignore) {
			return 0;
		}
	}

	/**
	 * The constructor.
	 */
	public GrammarEditorPlugin() {
		super();
		fPlugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		initGlobalDocumentOptions();
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		if (fColorManager != null)
			fColorManager.dispose();
		disposeImages();
		getPreferenceStore().removePropertyChangeListener(this);
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
		if (fResourceBundle != null)
			return fResourceBundle;
		try {
			fResourceBundle = ResourceBundle.getBundle("gedit.GrammarEditorResources");
		} catch (MissingResourceException x) {
			try {
				fResourceBundle = new PropertyResourceBundle(new ByteArrayInputStream(new byte[0]));
			} catch (IOException ignore) {
			}
		}
		return fResourceBundle;
	}
	
	private void disposeImages() {
		if (fImages == null)
			return;
		for (Iterator it = fImages.values().iterator(); it.hasNext();) {
			Image image = (Image) it.next();
			image.dispose();
		}
		fImages.clear();
		fImages = null;
	}

	private void initGlobalDocumentOptions() {
		fGlobalDocumentOptions = new DocumentOptions();
		fGlobalDocumentOptions.setIncludeDirs(StringUtils.split(getPreferenceStore().getString(PreferenceConstants.GRAMMAR_INCLUDE_DIRECTORIES),
				PreferenceConstants.INCLUDE_DIRECTORIES_SEPARATOR));
		getPreferenceStore().addPropertyChangeListener(this);
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (PreferenceConstants.GRAMMAR_INCLUDE_DIRECTORIES.equals(event.getProperty()))
			fGlobalDocumentOptions.setIncludeDirs(StringUtils.split((String) event.getNewValue(),
					PreferenceConstants.INCLUDE_DIRECTORIES_SEPARATOR));
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
	
	public static FileProzessor getFileProzessor() {
		return getDefault().fFileProzessor != null ? getDefault().fFileProzessor : (getDefault().fFileProzessor = new FileProzessor());
	}

	public static ColorManager getColorManager() {
		return getDefault().fColorManager != null ? getDefault().fColorManager : (getDefault().fColorManager = new ColorManager());
	}

	public static Document getDocumentModel(IDocument document, IProblemRequestor probemRequestor, boolean reconcile) {
		Map models = getDefault().fModels;
		if (models == null)
			getDefault().fModels = models = new WeakHashMap();
		Document doc = null;
		doc = (Document) models.get(document);
		if (doc != null && !reconcile)
			return doc;

		synchronized (document) {
			Document parentDocument = document instanceof GrammarDocument ? ((GrammarDocument) document).getParentDocument() : null;
			DocumentAnalyzer analyzer = parentDocument != null ? new DocumentAnalyzer(probemRequestor, getFileProzessor(), parentDocument)
					: new DocumentAnalyzer(probemRequestor, getFileProzessor(), getDefault().fGlobalDocumentOptions);
			models.put(document, doc = analyzer.analyze(doc, document.get()));
			if (DEBUG)
				System.out.println(models.size() + " documents cached");
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