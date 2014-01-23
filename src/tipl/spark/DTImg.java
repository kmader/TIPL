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
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.Function;

import scala.Tuple2;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgBlock;
import tipl.util.TImgTools;

/**
 * A distributed TImg based on a JavaRDD class and several subclass types
 * I intentionally used different variables for the generics to keep them straight from each other
 * particularly with the static functions and classes within this method. 
 * TODO This should probably be made into it's own RDD type, but right now I think this is easier
 * @author mader
 *
 */
public class DTImg<T extends Cloneable> implements TImg {
	final protected JavaSparkContext cJsc;
	final int imageType;
	final JavaRDD<TImgBlock<T>> baseImg;
	final String path;
	
	
	
	/**
	 * create a new image from a javasparkcontext and a path and type
	 * @param jsc
	 * @param imgName
	 * @param imgType
	 */
	public DTImg(JavaSparkContext jsc,final String imgName,int imgType) {
		baseImg=ImportImage(jsc,imgName,imgType);
		imageType=imgType;
		TImgTools.mirrorImage(TImgTools.ReadTImg(imgName), this);
		path=imgName;
		cJsc=jsc;
	}
	public DTImg(JavaSparkContext jsc,TImgRO parent,JavaRDD<TImgBlock<T>> newImage,int imgType) {
		baseImg=newImage;
		imageType=imgType;
		TImgTools.mirrorImage(parent, this);
		path="[virtual]";
		cJsc=jsc;
	}
	
	// Here are the specialty functions for DTImages
	/**
	 * Spread out an image over a range (useful for neigbhorhood operations)
	 * 
	 * @param windowSize range above and below to spread
	 * @return
	 */
	public JavaRDD<TImgBlock<T>> SpreadSlices(final int windowSize) {
		final D3int sliceDim=new D3int(getDim().x,getDim().y,1);
		return baseImg.flatMap(
				  new FlatMapFunction<TImgBlock<T>,TImgBlock<T>>() {
				    public Iterable<TImgBlock<T>> call(TImgBlock<T> inSlice) {
				    	List<TImgBlock<T>> outList = new ArrayList<TImgBlock<T>>(2*windowSize+1);
						for (int i = -windowSize; i <= windowSize; i++) {
							D3int oPos=inSlice.getPos();
							D3int nPos=new D3int(oPos.x,oPos.y,oPos.z+i);
							D3int nOffset=new D3int(0,0,i);
							/** the clone is used otherwise it loses slices when they drift between 
							 * machines (I think)
							 */
							if (nPos.z>0 & nPos.z<getDim().z) 
								outList.add(new TImgBlock<T>(inSlice.getClone(),nPos,sliceDim,nOffset));
						}
						return outList;
				    }
				  });
	}
	/**
	 * An overloaded function for reading slices from images
	 * @author mader
	 *
	 * @param <W> the type of the image as an array
	 */
	protected static class ReadSlice<W extends Cloneable> extends Function<Integer,TImgBlock<W>> {
		protected final String imgPath;
		protected final int imgType;
		protected final D3int imgPos;
		protected final D3int sliceDim;
		/**
		 * The function for reading slices from an image
		 * @param imgName the path to the image
		 * @param inType the type of image to be loaded (must match with W, convert later)
		 * @param imPos the starting position of the image
		 * @param imgDim the dimensions of the image
		 */
		public ReadSlice(String imgName,int inType,final D3int imPos,final D3int imgDim) {
			this.imgPos=imPos;
			this.sliceDim=new D3int(imgDim.x,imgDim.y,1);
			this.imgPath=(new File(imgName)).getAbsolutePath();
			this.imgType=inType;
		}
		@Override
		public TImgBlock<W> call(Integer sliceNum) {
			final W cSlice = (W) TImgTools.ReadTImgSlice(imgPath,sliceNum.intValue(),imgType);
			return new TImgBlock<W>(cSlice,new D3int(imgPos.x,imgPos.y,imgPos.z+sliceNum),sliceDim);
		}
	}
	/**
	 * import an image from a path by reading line by line
	 * @param jsc
	 * @param imgName
	 * @param imgType
	 * @return
	 */
	protected static <U extends Cloneable> JavaRDD<TImgBlock<U>> ImportImage(final JavaSparkContext jsc, final String imgName,final int imgType) {
		assert(TImgTools.isValidType(imgType));
		final TImgRO cImg=TImgTools.ReadTImg(imgName,false,true);
		final D3int imgDim=cImg.getDim();
		
		List<Integer> l = new ArrayList<Integer>(imgDim.z);
		for (int i = 0; i < imgDim.z; i++) {
			l.add(i);
		}
		return jsc.parallelize(l).map(new ReadSlice<U>(imgName,imgType,cImg.getPos(),cImg.getDim()));
	}
	/**
	 * import an image from an existing TImgRO by reading in every slice (this is no manually done and singe core..)
	 * 
	 * @param jsc
	 * @param cImg
	 * @param imgType
	 * @return
	 */
	protected static <U extends Cloneable> JavaRDD<TImgBlock<U>> MigrateImage(final JavaSparkContext jsc, TImgRO cImg,final int imgType) {
		assert(TImgTools.isValidType(imgType));
		final D3int imgDim=cImg.getDim();
		final D3int imgPos=cImg.getPos();
		final D3int sliceDim=new D3int(imgDim.x,imgDim.y,1);
		List<TImgBlock<U>> inSlices = new ArrayList<TImgBlock<U>>(imgDim.z);
		for (int i = 0; i < imgDim.z; i++) {
			D3int nPos=new D3int(imgPos.x,imgPos.y,imgPos.z+i);
			inSlices.add(new TImgBlock<U>((U) cImg.getPolyImage(i,imgType),nPos,sliceDim));
		}
		return jsc.parallelize(inSlices);
	}
	
	protected String procLog="";
	@Override
	public String appendProcLog(String inData) {
		procLog+="\n"+inData;
		return procLog;
	}

	@Override
	public boolean getCompression() {return false;}



	@Override
	public int getImageType() {return imageType;}



	@Override
	public String getPath() {return path;}



	@Override
	public Object getPolyImage(int sliceNumber, int asType) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public String getSampleName() {return path;}

	protected float ssf=1;

	@Override
	public float getShortScaleFactor() {return ssf;}



	@Override
	public int isFast() {return 1;}

	protected D3int dim,pos,offset;
	protected D3float elsize;

	@Override
	public D3int getDim() {return dim;}

	@Override
	public D3float getElSize() {return elsize;}



	@Override
	public D3int getOffset() {return offset;}



	@Override
	public D3int getPos() {return pos;}



	@Override
	public String getProcLog() {return procLog;}

	@Override
	public TImg inheritedAim(boolean[] imgArray, D3int dim, D3int offset) {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("Not Implemented");
	}

	@Override
	public TImg inheritedAim(char[] imgArray, D3int dim, D3int offset) {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("Not Implemented");
	}



	@Override
	public TImg inheritedAim(float[] imgArray, D3int dim, D3int offset) {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("Not Implemented");
	}



	@Override
	public TImg inheritedAim(int[] imgArray, D3int dim, D3int offset) {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("Not Implemented");
	}



	@Override
	public TImg inheritedAim(short[] imgArray, D3int dim, D3int offset) {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("Not Implemented");
	}



	@Override
	public TImg inheritedAim(TImgRO inAim) {
		JavaRDD<TImgBlock<T>> oldImage=MigrateImage(cJsc,inAim,getImageType());
		return new DTImg<T>(cJsc,this,oldImage,getImageType());
	}



	@Override
	public void setDim(D3int inData) { dim=inData;}



	@Override
	public void setElSize(D3float inData) {elsize=inData;}



	@Override
	public void setOffset(D3int inData) {offset=inData;}



	@Override
	public void setPos(D3int inData) {pos=inData;}



	@Override
	public boolean getSigned() {return false;}



	@Override
	public boolean InitializeImage(D3int dPos, D3int cDim, D3int dOffset,
			D3float elSize, int imageType) {
		throw new IllegalArgumentException("Not Implemented");
	}

	@Override
	public boolean isGood() {return true;}

	@Override
	public void setCompression(boolean inData) {throw new IllegalArgumentException("Not Implemented");}


	@Override
	public void setImageType(int inData) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setShortScaleFactor(float issf) {ssf=issf;}

	@Override
	public void setSigned(boolean inData) {throw new IllegalArgumentException("Not Implemented");}
	
}