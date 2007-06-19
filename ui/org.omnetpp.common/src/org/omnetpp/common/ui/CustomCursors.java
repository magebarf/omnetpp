package org.omnetpp.common.ui;

import static org.omnetpp.common.image.ImageFactory.CURSOR_IMAGE_ZOOMIN;

import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.omnetpp.common.image.ImageFactory;

/**
 * Custom cursors.
 *
 * @author tomi
 */
public class CustomCursors {

	/** Zoom in cursor */
	public static final Cursor ZOOMIN;
	
	static {
		ZOOMIN = createCursor(CURSOR_IMAGE_ZOOMIN, 0, 0);
	}
	
	private static Cursor createCursor(String imageName, int hotspotX, int hotspotY) {
		Image image = ImageFactory.getImage(imageName);
		if (image != null) {
			return new Cursor(null, image.getImageData(), hotspotX, hotspotY);
		}
		else
			return null;
	}
}
