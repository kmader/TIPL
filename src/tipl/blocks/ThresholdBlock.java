package tipl.blocks;

import tipl.formats.MappedImage;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.tools.EasyContour;
import tipl.tools.Peel;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TImgTools;

/** perform a threshold on an input image and remove edges if needed **/
public class ThresholdBlock extends BaseTIPLBlock {
	protected static TImg makeMask(final TImgRO cAim, final double remEdgesRadius) {
		EasyContour myContour = new EasyContour(cAim);
		myContour.useFixedCirc(remEdgesRadius);
		myContour.execute();
		cAim.appendProcLog(myContour.getProcLog());
		return myContour.ExportImages(cAim)[0];
	}
	/** A simple circular contour, edge removal, and peeling */
	public static TImg removeEdges(final TImgRO cAim, final double remEdgesRadius) {
		
		final Peel cPeel = new Peel(cAim,makeMask(cAim,remEdgesRadius), new D3int(
				1));
		System.out.println("Calculating Remove Edges Peel " + cAim + " ...");
		cPeel.execute();
		return cPeel.ExportImages(cAim)[0];
	}

	protected double threshVal, remEdgesRadius;
	protected boolean rmEdges,flipThreshold;
	public final String prefix;
	
	public final IBlockImage[] inImages = new IBlockImage[] { new BlockImage(
			"gfilt", "", "Input filtered image", true) };

	public final IBlockImage[] outImages = new IBlockImage[] {
			new BlockImage("threshold", "threshold.tif",
					"BW image with values above the threshold", true),
			new BlockImage("notthreshold", "",
					"BW image with values below the threshold", false),
			new BlockImage("mask", "",
					"Mask image containing the sum of both phases", false)};
	
	final protected String opString = ">";
	final protected String iopString = "<";

	public ThresholdBlock() {
		super("Threshold");
		prefix = "";
	}

	public ThresholdBlock(final String inPrefix) {
		super("Threshold");
		prefix = inPrefix;
	}
	/**
	 * for subclasses of this block
	 * @param namePrefix
	 * @param inPrefix
	 */
	protected ThresholdBlock(final String namePrefix,final String inPrefix) {
		super(namePrefix+"Threshold");
		prefix = inPrefix;
	}

	@Override
	protected IBlockImage[] bGetInputNames() {
		return inImages;
	}

	@Override
	protected IBlockImage[] bGetOutputNames() {
		return outImages;
	}
	protected boolean useFloat=true;
	@Override
	public boolean executeBlock() {
		TImg rawImg = TImgTools.WrapTImgRO(getInputFile("gfilt"));
		
		TImgRO.FullReadable rawImgPlus = TImgTools.makeTImgFullReadable(rawImg);
		//Object outImage
		float[] inImg = rawImgPlus.getFloatAim();
		final boolean isFlipped=flipThreshold;
		/**
		 * Threshold the data (this is not a nice solution but it works for now)
		 */
		final boolean[] scdat = new boolean[inImg.length];
		for (int i = 0; i < inImg.length; i++) {
			if (isFlipped) 
				scdat[i] = inImg[i] < threshVal;
			else 
				scdat[i] = inImg[i] > threshVal;
		}
		rawImgPlus = null;
		TImg threshImg = rawImg.inheritedAim(scdat, rawImg.getDim(),
				rawImg.getOffset());
		threshImg.appendProcLog("CMD:Threshold, Value:" + (isFlipped ? iopString : opString) + " "
				+ threshVal);
		TImgTools.RemoveTImgFromCache(getFileParameter("gfilt"));
		/*
		 * perform some post threshold operations on the image to clean it up if needed
		 */
		threshImg=postThreshFunction(threshImg);
		if (getFileParameter("notthreshold").length() > 0) {
			TImgRO notThreshImg = new MappedImage.InvertImage(threshImg, 10, 1);

			notThreshImg.appendProcLog("CMD:Threshold, Value:" + (isFlipped ? opString : iopString)
					+ " " + threshVal);
			notThreshImg=postNotthreshFunction(TImgTools.WrapTImgRO(notThreshImg));
			finishImages(notThreshImg, getFileParameter("notthreshold"));
		}
		if (getFileParameter("mask").length() > 0) {
			finishImages(new MappedImage.FixedImage(threshImg, 10, 1), getFileParameter("mask"));
		}
		finishImages(threshImg, getFileParameter("threshold"));

		rawImg = null;

		return true;
	}
	/**
	 * The function performs post thresholding tasks on the binary image before the notthresh image is produced
	 * can be used for morphological operations and similar tasks
	 * @param inImage image directly after thresholding
	 * @return
	 */
	protected TImg postThreshFunction(TImg inImage) {
		return inImage;
	}
	
	/**
	 * The function performs post thresholding tasks on the inverse of the binary image after the notthresh image is produced
	 * can be used for morphological operations and similar tasks. Default is to run the postthreshfunction
	 * @param inImage image directly after thresholding
	 * @return
	 */
	protected TImg postNotthreshFunction(TImg inImage) {
		return postThreshFunction(inImage);
	}

	protected void finishImages(TImgRO inImage, final String inName) {
		if (rmEdges)
			inImage = removeEdges(inImage, remEdgesRadius);
		TImgTools.WriteTImg(inImage, inName, true);
	}

	@Override
	protected String getDescription() {
		return "Generic Block for filtering and rescaling an image";
	}

	@Override
	public String getPrefix() {
		return prefix;
	}

	@Override
	public ArgumentParser setParameterBlock(final ArgumentParser p) {
		threshVal = p.getOptionDouble(prefix + "threshvalue", 2200,
				"Value used to threshold image");
		flipThreshold = p.getOptionBoolean(prefix + "flipthresh",
				"Flip the threshold criteria (<) instead of (>)");
		rmEdges = p.getOptionBoolean(prefix + "removeedges",
				"Leave edges when making contour");
		remEdgesRadius = p.getOptionDouble(prefix + "edgeradius", 1.0,
				"% of radius to use for removing edges");
		return p;
	}

}