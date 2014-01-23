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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;

import scala.concurrent.duration.Duration;
import tipl.formats.TImgRO;
import tipl.tests.TestPosFunctions;

/** Computes an approximation to pi */
public class VolumeFraction {
	public static JavaSparkContext getContext(String masterName,String jobName) {
		return new JavaSparkContext(masterName, jobName,
				System.getenv("SPARK_HOME"), JavaSparkContext.jarOfClass(VolumeFraction.class));
	}
	final public static TImgRO classTestImage = TestPosFunctions.wrapIt(1000,
			new TestPosFunctions.DiagonalPlaneFunction());
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
	protected static Result processSlice(boolean[] workData) {
		long inV=0,outV=0;
		/** Do something silly like count each slice 100 times and count the in phase only on the even slices
		 * and the out of phase only on the odd slices
		 */
		for(int i=0;i<loopMax;i++) {
			for(boolean cVal: workData) {
				if(cVal && (i%2==0)) inV++;
				else if (!cVal && i%2==1) outV++;
			}
		}
		return new Result(inV,outV);
	}

	public static Result sendTImgTest(final TImgRO testImg,JavaSparkContext jsc) {

		final int sliceCount=testImg.getDim().z;
		int blockCount=sliceCount/20;
		if (blockCount<1) blockCount=sliceCount;
		List<Integer> l = new ArrayList<Integer>(sliceCount);
		for (int i = 0; i < sliceCount; i++) {
			l.add(i);
		}
		JavaRDD<Integer> dataSet = jsc.parallelize(l, blockCount);
		return dataSet.map(new Function<Integer, Result>() {
			@Override
			public Result call(Integer sliceNum) {
				return processSlice((boolean[]) testImg.getPolyImage(sliceNum.intValue(),10));
			}
		}).reduce(new Function2<Result, Result, Result>() {
			@Override
			public Result call(Result inA, Result inB) {
				return new Result(inA.intPixels+inB.intPixels,inA.outPixels+inB.outPixels);
			}
		});
	}
	public static Result globalTImgTest(JavaSparkContext jsc) {

		final int sliceCount=classTestImage.getDim().z;
		final int blockCount=sliceCount/20;
		List<Integer> l = new ArrayList<Integer>(sliceCount);
		for (int i = 0; i < sliceCount; i++) {
			l.add(i);
		}
		JavaRDD<Integer> dataSet = jsc.parallelize(l, blockCount);
		return dataSet.map(new Function<Integer, Result>() {
			@Override
			public Result call(Integer sliceNum) {
				return processSlice((boolean[]) classTestImage.getPolyImage(sliceNum.intValue(),10));
			}
		}).reduce(new Function2<Result, Result, Result>() {
			@Override
			public Result call(Result inA, Result inB) {
				return new Result(inA.intPixels+inB.intPixels,inA.outPixels+inB.outPixels);
			}
		});
	}
	public static Result localTImgTest(JavaSparkContext jsc) {

		final int sliceCount=1000;
		final int blockCount=sliceCount/20;
		List<Integer> l = new ArrayList<Integer>(sliceCount);
		for (int i = 0; i < sliceCount; i++) {
			l.add(i);
		}
		JavaRDD<Integer> dataSet = jsc.parallelize(l, blockCount);
		return dataSet.map(new Function<Integer, Result>() {
			@Override
			public Result call(Integer sliceNum) {
				final TImgRO testImg = TestPosFunctions.wrapIt(sliceCount,
						new TestPosFunctions.DiagonalPlaneFunction());
				return processSlice((boolean[]) testImg.getPolyImage(sliceNum.intValue(),10));
			}
		}).reduce(new Function2<Result, Result, Result>() {
			@Override
			public Result call(Result inA, Result inB) {
				return new Result(inA.intPixels+inB.intPixels,inA.outPixels+inB.outPixels);
			}
		});
	}

	public static void main(String[] args) throws Exception {
		String masterName;
		if (args.length == 0) {
			System.err.println("Usage: JavaLogQuery <master> [slices], assuming local[4]");
			masterName="local[4]";
		} else {
			masterName=args[0];
		}
		JavaSparkContext jsc = getContext(masterName,"VFTest");
		long start1=System.currentTimeMillis();
		Result outCount1=sendTImgTest(TestPosFunctions.wrapIt(1000,
				new TestPosFunctions.DiagonalPlaneFunction()),jsc);

		long start2=System.currentTimeMillis();
		Result outCount2=localTImgTest(jsc);
		long start3=System.currentTimeMillis();
		Result outCount3=globalTImgTest(jsc);
		long startLocal=System.currentTimeMillis();
		// run test locally for comparison
		Result outCountLocal=sendTImgTest(TestPosFunctions.wrapIt(1000,
				new TestPosFunctions.DiagonalPlaneFunction()),getContext("local[1]","LocalVF"));
		System.out.println(String.format("Distributed Image\n\tVolume Fraction: \t\t%s\n\tCalculation time: \t%s",
				outCount1.getVF(), Duration.create(start2 - start1, TimeUnit.MILLISECONDS) ));
		System.out.println(String.format("Local Image\n\tVolume Fraction: \t\t%s\n\tCalculation time: \t%s",
				outCount2.getVF(), Duration.create(start3 - start2, TimeUnit.MILLISECONDS) ));
		System.out.println(String.format("Global Image\n\tVolume Fraction: \t\t%s\n\tCalculation time: \t%s",
				outCount3.getVF(), Duration.create(startLocal-start3, TimeUnit.MILLISECONDS) ));
		System.out.println(String.format("Local Calculation\n\tVolume Fraction: \t\t%s\n\tCalculation time: \t%s",
				outCountLocal.getVF(), Duration.create(System.currentTimeMillis() - startLocal, TimeUnit.MILLISECONDS) ));

	}
}