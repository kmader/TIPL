package tipl.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tipl.formats.TImgRO;
import tipl.tools.ComponentLabel;
import tipl.util.D3float;
import tipl.util.TIPLPluginIO;
import tipl.util.TIPLPluginIn;
import tipl.util.TImgTools;

/**
 * Test the component labeling class using a synthetically created image
 * 
 * @author mader
 * 
 */
public class CLTest {
	protected static void checkVals(final TIPLPluginIn CL, final int maxLabel,
			final double avgCount) {
		System.out.println("Maximum Label of CL Image:" + getMax(CL)
				+ ", average count:" + getAvg(CL));
		assertEquals(getMax(CL), maxLabel);
		assertEquals(getAvg(CL), avgCount, 0.1);
	}

	protected static double getAvg(final TIPLPluginIn iP) {
		return ((Double) iP.getInfo("avgcount")).doubleValue();
	}

	protected static int getMax(final TIPLPluginIn iP) {
		return ((Integer) iP.getInfo("maxlabel")).intValue();
	}

	protected static TIPLPluginIO makeCL(final TImgRO sImg) {
		final TIPLPluginIO CL = new ComponentLabel();
		CL.LoadImages(new TImgRO[] { sImg });
		return CL;
	}

	@Test
	public void testComponentLimit() {
		System.out.println("Testing Component volume limits");
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.DiagonalPlaneAndDotsFunction());
		ComponentLabel CL = (ComponentLabel) makeCL(testImg);
		final D3float voxSize = new D3float(1.0, 1.0, 1.0);
		CL.setParameter("-kernel=2 -sphradius=1.0");
		// 1 volumed things
		CL.runVolume(voxSize, 0, 1000);
		checkVals(CL, 393, 1.4);
		// remove the smallest component
		CL = (ComponentLabel) makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.0");
		// 1 volumed things
		CL.runVolume(voxSize, 0, 162);
		checkVals(CL, 392, 1);

		// remove it with the export mask
		CL = (ComponentLabel) makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.0");
		CL.runVoxels(0);
		checkVals(CL, 393, 1.4);
		// just the small objects
		final TImgRO outImageSmall = CL.ExportMaskAimVolume(
				TImgTools.makeTImgExportable(testImg), voxSize, 0, 162);
		final ComponentLabel CLsmall = (ComponentLabel) makeCL(outImageSmall);
		CLsmall.setParameter("-kernel=2 -sphradius=1.0");
		CLsmall.runVoxels(0);
		checkVals(CLsmall, 392, 1);

		// just the big object
		final TImgRO outImageBig = CL.ExportMaskAimVolume(
				TImgTools.makeTImgExportable(testImg), voxSize, 162);
		final ComponentLabel CLbig = (ComponentLabel) makeCL(outImageBig);
		CLbig.setParameter("-kernel=2 -sphradius=1.0");
		CLbig.runVoxels(0);
		checkVals(CLbig, 1, 163);
		System.out.println("Made it to end!");
	}

	/**
	 * Test method for {@link tipl.tools.ComponentLabel#execute()}.
	 */
	@Test
	public void testExecute() {
		System.out.println("Testing execute");
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.SheetImageFunction());
		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test2.tif");
		final TIPLPluginIn CL = makeCL(testImg);
		CL.execute();
		checkVals(CL, 5, 100);

	}

	/**
	 * Test method for {@link tipl.tools.ComponentLabel#runVoxels(int, int)}.
	 */
	// @Test
	public void testRunVoxelsIntInt() {
		System.out.println("Testing runVoxelsIntInt");
		final TImgRO testImg = TestFImages.wrapIt(10,
				new TestFImages.DiagonalPlaneFunction());
		TIPLPluginIn CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.72");
		CL.execute("runVoxels", new Integer(0));
		checkVals(CL, 1, 10);

		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.72");
		CL.execute("runVoxels", new Integer(2));
		checkVals(CL, 3, 12.67);

		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.72");
		CL.execute("runVoxels", new Integer(6));
		checkVals(CL, 2, 16);

		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.72");
		CL.execute("runVoxels", new Integer(12));
		checkVals(CL, 1, 20);

		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.72");
		CL.execute("runVoxels", new Integer(20));
		checkVals(CL, 0, 0);

	}

	/**
	 * Test spherical radius.
	 */
	@Test
	public void testSphRadius() {
		// offset lines
		TImgRO testImg = TestFImages
				.wrapIt(10, new TestFImages.LinesFunction());
		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		TIPLPluginIO CL = makeCL(testImg);
		System.out.println("Testing SphRadius");

		CL.setParameter("-kernel=2 -sphradius=1.0");
		CL.execute();
		checkVals(CL, 50, 10);

		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.42");
		CL.execute();
		checkVals(CL, 1, 500);

		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.74");
		CL.execute();
		checkVals(CL, 1, 500);

		testImg = TestFImages.wrapIt(10, new TestFImages.DotsFunction());
		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.00");
		CL.execute();
		checkVals(CL, 500, 1);

		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.42");
		CL.execute();
		checkVals(CL, 1, 500);

		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.74");
		CL.execute();
		checkVals(CL, 1, 500);

		testImg = TestFImages.wrapIt(10,
				new TestFImages.DiagonalPlaneFunction());
		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.00");
		CL.execute();
		checkVals(CL, 55, 1);

		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.42");
		CL.execute();
		checkVals(CL, 1, 55);

		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.74");
		CL.execute();
		checkVals(CL, 1, 55);

		testImg = TestFImages
				.wrapIt(10, new TestFImages.DiagonalLineFunction());
		TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.00");
		CL.execute();
		checkVals(CL, 10, 1);

		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.42");
		CL.execute();
		checkVals(CL, 10, 1);

		CL = makeCL(testImg);
		CL.setParameter("-kernel=2 -sphradius=1.74");
		CL.execute();
		checkVals(CL, 1, 10);
	}

}
