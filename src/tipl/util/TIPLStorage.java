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
 * The standard implementation of the storage engine
 * Provides basic mechanisms for caching images using a linkedhashmap
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
			cachedImages.put(path, new ITIPLStorage.StampedObj<TImg>(curImg));
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
	
	/**
	 * Method to write an image to disk and return whether or not it was
	 * successful
	 * 
	 * @param curImg
	 * @param path
	 * @param saveToCache
	 * @return success
	 */
	public boolean writeTImg(final TImgRO curImg, final String path,
			final boolean saveToCache) {

		try {
			if (curImg instanceof VirtualAim)
				((VirtualAim) curImg).WriteAim(path);
			else
				VirtualAim.TImgToVirtualAim(curImg).WriteAim(path);
			if (saveToCache)
				cachedImages.put(path, new ITIPLStorage.StampedObj<TImg>(wrapTImgRO(curImg)));
			return true;
		} catch (final Exception e) {
			System.err.println("Image: " + curImg.getSampleName() + " @ "
					+ curImg + ", could not be written to " + path);
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean writeTImg( TImgRO inImg,  String outpath,
			 int outType, float scaleVal, boolean IisSigned, boolean saveToCache) {
		VirtualAim cAim=VirtualAim.TImgToVirtualAim(inImg);
		cAim.WriteAim(outpath, outType, scaleVal,IisSigned);
		if (saveToCache)
			cachedImages.put(outpath, new ITIPLStorage.StampedObj<TImg>(cAim));
		return true;
	}
	@Override
	public TImg allocateTImg(D3int dims, int type) {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException(this+" allocTImg, NOT IMPLEMENTED YET");
	}

	@Override
	public TImg wrapTImgRO(final TImgRO inImage) {
		if (inImage instanceof TImg) return (TImg) inImage;
		return new VirtualAim(inImage);
	}

}
