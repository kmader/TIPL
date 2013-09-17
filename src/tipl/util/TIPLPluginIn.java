/**
 * 
 */
package tipl.util;

import tipl.formats.TImgRO;

/**
 * An interface for plugins accepting input images
 * 
 * @author mader
 * 
 */
public interface TIPLPluginIn extends TIPLPlugin {
	public void LoadImages(TImgRO[] inImages);
}
