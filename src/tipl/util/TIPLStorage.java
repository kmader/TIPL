/**
 * 
 */
package tipl.util;

import java.util.Date;
import java.util.LinkedHashMap;

import tipl.formats.ConcurrentReader;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.VirtualAim;

/**
 * @author mader
 *
 */
public class TIPLStorage implements ITIPLStorage {
	
	public static int FAST_CACHED = FAST_MEMORY_MAP_BASED;
	public static boolean validCachingState(int inValue) {
		switch (inValue) {
			case FAST_TIFF_BASED:
				return true;
			case FAST_MEMORY_MAP_BASED:
				return true;
			case FAST_MEMORY_COMPUTATION_BASED:
				return true;
			case FAST_MEMORY_BASED:
				return true;
			default:
				return false;
		}
	}
	/**
	 * A global image cache so images can be referenced until they are unloaded
	 * by just their name
	 */
	protected static class StampedObj<T> {
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
	protected LinkedHashMap<String, StampedObj<TImg>> cachedImages = new LinkedHashMap<String,  StampedObj<TImg>>();

	/**
	 * Nothing really to be constructed yet
	 */
	public TIPLStorage() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * check to see if the image is faster than loading a tiff, if it is not
	 * fast and there is enough memory (not yet implemented), than cache it
	 */
	@Deprecated
	public TImgRO CacheImage(final TImgRO inImage) {
		if (inImage.isFast() > FAST_TIFF_BASED)
			return inImage;
		else
			return ConcurrentReader.CacheImage(inImage, inImage.getImageType());
	}

	@Override
	public int getCacheLevel() {
		// TODO Auto-generated method stub
		return FAST_CACHED;
	}

	@Override
	public boolean setCacheLevel(int inValue) {
		// TODO Auto-generated method stub
		if (!validCachingState(inValue)) return false;
		FAST_CACHED=inValue;
		return true;
	}
	

	public TImg readTImg(final String path) {return readTImg(path,true,true);}
	
	public TImg readTImg(final String path, final boolean readFromCache,
			final boolean saveToCache) {
		if (readFromCache)
			if (cachedImages.containsKey(path))
				return cachedImages.get(path).get();
		final TImg curImg = new VirtualAim(path);
		if (saveToCache)
			cachedImages.put(path, new StampedObj<TImg>(curImg));
		return curImg;
	}

	public boolean RemoveTImgFromCache(final String path) {
		try {
			cachedImages.remove(path);
			TIPLGlobal.runGC();
			return true;
		} catch (final Exception e) {
			e.printStackTrace();
			System.err.println("Image:" + path + " is not in the cache!");
			return false;
		}
	}
	
	

	@Override
	public boolean writeTImg(TImgRO outImg, String path) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TImg allocateTImg(D3int dims, int type) {
		// TODO Auto-generated method stub
		return null;
	}

}
