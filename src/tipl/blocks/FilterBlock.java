package tipl.blocks;

import tipl.formats.DirectoryReader;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.DirectoryReader.DRFactory;
import tipl.tools.VFilterScale;
import tipl.util.ArgumentParser;
import tipl.util.D3float;
import tipl.util.ITIPLPluginIO;
import tipl.util.TImgTools;

/**
 * Generic Block for filtering and rescaling an image
 * 
 * @author mader
 * 
 */
public class FilterBlock extends BaseTIPLBlock {
	
	@BaseTIPLBlock.BlockIdentity(blockName = "FilterBlock",
			inputNames= {"unfiltered image"}, 
			outputNames= {"filtered image"})
	final public static TIPLBlockFactory myFactory = new BaseTIPLBlock.TIPLBlockFactory() {
		@Override
		public ITIPLBlock get() {
			return new FilterBlock();
		}
	};
	
	public String prefix;
	protected final static String blockName = "Filter";
	public final IBlockImage[] inImages = new IBlockImage[] { new BlockImage(
			"ufilt", "", "Input unfiltered image", true) };
	public final IBlockImage[] outImages = new IBlockImage[] { new BlockImage(
			"gfilt", "gfilt.tif", "Post-filtering image", true) };
	ITIPLPluginIO fs = new VFilterScale();

	public FilterBlock() {
		super(blockName);
		prefix = "";
	}

	public FilterBlock(final String inPrefix) {
		super(blockName);
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
	protected boolean changeElSize=false;
	protected D3float forcedElSize=new D3float(1.0f,1.0f,1.0f);
	/**
	 * wrapper to get the ufilt image and change the voxel size if necessary
	 * @return the image (with changed voxel size)
	 */
	protected TImgRO getUfiltImage() {
		final TImgRO ufiltAim = getInputFile("ufilt");
		if (!changeElSize) return ufiltAim;
		TImg ufiltAimEditable = TImgTools.WrapTImgRO(ufiltAim);
		ufiltAimEditable.setElSize(forcedElSize);
		return ufiltAimEditable;
	}
	@Override
	public boolean executeBlock() {
		final TImgRO ufiltAim=getUfiltImage();
		fs.LoadImages(new TImgRO[] { ufiltAim });
		fs.execute();
		final TImg gfiltAim = fs.ExportImages(ufiltAim)[0];
		TImgTools.WriteTImg(gfiltAim, getFileParameter("gfilt"), true);
		TImgTools.RemoveTImgFromCache(getFileParameter("ufilt"));
		return true;
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
	public void setPrefix(String newPrefix) {
		prefix=newPrefix;
		
	}
	@Override
	public ArgumentParser setParameterBlock(final ArgumentParser p) {
		changeElSize = p.getOptionBoolean(prefix+"changeelsize", changeElSize,
				"Change the voxel size in the ufilt image");
		forcedElSize = p.getOptionD3float(prefix+"elsize", forcedElSize,
				"New voxel size for ufilt image");
		return fs.setParameter(p, prefix);
	}

}