/**
 * 
 */
package tipl.akka;



import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import tipl.formats.TImgRO;
import tipl.tests.TestPosFunctions;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.RoundRobinRouter;
/**
 * Calculates volume fraction for a test image
 * @author mader
 *
 */

public class VolumeFraction {
	public static final boolean debugMode=false;
	public static int workerCount=4;
	
	/**
	 * @throws java.lang.Exception
	 */

	public static void main(String[] args) {
		final TImgRO testImg = TestPosFunctions.wrapIt(1000,
				new TestPosFunctions.DiagonalPlaneFunction());
		calculateSingle(testImg);
		calculate2(testImg,workerCount);
	}

	/**
	 * Actor version with linear thread reading
	 * @param inImage
	 * @param nrOfWorkers
	 */
	public static void calculate(final TImgRO inImage,final int nrOfWorkers) {
		// Create an Akka system
		ActorSystem system = ActorSystem.create("VolumeFraction");

		// create the result listener, which will print the result and shutdown the system
		final ActorRef listener = system.actorOf(Props.create(Listener.class), "listener");

		// create the master
		ActorRef master = system.actorOf(Props.create(Master.class, nrOfWorkers, listener,system), "master");

		// start the calculation
		master.tell(inImage,ActorRef.noSender());

	}
	/**
	 * Parallel actor version (with parallel reading)
	 * @param inImage
	 * @param nrOfWorkers
	 */
	public static void calculate2(final TImgRO inImage,final int nrOfWorkers) {
		System.out.println("Parallel Actor Version");
		// Create an Akka system
		ActorSystem system = ActorSystem.create("VolumeFraction");

		// create the result listener, which will print the result and shutdown the system
		final ActorRef listener = system.actorOf(Props.create(Listener.class), "listener");
		final ActorRef TImgRouter = system.actorOf(Props.create(TImgWorker.class,inImage).withRouter(new RoundRobinRouter(nrOfWorkers)),
				"workerRouter");
		// create the master
		ActorRef master = system.actorOf(Props.create(TMaster.class, TImgRouter, listener,system), "master");

		// start the calculation
		master.tell(inImage,ActorRef.noSender());

	}
	/**
	 * Single threaded actor free version
	 * @param inImage
	 */
	public static void calculateSingle(final TImgRO inImage) {
		System.out.println("Single Threaded Actor Free Version");
		ActorSystem system = ActorSystem.create("VolumeFraction");
		final ActorRef listener = system.actorOf(Props.create(Listener.class), "listener");
		long intVal=0,outVal=0,start=System.currentTimeMillis();
		for (int slice = 0; slice < inImage.getDim().z; slice++) {
			for(boolean cVal: (boolean[]) inImage.getPolyImage(slice, 10)) {
				if(cVal) intVal++;
				else outVal++;
			}
		}
		Duration duration = Duration.create(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
		listener.tell(new VFResult(intVal,outVal, duration), ActorRef.noSender());
	}


	public static class Listener extends UntypedActor {
		public void onReceive(Object message) {
			if (message instanceof VFResult) {
				VFResult approximation = (VFResult) message;
				System.out.println(String.format("\n\tVolume Fraction: \t\t%s\n\tCalculation time: \t%s",
						approximation.getVF(), approximation.getDuration()));
				getContext().system().shutdown();
			} else {
				unhandled(message);
			}
		}
	}

	public static class Master extends UntypedActor {

		private int nrOfResults;
		private final long start = System.currentTimeMillis();

		protected final ActorRef listener;
		protected final ActorRef workerRouter;
		protected final ActorSystem asystem;

		public Master(final int nrOfWorkers, ActorRef listener, ActorSystem system) {
			this.listener = listener;
			this.asystem = system;
			if (debugMode) System.out.println("Created Master:"+this);
			workerRouter = asystem.actorOf(Props.create(Worker.class).withRouter(new RoundRobinRouter(nrOfWorkers)),
					"workerRouter");

			if (debugMode) System.out.println("Created Router:"+workerRouter);
		}
		protected Master(final ActorRef inputRouter, ActorRef listener, ActorSystem system) {
			this.listener = listener;
			this.asystem = system;
			if (debugMode) System.out.println("Created Master:"+this);
			workerRouter = inputRouter;
			if (debugMode) System.out.println("Using Router:"+workerRouter);
			
		}
		
		protected long intVal;
		protected long outVal;
		protected int totalSlices;
		@Override
		public void onReceive(Object message) {
			if (debugMode) System.out.println("Master's listenin:"+message);

			if (message instanceof TImgRO) {
				TImgRO msgImg=(TImgRO) message;
				totalSlices=msgImg.getDim().z;
				for (int slice = 0; slice < msgImg.getDim().z; slice++) {
					workerRouter.tell(msgImg.getPolyImage(slice, 10), getSelf());
				}
			} else if (message instanceof Result) {
				Result result = (Result) message;
				intVal += result.getInt();
				outVal += result.getOut();
				
				nrOfResults += 1;
				if (nrOfResults == totalSlices) {
					// Send the result to the listener
					Duration duration = Duration.create(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
					listener.tell(new VFResult(intVal,outVal, duration), getSelf());
					// Stops this actor and all its supervised children
					asystem.stop(getSelf());
				}
			} else {
				unhandled(message);
			}
		}
	}
	public static class TMaster extends Master {
		public TMaster(final ActorRef inputRouter, ActorRef listener, ActorSystem system) {
			super(inputRouter,listener,system);
		}
		@Override
		public void onReceive(Object message) {
			if (debugMode) System.out.println("TMaster's listenin:"+message);
			if (message instanceof TImgRO) {
				TImgRO msgImg=(TImgRO) message;
				totalSlices=msgImg.getDim().z;
				for (int slice = 0; slice < msgImg.getDim().z; slice++) {
					workerRouter.tell(new Integer(slice), getSelf());
				}
			} else super.onReceive(message);
		}
	}
		
	public static class Worker extends UntypedActor {

		public Worker() {
			super();
			if (debugMode) System.out.println("Created image-free worker:"+this);
		}

		protected static void processSlice(boolean[] workData,ActorRef sender,ActorRef myself) {
			long inV=0,outV=0;
			for(boolean cVal: workData) {
				if(cVal) inV++;
				else outV++;
			}
			sender.tell(new Result(inV,outV), myself);
		}
		public void onReceive(Object message) {
			if (debugMode) System.out.println("Worker's listenin:"+message);
			if (message instanceof boolean[]) {
				processSlice((boolean[]) message,getSender(),getSelf());

			} else  {
				unhandled(message);
			}
		}
	}

	public static class TImgWorker extends Worker {
		private final TImgRO basisImg;
		public TImgWorker(final TImgRO inImg) {
			super();
			if (debugMode) System.out.println("Created timgworker:"+this);
			basisImg=inImg;	
		}
		@Override
		public void onReceive(Object message) {
			if (message instanceof Integer) {
				int currentSlice=((Integer) message).intValue();
				processSlice((boolean[]) basisImg.getPolyImage(currentSlice, 10),getSender(),getSelf());
			} else super.onReceive(message);
		}


	}

	static class Result {
		private final long intPixels;
		private final long outPixels;
		public Result(long sintPixels,long soutPixels) {
			intPixels=sintPixels;
			outPixels=soutPixels;
		}
		public long getInt() {
			return intPixels;
		}
		public long getOut() {
			return outPixels;
		}
		public double getVF() {
			return (100.*intPixels)/(intPixels+outPixels);
		}
	}

	static class VFResult extends Result {
		private final Duration duration;

		public VFResult(long sintPixels,long soutPixels, Duration duration) {
			super(sintPixels,soutPixels);
			this.duration = duration;
		}

		public Duration getDuration() {
			return duration;
		}
	}

}
