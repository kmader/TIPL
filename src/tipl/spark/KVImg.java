/**
 * 
 */
package tipl.spark;

import java.io.Serializable;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;
import tipl.formats.TImgRO;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgBlock;
import tipl.util.TImgTools;

/**
 * KVImg is a key value pair image consisting of a key (position formatted as D3int) and a value of type T (extension of number)
 * <strong>Note since neither Character or Boolean are subclasses of number they have both been replaced with Byte</strong>
 * 
 * @author mader
 *
 */
public class KVImg<T extends Number> implements TImgRO,Serializable {
	final protected JavaPairRDD<D3int, T> baseImg;
	final protected D3int dim;
	final protected D3int pos;
	final protected D3float elSize;
	final protected D3int offset=new D3int(0);
	final protected int imageType;
	protected String procLog="";
	
	public KVImg(D3int idim,D3int ipos, D3float ielSize,final int iimageType,JavaPairRDD<D3int, T> ibImg) {
		assert(TImgTools.isValidType(iimageType));
		dim=idim;
		pos=ipos;
		elSize=ielSize;
		imageType=iimageType;
		baseImg=ibImg;
	}
	/**
	 * Force the KVImg to be a long no matter what it is now (used for shape analysis and other applications)
	 */
	@SuppressWarnings("serial")
	public KVImg<Long> toKVLong() {
		if (imageType==TImgTools.IMAGETYPE_LONG) return (KVImg<Long>) this;
		return new KVImg<Long>(dim,pos,elSize,TImgTools.IMAGETYPE_LONG,
				baseImg.mapValues(new Function<T,Long>() {
			@Override
			public Long call(T arg0) throws Exception {
				return arg0.longValue();
			}
		}));
	}
	
	static public KVImg ConvertTImg(JavaSparkContext jsc,TImgRO inImage,int imageType) {
		return DTImg.ConvertTImg(jsc, inImage, imageType).asKV();
	}
	
	
	/* (non-Javadoc)
	 * @see tipl.util.TImgTools.HasDimensions#getDim()
	 */
	@Override
	public D3int getDim() {
		return dim;
	}

	/* (non-Javadoc)
	 * @see tipl.util.TImgTools.HasDimensions#getElSize()
	 */
	@Override
	public D3float getElSize() {
		return elSize;
	}

	/* (non-Javadoc)
	 * @see tipl.util.TImgTools.HasDimensions#getOffset()
	 */
	@Override
	public D3int getOffset() {
		return offset;
	}

	/* (non-Javadoc)
	 * @see tipl.util.TImgTools.HasDimensions#getPos()
	 */
	@Override
	public D3int getPos() {
		return pos;
	}

	/* (non-Javadoc)
	 * @see tipl.util.TImgTools.HasDimensions#getProcLog()
	 */
	@Override
	public String getProcLog() {
		return procLog;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#appendProcLog(java.lang.String)
	 */
	@Override
	public String appendProcLog(String inData) {
		procLog+=inData;
		return procLog;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getCompression()
	 */
	@Override
	public boolean getCompression() {
		return false;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getImageType()
	 */
	@Override
	public int getImageType() {
		return imageType;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getPath()
	 */
	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return "";
	}
	/**
	 * Turns a KV image listed by position (as D3int) to one listed by index
	 * @author mader
	 *
	 * @param <U>
	 */
	static public class posToIndex<U> implements PairFunction<Tuple2<D3int,U>,Integer,U> {
		protected final D3int cDim,cPos;
		public posToIndex(D3int inDim,D3int inPos) {
			cDim=inDim;
			cPos=inPos;
		}
		@Override
		public Tuple2<Integer, U> call(Tuple2<D3int, U> curElement)
				throws Exception {
			
			return new Tuple2<Integer,U>((curElement._1.y-cPos.y)*cDim.x+curElement._1.x-cPos.x,curElement._2);
		}
	}

	/* The function to collect all the key value pairs and return it as the appropriate array for a given slice
	 * @see tipl.formats.TImgRO#getPolyImage(int, int)
	 */
	@Override
	public Object getPolyImage(final int sliceNumber, int asType) {
		assert(TImgTools.isValidType(asType));
		final D3int cDim=getDim();
		final int sliceSize=cDim.x*cDim.y;
		final int cImageType=imageType;
		
		List<Tuple2<Integer, T>> sliceAsList=baseImg.filter(new Function<Tuple2<D3int,T>,Boolean>() {

			@Override
			public Boolean call(Tuple2<D3int, T> arg0) throws Exception {
				if (arg0._1.z==sliceNumber) return true;
				else return false;
			}
			
		}).mapToPair(new posToIndex<T>(cDim,getPos())).sortByKey().collect();
		
		// first create an array of the current type
		Object curSlice=null;
		switch(cImageType) {
		case TImgTools.IMAGETYPE_BOOL: 
			curSlice=new boolean[sliceSize];
			break;
		case TImgTools.IMAGETYPE_CHAR: 
			curSlice=new char[sliceSize];
			break;
		case TImgTools.IMAGETYPE_INT: 
			curSlice=new int[sliceSize];
			break;
		case TImgTools.IMAGETYPE_FLOAT: 
			curSlice=new float[sliceSize];
			break;
		case TImgTools.IMAGETYPE_DOUBLE: 
			curSlice=new double[sliceSize];
			break;
		case TImgTools.IMAGETYPE_LONG: 
			curSlice=new long[sliceSize];
			break;
		default:
			throw new IllegalArgumentException("Image type :"+TImgTools.getImageTypeName(imageType)+" is not yet supported");
			
			
		}
		// now copy the list into this array
		for (Tuple2<Integer, T> curElement : sliceAsList) {
			final int index=curElement._1.intValue();
			switch(cImageType) {
			case TImgTools.IMAGETYPE_BOOL: 
				((boolean[]) curSlice)[index]=((Byte) curElement._2).byteValue()>0;
				break;
			case TImgTools.IMAGETYPE_CHAR: 
				((char[]) curSlice)[index]=(char) ((Byte) curElement._2).byteValue();
				break;
			case TImgTools.IMAGETYPE_INT: 
				((int[]) curSlice)[index]=((Integer) curElement._2).intValue();
				break;
			case TImgTools.IMAGETYPE_FLOAT: 
				((float[]) curSlice)[index]=((Float) curElement._2).floatValue();
				break;
			case TImgTools.IMAGETYPE_DOUBLE: 
				((double[]) curSlice)[index]=((Double) curElement._2).doubleValue();
				break;
			case TImgTools.IMAGETYPE_LONG: 
				((long[]) curSlice)[index]=((Long) curElement._2).longValue();
				break;
			default:
				throw new IllegalArgumentException("Image type :"+TImgTools.getImageTypeName(imageType)+" is not yet supported");
				
			}
		}
		// convert this array into the proper output format
		return TImgTools.convertArrayType(curSlice, cImageType, asType, getSigned(), getShortScaleFactor());
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getSampleName()
	 */
	@Override
	public String getSampleName() {
		return baseImg.name();
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getShortScaleFactor()
	 */
	@Override
	public float getShortScaleFactor() {
		// TODO Auto-generated method stub
		return 1;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getSigned()
	 */
	@Override
	public boolean getSigned() {
		return false;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#isFast()
	 */
	@Override
	public int isFast() {
		return TImgTools.SPEED_DISK_AND_MEM;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#isGood()
	 */
	@Override
	public boolean isGood() {
		return true;
	}

}
