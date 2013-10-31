/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.assertEquals;
import tipl.formats.TImgRO;
import tipl.util.D3int;

/**
 * @author mader
 *
 */
public abstract class TIPLTestingLibrary {
	public static final boolean verbose=false;
	public final TImgRO emptyImg = TestPosFunctions.wrapIt(0,
			new TestPosFunctions.SinglePointFunction(0, 0, 0));
	public static void checkDimensions(final TImgRO img, final D3int dim,final D3int pos) {
		checkDim(img,dim);
		checkDim(img,pos);
	}
	public static void checkDim(final TImgRO img, final D3int dim) {
		assertEquals(img.getDim().x, dim.x);
		assertEquals(img.getDim().y, dim.y);
		assertEquals(img.getDim().z, dim.z);
	}
	public static void checkPos(final TImgRO img, final D3int pos) {
		assertEquals(img.getPos().x, pos.x);
		assertEquals(img.getPos().y, pos.y);
		assertEquals(img.getPos().z, pos.z);
	}

	public static boolean doSlicesMatch(final boolean[] slice1,
			final boolean[] slice2) {
		assertEquals(slice1.length, slice2.length);
		for (int i = 0; i < slice1.length; i++) {
			if (verbose) System.out.println(i+", "+slice1[i]+" : "+slice2[i]);
			assertEquals(slice1[i], slice2[i]);
		}
		return true;
	}

	public static boolean doSlicesMatch(final int[] slice1,
			final int[] slice2) {
		assertEquals(slice1.length, slice2.length);
		for (int i = 0; i < slice1.length; i++) {
			if (verbose) System.out.println(i + ", " + slice1[i] + " : " + slice2[i]);
			assertEquals(slice1[i], slice2[i]);
		}
		return true;
	}

	public static void doSlicesMatchB(final TImgRO imgA,
			final int sliceA, final TImgRO imgB, final int sliceB) {
		final boolean[] aSlice = (boolean[]) imgA.getPolyImage(sliceA, 10);
		final boolean[] bSlice = (boolean[]) imgB.getPolyImage(sliceB, 10);
		doSlicesMatch(aSlice, bSlice);
	}
	public static void doSlicesMatchI(final TImgRO imgA,
			final int sliceA, final TImgRO imgB, final int sliceB) {
		final int[] aSlice = (int[]) imgA.getPolyImage(sliceA, 2);
		final int[] bSlice = (int[]) imgB.getPolyImage(sliceB, 2);
		doSlicesMatch(aSlice, bSlice);
	}
	
	public static void doImagesMatch(final TImgRO imgA,final TImgRO imgB) {
		for(int i=0;i<imgA.getDim().z;i++) doSlicesMatchI(imgA,i,imgB,i);
	}
	public static void doImagesMatchB(final TImgRO imgA,final TImgRO imgB) {
		for(int i=0;i<imgA.getDim().z;i++) doSlicesMatchB(imgA,i,imgB,i);
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
