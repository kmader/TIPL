/**
 * 
 */
package tipl.tools;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tipl.util.D3int;
import tipl.util.TIPLGlobal;

/**
 * @author maderk Implements the TIPLPLuginInput class using executor services
 *         instead of thread-based parallelism
 */
public abstract class BaseTIPLPluginInExecutor extends BaseTIPLPluginIn {
	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.tools.TIPLPluginInput#PluginName()
	 */
	public static class jobRunner implements Runnable {
		final BaseTIPLPluginIn runObj;
		final Object runWork;

		public jobRunner(BaseTIPLPluginIn inObj, Object inWork) {
			runObj = inObj;
			runWork = inWork;
		}

		@Override
		public void run() {
			runObj.processWork(runWork);
		}
	}

	/** the pool of workers for executing specific tasks */
	protected final ExecutorService myPool;

	/**
	 * 
	 */
	public BaseTIPLPluginInExecutor() {
		super();
		myPool = TIPLGlobal.getTaskExecutor();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param idim
	 * @param ioffset
	 */
	public BaseTIPLPluginInExecutor(D3int idim, D3int ioffset) {
		super(idim, ioffset);
		myPool = Executors.newFixedThreadPool(supportedCores,
				TIPLGlobal.daemonFactory);
		// TODO Auto-generated constructor stub
	}

	/**
	 * jobRunner factory returns a runnable for the given chunk of work, the
	 * default is based on the divideWork code and the processWork function but
	 * both of these commands can be rewritten
	 */

	protected Runnable jobRunnerFactory(final int curCore, final int totalCores) {
		return new jobRunner(this, divideThreadWork(curCore, totalCores));
	}

	/**
	 * Placeholder for the real function which processes the work, basically
	 * takes the section of the computational task and processes it
	 * 
	 * @param myWork
	 *            myWork (typically int[2] with starting and ending slice) is an
	 *            object which contains the work which needs to be processed by
	 *            the given thread
	 */
	@Override
	protected void processWork(Object myWork) {
		System.out
				.println("THIS IS AN pseudo-ABSTRACT FUNCTION AND DOES NOTHING, PLEASE EITHER TURN OFF MULTICORE SUPPORT OR REWRITE YOUR PLUGIN!!!!");
		return; // nothing to do
	}

	/**
	 * Distribute (using divideThreadWork) and process (using processWork) the
	 * work across the various threads, returns true if thread is launch thread
	 */
	@Override
	public boolean runMulticore() {
		jStartTime = System.currentTimeMillis();
		for (int i = 1; i < wantedCores(); i++) {
			myPool.submit(jobRunnerFactory(i, wantedCores()));

		}
		try {
			myPool.wait();
		} catch (final InterruptedException e) {
			System.out.println(getPluginName()
					+ " was interupted and did not process successfully");
			e.printStackTrace();
			return false;
		}
		final String outString = "MCJob Ran in "
				+ StrRatio(System.currentTimeMillis() - jStartTime, 1000)
				+ " seconds in " + supportedCores;
		System.out.println(outString);
		procLog += outString + "\n";
		return true;
	}

}
