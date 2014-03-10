package tipl.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tipl.formats.TImgRO;
import tipl.tools.ComponentLabel;
import tipl.tools.GrayAnalysis;
import tipl.util.D3float;
import tipl.util.ITIPLPlugin;
import tipl.util.ITIPLPluginIO;
import tipl.util.ITIPLPluginIn;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

/**
 * Test the component labeling class using a synthetically created image
 * 
 * @author mader
 * 
 */
public class GrayAnalysisTest {
	protected static void checkVals(final ITIPLPluginIn CL, final int maxLabel,
			final double avgCount) {
		System.out.println("Maximum Label of CL Image:" + getMax(CL)
				+ ", average count:" + getAvg(CL));
		assertEquals(maxLabel,getMax(CL));
		assertEquals(avgCount,getAvg(CL), 0.1);
	}

	protected static double getAvg(final ITIPLPluginIn iP) {
		return ((Double) iP.getInfo("avgcount")).doubleValue();
	}

	protected static int getMax(final ITIPLPluginIn iP) {
		return ((Integer) iP.getInfo("maxlabel")).intValue();
	}

	protected static ITIPLPluginIO makeCL(final TImgRO sImg) {
		final ITIPLPluginIO CL = new ComponentLabel();
		CL.LoadImages(new TImgRO[] { sImg });
		return CL;
	}

	
	/**
	 * Test method for {@link tipl.tools.ComponentLabel#execute()}.
	 */
	@Test
	public void testExecute() {
		System.out.println("Testing execute");
		final TImgRO testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.SheetImageFunction());
		final ITIPLPluginIO CL = makeCL(testImg);
		CL.execute();
		TImgRO labelImage=CL.ExportImages(testImg)[0];
		ITIPLPlugin cGA=GrayAnalysis.StartLacunaAnalysis(labelImage,"/tmp/junk.txt","First Run");
		if (TIPLGlobal.getDebug()) {
			System.out.println("Groups:"+cGA.getInfo("groups"));
			System.out.println("Average X Position:"+cGA.getInfo("average,meanx"));
			System.out.println("Average Y Position:"+cGA.getInfo("average,meany"));
			System.out.println("Average Z Position:"+cGA.getInfo("average,meanz"));
		}
		assertEquals(5.0,((Double) cGA.getInfo("average,meanx")).doubleValue(),0.1);
		assertEquals(4.5,((Double) cGA.getInfo("average,meany")).doubleValue(),0.1);
		assertEquals(4.5,((Double) cGA.getInfo("average,meanz")).doubleValue(),0.1);
		assertEquals(5,((Long) cGA.getInfo("groups")).longValue());
		checkVals(CL, 5, 100);

	}

	/**
	 * Test method for {@link tipl.tools.ComponentLabel#runVoxels(int, int)}.
	 */
	// @Test
	public void testRunVoxelsIntInt() {
		System.out.println("Testing runVoxelsIntInt");
		final TImgRO testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.DiagonalPlaneFunction());
		ITIPLPluginIn CL = makeCL(testImg);
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
	//@Test
	public void testSphRadius() {
		// offset lines
		TImgRO testImg = TestPosFunctions
				.wrapIt(10, new TestPosFunctions.LinesFunction());
		// TImgTools.WriteTImg(testImg, "/Users/mader/Dropbox/test.tif");
		ITIPLPluginIO CL = makeCL(testImg);
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

		testImg = TestPosFunctions.wrapIt(10, new TestPosFunctions.DotsFunction());
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

		testImg = TestPosFunctions.wrapIt(10,
				new TestPosFunctions.DiagonalPlaneFunction());
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

		testImg = TestPosFunctions
				.wrapIt(10, new TestPosFunctions.DiagonalLineFunction());
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
