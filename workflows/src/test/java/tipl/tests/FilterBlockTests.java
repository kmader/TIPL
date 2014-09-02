/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.fail;

import org.junit.Test;

import tipl.blocks.BaseTIPLBlock;

/**
 * @author mader
 *
 */
public class FilterBlockTests {
	public static final String dmpPath="/Users/mader/Dropbox/TIPL/test/io_tests/rec_DMP";
	public static final String tifPath="/Users/mader/Dropbox/TIPL/test/io_tests/rec8bit";
	public static final String tif16Path="/Users/mader/Dropbox/TIPL/test/io_tests/rec16bit";
	public static final String isqPath="/Users/mader/Dropbox/TIPL/test/io_tests/test.isq;2";
	@Test
	public void testTifFilter() {
		String curArgs="-blockname=tipl.blocks.FilterBlock -ufilt="+tif16Path+" -gfilt="+tif16Path+"_gfilt.tif -filter=4";
		System.out.println("Simulate:"+curArgs);
		BaseTIPLBlock.main(curArgs.split(" "));
	}
	@Test
	public void test() {
		fail("Not yet implemented"); // TODO
	}

}
