package tipl.tests;

import ij.ImagePlus;

import org.junit.Test;

import tipl.formats.TImgRO;
import tipl.ij.TImgToImagePlus;
import tipl.ij.TImgToImageStack;

public class ImagePlusTest {

	@Test
	public void testMakeLayeredTest() {
		final TestPosFunctions bgLayers= new TestPosFunctions.LayeredImage(1, 2, 25,0,0);
		final TestPosFunctions densePart=  new TestPosFunctions.EllipsoidFunction(75, 75, 75,
				10, 10, 10); 
		final TestPosFunctions comboFun=new TestPosFunctions.BGPlusPhase(bgLayers, densePart, 3);
		comboFun.setRotation(45, 0,75,75,75);
		final TImgRO testImg = TestPosFunctions
				.wrapIt(150, comboFun );

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
		final TestPosFunctions elFun=new TestPosFunctions.EllipsoidFunction(100, 200, 500,
					100, 200, 500);
		elFun.setRotation(45, 0, 100, 200, 500);
		final TImgRO testImg = TestPosFunctions
				.wrapIt(400,elFun);
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

	// @Test
	public void testMakeImagePlus() {
		final TImgRO testImg = TestPosFunctions.wrapIt(100,
				new TestPosFunctions.DiagonalPlaneFunction());
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
