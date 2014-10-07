package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.settings.FilterSettings;
import tipl.util.*;

/**
 * A plugin to rescale and/or filter Aim files
 * 
 * @author Kevin Mader
 */
public class FilterScale extends BaseTIPLPluginMult implements FilterSettings.HasFilterSettings {
	
	protected FilterSettings curSettings = new FilterSettings();
	@Override
	public ArgumentParser setParameter(final ArgumentParser p,
			final String prefix) {
		ArgumentParser oArgs = curSettings.setParameter(p, prefix);
		if (isInitialized) CalcOutDim(); // only run if the images have been loaded
		return oArgs;
	}
	
	
	public FilterSettings getFilterSettings() {return curSettings;}
	public void setFilterSettings(FilterSettings inSettings) {curSettings=inSettings;}
	

	@TIPLPluginManager.PluginInfo(pluginType = "Filter",
			desc="Full memory filter",
			sliceBased=false,
			maximumSize=1024*1024*1024,
			bytesPerVoxel=-1,
			speedRank=11)
    final public static class vfsFactory implements TIPLPluginManager.TIPLPluginFactory {
		@Override
		public ITIPLPlugin get() {
			return new FilterScale();
		}
	};
	
	

	/** Output aim length */
	int itAimLength;

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
		
	}

	/**
	 * calculate the dimensions of the output volume
	 */
	protected void CalcOutDim() {
		final D3int up = curSettings.upfactor;
		final D3int dn = curSettings.downfactor;
		final int nx = (int) Math.round((float) (up.x + 0.0) / (dn.x + 0.0)
				* dim.x);
		final int ny = (int) Math.round((float) (up.y + 0.0) / (dn.y + 0.0)
				* dim.y);
		final int nz = (int) Math.round((float) (up.z + 0.0) / (dn.z + 0.0)
				* dim.z);

		odim = new D3int(nx, ny, nz);

		// Calculate bounds for output image olow/upp
		olowx = 0;
		olowy = 0;
		olowz = 0;

		ouppx = odim.x;
		ouppy = odim.y;
		ouppz = odim.z;

		oelsize = new D3float((dn.x + 0.0) / (up.x + 0.0) * elSize.x, (dn.y + 0.0)
				/ (up.y + 0.0) * elSize.y, (dn.z + 0.0) / (up.z + 0.0) * elSize.z);

		opos = new D3int(
				(int) Math.round((up.x + 0.0) / (dn.x + 0.0) * ipos.x),
				(int) Math.round((up.y + 0.0) / (dn.y + 0.0) * ipos.y),
				(int) Math.round((up.z + 0.0) / (dn.z + 0.0) * ipos.z)); // rescale
																		// outpos


	}
	/**
	 * actually allocated the new dimensions
	 */
	protected void AllocateDim() {
		//
		if (curSettings.oimageType == -1)
			curSettings.oimageType = imageType;
		// Initialize aimArray
		switch (curSettings.oimageType) {
		case TImgTools.IMAGETYPE_CHAR: // Byte
			outAimByte =(char[]) TImgTools.watchBigAlloc(curSettings.oimageType, (int) odim.prod());
			break;
		case TImgTools.IMAGETYPE_SHORT: // Short
			outAimShort = (short[]) TImgTools.watchBigAlloc(curSettings.oimageType, (int) odim.prod());
			break;
		case TImgTools.IMAGETYPE_INT: // Int
			outAimInt = (int[]) TImgTools.watchBigAlloc(curSettings.oimageType, (int) odim.prod());
			break;
		case TImgTools.IMAGETYPE_FLOAT: // Float
			outAimFloat=(float[]) TImgTools.watchBigAlloc(curSettings.oimageType, (int) odim.prod());
			break;
		case TImgTools.IMAGETYPE_BOOL: // Boolean
			outAimMask = (boolean[]) TImgTools.watchBigAlloc(curSettings.oimageType, (int) odim.prod());
			break;
		}

		System.out.println("New output: dim-" + odim + ", ElSize:" + oelsize
				+ ", Pos:" + opos + "," + StrPctRatio(odim.prod(), dim.prod())
				+ " VolRed, ImgT:" + curSettings.oimageType);
		procLog += "New output: dim-" + odim + ", pos:" + opos + ", "
				+ StrPctRatio(odim.prod(), dim.prod()) + " VolRed, ImgT:"
				+ imageType + "\n";
	}
	
	@Override
	public int neededCores() {
		return BaseTIPLPluginIn.maxUtilizedCores(TIPLGlobal.availableCores, olowz, ouppz, 1);
	}
	/**
	 * Object to divide the thread work into supportCores equal parts, default
	 * is z-slices
	 */
	@Override
	public Object divideThreadWork(final int cThread, final int maxCores) {
		return BaseTIPLPluginIn.sliceProcessWork(cThread, neededCores(), olowz, ouppz, 1);
	}

	@Override
	public boolean execute() {
		return runFilter();
	}
	
	@Override
	public TImg[] ExportImages(final TImgRO inImage) {
		TImgRO.CanExport templateAim = TImgTools.makeTImgExportable(inImage);
		return new TImg[] {CreateOutputImage(templateAim)};
	}
	/**
	 * Exports the FilterScaled result based on a template aim
	 * 
	 * @param templateAim
	 *            input template aim file
	 */

	public TImg CreateOutputImage(final TImgRO.CanExport templateAim) {
		if (isInitialized) {
			if (runCount > 0) {
				TImg outVirtualAim;
				switch (curSettings.oimageType) {
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
							.println("Input type not supported " + curSettings.oimageType);
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
		return curSettings.scalingFilterGenerator.make();

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
		CalcOutDim();
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
		final D3int up = curSettings.upfactor;
		final D3int dn = curSettings.downfactor;
		System.out.println("Rescale Operation Running : Upscale:(" + up.x + ","
				+ up.y + "," + up.z + "), Downscale (" + dn.x + "," + dn.y + ","
				+ dn.z + ")");
		CalcOutDim();
		AllocateDim();
		runMulticore();
		runCount++;
		String filterNameOut = "NearestNeighbor";
		if (curSettings.scalingFilterGenerator != null)
			filterNameOut = curSettings.scalingFilterGenerator.make().filterName();

		final String logAdd = "FilterScale Operation (" + filterNameOut
				+ ") : Upscale:(" + up.x + ", " + up.y + ", " + up.z
				+ "), Downscale (" + dn.x + ", " + dn.y + ", " + dn.z + ")";
		procLog += logAdd;
		return true;
	}

	/** Code to actually run the filter code on a portion of the image */
	protected boolean runFilter(final int bSlice, final int tSlice) {
		final D3int up = curSettings.upfactor;
		final D3int dn = curSettings.downfactor;
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
					float iposz = (dn.z + 0.0f) / (up.z + 0.0f) * oz;
					float iposy = (dn.y + 0.0f) / (up.y + 0.0f) * oy;
					float iposx = (dn.x + 0.0f) / (up.x + 0.0f) * ox;

					// Range to scan in input image
					int tilowx, tilowy, tilowz, tiuppx, tiuppy, tiuppz;

					tilowx = (int) Math.floor(iposx - dn.x);
					tilowy = (int) Math.floor(iposy - dn.y);
					tilowz = (int) Math.floor(iposz - dn.z);

					tiuppx = (int) Math.ceil(iposx + dn.x);
					tiuppy = (int) Math.ceil(iposy + dn.y);
					tiuppz = (int) Math.ceil(iposz + dn.z);

					if (curSettings.scalingFilterGenerator == null) {
						iposx = min(uppx, max((int) iposx, lowx));
						iposy = min(uppy, max((int) iposy, lowy));
						iposz = min(uppz, max((int) iposz, lowz));

						inoff = ((int) iposz * dim.y + (int) iposy) * dim.x
								+ (int) iposx;

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
						switch (curSettings.oimageType) {
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
						switch (curSettings.oimageType) {

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
		if (curSettings.scalingFilterGenerator != null)
			filterNameOut = curSettings.scalingFilterGenerator.make().filterName();

		String logAdd = "FilterScale Operation (" + filterNameOut
				+ ") : Upscale:(" + up.x + ", " + up.y + ", " + up.z
				+ "), Downscale (" + dn.x + ", " + dn.y + ", " + dn.z + "), T:"
				+ eTime;
		logAdd += "\n InMean:" + String.format("%.2f", inSum / inCnt)
				+ ", OutMean:" + String.format("%.2f", outSum / outCnt) + ", "
				+ String.format("%.2f", outCnt / 1e6) + " Mvx, "
				+ String.format("%.2f", inCnt / 1e3) + " Kop, "
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




}
