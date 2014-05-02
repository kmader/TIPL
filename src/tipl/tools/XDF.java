package tipl.tools;

import java.util.Random;

import tipl.formats.PureFImage;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.VirtualAim;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.ITIPLPlugin;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;

/**
 * The general class for radial distribution function, and radial
 * cross-correlation function
 */
public class XDF extends BaseTIPLPluginMult {
	@TIPLPluginManager.PluginInfo(pluginType = "XDF",
			desc="Full memory xdf command",
			sliceBased=false)
	final public static TIPLPluginManager.TIPLPluginFactory myFactory = new TIPLPluginManager.TIPLPluginFactory() {
		@Override
		public ITIPLPlugin get() {
			return new XDF();
		}
	};
	public static class radDistFun {
		public D3int len;
		public D3int rlen;
		/** number of elements in rad distribution function **/
		public int eleMax;
		/** counts */
		public int[] n;
		/** mean value */
		public float[] hf;
		/** output */
		public float[] out;
		/** The neighbor size for identifying connected components **/
		public D3int neighborSize = new D3int(1);
		public int MINCNT = 5;

		public float MINCNTTHRESH = 0.25f;
		boolean normError;

		double totalError;
		double errorVoxels;
		double avgCounts;
		double avgValue;
		double errorThreshold;
		int emptyVoxels;
		int nearlyEmptyVoxels = 0;
		int sStep;

		/** ilen is the side of the box in each dimension */
		public radDistFun(D3int ilen) {
			len = new D3int(ilen);
			rlen = new D3int(len.x * 2 + 1, len.y * 2 + 1, len.z * 2 + 1);
			eleMax = (int) rlen.prod();

			n = new int[eleMax];
			hf = new float[eleMax];
			for (int i = 0; i < eleMax; i++) {
				n[i] = 0;
				hf[i] = 0;
			}
			out = new float[eleMax]; // the function values

			errorThreshold = 1e-3;
			totalError = 0;
			errorVoxels = 0;
			normError = false;
			emptyVoxels = 0;

		}

		/** for adding two functions together, useful for parallel programming **/
		public void add(radDistFun oRDF) {
			if (oRDF.eleMax == eleMax) {
				// probably the same size
				for (int i = 0; i < eleMax; i++) {
					n[i] += oRDF.n[i];
					hf[i] += oRDF.hf[i];
				}
			} else
				System.out
						.println(" Cannot Add Different Sized Radial Distribution Functions.. Please");
		}

		public int addpt(int x, int y, int z, boolean val) {
			return addpt(x, y, z, val ? 1.0f : 0.0f);
		}

		public int addpt(int x, int y, int z, char val) {
			return addpt(x, y, z, (float) val);
		}

		public int addpt(int x, int y, int z, float val) {
			final int cDex = index(x, y, z);
			if (cDex >= 0) {
				n[cDex]++;
				hf[cDex] += val;
				return n[cDex];
			} else
				return 0;
		}

		public int addpt(int x, int y, int z, int val) {
			return addpt(x, y, z, (float) val);
		}

		public int addpt(int x, int y, int z, short val) {
			return addpt(x, y, z, (float) val);
		}

		public float[] distmap() {
			final float[] dist = new float[eleMax];
			int i = 0;
			for (float z = -len.z; z <= len.z; z++) {
				for (float y = -len.y; y <= len.y; y++) {
					for (float x = -len.x; x <= len.x; x++, i++) {
						dist[i] = (float) Math.sqrt(x * x + y * y + z * z);
					}
				}
			}

			return dist;
		}

		public TImg ExportAim(TImg.CanExport templateAim) {
			final TImg outAim = templateAim.inheritedAim(rdf(), rlen,
					new D3int(0));
			outAim.setPos(new D3int(-len.x, -len.y, -len.z));
			return outAim;
		}

		/**
		 * Function to fit a shape tensor (from the shape analysis) to the
		 * distribution function
		 **/
		public GrayVoxels FitTensor() {
			int i = 0;
			final GrayVoxels gv = new GrayVoxels(0.0);
			gv.useWeights = true;
			for (int z = -len.z; z <= len.z; z++) {
				for (int y = -len.y; y <= len.y; y++) {
					for (int x = -len.x; x <= len.x; x++, i++) {
						gv.addVox(x, y, z, out[i]);
					}
				}
			}
			i = 0;
			for (int z = -len.z; z <= len.z; z++) {
				for (int y = -len.y; y <= len.y; y++) {
					for (int x = -len.x; x <= len.x; x++, i++) {
						gv.addCovVox(x, y, z, out[i]);
					}
				}
			}

			return gv;

		}

		public GrayVoxels FitTensor(float threshold) {
			int i = 0;
			final GrayVoxels gv = new GrayVoxels(0.0);
			gv.useWeights = true;
			for (int z = -len.z; z <= len.z; z++) {
				for (int y = -len.y; y <= len.y; y++) {
					for (int x = -len.x; x <= len.x; x++, i++) {
						if (out[i] > threshold)
							gv.addVox(x, y, z, threshold);
					}
				}
			}
			i = 0;
			for (int z = -len.z; z <= len.z; z++) {
				for (int y = -len.y; y <= len.y; y++) {
					for (int x = -len.x; x <= len.x; x++, i++) {
						if (out[i] > threshold)
							gv.addCovVox(x, y, z, threshold);
					}
				}
			}

			return gv;

		}

		/** not used yet but would allow for much bigger xdf functions **/
		public D3int fixSize(D3int trySize, int iStep) {
			final D3int oSize = new D3int(0);
			oSize.x = Math.round(trySize.x / iStep) * iStep;
			oSize.y = Math.round(trySize.y / iStep) * iStep;
			oSize.z = Math.round(trySize.z / iStep) * iStep;
			if ((oSize.x != trySize.x) | (oSize.y != trySize.y)
					| (oSize.z != trySize.z))
				System.out.println("Resized Neighborhood : " + trySize + " -> "
						+ oSize + " (" + iStep + ")");

			return trySize;
		}

		public float frdf() {

			avgCounts = 0;
			avgValue = 0;
			for (int i = 0; i < eleMax; i++) {
				avgCounts += n[i];
				avgValue += hf[i];
			}
			avgValue /= avgCounts;

			totalError = 0;
			errorVoxels = 0;
			nearlyEmptyVoxels = 0;
			emptyVoxels = 0;
			for (int i = 0; i < eleMax; i++) {
				if (n[i] > MINCNT) {
					float cVal;
					if (n[i] < (avgCounts * MINCNTTHRESH / eleMax)) {
						cVal = 0;
						nearlyEmptyVoxels++;
					} else {
						cVal = (hf[i]) / (n[i] + 0.0f);
					}

					final double cError = (cVal - out[i]) * (cVal - out[i]);
					totalError += cError * n[i];
					if (normError)
						if ((Math.sqrt(cError) / cVal) > errorThreshold)
							errorVoxels++;
						else if ((Math.sqrt(cError)) > errorThreshold)
							errorVoxels++;
					out[i] = cVal;
				} else
					emptyVoxels++;
			}

			return (float) Math.sqrt(totalError / avgCounts);
		}

		int index(int x, int y, int z) {
			if ((Math.abs(x) <= len.x) && (Math.abs(y) <= len.y)
					&& (Math.abs(z) <= len.z)) {
				int xi, yi, zi;
				xi = x + len.x;
				yi = y + len.y;
				zi = z + len.z;
				return (zi * rlen.y + yi) * rlen.x + xi;

			} else
				return -1;
		}

		public float[] phimap() {
			final float[] phi = new float[eleMax];
			int i = 0;
			for (float z = -len.z; z <= len.z; z++) {
				for (float y = -len.y; y <= len.y; y++) {
					for (float x = -len.x; x <= len.x; x++, i++) {
						phi[i] = (float) Math.atan2(y, x);
					}
				}
			}

			return phi;
		}

		public String PluginName() {
			return "_XDF";
		}

		/** two point tensor from all the points added to the radDistFun */
		public GrayVoxels PosTensor() {
			int i = 0;
			final GrayVoxels gv = new GrayVoxels(0.0);
			gv.useWeights = true;
			gv.noRecenter = true;
			for (int z = -len.z; z <= len.z; z++) {
				for (int y = -len.y; y <= len.y; y++) {
					for (int x = -len.x; x <= len.x; x++, i++) {
						gv.addVox(x, y, z, n[i]);
					}
				}
			}
			return gv;
		}

		public void print() {
			int i = 0;
			for (int z = -len.z; z <= len.z; z++) {
				for (int y = -len.y; y <= len.y; y++) {
					for (int x = -len.x; x <= len.x; x++, i++) {
						System.out.println(x + "," + y + "," + z + "::"
								+ out[i] + " " + n[i] + "," + hf[i]);

					}
				}
			}
		}

		/**
		 * Removes gaps in the XDF function (everything above or below points
		 * which are disconnected), basically removing points where a solid line
		 * of voxels does not connect (0,0,0) to (x,y,z)
		 **/
		public int pruneGaps() {
			final boolean[] keepvox = new boolean[n.length];
			keepvox[index(0, 0, 0)] = true;
			boolean changes = true;
			while (changes) {
				changes = false;
				int i = 0;
				for (int z = -len.z; z <= len.z; z++) {
					for (int y = -len.y; y <= len.y; y++) {
						for (int x = -len.x; x <= len.x; x++, i++) {
							if ((!keepvox[i]) && (n[i] > 0)) {
								if (subscan(keepvox, x, y, z)) {
									keepvox[i] = true;
									changes = true;
								}
							}
						}
					}
				}
			}
			for (int i = 0; i < n.length; i++) {
				if (!keepvox[i]) {
					n[i] = 0;
					hf[i] = 0;
				}
			}
			return 1;
		}

		public int pruneGapsCL() {
			final boolean[] nmask = new boolean[n.length];
			for (int i = 0; i < n.length; i++)
				nmask[i] = n[i] > 0;

			final ComponentLabel myCL = new ComponentLabel(nmask, rlen,
					new D3int(0));
			myCL.verboseMode = false;
			myCL.execute();
			final int keepLabel = myCL.labels[index(0, 0, 0)];
			for (int i = 0; i < n.length; i++) {
				if (myCL.labels[i] != keepLabel) {
					n[i] = 0;
					hf[i] = 0;
				}
			}
			return keepLabel;
		}

		public int pruneGapsJunk() {
			// Assume all voxels are valid
			int minX = -len.x, maxX = len.x, minY = -len.y, maxY = len.y, minZ = -len.z, maxZ = len.z;

			int i = 0;
			for (int z = -len.z; z <= len.z; z++) {
				for (int y = -len.y; y <= len.y; y++) {
					for (int x = -len.x; x <= len.x; x++, i++) {
						if (n[i] < 1) {
							// If a voxel is empty than the box

							if (x > 0)
								if (x < maxX)
									maxX = x;
								else if (x > minX)
									minX = x;

							if (y > 0)
								if (y < maxY)
									maxY = y;
								else if (y > minY)
									minY = y;

							if (z > 0)
								if (z < maxZ)
									maxZ = z;
								else if (z > minZ)
									minZ = z;

						}
					}
				}
			}
			int trimCount = 0;

			i = 0;
			for (int z = -len.z; z <= len.z; z++) {
				for (int y = -len.y; y <= len.y; y++) {

					for (int x = -len.x; x <= len.x; x++, i++) {
						if (n[i] > 0) {
							// If a voxel is empty than all
							boolean zeroOut = false;
							if (x > maxX)
								zeroOut = true;
							if (x < minX)
								zeroOut = true;

							if (y > maxY)
								zeroOut = true;
							if (y < minY)
								zeroOut = true;

							if (z > maxZ)
								zeroOut = true;
							if (z < minZ)
								zeroOut = true;

							if (zeroOut) {
								n[i] = 0;
								hf[i] = 0;
								trimCount++;
							}

						}
					}
				}
			}

			return trimCount;

		}

		public float[] rdf() {
			frdf();
			return out;
		}

		/** are there any keep voxels nearby? **/
		public boolean subscan(boolean[] keepvox, int x, int y, int z) {
			for (int z2 = max(z - neighborSize.z, -len.z); z2 <= min(z
					+ neighborSize.z, len.z); z2++) {
				for (int y2 = max(y - neighborSize.y, -len.y); y2 <= min(y
						+ neighborSize.y, len.y); y2++) {
					final int stx = max(x - neighborSize.x, -len.x);
					int off2 = index(z2, y2, stx);
					for (int x2 = stx; x2 <= min(x + neighborSize.x, len.x); x2++, off2++) {
						if (keepvox[off2])
							return true;
					}
				}
			}
			return false;
		}

		public float[] thetamap() {
			final float[] theta = new float[eleMax];
			int i = 0;
			for (float z = -len.z; z <= len.z; z++) {
				for (float y = -len.y; y <= len.y; y++) {
					for (float x = -len.x; x <= len.x; x++, i++) {
						theta[i] = (float) Math.atan2(z,
								Math.sqrt(x * x + y * y + z * z));
					}
				}
			}

			return theta;
		}

		@Override
		public String toString() {
			frdf();
			return "LcV:"
					+ String.format("%.2f",
							Math.sqrt(totalError / eleMax) * 100)
					+ ", LcVx:"
					+ String.format("%.2f",
							Math.sqrt(errorVoxels / eleMax) * 100)
					+ ", EmptyVx:"
					+ String.format("%.2f",
							(emptyVoxels / (eleMax + 0.0f)) * 100.0f)
					+ ", Avg#:"
					+ String.format("%.2f", (avgCounts + 0.0f) / eleMax)
					+ ", NearlyEmpty#:"
					+ String.format("%.2f", (nearlyEmptyVoxels + 0.0f) / eleMax
							* 100f);
		}
	}

	private static class xdfScanner implements Runnable {
		volatile XDF parent;
		public boolean isFinished = false; // needs a new position
		private int ix, iy, iz;
		public radDistFun crdf = null;
		public final int inputType;
		public long runningTime;
		public long iters;
		public Thread myThread = null;
		public final String baseName;
		public String curName;
		protected BaseTIPLPluginIn.stationaryKernel curKernel;
		protected final D3int dim;
		protected final int lowx, uppx, lowy, uppy, lowz, uppz;
		protected final D3int neighborSize;
		protected final boolean doNormalizeFloat;
		/**
		 * 
		 * @param iparent name of the parent class
		 * @param iinputType input type (for analysis to perform later)
		 * @param core which core is being used
		 * @param normalizeFloat should the float values be normalized (default is false)
		 */
		public xdfScanner(XDF iparent, int iinputType, int core,boolean normalizeFloat) {
			baseName = ("xdfScanner[" + core + "]:<>");
			curName = baseName;
			isFinished = true;
			parent = iparent;
			inputType = iinputType;
			runningTime = 0;
			iters = 0;
			doNormalizeFloat=normalizeFloat;
			// System.err.println("Running Cross-Correlation Function, "+curIter+", "+Thread.currentThread());

			if (parent.neighborKernel == null)
				curKernel = new BaseTIPLPluginIn.stationaryKernel();
			else
				curKernel = new BaseTIPLPluginIn.stationaryKernel(
						parent.neighborKernel);
			neighborSize = parent.neighborSize;
			if (crdf == null)
				crdf = new radDistFun(neighborSize);

			dim = parent.dim;
			lowx = parent.lowx;
			uppx = parent.uppx;
			lowy = parent.lowy;
			uppy = parent.uppy;
			lowz = parent.lowz;
			uppz = parent.uppz;

		}

		public void join() {
			if (myThread == null) {
				System.out.println("Thread does not exist!");
				return;
			}
			try {
				myThread.join(); // wait until thread has finished
			} catch (final InterruptedException e) {
				System.out.println("ERROR - Thread : " + this
						+ " was interrupted, proceed carefully!");
			}
		}

		public void newPosition(int x, int y, int z) {
			ix = x;
			iy = y;
			iz = z;
			curName = baseName + " <" + x + "," + y + "," + z + ">";
		}

		@Override
		public void run() {
			isFinished = false;
			final long ist = System.currentTimeMillis();
			try {
				switch (inputType) {
				case 3:
					runFloatSub(ix, iy, iz);
					break;

				case 0:
				case 1:
				case 2:
				case 10:
					runLabeledSub(ix, iy, iz);
					break;
				}
			} catch (final Exception e) {
				System.out.println("ERROR - Thread : " + this
						+ " has crashed, proceed carefully!");
				e.printStackTrace();
			}
			iters++;
			isFinished = true;
			runningTime += (System.currentTimeMillis() - ist);

		}

		protected void runFloatSub(int x, int y, int z) {
			final boolean useMask=parent.hasMask;
			final int off = (z * dim.y + y) * dim.x + x;
			int off2;
			for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(z
					+ neighborSize.z, uppz - 1); z2 += parent.xStep) {
				for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(y
						+ neighborSize.y, uppy - 1); y2 += parent.xStep) {
					off2 = (z2 * dim.y + y2) * dim.x
							+ max(x - neighborSize.x, lowx);
					for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(x
							+ neighborSize.x, uppx - 1); x2 += parent.xStep, off2++) {
						boolean validInside = true;
						if (useMask)
							validInside = ((parent.xdfMask[off2]));
						if (validInside) {
							if (curKernel
									.inside(off, off2, x, x2, y, y2, z, z2)) {
								
								float outValue=parent.inAimFloat[off2];
								if (doNormalizeFloat) 
									outValue = (outValue - parent.meanVal)
										* (outValue - parent.meanVal)
										/ parent.varVal;
								
								crdf.addpt(
										x2 - x,
										y2 - y,
										z2 - z,
										outValue);
							}
								
						}
					}
				}
			}
		}

		/** for labeled/segmented images **/
		protected void runLabeledSub(int x, int y, int z) {
			final boolean useMask=parent.hasMask;
			final int off = (z * parent.dim.y + y) * parent.dim.x + x;
			int off2;
			// Store just the current iteration (allows pruning of disconnected
			// branches for MIL tensor generation)
			radDistFun singleRdf = null;
			if (parent.milMode)
				singleRdf = new radDistFun(neighborSize);
			else
				singleRdf = crdf;

			for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(z
					+ neighborSize.z, uppz - 1); z2 += parent.xStep) {
				for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(y
						+ neighborSize.y, uppy - 1); y2 += parent.xStep) {
					off2 = (z2 * dim.y + y2) * dim.x
							+ max(x - neighborSize.x, lowx);
					for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(x
							+ neighborSize.x, uppx - 1); x2 += parent.xStep, off2++) {
						boolean validInside = true;
						if (useMask)
							validInside = ((parent.xdfMask[off2]));
						
						if (validInside) {
							boolean pointMatch = false;
							switch (inputType) {
							case 0:
								pointMatch = (parent.inAimByte[off2] == parent.outPhase);
								break;
							case 1:
								pointMatch = (parent.inAimShort[off2] == parent.outPhase);
								break;
							case 2:
								pointMatch = (parent.inAimInt[off2] == parent.outPhase);
								break;
							case 10:
								pointMatch = parent.inAimMask[off2];
								break;
							}

							if (curKernel
									.inside(off, off2, x, x2, y, y2, z, z2))
								singleRdf.addpt(x2 - x, y2 - y, z2 - z,
										pointMatch);

						}
					}
				}
			}

			if (parent.milMode) {
				singleRdf.pruneGaps();
				crdf.add(singleRdf);
				singleRdf = null;
			}

		}

		public Thread spawn() {
			myThread = new Thread(this);
			isFinished = false;
			myThread.start();
			return myThread;
		}

		public Thread spawn(int x, int y, int z) {
			newPosition(x, y, z);
			return spawn();
		}

		@Override
		public String toString() {
			return curName;
		}
	}
	/** type for input image (-1 means automatic) **/
	protected int inputType=-1; 
	/** size of the image to use **/
	protected D3int rdfSize=new D3int(20,20,20);
	public void setSize(D3int newSize) {rdfSize=newSize;}
	radDistFun rdf;
	/** should a mask be used in the image */
	protected boolean hasMask;
	/** type for starting image (-1 means automatic) **/
	protected int startImageType=-1;
	/** the value image is the image used for comparison **/
	protected boolean hasValueImage;
	/** type for value image (-1 means automatic) **/
	protected int valueImageType=-1;
	
	protected TImgRO internalImageReference=null;
	protected TImgRO internalValueImageReference=null;
	
	/** calculate the surface voxels from the image **/
	public boolean useSurface;
	/** xdfMask is the background mask */
	boolean[] xdfMask = null;

	/**
	 * when looking at segmented data outphase is the value of the starting
	 * phase
	 **/
	public int inPhase = -1;
	/**
	 * when looking at segmented data outphase is the value of the comparison
	 * phase
	 **/
	public int outPhase = -1;
	/**
	 * Run the problem as mean intercept length (no gaps between objects allowed
	 * when calculating XDF
	 **/
	public boolean milMode = false;
	/** scan every voxel in the image (true) or just a random sampling (false) **/
	public boolean fullScan = false;

	/** when running fullscan how many voxels to skip over **/
	public D3int skipFactor = new D3int(5);

	Random rgen;
	public float MAXPROB = 1.0f;
	public float MAXVAL = 1000f;
	public float tensorCutOff = 0.5f;
	final int MAXDISTVAL = 32765;

	final int OUTERSHELL = 0;
	public int mcIter=5000;
	private int curIter;
	private int missedIter;
	public float meanVal = 0f;

	public float varVal = 0f;
	public boolean normalizeFloat=false;
	public boolean isSigned = false;
	public final boolean supportsThreading = true;
	public int xStep = 1; // x++ replaced with x+=xdfStep
	int vCount = 0;

	public D3int surfNeighborSize = new D3int(1); // Surface Neighborhood Size

	protected int printStep = 1000;
	int remVoxels = aimLength;
	int totalVoxels = aimLength;
	BaseTIPLPluginIn.stationaryKernel curKernel;
	/**
	 * The command line executable version of the code The code run in the main
	 * function looks like this
	 * 
	 * <pre>
	 *      <p> Read in the inputFile as an VirtualAim
	 *      <li>VirtualAim inputAim=TImgTools.ReadTImg(inputFile);
	 *      <p> Create a new XDF object with the input file, using float (3) type, and a (2*rdfs+1) x (2*rdfs+1) x (2*rdfs+1) sized space ranging from -rdfs to +rdfs in x,y, and z
	 *      <li>XDF cXDF=new XDF(inputAim,3,new D3int(rdfs,rdfs,rdfs));
	 *      <li>System.out.println("Calculating XDF "+inputFile+" ...");
	 *      <p> Run the XDF analysis with rdfIter iterations
	 *      <li>cXDF.run(rdfIter);
	 *      <p> Save the result using the inputAim as a template into the new aim outputAim
	 *      VirtualAim outputAim=cXDF.ExportAim(inputAim);
	 *      <p> Write the output aim as a short to the dist using the probability scalar from XDF
	 *      <li>outputAim.WriteAim(outputFile,1,(float) cXDF.probScalar());
	 * </pre>
	 */
	public static final String kVer = "131021_015";

	/**
	 * function for creating an two point correlation using a labeled image and
	 * a mask image
	 **/
	public static XDF CreateLabeledXDF(VirtualAim labImg, VirtualAim maskImg,
			D3int rdfS, int inPhase, int outPhase) {
		final XDF cXDF = new XDF();
		cXDF.setParameter("-rdfsize="+rdfS+", -inphase="+inPhase+", -outphase="+outPhase+" -asint");
		cXDF.LoadImages(new TImgRO[] {labImg,maskImg});
		return cXDF;
	}
	@Override
	public ArgumentParser setParameter(ArgumentParser p,String cPrefix) {
		
		int rdfs = p.getOptionInt(cPrefix + "rdfs", -1, "RDF Size (-1 leaves the value as default)");
		if (rdfs>0) rdfSize=new D3int(rdfs); // if it is greater than 0 recreate it
		rdfSize = p.getOptionD3int(cPrefix + "rdfsize", rdfSize,
				"RDF Size");
		mcIter = p.getOptionInt(cPrefix + "iter", mcIter, "iteration count for correlation function");
		tensorCutOff = (float) p.getOptionDouble(cPrefix + "tensorthresh",
				tensorCutOff, "Threshold for RDF tensor");
		final boolean asFloat = p.getOptionBoolean(cPrefix + "asfloat",
				"Load data as float");
		final boolean asInt = p.getOptionBoolean(cPrefix + "asint",
				"Load data as Int (specify inPhase and outPhase)");
		final boolean asNative = p.getOptionBoolean(cPrefix + "asnative",
				"Load data as native type");
		
		if (asFloat) {
			startImageType = 3;
		} else if (asInt) {
			startImageType = 2;
			
		} else if (asNative) {
			startImageType=-1;
		} else {
			startImageType = 10;
		}
		inPhase = p.getOptionInt(cPrefix + "inphase", inPhase, 
				"Input phase to use as starting points for the analysis (only for asint)");
		outPhase = p.getOptionInt(cPrefix + "outphase", outPhase,
				"Out phase to use as landing points for the analysis (only for asint)");
		
		useSurface = p.getOptionBoolean(cPrefix + "usesurface",
				"Calculate from object surface");
		
		valueImageType = p.getOptionInt(cPrefix + "valueImageType", valueImageType, "Type for value image (-1 is native, "+TImgTools.IMAGETYPE_HELP);
		
		normalizeFloat = p.getOptionBoolean(cPrefix + "normalizefloat","Normalize the floating point values (mean subtracted and divided by sqrt of variance)");
		
		fullScan = p.getOptionBoolean(cPrefix + "fullscan",
				"Scan the entire image");
		skipFactor = p.getOptionD3int(cPrefix + "skipfactor", skipFactor,
				"Skip factor");
		
		return p;
	}
	

	public static TImg WriteHistograms(XDF cXDF, TImgRO.CanExport inAim,
			String outfile) {
		final TImg outAim = cXDF.ExportAim(inAim);
		GrayAnalysis.StartRProfile(outAim, outfile + "_r.txt", 0.0f);
		GrayAnalysis.StartZProfile(outAim, outfile + "_z.txt", 0.0f);
		GrayAnalysis.StartRCylProfile(outAim, outfile + "_rcyl.txt", 0.0f);
		GrayAnalysis.StartFProfile(outAim,
				new PureFImage.PhiImageSph(outAim, 3), outfile + "_rphi.txt",
				0.0f);
		GrayAnalysis.StartFProfile(outAim, new PureFImage.ThetaImageCyl(outAim,
				3), outfile + "_thcyl.txt", 0.0f);

		return outAim;
	}

	
	public XDF() {
		hasMask=false;
	}

	
	@Override
	public void LoadImages(TImgRO[] tinImages) {
		TImgRO[] inImages=TImgTools.fillListWithNull(tinImages, 3);

		if (inImages[0]==null)
			throw new IllegalArgumentException("Too few input images given!");

		internalImageReference = inImages[0];

		if (startImageType<0) startImageType = internalImageReference.getImageType();


		if (inImages[1]!=null) { // sometimes an empty or invalid mask is inserted
			System.out.println("Evidently a mask is also present, so it will be used:"+inImages[1]);
			hasMask=true;
			xdfMask = TImgTools.makeTImgFullReadable(inImages[1]).getBoolAim();
		} else {
			hasMask=false;
		}
		if (inImages[2]!=null) {
			System.out.println("Evidently a gray / second phase images is present:"+inImages[2]);
			hasValueImage=true;
			internalValueImageReference=inImages[2];
		} else {
			hasValueImage=false;
		}
	}
	
	protected synchronized void addToResult(radDistFun crdf) {
		rdf.add(crdf);
	}
	
	protected boolean checkNeighborSurface(final int x,final int y,final int z,final int off, final boolean[] inStartMask) {
		final int cImgTyp = inputType;
		for (int z2 = max(z - surfNeighborSize.z, lowz); z2 <= min(z
				+ surfNeighborSize.z, uppz - 1); z2++) {
			for (int y2 = max(y - surfNeighborSize.y, lowy); y2 <= min(y
					+ surfNeighborSize.y, uppy - 1); y2++) {
				int off2 = (z2 * dim.y + y2) * dim.x
						+ max(x - neighborSize.x, lowx);
				for (int x2 = max(x - surfNeighborSize.x, lowx); x2 <= min(x
						+ surfNeighborSize.x, uppx - 1); x2++, off2++) {
					if (curKernel.inside(off, off2, x, x2, y, y2, z, z2)) {
						return !inStartMask[off2];
						
					}
				}
			}
		}
		return false;
	}

	public int[] countImage() {

		procLog += "CMD:XDF :N" + neighborSize + "\n";
		System.out.println("CMD:XDF :N" + neighborSize);
		return new int[2];

	}
	/**
	 * An mask with all the valid starting locations based on the input type and selected phase
	 * @return
	 */
	protected boolean[] prepareStartMask(final int cImgTyp) {
		long totVoxels=0,allVoxels=0;
		boolean[] outImage= new boolean[aimLength];
		for (int z = lowz; z < (uppz); z++) {
			for (int y = (lowy); y < (uppy); y++) {
				int off = (z * dim.y + y) * dim.x + lowx;
				for (int x = (lowx); x < (uppx); x++, off++) {
					boolean validStart = true;
					switch (cImgTyp) {
					case 0:
						validStart = (inAimByte[off] == inPhase);
						break;
					case 1:
						validStart = (inAimShort[off] == inPhase);
						break;
					case 2:
						validStart = (inAimInt[off] == inPhase);
						break;
					case 3:
						validStart = (inAimFloat[off]>0.0f);
						break;
					case 10:
						validStart = inAimMask[off];
						break;
					}
					outImage[off]=validStart;
					if (validStart) totVoxels++;
					allVoxels++;
				}
			}
		}
		System.out.println("Starting Image Calculation finished (IM:"+cImgTyp+",IP:"+inPhase+") : "
				+ StrPctRatio(totVoxels, allVoxels) + " , " + StrMvx(totVoxels)
				+ " vox");
		return outImage;
	}
	/**
	 * prepares the surface mask to be used for the rest of the analysis
	 */
	protected boolean[] prepareSurfaceMask(final boolean[] inStartMask) {
		
		boolean[] xdfSurfMask = new boolean[inStartMask.length];
		if (neighborKernel == null)
			curKernel = new BaseTIPLPluginIn.stationaryKernel();
		else
			curKernel = new BaseTIPLPluginIn.stationaryKernel(
					neighborKernel);
		System.out.println("Calculating Surface Voxels ... (" + inPhase
				+ ")");
		long totVox = 0;
		long surfVox = 0;
		if (xdfMask==null) {
			xdfMask=new boolean[inStartMask.length];
			for(int i=0;i<xdfMask.length;i++) xdfMask[i]=true;
		}
		hasMask=true;
		for (int z = lowz; z < (uppz); z++) {
			for (int y = (lowy); y < (uppy); y++) {
				int off = (z * dim.y + y) * dim.x + lowx;
				for (int x = (lowx); x < (uppx); x++, off++) {
					if (inStartMask[off]) {
						totVox++;
						boolean outValue = checkNeighborSurface(x, y, z,
								off,inStartMask);
						xdfSurfMask[off]=outValue;
						if (!outValue) xdfMask[off]=false; // remove points from the mask that don't belong
						if (outValue) surfVox++;
					}
				}
			}

		}
		System.out.println("Surface Calculation finished  : "
				+ StrPctRatio(surfVox, totVox) + " , " + StrMvx(surfVox)
				+ " vox");
		return xdfSurfMask;
	}
	/**
	 * run a full scan using the following scanners, masks, and limited starting positions
	 * @param bfArray
	 * @param useMask
	 * @param xdfStartMask which points are acceptable starting points for the analysis
	 */
	protected void runFullScan(final xdfScanner[] bfArray,final boolean useMask, final boolean[] xdfStartMask) {
		int curCore = 0;
		mcIter = (int) (vCount / skipFactor.prod());
		for (int z = lowz + OUTERSHELL; z < (uppz - OUTERSHELL); z += skipFactor.z) {
			for (int y = (lowy + OUTERSHELL); y < (uppy - OUTERSHELL); y += skipFactor.y) {
				int off = (z * dim.y + y) * dim.x + lowx + OUTERSHELL;
				for (int x = (lowx + OUTERSHELL); x < (uppx - OUTERSHELL); x += skipFactor.x, off += skipFactor.x) {
					if (xdfStartMask[off]) {

						curCore = 0; // always start at 0
						while (!isCoreFree(bfArray, curCore)) {
							curCore++;
							if (curCore >= neededCores()) {
								curCore = 0;
								Thread.yield();
							}
						}
						bfArray[curCore].spawn(x, y, z);

						curIter++;
						if ((curIter) % printStep == 0) {

							System.out.println(curCore + ":"
									+ StrPctRatio(off, dim.prod()) + " - "
									+ toString(bfArray[curCore].crdf));
						}
					}
				}
			}
		}
	}
	protected void runIterativeScan(xdfScanner[] bfArray,final boolean useMask,final boolean[] xdfStartMask) {
		int curCore = 0;
		for (curIter = 0; curIter < mcIter; curIter++) {
			boolean validStart = false;
			int x = 0, y = 0, z = 0, noff = 0;

			while (!validStart) {
				x = rgen.nextInt(uppx - lowx - 2 * OUTERSHELL) + lowx
						+ OUTERSHELL;
				y = rgen.nextInt(uppy - lowy - 2 * OUTERSHELL) + lowy
						+ OUTERSHELL;
				z = rgen.nextInt(uppz - lowz - 2 * OUTERSHELL) + lowz
						+ OUTERSHELL;
				noff = (z * dim.y + y) * dim.x + x;
				validStart = xdfStartMask[noff];
			}
			curCore = 0; // always start at 0
			while (!isCoreFree(bfArray, curCore)) {
				curCore++;
				if (curCore >= neededCores()) {
					curCore = 0;
					Thread.yield();
				}
			}
			bfArray[curCore].spawn(x, y, z);

			if ((curIter) % printStep == 0)
				System.out.println(curCore + ":"
						+ toString(bfArray[curCore].crdf));
			else if (((missedIter + 1) % (5 * printStep)) == 0) {
				System.out.println(curCore + ": M" + missedIter + "-"
						+ toString(bfArray[curCore].crdf));
			}
		}
	}
	@Override
	public boolean execute() {
		
		
		// Setup the XDF Code
		InitRDF(rdfSize);
		
		
		jStartTime = System.currentTimeMillis();
		// first import the basic image (sloppy)
		inputType=startImageType;
		System.out.println("Loading Start Image:"+internalImageReference+" of type:"+startImageType);
		ImportAim(internalImageReference,startImageType);
		boolean[] xdfStartMask=prepareStartMask(startImageType); // now find the right values in the image
		
		if (startImageType == 3)
			useSurface = false;
		/*
		 * if the surface is needed pluck it from the start image
		 */
		if (useSurface) xdfStartMask=prepareSurfaceMask(xdfStartMask);
		
		// if there is a mask only keep the points which are inside it (boolean and)
		if (hasMask) for(int ii=0;ii<xdfStartMask.length;ii++) xdfStartMask[ii]&=xdfMask[ii];
		
		/*
		 * if there is a value image now load that since the starting values 
		 * have been stripped from the internal variables
		 */ 
		if (hasValueImage) {
			System.out.println("Loading Value Image:"+internalValueImageReference+" of type:"+valueImageType);
			inputType=valueImageType; // set the input to the value image
			ImportAim(internalValueImageReference,valueImageType); 
		} else {
			valueImageType=startImageType;
		}
		
		
		printStep = (new Integer(mcIter / 10)).intValue();

		
		final xdfScanner[] bfArray = new xdfScanner[neededCores()];

		if (hasMask) {
			if (aimLength != xdfMask.length) {
				System.out.println("SIZES DO NOT MATCH  !!!!!!!!");
				return false;
			}
		}
		// Wind up
		for (int i = 0; i < neededCores(); i++) {
			bfArray[i] = new xdfScanner(this, valueImageType, i,normalizeFloat);
		}
		curIter = 0;
		
		System.out.println("Calculating XDF ...Type:("+inputType+"), Phase:(" + outPhase + ")");
		if (fullScan) {
			runFullScan(bfArray,hasMask,xdfStartMask);
		} else { // Iterative Approach
			runIterativeScan(bfArray,hasMask,xdfStartMask);
		}

		// Wind down
		for (int i = 0; i < neededCores(); i++) {
			if (bfArray[i] != null) {
				bfArray[i].join(); // pseudo-join
				addToResult(bfArray[i].crdf);
				System.out.println(bfArray[i] + " finished in : <"
						+ StrRatio(bfArray[i].runningTime, 1000) + "s, "
						+ bfArray[i].iters + " iters, "
						+ StrRatio(bfArray[i].runningTime, bfArray[i].iters)
						+ " ms/iter>");
			}
		}

		final String outString = "XDF: Ran in "
				+ StrRatio(System.currentTimeMillis() - jStartTime, 1000)
				+ " seconds on " + neededCores() + " cores: "
				+ StrRatio(System.currentTimeMillis() - jStartTime, curIter)
				+ " ms/iter";
		System.out.println(outString);
		procLog += outString + "\n";

		procLog += "XDF : " + toString() + "\n";
		if (milMode)
			procLog += "MIL Mode Enabled\n";
		if (inputType < 3)
			procLog += "Two Phase Analysis:(" + inPhase + ", " + outPhase
					+ ")\n";
		System.out.println(toString());

		// Position Tensor
		String curLine = " Position Tensor\n";
		GrayVoxels gv = rdf.PosTensor();
		curLine += gv.diag(true);
		procLog += curLine;
		System.out.println(curLine);

		// Value Tensor / Probabilitity Tensor (for bw)
		curLine = " Value (Probability) Tensor\n";
		gv = rdf.FitTensor();
		curLine += gv.diag(true);
		procLog += curLine;
		System.out.println(curLine);

		// Thresheld Probability Tensor
		curLine = " Threshold " + tensorCutOff + " vs " + meanVal + "\n";
		gv = rdf.FitTensor(tensorCutOff);
		curLine += gv.diag(true);
		procLog += curLine;
		System.out.println(curLine);

		runCount++;
		return true;
	}

	@Override
	public TImg ExportAim(TImg.CanExport templateAim) {
		if (isInitialized) {
			if (runCount > 0) {
				final TImg tempAim = rdf.ExportAim(templateAim);// .inheritedAim(rdf.rdf(),rdf.rlen,new
																// D3int(0));
				tempAim.appendProcLog(procLog);
				return tempAim;

			} else {
				System.err
						.println("The plug-in : "
								+ getPluginName()
								+ ", has not yet been run, exported does not exactly make sense, original data will be sent.");
				return null;
			}
		} else {
			System.err
					.println("The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exported does not make any sense");
			return null;

		}
	}

	@Override
	public String getPluginName() {
		return "XDF";
	}

	@Override
	protected void InitByte() {
		final boolean useMask=hasMask;
		vCount = 0;
		meanVal = 0.0f;
		varVal = 0.0f;
		int off = 0;
		isSigned = true;
		for (int z = lowz; z < uppz; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
					boolean validStart = true; // (inPhase==inAimByte[off]);
					if (useMask)
						validStart = xdfMask[off];
					if (validStart) {
						meanVal += inAimByte[off];
						varVal += inAimByte[off];
						vCount++;
					}
				}
			}
		}
		meanVal /= vCount;
		varVal = varVal / (vCount + 0.0f) - meanVal * meanVal;
		final String cMsg = "Reporting porosity and std: "
				+ StrPctRatio(meanVal, 1) + ", "
				+ StrPctRatio(Math.sqrt(varVal), 1);
		procLog += cMsg + "\n";
		System.out.println(cMsg);
	}

	@Override
	protected void InitFloat() {
		final boolean useMask=hasMask;
		vCount = 0;
		meanVal = 0.0f;
		varVal = 0.0f;
		int off = 0;
		isSigned = true;
		for (int z = lowz; z < uppz; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
					boolean validStart = true;
					if (useMask)
						validStart = xdfMask[off];
					if (validStart) {
						meanVal += inAimFloat[off];
						varVal += inAimFloat[off] * inAimFloat[off];
						vCount++;
					}
				}
			}
		}
		meanVal /= vCount;
		varVal = varVal / (vCount + 0.0f) - meanVal * meanVal;
		System.out.println("Calculating averages and std:" + meanVal + ", "
				+ varVal);
	}

	@Override
	protected void InitInt() {
		final boolean useMask=hasMask;
		vCount = 0;
		meanVal = 0.0f;
		varVal = 0.0f;
		int off = 0;
		isSigned = true;
		for (int z = lowz; z < uppz; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
					boolean validStart = true; // (inPhase==inAimInt[off]);
					if (useMask)
						validStart = xdfMask[off];
					if (validStart) {
						meanVal += inAimInt[off];
						varVal += inAimInt[off];
						vCount++;
					}
				}
			}
		}
		meanVal /= vCount;
		varVal = varVal / (vCount + 0.0f) - meanVal * meanVal;
		final String cMsg = "Reporting porosity and std: "
				+ StrPctRatio(meanVal, 1) + ", "
				+ StrPctRatio(Math.sqrt(varVal), 1);
		procLog += cMsg + "\n";
		System.out.println(cMsg);
	}

	@Override
	protected void InitMask() {
		final boolean useMask=hasMask;
		vCount = 0;
		meanVal = 0.0f;
		varVal = 0.0f;
		int off = 0;
		isSigned = true;
		for (int z = lowz; z < uppz; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
					boolean validStart = true;
					if (useMask)
						validStart = xdfMask[off];
					if (validStart) {
						if (inAimMask[off]) {
							meanVal += 1;
							varVal += 1;
						}
						vCount++;
					}
				}
			}
		}
		meanVal /= vCount;
		varVal = varVal / (vCount + 0.0f) - meanVal * meanVal;
		final String cMsg = "Reporting porosity and std: "
				+ StrPctRatio(meanVal, 1) + ", "
				+ StrPctRatio(Math.sqrt(varVal), 1);
		procLog += cMsg + "\n";
		System.out.println(cMsg);
	}


	private void InitRDF(D3int rdfS) {
		runCount = 0;
		rdf = new radDistFun(rdfS);
		neighborSize = rdfS;
		curIter = 0;
		missedIter = 0;
		rgen = new Random();
	}

	@Override
	protected void InitShort() {
		vCount = 0;
		meanVal = 0.0f;
		varVal = 0.0f;
		int off = 0;
		isSigned = true;
		final boolean useMask=hasMask;
		for (int z = lowz; z < uppz; z++) {
			for (int y = lowy; y < uppy; y++) {
				off = (z * dim.y + y) * dim.x + lowx;
				for (int x = lowx; x < uppx; x++, off++) {
					boolean validStart = true;// (inPhase==inAimShort[off]);
					if (useMask)
						validStart = xdfMask[off];
					if (validStart) {
						meanVal += inAimShort[off];
						varVal += inAimShort[off];

						vCount++;
					}
				}
			}
		}
		meanVal /= vCount;
		varVal = varVal / (vCount + 0.0f) - meanVal * meanVal;
		final String cMsg = "Reporting porosity and std: "
				+ StrPctRatio(meanVal, 1) + ", "
				+ StrPctRatio(Math.sqrt(varVal), 1);
		procLog += cMsg + "\n";
		System.out.println(cMsg);
	}

	protected boolean isCoreFree(xdfScanner[] coreArray, int coreIndex) {
		if (coreArray[coreIndex] != null) {
			return (coreArray[coreIndex].isFinished);
		} else
			return true;
	}

	public double probScalar() {
		return (MAXPROB + 0.0) / (MAXDISTVAL + 0.0);
	}
	/**
	 * Run the XDF analysis for the given number of iterations
	 * 
	 * @param iters
	 *            The number of iterations to run the simulation for
	 */
	@Deprecated
	public void execute(int iters) {
		mcIter = iters;
		execute();
	}

	@Override
	protected void runByte() {
		System.err.println("Input type" + inputType + " not supported");
		return;
	}

	@Override
	protected void runFloat() {
		System.err.println("Input type" + inputType + " not supported");
		return;
	}

	@Override
	protected void runInt() {
		System.err.println("Input type" + inputType + " not supported");
		return;
	}

	@Override
	protected void runMask() {
		System.err.println("Input type" + inputType + " not supported");
		return;
	};

	@Override
	protected void runShort() {
		System.err.println("Input type" + inputType + " not supported");
		return;
	}

	@Override
	public synchronized String toString() {
		return toString(rdf);
	}

	public synchronized String toString(radDistFun crdf) {
		return "Iteration : " + curIter + "/" + mcIter + ", RDF:" + crdf
				+ ", Missed:" + missedIter;
	}

	public double valScalar() {
		return (MAXVAL + 0.0) / (MAXDISTVAL + 0.0);
	}
}
