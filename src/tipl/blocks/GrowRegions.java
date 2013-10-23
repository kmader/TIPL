package tipl.blocks;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.tools.cVoronoi;
import tipl.tools.kVoronoiShrink;
import tipl.util.ArgumentParser;
import tipl.util.ITIPLPluginIO;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

/**
 * Takes labeled regions and grows them into a mask image (bubbles, cells, etc)
 * Optionally runs shape and neighbor analyses
 * 
 * @author mader
 * 
 */
public class GrowRegions extends BaseTIPLBlock {
	protected static class GrownShapeNeighborAnalysis extends
			AnalyzePhase.ShapeAndNeighborAnalysis {
		public final boolean useGrownLabel = true;
		public int fillType = 0;

		@Override
		public ITIPLPluginIO getGrowingPlugin(final TImgRO obj, final TImgRO mask) {
			switch (fillType) {
			case 0:
				return new kVoronoiShrink(obj, mask);
			case 1:
				return new cVoronoi(obj, mask, false);
			default:
				throw new IllegalArgumentException("Cannot use fillType:"
						+ fillType + " in GrowRegions");
			}

		}

	}

	public final String prefix;
	public String phaseName;
	protected boolean writeShapeTensor;
	protected double sphKernelRadius;
	protected GrownShapeNeighborAnalysis SNA = new GrownShapeNeighborAnalysis();
	public final IBlockImage[] inImages = new IBlockImage[] {
			new BlockImage("labels", "label.tif", "Labeled image", true),
			new BlockImage("mask", "mask.tif", "Mask Image", true) };

	public final IBlockImage[] outImages = new IBlockImage[] {
			new BlockImage("fillvols", "filled_labels.tif", "Filled Image",
					true),
			new BlockImage("fillnh", "filled_nh.tif", "Filled Neighborhood",
					false) };

	public GrowRegions() {
		super("GrowRegions");
		prefix = "";
	}

	public GrowRegions(final String inPrefix) {
		super("GrowRegions");
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
		final TImgRO labelAim = getInputFile("labels");
		final TImgRO maskImg = getInputFile("mask");
		final TImg[] outImages = SNA.execute(labelAim, maskImg, phaseName,
				writeShapeTensor);
		TImgTools.WriteBackground(outImages[0], getFileParameter("fillvols"));
		TImgTools.WriteBackground(outImages[1], getFileParameter("fillnh"));
		return true;
	}

	@Override
	protected String getDescription() {
		return "Fill a labeled image into a mask";
	}

	@Override
	public String getPrefix() {
		return prefix;
	}

	@Override
	public ArgumentParser setParameterBlock(final ArgumentParser p) {
		SNA.fillType = p
				.getOptionInt(
						"filltype",
						0,
						"Bubble filling method (0= Surface Voronoi, 1 = Centroid Voronoi",
						0, 1);
		sphKernelRadius = p
				.getOptionDouble(
						"sphkernelradius",
						1.74,
						"Radius of spherical kernel to use for component labeling: vertex sharing is sqrt(3)*r, edge sharing is sqrt(2)*r,face sharing is 1*r ");
		writeShapeTensor = p.getOptionBoolean("shapetensor",
				"Include Shape Tensor");
		phaseName = p.getOptionString("phase", "filled_labels", "Phase name");
		TIPLGlobal.availableCores = p.getOptionInt("maxcores",
				TIPLGlobal.availableCores,
				"Number of cores/threads to use for processing");
		final ArgumentParser p2 = SNA.getNeighborPlugin().setParameter(p,
				prefix);
		return p2;
	}

}