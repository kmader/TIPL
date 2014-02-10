/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import tipl.formats.PureFImage;
import tipl.formats.TImgRO;
import tipl.formats.VirtualAim;
import tipl.util.D3int;
import tipl.util.ITIPLPluginIO;
import tipl.util.TIPLPluginManager;
import tipl.util.TIPLPluginManager.PluginInfo;

/**
 * Test the Resize class using synthetic data
 * 
 * @author mader
 * 
 */
@RunWith(value=Parameterized.class)
public class ResizeTest {


	@Parameters
	public static Collection<PluginInfo[]> getPlugins() {
		List<PluginInfo> possibleClasses=TIPLPluginManager.getPluginsNamed("Resize");
		return TIPLTestingLibrary.wrapCollection(possibleClasses);
	}
	final protected PluginInfo pluginId;
	public ResizeTest(PluginInfo pluginToUse) {
		pluginId=pluginToUse;
	}
	

	protected static ITIPLPluginIO makeRS(final PluginInfo idPlugin,final TImgRO inImage) {
		final ITIPLPluginIO RS =TIPLPluginManager.getPluginIO(idPlugin);
		RS.LoadImages(new TImgRO[] { inImage });
		return RS;
	}

	/**
	 * Test the values in the slices actually match
	 */
	public static void testSlicesMatchBool(final PluginInfo idPlugin,final TImgRO testImg) {
		// offset lines

		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		System.out.println("Testing Slices Match  in BW");
		ITIPLPluginIO RS = makeRS(idPlugin,testImg);

		RS.setParameter("-pos=0,0,5 -dim=10,10,2");
		RS.execute();

		final TImgRO outImg = RS.ExportImages(testImg)[0];
		System.out.println(outImg.getPos() + ", " + testImg.getPos());
		TIPLTestingLibrary.doSlicesMatchB(outImg, 0, testImg, 5);
		TIPLTestingLibrary.doSlicesMatchB(outImg, 1, testImg, 6);

		// now make another subimage

		RS = makeRS(idPlugin,outImg);

		RS.setParameter("-pos=0,0,6 -dim=10,10,1");
		RS.execute();

		final TImgRO outImg2 = RS.ExportImages(outImg)[0];
		TIPLTestingLibrary.doSlicesMatchB(outImg2, 0, testImg, 6);

	}

	/**
	 * Test the values in the slices actually match using integers
	 */
	public static void testSlicesMatchInt(final PluginInfo idPlugin,final TImgRO testImg) {
		// offset lines

		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		System.out.println("Testing Slices Match");
		ITIPLPluginIO RS = makeRS(idPlugin,testImg);

		RS.setParameter("-pos=0,0,5 -dim=10,10,2");
		RS.execute();

		final TImgRO outImg = RS.ExportImages(testImg)[0];
		System.out.println(outImg.getPos() + ", " + testImg.getPos());
		TIPLTestingLibrary.doSlicesMatchI(outImg, 0, testImg, 5);
		TIPLTestingLibrary.doSlicesMatchI(outImg, 1, testImg, 6);

		// now make another subimage

		RS = makeRS(idPlugin,outImg);

		RS.setParameter("-pos=0,0,6 -dim=10,10,1");
		RS.execute();

		final TImgRO outImg2 = RS.ExportImages(outImg)[0];
		TIPLTestingLibrary.doSlicesMatchI(outImg2, 0, testImg, 6);

	}

	/**
	 * Test dimensions of output image
	 */
	@Test
	public void testOutDim2() {
		// offset lines
		final TImgRO testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.LinesFunction());

		final ITIPLPluginIO RS = makeRS(pluginId,testImg);
		RS.setParameter("-pos=5,5,1 -dim=5,5,2");
		RS.execute();
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		TIPLTestingLibrary.checkDimensions(outImg, new D3int(5, 5, 2), new D3int(5, 5, 1));
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
		final ITIPLPluginIO RS = makeRS(pluginId,testImg);
		RS.setParameter("-pos=5,5,5 -dim=5,5,1");
		RS.execute();
		final TImgRO outImg = RS.ExportImages(testImg)[0];
		if(TIPLTestingLibrary.verbose) System.out.println("Current Dimensions, pos:"+outImg.getPos()+", dim:"+outImg.getDim());
		TIPLTestingLibrary.checkDimensions(outImg, new D3int(5, 5, 1),new D3int(5, 5, 5));
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
		ITIPLPluginIO RS;
		for (final TImgRO testImg : testImgs) {
			System.out.println("Testing Image Type: " + testImg.getImageType());
			RS = makeRS(pluginId,testImg);
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
		final ITIPLPluginIO RS = makeRS(pluginId,testImg);

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
		final ITIPLPluginIO RS = makeRS(pluginId,testImg);

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
		testSlicesMatchInt(pluginId,testImg);

	}

	@Test
	public void testSlicesMatchIntPreload() {
		final TImgRO testImg = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.ProgZImage(), 2);
		testSlicesMatchInt(pluginId,testImg);
		final VirtualAim vImg = new VirtualAim(testImg);

		vImg.getIntAim();
		testSlicesMatchBool(pluginId,vImg);

	}

	@Test
	public void testSlicesMatchNormal() {
		final TImgRO testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.DiagonalPlaneFunction());
		testSlicesMatchBool(pluginId,testImg);

	}

	@Test
	public void testSlicesMatchPreload() {
		final TImgRO testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.DiagonalPlaneFunction());
		// TImgRO.FullReadable vImg=TImgTools.makeTImgFullReadable(testImg);
		final VirtualAim vImg = new VirtualAim(testImg);

		vImg.getBoolAim();
		testSlicesMatchBool(pluginId,vImg);
	}

	@Test
	public void testSlicesMatchPreLoaded() {
		final TImgRO testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.DiagonalPlaneFunction());

		testSlicesMatchBool(pluginId,testImg);

	}

}
