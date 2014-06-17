package tipl.formats;

import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A Read-only TImg (the full TImg is a subinterface of this) useful for
 * concurrent tasks where writing should be impossible
 */
public interface TImgRO extends TImgTools.HasDimensions, Serializable {
    public String appendProcLog(String inData);

    /**
     * Whether or not basic compression (compatible almost everywhere) should be
     * used when writing data
     */
    public boolean getCompression();

    /**
     * The aim type of the image (0=char, 1=short, 2=int, 3=float, 10=bool, -1
     * same as input)
     */
    public int getImageType();

    /**
     * The path of the data (whatever it might be)
     */
    public String getPath();

    public Object getPolyImage(int sliceNumber, int asType);

    /**
     * The name of the data
     */
    public String getSampleName();

    /**
     * The factor to scale bool/short/int/char values by when converting to/from
     * float (for distance maps is (1000.0/32767.0))
     */
    @Override
    public float getShortScaleFactor();

    /**
     * Is the image signed (should an offset be added / subtracted when the data
     * is loaded to preserve the sign)
     * Note
     * the array conversion does nothing strange when getsigned is off (it is only in place to ensure a consist translation between char/short to int/float but is probably out of date
     */
    public boolean getSigned();

    /**
     * Check to see if the image is cached or otherwise fast access (used for
     * caching methods to avoid double caching), 0 is encoded disk-based, 1 is
     * memory map, 2 is in memory + computation, 3 is memory based, in general
     * each computational layer (FImage) subtracts 1 from the isFast
     */
    public int isFast();

    /**
     * Is the data in good shape
     */
    public boolean isGood();

    /**
     * CanExport means the TImg is capable of imparting itself on other images.
     *
     * @author mader
     */
    public static interface CanExport extends TImgRO {
        @Deprecated
        public TImg inheritedAim(boolean[] imgArray, D3int dim, D3int offset);

        @Deprecated
        public TImg inheritedAim(char[] imgArray, D3int dim, D3int offset);

        @Deprecated
        public TImg inheritedAim(float[] imgArray, D3int dim, D3int offset);


        @Deprecated
        public TImg inheritedAim(int[] imgArray, D3int dim, D3int offset);

        @Deprecated
        public TImg inheritedAim(short[] imgArray, D3int dim, D3int offset);

        public TImg inheritedAim(TImgRO inAim);
    }

    /**
     * FullReadable means the entire image can be read out into a single linear
     * array sized x*y*z The standard processing format used in IPL and limits
     * array to 2^31 elements ~ 1300^3 This really should not be used in future
     * plugins if at all possible
     *
     * @author mader
     */
    @Deprecated
    public static interface FullReadable extends TImgRO {
        public boolean[] getBoolAim();

        public char[] getByteAim();

        public float[] getFloatAim();

        public int[] getIntAim();

        public short[] getShortAim();
    }

    /**
     * an object that can output its entire stack
     *
     * @author mader
     */
    public static interface HasStack extends TImgRO {
        /**
         * Returns a stack of images as an array of linear arrays
         *
         * @param asType
         * @return
         */
        public Object[] getPolyStack(int asType);
    }

    public static interface TImgOld {
        public boolean[] getBoolArray(int sliceNumber);

        public char[] getByteArray(int sliceNumber);

        public float[] getFloatArray(int sliceNumber);

        public int[] getIntArray(int sliceNumber);

        public short[] getShortArray(int sliceNumber);
    }

    /**
     * an abstract class with all of the standard TImgRO functions but getPoly
     *
     * @author mader
     */
    public abstract class ATImgRO implements TImgRO {
        private static final long serialVersionUID = -8883859233940303695L;
        final protected D3int dim;
        final protected int imageType;
        protected D3int pos;
        protected D3float elSize;
        protected D3int offset = new D3int(0);
        protected String procLog = "";

        public ATImgRO(TImgTools.HasDimensions tempImg, final int iimageType) {
            assert (TImgTools.isValidType(iimageType));
            dim = tempImg.getDim();
            pos = tempImg.getPos();
            elSize = tempImg.getElSize();
            procLog = tempImg.getProcLog();

            imageType = iimageType;
        }

        public ATImgRO(D3int idim, D3int ipos, D3float ielSize, final int iimageType) {
            assert (TImgTools.isValidType(iimageType));
            dim = idim;
            pos = ipos;
            elSize = ielSize;
            imageType = iimageType;
        }

        /* (non-Javadoc)
         * @see tipl.util.TImgTools.HasDimensions#getDim()
         */
        @Override
        public D3int getDim() {
            return dim;
        }

        /* (non-Javadoc)
         * @see tipl.util.TImgTools.HasDimensions#getElSize()
         */
        @Override
        public D3float getElSize() {
            return elSize;
        }

        /* (non-Javadoc)
         * @see tipl.util.TImgTools.HasDimensions#getOffset()
         */
        @Override
        public D3int getOffset() {
            return offset;
        }

        /* (non-Javadoc)
         * @see tipl.util.TImgTools.HasDimensions#getPos()
         */
        @Override
        public D3int getPos() {
            return pos;
        }

        /* (non-Javadoc)
         * @see tipl.util.TImgTools.HasDimensions#getProcLog()
         */
        @Override
        public String getProcLog() {
            return procLog;
        }

        /* (non-Javadoc)
         * @see tipl.formats.TImgRO#appendProcLog(java.lang.String)
         */
        @Override
        public String appendProcLog(String inData) {
            procLog += inData;
            return procLog;
        }

        /* (non-Javadoc)
         * @see tipl.formats.TImgRO#getCompression()
         */
        @Override
        public boolean getCompression() {
            return false;
        }

        /* (non-Javadoc)
         * @see tipl.formats.TImgRO#getImageType()
         */
        @Override
        public int getImageType() {
            return imageType;
        }

        /* (non-Javadoc)
         * @see tipl.formats.TImgRO#getPath()
         */
        @Override
        public String getPath() {
            // TODO Auto-generated method stub
            return "";
        }

        /* (non-Javadoc)
         * @see tipl.formats.TImgRO#getShortScaleFactor()
         */
        @Override
        public float getShortScaleFactor() {
            // TODO Auto-generated method stub
            return 1;
        }

        /* (non-Javadoc)
         * @see tipl.formats.TImgRO#getSigned()
         */
        @Override
        public boolean getSigned() {
            return false;
        }

        /* (non-Javadoc)
         * @see tipl.formats.TImgRO#isFast()
         */
        @Override
        public int isFast() {
            return TImgTools.SPEED_DISK_AND_MEM;
        }

        /* (non-Javadoc)
         * @see tipl.formats.TImgRO#isGood()
         */
        @Override
        public boolean isGood() {
            return true;
        }
    }

    public static class TImgFull implements TImgOld {
        protected final TImgRO myImg;

        public TImgFull(final TImgRO inImg) {
            myImg = inImg;
        }

        @Override
        public boolean[] getBoolArray(final int sliceNumber) {
            return (boolean[]) myImg.getPolyImage(sliceNumber, 10);
        }

        @Override
        public char[] getByteArray(final int sliceNumber) {
            return (char[]) myImg.getPolyImage(sliceNumber, 0);
        }

        @Override
        public float[] getFloatArray(final int sliceNumber) {
            return (float[]) myImg.getPolyImage(sliceNumber, 3);
        }

        @Override
        public int[] getIntArray(final int sliceNumber) {
            return (int[]) myImg.getPolyImage(sliceNumber, 2);
        }

        @Override
        public short[] getShortArray(final int sliceNumber) {
            return (short[]) myImg.getPolyImage(sliceNumber, 1);
        }

        /**
         * return the underlying TImgRO object
         *
         * @return the TImgRO object
         */
        public TImgRO gT() {
            return myImg;
        }
    }

    public abstract static class TImgStack implements HasStack {
        @Override
        public Object[] getPolyStack(final int asType) {
            final ArrayList<Object> cStack = new ArrayList<Object>();
            for (int i = 0; i <= getDim().z; i++)
                cStack.add(getPolyImage(i, asType));
            return cStack.toArray();
        }
    }

}
