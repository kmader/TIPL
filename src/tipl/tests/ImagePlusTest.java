package tipl.tests;

import static org.junit.Assert.*;
import ij.ImagePlus;
import ij.ImageStack;

import org.junit.Test;

import tipl.formats.TImgRO;
import tipl.ij.TImgToImagePlus;
import tipl.ij.TImgToImageStack;

public class ImagePlusTest {

	@Test
	public void testMakeImagePlus() {
		final TImgRO testImg = TestFImages.wrapIt(100,
				new TestFImages.DiagonalPlaneFunction());
		ImageStack curStack=TImgToImageStack.MakeImageStack(testImg);
		//ij.IJ.
		ImagePlus curImg=TImgToImagePlus.MakeImagePlus(testImg);
		curImg.show();
		try {
		Thread.currentThread().sleep(5000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
