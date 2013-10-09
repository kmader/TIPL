package tipl.blocks;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.tools.EasyContour;
import tipl.tools.Peel;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TImgTools;

/** perform a threshold on an input image and remove edges if needed **/
public class ThresholdBlock extends BaseTIPLBlock {
	/** A simple circular contour, edge removal, and peeling */
	public static TImg removeEdges(final TImg cAim, final double remEdgesRadius) {
		EasyContour myContour = new EasyContour(cAim);
		myContour.useFixedCirc(remEdgesRadius);
		myContour.run();
		cAim.appendProcLog(myContour.getProcLog());
		final Peel cPeel = new Peel(cAim, myContour.ExportAim(cAim), new D3int(
				1));
		myContour = null;
		System.out.println("Calculating Remove Edges Peel " + cAim + " ...");
		cPeel.run();
		return cPeel.ExportAim(cAim);
	}

	protected double threshVal, remEdgesRadius;
	protected boolean rmEdges;
	public final String prefix;
	public final IBlockImage[] inImages = new IBlockImage[] { new BlockImage(
			"gfilt", "", "Input filtered image", true) };

	public final IBlockImage[] outImages = new IBlockImage[] {
			new BlockImage("threshold", "threshold.tif",
					"BW image with values above the threshold", true),
			new BlockImage("notthreshold", "",
					"BW image with values below the threshold", false) };
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

	@Override
	protected IBlockImage[] bGetInputNames() {
		return inImages;
	}

	@Override
	protected IBlockImage[] bGetOutputNames() {
		return outImages;
	}

	@Override
	public boolean executeBlock() {
		TImg rawImg = TImgTools.ReadTImg(getFileParameter("gfilt"), true, true);
		TImgRO.FullReadable rawImgPlus = TImgTools.makeTImgFullReadable(rawImg);
		short[] inImg = rawImgPlus.getShortAim();

		// Threshold the data
		final boolean[] scdat = new boolean[inImg.length];
		for (int i = 0; i < inImg.length; i++) {
			scdat[i] = inImg[i] > threshVal;
		}
		inImg = null;
		rawImgPlus = null;
		final TImg threshImg = rawImg.inheritedAim(scdat, rawImg.getDim(),
				rawImg.getOffset());
		threshImg.appendProcLog("CMD:Threshold, Value:" + opString + " "
				+ threshVal);
		TImgTools.RemoveTImgFromCache(getFileParameter("gfilt"));
		if (getFileParameter("notthreshold").length() > 0) {
			final boolean[] ncdat = new boolean[inImg.length];
			for (int i = 0; i < inImg.length; i++) {
				ncdat[i] = inImg[i] < threshVal;
			}
			final TImg notThreshImg = rawImg.inheritedAim(ncdat,
					rawImg.getDim(), rawImg.getOffset());
			notThreshImg.appendProcLog("CMD:Threshold, Value:" + iopString
					+ " " + threshVal);
			finishImages(notThreshImg, getFileParameter("notthreshold"));
		}
		finishImages(threshImg, getFileParameter("threshold"));

		rawImg = null;

		return true;
	}

	protected void finishImages(TImg inImage, final String inName) {
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
		threshVal = p.getOptionInt(prefix + "threshvalue", 2200,
				"Value used to threshold image");
		rmEdges = p.getOptionBoolean("removeedges",
				"Leave edges when making contour");
		remEdgesRadius = p.getOptionDouble("edgeradius", 1.0,
				"% of radius to use for removing edges");
		return p;
	}

}