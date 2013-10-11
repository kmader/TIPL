/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.*;
import ij.ImagePlus;

import org.junit.Test;

import tipl.formats.TImgRO;
import tipl.ij.TImgToImagePlus;

/**
 * @author mader
 *
 */
public class VolumeViewerTests {

	/**
	 * Test method for {@link tipl.ij.volviewer.Volume_Viewer#run(java.lang.String)}.
	 */
	@Test
	public void testRunString() {
		final String testName="testSphLayer";
		final TestPosFunctions bgLayers= new TestPosFunctions.LayeredImage(1, 2, 25,0,0);
		final TestPosFunctions densePart=  new TestPosFunctions.EllipsoidFunction(75, 75, 75,
				10, 10, 10); 
		final TImgRO testImg = TestPosFunctions
				.wrapIt(150, new TestPosFunctions.BGPlusPhase(bgLayers, densePart, 3) );
		final TImgToImagePlus curImg = TImgToImagePlus.MakeImagePlus(testImg);
		curImg.render("");
		
		fail("Not yet implemented"); // TODO
	}

}
