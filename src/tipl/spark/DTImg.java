/**
 * 
 */
package tipl.spark;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.storage.StorageLevel;

import scala.Tuple2;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.TSliceWriter;
import tipl.util.ArgumentList.TypedPath;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgBlock;
import tipl.util.TImgTools;

/**
 * A distributed TImg based on a JavaRDD class and several subclass types I
 * intentionally used different variables for the generics to keep them straight
 * from each other particularly with the static functions and classes within
 * this method. TODO This should probably be made into it's own RDD type, but
 * right now I think this is easier
 * 
 * @author mader
 * 
 */
public class DTImg<T extends Cloneable> implements TImg, Serializable {
	/**
	 * An overloaded function for reading slices from images
	 * 
	 * @author mader
	 * 
	 * @param <W>
	 *            the type of the image as an array
	 */
	protected static class ReadSlice<W extends Cloneable> extends
			PairFunction<Integer, D3int, TImgBlock<W>> {
		protected final String imgPath;
		protected final int imgType;
		protected final D3int imgPos;
		protected final D3int sliceDim;

		/**
		 * The function for reading slices from an image
		 * 
		 * @param imgName
		 *            the path to the image
		 * @param inType
		 *            the type of image to be loaded (must match with W, convert
		 *            later)
		 * @param imPos
		 *            the starting position of the image
		 * @param imgDim
		 *            the dimensions of the image
		 */
		public ReadSlice(String imgName, int inType, final D3int imPos,
				final D3int imgDim) {
			this.imgPos = imPos;
			this.sliceDim = new D3int(imgDim.x, imgDim.y, 1);
			// this is important since spark instances do not know the current
			// working directory
			this.imgPath = (new File(imgName)).getAbsolutePath();
			this.imgType = inType;
		}

		@Override
		public Tuple2<D3int, TImgBlock<W>> call(Integer sliceNum) {
			final W cSlice = (W) TImgTools.ReadTImgSlice(imgPath,
					sliceNum.intValue(), imgType);
			final D3int cPos = new D3int(imgPos.x, imgPos.y, imgPos.z
					+ sliceNum);
			return new Tuple2<D3int, TImgBlock<W>>(cPos, new TImgBlock<W>(
					cSlice, cPos, sliceDim));
		}
	}
	/**
	 * Another version of the read slice code where the read itself is a future rather than upon creation
	 * 
	 * @author mader
	 * 
	 * @param <W>
	 *            the type of the image as an array
	 */
	protected static class ReadSlicePromise<W extends Cloneable> extends
			PairFunction<Integer, D3int, TImgBlock<W>> {
		protected final String imgPath;
		protected final int imgType;
		protected final D3int imgPos;
		protected final D3int sliceDim;

		/**
		 * The function for reading slices from an image
		 * 
		 * @param imgName
		 *            the path to the image
		 * @param inType
		 *            the type of image to be loaded (must match with W, convert
		 *            later)
		 * @param imPos
		 *            the starting position of the image
		 * @param imgDim
		 *            the dimensions of the image
		 */
		public ReadSlicePromise(String imgName, int inType, final D3int imPos,
				final D3int imgDim) {
			this.imgPos = imPos;
			this.sliceDim = new D3int(imgDim.x, imgDim.y, 1);
			// this is important since spark instances do not know the current
			// working directory
			this.imgPath = (new File(imgName)).getAbsolutePath();
			this.imgType = inType;
		}
		@Override
		public Tuple2<D3int, TImgBlock<W>> call(Integer sliceNum) {
			final D3int cPos = new D3int(imgPos.x, imgPos.y, imgPos.z
					+ sliceNum);
			return new Tuple2<D3int, TImgBlock<W>>(cPos, new TImgBlock.TImgBlockFile<W>(
					imgPath,sliceNum.intValue(),imgType, cPos, sliceDim,TImgBlock.zero));
		}
	}
	
	/**
	 * import an image from a path by reading line by line
	 * 
	 * @param jsc
	 * @param imgName
	 * @param imgType
	 * @return
	 */
	protected static <U extends Cloneable> JavaPairRDD<D3int, TImgBlock<U>> ImportImage(
			final JavaSparkContext jsc, final String imgName, final int imgType) {
		assert (TImgTools.isValidType(imgType));
		final TImgRO cImg = TImgTools.ReadTImg(imgName, false, true);
		final D3int imgDim = cImg.getDim();

		final List<Integer> l = new ArrayList<Integer>(imgDim.z);
		for (int i = 0; i < imgDim.z; i++) {
			l.add(i);
		}
		/**
		 * performance is much better when partition count matches slice count
		 * (or is at least larger than 2)
		 **/
		final int partitionCount = SparkGlobal.calculatePartitions(cImg.getDim().z);
		return jsc.parallelize(l, partitionCount)
				.map(new ReadSlicePromise<U>(imgName, imgType, cImg.getPos(), cImg
						.getDim()));
	}

	/**
	 * import an image from an existing TImgRO by reading in every slice (this
	 * is no manually done and singe core..)
	 * 
	 * @param jsc
	 * @param cImg
	 * @param imgType
	 * @return
	 */
	protected static <U extends Cloneable> JavaPairRDD<D3int, TImgBlock<U>> MigrateImage(
			final JavaSparkContext jsc, TImgRO cImg, final int imgType) {
		assert (TImgTools.isValidType(imgType));
		final D3int imgDim = cImg.getDim();
		final D3int imgPos = cImg.getPos();
		final D3int sliceDim = new D3int(imgDim.x, imgDim.y, 1);
		final List<Tuple2<D3int, TImgBlock<U>>> inSlices = new ArrayList<Tuple2<D3int, TImgBlock<U>>>(
				imgDim.z);
		for (int i = 0; i < imgDim.z; i++) {
			final D3int nPos = new D3int(imgPos.x, imgPos.y, imgPos.z + i);
			inSlices.add(new Tuple2<D3int, TImgBlock<U>>(nPos,
					new TImgBlock<U>((U) cImg.getPolyImage(i, imgType), nPos,
							sliceDim)));
		}
		return jsc.parallelizePairs(inSlices);
	}

	final int imageType;
	/** should be final but sometimes it changes **/
	protected JavaPairRDD<D3int, TImgBlock<T>> baseImg;
	public JavaPairRDD<D3int, TImgBlock<T>> getBaseImg() {
		return baseImg;
	}
	final String path;
	protected String procLog = "";

	protected float ssf = 1;
	protected D3int dim, pos, offset;
	protected D3float elsize;
	
	
	/**
	 * Everything is much easier with one unified constructor and then several factories which call it
	 * @param parent
	 * @param newImage
	 * @param imgType
	 * @param path
	 */
	protected DTImg(TImgTools.HasDimensions parent, JavaPairRDD<D3int, TImgBlock<T>> newImage,
			int imgType,String path) {
		this.baseImg = newImage;
		this.imageType = imgType;
		TImgTools.mirrorImage(parent, this);
		this.path = path;
		SparkGlobal.assertPersistance(this);
	}
	
	/** factory for wrapping RDDs into DTImg classes
	 * 
	 * @param parent
	 * @param newImage
	 * @param imgType
	 * @return
	 */
	static public <Fc extends Cloneable> DTImg<Fc> WrapRDD(TImgRO parent, JavaPairRDD<D3int, TImgBlock<Fc>> newImage,
			int imgType) {
		DTImg<Fc> outImage=new DTImg<Fc>(parent,newImage,imgType,"[virtual]");
		return outImage;
	}
	/**
	 * factory create a new image from a javasparkcontext and a path and type
	 * 
	 * @param jsc
	 * @param imgName
	 * @param imgType
	 */
	static public <Fc extends Cloneable> DTImg<Fc> ReadImage(JavaSparkContext jsc, final String imgName, int imgType) {
		JavaPairRDD<D3int, TImgBlock<Fc>> newImage = ImportImage(jsc, imgName, imgType);
		TImgTools.HasDimensions parent = TImgTools.ReadTImg(imgName);
		DTImg<Fc> outImage=new DTImg<Fc>(parent,newImage,imgType,imgName);
		return outImage;
	}
	
	static public <Fc extends Cloneable> DTImg<Fc> ReadObjectFile(JavaSparkContext jsc, final String imgName, int imgType) {
		final JavaRDD<TImgBlock<Fc>> newImage=jsc.objectFile(imgName);
		final TImgBlock<Fc> cBlock=newImage.first();
		JavaPairRDD<D3int,TImgBlock<Fc>> baseImg=newImage.map(new PairFunction<TImgBlock<Fc>,D3int,TImgBlock<Fc>>() {

			@Override
			public Tuple2<D3int, TImgBlock<Fc>> call(final TImgBlock<Fc> arg0)
					throws Exception {
				return new Tuple2<D3int, TImgBlock<Fc>>(arg0.getPos(),arg0);
			}
			
		});
		//TODO this assumes slices for normal data you will need to prowl the whole thing
		return new DTImg<Fc>(new TImgTools.HasDimensions() {
			final D3int pos=cBlock.getPos();
			final D3int dim=new D3int(cBlock.getDim().x,cBlock.getDim().y,(int) newImage.count());
			@Override
			public D3int getDim() {return dim;}

			@Override
			public D3float getElSize() {
				return new D3float(1.0f);
			}

			@Override
			public D3int getOffset() {return new D3int(0,0,0);}

			@Override
			public D3int getPos() {return pos;}

			@Override
			public String getProcLog() { return "";}

			@Override
			public float getShortScaleFactor() {return 1;}
			
		},baseImg,imgType,imgName);
		
		
	}

	@Override
	public String appendProcLog(String inData) {
		procLog += "\n" + inData;
		return procLog;
	}

	/**
	 * Save the image in parallel using a TSliceWriter
	 * 
	 * @param path
	 */
	public void DSave(TypedPath path) {
		final TypedPath absTP = new TypedPath(
				(new File(path.getPath())).getAbsolutePath(), path.getType());
		final TSliceWriter cWriter = TSliceWriter.Writers.ChooseBest(this,
				absTP, imageType);
		baseImg.foreach(new VoidFunction<Tuple2<D3int, TImgBlock<T>>>() {

			@Override
			public void call(Tuple2<D3int, TImgBlock<T>> arg0) throws Exception {

				cWriter.WriteSlice(arg0._2(), arg0._1().z);

			}

		});
	}
	/**
	 * Save the image in parallel using the built in serializer, it first transforms the image into a list of blocks rather than the pair
	 * @param path
	 */
	public void HSave(String path) {
		final String outpath=(new File(path)).getAbsolutePath();
		this.baseImg.setName(path).map(new Function<Tuple2<D3int,TImgBlock<T>>,TImgBlock<T>>() {
			@Override
			public TImgBlock<T> call(final Tuple2<D3int, TImgBlock<T>> arg0)
					throws Exception {
				return arg0._2;
			}
		}).saveAsObjectFile(outpath);
	}

	@Override
	public boolean getCompression() {
		return false;
	}

	@Override
	public D3int getDim() {
		return dim;
	}

	@Override
	public D3float getElSize() {
		return elsize;
	}

	@Override
	public int getImageType() {
		return imageType;
	}

	@Override
	public D3int getOffset() {
		return offset;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public Object getPolyImage(int sliceNumber, int asType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public D3int getPos() {
		return pos;
	}

	@Override
	public String getProcLog() {
		return procLog;
	}

	@Override
	public String getSampleName() {
		return path;
	}

	@Override
	public float getShortScaleFactor() {
		return ssf;
	}

	@Override
	public boolean getSigned() {
		return true;
	}

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
		final JavaSparkContext cJsc = SparkGlobal.getContext();
		final JavaPairRDD<D3int, TImgBlock<T>> oldImage = MigrateImage(cJsc,
				inAim, getImageType());
		return DTImg.<T>WrapRDD(this, oldImage, getImageType());
	}

	@Override
	public boolean InitializeImage(D3int dPos, D3int cDim, D3int dOffset,
			D3float elSize, int imageType) {
		throw new IllegalArgumentException("Not Implemented");
	}

	@Override
	public int isFast() {
		return 1;
	}

	@Override
	public boolean isGood() {
		return true;
	}

	/**
	 * passes basically directly through to the JavaPair rdd but it wraps
	 * everything in a DTImg class
	 * 
	 * @param mapFunc
	 * @return
	 */
	public <U extends Cloneable> DTImg<U> map(
			final PairFunction<Tuple2<D3int, TImgBlock<T>>, D3int, TImgBlock<U>> mapFunc,
			final int outType) {
		return DTImg.<U>WrapRDD(this, this.baseImg.map(mapFunc), outType);
	}
	/**
	 * Performs a subselection (a function filter) of the dataset based on the blocks
	 * @param filtFunc the function to filter with
	 * @return a subselection of the image
	 */
	public  DTImg<T> subselect(
			final Function<Tuple2<D3int,TImgBlock<T>>,Boolean> filtFunc
			) {
		final JavaPairRDD<D3int,TImgBlock<T>> subImg=this.baseImg.filter(filtFunc);
		DTImg<T> outImage=DTImg.WrapRDD(this,subImg , this.getImageType());
		//TODO Only works on slices
		int sliceCount=(int) subImg.count();
		outImage.setDim(new D3int(outImage.getDim().x,outImage.getDim().y,sliceCount));
		outImage.setPos(subImg.first()._1);
		return outImage;
	}
	/**
	 * The same as subselect but takes a function which operates on just the positions instead (needs to be erased because of strange type erasure behavior)
	 * @param filtFunc
	 * @return
	 */
	public  DTImg<T> subselectPos(
			final Function<D3int,Boolean> filtFunc
			) {
		return subselect(new Function<Tuple2<D3int,TImgBlock<T>>,Boolean>() {

			@Override
			public Boolean call(Tuple2<D3int, TImgBlock<T>> arg0)
					throws Exception {
				return filtFunc.call(arg0._1);
			}
			
		});
	}
	
	
	
	/**
	 * first spreads the slices out, then runs a group by key, then applies the given mapfunction and creates a new DTImg that wraps around the object
	 * @param spreadWidth number of slices to spread out over
	 * @param mapFunc
	 * @return
	 */
	public <U extends Cloneable> DTImg<U> spreadMap(
			final int spreadWidth,
			final PairFunction<Tuple2<D3int, List<TImgBlock<T>>>, D3int, TImgBlock<U>> mapFunc,
			final int outType) {
		return DTImg.<U>WrapRDD(this, this.spreadSlices(spreadWidth).groupByKey(getPartitions()).map(mapFunc), outType);
	}
	
	public void showPartitions() {
		//this.baseImg.mapPartition()
		List<String> curPartitions=this.baseImg.mapPartitions(new FlatMapFunction<Iterator<Tuple2<D3int,TImgBlock<T>>>,String>() {

			@Override
			public Iterable<String> call(
					Iterator<Tuple2<D3int, TImgBlock<T>>> arg0)
					throws Exception {
				List<String> outList=new LinkedList<String>();
				outList.add("\nPartition:");
				while (arg0.hasNext()) outList.add(""+arg0.next()._1.z);
				return outList;
			}
			
		}).collect();
		String partStr="";

		for(String cPartition : curPartitions) partStr+=", "+cPartition;
		System.out.println("Partitions=>"+partStr);
	}
	public void setRDDName(String cName) {
		this.baseImg.setName(cName);
	}
	public Partitioner getPartitioner() {
		return new Partitioner() {
			final int slCnt=getDim().z;
			final int ptCnt=SparkGlobal.calculatePartitions(getDim().z);
			final int slPpt=SparkGlobal.getSlicesPerCore();
			@Override
			public int getPartition(Object arg0) {
				D3int curPos=((Tuple2<D3int,TImgBlock<T>>) arg0)._1;
				return (int) Math.floor(curPos.z*1.0/slPpt);
			}

			@Override
			public int numPartitions() {
				return ptCnt;
			}
			
		};
	}
	/**
	 * the number of partitions to use when breaking up data
	 * @return partition count
	 */
	public int getPartitions() {
		return  SparkGlobal.calculatePartitions(getDim().z);
	}
	public void cache() {
		this.baseImg=this.baseImg.cache();
	}
	
	/**
	 * Switches the JavaRDD to memory on the disk
	 */
	public void persistToDisk() {
		persist(StorageLevel.MEMORY_AND_DISK());
	}
	/**
	 * Set the persistence level of the image
	 * @param setLevel the level from the storagelevel class
	 */
	public void persist(StorageLevel setLevel) {
		this.baseImg.persist(setLevel);
	}

	/**
	 * Switches the JavaRDD to only the disk
	 */
	public void persistToDiskOnly() {
		persist(StorageLevel.DISK_ONLY());
	}

	@Override
	public void setCompression(boolean inData) {
		throw new IllegalArgumentException("Not Implemented");
	}

	@Override
	public void setDim(D3int inData) {
		dim = inData;
	}

	@Override
	public void setElSize(D3float inData) {
		elsize = inData;
	}

	@Override
	public void setImageType(int inData) {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("Cannot set Imagetype");
	}

	@Override
	public void setOffset(D3int inData) {
		offset = inData;
	}

	@Override
	public void setPos(D3int inData) {
		pos = inData;
	}

	@Override
	public void setShortScaleFactor(float issf) {
		ssf = issf;
	}

	@Override
	public void setSigned(boolean inData) {
		throw new IllegalArgumentException("Not Implemented");
	}

	// Here are the specialty functions for DTImages
	/**
	 * Spread blocks out over a given range
	 * @param blockDimension the size of the blocks to distribute
	 * @param offsetList the offsets of the starting position of the blocks
	 * @return
	 */
	public JavaPairRDD<D3int, TImgBlock<T>> spreadBlocks(final D3int blockDimension,final D3int[] offsetList) {
		return baseImg.flatMap(new BlockSpreader(offsetList,blockDimension,getDim()));
	}
	/** 
	 * A class for spreading out blocks
	 * @author mader
	 *
	 * @param <T>
	 */
	protected static class BlockSpreader<T extends Cloneable> extends PairFlatMapFunction<Tuple2<D3int, TImgBlock<T>>, D3int, TImgBlock<T>> {
		
		protected final D3int[] inOffsetList;
		protected final D3int blockDimension;
		protected final D3int imgDim;
		// Since we can't have constructors here (probably should make it into a subclass)
		public BlockSpreader(D3int[] inOffsetList,D3int blockDimension,D3int imgDim){
			this.inOffsetList=inOffsetList;
			//this.inOffsetList=new D3int[inOffsetList.length];
			//for(int i=0;i<inOffsetList.length;i++) this.inOffsetList[i]=new D3int(inOffsetList[i]);
			//System.arraycopy(inOffsetList, 0, this.inOffsetList, 0, inOffsetList.length);
			this.blockDimension=blockDimension;
			this.imgDim=imgDim;
			
		}
		@Override
		public Iterable<Tuple2<D3int, TImgBlock<T>>> call(
				Tuple2<D3int, TImgBlock<T>> inData) {
			final TImgBlock<T> inSlice = inData._2();
			final List<Tuple2<D3int, TImgBlock<T>>> outList = new ArrayList<Tuple2<D3int, TImgBlock<T>>>(inOffsetList.length);
			for (final D3int nOffset : this.inOffsetList) {
				final D3int oPos = inData._1();
				final D3int nPos = new D3int(oPos.x+nOffset.x, oPos.y+nOffset.y, oPos.z+nOffset.z);
				/**
				 * the clone is used otherwise it loses slices when
				 * they drift between machines (I think)
				 */
				if (nPos.z >= 0 & nPos.z < imgDim.z)
					outList.add(new Tuple2<D3int, TImgBlock<T>>(
							nPos, new TImgBlock<T>(inSlice
									.getClone(), nPos, blockDimension,
									nOffset)));
			}
			return outList;
		}
	}
	/**
	 * Spread out image over slices
	 * 
	 * @param windowSize
	 *            range above and below to spread
	 * @return
	 */
	public JavaPairRDD<D3int, TImgBlock<T>> spreadSlices(final int windowSize) {
		final D3int sliceDim = new D3int(getDim().x, getDim().y, 1);
		
		final D3int[] offsetList=new D3int[2*windowSize+1];
		for(int i=-windowSize;i<=windowSize;i++) offsetList[i+windowSize]=new D3int(0,0,i);
		
		return spreadBlocks(sliceDim,offsetList);
		
	}

}
