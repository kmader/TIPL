/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tipl.formats.TImgRO;
import tipl.tools.BaseTIPLPluginIn;
import tipl.tools.Morpho;
import tipl.util.TIPLPluginIO;

/**
 * @author mader
 * 
 */
public class MorphKernelTest {
	// start the morphological plugin tests
	protected static TIPLPluginIO makeMorpho(final TImgRO inImage) {
		final TIPLPluginIO MP = new Morpho();

		MP.LoadImages(new TImgRO[] { inImage });
		return MP;
	}

	final private BaseTIPLPluginIn.morphKernel sph1 = BaseTIPLPluginIn
			.sphKernel(1, 1, 1);

	final private BaseTIPLPluginIn.morphKernel sph3 = BaseTIPLPluginIn
			.sphKernel(Math.sqrt(3) + 0.1, Math.sqrt(3) + 0.1,
					Math.sqrt(3) + 0.1);

	private final BaseTIPLPluginIn.morphKernel d = BaseTIPLPluginIn.fullKernel;

	private void checkD(final BaseTIPLPluginIn.morphKernel b) {
		assertEquals(b.inside(0, 0, 10, 10, 10, 10, 10, 10), true);
		// x
		assertEquals(b.inside(0, 0, 10, 9, 10, 10, 10, 10), true);
		assertEquals(b.inside(0, 0, 10, 11, 10, 10, 10, 10), true);
		// y
		assertEquals(b.inside(0, 0, 10, 10, 10, 9, 10, 10), true);
		assertEquals(b.inside(0, 0, 10, 10, 10, 11, 10, 10), true);
		// z
		assertEquals(b.inside(0, 0, 10, 10, 10, 10, 10, 9), true);
		assertEquals(b.inside(0, 0, 10, 10, 10, 10, 10, 11), true);
	}

	private void checkFull(final BaseTIPLPluginIn.morphKernel b) {
		for (int i = -1; i <= 1; i++)
			for (int j = -1; j <= 1; j++)
				for (int k = -1; k <= 1; k++)
					assertEquals(
							b.inside(0, 0, 10, 10 + i, 10, 10 + j, 10, 10 + k),
							true);
	}

	@Test
	public void testMorphClose() {
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.SinglePointFunction(5, 5, 5));
		System.out.println("Testing Closing with D-kernel");
		final TIPLPluginIO RS = makeMorpho(testImg);
		// kernel all (0), d-all on the axes (1), spherical (2)
		RS.setParameter("-kernel=1");
		RS.execute("closeMany", new Integer(1));
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		assertEquals(TestFImages.countVoxelsImage(outImg), 1); // from one point
																// to one point
	}

	@Test
	public void testMorphCloseAll() {
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.SinglePointFunction(5, 5, 5));
		System.out.println("Testing Closing with All-Kernel");
		final TIPLPluginIO RS = makeMorpho(testImg);
		// kernel all (0), d-all on the axes (1), spherical (2)
		RS.setParameter("-kernel=0");
		RS.execute("closeMany", new Integer(1));
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		assertEquals(TestFImages.countVoxelsImage(outImg), 1); // from one point
																// to one point
	}

	@Test
	public void testMorphDilationAll() {
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.SinglePointFunction(5, 5, 5));
		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		System.out.println("Testing Dilation with all kernel");
		final TIPLPluginIO RS = makeMorpho(testImg);
		// kernel all (0), d-all on the axes (1), spherical (2)
		RS.setParameter("-kernel=0");
		RS.execute("dilateMany", new Integer(1));
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		assertEquals(TestFImages.countVoxelsImage(outImg), 27); // 27=3x3x3
																// (perfect)
	}

	/**
	 * Test the same number of voxels in the right slices
	 */
	@Test
	public void testMorphDilationDK() {
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.SinglePointFunction(5, 5, 5));
		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		System.out.println("Testing Dilation with D-kernel");
		final TIPLPluginIO RS = makeMorpho(testImg);
		// kernel all (0), d-all on the axes (1), spherical (2)
		RS.setParameter("-kernel=1");
		RS.execute("dilateMany", new Integer(1));
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		assertEquals(TestFImages.countVoxelsImage(outImg), 7); // 7 = self +
																// (+/- 1) in
																// x,y,z (6)
	}

	@Test
	public void testMorphDilationSph() {
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.SinglePointFunction(5, 5, 5));
		System.out.println("Testing Dilation with spherical-kernel");
		final TIPLPluginIO RS = makeMorpho(testImg);
		// kernel all (0), d-all on the axes (1), spherical (2)
		RS.setParameter("-kernel=2 -neighborhood=2,2,2");
		RS.execute("dilateMany", new Integer(1));
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		assertEquals(TestFImages.countVoxelsImage(outImg), 33);
	}

	@Test
	public void testMorphErosionDK() {
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.SinglePointFunction(5, 5, 5));
		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		System.out.println("Testing Dilation with D-kernel");
		final TIPLPluginIO RS = makeMorpho(testImg);
		// kernel all (0), d-all on the axes (1), spherical (2)
		RS.setParameter("-kernel=1");
		RS.execute("erodeMany", new Integer(1));
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		assertEquals(TestFImages.countVoxelsImage(outImg), 0);
	}

	/**
	 * Test method for {@link tipl.tools.BaseTIPLPluginIn#sphKernel(double)}.
	 */
	@Test
	public void testSphKernelDouble() {

		// BaseTIPLPluginIn.printKernel(sph1);
		checkD(sph1);
		// xy should not belong
		assertEquals(sph1.inside(0, 0, 10, 9, 10, 9, 10, 10), false);
		// nor xz
		assertEquals(sph1.inside(0, 0, 10, 9, 10, 10, 10, 9), false);

		// BaseTIPLPluginIn.printKernel(sph3);
		checkFull(sph3);

	}

	/**
	 * Test method for {@link tipl.tools.BaseTIPLPluginIn#stationaryKernel}.
	 */
	@Test
	public void testStationaryKernel() {
		BaseTIPLPluginIn.morphKernel b = new BaseTIPLPluginIn.stationaryKernel(
				sph1);
		assertEquals(BaseTIPLPluginIn.printKernel(b),
				BaseTIPLPluginIn.printKernel(sph1));
		// checkD(b);
		b = new BaseTIPLPluginIn.stationaryKernel(sph3);
		assertEquals(BaseTIPLPluginIn.printKernel(b),
				BaseTIPLPluginIn.printKernel(sph3));
		// checkFull(b);
	}

	/**
	 * Test method for {@link tipl.tools.BaseTIPLPluginIn#useDKernel()}.
	 */
	@Test
	public void testUseDKernel() {
		final BaseTIPLPluginIn.morphKernel b = BaseTIPLPluginIn.dKernel;
		checkD(b);
		// xy should not belong
		assertEquals(b.inside(0, 0, 10, 9, 10, 9, 10, 10), false);
		// nor xz
		assertEquals(b.inside(0, 0, 10, 9, 10, 10, 10, 9), false);
	}

	/**
	 * Test method for {@link tipl.tools.BaseTIPLPluginIn#useFullKernel()}.
	 */
	@Test
	public void testUseFullKernel() {
		checkFull(d);
	}
}
