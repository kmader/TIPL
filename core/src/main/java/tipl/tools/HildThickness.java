package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.*;

import java.util.ArrayList;

/** Thickness map based on the Hildebrand Method */
public class HildThickness extends Thickness {
	@TIPLPluginManager.PluginInfo(pluginType = "HildThickness",
			desc="Full memory hildebrand thickness",
			sliceBased=false)
    final public static class hthickFactory implements TIPLPluginManager.TIPLPluginFactory {
		@Override
		public ITIPLPlugin get() {
			return new HildThickness();
		}
	}
	/** Run the distance label initialization routines in parallel */
	private static class dlRunner extends Thread {
		int sslice, fslice;
		volatile HildThickness parent;
		long bcount = 0;

		public dlRunner(final HildThickness iparent, final int isslice,
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
			bcount += parent.locateSeeds(sslice, fslice);
			System.out.println("DlRunner Finished:, <" + sslice + ", " + fslice
					+ ">" + ", Ridges-" + bcount);
		}
	}

	/** date and version number of the script **/
	public static final String kVer = "31-07-2014 v03";

	/**
	 * Similar to DTObjection function in XIPL, takes a black and white input
	 * image and calculates the thickness map of the object (white) of this
	 * image. It then writes the thickness map to the file outAimFile and the
	 * histogram to histoFile
	 * 
	 * @param inAimFile
	 *            the binary aim file to use for the thickness map
	 * @param outAimFile
	 *            the name of the output thickness map file
	 * @param histoFile
	 *            the name of the csv histogram file to write
	 */
	public static boolean DTO(final TypedPath inAimFile, final TypedPath outAimFile,
			final TypedPath histoFile) {
		final TImg thickmapAim = DTO(TImgTools.ReadTImg(inAimFile));
		TImgTools.WriteTImg(thickmapAim,outAimFile);
		GrayAnalysis.StartHistogram(thickmapAim, histoFile.append( ".csv"));
		return true;
	}

	/**
	 * Similar to DTObjection function in XIPL, takes a black and white input
	 * image and calculates the thickness map of the object (white) of this
	 * image. It then writes the thickness map to the file outAimFile and the
	 * histogram to histoFile
	 * 
	 * @param inAimFile
	 *            the binary aim file to use for the thickness map
	 * @param outDistFile
	 *            the name of the output distance map file
	 * @param outAimFile
	 *            the name of the output thickness map file
	 * @param histoFile
	 *            the name of the csv histogram file to write
	 * @param profileFile
	 *            the name of the file to write with the profile information
	 */
	public static boolean DTO(final TypedPath inAimFile, final TypedPath outDistFile,
			final TypedPath outAimFile, final TypedPath histoFile,
			final TypedPath profileFile) {
		final TImg maskAim = TImgTools.ReadTImg(inAimFile);
		final TImg[] mapAims = DTOD(maskAim);
		if (outDistFile.length() > 0)
			TImgTools.WriteTImg(mapAims[0],outDistFile);
		if (outAimFile.length() > 0)
			TImgTools.WriteTImg(mapAims[1],outAimFile);
		if (histoFile.length() > 0)
			GrayAnalysis.StartHistogram(mapAims[1], histoFile.append( ".tsv"));
		if (profileFile.length() > 0) {
			GrayAnalysis.StartZProfile(mapAims[1], maskAim, profileFile
					.append("_z.tsv"), 0.1f);
			GrayAnalysis.StartRProfile(mapAims[1], maskAim, profileFile
					.append( "_r.tsv"), 0.1f);
			GrayAnalysis.StartRCylProfile(mapAims[1], maskAim, profileFile
					.append("_rcyl.tsv"), 0.1f);
		}
		return true;
	}

	/**
	 * Similar to DTObjection function in XIPL, takes a black and white input
	 * image and calculates the thickness map of the object (white) of this
	 * image. It returns the thickness as an colored image
	 * 
	 * @param bwObject
	 *            The binary input image
	 */
	public static TImg DTO(final TImgRO bwObject) {
		final TImg[] outImgs = DTOD(bwObject);
		return outImgs[1];
	}

    /**
     * for the TIPLOps command
     * @param bwObject
     * @param histoFile
     * @return TImgRO array (distance map, thickness map)
     */
    public static TImgRO[] DTO(final TImgRO bwObject , final TypedPath histoFile) {
        final TImg[] mapAims = DTOD(bwObject);
        if (histoFile.length() > 0)
            GrayAnalysis.StartHistogram(mapAims[1], histoFile.append( ".tsv"));
        TypedPath profileFile = histoFile.append("");
            GrayAnalysis.StartZProfile(mapAims[1], bwObject, profileFile
                    .append("_z.tsv"), 0.1f);
            GrayAnalysis.StartRProfile(mapAims[1], bwObject, profileFile
                    .append( "_r.tsv"), 0.1f);
            GrayAnalysis.StartRCylProfile(mapAims[1], bwObject, profileFile
                    .append("_rcyl.tsv"), 0.1f);
        return mapAims;
    }

	/**
	 * Similar to DTObjection function in XIPL, takes a black and white input
	 * image and calculates the thickness map of the object (white) of this
	 * image. It returns the distance map and thickness map as an colored image
	 * 
	 * @param bwObject
	 *            The binary input image
	 */
	public static TImg[] DTOD(final TImgRO bwObject) {

		ITIPLPluginIO KV = TIPLPluginManager.createBestPluginIO("kVoronoi", new TImgRO[] {bwObject});
		KV.setParameter("-includeEdges=false");
		KV.LoadImages(new TImgRO[] {null,bwObject});
		KV.execute();
		final TImg distAim = KV.ExportImages(bwObject)[1];
		KV = null;
		final ITIPLPluginIO KT = TIPLPluginManager.createBestPluginIO("HildThickness", new TImg[] { distAim });
        KT.LoadImages(new TImg[] { distAim });
		KT.execute();
		return new TImg[] { distAim, KT.ExportImages(distAim)[0] };
	}
	public static void main(final String[] args) {
		System.out.println("Hildebrand-based Thickness Map v"
				+ HildThickness.kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final ArgumentParser p =  TIPLGlobal.activeParser(args);
		final TypedPath inAimFile = p.getOptionPath("in", "",
				"In image to calculate the thickness map of");

		String defOutName = inAimFile.getPath();
		if (defOutName.lastIndexOf(".") > 0)
			defOutName = defOutName.substring(0, defOutName.lastIndexOf("."));
		final TypedPath outDistFile = p.getOptionPath("distmap", defOutName
				+ "_dist.tif", "Output distance map");
		final TypedPath outAimFile = p.getOptionPath("thickmap", defOutName
				+ "_dto.tif", "Output thickness map");
		final TypedPath histoFile = p.getOptionPath("csv", defOutName + "_dto",
				"Histogram of thickness values");
		final TypedPath profileFile = p.getOptionPath("profile", histoFile+"_z",
				"Profile of thickness values");

		final boolean runAsJob = p
				.getOptionBoolean("sge:runasjob",
						"Run this script as an SGE job (adds additional settings to this task");
		SGEJob jobToRun = null;
		if (runAsJob)
			jobToRun = SGEJob.runAsJob("tipl.tools.HildThickness", p, "sge:");
		p.checkForInvalid();
		if (runAsJob)
			jobToRun.submit();
		else
			DTO(inAimFile, outDistFile, outAimFile, histoFile, profileFile);
	}

	public boolean[] diffmask;
	public int maxlabel;
	public int unfilledVox = 0;

	private final double MINWALLDIST = 3;
	/** criterium to identify high points on the distance map for the ridge **/
	public double FLATCRIT = 0.45;// 0.401;
	/** how many voxels to skip while running the filling **/
	public int SKIPFILLING = 3;
	protected boolean isDiffMaskReady = false;

	int remVoxels = aimLength;

	int totalVoxels = aimLength;
	public int bubbleCount = 0;

	private HildThickness() {

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

	@Override
	public boolean execute() {
		if (runMulticore()) {

			final String outString = "HildThicknes: Ran in "
					+ StrRatio(System.currentTimeMillis() - jStartTime, 1000)
					+ " seconds on " + neededCores() + " cores";
			System.out.println(outString);
			procLog += outString + "\n";

			procLog += "CMD:" + getPluginName() + ": Max Bubbles:"
					+ bubbleCount + "\n";
			runCount++;
			return true;
		} else
			return false;

	}
	/** add the ridge file to the exported aim
	 * 
	 */
	@Override
	public TImg[] ExportImages(final TImgRO templateImage) {
		final TImg cImg = TImgTools.WrapTImgRO(templateImage);
		return new TImg[] { CreateOutputImage(cImg), ExportRidgeAim(cImg) };
	}

	/** export the bubble seeds if anyone actually wants them */
	public TImg ExportRidgeAim(final TImgRO.CanExport templateAim) {
		if (isDiffMaskReady) {
			final TImg outAimData = templateAim.inheritedAim(diffmask, dim,
					offset);
			outAimData.appendProcLog(procLog);
			return outAimData;
		} else {
			throw new IllegalArgumentException(
					"The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exporting the ridge map does not make any sense");
			// return templateAim.inheritedAim(templateAim);

		}
	}

	@Override
	public String getPluginName() {
		return "Hildebrand Thickness";
	}

	protected void Init(final D3int idim, final D3int ioffset) {
		outAim = new int[aimLength];
		System.arraycopy(inAim, 0, outAim, 0, inAim.length);
		diffmask = new boolean[aimLength];
		InitDims(idim, ioffset);
		InitDiffmask();
	}

	public void InitDiffmask() {

		Thread.currentThread();
		final ArrayList<dlRunner> threadList = new ArrayList<dlRunner>();

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

		}

		final String outString = "Distance Ridge: Ran in "
				+ StrRatio(System.currentTimeMillis() - jStartTime, 1000)
				+ " seconds on " + neededCores() + " cores and found ...";
		System.out.println(outString);
		procLog += outString + "\n";
		isDiffMaskReady = true;
	}

	@Override
	protected void InitLabels(final D3int idim, final D3int ioffset) {

	}

	/**
	 * Load the distance map and create the ridge data
	 * 
	 */
	@Override
	public void LoadImages(final TImgRO[] inImages) {
		// TODO Auto-generated method stub
		if (inImages.length < 1)
			throw new IllegalArgumentException(
					"Too few arguments for LoadImages in:" + getPluginName());
		final TImgRO inImg = inImages[0];
		final int[] inputmap = TImgTools.makeTImgFullReadable(inImg)
				.getIntAim();
		aimLength = inputmap.length;
		if (Thickness.doPreserveInput) {
			inAim = new int[aimLength];
			System.arraycopy(inputmap, 0, inAim, 0, aimLength);
		} else {
			inAim = inputmap;
		}
		Init(inImg.getDim(), inImg.getOffset());
	}

	private int locateSeeds(final int startSlice, final int finalSlice) {
		final D3int iNeighborSize = new D3int(2);
		int off = 0;
		double avgGrad = 0;
		double avgDGrad = 0;
		double avgSGrad = 0;
		int inVox = 0;
		unfilledVox = 0;
		int ridgeCnt = 0;
		final double cFLATCRIT = FLATCRIT;
		for (int z = startSlice; z < finalSlice; z++) {
			for (int y = lowy + OUTERSHELL; y < (uppy - OUTERSHELL); y++) {
				off = (z * dim.y + y) * dim.x + lowx + OUTERSHELL;
				for (int x = lowx + OUTERSHELL; x < (uppx - OUTERSHELL); x++, off++) {
					// The code is optimized so the least number of voxels make
					// it past the first check
					final float cVDist = (float) distScalar * inAim[off];

					if (((cVDist) > MINWALLDIST)) {
						unfilledVox++;
						double gradX = 0.0, gradY = 0.0, gradZ = 0.0;
						int gradCount = 0;
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
											gradX += (x2 - x) * (inAim[off2]);
											gradY += (y2 - y) * (inAim[off2]);
											gradZ += (z2 - z) * (inAim[off2]);
											gradCount++;
										}

										if (((x2 != x) ? 1 : 0)
												+ ((y2 != y) ? 1 : 0)
												+ ((z2 != z) ? 1 : 0) <= 1) { // slightly

										}

										// First derivative is 0 and second
										// derivative is less than 0 (local
										// maxima)

									}
								}
							}
						}
						gradX /= gradCount;
						gradY /= gradCount;
						gradZ /= gradCount;

						gradX *= distScalar;
						gradY *= distScalar;
						gradZ *= distScalar;

						final double cGrad = Math.sqrt(gradX * gradX + gradY
								* gradY + gradZ * gradZ);

						avgGrad += cGrad;
						avgSGrad += cGrad * cGrad;
						inVox++;
						if ((cGrad <= cFLATCRIT)) {
							// System.out.println("GradVal:"+cGrad+"->("+gradX+", "+gradY+", "+gradZ+"), "+gradCount+", Lap:"+lapVal+", LC"+lapCount);
							diffmask[off] = true;
							ridgeCnt++;
							avgDGrad += cGrad;
						}

					} // End mask and dist-check
				} // End x
			} // End y
		} // End z
		avgGrad /= inVox;
		avgDGrad /= ridgeCnt;
		System.out.println("Average GradVal:" + avgGrad + " - STD:"
				+ Math.sqrt(avgSGrad / inVox - avgGrad * avgGrad)
				+ ", AvgRidgeVal:" + avgDGrad + " - Ridge Comp:"
				+ StrPctRatio(ridgeCnt * 100, inVox));
		// procLog+="CMD:LocalMaxima: GradVal:"+avgGrad+" - STD:"+Math.sqrt(avgSGrad/inVox-avgGrad*avgGrad)+", Lap:"+avgLap+" - STD:"+Math.sqrt(avgSLap/inVox-avgLap*avgLap)+"\n";
		return ridgeCnt;

	}

	@Override
	protected void processWork(final Object currentWork) {
		final int[] range = (int[]) currentWork;
		final int bSlice = range[0];
		final int tSlice = range[1];
		runSection(bSlice, tSlice);
	}


	protected void runSection(final int startSlice, final int endSlice) {
		System.out.println("RidgeGrow Started:, <" + startSlice + ", "
				+ endSlice + ">");
		int cBubbleCount = 0;
		final int cSKIPFILLING = SKIPFILLING;
		for (int z = startSlice + OUTERSHELL; z < (endSlice + OUTERSHELL); z++) {
			if ((z - startSlice) % 3 == 2)
				System.out.println("RGRunning:, <" + startSlice + ", "
						+ endSlice + ">:" + z + ", " + cBubbleCount);
			for (int y = lowy + OUTERSHELL; y < (uppy + OUTERSHELL); y++) {
				int off = (z * dim.y + y) * dim.x + lowx + OUTERSHELL;
				for (int x = lowx + OUTERSHELL; x < (uppx + OUTERSHELL); x++, off++) {
					if (diffmask[off]) {
						if (inAim[off] > 0) {
							if ((cBubbleCount % cSKIPFILLING) == 0) {
								final double nVal = (inAim[off]) * distScalar;
								final boolean useSync = ((z + nVal) >= endSlice)
										| ((z - nVal) < startSlice);
								fillBubble(x, y, z, nVal, useSync);

							}
							cBubbleCount++;
						}
					}
				}
			}
		}

		bubbleCount += cBubbleCount;

	}

	@Override
	public ArgumentParser setParameter(final ArgumentParser p,
			final String prefix) {
		final ArgumentParser args = super.setParameter(p, prefix);
		FLATCRIT = args
				.getOptionDouble(prefix + "flatcrit", FLATCRIT,
						"Criterion for determining if ridge points on the distance map are flat enough");
		return args;
	}

}
