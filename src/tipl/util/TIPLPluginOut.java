/**
 * 
 */
package tipl.util;

import tipl.formats.TImg;
import tipl.formats.TImgRO;

/**
 * For plugins that produce output.
 * 
 * @author mader
 * 
 */
public interface TIPLPluginOut extends TIPLPlugin {
	/**
	 * export the images that are outputs of this plugin output is TImg since it
	 * should be writable
	 * 
	 * @return output images as array
	 */
	public TImg[] ExportImages(TImgRO templateImage);
}
