package tipl.util;

import tipl.formats.TImgRO;

import java.io.Serializable;

/**
 * The representation of a single block in an image (typically a slice) containing the original position and any offset from this position which is useful for filtering
 *
 * @param <V> The class of the data inside (typically int[] or boolean[])
 * @author mader
 */
@Deprecated // ("Use TBlockSlice or TImgBlock instead")
public class TImgSlice<V> implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -7608227903926605173L;
    /**
     * the dimensions of the slice (2d since it is only a slice)
     */
    final ID2int dim;
    /**
     * the position of the slice and/or block
     */
    final D3int pos;
    /**
     * the offset from the original slice number of the image (used in filtering)
     */
    final D3int offset;
    /**
     * the contents of the slice itself
     */
    final private V sliceData;

    /**
     * create a new block given a chunk of data and a position and dimensions
     *
     * @param pos
     * @param cSlice
     * @param dim
     */
    public TImgSlice(V cSlice, D3int pos, ID2int dim) {
        this.sliceData = cSlice;
        this.pos = pos;
        this.offset = D3int.zero;
        this.dim = dim;
    }



    /**
     * Create a new block with an offset given a chunk of data and position, dimensions
     *
     * @param cSlice the block data itself
     * @param pos    position of the upper left corner of the block
     * @param dim    the dimension of the block
     * @param offset the offset of the block
     */
    public TImgSlice(V cSlice, D3int pos, ID2int dim, D3int offset) {
        this.sliceData = cSlice;
        this.pos = pos;
        this.dim = dim;
        this.offset = offset;
    }

    /**
     * Just for subclasses to use (like read future)
     *
     * @param pos
     * @param dim
     * @param offset
     */
    protected TImgSlice(D3int pos, ID2int dim, D3int offset) {
        this.sliceData = null;
        this.pos = pos;
        this.dim = dim;
        this.offset = offset;
    }

    /**
     * Build a new block using information from an old block
     *
     * @param cSlice
     * @param oldBlock
     */
    public TImgSlice(V cSlice, TImgSlice oldBlock) {
        this.sliceData = cSlice;
        this.pos = oldBlock.getPos();
        this.dim = oldBlock.getDim();
        this.offset = oldBlock.getOffset();
    }

    public V get() {
        return sliceData;
    }

    public V getClone() {
        return get();
    }

    public double[] getAsDouble() {
        return TImgTools.convertArrayDouble(sliceData);
    }

    public D3int getPos() {
        return pos;
    }

    public ID2int getDim() {
        return dim;
    }

    public D3int getOffset() {
        return offset;
    }

    /**
     * A class for storing a TImgSlice that reads the file
     *
     * @param <Fu>
     * @author mader
     */
    static abstract public class TImgSliceFuture<Fu> extends TImgSlice<Fu> {
        protected static final boolean debug = TIPLGlobal.getDebug();
        private static final long serialVersionUID = 5184302069088589618L;
        protected boolean cacheResult = true;
        protected int readTimes = 0;
        // should be more or less final
        transient private Fu sliceData;
        transient private boolean isRead = false;

        protected TImgSliceFuture(D3int pos, ID2int dim, D3int offset) {
            super(pos, dim, offset);
        }

        @Override
        synchronized public Fu get() {
            readTimes++;
            if (debug) System.out.println("Calling read function:(#" + readTimes + "):" + this);
            if (isRead) return sliceData;
            else {
                final Fu cSlice = getSliceData();
                if (cacheResult) {
                    sliceData = cSlice;
                    isRead = true;
                }
                return cSlice;
            }
        }

        abstract protected Fu getSliceData();

    }

    /**
     * Create a TImgSlice from a given slice of TImgRO object
     * @param imgObj input image to take slice from
     * @param sliceNo the slice number to take
     * @param outType the output type (should be matched to the type of V
     * @param <V> the type of the data in the slice (must be an array)
     * @return
     */
    static public <V> TImgSlice<V> fromTImg(final TImgRO imgObj, final int sliceNo, final int
            outType) {
        TImgTools.isValidType(outType);
        assert(imgObj.getDim().z<=sliceNo);
        assert(sliceNo>=0);
        return new TImgSlice<V>(
                (V) imgObj.getPolyImage(sliceNo,outType),
                new D3int(imgObj.getPos().x,imgObj.getPos().y,imgObj.getPos().z+sliceNo),
                imgObj.getDim()
        );
    }

    /**
     * Read from the given slice in the future (send the object across the wire and read it on the other side, good for virtual objects)
     *
     * @param <Fu>
     * @author mader
     */
    static public class TImgSliceFromImage<Fu> extends TImgSliceFuture<Fu> {
        protected final TImgRO inImObj;
        protected final int sliceNumber;
        protected final int imgType;

        /**
         * Create a new block with an offset given a chunk of data and position, dimensions
         *
         * @param sliceNumber
         * @param imageType
         * @param pos         position of the upper left corner of the block
         * @param dim         the dimension of the block
         * @param offset      the offset of the block
         */
        public TImgSliceFromImage(final TImgRO inImObj, final int sliceNumber, final int imageType, D3int pos, ID2int dim, D3int offset) {
            super(pos, dim, offset);
            assert (TImgTools.isValidType(imageType));
            this.inImObj = inImObj;
            this.sliceNumber = sliceNumber;
            this.imgType = imageType;
        }

        @SuppressWarnings("unchecked")
        protected Fu getSliceData() {
            Fu outSlice = (Fu) inImObj.getPolyImage(sliceNumber, imgType);
            return outSlice;
        }

        @Override
        public String toString() {
            return "TBFSlice:sl=" + sliceNumber + ",obj=" + inImObj;
        }

    }
    /**
     * Convert a TImgSlice (a single slice) into a TImg Object since it is easier like that
     * @author mader
     *
     */
    static public class TImgSliceAsTImg extends TImgRO.ATImgRO implements TImgRO {
        final protected TImgSlice baseBlock;

        public TImgSliceAsTImg(final TImgSlice baseBlock) {
            super(new D3int(baseBlock.getDim(),1),baseBlock.getPos(),D3float.one,
                    TImgTools.identifySliceType(baseBlock.get()));
            this.baseBlock=baseBlock;
        }
        public TImgSliceAsTImg(final TImgSlice baseBlock,final D3float elSize) {
            super(new D3int(baseBlock.getDim(),1),baseBlock.getPos(),elSize,
                    TImgTools.identifySliceType(baseBlock.get()));
            this.baseBlock=baseBlock;
        }
        @Override
        public Object getPolyImage(int sliceNumber, int asType) {
            assert(sliceNumber==0); //TODO make this more generic in case the block is not a slice
            return TImgTools.convertArrayType(baseBlock.get(), getImageType(), asType, getSigned(), getShortScaleFactor());
        }
        @Override
        public String getSampleName() {
            return baseBlock.toString();
        }

    }

    /**
     * For reading files remotely, creates a future object with the path and slice number of the file to read
     *
     * @param <Fu>
     * @author mader
     */
    static public class TImgSliceFile<Fu> extends TImgSliceFuture<Fu> {
        protected final TypedPath fileName;
        protected final int sliceNumber;
        protected final int imgType;

        /**
         * Create a new block with an offset given a chunk of data and position, dimensions
         *
         * @param path
         * @param sliceNumber
         * @param imageType
         * @param pos         position of the upper left corner of the block
         * @param dim         the dimension of the block
         * @param offset      the offset of the block
         */
        public TImgSliceFile(final TypedPath path, final int sliceNumber, final int imageType, D3int pos, D3int dim, D3int offset) {
            super(pos, dim, offset);
            this.fileName = path;
            this.sliceNumber = sliceNumber;
            this.imgType = imageType;
        }

        protected Fu getSliceData() {
            readTimes += 100;
            if (debug) System.out.println("Reading (#" + readTimes + ") slice:" + this);
            if (!TIPLGlobal.waitForReader())
                throw new IllegalArgumentException("Process was interupted while waiting for reader");
            Fu outSlice = (Fu) TImgTools.ReadTImgSlice(fileName, sliceNumber, imgType);
            TIPLGlobal.returnReader();
            return outSlice;


        }

        @Override
        public String toString() {
            return "SLR:sl=" + sliceNumber + ",path=" + fileName;
        }

        static public int getImageType(TImgSlice blockOfInterest) {
            return TImgTools.identifySliceType(blockOfInterest.get());
        }

    }

    /**
     * Quick check to see if they match
     * @param sliceA
     * @param sliceB
     * @return true if they match in pos and dimension
     */
    static public boolean slicesSizeMatch(TImgSlice<?> sliceA, TImgSlice<?> sliceB) {
        boolean matches = true;
        matches &= sliceA.getDim().gx() == sliceB.getDim().gx();
        matches &= sliceA.getDim().gy() == sliceB.getDim().gy();
        matches &= sliceA.getPos().gx() == sliceB.getPos().gx();
        matches &= sliceA.getPos().gy() == sliceB.getPos().gy();
        return matches;
    }

    /**
     * Throws an error if they do not match since they need to
     * @param sliceA
     * @param sliceB
     */
    static public void doSlicesSizeMatch(TImgSlice<?> sliceA, TImgSlice<?> sliceB) {
        if(!slicesSizeMatch(sliceA,sliceB))
            throw new IllegalArgumentException("Slices do not match in size:"+sliceA+" and " +
                    ""+sliceB+", dim:"+sliceA.getDim()+"=="+sliceB.getDim()+", pos:"+sliceA
                    .getPos()+"=="+sliceB.getPos());
    }


}
