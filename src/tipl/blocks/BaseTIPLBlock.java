/**
 * 
 */
package tipl.blocks;

import java.util.LinkedHashMap;

import tipl.formats.TImg;
import tipl.util.ArgumentParser;
import tipl.util.SGEJob;
import tipl.util.TImgTools;

/**
 * A basic concrete implementation of TIPLBlock with the helper functions
 * 
 * @author mader
 * 
 */
public abstract class BaseTIPLBlock implements TIPLBlock {
	protected TIPLBlock[] prereqBlocks = new TIPLBlock[] {};
	// protected String[] neededPathArgs=new String[]{};
	protected ArgumentParser args = new ArgumentParser(new String[] {});
	protected String blockName = "";
	protected boolean skipBlock = true;
	protected LinkedHashMap<String, String> blockConnections = new LinkedHashMap<String, String>();
	final protected LinkedHashMap<String, String> ioParameters = new LinkedHashMap<String, String>();
	public final static String kVer = "130819_003";

	protected static void checkHelp(ArgumentParser p) {
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
	 * Create a block from a few parameters and a runnable
	 * 
	 * @param name
	 * @param prefix
	 * @param earlierPathArgs
	 * @param outArgs
	 * @param job
	 * @param parFunc
	 * @return TIPLBlock to be run later
	 */
	public static TIPLBlock InlineBlock(String name, String prefix,
			final String[] earlierPathArgs, final String[] outArgs,
			final Runnable job, final ArgumentParser.IsetParameter parFunc) {
		return InlineBlock(name, prefix, new TIPLBlock[] {}, earlierPathArgs,
				outArgs, job, parFunc);
	}

	/**
	 * Create a block inline by defining the code (job : runnable) and the
	 * inputs (parFunc) before hand
	 * 
	 * @param name
	 * @param prefix
	 * @param earlierBlocks
	 * @param earlierPathArgs
	 *            needed input arguments
	 * @param outputArgs
	 *            needed output arguments
	 * @param job
	 * @param parFunc
	 * @return TIPLBlock to be run later
	 */
	public static TIPLBlock InlineBlock(String name, final String prefix,
			final TIPLBlock[] earlierBlocks, final String[] earlierPathArgs,
			final String[] outputArgs, final Runnable job,
			final ArgumentParser.IsetParameter parFunc) {
		final TIPLBlock cBlock = new BaseTIPLBlock(name, earlierBlocks) {
			@Override
			protected IBlockImage[] bGetInputNames() {
				final IBlockImage[] inNames = new BlockImage[earlierPathArgs.length];
				for (int i = 0; i <= earlierPathArgs.length; i++) {
					inNames[i] = new BlockImage(earlierPathArgs[i],
							"No description provided", true);
				}
				return inNames;
			}

			@Override
			protected IBlockImage[] bGetOutputNames() {
				final IBlockImage[] outNames = new BlockImage[outputArgs.length];
				for (int i = 0; i <= outputArgs.length; i++) {
					outNames[i] = new BlockImage(outputArgs[i],
							"No description provided", true);
				}
				return outNames;
			}

			@Override
			public boolean executeBlock() {
				if (!isReady()) {
					System.out.println("Block is not ready!");
					return false;
				}
				try {
					job.run();
					return true;
				} catch (final Exception e) {
					System.out.println("Execution of block has failed!");
					e.printStackTrace();
				}
				return true;
			}

			@Override
			public String getDescription() {
				return "InlineBlocks normally don't get fancy names";
			}

			@Override
			public String getPrefix() {
				return prefix;
			}

			@Override
			public ArgumentParser setParameterBlock(ArgumentParser p) {
				return parFunc.setParameter(p, prefix);
			}

		};
		return cBlock;
	}

	public static void main(String[] args) {

		System.out.println("BlockRunner v" + kVer);
		System.out.println("Runs a block by its name");
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		ArgumentParser p = new ArgumentParser(args);
		final String blockname = p.getOptionString("blockname", "",
				"Class name of the block to run");
		/*
		 * if (p.getOptionBoolean("stringconst",
		 * "Use the string array constructor for the TIPLBlock")) { String[]
		 * requiredfiles = p.getOptionString("stringarray","",
		 * "String array (& delimited) to use in constructor").split("&");
		 * forConstructor=String[].class; toConstructor=new
		 * Object[]{requiredfiles}; }
		 */
		// black magic
		if (blockname.length() > 0) {
			TIPLBlock cBlock = null;
			try {
				// cBlock=(TIPLBlock)
				// Class.forName(blockname).getConstructor(forConstructor).newInstance(toConstructor);
				cBlock = (TIPLBlock) Class.forName(blockname).newInstance();
			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Block Class:" + blockname
						+ " was not found, does it exist?");
			} catch (final Exception e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Block Class:" + blockname
						+ " could not be created, sorry!");
			}
			p = cBlock.setParameter(p);
			// code to enable running as a job
			final boolean runAsJob = p
					.getOptionBoolean("sge:runasjob",
							"Run this script as an SGE job (adds additional settings to this task");
			SGEJob jobToRun = null;
			if (runAsJob)
				jobToRun = SGEJob.runAsJob("tipl.blocks.BaseTIPLBlock", p,
						"sge:");

			checkHelp(p);

			final boolean isBlockReady = cBlock.isReady();
			if (isBlockReady) {
				if (runAsJob) {
					jobToRun.submit();
				} else {
					cBlock.execute();
				}

			}

		} else
			checkHelp(p);

	}

	/**
	 * Attempts to load the aim file with the given name (usually tif stack) and
	 * returns whether or not something has gone wrong during this loading
	 * 
	 * @param filename
	 *            Path and name of the file/directory to open
	 */
	public static boolean tryOpenImagePath(String filename) {

		TImg tempAim = null;
		if (filename.length() > 0) {
			System.out.println("Trying to open ... " + filename);
		} else {
			System.out
					.println("Filename is empty, assuming that it is not essential and proceeding carefully!! ... ");
			return true;
		}

		try {
			tempAim = TImgTools.ReadTImg(filename);
			if (tempAim.getDim().prod() < 1)
				return false;
			return (tempAim.isGood());
		} catch (final Exception e) {
			tempAim = null;
			System.gc();
			return false;
		}

	}

	public BaseTIPLBlock(String inName) {
		blockName = inName;
	}

	public BaseTIPLBlock(String inName, TIPLBlock[] earlierBlocks) {
		blockName = inName;
		prereqBlocks = earlierBlocks;
	}

	protected abstract IBlockImage[] bGetInputNames();

	protected abstract IBlockImage[] bGetOutputNames();

	@Override
	public void connectInput(String inputName, TIPLBlock outputBlock,
			String outputName) {
		blockConnections.put(inputName, outputBlock.getPrefix() + outputName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.util.TIPLBlock#execute()
	 */
	@Override
	final public boolean execute() {
		if (!skipBlock)
			return executeBlock();
		else
			System.out.println(toString() + " skipped!");
		return true;
	}

	/**
	 * Local block code for running
	 * 
	 * @return success
	 */
	public abstract boolean executeBlock();

	protected abstract String getDescription();

	@Override
	public String getFileParameter(String argument) {
		return ioParameters.get(argument);
	}

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
	public abstract String getPrefix();

	@Override
	public boolean isComplete() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.util.TIPLBlock#isReady()
	 */
	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		boolean retValue = true;
		for (final TIPLBlock cblock : prereqBlocks)
			if (!cblock.isComplete()) {
				System.out.println("Not ready for block " + toString()
						+ ", block:" + cblock + " has not completed");
				retValue = false;
			}
		for (final IBlockImage cImage : getInfo().getInputNames()) {
			if (cImage.isEssential()) {
				final String carg = getPrefix() + cImage.getName(); // create
																	// argument
																	// from info
				if (args.hasOption(carg)) {
					final String curFile = args.getOptionAsString(carg);
					if (!tryOpenImagePath(curFile)) {
						System.out.println("Not ready for block " + toString()
								+ ", file:" + carg + "=" + curFile
								+ " cannot be found / loaded");
						retValue = false;
					}
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
	final public ArgumentParser setParameter(ArgumentParser p) { // prevent
																	// overrriding
		// p.blockOverwrite();
		skipBlock = p.getOptionBoolean(getPrefix() + "skipblock",
				"Skip this block");
		// Process File Inputs
		final IBlockInfo aboutMe = getInfo();
		for (final IBlockImage cImage : aboutMe.getInputNames()) {

			if (blockConnections.containsKey(cImage.getName()))
				p.forceMatchingValues(blockConnections.get(cImage.getName()),
						getPrefix() + cImage.getName());
			// otherwise treat it like a normal argument
			final String oValue = p.getOptionPath(
					getPrefix() + cImage.getName(), cImage.getDefaultValue(),
					cImage.getDesc()
							+ ((cImage.isEssential()) ? ", Needed"
									: ", Optional"));
			ioParameters.put(cImage.getName(), oValue);
		}
		for (final IBlockImage cImage : aboutMe.getOutputNames()) {
			if (blockConnections.containsKey(cImage.getName()))
				p.forceMatchingValues(blockConnections.get(cImage.getName()),
						getPrefix() + cImage.getName());

			// otherwise treat it like a normal argument
			final String oValue = p.getOptionPath(
					getPrefix() + cImage.getName(), cImage.getDefaultValue(),
					cImage.getDesc()
							+ ((cImage.isEssential()) ? ", Needed"
									: ", Optional"));
			ioParameters.put(cImage.getName(), oValue);
		}
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

}
