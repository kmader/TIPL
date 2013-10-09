package tipl.tools;

//import java.awt.*;
//import java.awt.image.*;
//import java.awt.image.ColorModel.*;
import java.io.FileWriter;

import tipl.util.D3float;
import tipl.util.D3int;

// Used as a replacement for the moment function as it allows much more control over data
// and communication with webservices (potentially?)

/**
 * Performs shape analysis on a labeled aim image (map) and if given another
 * image
 */
public class Hist2D {
	/** An inteface for writing bin names from the bin count value **/
	public interface BinExtract {
		/** The actual bin name code, returns a string **/
		public String get(int binNum);

		public int getBin(float fval);

		/** The name to put in the header file **/
		public String name();
	}

	/**
	 * An inteface for selecting which data is extracted from the GrayVoxel
	 * class, defaults are mean value, std value, position, etc
	 **/
	public interface GrayVoxExtract {
		/** The actual extraction code, returns a string **/
		public String get(GrayVoxels ivoxel);

		/** The name to put in the header file **/
		public String name();
	}

	protected static BinExtract bneSimple() {
		return new BinExtract() {
			@Override
			public String get(final int binNum) {
				return "" + binNum + "";
			}

			@Override
			public int getBin(float fval) {
				if (fval < 0)
					fval = 0;
				return Math.round(fval);
			}

			@Override
			public String name() {
				return "Simple Bin Number";
			}
		};
	}

	protected static GrayVoxExtract gvCount() {
		return new GrayVoxExtract() {
			@Override
			public String get(final GrayVoxels ivoxel) {
				return "" + ivoxel.count();
			}

			@Override
			public String name() {
				return "Voxel Count";
			}
		};
	}

	protected static GrayVoxExtract gvMax() {
		return new GrayVoxExtract() {
			@Override
			public String get(final GrayVoxels ivoxel) {
				if (ivoxel.count() > 0)
					return "" + ivoxel.max();
				else
					return "0";
			}

			@Override
			public String name() {
				return "Maximum Value";
			}
		};
	}

	protected static GrayVoxExtract gvMean() {
		return new GrayVoxExtract() {
			@Override
			public String get(final GrayVoxels ivoxel) {
				if (ivoxel.count() > 0)
					return "" + ivoxel.mean();
				else
					return "-1";
			}

			@Override
			public String name() {
				return "Average Value (-1 is NAN)";
			}
		};
	}

	protected static GrayVoxExtract gvMin() {
		return new GrayVoxExtract() {
			@Override
			public String get(final GrayVoxels ivoxel) {
				if (ivoxel.count() > 0)
					return "" + ivoxel.min();
				else
					return "0";
			}

			@Override
			public String name() {
				return "Minimum Value";
			}
		};
	}

	private GrayVoxels[][] gvArray;

	double totVox = 0;
	double totSum = 0;
	double totSqSum = 0;

	static final int MAXARRVAL = 100000; // Integer.MAX_VALUE;
	public int fbins = MAXARRVAL;

	boolean debugMode;
	public boolean invertValue;
	boolean useCount;
	boolean useAname = false;
	// Parameters
	boolean noBlank = true;

	String analysisName = "Hist2D";

	String dlmChar = ", ";
	String headerString = "";

	String headerStr = "";
	String csvName = "";

	String insName = "N/A";
	boolean asList = false;
	/** which information to extract from the GrayVoxel class **/
	public GrayVoxExtract curGVE = gvCount();
	public GrayVoxExtract[] listGVE = new GrayVoxExtract[] { gvCount(),
			gvMean(), gvMin(), gvMax() };
	/** how should the x bin names be written **/
	public BinExtract xBNE = bneSimple();
	/** how should the y bin names be written **/
	public BinExtract yBNE = bneSimple();

	/** Simple initializer */
	public Hist2D() {
	}

	/** Simple initializer */
	public Hist2D(final boolean iSparse) {
	}

	/**
	 * Standard operation for a histogram
	 * 
	 * @param mapA
	 *            map for columns A
	 * @param mapB
	 *            map for rows B
	 * @param gfiltAim
	 *            map for values
	 * @param outFile
	 *            path and name of output file
	 */
	/** the second to calculate the covariances **/
	public boolean addCovVox(final int xBin, final int yBin, final double x,
			final double y, final double z) {
		try {
			gvArray[yBin][xBin].addCovVox(x, y, z);
			return true;
		} catch (final Exception e) {
			System.out.println("HistogramAdditionProblem@(" + xBin + "," + yBin
					+ ")");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * for float bin values
	 * 
	 * @param xF
	 *            the float value which needs to be assigned a bin
	 * @param yF
	 *            the y float value which needs to be assigned a bin
	 **/
	public boolean addFVox(final float xF, final float yF, final double x,
			final double y, final double z, final double cVal) {
		final int xBin = transXbin(xF);
		final int yBin = transXbin(yF);
		return addVox(xBin, yBin, x, y, z, cVal);
	}

	/**
	 * the first round of adding a voxel
	 * 
	 * @param xBin
	 *            is the bin value to use
	 * @param yBin
	 *            is the y-bin value to use
	 **/
	public boolean addVox(final int xBin, final int yBin, final double x,
			final double y, final double z, final double cVal) {
		try {
			gvArray[yBin][xBin].addVox(x, y, z, cVal);
			return true;
		} catch (final Exception e) {
			System.out.println("HistogramAdditionProblem@(" + xBin + "," + yBin
					+ ")");
			e.printStackTrace();
			return false;
		}
	}

	protected void initHistogram(final int xBins, final int yBins) {
		System.currentTimeMillis();
		// Initialize the output
		System.out.println("Setting up output data :[" + yBins + " - ( "
				+ yBNE.get(0) + "..." + yBNE.get(yBins) + ")]\t[" + xBins
				+ " - ( " + xBNE.get(0) + "..." + xBNE.get(xBins) + ")]");
		gvArray = new GrayVoxels[yBins][xBins];

		for (int aBin = 0; aBin < yBins; aBin++) {
			for (int bBin = 0; bBin < xBins; bBin++) {
				gvArray[aBin][bBin] = new GrayVoxels(bBin);
			}
		}
	}

	public String PluginName() {
		return "Hist2D";
	}

	/**
	 * the third round (for matching extents based on the eigenvectors of the
	 * shape tensor
	 **/
	public boolean setExtentsVoxel(final int xBin, final int yBin,
			final double x, final double y, final double z) {
		try {
			gvArray[yBin][xBin].setExtentsVoxel(x, y, z);
			return true;
		} catch (final Exception e) {
			System.out.println("HistogramAdditionProblem@(" + xBin + "," + yBin
					+ ")");
			e.printStackTrace();
			return false;
		}
	}

	private int transXbin(final float xF) {
		final int xBin = xBNE.getBin(xF);
		if ((xBin < 0) | (xBin >= gvArray[0].length))
			System.out.println("HistogramBinTranslationProblem-X@(" + xF + "->"
					+ xBin + "max " + gvArray[0].length + ")");
		return xBin;
	}

	/**
	 * Actually runs the GrayAnalysis2D code on the dataset, can be run inside
	 * of a thread
	 */
	protected boolean writeHeader(final String sampName, final String sampPath,
			final String greyName) {
		return writeHeader(sampName, sampPath, greyName, new D3int(-1),
				new D3int(-1), new D3int(-1), new D3float(0, 0, 0));
	}

	protected boolean writeHeader(final String sampName, final String sampPath,
			final String greyName, final D3int imDim, final D3int imOffset,
			final D3int imPos, final D3float imElSize) {
		try {
			headerStr += analysisName + " Histogram2D \n";
			headerStr += "Sample Name:        	" + sampName + "\n";
			headerStr += "Map Aim File:          	" + sampPath + "\n";
			headerStr += "GrayScale Aim File: 	" + greyName + "\n";
			headerStr += "Dim:                    " + imDim.x + "	" + imDim.y
					+ "	" + imDim.z + "\n";
			headerStr += "Off:        		    " + imOffset.x + " 	    "
					+ imOffset.y + " 	    " + imOffset.z + "\n";
			headerStr += "Pos:        		    " + imPos.x + " 	   " + imPos.y
					+ " 	    " + imPos.z + "\n";
			headerStr += "El_size_mm: 		" + imElSize.x + "	" + imElSize.y + "	"
					+ imElSize.z + "\n";
			final FileWriter out = new FileWriter(csvName, false);
			out.write(headerStr);
			out.flush();
			out.close();
			return true;
		} catch (final Exception e) {
			System.out.println("Writing Output File Problem");
			e.printStackTrace();
			return false;
		}
	}

	protected boolean writeHistogram(final String extraInfo) {
		try {

			final FileWriter out = new FileWriter(csvName, true);
			headerStr = "";
			headerStr += "Total Number of voxels 	:" + totVox;
			headerStr += extraInfo + "\n";
			headerStr += "---------------------------------------------------\n";
			headerStr += "X-Value:" + xBNE.name() + "\n";
			headerStr += "Y-Value:" + yBNE.name() + "\n";
			if (asList) {
				headerStr += "\n";
				headerStr += "---------------------------------------------------\n";
				headerStr += "X-Value" + dlmChar + "Y-Value";
				for (final GrayVoxExtract tGVE : listGVE) {
					headerStr += dlmChar + tGVE.name();
				}
				headerStr += "\n";
				out.append(headerStr);

				for (int aBin = 0; aBin < gvArray.length; aBin++) {
					final String aName = yBNE.get(aBin);
					for (int bBin = 0; bBin < gvArray[aBin].length; bBin++) {
						if (!(noBlank && gvArray[aBin][bBin].count() < 1)) {
							String outString = xBNE.get(bBin) + dlmChar + aName;
							for (final GrayVoxExtract tGVE : listGVE)
								outString += dlmChar
										+ tGVE.get(gvArray[aBin][bBin]);
							out.append(outString + "\n");
						}
					}

				}

			} else {
				headerStr += "Z-Value: Fn-" + curGVE.name() + "\n";
				headerStr += "---------------------------------------------------\n";
				headerStr += "";
				for (int bBin = 0; bBin < gvArray[0].length; bBin++) {
					headerStr += dlmChar + xBNE.get(bBin);
				}
				headerStr += "\n";
				out.append(headerStr);

				for (int aBin = 0; aBin < gvArray.length; aBin++) {
					String outString = "" + yBNE.get(aBin);
					int filled = 0;
					for (int bBin = 0; bBin < gvArray[aBin].length; bBin++) {
						outString += dlmChar + curGVE.get(gvArray[aBin][bBin]);
						if (gvArray[aBin][bBin].count() > 0)
							filled++;
					}
					if (!(noBlank && filled < 1))
						out.append(outString + "\n");
				}
			}

			out.flush();
			out.close();
			return true;
		} catch (final Exception e) {
			System.out.println("Writing Output File Problem");
			e.printStackTrace();
			return false;
		}
	}

}
