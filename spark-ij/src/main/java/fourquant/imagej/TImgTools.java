package fourquant.imagej;

/**
 *
 */

import java.util.Date;

import java.io.IOException;
import java.util.Date;

/**
 * Library of static functions used for TImg (since TImg is just an interface, stolen from my
 * TIPL library)
 *
 * @author maderk
 */
public class TImgTools {
    /**
     * the values define the types of various images and should be used instead of hardcoded values
     * Type refers to binary or stored values
     */

    public static final int IMAGETYPE_BOOL = 10;
    public static final int IMAGETYPE_CHAR = 0;
    public static final int IMAGETYPE_SHORT = 1;
    public static final int IMAGETYPE_INT = 2;
    public static final int IMAGETYPE_FLOAT = 3;
    public static final int IMAGETYPE_DOUBLE = 4;
    public static final int IMAGETYPE_INTARRAY = 5;
    public static final int IMAGETYPE_DBLARRAY = 6;
    public static final int IMAGETYPE_GLOB = 7;
    public static final int IMAGETYPE_LONG = 8;
    public static final int IMAGETYPE_RGB = 9;
    public static final int IMAGETYPE_UNKNOWN = -1;
    /**
     * A list of all supported image types
     */
    public static final int[] IMAGETYPES = new int[] {
            IMAGETYPE_BOOL, IMAGETYPE_CHAR, IMAGETYPE_SHORT, IMAGETYPE_INT, IMAGETYPE_DOUBLE, IMAGETYPE_LONG,
            IMAGETYPE_RGB
    };
    public static final String IMAGETYPE_HELP = "(boolean image/1bit=" + IMAGETYPE_BOOL + ", character image/8bit=" + IMAGETYPE_CHAR + ", short image/16bit=" + IMAGETYPE_SHORT + ", integer image/32bit=" + IMAGETYPE_INT + ", float image/32bit=" + IMAGETYPE_FLOAT + ", double image/64bit="+IMAGETYPE_DOUBLE+", long image/64bit="+IMAGETYPE_LONG+", RGB image/24bit-byte[3]="+IMAGETYPE_RGB+")";

    /**
     * Image class refers to the information which is being stored in the image (distinct values from imagetype)
     */
    public static final int IMAGECLASS_BINARY = 100;
    public static final int IMAGECLASS_LABEL = 101;
    public static final int IMAGECLASS_VALUE = 102;
    public static final int IMAGECLASS_OTHER = 103;

    /**
     * Convert a type to a class of image (makes processing easier)
     * @param type the IMAGETYPE of the data coming in
     * @return the ImageClass of this type
     */
    public static int imageTypeToClass(int type) {
        switch(type) {
            case IMAGETYPE_BOOL:
                return IMAGECLASS_BINARY;
            case IMAGETYPE_CHAR:
            case IMAGETYPE_SHORT:
            case IMAGETYPE_INT:
            case IMAGETYPE_LONG:
                return IMAGECLASS_LABEL;
            case IMAGETYPE_FLOAT:
            case IMAGETYPE_DOUBLE:
            case IMAGETYPE_RGB:
                return IMAGECLASS_VALUE;

            default:
                return IMAGECLASS_OTHER;


        }
    }
    /**
     * Check to see if the image is cached or otherwise fast access (used for
     * caching methods to avoid double caching), 0 is encoded disk-based, 1 is
     * memory map, 2 is in memory + computation, 3 is memory based, in general
     * each computational layer (FImage) subtracts 1 from the isFast
     */
    public static int SPEED_DISK = 0;
    public static int SPEED_DISK_MMAP = 1;
    public static int SPEED_DISK_AND_MEM = 2;
    public static int SPEED_MEMORY_CALCULATE = 3;
    public static int SPEED_MEMORY = 4;
    public static boolean useSparkStorage = false;





    /**
     * Standard array conversion with correct default parameters for most datasets
     * @param inArray
     * @param inType
     * @param outType
     * @return
     */
    public static Object convertArrayType(final Object inArray,
                                          final int inType, final int outType) {
        return convertArrayType(inArray,inType,outType,false,1.0f);
    }
    /**
     * Generic function for converting array types (with maxvalue as 127)
     *
     * @param inArray          the input array as an object
     * @param inType           the type for the input
     * @param outType          the desired type for the output
     * @param isSigned         whether or not the value is signed
     * @param shortScaleFactor the factor to scale shorts/integers/chars by when converting
     *                         to a float and vice versa
     * @return slice as an object (must be casted)
     * @throws java.io.IOException
     */
    public static Object convertArrayType(final Object inArray,
                                          final int inType, final int outType, final boolean isSigned,
                                          final float shortScaleFactor) {
        return convertArrayType(inArray,
                inType, outType, isSigned,
                shortScaleFactor, 127);
    }

    /**
     * Generic function for converting array types
     *
     * @param inArray          the input array as an object
     * @param inType           the type for the input
     * @param outType          the desired type for the output
     * @param isSigned         whether or not the value is signed
     * @param shortScaleFactor the factor to scale shorts/integers/chars by when converting
     *                         to a float and vice versa
     * @param maxVal
     * @return slice as an object (must be casted)
     * @throws java.io.IOException
     */
    public static Object convertArrayType(final Object inArray,
                                          final int inType, final int outType, final boolean isSigned,
                                          final float shortScaleFactor, final int maxVal) {

        final int autoInType = identifySliceType(inArray);
        if(autoInType>0) assert (inType==autoInType); // make sure it is what it says it is
        // save time if it is the same
        if(inType==outType) return inArray;
        assert isValidType(inType);
        assert isValidType(outType);

        switch (inType) {
            case IMAGETYPE_CHAR: // byte
                return convertCharArray((char[]) inArray, outType, isSigned,
                        shortScaleFactor, maxVal);
            case IMAGETYPE_RGB: // rgb array
                return convertRGBArray((byte[][]) inArray, outType, isSigned,
                        shortScaleFactor);
            case IMAGETYPE_SHORT: // short
                return convertShortArray((short[]) inArray, outType, isSigned,
                        shortScaleFactor, maxVal);
            case IMAGETYPE_INT: // int
                return convertIntArray((int[]) inArray, outType, isSigned,
                        shortScaleFactor, 65536);
            case IMAGETYPE_LONG: // int
                return convertLongArray((long[]) inArray, outType, isSigned,
                        shortScaleFactor, 65536);
            case IMAGETYPE_FLOAT: // float
                return convertFloatArray((float[]) inArray, outType, isSigned,
                        shortScaleFactor);
            case IMAGETYPE_DOUBLE: // float
                return convertDoubleArray((double[]) inArray, outType, isSigned,
                        shortScaleFactor);
            case IMAGETYPE_BOOL: // boolean
                return convertBooleanArray((boolean[]) inArray, outType);

        }
        return inArray;
    }

    @Deprecated
    private static Object convertBooleanArray(final boolean[] gf,
                                              final int asType) {
        assert (asType >= 0 && asType <= 3) || asType == 10;
        final int sliceSize = gf.length;
        switch (asType) {
            case IMAGETYPE_CHAR: // Char
                final char[] gb = new char[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    if (gf[i])
                        gb[i] = 127;
                return gb;
            case IMAGETYPE_RGB: // RGB
                final byte[][] grgb = new byte[sliceSize][3];
                for (int i = 0; i < sliceSize; i++)
                    if (gf[i])
                    {
                        grgb[i][0] = 127;
                        grgb[i][1] = 127;
                        grgb[i][2] = 127;
                    }
                return grgb;
            case IMAGETYPE_SHORT: // Short
                // Read short data type in
                final short[] gs = new short[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    if (gf[i])
                        gs[i] = 127;
                return gs;
            case IMAGETYPE_INT: // Spec / Int
                // Read integer data type in
                final int[] gi = new int[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    if (gf[i])
                        gi[i] = 127;
                return gi;

            case IMAGETYPE_LONG: // Spec / Int
                // Read integer data type in
                final long[] gl = new long[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    if (gf[i])
                        gl[i] = 127;
                return gl;

            case IMAGETYPE_FLOAT: // Float - Long
                final float[] gout = new float[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    if (gf[i])
                        gout[i] = 1.0f;
                return gout;
            case IMAGETYPE_DOUBLE: // Float - Long
                final double[] goutd = new double[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    if (gf[i])
                        goutd[i] = 1.0f;
                return goutd;
            case IMAGETYPE_BOOL: // Mask
                return gf;
            default:
                throw new IllegalArgumentException("Unknown data type!!!" + asType
                        + ", " + gf);
        }

    }

    @Deprecated
    private static Object convertCharArray(final char[] gs, final int asType,
                                           final boolean isSigned, final float shortScaleFactor,
                                           final int maxVal) {
        final int sliceSize = gs.length;
        switch (asType) {
            case IMAGETYPE_CHAR: // Char
                return gs;
            case IMAGETYPE_RGB: // RGB
                final byte[][] grgb = new byte[sliceSize][3];
                for (int i = 0; i < sliceSize; i++)
                {
                    grgb[i][0] = (byte) gs[i];
                    grgb[i][1] = grgb[i][0];
                    grgb[i][2] = grgb[i][0];
                }
                return grgb;
            case IMAGETYPE_SHORT: // Short
                // Read short data type in
                final short[] gshort = new short[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gshort[i] = (short) gs[i];
                return gshort;
            case IMAGETYPE_INT: // Spec / Int
                // Read integer data type in
                final int[] gi = new int[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gi[i] = gs[i];
                return gi;

            case IMAGETYPE_LONG: // Spec / Int
                // Read integer data type in
                final long[] gl = new long[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gl[i] = gs[i];
                return gl;

            case IMAGETYPE_FLOAT: // Float - Long
                final float[] gf = new float[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gf[i] = (gs[i] - (isSigned ? maxVal / 2.0f : 0.0f))
                            * shortScaleFactor;
                return gf;

            case IMAGETYPE_DOUBLE: // Float - Long
                final double[] gd = new double[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gd[i] = (gs[i] - (isSigned ? maxVal / 2.0f : 0.0f))
                            * shortScaleFactor;
                return gd;

            case IMAGETYPE_BOOL: // Mask
                final boolean[] gbool = new boolean[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gbool[i] = gs[i] > 0;
                return gbool;
            default:
                throw new IllegalArgumentException("Unknown data type!!!" + asType
                        + " from char");

        }
    }

    /**
     * The rules for converting RGB images to standard images
     */
    public static enum RGBConversion {
        MEAN, SUM, MIN, MAX, INT, RED, GREEN, BLUE
    }

    public static RGBConversion rgbConversionMethod = RGBConversion.MEAN;


    public static Object convertRGBArray(final byte[][] grgb, final int asType,
                                         final boolean isSigned,
                                         final float shortScaleFactor) {
        final RGBConversion curMethod = rgbConversionMethod;

        // convert it to a double array and spit it back
        final int sliceSize = grgb.length;

        final double[] gdouble = new double[sliceSize];
        for (int i = 0; i < sliceSize; i++)
        {
            double outValue = 0;
            // since the data is actually stored as an unsigned byte
            double[] drgb= new double[3];
            for(int k=0;k<3;k++) {
                drgb[k]=grgb[i][k];
                if (drgb[k]<0) drgb[k]+=127;
            }

            switch(curMethod) {
                case MEAN:
                    outValue = (drgb[0]+drgb[1]+drgb[2])/3;
                    break;
                case SUM:
                    outValue = (drgb[0]+drgb[1]+drgb[2]);
                    break;
                case MIN:
                    outValue = Math.min(Math.min(drgb[0],drgb[1]),drgb[2]);
                    break;
                case MAX:
                    outValue = Math.max(Math.max(drgb[0],drgb[1]),drgb[2]);
                    break;
                case INT:
                    outValue =  ((int)grgb[i][0] << 24) + ((int)grgb[i][1] << 8) + (int) grgb[i][2];
                    break;
                case RED:
                    outValue = drgb[0];
                    break;
                case GREEN:
                    outValue = drgb[1];
                    break;
                case BLUE:
                    outValue = drgb[2];
                    break;
            }
            gdouble[i] = outValue+(isSigned ? 127 : 0);
        }
        return convertDoubleArray(gdouble, asType,isSigned,shortScaleFactor);
    }


    public static Object convertFloatArray(final float[] gf, final int asType,
                                           final boolean isSigned, final float shortScaleFactor) {
        assert isValidType(asType);
        final int sliceSize = gf.length;
        switch (asType) {
            case IMAGETYPE_CHAR: // Char
                final char[] gb = new char[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gb[i] = (char) ((gf[i] / shortScaleFactor) + (isSigned ? 127
                            : 0));
                return gb;
            case IMAGETYPE_RGB: // RGB
                final byte[][] grgb = new byte[sliceSize][3];
                for (int i = 0; i < sliceSize; i++)
                {
                    grgb[i][0] = (byte) ((gf[i] / shortScaleFactor) + (isSigned ? 127
                            : 0));
                    grgb[i][1] = grgb[i][0];
                    grgb[i][2] = grgb[i][0];
                }
                return grgb;
            case IMAGETYPE_SHORT: // Short
                // Read short data type in
                final short[] gs = new short[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gs[i] = (short) ((gf[i] / shortScaleFactor) + (isSigned ? 32768
                            : 0));
                return gs;
            case IMAGETYPE_INT: // Spec / Int
                // Read integer data type in
                final int[] gi = new int[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gi[i] = (int) ((gf[i] / shortScaleFactor) + (isSigned ? 32768
                            : 0));
                return gi;
            case IMAGETYPE_LONG: // Long
                // Read integer data type in
                final long[] gl = new long[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gl[i] = (long) ((gf[i] / shortScaleFactor) + (isSigned ? 32768
                            : 0));
                return gl;
            case IMAGETYPE_FLOAT: // Float - Long
                return gf;
            case IMAGETYPE_DOUBLE: // double
                final double[] gd = new double[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gd[i] = gf[i];
                return gd;
            case IMAGETYPE_BOOL: // Mask
                final boolean[] gbool = new boolean[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gbool[i] = gf[i] > 0;
                return gbool;
            default:
                throw new IllegalArgumentException("Unknown data type!!!" + asType
                        + ", " + gf);
        }

    }

    private static Object convertDoubleArray(final double[] gf, final int asType,
                                             final boolean isSigned, final float shortScaleFactor) {
        assert isValidType(asType);
        final int sliceSize = gf.length;
        switch (asType) {
            case IMAGETYPE_CHAR: // Char
                final char[] gb = new char[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gb[i] = (char) ((gf[i] / shortScaleFactor) + (isSigned ? 127
                            : 0));
                return gb;
            case IMAGETYPE_RGB: // RGB
                final byte[][] grgb = new byte[sliceSize][3];
                for (int i = 0; i < sliceSize; i++)
                {
                    grgb[i][0] = (byte) ((gf[i] / shortScaleFactor) + (isSigned ? 127
                            : 0));
                    grgb[i][1] = grgb[i][0];
                    grgb[i][2] = grgb[i][0];
                }
                return grgb;
            case IMAGETYPE_SHORT: // Short
                // Read short data type in
                final short[] gs = new short[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gs[i] = (short) ((gf[i] / shortScaleFactor) + (isSigned ? 32768
                            : 0));
                return gs;
            case IMAGETYPE_INT: // Spec / Int
                // Read integer data type in
                final int[] gi = new int[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gi[i] = (int) ((gf[i] / shortScaleFactor) + (isSigned ? 32768
                            : 0));
                return gi;
            case IMAGETYPE_LONG: // Long
                // Read integer data type in
                final long[] gl = new long[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gl[i] = (long) ((gf[i] / shortScaleFactor) + (isSigned ? 32768
                            : 0));
                return gl;
            case IMAGETYPE_DOUBLE: // double
                return gf;
            case IMAGETYPE_FLOAT: // Float - Long
                final float[] gd = new float[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gd[i] = (float) gf[i];
                return gd;
            case IMAGETYPE_BOOL: // Mask
                final boolean[] gbool = new boolean[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gbool[i] = gf[i] > 0;
                return gbool;
            default:
                throw new IllegalArgumentException("Unknown data type!!!" + asType
                        + ", " + gf);
        }

    }

    public static Object convertIntArray(final int[] gi, final int asType,
                                         final boolean isSigned, final float ShortScaleFactor,
                                         final int maxVal) {
        final int sliceSize = gi.length;
        switch (asType) {
            case IMAGETYPE_CHAR: // Char
                final char[] gb = new char[sliceSize];
                for (int i = 0; i < sliceSize; i++) {
                    gb[i] = (char) gi[i];
                }

                return gb;
            case IMAGETYPE_RGB: // RGB
                final byte[][] grgb = new byte[sliceSize][3];
                for (int i = 0; i < sliceSize; i++)
                {
                    grgb[i][0] = (byte) gi[i];
                    grgb[i][1] = grgb[i][0];
                    grgb[i][2] = grgb[i][0];
                }
                return grgb;

            case IMAGETYPE_SHORT: // Short
                // Read short data type in
                final short[] gs = new short[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gs[i] = (short) gi[i];
                return gs;

            case IMAGETYPE_INT: // Spec / Int
                // Read integer data type in
                return gi;

            case IMAGETYPE_LONG:
                final long[] gl = new long[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gl[i] = gi[i];
                return gl;

            case IMAGETYPE_FLOAT: // Float - Long
                final float[] gf = new float[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gf[i] = (gi[i] - (isSigned ? maxVal / 2.0f : 0.0f))
                            * ShortScaleFactor;
                return gf;

            case IMAGETYPE_DOUBLE: // Float - Long
                final double[] gd = new double[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gd[i] = (gi[i] - (isSigned ? maxVal / 2.0f : 0.0f))
                            * ShortScaleFactor;
                return gd;
            case IMAGETYPE_BOOL: // Mask
                final boolean[] gbool = new boolean[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gbool[i] = gi[i] > 0;

                return gbool;
            default:
                throw new IllegalArgumentException("Unknown data type!!!" + asType
                        + ", " + gi);

        }
    }

    @Deprecated
    private static Object convertLongArray(final long[] gi, final int asType,
                                           final boolean isSigned, final float ShortScaleFactor,
                                           final int maxVal) {
        final int sliceSize = gi.length;
        switch (asType) {
            case IMAGETYPE_CHAR: // Char
                final char[] gb = new char[sliceSize];
                for (int i = 0; i < sliceSize; i++) {
                    gb[i] = (char) gi[i];
                }

                return gb;
            case IMAGETYPE_RGB: // RGB
                final byte[][] grgb = new byte[sliceSize][3];
                for (int i = 0; i < sliceSize; i++)
                {
                    grgb[i][0] = (byte) gi[i];
                    grgb[i][1] = grgb[i][0];
                    grgb[i][2] = grgb[i][0];
                }
                return grgb;
            case IMAGETYPE_SHORT: // Short
                final short[] gs = new short[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gs[i] = (short) gi[i];
                return gs;

            case IMAGETYPE_INT: // Spec / Int
                final int[] gint = new int[sliceSize];
                for (int i = 0; i < sliceSize; i++) {
                    if (gi[i] > Integer.MAX_VALUE) {
                        gint[i] = Integer.MAX_VALUE;
                        System.out.println("Unsafe conversion from long to integer, saturation has occurred");
                    } else {
                        gint[i] = (int) gi[i];
                    }
                }

                return gint;

            case IMAGETYPE_LONG: // Spec / Int
                // Read integer data type in

                return gi;

            case IMAGETYPE_FLOAT: // Float - Long
                final float[] gf = new float[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gf[i] = (gi[i] - (isSigned ? maxVal / 2.0f : 0.0f))
                            * ShortScaleFactor;
                return gf;

            case IMAGETYPE_DOUBLE: // Float - Long
                final double[] gd = new double[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gd[i] = (gi[i] - (isSigned ? maxVal / 2.0f : 0.0f))
                            * ShortScaleFactor;
                return gd;
            case IMAGETYPE_BOOL: // Mask
                final boolean[] gbool = new boolean[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gbool[i] = gi[i] > 0;

                return gbool;
            default:
                throw new IllegalArgumentException("Unknown data type!!!" + asType
                        + ", " + gi);

        }
    }

    @Deprecated
    private static Object convertShortArray(final short[] gs, final int asType,
                                            final boolean isSigned, final float ShortScaleFactor,
                                            final int maxVal) {
        final int sliceSize = gs.length;
        switch (asType) {
            case IMAGETYPE_CHAR: // Char
                final char[] gb = new char[sliceSize];
                for (int i = 0; i < sliceSize; i++) {
                    gb[i] = (char) gs[i];
                }

                return gb;
            case IMAGETYPE_RGB: // RGB
                final byte[][] grgb = new byte[sliceSize][3];
                for (int i = 0; i < sliceSize; i++)
                {
                    grgb[i][0] = (byte) gs[i];
                    grgb[i][1] = grgb[i][0];
                    grgb[i][2] = grgb[i][0];
                }
                return grgb;
            case IMAGETYPE_SHORT: // Short
                // Read short data type in

                return gs;

            case IMAGETYPE_INT: // Spec / Int
                // Read integer data type in
                final int[] gi = new int[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gi[i] = gs[i];
                return gi;

            case IMAGETYPE_LONG: // Spec / Int
                // Read integer data type in
                final long[] gl = new long[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gl[i] = gs[i];
                return gl;

            case IMAGETYPE_FLOAT: // Float - Long
                final float[] gf = new float[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gf[i] = (gs[i] - (isSigned ? maxVal / 2.0f : 0.0f))
                            * ShortScaleFactor;
                return gf;
            case IMAGETYPE_DOUBLE: // Float - Long
                final double[] gd = new double[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gd[i] = (gs[i] - (isSigned ? maxVal / 2.0f : 0.0f))
                            * ShortScaleFactor;
                return gd;

            case IMAGETYPE_BOOL: // Mask
                final boolean[] gbool = new boolean[sliceSize];
                for (int i = 0; i < sliceSize; i++)
                    gbool[i] = gs[i] > 0;

                return gbool;
            default:
                throw new IllegalArgumentException("Unknown data type!!!" + asType
                        + ", " + gs);

        }
    }

    /**
     * Used for dividing stacks of images into a smaller range
     *
     * @param startValue first slice
     * @param endValue   last slice
     * @param blockCount number of blocks to subdivide into
     * @param curBlock   the current block (between 0 and blockCount)
     * @return an integer array with the starting and ending slice numbers
     */
    public static int[] getRange(int startValue, int endValue, int blockCount, int curBlock) {
        assert (curBlock < blockCount);
        assert (curBlock >= 0);
        assert (blockCount >= 0);
        assert (endValue > startValue);
        final int blockSize = (int) ((endValue - startValue) / (1.0f * blockCount));
        final int blockStart = blockSize * curBlock + 1;
        int blockEnd = blockSize * (curBlock + 1);
        if (curBlock == (blockCount - 1)) blockEnd = endValue;
        return new int[]{blockStart, blockEnd};
    }

    public static int[] getRange(int endValue, int blockCount, int curBlock) {
        return getRange(0, endValue, blockCount, curBlock);
    }



    /**
     * Calculate the type of object it is from the slice information
     * (getPolyImage, etc)
     *
     * @param iData a slice from the image (usually an array)
     * @return the type of the object
     */
    public static int identifySliceType(final Object iData) {
        if (iData instanceof boolean[])
            return TImgTools.IMAGETYPE_BOOL;
        if (iData instanceof char[])
            return TImgTools.IMAGETYPE_CHAR;
        if (iData instanceof short[])
            return TImgTools.IMAGETYPE_SHORT;
        if (iData instanceof int[])
            return TImgTools.IMAGETYPE_INT;
        if (iData instanceof float[])
            return TImgTools.IMAGETYPE_FLOAT;
        if (iData instanceof double[])
            return TImgTools.IMAGETYPE_DOUBLE;
        if (iData instanceof long[])
            return TImgTools.IMAGETYPE_LONG;
        if (iData instanceof byte[][])
            return TImgTools.IMAGETYPE_RGB;
        if (iData instanceof scala.Int[])
            throw new IllegalArgumentException("Scala types are not acceptable image types!");
        throw new IllegalArgumentException("Type of object:" + iData
                + " cannot be determined!! Proceed with extreme caution");
    }
    /**
     * convert any known array type to an array of doubles (good enough for labels or values)
     * @param inSlice
     * @return slice as an array of double
     */
    public static double[] convertArrayDouble(Object inSlice) {
        final int type = identifySliceType(inSlice);
        return (double[]) convertArrayType(inSlice,type,IMAGETYPE_DOUBLE);
    }


    /**
     * Calculate the type of object it is from the type name
     *
     * @param inType the type of the object
     * @return the normal name for the slice type
     */
    public static String getImageTypeName(final int inType) {
        assert (isValidType(inType));
        switch (inType) {
            case IMAGETYPE_BOOL:
                return "1bit";
            case IMAGETYPE_CHAR:
                return "8bit";
            case IMAGETYPE_SHORT:
                return "16bit";
            case IMAGETYPE_INT:
                return "32bit-integer";
            case IMAGETYPE_LONG:
                return "64bit-long";
            case IMAGETYPE_FLOAT:
                return "32bit-float";
            case IMAGETYPE_DOUBLE:
                return "64bit-double";
            case IMAGETYPE_RGB:
                return "24bit-rgb image";
            default:
                return throwImageTypeError(inType);
        }
    }
    /**
     * A standard error for typing problems
     * @param inType
     */
    public static String throwImageTypeError(int inType) {
        throw new IllegalArgumentException("Type of object:" + inType+ " is not known, program cannot continue");
    }

    /**
     * get the range of values for a given image type
     *
     * @param inType
     * @return
     */
    public static double[] identifyTypeRange(final int inType) {
        assert (isValidType(inType));
        switch (inType) {
            case IMAGETYPE_BOOL:
                return new double[]{0, 1};
            case IMAGETYPE_CHAR:
                return new double[]{0, 127};
            case IMAGETYPE_SHORT:
                return new double[]{Short.MIN_VALUE, Short.MAX_VALUE};
            case IMAGETYPE_INT:
                return new double[]{Integer.MIN_VALUE, Integer.MAX_VALUE};
            case IMAGETYPE_LONG:
                return new double[]{Long.MIN_VALUE, Long.MAX_VALUE};
            case IMAGETYPE_FLOAT:
                return new double[]{Float.MIN_VALUE, Float.MAX_VALUE};
            case IMAGETYPE_DOUBLE:
                return new double[]{Double.MIN_VALUE, Double.MAX_VALUE};
            default:
                throw new IllegalArgumentException("Range of object:" + inType
                        + " cannot be determined!! Proceed with extreme caution");
        }
    }



    /**
     * allocate large arrays (important for old model)
     *
     * @param inType
     * @param arrSize
     * @return
     */
    protected static Object bigAlloc(int inType, int arrSize) {
        assert (isValidType(inType));
        try {
            switch (inType) {
                case IMAGETYPE_BOOL:
                    return new boolean[arrSize];
                case IMAGETYPE_CHAR:
                    return new char[arrSize];
                case IMAGETYPE_SHORT:
                    return new short[arrSize];
                case IMAGETYPE_INT:
                    return new int[arrSize];
                case IMAGETYPE_FLOAT:
                    return new float[arrSize];
                case IMAGETYPE_DOUBLE:
                    return new double[arrSize];
                case IMAGETYPE_LONG:
                    return new long[arrSize];
                case IMAGETYPE_RGB:
                    return new byte[arrSize][3];
                default:
                    throw new IllegalArgumentException("Allocation of object:" + inType
                            + " cannot be made!! Proceed with extreme caution");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Allocation Failed:Type:" + inType + " [" + arrSize + "]" + "\n" + e.getMessage());
        }
    }

    /**
     * Check to see if the type chosen is valid
     *
     * @param asType the type to check
     * @return true if valid otherwise false
     */
    public static boolean isValidType(final int asType) {
        if (asType == IMAGETYPE_BOOL) return true;
        if (asType == IMAGETYPE_CHAR) return true;
        if (asType == IMAGETYPE_INT) return true;
        if (asType == IMAGETYPE_FLOAT) return true;
        if (asType == IMAGETYPE_SHORT) return true;
        System.err.println("Double and long type images are not yet fully supported, proceed with caution:" + asType);
        if (asType == IMAGETYPE_DOUBLE) return true;
        if (asType == IMAGETYPE_LONG) return true;
        return false;
    }

    /**
     * The size in bytes of each datatype
     *
     * @param inType
     * @return size in bytes
     */
    public static long typeSize(final int inType) {
        assert isValidType(inType);
        switch (inType) {
            case IMAGETYPE_CHAR:
                return 1;
            case IMAGETYPE_SHORT:
                return 2;
            case IMAGETYPE_INT:
                return 4;
            case IMAGETYPE_RGB:
                return 3;
            case IMAGETYPE_FLOAT:
                return 4;
            case IMAGETYPE_DOUBLE:
                return 8;
            case IMAGETYPE_LONG:
                return 8;
            case IMAGETYPE_BOOL:
                return 1;
        }
        return -1;
    }


}

