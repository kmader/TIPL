/**
 * 
 */
package tipl.formats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import tipl.tools.BaseTIPLPluginIn;
import tipl.util.ArgumentList.TypedPath;
import tipl.util.ArgumentList;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

/**
 * @author maderk
 * 
 */
public class ConcurrentReader implements TImgRO {
	/**
	 * a class for caching images read in this fashion with all of the slices
	 * lazily read upon starting
	 * 
	 * @author maderk
	 * 
	 */
	public static class CachedConcurrentReader extends ConcurrentReader {
		final List<Future<Object>> slices;
		final int type;

		public CachedConcurrentReader(final TImgRO inImage,
				final List<Future<Object>> inSlices, final int inType) {
			super(inImage);
			slices = inSlices;
			type = inType;
		}

		@Override
		public Future<Object> getPolyImageLater(final int iSliceNumber,
				final int asType) {
			assert iSliceNumber >= 0 && iSliceNumber < getDim().z;
			final Future<Object> curSlice = slices.get(iSliceNumber);
			if (asType == type)
				return curSlice;
			else
				return readRunner.submit(new Callable<Object>() {
					@Override
					public Object call() {
						try {
							return TImgTools.convertArrayType(curSlice.get(),
									type, asType, false, 1.0f, 255);
						} catch (final Exception e) {
							e.printStackTrace();
							throw new IllegalArgumentException(
									"Interrupt or Processing Error- Input Image: "
											+ templateData + ", slice:"
											+ iSliceNumber + " in list:"
											+ curSlice);

						}
					}
				});

		}
	}

	/**
	 * create from a standard TImg a cached version of an image
	 * 
	 * @param inImage
	 *            the input image
	 * @param asType
	 *            the type to cache for
	 * @return a CachedConcurrentReader image class with all the slices read or
	 *         (future) being read in
	 */
	public static TImgRO CacheImage(final TImgRO inImage, final int asType) {
		return new ConcurrentReader(inImage).asCachedImage(asType);
	}

	protected final TImgRO templateData;
	protected final ExecutorService readRunner;

	public ConcurrentReader(final TImgRO inImage) {
		templateData = inImage;
		readRunner = TIPLGlobal.getIOExecutor();
	}

	@Override
	public String appendProcLog(final String inData) {
		return templateData.appendProcLog(inData);
	}

	/**
	 * read in all of the slices in the image and block when reading a slice
	 * until it has been loaded
	 * 
	 * @param asType
	 * @return TImgRO cached version of the image
	 */
	public TImgRO asCachedImage(final int asType) {
		assert TImgTools.isValidType(asType);
		return new CachedConcurrentReader(this, getPolyImageSlices(0,
				getDim().z, asType), asType);
	}

	public boolean CheckSizes(final TImg otherTImg) {
		return TImgTools.CheckSizes2(this, otherTImg);
	}

	public void close() {
		readRunner.shutdownNow();
	}

	@Override
	public boolean getCompression() {
		return templateData.getCompression();
	}

	/** The size of the image */
	@Override
	public D3int getDim() {
		return templateData.getDim();
	}

	@Override
	public D3float getElSize() {
		return templateData.getElSize();
	}

	@Override
	public int getImageType() {
		return templateData.getImageType();
	}

	/**
	 * The size of the border around the image which does not contain valid
	 * voxel data
	 */
	@Override
	public D3int getOffset() {
		return templateData.getOffset();
	}

	@Override
	public TypedPath getPath() {
		return ArgumentList.TypedPath.virtualPath("CR @ " + templateData.getPath());
	}

	@Override
	public Object getPolyImage(final int isliceNumber, final int asType) {
		try {
			return getPolyImageLater(isliceNumber, asType).get();
		} catch (final Exception e) {
			System.out.println("Reading Slice Failed!!!!!");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * lazily read a slice and return a future reference for it
	 * 
	 * @param isliceNumber
	 *            the slice number to read
	 * @param asType
	 *            the type to read
	 * @return the function returns an object which must be typecast to the
	 *         correct type
	 */
	public Future<Object> getPolyImageLater(final int isliceNumber,
			final int asType) {
		// Callable cFun=

		return readRunner.submit(new Callable<Object>() {
			@Override
			public Object call() {
				// System.out.println("Reading Slice #"+isliceNumber);
				return templateData.getPolyImage(isliceNumber, asType);
			}
		});
	}

	public List<Future<Object>> getPolyImageSlices(final int iStart,
			final int iFinish, final int asType) {
		final List<Future<Object>> slices = new ArrayList<Future<Object>>(
				iFinish - iStart + 1);
		final int rStart = BaseTIPLPluginIn.max(iStart, 0);
		final int rEnd = BaseTIPLPluginIn.min(iFinish, getDim().z);
		// System.out.println("Requesting Slices :"+rStart+"-"+rEnd);
		for (int slice = rStart; slice < rEnd; slice++)
			slices.add(getPolyImageLater(slice, asType));
		return slices;
	}

	/**
	 * The position of the bottom leftmost voxel in the image in real space,
	 * only needed for ROIs
	 */
	@Override
	public D3int getPos() {
		return templateData.getPos();
	}

	@Override
	public String getProcLog() {
		return templateData.getProcLog();
	}

	@Override
	public String getSampleName() {
		return "CR @ " + templateData.getSampleName();
	}

	@Override
	public float getShortScaleFactor() {
		return templateData.getShortScaleFactor();
	}

	/**
	 * Is the image signed (should an offset be added / subtracted when the data
	 * is loaded to preserve the sign)
	 */
	@Override
	public boolean getSigned() {
		return templateData.getSigned();
	}

	public Double[] getXYZVec(final int cIndex, final int sliceNumber) {
		return TImgTools.getXYZVecFromVec(this, cIndex, sliceNumber);
	}

	public boolean InitializeImage(final D3int iPos, final D3int iDim,
			final D3int iOff, final D3float iSize, final int iType) {
		return false;
	}

	@Override
	public int isFast() {
		return templateData.isFast();
	}

	@Override
	public boolean isGood() {
		return templateData.isGood();
	}
}
