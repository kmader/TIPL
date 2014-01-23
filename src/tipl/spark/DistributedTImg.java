/**
 * 
 */
package tipl.spark;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.Function;

import scala.Tuple2;
import tipl.formats.TImgRO;
import tipl.util.TImgTools;

/**
 * @author mader
 *
 */
public class DistributedTImg<T> {
	
	protected static class ReadSlice<W> extends Function<Integer,TImgSlice<W>> {
		protected final String imgPath;
		protected final int imgType;
		public ReadSlice(String imgName,int inType) {
			
			imgPath=(new File(imgName)).getAbsolutePath();
			imgType=inType;
		}
		@Override
		public TImgSlice<W> call(Integer sliceNum) {
			final W cSlice= (W) TImgTools.ReadTImgSlice(imgPath,sliceNum.intValue(),imgType);
			return new TImgSlice<W>(sliceNum,cSlice);
		}
	}
	
	protected static <U> JavaRDD<TImgSlice<U>> ImportImage(final JavaSparkContext jsc, final String imgName,final int imgType) {
		assert(TImgTools.isValidType(imgType));
		final TImgRO cImg=TImgTools.ReadTImg(imgName,false,true);
		final int sliceCount=cImg.getDim().z;
		
		List<Integer> l = new ArrayList<Integer>(sliceCount);
		for (int i = 0; i < sliceCount; i++) {
			l.add(i);
		}
		return jsc.parallelize(l).map(new ReadSlice<U>(imgName,imgType));
	}
	static public class TImgSlice<V> {
		private int slice;
		private int offset;
		private V sliceData;
		public TImgSlice(Integer sliceNumber,V cSlice) {
			sliceData=cSlice;
			slice=sliceNumber.intValue();
			offset=0;
		}
	}
	
}
