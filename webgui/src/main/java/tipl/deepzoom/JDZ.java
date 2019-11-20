package tipl.deepzoom;

/**
 *  Deep Zoom Converter
 *
 *  A Java application for converting large image files into the tiled images
 *  required by the Microsoft Deep Zoom format and is suitable for use with
 *  Daniel Gasienica's OpenZoom library presently at http://openzoom.org
 *
 *  This source code is provided in the form of a Java application, but is
 *  designed to be adapted for use in a library.
 *
 *  To simplify compliation, deployment and packaging I have provided the
 *  original code in a single source file.
 *
 *  Version: MPL 1.1/GPL 3/LGPL 3
 *
 *  The contents of this file are subject to the Mozilla Public License Version
 *  1.1 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http: *www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  The Original Code is this Java package called DeepZoomConverter
 *
 *  The Initial Developer of the Original Code is Glenn Lawrence.
 *  Portions created by the Initial Developer are Copyright (c) 2007-2009
 *  the Initial Developer. All Rights Reserved.
 *
 *  Contributor(s):
 *    Glenn Lawrence  <glenn.c.lawrence@gmail.com>
 *    Kevin Mader <kevin.mader@gmail.com>
 *
 *  Alternatively, the contents of this file may be used under the terms of
 *  either the GNU General Public License Version 3 or later (the "GPL"), or
 *  the GNU Lesser General Public License Version 3 or later (the "LGPL"),
 *  in which case the provisions of the GPL or the LGPL are applicable instead
 *  of those above. If you wish to allow use of your version of this file only
 *  under the terms of either the GPL or the LGPL, and not to allow others to
 *  use your version of this file under the terms of the MPL, indicate your
 *  decision by deleting the provisions above and replace them with the notice
 *  and other provisions required by the GPL or the LGPL. If you do not delete
 *  the provisions above, a recipient may use your version of this file under
 *  the terms of any one of the MPL, the GPL or the LGPL.
 */

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codecimpl.util.*;
import tipl.formats.TImgRO;
import tipl.tools.BaseTIPLPluginIn;
import tipl.util.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.DataBufferFloat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import javax.imageio.ImageIO;
import java.util.Vector;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

/**
 * Modified from original version to integrate with TIPL
 *
 * @author Glenn Lawrence
 * @author Kevin Mader
 */
public class JDZ extends BaseTIPLPluginIn {

    static final String xmlHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    static final String schemaName = "http://schemas.microsoft.com/deepzoom/2009";
    protected String nameWithoutExtension = "dzimage";
    protected TileableImage image;
    boolean deleteExisting = true;
    String tileFormat = "jpg";
    // The following can be overriden/set by the indicated command line arguments
    int tileSize = 256;            // -tilesize
    int tileOverlap = 1;           // -overlap
    TypedPath outputDir = TIPLStorageManager.openPath(".");         // -outputdir or -o
    boolean verboseMode = false;   // -verbose or -v

    public static void main(String[] args) {
        ITIPLPluginIn jdzPlug = filebasedVersion();
        jdzPlug.setParameter(TIPLGlobal.activeParser(args), "");
        jdzPlug.LoadImages(new TImgRO[]{});
        jdzPlug.execute();
    }

    public static ITIPLPluginIn filebasedVersion() {
        // A special file based version
        return new JDZ() {
            TypedPath inputFile = TIPLStorageManager.openPath(".");  // must follow all other args

            @Override
            public ArgumentParser setParameter(ArgumentParser p, final String prefix) {
                inputFile = p.getOptionPath(prefix + "input", inputFile,
                        "The input image to mosaic");
                String fileName = inputFile.getFile().getName();
                nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
                return super.setParameter(p, prefix);
            }

            @Override
            public void LoadImages(final TImgRO[] inImages) {
                try {
                    TileableImage image = loadImage(inputFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("The input-file could not be " +
                            "loaded:" + inputFile + ": " + e);
                }
            }

            /**
             * Loads image from file
             * @param imageName the file containing the image
             */
            private TileableImage loadImage(TypedPath imageName) throws IOException {
                try {
                    return new TBIImgImpl(ImageIO.read(imageName.getFile()));
                } catch (Exception e) {
                    throw new IOException("Cannot read image file: " + imageName);
                }
            }

        };

    }

    ;

    private static int ceilLevelDivide(final double origSize, final int level) {
        if (level <= 0) return (int) Math.ceil(origSize);
        else return ceilLevelDivide((int) Math.ceil(origSize * 1.0 / 2), level - 1);
    }

    ;

    /**
     * Delete a file
     *
     * @param file the path of the directory to be deleted
     */
    private static void deleteFile(File file) throws IOException {
        if (!file.delete())
            throw new IOException("Failed to delete file: " + file);
    }



    /**
     * Creates a directory
     *
     * @param parent the parent directory for the new directory
     * @param name   the new directory name
     */
    private static File createDir(File parent, String name) throws IOException {
        assert (parent.isDirectory());
        File result = new File(parent + File.separator + name);
        if (!result.mkdir())
            throw new IOException("Unable to create directory: " + result);
        return result;
    }

    /**
     * Saves strings as text to the given file
     *
     * @param lines the image to be saved
     * @param file  the file to which it is saved
     */
    private static void saveText(Vector lines, TypedPath file) throws IOException {
            PrintStream ps = new PrintStream(file.getFileObject().getOutputStream(true));
            for (int i = 0; i < lines.size(); i++)
                ps.println((String) lines.elementAt(i));

    }

    @Override
    public ArgumentParser setParameter(ArgumentParser p, String prefix) {
        nameWithoutExtension = p.getOptionString("name",
                nameWithoutExtension, "Base filename for output image");

        tileFormat = p.getOptionString("tileformat", tileFormat, "Format for the tile images");
        outputDir = p.getOptionPath(prefix + "outputdir", outputDir, "The output directory");
        tileSize = p.getOptionInt(prefix + "tilesize", tileSize, "Size of the tiles");
        tileOverlap = p.getOptionInt(prefix + "tileoverlap", tileOverlap, "Overlap between tiles");
        verboseMode = p.getOptionBoolean(prefix + "verbose", verboseMode, "Verbose");
        deleteExisting = p.getOptionBoolean(prefix + "delexisting", deleteExisting,
                "Delete existing folders");
        if (!outputDir.getFile().exists())
            throw new IllegalArgumentException("Output directory does not exist: "
                    + outputDir.getPath());
        if (!outputDir.getFile().isDirectory())
            throw new IllegalArgumentException("Output directory is not a directory: "
                    + outputDir.getPath());
        return p;
    }

    @Override
    public boolean execute() {
        if (TIPLGlobal.getDebug()) {
            System.out.printf("tileSize=%d ", tileSize);
            System.out.printf("tileOverlap=%d ", tileOverlap);
            System.out.printf("outputDir=%s\n", outputDir.getPath());
        }

        try {
            processImageFile(outputDir);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(this + ": Problem writing files to " + outputDir + ": " + e);
            return false;
        }
        return true;
    }

    @Override
    public String getPluginName() {
        return "JavaDeepZoom";
    }

    @Override
    public void LoadImages(final TImgRO[] inImages) {
        TileableImage image = null;
    }

    /**
     * Process the given image file, producing its Deep Zoom output files in a subdirectory of the
     * given output directory.
     *
     * @param outputDir the output directory
     */
    private void processImageFile(TypedPath outputDir) throws IOException {
        if (verboseMode)
            System.out.printf("Processing image file: %s\n", image);

        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        double maxDim = Math.max(originalWidth, originalHeight);

        int nLevels = (int) Math.ceil(Math.log(maxDim) / Math.log(2));

        if (TIPLGlobal.getDebug())
            System.out.printf("nLevels=%d\n", nLevels);

        // Delete any existing output files and folders for this image
        String pathWithoutExtension = outputDir + File.separator + nameWithoutExtension;
        TypedPath descriptor = TIPLStorageManager.openPath(pathWithoutExtension + ".xml");
        if (descriptor.exists()) {
            if (deleteExisting)
                descriptor.delete();
            else
                throw new IOException("File already exists in output dir: " + descriptor);
        }

        TypedPath imgDir = TIPLStorageManager.openPath(pathWithoutExtension);
        if (imgDir.exists()) {
            if (deleteExisting) {
                if (TIPLGlobal.getDebug())
                    System.out.printf("Deleting directory: %s\n", imgDir);
                imgDir.recursiveDelete();
            } else
                throw new IOException("Image directory already exists in output dir: " + imgDir);
        }

        imgDir = outputDir.append(nameWithoutExtension);

        ExecutorService curES = TIPLGlobal.getIOExecutor();

        for (int level = nLevels; level >= 0; level--) {
            curES.submit(new createLevel(imgDir, nLevels, level,
                    tileSize,
                    tileOverlap,
                    image,
                    tileFormat));

        }

        saveImageDescriptor(originalWidth, originalHeight, descriptor);
    }

    /**
     * Write image descriptor XML file
     *
     * @param width  image width
     * @param height image height
     * @param file   the file to which it is saved
     */
    private void saveImageDescriptor(int width, int height, TypedPath file) throws IOException {
        Vector lines = new Vector();
        lines.add(xmlHeader);
        lines.add("<Image TileSize=\"" + tileSize + "\" Overlap=\"" + tileOverlap +
                "\" Format=\"" + tileFormat + "\" ServerFormat=\"Default\" xmnls=\"" +
                schemaName + "\">");
        lines.add("<Size Width=\"" + width + "\" Height=\"" + height + "\" />");
        lines.add("</Image>");
        saveText(lines, file);
    }

    /**
     * Class for any image which can be tiled easily since it can be resized and written into a
     * bufferimage
     */
    static public interface TileableImage {
        public int getWidth();

        public int getHeight();

        public int getType();

        public boolean writeTileInBufferedImage(BufferedImage img, int w, int h, int x, int y);

        public TileableImage rescale(double scaleF);
    }


    @TIPLPluginManager.PluginInfo(pluginType = "JDZ",
            desc = "Java DeepZoom Tool",
            sliceBased = false)
    final public static class dzFactory implements TIPLPluginManager.TIPLPluginFactory {
        @Override
        public ITIPLPlugin get() {
            return new JDZ();
        }
    }


    @TIPLPluginManager.PluginInfo(pluginType = "JDZ-File",
            desc = "Java DeepZoom Tool for Single Images",
            sliceBased = true)
    final public static class dzFileFactory implements TIPLPluginManager.TIPLPluginFactory {
        @Override
        public ITIPLPlugin get() {
            return filebasedVersion();
        }
    }


    private static class createLevel implements Runnable {
        final TypedPath imgDir;
        final String tileFormat;
        final int nLevels, level, tileSize, tileOverlap, nRows, nCols;
        final double cWidth, cHeight;
        final TileableImage iimage;

        public createLevel(final TypedPath imgDir, final int nLevels, final int level,
                           final int tileSize,
                           final int tileOverlap, final TileableImage image,
                           final String tileFormat) {
            this.imgDir = imgDir;
            this.nLevels = nLevels;
            this.level = level;
            this.tileSize = tileSize;
            this.tileOverlap = tileOverlap;

            this.iimage = image;
            this.tileFormat = tileFormat;

            cWidth = ceilLevelDivide(image.getWidth(), nLevels - level);
            cHeight = ceilLevelDivide(image.getHeight(), nLevels - level);
            nCols = (int) Math.ceil(cWidth / tileSize);
            nRows = (int) Math.ceil(cHeight / tileSize);

        }

        public void run() {
            if (TIPLGlobal.getDebug())
                System.out.printf("level=%d w/h=%f/%f cols/rows=%d/%d\n",
                        level, cWidth, cHeight, nCols, nRows);
            // resize image
            TileableImage image = iimage;
            for (int i = 0; i < (nLevels - level); i++) image = image.rescale(0.5);
            try {
                TypedPath dir = imgDir.appendDir(Integer.toString(level),true);

                for (int col = 0; col < nCols; col++) {
                    for (int row = 0; row < nRows; row++) {
                        BufferedImage tile = getTile(image, row, col);
                        saveImage(tile, dir.append(File.separator + col + '_' + row));
                    }
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Problem writing files in " + imgDir
                        + "-" + level);
            }
        }

        /**
         * Gets an image containing the tile at the given row and column for the given image.
         *
         * @param img - the input image from whihc the tile is taken
         * @param row - the tile's row (i.e. y) index
         * @param col - the tile's column (i.e. x) index
         */
        private BufferedImage getTile(TileableImage img, int row, int col) {
            int x = col * tileSize - (col == 0 ? 0 : tileOverlap);
            int y = row * tileSize - (row == 0 ? 0 : tileOverlap);
            int w = tileSize + (col == 0 ? 1 : 2) * tileOverlap;
            int h = tileSize + (row == 0 ? 1 : 2) * tileOverlap;

            if (x + w > img.getWidth())
                w = img.getWidth() - x;
            if (y + h > img.getHeight())
                h = img.getHeight() - y;

            if (TIPLGlobal.getDebug())
                System.out.printf("getTile: row=%d, col=%d, x=%d, y=%d, w=%d, h=%d\n",
                        row, col, x, y, w, h);

            assert (w > 0);
            assert (h > 0);

            BufferedImage result = new BufferedImage(w, h, img.getType());
            img.writeTileInBufferedImage(result, w, h, x, y);

            return result;
        }

        /**
         * Saves image to the given file
         *
         * @param img  the image to be saved
         * @param path the path of the file to which it is saved (less the extension)
         */
        private void saveImage(BufferedImage img, TypedPath path) throws IOException {
            TypedPath outputFile = path.append("." + tileFormat);
            try {
                ImageIO.write(img, tileFormat, outputFile.getFileObject().getOutputStream(true));
            } catch (IOException e) {
                throw new IOException("Unable to save image file: " + outputFile);
            }
        }
    }


    /**
     * The working implementation for a simple bufferedimage object
     */
    static private final class TBIImgImpl extends TileableBImage {
        final BufferedImage bi;

        public TBIImgImpl(BufferedImage bi) {
            this.bi = bi;
        }

        @Override
        public BufferedImage getAsBI() {
            return bi;
        }
    }


    static abstract public class TileableBImage implements TileableImage {
        abstract public BufferedImage getAsBI();

        @Override
        public boolean writeTileInBufferedImage(BufferedImage img, int w, int h, int x, int y) {
            Graphics2D g = img.createGraphics();
            return g.drawImage(getAsBI(), 0, 0, w, h, x, y, x + w, y + h, null);
        }

        public TileableImage rescale(double scaleF) {
            // Scale down image for next level
            double width = Math.ceil(getWidth() * scaleF);
            double height = Math.ceil(getHeight() * scaleF);
            if (width > 10 && height > 10) {
                // resize in stages to improve quality
                return resize(width * 1.66, height * 1.66).
                        resize(width * 1.33, height * 1.33);
            }
            return resize(width, height);
        }

        /**
         * Returns resized image NB - useful reference on high quality image resizing can be found
         * here: http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
         *
         * @param width  the required width
         * @param height the frequired height
         */
        public TileableBImage resize(double width, double height) {
            int w = (int) width;
            int h = (int) height;
            final BufferedImage result = new BufferedImage(w, h, getType());
            Graphics2D g = result.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(getAsBI(), 0, 0, w, h, 0, 0, getWidth(), getHeight(), null);
            return new TBIImgImpl(result);
        }

        @Override
        public int getWidth() {
            return getAsBI().getWidth();
        }

        @Override
        public int getHeight() {
            return getAsBI().getHeight();
        }

        @Override
        public int getType() {
            return getAsBI().getType();
        }
    }


    /**
     * A class that creates a mosaic from a TImgRO stack
     */
    static public class MosaicTImg implements TileableImage {
        final TImgRO iimg;
        final int tilesX, tilesY;
        final double scale;

        /**
         * Creates a tileable image
         *
         * @param inImage
         * @param scale   the factor to use for making the image (used for making different levels)
         */
        public MosaicTImg(final TImgRO inImage, final double scale) {
            this(inImage, (int) Math.ceil(Math.sqrt(inImage.getDim().z)),
                    (int) Math.ceil(inImage.getDim().z / Math.ceil(Math.sqrt(inImage.getDim().z))
                    ), scale);
        }

        protected MosaicTImg(final TImgRO inImage, final int tilesX, final int tilesY,
                             final double scale) {
            iimg = inImage;
            this.tilesX = tilesX;
            this.tilesY = tilesY;
            this.scale = scale;
        }
        // get the data and put it into a bufferedimage
        protected static BufferedImage sliceToBImg(final TImgRO img, final int slice) {
            final int cType = BufferedImage.TYPE_USHORT_GRAY;
            int maxVal = 255;
            if (cType == BufferedImage.TYPE_BYTE_GRAY)
                maxVal = 127;
            if (cType == BufferedImage.TYPE_USHORT_GRAY)
                maxVal = 65536;
            if (cType == BufferedImage.TYPE_BYTE_BINARY)
                maxVal = 255;

            switch (cType) {
                case BufferedImage.TYPE_CUSTOM:  // Float is a special case
                    return sliceDataToBImg((float[]) img
                            .getPolyImage(slice,
                                    TImgTools.IMAGETYPE_FLOAT),img.getDim().x,img.getDim().y);

                case BufferedImage.TYPE_USHORT_GRAY:
                    return sliceDataToBImg((int[]) img.getPolyImage(slice,
                            TImgTools.IMAGETYPE_INT),img.getDim().x,img.getDim().y);
                default:
                    throw new IllegalArgumentException("The type:" + cType + " is not supported " +
                            "for " +
                            "conversion");

            }
        }
        public static BufferedImage sliceDataToBImg(final int[] sliceData,final int xDim,
                                                         final int yDim) {
            final int cType = BufferedImage.TYPE_USHORT_GRAY;
            BufferedImage rawSliceData = new BufferedImage(xDim, yDim,
                    cType);
            WritableRaster rs = (WritableRaster) rawSliceData.getRaster();
            rs.setPixels(0, 0, xDim, yDim,sliceData);
            return rawSliceData;
        }

        public static BufferedImage byteSliceDataToBImg(final int[] sliceData,final int xDim,
                                                    final int yDim) {
            final int cType = BufferedImage.TYPE_BYTE_GRAY;
            BufferedImage rawSliceData = new BufferedImage(xDim, yDim,
                    cType);
            WritableRaster rs = (WritableRaster) rawSliceData.getRaster();
            rs.setPixels(0, 0, xDim, yDim,sliceData);
            return rawSliceData;
        }
        public static BufferedImage sliceDataToBImg(final float[] sliceData,final int xDim,
                                                         final int yDim) {
                    final int nbBands = 1;
                    final int[] rgbOffset = new int[nbBands];
                    final SampleModel sampleModel = RasterFactory
                            .createPixelInterleavedSampleModel(
                                    DataBuffer.TYPE_FLOAT, xDim, yDim, nbBands,
                                    nbBands * xDim, rgbOffset);

                    final ColorModel colorModel = ImageCodec
                            .createComponentColorModel(sampleModel);

                    final com.sun.media.jai.codecimpl.util.DataBufferFloat dataBuffer = new com
                            .sun.media.jai.codecimpl.util.DataBufferFloat(sliceData,
                            xDim * yDim);

                    final WritableRaster raster = RasterFactory
                            .createWritableRaster(sampleModel, dataBuffer,
                                    new Point(0, 0));

                    return new BufferedImage(colorModel, raster, false, null);
            }

        // resize the slice into a new bufferedimage
        protected static BufferedImage sliceToPanel(final TImgRO img, final int slice,
                                                    final double scale, final int panelWidth,
                                                    final int panelHeight) {
            BufferedImage rawSliceData = sliceToBImg(img, slice);

            BufferedImage outBImg = new BufferedImage(panelWidth,
                    panelHeight,
                    BufferedImage.TYPE_USHORT_GRAY);
            Graphics2D g = outBImg.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            g.drawImage(rawSliceData, 0, 0, panelWidth, panelHeight, 0, 0, img.getDim().x,
                    img.getDim().y,
                    null);

            return outBImg;

        }

        protected int getSliceWidth() {
            return (int) Math.floor(iimg.getDim().x * scale);
        }

        protected int getSliceHeight() {
            return (int) Math.floor(iimg.getDim().y * scale);
        }

        @Override
        public int getWidth() {
            return tilesX * getSliceWidth();
        }

        @Override
        public int getHeight() {
            return tilesY * getSliceHeight();
        }

        @Override
        public int getType() {
            return iimg.getImageType();
        }

        protected static int getSliceInd(int xInd, int yInd,int tilesX) {
            return tilesX*yInd+xInd;
        }

        @Override
        public boolean writeTileInBufferedImage(BufferedImage img, int w, int h, int x, int y) {
            //TODO implement write tile

            Graphics2D g = img.createGraphics();

            int curX = x;
            while (curX<(x+h)) {
                int xInd = (int) Math.ceil( ((double)curX)/getSliceWidth());
                curX = xInd*getSliceWidth();
                int curY = y;
                while (curY<(y+h)) {
                    int yInd = (int) Math.ceil(((double)curY)/getSliceHeight());
                    curY = yInd*getSliceHeight();
                    int osX = x-curX; // output starting position in x
                    int osY = y-curY; // output starting position in y

                    int outWidth = Math.min(w,getSliceWidth());
                    int outHeight = Math.min(h,getSliceHeight());

                    g.drawImage(sliceToPanel(iimg, getSliceInd(xInd,yInd,tilesX), scale,
                                    getSliceWidth(),
                                    getSliceHeight()),
                            osX, osY, osX+outWidth, osY+outHeight, // output image coordinates
                            0, 0, outWidth,  outHeight, // input image coordinates
                            null);
                }
            }


            //throw new IllegalArgumentException("WriteTile has not yet been implemented");
             return true;
        }

        @Override
        public TileableImage rescale(double scaleF) {
            return new MosaicTImg(iimg, tilesX, tilesY, scale * scaleF);
        }
    }
}