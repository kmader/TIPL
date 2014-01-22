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

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.junit.Test;

import akka.actor.ActorRef;
import scala.concurrent.duration.Duration;
import tipl.formats.TImgRO;
import tipl.tests.TestPosFunctions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Computes an approximation to pi */
public class VolumeFraction {
  public static JavaSparkContext getContext(String masterName) {
	  return new JavaSparkContext(masterName, "JavaLogQuery",
		      System.getenv("SPARK_HOME"), JavaSparkContext.jarOfClass(VolumeFraction.class));
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
  protected static Result processSlice(boolean[] workData) {
		long inV=0,outV=0;
		for(boolean cVal: workData) {
			if(cVal) inV++;
			else outV++;
		}
		return new Result(inV,outV);
	}
	
  
	public static void main(String[] args) throws Exception {
    String masterName;
	  if (args.length == 0) {
      System.err.println("Usage: JavaLogQuery <master> [slices], assuming local[4]");
      masterName="local[4]";
    } else {
    	masterName=args[0];
    }
    JavaSparkContext jsc = getContext(masterName);
    final TImgRO testImg = TestPosFunctions.wrapIt(1000,
			new TestPosFunctions.DiagonalPlaneFunction());
	final int sliceCount=testImg.getDim().z;
	final int blockCount=sliceCount/20;
    List<Integer> l = new ArrayList<Integer>(sliceCount);
    for (int i = 0; i < sliceCount; i++) {
      l.add(i);
    }
     
    JavaRDD<Integer> dataSet = jsc.parallelize(l, blockCount);
    long start=System.currentTimeMillis();
    Result outCount = dataSet.map(new Function<Integer, Result>() {
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
    System.out.println(String.format("\n\tVolume Fraction: \t\t%s\n\tCalculation time: \t%s",
			outCount.getVF(), Duration.create(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS) ));
  }
}