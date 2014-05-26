package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.D3int;
import tipl.util.TImgTools;

// Used as a replacement for the moment function as it allows much more control over data
// and communication with webservices (potentially?)
/**
 * Class for plugins with binary input and binary output operations on Aim
 * (linear array) files. Provides interfaces for casting inputs in other formats
 * to binary
 */
abstract public class BaseTIPLPluginBW extends BaseTIPLPluginIO {
	/** First input aim */
	public boolean[] inAim;

	/** Output aim */
	public volatile boolean[] outAim;

	public BaseTIPLPluginBW() {
		isInitialized = false;
	}

	
	@Override
	public TImg[] ExportImages(final TImgRO templateImage) {
		TImgRO.CanExport templateAim = TImgTools.makeTImgExportable(templateImage);
		if (isInitialized) {
			if (runCount > 0) {
				final TImg outAimData = templateAim.inheritedAim(outAim, dim,
						offset);
				outAimData.appendProcLog(procLog);
				return  new TImg[] {outAimData};
			} else {
				System.err
						.println("The plug-in : "
								+ getPluginName()
								+ ", has not yet been run, exported does not exactly make sense, original data will be sent.");
				return new TImg[] {templateAim.inheritedAim(inAim, dim, offset)};
			}
		} else {
			System.err
					.println("The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exported does not make any sense");
			return  new TImg[] {templateAim.inheritedAim(templateAim)};

		}
	}

	/**
	 * initializer function taking boolean (other castings just convert the
	 * array first) linear array and the dimensions
	 */
	@Deprecated
	public void ImportAim(final boolean[] inputmap, final D3int idim,
			final D3int ioffset) {
		aimLength = inputmap.length;
		inAim = new boolean[aimLength];
		for (int i = 0; i < aimLength; i++)
			inAim[i] = inputmap[i];
		InitLabels(idim, ioffset);
	}

	/**
	 * initializer function taking a float (thresheld>0) linear array and the
	 * dimensions
	 */
	@Deprecated
	public void ImportAim(final float[] inputmap, final D3int idim,
			final D3int ioffset) {
		aimLength = inputmap.length;
		inAim = new boolean[aimLength];
		for (int i = 0; i < aimLength; i++)
			inAim[i] = inputmap[i] > 0;
		InitLabels(idim, ioffset);
	}

	/**
	 * initializer function taking int (thresheld>0) linear array and the
	 * dimensions
	 */
	@Deprecated
	public void ImportAim(final int[] inputmap, final D3int idim,
			final D3int ioffset) {
		aimLength = inputmap.length;
		inAim = new boolean[aimLength];
		for (int i = 0; i < aimLength; i++)
			inAim[i] = inputmap[i] > 0;
		InitLabels(idim, ioffset);
	}

	/**
	 * initializer function taking short (thresheld>0) linear array and the
	 * dimensions
	 */
	@Deprecated
	public void ImportAim(final short[] inputmap, final D3int idim,
			final D3int ioffset) {
		aimLength = inputmap.length;
		inAim = new boolean[aimLength];
		for (int i = 0; i < aimLength; i++)
			inAim[i] = inputmap[i] > 0;
		InitLabels(idim, ioffset);
	}

	/** initializer function taking an aim-file */
	public void ImportAim(final TImgRO inImg) {
		ImportAim(TImgTools.makeTImgFullReadable(inImg).getBoolAim(),
				inImg.getDim(), inImg.getOffset());
	}

	protected void InitLabels(final D3int idim, final D3int ioffset) {
		outAim=(boolean[]) TImgTools.watchBigAlloc(TImgTools.IMAGETYPE_BOOL, aimLength);
		InitDims(idim, ioffset);
		isInitialized = true;
	}

	@Override
	public void LoadImages(final TImgRO[] inImages) {
		// TODO Auto-generated method stub
		if (inImages.length < 1)
			throw new IllegalArgumentException(
					"Too few arguments for LoadImages in:" + getPluginName());
		final TImgRO inImg = inImages[0];
		ImportAim(inImg);
	}


}
