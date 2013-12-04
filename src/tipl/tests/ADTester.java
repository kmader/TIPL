/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tipl.blocks.FilterBlock;
import tipl.blocks.ITIPLBlock;
import tipl.util.ArgumentDialog;
import tipl.util.ArgumentParser;
import tipl.util.D3float;

/**
 * @author mader
 * 
 */
public class ADTester {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link tipl.util.ArgumentDialog#GUIBlock()}.
	 */
	//@Test
	public void testGUIBlock() {
		// fail("Not yet implemented"); // TODO
		ITIPLBlock cBlock = new FilterBlock();
		cBlock = ArgumentDialog.GUIBlock(cBlock);

	}

	/**
	 * Test method for {@link tipl.util.ArgumentDialog#scrapeDialog()}.
	 */
	@Test
	public void testScrapeDialog() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link tipl.util.ArgumentParser#getOptionBoolean(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public void testToDialog() {
		final String[] strArr = new String[] { "-ted", "-bob=10",
				"-scale=1.0,1.0,1.0", "-tandy=0.1", "-david=joey" };
		final ArgumentParser p = new ArgumentParser(strArr);
		assertEquals(p.getOptionBoolean("ted", "Test-Help"), true);
		assertEquals(p.getOptionInt("bob", 10, "Test-Help", 10, 11), 10);
		final D3float fArgs = p
				.getOptionD3float("scale", new D3float(), "junk");
		assertEquals(fArgs.x, 1.0, .1);
		assertEquals(fArgs.y, 1.0, .1);
		assertEquals(fArgs.z, 1.0, .1);
		assertEquals(p.getOptionDouble("tandy", 0.1, "Test-Help"), 0.1, 0.01);
		assertEquals(p.getOptionString("david", "nudda", "Test-Help David"),
				"joey");
		final ArgumentDialog a = new ArgumentDialog(p, "whaddup",
				"nohelpforyou");

		System.out.println(a.scrapeDialog());
	}

}
