package tipl.tests;


import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tipl.util.HZClient;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IMap;
import com.hazelcast.core.PartitionAware;
/**
 * A set of tests to ensure the hazelcast functionality is working as it is supposed to
 * @author mader
 *
 */
public class HazelcastTests {
	protected ClientConfig config;
	protected HazelcastInstance hazelcastClient;
	public static final int sliceCount=10000;
	public static final int sliceSize=5000;
	public static final int sliceLoops=1;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		config = new ClientConfig();
		//HZClient.addMerlin(config);
		//config.addAddress("129.129.158.222:5701");
		
		//config.setExecutorPoolSize(1000);
		hazelcastClient = HZClient.createCluster(HZClient.getMerlin());//HazelcastClient.newHazelcastClient(config);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		Hazelcast.shutdownAll();
	}
	


	public static long sumArray(HazelcastInstance client,String arrayName) {
		Map<Long,int[]> test1=client.getMap(arrayName);
		return sumMap(test1);
	}
	public static long sumMap(Map<Long,int[]> inMap) {
		long totVal=0;
		for (Long curSlice : inMap.keySet()) {
			for (int cVal : inMap.get(curSlice)) totVal+=cVal;
		}
		return totVal;
	}

	public static Map<Long,int[]> createEmptyHazelImage(HazelcastInstance client,String arrayName) {
		Map<Long,int[]> test1=client.getMap("testSimpleImage");

		return test1;
	}
	public static Map<Long,int[]> createEmptyHashImage(HazelcastInstance client,String arrayName) {
		Map<Long,int[]> test1=new HashMap<Long,int[]>();
		return test1;
	}
	/** 
	 * fill the map with the current slice number
	 * @param inMap
	 * @param sliceCount
	 * @param sliceSize
	 */
	public static void initializeMap(Map<Long,int[]> inMap,int sliceCount,int sliceSize) {
		for (int i=0;i<sliceCount;i++) {
			int[] cVal=new int[sliceSize];
			for(int j=0;j<sliceSize;j++) cVal[j]=i;
			inMap.put(new Long(i), cVal);
		}
	}
	/**
	 * checks that the initial sum of all slices is 0 and then sets two slices and checks again
	 * @param inMap
	 */
	public static void setAndTestMap(Map<Long,int[]> inMap,int sliceCount,int sliceSize,int loops) {

		for(int j=0;j<=loops;j++) {
			initializeMap(inMap,sliceCount,sliceSize);
			long outVal=sumMap(inMap);
			if (j==0) System.out.println("Checking that initial value is 0 == "+outVal); 
			assertEquals(outVal,0);
			int[] tempArr=inMap.get(new Long(0));
			tempArr[50]=20;
			inMap.put(new Long(0), tempArr);
			tempArr=inMap.get(new Long(1));
			tempArr[90]=10;
			inMap.put(new Long(1), tempArr);
			assertEquals(sumMap(inMap),30); 
		}
	}
	@Test
	public void simpleSliceSum() {
		long totVal=0;
		int[] curSlice=randomSlice(sliceSize);
		for (int cVal : curSlice) totVal+=cVal;
		System.out.println(totVal+" for "+sliceSize+" pixels "+curSlice[0]+", "+curSlice[1]);
		assertEquals(totVal,((sliceSize-1)*sliceSize)/2);
	}
	
	//@Test
	public void testSimpleHazelMapRW() {
		Map<Long,int[]> testImg=createEmptyHazelImage(hazelcastClient,"testSimpleImage");
		setAndTestMap(testImg,sliceCount,sliceSize,sliceLoops);
	}
	//@Test
	public void testSimpleMapRW() {
		Map<Long,int[]> testImg=new HashMap<Long,int[]>();
		setAndTestMap(testImg,sliceCount,sliceSize,sliceLoops);
	}
	protected static final String imName="testSimpleImage";
	
	@Test
	public void testHazelMapOperationParallelDumb() {
		hazelMapOperation(true,true);
	}
	@Test
	public void testHazelMapOperationParallel() {
		hazelMapOperation(true,false);
	}
	@Test
	public void testHazelMapOperationSerial() {
		hazelMapOperation(false,false);
	}
	public void hazelMapOperation(boolean isParallel, boolean isDumb) {
		long ExpectedSum=sliceSize;
		ExpectedSum*=sliceCount-1;
		ExpectedSum*=sliceCount;
		ExpectedSum/=2;
		System.out.println(config.getExecutorPoolSize()+" is the pool size");
		Map<Long,int[]> testImg=createEmptyHazelImage(hazelcastClient,imName);
		initializeMap(testImg,sliceCount,sliceSize);
		try {assertEquals(testSimpleHazelParallel(imName,isParallel,isDumb),ExpectedSum);}
		catch (Exception e) {
			System.out.println("Parallel Execution was Interruped!!!!");
		}
	}
	@Test
	public void testMakeImageSerial() {
		final String testName="testImg";
		long ExpectedSum=sliceCount;
		ExpectedSum*=sliceSize-1;
		ExpectedSum*=sliceSize;
		ExpectedSum/=2;
		try {
			Set<MakeSlices> tasks=new HashSet<MakeSlices>();
			for (int i=0;i<sliceCount;i++) tasks.add(new RandomSlices(testName,i,sliceSize));
		}
		
		catch (Exception e) {
			System.out.println("Parallel Execution was Interruped for Making Image!!!!");
		}
		
		try {assertEquals(testSimpleHazelParallel(testName,false,false),ExpectedSum);}
		catch (Exception e) {
			System.out.println("Parallel Execution was Interruped!!!!");
		}
	}
	
	// parallel code
	protected final static String execName="sliceprocessor";
	
	@Test
	public void testMakeImageParallel() {
		final String testName="testImg";
		long ExpectedSum=sliceCount;
		ExpectedSum*=sliceSize-1;
		ExpectedSum*=sliceSize;
		ExpectedSum/=2;
		try {
			ExecutorService es = hazelcastClient.getExecutorService(execName);
			Set<Callable<Long>> tasks=new HashSet<Callable<Long>>();
			for (int i=0;i<sliceCount;i++) tasks.add(new RandomSlices(testName,i,sliceSize));
			es.invokeAll(tasks);
			es.shutdown();
		}
		
		catch (Exception e) {
			System.out.println("Parallel Execution was Interruped for Making Image!!!!");
		}
		
		try {assertEquals(testSimpleHazelParallel(testName,true,false),ExpectedSum);}
		catch (Exception e) {
			System.out.println("Parallel Execution was Interruped!!!!");
		}
	}
	
	public long testSimpleHazelParallel(String objName, boolean isParallel,boolean isDumb) throws Exception {
		ExecutorService es = hazelcastClient.getExecutorService(execName);
		Set<Callable<Long>> tasks=new HashSet<Callable<Long>>();
		for (int i=0;i<sliceCount;i++) {
			if (isDumb) {
				tasks.add(new SliceSumDumb(objName, new Long(i)));
			} else {
				tasks.add(new SliceSum(objName, new Long(i)));
			}
		}
		List<Future<Long>> output;
		if (isParallel) {
			output=es.invokeAll(tasks);
			es.shutdown();
		} else {
			output=new LinkedList<Future<Long>>();
			for(Callable<Long> ctask : tasks) {
				if (ctask instanceof HazelcastInstanceAware) {
					((HazelcastInstanceAware) ctask).setHazelcastInstance(hazelcastClient);
				}
				output.add(new Predestiny<Long>(ctask.call()));
			}
		}
		
		long allResults=0;
		for (Future<Long> cVal : output) allResults+=cVal.get();
		
		return allResults;
	}
	public static abstract class MakeSlices
	implements Callable, Serializable, HazelcastInstanceAware {
		private final String objName;
		private final Long sliceId;
		private transient HazelcastInstance hz;

		public MakeSlices(String inName, long inSliceId) {
			super();
			this.objName = inName;
			this.sliceId = new Long(inSliceId);
		}
		public Long call() {
			IMap<Long, int[]> curImage = hz.getMap(objName);
			System.out.println(hz.getCluster().getLocalMember()+" makes image:"+objName+" slice #"+sliceId);
			
			int[] sliceData = getSlice(sliceId);
			curImage.put(sliceId,sliceData);
			return null;
		}
		public abstract int[] getSlice(Long sliceId);

		public void setHazelcastInstance(HazelcastInstance hz) {
			this.hz = hz;
		}

	}
public static int[] randomSlice(int sliceSize) {
		
		int[] outMat=new int[sliceSize];
		for (int i=0;i<sliceSize;i++) {
			int cVal;
			boolean isUnique=false;
			while (!isUnique) {
				isUnique=true;
				cVal=(int) Math.round(Math.random()*(sliceSize-1));
				for(int j=0;j<i;j++) {
					if (cVal==outMat[j]) {
						isUnique=false;
						break;
					}
				}
				outMat[i]=cVal;
			}
		}
		return outMat;
		
	}
	public static class RandomSlices extends MakeSlices {

		/**
		 * 
		 */
		final public int sliceLength;
		public RandomSlices(String inName, long inSliceId,int sliceLength) {
			super(inName, inSliceId);
			this.sliceLength=sliceLength;
		}

		@Override
		public int[] getSlice(Long sliceId) {
			return randomSlice(sliceLength);
		}

	}
	
	/**
	 * Abstract class for performing an operation on a slice
	 * @author mader
	 *
	 */
	public static abstract class SliceOperation
	implements Callable<Long>, PartitionAware<Long>, Serializable, HazelcastInstanceAware {
		/**
		 * 
		 */
		private static final long serialVersionUID = 638672646935290616L;
		private final String objName;
		private final Long sliceId;
		private transient HazelcastInstance hz;

		public SliceOperation(String inName, long inSliceId) {
			super();
			this.objName = inName;
			this.sliceId = new Long(inSliceId);
		}
		public Long call() {
			IMap<Long, int[]> curImage = hz.getMap(objName);
			int[] sliceData = curImage. get(sliceId);
			System.out.println(hz.getCluster().getLocalMember()+" processes image:"+objName+" slice #"+sliceId);
			return processSlice(sliceData);
		}
		public abstract Long processSlice(int[] sliceData);

		public void setHazelcastInstance(HazelcastInstance hz) {
			this.hz = hz;
		}
		public Long getPartitionKey() {
			return sliceId;
		}
	}
	public static class SliceSum extends SliceOperation {

		/**
		 * 
		 */
		private static final long serialVersionUID = -5369791919681571123L;

		public SliceSum(String inName, long inSliceId) {
			super(inName, inSliceId);
		}
		/** really inefficient adding routine **/
		@Override
		public Long processSlice(int[] sliceData) {
			
			long totVal=0;
			for(int i=0;i<10000;i++) {
			totVal=0;
			for (int cVal : sliceData) totVal+=cVal;
			}
			return totVal;
		}

	}
	/** 
	 * slicesum that sends invalid partition keys making the partitionaware functionality useless
	 * @author mader
	 *
	 */
	public static class SliceSumDumb extends SliceSum {
		
		public SliceSumDumb(String inName, long inSliceId) {
			super(inName, inSliceId);
			
		}
		@Override
		public Long getPartitionKey() {
			return new Long(0);
		}
		
	}
	/** a very boring future which has been defined in the constructor, allows compatibility with other classes
	 * 
	 * @author mader
	 *
	 * @param <T>
	 */
	public static class Predestiny<T> implements Future<T> {
		protected T storedValue;
		/** in case we decide to subclass it
		 * 
		 * @return the value to return with get commands
		 */
		public T getStoredValue() { return storedValue;}
		public Predestiny(T inValue) {
			storedValue=inValue;
		}
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			return getStoredValue();
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			return getStoredValue();
		}
		
		
	}
	
	
	




}
