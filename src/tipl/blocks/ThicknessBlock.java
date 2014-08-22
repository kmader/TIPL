package tipl.blocks;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.tools.GrayAnalysis;
import tipl.util.ArgumentParser;
import tipl.util.ITIPLPluginIO;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;

/** performs a thickness analysis (similar to the DTO function) using the best available kVoronoi and HildThickness plugins
 * **/
public class ThicknessBlock extends BaseTIPLBlock {
	@BaseTIPLBlock.BlockIdentity(blockName = "ThicknessBlock",
			inputNames= {"threshold image"}, 
			outputNames= {"thickness map","distance map","ridge map"})
	final public static TIPLBlockFactory myFactory = new BaseTIPLBlock.TIPLBlockFactory() {
		@Override
		public ITIPLBlock get() {
			return new ThicknessBlock();
		}
	};

	protected double threshVal,maxThreshVal, remEdgesRadius;
	protected boolean rmEdges,flipThreshold;
	public String prefix;

	public final IBlockImage[] inImages = new IBlockImage[] { new BlockImage(
			"threshold", "", "Input thresheld image", true) };

	public final IBlockImage[] outImages = new IBlockImage[] {
			new BlockImage("thickness_map", "dto.tif",
					"Thickness map", true),
					new BlockImage("distance_map", "distmap.tif",
							"Distance map", false),
							new BlockImage("ridge_map", "ridge.tif",
									"Distance Ridge Map", false)};


	public ThicknessBlock() {
		super("ThicknessBlock");
		setup();
		prefix = "";
	}

	public ThicknessBlock(final String inPrefix) {
		super("ThicknessBlock");
		setup();
		prefix = inPrefix;
	}
	/**
	 * for subclasses of this block
	 * @param namePrefix
	 * @param inPrefix
	 */
	protected ThicknessBlock(final String namePrefix,final String inPrefix) {
		super(namePrefix+"ThicknessBlock");
		setup();
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
	static final protected String distName = "kVoronoi";
	static final protected String plugName = "HildThickness";
	protected ITIPLPluginIO distPlugin;
	protected ITIPLPluginIO thickPlugin;
	protected void setup() {
		distPlugin = TIPLPluginManager.createBestPluginIO(distName);
		distPlugin.setParameter("-includeEdges=false");
		thickPlugin = TIPLPluginManager.createBestPluginIO(plugName);
	}
	protected boolean useFloat=true;
	@Override
	public boolean executeBlock() {
		
		TImgRO threshImg = getInputFile("threshold");
		TImgRO[] threshImgs = new TImgRO[] {null,threshImg};

		distPlugin.LoadImages(threshImgs);
		distPlugin.execute();
		final TImg distAim = distPlugin.ExportImages(threshImg)[1];
		if (getFileParameter("distance_map").length() > 0) {
			finishImages(distAim, "distance_map");
		}
		
		thickPlugin.LoadImages(new TImg[] { distAim });
		thickPlugin.execute();
		TImg[] thickOut = thickPlugin.ExportImages(distAim);

		if (getFileParameter("thickness_map").length() > 0) {
			finishImages(thickOut[0], "thickness_map");
		}
		if (getFileParameter("ridge_map").length() > 0) {
			finishImages(thickOut[1], "ridge_map");
		}
		
		if (histoFile.length() > 0)
			GrayAnalysis.StartHistogram(thickOut[0], histoFile + ".tsv");
		if (profileFile.length() > 0) {
			GrayAnalysis.StartZProfile(thickOut[0], threshImg, profileFile
					+ "_z.tsv", 0.1f);
			GrayAnalysis.StartRProfile(thickOut[0], threshImg, profileFile
					+ "_r.tsv", 0.1f);
			GrayAnalysis.StartRCylProfile(thickOut[0], threshImg, profileFile
					+ "_rcyl.tsv", 0.1f);
		}

		return true;
	}


	protected void finishImages(TImgRO inImage, final String inNameArg) {
		SaveImage(inImage, inNameArg);
	}

	@Override
	protected String getDescription() {
		return "Block for running a thickness analysis";
	}

	@Override
	public String getPrefix() {
		return prefix;
	}
	@Override
	public void setPrefix(String newPrefix) {
		prefix=newPrefix;

	}
	protected String histoFile="thickmap_dto";
	protected String profileFile="thickmap_dto";
	@Override
	public ArgumentParser setParameterBlock(final ArgumentParser p) {
		distPlugin.setParameter(p,prefix);
		thickPlugin.setParameter(p,prefix);
		histoFile = p.getOptionString("csv", histoFile,"Histogram of thickness values");
		profileFile = p.getOptionString("profile",profileFile,"Profile of thickness values");
		
		return p;
	}

}