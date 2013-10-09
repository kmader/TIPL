package tipl.blocks;

import tipl.formats.TImg;
import tipl.tools.VFilterScale;
import tipl.util.ArgumentParser;
import tipl.util.TIPLPluginIO;
import tipl.util.TImgTools;

/**
 * Generic Block for filtering and rescaling an image
 * 
 * @author mader
 * 
 */
public class FilterBlock extends BaseTIPLBlock {
	public final String prefix;
	protected final static String blockName = "Filter";
	public final IBlockImage[] inImages = new IBlockImage[] { new BlockImage(
			"ufilt", "", "Input unfiltered image", true) };
	public final IBlockImage[] outImages = new IBlockImage[] { new BlockImage(
			"gfilt", "gfilt.tif", "Post-filtering image", true) };
	TIPLPluginIO fs = new VFilterScale();

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

	@Override
	public boolean executeBlock() {
		final TImg ufiltAim = TImgTools.ReadTImg(getFileParameter("ufilt"),
				true, true);
		fs.LoadImages(new TImg[] { ufiltAim });
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
	public ArgumentParser setParameterBlock(ArgumentParser p) {
		return fs.setParameter(p, prefix);
	}

}