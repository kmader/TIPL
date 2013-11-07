package tipl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * SGEJob is for creating and managing (later) SGEJobs, simplifying many of the
 * more complicated tasks in parallel computing with these tools
 * 
 * @author mader
 * 
 * 
 */
public class SGEJob {
	public static void main(final String[] argv) {
		final SGEJob test = new SGEJob();
		final ArgumentParser p = test.setParameter(new ArgumentParser(argv));

		p.checkForInvalid();
		test.submit();
	}

	/**
	 * Creates a new SGEJob using the given input arguments p, with the prefix
	 * 
	 * @param className
	 *            is the name of the class to run
	 * @param p
	 *            given input arguments p
	 * @param prefix
	 *            text to be used before each argument, (preferably format xyz:)
	 */
	public static SGEJob runAsJob(final String className,
			final ArgumentParser p, final String prefix) {
		final SGEJob test = new SGEJob(p, prefix, "$JCMD&" + className
				+ p.subArguments(prefix).toString("&"));
		return test;

	}

	/**
	 * Creates a new SGEJob using the given input arguments p, with the prefix
	 * 
	 * @param scriptPath
	 *            is the path of the script to run
	 * @param p
	 *            given input arguments p
	 * @param prefix
	 *            text to be used before each argument, (preferably format xyz:)
	 */
	public static SGEJob runScriptAsJob(final String scriptPath,
			final ArgumentParser p, final String prefix) {
		final SGEJob test = new SGEJob(p, prefix, "$TIPLCMD&" + scriptPath
				+ p.subArguments(prefix).toString("&"));
		return test;

	}

	/**
	 * Used for submitting jobs on the Merlin4 cluster using the SGE system
	 * */
	public static int sendJob(final String execStr, final String pushStr) {
		System.out.println("Job-Details:" + pushStr);
		try {
			final Runtime rt = Runtime.getRuntime();
			final Process p = rt.exec(execStr);
			String line = "";
			String retVal = "";
			final BufferedReader stdInput = new BufferedReader(
					new InputStreamReader(p.getInputStream()));
			final BufferedWriter stdOutput = new BufferedWriter(
					new OutputStreamWriter(p.getOutputStream()));
			final BufferedReader stdError = new BufferedReader(
					new InputStreamReader(p.getErrorStream()));
			stdOutput.write(pushStr);
			stdOutput.flush();
			stdOutput.close();
			while ((line = stdInput.readLine()) != null) {
				retVal += line;
				System.out.println("Readback: " + line);
			}
			stdInput.close();
			while ((line = stdError.readLine()) != null) {
				retVal += line;
				System.err.println("Error: " + line);
			}
			stdError.close();
			System.out.println(retVal);
			final int exitCode = p.exitValue();
			return exitCode;

		} catch (final Exception e) {
			System.out.println("Error Executing Task");
			e.printStackTrace();
			return -1;
		}
	}

	public D3int guessDim = new D3int(1000);
	/** when should messages be sent **/
	protected String sendMail = "ae";

	protected String email = "kevinmader+merlinsge@gmail.com";
	/**
	 * number of CPU's to utilize
	 */
	protected String argPrefix = "";
	protected int cores = 2;

	protected String logName = "SGEJob.log";
	protected String jobName = "CallMeAl";
	protected String queueName = "all.q";
	protected String qsubPath = "/gpfs/home/gridengine/sge6.2u5p2/bin/lx26-amd64/qsub";
	protected String tiplPath = "/afs/psi.ch/project/tipl/jar/TIPL.jar";
	protected String tiplPathBeta = "/afs/psi.ch/project/tipl/jar/TIPL_beta.jar";
	protected String javaCmdPath = "/afs/psi.ch/project/tipl/jvm/bin/java";
	protected String jobToRun = "ls -R *";
	protected boolean waitForJob = false;
	protected boolean includeSGERAM = true;
	/**
	 * memory needed in megabytes
	 */
	protected double memory = 22000;
	/**
	 * factor to calculate additional memory needed based on image size
	 */
	protected double memoryFactor = 7;
	protected String jvmMaxMemory = "15G";
	protected String jvmStartMemory = "16G";
	/** the maximum execution time in minutes **/
	protected double maxExecutionTime = 420;
	/**
	 * The size of memory to keep reserved for the heap
	 * 
	 */
	public double jvmHeapReserve = 200;
	/**
	 * Once the heap is removed how much of the free memory can be used for
	 * java.
	 * 
	 */
	public double jvmUtility = 0.75;

	public SGEJob() {
		init();
	}

	/**
	 * Creates a new SGEJob using the given input arguments p
	 * 
	 * @param p
	 *            given input arguments p
	 */
	public SGEJob(final ArgumentParser p) {
		init();
		setParameter(p);
	}

	/**
	 * Creates a new SGEJob using the given input arguments p, with the prefix
	 * 
	 * @param p
	 *            given input arguments p
	 * @param prefix
	 *            text to be used before each argument, (preferably format xyz:)
	 */
	protected SGEJob(final ArgumentParser p, final String prefix,
			final String baseCmd) {
		init();
		jobToRun = baseCmd;
		argPrefix = prefix;
		setParameter(p);
	}

	/**
	 * Creates a new SGEJob using the given input arguments, and the prefix
	 * 
	 * @param prefix
	 *            text to be used before each argument, (preferably format xyz:)
	 */
	public SGEJob(final String prefix) {
		init();
		argPrefix = prefix;
	}

	public SGEJob(final String[] cmdLine) {
		init();
		setParameter(new ArgumentParser(cmdLine));
	}

	protected String createHeader() {
		String headerStr = "#!/bin/bash\n";
		headerStr += "# SGEJob Default TIPL Script \n# \n# This script was automatically generated by TIPL for running tasks and dividing work\n";
		headerStr += "# using the parallel environment (PE).\n";
		headerStr += "#$ -N " + jobName + "\n";
		headerStr += "#$ -cwd\n";
		headerStr += "#$ -j y\n";
		headerStr += "#$ -q " + queueName + "\n";
		headerStr += "#$ -pe smp " + cores + "\n";
		if (email.length() > 0)
			headerStr += "#$ -M " + email + "\n";
		if (sendMail.length() > 0)
			headerStr += "#$ -m " + sendMail + "\n";
		headerStr += "#$ -o " + logName + "\n";
		headerStr += "#$ -l mem_free=" + mem_free() + (includeSGERAM ? (",ram=" + ram()) : "")
				+ ",s_rt=" + runtime() + ",h_rt=" + runtime() + "\n\n";
		// some strange handling bug, no idea what it is for
		headerStr += "###################################################\n# Fix the SGE environment-handling bug (bash):\nsource /usr/share/Modules/init/sh\nexport -n -f module\n";
		return headerStr;
	}

	public void init() {
		setMemory(memory);
		setExecutionTime(maxExecutionTime);
	}

	public String mem_free() {
		return Math.round(memory) + "M";
	};

	/**
	 * estimate the memory needed for standard operations (distance map,
	 * filtering etc) for a given sized image
	 * 
	 * @param guessDim
	 * @return memory needed in megabytes
	 */
	public double memEstimate(final D3int imgDim) {
		return (memoryFactor * (imgDim.prod() * 2) / (1e6));
	}

	public String ram() {
		return Math.round(memory / cores) + "M";
	}

	/**
	 * Maximum execution time in minutes
	 */
	public String runtime() {
		final int hours = (int) Math.floor(maxExecutionTime / 60);
		final int minutes = (int) Math.floor(maxExecutionTime - hours * 60);
		final int seconds = (int) Math
				.round((maxExecutionTime - hours * 60 - minutes) * 60);
		return String.format("%02d", hours) + ":"
				+ String.format("%02d", minutes) + ":"
				+ String.format("%02d", seconds);
	}

	public void setExecutionTime(final double etime) {
		maxExecutionTime = etime;
	}

	/**
	 * Set the amount of memory java needs for the job in megabytes (total
	 * calculated from this)
	 * 
	 * @param mem
	 */
	public void setJavaMemory(final double mem) {
		setMemory((mem + jvmHeapReserve) / jvmUtility);
	}

	public void setJobToRun(final String inCmd) {
		jobToRun = inCmd;
	}

	protected void setJvmMemory(final double mem) {
		final double jvmMaxVal = (mem - jvmHeapReserve) * jvmUtility;
		jvmMaxMemory = Math.round(jvmMaxVal) + "M";
		jvmStartMemory = Math.round(jvmMaxVal - jvmHeapReserve) + "M";
	}

	/**
	 * Set the total memory for the job in megabytes
	 * 
	 * @param mem
	 */
	public void setMemory(final double mem) {
		memory = mem;
		setJvmMemory(mem);
	}
	
	/**
	 * code to set the defaults to the beamline cluster values
	 */
	public void setBeamlineCluster() {
		qsubPath="/gridware/sge/bin/lx24-amd64/qsub";
		includeSGERAM=false;
		queueName="tomcat_smp_standard.q";
		
	}
	public ArgumentParser setParameter(final ArgumentParser p) {
		if (p.getOptionBoolean(argPrefix + "atbeamline",
				"Load defaults from the tomcat cluster")) {
			setBeamlineCluster();
		}
		cores = p.getOptionInt(argPrefix + "cores", cores,
				"Number of CPUs to request for a job", 1, 128);
		email = p.getOptionString(argPrefix + "email", email,
				"Email Address to Send Job Status To");
		sendMail = p
				.getOptionString(argPrefix + "sendmail", sendMail,
						"When to send messages? (a is after/on completion, e is on error)");
		queueName = p
				.getOptionString(argPrefix + "queue", queueName,
						"Name of the queue to run in (normally all.q, long.q and short.q)");

		final boolean runScript = p
				.getOptionBoolean(argPrefix + "tiplscript",
						"Run a tipl jython script put script name and arguments in execute");
		final boolean runBlock = p.getOptionBoolean(argPrefix + "tiplblock",
				"Run a tipl block");

		final String jobDelimiter = p.getOptionString(argPrefix + "delimiter",
				"&", "Delimiter to use to turn job text into readable text");
		final String lineDelimiter = p.getOptionString(argPrefix + "newline",
				"#", "Delimiter to use to turn into a hard return");

		// if runBlock

		final String jobText = p
				.getOptionString(argPrefix + "execute", jobToRun,
						"Code to execute").replaceAll(jobDelimiter, " ")
				.replaceAll(lineDelimiter, "\n");
		if (runScript)
			setJobToRun("$TIPLCMD " + jobText);
		else if (runBlock)
			setJobToRun("$JCMD tipl.blocks.BaseTIPLBlock ");
		else
			setJobToRun(jobText);
		setExecutionTime(p
				.getOptionDouble(
						argPrefix + "runtime",
						maxExecutionTime,
						"Maximum run time for job in minutes (normally limited to 24 hours except for long.q"));

		jobName = p.getOptionString(argPrefix + "jobname", jobName,
				"Name of job");
		logName = p.getOptionString(argPrefix + "logname", jobName + ".log",
				"Name for log files");
		memoryFactor = p
				.getOptionDouble(argPrefix + "memoryfactor", memoryFactor,
						"Number of times the image size needed (stored as integer in megabytes)");

		final String inFile = p.getOptionString(argPrefix + "memoryfromaim",
				"", "File to use to estimate memory");
		if (inFile.length() > 0)
			setJavaMemory(memEstimate(TImgTools.ReadTImg(inFile).getDim()));

		setMemory(p.getOptionDouble(argPrefix + "memory", memory,
				"Memory needed for job"));
		javaCmdPath = p.getOptionPath(argPrefix + "javapath", javaCmdPath,
				"Java executable path");

		final boolean useBeta = p.getOptionBoolean(argPrefix + "tiplbeta",
				"Use the beta version of tipl (overridden by tiplpath)");
		if (useBeta)
			tiplPath = tiplPathBeta;

		tiplPath = p.getOptionPath(argPrefix + "tiplpath", tiplPath,
				"TIPL Jar Path");
		qsubPath = p.getOptionPath(argPrefix + "qsubpath", qsubPath,
				"Path to qsub executable");
		waitForJob = p.getOptionBoolean(argPrefix + "waitforjob",
				"Wait for job to complete before continuing");

		return p;
	}

	protected String setupJava() {
		String javaString = "# Load the environment modules for this job (the order may be important): \n\n###################################################\n";
		javaString += "# Set the environment variables:\n\n";
		javaString += "export CLASSPATH=" + tiplPath + "\n";
		javaString += "JAVACMD=\"" + javaCmdPath + "\"\n";
		javaString += "JARGS=\"-d64 -Xmx" + jvmMaxMemory + " -Xms"
				+ jvmStartMemory + "\" \n";
		javaString += "JCMD=\"$JAVACMD $JARGS\" \n";
		javaString += "JTEST=\"$JCMD -version\"\n";
		javaString += "TIPLCMD=\"$JCMD -jar " + tiplPath + " \"\n ";
		javaString += "$JTEST\n";
		return javaString;
	}

	public void submit() {
		submitJob(jobToRun);
	}

	protected void submitJob(final String cmdToRun) {
		final String jobInfo = createHeader() + setupJava();
		String ePath = qsubPath;
		if (waitForJob)
			ePath += " -sync y";
		System.out.println("JobSubmissionStatus:"
				+ SGEJob.sendJob(ePath, jobInfo + cmdToRun));
	}

}
