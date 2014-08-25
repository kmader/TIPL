/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.*;

import org.apache.spark.api.java.JavaSparkContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import tipl.formats.PureFImage;
import tipl.formats.TImgRO;
import tipl.spark.DTImg;
import tipl.spark.SparkGlobal;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

/**
 * @author mader
 *
 */
public class DTImgTests {
	final TImgRO lineImage = TestPosFunctions.wrapItAs(10,
			new TestPosFunctions.LinesFunction(),TImgTools.IMAGETYPE_FLOAT);
	final TImgRO lineImageDouble = TestPosFunctions.wrapItAs(10,
			new TestPosFunctions.LinesFunction(),TImgTools.IMAGETYPE_DOUBLE);
	final TImgRO pointImage = TestPosFunctions.wrapItAs(10,
			new TestPosFunctions.SinglePointFunction(5, 5, 5),TImgTools.IMAGETYPE_FLOAT);
	private static JavaSparkContext jsc;
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		jsc = SparkGlobal.getContext("DTImgTests");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}
	final int zsize = 10;
	/**
	 * 
	 */
	@Test
	public void testAllTypes() {
		for(int cType: TImgTools.IMAGETYPES) {
			final TImgRO curTestImage = TestPosFunctions.wrapItAs(zsize,
					new TestPosFunctions.SinglePointFunction(5, 5, 5),cType);
			DTImg curImage = DTImg.ConvertTImg(jsc, curTestImage, cType);
			int[] intSlice = (int[]) curImage.getPolyImage(5, TImgTools.IMAGETYPE_INT);
			assert(intSlice[5*10+5]>0);
			for(int i = 0; i<zsize;i++ ) {
				double[] dblSlice = (double[]) curImage.getPolyImage(i, TImgTools.IMAGETYPE_DOUBLE);
				if(i==5) assert(dblSlice[5*10+5]>0);
				else assert(dblSlice[5*10+5]<1);
			}
		}
		
	}
	
	/**
	 * Test method for {@link tipl.spark.DTImg#ConvertTImg(org.apache.spark.api.java.JavaSparkContext, tipl.formats.TImgRO, int)}.
	 */
	@Test
	public void testConvertTImg() {
		
		final TImgRO curTestImage = TestPosFunctions.wrapItAs(zsize,
				new TestPosFunctions.SinglePointFunction(5, 5, 5),TImgTools.IMAGETYPE_DOUBLE);
		DTImg<double[]> curImage = DTImg.ConvertTImg(jsc, curTestImage, TImgTools.IMAGETYPE_DOUBLE);
		// apply a function to the image to make sure it still works
		DTImg<double[]> newImage = curImage.applyVoxelFunction(PureFImage.fromPositionFunction(new PureFImage.ZFunc(curTestImage)));
		for(int i = 0; i < zsize;i++ ) {
			double[] dblSlice = (double[]) newImage.getPolyImage(i, TImgTools.IMAGETYPE_DOUBLE);
			if (TIPLGlobal.getDebug()) System.out.println("Current Slice:"+i+" => "+dblSlice[1]);
			assertEquals(i,dblSlice[1],0.1);
		}
	}

	/**
	 * Test method for {@link tipl.spark.DTImg#ReadObjectFile(org.apache.spark.api.java.JavaSparkContext, java.lang.String, int)}.
	 */
	//@Test
	public void testReadObjectFile() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link tipl.spark.DTImg#getPolyImage(int, int)}.
	 */
	//@Test
	public void testGetPolyImage() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link tipl.spark.DTImg#asDTDouble()}.
	 */
	//@Test
	public void testAsDTDouble() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link tipl.spark.DTImg#asDTBool()}.
	 */
	//@Test
	public void testAsDTBool() {
		fail("Not yet implemented"); // TODO
	}

}
