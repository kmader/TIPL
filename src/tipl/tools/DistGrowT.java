package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;
import tipl.util.D3float;
import tipl.util.ITIPLPlugin;
import tipl.util.TIPLGlobal;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;

/**
 * DistGrowT is like distgrow but instead of recursive filling it uses distance
 * based filling with rings
 */
public class DistGrowT extends DistGrow {
	@TIPLPluginManager.PluginInfo(pluginType = "DistGrow",
			desc="Full memory distgrowing with ring based distance filling",
			sliceBased=false,
			speedRank=1)
	final public static TIPLPluginManager.TIPLPluginFactory myFactory = new TIPLPluginManager.TIPLPluginFactory() {
		@Override
		public ITIPLPlugin get() {
			return new DistGrow();
		}
	};
	/** Command line accessible program interface */
	public static void main(final String[] args) {
		final String kVer = "120215_001";
		System.out.println(" DistGrow Script v" + kVer);
		System.out.println(" Gradient Guided Watershed and  v" + kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final ArgumentParser p = TIPLGlobal.activeParser(args);

		p.hasOption("debug");

		p.getOptionFloat("mindistthresh", 0.1f,
				"Minimum difference between two distance map values in order to roll down");

		final String distmapAimFile = p.getOptionString("distmap", "",
				"Name of the distance map");
		final String inlabelsAimFile = p.getOptionString("inlabels", "",
				"Name of the starting labels");
		final String outlabelsAimFile = p.getOptionString("outlabels",
				"dg_labels.tif", "Name of the output labels");
		final String maskAimFile = p.getOptionString("mask", "",
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
		DistGrowT DG1;
		if (maskAimFile.length() > 0) {
			final TImg maskAim = TImgTools.ReadTImg(maskAimFile);
			DG1 = new DistGrowT(distmapAim, inlabelsAim, maskAim);
		} else
			DG1 = new DistGrowT(distmapAim, inlabelsAim);

		DG1.runDG();
		final TImg outlabelsAim = DG1.ExportImages(inlabelsAim)[0]; // Saves a bit of
																// memory
		TImgTools.WriteTImg(outlabelsAim,outlabelsAimFile);
	}

	protected double MAXDIST;

	/**
	 * Constructor based on the distance map and mask aim images
	 * 
	 * @param imap
	 *            Distance map image
	 * @param ilabels
	 *            Starting labels
	 */
	public DistGrowT(final TImgRO imap, final TImgRO ilabels) {
		super(imap, ilabels);
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
	public DistGrowT(final TImgRO imap, final TImgRO ilabels, final TImgRO imask) {
		super(imap, ilabels, imask);
	}

	/** the nonrecurvsive version of this function */
	@Override
	protected D3float checkNeighborhood(final int x, final int y, final int z,
			final int off) {

		chkNbrCalls++;
		final double sDist = distScalar * distmap[off];
		final D3float bestRoll = new D3float(labels[off], sDist, 0); // label,
																		// distance,
																		// void

		for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(z
				+ neighborSize.z, uppz - 1); z2++) {
			for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(y
					+ neighborSize.y, uppy - 1); y2++) {
				int off2 = (z2 * dim.y + y2) * dim.x
						+ max(x - neighborSize.x, lowx);
				for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(x
						+ neighborSize.x, uppx - 1); x2++, off2++) {
					if (off != off2) {
						if ((mask[off2]) && (labels[off2] > 0)) {
							final double cDist = distScalar * distmap[off2];
							if ((cDist - sDist) > minDistThresh) {
								bestRoll.x = labels[off2];
								bestRoll.y = cDist;
							}
						}
					}
				}
			}
		}

		// Flood only if it is better for this voxel than it was before
		if (((int) bestRoll.x) > 0) {
			setLabel(off, (int) bestRoll.x);
			return bestRoll;
		} else {
			return new D3float(0, 0, 0);
		}

	}

	@Override
	public String getPluginName() {
		return "DistGrowT";
	}

	@Override
	protected boolean labNeighbors(final int x, final int y, final int z,
			final int off) {
		return (distmap[off] <= iterations);
	}

	@Override
	public void runDG() {

		curMaxDepth = MAXDEPTH;
		scanDG();
		lastIter = false;
		for (double cDist = 0; cDist < MAXDIST; cDist += 1) {
			unfilledVoxels = 0;
			filledVoxels = 0;
			iterations = (int) (cDist / distScalar);
			chkNbrCalls = 0;
			runMode = 1;
			execute();
			runMode = 0;
		}
	}

	public void scanDG() {
		int tMaxDist = 0;
		for (int z = lowz; z < uppz; z++) {
			for (int y = lowy; y < uppy; y++) {
				int off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
					// The code is optimized so the least number of voxels make
					// it past the first check
					if ((mask[off]) && (distmap[off] > tMaxDist))
						tMaxDist = distmap[off]; // End mask and dist-check
				} // End x
			} // End y
		} // End z
			// Maximum label
		MAXDIST = distScalar * tMaxDist;
	}
}
