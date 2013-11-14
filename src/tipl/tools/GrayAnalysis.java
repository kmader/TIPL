package tipl.tools;

//import java.awt.*;
//import java.awt.image.*;
//import java.awt.image.ColorModel.*;
import java.io.FileWriter;
import java.util.Date;
import java.util.Hashtable;

import tipl.formats.FImage;
import tipl.formats.PureFImage;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;
import tipl.util.CSVFile;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

/**
 * Performs shape analysis on a labeled aim image (map) and if given another
 * image
 */
public class GrayAnalysis implements Runnable {
	public static final String kVer = "03-26-13 v 102";
	// LinkedList<GrayVoxels> gvArray;
	public static boolean doPreload = false;

	/**
	 * Standard operation for density column addition on
	 * 
	 * @param inDMap
	 *            labeled density image (voronoi volumes)
	 * @param inName
	 *            name of the input csv file (to read from)
	 * @param outName
	 *            name of the output csv file (to write to)
	 * @param aName
	 *            name of analysis in the output file (header column prefix)
	 */
	public static void AddDensityColumn(final TImgRO inDMap, final String inName,
			final String outName, final String aName) {
		final GrayAnalysis newGray = new GrayAnalysis(inDMap, inName, outName,
				aName);
		newGray.run();
	}

	/**
	 * Standard operation for an additional distance column to data
	 * 
	 * @param inMap
	 *            labeled image
	 * @param inGfilt
	 *            distance map to use
	 * @param inName
	 *            name of the input csv file (to read from)
	 * @param outName
	 *            name of the output csv file (to write to)
	 * @param aName
	 *            name of analysis in the output file (header column prefix,
	 *            _distance will be appended)
	 */
	public static void AddDistanceColumn(final TImgRO inMap, final TImgRO inGfilt,
			final String inName, final String outName, final String aName) {
		final GrayAnalysis newGray = new GrayAnalysis(inMap, inGfilt, inName,
				outName, aName);
		newGray.useAname = true;
		newGray.analysisName = aName + "_Distance";
		newGray.mapA = inMap;
		newGray.useGFILT = true;
		newGray.gfiltA = inGfilt;
		newGray.useInsert = true;

		newGray.insName = inName;
		newGray.csvName = outName;

		newGray.lacunaMode = false;
		newGray.cntcol = false;
		newGray.stdcol = true;
		newGray.covcol = true;
		newGray.mincol = false;
		newGray.maxcol = false;

		newGray.gradcol = true;
		newGray.angcol = true;

		newGray.SetupGA();
		newGray.run();
	}

	/**
	 * Standard operation for column addition to a existing lacuna file (canal
	 * region, gray level, density etc...)
	 * 
	 * @param inMap
	 *            labeled image
	 * @param inGfilt
	 *            value file for additional column to use
	 * @param inName
	 *            name of the input csv file (to read from)
	 * @param outName
	 *            name of the output csv file (to write to)
	 * @param aName
	 *            name of analysis in the output file (header column prefix)
	 */
	public static void AddFixedColumn(final TImgRO inMap, final TImgRO inGfilt,
			final String inName, final String outName, final String aName) {
		final GrayAnalysis newGray = new GrayAnalysis(inMap, inGfilt, inName,
				outName, aName);
		newGray.run();
	}

	/**
	 * Standard operation for column addition to a existing lacuna file (canal
	 * region, gray level, density etc...)
	 * 
	 * @param inMap
	 *            labeled image
	 * @param inGfilt
	 *            value file for additional column to use
	 * @param inName
	 *            name of the input csv file (to read from)
	 * @param outName
	 *            name of the output csv file (to write to)
	 * @param aName
	 *            name of analysis in the output file (header column prefix)
	 */
	public static void AddRegionColumn(final TImgRO inMap, final TImgRO inGfilt,
			final String inName, final String outName, final String aName) {

		final GrayAnalysis newGray = new GrayAnalysis(inMap, inGfilt, inName,
				outName, aName);
		newGray.lacunaMode = false;
		newGray.stdcol = true;
		newGray.covcol = false;
		newGray.mincol = false;
		newGray.maxcol = false;
		newGray.useThresh = false;
		newGray.run();

	}

	/** The standard version of grayanalysis which is run from the command line */
	public static void main(final String[] args) {

		System.out.println(" Gray Value and Lacuna Analysis v" + kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");

		final ArgumentParser p = new ArgumentParser(args);
		if (p.hasOption("?")) {
			showHelp();
		}

		final GrayAnalysis myGrayAnalysis = new GrayAnalysis(p);
		myGrayAnalysis.run();

	}

	/** Show's command line help when it is requested */
	public static void showHelp() {
		System.out.println(" Gray Analysis Help");
		System.out
				.println(" Analyzes Labeled Gray values inside of Int Labeled Regions");
		System.out.println(" Arguments::");
		System.out.println("  Map Parameters::");
		System.out.println("	-map = Map aim with labels");
		System.out.println("	-usefloat = use scaled float values for bins");
		System.out
				.println("	-fmin = minimum value (0) for float datasets (make bins)");
		System.out
				.println("	-fmax = max value (32766) for float datasets (make bins)");
		System.out
				.println("	-fsteps = bins (32767) for float datasets (make bins)");
		System.out.println("  GrayValue Parameters::");
		System.out.println("	-gfilt = Gray Value Image");
		System.out.println("	-useshort = use short values instead of scaled");
		System.out.println("	-invert = invert (1/x) values before using");
		System.out.println("	-thresh = set threshold (default 0)");
		System.out.println("	-nothresh = do not use threshold");
		System.out
				.println("	-boxroidist = instead of gfilt calculate distance from box edge");
		System.out.println("  CSV Parameters::");
		System.out.println("	-csv = CSV out filename");
		System.out.println("	-noblank = suppress printing blank lines");
		System.out.println("	-usecsv = use commas instead of tabs");
		System.out.println("	-debug = debug mode enable");
		System.out.println("  -lacun  = write output in _LACUN.CSV format");
		System.out
				.println("--------------------------------------------------");
		System.out
				.println("-- Embedding Output in LACUN output file ---------");
		System.out.println("	-insert = insert results from existing csv file");
		System.out
				.println("	-meancol (def:on) = don't write mean column in file");
		System.out
				.println("	-stdcol (def:on) = don't write std column in file");
		System.out.println("	-maxcol = write max as additional column");
		System.out.println("	-mincol = write min as additional column");
		System.out.println("	-cntcol = write bin count as additional column");
		System.out.println("	-covcol = write CoV as additional columns");
		System.out.println("	-comcol = write CoM as additional columns");
		System.out.println("	-sovcol = write CoV.Std as additional columns");
		System.out
				.println("	-pcacols= write PCA vectors of inertial matrix as additional columns");
		System.out.println("	-gradcol = write gradient as additional columns");
		System.out.println("	-angcol = write angles as additional columns");
		System.out.println("	-analysis = name for analysis");
		System.exit(0);
	}

	/**
	 * Create a profile based on an FImage Class
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param profImage
	 *            function image to use as the map
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartFProfile(final TImgRO inGfilt,
			final FImage profImage, final String outFile, final float threshVal) {
		StartFProfile(inGfilt, profImage, outFile, threshVal, PROFILEPOINTS); // err
		// on
		// the
		// cautious
		// side
	}

	/**
	 * Create a profile based on an FImage Class
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param profImage
	 *            function image to use as the map
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartFProfile(final TImgRO inGfilt,
			final FImage profImage, final String outFile,
			final float threshVal, final int fbins) {
		final GrayAnalysis newGray = new GrayAnalysis();
		newGray.mapA = profImage;
		newGray.csvName = outFile;
		newGray.useGFILT = true;
		newGray.gfiltA = inGfilt;
		newGray.threshVal = threshVal;
		newGray.useThresh = true;
		newGray.SetupGA();
		newGray.meancol = true;
		newGray.stdcol = true;
		newGray.mincol = true;
		newGray.maxcol = true;
		newGray.cntcol = true;
		if (newGray.mapA.getImageType() == 3) {
			final double[] ofloat = profImage.getRange();
			newGray.fmin = ofloat[0];
			newGray.fmax = ofloat[1];
			newGray.fbins = fbins;
			System.out.println("Binning Settings :: ( " + newGray.fmin + ", "
					+ newGray.fmax + " ) in " + newGray.fbins + " steps");
			newGray.useFMap = true;
		}
		newGray.run();
	}

	/**
	 * Create a profile based on an FImage Class
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param profImage
	 *            function image to use as the map
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartFProfile(final TImgRO inGfilt,
			final PureFImage profImage, final String outFile,
			final float threshVal) {
		StartFProfile(inGfilt, profImage, outFile, threshVal, PROFILEPOINTS); // err
		// on
		// the
		// cautious
		// side
	}

	/**
	 * Create a profile based on an FImage Class
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param profImage
	 *            function image to use as the map
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartFProfile(final TImgRO inGfilt,
			final PureFImage profImage, final String outFile,
			final float threshVal, final int fbins) {
		final GrayAnalysis newGray = new GrayAnalysis();
		newGray.mapA = profImage;
		newGray.csvName = outFile;
		newGray.useGFILT = true;
		newGray.gfiltA = inGfilt;
		newGray.threshVal = threshVal;
		newGray.useThresh = true;
		newGray.SetupGA();
		newGray.meancol = true;
		newGray.stdcol = true;
		newGray.mincol = true;
		newGray.maxcol = true;
		newGray.cntcol = true;
		if (newGray.mapA.getImageType() == 3) {
			final double[] ofloat = profImage.getRange();
			newGray.fmin = ofloat[0];
			newGray.fmax = ofloat[1];
			newGray.fbins = fbins;
			System.out.println("Binning Settings :: ( " + newGray.fmin + ", "
					+ newGray.fmax + " ) in " + newGray.fbins + " steps");
			newGray.useFMap = true;
		}
		newGray.run();
	}

	/**
	 * Standard operation for a histogram
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param outFile
	 *            path and name of output file
	 */
	public static GrayVoxels[] StartHistogram(final TImgRO inGfilt,
			final String outFile) {
		return StartHistogram(inGfilt, outFile, false);
	}

	/**
	 * Standard operation for a histogram
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param outFile
	 *            path and name of output file
	 */
	public static GrayVoxels[] StartHistogram(final TImgRO inGfilt,
			final String outFile, final boolean removeBlanks) {
		final GrayAnalysis newGray = new GrayAnalysis();
		newGray.mapA = inGfilt;
		newGray.csvName = outFile;
		newGray.gfiltA = null;
		newGray.mincol = false;
		newGray.maxcol = false;
		newGray.noBlank = removeBlanks;
		newGray.SetupGA();
		newGray.run();
		return newGray.gvArray;
	}

	/**
	 * Standard operation for a histogram
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param outFile
	 *            path and name of output file
	 * @param minVal
	 *            minimum value to use for a bin
	 * @param maxVal
	 *            maximum value to use for a bin
	 * @param steps
	 *            the number of bins to use
	 */
	public static GrayVoxels[] StartHistogram(final TImgRO inGfilt,
			final String outFile, final float minVal, final float maxVal,
			final int steps) {
		return StartHistogram(inGfilt, outFile, minVal, maxVal, steps, false);
	}

	/**
	 * Standard operation for a histogram
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param outFile
	 *            path and name of output file
	 * @param minVal
	 *            minimum value to use for a bin
	 * @param maxVal
	 *            maximum value to use for a bin
	 * @param steps
	 *            the number of bins to use
	 */
	public static GrayVoxels[] StartHistogram(final TImgRO inGfilt,
			final String outFile, final float minVal, final float maxVal,
			final int steps, final boolean removeBlanks) {
		final GrayAnalysis newGray = new GrayAnalysis();
		newGray.mapA = inGfilt;
		newGray.csvName = outFile;
		newGray.noBlank = removeBlanks;
		newGray.gfiltA = null;
		newGray.mincol = false;
		newGray.maxcol = false;
		newGray.fmin = minVal;
		newGray.fmax = maxVal;
		newGray.fbins = steps;
		newGray.useFMap = true;
		newGray.SetupGA();
		newGray.run();
		return newGray.gvArray;
	}

	public static void StartLacunaAnalysis(final TImgRO inMap,
			final String outName, final String aName) {
		StartLacunaAnalysis(inMap, outName, aName, false);
	}

	public static void StartLacunaAnalysis(final TImgRO inMap,
			final String outName, final String aName,
			final boolean includeShapeT) {
		final GrayAnalysis newGray = new GrayAnalysis(inMap, (TImg) null,
				outName, aName);
		newGray.includeShapeTensor = includeShapeT;
		newGray.run();
	}

	/**
	 * Standard operation for full shape analysis with a distance map from
	 * scratch
	 * 
	 * @param inMap
	 *            labeled image
	 * @param inGfilt
	 *            distance map to use
	 * @param outName
	 *            path and name of output file
	 * @param aName
	 *            name of the analysis in the output file (header column prefix)
	 */
	public static void StartLacunaAnalysis(final TImgRO inMap,
			final TImgRO inGfilt, final String outName, final String aName) {
		StartLacunaAnalysis(inMap, inGfilt, outName, aName, false);
	}

	/**
	 * Standard operation for full shape analysis with a distance map from
	 * scratch
	 * 
	 * @param inMap
	 *            labeled image
	 * @param inGfilt
	 *            distance map to use
	 * @param outName
	 *            path and name of output file
	 * @param aName
	 *            name of the analysis in the output file (header column prefix)
	 * @param includeShapeT
	 *            include the entire shape tensor in the output (for paraview
	 *            visualization and analysis)
	 */
	public static void StartLacunaAnalysis(final TImgRO inMap,
			final TImgRO inGfilt, final String outName, final String aName,
			final boolean includeShapeT) {
		final GrayAnalysis newGray = new GrayAnalysis(inMap, inGfilt, outName,
				aName);
		newGray.includeShapeTensor = includeShapeT;
		newGray.run();
	}

	/**
	 * Standard operation for a histogram with the value plotted against the
	 * radius (in cylindrical coordinates)
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartRCylProfile(final TImgRO inGfilt,
			final String outFile, final float threshVal) {
		final PureFImage cFImg = new PureFImage.RImageCyl(inGfilt, 3);
		final double[] rng = cFImg.getRange();
		StartFProfile(inGfilt, cFImg, outFile, threshVal,
				(int) (rng[1] - rng[0]) * 2);
	}

	/**
	 * Standard operation for a histogram with the value plotted against the
	 * radius (in cylindrical coordinates)
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param inMask
	 *            mask for slices
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartRCylProfile(final TImgRO inGfilt, final TImgRO inMask,
			final String outFile, final float threshVal) {
		final FImage maskedF = new FImage.MaskablePFImage(inMask,
				new PureFImage.RImageCyl(inMask, 3));
		maskedF.useMask = true;
		final double[] rng = maskedF.getRange();
		StartFProfile(inGfilt, maskedF, outFile, threshVal,
				(int) (rng[1] - rng[0]) * 2);
	}

	/**
	 * Standard operation for a histogram with the value plotted against the
	 * radius
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartRProfile(final TImgRO inGfilt, final String outFile,
			final float threshVal) {
		final PureFImage cFImg = new PureFImage.RImage(inGfilt, 3);
		final double[] rng = cFImg.getRange();
		StartFProfile(inGfilt, cFImg, outFile, threshVal,
				(int) (rng[1] - rng[0]) * 2);
	}

	/**
	 * Standard operation for a histogram with the value plotted against the
	 * radius
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param inMask
	 *            mask for slices
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartRProfile(final TImgRO inGfilt, final TImgRO inMask,
			final String outFile, final float threshVal) {
		final FImage maskedF = new FImage.MaskablePFImage(inMask,
				new PureFImage.RImage(inMask, 3));
		maskedF.useMask = true;
		final double[] rng = maskedF.getRange();
		StartFProfile(inGfilt, maskedF, outFile, threshVal,
				(int) (rng[1] - rng[0]) * 2);
	}

	/**
	 * Standard operation for a histogram with the value plotted against the
	 * radius (in cylindrical coordinates)
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartThetaCylProfile(final TImgRO inGfilt,
			final String outFile, final float threshVal) {
		final PureFImage cFImg = new PureFImage.ThetaImageCyl(inGfilt, 3);
		StartFProfile(inGfilt, cFImg, outFile, threshVal, 1000);
	}

	/**
	 * Standard operation for a histogram with the value plotted against the
	 * radius (in cylindrical coordinates)
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param inMask
	 *            mask for slices
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartThetaCylProfile(final TImgRO inGfilt,
			final TImgRO inMask, final String outFile, final float threshVal) {
		final FImage maskedF = new FImage.MaskablePFImage(inMask,
				new PureFImage.ThetaImageCyl(inMask, 3));
		maskedF.useMask = true;
		StartFProfile(inGfilt, maskedF, outFile, threshVal, 1000);
	}

	/**
	 * Standard operation for a histogram with the value plotted against the z
	 * position
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartZProfile(final TImgRO inGfilt, final String outFile,
			final float threshVal) {
		StartFProfile(inGfilt, new PureFImage.ZImage(inGfilt, 1), outFile,
				threshVal);
	}

	/**
	 * Standard operation for a histogram with the value plotted against the z
	 * position
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param inMask
	 *            mask for slices
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartZProfile(final TImgRO inGfilt, final TImgRO inMask,
			final String outFile, final float threshVal) {
		final PureFImage maskedF = new PureFImage.ZImage(inMask, 1);
		// maskedF.useMask=true;

		StartFProfile(inGfilt, maskedF, outFile, threshVal);
	}

	GrayVoxels[] gvArray;
	TImgRO mapA;
	double mapScaleFactor = 1.0;
	TImgRO gfiltA;
	int maxGroup = 0;
	double totVox = 0;
	double totSum = 0;
	double totSqSum = 0;
	double fmin = 0;
	double fmax = 32765;
	static final int MAXARRVAL = 500000; // Integer.MAX_VALUE;
	/** the number of points to use in a standard float profile plot */
	public static int PROFILEPOINTS = 1000;
	public int fbins = MAXARRVAL;
	boolean debugMode;
	private boolean useGFILT;
	private boolean useFMap;
	public boolean invertGFILT;
	boolean useCount;
	boolean useAname = false;
	// Parameters
	/** Use commas in output instead of tabs */
	public boolean useComma = false;
	/** Use a threshold on the gfilt data */
	public boolean useThresh = false;
	/** Add column with mean of gfilt data inside each object (true) */
	public boolean meancol = true;

	/** Add column with std of gfilt data inside each object (true) */
	public boolean stdcol = true;

	/** Add column with max of gfilt data inside each object (true) */
	public boolean maxcol = false;

	/** Add column with min of gfilt data inside each object (true) */
	public boolean mincol = false;

	/** Add column with the voxel count inside each object (false) */
	public boolean cntcol = false;

	/** Add columns with the center of volumes for each object (false) */
	public boolean covcol = false;

	/**
	 * Add columns with the center of mass (gfilt weighted) for each object
	 * (false)
	 */
	public boolean comcol = false;

	/**
	 * Add columns with the std of the center of volumes for each object (false)
	 */
	public boolean sovcol = false;

	/** Add columns with the gfilt-calculated gradient for each object (false) */
	public boolean gradcol = false;

	/**
	 * Add columns with the angle between the objects main direction and the
	 * direction of the gradient (false)
	 */
	public boolean angcol = false;

	/** Add columns with the PCA components and scores for each object (false) */
	public boolean pcacols = false;

	private boolean noThresh = false;

	/** write out the entire shape tensor **/
	private boolean includeShapeTensor = false;

	boolean useShort = false;

	boolean useFloat = false;

	/**
	 * Perform the standard shape analysis and save as a list of objects inside
	 * a csv file. If false result is formatted more as a histogram or 2D
	 * histogram
	 */
	public boolean lacunaMode = false;

	boolean noBlank = false;

	boolean useInsert = false;

	/** Calculate distance from wall as distance from edge of ROI volume */
	public boolean boxDist = false;

	/** Value to use for the threshold (0) */
	public float threshVal = 0;

	String analysisName = "GrayAnalysis";

	String dlmChar = "\t";

	String headerString = "";

	String headerStr = "";

	String csvName = "";

	String insName = "N/A";

	protected String gfiltName = "";

	/** Plain old initializer */
	public GrayAnalysis() {
	}

	public GrayAnalysis(final ArgumentParser p) {
		debugMode = p.hasOption("debug");
		useComma = p.hasOption("usecsv");
		useThresh = p.hasOption("thresh");

		// Columns to be written in output file
		meancol = !p.hasOption("meancol"); // Default Column
		stdcol = !p.hasOption("stdcol"); // Default Column
		maxcol = p.hasOption("maxcol"); // Special Column
		mincol = p.hasOption("mincol"); // Special Column
		cntcol = p.hasOption("cntcol"); // Special Column
		covcol = p.hasOption("covcol"); // Special Column
		comcol = p.hasOption("comcol"); // Special Column
		sovcol = p.hasOption("sovcol"); // Special Column
		gradcol = p.hasOption("gradcol"); // Special Column
		angcol = p.hasOption("angcol"); // Special Column
		pcacols = p.hasOption("pcacols"); // Special Column
		noThresh = p.hasOption("nothresh");
		useShort = p.hasOption("useshort"); // use short for GFILT
		useFloat = p.hasOption("usefloat"); // use float for MAP (just labels in
		// CSV file)
		lacunaMode = p.hasOption("lacuna");
		noBlank = p.hasOption("noblank");
		useInsert = p.hasOption("insert");
		boxDist = p.hasOption("boxroidist");
		includeShapeTensor = p.hasOption("shapetensor");
		invertGFILT = p.hasOption("invert");
		useGFILT = p.hasOption("gfilt");

		// Float Binning Settings
		if (p.hasOption("fmin"))
			fmin = Double.valueOf(p.getOptionAsString("fmin").trim())
					.doubleValue();
		if (p.hasOption("fmax"))
			fmax = Double.valueOf(p.getOptionAsString("fmax").trim())
					.doubleValue();
		if (p.hasOption("fbins"))
			fbins = Integer.valueOf(p.getOptionAsString("fbins").trim())
					.intValue();

		useAname = p.hasOption("analysis");

		if (useThresh) {
			threshVal = Float.parseFloat(p.getOptionAsString("thresh"));
		} else {
			threshVal = 0;
		}

		final String mapName = p.getOptionAsString("map"); // Map is a needed
															// parameter

		if (useGFILT)
			gfiltName = p.getOptionAsString("gfilt"); // gfilt is a given
														// parameter
		csvName = p.getOptionAsString("csv"); // CSV file is a needed parameter
		insName = "N/A";
		if (useInsert) {
			insName = p.getOptionAsString("insert");
		}

		if (useAname)
			analysisName = p.getOptionAsString("analysis");
		if (useComma)
			dlmChar = ", ";

		System.out.println("Map Aim: " + mapName);
		if (useGFILT)
			System.out.println("Gray Value Aim: " + gfiltName);
		System.out.println("Debug: " + debugMode);
		System.out.println("Template CSV: " + insName);
		System.out.println("Output CSV: " + csvName);

		mapA = TImgTools.ReadTImg(mapName);
		if (useFloat)
			mapA.getShortScaleFactor();

		// to be preserved and not
		// skewed

		if (mapA.getImageType() == 3) {
			System.out.println("Binning Settings :: ( " + fmin + ", " + fmax
					+ " ) in " + fbins + " steps");
			useFMap = true;
		}
		if (useGFILT) {
			gfiltA = TImgTools.ReadTImg(gfiltName);

			
			if (TImgTools.CheckSizes2(mapA, gfiltA)) {
				System.out.println("Sizes Match..");
			} else {
				System.out
						.println("ERROR : Aim Dimensions DO NOT MATCH, PROCEED WITH CAUTION");
			}
		}

		SetupGA();

	}

	/**
	 * Standard operation for shape analysis on
	 * 
	 * @param inMap
	 *            labeled image
	 * @param outName
	 *            output csv file
	 * @param aName
	 *            name of the analysis in the output file (header column prefix)
	 */
	public GrayAnalysis(final TImgRO inMap, final String outName,
			final String aName) {
		mapA = inMap;
		useGFILT = false;
		gfiltA = null;
		csvName = outName;
		lacunaMode = true;
		analysisName = aName;
		SetupGA();
	}

	/**
	 * Standard operation for density column addition on
	 * 
	 * @param inDMap
	 *            labeled density image (voronoi volumes)
	 * @param inName
	 *            name of the input csv file (to read from)
	 * @param outName
	 *            name of the output csv file (to write to)
	 * @param aName
	 *            name of analysis in the output file (header column prefix)
	 */
	public GrayAnalysis(final TImgRO inDMap, final String inName,
			final String outName, final String aName) {
		useAname = true;
		analysisName = aName;
		mapA = inDMap;
		useGFILT = false;
		gfiltA = null;
		useInsert = true;

		meancol = false;
		stdcol = false;
		mincol = false;
		maxcol = false;
		cntcol = true;
		covcol = true;

		insName = inName;
		csvName = outName;

		lacunaMode = false;
		SetupGA();
	}

	/**
	 * Standard operation for full shape analysis with a distance map from
	 * scratch
	 * 
	 * @param inMap
	 *            labeled image
	 * @param inGfilt
	 *            distance map to use
	 * @param outName
	 *            path and name of output file
	 * @param aName
	 *            name of the analysis in the output file (header column prefix)
	 */
	public GrayAnalysis(final TImgRO inMap, final TImgRO inGfilt,
			final String outName, final String aName) {
		mapA = inMap;
		useGFILT = true;
		gfiltA = inGfilt;
		if (gfiltA == null)
			boxDist = true;

		csvName = outName;
		lacunaMode = true;
		analysisName = aName;
		SetupGA();
	}

	/**
	 * Standard operation for column addition to a existing lacuna file (canal
	 * region, gray level, density etc...)
	 * 
	 * @param inMap
	 *            labeled image
	 * @param inGfilt
	 *            value file for additional column to use
	 * @param inName
	 *            name of the input csv file (to read from)
	 * @param outName
	 *            name of the output csv file (to write to)
	 * @param aName
	 *            name of analysis in the output file (header column prefix)
	 */
	public GrayAnalysis(final TImgRO inMap, final TImgRO inGfilt,
			final String inName, final String outName, final String aName) {
		useAname = true;
		analysisName = aName;
		mapA = inMap;
		useGFILT = true;
		gfiltA = inGfilt;
		useInsert = true;

		insName = inName;
		csvName = outName;

		lacunaMode = false;
		SetupGA();
	}

	/**
	 * Standard operation for an additional distance column to data
	 * 
	 * @param inMap
	 *            labeled image
	 * @param inGfilt
	 *            distance map to use
	 * @param inName
	 *            name of the input csv file (to read from)
	 * @param outName
	 *            name of the output csv file (to write to)
	 * @param aName
	 *            name of analysis in the output file (header column prefix,
	 *            _distance will be appended)
	 * @param makemedist
	 *            Force distance map (can be empty just ensures that the
	 *            constructor uses the right property
	 */
	public GrayAnalysis(final TImgRO inMap, final TImgRO inGfilt,
			final String inName, final String outName, final String aName,
			final boolean makemedist) {
		useAname = true;
		analysisName = aName + "_Distance";
		mapA = inMap;
		useGFILT = true;
		gfiltA = inGfilt;
		useInsert = true;

		insName = inName;
		csvName = outName;

		lacunaMode = false;
		cntcol = false;
		stdcol = true;
		covcol = true;
		mincol = false;
		maxcol = false;

		gradcol = true;
		angcol = true;

		SetupGA();

	}

	private void AnalyzeSlice(final int sliceNumber, final boolean noThresh,
			final int operationMode) {
		// Operation Mode -> 0- find COM/COV, 1- find covariance matrix, 2- find
		// extents
		if (debugMode)
			System.out.println("Reading MapSlice " + sliceNumber + "/"
					+ mapA.getDim().z);
		final TImg.TImgFull fullMapA = new TImg.TImgFull(mapA);

		int[] mapSlice;
		switch (mapA.getImageType()) {
		case 0:
		case 1:
			final short[] smapSlice = fullMapA.getShortArray(sliceNumber);
			mapSlice = new int[smapSlice.length];
			for (int cIndex = 0; cIndex < smapSlice.length; cIndex++)
				mapSlice[cIndex] = smapSlice[cIndex];
			break;
		case 2:
			mapSlice = fullMapA.getIntArray(sliceNumber);
			break;
		case 3:
			float[] fmapSlice = fullMapA.getFloatArray(sliceNumber);
			mapSlice = new int[fmapSlice.length];
			for (int cIndex = 0; cIndex < fmapSlice.length; cIndex++)
				mapSlice[cIndex] = f2i(fmapSlice[cIndex]);
			fmapSlice = null;
			break;
		case 10:
			final boolean[] bmap = fullMapA.getBoolArray(sliceNumber);
			mapSlice = new int[bmap.length];
			for (int cIndex = 0; cIndex < bmap.length; cIndex++)
				if (bmap[cIndex])
					mapSlice[cIndex] = 127;
			break;
		default:
			System.err.println("Type " + mapA.getImageType()
					+ " is not supported");
			return;

		}

		if (debugMode)
			System.out.println("Reading gfiltSlice " + sliceNumber + "/"
					+ gfiltA.getDim().z);

		int[] gfiltSlice = new int[1];
		float[] fgfiltSlice = new float[1];
		if (useGFILT) {
			final TImg.TImgFull fullGfiltA = new TImg.TImgFull(gfiltA);
			switch (gfiltA.getImageType()) {
			case 0:
			case 1:
				final short[] sgfiltSlice = fullGfiltA
						.getShortArray(sliceNumber);
				gfiltSlice = new int[sgfiltSlice.length];
				for (int cIndex = 0; cIndex < sgfiltSlice.length; cIndex++)
					gfiltSlice[cIndex] = sgfiltSlice[cIndex];
				break;
			case 2:
				gfiltSlice = fullGfiltA.getIntArray(sliceNumber);
				break;
			case 3:
				fgfiltSlice = fullGfiltA.getFloatArray(sliceNumber);
				break;
			case 10:
				final boolean[] bgfiltSlice = fullGfiltA
						.getBoolArray(sliceNumber);
				gfiltSlice = new int[bgfiltSlice.length];
				for (int cIndex = 0; cIndex < bgfiltSlice.length; cIndex++)
					if (bgfiltSlice[cIndex])
						gfiltSlice[cIndex] = 127;
				break;
			default:
				System.err.println("Gfilt" + gfiltA + ":Type "
						+ gfiltA.getImageType() + " is not supported");
				return;
			}
		}
		double cVal;
		if (debugMode)
			System.out.println("Reading Points " + mapSlice.length);
		for (int cIndex = 0; cIndex < mapSlice.length; cIndex++) {
			final int cMapVal = mapSlice[cIndex];

			if ((cMapVal > 0) & (cMapVal < gvArray.length)) {
				if (cMapVal > maxGroup)
					maxGroup = cMapVal;
				if (useGFILT) {
					if (gfiltA.getImageType() == 3) {
						cVal = fgfiltSlice[cIndex];
					} else {
						cVal = gfiltSlice[cIndex];
						if (Math.abs(gfiltA.getShortScaleFactor()) > 1e-6)
							cVal *= gfiltA.getShortScaleFactor();
					}
				} else {
					cVal = 1;
				}
				if (invertGFILT)
					cVal = 1 / cVal;
				if ((cVal >= threshVal) | (noThresh)) {

					// Track Overall Statistics
					totVox++;
					totSum += cVal;
					totSqSum += Math.pow(cVal, 2);
					if (cVal < fbins) {

						final Double[] cPos = TImgTools.getXYZVecFromVec(mapA,
								cIndex, sliceNumber);

						if (operationMode == 0) {
							gvArray[cMapVal].addVox(cPos[0].floatValue(),
									cPos[1].floatValue(), cPos[2].floatValue(),
									cVal);
						} else if (operationMode == 1) {
							gvArray[cMapVal].addCovVox(cPos[0].floatValue(),
									cPos[1].floatValue(), cPos[2].floatValue());
						} else if (operationMode == 2) {
							gvArray[cMapVal].setExtentsVoxel(
									cPos[0].floatValue(), cPos[1].floatValue(),
									cPos[2].floatValue());
						}
					} else {
						if (debugMode)
							System.out.println(" Value " + cVal
									+ " Above Max : " + fbins);
					}

				}
			}
		}
		if (debugMode)
			System.out.println("Done Reading Points " + mapSlice.length);
	}

	private int f2i(final double val) {
		if (val < fmin)
			return 0;
		if (val > fmax)
			return fbins;
		return (int) (((val - fmin) / (fmax - fmin) * fbins));
	}

	private float i2f(final int val) {
		if (val < 0)
			return (float) fmin;
		if (val > fbins)
			return (float) fmax;
		return (float) (val * (fmax - fmin) / (fbins + 0.0) + (float) fmin);
	}

	/**
	 * Actually runs the grayanalysis code on the dataset, can be run inside of
	 * a thread
	 */
	@Override
	public void run() {
		if (doPreload) {
			System.out.println("Preloading Datasets..." + mapA);
			mapA = TImgTools.WrapTImgRO(TImgTools.CacheImage(mapA));
			if (useGFILT)
				gfiltA = TImgTools.WrapTImgRO(TImgTools.CacheImage(gfiltA));
		}
		long start = System.currentTimeMillis();
		boolean gfiltGood = true;
		if (useGFILT)
			gfiltGood = gfiltA.isGood();
		if ((mapA.isGood()) & (gfiltGood)) {
			try {
				if (useInsert) {
					final CSVFile insFile = new CSVFile(insName, 2);
					for (int i = 0; i < 2; i++) {
						headerStr += insFile.rawHeader[i];
						if (i == 0) {
							if (meancol)
								headerStr += ", " + analysisName + ":(" + 1.0
										/ gfiltA.getShortScaleFactor() + ")"
										+ ":" + gfiltA.getPath();
							else if (cntcol)
								headerStr += ", " + analysisName + ":"
										+ mapA.getPath();
						}
						if (i == 1) {
							if (meancol)
								headerStr += ", " + analysisName;
							if (stdcol)
								headerStr += ", " + analysisName + "_STD";
							if (mincol)
								headerStr += ", " + analysisName + "_MIN";
							if (maxcol)
								headerStr += ", " + analysisName + "_MAX";
							if (cntcol)
								headerStr += ", " + analysisName + "_CNT";
							// CoV
							if (covcol)
								headerStr += ", " + analysisName + "_CX";
							if (covcol)
								headerStr += ", " + analysisName + "_CY";
							if (covcol)
								headerStr += ", " + analysisName + "_CZ";
							// SoV
							if (sovcol)
								headerStr += ", " + analysisName + "_SX";
							if (sovcol)
								headerStr += ", " + analysisName + "_SY";
							if (sovcol)
								headerStr += ", " + analysisName + "_SZ";
							// CoM
							if (comcol)
								headerStr += ", " + analysisName + "_WX";
							if (comcol)
								headerStr += ", " + analysisName + "_WY";
							if (comcol)
								headerStr += ", " + analysisName + "_WZ";
							// Grad
							if (gradcol)
								headerStr += ", " + analysisName + "_GRAD_X";
							if (gradcol)
								headerStr += ", " + analysisName + "_GRAD_Y";
							if (gradcol)
								headerStr += ", " + analysisName + "_GRAD_Z";
							// Angle
							if (angcol)
								headerStr += ", " + analysisName + "_ANGLE";
							if (includeShapeTensor) {
								for (final char cX : "XYZ".toCharArray()) {
									for (final char cY : "XYZ".toCharArray()) {
										headerStr += ", " + analysisName
												+ "_SHAPET_" + cX + "" + cY;
									}
								}
							}

						}
						headerStr += "\n";
					}
					insFile.close();
				} else {
					if (lacunaMode) {
						headerStr = "// Sample: " + mapA.getSampleName()
								+ ", Map: " + mapA.getPath();
						if (useGFILT)
							headerStr += ", " + analysisName + " Dist : "
									+ gfiltA.getPath() + "(" + 1.0
									/ gfiltA.getShortScaleFactor() + ")";
						headerStr += "\n//LACUNA_NUMBER, TRIANGLES, SCALE_X, SCALE_Y, SCALE_Z, POS_X, POS_Y, POS_Z , STD_X, STD_Y, STD_Z,";
						headerStr += "PROJ_X, PROJ_Y, PROJ_Z, PCA1_X, PCA1_Y, PCA1_Z, PCA1_S, PCA2_X, PCA2_Y, PCA2_Z, PCA2_S, PCA3_X, PCA3_Y, PCA3_Z, PCA3_S,";
						headerStr += "PROJ_PCA1, PROJ_PCA2, PROJ_PCA3,";
						headerStr += "OBJ_RADIUS, OBJ_RADIUS_STD, VOLUME, VOLUME_BOX";
						if (useGFILT | boxDist) {
							headerStr += ", " + analysisName + "_Grad_X,"
									+ analysisName + "_Grad_Y," + analysisName
									+ "_Grad_Z," + analysisName + "_Angle";
							headerStr += ", " + analysisName
									+ "_Distance_Mean," + analysisName
									+ "_Distance_COV, " + analysisName
									+ "_Distance_STD";
						}
						if (includeShapeTensor) {
							for (final char cX : "XYZ".toCharArray()) {
								for (final char cY : "XYZ".toCharArray()) {
									headerStr += ", SHAPET_" + cX + "" + cY;
								}
							}
						}
						headerStr += "\n";

					} else {
						headerStr += analysisName + " Histogram \n";
						headerStr += "Sample Name:        	"
								+ mapA.getSampleName() + "\n";
						headerStr += "Map Aim File:          	"
								+ mapA.getPath() + "\n";
						headerStr += "GrayScale Aim File: 	" + gfiltName + "\n";
						headerStr += "Dim:        		" + mapA.getDim().x + "	"
								+ mapA.getDim().y + "	" + mapA.getDim().z
								+ "\n";
						headerStr += "Off:        		    " + mapA.getOffset().x
								+ " 	    " + mapA.getOffset().y + " 	    "
								+ mapA.getOffset().z + "\n";
						headerStr += "Pos:        		    " + mapA.getPos().x
								+ " 	   " + mapA.getPos().y + " 	    "
								+ mapA.getPos().z + "\n";
						headerStr += "El_size_mm: 		" + mapA.getElSize().x
								+ "	" + mapA.getElSize().y + "	"
								+ mapA.getElSize().z + "\n";

					}

				}
				final FileWriter out = new FileWriter(csvName, false);
				out.write(headerStr);
				out.flush();
				out.close();

			} catch (final Exception e) {
				System.out.println("Writing Output File Problem");
				e.printStackTrace();
			}
			// Restart running time

			gvArray = new GrayVoxels[fbins];
			// gvArray = new LinkedList<GrayVoxels>();
			// Initialize
			System.out.println("Initializing GrayVoxel Array...");

			// Faster To Put if outside of for-loop, methinks
			if (useFMap) {
				for (int cVox = 0; cVox < fbins; cVox++)
					gvArray[cVox] = new GrayVoxels(i2f(cVox));
			} else if (useFloat) {
				for (int cVox = 0; cVox < fbins; cVox++)
					gvArray[cVox] = new GrayVoxels(mapScaleFactor * cVox);
			} else {
				for (int cVox = 0; cVox < fbins; cVox++)
					gvArray[cVox] = new GrayVoxels(cVox);
			}

			start = System.currentTimeMillis();
			System.out.println("Reading Slices... " + mapA.getDim().z);
			if ((mapA.getImageType() == 1) | (mapA.getImageType() == 2)
					| (mapA.getImageType() == 3)) {
				for (int cSlice = 0; cSlice < mapA.getDim().z; cSlice++) {
					if (debugMode)
						System.out.println("Reading Slices " + cSlice + "/"
								+ mapA.getDim().z);
					AnalyzeSlice(cSlice, noThresh, 0);
				}
				System.out.println("Rescanning Slices for COV Matrix... "
						+ mapA.getDim().z);
				for (int cSlice = 0; cSlice < mapA.getDim().z; cSlice++) {
					if (debugMode)
						System.out.println("Reading Slices " + cSlice + "/"
								+ mapA.getDim().z);
					AnalyzeSlice(cSlice, noThresh, 1);
				}
			} else {
				System.out.println("ERROR: Map of type : "
						+ mapA.getImageType() + " not supported!");
				System.exit(-1);
			}
			System.out.println("Done Reading..."
					+ (System.currentTimeMillis() - start) / (60 * 1000F)
					+ "mins, Objects:" + maxGroup + "; Voxels:" + totVox);
			long restart = System.currentTimeMillis();
			if ((lacunaMode) || (angcol)) {
				System.out.println("Generating Diagonalization...");
				for (int cGroup = 1; cGroup <= maxGroup; cGroup++) {
					if ((gvArray[cGroup].count() > 5)) { // At least 5 voxels
						gvArray[cGroup].diag();
					}

				}

				System.out.println("Done Diagonalizing..."
						+ (System.currentTimeMillis() - restart) / (60 * 1000F)
						+ "mins , Rescanning for Diagonal Extents...");
				restart = System.currentTimeMillis();
				if ((mapA.getImageType() == 1) | (mapA.getImageType() == 2)
						| (mapA.getImageType() == 3)) {
					for (int cSlice = 0; cSlice < mapA.getDim().z; cSlice++) {
						if (debugMode)
							System.out.println("Reading Slices " + cSlice + "/"
									+ mapA.getDim().z);
						AnalyzeSlice(cSlice, noThresh, 2);
					}
				}
				System.out.println("Done Extening..."
						+ (System.currentTimeMillis() - restart) / (60 * 1000F)
						+ " mins");
			}
			if (boxDist) {
				System.out.println("Calculating ROI Box Distance...");
				for (int cGroup = 1; cGroup < maxGroup; cGroup++)
					gvArray[cGroup].calculateBoxDist(mapA.getPos().x,
							mapA.getPos().y, mapA.getPos().z, mapA.getDim().x
									+ mapA.getPos().x,
							mapA.getDim().y + mapA.getPos().y, mapA.getDim().z
									+ mapA.getPos().z);
			}

			TIPLGlobal.runGC();
			try {

				final FileWriter out = new FileWriter(csvName, true);
				if (useInsert) {
					final CSVFile insFile = new CSVFile(insName, 2);
					// Insert values as last two columns
					while (!insFile.fileDone) {
						final Hashtable cLine = insFile.parseline();
						if (cLine.containsKey("lacuna_number")) {
							final int curRow = (new Integer(
									(String) cLine.get("lacuna_number")))
									.intValue();
							double valMean = 0;
							double valStd = -1;
							double valCnt = 0;
							double valMin = 0;
							double valMax = 0;

							double covX = 0;
							double covY = 0;
							double covZ = 0;

							double sovX = -5;
							double sovY = -5;
							double sovZ = -5;

							double comX = 0;
							double comY = 0;
							double comZ = 0;

							double gradX = 0;
							double gradY = 0;
							double gradZ = 0;

							double angT = 0;
							String gtStr = "";

							String outString = insFile.rline;
							if ((curRow > 0) & (curRow <= maxGroup)) {
								valMean = gvArray[curRow].mean();
								valStd = gvArray[curRow].std();
								valCnt = gvArray[curRow].count();
								valMin = gvArray[curRow].min();
								valMax = gvArray[curRow].max();

								covX = gvArray[curRow].meanx();
								covY = gvArray[curRow].meany();
								covZ = gvArray[curRow].meanz();

								sovX = gvArray[curRow].stdx();
								sovY = gvArray[curRow].stdy();
								sovZ = gvArray[curRow].stdz();

								comX = gvArray[curRow].wmeanx();
								comY = gvArray[curRow].wmeany();
								comZ = gvArray[curRow].wmeanz();

								gradX = gvArray[curRow].gradx();
								gradY = gvArray[curRow].grady();
								gradZ = gvArray[curRow].gradz();

								angT = gvArray[curRow].angVec(0);
								gtStr = gvArray[curRow].getTensorString();

							} else {
								System.out
										.println("ERROR: Index NOT IN MAP!!!...Index:"
												+ curRow
												+ " Objects:"
												+ maxGroup);
							}
							if (meancol)
								outString += ", " + valMean;
							if (stdcol)
								outString += ", " + valStd;
							if (mincol)
								outString += ", " + valMin;
							if (maxcol)
								outString += ", " + valMax;
							if (cntcol)
								outString += ", " + valCnt;
							// CoV
							if (covcol) {
								outString += ", " + covX;
								outString += ", " + covY;
								outString += ", " + covZ;
							}
							// SoV
							if (sovcol) {
								outString += ", " + sovX;
								outString += ", " + sovY;
								outString += ", " + sovZ;
							}
							// CoM
							if (comcol) {
								outString += ", " + comX;
								outString += ", " + comY;
								outString += ", " + comZ;
							}
							// Grad
							if (gradcol) {
								outString += ", " + gradX;
								outString += ", " + gradY;
								outString += ", " + gradZ;
							}
							// Angle
							if (angcol) {
								outString += ", " + angT;
							}
							if (includeShapeTensor)
								outString += gtStr;
							outString += "\n";
							out.append(outString);

						} else {
							if (!insFile.fileDone)
								System.out
										.println("ERROR: Line missing LACUNA_NUMBER column...");

						}
					}
					insFile.close();
				} else if (lacunaMode) {

					// Write Lacuna Style Output File
					for (int cGroup = 1; cGroup <= maxGroup; cGroup++) {
						if ((gvArray[cGroup].count() > 5)) { // At least 5
							// voxels
							String lacString = "";
							lacString = cGroup + ", 0 ," + mapA.getElSize().x
									+ "," + mapA.getElSize().y + ","
									+ mapA.getElSize().z;
							// Position
							lacString += "," + gvArray[cGroup].meanx() + ","
									+ gvArray[cGroup].meany() + ","
									+ gvArray[cGroup].meanz();
							// STD
							lacString += "," + gvArray[cGroup].stdx() + ","
									+ gvArray[cGroup].stdy() + ","
									+ gvArray[cGroup].stdz();
							// Projection XYZ
							lacString += "," + gvArray[cGroup].rangex() + ","
									+ gvArray[cGroup].rangey() + ","
									+ gvArray[cGroup].rangez();
							// PCA Components
							for (int cpca = 0; cpca < 3; cpca++) {
								lacString += ","
										+ gvArray[cGroup].getComp(cpca)[0]
										+ ","
										+ gvArray[cGroup].getComp(cpca)[1]
										+ ","
										+ gvArray[cGroup].getComp(cpca)[2]
										+ "," + gvArray[cGroup].getScore(cpca);
							}
							lacString += "," + gvArray[cGroup].rangep1() + ","
									+ gvArray[cGroup].rangep2() + ","
									+ gvArray[cGroup].rangep3();
							// Radius
							lacString += "," + gvArray[cGroup].radius() + ","
									+ gvArray[cGroup].stdr();
							// Volume
							lacString += ","
									+ gvArray[cGroup].count()
									+ ","
									+ (gvArray[cGroup].rangep1()
											* gvArray[cGroup].rangep2() * gvArray[cGroup]
												.rangep3());
							if (useGFILT | boxDist) {
								// Grad X,y,z, angle
								lacString += ", " + gvArray[cGroup].gradx()
										+ ", " + gvArray[cGroup].grady() + ","
										+ gvArray[cGroup].gradz() + ","
										+ gvArray[cGroup].angVec(0);
								// Distance mean, cov, std
								lacString += ", " + gvArray[cGroup].mean()
										+ "," + gvArray[cGroup].mean() + ","
										+ gvArray[cGroup].std();
							}
							if (includeShapeTensor)
								lacString += gvArray[cGroup].getTensorString();
							out.append(lacString + "\n");
						}
					}

				} else {
					headerStr = "";
					headerStr += "Total Number of voxels 	:" + totVox;
					if (useGFILT)
						headerStr += "	Scaled By = " + 1.0
								/ gfiltA.getShortScaleFactor();
					headerStr += "\n Mean =	"
							+ totSum
							/ totVox
							+ "	sd =	"
							+ Math.sqrt(totSqSum / totVox
									- Math.pow(totSum / totVox, 2))
							+ " 	Threshold =	" + threshVal + "\n";
					headerStr += "Mean_unit   =	" + totSum / totVox
							+ " 	[short intensity]\n";
					headerStr += "SD_unit     =	"
							+ Math.sqrt(totSqSum / totVox
									- Math.pow(totSum / totVox, 2))
							+ "	[short intensity]\n";
					headerStr += "Median_unit =	0 	[short intensity]\n";
					headerStr += "---------------------------------------------------\n";
					headerStr += maxGroup + "	Bins with Size	      "
							+ mapScaleFactor + "	each\n";
					headerStr += "BinSize in Units [";
					if (useFloat)
						headerStr += "Scaled";
					headerStr += "]	 " + mapScaleFactor + "	each\n";
					headerStr += "Region Number" + dlmChar + "Count";

					if (useGFILT)
						headerStr += dlmChar + "Gray-Mean" + dlmChar
								+ "Gray-Std" + dlmChar + "Gray-Min" + dlmChar
								+ "Gray-Max";
					// CoV
					if (covcol)
						headerStr += dlmChar + "COV-X" + dlmChar + "COV-Y"
								+ dlmChar + "COV-Z";
					// SoV
					if (sovcol)
						headerStr += dlmChar + "SOV-X" + dlmChar + "SOV-Y"
								+ dlmChar + "SOV-Z";
					// CoM
					if (comcol)
						headerStr += dlmChar + "COM-X" + dlmChar + "COM-Y"
								+ dlmChar + "COM-Z";
					// GRAD
					if (gradcol)
						headerStr += dlmChar + "GRAD-X" + dlmChar + "GRAD-Y"
								+ dlmChar + "GRAD-Z";
					// ANG
					if (angcol)
						headerStr += dlmChar + "ANGLE";

					// PCA Cols

					if (pcacols)
						headerStr += dlmChar + "PCA1-X" + dlmChar + "PCA1-Y"
								+ dlmChar + "PCA1-Z" + dlmChar + "PCA1-S";
					if (pcacols)
						headerStr += dlmChar + "PCA2-X" + dlmChar + "PCA2-Y"
								+ dlmChar + "PCA3-Z" + dlmChar + "PCA1-S";
					if (pcacols)
						headerStr += dlmChar + "PCA3-X" + dlmChar + "PCA3-Y"
								+ dlmChar + "PCA3-Z" + dlmChar + "PCA1-S";
					if (includeShapeTensor) {
						for (final char cX : "XYZ".toCharArray()) {
							for (final char cY : "XYZ".toCharArray()) {
								headerStr += dlmChar + "SHAPET_" + cX + "" + cY;
							}
						}
					}
					headerStr += "\n";

					out.append(headerStr);
					for (int cGroup = 1; cGroup < maxGroup; cGroup++) {
						if ((gvArray[cGroup].count() < 1) && (noBlank)) {
							// Do Nothing
							// Since there are no voxels and blank is suppressed
						} else {
							String extraColString = "";
							if (useGFILT | boxDist)
								extraColString += dlmChar
										+ gvArray[cGroup].mean() + dlmChar
										+ gvArray[cGroup].std() + dlmChar
										+ gvArray[cGroup].min() + dlmChar
										+ gvArray[cGroup].max();
							// CoV
							if (covcol)
								extraColString += dlmChar
										+ gvArray[cGroup].meanx() + dlmChar
										+ gvArray[cGroup].meany() + dlmChar
										+ gvArray[cGroup].meanz();

							// SoV
							if (sovcol)
								extraColString += dlmChar
										+ gvArray[cGroup].stdx() + dlmChar
										+ gvArray[cGroup].stdy() + dlmChar
										+ gvArray[cGroup].stdz();

							// CoM
							if (comcol)
								extraColString += dlmChar
										+ gvArray[cGroup].wmeanx() + dlmChar
										+ gvArray[cGroup].wmeany() + dlmChar
										+ gvArray[cGroup].wmeanz();

							// GRAD
							if (gradcol)
								extraColString += dlmChar
										+ gvArray[cGroup].gradx() + dlmChar
										+ gvArray[cGroup].grady() + dlmChar
										+ gvArray[cGroup].gradz();

							// Angle
							if (gradcol)
								extraColString += dlmChar
										+ gvArray[cGroup].angVec(0);

							// PCA Cols
							if (pcacols) {
								gvArray[cGroup].diag();
								for (int cpca = 0; cpca < 3; cpca++) {
									extraColString += dlmChar
											+ gvArray[cGroup].getComp(cpca)[0]
											+ dlmChar
											+ gvArray[cGroup].getComp(cpca)[1]
											+ dlmChar
											+ gvArray[cGroup].getComp(cpca)[2];
								}
							}
							if (includeShapeTensor)
								extraColString += gvArray[cGroup]
										.getTensorString(dlmChar);
							out.append(gvArray[cGroup].toString(dlmChar)
									+ extraColString + "\n");
						}
					}
				}
				out.flush();
				out.close();
			} catch (final Exception e) {
				System.out.println("Writing Output File Problem");
				e.printStackTrace();
			}
		} else {
			System.out.println("Files Not Present");
		}

		final float eTime = (System.currentTimeMillis() - start) / (60 * 1000F);

		String outString = "";
		outString += "Groups " + (maxGroup) + ", Groups/Second="
				+ ((maxGroup) / (eTime * 60.0)) + "\n";
		outString += "Run Finished in " + eTime + " mins @ " + new Date()
				+ "\n";
		System.out.println(outString);

	}

	private void SetupGA() {
		if (lacunaMode)
			System.out.println("-- Operating in Lacuna-File Output Mode");

		if (boxDist)
			useGFILT = false;

		useCount = !useGFILT;

		if (useCount) {
			meancol = false;
			stdcol = false;
			maxcol = false;
			mincol = false;
			cntcol = true;
		}

	}

}
