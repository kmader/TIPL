package tipl.tools;


import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Hashtable;

import tipl.formats.FImage;
import tipl.formats.PureFImage;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;
import tipl.util.CSVFile;
import tipl.util.ITIPLPlugin;
import tipl.util.TIPLGlobal;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;

/**
 * Performs shape analysis on a labeled aim image (map) and if given another
 * image
 */
public class GrayAnalysis extends BaseTIPLPluginIn {
	@TIPLPluginManager.PluginInfo(pluginType = "GrayAnalysis",
			desc="Full memory gray value analysis",
			sliceBased=false)
	final public static TIPLPluginManager.TIPLPluginFactory myFactory = new TIPLPluginManager.TIPLPluginFactory() {
		@Override
		public ITIPLPlugin get() {
			return new GrayAnalysis();
		}
	};
	@Override
	public String getPluginName() {
		return "GrayAnalysis";
	}
	
	public static final String kVer = "03-06-14 v 104";
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
	public static ITIPLPlugin AddDensityColumn(final TImgRO inDMap, final String inName,
			final String outName, final String aName) {
		final GrayAnalysis newGray = new GrayAnalysis(inDMap, inName, outName,
				aName);
		newGray.execute();
		return newGray;
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
	public static ITIPLPlugin AddDistanceColumn(final TImgRO inMap, final TImgRO inGfilt,
			final String inName, final String outName, final String aName) {
		final GrayAnalysis newGray = new GrayAnalysis(inMap, inGfilt, inName,
				outName, aName);
		newGray.analysisName = aName + "_Distance";
		newGray.mapA = inMap;
		newGray.useGFILT = true;
		newGray.gfiltA = inGfilt;

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
		newGray.execute();
		return newGray;
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
	public static ITIPLPlugin AddFixedColumn(final TImgRO inMap, final TImgRO inGfilt,
			final String inName, final String outName, final String aName) {
		final GrayAnalysis newGray = new GrayAnalysis(inMap, inGfilt, inName,
				outName, aName);
		newGray.execute();
		return newGray;
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
	public static ITIPLPlugin AddRegionColumn(final TImgRO inMap, final TImgRO inGfilt,
			final String inName, final String outName, final String aName) {

		final GrayAnalysis newGray = new GrayAnalysis(inMap, inGfilt, inName,
				outName, aName);
		newGray.lacunaMode = false;
		newGray.stdcol = true;
		newGray.covcol = false;
		newGray.mincol = false;
		newGray.maxcol = false;
		newGray.useThresh = false;
		newGray.execute();
		return newGray;

	}

	/** The standard version of grayanalysis which is run from the command line */
	public static void main(final String[] args) {

		System.out.println(" Gray Value and Lacuna Analysis v" + kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");

		final ArgumentParser p = TIPLGlobal.activeParser(args);
		if (p.hasOption("?")) {
			showHelp();
		}

		final GrayAnalysis myGrayAnalysis = new GrayAnalysis(p);
		myGrayAnalysis.execute();

	}

	/** Show's command line help when it is requested */
	@Deprecated
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
		newGray.execute();
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
		newGray.execute();
	}

	/**
	 * Standard operation for a histogram
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartHistogram(final TImgRO inGfilt,
			final String outFile) {
		StartHistogram(inGfilt, outFile, false);
	}

	/**
	 * Standard operation for a histogram
	 * 
	 * @param inGfilt
	 *            value file to histogram
	 * @param outFile
	 *            path and name of output file
	 */
	public static void StartHistogram(final TImgRO inGfilt,
			final String outFile, final boolean removeBlanks) {
		final GrayAnalysis newGray = new GrayAnalysis();
		newGray.mapA = inGfilt;
		newGray.csvName = outFile;
		newGray.gfiltA = null;
		newGray.mincol = false;
		newGray.maxcol = false;
		newGray.noBlank = removeBlanks;
		newGray.SetupGA();
		newGray.execute();
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
	public static void StartHistogram(final TImgRO inGfilt,
			final String outFile, final float minVal, final float maxVal,
			final int steps) {
		 StartHistogram(inGfilt, outFile, minVal, maxVal, steps, false);
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
	public static void StartHistogram(final TImgRO inGfilt,
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
		newGray.execute();
	}

	public static ITIPLPlugin StartLacunaAnalysis(final TImgRO inMap,
			final String outName, final String aName) {
		return StartLacunaAnalysis(inMap, outName, aName, false);
	}

	public static ITIPLPlugin StartLacunaAnalysis(final TImgRO inMap,
			final String outName, final String aName,
			final boolean includeShapeT) {
		final GrayAnalysis newGray = new GrayAnalysis(inMap, (TImg) null,
				outName, aName);
		newGray.includeShapeTensor = includeShapeT;
		newGray.execute();
		return newGray;
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
		newGray.execute();
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
	protected TImgRO mapA;
	protected double mapScaleFactor = 1.0;
	protected TImgRO gfiltA;
	protected int maxGroup = 0;
	
	static protected double totVox = 0;
	static protected double totSum = 0;
	static protected double totSqSum = 0;
	
	protected double fmin = 0;
	protected double fmax = 32765;
	static final int MAXARRVAL = 500000; // Integer.MAX_VALUE;
	/** the number of points to use in a standard float profile plot */
	public static int PROFILEPOINTS = 1000;
	public int fbins = MAXARRVAL;
	private boolean useGFILT;
	private boolean useFMap;
	public boolean invertGFILT;
	boolean useCount;
	// Parameters
	/** Use commas in output instead of tabs */
	protected boolean useComma = false;
	/** Use a threshold on the gfilt data */
	protected boolean useThresh = false;
	/** Add column with mean of gfilt data inside each object (true) */
	protected boolean meancol = true;

	/** Add column with std of gfilt data inside each object (true) */
	protected boolean stdcol = true;

	/** Add column with max of gfilt data inside each object (true) */
	protected boolean maxcol = false;

	/** Add column with min of gfilt data inside each object (true) */
	protected boolean mincol = false;

	/** Add column with the voxel count inside each object (false) */
	protected boolean cntcol = false;

	/** Add columns with the center of volumes for each object (false) */
	protected boolean covcol = false;

	/**
	 * Add columns with the center of mass (gfilt weighted) for each object
	 * (false)
	 */
	protected boolean comcol = false;

	/**
	 * Add columns with the std of the center of volumes for each object (false)
	 */
	protected boolean sovcol = false;

	/** Add columns with the gfilt-calculated gradient for each object (false) */
	protected boolean gradcol = false;

	/**
	 * Add columns with the angle between the objects main direction and the
	 * direction of the gradient (false)
	 */
	protected boolean angcol = false;

	/** Add columns with the PCA components and scores for each object (false) */
	protected boolean pcacols = false;

	protected boolean noThresh = false;

	/** write out the entire shape tensor **/
	protected boolean includeShapeTensor = false;

	protected boolean useShort = false;

	protected boolean useFloat = false;

	/**
	 * Perform the standard shape analysis and save as a list of objects inside
	 * a csv file. If false result is formatted more as a histogram or 2D
	 * histogram
	 */
	protected boolean lacunaMode = false;

	protected boolean noBlank = false;


	/** Calculate distance from wall as distance from edge of ROI volume */
	protected boolean boxDist = false;

	/** Value to use for the threshold (0) */
	protected float threshVal = 0;

	protected String analysisName = "GrayAnalysis";

	protected String dlmChar = "\t";

	protected String headerString = "";

	protected String headerStr = "";

	protected String csvName = "";

	protected String insName = "";

	protected String gfiltName = "";

	/** Plain old initializer */
	protected GrayAnalysis() {
	}
	
	@Override
	public ArgumentParser setParameter(ArgumentParser inArgs,final String prefix) {
		useComma = inArgs.getOptionBoolean(prefix+"usecsv",useComma,"Use commas in output");
		useThresh = inArgs.getOptionBoolean(prefix+"usethresh",useThresh,"use threshold value");

		// Columns to be written in output file
		meancol = !inArgs.getOptionBoolean(prefix+"meancol",meancol,"Add mean value column"); // Default Column
		stdcol = !inArgs.getOptionBoolean(prefix+"stdcol",stdcol,"Add standard deviation column"); // Default Column
		maxcol = inArgs.getOptionBoolean(prefix+"maxcol",maxcol,"Add max column"); // Special Column
		mincol = inArgs.getOptionBoolean(prefix+"mincol",mincol,"Add min column"); // Special Column
		cntcol = inArgs.getOptionBoolean(prefix+"cntcol",cntcol,"Add count column"); // Special Column
		covcol = inArgs.getOptionBoolean(prefix+"covcol",covcol,"Add covariance columns"); // Special Column
		comcol = inArgs.getOptionBoolean(prefix+"comcol",comcol,"Add center of mass columns"); // Special Column
		sovcol = inArgs.getOptionBoolean(prefix+"sovcol",sovcol,"Add std center of mass colums"); // Special Column
		gradcol = inArgs.getOptionBoolean(prefix+"gradcol",gradcol,"Add gradient columns"); // Special Column
		angcol = inArgs.getOptionBoolean(prefix+"angcol",angcol,"Add angular column (calculated from gradient"); // Special Column
		pcacols = inArgs.getOptionBoolean(prefix+"pcacols",pcacols,"Add principal component columns"); // Special Column
		noThresh = inArgs.getOptionBoolean(prefix+"nothresh",noThresh,"don use threshold");
		useShort = inArgs.getOptionBoolean(prefix+"useshort",useShort,"use short values for value image"); // use short for GFILT
		useFloat = inArgs.getOptionBoolean(prefix+"usefloat",useFloat,"use float values for value image"); // use float for MAP (just labels in
		// CSV file)
		lacunaMode = inArgs.getOptionBoolean(prefix+"lacuna",lacunaMode,"use lacuna mode");
		noBlank = inArgs.getOptionBoolean(prefix+"noblank",noBlank,"remove blank lines where count is zero");
		
		boxDist = inArgs.getOptionBoolean(prefix+"boxroidist",boxDist,"calculated distance based on a box of the region of interest (box edge distance)");
		includeShapeTensor = inArgs.getOptionBoolean(prefix+"shapetensor",includeShapeTensor,"add columns for shape tensor");
		invertGFILT = inArgs.getOptionBoolean(prefix+"invert",invertGFILT,"invert the values in the value image (gfilt)");
		useGFILT = inArgs.getOptionBoolean(prefix+"gfilt",useGFILT,"use a gfilt image");
		threshVal=inArgs.getOptionFloat(prefix+"thresh", threshVal, "Threshold value to use");
		fmin=inArgs.getOptionDouble(prefix+"fmin",fmin,"Min value for float binning of value image");
		fmax=inArgs.getOptionDouble(prefix+"fmax",fmax,"Max value for float binning of value image");
		fbins=inArgs.getOptionInt(prefix+"fmin",fbins,"Number of bines for float binning of value image");
		
		analysisName = inArgs.getOptionString(prefix+"analysis",analysisName,"Name of analysis");
		insName = inArgs.getOptionString(prefix+"insert",insName,"insert results into an existing csv");

		
		if (useComma)
			dlmChar = ", ";
		else
			dlmChar="\t";
		
		
		return inArgs;
	}
	@Deprecated
	public GrayAnalysis(final ArgumentParser p) {


		

		final String mapName = p.getOptionAsString("map"); // Map is a needed
															// parameter

		if (useGFILT)
			gfiltName = p.getOptionAsString("gfilt"); // gfilt is a given
														// parameter
		csvName = p.getOptionAsString("csv"); // CSV file is a needed parameter

			
		

		System.out.println("Map Aim: " + mapName);
		if (useGFILT)
			System.out.println("Gray Value Aim: " + gfiltName);
		System.out.println("Debug: " + TIPLGlobal.getDebug());
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
	@Deprecated
	public GrayAnalysis(final TImgRO inMap, final String outName,
			final String aName) {
		LoadImages(new TImgRO[] {inMap});
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
	@Deprecated
	public GrayAnalysis(final TImgRO inDMap, final String inName,
			final String outName, final String aName) {
		LoadImages(new TImgRO[] {inDMap});
		analysisName = aName;


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
	@Deprecated
	public GrayAnalysis(final TImgRO inMap, final TImgRO inGfilt,
			final String outName, final String aName) {
		LoadImages(new TImgRO[] {inMap,inGfilt});
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
		analysisName = aName;
		LoadImages(new TImgRO[] {inMap,inGfilt});

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
	@Deprecated
	public GrayAnalysis(final TImgRO inMap, final TImgRO inGfilt,
			final String inName, final String outName, final String aName,
			final boolean makemedist) {
		analysisName = aName + "_Distance";
		LoadImages(new TImgRO[] {inMap,inGfilt});

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
	/**
	 * The core of the grayanalysis tool which analyzes each slice that it is given. Made it static to keep the functions consequences clear
	 * @param sliceNumber
	 * @param noThresh
	 * @param operationMode -> 0- find COM/COV, 1- find covariance matrix, 2- find extents
	 */
	static protected int AnalyzeSlice(
			TImgRO mapA,TImgRO gfiltA,
			final GrayVoxels[] gvArray,
			final int sliceNumber, final boolean noThresh,final int operationMode,
			double fmin,double fmax,int fbins,boolean invertGFILT,int maxGroup,
			double threshVal,boolean useGFILT) {
		// 
		if (TIPLGlobal.getDebug())
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
				mapSlice[cIndex] = f2i(fmapSlice[cIndex],fmin,fmax,fbins);
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
			throw new IllegalArgumentException("Type " + mapA.getImageType()
					+ " is not supported");

		}

		

		int[] gfiltSlice = new int[1];
		float[] fgfiltSlice = new float[1];
		if (useGFILT) {
			if (TIPLGlobal.getDebug())
				System.out.println("Reading gfiltSlice " + sliceNumber + "/"
						+ gfiltA.getDim().z);
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
				throw new IllegalArgumentException("Gfilt" + gfiltA + ":Type "
						+ gfiltA.getImageType() + " is not supported");
			}
		}
		double cVal;
		
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
						if (TIPLGlobal.getDebug())
							System.out.println(" Value " + cVal
									+ " Above Max : " + fbins);
					}

				}
			}
		}
		if (TIPLGlobal.getDebug())
			System.out.println("Done Reading Points " + mapSlice.length);
		return maxGroup;
	}

	static private int f2i(final double val,double fmin, double fmax, int fbins) {
		if (val < fmin)
			return 0;
		if (val > fmax)
			return fbins;
		return (int) (((val - fmin) / (fmax - fmin) * fbins));
	}

	static private float i2f(final int val,double fmin, double fmax, int fbins) {
		if (val < 0)
			return (float) fmin;
		if (val > fbins)
			return (float) fmax;
		return (float) (val * (fmax - fmin) / (fbins + 0.0) + (float) fmin);
	}

	protected void writeHeader(boolean useInsert) {
		try {
			if (useInsert) {
				final CSVFile insFile = CSVFile.FromPath(insName, 2);
				for (int i = 0; i < 2; i++) {
					headerStr += insFile.getRawHeader(i);
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
	}
	protected GrayVoxels[] runAllSlices() {
		GrayVoxels[] gvArray = new GrayVoxels[fbins];
		// gvArray = new LinkedList<GrayVoxels>();
		// Initialize
		System.out.println("Initializing GrayVoxel Array...");

		// Faster To Put if outside of for-loop, methinks
		if (useFMap) {
			for (int cVox = 0; cVox < fbins; cVox++)
				gvArray[cVox] = new GrayVoxels(i2f(cVox,fmin,fmax,fbins));
		} else if (useFloat) {
			for (int cVox = 0; cVox < fbins; cVox++)
				gvArray[cVox] = new GrayVoxels(mapScaleFactor * cVox);
		} else {
			for (int cVox = 0; cVox < fbins; cVox++)
				gvArray[cVox] = new GrayVoxels(cVox);
		}

		long start = System.currentTimeMillis();
		System.out.println("Reading Slices... " + mapA.getDim().z);
		if ((mapA.getImageType() == 1) | (mapA.getImageType() == 2)
				| (mapA.getImageType() == 3)) {
			for (int cSlice = 0; cSlice < mapA.getDim().z; cSlice++) {
				if (TIPLGlobal.getDebug())
					System.out.println("Reading Slices " + cSlice + "/"
							+ mapA.getDim().z);
				maxGroup=AnalyzeSlice(mapA, gfiltA,gvArray,cSlice, 
						noThresh,0, fmin, fmax, fbins, invertGFILT, 
						maxGroup, threshVal, useGFILT);
					
			}
			System.out.println("Rescanning Slices for COV Matrix... "
					+ mapA.getDim().z);
			for (int cSlice = 0; cSlice < mapA.getDim().z; cSlice++) {
				if (TIPLGlobal.getDebug())
					System.out.println("Reading Slices " + cSlice + "/"
							+ mapA.getDim().z);
				maxGroup=AnalyzeSlice(mapA, gfiltA,gvArray,cSlice, 
						noThresh,1, fmin, fmax, fbins, invertGFILT, 
						maxGroup, threshVal, useGFILT);
			}
		} else {
			throw new IllegalArgumentException("ERROR: Map of type : "
					+ mapA.getImageType() + " not supported!");
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
					if (TIPLGlobal.getDebug())
						System.out.println("Reading Slices " + cSlice + "/"
								+ mapA.getDim().z);
					maxGroup=AnalyzeSlice(mapA, gfiltA,gvArray,cSlice, 
							noThresh,2, fmin, fmax, fbins, invertGFILT, 
							maxGroup, threshVal, useGFILT);
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
		
		if(TIPLGlobal.getDebug())  
			for(GrayVoxels curVox : gvArray) 
				if (curVox.count()>0) System.out.println(curVox.getLabel()+", "+curVox.toString()+"="+curVox.count());
		
		
		return gvArray;
	}
	protected void writeOutputToCSV(GrayVoxels[] gvArray,boolean useInsert) {
		try {
			
			final FileWriter out = new FileWriter(csvName, true);
			if (useInsert) {
				
				final CSVFile insFile = CSVFile.FromPath(insName, 2);
				// Insert values as last two columns
				while (!insFile.fileDone) {
					final Hashtable<String,String> cLine = insFile.lineAsDictionary();
					if (cLine.containsKey("lacuna_number")) {
						
						// the default values for the line
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
						
						String outString = insFile.readLine().getLine();
						
						final int curRow = (new Integer(
								cLine.get("lacuna_number")))
								.intValue();
						
						if (TIPLGlobal.getDebug()) 
							System.out.println("Processing Line:#"+curRow);
						
						
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
	} 
	protected GrayVoxels[] intGvArray = new GrayVoxels[0];
	/**
	 * getInfo for GrayAnalysis supports the request for 
	 * <li> bins which returns the GrayVoxels array
	 * <li> groups which returns the group count as a long
	 * 
	 */
	@Override
	public Object getInfo(String request) {
		String niceRequest=request.trim().toLowerCase();
		if(niceRequest.equalsIgnoreCase("bins")) return intGvArray;
		if(niceRequest.equalsIgnoreCase("groups")) return new Long(maxGroup);
		if(niceRequest.contains("average")) {
			
			String[] fullRequest= niceRequest.split(",");
			Method callMethod=null;
			try {
				callMethod=GrayVoxels.class.getDeclaredMethod(fullRequest[1], null);
			
				int grpCount=0;
				double valSum=0;
				for(GrayVoxels curVox: intGvArray) 
				
				if(curVox.count()>0) {
					grpCount++;
					valSum+=((Double) callMethod.invoke(curVox, null)).doubleValue();
					}
				return new Double(valSum*1.0/grpCount);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Method Could Not be found in "+GrayVoxels.class+e);
			}
		}
		if(niceRequest.equalsIgnoreCase("average_volume")) {
			int grpCount=0;
			long voxCount=0;
			for(GrayVoxels curVox: intGvArray) 
				if(curVox.count()>0) {
					grpCount++;
					voxCount+=curVox.count();
					}
			return new Double(voxCount*1.0/grpCount);
		}
		
		return super.getInfo(request);
	}
	/**
	 * Actually runs the grayanalysis code on the dataset, can be run inside of
	 * a thread
	 */
	@Override
	public boolean execute() {
		if (doPreload) {
			System.out.println("Preloading Datasets..." + mapA);
			mapA = TImgTools.WrapTImgRO(TImgTools.CacheImage(mapA));
			if (useGFILT)
				gfiltA = TImgTools.WrapTImgRO(TImgTools.CacheImage(gfiltA));
		}
		long start = System.currentTimeMillis();
		boolean gfiltGood = true;
		final boolean useInsert=!insName.equals(""); // if it is not empty
		if (useGFILT)
			gfiltGood = gfiltA.isGood();
		if ((mapA.isGood()) & (gfiltGood)) {
			writeHeader(useInsert);
			// Restart running time

			
			intGvArray=runAllSlices();

			TIPLGlobal.runGC();
			
			writeOutputToCSV(intGvArray,useInsert);
		} else {
			throw new IllegalArgumentException("Files Not Present");
		}
		final float eTime = (System.currentTimeMillis() - start) / (60 * 1000F);

		String outString = "";
		outString += "Groups " + (maxGroup) + ", Groups/Second="
				+ ((maxGroup) / (eTime * 60.0)) + "\n";
		outString += "Run Finished in " + eTime + " mins @ " + new Date()
				+ "\n";
		System.out.println(outString);
		return true;
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

	@Override
	public void LoadImages(TImgRO[] inImages) {
		if(inImages.length<2) {
			mapA=inImages[0];
			gfiltA=null;
			useGFILT=false;
		} else {
			mapA=inImages[0];
			gfiltA=inImages[1];
			useGFILT=true;
		}
		
	}

}
