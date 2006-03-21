/*
 * (c) Copyright 2002, 2005 Uwe Voigt
 * All Rights Reserved.
 */
package gedit.editor;

import gedit.GrammarEditorPlugin;
import gedit.model.ModelBase;
import gedit.model.Problem;

import java.util.Arrays;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ModelLabelProvider extends WorkbenchLabelProvider {
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

		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof OverlayIcon))
				return false;
			OverlayIcon other = (OverlayIcon) o;
			return fBase.equals(other.fBase) && Arrays.equals(fOverlays, other.fOverlays) &&
					Arrays.equals(fLocations, other.fLocations);
		}
		
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

		protected void drawCompositeImage(int width, int height) {
			ImageData bg = fBase.getImageData();
			drawImage(bg, 0, 0);
			
			drawOverlays(fOverlays, fLocations);
		}
		protected Point getSize() {
			return fSize;
		}
	};
	
	private final static int DEFAULT_WIDTH = 16;
	private final static int DEFAULT_HEIGHT = 16;
	private final static int TOP_LEFT = 1;
	private final static int TOP_RIGHT = 2;
	private final static int BOTTOM_LEFT = 3;
	private final static int BOTTOM_RIGHT = 4;
	
	private final static ImageDescriptor[] OVERLAYS = {
		GrammarEditorPlugin.getImageDescriptor("icons/warning_co.gif"), //$NON-NLS-1$
		GrammarEditorPlugin.getImageDescriptor("icons/error_co.gif"), //$NON-NLS-1$
	};
	
	protected ImageDescriptor decorateImage(ImageDescriptor input, Object element) {
		if (!(element instanceof ModelBase))
			return input;
		Problem[] problems = ((ModelBase) element).getProblems();
		if (problems.length == 0)
			return input;
		int[] locations = { 0, 0 };
		for (int i = 0; i < problems.length; i++) {
			Problem problem = problems[i];
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
