/**
 *
 */
package tipl.ij;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.ITIPLStorage;
import tipl.util.TImgTools;

/**
 * @author mader
 */
public class ImageStackToTImg implements TImg {
    protected final Object[] stack;
    final int imageType;
    final D3int dim;
    protected D3int pos = new D3int(0);
    protected D3int offset = new D3int(0);
    protected D3float elSize = new D3float(0, 0, 0);
    protected String procLog = "";

    /**
     *
     */
    protected ImageStackToTImg(final ImageStack inStack) {
        dim = (new D3int(inStack.getWidth(), inStack.getHeight(),
                inStack.getSize() + 1));
        stack = inStack.getImageArray();
        // Figure out what is in the stack
        imageType = TImgTools.identifySliceType(stack[0]);
    }

    public static TImg FromStack(final ImageStack inStack) {
        return new ImageStackToTImg(inStack);
    }

    public static TImg FromImagePlus(final ImagePlus inPlus) {
        ImageStackToTImg ist = new ImageStackToTImg(inPlus.getStack());
        // Read in element size
        final Calibration cal = inPlus.getCalibration();
        final double pw = cal.pixelWidth;
        final double ph = cal.pixelHeight;
        final double pd = cal.pixelDepth;

        ist.setElSize(new D3float(pw, ph, pd));
        return ist;
    }

    @Override
    public D3int getDim() {
        return dim;
    }

    /* (non-Javadoc)
     * @see tipl.util.TImgTools.ChangesDimensions#setDim(tipl.util.D3int)
     */
    @Override
    public void setDim(D3int inData) {
        throw new IllegalArgumentException("Dimensions of this image cannot be changed");
    }

    @Override
    public D3float getElSize() {
        return elSize;
    }

    /* (non-Javadoc)
     * @see tipl.util.TImgTools.ChangesDimensions#setElSize(tipl.util.D3float)
     */
    @Override
    public void setElSize(D3float inData) {
        elSize = inData;
    }

    @Override
    public D3int getOffset() {
        return offset;
    }

    /* (non-Javadoc)
     * @see tipl.util.TImgTools.ChangesDimensions#setOffset(tipl.util.D3int)
     */
    @Override
    public void setOffset(D3int inData) {
        offset = inData;
    }

    @Override
    public D3int getPos() {
        return pos;
    }

    /* (non-Javadoc)
     * @see tipl.util.TImgTools.ChangesDimensions#setPos(tipl.util.D3int)
     */
    @Override
    public void setPos(D3int inData) {
        pos = inData;
    }

    @Override
    public String getProcLog() {
        // TODO Auto-generated method stub
        return procLog;
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO.CanExport#inheritedAim(boolean[], tipl.util.D3int, tipl.util.D3int)
     */
    @Override
    public TImg inheritedAim(boolean[] imgArray, D3int dim, D3int offset) {
        throw new IllegalArgumentException("no inheritance here!");
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO.CanExport#inheritedAim(char[], tipl.util.D3int, tipl.util.D3int)
     */
    @Override
    public TImg inheritedAim(char[] imgArray, D3int dim, D3int offset) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO.CanExport#inheritedAim(float[], tipl.util.D3int, tipl.util.D3int)
     */
    @Override
    public TImg inheritedAim(float[] imgArray, D3int dim, D3int offset) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO.CanExport#inheritedAim(int[], tipl.util.D3int, tipl.util.D3int)
     */
    @Override
    public TImg inheritedAim(int[] imgArray, D3int dim, D3int offset) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO.CanExport#inheritedAim(short[], tipl.util.D3int, tipl.util.D3int)
     */
    @Override
    public TImg inheritedAim(short[] imgArray, D3int dim, D3int offset) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO.CanExport#inheritedAim(tipl.formats.TImgRO)
     */
    @Override
    public TImg inheritedAim(TImgRO inAim) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImg#InitializeImage(tipl.util.D3int, tipl.util.D3int, tipl.util.D3int, tipl.util.D3float, int)
     */
    @Override
    public boolean InitializeImage(D3int dPos, D3int cDim, D3int dOffset,
                                   D3float elSize, int imageType) {
        throw new IllegalArgumentException(this + "Image Compression cannot be changed!");

    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO#appendProcLog(java.lang.String)
     */
    @Override
    public String appendProcLog(String inData) {
        // TODO Auto-generated method stub
        procLog += this.getClass().getSimpleName() + ":" + inData + "\n";
        return getProcLog();
    }

    @Override
    public boolean getCompression() {
        return false;
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImg#setCompression(boolean)
     */
    @Override
    public void setCompression(boolean inData) {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException(this + "Image Compression cannot be changed!");
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
        return this.toString();
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO#getPolyImage(int, int)
     */
    @Override
    public Object getPolyImage(int sliceNumber, int asType) {
        return TImgTools.convertArrayType(getCurrentImageRaw(sliceNumber), imageType, asType, false, getShortScaleFactor(), 100);
    }

    protected Object getCurrentImageRaw(int sliceNumber) {
        final int imgVoxCnt = dim.x * dim.y;
        // Pre allocate array and read in data

        switch (imageType) {
            case 10:
                boolean[] aimMask = new boolean[imgVoxCnt];
                final char[] bstack = (char[]) stack[sliceNumber];
                for (int j = 0; j < imgVoxCnt; j++)
                    aimMask[j] = (bstack[j] > 0);
                return aimMask;
            case 1:
                return stack[sliceNumber];
            default:
                return stack[sliceNumber];

        }
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO#getSampleName()
     */
    @Override
    public String getSampleName() {
        // TODO Auto-generated method stub
        return this.toString();
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
     * @see tipl.formats.TImg#setShortScaleFactor(float)
     */
    @Override
    public void setShortScaleFactor(float ssf) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO#getSigned()
     */
    @Override
    public boolean getSigned() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImg#setSigned(boolean)
     */
    @Override
    public void setSigned(boolean inData) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO#isFast()
     */
    @Override
    public int isFast() {
        // TODO Auto-generated method stub
        return ITIPLStorage.FAST_MEMORY_COMPUTATION_BASED;
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO#isGood()
     */
    @Override
    public boolean isGood() {
        // TODO Auto-generated method stub
        return true;
    }

}
