/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tipl.formats.DirectoryReader;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.TImgRO.TImgFull;
import tipl.util.D3int;
import tipl.util.TImgTools;
import tipl.util.TypedPath;

/**
 * @author maderk
 * 
 */
public class DirectoryIOTest {
	public static final TypedPath dmpPath=new TypedPath("/Users/mader/Dropbox/TIPL/test/io_tests/rec_DMP");
	public static final TypedPath tifPath=new TypedPath("/Users/mader/Dropbox/TIPL/test/io_tests/rec8bit");
	/** to test if folders with extensions *.tiff can be read as well **/
	public static final TypedPath tiffPath=new TypedPath("/Users/mader/Dropbox/TIPL/test/io_tests/rec8tiff");
	public static final TypedPath tif16Path=new TypedPath("/Users/mader/Dropbox/TIPL/test/io_tests/rec16bit");
	public static final TypedPath isqPath=new TypedPath("/Users/mader/Dropbox/TIPL/test/io_tests/test.isq;2");
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
	//@Test
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
		fail("Not implemented yet");
	}
	/**
	 * Test method for
	 * {@link tipl.formats.DirectoryReader#EvaluateDirectory(java.lang.String)}.
	 */
	@Test
	public void testReadFolder() {
		System.out.println("Testing ReadFolder : tif="+tifPath);
		TImg curImg=TImgTools.ReadTImg(tifPath);
		char[] alles=TImgTools.makeTImgFullReadable(curImg).getByteAim();
		System.out.println(alles+" is loaded");
	}
	@Test
	public void testReadFolder16bit() {
		System.out.println("Testing ReadFolder : tif="+tif16Path);
		TImg curImg=TImgTools.ReadTImg(tif16Path);
		TImgFull curImgFull=new TImgRO.TImgFull(curImg);
		char[] allea=curImgFull.getByteArray(0);
		System.out.println(allea+" is loaded: 0="+allea[0]);
		char[] alles=TImgTools.makeTImgFullReadable(curImg).getByteAim();
		System.out.println(alles+" is loaded");
	}
	@Test
	public void testReadTiffFolder() {
		System.out.println("Testing ReadFolder : tif="+tiffPath);
		TImg curImg=TImgTools.ReadTImg(tiffPath);
		char[] alles=TImgTools.makeTImgFullReadable(curImg).getByteAim();
		System.out.println(alles+" is loaded");
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
