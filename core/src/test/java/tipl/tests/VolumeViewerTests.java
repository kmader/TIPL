/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.fail;

import org.junit.Test;

import tipl.formats.TImgRO;
import tipl.ij.ImageStackToTImg;
import tipl.ij.TImgToImagePlus;
import tipl.ij.volviewer.Volume_Viewer;

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
		final TestPosFunctions comboFun = new TestPosFunctions.BGPlusPhase(bgLayers, densePart, 3);
		final TImgRO testImg = TestPosFunctions
				.wrapIt(150, comboFun);
		final TImgToImagePlus curImg = TImgToImagePlus.MakeImagePlus(testImg);
		curImg.setDisplayRange(comboFun.getRange()[0], comboFun.getRange()[1]);
		
		Volume_Viewer vv=new Volume_Viewer();
		vv.tiplShowView(ImageStackToTImg.FromImagePlus(curImg));
		
		fail("Not yet implemented"); // TODO
	}

}
