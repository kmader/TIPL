package tipl.spark;

import org.apache.spark.api.java.JavaSparkContext;

import tipl.formats.VirtualAim;
import tipl.util.ArgumentParser;
import tipl.util.TIPLGlobal;

abstract public class SparkGlobal {
	static protected JavaSparkContext currentContext=null;
	static protected String masterName="";
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
			currentContext=new JavaSparkContext(getMasterName(), jobName,System.getenv("SPARK_HOME"), JavaSparkContext.jarOfClass(SparkGlobal.class));
		}
		return currentContext;
	}
	
	public static ArgumentParser activeParser(String[] args) {return activeParser(TIPLGlobal.activeParser(args));}
	/**
	 * parser which actively changes spark relevant parameters
	 * @param sp input argumentparser
	 * @return
	 */
	public static ArgumentParser activeParser(ArgumentParser sp) {
		masterName=sp.getOptionString("@masternode",getMasterName(),"The name of the master node to connect to");
		return sp;//.subArguments("@");
	}
}