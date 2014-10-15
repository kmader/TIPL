package tipl.util;

import tipl.formats.TImgRO;

import java.io.Serializable;

/**
 * The representation of a single block in an image (typically a slice) containing the original position and any offset from this position which is useful for filtering
 *
 * @param <V> The class of the data inside (typically int[] or boolean[])
 * @author mader
 */
public class TImgBlock<V> implements Serializable {
    final public static D3int zero = new D3int(0);
    /**
     *
     */
    private static final long serialVersionUID = -7608227903926605173L;
    /**
     * the dimensions of the slice
     */
    final D3int dim;
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
    public TImgBlock(V cSlice, D3int pos, D3int dim) {
        this.sliceData = cSlice;
        this.pos = pos;
        this.offset = zero;
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
    public TImgBlock(V cSlice, D3int pos, D3int dim, D3int offset) {
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
    protected TImgBlock(D3int pos, D3int dim, D3int offset) {
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
    public TImgBlock(V cSlice, TImgBlock oldBlock) {
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

    public D3int getDim() {
        return dim;
    }

    public D3int getOffset() {
        return offset;
    }

    /**
     * A class for storing a TImgBlock that reads the file
     *
     * @param <Fu>
     * @author mader
     */
    static abstract public class TImgBlockFuture<Fu> extends TImgBlock<Fu> {
        protected static final boolean debug = TIPLGlobal.getDebug();
        private static final long serialVersionUID = 5184302069088589618L;
        protected boolean cacheResult = true;
        protected int readTimes = 0;
        // should be more or less final
        transient private Fu sliceData;
        transient private boolean isRead = false;

        protected TImgBlockFuture(D3int pos, D3int dim, D3int offset) {
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
     * Read from the given slice in the future (send the object across the wire and read it on the other side, good for virtual objects)
     *
     * @param <Fu>
     * @author mader
     */
    static public class TImgBlockFromImage<Fu> extends TImgBlockFuture<Fu> {
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
        public TImgBlockFromImage(final TImgRO inImObj, final int sliceNumber, final int imageType, D3int pos, D3int dim, D3int offset) {
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
            return "TBF:sl=" + sliceNumber + ",obj=" + inImObj;
        }

    }
    /**
     * Convert a TImgBlock (a single slice) into a TImg Object since it is easier like that
     * @author mader
     *
     */
    static public class TImgBlockAsTImg extends TImgRO.ATImgRO implements TImgRO {
    	final protected TImgBlock baseBlock;
    	
    	public TImgBlockAsTImg(final TImgBlock baseBlock) {
    		super(baseBlock.getDim(),baseBlock.getPos(),new D3float(1,1,1),TImgTools.identifySliceType(baseBlock.get()));
    		this.baseBlock=baseBlock;
    	}
    	public TImgBlockAsTImg(final TImgBlock baseBlock,final D3float elSize) {
    		super(baseBlock.getDim(),baseBlock.getPos(),elSize,TImgTools.identifySliceType(baseBlock.get()));
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
    static public class TImgBlockFile<Fu> extends TImgBlockFuture<Fu> {
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
        public TImgBlockFile(final TypedPath path, final int sliceNumber, final int imageType, D3int pos, D3int dim, D3int offset) {
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

        static public int getImageType(TImgBlock blockOfInterest) {
            return TImgTools.identifySliceType(blockOfInterest.get());
        }

    }


}