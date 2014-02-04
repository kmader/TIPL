package tipl.spark;

import java.io.File;

import org.apache.spark.api.java.JavaSparkContext;

import tipl.formats.VirtualAim;
import tipl.util.ArgumentParser;
import tipl.util.TIPLGlobal;

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
	static public int maxCores=-1;
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
		
		maxCores=sp.getOptionInt("@sparkcores",maxCores,"The maximum number of cores each job can use (-1 is no maximum)");
		return sp;//.subArguments("@");
	}
}
