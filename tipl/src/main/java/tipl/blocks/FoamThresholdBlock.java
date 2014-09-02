/**
 * 
 */
package tipl.blocks;

import tipl.formats.TImg;
import tipl.tools.ComponentLabel;
import tipl.tools.Morpho;
import tipl.util.ArgumentParser;
import tipl.util.D3int;

/**
 * The threshold code used for processing liquid foam samples (morphological cleaning and the like
 * @author mader
 *
 */
public class FoamThresholdBlock extends ThresholdBlock {
	@BaseTIPLBlock.BlockIdentity(blockName = "Foam Threshold",
			inputNames= {"gray valued image"}, 
			outputNames= {"threshold image", "inverse threshold", "mask image"})
	final public static TIPLBlockFactory myFactory = new BaseTIPLBlock.TIPLBlockFactory() {
		@Override
		public ITIPLBlock get() {
			return new FoamThresholdBlock();
		}
	};

	/**
	 * 
	 */
	public FoamThresholdBlock() {
		super("Foam","");
	}

	/**
	 * @param inPrefix
	 */
	public FoamThresholdBlock(String inPrefix) {
		super("Foam",inPrefix);
	}


	@Override
	protected String getDescription() {
		return super.getDescription()+" with additional morphological operations to clean foam images";
	}

	protected int closeIter=1,openIter=1,closeNH=1,openNH=1,dilNH=1;
	protected double eroOccup=1.0,morphRadius=1.75,dilOccup=1.0,minVolumePct=5;
	protected boolean clthresh,clnotthresh;

	/** adds parameters to the standard threshold block */
	@Override
	public ArgumentParser setParameterBlock(final ArgumentParser inp) {
		final ArgumentParser p=super.setParameterBlock(inp);
		closeIter = p.getOptionInt(prefix+"closeiter", closeIter,
				"Number of closing iterations to perform on PlatBorders");
		closeNH = p.getOptionInt(prefix+"closenh", closeNH, "Neighborhood used for closing");

		openIter = p.getOptionInt(prefix+"openiter", openIter,
				"Number of opening iterations to perform on Bubbles");
		openNH = p.getOptionInt(prefix+"opennh", openNH,
				"Neighborhood used for opening");
		eroOccup = p
				.getOptionDouble(prefix+"erooccup", eroOccup,
						"Minimum neighborhood occupancy % for to prevent erosion deletion");
		dilNH = p.getOptionInt(prefix+"dilnh", dilNH, "Neighborhood used for dilation");

		dilOccup = p.getOptionDouble(prefix+"diloccup", dilOccup,
				"Minimum neighborhood occupancy % for dilation addition");
		morphRadius = p
				.getOptionDouble(
						prefix+"morphRadius",
						morphRadius,
						"Radius to use for the kernel (vertex shaing is sqrt(3), edge sharing is sqrt(2), and face sharing is 1)");

		clthresh=p.getOptionBoolean(prefix+"clthresh","Perform component labeling on thresheld image");
		clnotthresh=p.getOptionBoolean(prefix+"clnotthresh","Perform component labeling on inverse thresheld image");
		minVolumePct = p.getOptionDouble(prefix+"clminpct", minVolumePct,
				"Minimum Volume Percent for Component labeling");

		return p;
	}

	@Override
	protected TImg postThreshFunction(TImg inImage) {
		// Close the thresheld image
		final TImg afterCloseImg;
		if (closeIter>0) {
			Morpho platClose = new Morpho(inImage);

			platClose.erode(new D3int(2, 2, 0), 0.2); // Remove single xy voxels

			platClose.closeMany(closeIter, closeNH, 1.0);
			platClose.useSphKernel(morphRadius * closeNH);
			// Erode once to remove small groups 5/125 of voxels
			platClose.erode(2, 0.08);
			// Remove single voxels groups in the xy plane

			platClose.erode(new D3int(2, 2, 0), 0.2);

			afterCloseImg = platClose.ExportImages(inImage)[0];

		} else { 
			afterCloseImg=inImage;
		}
		
		if (clthresh) return performComponentLabeling(afterCloseImg,minVolumePct);
		else return afterCloseImg;
	}

	@Override
	protected TImg postNotthreshFunction(TImg inImage) {
		final TImg afterCloseImg;
		if (closeIter>0) {
			Morpho bubOpen = new Morpho(inImage);
			bubOpen.openMany(openIter, openNH, 1.0);
			bubOpen.useSphKernel(morphRadius * openNH);
			// bubOpen.erode(2,0.041);
			afterCloseImg = bubOpen.ExportImages(inImage)[0];
		} else {
			afterCloseImg=inImage;
		}
		if (clnotthresh) return performComponentLabeling(afterCloseImg,minVolumePct);
		else return afterCloseImg;
	}
	protected static TImg performComponentLabeling(TImg inImage,double minVolPct) {
		System.out.println("Running Component Label Check " + inImage.getSampleName()
				+ " ...");
		final ComponentLabel myCL = new ComponentLabel(inImage);
		myCL.runRelativeVolume(minVolPct, 101);
		return myCL.ExportImages(inImage)[1];
	}



}
