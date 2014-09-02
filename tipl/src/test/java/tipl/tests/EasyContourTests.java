/**
 * 
 */
package tipl.tests;

import org.junit.Test;

import tipl.formats.MappedImage;
import tipl.formats.TImgRO;
import tipl.tools.EasyContour;
import tipl.util.TImgTools;
import tipl.util.TypedPath;

/**
 * @author mader
 * 
 */
public class EasyContourTests {
	public static final TypedPath testDir=TIPLTestingLibrary.createTestImage("/Users/mader/Dropbox/TIPL/test/ec_tests/");
	protected static EasyContour makeEC(final TImgRO inImage) {
		final EasyContour EC = new EasyContour(inImage);
		//EC.LoadImages(new TImgRO[] { inImage });
		return EC;
	}

	protected static final TImgRO simpleSphereImg = TestPosFunctions.wrapIt(50,
			new TestPosFunctions.EllipsoidFunction(25, 25, 25, 10));
	/**
	 * Test method for an sphere using a polygon
	 */
	@Test
	public void testSpherePoly4() {
		final String testName="testSph";

		final EasyContour ec = makeEC(simpleSphereImg);
		ec.usePolygon(4);
		ec.execute();
		final TImgRO outImage = ec.ExportImages(simpleSphereImg)[0];
		TImgTools.WriteTImg(outImage, testDir.append(testName+"_poly.tif"));
		
	}
	/**
	 * Test method for an sphere using a polygon
	 */
	@Test
	public void testInvSpherePoly4() {
		final String testName="invTestSph";
		TImgRO invSph=new MappedImage.InvertImage(simpleSphereImg, 2, 127);
		final EasyContour ec = makeEC(TImgTools.WrapTImgRO(invSph));
		ec.usePolygon(4);
		ec.execute();
		final TImgRO outImage = ec.ExportImages(invSph)[0];
		TImgTools.WriteTImg(outImage, testDir.append(testName+"_poly2.tif"));
		
	}
	
	/**
	 * Test method for a simple sphere
	 */
	@Test
	public void testSphereFixedCircle() {
		final String testName="testSph";
		
		final EasyContour ec = makeEC(simpleSphereImg);
		ec.useFixedCirc();
		
		ec.execute();
		final TImgRO outImage = ec.ExportImages(simpleSphereImg)[0];
		TImgTools.WriteTImg(outImage, testDir.append(testName+"_fixed.tif"));
		
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
		
		
	}
	


}
