package tipl.formats;

import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codec.TIFFField;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

import java.io.Serializable;

/**
 * TImgHeader is a unified repository for all of the functions related to reading and writing header information
 * 
 *
 * @author mader
 */

public class TImgHeader implements Serializable, TImgTools.HasDimensions, TImgTools.ChangesDimensions {
    static public final int TAG_PROCLOGSTART = 1000;
    static public final int TAG_ELSIZE = 998;
    static public final int TAG_OFFSET = 997;
    static public final int TAG_POS = 996;
    static public final int TAG_SHORTSCALE = 995;
    static public final int TAG_ISSIGNED = 994;

    protected String procLog = "";
    protected String outpath = "";
    protected D3int indim = new D3int(-1, -1, 1);
    protected D3int inpos = new D3int(0);
    protected D3int inoffset = new D3int(0);
    protected D3float elSize = new D3float();
    protected boolean isSigned = false;
    protected float ssf = 1.0f;

    /**
     * shouldn't be constructed, use the factories
     */
    protected TImgHeader() {
    }

    public static TImgHeader ReadHeadersFromTImg(TImgRO templateImage) {
        TImgHeader out = new TImgHeader();
        TImgTools.mirrorImage(templateImage, out);
        return out;
    }

    public static TImgHeader ReadHeaderFromTIFF(final TIFFField[] allfields) {
        TImgHeader out = new TImgHeader();
        out.readFromTIFF(allfields);
        return out;
    }

    /**
     * set the header for each slice
     *
     * @param tparam
     */
    public void writeToTIFF(final TIFFEncodeParam tparam) {
        final TIFFField[] tiffProcLog = new TIFFField[6];
        tiffProcLog[0] = new TIFFField(TAG_PROCLOGSTART, TIFFField.TIFF_ASCII,
                1, new String[]{procLog + "\0"});
        tiffProcLog[1] = new TIFFField(TAG_ELSIZE, TIFFField.TIFF_FLOAT, 3,
                new float[]{(float) getElSize().x, (float) getElSize().y,
                        (float) elSize.z});
        tiffProcLog[2] = new TIFFField(TAG_POS, TIFFField.TIFF_SSHORT, 3,
                new short[]{(short) getPos().x, (short) getPos().y, (short) getPos().z});
        tiffProcLog[3] = new TIFFField(TAG_OFFSET, TIFFField.TIFF_SSHORT, 3,
                new short[]{(short) getOffset().x, (short) getOffset().y,
                        (short) getOffset().z});
        tiffProcLog[4] = new TIFFField(TAG_SHORTSCALE, TIFFField.TIFF_FLOAT, 1,
                new float[]{getShortScaleFactor()});
        tiffProcLog[5] = new TIFFField(TAG_ISSIGNED, TIFFField.TIFF_BYTE, 1,
                new byte[]{(byte) (isSigned ? 5 : 0)});
        tparam.setExtraFields(tiffProcLog);
    }

    public void readFromTIFF(final TIFFField[] allfields) {
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
                        D3int opos = new D3int();
                        opos.x = allfield.getAsInt(0);
                        opos.y = allfield.getAsInt(1);
                        opos.z = allfield.getAsInt(2);
                        setPos(opos);
                        System.out.println("Header-POS :" + opos);

                    } else {
                        System.out.println("Invalid POS...");
                    }
                    break;
                case TAG_OFFSET:
                    if ((allfield.getType() == TIFFField.TIFF_SSHORT)
                            && (allfield.getCount() == 3)) {
                        D3int offset = new D3int();
                        offset.x = allfield.getAsInt(0);
                        offset.y = allfield.getAsInt(1);
                        offset.z = allfield.getAsInt(2);
                        setOffset(offset);
                        System.out.println("Header-OFFSET :" + offset);

                    } else {
                        System.out.println("Invalid OFFSET...");
                    }
                    break;
                case TAG_ELSIZE:
                    if ((allfield.getType() == TIFFField.TIFF_FLOAT)
                            && (allfield.getCount() == 3)) {
                        D3float elSize = new D3float();
                        elSize.x = allfield.getAsFloat(0);
                        elSize.y = allfield.getAsFloat(1);
                        elSize.z = allfield.getAsFloat(2);
                        setElSize(elSize);
                        System.out.println("Header-ELSIZE :" + elSize);

                    } else {
                        System.out.println("Invalid ELSIZE...");
                    }
                    break;
                case TAG_SHORTSCALE:
                    if ((allfield.getType() == TIFFField.TIFF_FLOAT)
                            && (allfield.getCount() > 0)) {

                        float ShortScaleFactor = allfield.getAsFloat(0);
                        if (Math.abs(ShortScaleFactor) < 1e-6) {
                            System.out.println("Invalid SSF (too small)"
                                    + ShortScaleFactor + ", Reseting to 1.0");
                            ShortScaleFactor = 1.0f;

                        }
                        setShortScaleFactor(ShortScaleFactor);
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
                        System.out.println("Signed Values :" + isSigned);

                    } else {
                        System.out.println("Invalid isSigned Field...");
                    }
                    break;
                default:
                    // Ignore unknown header tags (but dont make a big deal of it)
                    skippedTag++;
                    break;
            }

        }
        System.out.println("Scanning tiff-header..." + skippedTag
                + " tags skipped of " + totalTag);
    }

    @Override
    public String appendProcLog(String inData) {
        procLog += "\n" + inData;
        return procLog;
    }

    @Override
    public D3int getDim() {
        return indim;
    }

    @Override
    public void setDim(D3int inData) {
        indim = inData;
    }

    @Override
    public D3float getElSize() {
        return elSize;
    }

    @Override
    public void setElSize(D3float inData) {
        elSize = inData;
    }

    @Override
    public D3int getOffset() {
        return inoffset;
    }

    @Override
    public void setOffset(D3int inData) {
        inoffset = inData;
    }

    @Override
    public D3int getPos() {
        return inpos;
    }

    @Override
    public void setPos(D3int inData) {
        inpos = inData;
    }

    @Override
    public String getProcLog() {
        return procLog;
    }

    @Override
    public float getShortScaleFactor() {
        return ssf;
    }

    @Override
    public void setShortScaleFactor(float ssf) {
        this.ssf = ssf;
    }

}
