/**
 *
 */
package tipl.formats;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.media.jai.PlanarImage;

import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;
import tipl.util.TypedPath;
import tipl.util.TypedPath.PathFilter;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;

/**
 * Tiff folder is the code used to read in a file-system directory (as compared with a tiffdirectory) of tiff files (as normally produced with the reconstruction pipeline as of 2012/13)
 * It handles listing and ordering the files and reading them in, in a parallel compatible manner
 * @author maderk
 *
 */
public class TiffFolder extends DirectoryReader {
    private static class TiffSliceFactory implements TSliceFactory {
        @Override
        public TSliceReader ReadFile(final TypedPath infile) throws IOException {
            return new TIFSliceReader(infile);
        }
    }

    public static double[] ReadByteStreamAsDouble(final byte[] buffer)  throws IOException {
        TIFSliceReader outReader = new TIFSliceReader(buffer);
        return (double[]) outReader.polyReadImage(TImgTools.IMAGETYPE_DOUBLE);
    }

    public static class TIFSliceReader extends SliceReader implements TSliceReader {

        public TIFSliceReader(final TypedPath infile) throws IOException {
            final byte[] bufferArr = infile.getFileObject().getData();
            SetupFromBuffer(bufferArr,IdentifyDecoderNames(bufferArr)[0]);
        }
        public TIFSliceReader(final byte[] buffer,String decName)  throws IOException {
            SetupFromBuffer(buffer,decName);
        }
        public TIFSliceReader(final byte[] buffer)  throws IOException {
            SetupFromBuffer(buffer,IdentifyDecoderNames(buffer)[0]);
        }
        public TIFSliceReader(final RenderedImage im) throws IOException {
            SetupFromRenderImage(im);
        }

        protected int[] gi;
        protected float[] gf;
        protected byte[][] grgb;

        @Override
        public Object polyReadImage(final int asType) throws IOException {

            switch (imageType) {
                case TImgTools.IMAGETYPE_CHAR: // Char use the interface for short with a different max val
                case TImgTools.IMAGETYPE_INT: // Int
                case TImgTools.IMAGETYPE_BOOL: // binary also uses the same reader
                    return TImgTools.convertArrayType(gi,TImgTools.IMAGETYPE_INT, asType, useSignedConversion, 1, maxVal);
                case TImgTools.IMAGETYPE_FLOAT: // Float
                    return TImgTools.convertArrayType(gf,TImgTools.IMAGETYPE_FLOAT, asType, useSignedConversion, 1);
                case TImgTools.IMAGETYPE_RGB: // color image
                    return TImgTools.convertArrayType(grgb,TImgTools.IMAGETYPE_RGB,asType,useSignedConversion,1);
                default:
                    throw new IOException("Input file format is not known!!!!");
            }
        }
        /**
         * should the conversions be signed
         */
        protected boolean useSignedConversion=false;
        protected boolean readAsByte=false;
        public static String[] IdentifyDecoderNames(final byte[] buffer) throws IOException {
            final SeekableStream stream = new ByteArraySeekableStream(buffer);
            return ImageCodec.getDecoderNames(stream);
        }

        private void SetupFromBuffer(final byte[] buffer,String decName)  throws IOException {
            final SeekableStream stream = new ByteArraySeekableStream(buffer);
            final ImageDecoder dec = ImageCodec.createImageDecoder(decName,
                    stream, null);
            SetupFromRenderImage(dec.decodeAsRenderedImage());
        }

        private void SetupFromRenderImage(final RenderedImage im)
                throws IOException {
            switch (im.getColorModel().getPixelSize()) {
                case 1: // boolean
                    imageType = TImgTools.IMAGETYPE_BOOL;
                    maxVal = 1;
                    break;
                case 8: // Char
                    imageType = TImgTools.IMAGETYPE_CHAR;
                    maxVal = 255;
                    break;
                case 16: // Integer
                    imageType = TImgTools.IMAGETYPE_INT;
                    maxVal = 65536;
                    break;

                case 24: // for color images
                    imageType=TImgTools.IMAGETYPE_RGB;
                    maxVal=255;
                    break;
                case 32: // Float
                    imageType = TImgTools.IMAGETYPE_FLOAT;
                    maxVal = 65536;
                    break;
                default:
                    throw new IOException("What the fuck is going on:"
                            + im.getColorModel() + ", "
                            + im.getColorModel().getPixelSize());
            }
            final BufferedImage bim = PlanarImage.wrapRenderedImage(im)
                    .getAsBufferedImage();

            Raster activeRaster = bim.getData();
            dim = new D3int(activeRaster.getWidth(), activeRaster.getHeight(),
                    1);

            // number of pixels in a slice
            sliceSize = activeRaster.getWidth() * activeRaster.getHeight();

            switch (imageType) {
                case TImgTools.IMAGETYPE_CHAR: // Char use the interface for short with a different max val
                case TImgTools.IMAGETYPE_INT: // Int
                case TImgTools.IMAGETYPE_BOOL: // binary also uses the same reader

                    gi =  new int[sliceSize];

                    gi = activeRaster.getPixels(0, 0, activeRaster.getWidth(),
                            activeRaster.getHeight(), gi);

                    break;

                case TImgTools.IMAGETYPE_FLOAT: // Float
                    gf = new float[sliceSize];
                    gf = activeRaster.getPixels(0, 0, activeRaster.getWidth(),
                            activeRaster.getHeight(), gf);
                    break;
                case TImgTools.IMAGETYPE_RGB:
                    grgb = new byte[sliceSize][3];
                    // how to handle a 24 bit color image, just take the red channel
                    byte[] inByteData=( (DataBufferByte) activeRaster.getDataBuffer()).getData();

                    for(int i=0;i<sliceSize;i++) {
                        grgb[i][0]=(byte) (inByteData[3*i] & 0xFF);
                        grgb[i][1]=(byte) (inByteData[3*i+1] & 0xFF);
                        grgb[i][2]=(byte) (inByteData[3*i+2] & 0xFF);
                    }

                    break;

                default:
                    throw new IOException("Input file format is not known!!!!"+imageType+" because:"+TImgTools.getImageTypeName(imageType));
            }

        }

    }

    final static String version = "03-12-2014";

    @DirectoryReader.DReader(name = "tiff")
    final public static class tifReaderFactory extends DRFactory {
        @Override
        public DirectoryReader get(final TypedPath path) {
            try {
                return new TiffFolder(path,tifFilter,"tiff");
            } catch (final Exception e) {
                System.out.println("Error converting or reading slice");
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public TSliceReader getSliceReader(TypedPath slice) {
            try {
                return new TIFSliceReader(slice);
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalArgumentException(slice+" could not be read");
            }
        }

        final static public PathFilter tifFilter = new PathFilter.ExtBased("tif");

        @Override
        public PathFilter getFilter() {
            return tifFilter;
        }
    };

    @DirectoryReader.DReader(name = "jpeg")
    final public static class jpgReaderFactory extends DRFactory {
        @Override
        public DirectoryReader get(final TypedPath path) {
            try {
                return new TiffFolder(path,jpegFilter,"jpeg");
            } catch (final Exception e) {
                System.out.println("Error converting or reading slice");
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public TSliceReader getSliceReader(TypedPath slice) {

            try {
                return new TIFSliceReader(slice);
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalArgumentException(slice+" could not be read");
            }
        }

        final static public PathFilter jpegFilter = new PathFilter.ExtBased("jpg","jpeg");

        @Override
        public PathFilter getFilter() {
            return jpegFilter;
        }

    }
    /**
     * operating as a tiff or jpeg folder
     */
    protected final String pluginMode;
    public TiffFolder(final TypedPath path,final PathFilter ffilter,final String mode) throws IOException {
        super(path, ffilter, new TiffSliceFactory());
        pluginMode=mode;

    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.DirectoryReader#ParseFirstHeader()
     */
    @Override
    public void ParseFirstHeader() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see tipl.formats.DirectoryReader#readerName()
     */
    @Override
    public String readerName() {
        // TODO Auto-generated method stub
        return pluginMode+"-Folder-Reader " + version+":"+this;
    }

    @Override
    public void SetupReader(TypedPath inPath) {
        //TODO add some setup code here if it is needed
    }

}
