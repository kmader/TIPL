package tipl.tools;

//import java.awt.*;
//import java.awt.image.*;
//import java.awt.image.ColorModel.*;

import tipl.formats.FImage;
import tipl.formats.PureFImage;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.*;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

// Used as a replacement for the moment function as it allows much more control over data
// and communication with webservices (potentially?)

/**
 * Performs shape analysis on a labeled aim image (map) and if given another
 * image
 */
public class GrayAnalysis2D extends Hist2D implements ITIPLPluginIn {
    @TIPLPluginManager.PluginInfo(pluginType = "GrayAnalysis2D",
            desc = "Full memory 2D grayscale analysis",
            sliceBased = false,
            maximumSize = 1024 * 1024 * 1024)
    final public static TIPLPluginManager.TIPLPluginFactory myFactory = new TIPLPluginManager.TIPLPluginFactory() {
        @Override
        public ITIPLPlugin get() {
            return new GrayAnalysis2D();
        }
    };
    public static boolean doPreload = false;
    public static int supportedCores = Runtime.getRuntime()
            .availableProcessors();
    /**
     * how many cores does the plugin want (-1 = as many as possible)
     */
    public static int neededCores = -1;
    protected static boolean profileAsList = false;
    final int DEFAULTBINS = 1000;
    public boolean invertGFILT = false;
    protected DiscreteReader dmapA;
    protected DiscreteReader dmapB;
    /**
     * Value to use for the threshold (0)
     */
    protected float threshVal = 0;
    protected String analysisName = "GrayAnalysis2D";
    protected String gfiltName = "";
    TImg gfiltAim;
    double totVox = 0;
    double totSum = 0;
    double totSqSum = 0;
    boolean debugMode = false;
    boolean useCount;
    ExecutorService executor;

    /**
     * Simple initializer
     */
    protected GrayAnalysis2D() {
        if (neededCores < 1) {
            System.out.println(getPluginName() + " has " + neededCores
                    + " number of cores, defaulting to :" + supportedCores);
            neededCores = supportedCores;

        }
        executor = new ScheduledThreadPoolExecutor(neededCores);
        dmapA = new DiscreteReader(executor);
        dmapB = new DiscreteReader(executor);

    }

    private static void checkHelp(final ArgumentParser p) {
        if (p.hasOption("?")) {
            System.out.println(" GrayAnalysis2D Processing Help");
            System.out.println(" 2D Histograms...");
            System.out.println(" Arguments::");
            System.out.println(" ");
            System.out.println(p.getHelp());
            System.exit(0);
        }
    }

    /**
     * The standard version of GrayAnalysis2D which is run from the command line
     */
    public static void main(final String[] args) {
        final String kVer = "140113_006";
        System.out.println(" Gray Value and Lacuna Analysis v" + kVer);
        System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
        final GrayAnalysis2D ga2 = new GrayAnalysis2D();
        final ArgumentParser p = TIPLGlobal.activeParser(args);
        final boolean doProfile = p.getOptionBoolean("profile",
                "Run Profile Command");
        ga2.setParameter(p, "");
        if (doProfile) {
            final String aimName = p.getOptionString("input", "",
                    "Input Aim-File"); // CSV file is a needed parameter

            final int profileMode = p.getOptionInt("profilemode", 0,
                    "0 - Cyl: RZ, 1 - Cyl: RTheta, 2 - Sph: PhiTheta"); // CSV
            // file
            // is a
            // needed
            // parameter
            checkHelp(p);
            if (aimName.length() > 0) {
                final TImg myAim = TImgTools.ReadTImg(aimName);
                switch (profileMode) {
                    case 0:
                        GrayAnalysis2D.StartRZProfile(myAim, ga2.outCsvName, 0.0f, 1000);
                        break;
                    case 1:
                        GrayAnalysis2D.StartRThProfile(myAim, ga2.outCsvName, 0.0f, 1000);
                        break;
                    case 2:
                        GrayAnalysis2D.StartPolePlot(myAim, ga2.outCsvName, 0.0f, 1000);
                        break;
                    default:
                        System.out.println("Profile Mode : " + profileMode
                                + " is not supported, check help!");
                        checkHelp(p);
                }

            }
        } else {
            final String gfiltName = p.getOptionString("float", "",
                    "The value channel"); // gfilt is a given parameter

            if (gfiltName.length() > 0) ga2.gfiltAim = TImgTools.ReadTImg(gfiltName);
            p.checkForInvalid();
            checkHelp(p);
            ga2.execute();
        }

    }

    /**
     * Create a profile (counting) based on 2 FImage Classes
     *
     * @param inGfilt    value file to histogram
     * @param profImageA function image to use as the x map
     * @param profImageB function image to use as the y map
     * @param outFile    path and name of output file
     * @param theshVal   the threshold value to use fro the inGfilt image
     * @param fbinsA     the number of bins to use for the x map
     * @param fbinsB     the number of bins to use for the y map
     */
    public static void StartFProfile(final TImg inGfilt,
                                     final FImage profImageA, final FImage profImageB,
                                     final String outFile, final float threshVal, final int fbinsA,
                                     final int fbinsB) {
        final GrayAnalysis2D newGray = new GrayAnalysis2D();
        GrayAnalysis2D.doPreload = false;

        double[] rng = profImageA.getRange();
        newGray.dmapA.LoadData(profImageA);
        newGray.dmapA.fmin = rng[0];
        newGray.dmapA.fmax = rng[1];
        newGray.dmapA.fbins = fbinsA;

        rng = profImageB.getRange();
        newGray.dmapB.LoadData(profImageB);
        newGray.dmapB.fmin = rng[0];
        newGray.dmapB.fmax = rng[1];
        newGray.dmapB.fbins = fbinsB;

        newGray.outCsvName = outFile;
        newGray.gfiltAim = inGfilt;
        newGray.threshVal = threshVal;
        newGray.SetupGA();
        newGray.asList = profileAsList;
        newGray.executeUsing(gvMean());
    }

    /**
     * Create a profile (counting) based on 2 FImage Classes
     *
     * @param inGfilt    value file to histogram
     * @param profImageA function image to use as the x map
     * @param profImageB function image to use as the y map
     * @param outFile    path and name of output file
     * @param theshVal   the threshold value to use fro the inGfilt image
     * @param fbinsA     the number of bins to use for the x map
     * @param fbinsB     the number of bins to use for the y map
     */
    public static void StartFProfile(final TImg inGfilt,
                                     final PureFImage profImageA, final PureFImage profImageB,
                                     final String outFile, final float threshVal, final int fbinsA,
                                     final int fbinsB) {
        final GrayAnalysis2D newGray = new GrayAnalysis2D();
        GrayAnalysis2D.doPreload = false;

        double[] rng = profImageA.getRange();
        newGray.dmapA.LoadData(profImageA);
        newGray.dmapA.fmin = rng[0];
        newGray.dmapA.fmax = rng[1];
        newGray.dmapA.fbins = fbinsA;

        rng = profImageB.getRange();
        newGray.dmapB.LoadData(profImageB);
        newGray.dmapB.fmin = rng[0];
        newGray.dmapB.fmax = rng[1];
        newGray.dmapB.fbins = fbinsB;

        newGray.outCsvName = outFile;
        newGray.gfiltAim = inGfilt;
        newGray.threshVal = threshVal;
        newGray.SetupGA();
        newGray.asList = profileAsList;
        newGray.executeUsing(gvMean());
    }

    /**
     * Standard operation for a histogram
     *
     * @param mapA     map for columns A
     * @param mapB     map for rows B
     * @param gfiltAim map for values
     * @param outFile  path and name of output file
     */
    public static void StartHistogram(final TImg mapA, final TImg mapB,
                                      final TImg gfiltAim, final String outFile) {
        final GrayAnalysis2D newGray = new GrayAnalysis2D();

        newGray.dmapA.LoadData(mapA);
        newGray.dmapB.LoadData(mapB);
        newGray.outCsvName = outFile;
        newGray.gfiltAim = gfiltAim;

        newGray.SetupGA();
        newGray.execute();
    }

    /**
     * Standard operation for a histogram with the value plotted against the
     * radius and phi in spherical coordinates
     *
     * @param inGfilt value file to histogram
     * @param outFile path and name of output file
     */
    public static void StartPolePlot(final TImg inGfilt, final String outFile,
                                     final float threshVal, final int fbins) {
        final TImg cachedGfilt = TImgTools.WrapTImgRO(TImgTools
                .CacheImage(inGfilt));
        final PureFImage cFImgA = new PureFImage.PhiImageSph(cachedGfilt, 3);
        final PureFImage cFImgB = new PureFImage.ThetaImageCyl(cachedGfilt, 3);
        StartFProfile(cachedGfilt, cFImgA, cFImgB, outFile, threshVal, fbins,
                fbins);
    }

    /**
     * Standard operation for a histogram with the value plotted against the
     * radius and theta in cylindrical coordinates
     *
     * @param inGfilt value file to histogram
     * @param outFile path and name of output file
     */
    public static void StartRThProfile(final TImg inGfilt,
                                       final String outFile, final float threshVal, final int fbins) {
        final TImg cachedGfilt = TImgTools.WrapTImgRO(TImgTools
                .CacheImage(inGfilt));
        final PureFImage cFImgA = new PureFImage.RImageCyl(cachedGfilt, 3);
        final PureFImage cFImgB = new PureFImage.ThetaImageCyl(cachedGfilt, 3);
        StartFProfile(cachedGfilt, cFImgA, cFImgB, outFile, threshVal, fbins,
                fbins);

    }

    /**
     * Standard operation for a histogram with the value plotted against the
     * radius and z (in cylindrical coordinates)
     *
     * @param inGfilt value file to histogram
     * @param outFile path and name of output file
     */
    public static void StartRZProfile(final TImg inGfilt, final String outFile,
                                      final float threshVal, final int fbins) {
        final TImg cachedGfilt = TImgTools.WrapTImgRO(TImgTools
                .CacheImage(inGfilt));

        final PureFImage cFImgA = new PureFImage.RImageCyl(cachedGfilt, 3);
        final PureFImage cFImgB = new PureFImage.ZImage(cachedGfilt, 1);
        StartFProfile(cachedGfilt, cFImgA, cFImgB, outFile, threshVal, fbins,
                fbins);
    }

    /**
     * Standard operation for a histogram with the value plotted against the
     * theta and z in cylindrical coordinates
     *
     * @param inGfilt value file to histogram
     * @param outFile path and name of output file
     */
    public static void StartThZProfile(final TImg inGfilt,
                                       final String outFile, final float threshVal, final int fbins) {
        final TImg cachedGfilt = TImgTools.WrapTImgRO(TImgTools
                .CacheImage(inGfilt));
        final PureFImage cFImgA = new PureFImage.ThetaImageCyl(cachedGfilt, 3);
        final PureFImage cFImgB = new PureFImage.ZImage(cachedGfilt, 1);
        StartFProfile(cachedGfilt, cFImgA, cFImgB, outFile, threshVal, fbins,
                fbins);

    }

    @Override
    public ArgumentParser setParameter(final ArgumentParser p,
                                       final String prefix) {
        final ArgumentParser args = super.setParameter(p, prefix);
        doPreload = p
                .getOptionBoolean(prefix + "preload", "preload data");
        // Float Binning Settings
        dmapA.getArguments(p, prefix + "a", " of MapA (rows) ");
        dmapB.getArguments(p, prefix + "b", " of MapB (columns)");
        outCsvName = p.getOptionString(prefix + "csv", outCsvName,
                "Output csv filename"); // CSV file is a needed parameter

        profileAsList = p.getOptionBoolean(prefix + "aslist", profileAsList,
                "Out Profile Command as a list");
        invertGFILT = p.getOptionBoolean("invert", "Invert float data");
        SetupGA();

        return args;
    }

    private void AnalyzeSlice(final int sliceNumber, final boolean noThresh,
                              final int operationMode) {
        // Operation Mode -> 0- find COM/COV, 1- find covariance matrix, 2- find
        // extents
        if (debugMode)
            System.out.println("Reading MapSlice " + sliceNumber + "/"
                    + dmapA.getDim().z);
        // Read in the indices
        final int[] mapSliceA = dmapA.getSliceIndices(sliceNumber);
        final int[] mapSliceB = dmapB.getSliceIndices(sliceNumber);

        int[] gfiltSlice = new int[1];
        float[] fgfiltSlice = new float[1];
        final TImg.TImgFull fullGfilt = new TImg.TImgFull(gfiltAim);
        if (gfiltAim != null) {
            switch (gfiltAim.getImageType()) {
                case 0:
                case 1:
                    final short[] sgfiltSlice = fullGfilt
                            .getShortArray(sliceNumber);
                    gfiltSlice = new int[sgfiltSlice.length];
                    for (int cIndex = 0; cIndex < sgfiltSlice.length; cIndex++)
                        gfiltSlice[cIndex] = sgfiltSlice[cIndex];
                    break;
                case 2:
                    gfiltSlice = fullGfilt.getIntArray(sliceNumber);
                    break;
                case 3:
                    fgfiltSlice = fullGfilt.getFloatArray(sliceNumber);
                    break;
                default:
                    System.err.println("Gfilt" + gfiltAim + ":Type "
                            + gfiltAim.getImageType() + " is not supported");
                    return;
            }
        }

        double cVal;
        if (debugMode)
            System.out.println("Reading Points " + mapSliceA.length);
        for (int cIndex = 0; cIndex < mapSliceA.length; cIndex++) {
            if (gfiltAim != null) {
                if (gfiltAim.getImageType() == 3) {
                    cVal = fgfiltSlice[cIndex];
                } else {
                    cVal = gfiltSlice[cIndex];
                    if (Math.abs(gfiltAim.getShortScaleFactor()) > 1e-6)
                        cVal *= gfiltAim.getShortScaleFactor();
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
                final Double[] cPos = dmapA.getXYZVec(cIndex, sliceNumber);
                if (operationMode == 0) {
                    addVox(mapSliceB[cIndex], mapSliceA[cIndex],
                            cPos[0], cPos[1],
                            cPos[2], cVal);
                } else if (operationMode == 1) { // Not used yet
                    addCovVox(mapSliceB[cIndex], mapSliceA[cIndex],
                            cPos[0], cPos[1],
                            cPos[2]);
                } else if (operationMode == 2) { // Not used yet
                    setExtentsVoxel(mapSliceB[cIndex], mapSliceA[cIndex],
                            cPos[0], cPos[1],
                            cPos[2]);
                }

            }

        }
        if (debugMode)
            System.out.println("Done Reading Points " + mapSliceA.length);
    }

    @Override
    public String getPluginName() {
        return "GrayAnalysis2D";
    }

    private void processSlices() {
        final long start = System.currentTimeMillis();
        System.out.println("Reading Slices... " + dmapA.getDim().z);
        for (int cSlice = 0; cSlice < dmapA.getDim().z; cSlice++) {
            if (debugMode)
                System.out.println("Reading Slices " + cSlice + "/"
                        + dmapA.getDim().z);
            AnalyzeSlice(cSlice, true, 0);
        }

        System.out.println("Done Reading..."
                + (System.currentTimeMillis() - start) / (60 * 1000F)
                + "mins; Voxels:" + totVox);
    }

    public boolean execute() {

        if (gfiltAim != null)
            return executeUsing(gvMean());
        else
            return executeUsing(gvCount());
    }

    public boolean executeUsing(final GrayVoxExtract gve) {
        curGVE = gve;
        long start = System.currentTimeMillis();
        boolean gfiltGood = true;
        if (gfiltAim != null)
            gfiltGood = gfiltAim.isGood();
        final int maxGroup = dmapA.getBins() * dmapB.getBins();
        if (dmapA.isGood() & dmapB.isGood() & gfiltGood) {
            // Setup Bin Translators
            xBNE = new BinExtract() {
                @Override
                public String get(final int binNum) {
                    return "" + dmapB.ind2f(binNum) + "";
                }

                @Override
                public int getBin(final float fval) {
                    return dmapB.f2ind(fval);
                }

                @Override
                public String name() {
                    return dmapB.getSampleName();
                }
            };
            yBNE = new BinExtract() {
                @Override
                public String get(final int binNum) {
                    return "" + dmapA.ind2f(binNum) + "";
                }

                @Override
                public int getBin(final float fval) {
                    return dmapB.f2ind(fval);
                }

                @Override
                public String name() {
                    return dmapA.getSampleName();
                }
            };

            writeHeader(dmapA.getSampleName(), dmapA.getPath(), gfiltName,
                    dmapA.getDim(), dmapA.getOffset(), dmapA.getPos(),
                    dmapA.getElSize());
            initHistogram(dmapB.getBins(), dmapA.getBins());

            // Restart running time
            start = System.currentTimeMillis();
            processSlices();
            System.currentTimeMillis();

            TIPLGlobal.runGC();
            String extraInfo = "";
            if (gfiltAim != null)
                extraInfo += "	Scaled By = " + 1.0
                        / gfiltAim.getShortScaleFactor();
            extraInfo += "\n Mean =	"
                    + totSum
                    / totVox
                    + "	sd =	"
                    + Math.sqrt(totSqSum / totVox
                    - Math.pow(totSum / totVox, 2)) + " 	Threshold =	"
                    + threshVal + "\n";
            extraInfo += "Mean_unit   =	" + totSum / totVox
                    + " 	[short intensity]\n";
            extraInfo += "SD_unit     =	"
                    + Math.sqrt(totSqSum / totVox
                    - Math.pow(totSum / totVox, 2))
                    + "	[short intensity]";
            writeHistogram(extraInfo);
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
        return true;

    }

    /**
     * basic setup things *
     */
    private void SetupGA() {

        if (doPreload) {
            System.out.println("Preloading Datasets..." + dmapA);
            dmapA.CacheFullImage();
            dmapB.CacheFullImage();
            if (gfiltAim != null)
                gfiltAim = TImgTools.WrapTImgRO(TImgTools.CacheImage(gfiltAim));
            ;
        }
        dmapA.prescan();
        dmapB.prescan();
        executor.shutdown();

    }

    public void SetupGA(final TImg mapA, final TImg mapB) {
        dmapA.LoadData(mapA);
        dmapB.LoadData(mapB);
    }

    @Override
    public void LoadImages(TImgRO[] inImages) {
        // TODO Auto-generated method stub

    }

    public class DiscreteReader {
        /**
         * min float value *
         */
        public double fmin = 0;
        /**
         * max float value *
         */
        public double fmax = 0;
        protected TImgRO img = null;
        protected String htext = null;
        ExecutorService exec;
        boolean asFloat = true;
        boolean doPrescan = false;
        /**
         * number of bins *
         */
        public int fbins = DEFAULTBINS;
        boolean valuesWaiting = false;
        String ObjName = "";
        Future<double[]> psvals = null;

        public DiscreteReader() {
            exec = new ScheduledThreadPoolExecutor(4);
        }

        public DiscreteReader(final ExecutorService iexec) {
            exec = iexec;
        }

        public void CacheFullImage() {
            img = TImgTools.CacheImage(img);
        }

        /**
         * check to see if the values are back from the executor service yet *
         */
        public void checkVals() {
            if (valuesWaiting) {
                try {
                    final double[] psv = psvals.get();
                    fmin = psv[0];
                    fmax = psv[3];
                    valuesWaiting = false;
                    if (!asFloat)
                        fbins = (int) (fmax - fmin);
                    htext = null;
                    htext = headerText() + ", Mean:" + psv[1] + ", Std:"
                            + psv[2];
                } catch (final Exception e) {
                    System.out.println("ERROR Object Scanning " + psvals
                            + " Failed...!!!");
                    e.printStackTrace();
                }
            }
        }

        /**
         * convert a float value into an index *
         */
        public int f2ind(final double val) {
            checkVals();
            if (val <= fmin)
                return 0;
            if (val >= fmax)
                return fbins - 1;
            return (int) (((val - fmin) / (fmax - fmin) * fbins));
        }

        public void getArguments(final ArgumentParser p, final String prefix,
                                 final String suffix) {
            ObjName = p.getOptionString(prefix + "_file", "",
                    "Name of map aim file to open," + suffix);
            fmin = p.getOptionDouble(prefix + "_fmin", 0,
                    "The minimum value to use for rebinning," + suffix);
            fmax = p.getOptionDouble(prefix + "_fmax", 32766,
                    "The minimum value to use for rebinning," + suffix);
            fbins = p.getOptionInt(prefix + "_fbins", 32765,
                    "The minimum value to use for rebinning," + suffix);
            asFloat = p.getOptionBoolean(prefix + "_asfloat",
                    "Read data as float value (metric) or as integer values (labels),"
                            + suffix);
            doPrescan = p.getOptionBoolean(prefix + "_prescan",
                    "Do prescan the aims (ignore fmin/fmax )" + suffix);
            if (ObjName.length() > 0)
                img = TImgTools.ReadTImg(ObjName);
        }

        public int getBins() {
            checkVals();
            if (asFloat)
                return fbins;
            else
                return (int) (fmax - fmin + 1);
        }

        public D3int getDim() {
            return img.getDim();
        }

        public D3float getElSize() {
            return img.getElSize();
        }

        public D3int getOffset() {
            return img.getOffset();
        }

        public String getPath() {
            return img.getPath();
        }

        public D3int getPos() {
            return img.getPos();
        }

        public String getSampleName() {
            return img.getSampleName();
        }

        public int[] getSliceIndices(final int sliceNumber) {
            int[] outslice = null;
            final TImg.TImgFull fullImg = new TImg.TImgFull(img);
            if (asFloat) {
                final float[] fmapSlice = fullImg.getFloatArray(sliceNumber);
                outslice = new int[fmapSlice.length];
                for (int cIndex = 0; cIndex < fmapSlice.length; cIndex++)
                    outslice[cIndex] = f2ind(fmapSlice[cIndex]);
            } else {
                outslice = fullImg.getIntArray(sliceNumber);
                for (int cIndex = 0; cIndex < outslice.length; cIndex++)
                    outslice[cIndex] = i2ind(outslice[cIndex]);
            }
            return outslice;
        }

        public Double[] getXYZVec(final int cIndex, final int cSlice) {
            return TImgTools.getXYZVecFromVec(img, cIndex, cSlice);
        }

        public String headerText() {
            if (htext == null)
                return "[" + fmin + " - " + fmax + "] in " + getBins()
                        + ", Float:" + asFloat;
            else
                return htext;
        }

        /**
         * convert an integer value into an index, fbins is ignored *
         */
        public int i2ind(final int val) {
            checkVals();
            if (val < fmin)
                return 0;
            if (val >= fmax)
                return fbins - 1;
            return (int) (val - fmin);
        }

        /**
         * convert an index into an float *
         */
        public float ind2f(final int val) {
            checkVals();
            if (val <= 0)
                return (float) fmin;
            if (val >= fbins)
                return (float) fmax;
            return (float) (val * (fmax - fmin) / (fbins + 0.0) + (float) fmin);
        }

        /**
         * convert an index value into an integer value, fbins is ignored *
         */
        public int ind2i(final int val) {
            checkVals();
            if (val < 0)
                return (int) fmin;
            if (val >= fmax)
                return (int) fmax;
            return (int) (val + fmin);
        }

        public boolean isGood() {
            return img.isGood();
        }

        public void LoadData(final TImgRO iImg) {
            img = iImg;
            prescan();
        }

        public void LoadData(final TImgRO iImg, final int ibins) {
            img = iImg;
            fbins = ibins;
            prescan();
        }

        private void prescan() {
            if (doPrescan) {
                psvals = exec.submit(new prescanRunner(this.img));
                valuesWaiting = true;
            }
        }

        /**
         * Run the distance label initialization routines in parallel
         */
        private class prescanRunner implements Callable<double[]> {
            public double minVal = 0;
            public double maxVal = 0;
            public double meanVal = 0;
            public double stdVal = 0;
            public double voxCnt = 0;
            public boolean firstIn = false;
            protected double sqVal = 0;
            volatile TImgRO img;

            public prescanRunner(final TImgRO iImg) {
                img = iImg;
            }

            @Override
            public double[] call() {
                System.out.println("Prescanner Started:, <" + img + "> -- ["
                        + minVal + ", (" + meanVal + "," + stdVal + "), "
                        + maxVal + "]");
                final TImg.TImgFull fullImg = new TImg.TImgFull(img);
                for (int sliceNumber = 0; sliceNumber < img.getDim().z; sliceNumber++) {
                    final float[] fmapSlice = fullImg
                            .getFloatArray(sliceNumber);
                    if (!firstIn) {
                        minVal = fmapSlice[0];
                        maxVal = fmapSlice[0];
                        firstIn = true;
                    }
                    for (float aFmapSlice : fmapSlice) {
                        voxCnt++;
                        meanVal += aFmapSlice;
                        sqVal += aFmapSlice * aFmapSlice;
                        if (aFmapSlice > maxVal)
                            maxVal = aFmapSlice;
                        if (aFmapSlice < minVal)
                            minVal = aFmapSlice;
                    }
                }
                meanVal /= voxCnt;
                stdVal = Math.sqrt(sqVal / voxCnt - meanVal * meanVal);
                System.out.println("Prescanner Finished:, <" + img + "> -- ["
                        + minVal + ", (" + meanVal + "," + stdVal + "), "
                        + maxVal + "]");
                return new double[]{minVal, meanVal, stdVal, maxVal};
            }
        }


    }

}
