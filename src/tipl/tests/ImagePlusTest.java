package tipl.tests;

import ij.ImagePlus;
import ij.ImageStack;

import org.junit.Test;

import tipl.formats.TImgRO;
import tipl.ij.TImgToImagePlus;
import tipl.ij.TImgToImageStack;

public class ImagePlusTest {

	//@Test
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
	@Test
	public void testMakeEllipsoidImagePlus() {
		final TImgRO testImg = TestFImages.wrapIt(400,
				new TestFImages.EllipsoidFunction(100,200,500,100,200,500));
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
