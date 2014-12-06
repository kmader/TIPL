package tipl.spark;

import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.storage.StorageLevel;
import scala.Tuple2;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

/**
 * This has all the static functions and settings for dealing with spark,
 * particularly allowing for a custom configuration from the command line and singleton access to
 * the JavaSparkContext. It also calculates partitions and a few other commonly used operations.
 *
 * @author mader
 */
@SuppressWarnings("serial")
abstract public class SparkGlobal {
    /**
     * The default persistence settings basically copied from spark
     */
    private final static int MEMORY_ONLY = 0;
    private final static int DEFAULT_LEVEL = MEMORY_ONLY;
    private final static int MEMORY_ONLY_SER = 1;
    private final static int MEMORY_AND_DISK = 2;
    private final static int MEMORY_AND_DISK_SER = 3;
    private final static int DISK_ONLY = 4;
    private static String memorySettings = "";
    private static String sparkLocal = "/scratch"; // much better than tmp
    private static boolean useKyro = false;
    private static final int kyroBufferSize = 200;
    private static final int shuffleBufferSize = 20 * 1024;
    private static final int maxMBforReduce = 128;
    static public boolean useCompression = true;
    private static final double memFraction = 0.3;
    private static final int retainedStages = 10;
    static public int defaultPartitions = 25;

    /**
     * how long to remember in seconds stage and task information (default 12 hours)
     */
    private static final int metaDataMemoryTime = 12 * 60 * 60;
    private static JavaSparkContext currentContext = null;
    private static String masterName = "";
    /**
     * The maximum number of cores which can be used per job
     */
    static int maxCores = -1;

    /**
     * Utility Function Section
     */
    private static int sparkPersistence = -1;
    private static int intSlicesPerCore = 1;

    static public boolean inheritContext(final JavaSparkContext activeContext) {
        if (activeContext != null) currentContext = activeContext;
        else {
            throw new IllegalArgumentException("The context cannot be inherited because it is " +
                    "null");
        }
        return true;
    }

    static public int getMaxCores() {
        return maxCores;
    }

    private static void setMaxCores(int imaxCores) {
        assert (imaxCores > 0);
        maxCores = imaxCores;
    }

    static String getMasterName() {
        if (masterName.length() < 1) masterName = "local[" + TIPLGlobal.availableCores + "]";
        return masterName;
    }

    static public JavaSparkContext getContext() {
        if (currentContext == null) currentContext = getContext("temporaryContext");
        return currentContext;
    }

    /**
     * For a stopping of the spark context (isn't really necessary)
     *
     * @note this is automatically done when the VM shutsdown but something else might find it
     * useful
     */
    static public void stopContext() {
        if (currentContext != null) currentContext.stop();
        currentContext = null;
    }

    /**
     * Create or reuses the instance of the JavaSparkContext
     *
     * @param jobName
     * @return
     */
    static public JavaSparkContext getContext(final String jobName) {
        return getContext(jobName,true);
    }

    static public JavaSparkContext getContext(final String jobName, boolean autoKill) {

        if (currentContext == null) {
            if (maxCores > 0) System.setProperty("spark.cores.max", "" + maxCores);
            if (memorySettings.length() > 0)
                System.setProperty("spark.executor.memory", "" + memorySettings);
            if (sparkLocal.length() > 0) System.setProperty("spark.local.dir", "" + sparkLocal);
            if (useKyro) {
                System.setProperty("spark.serializer", "org.apache.spark.serializer" +
                        ".KryoSerializer");
                System.setProperty("spark.kryo.registrator", "tipl.spark.TIPLRegistrator");
                System.setProperty("spark.kryoserializer.buffer.mb", "" + kyroBufferSize);
            }
            System.setProperty("spark.shuffle.compress", "" + useCompression);
            System.setProperty("spark.shuffle.spill.compress", "" + useCompression);
            System.setProperty("spark.shuffle.file.buffer.kb", "" + shuffleBufferSize);

            System.setProperty("spark.rdd.compress", "" + useCompression);
            //
            System.setProperty("spark.akka.frameSize", "" + maxMBforReduce);
            System.setProperty("spark.hadoop.validateOutputSpecs", "false");

            System.setProperty("spark.storage.memoryFraction", "" + memFraction); // there is a
            // fair amount of overhead in my scripts
            System.setProperty("spark.shuffle.consolidateFiles",
                    "true"); // consolidates intermediate files
            System.setProperty("spark.speculation", "true"); // start rerunning long running jobs
            System.setProperty("spark.reducer.maxMbInFlight", "" + maxMBforReduce); // size of
            // data to send to each reduce task (should be larger than any output)
            System.setProperty("spark.ui.retainedStages", "" + retainedStages); // number of
            // stages to retain before GC
            System.setProperty("spark.cleaner.ttl", "" + metaDataMemoryTime); // time to remember
            // metadata

            currentContext = new JavaSparkContext(getMasterName(), jobName,
                    System.getenv("SPARK_HOME"), JavaSparkContext.jarOfClass(SparkGlobal.class));

            if(autoKill) StopSparkeAtFinish(currentContext);
        }
        return currentContext;
    }

    /**
     * A function to register the runtime to be stopped so it needn't be done manually
     */
    private static void StopSparkeAtFinish(final JavaSparkContext jsc) {
        TIPLGlobal.curRuntime.addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out
                        .println("SHUTHOOK\tStopping SparkContext");
                jsc.stop();
            }
        });
    }

    /**
     * Basic text describing how the persistence is mapped to integer values for input
     *
     * @return
     */
    private static String getPersistenceText() {
        return "Memory only (" + MEMORY_ONLY + "), " +
                "Serialized Memory only (" + MEMORY_ONLY_SER + "), " +
                "Memory and disk (" + MEMORY_AND_DISK + "), " +
                "Serialized Memory and disk (" + MEMORY_AND_DISK_SER + "), " +
                "Disk only (" + DISK_ONLY + ")";

    }

    static public int getSparkPersistenceValue() {
        return sparkPersistence;
    }

    static public StorageLevel getSparkPersistence() {
        int inSparkLevel = sparkPersistence;
        if (inSparkLevel < 0) inSparkLevel = DEFAULT_LEVEL;
        assert (inSparkLevel >= 0);
        assert (inSparkLevel <= 4);
        switch (inSparkLevel) {
            case MEMORY_ONLY:
                return StorageLevel.MEMORY_ONLY();
            case MEMORY_ONLY_SER:
                return StorageLevel.MEMORY_ONLY_SER();
            case MEMORY_AND_DISK:
                return StorageLevel.MEMORY_AND_DISK();
            case MEMORY_AND_DISK_SER:
                return StorageLevel.MEMORY_AND_DISK_SER();
            case DISK_ONLY:
                return StorageLevel.DISK_ONLY();
            default:
                throw new IllegalArgumentException("Spark Persistance Storage Setting is not " +
                        "known:" + inSparkLevel);
        }
    }

    /**
     * asserts the persistence on a DTImg or JavaRDD if the sparkpersistence value is above 0
     * otherwise it does nothing
     *
     * @param inImage
     */
    @SuppressWarnings("rawtypes")
    static public void assertPersistance(DTImg inImage) {
        if (sparkPersistence >= 0) inImage.persist(getSparkPersistence());
    }

    @SuppressWarnings("rawtypes")
    static public void assertPerstance(JavaRDD inRdd) {
        if (sparkPersistence >= 0) {
            if (inRdd.getStorageLevel() != StorageLevel.NONE()) inRdd.unpersist();
            inRdd.persist(getSparkPersistence());
        }
    }

    private static void setSparkPersistance(int inPersist) {
        assert (inPersist < 4);
        assert (inPersist == -1 || inPersist >= 0);
        sparkPersistence = inPersist;
    }

    private static int getSlicesPerCore() {
        return intSlicesPerCore;
    }

    private static void setSlicesPerCore(int slicesPerCore) {
        assert (slicesPerCore > 0);
        intSlicesPerCore = slicesPerCore;
    }

    /**
     * Calculates the number of partitions based on a given dataset and slices per core distribution
     *
     * @param slices
     * @param slicesPerCore
     * @return
     */
    private static int calculatePartitions(int slices, int slicesPerCore) {
        assert (slices > 0);
        assert (slicesPerCore > 0);
        int partCount = 1;
        if (slicesPerCore < slices) partCount = (int) Math.ceil(slices * 1.0 / slicesPerCore);
        assert (partCount > 0 & partCount < slices);
        return partCount;
    }


    static public int calculatePartitions(int slices) {
        return calculatePartitions(slices, getSlicesPerCore());
    }

    /**
     * Get the default slice-based partitioner from DSImg. This greatly speeds up lookup operations
     * @param pos position of the first slice
     * @param dim the dimensions of the entire image
     * @return partitioner object
     */
    static public Partitioner getPartitioner(final D3int pos,final D3int dim) {
        return new DSImg.D3IntSlicePartitioner(pos, dim);
    }

    static public Partitioner getPartitioner(final TImgTools.HasDimensions imgObj) {
        return getPartitioner(imgObj.getPos(), imgObj.getDim());
    }

    static public ArgumentParser activeParser(String[] args) {
        return activeParser(TIPLGlobal.activeParser(args));
    }

    /**
     * parser which actively changes spark relevant parameters
     *
     * @param sp input argumentparser
     * @return
     */
    private static ArgumentParser activeParser(ArgumentParser sp) {
        sp.createNewLayer("Spark Settings");
        masterName = sp.getOptionString("@masternode", getMasterName(),
                "The name of the master node to connect to");
        memorySettings = sp.getOptionString("@sparkmemory", memorySettings, "The memory per job");
        sparkLocal = sp.getOptionString("@sparklocal", sparkLocal, "The local drive to cache onto");
        useKyro = sp.getOptionBoolean("@sparkkyro", useKyro, "Use the kyro serializer");
        useCompression = sp.getOptionBoolean("@sparkcompression", useCompression,
                "Use compression in spark for RDD and Shuffles");

        setSlicesPerCore(sp.getOptionInt("@sparkpartitions", getSlicesPerCore(),
                "The number of slices to load onto a single operating core",
                1, Integer.MAX_VALUE));
        setSparkPersistance(sp.getOptionInt("@sparkpersist", getSparkPersistenceValue(),
                "Image default persistance options:" + getPersistenceText(),
                -1, 4));

        setMaxCores(sp.getOptionInt("@sparkcores", maxCores, "The maximum number of cores each " +
                        "job can use (-1 is no maximum)",
                -1, Integer.MAX_VALUE));
        return sp;//.subArguments("@");
    }


}
