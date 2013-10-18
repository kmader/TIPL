/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import tipl.formats.FNImage;
import tipl.formats.TImgRO;
import tipl.util.TImgTools;

/**
 * Test the FNImage package which is used to combine multiple images together in an efficient manner
 * @author mader
 *
 */
public class FNImageTest {
	@Test
	public void testSphPhaseImage() {
		final String testName="testSphLayer";
		
		final TestPosFunctions sphPartA=  new TestPosFunctions.EllipsoidFunction(5, 5, 5,
				2, 2, 2); 
		final TestPosFunctions sphPartB=  new TestPosFunctions.EllipsoidFunction(10, 10, 10,
				2, 2, 2); 
		final TImgRO sphImgA = TestPosFunctions
				.wrapIt(20,sphPartA);
		final TImgRO sphImgB = TestPosFunctions
				.wrapIt(20,sphPartB);
		TImgRO phaseMap=new FNImage(new TImgRO[] {sphImgA,sphImgB},2,new FNImage.PhaseImage(new double[] {10,20}));
		
		TImgRO revPhaseMap=new FNImage(new TImgRO[] {sphImgB,sphImgA},2,new FNImage.PhaseImage(new double[] {20,10}));
		TIPLTestingLibrary.doImagesMatch(phaseMap, revPhaseMap);
		TImgTools.WriteTImg(phaseMap, "/Users/mader/Dropbox/TIPL/build/test.tif");
		TImgTools.WriteTImg(revPhaseMap, "/Users/mader/Dropbox/TIPL/build/revtest.tif");
		TImgTools.WriteTImg(sphImgB, "/Users/mader/Dropbox/TIPL/build/testbg.tif");
	}
	@Test
	public void test() {
		final TestPosFunctions bgLayers= new TestPosFunctions.LayeredImage(0, 5, 5);
		fail("Not yet implemented"); // TODO
	}

}
