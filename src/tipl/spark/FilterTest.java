package tipl.spark;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;
import scala.Tuple3;
import scala.concurrent.duration.Duration;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.TImgRO.CanExport;
import tipl.tools.BaseTIPLPluginIn;
import tipl.util.ArgumentList.TypedPath;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TIPLGlobal;
import tipl.util.TImgBlock;

@SuppressWarnings("serial")
public class FilterTest extends NeighborhoodPlugin.FloatFilter {
	public static String ImageSummary(final JavaPairRDD<D3int,TImgBlock<float[]>> inImg) {
		Tuple3<Double,Double,Double> imSum=inImg.map(new Function<Tuple2<D3int,TImgBlock<float[]>>,Tuple3<Double,Double,Double>>() {
			@Override
			public Tuple3<Double, Double,Double> call(Tuple2<D3int,TImgBlock<float[]>> arg0)
					throws Exception {
				return countSlice(arg0._2().get());
			}
		}).reduce(new Function2<Tuple3<Double,Double,Double>,Tuple3<Double,Double,Double>,Tuple3<Double,Double,Double>>() {
			@Override
			public Tuple3<Double, Double, Double> call(Tuple3<Double, Double, Double> arg0,
					Tuple3<Double, Double, Double> arg1) throws Exception {
				return new Tuple3<Double,Double,Double>(arg0._1()+arg1._1(),arg0._2()+arg1._2(),arg0._3()+arg1._3());
			}
			
		});
		String outString=printImSummary(imSum)+"\n";
		
		final List<Integer> keyList=inImg.map(new Function<Tuple2<D3int,TImgBlock<float[]>>,Integer>() {
			@Override
			public Integer call(Tuple2<D3int,TImgBlock<float[]>> arg0) throws Exception {
				return arg0._1().z;
			}
		}).collect();
		for(Integer cVal: keyList) outString+=cVal+",";
		return outString;
	}
	public static String printImSummary(final Tuple3<Double,Double,Double> inData) {
		final double sumV=inData._1().doubleValue();
		final double sumV2=inData._2().doubleValue();
		final double cntV=inData._3().doubleValue();
		double meanV=(sumV/(1.0*cntV));
		return String.format("Mean:%3.2f\tSd:%3.2f\tSum:%3.0f",meanV,Math.sqrt((sumV2/(1.0*cntV)-Math.pow(meanV,2))),cntV);
	}
	protected static Tuple3<Double,Double,Double> countSlice(float[] workData) {
		double sumV=0;
		double sumV2=0;
		long cntV=0;
		for(float cVal: workData) {
				sumV+=cVal;
				sumV2+=Math.pow(cVal,2);
				cntV++;
		}
		return new Tuple3<Double,Double,Double>(new Double(sumV),new Double(sumV2),new Double(cntV));	
	}
	public static void main(String[] args) {
		ArgumentParser p=SparkGlobal.activeParser(args);
		final String imagePath=p.getOptionPath("path", "/Users/mader/Dropbox/TIPL/test/io_tests/rec8tiff", "Path of image (or directory) to read in");
		int range=p.getOptionInt("range", 1, "The range to use for the filter");
		//int maximumSlice=p.getOptionInt("maxs", maximumSlice, "The maximum slice to keep");
		
		p.checkForInvalid();
		JavaSparkContext jsc = SparkGlobal.getContext(SparkGlobal.getMasterName(),"IOTest");
		long start1=System.currentTimeMillis();
		DTImg<float[]> cImg=new DTImg<float[]>(jsc,imagePath,3);
		final FilterTest f=new FilterTest();
		JavaPairRDD<D3int,TImgBlock<float[]>> oImg=cImg.SpreadSlices(range).groupByKey(cImg.getDim().z).map(new PairFunction<Tuple2<D3int,List<TImgBlock<float[]>>>,D3int,TImgBlock<float[]>>() {
			@Override
			public Tuple2<D3int, TImgBlock<float[]>> call(
					Tuple2<D3int, List<TImgBlock<float[]>>> arg0)
					throws Exception {
				return f.GatherBlocks(arg0);
			}
			
		});
		
		System.out.println("Input Image\t# of Slices "+cImg.baseImg.count()+", "+ImageSummary(cImg.baseImg));
		System.out.println("After Filter\t# of Slices "+oImg.count()+", "+ImageSummary(oImg));
		
		DTImg<float[]> nImg=new DTImg<float[]>(cImg,oImg,3);
		
		nImg.DSave(new TypedPath(imagePath+"/badass"));
		
		System.out.println(String.format("Distributed Image\n\tVolume Fraction: \t\t%s\n\tCalculation time: \t%s","HAI",
				Duration.create(System.currentTimeMillis() - start1, TimeUnit.MILLISECONDS) ));
	}

	@Override
	public BaseTIPLPluginIn.filterKernel getKernel() {
		// TODO Auto-generated method stub
		return BaseTIPLPluginIn.gaussFilter(2.0);
	}

	@Override
	public BaseTIPLPluginIn.morphKernel getMKernel() {
		// TODO Auto-generated method stub
		return BaseTIPLPluginIn.fullKernel;
	}
	
	@Override
	public D3int getNeighborSize() {
		return new D3int(10,10,1);
	}


}
