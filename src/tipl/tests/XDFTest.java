/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import tipl.formats.TImgRO;
import tipl.tools.Resize;
import tipl.tools.XDF;
import tipl.util.TIPLPluginIO;

/**
 * @author mader
 *
 */
public class XDFTest {
	protected static TIPLPluginIO makeXDF(TImgRO inImage) {
		final TIPLPluginIO RS = new XDF();
		RS.LoadImages(new TImgRO[] { inImage });
		return RS;
	}
	/**
	 * Test method for {@link tipl.tools.XDF#CreateLabeledXDF(tipl.formats.VirtualAim, tipl.formats.VirtualAim, tipl.util.D3int, int, int)}.
	 */
	@Test
	public void testCreateLabeledXDF() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link tipl.tools.XDF#WriteHistograms(tipl.tools.XDF, tipl.formats.TImgRO.CanExport, java.lang.String)}.
	 */
	@Test
	public void testWriteHistograms() {
		fail("Not yet implemented"); // TODO
	}

}
