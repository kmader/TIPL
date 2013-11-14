package tipl.tools;

import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * Does a neighborhood analysis on the labeled image inAim returns CountImg - an
 * image with the count of the neighbors of each object or countvox with the
 * count of the neighbors at each voxel
 */
public class Neighbors extends BaseTIPLPluginIO {
	private class NeighborList {
		// label of neighbor, voxel count
		private final HashMap<Integer, Integer> nvox;
		private final Integer one = new Integer(1);
		private final Integer zero = new Integer(0);

		public NeighborList(final int ilabel) {
			nvox = new HashMap<Integer, Integer>();
		}

		public void addvox(final int inlabel) {
			final Integer nlabel = new Integer(inlabel);
			addvox(nlabel);
		}

		public void addvox(final Integer nlabel) {
			if (nvox.containsKey(nlabel)) {
				nvox.put(nlabel, new Integer(nvox.get(nlabel).intValue() + 1));
			} else
				nvox.put(nlabel, one);
		}

		public int count() { // number of neighbors it has
			return nvox.size();
		}

		public Iterator<Integer> getIter() {
			return nvox.keySet().iterator();
		}

		public Integer getvoxs(final Integer nlabel) {
			if (nvox.containsKey(nlabel)) {
				return nvox.get(nlabel);
			} else
				return zero;
		}
	}

	private class NeighborVoxel implements Iterator {
		Map<Integer, NeighborList> mp;

		// Iterator Implementation, provides an iterator for all edges in the
		// image, useful for
		private Iterator<Integer> nvxIt = null;
		private Iterator<Integer> subNvxIt = null;

		private Integer nvxCur = null;
		private Integer snvxCur = null;

		public NeighborVoxel() {
			mp = new HashMap<Integer, NeighborList>();
		}

		public void addvox(final int ilabel, final int jlabel) {

			final Integer nlabel = new Integer(Math.min(ilabel, jlabel));
			final Integer mlabel = new Integer(Math.max(ilabel, jlabel));
			if (!mp.containsKey(nlabel))
				mp.put(nlabel, new NeighborList(nlabel));
			mp.get(nlabel).addvox(mlabel);
		}

		public int count(final int ilabel) { // number of neighbors
			final Integer nlabel = new Integer(ilabel);
			if (mp.containsKey(nlabel))
				return mp.get(nlabel).count();
			else
				return 0;
		}

		/**
		 * Iterator Implementation, provides an iterator for all edges in the
		 * image, useful for outputting data as csv
		 */
		@Override
		public boolean hasNext() {
			if (subNvxIt.hasNext())
				return true;
			else {
				while (nvxIt.hasNext()) {
					nvxCur = nvxIt.next();
					subNvxIt = (mp.get(nvxCur)).getIter();
					if (subNvxIt.hasNext())
						return true;
				}
			}
			return false;
		}

		public void InitIter() {
			nvxIt = mp.keySet().iterator();
			nvxCur = nvxIt.next();
			subNvxIt = (mp.get(nvxCur)).getIter();
		}

		/**
		 * Iterator Implementation, provides an iterator for all edges in the
		 * image, useful for outputting data as csv
		 */
		@Override
		public Integer[] next() {
			Integer[] outData = null;
			if (hasNext()) {
				outData = new Integer[3];
				snvxCur = subNvxIt.next();
				outData[0] = nvxCur;
				outData[1] = snvxCur;
				outData[2] = mp.get(nvxCur).getvoxs(snvxCur);
			}
			return outData;
		}

		@Override
		public void remove() {
			System.out.println("Please don't hurt me!");
		}
	}

	public static void main(final String[] args) {
		final String kVer = "120105_001";
		System.out.println(" Neighborhood Script v" + kVer);
		System.out.println(" Counts neighbors for each object and voxel v"
				+ kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final Neighbors nbor = new Neighbors();
		final ArgumentParser p = new ArgumentParser(args);
		final String labelsAim = p.getOptionString("labels", "",
				"Input labeled image");
		final String countimageAim = p.getOptionString("countimage", "",
				"Output neighbor count image");
		final String voxcountimageAim = p.getOptionString("voximage", "",
				"Output voxel neighbor count image");
		final String outCSV = p.getOptionString("csv", "",
				"Output neighbor edge file");
		// D3int nSize = p.getOptionD3int("neighborsize",new
		// D3int(1,1,1),"Neighborhood size (1,1,1) = (N27)");
		nbor.setParameter(p);

		if (p.hasOption("?")) {
			System.out.println(" IPL Demo Help");
			System.out
					.println(" Filters, thresholds, segements, and labels bubbles inside a foam");
			System.out.println(" Arguments::");
			System.out.println(" ");
			System.out.println(p.getHelp());
			System.exit(0);
		}
		if (labelsAim.length() > 0) { // Read in labels

			System.out.println("Loading " + labelsAim + " ...");
			final TImg inputAim = TImgTools.ReadTImg(labelsAim);

			nbor.LoadImages(new TImgRO[] { inputAim });

			if ((outCSV.length() > 0) | (countimageAim.length() > 0)) {
				System.out.println("Calculating neighbors " + labelsAim
						+ " ...");
				nbor.run();
			}

			if (countimageAim.length() > 0) {
				System.out.println("Writing counts ...");
				TImgTools.WriteTImg(nbor.ExportAim(inputAim),countimageAim, 0, 0, false,false);
			}
			if (voxcountimageAim.length() > 0) {

				System.out.println("Writing voxel counts ...");
				TImgTools.WriteTImg(nbor.ExportVoxCountImageAim(inputAim), p.getOptionAsString("voximage"), 0, 0, false,false);

			}
			if (outCSV.length() > 0) {
				System.out.println("Writing csv neigbhor-list ...");
				nbor.WriteNeighborList(outCSV);
			}

		} else {

			System.out.println(" Arguments::");
			System.out.println(" ");
			System.out.println(p.getHelp());

		}

	}

	public int[] inAim;
	NeighborVoxel nvlist;
	boolean isRun = false;
	/** Count background voxels (=0) as neighbors */
	public boolean countBg = false;

	public Neighbors() {

	}

	@Deprecated
	public Neighbors(final int[] inputmap, final D3int idim, final D3int ioffset) {
		aimLength = inputmap.length;
		inAim = inputmap;
		InitLabels(idim, ioffset);
	}

	@Deprecated
	public Neighbors(final short[] inputmap, final D3int idim,
			final D3int ioffset) {
		aimLength = inputmap.length;
		inAim = new int[aimLength];
		for (int i = 0; i < aimLength; i++)
			inAim[i] = inputmap[i];
		InitLabels(idim, ioffset);
	}

	public Neighbors(final TImgRO inputAim) {
		LoadImages(new TImgRO[] { inputAim });
	}

	/**
	 * Returns an image with each object colored by the number of objects it is
	 * in contact with
	 */
	public int[] countImage() {
		double ncnt = 0.0;
		double nvox = 0.0;
		int maxn = 0;
		int minn = 1000;
		int off = 0;

		if (!isRun)
			run();

		final int[] countImageVar = new int[aimLength];
		for (int z = lowz; z < uppz; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
					if (inAim[off] > 0) {
						final int ccount = nvlist.count(inAim[off]);
						countImageVar[off] = ccount;
						ncnt += ccount;
						nvox++;
						if (ccount > maxn)
							maxn = ccount;
						if (ccount < minn)
							minn = ccount;
					}
				}
			}
		}
		procLog += "CMD:Neighbors :N" + neighborSize + ", Mean Neighbors: "
				+ Math.round(ncnt / nvox) + ", Max:" + maxn + "\n";
		System.out.println("CMD:Neighbors :N" + neighborSize
				+ ", Mean Neighbors: " + Math.round(ncnt / nvox) + ", Max:"
				+ maxn);

		return countImageVar;

	}

	@Override
	public boolean execute() {
		int off = 0;

		// Code for stationaryKernel
		BaseTIPLPluginIn.stationaryKernel curKernel;
		if (neighborKernel == null)
			curKernel = new BaseTIPLPluginIn.stationaryKernel();
		else
			curKernel = new BaseTIPLPluginIn.stationaryKernel(neighborKernel);

		for (int z = lowz; z < uppz; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {

					if (inAim[off] > 0) {
						int off2;
						for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(
								z + neighborSize.z, uppz - 1); z2++) {
							for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(
									y + neighborSize.y, uppy - 1); y2++) {
								off2 = (z2 * dim.y + y2) * dim.x
										+ max(x - neighborSize.x, lowx);
								for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(
										x + neighborSize.x, uppx - 1); x2++, off2++) {
									boolean checkPt = true;
									if (!countBg)
										if (inAim[off2] == 0)
											checkPt = false;
									if (checkPt) {
										if (curKernel.inside(off, off2, x, x2,
												y, y2, z, z2)) {
											if ((off != off2)) {
												if (inAim[off] != inAim[off2]) {
													// New neighbor

													nvlist.addvox(inAim[off],
															inAim[off2]);
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
		isRun = true;
		runCount++;
		return true;
	}

	@Override
	public boolean execute(final String command, final Object cObj) {
		if (command.equalsIgnoreCase("WriteNeighborList"))
			return WriteNeighborList((String) cObj);

		return super.execute(command, cObj);
	}

	/** export the default (count image) */
	@Override
	public TImg ExportAim(final TImg.CanExport templateAim) {
		return ExportCountImageAim(templateAim);
	}

	/**
	 * function to export the count image as an aim file (the number of
	 * neighbors for each object)
	 */
	public TImg ExportCountImageAim(final TImg.CanExport templateAim) {
		TImg outAim = null;
		if (isInitialized) {

			if (runCount > 0) {
				outAim = templateAim.inheritedAim(countImage(), dim, offset);
				outAim.appendProcLog(procLog);
				outAim.setShortScaleFactor(1.0f);

			} else {
				throw new IllegalArgumentException(
						"The plug-in : "
								+ getPluginName()
								+ ", has not yet been run, exported does not exactly make sense, original data will be sent.");
				// outAim=templateAim.inheritedAim(inAim,dim,offset);
				// outAim.appendProcLog("The plug-in : "+getPluginName()+", has not yet been run, exported does not exactly make sense, original data will be sent.");

			}
		} else {

			throw new IllegalArgumentException(
					"The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exported does not make any sense");
			// outAim=
			// templateAim.inheritedAim(templateAim.getBoolAim(),dim,offset);
			// outAim.appendProcLog("The plug-in : "+getPluginName()+", has not yet been initialized, exported does not make any sense");

		}
		return outAim;
	}

	/**
	 * This implementation exports the neighbors and then the voxel count
	 */
	@Override
	public TImg[] ExportImages(final TImgRO templateImage) {
		// TODO Auto-generated method stub
		final TImg cImg = TImgTools.WrapTImgRO(templateImage);
		return new TImg[] { ExportAim(cImg), ExportVoxCountImageAim(cImg) };
	}

	/**
	 * function to export the voxel count image as an aim file (the number of
	 * neighbors for each voxel)
	 */
	public TImg ExportVoxCountImageAim(final TImg.CanExport templateAim) {
		TImg outAim = null;
		if (isInitialized) {
			outAim = templateAim.inheritedAim(voxCountImage(), dim, offset);
			outAim.appendProcLog(procLog);
			return outAim;

		} else {
			throw new IllegalArgumentException(
					"The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exported does not make any sense");
		}
	}

	@Override
	public String getPluginName() {
		return "Neighbors";
	}

	private void InitLabels(final D3int idim, final D3int ioffset) {
		isRun = false;
		nvlist = new NeighborVoxel();
		InitDims(idim, ioffset);
	}

	/**
	 * LoadImages assumes the first image is label map
	 */
	@Override
	public void LoadImages(final TImgRO[] inImages) {
		// TODO Auto-generated method stub
		if (inImages.length < 1)
			throw new IllegalArgumentException(
					"Too few arguments for LoadImages in:" + getPluginName());
		final TImgRO.FullReadable labelImg = TImgTools
				.makeTImgFullReadable(inImages[0]);
		inAim = labelImg.getIntAim();
		aimLength = inAim.length;
		InitLabels(labelImg.getDim(), labelImg.getOffset());
	}

	@Override
	@Deprecated
	public void run() {
		execute();
	}

	@Override
	public ArgumentParser setParameter(final ArgumentParser p) {
		countBg = p
				.getOptionBoolean("countbg", "Count background as an object");
		return super.setParameter(p);
	}

	/**
	 * Returns the number of different objects are neighboring each voxel, good
	 * for extracting interfaces between one or more objects
	 */
	public int[] voxCountImage() {
		double ncnt = 0.0;
		double nvox = 0.0;
		int maxn = 0;
		int off = 0;
		final int[] vcountImageVar = new int[aimLength];

		// Code for stationaryKernel
		BaseTIPLPluginIn.stationaryKernel curKernel;
		if (neighborKernel == null)
			curKernel = new BaseTIPLPluginIn.stationaryKernel();
		else
			curKernel = new BaseTIPLPluginIn.stationaryKernel(neighborKernel);

		for (int z = lowz; z < uppz; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {

					if (inAim[off] > 0) {
						int off2;
						NeighborList nltemp = new NeighborList(inAim[off]);
						for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(
								z + neighborSize.z, uppz - 1); z2++) {
							for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(
									y + neighborSize.y, uppy - 1); y2++) {
								off2 = (z2 * dim.y + y2) * dim.x
										+ max(x - neighborSize.x, lowx);
								for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(
										x + neighborSize.x, uppx - 1); x2++, off2++) {
									boolean checkPt = true;
									if (!countBg)
										if (inAim[off2] == 0)
											checkPt = false;
									if (checkPt) {
										if (curKernel.inside(off, off2, x, x2,
												y, y2, z, z2)) {
											if ((off != off2)) {
												if (inAim[off] != inAim[off2]) {
													// New neighbor
													nltemp.addvox(inAim[off2]);
												}
											}
										}
									}
								}
							}
						}
						vcountImageVar[off] = nltemp.count();
						ncnt += vcountImageVar[off];
						if (vcountImageVar[off] > maxn)
							maxn = vcountImageVar[off];
						nvox++;
						nltemp = null;
					}
				}
			}
		}

		procLog += "CMD:Neighbors :N" + neighborSize
				+ ", Mean Voxel Neighbors: " + Math.round(ncnt / nvox)
				+ ", Max:" + maxn + "\n";
		System.out.println("CMD:Neighbors :N" + neighborSize
				+ ", Mean Voxel Neighbors: " + Math.round(ncnt / nvox)
				+ ", Max:" + maxn);
		return vcountImageVar;

	}

	/**
	 * Write list of neighbors from analysis as a csv file with (obj_a,
	 * obj_b,overlapping_voxels) for all obj_a IS_TOUCHING obj_b
	 */
	public boolean WriteNeighborList(final String outfileName) {
		if (!isRun)
			run();
		System.out.println("Writing EdgeFile..." + outfileName);

		try {
			final FileWriter out = new FileWriter(outfileName, false);
			String outString = "// Sample: " + outfileName + "\n";
			outString += "// Component 1, Component 2, Voxels\n";
			out.write(outString);
			nvlist.InitIter();
			while (nvlist.hasNext()) {
				final Integer[] curLine = (nvlist.next());
				outString = curLine[0] + ", " + curLine[1] + ", " + curLine[2]
						+ "\n";
				out.write(outString);
				out.flush();
			}
			out.flush();
			out.close();

		} catch (final Exception e) {
			System.out.println("Writing Output File Problem");
			e.printStackTrace();
			return false; // not successful
		}
		return true;

	}
}
