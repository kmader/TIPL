/**
 * 
 */
package tipl.tests;

import tipl.formats.TImgRO;

/**
 * @author mader
 *
 */
public abstract class TIPLTestingLibrary {
	public static TestPosFunctions RotateFunction(TestPosFunctions inFunction) {
		return inFunction; //TODO fix or remove
				
	}
	/**
	 * count voxels in an entire image
	 * 
	 * @param img
	 *            image
	 * @return total number of true voxels
	 */
	public static long countVoxelsImage(final TImgRO img) {
		long totalCount = 0;
		for (int i = 0; i < img.getDim().z; i++)
			totalCount += TIPLTestingLibrary.countVoxelsSlice(img, i);
		return totalCount;
	}

	/**
	 * count the number of voxels in a slice
	 * 
	 * @param img
	 *            the image to use
	 * @param sliceZ
	 *            the slice number to look at
	 * @return the number of voxels
	 */
	public static long countVoxelsSlice(final TImgRO img, final int sliceZ) {
		final boolean[] cSlice = (boolean[]) img.getPolyImage(sliceZ, 10);
		long i = 0;
		for (final boolean cVal : cSlice)
			if (cVal)
				i++;
		return i;
	}

}
