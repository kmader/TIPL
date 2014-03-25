package tipl.spark;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.spark.AccumulableParam;
import org.apache.spark.Accumulator;
import org.apache.spark.Partition;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;
import scala.Tuple3;
import scala.concurrent.duration.Duration;
import tipl.formats.TImgRO;
import tipl.tests.TestPosFunctions;
import tipl.tools.BaseTIPLPluginIn;
import tipl.util.ArgumentList.TypedPath;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TIPLGlobal;
import tipl.util.TImgBlock;
import tipl.util.TImgTools;
/**
 * FilterTest performs a gaussian filtering operation on whatever image is given as input, will not resize.
 * @author mader
 *
 */
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
		int boxSize=p.getOptionInt("boxsize", 8, "The dimension of the image used for the analysis");

		final TImgRO testImg = TestPosFunctions.wrapIt(boxSize,
				new TestPosFunctions.SphericalLayeredImage(boxSize/2, boxSize/2, boxSize/2, 0, 1, 2));

		int iters=p.getOptionInt("iters", 1, "The number of iterations to use for the filter");
		final float threshold=p.getOptionFloat("threshold", -1, "Threshold Value");

		final FilterTest f=new FilterTest();
		p=f.setParameter(p);

		int maximumSlice=p.getOptionInt("maxs", -1, "The maximum slice to keep");

		p.checkForInvalid();

		JavaSparkContext jsc = SparkGlobal.getContext("FilterTest");
		long start1=System.currentTimeMillis();
		DTImg<float[]> cImg;
		if(imagePath.length()>0) cImg=DTImg.<float[]>ReadImage(jsc,imagePath,TImgTools.IMAGETYPE_FLOAT);
		else cImg = DTImg.<float[]>ConvertTImg(jsc, testImg, TImgTools.IMAGETYPE_FLOAT);


		cImg.setRDDName("Loading Data");

		DTImg<float[]> filtImg;
		if (maximumSlice>0) {
			final int lastSlice=cImg.getPos().z+maximumSlice;
			filtImg=cImg.subselectPos(new Function<D3int,Boolean>() {
				@Override
				public Boolean call(D3int arg0) throws Exception {
					return arg0.z<lastSlice;
				}

			});
		} else filtImg=cImg;
		cImg.showPartitions();

		final Accumulator<Double> timeElapsed=filtImg.getContext().accumulator(new Double(0));
		final Accumulator<Integer> mapOperations=filtImg.getContext().accumulator(new Integer(0));
		filtImg.setRDDName("Running Filter");

		for(int ic=0;ic<iters;ic++) {
			filtImg=filtImg.spreadMap(f.getNeighborSize().z, new PairFunction<Tuple2<D3int,List<TImgBlock<float[]>>>,D3int,TImgBlock<float[]>>() {
				@Override
				public Tuple2<D3int, TImgBlock<float[]>> call(
						Tuple2<D3int, List<TImgBlock<float[]>>> arg0)
								throws Exception {
					long start=System.currentTimeMillis();
					Tuple2<D3int, TImgBlock<float[]>> outVal=f.GatherBlocks(arg0);
					// add execution time
					timeElapsed.$plus$eq(new Double(System.currentTimeMillis() - start));
					mapOperations.$plus$eq(1);
					return outVal;

				}
			}, TImgTools.IMAGETYPE_FLOAT);
			filtImg.setRDDName("Filter:Iter"+ic);
			filtImg.showPartitions();
		}

		if (imagePath.length()>0) {
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
		} else {
			filtImg.getBaseImg().sample(true, 2/filtImg.getDim().z,1);
			//filtImg.getBaseImg().collect();
		}

		double runTime=System.currentTimeMillis() - start1;
		double mapTime=timeElapsed.value();
		int mapOps=mapOperations.value();
		String outPrefix="SP_OUT,\t"+SparkGlobal.getMasterName()+",\t"+iters+",\t"+filtImg.getDim().x+","+filtImg.getDim().y+","+filtImg.getDim().z;
		  System.out.println(String.format("SP_OUT,\tImage Filter,\tTotal,\t\t\tPer Iteration\n"
		 
				+ "SP_OUT,\tMapTime Time, \t%f,\t%f\n"
				+ "SP_OUT,\tScript time, \t%f\t%f\n"
				+ "SP_OUT,\tEfficiency, \t%f pct,",
				Math.round(mapTime*10.)/10.,
				Math.round(mapTime/iters*10.)/10.,
				Math.round(runTime*10.)/10.,
				Math.round(runTime/iters*10.)/10.,
				Math.round(mapTime/runTime*1000.)/1000.));
		// for reading in with other tools
		System.out.println("CSV_OUT,"+SparkGlobal.getMasterName()+","+iters+","+
		filtImg.getDim().x+","+filtImg.getDim().y+","+filtImg.getDim().z+","+mapTime+","+
				runTime+","+mapOps+","+SparkGlobal.maxCores+","+SparkGlobal.getSparkPersistenceValue()+","+SparkGlobal.useCompression);
		jsc.stop();
	}

	@Override
	public BaseTIPLPluginIn.filterKernel getImageKernel() {
		return BaseTIPLPluginIn.gaussFilter(1.0);
	}

	@Override
	public String getPluginName() {
		// TODO Auto-generated method stub
		return FilterTest.class.getCanonicalName();
	}

	@Override
	public ArgumentParser setParameter(ArgumentParser p, String prefix) {
		super.setParameter(p,prefix);
		return p;
	}

	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
		return false;
	}



}
