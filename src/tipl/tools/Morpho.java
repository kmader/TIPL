package tipl.tools;

import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;
import tipl.util.D3int;

// Used as a replacement for the moment function as it allows much more control over data
// and communication with webservices (potentially?)
/** Class for performing morphological operations on Aim (linear array) files */
public class Morpho extends BaseTIPLPluginBW {
	/**
	 * Not yet implemented but will be a mask indicating protected voxels which
	 * cannot be changed during operations
	 */
	public boolean[] morphMask; // A mask indicating which pixels may be changed
	boolean useMask = false;
	/**
	 * The percentage of neighbors (1.0=100%) required for a successful
	 * operation, for example 1.0 means that 100% of a voxels neighbors must be
	 * on in order for that voxel to stay on
	 */
	double neighborOccupancy = 1.0;

	public String procLog = "";
	public boolean supportsThreading = true;

	/**
	 * has an operation been performed (does the data from output need to be
	 * copied to input before the next)
	 */
	boolean lastInOutput = false;

	/** current operation, erode = -1, dilate = +1 */
	protected volatile int curOperation = 0;

	/** constructor function using TIPLPluginBW standard classes */
	@Deprecated
	public Morpho(boolean[] inputmap, D3int idim, D3int ioffset) {
		ImportAim(inputmap, idim, ioffset);
	}

	/** constructor function using TIPLPluginBW standard classes */
	@Deprecated
	public Morpho(float[] inputmap, D3int idim, D3int ioffset) {
		ImportAim(inputmap, idim, ioffset);
	}

	/** constructor function using TIPLPluginBW standard classes */
	@Deprecated
	public Morpho(int[] inputmap, D3int idim, D3int ioffset) {
		ImportAim(inputmap, idim, ioffset);
	}

	/** constructor function using TIPLPluginBW standard classes */
	@Deprecated
	public Morpho(short[] inputmap, D3int idim, D3int ioffset) {
		ImportAim(inputmap, idim, ioffset);
	}

	/** constructor function using TIPLPluginBW standard classes */
	public Morpho(TImgRO inputAim) {
		LoadImages(new TImgRO[] { inputAim });
	}

	@Deprecated
	public void close() {
		closeMany(1);
	}

	@Deprecated
	public void close(D3int neighborSizeI, double neighborOccupancyI) {
		closeMany(1, neighborSizeI, neighborOccupancy);
	}

	@Deprecated
	public void close(int neighborSizeI, double neighborOccupancyI) {
		closeMany(1, neighborSizeI, neighborOccupancy);
	}

	@Deprecated
	public void closeMany(int iterations) {
		System.out.println("CloseOperation-" + iterations);
		for (int i = 0; i < iterations; i++) {
			dilate();
			erode();
		}
	}

	@Deprecated
	public void closeMany(int iterations, D3int neighborSizeI,
			double neighborOccupancyI) {
		neighborSize = neighborSizeI;
		neighborOccupancy = neighborOccupancyI;
		closeMany(iterations);
	}

	@Deprecated
	public void closeMany(int iterations, int neighborSizeI,
			double neighborOccupancyI) {
		neighborSize = null;
		neighborSize = new D3int(neighborSizeI);
		neighborOccupancy = neighborOccupancyI;
		closeMany(iterations);
	}

	@Deprecated
	public void dilate() {
		if (lastInOutput) {
			System.arraycopy(outAim, 0, inAim, 0, aimLength);
			System.gc();
			lastInOutput = false;
		}
		curOperation = 1;
		runMulticore();
		curOperation = 0;

		procLog += "CMD:Dilation :N" + neighborSize + ", @ > "
				+ (1 - neighborOccupancy) * 100 + "% \n";
		lastInOutput = true;
		runCount++;
	}

	@Deprecated
	public void dilate(D3int neighborSizeI, double neighborOccupancyI) {
		neighborSize = neighborSizeI;
		neighborOccupancy = neighborOccupancyI;
		dilate();
	}

	@Deprecated
	public void dilate(int neighborSizeI, double neighborOccupancyI) {
		neighborSize = null;
		neighborSize = new D3int(neighborSizeI);
		neighborOccupancy = neighborOccupancyI;
		dilate();
	}

	@Deprecated
	public void dilateMany(int iterations) {
		for (int i = 0; i < iterations; i++)
			dilate();
	}

	@Deprecated
	public void dilateMany(int iterations, D3int neighborSizeI,
			double neighborOccupancyI) {
		neighborSize = neighborSizeI;
		neighborOccupancy = neighborOccupancyI;
		dilateMany(iterations);
	}

	@Deprecated
	public void dilateMany(int iterations, int neighborSizeI,
			double neighborOccupancyI) {
		neighborSize = null;
		neighborSize = new D3int(neighborSizeI);
		neighborOccupancy = neighborOccupancyI;
		dilateMany(iterations);
	}

	/** Dilate code, not suitable for external use! */
	protected void dilateSection(int bSlice, int tSlice) {
		int cVox = 0;
		int sVox = 0;
		int off = 0;
		double mNeighs = 0.0;
		double mNeighsCnt = 0.0;

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

					outAim[off] = false;
					if (inAim[off])
						cVox++;

					int nCount = 0;
					int nFull = 0;
					int off2;
					boolean runVoxel = !inAim[off]; // only run if the voxel is
													// currently off (otherwise
													// leave it on)
					if (useMask)
						if (!morphMask[off])
							runVoxel = false;
					if (runVoxel) {
						for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(
								z + neighborSize.z, uppz - 1); z2++) {
							for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(
									y + neighborSize.y, uppy - 1); y2++) {
								off2 = (z2 * dim.y + y2) * dim.x
										+ max(x - neighborSize.x, lowx);
								for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(
										x + neighborSize.x, uppx - 1); x2++, off2++) {
									if (curKernel.inside(off, off2, x, x2, y,
											y2, z, z2)) {
										nCount++;
										if (inAim[off2])
											nFull++;
									}
								}

							}
						}
						mNeighs += nCount;
						mNeighsCnt++;
						if (nFull > ((nCount) * (1.0 - neighborOccupancy))) {
							// if the number of filled neighbors is greater than
							// (1-neighborOccupancy) default is if any neighbors
							// are filled
							outAim[off] = true;
						} else
							outAim[off] = false;
					} else
						outAim[off] = inAim[off];
					if (outAim[off])
						sVox++;

				}
			}
		}
		System.out.println("Operation Dilation :N" + neighborSize + "-"
				+ StrRatio(mNeighs, mNeighsCnt) + " @ > "
				+ (1 - neighborOccupancy) * 100 + "%, retained= "
				+ StrPctRatio(sVox, cVox) + "%, " + StrMvx(sVox) + ", "
				+ StrPctRatio(sVox, aimLength) + " Porosity");

	}

	@Deprecated
	public void erode() {

		if (lastInOutput) {
			System.arraycopy(outAim, 0, inAim, 0, aimLength);
			System.gc();
			lastInOutput = false;
		}
		curOperation = -1;
		runMulticore();
		curOperation = 0;

		procLog += "CMD:Erosion :N" + neighborSize + ", >= "
				+ neighborOccupancy * 100 + "% \n";

		lastInOutput = true;
		runCount++;

	}

	@Deprecated
	public void erode(D3int neighborSizeI, double neighborOccupancyI) {
		neighborSize = neighborSizeI;
		neighborOccupancy = neighborOccupancyI;
		erode();
	}

	@Deprecated
	public void erode(int neighborSizeI, double neighborOccupancyI) {
		neighborSize = new D3int(neighborSizeI);
		neighborOccupancy = neighborOccupancyI;
		erode();
	}

	@Deprecated
	public void erodeMany(int iterations) {
		for (int i = 0; i < iterations; i++)
			erode();
	}

	@Deprecated
	public void erodeMany(int iterations, D3int neighborSizeI,
			double neighborOccupancyI) {
		neighborSize = neighborSizeI;
		neighborOccupancy = neighborOccupancyI;
		erodeMany(iterations);
	}

	@Deprecated
	public void erodeMany(int iterations, int neighborSizeI,
			double neighborOccupancyI) {
		neighborSize = new D3int(neighborSizeI);
		neighborOccupancy = neighborOccupancyI;
		erodeMany(iterations);
	}

	/** only erode specific slices */
	protected void erodeSection(int bSlice, int tSlice) {
		int cVox = 0;
		int sVox = 0;
		int off = 0;
		double mNeighs = 0.0;
		double mNeighsCnt = 0.0;

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
					outAim[off] = false;
					boolean runVoxel = inAim[off];
					if (useMask)
						runVoxel = (inAim[off] && morphMask[off]);
					if (runVoxel) {
						cVox++;
						int nCount = 0;
						int nFull = 0;
						int off2;
						for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(
								z + neighborSize.z, uppz - 1); z2++) {
							for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(
									y + neighborSize.y, uppy - 1); y2++) {
								off2 = (z2 * dim.y + y2) * dim.x
										+ max(x - neighborSize.x, lowx);
								for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(
										x + neighborSize.x, uppx - 1); x2++, off2++) {
									if (curKernel.inside(off, off2, x, x2, y,
											y2, z, z2)) {
										if ((off != off2)) {
											nCount++;
											if (inAim[off2])
												nFull++;
										}
									}

								}
							}
						}
						mNeighs += nCount;
						mNeighsCnt++;

						if (nFull >= Math.round((nCount) * neighborOccupancy)) {
							outAim[off] = true;
						} else
							outAim[off] = false;

					} else
						outAim[off] = inAim[off];

					if (outAim[off])
						sVox++;

				}

			}
		}
		System.out.println("Operation Erosion :N" + neighborSize + "-"
				+ StrRatio(mNeighs, mNeighsCnt) + " @ >= " + neighborOccupancy
				* 100 + "%, retained= " + StrPctRatio(sVox, cVox) + ", "
				+ StrMvx(sVox) + ", " + StrPctRatio(sVox, aimLength)
				+ " (%) Porosity \n");
	}

	/**
	 * constructor function taking boolean (other castings just convert the
	 * array first) linear array and the dimensions
	 */
	@Override
	public boolean execute() {
		switch (curOperation) {
		case -1:
			// Erosion
		case 1:
			// Dilation
			return runMulticore();
		case 0:
		default:
			System.out
					.println("Warning : 'Morpho' is not really a plug-in in that sense of the word, but will update the output anyways....");
			if (lastInOutput) {
				System.arraycopy(outAim, 0, inAim, 0, aimLength);
				System.gc();
				lastInOutput = false;
			}
			return false;
		}

	}

	/**
	 * The default action is just to run execute, other features can be
	 * implemented on a case by case basis
	 */
	@Override
	public boolean execute(String curAction) {
		if (curAction.equals("erode"))
			erode();
		else if (curAction.equals("dilate"))
			dilate();
		else if (curAction.equals("open"))
			open();
		else if (curAction.equals("close"))
			close();
		else if (!curAction.equals(""))
			throw new IllegalArgumentException(
					"Execute Does not offer any control in this plugins"
							+ getPluginName());
		return execute();
	}

	/**
	 * The default action is just to run execute, other features can be
	 * implemented on a case by case basis
	 */
	@Override
	public boolean execute(String curAction, Object objectToExecute) {
		final int iters = ((Integer) objectToExecute).intValue();
		if (curAction.equals("erodeMany"))
			erodeMany(iters);
		else if (curAction.equals("dilateMany"))
			dilateMany(iters);
		else if (curAction.equals("openMany"))
			openMany(iters);
		else if (curAction.equals("closeMany"))
			closeMany(iters);
		else if (!curAction.equals(""))
			throw new IllegalArgumentException(
					"Execute Does not offer any control in this plugins"
							+ getPluginName());
		return execute();
	}

	@Override
	public String getPluginName() {
		return "Morpho";
	}

	/** Perform open operation */
	@Deprecated
	public void open() {
		openMany(1);
	}

	/**
	 * Perform open operation using specificed neighbor size of neighborSizeI
	 * (total neighbors = (2*neighborSizeI+1)**3 ) and occupancy
	 * neighborOccupancyI
	 */
	@Deprecated
	public void open(D3int neighborSizeI, double neighborOccupancyI) {
		openMany(1, neighborSizeI, neighborOccupancy);
	}

	/**
	 * Perform open operation using isotropic neighbor size of neighborSizeI
	 * (total neighbors = (2*neighborSizeI+1)**3 ) and occupancy
	 * neighborOccupancyI
	 */
	@Deprecated
	public void open(int neighborSizeI, double neighborOccupancyI) {
		openMany(1, neighborSizeI, neighborOccupancy);
	}

	/** Perform open operation repeatedly iterations times */
	@Deprecated
	public void openMany(int iterations) {
		System.out.println("OpenOperation-" + iterations);
		for (int i = 0; i < iterations; i++) {
			erode();
			dilate();
		}
	}

	/**
	 * Perform open operation repeatedly for iterations using specified neighbor
	 * size of neighborSizeI (total neighbors = (2*neighborSizeI+1)**3 ) and
	 * occupancy neighborOccupancyI
	 */
	@Deprecated
	public void openMany(int iterations, D3int neighborSizeI,
			double neighborOccupancyI) {
		neighborSize = neighborSizeI;
		neighborOccupancy = neighborOccupancyI;
		openMany(iterations);
	}

	/**
	 * Perform open operation repeatedly for iterations using isotropic neighbor
	 * size of neighborSizeI (total neighbors = (2*neighborSizeI+1)**3 ) and
	 * occupancy neighborOccupancyI
	 */
	@Deprecated
	public void openMany(int iterations, int neighborSizeI,
			double neighborOccupancyI) {
		neighborSize = null;
		neighborSize = new D3int(neighborSizeI);
		neighborOccupancy = neighborOccupancyI;
		openMany(iterations);
	}

	@Override
	protected void processWork(Object currentWork) {
		final int[] range = (int[]) currentWork;
		final int bSlice = range[0];
		final int tSlice = range[1];
		switch (curOperation) {
		case -1:
			// Erosion
			erodeSection(bSlice, tSlice);
			return;
		case 1:
			// Dilation
			dilateSection(bSlice, tSlice);
			return;
		case 0:
		default:
			System.err
					.println("Warning : 'Morpho' is being used incorrectly in multicore mode!!: "
							+ curOperation
							+ ", <"
							+ bSlice
							+ ", "
							+ tSlice
							+ ">");
			if (lastInOutput) {
				System.arraycopy(outAim, 0, inAim, 0, aimLength);
				System.gc();
				lastInOutput = false;
			}
		}
	}

	@Override
	@Deprecated
	public void run() {
		execute();
	}

	public ArgumentParser setParameters(ArgumentParser p) {
		return super.setParameter(p, getPluginName() + ":");
	}

}
