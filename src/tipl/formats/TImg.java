package tipl.formats;

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

    /**
     * A class for handling the basic mundane functions inside TImg
     *
     * @author mader
     */

    static public abstract class ATImg extends TImgRO.ATImgRO implements TImg {

        public ATImg(HasDimensions tempImg, int iimageType) {
            super(tempImg, iimageType);
        }

        public ATImg(D3int idim, D3int ipos, D3float ielSize, final int iimageType) {
            super(idim, ipos, ielSize, iimageType);
        }

        @Override
        public TImg inheritedAim(boolean[] imgArray, D3int dim, D3int offset) {
            throw new IllegalArgumentException("This annoys me, please do not use this function");

        }

        @Override
        public TImg inheritedAim(char[] imgArray, D3int dim, D3int offset) {
            throw new IllegalArgumentException("This annoys me, please do not use this function");

        }

        @Override
        public TImg inheritedAim(float[] imgArray, D3int dim, D3int offset) {
            throw new IllegalArgumentException("This annoys me, please do not use this function");

        }

        @Override
        public TImg inheritedAim(int[] imgArray, D3int dim, D3int offset) {
            throw new IllegalArgumentException("This annoys me, please do not use this function");

        }

        @Override
        public TImg inheritedAim(short[] imgArray, D3int dim, D3int offset) {
            throw new IllegalArgumentException("This annoys me, please do not use this function");

        }

        @Override
        public void setDim(D3int inData) {
            throw new IllegalArgumentException("This annoys me, please do not use this function");
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
            throw new IllegalArgumentException("This annoys me, please do not use this function");

        }

        @Override
        public void setSigned(boolean inData) {
            throw new IllegalArgumentException("This annoys me, please do not use this function");

        }
    }

}
