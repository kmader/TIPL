/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tipl.formats.DirectoryReader;
import tipl.formats.TImg;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * @author maderk
 * 
 */
public class DirectoryIOTest {
	public static final String dmpPath="/Users/mader/Dropbox/TIPL/test/io_tests/rec_DMP";
	public static final String tifPath="/Users/mader/Dropbox/TIPL/test/io_tests/rec8bit";
	public static final String isqPath="/Users/mader/Dropbox/TIPL/test/io_tests/test.isq;2";
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
	 * Test method for
	 * {@link tipl.formats.DirectoryReader#ChooseBest(java.lang.String)}.
	 */
	@Test
	public void testChooseBest() {
		DirectoryReader curReader=DirectoryReader.ChooseBest(dmpPath);
		System.out.println("Reading folder:"+dmpPath+" as "+curReader);
		assert(curReader instanceof tipl.formats.DMPFolder);
		
		curReader=DirectoryReader.ChooseBest(tifPath);
		System.out.println("Reading folder:"+tifPath+" as "+curReader);
		assert(curReader instanceof tipl.formats.TiffFolder);
	}
	
	/**
	 * Test method for
	 * {@link tipl.formats.DirectoryReader#EvaluateDirectory(java.lang.String)}.
	 */
	@Test
	public void testEvaluateDirectory() {
	}

	/**
	 * Test method for {@link tipl.formats.DirectoryReader#getDim()}.
	 */
	@Test
	public void testGetDim() {
		System.out.println("Reading dmp:"+dmpPath);
		TImg curImg=TImgTools.ReadTImg(dmpPath);
		TIPLTestingLibrary.checkDim(curImg, new D3int(2016,361,6));
		System.out.println("Reading tif:"+tifPath);
		curImg=TImgTools.ReadTImg(tifPath);
		TIPLTestingLibrary.checkDim(curImg, new D3int(1000,109,5));
		System.out.println("Reading isq:"+isqPath);
		curImg=TImgTools.ReadTImg(isqPath);
		TIPLTestingLibrary.checkDim(curImg, new D3int(256,41,41));
	}

}
