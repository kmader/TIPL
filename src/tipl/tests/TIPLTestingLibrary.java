/**
 *
 */
package tipl.tests;

import tipl.formats.TImgRO;
import tipl.tools.BaseTIPLPluginIn;
import tipl.util.D3int;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author mader
 */
public abstract class TIPLTestingLibrary {

    public static final boolean verbose = false;
    public final TImgRO emptyImg = TestPosFunctions.wrapIt(0,
            new TestPosFunctions.SinglePointFunction(0, 0, 0));

    /**
     * Wrap a collection into a collection of arrays which is useful for parameterization in junit testing
     *
     * @param inCollection input collection
     * @return wrapped collection
     */
    public static <T> Collection<T[]> wrapCollection(Collection<T> inCollection) {
        final List<T[]> out = new ArrayList<T[]>();
        for (T curObj : inCollection) {
            T[] arr = (T[]) new Object[1]; // this is just weird but new T[] {curObj} doesnt work
            arr[0] = curObj;
            out.add(arr);
        }
        return out;
    }

    public static void checkDimensions(final TImgRO img, final D3int dim, final D3int pos) {
        checkDim(img, dim);
        checkPos(img, pos);
    }

    public static void checkDim(final TImgRO img, final D3int dim) {
        assertEquals(img.getDim().x, dim.x);
        assertEquals(img.getDim().y, dim.y);
        assertEquals(img.getDim().z, dim.z);
    }

    public static void checkPos(final TImgRO img, final D3int pos) {
        assertEquals(img.getPos().x, pos.x);
        assertEquals(img.getPos().y, pos.y);
        assertEquals(img.getPos().z, pos.z);
    }

    /**
     * Check to see if a point in an image matches what it should be.
     * The position is taken in refernece to the pos of the image so that will be subtracted from the point
     *
     * @param img  input image to check
     * @param posX position in the image in x
     * @param posY
     * @param posZ
     * @param gval value the point should be
     * @param tol  the tolerance
     * @return
     */
    public static boolean doPointsMatch(final TImgRO img, int posX, int posY, int posZ, float gval, float tol) {
        final int rposX = posX - img.getPos().x;
        final int rposY = posY - img.getPos().y;
        final int rposZ = posZ - img.getPos().z;
        float[] curslice = (float[]) img.getPolyImage(rposZ, 3);
        assertEquals(gval, curslice[rposY * img.getDim().x + rposX], tol);
        return true;
    }

    public static boolean doSlicesMatch(final boolean[] slice1,
                                        final boolean[] slice2) {
        assertEquals(slice1.length, slice2.length);
        for (int i = 0; i < slice1.length; i++) {
            if (verbose) System.out.println(i + ", " + slice1[i] + " : " + slice2[i]);
            assertEquals(slice1[i], slice2[i]);
        }
        return true;
    }

    public static boolean doSlicesMatch(final int[] slice1,
                                        final int[] slice2) {
        assertEquals(slice1.length, slice2.length);
        for (int i = 0; i < slice1.length; i++) {
            if (verbose) System.out.println(i + ", " + slice1[i] + " : " + slice2[i]);
            assertEquals(slice1[i], slice2[i]);
        }
        return true;
    }

    public static void doSlicesMatchB(final TImgRO imgA,
                                      final int sliceA, final TImgRO imgB, final int sliceB) {
        final boolean[] aSlice = (boolean[]) imgA.getPolyImage(sliceA, 10);
        final boolean[] bSlice = (boolean[]) imgB.getPolyImage(sliceB, 10);
        doSlicesMatch(aSlice, bSlice);
    }

    public static void doSlicesMatchI(final TImgRO imgA,
                                      final int sliceA, final TImgRO imgB, final int sliceB) {
        final int[] aSlice = (int[]) imgA.getPolyImage(sliceA, 2);
        final int[] bSlice = (int[]) imgB.getPolyImage(sliceB, 2);
        doSlicesMatch(aSlice, bSlice);
    }

    public static void doImagesMatch(final TImgRO imgA, final TImgRO imgB) {
        for (int i = 0; i < imgA.getDim().z; i++) doSlicesMatchI(imgA, i, imgB, i);
    }

    public static void doImagesMatchB(final TImgRO imgA, final TImgRO imgB) {
        for (int i = 0; i < imgA.getDim().z; i++) doSlicesMatchB(imgA, i, imgB, i);
    }

    /**
     * count voxels in an entire image
     *
     * @param img image
     * @return total number of true voxels
     */
    public static long countVoxelsImage(final TImgRO img) {
        long totalCount = 0;
        for (int i = 0; i < img.getDim().z; i++)
            totalCount += TIPLTestingLibrary.countVoxelsSlice(img, i);
        return totalCount;
    }

    /**
     * count the number of voxels in a slice
     *
     * @param img    the image to use
     * @param sliceZ the slice number to look at
     * @return the number of voxels
     */
    public static long countVoxelsSlice(final TImgRO img, final int sliceZ) {
        final boolean[] cSlice = (boolean[]) img.getPolyImage(sliceZ, 10);
        long i = 0;
        for (final boolean cVal : cSlice)
            if (cVal)
                i++;
        return i;
    }

    /**
     * A function to test that divideThreadWork (a function to be overridden in BaseTIPLPluginIn) is dividing work properly
     *
     * @param pluginToTest
     * @param maxSlice
     */
    public static void testDivideWork(final BaseTIPLPluginIn pluginToTest, final int minSlice, final int maxSlice, final boolean checkOverlap) {
        System.out.println("Testing that work is divided into non-overlapping chunks between " + minSlice + " and " + maxSlice + " for:" + pluginToTest);

        int lastSlice = minSlice - 1;
        for (int i = 0; i < pluginToTest.neededCores(); i++) {
            Object inWork = pluginToTest.divideThreadWork(i);
            if (inWork != null) { // if it is null there is no work to do
                int[] cWork = (int[]) inWork;
                System.out.println(pluginToTest + ": Core:" + i + " : (" + lastSlice + "-" + maxSlice + ") => W<" + cWork[0] + ", " + cWork[1] + ">");
                if (i == 0) assertEquals(cWork[0], minSlice);

                if (checkOverlap) {
                    System.out.println("Checking Overlap");
                    assertTrue(cWork[0] > lastSlice);
                    assertTrue(cWork[1] > lastSlice);
                } else {
                    assertTrue(cWork[0] > minSlice);
                    assertTrue(cWork[1] > minSlice);
                }
                assertTrue(cWork[1] >= cWork[0]);
                System.out.println("Checking Bounds");
                assertTrue(cWork[0] <= maxSlice);
                assertTrue(cWork[1] <= maxSlice);
                lastSlice = cWork[1];
            } else {
                break;
            }

        }
        // make sure the entire dataset is spanned
        assertEquals(lastSlice, maxSlice);

    }

}
