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
import scala.Tuple3;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.TSliceWriter;
import tipl.util.ArgumentList.TypedPath;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TIPLGlobal;
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
@SuppressWarnings("serial")
public class DTImg<T extends Cloneable> implements TImg, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1824695496632428954L;
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
		this.baseImg = newImage;//.partitionBy(SparkGlobal.getPartitioner(getDim()));
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
	/**
	 * Produce a new DTImg from an existing TImgRO object
	 * @param jsc the JavaSparkContext
	 * @param inImage the input inmage to convert
	 * @param imgType the type of the image (must match Fc)
	 * @return
	 */
	static public <Fc extends Cloneable> DTImg<Fc> ConvertTImg(JavaSparkContext jsc, final TImgRO inImage, int imgType) {
		JavaPairRDD<D3int, TImgBlock<Fc>> newImage = MigrateImage(jsc, inImage, imgType);
		return new DTImg<Fc>(inImage,newImage,imgType,inImage.getPath());
	}
	
	static public <Fc extends Cloneable> DTImg<Fc> ReadObjectFile(JavaSparkContext jsc, final String imgName, int imgType) {
		final JavaRDD<Tuple2<D3int,TImgBlock<Fc>>> newImage=jsc.objectFile(imgName);
		final Tuple2<D3int,TImgBlock<Fc>> cTuple=newImage.first();
		final TImgBlock<Fc> cBlock=cTuple._2();
		JavaPairRDD<D3int,TImgBlock<Fc>> baseImg=newImage.map(new PairFunction<Tuple2<D3int,TImgBlock<Fc>>,D3int,TImgBlock<Fc>>() {
			@Override
			public Tuple2<D3int, TImgBlock<Fc>> call(final Tuple2<D3int,TImgBlock<Fc>> arg0)
					throws Exception {
				return arg0;
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
		this.baseImg.setName(path).rdd().saveAsObjectFile(outpath);
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
	 * passes basically directly through to the JavaPair RDD but it wraps
	 * everything in a DTImg class. Automatically partition
	 * 
	 * @param mapFunc
	 * @return
	 */
	public <U extends Cloneable> DTImg<U> map(
			final PairFunction<Tuple2<D3int, TImgBlock<T>>, D3int, TImgBlock<U>> mapFunc,
			final int outType) {
		return DTImg.<U>WrapRDD(this, this.baseImg.map(mapFunc).partitionBy(SparkGlobal.getPartitioner(getDim())), outType);
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
		
		JavaPairRDD<D3int, List<TImgBlock<T>>> joinImg;
		joinImg=this.spreadSlices(spreadWidth).
				groupByKey(getPartitions()).
				partitionBy(SparkGlobal.getPartitioner(getDim()));
		/**
		final JavaPairRDD<D3int,TImgBlock<T>> savedBase=getBaseImg();
		final D3int imSize=getDim();
		final D3int ipos=getPos();
		savedBase.context().broadcast(savedBase);
		joinImg=savedBase.map(new PairFunction<Tuple2<D3int, TImgBlock<T>>, D3int, List<TImgBlock<T>>>() {
		
			@Override
			public Tuple2<D3int, List<TImgBlock<T>>> call(
					Tuple2<D3int, TImgBlock<T>> arg0) throws Exception {
				List<TImgBlock<T>> outList=new ArrayList<TImgBlock<T>>(2*spreadWidth+1);
				outList.add(arg0._2());
				
				for(int i=-spreadWidth;i<=spreadWidth;i++) {
					if(i!=0) {
						D3int lookupPos=new D3int(ipos.x,ipos.y,ipos.z+i);
						List<TImgBlock<T>> tempList=savedBase.lookup(lookupPos);
						if(TIPLGlobal.getDebug()) System.out.println("Lookup:"+lookupPos+", "+tempList.size());
						outList.addAll(tempList);
					}
				}
				return new Tuple2<D3int, List<TImgBlock<T>>>(arg0._1,outList);
			}
			
		});
		**/
		return DTImg.<U>WrapRDD(this, joinImg.
				map(mapFunc), outType); 
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

	/**
	 * the number of partitions to use when breaking up data
	 * @return partition count
	 */
	public int getPartitions() {
		return  SparkGlobal.calculatePartitions(getDim().z);
	}
	/**
	 * Set the persistence level of the image only if none has been set so far.
	 * @param setLevel the level from the storagelevel class
	 */
	public void persist(StorageLevel setLevel) {
		if(this.baseImg.getStorageLevel()==StorageLevel.NONE()) this.baseImg.persist(setLevel);
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
	public JavaPairRDD<D3int,List<TImgBlock<T>>>  spreadSlices3(int windSize) {
		JavaPairRDD<D3int, TImgBlock<T>> down1=baseImg.map(new BlockShifter(new D3int(0,0,-1)));
		JavaPairRDD<D3int, TImgBlock<T>> up1=baseImg.map(new BlockShifter(new D3int(0,0,1)));
		JavaPairRDD<D3int,Tuple3<List<TImgBlock<T>>,List<TImgBlock<T>>,List<TImgBlock<T>>>> joinImg=baseImg.cogroup(down1, up1, SparkGlobal.getPartitioner(getDim()));
		return joinImg.mapValues(new Function<Tuple3<List<TImgBlock<T>>,List<TImgBlock<T>>,List<TImgBlock<T>>>,List<TImgBlock<T>>>() {

			@Override
			public List<TImgBlock<T>> call(
					Tuple3<List<TImgBlock<T>>, List<TImgBlock<T>>, List<TImgBlock<T>>> arg0)
					throws Exception {
				// TODO Auto-generated method stub
				List<TImgBlock<T>> alist=arg0._1();
				List<TImgBlock<T>> blist=arg0._2();
				List<TImgBlock<T>> clist=arg0._3();
				List<TImgBlock<T>> outList=new ArrayList(alist.size()+blist.size()+clist.size());
				outList.addAll(alist);
				outList.addAll(blist);
				outList.addAll(clist);
				return outList;
			}
			
		});
		
	}
	/**
	 * A simple block shifting class for testing out the join operations
	 * @author mader
	 *
	 * @param <T>
	 */
	protected static class BlockShifter<T extends Cloneable> extends PairFunction<Tuple2<D3int, TImgBlock<T>>, D3int, TImgBlock<T>> {
		protected final D3int inOffset;
		// Since we can't have constructors here (probably should make it into a subclass)
		public BlockShifter(D3int inOffset){
			this.inOffset=inOffset;
		}
		@Override
		public Tuple2<D3int, TImgBlock<T>> call(
				Tuple2<D3int, TImgBlock<T>> inData) {
			final TImgBlock<T> inSlice = inData._2();
			final D3int nOffset=this.inOffset;

				final D3int oPos = inData._1();
				final D3int nPos = new D3int(oPos.x+nOffset.x, oPos.y+nOffset.y, oPos.z+nOffset.z);
				return new Tuple2<D3int, TImgBlock<T>>(
							nPos, new TImgBlock<T>(inSlice
									.getClone(), nPos, inSlice.getDim(),
									nOffset));

		}
	}
	/**
	 * Spread blocks out over a given range
	 * @param blockDimension the size of the blocks to distribute
	 * @param offsetList the offsets of the starting position of the blocks
	 * @return
	 */
	public JavaPairRDD<D3int, TImgBlock<T>> spreadBlocks(final D3int[] offsetList) {
		return baseImg.flatMap(new BlockSpreader(offsetList,getDim()));
	}
	/**
	 * Spread out image over slices
	 * 
	 * @param windowSize
	 *            range above and below to spread
	 * @return
	 */
	protected JavaPairRDD<D3int, TImgBlock<T>> spreadSlices(final int windowSize) {
		return baseImg.flatMap(BlockSpreader.<T>SpreadSlices(windowSize,getDim()));
		
	}
	/** 
	 * A class for spreading out blocks
	 * @author mader
	 *
	 * @param <T>
	 */
	protected static class BlockSpreader<T extends Cloneable> extends PairFlatMapFunction<Tuple2<D3int, TImgBlock<T>>, D3int, TImgBlock<T>> {
		
		protected final D3int[] inOffsetList;
		
		protected final D3int imgDim;
		// Since we can't have constructors here (probably should make it into a subclass)
		public BlockSpreader(D3int[] inOffsetList,D3int imgDim){
			this.inOffsetList=inOffsetList;
			this.imgDim=imgDim;	
		}
		static public <Fc extends Cloneable> BlockSpreader<Fc> SpreadSlices(final int windowSize,final D3int imgDim) {
			final D3int sliceDim = new D3int(imgDim.x, imgDim.y, 1);
			final D3int[] offsetList=new D3int[2*windowSize+1];
			for(int i=-windowSize;i<=windowSize;i++) offsetList[i+windowSize]=new D3int(0,0,i);
			return new BlockSpreader<Fc>(offsetList,imgDim);
			
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
									.getClone(), nPos, inSlice.getDim(),
									nOffset)));
			}
			return outList;
		}
	}

	

}
