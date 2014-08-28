/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.fail;

import org.junit.Test;

import tipl.formats.TImgRO;
import tipl.tools.XDF;
import tipl.util.ArgumentList;
import tipl.util.ITIPLPluginIO;
import tipl.util.TImgTools;

/**
 * @author mader
 * 
 */
public class XDFTest {
	public static final ArgumentList.TypedPath testDir=TIPLTestingLibrary.createTestImage("/Users/mader/Dropbox/TIPL/test/xdf_tests/");
	protected static ITIPLPluginIO makeXDF(final TImgRO inImage) {
		final ITIPLPluginIO XF = new XDF();
		XF.LoadImages(new TImgRO[] { inImage });
		return XF;
	}
	protected static ITIPLPluginIO makeXDF(final TImgRO inImage,final TImgRO valueImage) {
		final ITIPLPluginIO XF = new XDF();
		XF.LoadImages(new TImgRO[] { inImage,null,valueImage });
		return XF;
	}

	/**
	 * Test method for
	 * {@link tipl.tools.XDF#CreateLabeledXDF(tipl.formats.VirtualAim, tipl.formats.VirtualAim, tipl.util.D3int, int, int)}
	 * .
	 */
	@Test
	public void testCreateLabeledXDF() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for a simple sphere
	 */
	@Test
	public void testSphereXDF() {
		final String testName="testSph";
		final TImgRO testImg = TestPosFunctions.wrapIt(50,
				new TestPosFunctions.EllipsoidFunction(25, 25, 25, 10));
		final ITIPLPluginIO XF = makeXDF(testImg);
		XF.setParameter("-iter=2000 -rdfsize=20,20,20");
		XF.execute();
		final TImgRO outImage = XF.ExportImages(testImg)[0];
		TImgTools.WriteTImg(outImage, testDir.append(testName+"_img.tif"));
		fail("Not yet implemented"); // TODO
	}
	
	/**
	 * Test method for a fancy layered structure
	 */
	@Test
	public void testSphereLayerXDF() {
		final String testName="testSphLayer";
		final TestPosFunctions bgLayers= new TestPosFunctions.LayeredImage(1, 2, 25,0,0);
		final TestPosFunctions densePart=  new TestPosFunctions.EllipsoidFunction(75, 75, 75,
				10, 10, 10); 
		final TImgRO testImg = TestPosFunctions
				.wrapIt(150, new TestPosFunctions.BGPlusPhase(bgLayers, densePart, 3) );
		TImgTools.WriteTImg(testImg, testDir.append(testName+"_img.tif"));
		ITIPLPluginIO XF = makeXDF(testImg);
		XF.setParameter("-iter=2000 -rdfsize=30,30,30 -asint -inphase=3 -outphase=1");
		XF.execute();
		final TImgRO outImage = XF.ExportImages(testImg)[0];
		TImgTools.WriteTImg(outImage, testDir.append(testName+"_rdf_31.tif"));
		XDF.WriteHistograms(((XDF) XF), TImgTools.makeTImgExportable(testImg), testDir.append(testName+"rdf_31"));
		
		 XF = makeXDF(testImg);
		XF.setParameter("-iter=2000 -rdfsize=30,30,30 -asint -inphase=3 -outphase=2");
		XF.execute();
		final TImgRO outImage2 = XF.ExportImages(testImg)[0];
		TImgTools.WriteTImg(outImage2, testDir.append(testName+"_rdf_32.tif"));
		XDF.WriteHistograms(((XDF) XF), TImgTools.makeTImgExportable(testImg), testDir.append(testName+"_rdf_32"));
		fail("Not yet implemented"); // TODO
	}
	
	/**
	 * Test method for a fancy layered structure and a sphere
	 */
	@Test
	public void testSphereInLayerValFloatXDF() {
		final String testName="testSphLayerWithValFloat";
		final TestPosFunctions bgLayers= new TestPosFunctions.LayeredImage(1, 2, 25,0,0);
		final TestPosFunctions densePart=  new TestPosFunctions.EllipsoidFunction(75, 75, 75,
				10, 10, 10); 
		final TImgRO bgImg = TestPosFunctions
				.wrapIt(150, bgLayers);
		final TImgRO sphImg = TestPosFunctions
				.wrapIt(150, densePart);
		
		ITIPLPluginIO XF = makeXDF(sphImg,bgImg);
		XF.setParameter("-iter=10000 -rdfsize=30,30,0 -valueImageType=3 -usesurface");
		XF.execute();
		final TImgRO outImage = XF.ExportImages(sphImg)[0];
		TImgTools.WriteTImg(outImage, testDir.append(testName+"_rdf.tif"));
		
		fail("Not yet implemented"); // TODO
	}
	
	/**
	 * Test method for a fancy layered structure and a sphere
	 */
	@Test
	public void testSphereInLayerValXDF() {
		final String testName="testSphLayerWithVal";
		final TestPosFunctions bgLayers= new TestPosFunctions.LayeredImage(1, 2, 25,0,0);
		final TestPosFunctions densePart=  new TestPosFunctions.EllipsoidFunction(75, 75, 75,
				10, 10, 10); 
		final TImgRO bgImg = TestPosFunctions
				.wrapIt(150, bgLayers);
		final TImgRO sphImg = TestPosFunctions
				.wrapIt(150, densePart);
		
		ITIPLPluginIO XF = makeXDF(sphImg,bgImg);
		XF.setParameter("-iter=500 -rdfsize=30,30,0 -valueImageType=2 -outphase=2");
		XF.execute();
		final TImgRO outImage = XF.ExportImages(sphImg)[0];
		TImgTools.WriteTImg(outImage, testDir.append(testName+"_rdf.tif"));
		
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link tipl.tools.XDF#WriteHistograms(tipl.tools.XDF, tipl.formats.TImgRO.CanExport, java.lang.String)}
	 * .
	 */
	@Test
	public void testWriteHistograms() {
		fail("Not yet implemented"); // TODO
	}

}
