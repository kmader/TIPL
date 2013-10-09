package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.D3int;
import tipl.util.TIPLPluginIO;
import tipl.util.TIPLPluginOut;
import tipl.util.TImgTools;

/**
 * Abstract Class for performing TIPLPlugin TIPLPlugin is the class for Plug-ins
 * in the TIPL framework. A plugin should accept an AIM file as an input /
 * constructor. A plugin should then be able to be run using its run function
 * (it implements runnable to make threads and the executors easier) A plugin
 * must have an ExportAim function for writing its output into a
 * TImgTools.ReadTImg memory object
 * **/
abstract public class BaseTIPLPluginIO extends BaseTIPLPluginIn implements
		TIPLPluginOut, TIPLPluginIO {

	public BaseTIPLPluginIO() {
		super();
	}

	/**
	 * constructor function taking boolean (other castings just convert the
	 * array first) linear array and the dimensions
	 */
	public BaseTIPLPluginIO(final D3int idim, final D3int ioffset) {
		super(idim, ioffset);
	}

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
	@Deprecated
	abstract public TImg ExportAim(TImgRO.CanExport templateAim);

	/**
	 * Default implementation just uses the ExportAim command to produce the
	 * array
	 */
	@Override
	public TImg[] ExportImages(final TImgRO templateImage) {
		// TODO Auto-generated method stub
		final TImg cImg = TImgTools.WrapTImgRO(templateImage);
		return new TImg[] { ExportAim(cImg) };
	}

}
