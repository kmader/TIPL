package tipl.blocks;

import tipl.blocks.BaseTIPLBlock.TIPLBlockFactory;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.tools.GrayAnalysis;
import tipl.tools.Neighbors;
import tipl.util.ArgumentList;
import tipl.util.ArgumentParser;
import tipl.util.ITIPLPluginIO;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;

/**
 * Run analysis on a thresheld phase
 * 
 * @author mader
 * 
 */
public class AnalyzePhase extends BaseTIPLBlock {
    public final IBlockImage[] inImages = new IBlockImage[]{
            new BlockImage("segmented", "segmented.tif", "Segmented image",
                    true),
            new BlockImage("mask", "mask.tif", "Mask Image", true)};
    public final IBlockImage[] outImages = new IBlockImage[]{
    		new BlockImage("labeled", "labeled.tif", "Labeled Image", false),
    		new BlockImage("density", "density.tif", "Density Image", false),
    		new BlockImage("neighbors", "neighbors.tif", "Neighbor Image", false)
    };
    public String prefix;
    public int minVoxCount;
	public ArgumentList.TypedPath phaseName;
	// public double sphKernelRadius;
	public boolean writeShapeTensor;
    protected ShapeAndNeighborAnalysis SNA = new ShapeAndNeighborAnalysis(new ArgumentList.TypedPath("NOT INITIALIZED"));
    ITIPLPluginIO CL = TIPLPluginManager.createFirstPluginIO("ComponentLabel");

    public AnalyzePhase() {
        super("AnalyzePhase");
        prefix = "";
    }
    /**
     * Just for subclasses
     * @param blockName
     * @param anything
     */
    protected AnalyzePhase(final String blockName,boolean anything) {
    	super(blockName);
    }

    public AnalyzePhase(final String inPrefix) {
        super("AnalyzePhase");
        prefix = inPrefix;
    }

    @Override
    protected IBlockImage[] bGetInputNames() {
        return inImages;
    }

	@Override
    protected IBlockImage[] bGetOutputNames() {
        return outImages;
    }
	
	
	protected TImg performCL() {
		 final TImgRO segmentAim = getInputFile("segmented");

	        // volume filter
	        CL.LoadImages(new TImgRO[]{segmentAim});

	        // myCL.useSphKernel(sphKernelRadius)
	        CL.execute(); // count only objects with more than 5 voxels minvoxel
	        TImgTools.RemoveTImgFromCache(getFileParameter("segmented"));
	        final TImg labImg=CL.ExportImages(segmentAim)[0];
	        SaveImage(labImg, "labeled");
	        return labImg;
	}
	
	protected boolean performShapeAnalysis(final TImg labImg) {
		final TImgRO maskImg = getInputFile("mask");
        final TImg[] outImages = SNA.execute(labImg, maskImg, writeShapeTensor);
        SaveImage(outImages[0], "density");
        SaveImage(outImages[1], "neighbors");
        return true;
	}
	
	@Override
    public boolean executeBlock() {
		final TImg labImg = performCL();
        return performShapeAnalysis(labImg);
    }

	@Override
    protected String getDescription() {
        return "Run component labeling, shape, density, and neigbhorhood analysis on a thresheld phase";
    }

	@Override
    public String getPrefix() {
        return prefix;
    }

	@Override
    public void setPrefix(String newPrefix) {
        prefix = newPrefix;

	}
	
    @Override
    public ArgumentParser setParameterBlock(final ArgumentParser p) {
    	minVoxCount = p.getOptionInt(prefix + "minvoxcount", 1, "Minimum voxel count");
        // sphKernelRadius=p.getOptionDouble("sphkernelradius",1.74,"Radius of spherical kernel to use for component labeling: vertex sharing is sqrt(3)*r, edge sharing is sqrt(2)*r,face sharing is 1*r ");
        writeShapeTensor = p.getOptionBoolean(prefix + "shapetensor", true, "Include Shape Tensor");
        phaseName = p.getOptionPath(prefix + "phase", "phase1", "Phase name");
        SNA = new ShapeAndNeighborAnalysis(phaseName);
        final ArgumentParser p2 = SNA.getNeighborPlugin().setParameter(p,
                prefix);
        return CL.setParameter(p2, prefix);
    }

    /**
     * A class for the shape and neighbor analysis based on two easily
     * overridden functions for generating the voronoi and neighborhood
     * calculating plugins
     *
     * @author mader
     */
    public static class ShapeAndNeighborAnalysis {
        public final boolean useGrownLabel = false;
        ITIPLPluginIO myNH = new Neighbors();
        public ShapeAndNeighborAnalysis(ArgumentList.TypedPath phName) {
        	seedCSVFile=phName.append("_1.csv");
        	densCSVFile=phName.append("_2.csv");
        	nhCSVFile=phName.append("_3.csv");
        	edgeCSVFile=phName.append("_edge.csv");
        	appendMode=false;
        	this.phName=phName;
        }
        
        public ShapeAndNeighborAnalysis(ArgumentList.TypedPath seedFile, ArgumentList.TypedPath phName) {
        	seedCSVFile=seedFile;
        	densCSVFile=phName.append("_2.csv");
        	nhCSVFile=phName.append("_3.csv");
        	edgeCSVFile=phName.append("_edge.csv");
        	appendMode=true;
        	this.phName=phName;
        }
        final protected ArgumentList.TypedPath phName;
        final protected ArgumentList.TypedPath seedCSVFile;
        final protected ArgumentList.TypedPath densCSVFile;
        final protected ArgumentList.TypedPath nhCSVFile;
        final protected ArgumentList.TypedPath edgeCSVFile;
        
        /**
         * appendMode means the file is appended rather than created (skip LacunaAnalysis Step)
         */
        final protected boolean appendMode;
        
        public TImg[] execute(final TImgRO labeledImage, final TImgRO maskImage, final boolean writeShapeTensor) {
            // now make the dilation
            final ITIPLPluginIO vorn = getGrowingPlugin(labeledImage, maskImage);
            vorn.execute();
            final TImg growOut = vorn.ExportImages(labeledImage)[0];
            final TImgRO cImg = (useGrownLabel) ? (growOut) : (labeledImage);
            
            if (!appendMode) {
            	GrayAnalysis.StartLacunaAnalysis(cImg, seedCSVFile, "Mask",writeShapeTensor);
            }
            
                    
            GrayAnalysis.AddDensityColumn(growOut, seedCSVFile, densCSVFile, 
            		((appendMode) ? phName+"_" : "")+"Density");
            final ITIPLPluginIO myNH = getNeighborPlugin();
            myNH.LoadImages(new TImgRO[]{growOut});
            myNH.execute();
            myNH.execute("WriteNeighborList", edgeCSVFile);
            final TImg NHimg = myNH.ExportImages(growOut)[0];
            GrayAnalysis.AddRegionColumn(growOut, NHimg, densCSVFile,nhCSVFile,
            		((appendMode) ? phName+"_" : "")+"Neighbors");
            return new TImg[]{growOut, NHimg};
        }
        
        /**
         * @param obj
         * @param mask
         * @return a plugin object which when executed fills the mask with the
         * obj (voronoi transform or something like that)
         */
        public ITIPLPluginIO getGrowingPlugin(final TImgRO obj, final TImgRO mask) {
            ITIPLPluginIO KV = TIPLPluginManager.createBestPluginIO("kVoronoi", new TImgRO[]{obj, mask});
            KV.LoadImages(new TImgRO[]{obj, mask});
            return KV;
        }

        /**
         * @return a plugin object for managing neighbors
         */
        public ITIPLPluginIO getNeighborPlugin() {
            return myNH;
        }
    }

	

}