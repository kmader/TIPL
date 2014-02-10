package tipl.spark;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.storage.StorageLevel;

import tipl.util.ArgumentParser;
import tipl.util.TIPLGlobal;
/**
 * This has all the static functions and settings for dealing with spark, particularly allowing for a custom configuration from the command line and singleton access to the JavaSparkContext. It also calculates partitions and a few other commonly used operations.
 * @author mader
 *
 */
abstract public class SparkGlobal {
	static protected JavaSparkContext currentContext=null;
	static public boolean inheritContext(final JavaSparkContext activeContext) {
		if(activeContext!=null) currentContext=activeContext;
		else {
			throw new IllegalArgumentException("The context cannot be inherited because it is null");
		}
		return true;
	}
	static protected String masterName="";
	/**
	 * The maximum number of cores which can be used per job
	 */
	static protected int maxCores=-1;
	static public int getMaxCores() { return maxCores;}
	static public void setMaxCores(int imaxCores) {
		assert(imaxCores>0);
		maxCores=imaxCores;
	}
	static public String memorySettings="";
	static public String sparkLocal="/scratch"; // much better than tmp
	static protected String getMasterName() {
		if(masterName.length()<1) masterName="local["+TIPLGlobal.availableCores+"]";
		return masterName;
	}
	static public JavaSparkContext getContext() {
		if(currentContext==null) currentContext=getContext(getMasterName(),"temporaryContext");
		return currentContext;
	}
	/**
	 * Create or reuses the instance of the JavaSparkContext
	 * @param masterName
	 * @param jobName
	 * @return
	 */
	public static JavaSparkContext getContext(final String inMasterName,final String jobName) {
		if(currentContext==null) {
			masterName=inMasterName;
			if(maxCores>0) System.setProperty("spark.cores.max", ""+maxCores);
			if(memorySettings.length()>0) System.setProperty("spark.executor.memory", ""+memorySettings);
			if(sparkLocal.length()>0) System.setProperty("spark.local.dir", ""+sparkLocal);
			currentContext=new JavaSparkContext(getMasterName(), jobName,System.getenv("SPARK_HOME"), JavaSparkContext.jarOfClass(SparkGlobal.class));
			StopSparkeAtFinish(currentContext);
		}
		return currentContext;
	}
	
	/** Utility Function Section */
	/**
	 * A function to register the runtime to be stopped so it needn't be done manually
	 **/
	public static void StopSparkeAtFinish(final JavaSparkContext jsc) {
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
	 * The default persistance settings basically copied from spark
	 */
	public final static int MEMORY_ONLY=0;
	public final static int MEMORY_ONLY_SER=1;
	public final static int MEMORY_AND_DISK=2;
	public final static int MEMORY_AND_DISK_SER=3;
	public final static int DISK_ONLY=4;
	public final static int DEFAULT_LEVEL=MEMORY_ONLY;
	/** 
	 * Basic text describing how the persistence is mapped to integer values for input
	 * @return
	 */
	public static String getPersistenceText() {
		return "Memory only ("+MEMORY_ONLY+"), "+
				"Serialized Memory only ("+MEMORY_ONLY_SER+"), "+
				"Memory and disk ("+MEMORY_AND_DISK+"), "+
				"Serialized Memory and disk ("+MEMORY_AND_DISK_SER+"), "+
				"Disk only ("+DISK_ONLY+")";
				
	}
	protected static int sparkPersistence=-1;
	public static int getSparkPersistenceValue() {
		return sparkPersistence;
	}
	public static StorageLevel getSparkPersistence() {
		int inSparkLevel=sparkPersistence;
		if (inSparkLevel<0) inSparkLevel=DEFAULT_LEVEL;
		assert(inSparkLevel>=0);
		assert(inSparkLevel<=4);
		switch(inSparkLevel) {
			case MEMORY_ONLY:  return StorageLevel.MEMORY_ONLY();
			case MEMORY_ONLY_SER: return StorageLevel.MEMORY_ONLY_SER();
			case MEMORY_AND_DISK: return StorageLevel.MEMORY_AND_DISK();
			case MEMORY_AND_DISK_SER: return StorageLevel.MEMORY_AND_DISK_SER();
			case DISK_ONLY: return StorageLevel.DISK_ONLY();
			default: throw new IllegalArgumentException("Spark Persistance Storage Setting is not known:"+inSparkLevel);
		}
	}
	/** 
	 * asserts the persistence on a DTImg or JavaRDD if the sparkpersistence value is above 0 otherwise it does nothing
	 * @param inImage
	 */
	@SuppressWarnings("rawtypes")
	public static void assertPersistance(DTImg inImage) {
		if (sparkPersistence>=0) inImage.persist(getSparkPersistence());
	}
	@SuppressWarnings("rawtypes")
	public static void assertPerstance(JavaRDD inRdd) {
		if (sparkPersistence>=0) inRdd.persist(getSparkPersistence());
	}
	public static void setSparkPersistance(int inPersist) {
		assert(inPersist<4);
		assert(inPersist==-1 || inPersist>=0);
		sparkPersistence=inPersist;
	}
	protected static int intSlicesPerCore=1;
	public static int getSlicesPerCore() {
		return intSlicesPerCore;
	}
	public static void setSlicesPerCore(int slicesPerCore) {
		assert(slicesPerCore>0);
		intSlicesPerCore=slicesPerCore;
	}
	/**
	 * Calculates the number of partitions based on a given dataset and slices per core distribution
	 * @param slices
	 * @param slicesPerCore
	 * @return
	 */
	public static int calculatePartitions(int slices, int slicesPerCore) {
		assert(slices>0);
		assert(slicesPerCore>0);
		int partCount=1;
		if (slicesPerCore<slices) partCount=(int) Math.ceil(slices*1.0/slicesPerCore);
		assert(partCount>0 & partCount<slices);
		return partCount;
	}
	public static int calculatePartitions(int slices) { 
		return calculatePartitions(slices,getSlicesPerCore());
	}
	public static ArgumentParser activeParser(String[] args) {return activeParser(TIPLGlobal.activeParser(args));}
	/**
	 * parser which actively changes spark relevant parameters
	 * @param sp input argumentparser
	 * @return
	 */
	public static ArgumentParser activeParser(ArgumentParser sp) {
		masterName=sp.getOptionString("@masternode",getMasterName(),"The name of the master node to connect to");
		memorySettings=sp.getOptionString("@sparkmemory",memorySettings,"The memory per job");
		sparkLocal=sp.getOptionString("@sparklocal",sparkLocal,"The local drive to cache onto");
		setSlicesPerCore(sp.getOptionInt("@sparkpartitions", getSlicesPerCore(), "The number of slices to load onto a single operating core", 
				1, Integer.MAX_VALUE));
		setSparkPersistance(sp.getOptionInt("@sparkpersist", getSparkPersistenceValue(), "Image default persistance options:"+getPersistenceText(),
				-1,4));

		
		setMaxCores(sp.getOptionInt("@sparkcores",maxCores,"The maximum number of cores each job can use (-1 is no maximum)",
				-1,Integer.MAX_VALUE));
		return sp;//.subArguments("@");
	}
	
	
}
