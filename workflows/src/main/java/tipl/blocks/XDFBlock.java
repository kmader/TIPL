package tipl.blocks;

import tipl.formats.TImgRO;
import tipl.tools.XDF;
import tipl.util.ArgumentParser;
import tipl.util.ITIPLPluginIO;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

/**
 * Run XDF analysis on a (or more than one) input image
 * 
 * @author mader
 * 
 */
public class XDFBlock extends BaseTIPLBlock {
	/**
	 * XDF Block is the block designed for using the XDF plugin and calculating a number of different analyses from the input. 
	 * The primary is a simple two point correlation function of a fixed structure, but others include A to be phase correlations and ...
	 * 
	 * 
	 * 
	 * @author mader
	 * 
	 */
	@BaseTIPLBlock.BlockIdentity(blockName = "XDFBlock",
			inputNames= {"object(s) image", "mask image","gray value image"}, 
			outputNames= {"correlation function"})
    final public static class xdfBlockFactory implements BaseTIPLBlock.TIPLBlockFactory {
		@Override
		public ITIPLBlock get() {
			return new XDFBlock();
		}
	};
	public String prefix;
	public int minVoxCount;
	public String phaseName;
	// public double sphKernelRadius;
	public boolean writeShapeTensor;
	public final IBlockImage[] inImages = new IBlockImage[] {
			new BlockImage("input", "input.tif", "Input image",
					true),
			new BlockImage("mask", "", "Mask Image", false),
			new BlockImage("value", "", "Value Image", false)};

	public final IBlockImage[] outImages = new IBlockImage[] { new BlockImage(
			"rdf", "rdf.tif", "Correlation function", true) };

	public final ITIPLPluginIO cXDF = new XDF();
	public XDFBlock() {
		super("XDF");
		prefix = "";
	}

	public XDFBlock(final String inPrefix) {
		super("XDF");
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
		final TImgRO inputAim = getInputFile("input");
		final TImgRO maskAim = getInputFile("mask");
		final TImgRO valueAim = getInputFile("value");
		TImgRO[] inImgs=new TImgRO[] {inputAim,maskAim,valueAim};
		
		cXDF.LoadImages(inImgs);
		cXDF.execute();
		
		SaveImage(cXDF.ExportImages(inputAim)[0],"rdf");

		XDF.WriteHistograms((XDF) cXDF, TImgTools.makeTImgExportable(inputAim),getFileParameter("rdf"));
		return true;
	}

	@Override
	protected String getDescription() {
		return "Run two point correlation analysis";
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
		TIPLGlobal.availableCores = p.getOptionInt("maxcores",
				TIPLGlobal.availableCores,
				"Number of cores/threads to use for processing");
		return cXDF.setParameter(p,prefix);
	}

}