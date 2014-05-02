package tipl.tools;

import java.util.Random;

import tipl.formats.TImg;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.ITIPLPlugin;
import tipl.util.ITIPLPluginIO;
import tipl.util.TIPLGlobal;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;

/**
 * Calculate thickness using monte carlo methods by randomly throwing spheres
 * and filling them up
 * 
 * @author mader
 * 
 */
public class MCThickness extends Thickness {
	@TIPLPluginManager.PluginInfo(pluginType = "MCThickness",
			desc="Full memory monte carlo thickness calculation tool",
			sliceBased=false)
	final public static TIPLPluginManager.TIPLPluginFactory myFactory = new TIPLPluginManager.TIPLPluginFactory() {
		@Override
		public ITIPLPlugin get() {
			return new MCThickness();
		}
	};
	/**
	 * Similar to DTObjection function in XIPL, takes a black and white input
	 * image and calculates the thickness map of the object (white) of this
	 * image
	 * 
	 * @param inAimFile
	 *            the binary aim file to use for the thickness map
	 * @param outAimFile
	 *            the name of the output thickness map file
	 * @param histoFile
	 *            the name of the csv histogram file to write
	 */
	public static boolean DTO(final String inAimFile, final String outAimFile,
			final String histoFile) {
		final TImg thickmapAim = DTO(TImgTools.ReadTImg(inAimFile));
		TImgTools.WriteTImg(thickmapAim,outAimFile);
		GrayAnalysis.StartHistogram(thickmapAim, histoFile + ".csv");
		return true;
	}

	/**
	 * Similar to DTObjection function in XIPL, takes a black and white input
	 * image and calculates the thickness map of the object (white) of this
	 * image
	 * 
	 * @param bwObject
	 *            The binary input image
	 */
	public static TImg DTO(final TImg bwObject) {
		ITIPLPluginIO KV = TIPLPluginManager.createBestPluginIO("kVoronoi",new TImg[] {null,bwObject});
		KV.setParameter("-includeEdges=false");
		KV.LoadImages(new TImg[] {null,bwObject});
		KV.execute();
		final TImg distmapAim = KV.ExportImages(bwObject)[1];
		KV = null;
		final Thickness KT = new MCThickness();
		KT.LoadImages(new TImg[] {distmapAim});
		KT.execute();
		return KT.ExportAim(distmapAim);
	}

	public static void main(final String[] args) {
		final String kVer = "120322_006";
		System.out.println("Thickness v" + kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		ArgumentParser p =TIPLGlobal.activeParser(args);
		final ITIPLPluginIO myThick = new MCThickness();
		final String inputFile = p.getOptionString("input", "",
				"Input distance map image");
		p = myThick.setParameter(p, "");
		final String outputFile = p.getOptionString("output", "thickmap.tif",
				"Output thickness image");
		if (p.hasOption("?")) {
			System.out.println("Thickness v" + kVer);
			System.out
					.println(" Calculates thickness from distance map image v"
							+ kVer);
			System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
			System.out.println(" Arguments::");
			System.out.println(" ");
			System.out.println(p.getHelp());
			System.exit(0);
		}

		if (inputFile.length() > 0) { // Read in labels
			System.out.println("Loading " + inputFile + " ...");
			final TImg inputAim = TImgTools.ReadTImg(inputFile);
			myThick.LoadImages(new TImg[] { inputAim });
			System.out.println("Calculating Thickness " + inputFile + " ...");
			myThick.execute();
			final TImg outputAim = myThick.ExportImages(inputAim)[0];
			TImgTools.WriteTImg(outputAim,outputFile, 1, (float) Thickness.distScalar,false,false);

		}

	}

	double neighborOccupancy = 1.0; // delete if less than 100% of neighbors are
									// occupied

	final int OUTERSHELL = 0;
	public float MINTHICKNESS = 1;
	public int spheresMade;

	public int fillSpheres = 25000;
	public int spheresFilled;
	public boolean supportsThreading = true;

	/** search for the thickest remaining point in the region */
	public int bestFrequency = 20;

	// Multithreading functions

	public MCThickness() {

	}

	public MCThickness(final TImg distmapAim) {
		LoadImages(new TImg[] { distmapAim });
	}

	/**
	 * Object to divide the thread work into supportCores equal parts, default
	 * is z-slices, customized to include outershell and more slices per core
	 */
	@Override
	public Object divideThreadWork(final int cThread, final int maxCores) {
		final int minSlice = lowz + OUTERSHELL;
		final int maxSlice = uppz - OUTERSHELL;
		int myNeededCores = maxCores;

		if (3 * maxCores > (maxSlice - minSlice))
			myNeededCores = (maxSlice - minSlice) / 3; // At least 3 slices per
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

	@Override
	public boolean execute() {
		if (runMulticore()) {
			procLog += "CMD:Distance2Thickness : SpheresMade-" + spheresMade
					+ ", SpheresFilled-" + spheresFilled + "\n";
			System.out.println("Distance-> Thickness : SpheresMade-"
					+ spheresMade + ", SpheresFilled-" + spheresFilled);
			runCount++;
			return true;
		}
		return false;
	}

	public void execute(final int numIterations) {
		fillSpheres = numIterations;
		execute();
	}

	/** get the best position in the current image */
	protected int[] getBest(final int startSlice, final int endSlice) {
		int bigValue = -1;
		final int[] outVar = new int[3];
		for (int z = startSlice + OUTERSHELL; z < (endSlice + OUTERSHELL); z++) {
			for (int y = lowy + OUTERSHELL; y < (uppy + OUTERSHELL); y++) {
				int off = (z * dim.y + y) * dim.x + lowx + OUTERSHELL;
				for (int x = lowx + OUTERSHELL; x < (uppx + OUTERSHELL); x++, off++) {
					if (inAim[off] > bigValue) {
						bigValue = inAim[off];
						outVar[0] = x;
						outVar[1] = y;
						outVar[2] = z;
					}
				}
			}

		}
		// if (((double) bigValue)*distScalar<MINTHICKNESS) return null;
		// else

		return outVar;

	}

	@Override
	public String getPluginName() {
		return "Thickness";
	}

	/** get a random position in the current image */
	protected int[] getRandom(final int startSlice, final int endSlice) {
		final Random rgen = new Random();
		final int x = rgen.nextInt(uppx - lowx - 2 * OUTERSHELL) + lowx
				+ OUTERSHELL;
		final int y = rgen.nextInt(uppy - lowy - 2 * OUTERSHELL) + lowy
				+ OUTERSHELL;
		final int z = rgen.nextInt(endSlice - startSlice) + startSlice;
		final int[] outVar = new int[3];
		outVar[0] = x;
		outVar[1] = y;
		outVar[2] = z;
		return outVar;
	}

	@Override
	protected void InitLabels(final D3int idim, final D3int ioffset) {
		outAim = new int[aimLength];
		System.arraycopy(inAim, 0, outAim, 0, inAim.length);
		InitDims(idim, ioffset);

	}

	@Override
	public void processWork(final Object currentWork) {
		final int[] range = (int[]) currentWork;
		final int bSlice = range[0];
		final int tSlice = range[1];
		runSection(bSlice, tSlice);
	}


	public void runSection(final int startSlice, final int endSlice) {
		int changedPos = 0;
		int curChanges = 0;
		double avgRadius = 0;
		boolean validBest = true;
		boolean useSync = false;
		long rundi = 0;
		while ((upSpheres(false) < fillSpheres)
				&& (spheresMade < fillSpheres * 20) && validBest) {
			int[] sPos;
			if (rundi % bestFrequency == 0)
				sPos = getBest(startSlice, endSlice);
			else
				sPos = getRandom(startSlice, endSlice);

			if (sPos != null) {
				final int x = sPos[0];
				final int y = sPos[1];
				final int z = sPos[2];
				final int off = (z * dim.y + y) * dim.x + x;
				if (inAim[off] > 0) {

					final int myDist = inAim[off];
					/** the distance of the current point away from the edge */
					final double cMaxVal = (myDist) * distScalar;
					if (rundi % bestFrequency == 0) {
					}
					if (cMaxVal > MINTHICKNESS) {

						curSphere = 0;
						useSync = ((z + cMaxVal) >= endSlice)
								| ((z - cMaxVal) < startSlice);
						fillBubble(x, y, z, cMaxVal, useSync);

						spheresMade++;
						avgRadius += cMaxVal;
						curChanges += curSphere;
						if (curSphere > 0)
							upSpheres(true);

						final boolean spfTrigger = (((upSpheres(false) + 1) % 1000) == 0);
						final boolean spmTrigger = (((spheresMade + 1) % 5000) == 0);
						if (spmTrigger) {
							System.out.println("(Spheres: " + spheresFilled
									+ "/" + spheresMade + ", Average Radius:"
									+ avgRadius / spheresMade);
						}
						if (spfTrigger) {
							changedPos += curChanges;
							System.out.println("(Spheres: " + spheresFilled
									+ "/" + spheresMade + ", Last.Cng(pm):"
									+ (curChanges * 1000) / aimLength + ")"
									+ ", Kill(pm):" + killVox * 1000
									/ aimLength + ")" + ", Changes(pc):"
									+ changedPos * 100 / aimLength + ")");
							curChanges = 0;

						}
					}
				}
			} else {
				validBest = false;
				System.out
						.println("No More Suitable Points Have Been Found in this Region <"
								+ startSlice + ", " + endSlice + ">");
			}
			rundi++;
			if (rundi % 17000 == 0)
				System.out.println("Iter:" + rundi + ", (Spheres: "
						+ spheresFilled + "/" + spheresMade + ", Last.Cng(pm):"
						+ (curChanges * 1000) / aimLength + ")" + ", Kill(pm):"
						+ killVox * 1000 / aimLength + ")" + ", Changes(pc):"
						+ changedPos * 100 / aimLength + ")");
		}
		// Make all tiny thicknesses vanish
		useSync = false;
		final int mthkDist = (int) Math.floor((MINTHICKNESS / distScalar));
		for (int z = lowz; z < (uppz); z++) {
			for (int y = lowy; y < (uppy); y++) {
				int off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < (uppx); x++, off++) {
					if (outAim[off] < mthkDist) {
						if (useSync)
							synWriteOut(off, 0);
						else
							outAim[off] = 0;
					}
				}
			}
		}

	}

	@Override
	public ArgumentParser setParameter(final ArgumentParser p,
			final String prefix) {
		final ArgumentParser args = super.setParameter(p, prefix);
		fillSpheres = args.getOptionInt(prefix + "fillspheres", fillSpheres,
				"Number of spheres to fill");
		return args;
	}

	protected synchronized int upSpheres(final boolean doUpp) {
		if (doUpp)
			spheresFilled++;
		return spheresFilled;
	}
}
