package tipl.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tipl.formats.TImg;
import tipl.util.ArgumentParser;
import tipl.util.ITIPLPlugin;
import tipl.util.ITIPLPluginIO;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;

/**
 * cVoronoi implements a voronoi transform useing center of volume positions
 * rather than current shape
 */
public class cVoronoi extends VoronoiTransform {
	@TIPLPluginManager.PluginInfo(pluginType = "cVoronoi",
			desc="Full memory center voronoi",
			sliceBased=false,
			maximumSize=1024*1024*1024)
	final public static TIPLPluginManager.TIPLPluginFactory myFactory = new TIPLPluginManager.TIPLPluginFactory() {
		@Override
		public ITIPLPlugin get() {
			return new cVoronoi();
		}
	};
	static class cVorList {
		public int label;
		Map<Integer, cVorObj> mp;
		public boolean scaleddist = false;

		public cVorList() {
			mp = new HashMap<Integer, cVorObj>();
		}

		public void addvox(final int ilabel, final int x, final int y,
				final int z) {
			final Integer nlabel = new Integer(ilabel);
			if (!mp.containsKey(nlabel))
				mp.put(nlabel, new cVorObj());
			mp.get(nlabel).addvox(x, y, z);
		}

		public int count(final Integer nlabel) { // number of neighbors
			if (mp.containsKey(nlabel))
				return mp.get(nlabel).count();
			else
				return 0;
		}

		public double dist(final int ilabel, final int x, final int y,
				final int z) { // number of
			// neighbors
			final Integer nlabel = new Integer(ilabel);
			return dist(nlabel, x, y, z);
		}

		public double dist(final Integer nlabel, final int x, final int y,
				final int z) { // number of
			// neighbors
			if (mp.containsKey(nlabel))
				return mp.get(nlabel).dist(scaleddist, x, y, z);
			else
				return 0;
		}

		public cVorObj get(final Integer nlabel) {
			if (mp.containsKey(nlabel))
				return mp.get(nlabel);
			else
				return null;
		}

		public Iterator<Integer> kiterator() {
			final List<Integer> ks = new ArrayList<Integer>(mp.keySet());
			Collections.shuffle(ks); // ensures the list is randomized (prevents
										// collisions)
			return ks.iterator();
		}

	}

	static class cVorObj {
		// public int label;
		private float sumx = 0.0f;
		private float sumy = 0.0f;
		private float sumz = 0.0f;

		private float ssumx = 0.0f;
		private float ssumy = 0.0f;
		private float ssumz = 0.0f;
		private float voxcnt = 0.0f;

		private float cx = 0.0f;
		private float cy = 0.0f;
		private float cz = 0.0f;

		private float sx = 0.0f;
		private float sy = 0.0f;
		private float sz = 0.0f;

		private boolean calcdcov = false;

		// Scale distance by STD

		public cVorObj() {
			sumx = 0.0f;
			sumy = 0.0f;
			sumz = 0.0f;

			ssumx = 0.0f;
			ssumy = 0.0f;
			ssumz = 0.0f;

			voxcnt = 0.0f;
		}

		synchronized public void addvox(final int x, final int y, final int z) {
			sumx += x;
			sumy += y;
			sumz += z;
			ssumx += (float) Math.pow(x, 2);
			ssumy += (float) Math.pow(y, 2);
			ssumz += (float) Math.pow(z, 2);

			voxcnt++;
			calcdcov = false;
		}

		private void calccov() {
			cx = sumx / voxcnt;
			cy = sumy / voxcnt;
			cz = sumz / voxcnt;
			// std
			sx = (float) Math.sqrt(ssumx / voxcnt - Math.pow(cx, 2));
			sy = (float) Math.sqrt(ssumy / voxcnt - Math.pow(cy, 2));
			sz = (float) Math.sqrt(ssumz / voxcnt - Math.pow(cz, 2));

			if ((new Double(sx)).isNaN()) {
				System.out.println("sx is not valid... " + sx
						+ ", setting to 0.5");
				sx = 0.5f;
			}
			if ((new Double(sy)).isNaN()) {
				System.out.println("sy is not valid... " + sx
						+ ", setting to 0.5");
				sy = 0.5f;
			}
			if ((new Double(sz)).isNaN()) {
				System.out.println("sz is not valid... " + sx
						+ ", setting to 0.5");
				sz = 0.5f;
			}
			calcdcov = true;

		}

		public int count() {
			return (int) voxcnt;
		}

		public double dist(final boolean scaleddist, final float x,
				final float y, final float z) {
			if (!calcdcov)
				calccov();
			if (!scaleddist)
				return Math.sqrt(Math.pow(x - cx, 2) + Math.pow(y - cy, 2)
						+ Math.pow(z - cz, 2));
			else
				return 1000 * Math.sqrt(Math.pow((x - cx) / sx, 2)
						+ Math.pow((y - cy) / sy, 2)
						+ Math.pow((z - cz) / sz, 2));
		}

		public double dist(final boolean scaleddist, final int x, final int y,
				final int z) {
			return dist(scaleddist, (float) x, (float) y, (float) z);
		}

		@Override
		public String toString() {
			if (!calcdcov)
				calccov();
			return "COV:(" + cx + "," + cy + "," + cz + "), STD:(" + sx + ","
					+ sy + "," + sz + "), N:" + voxcnt;
		}
	}

	public static void main(final String[] args) {
		final String kVer = "120105_002";
		System.out
				.println(" cVoronoi (Centroid-based voronoi) Script v" + kVer);
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
		final boolean leaveVox = p.getOptionBoolean("leavevox",
				"Leave original voxels");
		final boolean distscale = p.getOptionBoolean("distscale",
				"Scale distance by object radius");
		if (p.hasOption("?") || (labelsAimName.length() < 1)) {
			System.out.println(" cVoronoi Help");
			System.out
					.println(" Performs voronoi transform on labeled images into given mask (filled ROI if no mask is given)");
			System.out.println(" Arguments::");
			System.out.println("  ");
			System.out.println(p.getHelp());
			System.exit(0);
		}

		long start;

		System.currentTimeMillis();

		System.out.println("Loading " + labelsAimName + " ...");
		final TImg labelsAim = TImgTools.ReadTImg(labelsAimName);
		TImg maskAim;

		start = System.currentTimeMillis();
		ITIPLPluginIO CV;

		if (maskAimName.length() > 0) {
			System.out.println("Loading " + maskAimName + " ...");
			maskAim = TImgTools.ReadTImg(maskAimName);
			start = System.currentTimeMillis();
			
			CV = TIPLPluginManager.createBestPluginIO("cVoronoi",new TImg[] {labelsAim,maskAim});
			CV.setParameter("-preservelabels="+leaveVox);
			CV.LoadImages(new TImg[] {labelsAim,maskAim});
		} else {
			CV = TIPLPluginManager.createBestPluginIO("cVoronoi",new TImg[] {labelsAim});
			CV.setParameter("-preservelabels="+leaveVox);
			CV.LoadImages(new TImg[] {labelsAim});
			maskAim = labelsAim;
		}
		CV.setParameter("-scaleddist="+distscale);
		CV.execute();
		TImg[] outImgs=CV.ExportImages(labelsAim);
		
		TImgTools.WriteTImg(outImgs[0], vorVolumesName);
		TImgTools.WriteTImg(outImgs[1], vorDistancesName);
		try {
			final float eTime = (System.currentTimeMillis() - start)
					/ (60 * 1000F);
			String outString = "";
			outString += "Run Finished in " + eTime + " mins @ " + new Date()
					+ "\n";
			System.out.println(outString);

		} catch (final Exception e) {
			System.out.println("DB Problem");
		}

	}

	protected boolean scaleddist = true;
	protected boolean updateCOV = false;
	@Override
	public ArgumentParser setParameter(final ArgumentParser p,
			final String prefix) {
		final ArgumentParser args = super.setParameter(p, prefix);
		scaleddist = args.getOptionBoolean(prefix + "scaleddist", scaleddist,
				"Scale the distance proportionally to the radius");
		
		updateCOV = args.getOptionBoolean(prefix + "updatecov", updateCOV,
				"Update the COV position with iterations");
		return args;
	}
	public boolean[] tmask;
	int maxVal = -1;
	protected volatile cVorList nvlist;
	double emptyVoxels = 0;

	public cVoronoi() {
		
	}

	@Override
	public boolean execute() {
		if (runMulticore()) {
			System.out.println("cVoronoi Finished");
			procLog += "CMD:cVoronoi, DistScale:" + scaleddist
					+ ", Replacement:" + preserveLabels + "\n";
			return true;
		}
		return false;
	}

	@Override
	public String getPluginName() {
		return "cVoronoi";
	}
	/**
	 * generate a new image tmask and create the nvlist of the points in the current image
	 */
	protected void customInitSteps() {
		nvlist = new cVorList();
		int off = 0;
		double fullVoxels = 0;
		// I don't want to screw the input mask up
		if (preserveLabels) {
			tmask = new boolean[aimLength];
			System.arraycopy(mask, 0, tmask, 0, mask.length);
		} else
			tmask = mask;

		System.out.println("Scanning Image...");
		for (int z = lowz; z < uppz; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
					// Label All Voxels
					if (labels[off] > 0) {
						if (labels[off] > maxVal)
							maxVal = labels[off];
						nvlist.addvox(labels[off], x, y, z);
						fullVoxels++;
						if (preserveLabels)
							tmask[off] = false;
					}
					if (tmask[off])
						emptyVoxels++;
				}
			}
		}
		System.out.println("(MegaP) Fillable Voxels:" + emptyVoxels / 1e6
				+ ", Full Voxels:" + fullVoxels / 1e6
				+ ",  Starting VoronoiLoop (" + maxVal + ")...");
		nvlist.scaleddist = scaleddist;
	}

	@Override
	public void processWork(final Object currentWork) {
		final int[] range = (int[]) currentWork;
		final int bSlice = range[0];
		final int tSlice = range[1];
		runSection(bSlice, tSlice);
	}

	protected void runSection(final int startSlice, final int finalSlice) {

		final boolean limitDist = (maxUsuableDistance > 0);
		final Iterator<Integer> it = nvlist.kiterator();
		int loopCnt = 0;

		while (it.hasNext()) {
			double swappedVox = 0;
			double maxDist = 0.0;
			final Integer curLabel = it.next();
			final cVorObj curCVO = nvlist.get(curLabel);
			if (curCVO.count() > 0) {
				for (int z = startSlice; z < finalSlice; z++) {
					for (int y = lowy; y < uppy; y++) {
						int off = (z * dim.y + y) * dim.x + lowx;
						for (int x = lowx; x < uppx; x++, off++) {
							// The code is optimized so the least number of
							// voxels make it past the first check
							if (tmask[off]) {
								final double cdist = curCVO.dist(scaleddist, x,
										y, z);
								boolean isValid = true;
								if (limitDist)
									if (cdist < maxUsuableDistance)
										isValid = false;
								if (isValid) {
									if (loopCnt == 0) {

										distmap[off] = (int) (cdist / distScalar);
										outlabels[off] = curLabel.intValue();
										swappedVox++;
									} else {
										if (distmap[off] > ((int) (cdist / distScalar))) {
											distmap[off] = (int) (cdist / distScalar);
											outlabels[off] = curLabel
													.intValue();

											swappedVox++;
											if (cdist > maxDist)
												maxDist = cdist;
										}
									}
								}

							}

						}
					}
				}
				System.out.println("Finished Loop, "
						+ curLabel
						+ " ("
						+ loopCnt
						+ "/ "
						+ maxVal
						+ "), Replaced Voxels "
						+ String.format("%.2f", swappedVox / emptyVoxels
								* 100.0) + "(%), Max Dist:"
						+ String.format("%.2f", maxDist) + ", " + curCVO);
			} else {
				System.out.println("Empty Loop?, " + curLabel + " (" + loopCnt
						+ "/ " + maxVal + ")");
			}

			loopCnt++;
		}

		procLog += "CMD:cVoronoi performed";
		runCount = 1;

	}

}
