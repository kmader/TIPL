/**
 *
 */
package tipl.tests;

import org.junit.Test;
import tipl.formats.FNImage;

import static org.junit.Assert.assertEquals;

/**
 * Test the voxel function itself, it makes testing the later steps much much easier
 *
 * @author mader
 */
public class VoxelFunctionNTest {
    protected final Double[] testPos = new Double[]{(double) 0, (double) 0, (double) 0};

    @Test
    public void testAddImagesVFN() {
        FNImage.VFNGenerator cFunGen = new FNImage.AddImages();
        FNImage.VoxelFunctionN cFun = cFunGen.get();
        cFun.add(testPos, 1.0);
        cFun.add(testPos, 3.0);
        assertEquals(cFun.get(), 4.0, 0.1);
    }

    @Test
    public void testMultiplyImagesVFN() {
        FNImage.VFNGenerator cFunGen = new FNImage.MultiplyImages();
        FNImage.VoxelFunctionN cFun = cFunGen.get();
        cFun.add(testPos, 2.5);
        cFun.add(testPos, 2.0);
        assertEquals(cFun.get(), 5.0, 0.1);
    }

    @Test
    public void testAvgImagesVFN() {
        FNImage.VFNGenerator cFunGen = new FNImage.AvgImages();
        FNImage.VoxelFunctionN cFun = cFunGen.get();
        cFun.add(testPos, 1.0);
        cFun.add(testPos, 3.0);
        assertEquals(cFun.get(), 2.0, 0.1);
    }

    @Test
    public void testPhaseImage() {
        FNImage.VFNGenerator cFunGen = new FNImage.PhaseImage(new double[]{10, 20, 30});
        FNImage.VoxelFunctionN cFun = cFunGen.get();
        cFun.add(testPos, 1.0);
        cFun.add(testPos, 0);
        cFun.add(testPos, 0);
        assertEquals(cFun.get(), 10.0, 0.1);

        cFun = cFunGen.get();
        cFun.add(testPos, 0.0);
        cFun.add(testPos, 1.0);
        cFun.add(testPos, 0);
        assertEquals(cFun.get(), 20.0, 0.1);

        cFun = cFunGen.get();
        cFun.add(testPos, 0.0);
        cFun.add(testPos, 0.0);
        cFun.add(testPos, 1.0);
        assertEquals(cFun.get(), 30.0, 0.1);
    }

    /**
     * A trickier test where multiple values are positive (always goes with the last value)
     */
    @Test
    public void testPhaseImageTricky() {
        FNImage.VFNGenerator cFunGen = new FNImage.PhaseImage(new double[]{10, 20, 30});
        FNImage.VoxelFunctionN cFun = cFunGen.get();
        cFun.add(testPos, 1.0);
        cFun.add(testPos, 1.0);
        cFun.add(testPos, 0);
        assertEquals(cFun.get(), 20.0, 0.1);

        cFun = cFunGen.get();
        cFun.add(testPos, 0.0);
        cFun.add(testPos, 1.0);
        cFun.add(testPos, 1.0);
        assertEquals(cFun.get(), 30.0, 0.1);

        cFun = cFunGen.get();
        cFun.add(testPos, 1.0);
        cFun.add(testPos, 0.0);
        cFun.add(testPos, 1.0);
        assertEquals(cFun.get(), 30.0, 0.1);
    }


}
