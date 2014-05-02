/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tipl.util.ArgumentParser;
import tipl.util.D3float;
import tipl.util.D3int;

/**
 * @author maderk
 * 
 */
public class ArgumentParserTest {

	/**
	 * Test method for
	 * {@link tipl.util.ArgumentParser#ArgumentParser(java.lang.String[])}.
	 */
	@Test
	public void testArgumentParser() {
		final String[] strArr = new String[] { "bob", "dan", "joe" };
		final ArgumentParser p = new ArgumentParser(strArr,true);
		assertEquals(p.ParamCount(), 3);
	}

	/**
	 * Test method for
	 * {@link tipl.util.ArgumentParser#getOptionBoolean(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public void testGetOptionBoolean() {
		final String[] strArr = new String[] { "-ted", "-teddy", "-bob=false" };
		final ArgumentParser p = new ArgumentParser(strArr,true);
		assertEquals(p.getOptionBoolean("ted", "Test-Help"), true);
		assertEquals(p.getOptionBoolean("teddy", "Test-Help"), true);
		assertEquals(p.getOptionBoolean("tedy", "Test-Help"), false);
		assertEquals(p.getOptionBoolean("bob", "Test-Help"), false);
	}

	/**
	 * Test method for
	 * {@link tipl.util.ArgumentParser#getOptionD3float(java.lang.String, tipl.util.D3float, java.lang.String)}
	 * .
	 */
	@Test
	public void testGetOptionD3float() {
		final String[] strArr = new String[] { "-scale=1.0,1.0,1.0",
				"-object=(99e3,28e2,29e1)" };
		final ArgumentParser p = new ArgumentParser(strArr,true);
		final D3float fArgs = p
				.getOptionD3float("scale", new D3float(), "junk");
		assertEquals(fArgs.x, 1.0, .1);
		assertEquals(fArgs.y, 1.0, .1);
		assertEquals(fArgs.z, 1.0, .1);
		final D3float gArgs = p.getOptionD3float("object", new D3float(),
				"junk");
		assertEquals(gArgs.x, 99e3, 1);
		assertEquals(gArgs.y, 28e2, .1);
		assertEquals(gArgs.z, 29e1, .1);
	}

	/**
	 * Test method for
	 * {@link tipl.util.ArgumentParser#getOptionD3int(java.lang.String, tipl.util.D3int, java.lang.String)}
	 * .
	 */
	@Test
	public void testGetOptionD3int() {
		final String[] strArr = new String[] { "-scale=(1,2,3)", "-bale=3,4,5" };
		final ArgumentParser p = new ArgumentParser(strArr,true);
		D3int fArgs = p.getOptionD3int("scale", new D3int(1, 1, 1), "junk");
		assertEquals(fArgs.x, 1);
		assertEquals(fArgs.y, 2);
		assertEquals(fArgs.z, 3);
		fArgs = p.getOptionD3int("bale", new D3int(1, 1, 1), "junk");
		assertEquals(fArgs.x, 3);
		assertEquals(fArgs.y, 4);
		assertEquals(fArgs.z, 5);
	}

	/**
	 * Test method for
	 * {@link tipl.util.ArgumentParser#getOptionDouble(java.lang.String, double, java.lang.String)}
	 * .
	 */
	@Test
	public void testGetOptionDoubleStringDoubleString() {
		final String[] strArr = new String[] { "-ted=0.1" };
		final ArgumentParser p = new ArgumentParser(strArr,true);
		assertEquals(p.getOptionDouble("ted", 0.1, "Test-Help"), 0.1, 0.01);
	}

	/**
	 * Test method for
	 * {@link tipl.util.ArgumentParser#getOptionDouble(java.lang.String, double, java.lang.String, double, double)}
	 * .
	 */
	@Test
	public void testGetOptionDoubleStringDoubleStringDoubleDouble() {
		final String[] strArr = new String[] { "-ted=0.1" };
		final ArgumentParser p = new ArgumentParser(strArr,true);
		assertEquals(p.getOptionDouble("ted", 0.1, "Test-Help", 0.0, 0.2), 0.1,
				0.01);
	}

	/**
	 * Test method for
	 * {@link tipl.util.ArgumentParser#getOptionInt(java.lang.String, int, java.lang.String)}
	 * .
	 */
	@Test
	public void testGetOptionIntStringIntString() {
		final String[] strArr = new String[] { "-bob=5" };
		final ArgumentParser p = new ArgumentParser(strArr,true);
		assertEquals(p.getOptionInt("bob", 1, "Test-Help"), 5);
	}

	/**
	 * Test method for
	 * {@link tipl.util.ArgumentParser#getOptionInt(java.lang.String, int, java.lang.String, int, int)}
	 * .
	 */
	@Test
	public void testGetOptionIntStringIntStringIntInt() {
		final String[] strArr = new String[] { "-bob=10" };
		final ArgumentParser p = new ArgumentParser(strArr,true);
		assertEquals(p.getOptionInt("bob", 10, "Test-Help", 10, 11), 10);
	}

}
