/**
 * 
 */
package tipl.akka;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.RoundRobinRouter;
import akka.testkit.JavaTestKit;
/**
 * Tests the akka framework for actors and some minor communication steps between them
 * particularly image data
 * @author mader
 *
 */

public class PiEstimator {
	public static final boolean debugMode=true;
	 static ActorSystem system;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		system = ActorSystem.create();

	}
	@After
	public void teardown() {
		    JavaTestKit.shutdownActorSystem(system);
		    system = null;
	}
	public static void main(String[] args) {
		calculate(5,100,100);
	}
	
	@Test
	public void testPi() {
		 new JavaTestKit(system) {{
			 calculate(5, 100, 100);
		 }};
		
		
	}
	public static void calculate(final int nrOfWorkers, final int nrOfElements, final int nrOfMessages) {
	    // Create an Akka system
	    ActorSystem system = ActorSystem.create("PiSystem");
	 
	    // create the result listener, which will print the result and shutdown the system
	    final ActorRef listener = system.actorOf(Props.create(Listener.class), "listener");
	 
	    // create the master
	    ActorRef master = system.actorOf(Props.create(Master.class, nrOfWorkers, nrOfMessages, nrOfElements, listener,system), "master");
	    
	    
	    // start the calculation
	    master.tell(new Calculate(),ActorRef.noSender());
	 
	  }

	
	public static class Listener extends UntypedActor {
		  public void onReceive(Object message) {
		    if (message instanceof PiApproximation) {
		      PiApproximation approximation = (PiApproximation) message;
		      System.out.println(String.format("\n\tPi approximation: \t\t%s\n\tCalculation time: \t%s",
		          approximation.getPi(), approximation.getDuration()));
		      getContext().system().shutdown();
		    } else {
		      unhandled(message);
		    }
		  }
		}
	public static class Master extends UntypedActor {
		  private final int nrOfMessages;
		  private final int nrOfElements;
		 
		  private double pi;
		  private int nrOfResults;
		  private final long start = System.currentTimeMillis();
		 
		  private final ActorRef listener;
		  private final ActorRef workerRouter;
		  private final ActorSystem asystem;
		 
		  public Master(final int nrOfWorkers, int nrOfMessages, int nrOfElements, ActorRef listener, ActorSystem system) {
		    this.nrOfMessages = nrOfMessages;
		    this.nrOfElements = nrOfElements;
		    this.listener = listener;
		    this.asystem=system;
		    if (debugMode) System.out.println("Created MasterA:"+this);
		    workerRouter = asystem.actorOf(Props.create(Worker.class).withRouter(new RoundRobinRouter(nrOfWorkers)),
		        "workerRouter");
		    
		    if (debugMode) System.out.println("Created Router:"+workerRouter);
		  }
		  
		  @Override
		  public void onReceive(Object message) {
			  if (debugMode) System.out.println("Master's listenin:"+message);
			  
			  if (message instanceof Calculate) {
			    for (int start = 0; start < nrOfMessages; start++) {
			     workerRouter.tell(new Work(start, nrOfElements), getSelf());
			    }
			  } else if (message instanceof Result) {
			    Result result = (Result) message;
			    pi += result.getValue();
			    nrOfResults += 1;
			    if (nrOfResults == nrOfMessages) {
			      // Send the result to the listener
			      Duration duration = Duration.create(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
			      listener.tell(new PiApproximation(pi, duration), getSelf());
			      // Stops this actor and all its supervised children
			      asystem.stop(getSelf());
			    }
			  } else {
			    unhandled(message);
			  }
			}
		}
	
	public static class Worker extends UntypedActor {
		public Worker() {
			if (debugMode) System.out.println("Created worker:"+this);
			//super();
			
		}
		
		private double calculatePiFor(int start, int nrOfElements) {
			  double acc = 0.0;
			  for (int i = start * nrOfElements; i <= ((start + 1) * nrOfElements - 1); i++) {
			    acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
			  }
			  return acc;
			}
		 
		  public void onReceive(Object message) {
			  if (debugMode) System.out.println("Worker's listenin:"+message);
		    if (message instanceof Work) {
		      Work work = (Work) message;
		      double result = calculatePiFor(work.getStart(), work.getNrOfElements());
		      getSender().tell(new Result(result), getSelf());
		    } else {
		      unhandled(message);
		    }
		  }
		}
	static class Calculate {
	}
	 
	static class Work {
	  private final int start;
	  private final int nrOfElements;
	 
	  public Work(int start, int nrOfElements) {
	    this.start = start;
	    this.nrOfElements = nrOfElements;
	  }
	 
	  public int getStart() {
	    return start;
	  }
	 
	  public int getNrOfElements() {
	    return nrOfElements;
	  }
	}
	 
	static class Result {
	  private final double value;
	 
	  public Result(double value) {
	    this.value = value;
	  }
	 
	  public double getValue() {
	    return value;
	  }
	}
	 
	static class PiApproximation {
	  private final double pi;
	  private final Duration duration;
	 
	  public PiApproximation(double pi, Duration duration) {
	    this.pi = pi;
	    this.duration = duration;
	  }
	 
	  public double getPi() {
	    return pi;
	  }
	 
	  public Duration getDuration() {
	    return duration;
	  }
	}

}
