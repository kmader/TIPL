package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.*;

/**
 * This class peels a mask away from an aim by taking the outermost layers
 * (defined in 3D by D3int peelS and proximity to null/false values)
 */
public class Peel extends BaseTIPLPluginMult {
	@TIPLPluginManager.PluginInfo(pluginType = "Peel",
			desc="Full memory peel command",
			sliceBased=false)
    final public static class peelFactory implements TIPLPluginManager.TIPLPluginFactory {
		@Override
		public ITIPLPlugin get() {
			return new Peel();
		}
	};
	/**
	 * The command line executable version of the code The code run in the main
	 * function looks like this
*/
	@Deprecated
	public static void main(final String[] args) {
		final String kVer = "120105_001";
		System.out.println("Peel v" + kVer);
		System.out.println(" Counts Peel for given images v" + kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final ArgumentParser p = TIPLGlobal.activeParser(args);
		final TypedPath inputFile = p.getOptionPath("input", "", "Input image");
		final TypedPath maskFile = p.getOptionPath("mask", "",
				"Input mask image");
		final TypedPath outputFile = p.getOptionPath("output", "",
				"Peeled image");
		final int peelS = p.getOptionInt("peelS", 1, "Peel Layers");
		final boolean isMask = p.getOptionBoolean("ismask",
				"Is input image a mask");

		if (p.hasOption("?")) {
			System.out.println(" IPL Demo Help");
			System.out
					.println(" Analyzes Labeled Gray values inside of Int Labeled Regions");
			System.out.println(" Arguments::");
			System.out.println(" ");
			System.out.println(p.getHelp());
			System.exit(0);
		}

		if (inputFile.length() > 0) { // Read in labels
			System.out.println("Loading " + inputFile + " ...");
			final TImg inputAim = TImgTools.ReadTImg(inputFile);
			// inputAim.getBoolAim();
			Peel cPeel;
			if (maskFile.length() > 0)
				cPeel = new Peel(inputAim, TImgTools.ReadTImg(maskFile),
						new D3int(peelS), isMask);
			else
				cPeel = new Peel(inputAim, new D3int(peelS));
			System.out.println("Calculating Peel " + inputFile + " ...");
			cPeel.execute();
			final TImg outputAim = cPeel.ExportImages(inputAim)[0];
			if (outputFile.length() > 0)
				TImgTools.WriteTImg(outputAim,outputFile);

		}

	}

	/** First input aim */
	protected boolean[] outAimMask;
	/** First input aim */
	protected char[] outAimByte;
	/** First input aim */
	protected short[] outAimShort;
	/** First input aim */
	protected int[] outAimInt;
	/** First input aim */
	protected float[] outAimFloat;

	boolean selfMask = false;
	/** The mask to be peeled */
	boolean[] peelMask = null;
	boolean isMask = false;

	protected long gKeptVoxels = 0;
	protected long gBorderVoxels = 0;

	protected Peel() {
		// Constructor for the plugin manager
	}
	@Deprecated
	public Peel(final TImgRO inAim, final D3int peelS) {
		ImportAim(inAim, -1);
		selfMask = true;
		InitPeel(peelS);
	}
	@Deprecated
	public Peel(final TImgRO inAim, final TImgRO maskAim, final D3int peelS) {
		LoadImages(new TImgRO[] { inAim, maskAim });
		InitPeel(peelS);
	}
	@Deprecated
	public Peel(final TImgRO inAim, final TImgRO maskAim, final D3int peelS,
			final boolean IisMask) {
		isMask = IisMask;
		LoadImages(new TImgRO[] { inAim, maskAim });
		InitPeel(peelS);
	}

	@Override
	public boolean execute() {
		if (runMulticore()) {
			runCount += 1;
			procLog += "TIPL:Peel(Layers):(" + neighborSize + ")\n";
			final String testLog = "Preserved Voxels:" + StrMvx(gKeptVoxels)
					+ ", Border Voxels:"
					+ StrPctRatio(gBorderVoxels, gKeptVoxels)
					+ ", Final Porosity:" + StrPctRatio(gKeptVoxels, aimLength);
			procLog += testLog + "\n";
			System.out.println(testLog);
			return true;
		} else
			return false;
	}

	public TImg[] ExportImages(final TImgRO templateImage) {
		final TImg cImg = TImgTools.WrapTImgRO(templateImage);
		return new TImg[] {CreateOutputImage(cImg)};
	}
	protected TImg CreateOutputImage(final TImg.CanExport templateAim) {
		if (isInitialized) {
			if (runCount > 0) {
				TImg outVirtualAim;
				switch (imageType) {
				case 10: // Boolean
					outVirtualAim = templateAim.inheritedAim(outAimMask, dim,
							new D3int(0));
					break;
				case 0: // Byte
					outVirtualAim = templateAim.inheritedAim(outAimByte, dim,
							new D3int(0));
					break;
				case 1: // Short
					outVirtualAim = templateAim.inheritedAim(outAimShort, dim,
							new D3int(0));
					break;
				case 2: // Int
					outVirtualAim = templateAim.inheritedAim(outAimInt, dim,
							new D3int(0));
					break;
				case 3: // Float
					outVirtualAim = templateAim.inheritedAim(outAimFloat, dim,
							new D3int(0));
					break;
				default:
					System.err.println("Input type not supported" + imageType);
					return null;
				}
				outVirtualAim.appendProcLog(procLog);
				return outVirtualAim;

			} else {
				System.err
						.println("The plug-in : "
								+ getPluginName()
								+ ", has not yet been run, exported does not exactly make sense, original data will be sent.");
				return null;
			}
		} else {
			System.err
					.println("The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exported does not make any sense");
			return null;

		}
	}

	@Override
	public String getPluginName() {
		return "Peel";
	}

	@Override
	protected void InitByte() {
		return;
	}

	@Override
	protected void InitFloat() {
		return;
	}

	@Override
	protected void InitInt() {
		return;
	}

	@Override
	protected void InitMask() {
		return;
	}

	private void InitPeel(final D3int peelS) {
		runCount = 0;
		neighborSize = peelS;

		if (selfMask)
			InitSelfMask();
		// Initializes Output
		switch (imageType) {
		case 10: // Boolean
			outAimMask = new boolean[aimLength];
			break;
		case 0: // Byte
			outAimByte = new char[aimLength];
			break;
		case 1: // Short
			outAimShort = new short[aimLength];
			break;
		case 2: // Int
			outAimInt = new int[aimLength];
			break;
		case 3: // Float
			outAimFloat = new float[aimLength];
			break;
		default:
			System.err.println("Input type not supported");
			return;
		}
	}

	protected void InitSelfMask() {
		peelMask = new boolean[aimLength];
		switch (imageType) {
		case 10: // Boolean
			System.arraycopy(inAimMask, 0, peelMask, 0, aimLength);
			break;
		case 0: // Byte
			for (int off = 0; off < aimLength; off++)
				peelMask[off] = inAimByte[off] > 0;
			break;
		case 1: // Short
			for (int off = 0; off < aimLength; off++)
				peelMask[off] = inAimShort[off] > 0;
			break;
		case 2: // Boolean
			for (int off = 0; off < aimLength; off++)
				peelMask[off] = inAimInt[off] > 0;
			break;
		case 3: // Float
			for (int off = 0; off < aimLength; off++)
				peelMask[off] = inAimFloat[off] > 0;
			break;
		default:
			System.err.println("Input type not supported");
			return;
		}
	}

	@Override
	protected void InitShort() {
		return;
	}

	/**
	 * the first image is the image to peel and the second is the mask to use
	 * 
	 */
	@Override
	public void LoadImages(final TImgRO[] inImages) {
		final TImgRO inAim = inImages[0];
		final TImgRO maskAim = inImages[1];
		TImgRO tempMask;
		if (!TImgTools.CheckSizes2(inAim, maskAim)) {
			System.out.println(" ==== Aim Sizes Do Not Match, Recutting...");
			// maskAim.getBoolAim();
			final Resize rsAim = new Resize(maskAim);
			rsAim.cutROI(inAim);
			rsAim.execute();
			final TImg[] tempAim = rsAim.ExportImages(maskAim);
			tempMask = tempAim[0];
			procLog += " ==== Aim Sizes Do Not Match, Recutting...\n";
			procLog += rsAim.procLog + "\n";
		} else {
			tempMask = maskAim;
		}
		peelMask = TImgTools.makeTImgFullReadable(tempMask).getBoolAim();
		if (!isMask)
			ImportAim(inAim, -1);
		else
			ImportAim(inAim, 10);
	}

	@Override
	public void processWork(final Object currentWork) {
		final int[] range = (int[]) currentWork;
		final int bSlice = range[0];
		final int tSlice = range[1];
		runSection(bSlice, tSlice);
	}

	@Override
	protected void runByte() {
		return;
	}

	@Override
	protected void runFloat() {
		return;
	}

	@Override
	protected void runInt() {
		return;
	}

	@Override
	protected void runMask() {
		return;
	}

	/** run the peeling code on a section of slices **/
	protected void runSection(final int startSlice, final int endSlice) {
		double keptVoxels = 0;
		double startingVoxels = 0.0;
		double borderVoxels = 0.0;
		BaseTIPLPluginIn.stationaryKernel curKernel;
		if (neighborKernel == null)
			curKernel = new BaseTIPLPluginIn.stationaryKernel();
		else
			curKernel = new BaseTIPLPluginIn.stationaryKernel(neighborKernel);

		int off = 0;
		for (int z = startSlice; z < endSlice; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
					boolean isBorder = false;
					float cVoxVal = 0.0f;
					if (peelMask[off]) {
						startingVoxels++;
						int off2;
						for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(
								z + neighborSize.z, uppz - 1); z2++) {
							for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(
									y + neighborSize.y, uppy - 1); y2++) {
								off2 = (z2 * dim.y + y2) * dim.x
										+ max(x - neighborSize.x, lowx);
								for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(
										x + neighborSize.x, uppx - 1); x2++, off2++) {
									// If there is an off voxel which neighbors
									// the current voxel
									if (!peelMask[off2]) {
										if (curKernel.inside(off, off2, x, x2,
												y, y2, z, z2)) {
											isBorder = true;
											break;
										}
									}
								}
							}
						}
						if (!isBorder) { // if the voxel is not on the border
											// than keep Voxel

							switch (imageType) {
							case 10: // Boolean
								if (inAimMask[off])
									cVoxVal = 1.0f;
								break;
							case 0: // Byte
								cVoxVal = (inAimByte[off]);
								break;
							case 1: // Short
								cVoxVal = inAimShort[off];
								break;
							case 2: // Int
								cVoxVal = inAimInt[off];
								break;
							case 3: // Float
								cVoxVal = inAimFloat[off];
								break;
							default:
								System.err.println("Input type not supported, "
										+ imageType);
								return;
							}
							if (cVoxVal > 0)
								keptVoxels++;
							borderVoxels++;
						} else {
						}
					}
					switch (imageType) {
					case 10: // Boolean
						outAimMask[off] = cVoxVal > 0.0f;
						break;
					case 0: // Byte
						outAimByte[off] = (char) cVoxVal;
						break;
					case 1: // Short
						outAimShort[off] = (short) cVoxVal;
						break;
					case 2: // Int
						outAimInt[off] = (int) cVoxVal;
						break;
					case 3: // Float
						outAimFloat[off] = cVoxVal;
						break;
					default:
						System.err.println("Output type not supported, "
								+ imageType);
						return;
					}

				}
			}
		}
		final String testLog = "Preserved Voxels:"
				+ String.format("%.2f", keptVoxels / 1e6)
				+ " Mvx, Border Voxels:"
				+ String.format("%.2f", borderVoxels * 100.0 / startingVoxels)
				+ "%, Final Porosity:"
				+ String.format("%.2f", keptVoxels * 100 / aimLength) + "%";
		System.out.println(testLog);
		// Increment global variables
		synchronized (this) {
			gKeptVoxels += keptVoxels;
			gBorderVoxels += borderVoxels;
		}
	}

	@Override
	protected void runShort() {
		return;
	}
}
