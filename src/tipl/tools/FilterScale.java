package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.ITIPLPlugin;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;

/**
 * A plugin to rescale and/or filter Aim files
 * 
 * @author Kevin Mader
 */
public class FilterScale extends BaseTIPLPluginMult {
	@TIPLPluginManager.PluginInfo(pluginType = "Filter",
			desc="Full memory filter",
			sliceBased=false,
			maximumSize=1024*1024*1024,
			bytesPerVoxel=-1,
			speedRank=11)
	final public static TIPLPluginManager.TIPLPluginFactory myFactory = new TIPLPluginManager.TIPLPluginFactory() {
		@Override
		public ITIPLPlugin get() {
			return new FilterScale();
		}
	};
	
	/** How a filter generating function looks */
	public interface filterGenerator {
		public BaseTIPLPluginIn.filterKernel make();
	}

	/**
	 * The command line executable version of the code The code that is run
	 * (without reading the arguments) is
	 * 
	 * <pre>
	 *      <li> System.out.println("Loading "+inputFile+" ...");
	 *      <p> Read the input file as an aim
	 *      <li> VirtualAim inputAim=TImgTools.ReadTImg(inputFile);
	 *      <p> Create a new instance of the FilterScale plugin using the aim file
	 *      <li> FilterScale myFilterScaler=new FilterScale(inputAim);
	 *      <li> System.out.println("Resizing"+inputFile+" ...");
	 *      <p> Use the >0 (default) criterion for removing the edges of the image
	 *      <li>myFilterScaler.find_edges();
	 *      <p> Run the plugin and generate the output image
	 *      <li>myFilterScaler.run();
	 *      <p> Save the output image into an aim file outputAim, use inputAim and its procedure log as a template
	 *      <li>VirtualAim outputAim=myFilterScaler.ExportAim(inputAim);
	 *      <p> Write the outputAim file to the hard disk as outputFile as an 8bit (0) image
	 *      <li>outputAim.WriteAim(outputFile,0);
	 * </pre>
	 */
	protected static void cmdLineFilter(final FilterScale tFS,
			final ArgumentParser p) {

		final int filterType = p
				.getOptionInt("filter", 0,
						"0 - Nearest Neighbor, 1 - Gaussian, 2 - Gradient, 3 - Laplace, 4 - Median");

		final D3int upfactor = p.getOptionD3int("upfactor", new D3int(1, 1, 1),
				"Upscale factor");
		final D3int downfactor = p.getOptionD3int("downfactor", new D3int(2, 2,
				2), "Downscale factor");
		if (tFS == null)
			return;

		tFS.SetScale(upfactor.x, upfactor.y, upfactor.z, downfactor.x,
				downfactor.y, downfactor.z);
		switch (filterType) {
		case 1:
			tFS.setGaussFilter();
			break;
		case 2:
			tFS.setGradientFilter();
			break;
		case 3:
			tFS.setLaplaceFilter();
			break;
		case 4:
			tFS.setMedianFilter();
			break;
		}
		tFS.runFilter();

	}

	public static void main(final String[] args) {
		final String kVer = "120514_002";
		System.out.println("FilterScale v" + kVer);
		System.out.println(" FilterScales Aim files based on given criteria");
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final ArgumentParser p = new ArgumentParser(args);
		final String inputFile = p.getOptionString("input", "",
				"Input masked image");
		final String outputFile = p.getOptionString("output",
				"FilterScaled.tif", "Output FilterScaled image");
		cmdLineFilter(null, p);
		if (p.hasOption("?")) {
			System.out.println(" FilterScale Demo Help");
			System.out
					.println(" Analyzes Labeled Gray values inside of Int Labeled Regions");
			System.out.println(" Arguments::");
			System.out.println(" ");
			System.out.println(p.getHelp());
			System.exit(0);
		}

		if (inputFile.length() > 0) { // Read in labels, if find edge is
										// selected or a mask is given
			System.out.println("Loading " + inputFile + " ...");
			final TImg inputAim = TImgTools.ReadTImg(inputFile);
			final FilterScale myFilterScaler = new FilterScale(inputAim);
			System.out.println("Resizing" + inputFile + " ...");
			cmdLineFilter(myFilterScaler, p);
			final TImg outputAim = myFilterScaler.ExportAim(inputAim);
			TImgTools.WriteTImg(outputAim,outputFile);

		}

	}

	/** Output aim length */
	int itAimLength;
	/** Upscale X */
	public int upX = 1;
	/** Upscale Y */
	public int upY = 1;
	/** Upscale Z */
	public int upZ = 1;
	/** Downscale X */
	public int dnX = 2;

	/** Downscale Y */
	public int dnY = 2;
	/** Downscale Z */
	public int dnZ = 2;
	/**
	 * Filter generating function (needed for threading to make multiple copies
	 * of the filter for each region)
	 */
	public filterGenerator scalingFilterGenerator = null;
	protected D3int ipos = new D3int(0);
	/** Selected Region of Interest */
	protected int itLowx;
	protected int itLowy;
	protected int itLowz;
	protected int itUppx;
	protected int itUppy;
	protected int itUppz;
	/** Output bounds */
	protected int olowx;
	protected int olowy;

	protected int olowz;
	protected int ouppx;
	protected int ouppy;
	protected int ouppz;

	/**
	 * Set imagetype of output image (default = -1 is the same as the input type
	 */
	public int oimageType = -1;
	/** First input aim */
	public boolean[] outAimMask;
	/** First input aim */
	public char[] outAimByte;
	/** First input aim */
	public short[] outAimShort;
	/** First input aim */
	public int[] outAimInt;

	/** First input aim */
	public float[] outAimFloat;
	/** input element size */
	public D3float elSize = new D3float(1.0f, 1.0f, 1.0f);
	/** output position */
	public D3int opos;
	/** output dimensions */
	public D3int odim;

	/** output element size */
	public D3float oelsize;

	protected int off = 0;

	public boolean supportsThreading = true;

	protected FilterScale() {
		// Just for subclasses
	}

	@Deprecated
	public FilterScale(final boolean[] inputmap, final D3int idim,
			final D3int ioffset, final D3float ielSize) {
		elSize = ielSize;
		ImportAim(inputmap, idim, ioffset);
	}

	@Deprecated
	public FilterScale(final float[] inputmap, final D3int idim,
			final D3int ioffset, final D3float ielSize) {
		elSize = ielSize;
		ImportAim(inputmap, idim, ioffset);
	}

	@Deprecated
	public FilterScale(final int[] inputmap, final D3int idim,
			final D3int ioffset, final D3float ielSize) {
		elSize = ielSize;
		ImportAim(inputmap, idim, ioffset);
	}

	@Deprecated
	public FilterScale(final short[] inputmap, final D3int idim,
			final D3int ioffset, final D3float ielSize) {
		elSize = ielSize;
		ImportAim(inputmap, idim, ioffset);
	}

	public FilterScale(final TImgRO inAim) {
		LoadImages(new TImgRO[] { inAim });
	}

	protected void CalcOutDim() {

		final int nx = (int) Math.round((float) (upX + 0.0) / (dnX + 0.0)
				* dim.x);
		final int ny = (int) Math.round((float) (upY + 0.0) / (dnY + 0.0)
				* dim.y);
		final int nz = (int) Math.round((float) (upZ + 0.0) / (dnZ + 0.0)
				* dim.z);

		odim = new D3int(nx, ny, nz);

		// Calculate bounds for output image olow/upp
		olowx = 0;
		olowy = 0;
		olowz = 0;

		ouppx = odim.x;
		ouppy = odim.y;
		ouppz = odim.z;

		oelsize = new D3float((dnX + 0.0) / (upX + 0.0) * elSize.x, (dnY + 0.0)
				/ (upY + 0.0) * elSize.y, (dnZ + 0.0) / (upZ + 0.0) * elSize.z);

		opos = new D3int((int) Math.round((upX + 0.0) / (dnX + 0.0) * ipos.x),
				(int) Math.round((upY + 0.0) / (dnY + 0.0) * ipos.y),
				(int) Math.round((upZ + 0.0) / (dnZ + 0.0) * ipos.z)); // rescale
																		// outpos
		//
		if (oimageType == -1)
			oimageType = imageType;
		// Initialize aimArray
		switch (oimageType) {
		case TImgTools.IMAGETYPE_CHAR: // Byte
			outAimByte =(char[]) TImgTools.watchBigAlloc(oimageType, (int) odim.prod());
			break;
		case TImgTools.IMAGETYPE_SHORT: // Short
			outAimShort = (short[]) TImgTools.watchBigAlloc(oimageType, (int) odim.prod());
			break;
		case TImgTools.IMAGETYPE_INT: // Int
			outAimInt = (int[]) TImgTools.watchBigAlloc(oimageType, (int) odim.prod());
			break;
		case TImgTools.IMAGETYPE_FLOAT: // Float
			outAimFloat=(float[]) TImgTools.watchBigAlloc(oimageType, (int) odim.prod());
			break;
		case TImgTools.IMAGETYPE_BOOL: // Boolean
			outAimMask = (boolean[]) TImgTools.watchBigAlloc(oimageType, (int) odim.prod());
			break;
		}

		System.out.println("New output: dim-" + odim + ", ElSize:" + oelsize
				+ ", Pos:" + opos + "," + StrPctRatio(odim.prod(), dim.prod())
				+ " VolRed, ImgT:" + oimageType);
		procLog += "New output: dim-" + odim + ", pos:" + opos + ", "
				+ StrPctRatio(odim.prod(), dim.prod()) + " VolRed, ImgT:"
				+ imageType + "\n";

	}

	/**
	 * Object to divide the thread work into supportCores equal parts, default
	 * is z-slices
	 */
	@Override
	public Object divideThreadWork(final int cThread, final int maxCores) {
		final int minSlice = olowz;
		final int maxSlice = ouppz;
		final int range = (maxSlice - minSlice) / maxCores;

		int startSlice = minSlice;
		int endSlice = startSlice + range;

		for (int i = 0; i < cThread; i++) {
			startSlice = endSlice;
			endSlice = startSlice + range;
		}

		if (cThread == (maxCores - 1))
			endSlice = maxSlice;
		if (cThread >= maxCores)
			return null;
		return (new int[] { startSlice, endSlice });
	}

	@Override
	public boolean execute() {
		return runMulticore();
	}

	/**
	 * Exports the FilterScaled result based on a template aim
	 * 
	 * @param templateAim
	 *            input template aim file
	 */
	@Override
	public TImg ExportAim(final TImgRO.CanExport templateAim) {
		if (isInitialized) {
			if (runCount > 0) {
				TImg outVirtualAim;
				switch (oimageType) {
				case 10: // Boolean
					outVirtualAim = templateAim.inheritedAim(outAimMask, odim,
							new D3int(0));
					break;
				case 0: // Byte
					outVirtualAim = templateAim.inheritedAim(outAimByte, odim,
							new D3int(0));
					break;
				case 1: // Short
					outVirtualAim = templateAim.inheritedAim(outAimShort, odim,
							new D3int(0));
					break;
				case 2: // Int
					outVirtualAim = templateAim.inheritedAim(outAimInt, odim,
							new D3int(0));
					break;
				case 3: // Float
					outVirtualAim = templateAim.inheritedAim(outAimFloat, odim,
							new D3int(0));
					break;
				default:
					System.err
							.println("Input type not supported " + oimageType);
					return null;
				}
				outVirtualAim.setPos(opos);
				outVirtualAim.setElSize(oelsize);
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

	/** Current Filter */
	protected BaseTIPLPluginIn.filterKernel getFilter() {
		return scalingFilterGenerator.make();

	}

	@Override
	public String getPluginName() {
		return "FilterScale";
	}

	@Override
	protected void InitByte() {
		InitMask();
	}

	@Override
	protected void InitFloat() {
		InitMask();
	}

	@Override
	protected void InitInt() {
		InitMask();
	}

	@Override
	protected void InitMask() {
	}

	@Override
	protected void InitShort() {
		InitMask();
	}

	@Override
	public void LoadImages(final TImgRO[] inImages) {
		// TODO Auto-generated method stub
		if (inImages.length < 1)
			throw new IllegalArgumentException(
					"Too few arguments for LoadImages in:" + getPluginName());
		final TImgRO inImg = inImages[0];
		elSize = inImg.getElSize();
		ipos = inImg.getPos();
		super.LoadImages(inImages);
	}

	@Override
	public void processWork(final Object currentWork) {
		final int[] range = (int[]) currentWork;
		final int bSlice = range[0];
		final int tSlice = range[1];
		runFilter(bSlice, tSlice);
	}
	
	@Override
	protected void runByte() {
	}

	/** Code to actually run the filter code on the image */
	public boolean runFilter() {
		System.out.println("Rescale Operation Running : Upscale:(" + upX + ","
				+ upY + "," + upZ + "), Downscale (" + dnX + "," + dnY + ","
				+ dnZ + ")");
		CalcOutDim();
		runMulticore();
		runCount++;
		String filterNameOut = "NearestNeighbor";
		if (scalingFilterGenerator != null)
			filterNameOut = scalingFilterGenerator.make().filterName();

		final String logAdd = "FilterScale Operation (" + filterNameOut
				+ ") : Upscale:(" + upX + ", " + upY + ", " + upZ
				+ "), Downscale (" + dnX + ", " + dnY + ", " + dnZ + ")";
		procLog += logAdd;
		return true;
	}

	/** Code to actually run the filter code on a portion of the image */
	protected boolean runFilter(final int bSlice, final int tSlice) {
		int ooff, inoff;
		// Loop through new image
		if (supportsThreading)
			System.out.println("Filter Running -- :<" + bSlice + "," + tSlice
					+ "> @ " + Thread.currentThread());
		final long sTime = System.currentTimeMillis();
		runCount++;
		double inSum = 0.0;
		double inCnt = 0.0;
		double outSum = 0.0;
		double outCnt = 0.0;

		for (int oz = bSlice; oz < tSlice; oz++) {

			for (int oy = olowy; oy < ouppy; oy++) {

				ooff = (oz * odim.y + oy) * odim.x + olowx;
				for (int ox = olowx; ox < ouppx; ox++, ooff++) {
					// Interpolate position in input image
					float iposz = (dnZ + 0.0f) / (upZ + 0.0f) * oz;
					float iposy = (dnY + 0.0f) / (upY + 0.0f) * oy;
					float iposx = (dnX + 0.0f) / (upX + 0.0f) * ox;

					// Range to scan in input image
					int tilowx, tilowy, tilowz, tiuppx, tiuppy, tiuppz;

					tilowx = (int) Math.floor(iposx - dnX);
					tilowy = (int) Math.floor(iposy - dnY);
					tilowz = (int) Math.floor(iposz - dnZ);

					tiuppx = (int) Math.ceil(iposx + dnX);
					tiuppy = (int) Math.ceil(iposy + dnY);
					tiuppz = (int) Math.ceil(iposz + dnZ);

					if (scalingFilterGenerator == null) {
						iposx = min(uppx, max((int) iposx, lowx));
						iposy = min(uppy, max((int) iposy, lowy));
						iposz = min(uppz, max((int) iposz, lowz));

						inoff = ((int) iposz * dim.y + (int) iposy) * dim.x
								+ (int) iposx;
						/*
						 * if (oz>lastSlice) {
						 * System.out.println("Slice:"+oz+", cop-in:"
						 * +String.format
						 * ("%.2f",(inoff+0.0)/1e6)+" MVx, out:"+String
						 * .format("%.2f"
						 * ,(ooff+0.0)/1e6)+" MVx, "+String.format(
						 * "%.2f",(ooff*100.0)/inoff)+" %"); lastSlice=oz; }
						 */

						double dcVox = 0;
						switch (imageType) {
						case 0: // Byte
							dcVox = inAimByte[inoff];
							break;
						case 1:
							dcVox = inAimShort[inoff];
							break;
						case 2:
							dcVox = inAimInt[inoff];
							break;
						case 3:
							dcVox = inAimFloat[inoff];
							break;
						case 10:
							if (inAimMask[inoff])
								dcVox++;
							break;
						}
						switch (oimageType) {
						case 0: // Byte
							outAimByte[ooff] = (char) dcVox;

							break;
						case 1: // Short
							outAimShort[ooff] = (short) dcVox;
							break;
						case 2: // Int
							outAimInt[ooff] = (int) dcVox;

							break;
						case 3: // Float
							outAimFloat[ooff] = (float) dcVox;

							break;
						case 10: // Boolean
							outAimMask[ooff] = dcVox >= 0.5;
							break;
						}
						inSum += dcVox;
						inCnt++;

					} else {
						/** Current Filter */
						final BaseTIPLPluginIn.filterKernel curFilter = getFilter();
						curFilter.reset();
						// Scan Through original image

						for (int iz = max(lowz, tilowz); iz < min(tiuppz, uppz); iz++) {
							for (int iy = max(lowy, tilowy); iy < min(tiuppy,
									uppy); iy++) {
								inoff = (iz * dim.y + iy) * dim.x
										+ max(lowx, tilowx);
								/*
								 * if (oz>lastSlice) {
								 * System.out.println("Slice:("
								 * +max(lowz,tilowz)+
								 * "-"+min(tiuppz,uppz)+")"+oz+
								 * ", cop-in_sr:"+String
								 * .format("%.2f",((max(lowz,tilowz)*dim.y +
								 * iy)*dim.x +
								 * max(lowx,tilowx)+0.0)/1e6)+" MVx, _sp:"
								 * +String
								 * .format("%.2f",((min(tiuppz,uppz)*dim.y +
								 * iy)*dim.x +
								 * max(lowx,tilowx)+0.0)/1e6)+" MVx, out:"
								 * +String
								 * .format("%.2f",(ooff+0.0)/1e6)+" MVx, "
								 * +String
								 * .format("%.2f",(ooff*100.0)/inoff)+" %");
								 * lastSlice=oz; }
								 */
								for (int ix = max(lowx, tilowx); ix < min(
										tiuppx, uppx); ix++, inoff++) {
									double dcVox = 0;
									switch (imageType) {
									case 0: // Byte
										dcVox = inAimByte[inoff];
										break;
									case 1:
										dcVox = inAimShort[inoff];
										break;
									case 2:
										dcVox = inAimInt[inoff];
										break;
									case 3:
										dcVox = inAimFloat[inoff];
										break;
									case 10:
										if (inAimMask[inoff])
											dcVox++;
										break;
									}
									inSum += dcVox;
									inCnt++;
									// The filter and coordinates apply in the
									// input image dimensional space not the
									// output.
									curFilter.addpt(ix, iposx, iy, iposy, iz,
											iposz, dcVox);

								}
							}
						}
						final double outputVal = curFilter.value();
						outSum += outputVal;
						outCnt++;
						switch (oimageType) {

						case 0: // Byte
							outAimByte[ooff] = (char) outputVal;

							break;
						case 1: // Short
							outAimShort[ooff] = (short) outputVal;
							break;
						case 2: // Int
							outAimInt[ooff] = (int) outputVal;

							break;
						case 3: // Float
							outAimFloat[ooff] = (float) outputVal;

							break;
						case 10: // Boolean
							outAimMask[ooff] = outputVal >= 0.5;
							break;
						}

					}

				}
			}
		}

		final float eTime = (System.currentTimeMillis() - sTime) / (1000F);
		String filterNameOut = "NearestNeighbor";
		if (scalingFilterGenerator != null)
			filterNameOut = scalingFilterGenerator.make().filterName();

		String logAdd = "FilterScale Operation (" + filterNameOut
				+ ") : Upscale:(" + upX + ", " + upY + ", " + upZ
				+ "), Downscale (" + dnX + ", " + dnY + ", " + dnZ + "), T:"
				+ eTime;
		logAdd += "\n InMean:" + String.format("%.2f", inSum / inCnt)
				+ ", OutMean:" + String.format("%.2f", outSum / outCnt) + ", "
				+ String.format("%.2f", outCnt / 1e6) + " Mvx, "
				+ String.format("%.2f", inCnt / 1e9) + " Gop, "
				+ String.format("%.2f", inCnt / outCnt) + " Op/Vx";
		System.out.println(logAdd);
		return true;

	}

	@Override
	protected void runFloat() {
	}

	@Override
	protected void runInt() {
	}

	@Override
	protected void runMask() {
	}

	@Override
	protected void runShort() {
	}

	/** Use a gaussian filter */
	@Deprecated
	public void setGaussFilter() {
		scalingFilterGenerator = new filterGenerator() {
			@Override
			public BaseTIPLPluginIn.filterKernel make() {
				return BaseTIPLPluginIn.gaussFilter(upX, upY, upZ);
			}
		};

	}

	/** Use a gaussian filter */
	@Deprecated
	public void setGaussFilter(final double sigma) {

		scalingFilterGenerator = new filterGenerator() {
			@Override
			public BaseTIPLPluginIn.filterKernel make() {
				return BaseTIPLPluginIn.gaussFilter(sigma, sigma, sigma);
			}
		};

	}

	/** Use a gradient filter */
	@Deprecated
	public void setGradientFilter() {
		scalingFilterGenerator = new filterGenerator() {
			@Override
			public BaseTIPLPluginIn.filterKernel make() {
				return BaseTIPLPluginIn.gradientFilter();
			}
		};

	}

	/** Use a laplace filter */
	@Deprecated
	public void setLaplaceFilter() {
		scalingFilterGenerator = new filterGenerator() {
			@Override
			public BaseTIPLPluginIn.filterKernel make() {
				return BaseTIPLPluginIn.laplaceFilter();
			}
		};

	}

	@Deprecated
	public void setMedianFilter() {

		scalingFilterGenerator = new filterGenerator() {
			@Override
			public BaseTIPLPluginIn.filterKernel make() {
				return BaseTIPLPluginIn.medianFilter(upX, upY, upZ);
			}
		};

	}

	/** Use a Median filter */
	@Deprecated
	public void setMedianFilter(final int size) {

		scalingFilterGenerator = new filterGenerator() {
			@Override
			public BaseTIPLPluginIn.filterKernel make() {
				return BaseTIPLPluginIn.medianFilter(size, size, size);
			}
		};

	}

	@Override
	public ArgumentParser setParameter(final ArgumentParser p,
			final String prefix) {
		// p=super.setParameter(p,prefix);
		final int filterType = p
				.getOptionInt(prefix + "filter", 0,
						"0 - Nearest Neighbor, 1 - Gaussian, 2 - Gradient, 3 - Laplace, 4 - Median");
		final double filterParameter = p
				.getOptionDouble(
						prefix + "filtersetting",
						-1.0,
						"For gaussian it is the Sigma Value, for Median it is the window size (values less than 0 are ignored)");
		oimageType = p
				.getOptionInt(prefix+"output", oimageType,
						"-1 is same as input, "+TImgTools.IMAGETYPE_HELP);
		final D3int upfactor = p.getOptionD3int(prefix + "upfactor", new D3int(
				1, 1, 1), "Upscale factor");
		final D3int downfactor = p.getOptionD3int(prefix + "downfactor",
				new D3int(2, 2, 2), "Downscale factor");

		SetScale(upfactor.x, upfactor.y, upfactor.z, downfactor.x,
				downfactor.y, downfactor.z);
		switch (filterType) {
		case 1:
			if (filterParameter > 0)
				setGaussFilter(filterParameter);
			else
				setGaussFilter();
			break;
		case 2:
			setGradientFilter();
			break;
		case 3:
			setLaplaceFilter();
			break;
		case 4:
			if (filterParameter > 0)
				setMedianFilter((int) filterParameter);
			else
				setMedianFilter();
			break;
		}

		return p;

	}

	@Deprecated
	public void SetScale(final int ux, final int uy, final int uz,
			final int dx, final int dy, final int dz) {
		upX = ux;
		upY = uy;
		upZ = uz;
		dnX = dx;
		dnY = dy;
		dnZ = dz;
	}
}
