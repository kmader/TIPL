/**
 * 
 */
package tipl.blocks;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import tipl.formats.TImgRO;
import tipl.util.ArgumentDialog;
import tipl.util.ArgumentList;
import tipl.util.ArgumentParser;
import tipl.util.SGEJob;
import tipl.util.TIPLGlobal;

/**
 * Standard base block runner that runs the separate blocks in a serial fashion
 * @author mader
 *
 */
public class BaseBlockRunner implements IBlockRunner,ITIPLBlock {
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
	public boolean execute() {
		boolean success=true;
		while (blockList.size()>0) {
			TIPLGlobal.runGC();
			success&=blockList.pop().execute();	
		}
		return success;
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
	public ArgumentParser setParameter(final ArgumentParser p) {
		final boolean withGui = p.getOptionBoolean("gui",false,"Show a GUI for parameter adjustment");
		ArgumentParser s=p;
		if (withGui) {
			s = ArgumentDialog.GUIBlock(this,p.subArguments("gui",true));
		} 
		for(ITIPLBlock cBlock : blockList) s=cBlock.setParameter(s);

		return s;
	}
	@Override
	public void connectInput(String inputName, ITIPLBlock outputBlock,
			String outputName) {
		// TODO Auto-generated method stub

	}

	@Override
	public ArgumentList.TypedPath getFileParameter(String argument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TImgRO getInputFile(String argument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBlockInfo getInfo() {
		final List<IBlockImage> inputs=new LinkedList<IBlockImage>();
		final List<IBlockImage> outputs=new LinkedList<IBlockImage>();
		String totDesc="";
		for(ITIPLBlock cBlock : blockList) {
			IBlockInfo cInfo=cBlock.getInfo();
			totDesc+=cInfo.getDesc();
			inputs.addAll(Arrays.asList(cInfo.getInputNames()));
			outputs.addAll(Arrays.asList(cInfo.getInputNames()));
		}
		final String finDesc=totDesc;
		return new IBlockInfo() {
			@Override
			public String getDesc() {
				return finDesc;
			}

			@Override
			public IBlockImage[] getInputNames() {
				return inputs.toArray(new IBlockImage[] {});
			}

			@Override
			public IBlockImage[] getOutputNames() {
				return outputs.toArray(new IBlockImage[] {});
			}

		};
	}
	protected String prefix="";
	@Override
	public String getPrefix() {
		// TODO Auto-generated method stub
		return prefix;
	}

	@Override
	public void setPrefix(String newPrefix) {
		prefix=newPrefix;
	}
	@Override
	public double memoryFactor() {
		System.out.println("Assuming the blockrunner requires just the maximum of its parts (probably not true)");
		double maxFactor=0;
		for(ITIPLBlock cBlock : blockList) if (cBlock.memoryFactor()>maxFactor) maxFactor=cBlock.memoryFactor();
		return maxFactor;
	}
	@Override
	public long neededMemory() {
		System.out.println("Assuming the blockrunner requires just the maximum of its parts (probably not true)");
		long cneededMemory=0;
		for(ITIPLBlock cBlock : blockList) if (cBlock.neededMemory()>cneededMemory) cneededMemory=cBlock.neededMemory();
		return cneededMemory;
	}

	@Override
	public boolean isComplete() {
		boolean allComplete=true;
		for(ITIPLBlock cBlock : blockList) allComplete&=cBlock.isComplete();
		return allComplete;
	}

	@Override
	public boolean isReady() {
		if (blockList.size()<1) return false;		
		return blockList.getFirst().isReady();
	}

	public final static String kVer="140821_02";
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
				"Class names of the blocks to run");
		final boolean simpleNames = p.getOptionBoolean("simplenames", false, "Use simple names (b1 instead of thresholdblock1");
		if (blocknames.length() > 0) {
			IBlockRunner cr=new BaseBlockRunner(); 
			int blockIndex=1;
			for(String iblockname : blocknames.split(",")) {
				String blockname = iblockname;
				try {
					Object junk = (ITIPLBlock) Class.forName(blockname).newInstance();
				} catch (Exception e) {
					// try adding the right prefix
					blockname = "tipl.blocks."+iblockname;
				}
				ITIPLBlock cBlock = null;
				try {
					cBlock = (ITIPLBlock) Class.forName(blockname).newInstance();
					String blockClassName = cBlock.getClass().getSimpleName();
					if(simpleNames) {
						cBlock.setPrefix(blockClassName.replaceAll("[^A-Z]","")+"B"+blockIndex+":");
					} else {
						cBlock.setPrefix(blockClassName+blockIndex+":");
					}
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
			
			p = cr.setParameter(p.subArguments("blocknames", true).subArguments("simplenames",true));
			
			// code to enable running as a job
			final boolean runAsJob = p
					.getOptionBoolean("sge:runasjob",
							"Run this script as an SGE job (adds additional settings to this task");
			SGEJob jobToRun = null;
			if (runAsJob)
				jobToRun = SGEJob.runAsJob(BaseBlockRunner.class.getName(), p.subArguments("gui",true),
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
