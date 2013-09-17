package tipl.tools;

import java.util.Date;

import tipl.formats.TImg;
import tipl.util.ArgumentParser;
import tipl.util.TImgTools;

// Used as a replacement for the moment function as it allows much more control over data
// and communication with webservices (potentially?)
/** Performs voronoi dilation on objects into a mask */
public class kVoronoi extends VoronoiTransform {
	public static void main(String[] args) {
		final String kVer = "120105_002";
		System.out.println(" kVoronoi Script v" + kVer);
		System.out.println(" Dilates and  v" + kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final ArgumentParser p = new ArgumentParser(args);

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

		if (p.hasOption("?") || (labelsAimName.length() < 1)) {
			System.out.println(" kVoronoi Help");
			System.out
					.println(" Performs voronoi transform on labeled images into given mask (filled ROI if no mask is given)");
			System.out.println(" Arguments::");
			System.out.println("  ");
			System.out.println(p.getHelp());
			System.exit(0);
		}

		long preload, start;

		// Read in the data set
		preload = System.currentTimeMillis();

		System.out.println("Loading " + labelsAimName + " ...");
		final TImg labelsAim = TImgTools.ReadTImg(labelsAimName);
		TImg maskAim;

		start = System.currentTimeMillis();
		kVoronoi KV;

		if (maskAimName.length() > 0) {
			System.out.println("Loading the mask " + maskAimName + " ...");
			maskAim = TImgTools.ReadTImg(maskAimName);
			start = System.currentTimeMillis();
			KV = new kVoronoi(labelsAim, maskAim);
		} else {
			KV = new kVoronoi(labelsAim);
			maskAim = labelsAim;
		}

		KV.run();

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

	protected volatile double emptyVoxels = 0;
	protected volatile double outsideMask = 0;
	protected volatile double fullVoxels = 0;
	protected volatile int cMaxDist = -1;
	/** curOperation (0 = default/master thread, 1 = grow, 2 = swap) */
	protected int curOperation = 0;
	/** The distance map correctly initialized */
	protected boolean preScanDone = false;

	protected volatile boolean changes;
	protected volatile int curIter;
	protected volatile double fDist;

	protected volatile int changescnt, gtBlocks = 0, validVoxels = 0;

	public kVoronoi(TImg labelAim) {
		super(labelAim);
	}

	/**
	 * Make a distance map of the distance away from the edge of a given object,
	 * useful for mask distance or bone surface distance
	 * 
	 * @param maskAim
	 *            The mask aim file to use for the edge
	 * @param includeEdges
	 *            Whether or not regions where the image touches the boundary
	 *            should be included as edges
	 **/
	public kVoronoi(TImg maskAim, boolean includeEdges) {
		super(1);
		EdgeMask(maskAim, includeEdges);
	}

	public kVoronoi(TImg labelAim, TImg maskAim) {
		super(labelAim, maskAim);
	}

	protected void EdgeMask(TImg maskAim, boolean includeEdges) {
		mask = TImgTools.makeTImgFullReadable(maskAim).getBoolAim();
		aimLength = mask.length;
		labels = new int[aimLength];
		outlabels = labels;
		distmap = new int[aimLength];
		InitDims(maskAim.getDim(), maskAim.getOffset());
		isInitialized = true;

		final int maxVal = 1;
		emptyVoxels = 0;
		outsideMask = 0;
		fullVoxels = 0;
		System.out.println("Scanning Mask File...");
		for (int z = lowz; z < uppz; z++) {
			for (int y = lowy; y < uppy; y++) {
				int off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
					if (labels[off] > 0) {
						distmap[off] = 0;
						fullVoxels++;
					} else if (mask[off]) {
						if (includeEdges) {
							// If we are on an image boundary, then set the
							// distance to 1 and turn the voxel on
							if ((x == lowx) | (x == uppx) | (y == lowy)
									| (y == uppy) | (z == lowz) | (z == uppz)) {
								distmap[off] = (int) (1.0 / distScalar);
								labels[off] = 1;
								fullVoxels++;
							} else {
								distmap[off] = MAXDISTVAL;
								emptyVoxels++;
							}
						} else {
							distmap[off] = MAXDISTVAL;
							emptyVoxels++;
						}

					} else {
						labels[off] = 1; // Everything outside of the mask is
											// turned on!
						outsideMask++;
					}

				}
			}
		}
		System.out.println("Mask Occupation(%) "
				+ StrPctRatio(emptyVoxels + fullVoxels, emptyVoxels
						+ fullVoxels + outsideMask) + ", Seed Porosity(%) : "
				+ StrPctRatio(emptyVoxels, emptyVoxels + fullVoxels) + ", "
				+ maxVal);
		preScanDone = true;
	}

	@Override
	public boolean execute() {
		return execute(MAXDIST);
	}

	public boolean execute(double maxIterDist) {
		maxUsuableDistance = maxIterDist;
		switch (curOperation) {
		case 1:
			// Grow
		case 2:
			// Swap
			return runMulticore();
		case 0:
		default:

			System.out.println("Setting up Voronoi Transform (" + neededCores()
					+ ")...");

			runTransform();
		}
		return true;

	}

	@Override
	public String getPluginName() {
		return "kVoronoi";
	}

	protected synchronized void ichanges(int nchangescnt, boolean nchanges) {
		changescnt += nchangescnt;
		if (nchanges)
			changes = true;
	}

	/** Command each sub thread should run */
	@Override
	protected void processWork(Object currentWork) {
		final int[] range = (int[]) currentWork;
		final int bSlice = range[0];
		final int tSlice = range[1];

		switch (curOperation) {
		case 1:
			// Erosion
			subGrow(bSlice, tSlice);
			return;
		case 2:
			// Dilation
			subSwap(bSlice, tSlice);
			return;
		case 0:
		default:
			System.err.println("Warning : '" + getPluginName()
					+ "' is being used incorrectly in multicore mode!!: "
					+ curOperation + ", <" + bSlice + ", " + tSlice + ">");
		}
	}

	@Override
	@Deprecated
	public void run() {
		execute();
	}

	protected void runPreScan() {
		int maxVal = -1;
		emptyVoxels = 0;
		outsideMask = 0;
		fullVoxels = 0;

		System.out.println("Scanning Image... MAXDIST:" + maxUsuableDistance);
		for (int z = lowz; z < uppz; z++) {
			for (int y = lowy; y < uppy; y++) {
				int off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {

					if (labels[off] > 0) {
						fullVoxels++;
						distmap[off] = 0;
						if (maxVal < labels[off])
							maxVal = labels[off];
					} else if (mask[off]) {
						distmap[off] = MAXDISTVAL;
						emptyVoxels++;
					} else {
						outsideMask++;
					}

				}
			}
		}
		System.out.println("Mask Occupation(%) "
				+ StrPctRatio(emptyVoxels + fullVoxels, emptyVoxels
						+ fullVoxels + outsideMask) + ", Seed Porosity(%) : "
				+ StrPctRatio(emptyVoxels, emptyVoxels + fullVoxels) + ", "
				+ maxVal);
		preScanDone = true;
	}

	public void runTransform() {
		if (!preScanDone)
			runPreScan();
		cMaxDist = ((int) (maxUsuableDistance / distScalar));
		changes = true;

		while (changes) {
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

				System.out.println("Distance :"
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
			// End While Loop
		}
		System.out.println("Checking Distance Map...");
		// This section of the code swaps neighbors which are close
		changes = true;
		int swapSteps = 0;
		while (changes) {

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
		}
		System.out.println("Cleaning up Distance Map");
		for (int i = 0; i < aimLength; i++)
			if (distmap[i] == MAXDISTVAL)
				distmap[i] = -1;
		procLog += "CMD:kVoronoi: Max Dist:" + fDist + " (" + curIter
				+ "), swaps: " + swapSteps;
		runCount = 1;

	}

	public void subGrow(int bSlice, int tSlice) {
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
		for (int z = bSlice; z < tSlice; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
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
												final double cDist = Math
														.sqrt(Math.pow(x2 - x,
																2)
																+ Math.pow(y2
																		- y, 2)
																+ Math.pow(z2
																		- z, 2))
														/ distScalar;
												final int ikDist = distmap[off2]
														+ (int) cDist;
												// ensure the nearest voxel is
												// full && the distance is
												// smaller than the current wave
												// && it is the local minimum
												final int ctPosVal = (short) outlabels[off2];
												if (checkGrowthTemplate(off,
														off2, gtmode)) {
													if ((ctPosVal > 0)
															&& (curIter >= ikDist)
															&& (ikDist < distmap[off])) {
														// If voxel is on and
														// matches value then
														// turn on original
														if (cPosVal < 1) {
															nfullVoxels++;
															nemptyVoxels--;
														}
														/**
														 * replace with
														 * synchronized code
														 */
														// outlabels[off]=outlabels[off2];
														// distmap[off]=ikDist;
														// // Voxel is
														// ineligible for
														// growing until next
														// iteration
														usSetVoxel(off, off2,
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
					} else { // Full voxels less than empty voxels (majority
								// porous)
						if ((cPosVal > 0)) {// Eligible Full Voxel
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

												final double cDist = Math
														.sqrt(Math.pow(x2 - x,
																2)
																+ Math.pow(y2
																		- y, 2)
																+ Math.pow(z2
																		- z, 2))
														/ distScalar;
												final int ikDist = distmap[off]
														+ (int) cDist;

												// System.out.println(x2+"-"+x+", "+y2+"-"+y+", "+z2+"-"+z+"  "+ikDist+", "+cDist);
												// ensure the nearest voxel is
												// full && the distance is
												// smaller than the current wave
												// && it is the local minimum
												final int ctPosVal = outlabels[off2];

												// ensure the vx is empty && the
												// distance is smaller than the
												// current wave && it is the
												// local minimum
												if (checkGrowthTemplate(off,
														off2, gtmode)) {
													if ((ctPosVal < 1)
															&& (curIter >= ikDist)
															&& (ikDist < distmap[off2])) {

														// If voxel is on and
														// matches value then
														// turn on original

														nfullVoxels++;
														nemptyVoxels--;

														usSetVoxel(off2, off,
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
			}
		}
		fullVoxels += nfullVoxels;
		emptyVoxels += nemptyVoxels;
		gtBlocks += ngtBlocks;
		changescnt += nchangescnt;
		validVoxels += nvalidVoxels;
		if (nchanges)
			changes = true;

	}

	protected void subSwap(int bSlice, int tSlice) {
		int off;
		boolean nchanges = false;
		int nchangescnt = 0;
		// Code for stationaryKernel
		BaseTIPLPluginIn.stationaryKernel curKernel;
		if (neighborKernel == null)
			curKernel = new BaseTIPLPluginIn.stationaryKernel();
		else
			curKernel = new BaseTIPLPluginIn.stationaryKernel(neighborKernel);
		for (int z = bSlice; z < tSlice; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
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
													nchangescnt++;
												}
											}
										}
									}

								}
							}
						}
					}
				}
			}
		}
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
	public synchronized void synSetVoxel(int off2, int off, int ikDist) {
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
	public void usSetVoxel(int off2, int off, int ikDist) {
		outlabels[off2] = outlabels[off];
		distmap[off2] = ikDist;

	}
}
