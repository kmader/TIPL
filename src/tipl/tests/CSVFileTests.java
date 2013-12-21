/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import tipl.util.CSVFile;

/**
 * @author mader
 *
 */
public class CSVFileTests {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Test method for {@link tipl.util.CSVFile#ReadColumns(int)}.
	 */
	@Test
	public void testReadColumns() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link tipl.util.CSVFile#FromString(java.lang.String[], int)}.
	 */
	@Test
	public void testFromString() {
		final String[] testInput=new String[] {"cola,colb,colc","1,2,3","4,5,6,-1","7,8,9","10,11"};
		CSVFile ns=CSVFile.FromString(testInput, 1);
		assertEquals(ns.getRawHeader(0),testInput[0]);
		assertEquals(ns.readLine().getLine(),testInput[1]);
		assertEquals(ns.readLine().getSplitLine().length,4);
		assertEquals(ns.ReadColumns(1).get("cola").length(),1);
		assertEquals(ns.ReadColumns(1).get("colc").toString(),"Empty Value:0");
	}

	/**
	 * Test method for {@link tipl.util.CSVFile#lineAsDictionary()}.
	 */
	@Test
	public void testLineAsDictionary() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link tipl.util.CSVFile#ZipArrays(java.lang.String[], java.lang.String[], boolean)}.
	 */
	@Test
	public void testZipArrays() {
		fail("Not yet implemented"); // TODO
	}

}
