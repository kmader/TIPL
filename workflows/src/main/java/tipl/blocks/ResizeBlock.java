package tipl.blocks;

import tipl.formats.TImg;
import tipl.tools.Resize;
import tipl.util.ArgumentParser;
import tipl.util.ITIPLPluginIO;
import tipl.util.TImgTools;

/**
 * Block for Resizing an image, based on a number of different criteria
 * 
 * @author mader
 * 
 */
public class ResizeBlock extends BaseTIPLBlock {
	
	@BaseTIPLBlock.BlockIdentity(blockName = "ResizeBlock",
			inputNames= {"entire image"}, 
			outputNames= {"rescaled image"})
    final public static class resizeBlockFactory implements BaseTIPLBlock.TIPLBlockFactory {
		@Override
		public ITIPLBlock get() {
			return new ResizeBlock();
		}
	};
	
	public String prefix;
	protected final static String blockName = "Resize";
	public final IBlockImage[] inImages = new IBlockImage[] { new BlockImage(
			"input", "", "Input unfiltered image", true) };
	public final IBlockImage[] outImages = new IBlockImage[] { new BlockImage(
			"output", "cropped.tif", "Resized image", true) };
	ITIPLPluginIO fs = new Resize();

	public ResizeBlock() {
		super(blockName);
		prefix = "";
	}

	public ResizeBlock(final String inPrefix) {
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
		final TImg inputAim = TImgTools.ReadTImg(getFileParameter("input"),
				true, true);
		fs.LoadImages(new TImg[] { inputAim });
		fs.execute();
		final TImg outputAim = fs.ExportImages(inputAim)[0];
		TImgTools.WriteTImg(outputAim, getFileParameter("output"), true);
		TImgTools.RemoveTImgFromCache(getFileParameter("input"));
		return true;
	}

	@Override
	protected String getDescription() {
		return "Generic Block for resizing an image";
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
		final ArgumentParser t = fs.setParameter(p, prefix);
		return t;
	}
	
}