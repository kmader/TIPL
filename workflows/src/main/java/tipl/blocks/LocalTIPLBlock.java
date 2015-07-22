/**
 *
 */
package tipl.blocks;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.tools.Resize;
import tipl.util.*;

import java.util.LinkedHashMap;

/**
 * The helper functions for locally running blocks Handles checking for files and determining ready
 * status Can be launched from either this script or the blockrunner tool
 *
 * @author mader
 */
public abstract class LocalTIPLBlock implements ITIPLBlock {

    public final static String kVer = "141017_005";


    /**
     * A subclass when only part of the images need to be read
     */
    static public class SubRegionIOHelper extends LocalIOHelper {
        /**
         * maximum number of slices to read in (-1 is unlimited)
         */
        protected int maxReadSlices = -1;

        protected int lastReadSlices = -2;
        protected int startReadSlices = -1;

        /**
         * only reads in a limited number of slices (enables quick and dirty script tests and easily
         * dividing data sets to speed up analysis)
         *
         * @param argument   name of commandline argument to read in
         * @param startSlice first slice to take from the image
         * @param endSlice   last slice to take
         * @return cropped (if needed) version of the file
         */
        private TImgRO getInputFileSliceRange(final String argument, int startSlice, int endSlice) {
            TImgRO fullImage = super.getInputFile(argument);
            Resize myResize = new Resize(fullImage);
            D3int outPos = fullImage.getPos();
            D3int outDim = fullImage.getDim();
            myResize.cutROI(new D3int(outPos.x, outPos.y, Math.max(outPos.x, startSlice)),
                    new D3int(outDim.x, outDim.y, Math.min(outDim.z - 1, endSlice - startSlice)));
            myResize.execute();
            return myResize.ExportImages(fullImage)[0];
        }

        public void setSliceRange(int startSlice, int finishSlice) {
            startReadSlices = startSlice;
            lastReadSlices = finishSlice;
        }

        /**
         * get the current range
         *
         * @return
         */
        public int[] getSliceRange() {
            return new int[]{startReadSlices, lastReadSlices};
        }

        @Override
        public TImgRO getInputFile(final String argument) {
            if (lastReadSlices >= startReadSlices)
                return getInputFileSliceRange(argument, startReadSlices, lastReadSlices);
            else return super.getInputFile(argument);
        }
    }


    /**
     * Images read from a linkedlist rather than IO
     */
    static public class CacheIOHelper extends LocalIOHelper {
        final protected LinkedHashMap<String, TImgRO> imageCache;

        public CacheIOHelper(LinkedHashMap<String, TImgRO> imageCache) {
            this.imageCache=imageCache;
        }

        @Override
        public void SaveImage(TImgRO imgObj, String nameArg) {
            imageCache.put(nameArg,imgObj);
        }

        @Override
        public boolean isReady(String prefix, IBlockInfo aboutMe) {
            boolean retValue = true;

            for (final IBlockImage cImage : aboutMe.getInputNames()) {
                if (cImage.isEssential()) {
                    final String carg = prefix + cImage.getName();
                    if (!imageCache.containsKey(carg)) {
                        System.out.println("Not ready for block " + toString()
                                + ", file:" + carg +
                                 " cannot be found");
                        retValue = false;
                    } else {
                        System.out.println("Not ready for block " + toString()
                                + ", argument:" + carg
                                + " cannot be found / loaded");
                        retValue = false;
                    }
                }
            }
            return retValue;
        }

        @Override
        public TImgRO getInputFile(String argument) {
            if (imageCache.containsKey(argument)) {
                return imageCache.get(argument);
            }
            else {
                throw new IllegalArgumentException(this+" does not have any image for " +
                        "key:"+argument);
            }
        }


        @Override
        public TypedPath getFileParameter(String argument) {
            return TIPLStorageManager.createVirtualPath(argument);
        }
    }

    static public class LocalIOHelper implements BlockIOHelper {
        protected boolean saveToCache = false;
        protected boolean readFromCache = true;

        protected LinkedHashMap<String, String> blockConnections = new LinkedHashMap<String,
                String>();
        final protected LinkedHashMap<String, TypedPath> ioParameters = new LinkedHashMap<String,
                TypedPath>();

        public static final boolean readImageDuringTry = false;

        /**
         * Attempts to load the aim file with the given name (usually tif stack) and returns whether
         * or not something has gone wrong during this loading
         *
         * @param filename Path and name of the file/directory to open
         */
        static public boolean tryOpenImagePath(final TypedPath filename) {

            TImg tempAim = null; // TImg (should be, but currently that eats way too much
            // computer time)
            if (filename.length() > 0) {
                System.out.println("Trying- to open ... " + filename);
            } else {
                System.out
                        .println("Filename is empty, assuming that it is not essential and " +
                                "proceeding carefully!! ... ");
                return true;
            }

            try {
                System.out.println("Trying-Image Found: " + filename + (readImageDuringTry ? " " +
                        "and will be open..." : " will be assumed to be ok!"));
                if (!readImageDuringTry) return true;

                tempAim = TImgTools.ReadTImg(filename); // ReadTImg (should be,
                // but currently that eats way too much computer time)
                System.out.println("Trying-Image Opened, checking dimensions: " + tempAim.getDim());
                if (tempAim.getDim().prod() < 1)
                    return false;
                return (tempAim.isGood());
            } catch (final Exception e) {
                tempAim = null;
                TIPLGlobal.runGC();
                return false;
            }

        }



        /**
         * Background saving makes things faster but can cause issues
         */
        final static protected boolean bgSave = false;

        /**
         * A save function so background saving can be controlled on a block level
         *
         * @param imgObj
         */
        public void SaveImage(TImgRO imgObj, String nameArg) {
            if (bgSave) {
                TImgTools.WriteBackground(imgObj, getFileParameter(nameArg));
            } else {
                TImgTools.WriteTImg(imgObj, getFileParameter(nameArg), saveToCache);
            }
        }

        @Override
        public boolean isReady(String prefix, IBlockInfo aboutMe) {
            boolean retValue = true;

            for (final IBlockImage cImage : aboutMe.getInputNames()) {
                if (cImage.isEssential()) {
                    final String carg = prefix + cImage.getName();

                    final TypedPath curFile = this.getFileParameter(carg);
                    if (!tryOpenImagePath(curFile)) {
                        System.out.println("Not ready for block " + toString()
                                + ", file:" + carg + "=" + curFile
                                + " cannot be found / loaded");
                        retValue = false;
                    } else {
                        System.out.println("Not ready for block " + toString()
                                + ", argument:" + carg
                                + " cannot be found / loaded");
                        retValue = false;
                    }
                }
            }
            return retValue;
        }

        @Override
        public ArgumentParser gatherIOArguments(ArgumentParser p, String prefix,
                                                IBlockInfo aboutMe) {
            // Process File Inputs
            for (final IBlockImage cImage : aboutMe.getInputNames()) {

                if (blockConnections.containsKey(cImage.getName()))
                    p.forceMatchingValues(blockConnections.get(cImage.getName()),
                            prefix + cImage.getName());
                // otherwise treat it like a normal argument
                final TypedPath oValue = p.getOptionPath(
                        prefix + cImage.getName(), cImage.getDefaultValue(),
                        cImage.getDesc()
                                + ((cImage.isEssential()) ? ", Needed"
                                : ", Optional"));
                ioParameters.put(cImage.getName(), oValue);
            }
            for (final IBlockImage cImage : aboutMe.getOutputNames()) {
                if (blockConnections.containsKey(cImage.getName()))
                    p.forceMatchingValues(blockConnections.get(cImage.getName()),
                            prefix + cImage.getName());

                // otherwise treat it like a normal argument
                final TypedPath oValue = p.getOptionPath(
                        prefix + cImage.getName(), cImage.getDefaultValue(),
                        cImage.getDesc()
                                + ((cImage.isEssential()) ? ", Needed"
                                : ", Optional"));
                ioParameters.put(cImage.getName(), oValue);
            }
            return p;
        }

        @Override
        public void connectInput(final String inputName,
                                 final ITIPLBlock outputBlock, final String outputName) {
            blockConnections.put(inputName, outputBlock.getPrefix() + outputName);
        }

        @Override
        @Deprecated
        public TypedPath getFileParameter(final String argument) {
            if (!ioParameters.containsKey(argument))
                throw new IllegalArgumentException("Block:"+this+" has no argument "+argument);
            return ioParameters.get(argument);
        }

        @Override
        public TImgRO getInputFile(final String argument) {
            if (getFileParameter(argument).length() < 1) {
                System.out.println("Block:" + this + " is missing file argument:" + argument);
                throw new IllegalArgumentException("Block:" + this + " is missing file argument:"
                        + argument);
            }
            return TImgTools.ReadTImg(getFileParameter(argument), readFromCache, saveToCache);
        }

    }


    protected ITIPLBlock[] prereqBlocks = new ITIPLBlock[]{};
    protected ArgumentParser args = TIPLGlobal.activeParser(new String[]{});
    protected String blockName = "";
    protected boolean skipBlock = true;
    protected boolean saveToCache = false;
    protected boolean readFromCache = true;

    protected static void checkHelp(final ArgumentParser p) {
        if (p.hasOption("?")) {
            System.out.println(" BlockRunner");
            System.out.println(" Runs TIPLBlocks and parse arguments");
            System.out.println(" Arguments::");
            System.out.println(" ");
            System.out.println(p.getHelp());
            System.exit(0);
        }
        p.checkForInvalid();
    }

    /**
     * A runnable block which takes the block itself as an argument so it can read parameters and
     * other information from the block while running It also internally reads parameters so clumsy
     * hacks need not always be made
     *
     * @author mader
     */
    public static interface BlockRunnable extends ArgumentParser.IsetParameter {
        /**
         * the run command itself
         *
         * @param inBlockID the block being run
         */
        public void run(BaseTIPLBlock inBlockID);

    }


    /**
     * Just like runnable but allows an addition setup step
     *
     * @author mader
     */
    public static interface ConfigurableBlockRunnable extends BlockRunnable {
        /**
         * in case any setup needs to be run before the execution starts useful for reading in
         * parameters and other assorted tasks
         *
         * @param inBlockID the block being run (typical use is to then grab the parameter list)
         */
        public void setup(BaseTIPLBlock inBlockID);
    }

    public static void main(final String[] args) {
        LocalTIPLBlock.main(args);
    }

    final protected BlockIOHelper ioHelper;

    public LocalTIPLBlock(final BlockIOHelper helperTools, final String inName) {
        ioHelper = helperTools;
        blockName = inName;
    }

    public LocalTIPLBlock(final BlockIOHelper helperTools, final String inName,
                          final ITIPLBlock[] earlierBlocks) {
        ioHelper = helperTools;
        blockName = inName;
        prereqBlocks = earlierBlocks;
    }



    @Override
    public double memoryFactor() {
        System.err.println("Defaulting to " + this + " probably not what you wanted");
        return 2;
    }

    @Override
    public long neededMemory() {
        System.err.println("Defaulting to " + this + " probably not what you wanted");
        return 22 * 1024; //22 gigabytes
    }

    protected abstract IBlockImage[] bGetInputNames();

    protected abstract IBlockImage[] bGetOutputNames();

    /*
     * (non-Javadoc)
     *
     * @see tipl.util.TIPLBlock#execute()
     */
    @Override
    final public boolean execute() {
        if (!skipBlock) {
            System.out.println(toString() + (isReady() ? " is ready!" : " is not ready!!!!"));
            return executeBlock();
        } else {
            System.out.println(toString() + " skipped!");
        }
        return true;
    }

    /**
     * Local block code for running
     *
     * @return success
     */
    public abstract boolean executeBlock();

    public boolean isReady() {
        boolean retValue = true;
        for (final ITIPLBlock cblock : prereqBlocks)
            if (!cblock.isComplete()) {
                System.out.println("Not ready for block " + toString()
                        + ", block:" + cblock + " has not completed");
                retValue = false;
            }
        return (retValue && ioHelper.isReady(getPrefix(), getInfo()));
    }

    protected abstract String getDescription();

    @Override
    public IBlockInfo getInfo() {
        return new IBlockInfo() {
            @Override
            public String getDesc() {
                return getDescription();
            }

            @Override
            public IBlockImage[] getInputNames() {
                return bGetInputNames();
            }

            @Override
            public IBlockImage[] getOutputNames() {
                return bGetOutputNames();
            }

        };
    }

    @Override
    public boolean isComplete() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    final public ArgumentParser setParameter(ArgumentParser p) { // prevent
        // overrriding
        // p.blockOverwrite();
        skipBlock = p.getOptionBoolean(getPrefix() + "skipblock",
                "Skip this block");
        // Process File-related inputs
        p = ioHelper.gatherIOArguments(p, getPrefix(), getInfo());

        // Process Standard Inputs
        p = setParameterBlock(p);
        args = p;
        return p;
    }

    /*
     * Note: setParameter sets the args to the result of setParameterBlock
     * (non-Javadoc)
     *
     * @see tipl.util.TIPLBlock#setParameters(tipl.util.ArgumentParser)
     */
    public abstract ArgumentParser setParameterBlock(ArgumentParser p);

    @Override
    public String toString() {
        return "BK:" + blockName;
    }

    @Override
    public TImgRO getInputFile(String argument) {
        return ioHelper.getInputFile(argument);
    }

    @Override
    public void SaveImage(TImgRO imgObj, String nameArg) {
        ioHelper.SaveImage(imgObj, nameArg);
    }

    @Deprecated
    public TypedPath getFileParameter(final String argument) {
        return ioHelper.getFileParameter(argument);
    }

    @Override
    public void connectInput(final String inputName,
                             final ITIPLBlock outputBlock, final String outputName) {
        ioHelper.connectInput(inputName, outputBlock, outputName);
    }

}
