package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to label connected objects which meet a given criteria with different
 * values
 */
public class ComponentLabel extends BaseTIPLPluginIO {
    @TIPLPluginManager.PluginInfo(pluginType = "ComponentLabel",
            desc = "Full memory component labeling",
            sliceBased = false,
            maximumSize = 1024 * 1024 * 1024,
            bytesPerVoxel = 3)
    final public static class clFactory implements TIPLPluginManager.TIPLPluginFactory {
        @Override
        public ITIPLPlugin get() {
            return new ComponentLabel();
        }
    };
    public static float threshVal = 0.0f;
    public static boolean invert = false;
    public final Integer zero = 0;
    public final Integer one = 1;
    /**
     * The input boolean image with the objects to be analyzed
     */
    public boolean[] scdat;
    /**
     * The generated labels image where each distinct group is labeled
     * differently
     */
    public int[] labels;
    public int maxlabel;
    public boolean verboseMode = true;
    /**
     * the filter to use when executing *
     */
    protected CLFilter objFilter = new CLFilter() {
        @Override
        public boolean accept(final int labelNumber, final int voxCount) {
            return (voxCount > 0);
        }

        @Override
        public String getProcLog() {
            return "CL: Using Default Volume Filter :>0";
        }

        @Override
        public void prescan(final int labelNumber, final int voxCount) {
            return;
        }

        ;
    };
    List<Integer> labelcounts;
    List<Integer> labelremap;

    public ComponentLabel() {
    }

    @Deprecated
    public ComponentLabel(final boolean[] inputmap, final D3int idim,
                          final D3int ioffset) {
        aimLength = inputmap.length;
        scdat = inputmap;
        if (invert) {
            scdat = new boolean[aimLength];
            for (int i = 0; i < aimLength; i++)
                scdat[i] = !inputmap[i];
        }
        InitLabels(idim, ioffset);
    }

    public ComponentLabel(final TImg bubblesAim) {
        LoadImages(new TImgRO[]{bubblesAim});
    }

    private Integer chase_up(final int clab) {
        // Follow a trail of remaps recursively
        // if (jumps>3) cout+"J"+jumps+", ";
        final Integer cval = labelremap.get(clab);
        if (cval != clab)
            return chase_up(cval);
        else
            return cval;
    }

    @Override
    public boolean execute() {

        int curLabel = 1;
        long cVox = 0;
        long sVox = 0;
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

                    if ((scdat[off]) && (labels[off] == 0)) {
                        cVox++;
                        int applyLabel = 0;
                        int off2;
                        // for(int ik=0; ik<n.length; ik++) {
                        // off2=off+n[ik];
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
                                        if ((scdat[off2]) && (off != off2)) {
                                            if (applyLabel < 1) {
                                                if (labels[off2] > 0) {
                                                    applyLabel = labels[off2];
                                                    set_label(off, applyLabel);
                                                }
                                            } else {
                                                set_label(off2, applyLabel);
                                                sVox++;
                                            }
                                        }
                                    }

                                }
                            }
                        }
                        // }
                        if (applyLabel < 1) {
                            curLabel = get_next_label(curLabel);
                            applyLabel = curLabel;
                            set_label(off, applyLabel);
                            if (curLabel % 5000 == 0)
                                if (verboseMode)
                                    System.out.println("(Z: " + z
                                            + ", LABELS: " + curLabel + ")");
                        }
                    }

                }
            }
        }
        if (verboseMode)
            System.out.println("Done with first screen, labels:" + curLabel
                    + ", " + labelcounts.get(1) + " of " + cVox + " and "
                    + sVox + ":" + (labelcounts.get(1) + 0.0)
                    / (aimLength + 0.0));

        if (verboseMode)
            System.out.println("Merging Similar Groups...");
        resetRemap();
        // This section of the code swaps neighbors which are close

        int swapSteps = 0;
        int changescnt = 1;

        while (changescnt > 0) {
            changescnt = 0;

            for (int z = lowz; z < uppz; z++) {
                for (int y = lowy; y < uppy; y++) {
                    off = (z * dim.y + y) * dim.x + lowx;
                    for (int x = lowx; x < uppx; x++, off++) {

                        if (labels[off] > 0) {// Eligble Full Voxel
                            // for(int ik=0; ik<n.length; ik++) {
                            // off2=off+n[ik];
                            int off2;
                            for (int z2 = max(z - neighborSize.z, lowz); z2 <= min(
                                    z + neighborSize.z, uppz - 1); z2++) {
                                for (int y2 = max(y - neighborSize.y, lowy); y2 <= min(
                                        y + neighborSize.y, uppy - 1); y2++) {
                                    off2 = (z2 * dim.y + y2) * dim.x
                                            + max(x - neighborSize.x, lowx);
                                    for (int x2 = max(x - neighborSize.x, lowx); x2 <= min(
                                            x + neighborSize.x, uppx - 1); x2++, off2++) {
                                        if (curKernel.inside(off, off2, x, x2,
                                                y, y2, z, z2)) {
                                            if ((labels[off2] != labels[off])
                                                    && (labels[off2] > 0)) {
                                                // label_merge(labels[off+n[ik]],labels[off],labelcounts,labels,aimLength);
                                                final int labA = max(
                                                        labels[off],
                                                        labels[off2]);
                                                final int labB = min(
                                                        labels[off],
                                                        labels[off2]);
                                                labelremap.set(labA,
                                                        labB);
                                                changescnt++;
                                            }
                                        }

                                    }
                                }
                            }

                            // }
                        }
                    }
                }
            }
            swapSteps++;
            remap_merge();
            if (verboseMode)
                System.out.println("Labels:" + maxlabel + ", Steps : "
                        + swapSteps + " Swaps : " + changescnt
                        + ", Active Region(pm): " + (1000 * changescnt)
                        / (aimLength));
            resetRemap();

            // for(int ir=1;ir<20;ir++) if (verboseMode)
            // System.out.println(ir+", "+(Integer) labelcounts.get(ir));
            printList();
            // End While Loop
        }
        int objCount = 0;
        // Prescan the list
        for (int ir = 1; ir <= maxlabel; ir++) {
            objFilter.prescan(ir, labelcounts.get(ir));
        }
        // Filter the list
        for (int ir = 1; ir <= maxlabel; ir++) {
            if (objFilter.accept(ir, labelcounts.get(ir))) {
                labelremap.set(ir, ir);
                objCount++;
            } else
                labelremap.set(ir, zero);
        }
        if (verboseMode)
            System.out
                    .println("Removing objects outside of filter, remaining : "
                            + objCount);
        remap_merge();

        int endSwap = 1;
        // while (endSwap>0) {
        endSwap = 0;
        resetRemap();
        // resetCounts();
        int lastlabel = get_next_free_label(1); // find the first free

        for (int ir = 1; ir <= maxlabel; ir++) {
            final Integer countIR = labelcounts.get(ir);
            if (countIR > 0) {
                if ((lastlabel < ir) && (lastlabel < maxlabel)
                        && (lastlabel > 0)) {
                    // cout<<ir<<"->"<<lastlabel<<endl;
                    // System.out.println(lastlabel+" of "+maxlabel+" at index:"+ir);
                    labelremap.set(ir, lastlabel);
                    labelcounts.set(lastlabel, countIR);
                    labelcounts.set(ir, zero);
                    lastlabel = get_next_free_label(lastlabel - 1);
                    endSwap++;
                }
            }
        }
        remap_merge_combine(endSwap);
        for (int ir = 1; ir < 5; ir++)
            if (verboseMode)
                System.out.println("SW:" + endSwap + ", " + ir + ", "
                        + labelcounts.get(ir));
        // }
        // for(int ir=1;ir<maxlabel;ir++) System.out.println(ir+", "+(Integer)
        // labelcounts.get(ir));
        printList();
        procLog += "CMD:ComponentLabel :N" + neighborSize + " Max Label:"
                + maxlabel + ", Swap Steps:" + swapSteps + "\n";
        procLog += "CL: Filter Used:" + objFilter.getProcLog() + "\n";
        procLog += "CL: Kernel Used ::\n";
        procLog += printKernel(curKernel) + "\n\n";
        runCount++;
        return true;
    }

    @Override
    public boolean execute(final String action) {
        if (!action.equals(""))
            throw new IllegalArgumentException(
                    "Execute Does not offer any control in this plugins"
                            + getPluginName());
        return execute();
    }

    @Override
    public boolean execute(final String command, final Object cObj) {
        if (command.equalsIgnoreCase("runVoxels")) {
            if (cObj instanceof Integer)
                return runVoxels((Integer) cObj);

        }
        return super.execute(command, cObj);
    }

    /**
     * This implementation exports the labels image and then the mask image
     */
    @Override
    public TImg[] ExportImages(final TImgRO templateImage) {
        final TImgRO.CanExport cImg = TImgTools
                .makeTImgExportable(templateImage);
        return new TImg[]{ExportLabelsAim(cImg), ExportMaskAim(cImg)};
    }

    /**
     * Exports the labeled result based on a template aim
     *
     * @param templateAim input template aim file
     */
    public TImg ExportLabelsAim(final TImgRO.CanExport templateAim) {
        if (isInitialized) {
            if (runCount > 0) {
                final TImg outVirtualAim = templateAim.inheritedAim(labels,
                        dim, new D3int(0));
                outVirtualAim.appendProcLog(procLog);
                return outVirtualAim;

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

    /**
     * Exports the mask of the labeled results based on a template aim
     *
     * @param templateAim input template aim file
     */
    public TImg ExportMaskAim(final TImgRO.CanExport templateAim) {
        if (isInitialized) {
            if (runCount > 0) {
                final boolean[] outputMask = new boolean[labels.length];
                for (int i = 0; i < labels.length; i++)
                    outputMask[i] = (labels[i] > 0);
                final TImg outVirtualAim = templateAim.inheritedAim(outputMask,
                        dim, new D3int(0));
                outVirtualAim.appendProcLog(procLog);
                return outVirtualAim;

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

    /**
     * Exports the mask of the results after filter inFilt has been applied to
     * them (allows for multiple maskings of the same data)
     *
     * @param templateAim  input template aim file
     * @param inMaskFilter filter to use for masking
     */
    @Deprecated
    public TImg ExportMaskAim(final TImgRO.CanExport templateAim,
                              final CLFilter inMaskFilter) {
        if (isInitialized) {
            if (runCount > 0) {
                // Prescan the list
                for (int ir = 1; ir <= maxlabel; ir++) {
                    inMaskFilter.prescan(ir, labelcounts.get(ir));
                }
                // Filter the list
                final boolean[] acceptList = new boolean[maxlabel + 1];
                for (int ir = 1; ir <= maxlabel; ir++) {
                    if (inMaskFilter.accept(ir, labelcounts.get(ir)))
                        acceptList[ir] = true;
                    else
                        acceptList[ir] = false;
                }

                final boolean[] outputMask = new boolean[labels.length];
                for (int i = 0; i < labels.length; i++)
                    outputMask[i] = acceptList[labels[i]];
                final TImg outVirtualAim = templateAim.inheritedAim(outputMask,
                        dim, new D3int(0));
                outVirtualAim.appendProcLog(procLog);
                outVirtualAim.appendProcLog("CL: Applied Mask Filter:"
                        + inMaskFilter.getProcLog());
                return outVirtualAim;

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

    /**
     * Exports the mask of the results after filter inFilt has been applied to
     * them (allows for multiple maskings of the same data) <li>Select
     * components which fall within the given voxel range
     *
     * @param templateAim input template aim file
     */
    public TImg ExportMaskAimFirstComponent(final TImgRO.CanExport templateAim) {
        final CLFilter maskObjFilter = new CLFilter() {
            int maxComp = -1;
            int maxVol = -1;

            @Override
            public boolean accept(final int labelNumber, final int voxCount) {
                return (labelNumber == maxComp);
            }

            @Override
            public String getProcLog() {
                return "CL: Using largest component filter\n";
            }

            @Override
            public void prescan(final int labelNumber, final int voxCount) {
                if ((voxCount > maxVol) | (maxComp == -1)) {
                    maxComp = labelNumber;
                    maxVol = voxCount;
                }
            }
        };
        final TImg outVirtualAim = ExportMaskAim(templateAim, maskObjFilter);
        return outVirtualAim;
    }

    /**
     * Exports the mask of the results after filter inFilt has been applied to
     * them (allows for multiple maskings of the same data) <li>Select
     * components which fall within the given voxel range
     *
     * @param templateAim input template aim file
     * @param minPct      the voxel count of the smallest object to be accepted
     * @param maxPct      the voxel count of the largest object to be accepted
     */
    public TImg ExportMaskAimRelativeVolume(final TImgRO.CanExport templateAim,
                                            final double minPct, final double maxPct) {
        final double fMinPct = minPct;
        final double fMaxPct = maxPct;
        final CLFilter maskObjFilter = new CLFilter() {
            double totalVolume = 0.0;

            @Override
            public boolean accept(final int labelNumber, final int voxCount) {
                final double cPct = (voxCount) / totalVolume * 100;
                return ((cPct > fMinPct) & (cPct < fMaxPct));
            }

            @Override
            public String getProcLog() {
                return "CL: Using Relative Volume Filter :" + minPct + "% -"
                        + maxPct + "%\n";
            }

            @Override
            public void prescan(final int labelNumber, final int voxCount) {
                totalVolume += voxCount;
            }
        };
        final TImg outVirtualAim = ExportMaskAim(templateAim, maskObjFilter);
        return outVirtualAim;

    }

    /**
     * Exports the mask of the results after filter inFilt has been applied to
     * them (allows for multiple maskings of the same data) <li>Select
     * components which fall within the given voxel range
     *
     * @param templateAim input template aim file
     * @param elSize      the element size in the aim being considered
     * @param minVolume   the volume of the smallest object to be accepted
     */
    public TImg ExportMaskAimVolume(final TImgRO.CanExport templateAim,
                                    final D3float elSize, final double minVolume) {
        return ExportMaskAimVoxels(templateAim,
                (int) (minVolume / (elSize.prod())));
    }

    /**
     * Exports the mask of the results after filter inFilt has been applied to
     * them (allows for multiple maskings of the same data) <li>Select
     * components which fall within the given voxel range
     *
     * @param templateAim input template aim file
     * @param elSize      the element size in the aim being considered
     * @param minVolume   the volume of the smallest object to be accepted
     * @param maxVolume   the volume of the largest object to be accepted
     */
    public TImg ExportMaskAimVolume(final TImgRO.CanExport templateAim,
                                    final D3float elSize, final double minVolume, final double maxVolume) {
        return ExportMaskAimVoxels(templateAim,
                (int) (minVolume / (elSize.prod())),
                (int) (maxVolume / (elSize.prod())));
    }

    /**
     * Exports the mask of the results after filter inFilt has been applied to
     * them (allows for multiple maskings of the same data) <li>Select
     * components which fall within the given voxel range
     *
     * @param templateAim input template aim file
     * @param minVolume   the voxel count of the smallest object to be accepted
     */
    @Deprecated
    public TImg ExportMaskAimVoxels(final TImgRO.CanExport templateAim,
                                    final int minVolume) {
        final int realMinVolume = minVolume;
        final CLFilter inObjFilter = new CLFilter() {
            @Override
            public boolean accept(final int labelNumber, final int voxCount) {
                return (voxCount > realMinVolume);
            }

            @Override
            public String getProcLog() {
                return "Using Absolute Volume Filter :>" + minVolume;
            }

            @Override
            public void prescan(final int labelNumber, final int voxCount) {
                return;
            }
        };

        final TImg outVirtualAim = ExportMaskAim(templateAim, inObjFilter);
        return outVirtualAim;
    }

    /**
     * Exports the mask of the results after filter inFilt has been applied to
     * them (allows for multiple maskings of the same data) <li>Select
     * components which fall within the given voxel range
     *
     * @param templateAim input template aim file
     * @param minVolume   the voxel count of the smallest object to be accepted
     * @param maxVolume   the voxel count of the largest object to be accepted
     */
    public TImg ExportMaskAimVoxels(final TImgRO.CanExport templateAim,
                                    final int minVolume, final int maxVolume) {
        final int realMinVolume = minVolume;
        final int realMaxVolume = maxVolume;
        objFilter = new CLFilter() {
            @Override
            public boolean accept(final int labelNumber, final int voxCount) {
                return ((voxCount > realMinVolume) & (voxCount < realMaxVolume));
            }

            @Override
            public String getProcLog() {
                return "CL: Using Absolute Volume Filter :" + minVolume + "-"
                        + maxVolume + "\n";
            }

            @Override
            public void prescan(final int labelNumber, final int voxCount) {
                return;
            }
        };

        final TImg outVirtualAim = ExportMaskAim(templateAim, objFilter);
        outVirtualAim.appendProcLog(objFilter.getProcLog());
        return outVirtualAim;
    }

    private int get_next_free_label(int lastlabel) {
        if (lastlabel < 1)
            lastlabel = 1;
        for (int i = lastlabel; i <= maxlabel; i++) {
            if (labelcounts.get(i) == 0)
                return i;
        }
        return -1;
    }

    private int get_next_label(final int lastlabel) {
        // Check from lastlabel and then restart at the beginning in case
        // any were missed
        final int clabel = get_next_free_label(lastlabel);
        if (clabel < 0) {
            final int dlabel = get_next_free_label(1);
            if (dlabel < 0) {
                final int oldlen = labelcounts.size();

                for (int ir = labelcounts.size(); ir <= (maxlabel + 10000); ir++) {
                    labelcounts.add(zero);
                }
                if (verboseMode)
                    System.out.println(oldlen
                            + " was full, Adding 10000 more labels..."
                            + labelcounts.size());
                maxlabel = labelcounts.size() - 1;
                return oldlen + 1;
            }
            return dlabel;
        }
        return clabel;

    }

    @Override
    public Object getInfo(final String request) {
        final String sr = request.trim().toLowerCase();
        if (sr.equals("maxlabel"))
            return maxlabel;
        if (sr.equals("firstcount"))
            return labelcounts.get(1);
        if (sr.equals("avgcount")) {
            long oVal = 0;
            int objCount = 0;

            // dont use iterator because labelcounts.get(0) is meaningless ::
            // for(Integer cCount: labelcounts) {

            for (int ir = 1; ir <= maxlabel; ir++) {
                final int cVal = labelcounts.get(ir);
                oVal += cVal;
                objCount += cVal > 0 ? 1 : 0;
            }

            return oVal / (objCount + ((objCount == 0) ? 1.0 : 0.0)); // dont
            // divide
            // by
            // 0!
        }
        return super.getInfo(request);
    }

    @Override
    public String getPluginName() {
        return "ComponentLabel";
    }

    private void InitLabels(final D3int idim, final D3int ioffset) {
        labels = new int[aimLength];
        labelcounts = new ArrayList<Integer>(10000);
        for (int ir = 0; ir <= (10000); ir++) {
            labelcounts.add(zero);
        }
        maxlabel = labelcounts.size() - 1;
        InitDims(idim, ioffset);

    }

    @Override
    public void LoadImages(final TImgRO[] inImages) {
        if (inImages.length < 1)
            throw new IllegalArgumentException("Too few input images given!");
        final boolean[] inputmap = TImgTools.makeTImgFullReadable(inImages[0])
                .getBoolAim();
        aimLength = inputmap.length;
        scdat = inputmap;
        if (invert) {
            scdat = new boolean[aimLength];
            for (int i = 0; i < aimLength; i++)
                scdat[i] = !inputmap[i];
        }
        InitLabels(inImages[0].getDim(), inImages[0].getOffset());

    }

    private void printList() {
        int ir = 1, ic = 0;
        if (verboseMode) {
            while (ic < 20) {
                final Integer aVal = labelcounts.get(ir);
                if (aVal > 0) {
                    System.out.println(ir + ", " + aVal);
                    ic++;
                }
                if (ir >= maxlabel) {
                    ic = 21;
                    break;
                }
                ir++;
            }
        }

    }

    private void remap_merge() {
        // Chase up code
        int merges = 0;
        for (int ij = maxlabel; ij > 0; ij--) {
            final Integer reval = chase_up(ij);
            // else reval=(Integer) labelremap.get(ij);
            if (reval != ij) {
                merges++;
                final int oldcount = labelcounts.get(ij);
                final int newcount = labelcounts.get(reval);
                labelcounts.set(reval, oldcount
                        + newcount);
                labelcounts.set(ij, zero);
                labelremap.set(ij, reval);
            }
        }
        remap_merge_combine(merges);
    }

    private void remap_merge_combine(final int merges) {
        maxlabel = 0;

        int delVox = 0;
        for (int ij = 0; ij < aimLength; ij++) {
            if (labels[ij] > 0) {
                final int labRemapVal = labelremap.get(labels[ij]);
                if (labRemapVal != labels[ij]) {
                    labels[ij] = labRemapVal;
                    if (labRemapVal == 0)
                        delVox++;
                }
                if (labels[ij] > maxlabel)
                    maxlabel = labels[ij];
            }
        }
        if (verboseMode)
            System.out.println(" Merges Performed:" + merges + ", del voxels:"
                    + delVox + " new max:" + maxlabel);

    }

    private void resetRemap() {
        labelremap = null;
        TIPLGlobal.runGC();
        labelremap = new ArrayList<Integer>(maxlabel);
        for (int ir = 0; ir <= maxlabel; ir++)
            labelremap.add(ir);
    }

    /**
     * Maintains backwards compatibilitiy
     */
    public void run(final BaseTIPLPluginIn.TIPLFilter inObjFilter) {
        final BaseTIPLPluginIn.TIPLFilter finObjFilter = inObjFilter;
        objFilter = new CLFilter() {
            @Override
            public boolean accept(final int labelNumber, final int voxCount) {
                return finObjFilter.accept(labelNumber, voxCount);
            }

            @Override
            public String getProcLog() {
                return "Using Old School TIPLFilter :" + inObjFilter;
            }

            @Override
            public void prescan(final int labelNumber, final int voxCount) {
                return;
            }
        };
        execute();
    }

    /**
     * run and return only the largest component
     */
    public void runFirstComponent() {
        objFilter = new CLFilter() {
            int maxComp = -1;
            int maxVol = -1;

            @Override
            public boolean accept(final int labelNumber, final int voxCount) {
                return (labelNumber == maxComp);
            }

            @Override
            public String getProcLog() {
                return "Using largest component filter\n";
            }

            @Override
            public void prescan(final int labelNumber, final int voxCount) {
                if ((voxCount > maxVol) | (maxComp == -1)) {
                    maxComp = labelNumber;
                    maxVol = voxCount;
                }
            }

            ;
        };
        execute();
    }

    /**
     * run and return only components which fall within the given range in terms
     * of percent of total object (mask/thresh) volume
     *
     * @param minPct Minimum percent to accept (0-100%)
     * @param maxPct Maximum percent to accept (0-100%)
     */
    public void runRelativeVolume(final double minPct, final double maxPct) {
        final double fMinPct = minPct;
        final double fMaxPct = maxPct;
        objFilter = new CLFilter() {
            double totalVolume = 0.0;

            @Override
            public boolean accept(final int labelNumber, final int voxCount) {
                final double cPct = (voxCount) / totalVolume * 100;
                return ((cPct > fMinPct) & (cPct < fMaxPct));
            }

            @Override
            public String getProcLog() {
                return "Using Relative Volume Filter :" + minPct + "% -"
                        + maxPct + "%\n";
            }

            @Override
            public void prescan(final int labelNumber, final int voxCount) {
                totalVolume += voxCount;
            }
        };
        execute();
    }

    /**
     * Select components which are greater than a given volume range
     *
     * @param elSize    the size of one voxel in mm (stored in the aim header file)
     * @param minVolume the volume of the smallest object to be accepted
     */
    public void runVolume(final D3float elSize, final double minVolume) {
        procLog += "CL: Using Scaled Volume Filter :" + minVolume
                + ",  VoxelSize:" + elSize + "\n";
        runVoxels((int) (minVolume / (elSize.prod())));
    }

    /**
     * Select components which fall within the given volume range
     *
     * @param elSize    the size of one voxel in mm (stored in the aim header file)
     * @param minVolume the volume of the smallest object to be accepted (in mm^3
     * @param maxVolume the volume of the largest object to be accepted
     */
    public void runVolume(final D3float elSize, final double minVolume,
                          final double maxVolume) {
        procLog += "CL: Using Scaled Volume Filter :" + minVolume + "-"
                + maxVolume + ", VoxelSize:" + elSize + "\n";
        runVoxels((int) (minVolume / (elSize.prod())),
                (int) (maxVolume / (elSize.prod())));
    }

    /**
     * Select components which fall within the given voxel range
     *
     * @param minVolume the voxel count of the smallest object to be accepted
     */
    public boolean runVoxels(final int minVolume) {
        final int realMinVolume = minVolume;
        objFilter = new CLFilter() {
            @Override
            public boolean accept(final int labelNumber, final int voxCount) {
                return (voxCount > realMinVolume);
            }

            @Override
            public String getProcLog() {
                return "Using Absolute Volume Filter :>" + minVolume + "\n";
            }

            @Override
            public void prescan(final int labelNumber, final int voxCount) {
                return;
            }
        };
        return execute();

    }

    /**
     * Select components which fall within the given voxel range
     *
     * @param minVolume the voxel count of the smallest object to be accepted
     * @param maxVolume the voxel count of the largest object to be accepted
     */
    public boolean runVoxels(final int minVolume, final int maxVolume) {
        final int realMinVolume = minVolume;
        final int realMaxVolume = maxVolume;
        objFilter = new CLFilter() {
            @Override
            public boolean accept(final int labelNumber, final int voxCount) {
                return ((voxCount > realMinVolume) & (voxCount < realMaxVolume));
            }

            @Override
            public String getProcLog() {
                return "Using Absolute Volume Filter :" + minVolume + "-"
                        + maxVolume;
            }

            @Override
            public void prescan(final int labelNumber, final int voxCount) {
                return;
            }
        };
        return execute();

    }

    private void set_label(final int pt, final int applyLabel) {
        labelcounts.set(applyLabel, labelcounts.get(applyLabel)
                .intValue() + 1);
        if (labels[pt] != 0)
            labelcounts.set(labels[pt], labelcounts.get(labels[pt])
                    .intValue() - 1);
        labels[pt] = applyLabel;
    }

    /**
     * A generic interface for thresholding operations, typically only one
     * function is needed but the interface provides all anyways
     */
    public interface CLFilter {
        /**
         * whether or not a group should be accepted based on a label number (for
         * sorted lists) or voxel count
         *
         * @param labelNumber The label number of the given object
         * @param voxCount    The voxel count of the given object
         */
        boolean accept(int labelNumber, int voxCount);

        /**
         * the information to add to the processing log aobut the filter being
         * used
         */
        String getProcLog();

        /**
         * the list is scanned twice, the first time is to collect statistics
         * (prescan), the second time to filter (accept)
         *
         * @param labelNumber The label number of the given object
         * @param voxCount    The voxel count of the given object
         */
        void prescan(int labelNumber, int voxCount);
    }

}
