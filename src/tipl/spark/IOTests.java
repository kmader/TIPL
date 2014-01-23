package tipl.spark;


/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;

import scala.Tuple1;
import scala.Tuple2;
import scala.concurrent.duration.Duration;
import tipl.formats.TImgRO;
import tipl.spark.VolumeFraction.Result;
import tipl.tests.TestPosFunctions;
import tipl.util.TImgTools;

/** Computes an approximation to pi */
public class IOTests {
	public static JavaSparkContext getContext(String masterName,String jobName) {
		return new JavaSparkContext(masterName, jobName,
				System.getenv("SPARK_HOME"), JavaSparkContext.jarOfClass(IOTests.class));
	}
	
	protected static class Result implements Serializable {
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
	
	static protected final int loopMax=200;
	protected static Result processSlice(int[] workData) {
		long inV=0,outV=0;
		/** Do something silly like count each slice 100 times and count the in phase only on the even slices
		 * and the out of phase only on the odd slices
		 */
		for(int i=0;i<loopMax;i++) {
			for(int cVal: workData) {
				if(cVal>0 && (i%2==0)) inV++;
				else if (cVal<0 && i%2==1) outV++;
			}
		}
		return new Result(inV,outV);
	}
	public static class DImg implements Serializable {
		final int sliceNum;
		final int[] sliceData;
		public DImg(int slice,int[] data) {
			sliceNum=slice;
			sliceData=data;
		}
		
	}
	public static JavaPairRDD<Integer,int[]> ReadIntImg(final JavaSparkContext jsc, final String imgName) {
		final TImgRO cImg=TImgTools.ReadTImg(imgName);
		final int sliceCount=cImg.getDim().z;
		
		List<Integer> l = new ArrayList<Integer>(sliceCount);
		for (int i = 0; i < sliceCount; i++) {
			l.add(i);
		}
		return jsc.parallelize(l).map(new PairFunction<Integer,Integer,int[]>() {
			@Override
			public Tuple2<Integer,int[]> call(Integer sliceNum) {
				final int[] cSlice=(int[]) cImg.getPolyImage(sliceNum.intValue(),2);
				return new Tuple2<Integer,int[]>(sliceNum,cSlice);
			}
		});
	}
	public static JavaPairRDD<Integer,int[]> SpreadImage(final JavaSparkContext jsc, final JavaPairRDD<Integer,int[]> inImg,final int windowSize) {
		return inImg.flatMap(
				  new PairFlatMapFunction<Tuple2<Integer,int[]>, Integer,int[]>() {
				    public Iterable<Tuple2<Integer,int[]>> call(Tuple2<Integer,int[]> inD) {
				    	List<Tuple2<Integer,int[]>> outList = new ArrayList<Tuple2<Integer,int[]>>(2*windowSize+1);
						for (int i = -windowSize; i <= windowSize; i++) {
							int oSlice=inD._1.intValue()+i;
							//if (oSlice>0 && oSlice<3) 
								outList.add(new Tuple2<Integer,int[]>(new Integer(oSlice),inD._2));
						}
						return outList;
				    }
				  });
	}
	public static JavaPairRDD<Integer,int[]> FilterImage(final JavaPairRDD<Integer,int[]> inImg) {
		return inImg.reduceByKey(new Function2<int[],int[],int[]>() {
			@Override
			public int[] call(int[] arg0, int[] arg1)
					throws Exception {
				int[] out=new int[arg0.length];
				System.arraycopy(arg0, 0, out, 0, arg0.length);
				for(int i=0;i<arg1.length;i++) out[i]+=arg1[i];
				System.out.println("Reduce filt:"+out[100]+" from "+arg0[100]+", "+arg1[100]);
				return out;
			}
		});
	}
	public static Result sendTImgTest(final JavaSparkContext jsc,final String imgName) {
		JavaPairRDD<Integer,int[]> dataSet = ReadIntImg(jsc,imgName);
		JavaPairRDD<Integer,int[]> dataSet2 = SpreadImage(jsc,dataSet,100);
		JavaPairRDD<Integer,int[]> dataSet3 = FilterImage(dataSet2);
		System.out.println("Number of Slices "+dataSet.count());
		System.out.println("Number of Slices after spread "+dataSet2.count());
		System.out.println("Number of Slices after filter "+dataSet.count());
		System.out.println("Number of Slices after spread (distinct)"+dataSet2.distinct().count());
		return new Result(0,0);
	}


	public static void main(String[] args) throws Exception {
		String masterName;
		if (args.length == 0) {
			System.err.println("Usage: IOTests <master> [slices], assuming local[4]");
			masterName="local[4]";
		} else {
			masterName=args[0];
		}
		String fileName="/Users/mader/Dropbox/TIPL/test/io_tests/rec8tiff";
		JavaSparkContext jsc = getContext(masterName,"IOTest");
		long start1=System.currentTimeMillis();
		Result outCount1=sendTImgTest(jsc,fileName);
		long startLocal=System.currentTimeMillis();
		// run test locally for comparison
		Result outCountLocal=sendTImgTest(getContext("local","LocalIO"),fileName);
		
		System.out.println(String.format("Distributed Image\n\tVolume Fraction: \t\t%s\n\tCalculation time: \t%s",
				outCount1.getVF(), Duration.create(startLocal - start1, TimeUnit.MILLISECONDS) ));
		System.out.println(String.format("Local Calculation\n\tVolume Fraction: \t\t%s\n\tCalculation time: \t%s",
				outCountLocal.getVF(), Duration.create(System.currentTimeMillis() - startLocal, TimeUnit.MILLISECONDS) ));

	}
}