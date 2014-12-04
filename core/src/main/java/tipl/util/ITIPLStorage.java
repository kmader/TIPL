/**
 * 
 */
package tipl.util;

import java.util.Date;

import tipl.formats.TImg;
import tipl.formats.TImgRO;

/**
 * ITIPLStorage describes the commands a storage class needs to fulfill in order to be usable. 
 * It must be able to read and write images from strings and create new images in memory or on disk
 * via memory mapped operations. The interface enables more flexible implementations later possibly
 * using HazelCast or similar in-memory distributed maps 
 * @author mader
 *
 */
public interface ITIPLStorage {
	/**
	 * A global image cache so images can be referenced until they are unloaded
	 * by just their name
	 */
	public static class StampedObj<T> {
		/**
		 * the time the object was created
		 */
		final public Date createdTime;
		/**
		 * the time the object was last accessed
		 */
		public Date lastAccessedTime;
		final protected T curImg;
		public StampedObj(T inImg) {
			createdTime=new Date();
			lastAccessedTime=new Date();
			curImg=inImg;
		}
		public T get() {
			lastAccessedTime=new Date();
			return curImg;
		}
		/**
		 * get the age of the stamped objects in seconds
		 * @return
		 */
		public double getAge() {
			 return  ( (new Date()).getTime() - lastAccessedTime.getTime()) / 1000. ;
		}
		
	}
	// enumerated memory states
	public static final int FAST_TIFF_BASED = 0;
	public static final int FAST_MEMORY_MAP_BASED = 1;
	public static final int FAST_MEMORY_COMPUTATION_BASED = 2;
	public static final int FAST_MEMORY_BASED = 3;
	/** minimum isfast level to count as being cached */
	public static int FAST_CACHED = FAST_MEMORY_MAP_BASED;
	
	
	/**
	 * get the current caching level
	 * @return
	 */
	public int getCacheLevel();
	/**
	 * set the caching level
	 * @param inValue
	 * @return
	 */
	public boolean setCacheLevel(final int inValue);
	/**
	 * read an image file from a path
	 * @param path
	 * @return
	 */
	public TImg readTImg(final TypedPath path);
	/**
	 * Read an image and save it to the global cache for later retrieval (must
	 * then be manually deleted)
	 * 
	 * @param path
	 * @param readFromCache
	 *            check the cache to see if the image is already present
	 * @param saveToCache
	 *            put the image into the cache after it has been read
	 * @return loaded image
	 */
	public TImg readTImg(final TypedPath path,boolean readFromCache,boolean saveToCache);
	/**
	 * write an image file
	 * @param outImg
	 * @param path
	 * @return
	 */
	public boolean writeTImg(final TImgRO outImg,final TypedPath path,boolean saveToCache);
	/**
	 * Write a TImg with all of the appropriate parameters
	 * 
	 * @param inImg
	 * @param outpath
	 * @param outType
	 * @param scaleVal
	 * @param IisSigned
	 */
	@Deprecated
	public boolean writeTImg(final TImgRO inImg, final TypedPath outpath,
			final int outType, final float scaleVal, final boolean IisSigned,boolean saveToCache);
		
	/**
	 * allocate a new image and return the result
	 * @param dims
	 * @param type
	 * @return
	 */
	public TImg allocateTImg(final D3int dims,final int type);
	/**
	 * take an image and return a cached or precaculated version of it
	 * @param inImage
	 * @return cached or pre-loaded image
	 */
	public TImgRO CacheImage(final TImgRO inImage);
	/**
	 * Converts a read-only TImg into a read-write TImg
	 * @param inImage as read-only
	 * @return rw image
	 */
	public TImg wrapTImgRO(final TImgRO inImage);
	public boolean RemoveTImgFromCache(String path);

	/**
	 * Get a version of the image which can be read as a single large array (useful for the old
	 * style of plugins, but will soon be removed)
	 * @param inImg
	 * @return an image which can be read-entirely from top to bottom
	 */
	@Deprecated
	public TImgRO.FullReadable makeTImgFullReadable(final TImgRO inImg);

	/**
	 * get the directory where scratch files are stored
	 */
	public TypedPath getScratchDirectory();
	/**
	 * set the directory where scratch files are stored
	 */
	public void setScratchDirectory(TypedPath scratchDirectory);

	/**
	 * does the storage method use scratch storage
	 * @return
	 */
	public boolean getUseScratch();

	/**
	 * set the ability to use scratch
	 * @param useScratch
	 */
	public void setUseScratch(boolean useScratch);


	/**
	 * Create a typed path object from a string
	 * @param currentString
	 * @return a typed path object
	 */
	public TypedPath IdentifyPath(final String currentString);


	
}
