package tipl.scripts;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.settings.FilterSettings;
import tipl.tools.*;
import tipl.util.*;

import java.util.Date;

/**
 * Script to Process Foam Data (specifically for the dk31 and dkg flow
 * experiments)
 *
 * @author Kevin Mader
 *         <p/>
 *         <pre> This and most of my scripts contain an almost excessively high number of functions, I did it in this way to make it easier for the garbage collector to work by keeeping as many variables as possible as local variables inside these subfunctions. It also makes the code somewhat easier to read and potentially reuse
 *         <p/>
 *                 <pre> Change Log:
 *         <p/>
 *                 <pre> v25 Fixed several bugs in the labeling code due to bugs induced by the update to plugins
 *         <p/>
 *                 <pre> v24 Added run as job functionality
 *         <p/>
 *                 <pre> v23 Added circle radius as parameter
 *         <p/>
 *                 <pre> v22 Added neighbor distance as a parameters
 *         <p/>
 *                 <pre> v21 Added new parameters to control the labeling
 *         <p/>
 *                 <pre> v20 Changed how bubble filling was done to be less aggressive and label the volumes correctly
 *         <p/>
 *                 <pre> v19 Added Curvature to standard output in clpor/lacun file if ccup==ccdown
 *         <p/>
 *                 <pre> v18 Added Ridge Aim Export
 *         <p/>
 *                 <pre> v17 Added morphological radius (the radius to use for the sphKernel function)
 *         <p/>
 *                 <pre> v16 Start
 */
public class UFOAM {
    public static final String kVer = "130809_024";
    public final int LASTSTAGE = 11;
    private final boolean runAsJob;
    ArgumentParser p;
    TypedPath ufiltAimFile;
    TImg ufiltAim = null;
    TypedPath outputDirectory;
    TypedPath floatAimFile;
    TImg floatAim = null;
    TypedPath threshoutAimFile;
    TImg threshoutAim = null;
    TypedPath platAimFile;
    TImg platAim = null;
    TypedPath maskAimFile;
    TImg maskAim = null;
    TypedPath bubblesAimFile;
    TImg bubblesAim = null;
    TypedPath distmapAimFile;
    TImg distmapAim = null;
    TypedPath bubbleseedsAimFile;
    TImg bubbleseedsAim = null;
    TypedPath labelsAimFile;
    TImg labelsAim = null;
    TypedPath bubblelabelsAimFile;
    TImg bubblelabelsAim = null;
    TypedPath thickmapAimFile;
    TImg thickmapAim = null;
    TypedPath rdfAimFile;
    TImg rdfAim = null;
    TypedPath curveAimFile;
    TImg curveAim = null;
    TypedPath labelnhAimFile;
    TImg labelnhAim;
    TypedPath ridgeAimFile;
    TypedPath clporCsv;
    int threshVal, closeIter, closeNH, openIter, openNH, dilNH,
            downsampleFactor, upsampleFactor, startingStage, minVolumeDef,
            maskContourSteps, porosMaskPeel, rdfSize, rdfIter, ccup, ccdown,
            neighborDist;
    double eroOccup, dilOccup, minVolumePct, maskContourBW, ccthresh, ccsigma,
            morphRadius, fixCircRadius;
    boolean doResample, doLaplace, doGradient, doMedian, doGauss, dontContour,
            doCL, doBCL, fullGrow, debugMode, resume, singleStep, doLabeling,
            rdfMILmode, writeShapeTensor, doFixCirc;
    String stageList;
    private SGEJob jobToRun = null;

    public UFOAM(final ArgumentParser pIn) {
        p = pIn;
        GrayAnalysis.doPreload = true;
        // Defaults
        // Parse filenames
        startingStage = p
                .getOptionInt(
                        "stage",
                        0,
                        "Point to start script : 0 -Filter and Scale, 1 - Threshold and Generate Plat and Bubbles, 2 - Sample Masks and peeling, 3 - Distance map, 4 - bubble labeling, 5 - bubble filling, 6 - calculate thickness, 7 - Curvature Analysis, 8 - Shape Analysis 9 - Neighborhood Analysis, 10 - structure analysis (XDF), 11 - Z/R Profiles");
        resume = p.getOptionBoolean("resume",
                "Resume based on which files are present");
        singleStep = p.getOptionBoolean("singlestep",
                "Run just the given stage");
        doLabeling = !p.getOptionBoolean("nolabeling",
                "Skip the DMGD Bubble Labeling (just use component labeling)");
        stageList = "" + startingStage;
        for (int cstg = startingStage + 1; cstg <= (singleStep ? startingStage
                : LASTSTAGE); cstg++)
            stageList += "," + cstg;
        stageList = p.getOptionString("stagelist", stageList,
                "Run the given stages (eg 1,2,3,5 )");

        ufiltAimFile = p.getOptionPath("ufilt", "", "Input unfiltered image");
        outputDirectory = p
                .getOptionPath(
                        "outdir",
                        "",
                        "Directory to save all the output files in, overwrites defaults but not user inputs");
        if (outputDirectory.length() > 0)
            useOutputDirectory();

        floatAimFile = p.getOptionPath("floatout", "",
                "Post-resampling and filtering image");
        threshoutAimFile = p
                .getOptionPath("threshout", "", "Thresheld image");
        maskAimFile = p.getOptionPath("mask", "", "Input mask");
        platAimFile = p.getOptionPath("plat", "", "Plateau border image");
        bubblesAimFile = p.getOptionPath("bubbles", "", "Bubble image");

        distmapAimFile = p.getOptionPath("distmap", "",
                "Plat Distance map image");

        bubbleseedsAimFile = p.getOptionPath("bubbleseeds", "",
                "Bubble seeds");
        labelsAimFile = p.getOptionPath("labels", "", "Bubble labels");
        bubblelabelsAimFile = p.getOptionPath("bubblelabels", "",
                "Filled bubble labels");
        thickmapAimFile = p.getOptionPath("thickmap", "", "Thickness map");
        ridgeAimFile = p
                .getOptionPath("ridge", "", "Ridge for thickness map");
        // RDF
        rdfAimFile = p.getOptionPath("rdf", "rdf.tif",
                "Radial Distribution Function Output");
        rdfMILmode = p
                .getOptionBoolean("rdfmil",
                        "Use Mean Intercept Length Mode (only connected objects count)");
        rdfSize = p.getOptionInt("rdfsize", 80,
                "Size of radial distribution function");
        rdfIter = p.getOptionInt("rdfiter", rdfSize * 1000,
                "Iterations to perform");
        // Curvature
        curveAimFile = p.getOptionPath("curvature", "curvature.tif",
                "Curvature Output");
        labelnhAimFile = p.getOptionPath("neighbors", "neighbors.tif",
                "Neighbor Count Image");
        clporCsv = p.getOptionPath("csv", "clpor",
                "Output shape analysis file (auto .csv)");

        // Parse the Parameters

        threshVal = p.getOptionInt("thresh", 170,
                "Value used to threshold image");
        closeIter = p.getOptionInt("closeIter", 3,
                "Number of closing iterations to perform on PlatBorders");
        closeNH = p.getOptionInt("closeNH", 1, "Neighborhood used for closing");

        openIter = p.getOptionInt("openIter", closeIter,
                "Number of opening iterations to perform on Bubbles");
        openNH = p.getOptionInt("openNH", closeNH,
                "Neighborhood used for opening");

        eroOccup = p
                .getOptionDouble("eroOccup", 1.0,
                        "Minimum neighborhood occupancy % for to prevent erosion deletion");
        dilNH = p.getOptionInt("dilNH", 1, "Neighborhood used for dilation");

        morphRadius = p
                .getOptionDouble(
                        "morphRadius",
                        1.75,
                        "Radius to use for the kernel (vertex shaing is sqrt(3), edge sharing is sqrt(2), and face sharing is 1)");
        dilOccup = p.getOptionDouble("dilOccup", 1.0,
                "Minimum neighborhood occupancy % for dilation addition");

        doResample = p.getOptionBoolean("resample", "Run resample on image");

        upsampleFactor = p.getOptionInt("upsampleFactor", 1,
                "Upscale Factor (scaling is upsampleFactor/downsampleFactor)");
        downsampleFactor = p
                .getOptionInt("downsampleFactor", 2,
                        "Downsample Factor (scaling is upsampleFactor/downsampleFactor)");
        doLaplace = p.getOptionBoolean("laplace", "Use a laplacian filter");
        doGradient = p.getOptionBoolean("gradient", "Use a gradient filter");
        doMedian = p.getOptionBoolean("median",
                "Use a median filter (size of upscale factor)");
        doGauss = p.getOptionBoolean("gauss", "Use a gaussian filter");
        doCL = p.getOptionBoolean("complabel",
                "Do a component labeling of the plateau borders before contouring");
        doBCL = !p
                .getOptionBoolean("nobubcl",
                        "Do not do a component labeling of the bubbles before labeling");
        minVolumePct = p.getOptionDouble("minPct", 5,
                "Minimum Volume Percent for Component labeling");

        DistLabel.ABSMINWALLDIST = p.getOptionDouble("bswalldist",
                DistLabel.ABSMINWALLDIST, "Bubble seed wall distance (vx)");
        DistLabel.MINWALLDIST = p.getOptionDouble("walldist",
                DistLabel.MINWALLDIST, "Bubble wall distance (vx)");

        DistLabel.FLATCRIT = p.getOptionDouble("flatness", DistLabel.FLATCRIT,
                "Flatness criteria");
        DistLabel.TGROWTH = p.getOptionDouble("tgrowth", DistLabel.TGROWTH,
                "TGrowth parameter");

        fullGrow = !p.getOptionBoolean("nofullgrow",
                " Do not use COV-VoronoiTransform to fill bubbles");

        dontContour = p.getOptionBoolean("nocontour",
                "Do not a contour of the plateau borders");

        writeShapeTensor = p.getOptionBoolean("shapetensor", true,
                "Include Shape Tensor");
        // Neighbor Parameters
        neighborDist = p
                .getOptionInt(
                        "neighbordist",
                        1,
                        "The distance the neighbor function should use for calculating the number of neighbors a bubble has, 1 means -1 to 1 in x,y,z");

        minVolumeDef = p.getOptionInt("minVolume", 10,
                "Minimum Volume for bubble preservation during labeling");
        debugMode = p.getOptionBoolean("debug", "Run program in debug mode");
        // Contouring
        doFixCirc = p.getOptionBoolean("circlecontour",
                "Use a simple circle contour instead of a polyellipse");
        fixCircRadius = p
                .getOptionDouble("circleradius", 1.0,
                        "Radius, as function of image size, to use for simple circle contour");
        maskContourSteps = p.getOptionInt("maskcontoursteps", 180,
                "Number of steps to use for the contouring of the mask");
        maskContourBW = p
                .getOptionDouble(
                        "maskcontourwidth",
                        0.4,
                        "Amount to blur the edges of the contouring of the mask (normalized to number of steps)");
        porosMaskPeel = p.getOptionInt("maskcontourpeel", 5,
                "Number layers to peel off with the mask");
        // Curvature Parameters
        ccup = p.getOptionInt("cupsample", 1, "Curvature - Upsample dimensions");
        ccdown = p.getOptionInt("cdownsample", 2,
                "Curvature - Downsample Factor dimensions");
        ccsigma = p.getOptionDouble("csigma", 0.5 * ccup,
                "Curvature Sigma (for filter)");
        ccthresh = p
                .getOptionDouble(
                        "cthresh",
                        0,
                        "Threshold for processing data (1.0 is only full points, 0.5 is all values which round to a full point))");

        runAsJob = p
                .getOptionBoolean("sge:runasjob",
                        "Run this script as an SGE job (adds additional settings to this task");

        if (runAsJob) {
            jobToRun = SGEJob.runAsJob("tipl.scripts.UFOAM", p, "sge:");
        }

        if (p.hasOption("?")) {
            System.out.println(" UFOAM Processing Help");
            System.out
                    .println(" Filters, thresholds, segements, and labels bubbles inside a foam");
            System.out.println(" Arguments::");
            System.out.println(" ");
            System.out.println(p.getHelp());
            System.exit(0);
        }
        p.checkForInvalid();

        if (!runAsJob) {
            System.out.println("Script Settings:" + p.toString());
            if (resume) {
                stageList = "";
                prepareResume();
                stageList = stageList.substring(1);
            }

            // Make sure the settings are valid here
            final String[] stages = stageList.split(",");

            System.out.println("Verifying Stages[" + stages.length + "] = "
                    + stageList);

            // Filter statement to check stages
            boolean notValid = false;
            for (final String cStage : stages) {
                if ((Integer.valueOf(cStage) >= 0)
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
        }

    }

    public static void main(final String[] args) {

        System.out.println(" Universal Foam Analysis (dk31 edition) Script v"
                + kVer);
        System.out
                .println(" Runs Segmentation and Generates Distance Map Script v"
                        + kVer);
        System.out.println(" Optimized for the dk31 style foams");
        System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");

        final UFOAM curScript = new UFOAM(TIPLGlobal.activeParser(args));

        curScript.runScript();

    }

    public void boundbox() {
        final Resize myResizer = new Resize(maskAim);
        myResizer.find_edges();
        myResizer.execute();
        maskAim = myResizer.ExportImages(maskAim)[0];
    }

    public TImg boundbox(final TImg cAim) {
        final Resize myResizer = new Resize(cAim);
        myResizer.cutROI(maskAim);
        myResizer.execute();
        return myResizer.ExportImages(cAim)[0];
    }

    public void cleanMasks() {
        // Close Plateau Borders

        Morpho platClose = new Morpho(threshoutAim);

        platClose.erode(new D3int(2, 2, 0), 0.2); // Remove single xy voxels

        platClose.closeMany(closeIter, closeNH, 1.0);
        platClose.useSphKernel(morphRadius * closeNH);
        // Erode once to remove small groups 5/125 of voxels
        platClose.erode(2, 0.08);
        // Remove single voxels groups in the xy plane

        platClose.erode(new D3int(2, 2, 0), 0.2);

        platAim = platClose.ExportImages(threshoutAim)[0];
        platClose = null; // Done with platClose tool

        final boolean[] platMask = TImgTools.makeTImgFullReadable(platAim)
                .getBoolAim();
        for (int i = 0; i < platMask.length; i++)
            platMask[i] = !platMask[i]; // Invert to bubbles
        platAim = platAim.inheritedAim(platMask, platAim.getDim(),
                platAim.getOffset());
        System.out.println("CMD:Invert");
        platAim.appendProcLog("CMD:Invert");

        // Open Bubbles
        Morpho bubOpen = new Morpho(platAim);
        bubOpen.openMany(openIter, openNH, 1.0);
        bubOpen.useSphKernel(morphRadius * openNH);
        // bubOpen.erode(2,0.041);

        bubblesAim = bubOpen.ExportImages(platAim)[0];
        bubOpen = null;
        if (doBCL) {
            System.out.println("Running Bubble Component Label " + bubblesAim
                    + " ...");
            final ComponentLabel myCL = new ComponentLabel(bubblesAim);
            myCL.runRelativeVolume(minVolumePct, 101);
            bubblesAim = myCL.ExportMaskAim(platAim);
        }

        final boolean[] bubbleMask = TImgTools.makeTImgFullReadable(bubblesAim)
                .getBoolAim();

        final boolean[] platdat = new boolean[bubbleMask.length];

        for (int i = 0; i < bubbleMask.length; i++)
            platdat[i] = !bubbleMask[i]; // Invert back to plateau
        System.out.println("CMD:Invert");

        platAim = bubblesAim.inheritedAim(platdat, bubblesAim.getDim(),
                bubblesAim.getOffset());
        platAim.appendProcLog("CMD:Invert");

        TIPLGlobal.runGC();

    }

    public void makeContour() {
        if (doCL) {
            System.out.println("Running Component Label " + platAim + " ...");
            final ComponentLabel myCL = new ComponentLabel(platAim);
            myCL.runRelativeVolume(minVolumePct, 101);
            platAim = myCL.ExportMaskAim(platAim);
            TImgTools.WriteTImg(platAim, platAimFile);
        }
        if (!dontContour) {
            final EasyContour myContour = new EasyContour(platAim);
            // myContour.bothContourModes=true;
            if (doFixCirc) {
                myContour.useFixedCirc(fixCircRadius);
            } else {
                myContour.usePoly(maskContourSteps, maskContourBW);
            }

            myContour.execute();
            maskAim = myContour.ExportImages(platAim)[0];
        } else {
            final boolean[] blankMask = new boolean[(int) platAim.getDim()
                    .prod()];
            for (int i = 0; i < blankMask.length; i++)
                blankMask[i] = true;
            maskAim = platAim.inheritedAim(blankMask, platAim.getDim(),
                    platAim.getOffset());
        }
    }

    public TypedPath nameVersion(final TypedPath inName, final int verNumber) {
        return inName.append("_" + verNumber + ".csv");
    }

    public TImg peelAim(final TImg cAim, final int iters) {
        return peelAim(cAim, maskAim, iters);
    }

    public TImg peelAim(final TImg cAim, final TImg pAim, final int iters) {
        final Peel cPeel = new Peel(cAim, pAim, new D3int(iters));
        System.out.println("Calculating Peel " + cAim + " ...");
        cPeel.execute();
        return cPeel.ExportImages(cAim)[0];
    }

    /**
     * Checks the status of the aim files in the directory and proceeds based on
     * their presence
     */
    public void prepareResume() {
        // "Point to start script : 0 -Filter and threshold, 1 - Generate Plat and Bubbles, 2 - Sample Masks and peeling, 3 - Distance map, 4 - bubble labeling, 5 - bubble filling, 6 - calculate thickness, 7 - Curvature Analysis, 8 - Shape Analysis 9 - Neighborhood Analysis, 10 - structure analysis (XDF)"
        if (!TIPLGlobal.tryOpen(floatAimFile)) {
            stageList += ",0";
            // Filtering is not that import if other stages are there then just
            // filter and continue

        }
        // For the intial steps if they are missing the entire analysis probably
        // needs to be redone
        if (!TIPLGlobal.tryOpen(threshoutAimFile)) {
            for (int cstg = 1; cstg <= (singleStep ? 1 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }

        if (!TIPLGlobal.tryOpen(platAimFile)) {
            for (int cstg = 1; cstg <= (singleStep ? 1 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }
        if (!TIPLGlobal.tryOpen(bubblesAimFile)) {
            for (int cstg = 1; cstg <= (singleStep ? 1 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }
        if (!TIPLGlobal.tryOpen(maskAimFile)) {
            for (int cstg = 2; cstg <= (singleStep ? 2 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }
        if (!TIPLGlobal.tryOpen(distmapAimFile)) {
            for (int cstg = 3; cstg <= (singleStep ? 3 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }
        if (!TIPLGlobal.tryOpen(labelsAimFile)) {
            for (int cstg = 4; cstg <= (singleStep ? 4 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }
        if (!TIPLGlobal.tryOpen(bubblelabelsAimFile)) {
            for (int cstg = 5; cstg <= (singleStep ? 5 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }
        if (!TIPLGlobal.tryOpen(thickmapAimFile)) {
            for (int cstg = 6; cstg <= (singleStep ? 6 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }
        if (!TIPLGlobal.tryOpen(curveAimFile)) {
            for (int cstg = 7; cstg <= (singleStep ? 7 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }
        if (!TIPLGlobal.tryOpen(labelnhAimFile)) {
            for (int cstg = 8; cstg <= (singleStep ? 8 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }
        if (!TIPLGlobal.tryOpen(rdfAimFile)) {
            for (int cstg = 10; cstg <= (singleStep ? 10 : LASTSTAGE); cstg++)
                stageList += "," + cstg;
            return;
        }
        // Basically always run 8 unless everything else is finished

    }

    public void runDistGrow() {
        DistGrow DG = new DistGrowT(distmapAim, labelsAim, bubblesAim);
        DG.runDG();
        bubblelabelsAim = DG.ExportImages(labelsAim)[0];
        labelsAim = null;

        DG = null;
        if (fullGrow) {
            ITIPLPluginIO CV2 = TIPLPluginManager.createBestPluginIO("cVoronoi", new TImg[]{bubblelabelsAim, bubblesAim});
            CV2.setParameter("-preservelabels -maxdistance=1");
            CV2.LoadImages(new TImg[]{bubblelabelsAim, bubblesAim});
            CV2.execute();
            bubblelabelsAim = CV2.ExportImages(bubblelabelsAim)[0];
            CV2 = null;
        }
    }

    public void runDistLabel() {
        DistLabel DL = new DistLabel(distmapAim, bubblesAim);

        if (bubbleseedsAimFile.length() > 0) {
            bubbleseedsAim = DL.ExportBubbleseedsAim(distmapAim);
            TImgTools.WriteTImg(bubbleseedsAim, bubbleseedsAimFile, 0, 1.0f, false, false);
        }
        DL.execute();
        labelsAim = DL.ExportImages(distmapAim)[0];
        DL = null;

    }

    public void runResample() {

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
        floatAim = fs.ExportImages(ufiltAim)[0];
        ufiltAim = null;
        TIPLGlobal.runGC();
    }

    /**
     * Run the script
     */
    public void runScript() {
        if (runAsJob)
            jobToRun.submit();
        else {
            long start;
            System.currentTimeMillis();
            start = System.currentTimeMillis();

            final String[] stages = stageList.split(",");

            int scount = 1;
            for (final String cStage : stages) {
                System.out.println("Running Stage #" + cStage + " (" + scount
                        + "/" + stages.length + ")");
                runSection(Integer.valueOf(cStage));
                TIPLGlobal.runGC();
                scount++;
            }
            System.out.println("Job Finished in "
                    + (System.currentTimeMillis() - start) / (60 * 1000F)
                    + " mins");
        }
    }

    public void runSection(final int scriptStage) {
        long start;
        System.currentTimeMillis();
        start = System.currentTimeMillis();
        switch (scriptStage) {
            case 0: // Filtering, resampling, threshold
                System.out.println("UFOAM--" + new Date()
                        + " ======: Preprocessing");
                if (ufiltAim == null)
                    ufiltAim = TImgTools.ReadTImg(ufiltAimFile);

                if (doResample) {
                    // Run filtering
                    System.out.println("UFOAM--" + new Date()
                            + " ======: Filtering");
                    runResample();
                    if (p.hasOption("floatout"))
                        TImgTools.WriteTImg(floatAim, floatAimFile);
                } else {
                    floatAim = ufiltAim;
                }

                ufiltAim = null;
                TIPLGlobal.runGC();
                break;
            case 1: // Cleaning masks
                System.out.println("UFOAM--" + new Date()
                        + " ======: Threshold and cleaning masks (morphology)");
                if (doResample) {
                    if (floatAim == null)
                        floatAim = TImgTools.ReadTImg(floatAimFile);
                } else {
                    floatAim = ufiltAim;
                    if (floatAim == null)
                        floatAim = TImgTools.ReadTImg(ufiltAimFile);
                }
                runThreshold();
                if (p.hasOption("threshout"))
                    TImgTools.WriteTImg(threshoutAim, threshoutAimFile);
                floatAim = null;
                TIPLGlobal.runGC();
                cleanMasks();
                threshoutAim = null;
                if (platAimFile.length() > 0)
                    TImgTools.WriteTImg(platAim, platAimFile);
                if (bubblesAimFile.length() > 0)
                    TImgTools.WriteTImg(bubblesAim, bubblesAimFile);
                TIPLGlobal.runGC();
                break;
            case 2: // Contour and peeling masks
                System.out.println("UFOAM--" + new Date()
                        + " ======: Contour and peeling masks");
                if (platAim == null)
                    platAim = TImgTools.ReadTImg(platAimFile);
                if (bubblesAim == null)
                    bubblesAim = TImgTools.ReadTImg(bubblesAimFile);
                makeContour();
                boundbox();
                if (maskAimFile.length() > 0)
                    TImgTools.WriteTImg(maskAim, maskAimFile);

                platAim = boundbox(platAim);
                bubblesAim = boundbox(bubblesAim);

                platAim = peelAim(platAim, 1);
                bubblesAim = peelAim(bubblesAim, porosMaskPeel);

                if (platAimFile.length() > 0)
                    TImgTools.WriteTImg(platAim, platAimFile);
                if (bubblesAimFile.length() > 0)
                    TImgTools.WriteTImg(bubblesAim, bubblesAimFile);

                maskAim = null; // Won't be needing that for the next step
                TIPLGlobal.runGC();
                break;
            case 3: // Plateau border distance maps
                System.out.println("UFOAM--" + new Date()
                        + " ======: Plateau border distance maps");
                if (platAim == null)
                    platAim = TImgTools.ReadTImg(platAimFile);
                if (maskAim == null)
                    maskAim = TImgTools.ReadTImg(maskAimFile);
                ITIPLPluginIO KV = TIPLPluginManager.createBestPluginIO("kVoronoi", new TImg[]{platAim, maskAim});
                KV.LoadImages(new TImg[]{platAim, maskAim});
                KV.execute();
                maskAim = null;
                distmapAim = KV.ExportImages(platAim)[1];
                platAim = null; // Won't be needing these for the next step
                KV = null;
                TIPLGlobal.runGC();
                if (bubblesAim == null)
                    bubblesAim = TImgTools.ReadTImg(bubblesAimFile);
                distmapAim = peelAim(distmapAim, bubblesAim, 0);
                if (distmapAimFile.length() > 0)
                    TImgTools.WriteTImg(distmapAim, distmapAimFile);
                break;
            case 4: // Label bubbles from plateau distance map
                System.out.println("UFOAM--" + new Date()
                        + " ======: Label Bubbles");
                if (bubblesAim == null)
                    bubblesAim = TImgTools.ReadTImg(bubblesAimFile);
                if (doLabeling) {
                    if (distmapAim == null)
                        distmapAim = TImgTools.ReadTImg(distmapAimFile);
                    runDistLabel();
                    if (labelsAimFile.length() > 0)
                        TImgTools.WriteTImg(labelsAim, labelsAimFile);

                    distmapAim = null;
                } else {
                    ComponentLabel clObjects = new ComponentLabel(bubblesAim);
                    clObjects.runVoxels(minVolumeDef);
                    labelsAim = clObjects.ExportLabelsAim(bubblesAim);
                    clObjects = null;
                    if (labelsAimFile.length() > 0)
                        TImgTools.WriteTImg(labelsAim, labelsAimFile);
                }

                TIPLGlobal.runGC();
                break;
            case 5: // Fill label map completely

                System.out.println("UFOAM--" + new Date()
                        + " ======: Growing Bubbles");

                if (bubblelabelsAimFile.length() > 0) {
                    if (labelsAim == null)
                        labelsAim = TImgTools.ReadTImg(labelsAimFile);
                    if (doLabeling) {
                        if (bubblesAim == null)
                            bubblesAim = TImgTools.ReadTImg(bubblesAimFile);
                        if (distmapAim == null)
                            distmapAim = TImgTools.ReadTImg(distmapAimFile);
                        System.out.println("UFOAM--" + new Date()
                                + " ======: Inflating (Gradient) Bubbles");
                        runDistGrow();
                        TImgTools.WriteTImg(bubblelabelsAim, bubblelabelsAimFile);
                    } else {
                        if (maskAim == null)
                            maskAim = TImgTools.ReadTImg(maskAimFile);
                        ITIPLPluginIO KV2 = TIPLPluginManager.createBestPluginIO("kVoronoi", new TImg[]{labelsAim, maskAim});
                        KV2.LoadImages(new TImg[]{labelsAim, maskAim});
                        KV2.execute();
                        bubblelabelsAim = KV2.ExportImages(labelsAim)[0];
                        TImgTools.WriteTImg(bubblelabelsAim, bubblelabelsAimFile);
                        KV2 = null;
                    }
                }
                labelsAim = null;
                bubblelabelsAim = null;
                bubblesAim = null;
                TIPLGlobal.runGC();
                break;
            case 6: // Calculate thickness
                System.out.println("UFOAM--" + new Date()
                        + " ======: Thickness map");
                if (distmapAim == null)
                    distmapAim = TImgTools.ReadTImg(distmapAimFile);
                if (bubblesAim == null)
                    bubblesAim = TImgTools.ReadTImg(bubblesAimFile);
                distmapAim = peelAim(distmapAim, bubblesAim, 0);
                if (thickmapAimFile.length() > 0) { // only makes sense to calculate
                    // if it will be saved
                    runThickness();
                    TImgTools.WriteTImg(thickmapAim, thickmapAimFile);
                    GrayAnalysis.StartHistogram(thickmapAim, thickmapAimFile
                    		.append( ".csv"));
                }
                thickmapAim = null;
                distmapAim = null;
                break;

            case 7: // Curvature
                System.out.println("UFOAM--" + new Date()
                        + " ======: Curvature map");
                if (curveAimFile.length() > 0) {
                    if (bubblesAim == null)
                        bubblesAim = TImgTools.ReadTImg(bubblesAimFile);
                    curveAim = Curvature.RunCC(bubblesAim, ccsigma, ccup, ccdown,
                            (float) ccthresh);
                    TImgTools.WriteTImg(curveAim, curveAimFile);
                    GrayAnalysis.StartHistogram(curveAim, curveAimFile.append( ".csv"),
                            -1, 1, 32765);
                    curveAim = null;
                } else {
                    System.out.println("Curvature map  ======: is empty skipping!");
                }
                break;
            case 8: // Shape Analysis
                // Lacuna Version Scheme 0 = mask distance, 1 = canal distance, 2 =
                // canal volume, 3 =
                System.out.println("UFOAM--" + new Date()
                        + " ======: Shape Analysis");

                if (bubblelabelsAim == null)
                    bubblelabelsAim = TImgTools.ReadTImg(bubblelabelsAimFile);

                GrayAnalysis.StartLacunaAnalysis(bubblelabelsAim,
                        nameVersion(clporCsv, 0), "Mask", writeShapeTensor);
                // Thickness
                if (thickmapAim == null)
                    thickmapAim = TImgTools.ReadTImg(thickmapAimFile);
                GrayAnalysis.AddRegionColumn(bubblelabelsAim, thickmapAim,
                        nameVersion(clporCsv, 0), nameVersion(clporCsv, 1),
                        "Thickness");
                thickmapAim = null;
                TIPLGlobal.runGC();

                break;
            case 9: // Neighborhood
                System.out.println("UFOAM--" + new Date()
                        + " ======: Neighbor Analysis");
                if (bubblelabelsAim == null)
                    bubblelabelsAim = TImgTools.ReadTImg(bubblelabelsAimFile);
                Neighbors nbor = new Neighbors(bubblelabelsAim);
                nbor.neighborSize = new D3int(neighborDist);
                System.out.println("Calculating neighbors " + bubblelabelsAim
                        + " ...");
                nbor.execute();
                System.out.println("Writing csv neigbhor-list ...");
                nbor.WriteNeighborList(clporCsv.append("_edge.csv"));
                labelnhAim = nbor.ExportCountImageAim(bubblelabelsAim);
                GrayAnalysis.AddRegionColumn(bubblelabelsAim, labelnhAim,
                        nameVersion(clporCsv, 1), nameVersion(clporCsv, 2),
                        "Neighbors");

                nbor = null;
                if (labelnhAimFile.length() > 0)
                    TImgTools.WriteTImg(labelnhAim, labelnhAimFile);
                labelnhAim = null;
                TIPLGlobal.runGC();

                // Curvature
                if (ccup == ccdown) {
                    if (curveAim == null)
                        curveAim = TImgTools.ReadTImg(curveAimFile);
                    GrayAnalysis.AddRegionColumn(labelsAim, curveAim,
                            nameVersion(clporCsv, 1), nameVersion(clporCsv, 2),
                            "Curvature");
                    curveAim = null;
                    TIPLGlobal.runGC();
                }

                break;
            case 10: // XDF
                if (bubblesAim == null)
                    bubblesAim = TImgTools.ReadTImg(bubblesAimFile);
                if (maskAim == null)
                    maskAim = TImgTools.ReadTImg(maskAimFile);
                final XDF cXDF = new XDF();
                cXDF.setParameter("-rdfsize=" + new D3int(rdfSize) + " -iter="
                        + rdfIter);
                cXDF.LoadImages(new TImgRO[]{bubblesAim, maskAim});
                cXDF.milMode = rdfMILmode; // for volanic rock this makes sense
                cXDF.execute();
                rdfAim = cXDF.ExportImages(bubblesAim)[0];
                TImgTools.WriteTImg(rdfAim, rdfAimFile);
                break;
            case 11: // Z Profiles
                if (maskAim == null)
                    maskAim = TImgTools.ReadTImg(maskAimFile);

                if (platAim == null)
                    platAim = TImgTools.ReadTImg(platAimFile);
                GrayAnalysis.StartZProfile(platAim, maskAim,
                        platAimFile.append( "_z.txt"), -1);
                GrayAnalysis.StartRProfile(platAim, maskAim,
                        platAimFile.append("_r.txt"), -1);
                GrayAnalysis.StartRCylProfile(platAim, maskAim, platAimFile.append( "_rcyl.txt"), -1);
                platAim = null;

                if (thickmapAim == null)
                    thickmapAim = TImgTools.ReadTImg(thickmapAimFile);
                GrayAnalysis.StartZProfile(thickmapAim, maskAim, thickmapAimFile.append("_z.txt"), 0.1f);
                GrayAnalysis.StartRProfile(thickmapAim, maskAim, thickmapAimFile.append("_r.txt"), 0.1f);
                GrayAnalysis.StartRCylProfile(thickmapAim, maskAim, thickmapAimFile.append("_rcyl.txt"), 0.1f);
                thickmapAim = null;

                if (curveAim == null)
                    curveAim = TImgTools.ReadTImg(curveAimFile);
                GrayAnalysis
                        .StartZProfile(curveAim, curveAimFile.append( "_z.txt"), -1000);
                GrayAnalysis
                        .StartRProfile(curveAim, curveAimFile.append( "_r.txt"), -1000);
                GrayAnalysis.StartRCylProfile(curveAim, curveAimFile.append( "_rcyl.txt"),
                        -1000);
                curveAim = null;

                if (labelnhAim == null)
                    labelnhAim = TImgTools.ReadTImg(labelnhAimFile);
                GrayAnalysis.StartZProfile(labelnhAim, maskAim, labelnhAimFile.append("_z.txt"), 1);
                GrayAnalysis.StartRProfile(labelnhAim, maskAim, labelnhAimFile.append("_r.txt"), 1);
                GrayAnalysis.StartRCylProfile(labelnhAim, maskAim, labelnhAimFile.append("_rcyl.txt"), 1);
                labelnhAim = null;

                break;

        }
        System.out
                .println("Task Finished in "
                        + (System.currentTimeMillis() - start) / (60 * 1000F)
                        + " mins");

    }

    public void runThickness() {
        final ITIPLPluginIO KT = TIPLPluginManager.createBestPluginIO("HildThickness", new TImg[] { distmapAim });
        KT.LoadImages(new TImg[] { distmapAim });
        if (ridgeAimFile.length() > 0)
            TImgTools.WriteTImg(((HildThickness) KT).ExportRidgeAim(distmapAim), ridgeAimFile);
        KT.execute();
        thickmapAim = KT.ExportImages(distmapAim)[0];
    }

    public void runThreshold() {
        // Threshold the data
        final short[] inImg = TImgTools.makeTImgFullReadable(floatAim).getShortAim();
        boolean[] scdat = new boolean[inImg.length];
        for (int i = 0; i < inImg.length; i++)
            scdat[i] = inImg[i] > threshVal;

        threshoutAim = floatAim.inheritedAim(scdat, floatAim.getDim(),
                new D3int(0));
        threshoutAim.appendProcLog("CMD:Threshold, Value:" + threshVal);

        // Clear out old variables
        scdat = null;

    }

    public void useOutputDirectory() {
        final String[] activeArguments = {"plat", "bubbleseeds", "labels",
                "threshout", "floatout", "distmap", "thickmap", "bubbles",
                "mask", "bubblelabels", "rdf", "curvature", "rdf", "ridge", "neighbors"};
        for (String curArg : activeArguments) {
            if (!p.hasOption(curArg)) {
                p.addOption(curArg, outputDirectory + "/"
                        + curArg + ".tif");
                System.out.println("\t -" + curArg + " = "
                        + outputDirectory + "/" + curArg + ".tif");
            }
        }
        if (!p.hasOption("csv")) p.addOption("csv", outputDirectory + "/"
                + "clpor");

    }

}
