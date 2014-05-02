/**

 * 
 */
package tipl.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import tipl.formats.ConcurrentReader;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.ITIPLPlugin;
import tipl.util.TIPLGlobal;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;

/**
 * FractalDimension
 * 
 * @author maderk Fractal dimension implements the method described in
 *         http://www.ncbi.nlm.nih.gov/pubmed/20472327
 * 
 *         The input image is a thresheld image of a structure
 */
public class FractalDimension extends BaseTIPLPluginInExecutor {
	@TIPLPluginManager.PluginInfo(pluginType = "FractalDimension",
			desc="Full memory fractal dimesnion analysis",
			sliceBased=false,
			maximumSize=1024*1024*1024)
	final public static TIPLPluginManager.TIPLPluginFactory myFactory = new TIPLPluginManager.TIPLPluginFactory() {
		@Override
		public ITIPLPlugin get() {
			return new FractalDimension();
		}
	};
	/**
	 * boxScanner scans the number of filled boxes using a given box size in the
	 * image
	 */
	protected static class boxScanner implements Callable<long[]> {
		final ConcurrentReader runImg;
		final D3int runBoxSize;
		final D3int runImgSize;

		public boxScanner(final ConcurrentReader inImg, final D3int inBoxSize) {
			runImg = inImg;
			runBoxSize = inBoxSize;
			runImgSize = inImg.getDim();
		}

		public boxScanner(final ConcurrentReader inImg, final int inBoxSize) {
			runImg = inImg;
			runBoxSize = new D3int(inBoxSize);
			runImgSize = inImg.getDim();
		}

		@Override
		public long[] call() throws InterruptedException, ExecutionException {
			// A big nested mess (loop inside of a loop)
			long filledCount = 0;
			long totalCount = 0;
			long filledCountV = 0;
			long totalCountV = 0;
			System.out.println("Being Called : " + runBoxSize);
			// sz,sy,sx are the starting positions
			int x, y, z;
			for (int sz = 0; sz < runImgSize.z; sz += runBoxSize.z) {
				if ((sz + runBoxSize.z) >= runImgSize.z)
					z = runImgSize.z - runBoxSize.z; // shrink it
				else
					z = sz;
				final List<Future<Object>> cachedSlices = runImg
						.getPolyImageSlices(z, z + runBoxSize.z, 10);
				for (int sy = 0; sy < runImgSize.y; sy += runBoxSize.y) {
					if ((sy + runBoxSize.y) >= runImgSize.y)
						y = runImgSize.y - runBoxSize.y; // shrink it
					else
						y = sy;
					for (int sx = 0; sx < runImgSize.x; sx += runBoxSize.x) {
						if ((sx + runBoxSize.x) >= runImgSize.x)
							x = runImgSize.x - runBoxSize.x; // shrink it
						else
							x = sx;

						// Use a smaller box near the edges (probably the
						// easiest solution)
						final int maxz = min(z + runBoxSize.z, runImgSize.z)
								- z;
						final int maxy = min(y + runBoxSize.y, runImgSize.y)
								- y;
						final int maxx = min(x + runBoxSize.x, runImgSize.x)
								- x;
						final long boxVol = maxz * maxy * maxx;
						if (boxVol > 0) {
							totalCount++;
							totalCountV += boxVol;
							BOX_LOOP: for (int iz = 0; iz < maxz; iz++) {
								// get the element from the list and then get
								// the item from the future
								final boolean[] curSlice = (boolean[]) (cachedSlices
										.get(iz)).get();

								for (int iy = 0; iy < maxy; iy++) {
									int off = (iy + y) * runImgSize.x + x;
									for (int ix = 0; ix < maxx; ix++, off++) {
										if (curSlice[off]) {
											filledCount++;
											filledCountV += boxVol;
											break BOX_LOOP; // no need to keep
															// once we know the
															// box is full
										}
									}
								}
							}
						} else {
							System.out.println("Negative Boxes,WTF?? " + maxx
									+ ", " + maxy + ", " + maxz);
						}
					}
				}
			}
			return new long[] { runBoxSize.x, filledCount, totalCount,
					filledCountV, totalCountV };
		}

	}

	public static void main(final String[] args) {
		final String kVer = "130527_001";
		System.out.println("FractalDimension v" + kVer);
		System.out.println(" Analyzes the fractal dimension of an image");
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final ArgumentParser p = TIPLGlobal.activeParser(args);
		final String inputFile = p.getOptionString("input", "",
				"Input masked image");
		final String outputFile = p.getOptionString("output", "fd_table.csv",
				"Output FractalDimension");
		FractalDimension.boxSizeCutoff = p.getOptionDouble("boxsizecutoff",
				FractalDimension.boxSizeCutoff,
				"Fraction of shortest length to use as volume size");
		p.getOptionBoolean("skippreload", "Dont preload the image data");
		if (p.hasOption("?")) {
			System.out.println(" FractalDimension Demo Help");
			System.out.println(" Arguments::");
			System.out.println(" ");
			System.out.println(p.getHelp());
			System.exit(0);
		}

		if (inputFile.length() > 0) { // Read in labels, if find edge is
										// selected or a mask is given
			System.out.println("Loading " + inputFile + " ...");
			final TImg inputAim = TImgTools.ReadTImg(inputFile);
			/*
			 * if (!NoPreLoad) { inputAim.getBoolAim(); }
			 */

			final FractalDimension myFD = new FractalDimension(inputAim,
					outputFile);
			myFD.execute();
		}
	}

	private ConcurrentReader inImage;
	private D3int inDim;
	private int largestBoxEdge;
	private String outFileName="out.csv";
	public static double boxSizeCutoff = 0.5; // unless it is at least half of
												// the normal volume, throw it
												// out

	final String dlm = ", ";
	protected FractalDimension() {
		
	}
	@Deprecated
	protected FractalDimension(final TImgRO inputImage, final String inOutFileName) {
		LoadImages(new TImgRO[] { inputImage });
		outFileName = inOutFileName;
	}

	@Override
	public boolean execute() {
		return runMulticore();
	}

	@Override
	public String getPluginName() {
		return "FractalDimension";
	}

	/**
	 * Load the Image
	 * 
	 */
	@Override
	public void LoadImages(final TImgRO[] inImages) {
		// TODO Auto-generated method stub
		if (inImages.length < 1)
			throw new IllegalArgumentException(
					"Too few arguments for LoadImages in:" + getPluginName());
		final TImgRO inImg = inImages[0];
		inImage = new ConcurrentReader(inImg);
		inDim = inImg.getDim();
		largestBoxEdge = min(min(inDim.x, inDim.y), inDim.z);
	}

	/**
	 * since we want to use futures and callables to store the results we
	 * override the runMulticore function
	 */
	@Override
	public boolean runMulticore() {
		final LinkedList<Future<long[]>> boxCountList = new LinkedList<Future<long[]>>();
		jStartTime = System.currentTimeMillis();
		FileWriter out = null;
		final int maxIndex = (int) (boxSizeCutoff * largestBoxEdge);
		// for(int curBoxSize=largestBoxEdge;curBoxSize>=1;curBoxSize--) {
		for (int curBoxSize = 1; curBoxSize <= maxIndex; curBoxSize++) {
			boxCountList
					.add(myPool.submit(new boxScanner(inImage, curBoxSize)));
		}
		myPool.shutdown();
		try {
			out = new FileWriter(outFileName, false);
		} catch (final IOException e) {
			System.out.println("Cannot Create File:" + outFileName);
			e.printStackTrace();
		}
		String headerStr = "Voxel Volume" + dlm + "Box Size" + dlm
				+ "Avg Box Voxels" + dlm + "Boxes Filled" + dlm + "Total Boxes"
				+ dlm;
		headerStr += "PercentBoxes" + dlm + "Voxels Filled" + dlm
				+ "Total Voxels" + dlm + "PercentVoxels";
		System.out.println(headerStr);
		try {
			out.write(headerStr + "\n");
		} catch (final IOException e) {
			System.out.println("Cannot Write File:" + outFileName);
			e.printStackTrace();
		}
		for (int curBoxSize = 1; curBoxSize <= maxIndex; curBoxSize++) {
			try {
				final long[] curBoxCount = boxCountList.get(curBoxSize - 1)
						.get();
				String outString = inImage.getElSize().prod() + dlm
						+ curBoxCount[0] + dlm + (1.0 * curBoxCount[4])
						/ curBoxCount[2] + dlm + curBoxCount[1] + dlm;
				outString += curBoxCount[2] + dlm + (100.0 * curBoxCount[1])
						/ curBoxCount[2] + dlm;
				outString += curBoxCount[3] + dlm + curBoxCount[4] + dlm
						+ (100.0 * curBoxCount[3]) / curBoxCount[4];
				System.out.println(outString);
				try {
					out.write(outString + "\n");
				} catch (final IOException e) {
					System.out.println("Cannot Write File:" + outFileName);
					e.printStackTrace();
				}
			} catch (final InterruptedException e) {
				System.out.println(getPluginName()
						+ " was interupted and did not process successfully");
				e.printStackTrace();
				return false;
			} catch (final ExecutionException e) {
				System.out.println(getPluginName()
						+ " did not execute successfully");
				e.printStackTrace();
				return false;
			}
			try {
				out.flush();
			} catch (final IOException e) {
				System.out.println("Cannot Flush File:" + outFileName);
				e.printStackTrace();
			}
		}
		try {
			out.flush();
			out.close();
		} catch (final IOException e) {
			System.out.println("Cannot Close File:" + outFileName);
			e.printStackTrace();
		}
		final String outString = "MCJob Ran in "
				+ StrRatio(System.currentTimeMillis() - jStartTime, 1000)
				+ " seconds in " + neededCores();
		System.out.println(outString);
		procLog += outString + "\n";
		myPool.shutdownNow();
		inImage.close();
		return true;

	}

	// public int neededCores() { return 4;}
	@Override
	public int wantedCores() {
		return largestBoxEdge;
	}
}
