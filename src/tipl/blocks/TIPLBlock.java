/**
 * 
 */
package tipl.blocks;

import tipl.util.ArgumentParser;

/**
 * TIPLBlock allows for processing segments to be enclosed into blocks which can
 * be run independently the blocks can have prerequisites which will be checked
 * before they are run
 * 
 * @author mader
 * 
 */
public interface TIPLBlock {

	public class BlockImage implements IBlockImage {
		protected final String name;
		protected final String desc;
		protected final boolean isess;
		protected final String dname;

		public BlockImage(String iName, String iDesc, boolean essential) {
			name = iName;
			desc = iDesc;
			isess = essential;
			dname = "";
		}

		public BlockImage(String iName, String defName, String iDesc,
				boolean essential) {
			name = iName;
			desc = iDesc;
			isess = essential;
			dname = defName;
		}

		@Override
		public String getDefaultValue() {
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

	public interface IBlockImage {
		/**
		 * the default filename to use (can be blank)
		 * 
		 * @return default filename or path
		 */
		public String getDefaultValue();

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

	public interface IBlockInfo {
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
	public void connectInput(String inputName, TIPLBlock outputBlock,
			String outputName);

	/**
	 * run block
	 * 
	 * @return success
	 */
	public boolean execute();

	/**
	 * Returns the value for a file contained in the getInputNames or
	 * getOutputNames list
	 * 
	 * @param argument
	 * @return file/path name
	 */
	public String getFileParameter(String argument);

	public IBlockInfo getInfo();

	/**
	 * Return the prefix to use for input arguments
	 * 
	 * @return block prefix (for commandline arguments)
	 */
	public String getPrefix();

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
}
