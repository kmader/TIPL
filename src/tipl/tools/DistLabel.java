package tipl.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.D3int;
import tipl.util.ITIPLPlugin;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * DistLabel is the class used for labeling bubbles based on a distance map and
 * a mask
 */
public class DistLabel extends BaseTIPLPluginIO {
	@TIPLPluginManager.PluginInfo(pluginType = "DistLabel",
			desc="Full memory dist labeling",
			sliceBased=false)
	final public static TIPLPluginManager.TIPLPluginFactory myFactory = new TIPLPluginManager.TIPLPluginFactory() {
		@Override
		public ITIPLPlugin get() {
			return new DistLabel();
		}
	};
	private static class bubbleFiller extends Thread {
		// int cLabel;
		// private SeedLabel cBubble;

		volatile DistLabel parent;
		private SeedList bubList = new SeedList(MAXOVERLAP);
		public boolean isFinished = false;
		/** hovering means the thread is waiting for new bubbles **/
		public boolean isHovering = true;
		public int completedBubbles = 0;
		private final int core;

		public bubbleFiller(final DistLabel iparent, final int icore) {
			super("BubbleFiller[" + icore + "]");
			parent = iparent;
			core = icore;
		}

		public synchronized void addBubble(final SeedLabel nBubble) {
			bubList.append(nBubble);
			completedBubbles++;
		}

		/** does the current bubble overlap one of the existing seeds **/
		public boolean doesOverlap(final SeedLabel nBubble) {
			return bubList.doesBubbleOverlap(nBubble);
		}

		public int pendingBubbles() {
			return bubList.length();
		}

		/**
		 * Distribute (using divideThreadWork) and process (using processWork)
		 * the work across the various threads
		 */
		@Override
		public void run() {
			/** wait for the list to be occupied **/
			while ((isHovering) || (bubList.length() > 0)) {
				while ((isHovering) && (bubList.length() < 1))
					yield();
				if (bubList.length() < 1)
					break;
				isFinished = false;
				final SeedLabel cBubble = bubList.getFirst();

				final long ist = System.currentTimeMillis();
				try {
					parent.fillBubble(cBubble.label, cBubble.cx, cBubble.cy,
							cBubble.cz, cBubble.rad);
				} catch (final Exception e) {
					System.out.println("ERROR - Thread : " + this
							+ " has crashed, proceed carefully!");
					e.printStackTrace();
				}

				isFinished = true;
				final SeedList newList = bubList.allButFirst();
				synchronized (this) {
					bubList = newList;
				}

				System.out.println("BF Finished:"
						+ StrRatio(System.currentTimeMillis() - ist, 1000)
						+ ":, " + core + " @ <" + pendingBubbles() + ","
						+ cBubble + ">");
			}

		}
	}

	/** Run the distance label initialization routines in parallel */
	private static class dlRunner extends Thread {
		int sslice, fslice;
		volatile DistLabel parent;
		public SeedList threadSeedList = null;

		public dlRunner(final DistLabel iparent, final int isslice,
				final int ifslice) {
			super("dlRunner:<" + isslice + ", " + ifslice + ">");
			sslice = isslice;
			fslice = ifslice;
			parent = iparent;
		}

		/**
		 * Distribute (using divideThreadWork) and process (using processWork)
		 * the work across the various threads
		 */
		@Override
		public void run() {
			threadSeedList = parent.locateSeeds(sslice, fslice);
			System.out.println("DlRunner Finished:, <" + sslice + ", " + fslice
					+ ">" + threadSeedList + ", k-"
					+ threadSeedList.killedLabels);
		}
	}

	static class SeedLabel implements Comparable<SeedLabel> {
		// public int label;

		public int cx;
		public int cy;
		public int cz;

		public float rad;
		public boolean valid;
		public int label = 0;

		// Scale distance by STD
		public SeedLabel(final int x, final int y, final int z, final float irad) {
			cx = x;
			cy = y;
			cz = z;
			rad = irad;
			valid = true;
		}

		public SeedLabel(final int x, final int y, final int z,
				final float irad, final int ilabel) {
			cx = x;
			cy = y;
			cz = z;
			rad = irad;
			label = ilabel;
			valid = true;
		}

		@Override
		public int compareTo(final SeedLabel otherBubble) // must be defined if
															// we are
		// implementing //Comparable
		// interface
		{
			if (!(otherBubble instanceof SeedLabel)) {
				throw new ClassCastException("Not valid SeedLabel object");
			}
			final SeedLabel tempName = otherBubble;
			// eliminate the duplicates when you sort
			if (this.rad > tempName.rad) {
				return -1;
			} else if (this.rad < tempName.rad) {
				return 1;
			} else {
				return 0;
			}
		}

		public double dist(final float x, final float y, final float z) {
			return Math.sqrt(Math.pow(x - cx, 2) + Math.pow(y - cy, 2)
					+ Math.pow(z - cz, 2));

		}

		public double dist(final int x, final int y, final int z) {
			return dist((float) x, (float) y, (float) z);
		}

		public void invalidate() {
			valid = false;
		}

		public boolean isValid() {
			return valid;
		}

		/** percentage the bubble nseed overlaps with this bubble (roughly) **/
		public double overlap(final SeedLabel nSeed) {
			final double cdist = nSeed.dist(cx, cy, cz);
			if (cdist > (rad + nSeed.rad))
				return 0;
			else
				return 100 * (rad + nSeed.rad - cdist) / nSeed.rad;
		}

		public void superSeed(final int x, final int y, final int z,
				final float nrad) {
			final String curString = toString();
			cx = x;
			cy = y;
			cz = z;
			rad = nrad;
			final String newString = toString();
			System.out.println("Too much overlap, bubble being replaced : "
					+ curString + " -> " + newString);
		}

		@Override
		public String toString() {
			return "COV:(" + cx + "," + cy + "," + cz + "), Rad:(" + rad + ")";
		}
	}

	static class SeedList {
		public int label;
		ArrayList<SeedLabel> sl;
		public int killedLabels = 0;
		public float overlapLimit;
		public boolean scaleddist = false;

		public SeedList(final float ioverlapLimit) {
			sl = new ArrayList<SeedLabel>();

			overlapLimit = ioverlapLimit;
		}

		public void addbubble(final int x, final int y, final int z,
				final float rad) {
			final SeedLabel newSeed = new SeedLabel(x, y, z, rad);
			boolean keepSeed = true;
			boolean hasReplaced = false;
			for (final SeedLabel curSeed : sl) {
				if (curSeed.isValid()) {
					if (newSeed.overlap(curSeed) >= overlapLimit) {
						if (hasReplaced) {
							// if it has already replaced another bubble then an
							// excessively overlapping bubble needs to be marked
							// for deletion
							curSeed.invalidate();
							killedLabels++;
						} else {
							if (newSeed.rad > curSeed.rad) {
								curSeed.superSeed(x, y, z, rad);
								killedLabels++;
								hasReplaced = true;
							} else {
								// System.out.println("New Seed Executed:"+newSeed);
								keepSeed = false;
								killedLabels++;

								break;
							}
						}
					}
				}
			}
			if (keepSeed)
				append(newSeed);

		}

		public SeedList allButFirst() {
			final SeedList newOut = new SeedList(overlapLimit);
			boolean isFirst = true;
			for (final SeedLabel curSeed : sl) {
				if (!isFirst)
					newOut.append(curSeed);
				else
					isFirst = false;
			}
			return newOut;
		}

		protected void append(final SeedLabel newSeed) {
			sl.add(newSeed);
		}

		/**
		 * checks whether the given seed overlaps with any bubbles already on
		 * the list, false if it does, true if not
		 **/
		public synchronized boolean doesBubbleOverlap(final SeedLabel newSeed) {
			for (final SeedLabel curSeed : sl) {
				if (curSeed.isValid()) {
					if (newSeed.overlap(curSeed) >= overlapLimit) {
						// System.out.println(curSeed+" overlaps "+newSeed+" by "+newSeed.overlap(curSeed));
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public void finalize() {
			final ArrayList<SeedLabel> filteredSL = new ArrayList<SeedLabel>();
			for (final SeedLabel cSeed : sl) {
				if (cSeed.isValid()) {
					filteredSL.add(cSeed);
				}
			}
			Collections.sort(filteredSL);
			sl = filteredSL;
		}

		/**
		 * public void removeFirst() { sl.remove(0); }
		 **/
		public SeedLabel getFirst() {
			return sl.get(0);
		}

		public Iterator<SeedLabel> kiter() {
			return sl.iterator();
		}

		public int length() {
			return sl.size();
		}

		@Override
		public String toString() {
			double maxRad = -1;
			if (sl.size() > 0)
				maxRad = sl.get(0).rad;
			return " " + length() + " bubbles (" + killedLabels
					+ "), largest with radius:" + maxRad + " ";
		}

	}

	public int[] distmap;

	public volatile int[] labels;
	public boolean[] mask;

	public boolean[] diffmask;
	public int maxlabel;
	public int unfilledVox = 0;

	private final int MAXDIST = 4000;

	private final int MAXDISTVAL = 32765;
	/** Voxels to remove the border **/
	public static final int OUTERSHELL = 2;
	public static final int MAXOVERLAP = 40;

	private final boolean DISTGROW = true;
	/** The value for T_Grow **/
	public static double TGROWTH = -0.9;
	/** The value for the flatness criteria **/
	public static double FLATCRIT = 3 / 8.0;// 0.401;
	private final int marchMode = 0;
	/** Scalar used for scaling the distance map into voxel space */
	public final double distScalar = (MAXDIST + 0.0) / (MAXDISTVAL + 0.0);

	/** The minimum distance from a wall or plateau border **/
	public static double ABSMINWALLDIST = 2;
	/** The minimum distance of a seed from a wall or plateau border **/
	public static double MINWALLDIST = 4;

	final private boolean useLaplacian = false;

	SeedList startSeedList;
	int remVoxels = aimLength;
	int totalVoxels = aimLength;
	protected double lcAvg = 0;

	protected DistLabel() {
		
	}
	/**
	 * Constructor based on just the distance map, assume everything is
	 * available mask
	 * 
	 * @param imap
	 *            Distance map image
	 */
	@Deprecated
	public DistLabel(final TImgRO imap) {
		LoadAimData(imap);
	}

	/**
	 * Constructor based on the distance map and mask aim images
	 * 
	 * @param imap
	 *            Distance map image
	 * @param imask
	 *            Mask to be filld and identified
	 */
	@Deprecated
	public DistLabel(final TImgRO imap, final TImgRO imask) {
		LoadAimData(imap, imask);
	}

	/**
	 * Object to divide the thread work into supportCores equal parts, default
	 * is z-slices
	 */
	public int[] divideSlices(final int cThread) {
		final int minSlice = lowz + OUTERSHELL;
		final int maxSlice = (uppz - OUTERSHELL);

		final int range = (maxSlice - minSlice) / neededCores();

		int startSlice = minSlice;
		int endSlice = startSlice + range;

		for (int i = 0; i < cThread; i++) {
			startSlice = endSlice; // must overlap since i<endSlice is always
									// used, endslice is never run
			endSlice = startSlice + range;
		}
		if (cThread == (neededCores() - 1))
			endSlice = maxSlice;
		if (cThread >= neededCores())
			return null;
		return (new int[] { startSlice, endSlice });
	}

	/**
	 * temporary solution until a more robust stack data managament gets
	 * integrated, since java cannot handle more than 8e9 array elements AIM
	 * style referencing from IPL will not work
	 **/
	private int[][][] dmapGet(final int x, final int y, final int z,
			final D3int nSize) {
		final int[][][] outMap = new int[nSize.x * 2 + 1][nSize.z * 2 + 1][nSize.z * 2 + 1];
		int dz = 0;
		for (int z2 = max(z - nSize.z, lowz); z2 <= min(z + nSize.z, uppz - 1); z2++, dz++) {
			int dy = 0;
			for (int y2 = max(y - nSize.y, lowy); y2 <= min(y + nSize.y,
					uppy - 1); y2++, dy++) {
				int off2 = (z2 * dim.y + y2) * dim.x + max(x - nSize.x, lowx);
				int dx = 0;
				for (int x2 = max(x - nSize.x, lowx); x2 <= min(x + nSize.x,
						uppx - 1); x2++, off2++, dx++) {
					outMap[dx][dy][dz] = distmap[off2];
				}
			}
		}
		return outMap;
	}

	@Override
	public boolean execute() {

		final int nCores = neededCores();
		// Identify local maxima regions

		// Run loop to make bubbles
		int clabel = 1;

		Thread.currentThread();
		jStartTime = System.currentTimeMillis();
		final bubbleFiller[] bfArray = new bubbleFiller[neededCores()];

		// Call the other threads

		for (int i = 0; i < nCores; i++) {
			bfArray[i] = new bubbleFiller(this, i);
			bfArray[i].start();
		}
		for (final SeedLabel cBubble : startSeedList.sl) {
			final int toff = (cBubble.cz * dim.y + cBubble.cy) * dim.x
					+ cBubble.cx;
			if (diffmask[toff]) {
				cBubble.label = clabel;

				int mcore = matchCore(bfArray, cBubble);
				if (mcore < 0)
					mcore = lightestCore(bfArray);
				if (mcore < 0) {
					throw new IllegalArgumentException("ERROR - No Filler Threads Available?? ");
				}
				bfArray[mcore].addBubble(cBubble);
				final int cbcnt = bfArray[mcore].pendingBubbles();
				if (cbcnt > 15) {
					try {
						lightestCore(bfArray, true);
						int tsteps = 0;
						while (bfArray[mcore].pendingBubbles() > 5) {
							Thread.sleep(200);
							tsteps++;
							if (tsteps % 50 == 0)
								lightestCore(bfArray, true); // Every 10s say
																// something
						}
						System.out.println((cbcnt - bfArray[mcore]
								.pendingBubbles())
								/ (0.2 * tsteps)
								+ " bubbles per second on " + bfArray[mcore]);
					} catch (final InterruptedException e) {
						System.out
								.println("ERROR - MainTHREAD was interrupted, proceed carefully!");
					}
				}

				/**
				 * while (!isCoreFree(bfArray,curCore)) { curCore++; if
				 * (curCore>=neededCores) { curCore=0; //myThread.sleep(10);
				 * myThread.yield(); } }
				 **/

				clabel++;
			}
		}
		// Wind down
		System.out.println("Winding Down Cores...");
		/** don't wait for new bubbles **/
		for (int i = 0; i < nCores; i++)
			bfArray[i].isHovering = false;
		for (int i = 0; i < nCores; i++) {
			if (bfArray[i] != null) {
				try {
					System.out.println("Joining " + bfArray[i]);
					bfArray[i].join(); // wait until thread has finished
				} catch (final InterruptedException e) {
					System.out.println("ERROR - Thread : " + bfArray[i]
							+ " was interrupted, proceed carefully!");
				}
			}
		}
		final String outString = "BubbleFiller: Ran in "
				+ StrRatio(System.currentTimeMillis() - jStartTime, 1000)
				+ " seconds on " + nCores + " cores";
		System.out.println(outString);
		procLog += outString + "\n";

		procLog += "CMD:DistLabel <FLATNESS:" + FLATCRIT + ",TGROWTH:"
				+ TGROWTH + ">: Max Label:" + clabel + "\n";
		runCount++;
		return true;
	}

	/** Write the labeled bubbles to an image based on the template */
	@Override
	public TImg ExportAim(final TImgRO.CanExport templateAim) {
		if (isInitialized) {
			if (runCount > 0) {
				final TImg outAimData = templateAim.inheritedAim(labels, dim,
						offset);
				outAimData.appendProcLog(procLog);
				return outAimData;
			} else {
				throw new IllegalArgumentException(
						"The plug-in : "
								+ getPluginName()
								+ ", has not yet been run, exported does not exactly make sense, original data will be sent.");
			}
		} else {
			throw new IllegalArgumentException(
					"The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exported does not make any sense");

		}
	}

	/** export the bubble seeds if anyone actually wants them */
	public TImg ExportBubbleseedsAim(final TImgRO.CanExport templateAim) {
		if (isInitialized) {
			final TImg outAimData = templateAim.inheritedAim(diffmask, dim,
					offset);
			outAimData.appendProcLog(procLog);
			return outAimData;

		} else {
			throw new IllegalArgumentException(
					"The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exported does not make any sense");

		}
	}

	protected void fillBubble(final int clabel, int xmax, int ymax, int zmax,
			double cMaxVal) {

		int off;
		int bubbleSize = 0;

		// Fill in the bubble based on the distance map

		// Check for Overlap with Other Bubbles!
		int overlapVox = 0;
		int selfVox = 0;
		boolean changedPos = true;
		final double MINNSCORE = TGROWTH;
		double nDist = cMaxVal;
		int nxi, nyi, nzi;
		int tlowx, tlowy, tlowz, tuppx, tuppy, tuppz;
		// Initial Values
		tlowx = max(xmax - cMaxVal, lowx);
		tlowy = max(ymax - cMaxVal, lowy);
		tlowz = max(zmax - cMaxVal, lowz);
		tuppx = min(xmax + cMaxVal, uppx);
		tuppy = min(ymax + cMaxVal, uppy);
		tuppz = min(zmax + cMaxVal, uppz);
		// Movable Values
		double startX, startY, startZ, startR;
		startX = xmax;
		startY = ymax;
		startZ = zmax;
		startR = cMaxVal;
		// Initialize Variables
		nxi = xmax;
		nyi = ymax;
		nzi = zmax;

		// Find maximum and verify overlap
		while (changedPos) {

			overlapVox = 0;
			selfVox = 0;
			changedPos = false;

			tlowx = max(xmax - cMaxVal, lowx);
			tlowy = max(ymax - cMaxVal, lowy);
			tlowz = max(zmax - cMaxVal, lowz);
			tuppx = min(xmax + cMaxVal, uppx);
			tuppy = min(ymax + cMaxVal, uppy);
			tuppz = min(zmax + cMaxVal, uppz);

			for (int z = tlowz; z < tuppz; z++) {
				final double zd = (zmax - z) * (zmax - z);
				for (int y = tlowy; y < tuppy; y++) {
					final double yd = (ymax - y) * (ymax - y);
					off = (z * dim.y + y) * dim.x + lowx;
					off += (tlowx - lowx);// since loop in next line starts at
											// tlowx instead of lowx
					for (int x = tlowx; x < tuppx; x++, off++) {
						if (Math.sqrt((xmax - x) * (xmax - x) + yd + zd) < cMaxVal) {
							if (labels[off] > 0)
								overlapVox++;
							if (mask[off])
								selfVox++;
							if ((distScalar * distmap[off]) > nDist) {
								nDist = (distScalar * distmap[off]);
								nxi = x;
								nyi = y;
								nzi = z;
								changedPos = true;
							}
						}
					}
				}
			}
			if (changedPos) {
				xmax = nxi;
				ymax = nyi;
				zmax = nzi;
				cMaxVal = nDist;
			}
		}
		if ((overlapVox * 100) / (selfVox + overlapVox + 0.1) > MAXOVERLAP) {
			int silenced = 0;

			for (int cleanVal = 0; cleanVal < 2; cleanVal++) {
				if (cleanVal == 1) {
					xmax = (int) startX;
					ymax = (int) startY;
					zmax = (int) startZ;
					cMaxVal = startR;
				}
				tlowx = max(xmax - cMaxVal, lowx);
				tlowy = max(ymax - cMaxVal, lowy);
				tlowz = max(zmax - cMaxVal, lowz);
				tuppx = min(xmax + cMaxVal, uppx);
				tuppy = min(ymax + cMaxVal, uppy);
				tuppz = min(zmax + cMaxVal, uppz);
				for (int z = tlowz; z < tuppz; z++) {
					final double zd = (zmax - z) * (zmax - z);
					for (int y = tlowy; y < tuppy; y++) {
						final double yd = (ymax - y) * (ymax - y);
						off = (z * dim.y + y) * dim.x + lowx;
						off += (tlowx - lowx);// since loop in next line starts
												// at tlowx instead of lowx
						for (int x = tlowx; x < tuppx; x++, off++) {
							if (mask[off]) {
								if (Math.sqrt((xmax - x) * (xmax - x) + yd + zd) < cMaxVal) {
									diffmask[off] = false;
									silenced++;
								}
							}
						}
					}
				}
			}
			System.out.println("excessive overlap: " + (overlapVox * 100.0)
					/ (selfVox + overlapVox)
					+ "%, removing DiffMask Eligibility, " + silenced
					+ " silenced vx");
		} else {

			tlowx = max(xmax - cMaxVal, lowx);
			tlowy = max(ymax - cMaxVal, lowy);
			tlowz = max(zmax - cMaxVal, lowz);
			tuppx = min(xmax + cMaxVal, uppx);
			tuppy = min(ymax + cMaxVal, uppy);
			tuppz = min(zmax + cMaxVal, uppz);

			for (int z = tlowz; z < tuppz; z++) {
				final double zd = (zmax - z) * (zmax - z);
				for (int y = tlowy; y < tuppy; y++) {
					final double yd = (ymax - y) * (ymax - y);
					off = (z * dim.y + y) * dim.x + lowx;
					off += (tlowx - lowx);// since loop in next line starts at
											// tlowx instead of lowx
					for (int x = tlowx; x < tuppx; x++, off++) {
						if ((distScalar * distmap[off]) > ABSMINWALLDIST) {
							if (Math.sqrt((xmax - x) * (xmax - x) + yd + zd) < cMaxVal) {
								if (mask[off]) {
									mask[off] = false;
									diffmask[off] = false;
									labels[off] = clabel;
									unfilledVox--;
									bubbleSize++;

								} else {
									// Already occupied, empty voxel since it is
									// shared between two (or more) bubbles
									labels[off] = 0;

								}
							}
						}
					}

				}
			}
			final double bubRad = Math
					.pow(bubbleSize / (1.33 * 3.14159), 0.333);

			System.out.println("CurBubble(" + clabel + ") Filled.Rad: "
					+ bubRad + " Voxels: " + bubbleSize + ", Overlap: "
					+ (overlapVox * 100) / (selfVox + overlapVox + 1) + " %");
			if (DISTGROW) {
				int changes = 1;
				int iter = 0;
				while (changes > 0) {

					changes = 0;
					// Fill in bubble with distgrow
					for (int z = tlowz; z < tuppz; z++) {
						// System.out.println+"Slice :: "+z+endl;
						for (int y = tlowy; y < tuppy; y++) {
							off = (z * dim.y + y) * dim.x + lowx;
							off += (tlowx - lowx);// since loop in next line
													// starts at tlowx instead
													// of lowx
							for (int x = tlowx; x < tuppx; x++, off++) {
								// [off] is the voxel to be filled
								// [off2] is the current voxel (filled, the one
								// doing the expanding)

								boolean distMatch = false;
								if (mask[off]) {// Eligble empty voxel
									for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(
											z + neighborSize.z, uppz - 1); z2++) {
										for (int y2 = max(y - neighborSize.y,
												lowy); y2 <= min(y
												+ neighborSize.y, uppy - 1); y2++) {
											int off2 = (z2 * dim.y + y2)
													* dim.x
													+ max(x - neighborSize.x,
															lowx);
											for (int x2 = max(x
													- neighborSize.x, lowx); x2 <= min(
													x + neighborSize.x,
													uppx - 1); x2++, off2++) {
												if ((off != off2)
														&& (!distMatch)) {
													totalVoxels++;
													if ((labels[off2] == clabel)) { // valid
																					// seed
																					// voxel

														// Now check distance
														// map
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
																				2));
														final double slope = (distScalar
																* distmap[off] - distScalar
																* distmap[off2])
																/ cDist;
														// System.out.println+"Current Slope:"+slope+endl;
														if ((distmap[off] == cMaxVal)) {
															distMatch = true;
															// System.out.println+"Flooding..."+endl;
														} else {
															switch (marchMode) {
															case 0: // Expands
																	// against
																	// distance
																	// gradient
																distMatch = slope < MINNSCORE;
																break;
															case 1: // Expands
																	// against
																	// distance
																	// gradient
																distMatch = slope <= MINNSCORE;
																break;
															case 2: // Expands
																	// with
																	// distance
																	// gradient
																distMatch = slope > MINNSCORE;
																break;
															case 3: // Expands
																	// with
																	// distance
																	// gradient
																distMatch = slope >= MINNSCORE;
																break;
															}
														}
														if (distMatch)
															break; // break only
																	// breaks
																	// out of
																	// one loop
																	// so added
																	// (!distMatch
																	// line
																	// above)
													}

												}

											}
										}
									}

									if (distMatch) {
										// System.out.println+"Slope Match: "+slope*dst[ik]+endl;
										labels[off] = clabel;
										unfilledVox--;
										mask[off] = false;
										tlowx = min(tlowx, max(x - 1, lowx));
										tlowy = min(tlowy, max(y - 1, lowy));
										tlowz = min(tlowz, max(z - 1, lowz));
										tuppx = max(tuppx, min(x + 1, uppx));
										tuppy = max(tuppy, min(y + 1, uppy));
										tuppz = max(tuppz, min(z + 1, uppz));
										changes++;
										bubbleSize++;
									}

								}

							}
						}
					}
					iter++;

				}
				System.out.println("Iters:"
						+ iter
						+ " : "
						+ bubbleSize
						+ " unfilled voxels (%) "
						+ String.format("%.2f", (unfilledVox * 100.0)
								/ (aimLength)));
			}
			remVoxels -= bubbleSize;
		}
	}

	@Override
	public String getPluginName() {
		return "DistLabel";
	}

	/** create the needed variables for the function **/
	private void Init(final D3int idim, final D3int ioffset) {
		if (distmap.length != mask.length) {
			System.out.println("SIZES DO NOT MATCH!!!!!!!!");
			return;
		}

		labels = new int[aimLength];
		diffmask = new boolean[aimLength];

		InitDims(idim, ioffset);

		InitDiffmask();

	}

	public void InitDiffmask() {

		Thread.currentThread();
		final ArrayList<dlRunner> threadList = new ArrayList<dlRunner>();
		startSeedList = new SeedList(MAXOVERLAP);

		jStartTime = System.currentTimeMillis();
		// Call the other threads
		for (int i = 0; i < neededCores(); i++) { // setup the background
													// threads
			final int[] mySlices = divideSlices(i);
			final dlRunner bgThread = new dlRunner(this, mySlices[0],
					mySlices[1]);
			threadList.add(bgThread);
			bgThread.start();
		}

		for (final dlRunner theThread : threadList) { // for all other threads:
			try {
				theThread.join(); // wait until thread has finished
			} catch (final InterruptedException e) {
				System.out.println("ERROR - Thread : " + theThread
						+ " was interrupted, proceed carefully!");
			}
			startSeedList.killedLabels += theThread.threadSeedList.killedLabels;
			for (final SeedLabel cBubble : theThread.threadSeedList.sl)
				if (cBubble.isValid())
					startSeedList.addbubble(cBubble.cx, cBubble.cy, cBubble.cz,
							cBubble.rad);

		}
		startSeedList.finalize();

		final String outString = "BubbleSeeds: Ran in "
				+ StrRatio(System.currentTimeMillis() - jStartTime, 1000)
				+ " seconds on " + neededCores() + " cores and found:"
				+ startSeedList.toString() + " FlatCrit:" + FLATCRIT;
		System.out.println(outString);
		procLog += outString + "\n";
	}

	/** depreciated scheme for old threading **/
	@Deprecated
	protected boolean isCoreFree(final bubbleFiller[] coreArray,
			final int coreIndex) {
		if (coreArray[coreIndex] != null) {
			return (coreArray[coreIndex].isFinished);
		} else
			return true;
	}

	@Deprecated
	protected int lightestCore(final bubbleFiller[] coreArray) {
		return lightestCore(coreArray, false);
	}

	@Deprecated
	protected int lightestCore(final bubbleFiller[] coreArray,
			final boolean printStatus) {
		int lcore = -1;
		int ljobs = 0;
		double lcSum = 0;
		int lcCount = 0;
		final String startString = "<Core,CurrentLoad,TotalLoad>:";
		String statusString = "";
		for (int i = 0; i < coreArray.length; i++) {
			if (coreArray[i] != null) {
				final int pbubs = coreArray[i].pendingBubbles();
				if (lcore < 0) {
					lcore = i;
					ljobs = pbubs;
				}
				if (pbubs < ljobs) {
					lcore = i;
					ljobs = pbubs;
				}
				lcSum += ljobs;
				lcCount++;
				if (printStatus)
					statusString += "<" + i + "," + pbubs + ","
							+ coreArray[i].completedBubbles + ">,";
			}
		}
		lcAvg = lcSum / lcCount;
		if (printStatus)
			System.out.println(startString + "AVG-" + ((int) Math.round(lcAvg))
					+ ":" + statusString);
		return lcore;
	}

	/**
	 * LoadAimData provides a single function which all of the other function
	 * funnel into, it handles copying the data and setting up the needed
	 * variables
	 * **/

	protected void LoadAimData(final TImgRO labelImg) {
		final int[] inputmap = TImgTools.makeTImgFullReadable(labelImg)
				.getIntAim();
		aimLength = inputmap.length;
		if (BaseTIPLPluginIn.doPreserveInput) {
			distmap = new int[aimLength];
			System.arraycopy(inputmap, 0, distmap, 0, aimLength);
		} else {
			distmap = inputmap;
		}
		for (int i = 0; i < aimLength; i++)
			mask[i] = distmap[i] > 0;
		Init(labelImg.getDim(), labelImg.getOffset());
	}

	protected void LoadAimData(final TImgRO labelImg, final TImgRO maskImg) {
		final int[] inputmap = TImgTools.makeTImgFullReadable(labelImg)
				.getIntAim();
		final boolean[] inputmask = TImgTools.makeTImgFullReadable(maskImg)
				.getBoolAim();
		aimLength = inputmap.length;
		if (BaseTIPLPluginIn.doPreserveInput) {
			distmap = new int[aimLength];
			System.arraycopy(inputmap, 0, distmap, 0, aimLength);
			mask = new boolean[aimLength];
			System.arraycopy(inputmask, 0, mask, 0, aimLength);
		} else {
			distmap = inputmap;
			mask = inputmask;
		}

		Init(labelImg.getDim(), labelImg.getOffset());
	}

	/**
	 * LoadImages assumes the first image is the label image and the second is
	 * the mask image (if present)
	 */
	@Override
	public void LoadImages(final TImgRO[] inImages) {
		// TODO Auto-generated method stub
		if (inImages.length < 1)
			throw new IllegalArgumentException(
					"Too few arguments for LoadImages in:" + getPluginName());
		final TImgRO labelImg = inImages[0];
		if (inImages.length < 2)
			LoadAimData(labelImg);
		final TImgRO maskImg = inImages[1];
		LoadAimData(labelImg, maskImg);
	}

	private SeedList locateSeeds(final int startSlice, final int finalSlice) {
		final SeedList tempSeedList = new SeedList(MAXOVERLAP);
		final D3int iNeighborSize = new D3int(2);
		int off = 0;
		double avgGrad = 0;
		double avgSGrad = 0;
		double avgLap = 0;
		double avgSLap = 0;
		int inVox = 0;
		unfilledVox = 0;

		for (int z = startSlice; z < finalSlice; z++) {
			for (int y = lowy + OUTERSHELL; y < (uppy - OUTERSHELL); y++) {
				off = (z * dim.y + y) * dim.x + lowx + OUTERSHELL;

				for (int x = lowx + OUTERSHELL; x < (uppx - OUTERSHELL); x++, off++) {
					// The code is optimized so the least number of voxels make
					// it past the first check
					final float cVDist = (float) distScalar * distmap[off];

					if ((mask[off]) && ((cVDist) > MINWALLDIST)) {
						unfilledVox++;
						double gradX = 0.0, gradY = 0.0, gradZ = 0.0;
						double lapVal = 0.0;
						int lapCount = 0;
						int gradCount = 0;
						final double[][] hessianMatrix = new double[3][3];
						double minCriteria = 0; // variable for the minimum
												// criteria (either laplacian or
						if (useLaplacian) {
							for (int z2 = max(z - iNeighborSize.z, lowz); z2 <= min(
									z + iNeighborSize.z, uppz - 1); z2++) {
								for (int y2 = max(y - iNeighborSize.y, lowy); y2 <= min(
										y + iNeighborSize.y, uppy - 1); y2++) {
									int off2 = (z2 * dim.y + y2) * dim.x
											+ max(x - iNeighborSize.x, lowx);
									for (int x2 = max(x - iNeighborSize.x, lowx); x2 <= min(
											x + iNeighborSize.x, uppx - 1); x2++, off2++) {
										if (off != off2) {
											if ((Math.abs(x2 - x) <= 1)
													&& (Math.abs(x2 - x) <= 1)
													&& (Math.abs(x2 - x) <= 1)) { // Local
																					// gradient
												gradX += (x2 - x)
														* (distmap[off2]);
												gradY += (y2 - y)
														* (distmap[off2]);
												gradZ += (z2 - z)
														* (distmap[off2]);
												gradCount++;
											}

											if (((x2 != x) ? 1 : 0)
													+ ((y2 != y) ? 1 : 0)
													+ ((z2 != z) ? 1 : 0) <= 1) { // slightly
																					// larger
																					// laplacian
												lapVal += (distScalar * (distmap[off2]));
												lapCount++;
											}

											// First derivative is 0 and second
											// derivative is less than 0 (local
											// maxima)

										}
									}
								}
							}
							lapVal += -lapCount * (distScalar * (distmap[off]));
							lapVal /= lapCount;
							minCriteria = lapVal;
							gradX /= gradCount;
							gradY /= gradCount;
							gradZ /= gradCount;
						} else {
							final double curVal = 2 * distmap[off];

							final int[][][] localValues = dmapGet(x, y, z,
									new D3int(1));

							// xx
							hessianMatrix[0][0] = localValues[2][1][1] - curVal
									+ localValues[0][1][1]; // dmapGet(x+1,y,z)-curVal+dmapGet(x-1,y,z);

							// yy
							hessianMatrix[1][1] = localValues[1][2][1] - curVal
									+ localValues[1][0][1]; // dmapGet(x,y+1,z)-curVal+dmapGet(x,y-1,z);
															// //img.get(x, y +
															// 1, z) - temp +
															// img.get(x, y - 1,
															// z);

							// zz
							hessianMatrix[2][2] = localValues[1][1][2] - curVal
									+ localValues[1][1][0]; // dmapGet(x, y, z +
															// 1) - curVal
															// +dmapGet(x, y, z
															// - 1);

							// xy
							hessianMatrix[0][1] = hessianMatrix[1][0] = ((localValues[2][2][1] - localValues[0][2][1]) / 2 - (localValues[2][0][1] - localValues[0][0][1]) / 2) / 2;

							// xz
							hessianMatrix[0][2] = hessianMatrix[2][0] = ((localValues[2][1][2] - localValues[0][1][2]) / 2 - (localValues[2][1][0] - localValues[0][1][0]) / 2) / 2;

							// yz
							hessianMatrix[1][2] = hessianMatrix[2][1] = ((localValues[1][2][2] - localValues[1][0][2]) / 2 - (localValues[1][2][0] - localValues[1][0][0]) / 2) / 2;

							final Matrix M = new Matrix(hessianMatrix);
							final EigenvalueDecomposition E = new EigenvalueDecomposition(
									M);

							final double[] result = E.getRealEigenvalues();
							for (int iz = -1; iz <= 1; iz++) {
								for (int iy = -1; iy <= 1; iy++) {
									for (int ix = -1; ix <= 1; ix++) {
										gradX += ix
												* localValues[ix + 1][iy + 1][iz + 1];
										gradY += iy
												* localValues[ix + 1][iy + 1][iz + 1];
										gradZ += iz
												* localValues[ix + 1][iy + 1][iz + 1];
										gradCount++;
									}
								}
							}
							if ((result[0] <= 0) && (result[1] <= 0)
									&& (result[2] <= 0))
								minCriteria = -5; // Negative Definite
							else
								lapVal = 1;
						}

						gradX *= distScalar;
						gradY *= distScalar;
						gradZ *= distScalar;

						final double cGrad = distScalar
								* Math.sqrt(gradX * gradX + gradY * gradY
										+ gradZ * gradZ);

						avgGrad += cGrad;

						avgSGrad += cGrad * cGrad;
						avgLap += lapVal;
						avgSLap += lapVal * lapVal;
						inVox++;
						// Write the procedure log to a text file

						// outLog.write(cGrad+","+minCriteria+"\n");
						if ((cGrad <= FLATCRIT) && (minCriteria < -0.1)) {
							// System.out.println("GradVal:"+cGrad+"->("+gradX+", "+gradY+", "+gradZ+"), "+gradCount+", Lap:"+lapVal+", LC"+lapCount);
							diffmask[off] = true;
							tempSeedList.addbubble(x, y, z, cVDist);
						}
					} // End mask and dist-check
				} // End x
			} // End y
		} // End z

		avgGrad /= inVox;
		avgLap /= inVox;
		System.out.println("Average GradVal:" + avgGrad + " - STD:"
				+ Math.sqrt(avgSGrad / inVox - avgGrad * avgGrad) + ", Lap:"
				+ avgLap + " - STD:"
				+ Math.sqrt(avgSLap / inVox - avgLap * avgLap));
		// procLog+="CMD:LocalMaxima: GradVal:"+avgGrad+" - STD:"+Math.sqrt(avgSGrad/inVox-avgGrad*avgGrad)+", Lap:"+avgLap+" - STD:"+Math.sqrt(avgSLap/inVox-avgLap*avgLap)+"\n";
		return tempSeedList;

	}

	@Deprecated
	protected int matchCore(final bubbleFiller[] coreArray,
			final SeedLabel nBubble) {
		// lightestCore(coreArray);
		for (int i = 0; i < coreArray.length; i++) {
			if (coreArray[i] != null)
				if (coreArray[i].doesOverlap(nBubble))
					return i;
		}
		return -1;
	}

	@Override
	public int wantedCores() {
		return dim.z / 2;
	} // no fewer than 2 slices per core

}
