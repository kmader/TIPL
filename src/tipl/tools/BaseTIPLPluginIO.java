package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.D3int;
import tipl.util.ITIPLPluginIO;
import tipl.util.ITIPLPluginOut;
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
		ITIPLPluginOut, ITIPLPluginIO {

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
	



}
