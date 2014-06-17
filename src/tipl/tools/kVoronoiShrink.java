package tipl.tools;

import java.util.Date;

import tipl.formats.TImg;
import tipl.util.ArgumentParser;
import tipl.util.ITIPLPlugin;
import tipl.util.TIPLGlobal;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;

/** Performs voronoi dilation on objects into a mask and shrinks based on usage */
public class kVoronoiShrink extends kVoronoi {
	@TIPLPluginManager.PluginInfo(pluginType = "kVoronoi",
			desc="Full memory kvoronoi tesselation, optimized with shrinking bounds",
			sliceBased=false,
			speedRank=11)
	final public static TIPLPluginManager.TIPLPluginFactory myFactory = new TIPLPluginManager.TIPLPluginFactory() {
		@Override
		public ITIPLPlugin get() {
			return new kVoronoiShrink();
		}
	};
	public static void main(final String[] args) {
		final String kVer = "120105_002";
		System.out.println(" kVoronoi-Shrink Script v" + kVer);
		System.out.println(" Dilates and  v" + kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final ArgumentParser p = TIPLGlobal.activeParser(args);

		p.hasOption("debug");

		// Parse the filenames
		final String labelsAimName = p.getOptionString("labels", "",
				"Name labeled object input file");
		final String maskAimName = p.getOptionString("mask", "",
				"Name of the mask output file");
		final String vorVolumesName = p.getOptionString("vorvols", "",
				"Name of voronoi volumes output file");
		final String vorDistancesName = p.getOptionString("vordist", "",
				"Name of voronoi distances output file (distance from label");

		if (p.hasOption("?")) {
			System.out.println(" kVoronoi Help");
			System.out
					.println(" Performs voronoi transform on labeled images into given mask (filled ROI if no mask is given)");
			System.out.println(" Arguments::");
			System.out.println("  ");
			System.out.println(p.getHelp());
			System.exit(0);
		}

		long start;

		System.currentTimeMillis();

		kVoronoiShrink KV;

		TImg maskAim, labelsAim;

		if (labelsAimName.length() > 0) {
			System.out.println("Loading " + labelsAimName + " ...");
			labelsAim = TImgTools.ReadTImg(labelsAimName);
			if (maskAimName.length() > 0) {
				System.out.println("Loading the mask " + maskAimName + " ...");
				maskAim = TImgTools.ReadTImg(maskAimName);
				start = System.currentTimeMillis();
				KV = new kVoronoiShrink();
				KV.LoadImages(new TImg[] {labelsAim,maskAim});
			} else {
				KV = new kVoronoiShrink();
				KV.LoadImages(new TImg[] {labelsAim});
				maskAim = labelsAim;
			}
		} else {
			System.out.println("Loading the mask " + maskAimName
					+ " ... and making distance map");
			maskAim = TImgTools.ReadTImg(maskAimName);
			labelsAim = maskAim;
			KV = new kVoronoiShrink();
			KV.includeEdges=true;
			KV.LoadImages(new TImg[] {null,maskAim});
		}
		start = System.currentTimeMillis();
		KV.execute();

		KV.WriteVolumesAim(labelsAim, vorVolumesName);
		KV.WriteDistanceAim(maskAim, vorDistancesName);

		try {
			final float eTime = (System.currentTimeMillis() - start)
					/ (60 * 1000F);
			String outString = "";
			outString += "Run Finished in " + eTime + " mins @ " + new Date()
					+ "\n";
			// Too Much Database Writing
			System.out.println(outString);

		} catch (final Exception e) {
			System.out.println("DB Problem");
		}

	}

	public boolean supportsThreading = true;
	protected volatile boolean changes;
	protected volatile int curIter;
	// protected volatile double fDist;
	protected volatile int changescnt, gtBlocks = 0, validVoxels = 0;

	protected volatile int minActx, minActy, minActz, maxActx, maxActy,
			maxActz;
	protected volatile int tlowx, tuppx, tlowy, tuppy, tlowz, tuppz;

	public kVoronoiShrink() {
		
	}

	/**
	 * Object to divide the thread work into supportCores equal parts, default
	 * is z-slices
	 */
	@Override
	public Object divideThreadWork(final int cThread, final int maxCores) {
		final int minSlice = tlowz;
		final int maxSlice = tuppz;

		int myNeededCores = maxCores;

		if (2 * maxCores > (maxSlice - minSlice))
			myNeededCores = (maxSlice - minSlice) / 2; // At least 2 slices per
														// core and definitely
														// no overlap
		if (myNeededCores < 1)
			myNeededCores = 1;
		final int range = (maxSlice - minSlice) / myNeededCores;

		int startSlice = minSlice;
		int endSlice = startSlice + range;

		for (int i = 0; i < cThread; i++) {
			startSlice = endSlice; // must overlap since i<endSlice is always
									// used, endslice is never run
			endSlice = startSlice + range;
		}
		if (cThread == (maxCores - 1))
			endSlice = maxSlice;
		if (cThread >= maxCores)
			return null;
		return (new int[] { startSlice, endSlice });
	}

	/** scan for empty voxels inside the mask */
	public void emptyScan() {
		for (int z = lowz; z < uppz; z++) {

			for (int y = lowy; y < uppy; y++) {
				int off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
					boolean isActive = false;
					if (mask[off])
						if (outlabels[off] < 1)
							isActive = true;
					if (isActive) {
						if (x < minActx)
							setMinActx(x);
						if (x > maxActx)
							setMaxActx(x);
					}
					if (isActive) {
						if (y < minActy)
							setMinActy(y);
						if (y > maxActy)
							setMaxActy(y);
					}
					if (isActive) {
						if (z < minActz)
							setMinActz(z);
						if (z > maxActz)
							setMaxActz(z);
					}
				}
			}
		}
	}

	@Override
	public String getPluginName() {
		return "kVoronoi-Shrinking";
	}

	@Override
	protected synchronized void ichanges(final int nchangescnt,
			final boolean nchanges) {
		changescnt += nchangescnt;
		if (nchanges)
			changes = true;
	}

	@Override
	public void runTransform() {
		runTransform(MAXDIST);
	}

	public void runTransform(final double maxIterDist) {
		maxUsuableDistance = maxIterDist;
		if (!preScanDone)
			customInitSteps();
		cMaxDist = ((int) (maxIterDist / distScalar));
		changes = true;

		// t-ranges are the ranges that are actually run while normal are the
		// limits
		tlowx = lowx;
		tlowy = lowy;
		tlowz = lowz;

		tuppx = uppx;
		tuppy = uppy;
		tuppz = uppz;
		fDist = 1 - (2 - Math.sqrt(3));

		while (changes) {
			// Initialize with opposite extremes
			minActx = uppx;
			minActy = uppy;
			minActz = uppz;

			maxActx = lowx;
			maxActy = lowy;
			maxActz = lowz;

			System.out.println("KVS - <" + tlowx + "," + tlowy + "," + tlowz
					+ "> - <" + tuppx + "," + tuppy + "," + tuppz + ">");

			changes = false;

			gtBlocks = 0;
			for (int md = 0; md < 3; md++) {
				changescnt = 0;
				validVoxels = 0;
				double growDist;
				switch (md % 3) {
				case 0:
					growDist = 2 - Math.sqrt(3);
					break;
				case 1:
					growDist = Math.sqrt(2) - 1;
					break;
				case 2:
					growDist = Math.sqrt(3) - Math.sqrt(2);
					break;
				default:
					growDist = 0;
					System.out.println("ERROR :: Distance is invalid!!?!");
					break;
				}

				fDist += growDist;
				curIter = (int) (fDist / distScalar);
				if (curIter > cMaxDist) {
					System.out
							.println("ERROR :: Distance Exceeds Maximum Distance (1000vx)");
					break;
				}

				curOperation = 1;
				launchThread = Thread.currentThread();
				runMulticore();
				curOperation = 0;

				System.out.println("KDistance :"
						+ StrRatio(fDist, 1)
						+ "("
						+ curIter
						+ "), valid(%) : "
						+ StrPctRatio(validVoxels, emptyVoxels + fullVoxels)
						+ ", changes(pm) : "
						+ StrRatio(1000.0 * changescnt, emptyVoxels
								+ fullVoxels) + ", porosity(%) : "
						+ StrPctRatio(emptyVoxels, emptyVoxels + fullVoxels));

				if (gtmode >= 0)
					System.out.println(", GTB(pm):"
							+ StrRatio(1000. * gtBlocks, emptyVoxels));

				// End Mode Loop
			}

			// emptyScan();

			// Shrink the box

			tlowx = max(minActx - 1, lowx);
			tlowy = max(minActy - 1, lowy);
			tlowz = max(minActz - 1, lowz);

			tuppx = min(maxActx + 2, uppx); // lopsided because of the < instead
											// of <=
			tuppy = min(maxActy + 2, uppy);
			tuppz = min(maxActz + 2, uppz);

			// End While Loop
		}

		// Reset to Normal Boundaries
		tlowx = lowx;
		tlowy = lowy;
		tlowz = lowz;
		tuppx = uppx;
		tuppy = uppy;
		tuppz = uppz;

		System.out.println("Checking Distance Map...");
		// This section of the code swaps neighbors which are close
		changes = true;
		int swapSteps = 0;
		while (changes) {
			// Initialize with opposite extremes

			minActx = uppx;
			minActy = uppy;
			minActz = uppz;

			maxActx = lowx;
			maxActy = lowy;
			maxActz = lowz;

			System.out.println("KVS - <" + tlowx + "," + tlowy + "," + tlowz
					+ "> - <" + tuppx + "," + tuppy + "," + tuppz + ">");

			changes = false;
			changescnt = 0;

			curOperation = 2; // Swap
			launchThread = Thread.currentThread();
			runMulticore();
			curOperation = 0;

			swapSteps++;

			System.out.println("Steps : " + swapSteps + " Swaps : "
					+ changescnt + ", Active Region(pm): "
					+ StrPctRatio(10 * changescnt, fullVoxels));
			// End While Loop

			// Shrink the box

			tlowx = max(minActx - 1, lowx);
			tlowy = max(minActy - 1, lowy);
			tlowz = max(minActz - 1, lowz);

			tuppx = min(maxActx + 2, uppx);
			tuppy = min(maxActy + 2, uppy);
			tuppz = min(maxActz + 2, uppz);
		}
		System.out.println("Cleaning up Distance Map");
		for (int i = 0; i < aimLength; i++)
			if (distmap[i] == MAXDISTVAL)
				distmap[i] = 0;
		procLog += "CMD:kVoronoi: Max Dist:" + fDist + " (" + curIter
				+ "), swaps: " + swapSteps;
		runCount = 1;

	}

	protected synchronized void setMaxActx(final int a) {
		if (a > maxActx)
			maxActx = a;
	}

	protected synchronized void setMaxActy(final int a) {
		if (a > maxActy)
			maxActy = a;
	}

	protected synchronized void setMaxActz(final int a) {
		if (a > maxActz)
			maxActz = a;
	}

	protected synchronized void setMinActx(final int a) {
		if (a < minActx)
			minActx = a;
	}

	protected synchronized void setMinActy(final int a) {
		if (a < minActy)
			minActy = a;
	}

	protected synchronized void setMinActz(final int a) {
		if (a < minActz)
			minActz = a;
	}

	@Override
	public void subGrow(final int ibSlice, final int itSlice) {
		int off = 0;
		// Code for stationaryKernel
		BaseTIPLPluginIn.stationaryKernel curKernel;
		if (neighborKernel == null)
			curKernel = new BaseTIPLPluginIn.stationaryKernel();
		else
			curKernel = new BaseTIPLPluginIn.stationaryKernel(neighborKernel);

		int nfullVoxels = 0;
		int nemptyVoxels = 0;
		boolean nchanges = false;
		int nchangescnt = 0;
		int ngtBlocks = 0;
		int nvalidVoxels = 0;
		final int bSlice = min(max(ibSlice, lowz), uppz);
		final int tSlice = min(max(itSlice, lowz), uppz); // Check the slice
															// positions
		for (int z = bSlice; z < tSlice; z++) {
			boolean isActive = false;
			for (int y = tlowy; y < tuppy; y++) {
				off = (z * dim.y + y) * dim.x + tlowx;
				for (int x = tlowx; x < tuppx; x++, off++) {
					// The code is optimized so the least number of voxels make
					// it past the first check
					final int cPosVal = outlabels[off];

					int off2;

					if (emptyVoxels < fullVoxels) {

						if ((cPosVal < 1) && (distmap[off] == MAXDISTVAL)
								&& (mask[off])) {// Eligble Empty Voxel
							nvalidVoxels++;
							for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(
									z + neighborSize.z, uppz - 1); z2++) {
								for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(
										y + neighborSize.y, uppy - 1); y2++) {
									off2 = (z2 * dim.y + y2) * dim.x
											+ max(x - neighborSize.x, lowx);
									for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(
											x + neighborSize.x, uppx - 1); x2++, off2++) {
										if (curKernel.inside(off, off2, x, x2,
												y, y2, z, z2)) {
											if (off != off2) {
												if (mask[off2]) {
													final int ctPosVal = (short) outlabels[off2];
													if (ctPosVal > 0) { // is
																		// the
																		// voxel
																		// even
																		// fricking
																		// on
														isActive = true; // empty
																			// voxel
																			// next
																			// to
																			// full
																			// voxel
														final double cDist = Math
																.sqrt(Math.pow(
																		x2 - x,
																		2)
																		+ Math.pow(
																				y2
																						- y,
																				2)
																		+ Math.pow(
																				z2
																						- z,
																				2))
																/ distScalar;
														final int ikDist = distmap[off2]
																+ (int) cDist;
														// ensure the nearest
														// voxel is full && the
														// distance is smaller
														// than the current wave
														// && it is the local
														// minimum

														if (checkGrowthTemplate(
																off, off2,
																gtmode)) {
															if ((curIter >= ikDist)
																	&& (ikDist < distmap[off])) {

																nfullVoxels++;
																nemptyVoxels--;
																/**
																 * replace with
																 * synchronized
																 * code
																 */
																// outlabels[off]=outlabels[off2];
																// distmap[off]=ikDist;
																// // Voxel is
																// ineligible
																// for growing
																// until next
																// iteration
																usSetVoxel(off,
																		off2,
																		ikDist);

																nchanges = true;
																nchangescnt++;
															}
														} else
															ngtBlocks++;
													}
												}
											}
										}

									}
								}
							}
						}
					} else { // Full voxels less than empty voxels (majority
								// porous)
						if (cPosVal > 0) {// Eligible Full Voxel
							nvalidVoxels++;
							for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(
									z + neighborSize.z, uppz - 1); z2++) {
								for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(
										y + neighborSize.y, uppy - 1); y2++) {
									off2 = (z2 * dim.y + y2) * dim.x
											+ max(x - neighborSize.x, lowx);
									for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(
											x + neighborSize.x, uppx - 1); x2++, off2++) {
										if (curKernel.inside(off, off2, x, x2,
												y, y2, z, z2)) {
											if ((off != off2) && (mask[off2])) {
												final int ctPosVal = outlabels[off2];
												if (ctPosVal < 1) { // Check to
																	// make sure
																	// it is
																	// empty
													isActive = true; // Empty
																		// voxel
																		// bordering
																		// a
																		// full
																		// foxel
													final double cDist = Math
															.sqrt(Math.pow(x2
																	- x, 2)
																	+ Math.pow(
																			y2
																					- y,
																			2)
																	+ Math.pow(
																			z2
																					- z,
																			2))
															/ distScalar;
													final int ikDist = distmap[off]
															+ (int) cDist;

													// System.out.println(x2+"-"+x+", "+y2+"-"+y+", "+z2+"-"+z+"  "+ikDist+", "+cDist);
													// ensure the nearest voxel
													// is full && the distance
													// is smaller than the
													// current wave && it is the
													// local minimum

													// ensure the vx is empty &&
													// the distance is smaller
													// than the current wave &&
													// it is the local minimum
													if (checkGrowthTemplate(
															off, off2, gtmode)) {
														if ((curIter >= ikDist)
																&& (ikDist < distmap[off2])) {

															// If voxel is on
															// and matches value
															// then turn on
															// original

															nfullVoxels++;
															nemptyVoxels--;

															usSetVoxel(off2,
																	off, ikDist);
															nchanges = true;
															nchangescnt++;

														}
													} else
														ngtBlocks++;
												}
											}
										}

									}
								}
							}
						}
					}

					if (isActive) {
						if (x < minActx)
							setMinActx(x);
						if (x > maxActx)
							setMaxActx(x);
					}
				} // End X loop
				if (isActive) {
					if (y < minActy)
						setMinActy(y);
					if (y > maxActy)
						setMaxActy(y);
				}
			} // End y loop
			if (isActive) {
				if (z < minActz)
					setMinActz(z);
				if (z > maxActz)
					setMaxActz(z);
			}
		} // End z loop
		fullVoxels += nfullVoxels;
		emptyVoxels += nemptyVoxels;
		gtBlocks += ngtBlocks;
		changescnt += nchangescnt;
		validVoxels += nvalidVoxels;
		if (nchanges)
			changes = true;
		// System.out.println(bSlice+"::"+nchanges+", "+StrMvx(nchangescnt));

	}

	@Override
	protected void subSwap(final int ibSlice, final int itSlice) {
		int off;
		boolean nchanges = false;
		int nchangescnt = 0;
		// Code for stationaryKernel
		BaseTIPLPluginIn.stationaryKernel curKernel;
		if (neighborKernel == null)
			curKernel = new BaseTIPLPluginIn.stationaryKernel();
		else
			curKernel = new BaseTIPLPluginIn.stationaryKernel(neighborKernel);
		final int bSlice = min(max(ibSlice, lowz), uppz);
		final int tSlice = min(max(itSlice, lowz), uppz); // Check the slice
															// positions
		for (int z = bSlice; z < tSlice; z++) {

			boolean isActive = false;
			for (int y = tlowy; y < tuppy; y++) {
				off = (z * dim.y + y) * dim.x + tlowx;
				for (int x = tlowx; x < tuppx; x++, off++) {
					// The code is optimized so the least number of voxels make
					// it past the first check
					final int cPosVal = outlabels[off];

					if (cPosVal > 0) {// Eligble Full Voxel
						for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(
								z + neighborSize.z, uppz - 1); z2++) {
							for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(
									y + neighborSize.y, uppy - 1); y2++) {
								int off2 = (z2 * dim.y + y2) * dim.x
										+ max(x - neighborSize.x, lowx);
								for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(
										x + neighborSize.x, uppx - 1); x2++, off2++) {
									if (curKernel.inside(off, off2, x, x2, y,
											y2, z, z2)) {
										if (off != off2) {
											final double cDist = Math.sqrt(Math
													.pow(x2 - x, 2)
													+ Math.pow(y2 - y, 2)
													+ Math.pow(z2 - z, 2))
													/ distScalar;
											final int ikDist = distmap[off]
													+ (int) cDist;
											// ensure the nearest voxel is full
											// && the distance is smaller than
											// the current wave && it is the
											// local minimum
											final int ctPosVal = (short) outlabels[off2];

											if (checkGrowthTemplate(off, off2,
													gtmode)) {
												if ((ctPosVal > 0)
														&& (ikDist < distmap[off2])) {
													synSetVoxel(off2, off,
															ikDist);

													nchanges = true;
													isActive = true;
													nchangescnt++;
												}
											}
										}
									}

								}
							}
						}
					}

					if (isActive) {
						if (x < minActx)
							setMinActx(x);
						if (x > maxActx)
							setMaxActx(x);
					}

				} // End X loop
				if (isActive) {
					if (y < minActy)
						setMinActy(y);
					if (y > maxActy)
						setMaxActy(y);
				}

			} // End y loop

			if (isActive) {
				if (z < minActz)
					setMinActz(z);
				if (z > maxActz)
					setMaxActz(z);
			}
		} // End z loop
		ichanges(nchangescnt, nchanges);
		System.out.println("<" + bSlice + "," + tSlice + ">: " + nchanges
				+ ", " + StrMvx(nchangescnt));
	}

	/**
	 * synchronized (among threads) set voxel, used in swap, and grow
	 * 
	 * @param off2
	 *            voxel to be changed
	 * @param off
	 *            voxel to be copied
	 * @param ikDist
	 *            distance to be putinto the distance map at off2
	 */
	@Override
	public synchronized void synSetVoxel(final int off2, final int off,
			final int ikDist) {
		outlabels[off2] = outlabels[off];
		distmap[off2] = ikDist;
	}

	/**
	 * unsynced (among threads) set voxel, used in swap, and grow
	 * 
	 * @param off2
	 *            voxel to be changed
	 * @param off
	 *            voxel to be copied
	 * @param ikDist
	 *            distance to be putinto the distance map at off2
	 */
	@Override
	public void usSetVoxel(final int off2, final int off, final int ikDist) {
		outlabels[off2] = outlabels[off];
		distmap[off2] = ikDist;

	}
}
