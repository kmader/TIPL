/**
 * 
 */
package tipl.tests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import tipl.blocks.*;
import tipl.util.ArgumentDialog;
import tipl.util.ArgumentParser;
import tipl.util.D3float;
import tipl.util.TIPLGlobal;

import static org.junit.Assert.assertEquals;

/**
 * @author mader
 * 
 */
public class BlockIOTests {

	/**
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		// disable headlessness

	}

	/**
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link LocalTIPLBlock}.
	 */
	@Test
	public void testLocalIOBlock() {

	}

	@Test
	public void testBaseBlockRunner() {
		ArgumentParser p = TIPLGlobal.activeParser(new String[] {});
		BaseBlockRunner bbr = new BaseBlockRunner();
		bbr.add(new ResizeBlock("resizeB:"));
		bbr.setParameter(p);
		bbr.execute();
	}


}
