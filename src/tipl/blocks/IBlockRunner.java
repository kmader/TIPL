/**
 * 
 */
package tipl.blocks;

import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;

/**
 * TIPLBlock allows for processing segments to be enclosed into blocks which can
 * be run independently the blocks can have prerequisites which will be checked
 * before they are run
 * 
 * @author mader
 * 
 */
public interface IBlockRunner {
	/**
	 * Add a new block to the runner list
	 * @param newBlock
	 */
	public void add(ITIPLBlock newBlock);
	/**
	 * Set the maximum number of slices to read in when using the get input file command
	 * @param maxNumberOfSlices
	 */
	public void setSliceRange(int startSlice,int finishSlice);
	/**
	 * run the given chain of blocks
	 */
	public void execute();
	/**
	 * Inputs can be given to all of the blocks through the setParameters command using
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
