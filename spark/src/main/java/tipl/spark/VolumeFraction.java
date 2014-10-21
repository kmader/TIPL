package tipl.spark;


import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import scala.concurrent.duration.Duration;
import tipl.formats.TImgRO;
import tipl.tests.TestPosFunctions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs a calculation of volume fraction using spark (one of the initial tests, will be removed)
 *
 * @author mader
 */
@Deprecated
public class VolumeFraction {
    private final static TImgRO classTestImage = TestPosFunctions.wrapIt(1000,
            new TestPosFunctions.DiagonalPlaneFunction());
    private static final int loopMax = 200;

    private static JavaSparkContext getContext(String masterName, String jobName) {
        return new JavaSparkContext(masterName, jobName,
                System.getenv("SPARK_HOME"), JavaSparkContext.jarOfClass(VolumeFraction.class));
    }

    private static Result processSlice(boolean[] workData) {
        long inV = 0, outV = 0;
        /** Do something silly like count each slice 100 times and count the in phase only on the
         *  even slices
         * and the out of phase only on the odd slices
         */
        for (int i = 0; i < loopMax; i++) {
            for (boolean cVal : workData) {
                if (cVal && (i % 2 == 0)) inV++;
                else if (!cVal && i % 2 == 1) outV++;
            }
        }
        return new Result(inV, outV);
    }

    private static Result sendTImgTest(final TImgRO testImg, JavaSparkContext jsc) {

        final int sliceCount = testImg.getDim().z;
        int blockCount = sliceCount / 20;
        if (blockCount < 1) blockCount = sliceCount;
        List<Integer> l = new ArrayList<Integer>(sliceCount);
        for (int i = 0; i < sliceCount; i++) {
            l.add(i);
        }
        JavaRDD<Integer> dataSet = jsc.parallelize(l, blockCount);
        return dataSet.map(new Function<Integer, Result>() {
            @Override
            public Result call(Integer sliceNum) {
                return processSlice((boolean[]) testImg.getPolyImage(sliceNum, 10));
            }
        }).reduce(new Function2<Result, Result, Result>() {
            @Override
            public Result call(Result inA, Result inB) {
                return new Result(inA.intPixels + inB.intPixels, inA.outPixels + inB.outPixels);
            }
        });
    }

    private static Result globalTImgTest(JavaSparkContext jsc) {

        final int sliceCount = classTestImage.getDim().z;
        final int blockCount = sliceCount / 20;
        List<Integer> l = new ArrayList<Integer>(sliceCount);
        for (int i = 0; i < sliceCount; i++) {
            l.add(i);
        }
        JavaRDD<Integer> dataSet = jsc.parallelize(l, blockCount);
        return dataSet.map(new Function<Integer, Result>() {
            @Override
            public Result call(Integer sliceNum) {
                return processSlice((boolean[]) classTestImage.getPolyImage(sliceNum, 10));
            }
        }).reduce(new Function2<Result, Result, Result>() {
            @Override
            public Result call(Result inA, Result inB) {
                return new Result(inA.intPixels + inB.intPixels, inA.outPixels + inB.outPixels);
            }
        });
    }

    private static Result localTImgTest(JavaSparkContext jsc) {

        final int sliceCount = 1000;
        final int blockCount = sliceCount / 20;
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
                return processSlice((boolean[]) testImg.getPolyImage(sliceNum, 10));
            }
        }).reduce(new Function2<Result, Result, Result>() {
            @Override
            public Result call(Result inA, Result inB) {
                return new Result(inA.intPixels + inB.intPixels, inA.outPixels + inB.outPixels);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        String masterName;
        if (args.length == 0) {
            System.err.println("Usage: VolumeFraction <master> [slices], assuming local[4]");
            masterName = "local[4]";
        } else {
            masterName = args[0];
        }
        JavaSparkContext jsc = getContext(masterName, "VFTest");
        long start1 = System.currentTimeMillis();
        Result outCount1 = sendTImgTest(TestPosFunctions.wrapIt(1000,
                new TestPosFunctions.DiagonalPlaneFunction()), jsc);

        long start2 = System.currentTimeMillis();
        Result outCount2 = localTImgTest(jsc);
        long start3 = System.currentTimeMillis();
        Result outCount3 = globalTImgTest(jsc);
        long startLocal = System.currentTimeMillis();
        // run test locally for comparison
        Result outCountLocal = sendTImgTest(TestPosFunctions.wrapIt(1000,
                new TestPosFunctions.DiagonalPlaneFunction()), getContext("local[1]", "LocalVF"));
        System.out.println(String.format("Distributed Image\n\tVolume Fraction: " +
                        "\t\t%s\n\tCalculation time: \t%s",
                outCount1.getVF(), Duration.create(start2 - start1, TimeUnit.MILLISECONDS)));
        System.out.println(String.format("Local Image\n\tVolume Fraction: \t\t%s\n\tCalculation " +
                        "time: \t%s",
                outCount2.getVF(), Duration.create(start3 - start2, TimeUnit.MILLISECONDS)));
        System.out.println(String.format("Global Image\n\tVolume Fraction: \t\t%s\n\tCalculation " +
                        "time: \t%s",
                outCount3.getVF(), Duration.create(startLocal - start3, TimeUnit.MILLISECONDS)));
        System.out.println(String.format("Local Calculation\n\tVolume Fraction: " +
                        "\t\t%s\n\tCalculation time: \t%s",
                outCountLocal.getVF(), Duration.create(System.currentTimeMillis() - startLocal,
                        TimeUnit.MILLISECONDS)));

    }

    protected static class Result implements Serializable {
        private final long intPixels;
        private final long outPixels;

        public Result(long sintPixels, long soutPixels) {
            intPixels = sintPixels;
            outPixels = soutPixels;
        }

        public long getInt() {
            return intPixels;
        }

        public long getOut() {
            return outPixels;
        }

        public double getVF() {
            return (100. * intPixels) / (intPixels + outPixels);
        }
    }
}