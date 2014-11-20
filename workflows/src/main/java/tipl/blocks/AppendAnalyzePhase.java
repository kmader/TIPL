/**
 *
 */
package tipl.blocks;

import tipl.formats.TImg;
import tipl.util.ArgumentParser;
import tipl.util.TImgTools;
import tipl.util.TypedPath;

/**
 * A block for adding additional analyses to a analyzephase (different masks for example)
 *
 * @author mader
 */
public class AppendAnalyzePhase extends AnalyzePhase {
    public final IBlockImage[] inImages = new IBlockImage[]{
            new BlockImage("labeled", "labeled.tif", "Labeled Image", false),
            new BlockImage("mask", "mask.tif", "Mask Image", true)};
    public final IBlockImage[] outImages = new IBlockImage[]{
            new BlockImage("density", "adensity.tif", "Density Image", false),
            new BlockImage("neighbors", "aneighbors.tif", "Neighbor Image", false)
    };

    public AppendAnalyzePhase(final BlockIOHelper helperTools, final String inPrefix) {
        super(helperTools,inPrefix);
    }


    @Override
    protected IBlockImage[] bGetInputNames() {
        return inImages;
    }

    @Override
    protected IBlockImage[] bGetOutputNames() {
        return outImages;
    }

    @Override
    public ArgumentParser setParameterBlock(final ArgumentParser inp) {
        TypedPath seedFileName = inp.getOptionPath(prefix + "seedfile", "seedfile.csv",
                "Shape file to append");
        phaseName = inp.getOptionPath(prefix + "phase", "phase2", "Phase name");
        SNA = new ShapeAndNeighborAnalysis(seedFileName, phaseName);
        final ArgumentParser p = SNA.getNeighborPlugin().setParameter(inp,
                prefix);
        return p;

    }

    @Override
    public boolean executeBlock() {
        final TImg labImg = TImgTools.ReadTImg(getFileParameter("labeled"), true,
                true);
        return performShapeAnalysis(labImg);
    }

    @Override
    protected String getDescription() {
        return "Add a density, and neighborhood analysis to an existing shape analysis";
    }
}
