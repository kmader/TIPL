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
		public Date createdTime;
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
	public TImg readTImg(final String path);
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
	public TImg readTImg(final String path,boolean readFromCache,boolean saveToCache);
	/**
	 * write an image file
	 * @param outImg
	 * @param path
	 * @return
	 */
	public boolean writeTImg(final TImgRO outImg,final String path,boolean saveToCache);
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
	
}
