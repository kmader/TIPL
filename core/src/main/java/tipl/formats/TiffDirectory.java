/**
 *
 */
package tipl.formats;

import com.sun.media.jai.codec.*;

import tipl.formats.TiffFolder.TIFSliceReader;
import tipl.util.*;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author maderk
 */
public class TiffDirectory implements TImg {
    private final int TAG_PROCLOGSTART = 1000;
    private final int TAG_ELSIZE = 998;
    private final int TAG_OFFSET = 997;
    private final int TAG_POS = 996;
    private final int TAG_SHORTSCALE = 995;
    private final int TAG_ISSIGNED = 994;
    private final File imgFile;
    final private ImageDecoder dec;
    final private TypedPath dirPath;
    /**
     * Whether or not to use compression should be used when writing data
     */
    public boolean useCompression = false;
    /**
     * Whether or not LZW (much better) compression should be used when writing
     * data
     */
    public boolean useHighCompression = true;
    private D3int dim;
    private D3int pos = new D3int(0, 0, 0);
    private D3float elSize = new D3float(1, 1, 1);
    private D3int offset = new D3int(0, 0, 0);
    private float ShortScaleFactor = 1.0f;
    private String procLog = "";
    private boolean signedValue = true;
    private int imageType;

    public TiffDirectory(final TypedPath path) throws IOException {
        dirPath = path;
        imgFile = new File(path.getPath());

        final SeekableStream s = new FileSeekableStream(imgFile);

        final TIFFDecodeParam param = null;

        final TIFFDirectory tifdir = new TIFFDirectory(s, 0);
        final TIFFField[] allfields = tifdir.getFields();
        // TIFFField tfProcLog=tifdir.getField(TAG_PROCLOG);
        getTIFFheader(allfields);
        dec = ImageCodec.createImageDecoder("tiff", s, param);

        System.out.println("Number of images in this TIFF: "
                + dec.getNumPages());
        appendProcLog("Reading in Layered Tiff: " + path + ", layers:"
                + dec.getNumPages());
        final int zlen = dec.getNumPages();

        final TIFSliceReader tsr = new TiffFolder.TIFSliceReader(
                dec.decodeAsRenderedImage(0));
        final D3int rDim = tsr.getDim();
        rDim.z = zlen;
        setDim(rDim);
    }

    public static void main(final ArgumentParser p) {
        System.out.println("TifDirectory Tool v" + VirtualAim.kVer);
        System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
        final TypedPath inputFile = p.getOptionPath("input", "",
                "Aim File to Convert");
        final TypedPath outputFile = p.getOptionPath("output", "test.tif",
                "Aim File to Convert");
        try {
            final TiffDirectory inputAim = new TiffDirectory(inputFile);
            final VirtualAim bob = new VirtualAim(inputAim);
            TImgTools.WriteTImg(bob, outputFile);
        } catch (final Exception e) {
            System.out.println("Error converting or reading slice");
            e.printStackTrace();
        }

    }

    public static void main(final String[] args) {
        main(TIPLGlobal.activeParser(args));
    }

	/*
     * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getImageType()
	 */

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#appendProcLog(java.lang.String)
     */
    @Override
    public String appendProcLog(final String inData) {
        // TODO Auto-generated method stub
        procLog = TImgTools.appendProcLog(procLog, inData);
        return procLog;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#getCompression()
     */
    @Override
    public boolean getCompression() {
        // TODO Auto-generated method stub
        return useCompression;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#setCompression(boolean)
     */
    @Override
    public void setCompression(final boolean inData) {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Cannot Set Compression Yet!");
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#getDim()
     */
    @Override
    public D3int getDim() {
        return dim;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#setDim(tipl.util.D3int)
     */
    @Override
    public void setDim(final D3int inData) {
        dim = inData;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#getElSize()
     */
    @Override
    public D3float getElSize() {
        return elSize;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#setElSize(tipl.util.D3float)
     */
    @Override
    public void setElSize(final D3float inData) {
        elSize = inData;

    }

    @Override
    public int getImageType() {
        return imageType;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#getOffset()
     */
    @Override
    public D3int getOffset() {
        return offset;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#setOffset(tipl.util.D3int)
     */
    @Override
    public void setOffset(final D3int inData) {
        offset = inData;

    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#getPath()
     */
    @Override
    public TypedPath getPath() {
        // TODO Auto-generated method stub
        return dirPath;
    }

    @Override
    public Object getPolyImage(final int sliceNumber, final int asType) {
        try {
            return readSlice(sliceNumber, asType);
        } catch (final Exception e) {
            System.out.println("Error Reading slicee from " + sliceNumber
                    + " of " + getDim().z + " as type ");
            e.printStackTrace();
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#getPos()
     */
    @Override
    public D3int getPos() {
        return pos;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#setPos(tipl.util.D3int)
     */
    @Override
    public void setPos(final D3int inData) {
        pos = inData;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#getProcLog()
     */
    @Override
    public String getProcLog() {
        // TODO Auto-generated method stub
        return procLog;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#getSampleName()
     */
    @Override
    public String getSampleName() {
        return dirPath.getPath();
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#getShortScaleFactor()
     */
    @Override
    public float getShortScaleFactor() {
        return ShortScaleFactor;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#setShortScaleFactor(float)
     */
    @Override
    public void setShortScaleFactor(final float ssf) {
        ShortScaleFactor = ssf;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#getSigned()
     */
    @Override
    public boolean getSigned() {
        return signedValue;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#setSigned(boolean)
     */
    @Override
    public void setSigned(final boolean inData) {
        signedValue = inData;
    }

    public void getTIFFheader(final TIFFField[] allfields) {

        int skippedTag = 0;
        int totalTag = 0;
        for (TIFFField allfield : allfields) {
            totalTag++;
            switch (allfield.getTag()) {
                case TAG_PROCLOGSTART:
                    if ((allfield.getType() == TIFFField.TIFF_ASCII)) {
                        appendProcLog("Reloaded...\n" + allfield.getAsString(0));
                    } else {
                        System.out.println("Invalid PROCLOG...");
                    }

                    break;
                case TAG_POS:
                    if ((allfield.getType() == TIFFField.TIFF_SSHORT)
                            && (allfield.getCount() == 3)) {

                        pos.x = allfield.getAsInt(0);
                        pos.y = allfield.getAsInt(1);
                        pos.z = allfield.getAsInt(2);
                        System.out.println("Header-POS :" + pos);

                    } else {
                        System.out.println("Invalid POS...");
                    }
                    break;
                case TAG_OFFSET:
                    if ((allfield.getType() == TIFFField.TIFF_SSHORT)
                            && (allfield.getCount() == 3)) {

                        offset.x = allfield.getAsInt(0);
                        offset.y = allfield.getAsInt(1);
                        offset.z = allfield.getAsInt(2);
                        System.out.println("Header-OFFSET :" + offset);

                    } else {
                        System.out.println("Invalid OFFSET...");
                    }
                    break;
                case TAG_ELSIZE:
                    if ((allfield.getType() == TIFFField.TIFF_FLOAT)
                            && (allfield.getCount() == 3)) {

                        elSize.x = allfield.getAsFloat(0);
                        elSize.y = allfield.getAsFloat(1);
                        elSize.z = allfield.getAsFloat(2);

                        System.out.println("Header-ELSIZE :" + elSize);

                    } else {
                        System.out.println("Invalid ELSIZE...");
                    }
                    break;
                case TAG_SHORTSCALE:
                    if ((allfield.getType() == TIFFField.TIFF_FLOAT)
                            && (allfield.getCount() > 0)) {

                        ShortScaleFactor = allfield.getAsFloat(0);
                        if (Math.abs(ShortScaleFactor) < 1e-6) {
                            System.out.println("Invalid SSF (too small)"
                                    + ShortScaleFactor + ", Reseting to 1.0");
                            ShortScaleFactor = 1.0f;
                        }

                        System.out.println("Short-to-float-Scale Factor :"
                                + ShortScaleFactor);

                    } else {
                        System.out.println("Invalid SHORTSCALE...");
                    }
                    break;
                case TAG_ISSIGNED:
                    if ((allfield.getType() == TIFFField.TIFF_BYTE)
                            && (allfield.getCount() > 0)) {

                        // isSigned=allfields[i].getAsInt(0)>0;

                        System.out.println("Signed Values :" + getSigned());

                    } else {
                        System.out.println("Invalid isSigned Field...");
                    }
                    break;
                default:
                    // Ignore unknown header tags (but dont make a big deal of it)
                    // System.out.println("Ignoring header: " +
                    // allfields[i].getType()+" size-"+allfields[i].getCount()+
                    // " @"+allfields[i].getTag());
                    skippedTag++;
                    break;
            }

        }
        System.out.println("Scanning tiff-header..." + skippedTag
                + " tags skipped of " + totalTag);
    }

    @Override
    public TImg inheritedAim(final boolean[] imgArray, final D3int dim,
                             final D3int offset) {
        return TImgTools.makeTImgExportable(this).inheritedAim(imgArray, dim,
                offset);
    }

    @Override
    public TImg inheritedAim(final char[] imgArray, final D3int dim,
                             final D3int offset) {
        return TImgTools.makeTImgExportable(this).inheritedAim(imgArray, dim,
                offset);
    }

    @Override
    public TImg inheritedAim(final float[] imgArray, final D3int dim,
                             final D3int offset) {
        return TImgTools.makeTImgExportable(this).inheritedAim(imgArray, dim,
                offset);
    }

    @Override
    public TImg inheritedAim(final int[] imgArray, final D3int dim,
                             final D3int offset) {
        return TImgTools.makeTImgExportable(this).inheritedAim(imgArray, dim,
                offset);
    }

    @Override
    public TImg inheritedAim(final short[] imgArray, final D3int dim,
                             final D3int offset) {
        return TImgTools.makeTImgExportable(this).inheritedAim(imgArray, dim,
                offset);
    }

    // Temporary solution,
    @Override
    public TImg inheritedAim(final TImgRO inAim) {
        return TImgTools.makeTImgExportable(this).inheritedAim(inAim);
    }

    @Override
    public boolean InitializeImage(final D3int dPos, final D3int cDim,
                                   final D3int dOffset, final D3float elSize, final int imageType) {
        throw new IllegalArgumentException(
                " Cannot Be Initialized in this manner");
    }

    @Override
    public int isFast() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.TImg#isGood()
     */
    @Override
    public boolean isGood() {
        // TODO Auto-generated method stub
        return false;
    }

    private Object readSlice(final int slice, final int asType)
            throws IOException {
        // TODO Auto-generated method stub
        if (slice >= dec.getNumPages()) {
            throw new IOException("Exceeds bound!!! " + slice + " of "
                    + dec.getNumPages());
        }
        final RenderedImage im = dec.decodeAsRenderedImage(slice);
        final TIFSliceReader tsr = new TiffFolder.TIFSliceReader(im);

        return tsr.polyReadImage(asType);
    }

    public void setTIFFheader(final TIFFEncodeParam tparam) {
        getProcLog().split("\n");
        final TIFFField[] tiffProcLog = new TIFFField[6];
        tiffProcLog[0] = new TIFFField(TAG_PROCLOGSTART, TIFFField.TIFF_ASCII,
                1, new String[]{getProcLog() + "\0"});
        tiffProcLog[1] = new TIFFField(TAG_ELSIZE, TIFFField.TIFF_FLOAT, 3,
                new float[]{(float) elSize.x, (float) elSize.y,
                        (float) elSize.z});
        tiffProcLog[2] = new TIFFField(TAG_POS, TIFFField.TIFF_SSHORT, 3,
                new short[]{(short) pos.x, (short) pos.y, (short) pos.z});
        tiffProcLog[3] = new TIFFField(TAG_OFFSET, TIFFField.TIFF_SSHORT, 3,
                new short[]{(short) offset.x, (short) offset.y,
                        (short) offset.z});
        tiffProcLog[4] = new TIFFField(TAG_SHORTSCALE, TIFFField.TIFF_FLOAT, 1,
                new float[]{ShortScaleFactor});
        tiffProcLog[5] = new TIFFField(TAG_ISSIGNED, TIFFField.TIFF_BYTE, 1,
                new byte[]{(byte) (getSigned() ? 5 : 0)});
        tparam.setExtraFields(tiffProcLog);
        // System.out.println("Writing tiff-header...");
    }


}
