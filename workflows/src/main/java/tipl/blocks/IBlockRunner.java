/**
 *
 */
package tipl.blocks;

/**
 * TIPLBlock allows for processing segments to be enclosed into blocks which can be run
 * independently the blocks can have prerequisites which will be checked before they are run. It can
 * also be exported as a block so it can be connected to other serial pathways
 *
 * @author mader
 */
public interface IBlockRunner extends ITIPLBlock {
    /**
     * Add a new block to the runner list
     *
     * @param newBlock
     */
    public void add(ITIPLBlock newBlock);

}
