/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import tipl.scripts.UFEM;
import tipl.util.TIPLGlobal;

import org.junit.BeforeClass;

/**
 * @author mader
 *
 */
public class UFEMtests {
	@BeforeClass
	public static void setup() {
		TIPLGlobal.setDebug(TIPLGlobal.DEBUG_BASIC);
	}
	/**
	 * Test method for {@link tipl.scripts.UFEM#makePoros(tipl.formats.TImg)}.
	 */
	@Test
	public void testMakePoros() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link tipl.scripts.UFEM#UFEM(tipl.util.ArgumentParser)}.
	 */
	@Test
	public void testUFEM() {
		UFEM.main(new String[] {"-ufilt=/Users/mader/Dropbox/tipl/test/simplefoam/distmap.tif","-thresh=15","-resample"});
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link tipl.scripts.UFEM#makePreview(java.lang.String, tipl.formats.TImg)}.
	 */
	@Test
	public void testMakePreview() {
		fail("Not yet implemented"); // TODO
	}

}
