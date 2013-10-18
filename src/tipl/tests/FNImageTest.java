/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import tipl.formats.FNImage;
import tipl.formats.TImgRO;
import tipl.formats.FNImage.VFNGenerator;
import tipl.formats.FNImage.VoxelFunctionN;
import tipl.util.TImgTools;

/**
 * Test the FNImage package which is used to combine multiple images together in an efficient manner
 * @author mader
 *
 */
public class FNImageTest {
	final TestPosFunctions sphPartA = new TestPosFunctions.EllipsoidFunction(5, 5, 5,
			2, 2, 2); 
	final TestPosFunctions sphPartB = new TestPosFunctions.EllipsoidFunction(10, 10, 10,
			2, 2, 2); 
	
	final TestPosFunctions teenyPart2 = new TestPosFunctions.EllipsoidFunction(1, 1, 1,
			1,1,1); 
	final TestPosFunctions teenyPart3 = new TestPosFunctions.EllipsoidFunction(1, 1, 1,
			2, 2, 2); 
	
	final TImgRO sphImgA = TestPosFunctions
			.wrapIt(20,sphPartA);
	final TImgRO sphImgB = TestPosFunctions
			.wrapIt(20,sphPartB);
	
	final TImgRO teenyImg2 = TestPosFunctions
			.wrapIt(3,teenyPart2);
	final TImgRO teenyImg3 = TestPosFunctions
			.wrapIt(3,teenyPart3);
	
	/**
	 * A test VoxelFunctionN class for debugging code
	 * @author mader
	 *
	 */
	public static class TestVerboseImageFunction implements FNImage.VFNGenerator {
		@Override
		public VoxelFunctionN get() {
			return new VoxelFunctionN() {
				int cDex=0;
				double[] vals=new double[2];
				double cValue = 0.0;
				
				@Override
				public void add(final Double[] ipos, final double v) {
					vals[cDex]=v;
					cDex++;
					System.out.println("Adding value @("+ipos[0]+","+ipos[1]+","+ipos[2]+"):"+v+" to existing value:"+cValue);
					System.out.println("Val = ("+vals[0]+","+vals[1]+"):");
					cValue += v;
				}

				@Override
				public double get() {
					return cValue;
				}

				@Override
				public double[] getRange() {
					return FNImage.floatRange;
				}

				@Override
				public String name() {
					return "Add Images (debug)";
				}
			};
		}
	}
	
	@Test 
	public void testVerboseImageFunction() {
		TImgRO testMap=new FNImage(new TImgRO[] {teenyImg2,teenyImg3},2,new TestVerboseImageFunction());
		TImgRO revTestMap=new FNImage(new TImgRO[] {teenyImg3,teenyImg2},2,new TestVerboseImageFunction());
		
		TIPLTestingLibrary.doSlicesMatchB(testMap, 2, revTestMap, 2); // that the first slices are correct
		
	}
	@Test 
	public void testSphAddImage() {
		TImgRO addMap=new FNImage(new TImgRO[] {sphImgA,sphImgB},2,new FNImage.AddImages());
		TImgRO revAddMap=new FNImage(new TImgRO[] {sphImgB,sphImgA},2,new FNImage.AddImages());
		TIPLTestingLibrary.doImagesMatch(addMap, revAddMap); // check that the order does not matter
		
	}
	@Test
	public void testSphPhaseImage() {
		final String testName="testSphLayer";
		TImgRO phaseMap=new FNImage(new TImgRO[] {sphImgA,sphImgB},2,new FNImage.PhaseImage(new double[] {10,20}));
		
		TImgRO revPhaseMap=new FNImage(new TImgRO[] {sphImgB,sphImgA},2,new FNImage.PhaseImage(new double[] {20,10}));
		
		TIPLTestingLibrary.doSlicesMatchB(revPhaseMap, 10, sphImgB, 10); // that the last slices are also correct
		TIPLTestingLibrary.doSlicesMatchB(revPhaseMap, 5, sphImgA, 5); // that the first slices are correct
		
		TIPLTestingLibrary.doImagesMatch(phaseMap, revPhaseMap); // check that the order does not matter
		
	}


}
