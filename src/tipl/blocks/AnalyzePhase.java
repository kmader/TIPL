package tipl.blocks;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.tools.ComponentLabel;
import tipl.tools.GrayAnalysis;
import tipl.tools.Neighbors;
import tipl.util.ArgumentParser;
import tipl.util.ITIPLPluginIO;
import tipl.util.TIPLGlobal;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;

/**
 * Run analysis on a thresheld phase
 * 
 * @author mader
 * 
 */
public class AnalyzePhase extends BaseTIPLBlock {
	/**
	 * A class for the shape and neighbor analysis based on two easily
	 * overridden functions for generating the voronoi and neighborhood
	 * calculating plugins
	 * 
	 * @author mader
	 * 
	 */
	public static class ShapeAndNeighborAnalysis {
		public final boolean useGrownLabel = false;
		ITIPLPluginIO myNH = new Neighbors();

		public TImg[] execute(final TImgRO labeledImage, final TImgRO maskImage,
				final String phName, final boolean writeShapeTensor) {
			// now make the dilation
			final ITIPLPluginIO vorn = getGrowingPlugin(labeledImage, maskImage);
			vorn.execute();
			final TImg growOut = vorn.ExportImages(labeledImage)[0];
			final TImgRO cImg = (useGrownLabel) ? (labeledImage) : (growOut);
			GrayAnalysis.StartLacunaAnalysis(cImg, phName + "_1.csv", "Mask",
					writeShapeTensor);
			GrayAnalysis.AddDensityColumn(growOut, phName + "_1.csv", phName
					+ "_2.csv", "Density");
			final ITIPLPluginIO myNH = getNeighborPlugin();
			myNH.LoadImages(new TImgRO[] { growOut });
			myNH.execute();
			myNH.execute("WriteNeighborList", phName + "_edge.csv");
			final TImg NHimg = myNH.ExportImages(growOut)[0];
			TImgTools.WriteBackground(NHimg, phName + "_nh.tif");
			GrayAnalysis.AddRegionColumn(growOut, NHimg, phName + "_2.csv",
					phName + "_3.csv", "Neighbors");
			return new TImg[] { growOut, NHimg };
		}

		/**
		 * 
		 * @param obj
		 * @param mask
		 * @return a plugin object which when executed fills the mask with the
		 *         obj (voronoi transform or something like that)
		 */
		public ITIPLPluginIO getGrowingPlugin(final TImgRO obj, final TImgRO mask) {
			ITIPLPluginIO KV = TIPLPluginManager.createBestPluginIO("kVoronoi",new TImgRO[] {obj,mask});
			KV.LoadImages(new TImgRO[] {obj,mask});
			return KV;
		}

		/**
		 * 
		 * @return a plugin object for managing neighbors
		 */
		public ITIPLPluginIO getNeighborPlugin() {
			return myNH;
		}
	}

	public String prefix;
	public int minVoxCount;
	public String phaseName;
	// public double sphKernelRadius;
	public boolean writeShapeTensor;
	ITIPLPluginIO CL = TIPLPluginManager.createFirstPluginIO("ComponentLabel");
	public final IBlockImage[] inImages = new IBlockImage[] {
			new BlockImage("segmented", "segmented.tif", "Segmented image",
					true),
			new BlockImage("mask", "mask.tif", "Mask Image", true) };

	public final IBlockImage[] outImages = new IBlockImage[] { new BlockImage(
			"labeled", "labeled.tif", "Labeled Image", false) };

	protected ShapeAndNeighborAnalysis SNA = new ShapeAndNeighborAnalysis();

	public AnalyzePhase() {
		super("AnalyzePhase");
		prefix = "";
	}

	public AnalyzePhase(final String inPrefix) {
		super("AnalyzePhase");
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
		final TImg segmentAim = TImgTools.ReadTImg(
				getFileParameter("segmented"), true, true);

		// volume filter
		CL.LoadImages(new TImgRO[] { segmentAim });

		// myCL.useSphKernel(sphKernelRadius)
		CL.execute(); // count only objects with more than 5 voxels minvoxel
		TImgTools.RemoveTImgFromCache(getFileParameter("segmented"));
		final TImg labImg = CL.ExportImages(segmentAim)[0];
		TImgTools.WriteBackground(labImg, phaseName + ".tif");
		final TImg maskImg = TImgTools.ReadTImg(getFileParameter("mask"), true,
				true);
		final TImg[] outImages = SNA.execute(labImg, maskImg, phaseName,
				writeShapeTensor);
		TImgTools.WriteBackground(outImages[0], phaseName + "_dens.tif");

		TImgTools.WriteBackground(outImages[1], phaseName + "_nh.tif");
		return true;
	}

	@Override
	protected String getDescription() {
		return "Run analysis on a thresheld phase";
	}

	@Override
	public String getPrefix() {
		return prefix;
	}
	@Override
	public void setPrefix(String newPrefix) {
		prefix=newPrefix;
		
	}

	@Override
	public ArgumentParser setParameterBlock(final ArgumentParser p) {
		minVoxCount = p.getOptionInt(prefix+"minvoxcount", 1, "Minimum voxel count");
		// sphKernelRadius=p.getOptionDouble("sphkernelradius",1.74,"Radius of spherical kernel to use for component labeling: vertex sharing is sqrt(3)*r, edge sharing is sqrt(2)*r,face sharing is 1*r ");
		writeShapeTensor = p.getOptionBoolean(prefix+"shapetensor",true,"Include Shape Tensor");
		phaseName = p.getOptionString(prefix+"phase", "pores", "Phase name");
		final ArgumentParser p2 = SNA.getNeighborPlugin().setParameter(p,
				prefix);
		return CL.setParameter(p2, prefix);
	}

	

}