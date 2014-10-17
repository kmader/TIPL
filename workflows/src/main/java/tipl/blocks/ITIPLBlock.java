/**
 * 
 */
package tipl.blocks;

import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;
import tipl.util.TypedPath;

/**
 * TIPLBlock allows for processing segments to be enclosed into blocks which can
 * be run independently the blocks can have prerequisites which will be checked
 * before they are run
 * 
 * @author mader
 * 
 */
public interface ITIPLBlock {

	
	static public class BlockImage implements IBlockImage {
		protected final String name;
		protected final String desc;
		protected final boolean isess;
		protected final TypedPath dname;

		public BlockImage(final String iName, final String iDesc,
				final boolean essential) {
			name = iName;
			desc = iDesc;
			isess = essential;
			dname = new TypedPath("");
		}

		public BlockImage(final String iName, final TypedPath defName,
				final String iDesc, final boolean essential) {
			name = iName;
			desc = iDesc;
			isess = essential;
			dname = defName;
		}
		@Deprecated
		public BlockImage(final String iName, final String defName,
				final String iDesc, final boolean essential) {
			name = iName;
			desc = iDesc;
			isess = essential;
			dname = new TypedPath(defName);
		}
		

		@Override
		public TypedPath getDefaultValue() {
			return dname;
		}

		@Override
		public String getDesc() {
			return desc;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isEssential() {
			return isess;
		}

	}

	static public interface IBlockImage {
		/**
		 * the default filename to use (can be blank)
		 * 
		 * @return default filename or path
		 */
		public TypedPath getDefaultValue();

		/**
		 * simple description of the image
		 */
		public String getDesc();

		/**
		 * the name of the image (internal)
		 */
		public String getName();

		/**
		 * is the image required to start the program or not
		 * 
		 * @return needed for isReady or isComplete?
		 */
		public boolean isEssential();
	}

	/**
	 * The IBlockInfo interface is used to supply inputs and outputs to Blocks /
	 * functions
	 * 
	 * @author mader
	 * 
	 */

	static public interface IBlockInfo {
		public String getDesc();

		public IBlockImage[] getInputNames();

		public IBlockImage[] getOutputNames();
	}

	/**
	 * Connects the input of this block to the output of another block (output
	 * of other block overrides this blocks name)
	 * 
	 * @param inputName
	 *            input field from this block to overwrite
	 * @param outputBlock
	 *            block to connect to
	 * @param outputName
	 *            the name of the field being connected
	 */
	public void connectInput(String inputName, ITIPLBlock outputBlock,
			String outputName);

	/**
	 * run block
	 * 
	 * @return success
	 */
	public boolean execute();



	/**
	 * get an input file (handles loading and everything in a consistent manner and returns null if the image is empty 
	 * @param argument image argument name
	 * @return
	 */
	public TImgRO getInputFile(String argument);

	public IBlockInfo getInfo();

	/**
	 * Return the prefix to use for input arguments
	 * 
	 * @return block prefix (for commandline arguments)
	 */
	public String getPrefix();
	/**
	 * Set the prefix for the block
	 * @param newPrefix the new prefix
	 */
	public void setPrefix(String newPrefix);

	
	/**
	 * has the block completed or are all of the files, etc present which
	 * indicate completion
	 */
	public boolean isComplete();

	/**
	 * Is the block ready to be run (all prerequisites met)
	 */
	public boolean isReady();

	/** 
	 * how many times the first input image dimensions are required
	 * @return
	 */
	public double memoryFactor();
	/**
	 * how much memory does this block require (total in megabytes)
	 * @return
	 */
	public long neededMemory();
	/**
	 * Inputs can be given to the block through the setParameters command using
	 * the ArgumentParser class
	 *
	 * @param p
	 *            input arguments
	 * @return updated arguments (in case they are replaced)
	 */
	public ArgumentParser setParameter(ArgumentParser p);

	/**
	 * Provide the name and some information about the block
	 * 
	 * @return block representation
	 */
	@Override
	public String toString();

    /**
     * A save function so background saving can be controlled on a block level
     * @param imgObj
     */
    public void SaveImage(TImgRO imgObj,String nameArg);

    /**
     * Get the file path of the current argument, still needed for writing csv files
     * @param argument
     * @return
     */
    @Deprecated
    public TypedPath getFileParameter(final String argument);

    /**
     * A class to handle IO for the blocks (either locally, through Hadoop or otherwise)
     * basically it makes it more straightforward to pass images in as objects instead of paths
     */
    static public interface BlockIOHelper {
        /**
         * A save function so background saving can be controlled on a block level
         * @param imgObj
         */
        public void SaveImage(TImgRO imgObj,String nameArg);

        /**
         * Is the IO ready (usually directly linked to the block being ready, but not always
         * @return do all files that are needed exist and are readable
         */
        public boolean isReady(final String prefix, IBlockInfo aboutMe);


        public TImgRO getInputFile(final String argument);

        /**
         * The arguments for the IO helper specifically
         * @param p the current argument list
         * @param prefix the prefix to use for the current extension
         * @param aboutMe the info about the current block
         * @return
         */
        public ArgumentParser gatherIOArguments(ArgumentParser p, String prefix, IBlockInfo aboutMe);

        public void connectInput(final String inputName,
                                 final ITIPLBlock outputBlock, final String outputName);

        @Deprecated
        public TypedPath getFileParameter(final String argument);


    }
}
