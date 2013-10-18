/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tipl.formats.PureFImage;
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




	



	protected static TIPLPluginIO makeRS(final TImgRO inImage) {
		final TIPLPluginIO RS = new Resize();
		RS.LoadImages(new TImgRO[] { inImage });
		return RS;
	}

	/**
	 * Test the values in the slices actually match
	 */
	public static void testSlicesMatchBool(final TImgRO testImg) {
		// offset lines

		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		System.out.println("Testing Slices Match  in BW");
		TIPLPluginIO RS = makeRS(testImg);

		RS.setParameter("-pos=0,0,5 -dim=10,10,2");
		RS.execute();

		final TImgRO outImg = RS.ExportImages(testImg)[0];
		System.out.println(outImg.getPos() + ", " + testImg.getPos());
		TIPLTestingLibrary.doSlicesMatchB(outImg, 0, testImg, 5);
		TIPLTestingLibrary.doSlicesMatchB(outImg, 1, testImg, 6);

		// now make another subimage

		RS = makeRS(outImg);

		RS.setParameter("-pos=0,0,6 -dim=10,10,1");
		RS.execute();

		final TImgRO outImg2 = RS.ExportImages(outImg)[0];
		TIPLTestingLibrary.doSlicesMatchB(outImg2, 0, testImg, 6);

	}

	/**
	 * Test the values in the slices actually match using integers
	 */
	public static void testSlicesMatchInt(final TImgRO testImg) {
		// offset lines

		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		System.out.println("Testing Slices Match");
		TIPLPluginIO RS = makeRS(testImg);

		RS.setParameter("-pos=0,0,5 -dim=10,10,2");
		RS.execute();

		final TImgRO outImg = RS.ExportImages(testImg)[0];
		System.out.println(outImg.getPos() + ", " + testImg.getPos());
		TIPLTestingLibrary.doSlicesMatchI(outImg, 0, testImg, 5);
		TIPLTestingLibrary.doSlicesMatchI(outImg, 1, testImg, 6);

		// now make another subimage

		RS = makeRS(outImg);

		RS.setParameter("-pos=0,0,6 -dim=10,10,1");
		RS.execute();

		final TImgRO outImg2 = RS.ExportImages(outImg)[0];
		TIPLTestingLibrary.doSlicesMatchI(outImg2, 0, testImg, 6);

	}

	/**
	 * Test dimensions of output image
	 */
	@Test
	public void test() {
		// offset lines
		final TImgRO testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.LinesFunction());

		final TIPLPluginIO RS = makeRS(testImg);
		RS.setParameter("-pos=5,5,5 -dim=5,5,1");
		RS.execute();
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		TIPLTestingLibrary.checkDimensions(outImg, new D3int(5, 5, 5), new D3int(5, 5, 1));
		System.out.println("Testing SphRadius");

	}

	/**
	 * Test dimensions of output image
	 */
	@Test
	public void testOutDims() {
		// offset lines
		final TImgRO testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.LinesFunction());
		final TIPLPluginIO RS = makeRS(testImg);
		RS.setParameter("-pos=5,5,5 -dim=5,5,1");
		RS.execute();
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		TIPLTestingLibrary.checkDimensions(outImg, new D3int(5, 5, 5), new D3int(5, 5, 1));
		System.out.println("Testing SphRadius");

	}

	/**
	 * a new test to ensure image type is preserved during resizing
	 */
	@Test
	public void testOutImageType() {
		final PureFImage.PositionFunction pf = new TestPosFunctions.DiagonalPlaneFunction();
		// make one of each
		final TImgRO[] testImgs = { TestPosFunctions.wrapItAs(10, pf, 0),
				TestPosFunctions.wrapItAs(10, pf, 1),
				TestPosFunctions.wrapItAs(10, pf, 2),
				TestPosFunctions.wrapItAs(10, pf, 3),
				TestPosFunctions.wrapItAs(10, pf, 10) };
		System.out.println("Testing Short Scale Factor");
		TIPLPluginIO RS;
		for (final TImgRO testImg : testImgs) {
			System.out.println("Testing Image Type: " + testImg.getImageType());
			RS = makeRS(testImg);
			RS.setParameter("-pos=0,0,5 -dim=10,10,1");
			RS.execute();
			assertEquals(testImg.getImageType(),
					RS.ExportImages(testImg)[0].getImageType());
		}

	}

	/**
	 * a new test to ensure short scale factor and type is preserved during
	 * resizing
	 */
	@Test
	public void testOutSSF() {
		// offset lines
		final int sizeX = 10;
		final float ssf = 1.5f;
		final TImgRO testImg = new PureFImage(TestPosFunctions.justDims(new D3int(
				sizeX, sizeX, sizeX)), 2,
				new TestPosFunctions.DiagonalPlaneFunction(), ssf);
		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		System.out.println("Testing Short Scale Factor");
		final TIPLPluginIO RS = makeRS(testImg);

		RS.setParameter("-pos=0,0,5 -dim=10,10,1");
		RS.execute();
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		assertEquals(testImg.getShortScaleFactor(),
				outImg.getShortScaleFactor(), 0.01f);

	}

	/**
	 * Test the same number of voxels in the right slices
	 */
	@Test
	public void testOutVoxCount() {
		// offset lines

		final TImgRO testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.DiagonalPlaneFunction());
		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		System.out.println("Testing Voxel Count");
		final TIPLPluginIO RS = makeRS(testImg);

		RS.setParameter("-pos=0,0,5 -dim=10,10,1");
		RS.execute();
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		assertEquals(TIPLTestingLibrary.countVoxelsSlice(outImg, 0),
				TIPLTestingLibrary.countVoxelsSlice(testImg, 5));

	}

	@Test
	public void testSlicesMatchIntNormal() {
		final TImgRO testImg = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.ProgZImage(), 2);
		testSlicesMatchInt(testImg);

	}

	@Test
	public void testSlicesMatchIntPreload() {
		final TImgRO testImg = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.ProgZImage(), 2);
		testSlicesMatchInt(testImg);
		final VirtualAim vImg = new VirtualAim(testImg);

		vImg.getIntAim();
		testSlicesMatchBool(vImg);

	}

	@Test
	public void testSlicesMatchNormal() {
		final TImgRO testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.DiagonalPlaneFunction());
		testSlicesMatchBool(testImg);

	}

	@Test
	public void testSlicesMatchPreload() {
		final TImgRO testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.DiagonalPlaneFunction());
		// TImgRO.FullReadable vImg=TImgTools.makeTImgFullReadable(testImg);
		final VirtualAim vImg = new VirtualAim(testImg);

		vImg.getBoolAim();
		testSlicesMatchBool(vImg);
	}

	@Test
	public void testSlicesMatchPreLoaded() {
		final TImgRO testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.DiagonalPlaneFunction());

		testSlicesMatchBool(testImg);

	}

}
