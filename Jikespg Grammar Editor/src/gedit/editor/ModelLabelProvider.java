/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import gedit.GrammarEditorPlugin;
import gedit.model.ModelBase;
import gedit.model.ModelType;
import gedit.model.Problem;
import gedit.model.Reference;
import gedit.model.Section;

public class ModelLabelProvider extends LabelProvider {
	private class OverlayIcon extends CompositeImageDescriptor {
		private Point fSize = null;
		private ImageDescriptor fBase;
		private ImageDescriptor fOverlays[];
		private int[] fLocations;

		public OverlayIcon(ImageDescriptor base, ImageDescriptor[] overlays, int[] locations) {
			this(base, overlays, locations, new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT));
		}

		public OverlayIcon(ImageDescriptor base, ImageDescriptor[] overlays, int[] locations, Point size) {
			fBase = base;
			if (fBase == null)
				fBase = ImageDescriptor.getMissingImageDescriptor();
			fOverlays = overlays;
			fLocations = locations;
			fSize = size;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof OverlayIcon))
				return false;
			OverlayIcon other = (OverlayIcon) o;
			return fBase.equals(other.fBase) && Arrays.equals(fOverlays, other.fOverlays) &&
					Arrays.equals(fLocations, other.fLocations);
		}

		@Override
		public int hashCode() {
			int code = fBase.hashCode();
			for (int i = 0; i < fOverlays.length; i++) {
				code ^= fOverlays[i].hashCode() + fLocations[i];
			}
			return code;
		}

		protected void drawOverlays(ImageDescriptor[] overlays, int[] locations) {
			Point size = getSize();
			for (int i = 0; i < overlays.length; i++) {
				ImageDescriptor overlay = overlays[i];
				ImageData overlayData = overlay.getImageData();
				switch (locations[i]) {
					case TOP_LEFT:
						drawImage(overlayData, 0, 0);
						break;
					case TOP_RIGHT:
						drawImage(overlayData, size.x - overlayData.width, 0);
						break;
					case BOTTOM_LEFT:
						drawImage(overlayData, 0, size.y - overlayData.height);
						break;
					case BOTTOM_RIGHT:
						drawImage(overlayData, size.x - overlayData.width, size.y - overlayData.height);
						break;
				}
			}
		}

		@Override
		protected void drawCompositeImage(int width, int height) {
			ImageData bg = fBase.getImageData();
			drawImage(bg, 0, 0);

			drawOverlays(fOverlays, fLocations);
		}
		@Override
		protected Point getSize() {
			return fSize;
		}
	}

	private final static int DEFAULT_WIDTH = 16;
	private final static int DEFAULT_HEIGHT = 16;
	private final static int TOP_LEFT = 1;
	private final static int TOP_RIGHT = 2;
	private final static int BOTTOM_LEFT = 3;
	private final static int BOTTOM_RIGHT = 4;

	private final static Map LABELS = createLabels();
	private final static Map MODEL_IMAGES = createModelImages();
	private final static Map SECTION_IMAGES = createSectionImages();

	private final static ImageDescriptor[] OVERLAYS = {
		GrammarEditorPlugin.getImageDescriptor("icons/warning_co.gif"), //$NON-NLS-1$
		GrammarEditorPlugin.getImageDescriptor("icons/error_co.gif"), //$NON-NLS-1$
	};

	private final static int MAX_LABEL_LENGTH = 32;

	private static Map createLabels() {
		Map map = new HashMap();
		map.put(ModelType.OPTION, "Options");
		map.put(ModelType.DEFINITION, "Define");
		map.put(ModelType.TERMINAL, "Terminals");
		map.put(ModelType.ALIAS, "Alias");
		map.put(ModelType.RULE, "Rules");
		map.put(ModelType.NAME, "Names");
		map.put(ModelType.START_TOK, "Start");
		map.put(ModelType.END_TOK, "End");
		map.put(ModelType.EOF_TOK, "Eof");
		map.put(ModelType.EOL_TOK, "Eol");
		map.put(ModelType.ERROR_TOK, "Error");
		map.put(ModelType.EMPTY_TOK, "empty");
		map.put(ModelType.INCLUDE, "Include");
		map.put(ModelType.NOTICE, "Notice");
		map.put(ModelType.EXPORT, "Export");
		map.put(ModelType.IMPORT, "Import");
		map.put(ModelType.HEADER, "Headers");
		map.put(ModelType.TRAILER, "Trailers");
		map.put(ModelType.GLOBAL, "Globals");
		map.put(ModelType.KEYWORD, "Keywords");
		map.put(ModelType.IDENTIFIER, "Identifier");
		map.put(ModelType.TYPE, "Types");
		map.put(ModelType.DROP_ACTION, "DropActions");
		map.put(ModelType.DROP_SYMBOL, "DropSymbols");
		map.put(ModelType.DROP_RULE, "DropRules");
		return Collections.unmodifiableMap(map);
	}

    private static Map createModelImages() {
		Map map = new HashMap();
		map.put(ModelType.OPTION, GrammarEditorPlugin.getImageDescriptor("icons/option.gif")); //$NON-NLS-1$
		map.put(ModelType.ALIAS, GrammarEditorPlugin.getImageDescriptor("icons/alias.gif")); //$NON-NLS-1$
		map.put(ModelType.DEFINITION, GrammarEditorPlugin.getImageDescriptor("icons/definition.gif")); //$NON-NLS-1$
		map.put(ModelType.TERMINAL, GrammarEditorPlugin.getImageDescriptor("icons/terminal.gif")); //$NON-NLS-1$
		map.put(ModelType.RULE, GrammarEditorPlugin.getImageDescriptor("icons/production.gif")); //$NON-NLS-1$
		map.put(ModelType.NAME, GrammarEditorPlugin.getImageDescriptor("icons/name.gif")); //$NON-NLS-1$
		map.put(ModelType.RHS, GrammarEditorPlugin.getImageDescriptor("icons/rhs.gif")); //$NON-NLS-1$
		map.put(ModelType.KEYWORD, GrammarEditorPlugin.getImageDescriptor("icons/keyword.gif")); //$NON-NLS-1$
		map.put(ModelType.INCLUDE, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE));
		return Collections.unmodifiableMap(map);
	}

	private static Map createSectionImages() {
		Map map = new HashMap();
		map.put(ModelType.OPTION, GrammarEditorPlugin.getImageDescriptor("icons/options.gif")); //$NON-NLS-1$
		map.put(ModelType.ALIAS, GrammarEditorPlugin.getImageDescriptor("icons/aliases.gif")); //$NON-NLS-1$
		map.put(ModelType.DEFINITION, GrammarEditorPlugin.getImageDescriptor("icons/definitions.gif")); //$NON-NLS-1$
		map.put(ModelType.TERMINAL, GrammarEditorPlugin.getImageDescriptor("icons/terminals.gif")); //$NON-NLS-1$
		map.put(ModelType.RULE, GrammarEditorPlugin.getImageDescriptor("icons/productions.gif")); //$NON-NLS-1$
		map.put(ModelType.NAME, GrammarEditorPlugin.getImageDescriptor("icons/names.gif")); //$NON-NLS-1$
		map.put(ModelType.EXPORT, GrammarEditorPlugin.getImageDescriptor("icons/exports.gif")); //$NON-NLS-1$
		map.put(ModelType.IMPORT, GrammarEditorPlugin.getImageDescriptor("icons/imports.gif")); //$NON-NLS-1$
		map.put(ModelType.KEYWORD, GrammarEditorPlugin.getImageDescriptor("icons/keywords.gif")); //$NON-NLS-1$

		map.put(ModelType.NOTICE, GrammarEditorPlugin.getImageDescriptor("icons/notice.gif")); //$NON-NLS-1$
		map.put(ModelType.INCLUDE, GrammarEditorPlugin.getImageDescriptor("icons/include.gif")); //$NON-NLS-1$
		map.put(ModelType.HEADER, GrammarEditorPlugin.getImageDescriptor("icons/header.gif")); //$NON-NLS-1$
		map.put(ModelType.TRAILER, GrammarEditorPlugin.getImageDescriptor("icons/trailer.gif")); //$NON-NLS-1$
		map.put(ModelType.GLOBAL, GrammarEditorPlugin.getImageDescriptor("icons/global.gif")); //$NON-NLS-1$
		map.put(ModelType.IDENTIFIER, GrammarEditorPlugin.getImageDescriptor("icons/identifier.gif")); //$NON-NLS-1$
		map.put(ModelType.EOL_TOK, GrammarEditorPlugin.getImageDescriptor("icons/eol_tok.gif")); //$NON-NLS-1$
		map.put(ModelType.EOF_TOK, GrammarEditorPlugin.getImageDescriptor("icons/eof_tok.gif")); //$NON-NLS-1$
		map.put(ModelType.ERROR_TOK, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_ERROR_TSK));
		map.put(ModelType.RECOVER, GrammarEditorPlugin.getImageDescriptor("icons/recover.gif")); //$NON-NLS-1$
		map.put(ModelType.START_TOK, GrammarEditorPlugin.getImageDescriptor("icons/start_tok.gif")); //$NON-NLS-1$
		map.put(ModelType.AST, GrammarEditorPlugin.getImageDescriptor("icons/ast.gif")); //$NON-NLS-1$
		map.put(ModelType.TYPE, GrammarEditorPlugin.getImageDescriptor("icons/type.gif")); //$NON-NLS-1$
		return Collections.unmodifiableMap(map);
	}

	@Override
	public String getText(Object element) {
    	if (element instanceof ModelBase)
    		return getModelLabel((ModelBase) element);
    	if (element instanceof ModelType) {
    		return getTypeLabel((ModelType) element);
    	}
        return super.getText(element);
    }

	private String getModelLabel(ModelBase model) {
		return model.getType() == ModelType.SECTION ? getTypeLabel(((Section) model).getChildType())
				: checkModelLabel(model.getLabel());
	}

	private String getTypeLabel(ModelType type) {
		String label = (String) LABELS.get(type);
		return label != null ? label : type.getString();
	}

    private String checkModelLabel(String label) {
		return label != null && label.length() > MAX_LABEL_LENGTH ? label.substring(0, MAX_LABEL_LENGTH) + " ..." : label;
	}

    @Override
	public Image getImage(Object element) {
    	ImageDescriptor descriptor = null;
    	if (element instanceof ModelBase)
    		descriptor = decorateImage(getModelImageDescriptor((ModelBase) element), element);
    	else if (element instanceof ModelType)
    		descriptor = decorateImage(getModelTypeImageDescriptor((ModelType) element), element);
   		return descriptor != null ? GrammarEditorPlugin.getImage(descriptor) : null;
    }

	private ImageDescriptor getModelImageDescriptor(ModelBase model) {
    	if (model instanceof Reference) {
    		ModelBase refererrer = ((Reference) model).getReferer();
    		if (refererrer != null)
    			model = refererrer;
    	}
    	ModelType type = model.getType();
    	if (model instanceof Section) {
    		type = ((Section) model).getChildType();
        	ImageDescriptor descriptor = (ImageDescriptor) SECTION_IMAGES.get(type);
        	if (descriptor != null)
        		return descriptor;
    	}
    	return getModelTypeImageDescriptor(type);
    }

    private ImageDescriptor getModelTypeImageDescriptor(ModelType type) {
    	ImageDescriptor descriptor = null;
    	if (type != ModelType.SECTION)
    		descriptor = (ImageDescriptor) MODEL_IMAGES.get(type);
    	if (descriptor == null)
    		descriptor = (ImageDescriptor) SECTION_IMAGES.get(type);
    	return descriptor;
	}

    protected ImageDescriptor decorateImage(ImageDescriptor input, Object element) {
		if (!(element instanceof ModelBase))
			return input;
		Problem[] problems = ((ModelBase) element).getProblems();
		if (problems.length == 0)
			return input;
		int[] locations = { 0, 0 };
		for (Problem problem : problems) {
			// error should have higher relevance than warning
			switch (problem.getType()) {
			case Problem.ERROR:
				locations[1] = BOTTOM_LEFT;
				break;
			case Problem.WARNING:
				locations[0] = BOTTOM_LEFT;
				break;
			}
		}
		return new OverlayIcon(input, OVERLAYS, locations);
	}

}
