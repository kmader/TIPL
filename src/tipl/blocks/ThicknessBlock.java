package tipl.blocks;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
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
							new BlockImage("ridge_map", "",
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
		TImgRO[] threshImgs = new TImgRO[] {getInputFile("threshold")};

		distPlugin.LoadImages(threshImgs);
		distPlugin.execute();
		final TImg distAim = distPlugin.ExportImages(threshImgs[0])[1];
		if (getFileParameter("distance_map").length() > 0) {
			finishImages(distAim, getFileParameter("distance_map"));
		}
		thickPlugin.LoadImages(new TImg[] { distAim });
		thickPlugin.execute();

		TImg[] thickOut =   thickPlugin.ExportImages(distAim);

		if (getFileParameter("thickness_map").length() > 0) {
			finishImages(thickOut[0], getFileParameter("thickness_map"));
		}
		if (getFileParameter("ridge_map").length() > 0) {
			finishImages(thickOut[0], getFileParameter("ridge_map"));
		}


		return true;
	}


	protected void finishImages(TImgRO inImage, final String inName) {
		TImgTools.WriteTImg(inImage, inName, true);
	}

	@Override
	protected String getDescription() {
		return "Generic Block for running a thickness analysis";
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
		distPlugin.setParameter(p,prefix);
		thickPlugin.setParameter(p,prefix);
		return p;
	}

}