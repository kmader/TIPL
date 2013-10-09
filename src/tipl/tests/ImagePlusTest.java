package tipl.tests;

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
		TImgToImageStack.MakeImageStack(testImg);

		final ImagePlus curImg = TImgToImagePlus.MakeImagePlus(testImg);
		curImg.show();
		try {
			Thread.currentThread();
			Thread.sleep(5000);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
