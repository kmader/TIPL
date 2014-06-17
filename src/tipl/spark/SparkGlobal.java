package tipl.spark;

import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.storage.StorageLevel;
import scala.Tuple2;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TIPLGlobal;

/**
 * This has all the static functions and settings for dealing with spark, particularly allowing for a custom configuration from the command line and singleton access to the JavaSparkContext. It also calculates partitions and a few other commonly used operations.
 *
 * @author mader
 */
@SuppressWarnings("serial")
abstract public class SparkGlobal {
    /**
     * The default persistence settings basically copied from spark
     */
    public final static int MEMORY_ONLY = 0;
    public final static int DEFAULT_LEVEL = MEMORY_ONLY;
    public final static int MEMORY_ONLY_SER = 1;
    public final static int MEMORY_AND_DISK = 2;
    public final static int MEMORY_AND_DISK_SER = 3;
    public final static int DISK_ONLY = 4;
    static public String memorySettings = "";
    static public String sparkLocal = "/scratch"; // much better than tmp
    static public boolean useKyro = false;
    static public int kyroBufferSize = 200;
    static public int shuffleBufferSize = 20 * 1024;
    static public int maxMBforReduce = 128;
    static public boolean useCompression = true;
    static public double memFraction = 0.3;
    static public int retainedStages = 10;
    /**
     * how long to remember in seconds stage and task information (default 12 hours)
     */
    static public int metaDataMemoryTime = 12 * 60 * 60;
    static protected JavaSparkContext currentContext = null;
    static protected String masterName = "";
    /**
     * The maximum number of cores which can be used per job
     */
    static protected int maxCores = -1;

    /**
     * Utility Function Section
     */
    protected static int sparkPersistence = -1;
    protected static int intSlicesPerCore = 1;

    static public boolean inheritContext(final JavaSparkContext activeContext) {
        if (activeContext != null) currentContext = activeContext;
        else {
            throw new IllegalArgumentException("The context cannot be inherited because it is null");
        }
        return true;
    }

    static public int getMaxCores() {
        return maxCores;
    }

    static public void setMaxCores(int imaxCores) {
        assert (imaxCores > 0);
        maxCores = imaxCores;
    }

    static protected String getMasterName() {
        if (masterName.length() < 1) masterName = "local[" + TIPLGlobal.availableCores + "]";
        return masterName;
    }

    static public JavaSparkContext getContext() {
        if (currentContext == null) currentContext = getContext("temporaryContext");
        return currentContext;
    }

    /**
     * Create or reuses the instance of the JavaSparkContext
     *
     * @param masterName
     * @param jobName
     * @return
     */
    static public JavaSparkContext getContext(final String jobName) {

        if (currentContext == null) {
            if (maxCores > 0) System.setProperty("spark.cores.max", "" + maxCores);
            if (memorySettings.length() > 0) System.setProperty("spark.executor.memory", "" + memorySettings);
            if (sparkLocal.length() > 0) System.setProperty("spark.local.dir", "" + sparkLocal);
            if (useKyro) {
                System.setProperty("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
                System.setProperty("spark.kryo.registrator", "tipl.spark.TIPLRegistrator");
                System.setProperty("spark.kryoserializer.buffer.mb", "" + kyroBufferSize);
            }
            System.setProperty("spark.shuffle.compress", "" + useCompression);
            System.setProperty("spark.shuffle.spill.compress", "" + useCompression);
            System.setProperty("spark.shuffle.file.buffer.kb", "" + shuffleBufferSize);

            System.setProperty("spark.rdd.compress", "" + useCompression);
            //
            System.setProperty("spark.akka.frameSize", "" + maxMBforReduce);


            System.setProperty("spark.storage.memoryFraction", "" + memFraction); // there is a fair amount of overhead in my scripts
            System.setProperty("spark.shuffle.consolidateFiles", "true"); // consolidates intermediate files
            System.setProperty("spark.speculation", "true"); // start rerunning long running jobs
            System.setProperty("spark.reducer.maxMbInFlight", "" + maxMBforReduce); // size of data to send to each reduce task (should be larger than any output)
            System.setProperty("spark.ui.retainedStages", "" + retainedStages); // number of stages to retain before GC
            System.setProperty("spark.cleaner.ttl", "" + metaDataMemoryTime); // time to remember metadata

            currentContext = new JavaSparkContext(getMasterName(), jobName, System.getenv("SPARK_HOME"), JavaSparkContext.jarOfClass(SparkGlobal.class));
            StopSparkeAtFinish(currentContext);
        }
        return currentContext;
    }

    /**
     * A function to register the runtime to be stopped so it needn't be done manually
     */
    static public void StopSparkeAtFinish(final JavaSparkContext jsc) {
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
    static public String getPersistenceText() {
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
                throw new IllegalArgumentException("Spark Persistance Storage Setting is not known:" + inSparkLevel);
        }
    }

    /**
     * asserts the persistence on a DTImg or JavaRDD if the sparkpersistence value is above 0 otherwise it does nothing
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

    static public void setSparkPersistance(int inPersist) {
        assert (inPersist < 4);
        assert (inPersist == -1 || inPersist >= 0);
        sparkPersistence = inPersist;
    }

    static public int getSlicesPerCore() {
        return intSlicesPerCore;
    }

    static public void setSlicesPerCore(int slicesPerCore) {
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
    static public int calculatePartitions(int slices, int slicesPerCore) {
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

    static public Partitioner getPartitioner(final D3int dim) {
        return new Partitioner() {
            final int slCnt = dim.z;
            final int ptCnt = SparkGlobal.calculatePartitions(dim.z);
            final int slPpt = SparkGlobal.getSlicesPerCore();

            @Override
            public int getPartition(Object arg0) {
                D3int curPos;
                if (arg0 instanceof Tuple2) {
                    curPos = ((Tuple2<D3int, ?>) arg0)._1;
                } else if (arg0 instanceof D3int) {
                    curPos = (D3int) arg0;

                } else {
                    throw new IllegalArgumentException("Object Cannot Be partitioned!!!!!" + arg0);
                }
                return (int) Math.floor(curPos.z * 1.0 / slPpt);


            }

            @Override
            public int numPartitions() {
                return ptCnt;
            }

        };
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
    static public ArgumentParser activeParser(ArgumentParser sp) {
        masterName = sp.getOptionString("@masternode", getMasterName(), "The name of the master node to connect to");
        memorySettings = sp.getOptionString("@sparkmemory", memorySettings, "The memory per job");
        sparkLocal = sp.getOptionString("@sparklocal", sparkLocal, "The local drive to cache onto");
        useKyro = sp.getOptionBoolean("@sparkkyro", useKyro, "Use the kyro serializer");
        useCompression = sp.getOptionBoolean("@sparkcompression", useCompression, "Use compression in spark for RDD and Shuffles");

        setSlicesPerCore(sp.getOptionInt("@sparkpartitions", getSlicesPerCore(), "The number of slices to load onto a single operating core",
                1, Integer.MAX_VALUE));
        setSparkPersistance(sp.getOptionInt("@sparkpersist", getSparkPersistenceValue(), "Image default persistance options:" + getPersistenceText(),
                -1, 4));


        setMaxCores(sp.getOptionInt("@sparkcores", maxCores, "The maximum number of cores each job can use (-1 is no maximum)",
                -1, Integer.MAX_VALUE));
        return sp;//.subArguments("@");
    }


}
