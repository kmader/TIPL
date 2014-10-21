/**
 *
 */
package tipl.blocks;

import org.reflections.Reflections;
import org.scijava.annotations.Index;
import org.scijava.annotations.IndexItem;
import org.scijava.annotations.Indexable;
import tipl.util.ArgumentDialog;
import tipl.util.ArgumentParser;
import tipl.util.SGEJob;
import tipl.util.TIPLGlobal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A basic concrete implementation of TIPLBlock with the helper functions Handles checking for files
 * and determining ready status Can be launched from either this script or the blockrunner tool
 *
 * @author mader
 */
public abstract class BaseTIPLBlock extends LocalTIPLBlock {

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Indexable
    public static @interface BlockIdentity {
        String blockName();

        String desc() default "";

        String[] inputNames();

        String[] outputNames();

    }


    /**
     * The static method to create a new TIPLBlock
     *
     * @author mader
     */
    public static abstract interface TIPLBlockFactory {
        public ITIPLBlock get();
    }


    public static abstract class BlockMaker implements TIPLBlockFactory {
        protected abstract ITIPLBlock make() throws InstantiationException, IllegalAccessException;

        public ITIPLBlock get() {
            try {
                return make();
            } catch (InstantiationException e) {
                e.printStackTrace();
                throw new IllegalArgumentException(e + " cannot be initiated");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new IllegalArgumentException(e + " cannot be accessed");
            }
        }
    }

    /**
     * Gets the list of all the blocks available in the tipl library
     *
     * @return a dictionary of all blocks
     */
    public static Map<String, BlockMaker> getBlockList() {
        Reflections reflections = new Reflections("tipl.blocks");
        Set<Class<? extends ITIPLBlock>> classes = reflections.getSubTypesOf(ITIPLBlock.class);
        HashMap<String, BlockMaker> blocks = new HashMap<String, BlockMaker>();
        for (final Class<? extends ITIPLBlock> curClass : classes) {
            if (TIPLGlobal.getDebug())
                System.out.println("tipl:Loading Class:" + curClass.getName());
            blocks.put(curClass.getCanonicalName(),
                    new BlockMaker() {
                        @Override
                        protected ITIPLBlock make()
                                throws InstantiationException,
                                IllegalAccessException {
                            return curClass.newInstance();
                        }
                    });
        }
        return blocks;
    }

    /**
     * Get a list of all the block factories that exist
     *
     * @return
     * @throws InstantiationException
     */
    public static HashMap<BlockIdentity, TIPLBlockFactory> getAllBlockFactories()
            throws InstantiationException {

        final HashMap<BlockIdentity, TIPLBlockFactory> current = new HashMap<BlockIdentity,
                TIPLBlockFactory>();

        for (Iterator<IndexItem<BlockIdentity>> cIter = Index.load(BlockIdentity.class).iterator
                (); cIter.hasNext(); ) {
            final IndexItem<BlockIdentity> item = cIter.next();

            final BlockIdentity bName = item.annotation();

            try {

                final TIPLBlockFactory dBlock = (TIPLBlockFactory) Class.forName(item.className()
                ).newInstance();
                System.out.println(bName + " loaded as: " + dBlock);
                current.put(bName, dBlock);
                System.out.println(item.annotation().blockName() + " loaded as: " + dBlock);
            } catch (InstantiationException e) {
                System.err.println(BaseTIPLBlock.class.getSimpleName() + ": " + bName.blockName()
                        + " could not be loaded or instantiated by plugin manager!\t" + e);
                if (TIPLGlobal.getDebug()) e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.err.println(BaseTIPLBlock.class.getSimpleName() + ": " + bName.blockName()
                        + " could not be found by plugin manager!\t" + e);
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                System.err.println(BaseTIPLBlock.class.getSimpleName() + ": " + bName.blockName()
                        + " was accessed illegally by plugin manager!\t" + e);
                e.printStackTrace();
            }
        }
        return current;
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

    /**
     * Create a block from a few parameters and a runnable
     *
     * @param name
     * @param prefix
     * @param earlierPathArgs
     * @param outArgs
     * @param job             the command to actually be run (type blockrunnable or
     *                        configurableblockrunnable)
     * @return TIPLBlock to be run later
     */
    public static ITIPLBlock InlineBlock(final String name, final String prefix,
                                         final String[] earlierPathArgs, final String[] outArgs,
                                         final BlockRunnable job) {
        return InlineBlock(name, prefix, new ITIPLBlock[]{}, earlierPathArgs,
                outArgs, job);
    }

    /**
     * Create a block inline by defining the code (job : runnable) and the inputs (parFunc) before
     * hand
     *
     * @param name
     * @param prefix
     * @param earlierBlocks
     * @param earlierPathArgs needed input arguments
     * @param outputArgs      needed output arguments
     * @param job             the command to actually be run (type blockrunnable or
     *                        configurableblockrunnable)
     * @return TIPLBlock to be run later
     */
    public static ITIPLBlock InlineBlock(final String name, final String prefix,
                                         final ITIPLBlock[] earlierBlocks,
                                         final String[] earlierPathArgs,
                                         final String[] outputArgs, final BlockRunnable job) {
        final ITIPLBlock cBlock = new BaseTIPLBlock(name, earlierBlocks) {
            protected String cPrefix = prefix;

            @Override
            protected IBlockImage[] bGetInputNames() {
                final IBlockImage[] inNames = new BlockImage[earlierPathArgs.length];
                for (int i = 0; i < earlierPathArgs.length; i++) {
                    inNames[i] = new BlockImage(earlierPathArgs[i],
                            "No description provided", true);
                }
                return inNames;
            }

            @Override
            protected IBlockImage[] bGetOutputNames() {
                final IBlockImage[] outNames = new BlockImage[outputArgs.length];
                for (int i = 0; i < outputArgs.length; i++) {
                    outNames[i] = new BlockImage(outputArgs[i],
                            "No description provided", true);
                }
                return outNames;
            }

            @Override
            public boolean executeBlock() {
                if (job instanceof ConfigurableBlockRunnable)
                    ((ConfigurableBlockRunnable) job).setup(this);
                if (!isReady()) {
                    System.out.println("Block is not ready!");
                    return false;
                }
                try {
                    job.run(this);
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
                return cPrefix;
            }

            @Override
            public void setPrefix(String setValue) {
                cPrefix = setValue;
            }

            @Override
            public ArgumentParser setParameterBlock(final ArgumentParser p) {
                ArgumentParser outPar = job.setParameter(p, prefix);

                return outPar;
            }

        };
        return cBlock;
    }

    public static void main(final String[] args) {

        System.out.println("BlockRunner v" + kVer);
        System.out.println("Runs a block by its name");
        System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");

        ArgumentParser p = TIPLGlobal.activeParser(args);
        final String blockname = p.getOptionString("blockname", "",
                "Class name of the block to run");
        final boolean withGui = p.getOptionBoolean("gui", false, "Show a GUI for parameter " +
                "adjustment");

        // black magic
        if (blockname.length() > 0) {
            ITIPLBlock cBlock = null;
            try {
                cBlock = (ITIPLBlock) Class.forName(blockname).newInstance();
            } catch (final ClassNotFoundException e) {
                // Try adding the tipl.blocks to the beginning
                try {
                    cBlock = (ITIPLBlock) Class.forName("tipl.blocks." + blockname).newInstance();
                } catch (final ClassNotFoundException e2) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("Block Class:" + blockname
                            + " was not found, does it exist?");
                } catch (final Exception e2) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("Block Class:" + blockname
                            + " could not be created, sorry!");
                }

            } catch (final Exception e) {
                e.printStackTrace();
                throw new IllegalArgumentException("Block Class:" + blockname
                        + " could not be created, sorry!");
            }
            if (withGui) {
                p = ArgumentDialog.GUIBlock(cBlock, p.subArguments("gui", true));
            }

            p = cBlock.setParameter(p);

            // code to enable running as a job
            final boolean runAsJob = p
                    .getOptionBoolean("sge:runasjob",
                            "Run this script as an SGE job (adds additional settings to this task");
            SGEJob jobToRun = null;
            if (runAsJob)
                jobToRun = SGEJob.runAsJob(BaseTIPLBlock.class.getName(), p.subArguments("gui",
                                true),
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

    public BaseTIPLBlock(final String inName) {
        super(new LocalIOHelper(), inName);
    }

    public BaseTIPLBlock(final String inName, final ITIPLBlock[] earlierBlocks) {
        super(new LocalIOHelper(), inName, earlierBlocks);
    }

}
