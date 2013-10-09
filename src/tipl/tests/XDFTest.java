/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.fail;

import org.junit.Test;

import tipl.formats.TImgRO;
import tipl.tools.XDF;
import tipl.util.TIPLPluginIO;
import tipl.util.TImgTools;

/**
 * @author mader
 * 
 */
public class XDFTest {
	protected static TIPLPluginIO makeXDF(final TImgRO inImage) {
		final TIPLPluginIO XF = new XDF();
		XF.LoadImages(new TImgRO[] { inImage });
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
		final TImgRO testImg = TestFImages.wrapIt(50,
				new TestFImages.EllipsoidFunction(25, 25, 25, 10));
		final TIPLPluginIO XF = makeXDF(testImg);
		XF.setParameter("-iter=2000 -rdfsize=20,20,20");
		XF.execute();
		final TImgRO outImage = XF.ExportImages(testImg)[0];
		TImgTools.WriteTImg(outImage, "/Users/mader/Dropbox/test1.tif");
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
