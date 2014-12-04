/**
 *
 */
package tipl.util;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.TImgRO.FullReadable;


import java.io.IOException;
import java.util.Date;

/**
 * Library of static functions used for TImg (since TImg is just an interface)
 *
 * @author maderk
 *         <p/>
 *         <pre> v3 28May13 Added generic slice converting function
 *         <p/>
 *                 <pre> v2 04Feb13 Added elSize to the mirrorImage function
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
    public static final int IMAGETYPE_COMPLEX = 5;
    public static final int IMAGETYPE_SPECTRAL = 6;
    public static final int IMAGETYPE_GLOB = 7;
    public static final int IMAGETYPE_LONG = 8;
    public static final int IMAGETYPE_RGB = 9;
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
                if (TIPLGlobal.getDebug()) System.out.println("Warning: ImageType ("+type+") is unknown and is being labeled as other");
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
    protected static ITIPLStorage storageBackend = null;
    public static boolean useSparkStorage = false;

    /**
     * get the current backend for storage / memory management, if none exists create a new one
     *
     * @return
     */
    public static ITIPLStorage getStorage() {
        if (storageBackend == null) {
            storageBackend = TIPLStorageManager.getFirstStorage
                    (useSparkStorage);
        }
        return storageBackend;
    }

    public static void setStorage(final ITIPLStorage inStorage) {
        storageBackend = inStorage;
    }

    @Deprecated
    public static TImgRO CacheImage(final TImgRO inImage) {
        return getStorage().CacheImage(inImage);
    }

    public static String appendProcLog(final String curLog, final String appText) {
        return curLog + "\n" + new Date() + "\t" + appText;
    }

    public static TImgRO[] fillListWithNull(TImgRO[] inImages, int keepLength) {
        TImgRO[] outImages = new TImgRO[keepLength];
        for (int i = 0; i < keepLength; i++) {
            TImgRO curElement = null;
            if (i < inImages.length) curElement = inImages[i];
            outImages[i] = curElement;
        }
        return outImages;
    }
    /**
     * A function to allow changing image types by wrapping objects instead of modifying the field
     * @param inImg
     * @param desiredType
     * @return
     */
    static public final TImg ChangeImageType(final TImgRO inImg, final int desiredType) {
        assert isValidType(desiredType);
        return new TImg.ATImg(inImg,desiredType) {
            final private TImgRO hiddenImg = inImg;
            @Override
            public Object getPolyImage(int sliceNumber, int asType) {
                return inImg.getPolyImage(sliceNumber, asType);
            }

            @Override
            public String getSampleName() {
                return inImg.getSampleName();
            }

        };
    }


    /**
     * The general function for comparing the dimensions of two TImg class
     * images
     */
    public static boolean CheckSizes2(final HasDimensions otherTImg, final HasDimensions otherVA) {

        boolean isMatch = true;
        isMatch = isMatch & (otherTImg.getDim().x == otherVA.getDim().x);
        isMatch = isMatch & (otherTImg.getDim().y == otherVA.getDim().y);
        isMatch = isMatch & (otherTImg.getDim().z == otherVA.getDim().z);
        isMatch = isMatch & (otherTImg.getPos().x == otherVA.getPos().x);
        isMatch = isMatch & (otherTImg.getPos().y == otherVA.getPos().y);
        isMatch = isMatch & (otherTImg.getPos().z == otherVA.getPos().z);
        isMatch = isMatch & (otherTImg.getOffset().x == otherVA.getOffset().x);
        isMatch = isMatch & (otherTImg.getOffset().y == otherVA.getOffset().y);
        isMatch = isMatch & (otherTImg.getOffset().z == otherVA.getOffset().z);
        return isMatch;
    }
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
     * @throws IOException
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
     * @throws IOException
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

    public static D3int getDXYZFromVec(final D3int vecDim, final int pixVal,
                                       final int slicen) {
        // int x,y,z;
        final D3int oPos = new D3int();
        oPos.x = pixVal % vecDim.getWidth();
        oPos.y = (pixVal - oPos.x) / vecDim.getWidth();
        oPos.z = slicen;
        return oPos;
    }

    public static int getJFromVec(final D3int vecPos, final D3int vecDim,
                                  final int x, final int y) {
        return getJFromVec(vecPos, vecDim, x, y, true);
    }

    public static int getJFromVec(final D3int vecPos, final D3int vecDim,
                                  final int x, final int y, final boolean relCoord) {
        int curX = x;
        int curY = y;
        if (relCoord) {
            curX -= vecPos.x;
            curY -= vecPos.y;
        }
        return (curY) * vecDim.getWidth() + curX;

    }

    public static D3float getRXYZFromVec(final D3int vecPos,
                                         final D3float vecSize, final D3int iPos, final boolean asMeasure) {
        final D3float oPos = new D3float();
        if (asMeasure) {
            oPos.x = ((float) iPos.x + (float) vecPos.x) * vecSize.x;
            oPos.y = ((float) iPos.y + (float) vecPos.y) * vecSize.y;
            oPos.z = ((float) iPos.z + (float) vecPos.z) * vecSize.z;
        } else {
            oPos.x = ((float) iPos.x + (float) vecPos.x);
            oPos.y = ((float) iPos.y + (float) vecPos.y);
            oPos.z = ((float) iPos.z + (float) vecPos.z);
        }
        return oPos;
    }

    public static D3float getRXYZFromVec(final D3int vecPos, final D3int iPos) {
        final D3float oPos = new D3float();
        return getRXYZFromVec(vecPos, oPos, iPos, false);
    }

    public static D3float getRXYZFromVec(final D3int vecPos,
                                         final D3int vecDim, final int pixVal, final int slicen) {
        final D3int iPos = getDXYZFromVec(vecDim, pixVal, slicen);
        return getRXYZFromVec(vecPos, iPos);
    }

    /**
     * Get a double array of the x,y,z position given a current slice index and
     * current slice
     */
    public static Double[] getXYZVecFromVec(final D3int vecPos,
                                            final D3int vecDim, final int cIndex, final int cSlice) {
        final D3float npos = getRXYZFromVec(vecPos, vecDim, cIndex, cSlice);
        final Double[] cPos = new Double[3];
        cPos[0] = npos.x;
        cPos[1] = npos.y;
        cPos[2] = npos.z;
        return cPos;
    }

    /**
     * Get a double array of the x,y,z position given a current slice index and
     * current slice
     */
    public static Double[] getXYZVecFromVec(final HasDimensions inImg,
                                            final int cIndex, final int cSlice) {
        return getXYZVecFromVec(inImg.getPos(), inImg.getDim(), cIndex, cSlice);
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
     * calculate how much memory is used when allocating large arrays
     *
     * @param inType
     * @param arrSize
     * @return
     */
    public static Object watchBigAlloc(int inType, int arrSize) {
        long usedBefore = TIPLGlobal.getUsedMB();
        Object out = bigAlloc(inType, arrSize);
        long usedAfter = TIPLGlobal.getUsedMB();
        long expectedSize = (long) (arrSize / (1024.0 * 1024.0) * typeSize(inType));
        if (TIPLGlobal.getDebugLevel() >= TIPLGlobal.DEBUG_GC)
            System.out.println("Alloc: " + getImageTypeName(inType) + ":[" + (arrSize / 1e6) + "M], used " + (usedAfter - usedBefore) + " (E:" + expectedSize + "), free:" + TIPLGlobal.getFreeMB());

        return out;
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
        if (TIPLGlobal.getDebug())
            System.err.println("Double and long type images are not yet fully supported, proceed with caution:" + asType);
        if (asType == IMAGETYPE_DOUBLE) return true;
        if (asType == IMAGETYPE_LONG) return true;
        return false;
    }

    /**
     * A method to implement the inheritance functionality to a standard TImgRO
     * currently uses VirtualAim, but this will be fixed soon
     *
     * @param inImg
     * @return an exportable version of inImg
     */
    public static TImgRO.CanExport makeTImgExportable(final TImgRO inImg) {
        return WrapTImgRO(inImg);
    }

    /**
     * A method to implement the full array reading functionality to a standard
     * TImgRO currently uses VirtualAim, but this will be fixed soon
     *
     * @param inImg
     * @return a full-readable version of inImg
     */
    @Deprecated
    public static FullReadable makeTImgFullReadable(final TImgRO inImg) {
        return getStorage().makeTImgFullReadable(inImg);
    }

    /**
     * Copy the size of one TImg to another *
     */
    public static void mirrorImage(final HasDimensions inData,
                                   final ChangesDimensions outData) {
        outData.setPos(inData.getPos());
        outData.setOffset(inData.getOffset());
        outData.setDim(inData.getDim());
        outData.setElSize(inData.getElSize());
        outData.appendProcLog(inData.getProcLog());
        outData.setShortScaleFactor(inData.getShortScaleFactor());
    }
    @Deprecated
    public static TImg ReadTImg(final String path) {
        return ReadTImg(TIPLStorageManager.openPath(path));
    }
    public static TImg ReadTImg(final TypedPath path) {
        TImg outImg = getStorage().readTImg(path);
        TIPLGlobal.getUsage().registerImage(path.getPath(), outImg.getDim().toString(), "read");
        return outImg;
    }

    /**
     * Read a single slice from the image and make sure the image is retained in the cache
     *
     * @param path        the path to the image
     * @param sliceNumber the number of the slice
     * @param imgType     the type of the image
     * @return the object containing the slice as whatever it should be
     */
    public static Object ReadTImgSlice(final TypedPath path, final int sliceNumber, final int imgType) {
        assert (isValidType(imgType));
        return ReadTImg(path, true, true).getPolyImage(sliceNumber, imgType);
    }

    /**
     * Read an image and save it to the global cache for later retrival (must
     * then be manually deleted)
     *
     * @param path
     * @param readFromCache check the cache to see if the image is already present
     * @param saveToCache   put the image into the cache after it has been read
     * @return loaded image
     */
    public static TImg ReadTImg(final TypedPath path, final boolean readFromCache,
                                final boolean saveToCache) {
        TImg outImg = getStorage().readTImg(path, readFromCache, saveToCache);
        TIPLGlobal.getUsage().registerImage(path.toString(), outImg.getDim().toString(), "read");
        return outImg;
    }

    /**
     * Read an image and save it to the global cache for later retrival (must
     * then be manually deleted)
     *
     * @param path
     * @param readFromCache check the cache to see if the image is already present
     * @param saveToCache   put the image into the cache after it has been read
     * @return loaded image
     */
    @Deprecated
    public static TImg ReadTImg(final String path, final boolean readFromCache,
                                final boolean saveToCache) {
        return ReadTImg(TIPLStorageManager.openPath(path), readFromCache, saveToCache);
    }

    public static boolean RemoveTImgFromCache(final String path) {
        return getStorage().RemoveTImgFromCache(path);
    }
    public static boolean RemoveTImgFromCache(final TypedPath path) {
        return getStorage().RemoveTImgFromCache(path.getPath());
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

    /**
     * For wrapping a TImgRO object so that it can be changed
     *
     * @param inImage
     * @return
     */
    public static TImg WrapTImgRO(final TImgRO inImage) {
        return getStorage().wrapTImgRO(inImage);
    }

    /**
     * Starts a new thread to save the current image without interrupting other
     * processings. The thread then closes when the saving operation is complete
     *
     * @param inImg    name of the file to save
     * @param filename path of the saved file
     */
    public static void WriteBackground(final TImgRO inImg,
                                       final TypedPath filename) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("BG Save Started for Image:" + inImg
                        + " to path:" + filename);
                TImgTools.WriteTImg(inImg, filename);
            }
        }).start();

    }

    /**
     * Method to write an image to disk and return whether or not it was
     * successful
     *
     * @param curImg
     * @param path
     * @return success
     */
    public static boolean WriteTImg(final TImgRO curImg, final TypedPath path) {
        return WriteTImg(curImg, path, false);
    }

    public static boolean WriteTImg(final TImgRO curImg, final TypedPath path,
                                    final boolean saveToCache) {
        TIPLGlobal.getUsage().registerImage(path.getPath(), curImg.getDim().toString(), curImg + "");
        return getStorage().writeTImg(curImg, path, saveToCache);
    }

    /**
     * Write a TImg with all of the appropriate parameters
     *
     * @param inImg
     * @param outpath
     * @param outType
     * @param scaleVal
     * @param IisSigned
     * @param toCache   should the output value be cached
     */
    @Deprecated
    public static void WriteTImg(final TImgRO inImg, final TypedPath outpath,
                                 final int outType, final float scaleVal, final boolean IisSigned, final boolean toCache) {
        TIPLGlobal.getUsage().registerImage(outpath.getPath(), inImg.getDim().toString(), inImg + ", " + outType);

        getStorage().writeTImg(inImg, outpath, outType, scaleVal, IisSigned, toCache);
    }


    /**
     * put just the relevant dimension reading code in a separate interface
     * since it is sometimes passed to objects to create create new images
     * where the dimensions but not the content is important
     *
     * @author mader
     */
    public static interface ChangesDimensions extends HasDimensions {
        /**
         * add a line to the procedure log *
         */
        public String appendProcLog(String inData);

        /**
         * The size of the image
         */
        public void setDim(D3int inData);

        /**
         * The element size (in mm) of a voxel
         */
        public void setElSize(D3float inData);

        /**
         * The size of the border around the image which does not contain valid
         * voxel data
         */
        @Deprecated
        public void setOffset(D3int inData);

        /**
         * The position of the bottom leftmost voxel in the image in real space,
         * only needed for ROIs
         */
        public void setPos(D3int inData);

        /**
         * A function to set the short scale factor used to convert shorts to
         * double and back
         *
         * @param ssf
         */
        @Deprecated
        public void setShortScaleFactor(float ssf);

    }
    /**
     * Make an editable dimensions object from a static one
     * @param inObj a static hasdimensions object
     * @return an editable changedimensions object
     */
    public static ChangesDimensions MakeEditable(final HasDimensions inObj) {
        return new ChangesDimensions() {
            D3int dim = new D3int(inObj.getDim());
            D3int pos = new D3int(inObj.getPos());
            D3int offset = new D3int(inObj.getOffset());
            D3float elSize = new D3float(inObj.getElSize());
            String procLog = inObj.getProcLog();

            @Override
            public D3int getDim() {return dim;}

            @Override
            public D3float getElSize() {return elSize;}

            @Override
            public D3int getOffset() {return offset;}

            @Override
            public D3int getPos() {return pos;}

            @Override
            public String getProcLog() {return procLog;}
            float ssf = inObj.getShortScaleFactor();
            @Override
            public float getShortScaleFactor() {return ssf;}

            @Override
            public String appendProcLog(String inData) {
                procLog+="\n"+inData;
                return procLog;
            }

            @Override
            public void setDim(D3int inData) {dim=inData;}

            @Override
            public void setElSize(D3float inData) {elSize=inData;}

            @Override
            public void setOffset(D3int inData) {offset=inData;}

            @Override
            public void setPos(D3int inData) {pos=inData;}

            @Override
            public void setShortScaleFactor(float ssf) {this.ssf=ssf;}

        };
    }
    /**
     * put just the relevant dimension reading code in a seperate interface
     *
     * @author mader
     */
    public static interface HasDimensions {
        /**
         * The size of the image
         */
        public D3int getDim();

        /**
         * The element size (in mm) of a voxel
         */
        public D3float getElSize();

        /**
         * The size of the border around the image which does not contain valid
         * voxel data
         */
        @Deprecated
        public D3int getOffset();

        /**
         * The position of the bottom leftmost voxel in the image in real space,
         * only needed for ROIs
         */
        public D3int getPos();

        /**
         * Procedure Log, string containing past operations and information on
         * the aim-file
         */
        public String getProcLog();

        /**
         * A function to change the short scale factor used to convert shorts to
         * double and back
         * The factor to scale bool/short/int/char values by when converting to/from
         * float (for distance maps is (1000.0/32767.0))
         *
         * @return
         */
        @Deprecated
        public float getShortScaleFactor();


    }

    public static HasDimensions SimpleDimensions(final D3int dim, final D3float elSize,final  D3int pos) {
        return new HasDimensions() {
            @Override
            public D3int getDim() {return dim;}

            @Override
            public D3float getElSize() {return elSize;}

            @Override
            public D3int getOffset() {	return new D3int(0,0,0);}

            @Override
            public D3int getPos() { return pos;}

            @Override
            public String getProcLog() {
                return "";
            }

            @Override
            public float getShortScaleFactor() {
                return 1;
            }

        };
    }
}
