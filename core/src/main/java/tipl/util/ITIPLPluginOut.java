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
public interface ITIPLPluginOut extends ITIPLPlugin {
	/**
	 * All plug-ins have an interface for exporting the main result to an Aim
	 * class based on a template aim, many have other methods for exporting the
	 * secondary results (distance maps, histograms, shape analyses, but these
	 * need to be examined individually
	 * 
	 * @param templateAim
	 *            TemplateAim is an aim file which will be used in combination
	 *            with the array of data saved in the plugin to generate a full
	 *            aim output class (element size, procedural log, etc..)
	 */
	public TImg[] ExportImages(TImgRO templateImage);
}
