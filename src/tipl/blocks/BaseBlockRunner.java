/**
 * 
 */
package tipl.blocks;

import java.util.LinkedList;
import java.util.List;

import tipl.util.ArgumentParser;
import tipl.util.SGEJob;
import tipl.util.TIPLGlobal;

/**
 * @author mader
 *
 */
public class BaseBlockRunner implements IBlockRunner {
	final protected LinkedList<ITIPLBlock> blockList;
	/**
	 * 
	 */
	public BaseBlockRunner() {
		blockList=new LinkedList<ITIPLBlock>();
	}

	/* (non-Javadoc)
	 * @see tipl.blocks.IBlockRunner#add(tipl.blocks.ITIPLBlock)
	 */
	@Override
	public void add(ITIPLBlock newBlock) {
		blockList.add(newBlock);
	}

	/* (non-Javadoc)
	 * @see tipl.blocks.IBlockRunner#execute()
	 */
	@Override
	public void execute() {
		if (blockList.size()>0) {
			ITIPLBlock cBlock=blockList.pop();
			cBlock.execute();
			System.gc();	
			execute();
		}
	}

	@Override
	public void setSliceRange(int startSlice,int finishSlice) {
		for(ITIPLBlock cBlock : blockList) cBlock.setSliceRange(startSlice, finishSlice);
	}
	@Override
	public String toString() {
		String outStr="";
		for(ITIPLBlock cBlock : blockList) outStr+=cBlock+",";
		return outStr;
	}
	/* (non-Javadoc)
	 * @see tipl.blocks.IBlockRunner#setParameter(tipl.util.ArgumentParser)
	 */
	@Override
	public ArgumentParser setParameter(ArgumentParser p) {
		// TODO Auto-generated method stub
		ArgumentParser s=p;
		for(ITIPLBlock cBlock : blockList) s=cBlock.setParameter(s);
		return s;
	}
	public final static String kVer="131107_01";
	protected static void checkHelp(final ArgumentParser p) {
		if (p.hasOption("?")) {
			System.out.println(" BaseBlockRunner");
			System.out.println(" Runs multiple TIPLBlocks and parse arguments");
			System.out.println(" Arguments::");
			System.out.println(" ");
			System.out.println(p.getHelp());
			System.exit(0);
		}
		p.checkForInvalid();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("BaseBlockRunner v" + kVer);
		System.out.println("Runs a series of blocks by their name");
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		
		ArgumentParser p = TIPLGlobal.activeParser(args);
		final String blocknames = p.getOptionString("blocknames", "",
				"Class names of the block to run");
		// black magic
		
		if (blocknames.length() > 0) {
			IBlockRunner cr=new BaseBlockRunner(); 
			int blockIndex=1;
			for(String blockname : blocknames.split(",")) {
				
				ITIPLBlock cBlock = null;
				try {
					cBlock = (ITIPLBlock) Class.forName(blockname).newInstance();
					cBlock.setPrefix(cBlock.getClass().getSimpleName()+blockIndex+":");
					cr.add(cBlock);
				} catch (final ClassNotFoundException e) {
					e.printStackTrace();
					throw new IllegalArgumentException("Block Class:" + blockname
							+ " was not found, does it exist?");
				} catch (final Exception e) {
					e.printStackTrace();
					throw new IllegalArgumentException("Block Class:" + blockname
							+ " could not be created, sorry!");
				}
				blockIndex++;
			}
			p = cr.setParameter(p);
			// code to enable running as a job
			final boolean runAsJob = p
					.getOptionBoolean("sge:runasjob",
							"Run this script as an SGE job (adds additional settings to this task");
			SGEJob jobToRun = null;
			if (runAsJob)
				jobToRun = SGEJob.runAsJob(BaseBlockRunner.class.getName(), p,
						"sge:");

			checkHelp(p);
			p.checkForInvalid();
			
				if (runAsJob) {
					jobToRun.submit();
				} else {
					cr.execute();
				}


		} else
			checkHelp(p);
	}

}
