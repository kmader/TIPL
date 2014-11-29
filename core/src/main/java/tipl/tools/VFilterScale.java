package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.*;

/**
 * A plugin to rescale and/or filter Aim files, the V means virtual since it
 * operates without loading the entire dataset first. Allowing datasets which
 * are much bigger than the current memory or javas 8gigavoxel limit to be
 * downsampled
 * 
 * @author Kevin Mader
 */

public class VFilterScale extends FilterScale {	
	
	
	@TIPLPluginManager.PluginInfo(pluginType = "Filter",
			desc="Slice based filter tool",
			sliceBased=true,
			maximumSize=-1,
			bytesPerVoxel=-1)
	final public static class vfsFactory implements TIPLPluginManager.TIPLPluginFactory {
		@Override
		public ITIPLPlugin get() {
			return new VFilterScale();
		}
	};
	


	public boolean isVirtual;
	/** Store the input aim-file, allows for slice by slice resizing */
	protected TImgRO _inputAim;
	/** Is the full dataset loaded or just slices */
	boolean fullLoaded = true;

	protected VFilterScale() {
	}

	private VFilterScale(final TImgRO inAim) {
		LoadImages(new TImgRO[] { inAim });
	}

	@Override
	public String getPluginName() {
		return "VFilterScale";
	}

	/**
	 * Exports the VFilterScaled result based on a template aim
	 * 
	 * @param rawTemplateAim
	 *            input template aim file
	 */
	public TImg JunkExportAim(final TImgRO rawTemplateAim) {
		if (isInitialized) {
			if (runCount > 0) {

				final TImgRO.CanExport templateAim =
						TImgTools.getStorage().wrapTImgRO(rawTemplateAim);

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
					System.err.println("Input type not supported" + imageType);
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

	@Override
	public void LoadImages(final TImgRO[] inImages) {
		if (inImages.length < 1)
			throw new IllegalArgumentException(
					"Too few arguments for LoadImages in:" + getPluginName());
		final TImgRO inImg = inImages[0];
		elSize = inImg.getElSize();
		ipos = inImg.getPos();
		_inputAim = inImg;
		imageType = inImg.getImageType();
		fullLoaded = false;
		System.out.println("VFS Image:" + inImg + " loaded as:" + imageType);
		InitLabels(inImg.getDim(), inImg.getOffset());
	}

	/**
	 * Code to actually run the filter code on a portion of the image, loading
	 * each slice as needed
	 */
	@Override
	protected boolean runFilter(final int bSlice, final int tSlice) {
		final D3int up = curSettings.upfactor;
		final D3int dn = curSettings.downfactor;
		final int inImageType = _inputAim.getImageType();
		final int outImageType = curSettings.oimageType;
		int ooff;
		// Loop through new image
		if (supportsThreading)
			System.out.println("Virtual (Slice-Based) Filter Running -- :<"
					+ bSlice + "," + tSlice + "> @ " + Thread.currentThread());
		final long sTime = System.currentTimeMillis();
		runCount++;
		double inSum = 0.0;
		double inCnt = 0.0;
		double outSum = 0.0;
		double outCnt = 0.0;
		int zSliceLoaded = -1;

		// Setup the slice cache
		Object[] cachedSlices = null;
		cachedSlices = new Object[uppz - lowz];
		for (int iz = lowz; iz < uppz; iz++)
			cachedSlices[iz - lowz] = null;

		for (int oz = bSlice; oz < tSlice; oz++) {
			boolean[] blSlice = null;
			char[] chSlice = null;
			int[] itSlice = null;
			short[] shSlice = null;
			float[] ftSlice = null;


			// Slices to scan in the input image (should be preloaded)
			// This code is still pretty crappy because if there are overlapping
			// slices it reloads them every time (2/3 scaling for example or
			// simply filtering)
			// but it keeps everything later much simpler i think

			float iposz = (dn.z + 0.0f) / (up.z + 0.0f) * oz;
			int tilowz, tiuppz;
			tilowz = (int) Math.floor(iposz - dn.z);
			tiuppz = (int) Math.ceil(iposz + dn.z);
			tilowz = max(lowz, tilowz);
			tiuppz = min(tiuppz, uppz);

			
			for (int iz = lowz; iz < uppz; iz++) {
				if ((iz >= tilowz) && (iz < tiuppz)) { // a slice we want to
														// cache
					if (cachedSlices[iz - lowz] == null) { // not already in
															// cache
						try {
							cachedSlices[iz - lowz] = _inputAim.getPolyImage(
									iz, inImageType);
							// _inputAim.PrivateSliceAccess(iz);
							// System.out.println("/Reading Slice!!! ["+iz+"]");
						} catch (final Exception e) {
							System.err.println("Error Reading Slice!!! [" + iz
									+ "]");
							e.printStackTrace();

						}
					}
				} else
					cachedSlices[iz - lowz] = null;
			}
			TIPLGlobal.runGC();
			for (int oy = olowy; oy < ouppy; oy++) {

				ooff = (oz * odim.y + oy) * odim.x + olowx;
				for (int ox = olowx; ox < ouppx; ox++, ooff++) {
					// Interpolate position in input image

					float iposy = (dn.y + 0.0f) / (up.y + 0.0f) * oy;
					float iposx = (dn.x + 0.0f) / (up.x + 0.0f) * ox;

					// Range to scan in input image
					int tilowx, tilowy, tiuppx, tiuppy;

					tilowx = (int) Math.floor(iposx - dn.x);
					tilowy = (int) Math.floor(iposy - dn.y);

					tiuppx = (int) Math.ceil(iposx + dn.x);
					tiuppy = (int) Math.ceil(iposy + dn.y);

					if (curSettings.scalingFilterGenerator == null) {
						iposx = min(uppx, max((int) iposx, lowx));
						iposy = min(uppy, max((int) iposy, lowy));
						iposz = min(uppz, max((int) iposz, lowz));

						double dcVox = 0;
						if ((int) iposz != zSliceLoaded) {
							try {

								switch (inImageType) { // Only way to be sure
														// the data is actually
														// the right type
								case TImgTools.IMAGETYPE_BOOL: // Boolean
									blSlice = (boolean[]) cachedSlices[(int) iposz
											- lowz];
									break;
								case TImgTools.IMAGETYPE_CHAR: // Byte
									chSlice = (char[]) cachedSlices[(int) iposz
											- lowz];
									break;
								case TImgTools.IMAGETYPE_SHORT: // Short
									shSlice = (short[]) cachedSlices[(int) iposz
											- lowz];
									break;
								case TImgTools.IMAGETYPE_INT: // Integer
									itSlice = (int[]) cachedSlices[(int) iposz
											- lowz];
									break;
								case TImgTools.IMAGETYPE_FLOAT: // Float
									ftSlice = (float[]) cachedSlices[(int) iposz
											- lowz];
									break;
								default:
									System.err
											.println("What sort of crap are you giving me:"
													+ _inputAim.getImageType());
								}
								zSliceLoaded = (int) iposz;
								// System.out.println("Reading Slice ["+oz+"]");
							} catch (final Exception e) {
								System.err.println("Error Reading Slice!!!");
								e.printStackTrace();

							}
						}

						final int sOff2 = ((int) iposy) * dim.x + (int) iposx; // Offset
																				// in
																				// the
																				// current
																				// slice
						switch (inImageType) { // Only way to be sure the data
												// is actually the right type
						case TImgTools.IMAGETYPE_BOOL: // Boolean
							if (blSlice[sOff2])
								dcVox++;
							break;
						case TImgTools.IMAGETYPE_CHAR: // Byte
							dcVox = (chSlice)[sOff2];
							break;
						case TImgTools.IMAGETYPE_SHORT: // Short
							dcVox = (shSlice)[sOff2];
							break;
						case TImgTools.IMAGETYPE_INT: // Integer
							dcVox = (itSlice)[sOff2];
							break;
						case TImgTools.IMAGETYPE_FLOAT: // Float
							dcVox = (ftSlice)[sOff2];
							break;
						}

						switch (curSettings.oimageType) {
						case TImgTools.IMAGETYPE_CHAR: // Byte
							outAimByte[ooff] = (char) dcVox;

							break;
						case TImgTools.IMAGETYPE_SHORT: // Short
							outAimShort[ooff] = (short) dcVox;
							break;
						case TImgTools.IMAGETYPE_INT: // Int
							outAimInt[ooff] = (int) dcVox;

							break;
						case TImgTools.IMAGETYPE_FLOAT: // Float
							outAimFloat[ooff] = (float) dcVox;

							break;
						case TImgTools.IMAGETYPE_BOOL: // Boolean
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
							try {
								switch (_inputAim.getImageType()) { // Only way
																	// to be
																	// sure the
																	// data is
																	// actually
																	// the right
																	// type
								case TImgTools.IMAGETYPE_BOOL: // Boolean
									blSlice = (boolean[]) cachedSlices[iz
											- lowz];
									break;
								case TImgTools.IMAGETYPE_CHAR: // Byte
									chSlice = (char[]) cachedSlices[iz - lowz];
									break;
								case TImgTools.IMAGETYPE_SHORT: // Short
									shSlice = (short[]) cachedSlices[iz - lowz];
									break;
								case TImgTools.IMAGETYPE_INT: // Integer
									itSlice = (int[]) cachedSlices[iz - lowz];
									break;
								case TImgTools.IMAGETYPE_FLOAT: // Float
									ftSlice = (float[]) cachedSlices[iz - lowz];
									break;
								default:
									System.err
											.println("What sort of crap are you giving me:"
													+ _inputAim.getImageType());
								}
								// System.out.println("Reading Slice ["+oz+"]");
							} catch (final Exception e) {
								System.err.println("Error Reading Slice!!! ["
										+ oz + "]");
								e.printStackTrace();

							}

							for (int iy = max(lowy, tilowy); iy < min(tiuppy,
									uppy); iy++) {

								int sOff2 = (iy) * dim.x + max(lowx, tilowx); // Offset
																				// in
																				// the
																				// current
																				// slice

								for (int ix = max(lowx, tilowx); ix < min(
										tiuppx, uppx); ix++, sOff2++) {
									double dcVox = 0;
									switch (_inputAim.getImageType()) { // Only
																		// way
																		// to be
																		// sure
																		// the
																		// data
																		// is
																		// actually
																		// the
																		// right
																		// type
									case TImgTools.IMAGETYPE_BOOL: // Boolean
										if (blSlice[sOff2])
											dcVox++;
										break;
									case TImgTools.IMAGETYPE_CHAR: // Byte
										dcVox = (chSlice)[sOff2];
										break;
									case TImgTools.IMAGETYPE_SHORT: // Short
										dcVox = (shSlice)[sOff2];
										break;
									case TImgTools.IMAGETYPE_INT: // Integer
										dcVox = (itSlice)[sOff2];
										break;
									case TImgTools.IMAGETYPE_FLOAT: // Float
										dcVox = (ftSlice)[sOff2];
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
						switch (outImageType) {

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

		String logAdd = "VFilterScale Operation (" + filterNameOut
				+ ") : Upscale:(" + up.x + ", " + up.y + ", " + up.z
				+ "), Downscale (" + dn.x + ", " + dn.y + ", " + dn.z + "), T:"
				+ eTime;
		logAdd += "\n InMean:" + String.format("%.2f", inSum / inCnt)
				+ ", OutMean:" + String.format("%.2f", outSum / outCnt) + ", "
				+ String.format("%.2f", outCnt / 1e6) + " Mvx, "
				+ String.format("%.2f", inCnt / 1e9) + " Gop, "
				+ String.format("%.2f", inCnt / outCnt) + " Op/Vx";
		System.out.println(logAdd);
		return true;

	}
}
