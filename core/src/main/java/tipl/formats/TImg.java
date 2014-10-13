package tipl.formats;

import java.io.Serializable;

import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;
import tipl.util.TImgTools.HasDimensions;

/**
 * TImg is the central read/writable class for image data used for moving around and exporting images
 */
public interface TImg extends TImgRO, TImgRO.CanExport,
        TImgTools.ChangesDimensions {
    /**
     * Is the image signed (should an offset be added / subtracted when the data
     * is loaded to preserve the sign)
     */
    @Override
    public boolean getSigned();

    public void setSigned(boolean inData);

    /**
     * The function the reader uses to initialize an AIM
     */
    public boolean InitializeImage(D3int dPos, D3int cDim, D3int dOffset,
                                   D3float elSize, int imageType);

    /**
     * Is the data in good shape
     */
    @Override
    public boolean isGood();

    public void setCompression(boolean inData);


    /**
     * Set the short scalar factor in the image data *
     */
    @Override
    public void setShortScaleFactor(float ssf);
    static public class ArrayBackedTImg extends ATImg implements Serializable {
    	final Object[] sliceData;
    	final int sliceType;
    	/**
    	 * Create an array backed image from any inAim
    	 * @param inAim the input image to solidify
    	 * @param stype the type of image to save
    	 * @return
    	 */
    	public static ArrayBackedTImg CreateFromTImg(TImgRO inAim,final int stype) {
    		Object[] sData = new Object[inAim.getDim().z];
    		for(int z=0;z<inAim.getDim().z;z++) sData[z]=inAim.getPolyImage(z, stype);
    		return new ArrayBackedTImg(inAim.getDim(),inAim.getPos(),inAim.getElSize(),stype,sData);
    	}
    	
    	private ArrayBackedTImg(HasDimensions tempImg, int iimageType,Object[] sliceData) {
    		super(tempImg,iimageType);
    		this.sliceData=sliceData;
    		this.sliceType=iimageType;
    	}
    	
    	public ArrayBackedTImg(D3int idim, D3int ipos, D3float ielSize, final int iimageType,Object[] sliceData) {
            super(idim, ipos, ielSize, iimageType);
            this.sliceData=sliceData;
    		this.sliceType=iimageType;
        }
		@Override
		public Object getPolyImage(int sliceNumber, int asType) {
			return TImgTools.convertArrayType(sliceData[sliceNumber], sliceType, asType, getSigned(), getShortScaleFactor());
		}
		@Override
		public String getSampleName() {
			return ArrayBackedTImg.class.getSimpleName()+",path:"+getPath()+", dim:"+getDim();
		}

    }
    
    	
    
    /**
     * A class for handling the basic mundane functions inside TImg
     *
     * @author mader
     */

    static public class ATImgWrappingTImgRO extends ATImg {
        final TImgRO coreImage;
        public ATImgWrappingTImgRO(final TImgRO inImage) {
            super(inImage);
            coreImage = inImage;
        }

        @Override
        public Object getPolyImage(int sliceNumber, int asType) {
            return coreImage.getPolyImage(sliceNumber,asType);
        }

        @Override
        public String getSampleName() {
            return coreImage.getSampleName();
        }
    }

    static public abstract class ATImg extends TImgRO.ATImgRO implements TImg {

        public ATImg(final TImgRO tempImg) { super(tempImg,tempImg.getImageType()); }

        public ATImg(HasDimensions tempImg, int iimageType) {
            super(tempImg, iimageType);
        }

        public ATImg(D3int idim, D3int ipos, D3float ielSize, final int iimageType) {
            super(idim, ipos, ielSize, iimageType);
        }
        /**
         * The default implementation (can be overridden for other datatypes like DTImg for example)
         * @param dim
         * @param pos
         * @param elSize
         * @param stype
         * @param sliceData
         * @return TImg object
         */
        protected TImg makeImageFromDataArray(D3int dim,D3int pos,D3float elSize,int stype,Object[] sliceData) {
        	return new ArrayBackedTImg(dim,pos,elSize,stype,sliceData);
        }

        @Override
        public TImg inheritedAim(boolean[] imgArray, D3int idim, D3int offset) {
        	final int stype = TImgTools.IMAGETYPE_BOOL;
			Object[] sData = new Object[idim.z];
			int sliceSize = idim.x*idim.y;
			for(int z=0;z<idim.z;z++) {
				sData[z]=new boolean[sliceSize];
				final int outPos = z * sliceSize;
				System.arraycopy(imgArray, outPos, sData[z], 0, sliceSize);
			}
			return makeImageFromDataArray(idim,this.getPos(),this.getElSize(),stype,sData);
        }

        @Override
        public TImg inheritedAim(char[] imgArray, D3int idim, D3int offset) {
        	final int stype = TImgTools.IMAGETYPE_CHAR;
			Object[] sData = new Object[idim.z];
			int sliceSize = idim.x*idim.y;
			for(int z=0;z<idim.z;z++) {
				sData[z]=new char[sliceSize];
				final int outPos = z *sliceSize;
				System.arraycopy(imgArray, outPos, sData[z], 0, sliceSize);
			}
			return makeImageFromDataArray(idim,this.getPos(),this.getElSize(),stype,sData);
        }

        @Override
        public TImg inheritedAim(float[] imgArray, D3int idim, D3int offset) {
        	final int stype = TImgTools.IMAGETYPE_FLOAT;
			Object[] sData = new Object[idim.z];
			int sliceSize = idim.x*idim.y;
			for(int z=0;z<idim.z;z++) {
				sData[z]=new float[sliceSize];
				final int outPos = z *sliceSize;
				System.arraycopy(imgArray, outPos, sData[z], 0, sliceSize);
			}
			return makeImageFromDataArray(idim,this.getPos(),this.getElSize(),stype,sData);
        }

        @Override
        public TImg inheritedAim(int[] imgArray, D3int idim, D3int offset) {
        	final int stype = TImgTools.IMAGETYPE_INT;
			Object[] sData = new Object[idim.z];
			int sliceSize = idim.x*idim.y;
			for(int z=0;z<idim.z;z++) {
				sData[z]=new int[sliceSize];
				final int outPos = z * sliceSize;
				System.arraycopy(imgArray, outPos, sData[z], 0, sliceSize);
			}
			return new ArrayBackedTImg(idim,this.getPos(),this.getElSize(),stype,sData);
        }

        @Override
        public TImg inheritedAim(short[] imgArray, D3int idim, D3int offset) {
        	final int stype = TImgTools.IMAGETYPE_SHORT;
			Object[] sData = new Object[idim.z];
			int sliceSize = idim.x*idim.y;
			for(int z=0;z<idim.z;z++) {
				sData[z]=new short[sliceSize];
				final int outPos = z *sliceSize;
				System.arraycopy(imgArray, outPos, sData[z], 0, sliceSize);
			}
			return makeImageFromDataArray(idim,this.getPos(),this.getElSize(),stype,sData);
        }
        
		@Override
		public TImg inheritedAim(TImgRO inAim) {
			final int stype = inAim.getImageType();
			Object[] sData = new Object[inAim.getDim().z];
			for(int z=0;z<inAim.getDim().z;z++) sData[z]=inAim.getPolyImage(z, stype);
			return makeImageFromDataArray(inAim.getDim(),this.getPos(),this.getElSize(),stype,sData);
		}

        @Override
        public void setDim(D3int inData) {
        	dim = inData;
        }

        @Override
        public void setElSize(D3float inData) {
            elSize = inData;
        }

        @Override
        public void setOffset(D3int inData) {
            offset = inData;
        }

        @Override
        public void setPos(D3int inData) {
            pos = inData;
        }

        @Override
        public boolean InitializeImage(D3int dPos, D3int cDim, D3int dOffset,
                                       D3float elSize, int imageType) {
            throw new IllegalArgumentException("This annoys me, please do not use this function");

        }

        @Override
        public void setCompression(boolean inData) {
            // TODO Auto-generated method stub
            throw new IllegalArgumentException("This annoys me, please do not use this function");

        }


        @Override
        public void setShortScaleFactor(float ssf) {
        	this.ssf=ssf;
        }

        @Override
        public void setSigned(boolean inData) {
           this.signed=inData;
        }
    }


}
