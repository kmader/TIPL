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
import tipl.util.TImgTools;

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
		//int range=p.getOptionInt("range", 1, "The range to use for the filter");
		final float threshold=p.getOptionFloat("threshold", -1, "Threshold Value");
		final FilterTest f=new FilterTest();
		p=f.setParameter(p);
		
		//int maximumSlice=p.getOptionInt("maxs", maximumSlice, "The maximum slice to keep");
		
		p.checkForInvalid();
		
		JavaSparkContext jsc = SparkGlobal.getContext(SparkGlobal.getMasterName(),"FilterTest");
		long start1=System.currentTimeMillis();
		DTImg<float[]> cImg=new DTImg<float[]>(jsc,imagePath,TImgTools.IMAGETYPE_FLOAT);
		
		final int keyCount=cImg.getDim().z;
		DTImg<float[]> filtImg=cImg.spreadMap(f.getNeighborSize().z, new PairFunction<Tuple2<D3int,List<TImgBlock<float[]>>>,D3int,TImgBlock<float[]>>() {
			@Override
			public Tuple2<D3int, TImgBlock<float[]>> call(
					Tuple2<D3int, List<TImgBlock<float[]>>> arg0)
					throws Exception {
				return f.GatherBlocks(arg0);
			}
		}, TImgTools.IMAGETYPE_FLOAT);
		
		filtImg.DSave(new TypedPath(imagePath+"/badass"));
		
		DTImg<boolean[]> thOutImg=filtImg.map(new PairFunction<Tuple2<D3int, TImgBlock<float[]>>,D3int,TImgBlock<boolean[]>>() {

			@Override
			public Tuple2<D3int, TImgBlock<boolean[]>> call(
					Tuple2<D3int, TImgBlock<float[]>> arg0) throws Exception {
				final TImgBlock<float[]> inBlock=arg0._2;
				final float[] cSlice=inBlock.get();
				final boolean[] oSlice=new boolean[cSlice.length];
				for(int i=0;i<oSlice.length;i++) oSlice[i]=cSlice[i]>threshold;
				return  new Tuple2<D3int, TImgBlock<boolean[]>>(arg0._1,
						new TImgBlock<boolean[]>(oSlice,inBlock.getPos(),inBlock.getDim()));
			}
			
		},TImgTools.IMAGETYPE_BOOL);
		
		thOutImg.DSave(new TypedPath(imagePath+"/threshold"));
		
		System.out.println(String.format("Distributed Image\n\tVolume Fraction: \t\t%s\n\tCalculation time: \t%s","HAI",
				Duration.create(System.currentTimeMillis() - start1, TimeUnit.MILLISECONDS) ));
	}

	@Override
	public BaseTIPLPluginIn.filterKernel getKernel() {
		return BaseTIPLPluginIn.gaussFilter(5.0);
	}

	@Override
	public BaseTIPLPluginIn.morphKernel getMKernel() {
		return BaseTIPLPluginIn.fullKernel;
	}
	@Override
	public String getPluginName() {
		// TODO Auto-generated method stub
		return FilterTest.class.getCanonicalName();
	}


	protected D3int nsize=new D3int(1,1,1);
	@Override
	public ArgumentParser setParameter(ArgumentParser p, String prefix) {
		// TODO Auto-generated method stub
		nsize=p.getOptionD3int(prefix+"filtersize", nsize, "The size of the neighborhood for the filter");
		return p;
	}
	
	@Override
	public D3int getNeighborSize() {
		return nsize;
	}
	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
		return false;
	}
	


}
