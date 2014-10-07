package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.*;

/**
 * DistGrow is the class used for labeling bubbles based on a distance map and a
 * mask
 */
public class DistGrow extends BaseTIPLPluginIO {
	@TIPLPluginManager.PluginInfo(pluginType = "DistGrow",
			desc="Full memory distance growing",
			sliceBased=false)
    final public static class dgFactory implements TIPLPluginManager.TIPLPluginFactory {
		@Override
		public ITIPLPlugin get() {
			return new DistGrow();
		}
	};
	/** Command line accessible program interface */
	public static void main(final String[] args) {
		final String kVer = "130913_002";
		System.out.println(" DistGrow Script v" + kVer);
		System.out.println(" Gradient Guided Watershed and  v" + kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final ArgumentParser p = TIPLGlobal.activeParser(args);

		p.hasOption("debug");

		p.getOptionFloat("mindistthresh", 0.1f,
				"Minimum difference between two distance map values in order to roll down");

		final TypedPath distmapAimFile = p.getOptionPath("distmap", "",
				"Name of the distance map");
		final TypedPath inlabelsAimFile = p.getOptionPath("inlabels", "",
				"Name of the starting labels");
		final TypedPath outlabelsAimFile = p.getOptionPath("outlabels",
				"dg_labels.tif", "Name of the output labels");
		final TypedPath maskAimFile = p.getOptionPath("mask", "",
				"Name of the mask output file");

		if (p.hasOption("?") || (distmapAimFile.length() < 1)) {
			System.out.println(" Component Label Demo Help");
			System.out
					.println(" Analyzes thresheld images and labels components");
			System.out.println(" Arguments::");
			System.out.println("  ");
			System.out.println(p.getHelp());
			System.exit(0);
		}

		System.currentTimeMillis();

		System.out.println("Loading " + distmapAimFile);
		final TImg distmapAim = TImgTools.ReadTImg(distmapAimFile);
		final TImg inlabelsAim = TImgTools.ReadTImg(inlabelsAimFile);
		System.out.println("Loaded (" + distmapAim + "," + inlabelsAim
				+ ") ...");
		System.currentTimeMillis();
		DistGrow DG1;
		if (maskAimFile.length() > 0) {
			final TImg maskAim = TImgTools.ReadTImg(maskAimFile);
			DG1 = new DistGrow(distmapAim, inlabelsAim, maskAim);
		} else
			DG1 = new DistGrow(distmapAim, inlabelsAim);

		DG1.runDG();
		final TImg outlabels = DG1.ExportImages(inlabelsAim)[0]; // Saves a bit of
															// memory
		TImgTools.WriteTImg(outlabels,outlabelsAimFile);
	}

	protected int[] distmap; // Won't be edited
	protected int[] in_labels; // Won't be edited
	protected int[] labels; // Output
	protected boolean[] mask; // Mask

	protected volatile int chkNbrCalls = 0;
	protected volatile int chkNbrIter = 0;

	/**
	 * The minimum difference in the distance map in order to roll down / join
	 * watershed
	 */
	public double minDistThresh = 0.1;

	protected final int MAXDIST = 4000;
	protected final int MAXDISTVAL = 32765;
	// protected final int MAXLABEL = 65543;
	// protected final int OUTERSHELL = 2;
	// protected final double MINWALLDIST = 4;
	// protected final double FLATCRIT=0.401;
	// protected final int MAXOVERLAP=40;
	// protected final double ABSMINWALLDIST = 2;
	protected final static int MAXDEPTH = 30;
	protected int curMaxDepth = MAXDEPTH;

	/** The T_Growth parameter **/
	// protected double MINNSCORE = -0.9;
	// protected int marchMode = 0;
	/** Scalar used for scaling the distance map into voxel space */
	public double distScalar = (MAXDIST + 0.0) / (MAXDISTVAL + 0.0);
	protected int iterations;

	protected boolean lastIter;
	protected int runMode = 0;

	volatile double unfilledVoxels = 0;

	volatile double filledVoxels = 0;
	protected DistGrow() {
		
	}
	@Deprecated
	public DistGrow(final short[] inputmap, final D3int idim,
			final D3int ioffset) {
		aimLength = inputmap.length;
		distmap = new int[aimLength];
		mask = new boolean[aimLength];
		for (int i = 0; i < aimLength; i++) {
			distmap[i] = inputmap[i];
			mask[i] = distmap[i] > 0;
		}
		Init(idim, ioffset);
	}

	/**
	 * Constructor based on the distance map and mask aim images
	 * 
	 * @param imap
	 *            Distance map image
	 * @param ilabels
	 *            Starting labels
	 */
	@Deprecated
	public DistGrow(final TImgRO imap, final TImgRO ilabels) {
		LoadAimData(imap, ilabels);
	}

	/**
	 * Constructor based on the distance map and mask aim images
	 * 
	 * @param imap
	 *            Distance map image
	 * @param ilabels
	 *            Starting labels
	 * @param imask
	 *            Mask to be filld and identified
	 */
	@Deprecated
	public DistGrow(final TImgRO imap, final TImgRO ilabels, final TImgRO imask) {
		LoadAimData(imap, ilabels, imask);
	}

	protected D3float checkNeighborhood(final int x, final int y, final int z,
			final int off) {
		return checkNeighborhood(x, y, z, off, distScalar * distmap[off], 0);
	}

	protected D3float checkNeighborhood(final int x, final int y, final int z,
			final int off, final double inDist, final int depth) {
		if (depth > chkNbrIter)
			chkNbrIter = depth;

		chkNbrCalls++;

		final D3float startRoll = new D3float(labels[off], distScalar
				* distmap[off], 0); // The starting circumstances
		D3float bestRoll = new D3float(labels[off], inDist, 0); // label,
																// distance,
																// void
		if (depth > curMaxDepth)
			return startRoll;

		for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(z
				+ neighborSize.z, uppz - 1); z2++) {
			for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(y
					+ neighborSize.y, uppy - 1); y2++) {
				int off2 = (z2 * dim.y + y2) * dim.x
						+ max(x - neighborSize.x, lowx);
				for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(x
						+ neighborSize.x, uppx - 1); x2++, off2++) {
					if (off != off2) {
						if (mask[off2]) {
							if (((distScalar * distmap[off2]) - bestRoll.y) > minDistThresh) {
								bestRoll.x = labels[off2];
								bestRoll.y = distScalar * distmap[off2];
								if (((int) bestRoll.x) == 0)
									bestRoll = checkNeighborhood(x2, y2, z2,
											off2, inDist, depth + 1); // if the
																		// current
																		// label
																		// is
																		// empty
																		// search
																		// more
							}
						}
					}
				}
			}
		}
		// Flood only if it is better for this voxel than it was before
		if (((int) bestRoll.x) > 0) {
			if ((bestRoll.y - startRoll.y) > minDistThresh)
				setLabel(off, (int) bestRoll.x);
			return bestRoll;
		} else {
			return new D3float(0, 0, 0);
		}

	}

	@Override
	public boolean execute() {
		switch (runMode) {
		case 0:
			System.err
					.println("Function cannot / shouldnot be called in this manner!");
			return false;
		case 1:
			return runMulticore();

		}
		return false;

	}

	/** Write the labeled bubbles to an image based on the template */
	@Override
	public TImg[] ExportImages(final TImgRO inImage) {
		final TImg.CanExport templateAim = TImgTools.WrapTImgRO(inImage);
		if (isInitialized) {
			if (runCount > 0) {
				final TImg outAimData = templateAim.inheritedAim(labels, dim,
						offset);
				outAimData.appendProcLog(procLog);
				return new TImg[] {outAimData};
			} else {
				System.err
						.println("The plug-in : "
								+ getPluginName()
								+ ", has not yet been run, exported does not exactly make sense, original data will be sent.");
				return new TImg[] {templateAim.inheritedAim(labels, dim, offset)};
			}
		} else {
			throw new IllegalArgumentException(
					"The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exported does not make any sense");

		}
	}

	@Override
	public String getPluginName() {
		return "DistGrow";
	}

	protected void Init(final D3int idim, final D3int ioffset) {
		if (distmap.length != mask.length) {
			System.out.println("SIZES DO NOT MATCH!!!!!!!!");
			return;
		}

		labels = new int[aimLength];
		System.arraycopy(in_labels, 0, labels, 0, aimLength);

		InitDims(idim, ioffset);

	}

	protected boolean labNeighbors(final int x, final int y, final int z,
			final int off) {
		for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(z
				+ neighborSize.z, uppz - 1); z2++) {
			for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(y
					+ neighborSize.y, uppy - 1); y2++) {
				int off2 = (z2 * dim.y + y2) * dim.x
						+ max(x - neighborSize.x, lowx);
				for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(x
						+ neighborSize.x, uppx - 1); x2++, off2++) {
					if (off != off2) {
						if (mask[off]) {
							if (labels[off2] > 0)
								return true;
						}
					} // end off!=off2 check
				} // end subx
			}// end suby
		} // end subz
		return false;
	}

	/** The real constructor functions */

	protected void LoadAimData(final TImgRO distImg, final TImgRO labImg) {
		final int[] inputmap = TImgTools.makeTImgFullReadable(distImg)
				.getIntAim();
		final int[] clabels = TImgTools.makeTImgFullReadable(labImg)
				.getIntAim();
		aimLength = inputmap.length;
		distmap = inputmap;
		in_labels = clabels;
		for (int i = 0; i < aimLength; i++)
			mask[i] = distmap[i] > 0;
		Init(distImg.getDim(), distImg.getOffset());
	}

	protected void LoadAimData(final TImgRO distImg, final TImgRO labImg,
			final TImgRO maskImg) {
		final int[] inputmap = TImgTools.makeTImgFullReadable(distImg)
				.getIntAim();
		final int[] clabels = TImgTools.makeTImgFullReadable(labImg)
				.getIntAim();
		final boolean[] inputmask = TImgTools.makeTImgFullReadable(maskImg)
				.getBoolAim();
		aimLength = inputmap.length;
		distmap = inputmap;
		mask = inputmask;
		in_labels = clabels;
		Init(distImg.getDim(), distImg.getOffset());
	}

	/**
	 * LoadImages assumes the first image is the distance map image the second
	 * is the label image the third is the mask image (if present)
	 */
	@Override
	public void LoadImages(final TImgRO[] inImages) {
		// TODO Auto-generated method stub
		if (inImages.length < 2)
			throw new IllegalArgumentException(
					"Too few arguments for LoadImages in:" + getPluginName());
		final TImgRO distImg = inImages[0];
		final TImgRO labelImg = inImages[1];
		if (inImages.length < 3)
			LoadAimData(distImg, labelImg);
		final TImgRO maskImg = inImages[2];
		LoadAimData(distImg, labelImg, maskImg);
	}

	@Override
	protected void processWork(final Object currentWork) {
		final int[] range = (int[]) currentWork;
		final int bSlice = range[0];
		final int tSlice = range[1];
		System.out.println("DistGrow<" + bSlice + "," + tSlice + ">");
		runSection(bSlice, tSlice);

		if (bSlice == lowz) { // if it is the first slice
			final String curStatus = "CMD:DistGrow #" + iterations
					+ ": Oper/Vx:" + StrRatio(chkNbrCalls, unfilledVoxels)
					+ ", Filled Ratio:"
					+ StrPctRatio(filledVoxels, unfilledVoxels)
					+ ", MaxRecDepth:" + chkNbrIter + ", VxVis:"
					+ StrMvx(unfilledVoxels);
			System.out.println(curStatus);
			procLog += curStatus + "\n";
			runCount++;
		}
	}


	public void runDG() {

		filledVoxels = 1;
		iterations = 0;
		lastIter = false;
		while (filledVoxels > 0) {
			unfilledVoxels = 0;
			filledVoxels = 0;
			chkNbrCalls = 0;
			if (lastIter)
				curMaxDepth = MAXDEPTH;
			else
				curMaxDepth = max(max(neighborSize.x, neighborSize.y),
						neighborSize.z);
			runMode = 1;
			execute();
			runMode = 0;
			if ((filledVoxels < 1)) {
				if (!lastIter) {
					lastIter = true;
					System.out.println("Last iteration..." + iterations);
					filledVoxels = 1;
				} else
					System.out.println("Finished...");
			}
			iterations++;
		}
	}

	public void runSection(final int bSlice, final int tSlice) {
		// Run loop to roll all voxels down
		int off = 0;

		for (int z = bSlice; z < tSlice; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
					// The code is optimized so the least number of voxels make
					// it past the first check
					if ((mask[off]) && (labels[off] == 0)) {
						boolean isGood = true;
						if (!lastIter)
							isGood = labNeighbors(x, y, z, off);
						if (isGood) {
							unfilledVoxels++;
							checkNeighborhood(x, y, z, off);
						}
					} // End mask and dist-check
				} // End x
			} // End y
		} // End z
	}

	protected synchronized void setLabel(final int off, final int val) {
		labels[off] = val;
		filledVoxels++;
	}
}
