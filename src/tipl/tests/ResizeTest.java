/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tipl.formats.TImgRO;
import tipl.formats.VirtualAim;
import tipl.tools.Resize;
import tipl.util.D3int;
import tipl.util.TIPLPluginIO;

/**
 * Test the Resize class using synthetic data
 * 
 * @author mader
 * 
 */

public class ResizeTest {

	protected static void checkDim(TImgRO img, D3int dim) {
		assertEquals(img.getDim().x, dim.x);
		assertEquals(img.getDim().y, dim.y);
		assertEquals(img.getDim().z, dim.z);
	}

	protected static void checkDimensions(TImgRO img, D3int pos, D3int dim) {
		checkDim(img, dim);
		checkPos(img, pos);
	}

	protected static void checkPos(TImgRO img, D3int pos) {
		assertEquals(img.getPos().x, pos.x);
		assertEquals(img.getPos().y, pos.y);
		assertEquals(img.getPos().z, pos.z);
	}

	protected static long countVoxelsSlice(TImgRO img, int sliceZ) {
		final boolean[] cSlice = (boolean[]) img.getPolyImage(sliceZ, 10);
		long i = 0;
		for (final boolean cVal : cSlice)
			if (cVal)
				i++;
		return i;
	}

	protected static boolean doSlicesMatch(boolean[] slice1, boolean[] slice2) {
		assertEquals(slice1.length, slice2.length);
		for (int i = 0; i < slice1.length; i++) {
			// System.out.println(i+", "+slice1[i]+" : "+slice2[i]);
			assertEquals(slice1[i], slice2[i]);
		}
		return true;
	}

	protected static boolean doSlicesMatch(int[] slice1, int[] slice2) {
		assertEquals(slice1.length, slice2.length);
		for (int i = 0; i < slice1.length; i++) {
			System.out.println(i + ", " + slice1[i] + " : " + slice2[i]);
			assertEquals(slice1[i], slice2[i]);
		}
		return true;
	}

	protected static boolean doSlicesMatchB(TImgRO imgA, int sliceA,
			TImgRO imgB, int sliceB) {
		final boolean[] aSlice = (boolean[]) imgA.getPolyImage(sliceA, 10);
		final boolean[] bSlice = (boolean[]) imgB.getPolyImage(sliceB, 10);
		return doSlicesMatch(aSlice, bSlice);
	}

	protected static boolean doSlicesMatchI(TImgRO imgA, int sliceA,
			TImgRO imgB, int sliceB) {
		final int[] aSlice = (int[]) imgA.getPolyImage(sliceA, 2);
		final int[] bSlice = (int[]) imgB.getPolyImage(sliceB, 2);
		return doSlicesMatch(aSlice, bSlice);
	}

	protected static TIPLPluginIO makeRS(TImgRO inImage) {
		final TIPLPluginIO RS = new Resize();
		RS.LoadImages(new TImgRO[] { inImage });
		return RS;
	}

	/**
	 * Test the values in the slices actually match
	 */
	public static void testSlicesMatchBool(TImgRO testImg) {
		// offset lines

		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		System.out.println("Testing Slices Match  in BW");
		TIPLPluginIO RS = makeRS(testImg);

		RS.setParameter("-pos=0,0,5 -dim=10,10,2");
		RS.execute();

		final TImgRO outImg = RS.ExportImages(testImg)[0];
		System.out.println(outImg.getPos() + ", " + testImg.getPos());
		doSlicesMatchB(outImg, 0, testImg, 5);
		doSlicesMatchB(outImg, 1, testImg, 6);

		// now make another subimage

		RS = makeRS(outImg);

		RS.setParameter("-pos=0,0,6 -dim=10,10,1");
		RS.execute();

		final TImgRO outImg2 = RS.ExportImages(outImg)[0];
		doSlicesMatchB(outImg2, 0, testImg, 6);

	}

	/**
	 * Test the values in the slices actually match using integers
	 */
	public static void testSlicesMatchInt(TImgRO testImg) {
		// offset lines

		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		System.out.println("Testing Slices Match");
		TIPLPluginIO RS = makeRS(testImg);

		RS.setParameter("-pos=0,0,5 -dim=10,10,2");
		RS.execute();

		final TImgRO outImg = RS.ExportImages(testImg)[0];
		System.out.println(outImg.getPos() + ", " + testImg.getPos());
		doSlicesMatchI(outImg, 0, testImg, 5);
		doSlicesMatchI(outImg, 1, testImg, 6);

		// now make another subimage

		RS = makeRS(outImg);

		RS.setParameter("-pos=0,0,6 -dim=10,10,1");
		RS.execute();

		final TImgRO outImg2 = RS.ExportImages(outImg)[0];
		doSlicesMatchI(outImg2, 0, testImg, 6);

	}

	/**
	 * Test dimensions of output image
	 */
	@Test
	public void test() {
		// offset lines
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.LinesFunction());
		final TIPLPluginIO RS = makeRS(testImg);
		RS.setParameter("-pos=5,5,5 -dim=5,5,1");
		RS.execute();
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		checkDimensions(outImg, new D3int(5, 5, 5), new D3int(5, 5, 1));
		System.out.println("Testing SphRadius");

	}

	/**
	 * Test dimensions of output image
	 */
	@Test
	public void testOutDims() {
		// offset lines
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.LinesFunction());
		final TIPLPluginIO RS = makeRS(testImg);
		RS.setParameter("-pos=5,5,5 -dim=5,5,1");
		RS.execute();
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		checkDimensions(outImg, new D3int(5, 5, 5), new D3int(5, 5, 1));
		System.out.println("Testing SphRadius");

	}

	/**
	 * Test the same number of voxels in the right slices
	 */
	@Test
	public void testOutVoxCount() {
		// offset lines

		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.DiagonalPlaneFunction());
		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		System.out.println("Testing Voxel Count");
		final TIPLPluginIO RS = makeRS(testImg);

		RS.setParameter("-pos=0,0,5 -dim=10,10,1");
		RS.execute();
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		assertEquals(countVoxelsSlice(outImg, 0), countVoxelsSlice(testImg, 5));

	}

	@Test
	public void testSlicesMatchIntNormal() {
		final TImgRO testImg = TestFImages.wrapItAs(10,
				new TestFImages.ProgZImage(), 2);
		testSlicesMatchInt(testImg);

	}

	@Test
	public void testSlicesMatchIntPreload() {
		final TImgRO testImg = TestFImages.wrapItAs(10,
				new TestFImages.ProgZImage(), 2);
		testSlicesMatchInt(testImg);
		final VirtualAim vImg = new VirtualAim(testImg);

		vImg.getIntAim();
		testSlicesMatchBool(vImg);

	}

	@Test
	public void testSlicesMatchNormal() {
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.DiagonalPlaneFunction());
		testSlicesMatchBool(testImg);

	}

	@Test
	public void testSlicesMatchPreload() {
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.DiagonalPlaneFunction());
		// TImgRO.FullReadable vImg=TImgTools.makeTImgFullReadable(testImg);
		final VirtualAim vImg = new VirtualAim(testImg);

		vImg.getBoolAim();
		testSlicesMatchBool(vImg);
	}

	@Test
	public void testSlicesMatchPreLoaded() {
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.DiagonalPlaneFunction());

		testSlicesMatchBool(testImg);

	}

}
