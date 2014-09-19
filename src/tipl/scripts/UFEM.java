package tipl.scripts;

import tipl.formats.PureFImage;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.VirtualAim;
import tipl.settings.FilterSettings;
import tipl.spark.SparkGlobal;
import tipl.tools.*;
import tipl.util.*;
// doesn't make sense to declare this function again
import static tipl.blocks.BaseTIPLBlock.tryOpenImagePath;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Hashtable;

/**
 * Script to Process Femur Data (specifically for 1.4um mid-diaphysis femur)
 *
 * @author Kevin Mader
 *         <p/>
 *         This and most of my scripts contain an almost excessively high number
 *         of functions, I did it in this way to make it easier for the garbage
 *         collector to work by keeeping as many variables as possible as local
 *         variables inside these subfunctions. It also makes the code somewhat
 *         easier to read and potentially reuse
 *         <p/>
 *         Change Log:
 *         <p/>
 *         v26 added the ability to run without any mask at all (just a white
 *         image as a mask)
 *         <p/>
 *         v25 added maximum io threads and fixed parameter for maskdistance in
 *         the subscript
 *         <p/>
 *         v24 fixed the contour option to output just the max instead of the
 *         masked bone
 *         <p/>
 *         v23 added an option to skip contouring and just use a circle (same
 *         radius as the initial circle)
 *         <p/>
 *         v22 Fixed critical bug in Resizing masks
 *         <p/>
 *         v21 Started to change many of the functions from member to static
 *         functions to make debugging easier.
 *         <p/>
 *         v20 Changed the script to produce a thresheld image first and then
 *         create bone and porosity
 *         <p/>
 *         v19 Fixed the resume feature to work on bone/poros as a starting
 *         point (generate mask etc)
 *         <p/>
 *         v18 Added minimum memory request of 5GB so the program always runs
 *         <p/>
 *         v17 Added lacuna distance to output (distance away from lacuna
 *         surface)
 *         <p/>
 *         v16 Added morphological radius (the radius to use for the sphKernel
 *         function)
 *         <p/>
 *         v15 Start
 */
public class UFEM implements Runnable {
    /**
     * date and version number of the script *
     */
    public static final String kVer = "19-09-2013 v026";
    protected static double morphRadius;
    public final int UFEM_CANALDIST = 2;
    public final int UFEM_MASKDIST = 1;
    public final int UFEM_LACUNDIST = 3;
    public final int LASTSTAGE = 23;
    protected final TypedPath ufiltAimFile;
    protected final TypedPath gfiltAimFile;
    protected final TypedPath threshAimFile;
    protected final TypedPath boneAimFile;
    protected final TypedPath maskAimFile;
    protected final TypedPath porosAimFile;
    protected final TypedPath maskdistAimFile;
    protected final TypedPath thickmapAimFile;
    protected final TypedPath lmaskAimFile;
    protected final TypedPath cmaskAimFile;
    protected final TypedPath comboAimFile;
    protected final TypedPath lacunAimFile;
    protected final TypedPath canalAimFile;
    protected final TypedPath lacunVolsAimFile;
    protected final TypedPath lacunDistAimFile;
    protected final TypedPath canalVolsAimFile;
    protected final TypedPath canalDistAimFile;
    protected final TypedPath cdtoAimFile;
    protected final TypedPath cdtbAimFile;
    protected final TypedPath mdtoAimFile;
    private final boolean runAsJob;
    private final SGEJob jobToRun;
    protected TImg ufiltAim = null;
    protected TImg gfiltAim = null;
    protected TImg threshAim = null;
    protected TImg boneAim = null;
    protected TImg maskAim = null;
    protected TImg porosAim = null;
    protected TImg maskdistAim = null;
    protected TImg thickmapAim = null;
    protected TImg lmaskAim = null;
    protected TImg cmaskAim = null;
    protected TImg comboAim = null;
    protected TImg lacunAim = null;
    protected TImg canalAim = null;
    protected TImg lacunVolsAim = null;
    protected TImg lacunDistAim = null;
    protected TImg canalVolsAim = null;
    protected TImg canalDistAim = null;
    protected TImg cdtoAim = null;
    protected TImg cdtbAim = null;
    protected TImg mdtoAim = null;
    protected volatile int submittedJobs = 0;
    protected Hashtable workForThread = null;
    /**
     * How big do you think the data set is
     */
    protected D3int guessDim = new D3int(1024, 1024, 1024);
    TypedPath lacunCsv, canalCsv;
    String stageList;
    volatile boolean maskdistAimReady = true;
    volatile boolean canaldistAimReady = true;
    volatile boolean lacundistAimReady = true;
    boolean doResample, rmEdges;
    int upsampleFactor, downsampleFactor, threshVal, maskContourSteps,
            porosMaskPeel, stage;
    boolean doLaplace, doGradient, doGauss, doMedian, singleStep, resume, makePreviews,
            multiJobs, doFixMasks, doCL, removeMarrowCore, justCircle,
            pureWhiteMask;
    int closeIter, eroNH, dilNH, minVoxCnt;
    double eroOccup, dilOccup, maskContourBW;
    double lacMinVol, lacMaxVol, canMinVol, canMaxVol, minVolumePct,
            remEdgesRadius;
    private int smcOperation = 0;
    private volatile int ufemCores = 0;
    /**
     * ParameterSweeper needs to be able to instantiate the class to call the main
     */
    public UFEM() {
    	final TypedPath emptyPath = new TypedPath("");
        ufiltAimFile=emptyPath;
        gfiltAimFile=emptyPath;
        threshAimFile=emptyPath;
        boneAimFile=emptyPath;
        maskAimFile=emptyPath;
        porosAimFile=emptyPath;
        maskdistAimFile=emptyPath;
        thickmapAimFile=emptyPath;
        lmaskAimFile=emptyPath;
        cmaskAimFile=emptyPath;
        comboAimFile=emptyPath;
        lacunAimFile=emptyPath;
        canalAimFile=emptyPath;
        lacunVolsAimFile=emptyPath;
        lacunDistAimFile=emptyPath;
        canalVolsAimFile=emptyPath;
        canalDistAimFile=emptyPath;
        cdtoAimFile=emptyPath;
        cdtbAimFile=emptyPath;
        mdtoAimFile=emptyPath;
        runAsJob = false;
        jobToRun = null;
    }
    public UFEM(final ArgumentParser p) {

        stage = p
                .getOptionInt(
                        "stage",
                        1,
                        "1 - Filtering, 2 - Thresholding, 3 - Segmentation, 4 - Contouring, 5 - Mask Distance [parallel], 6 -  Classification, 7 - Labeling, 8 - Canal Voronoi Volumes / Distance Map [parallel], 9 - Lacuna Volumes [parallel], [10,11,12,13] - Lacuna Shape Analysis, 14 - Lacuna Neighbor Analysis, [15,16] - Canal Shape Analysis, 17 - Canal Neighbor Analysis, 18 - Canal Thickness Analysis, 19 - Canal Spacing Analysis, 20 - Mask Thickness Analysis, 21 - Canal Thickness Maps, 22 - Canal Spacing Maps, 23 - Mask Thickness Maps   (Histograms,etc) ");
        resume = p.getOptionBoolean("resume",
                "Resume based on which files are present");
        singleStep = p.getOptionBoolean("singlestep",
                "Run just the given stage");
        stageList = "" + stage;
        for (int cstg = stage + 1; cstg <= (singleStep ? stage : LASTSTAGE); cstg++)
            stageList += "," + cstg;
        stageList = p.getOptionString("stagelist", stageList,
                "Run the given stages (eg 1,2,3,5 )");
        ufemCores = p
                .getOptionInt(
                        "scriptMulticore",
                        1,
                        "Number of different stages of the script in parallel (mask distance generation for example)");


        multiJobs = p
                .getOptionBoolean(
                        "multiJobs",
                        "Submit additional jobs [merlin only!] for subtasks (distance map, voronoi volumes, shape analysis)");


        // Read Filenames
        ufiltAimFile = p.getOptionPath("ufilt", "", "Input unfiltered image");

        gfiltAimFile = p.getOptionPath("gfilt", "gfilt.tif",
                "Post-filtering image");
        threshAimFile = p.getOptionPath("threshold", "threshold.tif",
                "Theshold Image");
        boneAimFile = p.getOptionPath("bone", "bone.tif", "Calcified Tissue");
        maskAimFile = p.getOptionPath("mask", "mask.tif",
                "Mask encompassing bone and porosity");

        maskdistAimFile = p.getOptionPath("maskdist", "maskdist.tif",
                "Bone Surface Distance map image");
        thickmapAimFile = p.getOptionPath("ctth", "ctth.tif",
                "Cortical thickness map");

        porosAimFile = p.getOptionPath("poros", "poros.tif",
                "Bone porosity image");

        lmaskAimFile = p.getOptionPath("lmask", "lmask.tif", "Lacuna labels");
        cmaskAimFile = p.getOptionPath("cmask", "cmask.tif", "Canal labels");

        comboAimFile = p.getOptionPath("combo", "combo.tif",
                "Combo Mask Image (Mask, Lacuna, Canals)");

        lacunAimFile = p.getOptionPath("lacun", "lacun.tif", "Lacuna labels");
        canalAimFile = p.getOptionPath("canal", "canal.tif", "Canal labels");
        lacunDistAimFile = p.getOptionPath("lacundist", "lacundist.tif",
                "Lacuna distance");
        lacunVolsAimFile = p.getOptionPath("lacunvols", "lacunvols.tif",
                "Lacuna volumes");

        canalVolsAimFile = p.getOptionPath("canalvols", "canalvols.tif",
                "Canal volumes");
        canalDistAimFile = p.getOptionPath("canaldist", "canaldist.tif",
                "Canal distance");
        cdtoAimFile = p.getOptionPath("canaldto", "canaldto.tif",
                "Canal thickness");
        cdtbAimFile = p.getOptionPath("canaldtb", "canaldtb.tif",
                "Canal spacing");
        mdtoAimFile = p.getOptionPath("maskdto", "maskdto.tif",
                "Mask thickness");
        // String
        // baseName=ufiltAimFile.substring(0,ufiltAimFile.lastIndexOf("."));
        // if (baseName.length()<1) baseName="UFILT";
        lacunCsv = p.getOptionPath("lacuncsv", "lacun",
                "Output shape analysis file (auto .csv)");
        canalCsv = p.getOptionPath("canalcsv", "canal",
                "Output shape analysis file (auto .csv)");

        // Parse the Parameters
        // Filtering
        doResample = p.getOptionBoolean("resample", "Run resample on image");
        upsampleFactor = p.getOptionInt("upsampleFactor", 1,
                "Upscale Factor (scaling is upsampleFactor/downsampleFactor)",
                0, 10);
        downsampleFactor = p
                .getOptionInt(
                        "downsampleFactor",
                        2,
                        "Downsample Factor (scaling is upsampleFactor/downsampleFactor)",
                        0, 10);
        doLaplace = p.getOptionBoolean("laplace", "Use a laplacian filter");
        doGradient = p.getOptionBoolean("gradient", "Use a gradient filter");
        doGauss = p.getOptionBoolean("gauss", "Use a gaussian filter");
        doMedian = p.getOptionBoolean("median", "Use a median filter");
        // Threshold
        threshVal = p.getOptionInt("thresh", 2200,
                "Value used to threshold image");
        // Segmentation
        closeIter = p.getOptionInt("closeIter", 1,
                "Number of closing iterations to perform on bone");
        eroNH = p.getOptionInt("eroNH", 1, "Neighborhood used for erosion");
        eroOccup = p
                .getOptionDouble("eroOccup", 1.0,
                        "Minimum neighborhood occupancy % for to prevent erosion deletion");
        dilNH = p.getOptionInt("dilNH", 1, "Neighborhood used for dilation");
        dilOccup = p.getOptionDouble("dilOccup", 1.0,
                "Minimum neighborhood occupancy % for dilation addition");
        morphRadius = p
                .getOptionDouble(
                        "morphRadius",
                        1.75,
                        "Radius to use for the kernel (vertex shaing is sqrt(3), edge sharing is sqrt(2), and face sharing is 1)");
        // Contouring
        doCL = !p
                .getOptionBoolean("nocomplabel",
                        "Do a component labeling of the cortical bone before contouring");
        minVolumePct = p.getOptionDouble("minpct", 50,
                "Minimum Volume Percent for Mask component labeling", 0, 102);
        justCircle = p
                .getOptionBoolean("justcircle",
                        "Use the same circle used to remove edges for the mask of the image");
        pureWhiteMask = p.getOptionBoolean("nomask",
                "Dont use a mask (just a white image)");

        maskContourSteps = p.getOptionInt("maskcontoursteps", 180,
                "Number of steps to use for the contouring of the mask");
        maskContourBW = p
                .getOptionDouble(
                        "maskcontourwidth",
                        0.4,
                        "Amount to blur the edges of the contouring of the mask (normalized to number of steps)");
        porosMaskPeel = p.getOptionInt("maskcontourpeel", 5,
                "Number layers to peel off with the mask");
        removeMarrowCore = !p.getOptionBoolean("leavemarrow",
                "Leave Marrow (center porosity)");
        rmEdges = !p.getOptionBoolean("leaveedges",
                "Leave edges when making contour");
        remEdgesRadius = p.getOptionDouble("edgeradius", 1.0,
                "% of radius to use for removing edges");
        // Recontour and fix images
        doFixMasks = p.getOptionBoolean("fixmasks",
                "Rerun Contouring and correct masks as needed");

        minVoxCnt = p.getOptionInt("minVoxCnt", 50,
                "Minimum Voxel Count (don't include artifact fragements)");
        lacMinVol = p.getOptionDouble("lacMinVol", 0,
                "Smallest lacuna size (defaults to minVoxCnt if empty) in mm3");
        lacMaxVol = p.getOptionDouble("lacMaxVol", 2.74e-6,
                "Largest lacuna size (defaults is 1000vx) in mm3");

        canMinVol = p.getOptionDouble("canMinVol", lacMaxVol,
                "Smallest lacuna size (defaults to lacMaxVol if empty) in mm3");

        canMaxVol = p.getOptionDouble("canMaxVol", 0,
                "Largest lacuna size (defaults is no limit) in mm3");
        makePreviews = p.getOptionBoolean("makepreview", "Make previews");

        runAsJob = p
                .getOptionBoolean("sge:runasjob",
                        "Run this script as an SGE job (adds additional settings to this task");
        jobToRun = SGEJob.runAsJob("tipl.scripts.UFEM", p, "sge:");

        // boolean
        // doContour=p.getOptionBoolean("contour","Make a contour of the boneeau borders");
        boolean notValid = false;
        if (p.hasOption("?")) {
            System.out.println(" UFEM Femur Processing Script Help");
            System.out
                    .println(" Analyzes Labeled Gray values inside of Int Labeled Regions");
            System.out.println(" Arguments::");
            System.out.println(" ");
            System.out.println(p.getHelp());
            notValid = true;
        }

        final boolean tempLocalLoad = VirtualAim.scratchLoading;
        VirtualAim.scratchLoading = false; // Turn off this feature for tryOpen
        if (resume) {
            stageList = "";
            prepareResume();
            stageList = stageList.substring(1);
        }
        VirtualAim.scratchLoading = tempLocalLoad; // set back to old value

        // Make sure the settings are valid here
        final String[] stages = stageList.split(",");

        System.out.println("Verifying Stages[" + stages.length + "] = "
                + stageList);

        // Filter statement to check stages
        for (final String cStage : stages) {
            if ((Integer.valueOf(cStage) > 0)
                    & (Integer.valueOf(cStage) <= LASTSTAGE)) {
                // Happily do nothing
            } else {
                System.out.println("Stage : " + cStage
                        + " is quite invalid, please fix");
                notValid = true;
            }
        }

        if (notValid)
            throw new IllegalArgumentException("One or more of the arguments is invalid");

        if (doFixMasks)
            stageList = "40," + stageList; // special fixmask routine
        GrayAnalysis.doPreload = true;
    }

    /**
     * find the extents of non-zero values in an image and resize based on the
     * smallest bounding box which can be fit around these edges
     */
    public static TImg boundbox(final TImg maskImage) {
        final Resize myResizer = new Resize(maskImage);
        myResizer.find_edges();
        myResizer.execute();
        return myResizer.ExportImages(maskImage)[0];
    }

    public static TImg boundbox(final TImg cAim, final TImg maskAim) {
        final Resize myResizer = new Resize(cAim);
        myResizer.cutROI(maskAim);
        myResizer.execute();
        return myResizer.ExportImages(cAim)[0];
    }

    /**
     * run the contouring code and perform open and closing operations
     * afterwards
     */
    public static TImg contour(final TImg maskAim, final boolean remEdges,
                               final double remEdgesRadius, final boolean doCL,
                               final double minVolumePct, final boolean removeMarrowCore,
                               final int maskContourSteps, final double maskContourBW,
                               final boolean justCircle, final boolean pureWhiteMask) {
        if (pureWhiteMask) {
            final PureFImage.PositionFunction whitePF = new PureFImage.PositionFunction() {

                @Override
                public final double get(final Double[] ipos) {
                    // TODO Auto-generated method stub
                    return 1;
                }

                @Override
                public double[] getRange() {
                    // TODO Auto-generated method stub
                    return new double[]{0, 1};
                }

                @Override
                public String name() {
                    // TODO Auto-generated method stub
                    return "WhiteMask";
                }

            };
            return TImgTools.WrapTImgRO(new PureFImage(maskAim, 10, whitePF));
        }
        if (justCircle) {
            final EasyContour myContour = new EasyContour(maskAim);
            myContour.useFixedCirc(remEdgesRadius);
            myContour.execute();

            return myContour.ExportImages(maskAim)[0];
        } else {
            TImg cleanedMaskAim;
            if (doCL) {
                System.out.println("Running Component Label " + maskAim
                        + " ...");
                final ComponentLabel myCL = new ComponentLabel(maskAim);
                myCL.runRelativeVolume(minVolumePct, 101);
                cleanedMaskAim = myCL.ExportMaskAim(maskAim);
            } else {
                cleanedMaskAim = maskAim;
            }
            // Cut a circle incase that somehow got f-ed up
            if (remEdges)
                cleanedMaskAim = removeEdges(cleanedMaskAim, remEdgesRadius);
            EasyContour myContour = new EasyContour(cleanedMaskAim);
            myContour.usePoly(maskContourSteps, maskContourBW);
            myContour.bothContourModes = removeMarrowCore;
            myContour.execute();
            final Morpho cleanContour = new Morpho(myContour.ExportImages(maskAim)[0]);
            myContour = null;

            // At least one opening operation to get rid of artifacts near the
            // borders and between lines
            cleanContour.useSphKernel(morphRadius);
            cleanContour.openMany(2, new D3int(1, 1, 3), 1.0);
            cleanContour.closeMany(1, new D3int(1, 1, 1), 1.0);
            return cleanContour.ExportImages(cleanedMaskAim)[0];
        }

    }

    public static TImg filter(final TImg ufiltAim, final boolean doLaplace,
                              final boolean doGradient, final boolean doGauss, final boolean doMedian,
                              final int upsampleFactor, final int downsampleFactor) {
        
    	
        final ITIPLPluginIO fs = TIPLPluginManager.createBestPluginIO("Filter", new TImgRO[] {ufiltAim});
        fs.LoadImages( new TImgRO[] {ufiltAim});
        int filterMode = FilterSettings.NEAREST_NEIGHBOR;
        if (doMedian) {
            filterMode=FilterSettings.MEDIAN;
        } else if (doLaplace) {
        	filterMode = FilterSettings.LAPLACE;
        } else if (doGradient) {
        	filterMode = FilterSettings.GRADIENT;
        } else if (doGauss) {
        	filterMode = FilterSettings.GAUSSIAN;
        }
        
        final D3int ds = new D3int(downsampleFactor,downsampleFactor,downsampleFactor);
        final D3int up = new D3int(upsampleFactor,upsampleFactor,upsampleFactor);
        fs.setParameter("-upfactor="+up+" -downfactor="+ds+" -filter="+filterMode);
        fs.execute();
        
        return fs.ExportImages(ufiltAim)[0];
    }

    public static void main(final String[] args) {

        System.out
                .println(" Micro Cortical Bone Analysis Script v" + UFEM.kVer);
        System.out
                .println(" Runs Segmentation and Generates Distance Map Script v"
                        + UFEM.kVer);
        System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
        final ArgumentParser p = SparkGlobal.activeParser(args);
        final UFEM cScript = new UFEM(p);

        p.checkForInvalid();
        cScript.runScript();

    }

    public static TImg makePoros(final TImg inBoneAim) {
        final TImgRO.FullReadable inBoneAimPlus = TImgTools
                .makeTImgFullReadable(inBoneAim);
        final boolean[] scdat = inBoneAimPlus.getBoolAim();
        final boolean[] iscdat = new boolean[scdat.length];
        for (int i = 0; i < iscdat.length; i++) {
            iscdat[i] = !scdat[i];
        }
        final TImg outPorosAim = inBoneAim.inheritedAim(iscdat,
                inBoneAim.getDim(), inBoneAim.getOffset());
        outPorosAim.appendProcLog("CMD:Invert");
        return outPorosAim;

    }

    protected static void makeProfiles(final TImg datAim, final TImg mskAim,
                                       final TypedPath fileroot) {
        GrayAnalysis.StartThetaCylProfile(datAim, mskAim, fileroot.append("_th.txt"),
                0.1f);
        GrayAnalysis.StartZProfile(datAim, mskAim, fileroot.append( "_z.txt"), 0.1f);
        GrayAnalysis2D.StartRZProfile(datAim, fileroot.append( "_rz.txt"), 0.1f, 1000);
        GrayAnalysis2D.StartThZProfile(datAim, fileroot.append("_thz.txt"), 0.1f,
                1000);
    }

    /**
     * a function to provide new names to the newly recontoured objects
     */
    public static TypedPath originalName(final TypedPath inFile) {
        final int cPos = inFile.getPath().lastIndexOf(File.separator);
        return new TypedPath(inFile.getPath().substring(0, cPos + 1) + "precont_"
                + inFile.getPath().substring(cPos + 1));
    }

    public static TImg peelAim(final TImg cAim, final TImg maskAim,
                               final int iters) {
        return peelAim(cAim, maskAim, iters, false);
    }

    public static TImg peelAim(final TImg cAim, final TImg maskAim,
                               final int iters, final boolean asBool) {
        final Peel cPeel = new Peel(cAim, maskAim, new D3int(iters), asBool);
        System.out.println("Calculating Peel " + cAim + " ...");
        cPeel.execute();
        return cPeel.ExportImages(cAim)[0];
    }

    /**
     * A simple circular contour, edge removal, and peeling
     */
    protected static TImg removeEdges(final TImg cAim,
                                      final double remEdgesRadius) {
        EasyContour myContour = new EasyContour(cAim);
        myContour.useFixedCirc(remEdgesRadius);
        myContour.execute();
        cAim.appendProcLog(myContour.getProcLog());
        final Peel cPeel = new Peel(cAim, myContour.ExportImages(cAim)[0], new D3int(
                1));
        myContour = null;
        System.out.println("Calculating Remove Edges Peel " + cAim + " ...");
        cPeel.execute();
        return cPeel.ExportImages(cAim)[0];
    }

    /**
     * Segment the thesheld bone file into a clean mask
     */
    public static TImg segment(final TImg threshAim, final double morphRadius,
                               final int closeIter) {

        final TImgRO.FullReadable threshAimPlus = TImgTools
                .makeTImgFullReadable(threshAim);

        final TImg maskAim = threshAim.inheritedAim(threshAimPlus.getBoolAim(),
                threshAim.getDim(), threshAim.getOffset());
        final TImgRO.FullReadable maskAimPlus = TImgTools
                .makeTImgFullReadable(maskAim);
        final boolean[] scdat = maskAimPlus.getBoolAim();
        // The Segmentation is a bit easier with arrays
        Morpho boneClose = new Morpho(scdat, maskAim.getDim(),
                maskAim.getOffset());
        boneClose.erode(new D3int(2, 2, 0), 0.2); // Remove xy voxels

        boneClose.neighborSize = new D3int(1);
        boneClose.useSphKernel(morphRadius);
        boneClose.openMany(closeIter);

        // Erode once to remove small groups 5/125 of voxels
        boneClose.erode(2, 0.08);
        // Remove single voxels groups in the xy plane

        boneClose.erode(new D3int(2, 2, 0), 0.2);

        maskAim.appendProcLog(boneClose.procLog);

        for (int i = 0; i < scdat.length; i++)
            scdat[i] = !boneClose.outAim[i]; // Invert to poros
        maskAim.appendProcLog("CMD:Invert");

        boneClose = null; // Done with boneClose tool

        // Open poros
        final Morpho bubOpen = new Morpho(scdat, maskAim.getDim(),
                maskAim.getOffset());
        bubOpen.erode(new D3int(2, 2, 0), 0.2);
        bubOpen.useSphKernel(morphRadius);
        bubOpen.closeMany(closeIter, 2, 1.0);

        // bubOpen.erode(2,0.041);
        maskAim.appendProcLog(bubOpen.procLog);

        for (int i = 0; i < scdat.length; i++)
            scdat[i] = !bubOpen.outAim[i]; // Invert back to bone

        maskAim.appendProcLog("CMD:Invert");
        return TImgTools.makeTImgExportable(maskAim).inheritedAim(scdat,
                maskAim.getDim(), maskAim.getOffset());
    }

    /**
     * perform a threshold on an input image and remove edges if needed *
     */
    public static TImg threshold(final TImg inAim, final int threshVal,
                                 final boolean rmEdges, final double remEdgesRadius) {
        final TImgRO.FullReadable inAimPlus = TImgTools
                .makeTImgFullReadable(inAim);
        int[] inImg = inAimPlus.getIntAim();

        // Threshold the data
        boolean[] scdat = new boolean[inImg.length];

        for (int i = 0; i < inImg.length; i++) {
            scdat[i] = inImg[i] > threshVal;
        }
        inImg = null;

        TIPLGlobal.runGC();
        TImg outAim = inAim.inheritedAim(scdat, inAim.getDim(),
                inAim.getOffset());
        if (rmEdges)
            outAim = removeEdges(outAim, remEdgesRadius);

        scdat = null;
        TIPLGlobal.runGC();
        outAim.appendProcLog("CMD:Threshold, Value:" + threshVal);
        return outAim;
    }

    /**
     * run the component labeling on the porosity
     */
    public void componentLabeling() {
        ComponentLabel clObjects = new ComponentLabel(porosAim);
        clObjects.runVoxels(minVoxCnt);
        if (canMaxVol > canMinVol)
            cmaskAim = clObjects.ExportMaskAimVolume(porosAim,
                    porosAim.getElSize(), canMinVol, canMaxVol);
        else
            cmaskAim = clObjects.ExportMaskAimVolume(porosAim,
                    porosAim.getElSize(), canMinVol);
        lmaskAim = clObjects.ExportMaskAimVolume(porosAim,
                porosAim.getElSize(), lacMinVol, lacMaxVol);
        clObjects = null;
        TIPLGlobal.runGC();
    }

    /**
     * create the voronoi volumes for the canals and canal distance
     */
    protected void makeCanalDist() {
        if (canalAim == null)
            canalAim = TImgTools.ReadTImg(canalAimFile);
        if (maskAim == null)
            maskAim = TImgTools.ReadTImg(maskAimFile);
        canaldistAimReady = false;

        ITIPLPluginIO vTransform = TIPLPluginManager.createBestPluginIO("kVoronoi", new TImg[]{canalAim, maskAim});
        vTransform.LoadImages(new TImg[]{canalAim, maskAim});
        vTransform.execute();
        TImg[] outImgs = vTransform.ExportImages(canalAim);
        canalVolsAim = outImgs[0];
        canalDistAim = outImgs[1];
        vTransform = null;
        TImgTools.WriteTImg(canalVolsAim, canalVolsAimFile);
        TImgTools.WriteTImg(canalDistAim, canalDistAimFile);

        maskAim = null;
        canalAim = null;

        canalVolsAim = null;
        canalDistAim = null;
        canaldistAimReady = true;
        TIPLGlobal.runGC();
    }

    /**
     * create the voronoi volumes for the lacuna
     */
    protected void makeLacunDist() {
        if (lacunAim == null)
            lacunAim = TImgTools.ReadTImg(lacunAimFile);
        if (boneAim == null)
            boneAim = TImgTools.ReadTImg(boneAimFile);
        lacundistAimReady = false;
        ITIPLPluginIO vTransform = TIPLPluginManager.createBestPluginIO("kVoronoi", new TImg[]{lacunAim, boneAim});
        vTransform.LoadImages(new TImg[]{lacunAim, boneAim});
        vTransform.execute();
        TImg[] outImgs = vTransform.ExportImages(lacunAim);
        lacunVolsAim = outImgs[0];
        lacunDistAim = outImgs[1];
        vTransform = null;
        TImgTools.WriteTImg(lacunVolsAim, lacunVolsAimFile);
        TImgTools.WriteTImg(lacunDistAim, lacunDistAimFile);

        lacunVolsAim = null;
        lacunDistAim = null;

        lacunAim = null;
        boneAim = null;
        lacundistAimReady = true;
        TIPLGlobal.runGC();
    }

    /**
     * create the Voronoi volumes for the canals and canal distance
     */
    protected void makeMaskDist() {
        if (maskAim == null)
            maskAim = TImgTools.ReadTImg(maskAimFile);

        ITIPLPluginIO KV = TIPLPluginManager.createBestPluginIO("kVoronoi", new TImg[]{maskAim});
        KV.setParameter("-includeedges=true");
        KV.LoadImages(new TImg[]{null, maskAim});

        maskdistAimReady = false;
        KV.execute();
        maskdistAim = KV.ExportImages(maskAim)[1];
        if (maskdistAimFile.length() > 0)
            TImgTools.WriteTImg(maskdistAim, maskdistAimFile);
        maskdistAim = null;
        maskdistAimReady = true;
        TIPLGlobal.runGC();
    }

    /**
     * Code to make preview (slices every 20 slides of the data)
     */
    public void makePreview(final TypedPath previewName, final TImg previewData) {
    	final int skipSlices=20;
        final ITIPLPluginIO fs = TIPLPluginManager.createBestPluginIO("Filter", new TImgRO[] {previewData});
        fs.LoadImages( new TImgRO[] {previewData});
        fs.setParameter("-upfactor=1,1,1 -downfactor=1,1,"+skipSlices);
        fs.execute();
        final TImg tempAim = fs.ExportImages(previewData)[0];
        TImgTools.WriteTImg(tempAim, previewName);
    }

    public TypedPath nameVersion(final TypedPath inName, final int verNumber) {
        return inName.append( "_" + verNumber + ".csv");
    }

    /**
     * run the labeling and return a colored image
     */
    public TImg objectLabeling(final TImg inputImage) {
        ComponentLabel clObjects = new ComponentLabel(inputImage);
        clObjects.runVoxels(0);
        final TImg outAim = clObjects.ExportLabelsAim(inputImage);
        clObjects = null;
        TIPLGlobal.runGC();
        return outAim;
    }

    /**
     * Checks the status of the aim files in the directory and proceeds based on
     * their presence
     */
    public void prepareResume() {
        // 1 - Filtering, 2 - Thresholding, 3 - Segmentation, 4 - Contouring, 5
        // - Mask Distance [parallel], 6 - Classification, 7 - Labeling, 8 -
        // Canal Voronoi Volumes / Distance Map [parallel], 9 - Lacuna Volumes
        // [parallel], [10,11,12,13] - Lacuna Shape Analysis, 14 - Lacuna
        // Neighbor Analysis, [15,16] - Canal Shape Analysis, 17 - Canal
        // Neighbor Analysis
        if (!tryOpenImagePath(gfiltAimFile)) {
            stageList += ",1";
            // Filtering is not that import if other stages are there then just
            // filter and continue

        }
        // For the intial steps if they are missing the entire analysis probably
        // needs to be redone
        if (!tryOpenImagePath(boneAimFile)) {
            for (int cstg = 2; cstg <= (singleStep ? 2 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            doFixMasks = false; // contour will be run anyways
            return;
        }

        if (!tryOpenImagePath(porosAimFile)) {
            for (int cstg = 3; cstg <= (singleStep ? 3 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            doFixMasks = false; // contour will be run anyways
            return;
        }

        if (!tryOpenImagePath(maskAimFile)) {
            for (int cstg = 4; cstg <= (singleStep ? 4 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            doFixMasks = false; // contour will be run anyways
            return;
        }

        if (!tryOpenImagePath(maskdistAimFile)) {
            stageList += ",5";
        }

        if (!(tryOpenImagePath(lmaskAimFile) & tryOpenImagePath(cmaskAimFile))) {
            for (int cstg = 6; cstg <= (singleStep ? 6 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }

        if (!(tryOpenImagePath(lacunAimFile) & tryOpenImagePath(canalAimFile))) {
            for (int cstg = 7; cstg <= (singleStep ? 7 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }

        if (!(tryOpenImagePath(canalVolsAimFile) & tryOpenImagePath(canalDistAimFile))) {
            stageList += ",8";
        }

        if (!tryOpenImagePath(lacunVolsAimFile)) {
            stageList += ",9";
        }

        if (multiJobs) {
            if (stageList == "") {
                // if everything is in place, then finish the analysis,
                // otherwise submit subjobs and wait, wait, wait
                multiJobs = false; // no more jobs to submit so just finish
                System.out
                        .println(" ====== Everything completed except shape analysis, running shape analysis!");
            } else
                return;
        }
        for (int cstg = 10; cstg <= LASTSTAGE; cstg++)
            stageList += "," + cstg;
    }

    @Override
    public void run() {
        final Thread myThread = Thread.currentThread();
        synchronized (this) {
            ufemCores--;
        }// eat a core
        try {
            switch (smcOperation) {
                case 0:
                    System.err
                            .println("You have erroneously ended up at the point in the script, please check whatever freak code got you here!");
                    System.out.println(myThread.getStackTrace());
                    break;
                case UFEM_MASKDIST:
                    System.out.println("Mask Distance Code running..." + myThread
                            + "-R" + ufemCores);
                    smcOperation = 0;
                    makeMaskDist();
                    break;
                case UFEM_CANALDIST:
                    System.out.println("Canal Distance Code running..." + myThread
                            + "-R" + ufemCores);
                    smcOperation = 0;
                    makeCanalDist();
                    break;
                case UFEM_LACUNDIST:
                    System.out.println("Lacuna Distance Code running..." + myThread
                            + "-R" + ufemCores);
                    smcOperation = 0;
                    makeLacunDist();
                    break;
                default:
                    System.err
                            .println("You have waaay erroneously ended up at the point in the script, please check whatever freak code got you here!"
                                    + smcOperation);
                    System.out.println(myThread.getStackTrace());
                    break;
            }
        } catch (final Exception e) {
            System.err.println("ERROR:: Thread : " + Thread.currentThread()
                    + " crashed while running, proceed carefully!" + e);
            e.printStackTrace();
        }
        synchronized (this) {
            ufemCores++;
        }// eat a core
    }

    /**
     * run the neighborhood analysis
     *
     * @param inputAim the image to perform the analysis on, typically hte post
     *                 voronoi image but for foams it is often better with the
     *                 labeled image
     */
    public TImg runNeighborhoodAnalysis(final TImg inputAim,
                                        final TypedPath edgeName) {
        final Neighbors nbor = new Neighbors(inputAim);
        System.out.println("Calculating neighbors " + inputAim + " ...");
        nbor.execute();
        System.out.println("Writing csv neigbhor-list ...");
        nbor.WriteNeighborList(edgeName.append("_edge.csv"));
        return nbor.ExportCountImageAim(inputAim);
    }

    public void runScript() {
        if (runAsJob) {
            jobToRun.submit();
        } else {
            System.currentTimeMillis();
            System.currentTimeMillis();

            final String[] stages = stageList.split(",");

            int scount = 1;
            for (final String cStage : stages) {
                System.out.println("Running Stage #" + cStage + " (" + scount
                        + "/" + stages.length + ")");
                runSection(Integer.valueOf(cStage));
                TIPLGlobal.runGC();
                scount++;
            }
        }
    }

    /**
     * The code required to run each step of the analysis and return. The code
     * should be entirely self-contained reading the data in if needed from
     * saved files and saving the results. Memory cleaning and thread management
     * will be done by other sections of the code
     *
     * @param sect The Section of code to run
     */
    public void runSection(final int sect) {
        System.out.println("UFEM--" + new Date());
        if ((multiJobs) & (sect >= 10)) {
            System.err
                    .println("Stages over "
                            + sect
                            + " are not run during multijob operation since waiting is not possible! Submitted Jobs ("
                            + submittedJobs + ")");
            if (submittedJobs > 0)
                return;
        }
        switch (sect) {
            case 1:
                System.out.println("Begin 1, Filtering: Loading " + ufiltAimFile
                        + " ...");

                ufiltAim = TImgTools.ReadTImg(ufiltAimFile);

                if (doResample) {
                    gfiltAim = filter(ufiltAim, doLaplace, doGradient, doGauss, doMedian,
                            upsampleFactor, downsampleFactor);
                    TImgTools.WriteTImg(gfiltAim, gfiltAimFile);

                } else
                    gfiltAim = ufiltAim;
                ufiltAim = null;
                break;
            case 2:
                System.out.println("Begin 2, Thresholding ...");
                if (gfiltAim == null)
                    gfiltAim = TImgTools.ReadTImg(gfiltAimFile);
                threshAim = threshold(gfiltAim, threshVal, rmEdges, remEdgesRadius);
                gfiltAim = null;
                TImgTools.WriteTImg(threshAim, threshAimFile);
                break;
            case 3:
                System.out.println("Begin 3, Mask Morphological Segmentation ...");
                // No need to write the bone before calculating the mask,
                // boneAim.WriteAim(boneAimFile);
                if (threshAim == null)
                    threshAim = TImgTools.ReadTImg(threshAimFile);
                boneAim = threshAim; // bone comes from the threshold image
                maskAim = segment(threshAim, morphRadius, closeIter);

                TIPLGlobal.runGC();
                porosAim = makePoros(threshAim);

                maskAim = boundbox(maskAim);
                TImgTools.WriteTImg(maskAim, maskAimFile);

                boneAim = boundbox(boneAim, maskAim);
                TImgTools.WriteTImg(boneAim, boneAimFile);
                TIPLGlobal.runGC();

                porosAim = boundbox(porosAim, maskAim);
                TImgTools.WriteTImg(porosAim, porosAimFile);

                break;
            case 4:
                System.out.println("Begin 4, Mask Contouring ...");
                // Open bone.tif but call it mask
                if (maskAim == null)
                    maskAim = TImgTools.ReadTImg(boneAimFile);
                maskAim = contour(maskAim, rmEdges, remEdgesRadius, doCL,
                        minVolumePct, removeMarrowCore, maskContourSteps,
                        maskContourBW, justCircle, pureWhiteMask);
                TImgTools.WriteTImg(maskAim, maskAimFile);
                // Now open the bone and porosity files to process them
                if (boneAim == null)
                    boneAim = TImgTools.ReadTImg(boneAimFile);
                if (porosAim == null)
                    porosAim = TImgTools.ReadTImg(porosAimFile);

                boneAim = peelAim(boneAim, maskAim, 1, true);
                TImgTools.WriteTImg(boneAim, boneAimFile);

                porosAim = peelAim(porosAim, maskAim, porosMaskPeel, true);
                TImgTools.WriteTImg(porosAim, porosAimFile);
                // Do not need for the next steps
                boneAim = null;
                break;
            case 40:
                final TypedPath[] imgListBW = {porosAimFile, lmaskAimFile,
                        cmaskAimFile};
                final TypedPath[] imgListColor = {lacunAimFile, canalAimFile};
                System.out
                        .println("Begin Special Stage, Recontouring and Mask Repairing...");

                // Make Backups
                for (final TypedPath imgFile : imgListBW)
                {
                	TIPLGlobal.copyFile(imgFile, originalName(imgFile));
                }
                for (final TypedPath imgFile : imgListColor)
                {
                	TIPLGlobal.copyFile(imgFile, originalName(imgFile));
                }
                TIPLGlobal.copyFile(boneAimFile, originalName(boneAimFile));
                TIPLGlobal.copyFile(maskAimFile, originalName(maskAimFile));

                if (boneAim == null)
                    boneAim = TImgTools.ReadTImg(boneAimFile);

                maskAim = boneAim.inheritedAim(
                        TImgTools.makeTImgFullReadable(boneAim).getBoolAim(),
                        boneAim.getDim(), boneAim.getOffset());
                maskAim = contour(maskAim, false, remEdgesRadius, doCL,
                        minVolumePct, removeMarrowCore, maskContourSteps,
                        maskContourBW, justCircle, pureWhiteMask);
                TImgTools.WriteTImg(maskAim, maskAimFile);

                boneAim = peelAim(boneAim, maskAim, 1, true);
                TImgTools.WriteTImg(boneAim, boneAimFile);
                boneAim = null;
                // Perform the 5 peels here and then just apply that mask to the
                // subsequent datasets
                maskAim = peelAim(maskAim, maskAim, porosMaskPeel, true);

                TIPLGlobal.runGC();
                // The code for mask data
                for (final TypedPath imgFile : imgListBW) {
                	
                    if (tryOpenImagePath(imgFile)) {
                        TImg tempAim = TImgTools.ReadTImg(imgFile);

                        tempAim = peelAim(tempAim, maskAim, 0, true);
                        TImgTools.WriteTImg(tempAim, imgFile);
                        tempAim = null;
                        TIPLGlobal.runGC();
                    }
                }
                // Te code for color data
                for (final TypedPath imgFile : imgListColor) {
                	
                    if (tryOpenImagePath(imgFile)) {
                        TImg tempAim = TImgTools.ReadTImg(imgFile);
                        tempAim = peelAim(tempAim, maskAim, 0);
                        TImgTools.WriteTImg(tempAim, imgFile);
                        tempAim = null;
                        TIPLGlobal.runGC();
                    }
                }

                // Replace mask aim with the correct version
                maskAim = TImgTools.ReadTImg(maskAimFile);
                // Do not need for the next steps
                break;

            case 5:
                System.out.println("Begin 5, Mask Distance ...");
                maskdistAimReady = false;
                ufemThread(UFEM_MASKDIST);
                break;
            case 6:
                System.out.println("Begin 6, Classifying porosity ...");
                if (porosAim == null)
                    porosAim = TImgTools.ReadTImg(porosAimFile);
                componentLabeling();
                porosAim = null;
                TImgTools.WriteTImg(lmaskAim, lmaskAimFile);
                TImgTools.WriteTImg(cmaskAim, cmaskAimFile);
                break;
            case 7:
                System.out.println("Begin 7, Labeling objects ...");
                if (lmaskAim == null)
                    lmaskAim = TImgTools.ReadTImg(lmaskAimFile);
                lacunAim = objectLabeling(lmaskAim);
                TImgTools.WriteTImg(lacunAim, lacunAimFile);

                lacunAim = null;
                lmaskAim = null;
                TIPLGlobal.runGC();

                if (cmaskAim == null)
                    cmaskAim = TImgTools.ReadTImg(cmaskAimFile);
                canalAim = objectLabeling(cmaskAim);
                TImgTools.WriteTImg(canalAim, canalAimFile);

                // canalAim=null;
                cmaskAim = null;
                break;
            case 8:
                System.out.println("Begin 8, Canal Voronoi volumes ...");
                canaldistAimReady = false;
                ufemThread(UFEM_CANALDIST);
                break;
            case 9:
                System.out.println("Begin 9, Lacuna Voronoi volumes ...");
                lacundistAimReady = false;
                ufemThread(UFEM_LACUNDIST);
                break;

            case 10: // Shape Analysis
                // Lacuna Version Scheme 0 = mask distance, 1 = canal distance, 2 =
                // canal volume, 3 =
                System.out.println("Performing Basic Lacuna Shape Analysis ...");

                if (lacunAim == null)
                    lacunAim = TImgTools.ReadTImg(lacunAimFile);
                System.out.println("Wait for mask distance...");
                // Only relavant in multithreaded mode
                while (!maskdistAimReady) {
                    try {
                        Thread.currentThread();
                        Thread.sleep(5000); // wait for mask distance analysis
                    } catch (final InterruptedException e) {
                        System.err
                                .println("Thread : "
                                        + Thread.currentThread()
                                        + " was interrupted while sleeping, proceed carefully!");
                    }
                }
                if (maskdistAim == null)
                    maskdistAim = TImgTools.ReadTImg(maskdistAimFile);
                GrayAnalysis.StartLacunaAnalysis(lacunAim, maskdistAim,
                        nameVersion(lacunCsv, 0), "Mask");
                maskdistAim = null;
                break;
            case 11:
                // Canal Distance
                if (lacunAim == null)
                    lacunAim = TImgTools.ReadTImg(lacunAimFile);
                System.out.println("Wait for canal distance...");
                while (!canaldistAimReady) {

                    try {
                        Thread.currentThread();
                        Thread.sleep(10000); // wait for mask distance analysis
                    } catch (final InterruptedException e) {
                        System.err
                                .println("Thread : "
                                        + Thread.currentThread()
                                        + " was interrupted while sleeping, proceed carefully!");

                    }
                }
                if (canalDistAim == null)
                    canalDistAim = TImgTools.ReadTImg(canalDistAimFile);
                GrayAnalysis
                        .AddDistanceColumn(lacunAim, canalDistAim,
                                nameVersion(lacunCsv, 0), nameVersion(lacunCsv, 1),
                                "Canal");
                canalDistAim = null;
                break;
            case 12:
                // Canal Region
                if (lacunAim == null)
                    lacunAim = TImgTools.ReadTImg(lacunAimFile);
                if (canalVolsAim == null)
                    canalVolsAim = TImgTools.ReadTImg(canalVolsAimFile);
                GrayAnalysis.AddRegionColumn(lacunAim, canalVolsAim,
                        nameVersion(lacunCsv, 1), nameVersion(lacunCsv, 2),
                        "Canal_Region");
                canalVolsAim = null;
                lacunAim = null;
                break;
            case 13:
                // Lacuna Densities
                System.out.println("Wait for lacuna density...");
                while (!lacundistAimReady) {

                    try {
                        Thread.currentThread();
                        Thread.sleep(10000); // wait for mask distance analysis
                    } catch (final InterruptedException e) {
                        System.err
                                .println("Thread : "
                                        + Thread.currentThread()
                                        + " was interrupted while sleeping, proceed carefully!");

                    }
                }
                if (lacunVolsAim == null)
                    lacunVolsAim = TImgTools.ReadTImg(lacunVolsAimFile);
                GrayAnalysis.AddDensityColumn(lacunVolsAim,
                        nameVersion(lacunCsv, 2), nameVersion(lacunCsv, 3),
                        "Density");
                break;
            case 14:
                System.out.println("Performing Basic Lacuna Neighbor Analysis ...");
                if (lacunVolsAim == null)
                    lacunVolsAim = TImgTools.ReadTImg(lacunVolsAimFile);
                GrayAnalysis.AddRegionColumn(lacunVolsAim,
                        runNeighborhoodAnalysis(lacunVolsAim, lacunCsv),
                        nameVersion(lacunCsv, 3), nameVersion(lacunCsv, 4),
                        "Neighbors");
                lacunVolsAim = null;
                TIPLGlobal.runGC();
                break;
            case 15:
                // Canal Version Scheme 0 = mask distance, 1 = density, 2 =
                // neighbors
                System.out.println("Performing Basic Canal Shape Analysis ...");
                if (canalAim == null)
                    canalAim = TImgTools.ReadTImg(canalAimFile);
                System.out.println("Wait for mask distance...");
                while (!maskdistAimReady) {
                    try {
                        Thread.currentThread();
                        Thread.sleep(5000); // wait for mask distance analysis
                    } catch (final InterruptedException e) {
                        System.err
                                .println("Thread : "
                                        + Thread.currentThread()
                                        + " was interrupted while sleeping, proceed carefully!");

                    }
                }
                if (maskdistAim == null)
                    maskdistAim = TImgTools.ReadTImg(maskdistAimFile);
                GrayAnalysis.StartLacunaAnalysis(canalAim, maskdistAim,
                        nameVersion(canalCsv, 0), "Mask");
                maskdistAim = null;
                canalAim = null;
                break;
            case 16:
                System.out.println("Wait for canal density...");
                // Canal Density
                while (!canaldistAimReady) {
                    try {
                        Thread.currentThread();
                        Thread.sleep(5000); // wait for mask distance analysis
                    } catch (final InterruptedException e) {
                        System.err
                                .println("Thread : "
                                        + Thread.currentThread()
                                        + " was interrupted while sleeping, proceed carefully!");

                    }
                }
                if (canalVolsAim == null)
                    canalVolsAim = TImgTools.ReadTImg(canalVolsAimFile);
                GrayAnalysis.AddDensityColumn(canalVolsAim,
                        nameVersion(canalCsv, 0), nameVersion(canalCsv, 1),
                        "Density");
                break;
            case 17:
                System.out.println("Performing Basic Canal Neighbor Analysis ...");
                if (canalVolsAim == null)
                    canalVolsAim = TImgTools.ReadTImg(canalVolsAimFile);
                GrayAnalysis.AddRegionColumn(canalVolsAim,
                        runNeighborhoodAnalysis(canalVolsAim, canalCsv),
                        nameVersion(canalCsv, 1), nameVersion(canalCsv, 2),
                        "Neighbors");
                canalVolsAim = null;
                break;
            case 18:
                System.out.println("Basic Canal Thickness Analysis ...");
                if (cmaskAim == null)
                    cmaskAim = TImgTools.ReadTImg(cmaskAimFile);
                cdtoAim = HildThickness.DTO(cmaskAim);
                cmaskAim = null;
                TImgTools.WriteTImg(cdtoAim, cdtoAimFile);
                GrayAnalysis.StartHistogram(cdtoAim, cdtoAimFile.append(".csv"));

                if (canalAim == null)
                    canalAim = TImgTools.ReadTImg(canalAimFile);
                GrayAnalysis.AddRegionColumn(canalAim, cdtoAim,
                        nameVersion(canalCsv, 2), nameVersion(canalCsv, 3),
                        "Thickness");
                canalAim = null;
                cdtoAim = null;
                break;
            case 19:
                System.out.println("Canal Spacing Analysis ...");
                if (canalDistAim == null)
                    canalDistAim = TImgTools.ReadTImg(canalDistAimFile);

                final ITIPLPluginIO KT = TIPLPluginManager.createBestPluginIO("HildThickness", new TImg[] { canalDistAim });
                KT.LoadImages(new TImg[] { canalDistAim });
                KT.execute();
                cdtbAim = KT.ExportImages(canalDistAim)[0];
                canalDistAim = null;
                TImgTools.WriteTImg(cdtbAim, cdtbAimFile);
                GrayAnalysis.StartHistogram(cdtbAim, cdtbAimFile.append(".csv"));
                cdtbAim = null;
                break;
            case 20:
                System.out.println("Basic Mask Thickness Analysis ...");
                if (maskdistAim == null)
                    maskdistAim = TImgTools.ReadTImg(maskdistAimFile);
                
                final ITIPLPluginIO MKT = TIPLPluginManager.createBestPluginIO("HildThickness", new TImg[] { maskdistAim });
                MKT.LoadImages(new TImg[] { maskdistAim });
                MKT.execute();
                mdtoAim = MKT.ExportImages(maskdistAim)[0];
                maskdistAim = null;
                TImgTools.WriteTImg(mdtoAim, mdtoAimFile);
                GrayAnalysis.StartHistogram(mdtoAim, mdtoAimFile .append( ".csv"));
                mdtoAim = null;
                break;
            case 21:
                System.out.println("Canal Thickness Profiles...");
                if (cdtoAim == null)
                    cdtoAim = TImgTools.ReadTImg(cdtoAimFile);
                if (cmaskAim == null)
                    cmaskAim = TImgTools.ReadTImg(cmaskAimFile);
                makeProfiles(cdtoAim, cmaskAim, cdtoAimFile);
                cdtoAim = null;
                cmaskAim = null;
                break;
            case 22:
                System.out.println("Canal Spacing Profiles...");
                if (cdtbAim == null)
                    cdtbAim = TImgTools.ReadTImg(cdtbAimFile);
                if (maskAim == null)
                    maskAim = TImgTools.ReadTImg(maskAimFile);
                makeProfiles(cdtbAim, maskAim, cdtbAimFile);
                cdtbAim = null;
                maskAim = null;
                break;
            case 23:
                System.out.println("Mask Thickness Profiles...");
                if (mdtoAim == null)
                    mdtoAim = TImgTools.ReadTImg(mdtoAimFile);
                if (maskAim == null)
                    maskAim = TImgTools.ReadTImg(maskAimFile);
                makeProfiles(mdtoAim, maskAim, mdtoAimFile);
                mdtoAim = null;
                maskAim = null;

                break;

        }
    }

    protected void submitJob(final String args, final String jobName,
                             final int cores) {
        submitJob(args, jobName, cores, false);
    }

    /**
     * Used for submitting jobs on the Merlin4 cluster using the SGE system
     */
    protected void submitJob(final String args, final String jobName,
                             final int cores, final boolean dSave) {
        double memEstimate = (3.8 * (guessDim.prod() * 4) / (1e9));
        if (dSave)
            memEstimate *= 1.5;
        if (memEstimate < 5)
            memEstimate = 5; // minimum memory estimate is now 5GB
        System.out
                .println("Setting up job for 64-bit:" + guessDim
                        + " requiring : " + memEstimate + " gb and " + cores
                        + " cores");
        String execStr = "/gpfs/home/gridengine/sge6.2u5p2/bin/lx26-amd64/qsub";
        execStr += " -l mem_free=" + Math.round(memEstimate + 5) + "G";
        execStr += " -N SFEM_" + jobName;
        execStr += " -pe smp " + cores;
        execStr += " -o S_FEM_" + jobName + ".log";
        execStr += " /afs/psi.ch/project/tipl/jobs/UFEM_subtask.sge -Xmx"
                + Math.round(memEstimate) + "G" + " -Xms"
                + Math.round(memEstimate) + "G";
        execStr += " tipl.scripts.UFEM -local -maxcores=" + cores + " " + args;
        System.out.println(execStr);
        try {
            final Runtime rt = Runtime.getRuntime();
            final Process p = rt.exec(execStr);
            String line = "";
            String retVal = "";
            final BufferedReader stdInput = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            final BufferedReader stdError = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()));
            while ((line = stdInput.readLine()) != null) {
                retVal += line;
                System.out.println(line);
            }
            stdInput.close();
            while ((line = stdError.readLine()) != null) {
                retVal += line;
                System.err.println(line);
            }
            stdError.close();
            System.out.println(retVal);
            p.exitValue();

        } catch (final Exception e) {
            System.out.println("Error Executing Task");
            e.printStackTrace();
        }
        submittedJobs++;
    }



    protected void ufemThread(final int threadTask) {
        smcOperation = threadTask;
        if (multiJobs) {
            System.out.println("Running given task :" + threadTask
                    + " in sub-job :(" + ufemCores + ")");
            // Executing directory will stay the same so just send the paths
            String args = "";
            switch (smcOperation) {
                case 0:
                    System.err
                            .println("You have erroneously ended up at the point in the script, please check whatever freak code got you here!");
                    break;
                case UFEM_MASKDIST:
                    System.out.println("Mask Distance Job Submitting...");
                    args = "-stagelist=5 -mask=" + maskAimFile + " -maskdist="
                            + maskdistAimFile;
                    submitJob(args, "MDIST", 6);
                    smcOperation = 0;
                    break;
                case UFEM_CANALDIST:
                    System.out.println("Canal Distance Job Submitting...");
                    args = "-stagelist=8 -canal=" + canalAimFile + " -mask="
                            + maskAimFile + " -canalvols=" + canalVolsAimFile
                            + " -canaldist=" + canalDistAimFile;
                    submitJob(args, "CANDIST", 6);
                    smcOperation = 0;
                    break;
                case UFEM_LACUNDIST:
                    System.out.println("Lacuna Distance Job Submitting...");
                    args = "-stagelist=9 -lacun=" + lacunAimFile + " -mask="
                            + boneAimFile + " -lacunvols=" + lacunVolsAimFile;
                    submitJob(args, "LACVOL", 4, true);
                    smcOperation = 0;
                    break;
                default:
                    System.err
                            .println("You have waaay erroneously ended up at the point in the script, please check whatever freak code got you here!"
                                    + smcOperation);

                    break;
            }

        } else {

            System.out.println("Running given task :" + threadTask
                    + " in sub thread :(" + ufemCores + ")");
            final Thread bgThread = new Thread(this, "UFEM:Subtask "
                    + threadTask);

            bgThread.start();
            // Wait for the background thread to start
            while (smcOperation > 0) {
                try {
                    Thread.currentThread();
                    Thread.sleep(500);
                } catch (final InterruptedException e) {
                    System.err
                            .println("Thread : "
                                    + Thread.currentThread()
                                    + " was interrupted while sleeping, proceed carefully!, hopefully thread:"
                                    + bgThread + " is still ok");

                }
            }
            while (ufemCores < 1) {
                try {
                    Thread.currentThread();
                    Thread.sleep(30000); // Sleep for 30s
                    System.out.println("Main thread is sleeping.");
                } catch (final InterruptedException e) {
                    System.err
                            .println("Thread : "
                                    + Thread.currentThread()
                                    + " was interrupted while sleeping, proceed carefully!, hopefully thread:"
                                    + bgThread + " is still ok");

                }
                synchronized (this) {
                    if (ufemCores > 0) {
                    }
                }// check core count
            }
            /**
             * } else { System.out.println("Running given task :"+threadTask+
             * " in main thread :("+ufemCores+")"); run(); }
             */
        }
    }
}
