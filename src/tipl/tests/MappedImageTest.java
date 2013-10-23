/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tipl.formats.MappedImage;
import tipl.formats.TImgRO;
import tipl.formats.MappedImage.StationaryVoxelFunction;

/**
 * @author mader
 *
 */
public class MappedImageTest {
	protected final static StationaryVoxelFunction constantImageFn(final int value) {
		return new StationaryVoxelFunction() {

		@Override
		public double get(final double voxval) {
			return value;
		}

		@Override
		public double[] getRange() {
			return new double[] { 0, 127 };
		}

		@Override
		public String name() {
			return "Dumb Voxel";
		}

		@Override
		public String toString() {
			return name();
		}
	};
	}
	
	protected static class NullImage extends MappedImage {
		protected TImgRO templateData;
		protected int imageType;
		public NullImage(final TImgRO dummyDataset, final int iimageType,
				final int outValue) {

			super(dummyDataset, iimageType, constantImageFn(outValue),true);
		}
	}
	final TImgRO testSphImage = TestPosFunctions.wrapIt(50,
			new TestPosFunctions.EllipsoidFunction(25, 25, 25, 10));
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
	@Test
	public void testStationaryVoxelFunction() {
		assertEquals(constantImageFn(0).get(0),0.0,0.1);
		
		for (int j : (int[]) (new NullImage(testSphImage,2,5)).getPolyImage(0, 2)) {
			assertEquals(j,5);
		}
		
		long nullVoxCount=TIPLTestingLibrary.countVoxelsImage(new NullImage(testSphImage,2,0));
		assertEquals(nullVoxCount,0);
		long fullVoxCount=TIPLTestingLibrary.countVoxelsImage(new NullImage(testSphImage,2,1));
		assertEquals(fullVoxCount,(long) testSphImage.getDim().prod());	
	}
	@Test
	public void test() {
		fail("Not yet implemented"); // TODO
	}

}
