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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;
import scala.Tuple3;
import scala.concurrent.duration.Duration;
import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

/**
 * A very general test of the IO abilities of spark based on the picompute code
 * @author mader
 *
 */
@Deprecated
public class IOTests {

	
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
	public static class DImg implements Serializable {
		final int sliceNum;
		final int[] sliceData;
		public DImg(int slice,int[] data) {
			sliceNum=slice;
			sliceData=data;
		}
		
	}
	public static JavaPairRDD<Integer,int[]> ReadIntImg(final JavaSparkContext jsc, final String localImgName) {
		final String imgName = (new File(localImgName)).getAbsolutePath();
		TImgRO cImg=TImgTools.ReadTImg(imgName);
		final int sliceCount=cImg.getDim().z;
		
		List<Integer> l = new ArrayList<Integer>(sliceCount);
		for (int i = 0; i < sliceCount; i++) {
			l.add(i);
		}
		int blocks=sliceCount/10;
		if (blocks<1) blocks=1;
		return jsc.parallelize(l,blocks).map(new PairFunction<Integer,Integer,int[]>() {
			@Override
			public Tuple2<Integer,int[]> call(Integer sliceNum) {
				final int[] cSlice=(int[]) TImgTools.ReadTImgSlice(imgName,sliceNum.intValue(),2);
				return new Tuple2<Integer,int[]>(sliceNum,cSlice);
			}
		});
	}
	public static JavaPairRDD<Integer,int[]> SpreadImage(final JavaPairRDD<Integer,int[]> inImg,final int windowSize) {
		return inImg.flatMap(
				  new PairFlatMapFunction<Tuple2<Integer,int[]>, Integer,int[]>() {
				    public Iterable<Tuple2<Integer,int[]>> call(Tuple2<Integer,int[]> inD) {
				    	List<Tuple2<Integer,int[]>> outList = new ArrayList<Tuple2<Integer,int[]>>(2*windowSize+1);
						for (int i = -windowSize; i <= windowSize; i++) {
							int oSlice=inD._1.intValue()+i;
							if (oSlice>0) //TODO Fix to remove the top slices as well
								outList.add(new Tuple2<Integer,int[]>(new Integer(oSlice),inD._2().clone()));
						}
						return outList;
				    }
				  });
	}
	
	@SuppressWarnings("serial")
	public static JavaPairRDD<Integer,int[]> FilterImage(final JavaPairRDD<Integer,List<int[]>> inImg) {
		return inImg.map(
				new PairFunction<Tuple2<Integer,List<int[]>>,Integer,int[]>() {
			@Override
			public Tuple2<Integer, int[]> call(Tuple2<Integer, List<int[]>> groupImg)
					throws Exception {
				int[] out=new int[groupImg._2().get(0).length];
				final int sliceCnt=groupImg._2().size();
				for(int[] cImg : groupImg._2()) for(int i=0;i<out.length;i++) out[i]+=cImg[i]/sliceCnt;
				return new Tuple2<Integer,int[]>(groupImg._1(),out);
			}
		});
	}
	protected static Tuple3<Double,Double,Double> countSlice(int[] workData) {
		double sumV=0;
		double sumV2=0;
		long cntV=0;
		for(int cVal: workData) {
				sumV+=cVal;
				sumV2+=Math.pow(cVal,2);
				cntV++;
		}
		return new Tuple3<Double,Double,Double>(new Double(sumV),new Double(sumV2),new Double(cntV));	
	}
	public static String printImSummary(final Tuple3<Double,Double,Double> inData) {
		final double sumV=inData._1().doubleValue();
		final double sumV2=inData._2().doubleValue();
		final double cntV=inData._3().doubleValue();
		double meanV=(sumV/(1.0*cntV));
		return String.format("Mean:%3.2f\tSd:%3.2f\tSum:%3.0f",meanV,Math.sqrt((sumV2/(1.0*cntV)-Math.pow(meanV,2))),cntV);
	}
	
	public static String ImageSummary(final JavaPairRDD<Integer,int[]> inImg) {
		Tuple3<Double,Double,Double> imSum=inImg.map(new Function<Tuple2<Integer,int[]>,Tuple3<Double,Double,Double>>() {
			@Override
			public Tuple3<Double, Double,Double> call(Tuple2<Integer, int[]> arg0)
					throws Exception {
				return countSlice(arg0._2());
			}
		}).reduce(new Function2<Tuple3<Double,Double,Double>,Tuple3<Double,Double,Double>,Tuple3<Double,Double,Double>>() {
			@Override
			public Tuple3<Double, Double, Double> call(Tuple3<Double, Double, Double> arg0,
					Tuple3<Double, Double, Double> arg1) throws Exception {
				return new Tuple3<Double,Double,Double>(arg0._1()+arg1._1(),arg0._2()+arg1._2(),arg0._3()+arg1._3());
			}
			
		});
		String outString=printImSummary(imSum)+"\n";
		
		final List<Integer> keyList=inImg.map(new Function<Tuple2<Integer,int[]>,Integer>() {
			@Override
			public Integer call(Tuple2<Integer, int[]> arg0) throws Exception {
				return arg0._1;
			}
		}).collect();
		for(Integer cVal: keyList) outString+=cVal+",";
		return outString;
	}
	protected static int range=3;
	protected static int maximumSlice=100;
	public static Result sendTImgTest(final JavaSparkContext jsc,final String imgName) {
		final int maxSlice=maximumSlice;
		JavaPairRDD<Integer,int[]> dataSet = ReadIntImg(jsc,imgName).filter(new Function<Tuple2<Integer,int[]>,Boolean>() {
			@Override
			public Boolean call(Tuple2<Integer, int[]> arg0) throws Exception {
				return new Boolean(arg0._1<maxSlice);
			}
			
		}).cache();
		
		JavaPairRDD<Integer,int[]> spreadDataSet = SpreadImage(dataSet,range);
		JavaPairRDD<Integer,int[]> dataSet3 = FilterImage(spreadDataSet.groupByKey());
		
		System.out.println("Input Image\t# of Slices "+dataSet.count()+", "+ImageSummary(dataSet));
		System.out.println("After Spread\t# of Slices "+spreadDataSet.count()+", "+ImageSummary(spreadDataSet));
		System.out.println("After Filter\t# of Slices "+dataSet3.count()+", "+ImageSummary(dataSet3));
		return new Result(0,0);
	}


	public static void main(String[] args) throws Exception {
		ArgumentParser p=TIPLGlobal.activeParser(args);
		final String masterName=p.getOptionString("master", "local[4]", "Name of the master node for Spark");
		final String imagePath=p.getOptionPath("path", "/Users/mader/Dropbox/TIPL/test/io_tests/rec8tiff", "Path of image (or directory) to read in");
		range=p.getOptionInt("range", range, "The range to use for the filter");
		maximumSlice=p.getOptionInt("maxs", maximumSlice, "The maximum slice to keep");
		p.checkForInvalid();
		
		JavaSparkContext jsc = SparkGlobal.getContext("IOTest");
		long start1=System.currentTimeMillis();
		Result outCount1=sendTImgTest(jsc,imagePath);
		long startLocal=System.currentTimeMillis();
		// run test locally for comparison
		//Result outCountLocal=sendTImgTest(getContext("local","LocalIO"),imagePath);
		
		System.out.println(String.format("Distributed Image\n\tVolume Fraction: \t\t%s\n\tCalculation time: \t%s",
				outCount1.getVF(), Duration.create(startLocal - start1, TimeUnit.MILLISECONDS) ));
		//System.out.println(String.format("Local Calculation\n\tVolume Fraction: \t\t%s\n\tCalculation time: \t%s",
		//		outCountLocal.getVF(), Duration.create(System.currentTimeMillis() - startLocal, TimeUnit.MILLISECONDS) ));

	}
}