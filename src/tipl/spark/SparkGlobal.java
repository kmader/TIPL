package tipl.spark;

import org.apache.spark.api.java.JavaSparkContext;

abstract public class SparkGlobal {
	static protected JavaSparkContext currentContext=null;
	static public JavaSparkContext getContext() {
		if(currentContext==null) currentContext=getContext("local","temporaryContext");
		return currentContext;
	}
	/**
	 * Create or reuses the instance of the JavaSparkContext
	 * @param masterName
	 * @param jobName
	 * @return
	 */
	public static JavaSparkContext getContext(String masterName,String jobName) {
		if(currentContext==null) currentContext=new JavaSparkContext(masterName, jobName,
				System.getenv("SPARK_HOME"), JavaSparkContext.jarOfClass(SparkGlobal.class));
		return currentContext;
	}	
}
