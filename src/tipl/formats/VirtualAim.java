package tipl.formats;

import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.HistogramWindow;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.media.jai.PlanarImage;

import tipl.ij.TImgToImagePlus;
import tipl.util.ArgumentParser;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.ITIPLStorage;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codec.TIFFField;
import com.sun.media.jai.codecimpl.util.DataBufferFloat;
import com.sun.media.jai.codecimpl.util.RasterFactory;

/**
 * The basis for the other image processing tools this class serves as a
 * framework for storing, reading, and writing 3D image data. It is built using
 * the memory model from OpenVMS and the framework of ImageJ so it is in theory
 * 100% compatible with both approaches to image processing.
 * 
 * <li>Oct 10, 2011 - Fixed support for reading in float arrays from float named
 * images <li>Dec 18, 2011 - Recoded to work on normal systems (windows, osx,
 * linux) using tiff stacks <li>Jan 25, 2012 - Restructure class as ImageStack
 * from ImageJ and added preview, ImagePlus and image processing capabilities
 * <li>Mar 13, 2013 - Added ability to load data using the DirectoryReader
 * classes <li>Mar 14, 2013 - Changed the way local loading works
 */
public class VirtualAim implements TImg, TImgRO.TImgOld, TImgRO.FullReadable,
		TImgRO.CanExport {

	public static class sliceLoader extends Thread {
		int sslice, fslice, asType;
		volatile VirtualAim parent;

		public sliceLoader(final VirtualAim iparent, final int iasType,
				final int isslice, final int ifslice) {
			super("sliceLoader:<" + isslice + ", " + ifslice + ">");
			sslice = isslice;
			fslice = ifslice;
			asType = iasType;
			parent = iparent;
		}

		/**
		 * Distribute (using divideThreadWork) and process (using processWork)
		 * the work across the various threads
		 */
		@Override
		public void run() {
			System.out.println("SliceLoader Running:, <" + sslice + ", "
					+ fslice + ">");
			parent.getAimSubSection(asType, sslice, fslice);
		}
	}

	public static String StrRatio(final double a, final double b) {
		return String.format("%.2f", ((a) / (b)));
	}

	/**
	 * method to quickly wrap (if needed) a TImgRO objectinto a virtual aim and
	 * provide all the additional needed methods
	 * 
	 * @param inTImg
	 * @return VirtualAim version of input image
	 */
	@Deprecated
	public static VirtualAim TImgToVirtualAim(final TImgRO inTImg) {
		if (inTImg instanceof VirtualAim)
			return (VirtualAim) inTImg;
		else
			return new VirtualAim(inTImg);
	}

	/** ImageJ.ImageStack Dimensions */
	public int width, height;
	/**
	 * The aim type of the image (0=char, 1=short, 2=int, 3=float, 10=bool, -1
	 * same as input)
	 */
	private int imageType = -1;
	/**
	 * Is the image signed (should an offset be added / subtracted when the data
	 * is loaded to preserve the sign)
	 */
	private boolean isSigned = false;
	/** Has the tifstack header been read already */
	public boolean tifStackHeaderRead = false;
	/** Scratch directory for local-loading */
	public static String scratchDirectory = "/home/scratch/";
	/**
	 * Should the data be copied to scratch first and then read to avoid
	 * random-access (tif) to gpfs
	 */
	public static boolean scratchLoading = false;
	/** The filename of the actual scratch file used */
	private String scratchFilename = "";

	/** The dimensions of the aim */
	public D3int dim;
	/**
	 * The position of the bottom leftmost voxel in the image in real space,
	 * only needed for ROIs
	 */
	public D3int pos;
	/**
	 * The size of the border around the image which does not contain valid
	 * voxel data
	 */
	public D3int offset;
	/** The element size (in mm) of a voxel */
	public D3float elSize;
	public boolean isConnected;
	/**
	 * The factor to scale bool/short/int/char values by when converting to/from
	 * float (for distance maps is (1000.0/32767.0))
	 */
	public float ShortScaleFactor = 1.0f;

	Vector<Float[]> positionList;
	Vector<Float> valueList;
	/** Is a valid filled data set */
	public boolean ischGuet;
	protected boolean layertiff;
	/** Not implemented */
	public float triangles;
	public float getSliceCalls;
	String dataName;
	public String sampleName = "";

	/**
	 * Procedure Log, string containing past operations and information on the
	 * aim-file
	 */
	public String procLog = "";
	public static final String kVer = "131025_045";
	/** Path of original Aim file */
	public String aimPath = "";

	/** List of tiff file names when reading a directory of images */
	Vector<String> filenames;
	public boolean debugMode = false;
	/**
	 * Whether or not basic compression (compatible almost everywhere) should be
	 * used when writing data
	 */
	public boolean useCompression = false;
	/**
	 * Whether or not LZW (much better) compression should be used when writing
	 * data
	 */
	public boolean useHighCompression = true;

	// The entire dataset in an Aim-style array
	/** if loaded, stores the whole aim as a 1d array */
	@Deprecated
	public volatile boolean[] aimMask = new boolean[1];
	/** if fullAimLoaded, stores the whole aim as a 1d array */
	@Deprecated
	public volatile char[] aimByte = new char[1];
	/** if fullAimLoaded, stores the whole aim as a 1d array */
	@Deprecated
	public volatile int[] aimInt = new int[1];
	/** if fullAimLoaded, stores the whole aim as a 1d array */
	@Deprecated
	public volatile short[] aimShort = new short[1];
	/** if fullAimLoaded, stores the whole aim as a 1d array */
	@Deprecated
	public volatile float[] aimFloat = new float[1];
	/**
	 * true if all the slices has been read into one of the aim[] arrays, if
	 * false than aim is file-based
	 */
	public boolean fullAimLoaded = false;
	/** if fullAimLoaded indicates in which array the data is */
	public int fullAimLoadedAs = -1;

	private File[] imglist;

	private int cgLength = -1;
	HistogramWindow curHistWind = null;

	// Image Stack Implementation Code
	protected Object[] stack = null;

	protected boolean isLoaded = false;
	/** Show a preview everytime a file is written or opened */
	public boolean showPreview = true;
	/** Is the stack in sync with the aim data */
	public boolean inSync = false;
	/**
	 * Convert the loaded image to a stack Warning loading an image as a stack
	 * requires twice as much memory due to the different methods used in Aim
	 * and ImageJ data models, and the lack of operator overloading in java :-(
	 */
	protected TImgRO baseTImg;
	protected boolean useTImg = false;

	/**
	 * Preview is Disabled by default since it can crash things on windowless
	 * clients
	 **/
	public static boolean previewDisabled = true;

	public static void main(final ArgumentParser p) {
		System.out.println("VirtualAim Tool v" + VirtualAim.kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		VirtualAim inputAim = null;

		final String inputFile = p.getOptionString("convert", "",
				"Aim File to Convert");
		final String previewFile = p.getOptionString("preview", "",
				"Aim File to Preview");
		String outputFile = "";
		if (inputFile.length() > 0) {

			outputFile = p.getOptionString("output", "",
					"Output Aim File (.raw, .tif, directory/, etc)");
			VirtualAim.scratchLoading = p.getOptionBoolean("local",
					"Load image data from local filesystems");
			VirtualAim.scratchDirectory = p.getOptionString("localdir",
					"/home/scratch/", "Directory to save local data to");
			
			System.out.println("Loading " + inputFile + " ...");
			
			inputAim = new VirtualAim(inputFile);
			
			inputAim.useCompression = !p.getOptionBoolean("nocompression",
					"Skip compression steps (more compatible but larger )");
			inputAim.useHighCompression = inputAim.useCompression;
			System.out.println("Dim:" + inputAim.getDim());
			inputAim.pos = p.getOptionD3int("cpos", inputAim.pos,
					"Change Starting Position");
			inputAim.offset = p.getOptionD3int("coffset", inputAim.pos,
					"Change Offset");
			inputAim.elSize = p.getOptionD3float("celsize", inputAim.elSize,
					"Change Element Size");
			if (p.hasOption("?"))
				ShowInfo(p);
			if (outputFile.length() > 0) { // Write output File
				System.out.println("Writing " + outputFile + " ...");
				inputAim.WriteAim(outputFile);
			}
		} else if (previewFile.length() > 0) {
			VirtualAim previewAim = null;
			previewAim = new VirtualAim(previewFile);
			previewAim.show();
		} else
			ShowInfo(p);

	}

	public static void main(final String[] args) {
		main(TIPLGlobal.activeParser(args));
	}

	public static float max(final float a, final float b) {
		if (a > b)
			return a;
		else
			return b;
	}

	public static float min(final float a, final float b) {
		if (a > b)
			return b;
		else
			return a;
	}

	private static void ShowInfo(final ArgumentParser p) {
		System.out.println("VirtualAim v" + kVer);
		System.out.println(" Allows the editing and exporting of aim data v"
				+ kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		System.out.println(" Arguments::");
		System.out.println(" ");
		System.out.println(p.getHelp());
		System.exit(0);
	}

	/** Multithreaded operation (faster reads and writes according to helga) */
	/** does this plugin support multi-threaded processing */
	public boolean supportsThreading = true;

	protected boolean checkMaxCores = true;

	/** Job Start Time */
	protected long jStartTime;

	/** Creates a new, empty Aim stack. */
	public VirtualAim() {
		ischGuet = false;
	}

	/**
	 * Creates a new, aim from a 1d boolean array and dimensions, offset,
	 * position, and element size
	 */
	public VirtualAim(final boolean[] inData, final D3int idim,
			final D3int ioffset, final D3int ipos, final D3float ielSize) {
		fullAimLoaded = true;
		fullAimLoadedAs = 10;
		imageType = 10;
		aimMask = inData;
		ischGuet = true;
		GenerateHeader(idim, ioffset, ipos, ielSize);
	}

	/**
	 * Creates a new, aim from a 1d char array and dimensions, offset, position,
	 * and element size
	 */
	public VirtualAim(final char[] inData, final D3int idim,
			final D3int ioffset, final D3int ipos, final D3float ielSize) {
		fullAimLoaded = true;
		fullAimLoadedAs = 0;
		imageType = 0;
		aimByte = inData;
		ischGuet = true;
		GenerateHeader(idim, ioffset, ipos, ielSize);
	}

	public VirtualAim(final D3int idim, final D3int ioffset, final D3int ipos,
			final D3float ielSize) {
		fullAimLoaded = false;
		GenerateHeader(idim, ioffset, ipos, ielSize);
	}

	/**
	 * Creates a new, aim from a 1d float array and dimensions, offset,
	 * position, and element size
	 */
	public VirtualAim(final float[] inData, final D3int idim,
			final D3int ioffset, final D3int ipos, final D3float ielSize) {
		fullAimLoaded = true;
		fullAimLoadedAs = 3;
		imageType = 3;
		aimFloat = inData;
		ischGuet = true;
		GenerateHeader(idim, ioffset, ipos, ielSize);
	}


	/**
	 * Creates a new, aim from a 1d int array and dimensions, offset, position,
	 * and element size
	 */
	public VirtualAim(final int[] inData, final D3int idim,
			final D3int ioffset, final D3int ipos, final D3float ielSize) {
		fullAimLoaded = true;
		fullAimLoadedAs = 2;
		imageType = 2;
		aimInt = inData;
		ischGuet = true;
		GenerateHeader(idim, ioffset, ipos, ielSize);
	}

	/**
	 * Creates a new, aim from a 1d short array and dimensions, offset,
	 * position, and element size
	 */
	public VirtualAim(final short[] inData, final D3int idim,
			final D3int ioffset, final D3int ipos, final D3float ielSize) {
		fullAimLoaded = true;
		fullAimLoadedAs = 1;
		imageType = 1;
		aimShort = inData;
		ischGuet = true;
		GenerateHeader(idim, ioffset, ipos, ielSize);
	}

	/**
	 * Creates a new, Aim from the path (either tiff directory or layered tif
	 * file given in path
	 */
	public VirtualAim(final String path) {
		ReadAim(path, false);
	}

	public VirtualAim(final String path, final boolean onlyHeader) {
		ReadAim(path, onlyHeader);
	}

	public VirtualAim(final TImgRO inTImg) {
		WrapTImg(inTImg);
	}


	/**
	 * Creates a buffered image for the given slice which can be used to save as
	 * jpg
	 * 
	 * @param n
	 *            slice number
	 * @param cType
	 *            type of image to buffer
	 * @return a bufferedimage
	 */
	protected BufferedImage aimSlice(final int n, final int cType) {

		BufferedImage image = null;
		TIPLGlobal.runGC();
		int maxVal = 255;
		if (cType == BufferedImage.TYPE_BYTE_GRAY)
			maxVal = 127;
		if (cType == BufferedImage.TYPE_USHORT_GRAY)
			maxVal = 65536;
		if (cType == BufferedImage.TYPE_BYTE_BINARY)
			maxVal = 255;
		// int outPos=n*width*height;
		final int outPos = n * dim.x * dim.y;
		final int sliceLen = dim.x * dim.y;
		// System.out.println("Slice:"+n+", cop-"+String.format("%.2f",(outPos+0.0)/1e6)+" MVx, "+String.format("%.2f",(outPos*100.0)/tPos)+" %");
		int[] pixels;
		float[] fpixels;
		if (!fullAimLoaded) {
			if (cType == BufferedImage.TYPE_BYTE_GRAY)
				getByteAim();
			if (cType == BufferedImage.TYPE_USHORT_GRAY)
				getShortAim();
			if (cType == BufferedImage.TYPE_BYTE_BINARY)
				getBoolAim();
			if (cType == BufferedImage.TYPE_CUSTOM)
				getFloatAim();
		}
		if (fullAimLoaded) {
			if (cType == BufferedImage.TYPE_CUSTOM) { // Float is a special case
				fpixels = new float[sliceLen];

				switch (imageType) {
				case 0:
					for (int cIndex = outPos; cIndex < (outPos + sliceLen); cIndex++) {
						fpixels[cIndex - outPos] = (aimByte[cIndex] + (isSigned ? maxVal / 2
								: 0))
								* ShortScaleFactor;
					}
					break;
				case 1:
					for (int cIndex = outPos; cIndex < (outPos + sliceLen); cIndex++) {
						fpixels[cIndex - outPos] = (aimShort[cIndex] + (isSigned ? maxVal / 2
								: 0))
								* ShortScaleFactor;
					}
					break;
				case 2:
					for (int cIndex = outPos; cIndex < (outPos + sliceLen); cIndex++) {
						fpixels[cIndex - outPos] = (aimInt[cIndex] + (isSigned ? maxVal / 2
								: 0))
								* ShortScaleFactor;
					}
					break;
				case 3:
					for (int cIndex = outPos; cIndex < (outPos + sliceLen); cIndex++) {
						fpixels[cIndex - outPos] = aimFloat[cIndex];
					}
					break;
				case 10:
					for (int cIndex = outPos; cIndex < (outPos + sliceLen); cIndex++) {
						if (aimMask[cIndex])
							fpixels[cIndex - outPos] = 1.0f;
					}
					break;

				default:
					System.out.println("Not supported!");
					break;
				}

				final int nbBands = 1;
				final int[] rgbOffset = new int[nbBands];
				final SampleModel sampleModel = RasterFactory
						.createPixelInterleavedSampleModel(
								DataBuffer.TYPE_FLOAT, dim.x, dim.y, nbBands,
								nbBands * dim.x, rgbOffset);

				final ColorModel colorModel = ImageCodec
						.createComponentColorModel(sampleModel);

				final DataBufferFloat dataBuffer = new DataBufferFloat(fpixels,
						fpixels.length);
				fpixels = null;
				final WritableRaster raster = RasterFactory
						.createWritableRaster(sampleModel, dataBuffer,
								new Point(0, 0));

				image = new BufferedImage(colorModel, raster, false, null);

			} else {
				image = new BufferedImage(dim.x, dim.y, cType);
				final WritableRaster raster = (WritableRaster) image.getData();

				pixels = new int[sliceLen]; // is unsigned (deletes negative
											// values)

				switch (imageType) {
				case 0:
					for (int cIndex = outPos; cIndex < (outPos + sliceLen); cIndex++) {
						pixels[cIndex - outPos] = (aimByte[cIndex])
								+ (isSigned ? maxVal / 2 : 0);

					}
					break;
				case 1:
					for (int cIndex = outPos; cIndex < (outPos + sliceLen); cIndex++) {
						pixels[cIndex - outPos] = (aimShort[cIndex])
								+ (isSigned ? maxVal / 2 : 0);
						// sliceAvg+=pixels[cIndex-outPos];
					}
					break;
				case 2:
					for (int cIndex = outPos; cIndex < (outPos + sliceLen); cIndex++) {
						pixels[cIndex - outPos] = aimInt[cIndex]
								+ (isSigned ? maxVal / 2 : 0);
						// sliceAvg+=pixels[cIndex-outPos];
					}
					break;
				case 3:
					for (int cIndex = outPos; cIndex < (outPos + sliceLen); cIndex++) {
						pixels[cIndex - outPos] = (int) (aimFloat[cIndex] / ShortScaleFactor)
								+ (isSigned ? maxVal / 2 : 0);
						// sliceAvg+=pixels[cIndex-outPos];
					}
					break;
				case 10:
					for (int cIndex = outPos; cIndex < (outPos + sliceLen); cIndex++) {
						if (aimMask[cIndex])
							pixels[cIndex - outPos] = maxVal;
						// sliceAvg+=pixels[cIndex-outPos];
					}

					break;

				default:
					System.out.println("Not supported!");
					break;
				}
				raster.setPixels(0, 0, dim.x, dim.y, pixels);
				image.setData(raster);
				pixels = null;
			}
		} else {
			System.out.println("Error, Full Aim data has not yet been loaded!");
		}
		TIPLGlobal.runGC();

		return image;

	}

	@Override
	public String appendProcLog(final String logText) {
		// String newLog=getProcLog()+"\n"+logText;
		// TheHeader.setProcLog(newLog);
		// return newLog;
		procLog += "\n" + new Date() + "\t" + logText;
		return procLog;
	}

	/**
	 * Apply a function to the current aim to make a new image using the FImage
	 * and VoxelFunction framework
	 * 
	 * @param ivf
	 *            The FImage.VoxelFunction to apply
	 * @param useFloatInput
	 *            whether or not to use float input or integer for the voxel
	 *            function
	 **/
	public VirtualAim ApplyFunction(final int iimageType,
			final FImage.VoxelFunction ivf, final boolean useFloatInput) {
		final FImage outF = new FImage(this, iimageType, ivf, useFloatInput);
		return new VirtualAim(outF);
	}

	public void CacheFullImage() {
		getAimImage();
	}

	public boolean CheckSizes(final String otherAimFile) {
		final VirtualAim otherVA = new VirtualAim(otherAimFile, true);
		return CheckSizes(otherVA);
	}

	public boolean CheckSizes(final TImgRO otherVA) {
		return TImgTools.CheckSizes2(otherVA, this);
	}

	private Object decodeimage(final int slice, final RenderedImage im,
			int asType) throws IOException {

		int maxVal = 65536;
		int dataType = -1;
		switch (im.getColorModel().getPixelSize()) {
		case 1: // Boolean
			dataType = 10;
			dataName = "Bool";
			break;
		case 8: // Char
			dataType = 0;
			dataName = "Char";
			break;
		case 16: // Integer
			dataType = 2;
			dataName = "Int";
			break;
		case 32: // Float
			dataType = 3;
			dataName = "Float";
			break;
		default:
			System.out.println("What the fuck is going on:"
					+ im.getColorModel() + ", "
					+ im.getColorModel().getPixelSize());
			return null;
		}
		if (imageType == -1)
			imageType = dataType;
		if (asType == -1)
			asType = imageType;

		PlanarImage.wrapRenderedImage(im).getAsBufferedImage();
		final BufferedImage bim = PlanarImage.wrapRenderedImage(im)
				.getAsBufferedImage();
		TIPLGlobal.runGC();
		final Raster activeRaster = bim.getData();
		if (dim.x != activeRaster.getWidth()) {
			if (dim.x < 1) {
				dim.x = activeRaster.getWidth();
			} else {
				System.out
						.println("Aim Sizes, Suddenly No Longer Match Input Images, x:"
								+ dim + ", " + activeRaster);
			}
		}
		if (dim.y != activeRaster.getHeight()) {
			if (dim.y < 1) {
				dim.y = activeRaster.getHeight();
			} else {
				System.out
						.println("Aim Sizes, Suddenly No Longer Match Input Images, y:"
								+ dim + ", " + activeRaster);
			}
		}

		final int sliceSize = activeRaster.getWidth()
				* activeRaster.getHeight();
		switch (dataType) {

		case 10:
		case 0: // Char use the interface for short with a different max val
			maxVal = 255;

		case 2: // Int
			int[] gi = new int[sliceSize];
			gi = activeRaster.getPixels(0, 0, activeRaster.getWidth(),
					activeRaster.getHeight(), gi);
			// System.out.println("Getting pixels:"+dataType+", converting to:"+asType+", status:"+gi);
			return TImgTools.convertIntArray(gi, asType, isSigned,
					ShortScaleFactor, maxVal);
		case 3: // Float
			float[] gf = new float[sliceSize];
			gf = activeRaster.getPixels(0, 0, activeRaster.getWidth(),
					activeRaster.getHeight(), gf);
			// System.out.println("Getting pixels:"+dataType+", converting to:"+asType+", status:"+gf);
			return TImgTools.convertFloatArray(gf, asType, isSigned,
					ShortScaleFactor);

		}
		System.out.println("Input file format is not known!!!!");
		return null;

	}

	/**
	 * Object to divide the thread work into supportCores equal parts, default
	 * is z-slices
	 */
	public int[] divideSlices(final int cThread) {
		final int minSlice = 0;
		final int maxSlice = dim.z;

		final int range = (maxSlice - minSlice) / neededCores();

		int startSlice = minSlice;
		int endSlice = startSlice + range;

		for (int i = 0; i < cThread; i++) {
			startSlice = endSlice; // must overlap since i<endSlice is always
									// used, endslice is never run
			endSlice = startSlice + range;
		}
		if (cThread == (neededCores() - 1))
			endSlice = maxSlice;
		if (cThread >= neededCores())
			return null;
		return (new int[] { startSlice, endSlice });
	}

	@Override
	protected void finalize() {
		aimMask = null;
		aimByte = null;
		aimShort = null;
		aimInt = null;
		aimFloat = null;
		if (scratchLoading)
			if (scratchFilename.length() > 0)
				TIPLGlobal.DeleteFile(scratchFilename, "Finalizer");

		TIPLGlobal.runGC();
	}

	public void free() {
		// AimEngine.free();
	}

	private void GenerateHeader(final D3int idim, final D3int ioffset,
			final D3int ipos, final D3float ielSize) {
		isConnected = true;
		ischGuet = true;

		System.out.println("VirtualAim v" + kVer + " : Sample: " + sampleName
				+ " Aim has been imported...");

		pos = new D3int(ipos.x, ipos.y, ipos.z);
		offset = new D3int(ioffset.x, ioffset.y, ioffset.z);
		elSize = new D3float(ielSize.x, ielSize.y, ielSize.z);
		dim = new D3int(idim.x, idim.y, idim.z);

		// ImageJ code
		width = dim.x;
		height = dim.y;
		// Offset only important for filtering
		// pos.x-=offset.x;
		// pos.y-=offset.y;
		// pos.z-=offset.z;
		appendProcLog("Dimensions: " + dim.x + ", " + dim.y + ", " + dim.z);
		appendProcLog("Position: " + pos.x + ", " + pos.y + ", " + pos.z);
		appendProcLog("Element Size: " + elSize.x + ", " + elSize.y + ", "
				+ elSize.z);
		appendProcLog("Image Type: " + imageType);

		System.out.println(getProcLog());
		getSliceCalls = 0;
	}

	/** a simple function to load all of the image data */
	@Deprecated
	public void getAimImage() {
		getAimImage(imageType);
	}

	@Deprecated
	protected void getAimImage(final int asType) {
		if (fullAimLoaded)
			if (asType == fullAimLoadedAs)
				return; // No futher conversion needed, just get out
		// Allocate new aim file
		final int aimLength = getDim().x * getDim().y * getDim().z;
		switch (asType) {
		case TImgTools.IMAGETYPE_CHAR:
			aimByte = null;
			TIPLGlobal.runGC();
			aimByte = new char[aimLength];
			break;
		case  TImgTools.IMAGETYPE_SHORT:
			aimShort = null;
			TIPLGlobal.runGC();
			aimShort = new short[aimLength];
			break;
		case  TImgTools.IMAGETYPE_INT:
			aimInt = null;
			TIPLGlobal.runGC();
			aimInt = new int[aimLength];
			break;
		case  TImgTools.IMAGETYPE_FLOAT:
			aimFloat = null;
			TIPLGlobal.runGC();
			aimFloat = new float[aimLength];
			break;
		case  TImgTools.IMAGETYPE_BOOL:
			aimMask = null;
			TIPLGlobal.runGC();
			aimMask = new boolean[aimLength];
			break;
		default:
			System.out.println("!!Aim Read Violation, Cannot Read "
					+ fullAimLoadedAs + " as boolean!");
			throw new IllegalArgumentException("!!Aim Read Violation, Cannot Read "
					+ fullAimLoadedAs + " as boolean!");
			
		}
		// Convert or read in aim file
		runSliceLoader(1, asType);

		// Delete old data
		if (fullAimLoaded) {
			switch (fullAimLoadedAs) {
			case 0:
				aimByte = null;
				break;
			case 1:
				aimShort = null;
				break;
			case 2:
				aimInt = null;
				break;
			case 3:
				aimFloat = null;
				break;
			case 10:
				aimMask = null;
				break;
			default:
				throw new IllegalArgumentException("!!Aim Read Violation, Cannot Read "
						+ fullAimLoadedAs + " as boolean!");
			}
		}

		fullAimLoaded = true;
		fullAimLoadedAs = asType;
		imageType = asType;
		switch (asType) {
		case 0:
			dataName = "Byte";
			break;
		case 1:
			dataName = "Short";
			break;
		case 2:
			dataName = "Int";
			break;
		case 3:
			dataName = "Float";
			break;
		case 10:
			dataName = "Mask";
			break;
		}
		System.out.println("VA: Sample: " + sampleName
				+ " Aim has been read as " + dataName);

	}

	/** Used to read in a section of aim file */
	protected void getAimSubSection(final int asType, final int bSlice,
			final int tSlice) {
		final int wid = dim.x;
		final int het = dim.y;
		if (!fullAimLoaded) {
			switch (asType) {
			case 10:
				for (int n = bSlice; n < tSlice; n++) {
					final int outPos = n * wid * het;
					final boolean[] gbool = getBoolArray(n);
					imaskCopy(gbool, outPos, wid * het);
				}
				break;
			case 0:
				for (int n = bSlice; n < tSlice; n++) {
					final int outPos = n * wid * het;
					final char[] gb = getByteArray(n);
					ibyteCopy(gb, outPos, wid * het);
				}
				break;
			case 1:
				for (int n = bSlice; n < tSlice; n++) {
					final int outPos = n * wid * het;
					final short[] gs = getShortArray(n);
					ishortCopy(gs, outPos, wid * het);
				}
				break;
			case 2:
				for (int n = bSlice; n < tSlice; n++) {
					final int outPos = n * wid * het;
					final int[] gi = getIntArray(n);
					iintCopy(gi, outPos, wid * het);
				}
				break;
			case 3:
				for (int n = bSlice; n < tSlice; n++) {
					final int outPos = n * wid * het;
					final float[] gf = getFloatArray(n);
					ifloatCopy(gf, outPos, wid * het);
				}
				break;
			}

		} else {
			final int bPos = bSlice * wid * het;
			final int tPos = tSlice * wid * het;
			switch (asType) {
			case 0:
				if (fullAimLoadedAs == 1) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimByte[cIndex] = (char) aimShort[cIndex];
				} else if (fullAimLoadedAs == 2) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimByte[cIndex] = (char) aimInt[cIndex];
				} else if (fullAimLoadedAs == 3) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimByte[cIndex] = (char) Math.round(aimFloat[cIndex]
								/ ShortScaleFactor);
				} else if (fullAimLoadedAs == 10) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimByte[cIndex] = (char) (aimMask[cIndex] ? 127 : 0);
				}
				break;
			case 1:
				if (fullAimLoadedAs == 0) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimShort[cIndex] = (short) aimByte[cIndex];
				} else if (fullAimLoadedAs == 2) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimShort[cIndex] = (short) aimInt[cIndex];
				} else if (fullAimLoadedAs == 3) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimShort[cIndex] = (short) Math.round(aimFloat[cIndex]
								/ ShortScaleFactor);
				} else if (fullAimLoadedAs == 10) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimShort[cIndex] = (short) (aimMask[cIndex] ? 127 : 0);
				}
				break;
			case 2:
				if (fullAimLoadedAs == 0) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimInt[cIndex] = (aimByte[cIndex]);
				} else if (fullAimLoadedAs == 1) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimInt[cIndex] = aimShort[cIndex];
				} else if (fullAimLoadedAs == 3) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimInt[cIndex] = Math.round(aimFloat[cIndex]
								/ ShortScaleFactor);
				} else if (fullAimLoadedAs == 10) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimInt[cIndex] = (aimMask[cIndex] ? 127 : 0);
				}
				break;
			case 3:
				if (fullAimLoadedAs == 0) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimFloat[cIndex] = (aimByte[cIndex]) * ShortScaleFactor;
				} else if (fullAimLoadedAs == 1) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimFloat[cIndex] = (aimShort[cIndex])
								* ShortScaleFactor;
				} else if (fullAimLoadedAs == 2) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimFloat[cIndex] = aimInt[cIndex] * ShortScaleFactor;
				} else if (fullAimLoadedAs == 10) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimFloat[cIndex] = (aimMask[cIndex] ? 1.0f : 0.0f);
				}
				break;
			case 10:
				if (fullAimLoadedAs == 0) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimMask[cIndex] = aimByte[cIndex] > 0;
				} else if (fullAimLoadedAs == 1) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimMask[cIndex] = aimShort[cIndex] > 0;
				} else if (fullAimLoadedAs == 2) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimMask[cIndex] = aimInt[cIndex] > 0;
				} else if (fullAimLoadedAs == 3) {
					for (int cIndex = bPos; cIndex < tPos; cIndex++)
						aimMask[cIndex] = aimFloat[cIndex] > 0;
				}
				break;
			}
		}
	}

	@Override
	@Deprecated
	public boolean[] getBoolAim() {
		getAimImage(10);
		return aimMask;
	}

	@Override
	public boolean[] getBoolArray(final int n) {
		boolean[] gbool = new boolean[1];
		getSliceCalls++;
		if (n < dim.z) {
			try {
				gbool = (boolean[]) loadslice(n, 10); // Load the first slice to
														// have an idea about
														// the data.
			} catch (final Exception e) {
				System.out.println("Error Reading " + n + " Slice!!!");
				e.printStackTrace();
			}
		} else {
			System.out
					.println("!!Aim Read Violation : Attempted to Read Slice "
							+ n + " of " + dim.z);
		}
		return gbool;
	}

	@Override
	@Deprecated
	public char[] getByteAim() {
		getAimImage(0);
		return aimByte;
	}

	@Override
	public char[] getByteArray(final int n) {
		char[] gb = new char[1];
		getSliceCalls++;
		if (n < dim.z) {
			try {
				gb = (char[]) loadslice(n, 0); // Load the first slice to have
												// an idea about the data.
			} catch (final Exception e) {
				System.out.println("Error Reading " + n + " Slice!!!");
				e.printStackTrace();
			}
		} else {
			if (debugMode)
				System.out
						.println("!!Aim Read Violation : Attempted to Read Slice "
								+ n + " of " + dim.z);

			gb[0] = 0;
		}
		return gb;
	}

	public void GetBytePoints(final int minValue, final int maxValue,
			final int startSlice) {
		char[] gg;

		int rCount = 0;
		float bCount = 0;
		float vSum = 0;
		D3float npos;

		// Marching Cube Header
		D3int rpos;
		boolean[][] curSlice;
		final float triArea = 0;
		// End Marching Header
		final int defArraySize = Math.min((int) (dim.x * dim.y * dim.z * 0.9),
				2000);
		positionList = new Vector<Float[]>(defArraySize);
		valueList = new Vector<Float>(defArraySize);
		for (int i = startSlice; i < dim.z; i++) {
			rCount = 0;
			bCount = 0;
			// Initializes slice as empty
			gg = getByteArray(i);
			curSlice = new boolean[getDim().x][getDim().y];
			for (final char b : gg) {
				if (b > minValue) {
					if ((b <= maxValue) | (maxValue < minValue)) {
						npos = TImgTools.getRXYZFromVec(pos, dim, rCount, i);
						rpos = TImgTools.getDXYZFromVec(dim, rCount, i);
						// rpos=getXYZ(rCount,i);
						// npos=getrXYZ(rpos);

						final Float[] cPos = new Float[3];
						cPos[0] = new Float(npos.x);
						cPos[1] = new Float(npos.y);
						cPos[2] = new Float(npos.z);
						positionList.add(cPos);
						vSum = b;
						// vSum*=ShortScaleFactor;
						valueList.add(new Float(vSum));
						// Code to manage the Marching-Cube Style Slice Creation

						curSlice[rpos.x][rpos.y] = true;
						// Keep track of the number of positive pixels
						bCount++;
					}
				}
				rCount++;

			}
			if (bCount == 0) {
				if (debugMode)
					System.out.println("Empty Slice!");
				break;
			}
			gg = null;
		}
		positionList.trimToSize();
		valueList.trimToSize();
		triangles = triArea;

	}

	/**
	 * Whether or not basic compression (compatible almost everywhere) should be
	 * used when writing data
	 */
	@Override
	public boolean getCompression() {
		return useCompression;
	}

	/** The size of the image */
	@Override
	public D3int getDim() {
		return dim;
	}

	/** The element size (in mm) of a voxel */
	@Override
	public D3float getElSize() {
		return elSize;
	}

	@Override
	@Deprecated
	public float[] getFloatAim() {
		getAimImage(3);
		return aimFloat;
	}

	@Override
	public float[] getFloatArray(final int n) {
		getSliceCalls++;
		float[] gf = new float[1];
		if (n < dim.z) {
			try {
				gf = (float[]) loadslice(n, 3); // Load the first slice to have
												// an idea about the data.
			} catch (final Exception e) {
				System.out.println("Error Reading " + n + " Slice!!!");
				e.printStackTrace();
			}
		} else {
			System.out
					.println("!!Aim Read Violation : Attempted to Read Slice "
							+ n + " of " + dim.z);
		}
		return gf;
	}

	/** Package as ImagePlus */
	public ImagePlus getImagePlus() {
		return TImgToImagePlus.MakeImagePlus(this);
	}

	// Implement TImg Interface
	/**
	 * The aim type of the image (0=char, 1=short, 2=int, 3=float, 10=bool, -1
	 * same as input)
	 */
	@Override
	public int getImageType() {
		return imageType;
	}

	@Override
	@Deprecated
	public int[] getIntAim() {
		getAimImage(2);
		return aimInt;
	}

	@Override
	public int[] getIntArray(final int n) {
		int[] gi = new int[1];
		getSliceCalls++;
		if (n < dim.z) {
			try {
				gi = (int[]) loadslice(n, 2); // Load the first slice to have an
												// idea about the data.
			} catch (final Exception e) {
				System.out.println("Error Reading " + n + " Slice!!!");
				e.printStackTrace();
			}
		} else {
			System.out
					.println("!!Aim Read Violation : Attempted to Read Slice "
							+ n + " of " + dim.z);

		}
		return gi;
	}

	public void GetIntPoints(final int minValue, final int maxValue,
			final int startSlice) {
		int[] gg;
		int rCount = 0;
		int bCount = 0;
		D3float npos;

		final float triArea = 0;
		// End Marching Header
		final int defArraySize = Math.min((int) (dim.x * dim.y * dim.z * 0.9),
				2000);
		positionList = new Vector<Float[]>(defArraySize);
		valueList = new Vector<Float>(defArraySize);
		for (int i = startSlice; i < dim.z; i++) {
			rCount = 0;
			bCount = 0;
			// new boolean[getWidth()][getHeight()];
			gg = getIntArray(i);
			for (final int b : gg) {
				if (b > minValue) {
					if ((b <= maxValue) | (maxValue < minValue)) {
						npos = TImgTools.getRXYZFromVec(pos, dim, rCount, i);

						final Float[] cPos = new Float[3];
						cPos[0] = new Float(npos.x);
						cPos[1] = new Float(npos.y);
						cPos[2] = new Float(npos.z);
						// System.out.println("Cur Pt Get :"+cPos[0]+":"+cPos[1]+":"+cPos[2]);
						positionList.add(cPos);
						valueList.add(new Float((b)));
						bCount++;
					}
				}
				rCount++;
			}
			if (bCount == 0) {
				if (debugMode)
					System.out.println("Empty Slice!");
				break;
			} // else System.out.println("Busy slice with  :"+bCount);

			gg = null;
		}
		positionList.trimToSize();
		valueList.trimToSize();
		triangles = triArea;
	}

	/**
	 * The size of the border around the image which does not contain valid
	 * voxel data
	 */
	@Override
	public D3int getOffset() {
		return offset;
	}

	@Override
	public String getPath() {
		return aimPath;
	}

	public void GetPoints() {
		GetPoints(0, -1, 0);
	}

	public void GetPoints(final int minValue, final int maxValue,
			final int startSlice) {
		switch (this.imageType) {
		case 0:
			if (debugMode)
				System.out.println("Getting Points from Byte Array From "
						+ minValue + "->" + maxValue);
			GetBytePoints(minValue, maxValue, startSlice);
			break;
		case 1:
			if (debugMode)
				System.out.println("Getting Points from Short Array From "
						+ minValue + "->" + maxValue);
			GetShortPoints((short) minValue, (short) maxValue, startSlice);
			break;
		case 2:
			if (debugMode)
				System.out.println("Getting Points from Integer Array From "
						+ minValue + "->" + maxValue);
			GetIntPoints(minValue, maxValue, startSlice);
			break;
		case 3:
			if (debugMode)
				System.out.println("Float Not Implemented Yet");
			break;
		}
	}

	public void GetPointsFromList(final Vector<Float[]> pList) {
		final Iterator<Float[]> itr = pList.iterator();
		positionList = null;
		positionList = pList;
		valueList = null;
		valueList = new Vector<Float>(pList.size());

		// for (int i=0;i<pList.size();i++) {
		while (itr.hasNext()) {
			final Float[] curPt = itr.next();// pList.elementAt(i);
			final int curX = (int) curPt[0].floatValue() - pos.x;
			final int curY = (int) curPt[1].floatValue() - pos.y;
			final int curZ = (int) curPt[2].floatValue() - pos.z;
			// Debugging Message Only
			// System.out.println("VirtualAim : Fetching - "+curX+","+curY+","+curZ);
			char[] gb = null;
			short[] gs = null;
			int[] gi = null;
			float[] gf = null;
			switch (imageType) {
			case 0:
				gb = getByteArray(curZ);
				cgLength = gb.length;
				break;
			case 1:
				gs = getShortArray(curZ);
				cgLength = gs.length;
				break;
			case 2:
				gi = getIntArray(curZ);
				cgLength = gi.length;
				break;
			case 3:
				gf = getFloatArray(curZ);
				cgLength = gf.length;
				break;
			}
			final int arrNum = TImgTools.getJFromVec(pos, dim,
					curPt[0].intValue(), curPt[1].intValue());

			if ((arrNum < cgLength) && (arrNum >= 0)) {
				switch (imageType) {
				case 0:
					valueList.add(new Float(gb[arrNum]));
					break;
				case 1:
					valueList.add(new Float(gs[arrNum] * ShortScaleFactor));
					break;
				case 2:
					valueList.add(new Float(gi[arrNum]));
					break;
				case 3:
					valueList.add(new Float(gf[arrNum]));
					break;
				}

			} else {
				valueList.add(new Float(0));
				if (debugMode) {
					System.out.println("Aim Sizes DO NOT MATCH!!! arrNum:"
							+ arrNum + ", clen " + cgLength);
					System.out.println("Map Pixel  (" + curPt[0].floatValue()
							+ "," + curPt[1].floatValue() + ","
							+ curPt[2].floatValue() + ") in (" + pos.x + ","
							+ pos.y + "," + pos.z + ")");
					System.out.println("Search for (" + curX + "," + curY + ","
							+ curZ + ") in (" + getDim().x + "," + getDim().y
							+ "," + getDim().z + ")");
				}
			}
		}

	}

	@Override
	public Object getPolyImage(final int slice, final int asType) {
		try {
			return loadslice(slice, asType);
		} catch (final Exception e) {
			System.out.println("ERROR LoadAim: Slice " + slice
					+ " cannot be opened as " + asType
					+ ", not sure what is wrong");
			e.printStackTrace();
			imglist = null;
			return null;
		}
	}

	/**
	 * The position of the bottom leftmost voxel in the image in real space,
	 * only needed for ROIs
	 */
	@Override
	public D3int getPos() {
		return pos;
	}

	@Override
	public String getProcLog() {
		return procLog;
	}

	/** The name of the data */
	@Override
	public String getSampleName() {
		return sampleName;
	}

	/**
	 * Procedure Log, string containing past operations and information on the
	 * aim-file
	 */
	// public String getProcLog() { return procLog;}

	public double getScaleFactorFromProc() {
		return ShortScaleFactor;

	}

	@Override
	@Deprecated
	public short[] getShortAim() {
		getAimImage(1);
		return aimShort;
	}

	@Override
	public short[] getShortArray(final int n) {
		short[] gs = new short[1];
		if (n < dim.z) {
			try {
				gs = (short[]) loadslice(n, 1); // Load the first slice to have
												// an idea about the data.
			} catch (final Exception e) {
				System.out.println("Error Reading " + n + " Slice!!!");
				e.printStackTrace();
			}

		} else {
			System.out
					.println("!!Aim Read Violation : Attempted to Read Slice "
							+ n + " of " + dim.z);

		}
		return gs;
	}

	public void GetShortPoints(final short minValue, final short maxValue,
			final int startSlice) {
		short[] gg;

		int rCount = 0;
		float bCount = 0;
		float vSum = 0;
		D3float npos;

		// Marching Cube Header
		D3int rpos;
		boolean[][] curSlice;
		final float triArea = 0;
		// End Marching Header
		final int defArraySize = Math.min((int) (dim.x * dim.y * dim.z * 0.9),
				2000);
		positionList = new Vector<Float[]>(defArraySize);
		valueList = new Vector<Float>(defArraySize);
		for (int i = startSlice; i < dim.z; i++) {
			rCount = 0;
			bCount = 0;
			// Initializes slice as empty
			gg = getShortArray(i);
			curSlice = new boolean[dim.x][dim.y];
			for (final short b : gg) {
				if (b > minValue) {
					if ((b <= maxValue) | (maxValue < minValue)) {
						npos = TImgTools.getRXYZFromVec(pos, dim, rCount, i);
						rpos = TImgTools.getDXYZFromVec(dim, rCount, i);

						// rpos=getXYZ(rCount,i);
						// npos=getrXYZ(rpos);

						final Float[] cPos = new Float[3];
						cPos[0] = new Float(npos.x);
						cPos[1] = new Float(npos.y);
						cPos[2] = new Float(npos.z);
						positionList.add(cPos);
						vSum = b;
						vSum *= ShortScaleFactor;
						valueList.add(new Float(vSum));
						// Code to manage the Marching-Cube Style Slice Creation

						curSlice[rpos.x][rpos.y] = true;
						// Keep track of the number of positive pixels
						bCount++;
					}
				}
				rCount++;

			}
			if (bCount == 0) {
				if (debugMode)
					System.out.println("Empty Slice!");
				break;
			}
			gg = null;
		}
		positionList.trimToSize();
		valueList.trimToSize();
		triangles = triArea;
		// System.out.println("Surface Area : "+triArea);
	}

	@Override
	public float getShortScaleFactor() {
		return ShortScaleFactor;
	}

	/**
	 * Is the image signed (should an offset be added / subtracted when the data
	 * is loaded to preserve the sign)
	 */
	@Override
	public boolean getSigned() {
		return isSigned;
	}

	public float GetSpot(final int x, final int y, final int z) {
		System.out.println("Not working yet...");
		return -1;
	}

	public Double[] getXYZVec(final int cIndex, final int sliceNumber) {
		return TImgTools.getXYZVecFromVec(pos, dim, cIndex, sliceNumber);
	}

	/** Used to write a slice into the main array */
	protected synchronized void ibyteCopy(final char[] slice, final int oPos,
			final int cLen) {
		System.arraycopy(slice, 0, aimByte, oPos, cLen);
	}

	/** Used to write a slice into the main array */
	protected synchronized void ifloatCopy(final float[] slice, final int oPos,
			final int cLen) {
		System.arraycopy(slice, 0, aimFloat, oPos, cLen);
	}

	/** Used to write a slice into the main array */
	protected synchronized void iintCopy(final int[] slice, final int oPos,
			final int cLen) {
		System.arraycopy(slice, 0, aimInt, oPos, cLen);
	}

	/** Used to write a slice into the main array */
	protected synchronized void imaskCopy(final boolean[] slice,
			final int oPos, final int cLen) {
		System.arraycopy(slice, 0, aimMask, oPos, cLen);
	}

	/**
	 * Creates a new, aim the given Aim and a 1d boolean array with the same
	 * length as the existing data
	 */
	public VirtualAim inheritedAim(final boolean[] inData) {
		return inheritedAim(inData, dim, offset);
	}

	/**
	 * Creates a new, aim the given Aim and a 1d boolean array with the same
	 * length as the existing data
	 */
	@Override
	public VirtualAim inheritedAim(final boolean[] inData, final D3int idim,
			final D3int ioffset) {
		// Clone aim with new elements inside
		final VirtualAim outImage = new VirtualAim();

		outImage.fullAimLoadedAs = 10;
		outImage.imageType = 10;
		outImage.aimMask = inData;

		outImage.dim = new D3int(idim.x, idim.y, idim.z);
		outImage.offset = new D3int(ioffset.x, ioffset.y, ioffset.z);

		if (inData.length != Math.round(outImage.dim.prod())) {
			System.out
					.println("Aim Sizes Do Not Match!, Cannot Use inheritedAim function!!!");
			final Throwable t = new Throwable();
			t.printStackTrace();
			return null;
		}
		inheritedAimHelper(outImage);

		return outImage;
	}

	/**
	 * Creates a new, aim the given Aim and a 1d int array with the same length
	 * as the existing data
	 */
	public VirtualAim inheritedAim(final char[] inData) {
		return inheritedAim(inData, dim, offset);
	}

	/**
	 * Creates a new, aim the given Aim and a 1d char array with the same length
	 * as the existing data
	 */
	@Override
	public VirtualAim inheritedAim(final char[] inData, final D3int idim,
			final D3int ioffset) {
		// Clone aim with new elements inside
		final VirtualAim outImage = new VirtualAim();

		outImage.fullAimLoadedAs = 0;
		outImage.imageType = 0;
		outImage.aimByte = inData;

		outImage.dim = new D3int(idim.x, idim.y, idim.z);
		outImage.offset = new D3int(ioffset.x, ioffset.y, ioffset.z);

		if (inData.length != Math.round(outImage.dim.prod())) {
			System.out
					.println("Aim Sizes Do Not Match!, Cannot Use inheritedAim function!!!");
			final Throwable t = new Throwable();
			t.printStackTrace();
			return null;
		}
		inheritedAimHelper(outImage);

		return outImage;
	}

	/**
	 * Creates a new, aim the given Aim and a 1d float array with the same
	 * length as the existing data
	 */
	public VirtualAim inheritedAim(final float[] inData) {
		return inheritedAim(inData, dim, offset);
	}

	/**
	 * Creates a new, aim the given Aim and a 1d float array with the same
	 * length as the existing data
	 */
	@Override
	public VirtualAim inheritedAim(final float[] inData, final D3int idim,
			final D3int ioffset) {
		// Clone aim with new elements inside
		final VirtualAim outImage = new VirtualAim();

		outImage.fullAimLoadedAs = 3;
		outImage.imageType = 3;
		outImage.aimFloat = inData;

		outImage.dim = new D3int(idim.x, idim.y, idim.z);
		outImage.offset = new D3int(ioffset.x, ioffset.y, ioffset.z);

		if (inData.length != Math.round(outImage.dim.prod())) {
			System.out
					.println("Aim Sizes Do Not Match!, Cannot Use inheritedAim function!!!");
			final Throwable t = new Throwable();
			t.printStackTrace();
			return null;
		}
		inheritedAimHelper(outImage);

		return outImage;
	}

	/**
	 * Creates a new, aim the given Aim and a 1d int array with the same length
	 * as the existing data
	 */
	public VirtualAim inheritedAim(final int[] inData) {
		return inheritedAim(inData, dim, offset);
	}

	/**
	 * Creates a new, aim the given Aim and a 1d int array with the same length
	 * as the existing data
	 */
	@Override
	public VirtualAim inheritedAim(final int[] inData, final D3int idim,
			final D3int ioffset) {
		// Clone aim with new elements inside
		final VirtualAim outImage = new VirtualAim();

		outImage.fullAimLoadedAs = 2;
		outImage.imageType = 2;
		outImage.aimInt = inData;

		outImage.dim = new D3int(idim.x, idim.y, idim.z);
		outImage.offset = new D3int(ioffset.x, ioffset.y, ioffset.z);

		if (inData.length != Math.round(outImage.dim.prod())) {
			System.out
					.println("Aim Sizes Do Not Match!, Cannot Use Simple inheritedAim function!!!");
			final Throwable t = new Throwable();
			t.printStackTrace();
			return null;
		}
		inheritedAimHelper(outImage);

		return outImage;
	}

	/**
	 * Creates a new, aim the given Aim and a 1d int array with the same length
	 * as the existing data
	 */
	public VirtualAim inheritedAim(final short[] inData) {
		return inheritedAim(inData, dim, offset);
	}

	/**
	 * Creates a new, aim the given Aim and a 1d short array with the same
	 * length as the existing data
	 */
	@Override
	public VirtualAim inheritedAim(final short[] inData, final D3int idim,
			final D3int ioffset) {
		// Clone aim with new elements inside
		final VirtualAim outImage = new VirtualAim();

		outImage.fullAimLoadedAs = 1;
		outImage.imageType = 1;
		outImage.aimShort = inData;

		outImage.dim = new D3int(idim.x, idim.y, idim.z);
		outImage.offset = new D3int(ioffset.x, ioffset.y, ioffset.z);

		if (inData.length != Math.round(outImage.dim.prod())) {
			System.out
					.println("Aim Sizes Do Not Match!, Cannot Use inheritedAim function!!!");
			final Throwable t = new Throwable();
			t.printStackTrace();
			return null;
		}
		inheritedAimHelper(outImage);

		return outImage;
	}

	@Override
	public TImg inheritedAim(final TImgRO inTImg) {
		return TImgToVirtualAim(inTImg);
	}

	private void inheritedAimHelper(final VirtualAim outImage) {
		outImage.fullAimLoaded = true;
		outImage.procLog = procLog;
		outImage.sampleName = sampleName;
		outImage.setShortScaleFactor(getShortScaleFactor());
		outImage.GenerateHeader(outImage.dim, outImage.offset, pos, elSize);

		outImage.ischGuet = true;
	}

	@Override
	public boolean InitializeImage(final D3int dPos, final D3int cDim,
			final D3int dOffset, final D3float dElSize, final int dImageType) {
		pos = dPos;
		dim = cDim;
		offset = dOffset;
		elSize = dElSize;
		imageType = dImageType;
		aimMask = null;
		aimByte = null;
		aimShort = null;
		aimInt = null;
		aimFloat = null;
		GenerateHeader(dim, offset, pos, elSize);
		final int imgVoxCnt = dim.x * dim.y;

		switch (imageType) {
		case 10:
			aimMask=(boolean[]) TImgTools.watchBigAlloc(TImgTools.IMAGETYPE_BOOL, imgVoxCnt * dim.z);
			break;
		case 0:
			aimByte=(char[]) TImgTools.watchBigAlloc(TImgTools.IMAGETYPE_CHAR, imgVoxCnt * dim.z);
			break;
		case 1:
			aimShort=(short[]) TImgTools.watchBigAlloc(TImgTools.IMAGETYPE_SHORT, imgVoxCnt * dim.z);
			break;
		case 2:
			aimInt=(int[]) TImgTools.watchBigAlloc(TImgTools.IMAGETYPE_INT, imgVoxCnt * dim.z);
			break;
		case 3:
			aimFloat=(float[]) TImgTools.watchBigAlloc(TImgTools.IMAGETYPE_FLOAT, imgVoxCnt * dim.z);
			break;
		default:
			System.out.println("Hats dir im gring gschisse? So s'typ hans ned "
					+ imageType);

		}
		fullAimLoaded = true;
		fullAimLoadedAs = imageType;
		return true;
	}

	@Override
	public int isFast() {
		if (fullAimLoaded)
			return ITIPLStorage.FAST_MEMORY_BASED;
		else
			return ITIPLStorage.FAST_TIFF_BASED;
	}

	/** Is the data in good shape */
	@Override
	public boolean isGood() {
		return ischGuet;
	}

	/** Used to write a slice into the main array */
	protected synchronized void ishortCopy(final short[] slice, final int oPos,
			final int cLen) {
		System.arraycopy(slice, 0, aimShort, oPos, cLen);
	}

	/**
	 * Convert the loaded image to a stack Warning loading an image as a stack
	 * requires twice as much memory due to the different methods used in Aim
	 * and ImageJ data models, and the lack of operator overloading in java :-(
	 */
	@Deprecated
	protected void loadAimfromStack() {
		System.out.println("Loading ImageJ stack as Aim");
		boolean changedSize = false;
		if (dim.z != stack.length) {
			dim.z = stack.length;
			changedSize = true;
		}
		if (dim.x != width) {
			dim.x = width;
			changedSize = true;
		}
		if (dim.y != height) {
			dim.y = height;
			changedSize = true;
		}
		if (changedSize)
			System.out.println("Volume has changed size, adjusting:" + dim);
		final int imgVoxCnt = dim.x * dim.y;

		// Figure out what is in the stack
		if (stack[0] instanceof char[])
			imageType = 0;
		else if (stack[0] instanceof short[])
			imageType = 1;
		else if (stack[0] instanceof int[])
			imageType = 2;
		else if (stack[0] instanceof float[])
			imageType = 3;
		else {
			System.out.println("Unreadable!!!");
			return;
		}

		// Erase old data
		aimMask = null;
		aimByte = null;
		aimShort = null;
		aimInt = null;
		aimFloat = null;
		TIPLGlobal.runGC();

		// Pre allocate array and read in data
		switch (imageType) {
		case 10:
			aimMask = new boolean[imgVoxCnt * dim.z];
			break;
		case 0:
			aimByte = new char[imgVoxCnt * dim.z];
			break;
		case 1:
			aimShort = new short[imgVoxCnt * dim.z];
			break;
		case 2:
			aimInt = new int[imgVoxCnt * dim.z];
			break;
		case 3:
			aimFloat = new float[imgVoxCnt * dim.z];
			break;
		default:
			System.out.println("Hats dir im gring gschisse? So s'typ hans ned "
					+ imageType);

		}
		System.out.println("Copying Slices...");
		int cPos = 0;
		for (int i = 0; i < dim.z; i++) {
			switch (imageType) {
			case 10:
				final char[] bstack = (char[]) stack[i];
				for (int j = 0; j < imgVoxCnt; j++)
					aimMask[cPos + j] = (bstack[j] > 0);
				break;
			case 0:
				System.arraycopy(stack[i], 0, aimByte, cPos, imgVoxCnt);
				break;
			case 1:
				System.arraycopy(stack[i], 0, aimShort, cPos, imgVoxCnt);
				break;
			case 2:
				final short[] sstack = (short[]) stack[i];
				for (int j = 0; j < imgVoxCnt; j++)
					aimInt[cPos + j] = (sstack[j]);
				break;
			case 3:
				System.arraycopy(stack[i], 0, aimFloat, cPos, imgVoxCnt);
				break;
			default:
				System.out
						.println("Hats dir im gring gschisse? So s'typ hans ned "
								+ imageType);
			}
			cPos += imgVoxCnt;
		}
		fullAimLoaded = true;
		fullAimLoadedAs = imageType;
		inSync = true;
	}

	/**
	 * Convert the loaded image to a stack Warning loading an image as a stack
	 * requires twice as much memory due to the different methods used in Aim
	 * and ImageJ data models, and the lack of operator overloading in java :-(
	 */
	@Deprecated
	protected void loadAimfromStack(final Object[] istack) {
		stack = istack;
		loadAimfromStack();
	}

	@Deprecated
	// this function is hideous, this really needs to be fixed
	protected void loadAimfromTImg(final TImgRO inTImg) {
		System.out.println("Loading generic TImg as Aim");
		useTImg = true;
		baseTImg = inTImg;
		imageType = inTImg.getImageType();
		// Erase old data
		aimMask = null;
		aimByte = null;
		aimShort = null;
		aimInt = null;
		aimFloat = null;
		TIPLGlobal.runGC();

		fullAimLoaded = false;
		fullAimLoadedAs = -1;
		inSync = true;
		useTImg = true;
	}

	private Object loadslice(final int slice) throws Exception {
		return loadslice(slice, -1);
	}

	private Object loadslice(final int slice, int asType) throws Exception {
		if (asType == -1)
			asType = imageType;
		if (fullAimLoaded) {

			final int sliceSize = dim.x * dim.y;
			final int outPos = slice * dim.x * dim.y;

			// Handle float images specially
			if (imageType == 3) {
				final float[] gf = new float[sliceSize];
				for (int cIndex = outPos; cIndex < (outPos + dim.x * dim.y); cIndex++)
					gf[cIndex - outPos] = aimFloat[cIndex];
				return TImgTools.convertFloatArray(gf, asType, isSigned,
						ShortScaleFactor);
			}

			final int[] gi = new int[sliceSize];

			int maxVal = 65536;
			switch (imageType) {
			case 0:
				for (int cIndex = outPos; cIndex < (outPos + dim.x * dim.y); cIndex++)
					gi[cIndex - outPos] = (aimByte[cIndex]);
				maxVal = 255;
				break;
			case 1:
				for (int cIndex = outPos; cIndex < (outPos + dim.x * dim.y); cIndex++)
					gi[cIndex - outPos] = aimShort[cIndex];
				break;
			case 2:
				for (int cIndex = outPos; cIndex < (outPos + dim.x * dim.y); cIndex++)
					gi[cIndex - outPos] = aimInt[cIndex];
				break;
			case 10:
				for (int cIndex = outPos; cIndex < (outPos + dim.x * dim.y); cIndex++)
					if (aimMask[cIndex])
						gi[cIndex - outPos] = 255;
				break;

			default:
				System.out.println("Not supported!" + imageType);
				return 0;
			}
			return TImgTools.convertIntArray(gi, asType, isSigned,
					ShortScaleFactor, maxVal);

		}

		// Read a single slice in (actually reads jpg), this don't week

		if (useTImg) {
			final TImg.TImgFull fullBaseTImg = new TImg.TImgFull(baseTImg);
			switch (asType) {
			case 0:
				return fullBaseTImg.getByteArray(slice);
			case 1:
				return fullBaseTImg.getShortArray(slice);
			case 2:
				return fullBaseTImg.getIntArray(slice);
			case 3:
				return fullBaseTImg.getFloatArray(slice);
			case 10:
				return fullBaseTImg.getBoolArray(slice);
			default:
				System.out.println("TImg -- Not supported!" + imageType);
				return 0;
			}
		} else if (layertiff) {
			// Which of the multiple images in the TIFF file do we want to load
			// 0 refers to the first, 1 to the second and so on.
			final ImageDecoder dec = parseTifStack(!tifStackHeaderRead);

			if (slice < dec.getNumPages()) {
				// System.out.println("Decoding slice"+slice+" of "+dec.getNumPages());
				return decodeimage(slice, dec.decodeAsRenderedImage(slice),
						asType);

			} else {
				System.out.println("Exceeds bound!!!" + slice + " of "
						+ dec.getNumPages());
				return null;
			}

		} else {
			throw new IllegalStateException(
					"Directory Datasets Should Now Be Handeled by the DirectoryReader class not by VirtualAim");

		}

	}

	protected void loadStackFromAim() {
		loadStackFromAim(false);
	}

	/**
	 * Convert the loaded image to a stack Warning loading an image as a stack
	 * requires twice as much memory due to the different methods used in Aim
	 * and ImageJ data models, and the lack of operator overloading in java :-(
	 */
	protected void loadStackFromAim(final boolean force) {
		System.out.println("Loading data as ImageJ stack...");
		if (isLoaded) {
			if (force) {
				System.out.println("Already loaded, loading again?...");
				stack = null;
				TIPLGlobal.runGC();
			} else {
				System.out.println("Already loaded, done...");
				return;
			}
		}
		stack = new Object[dim.z];
		int cPos = 0;
		final int imgVoxCnt = dim.x * dim.y;
		// Pre allocate array and read in data
		width = dim.x;
		height = dim.y;
		switch (imageType) {
		case 0:
		case 10:
			for (int i = 0; i < dim.z; i++)
				stack[i] = new byte[imgVoxCnt];
			getByteAim();

			break;
		case 1:
		case 2:
			for (int i = 0; i < dim.z; i++)
				stack[i] = new short[imgVoxCnt];
			getShortAim();

			break;
		case 3:
			for (int i = 0; i < dim.z; i++)
				stack[i] = new float[imgVoxCnt];
			getFloatAim();

			break;
		default:
			System.out.println("Hats dir im gring gschisse? So s'typ hans ned "
					+ imageType);

		}

		System.out.println("Copying Slices...");
		for (int i = 0; i < dim.z; i++) {
			switch (imageType) {
			case 0:
			case 10:
				// new approach converts char to byte
				for (int j = 0; j < imgVoxCnt; j++)
					((byte[]) stack[i])[j] = (byte) aimByte[cPos + j];
				// old approach
				// System.arraycopy(aimByte,cPos,stack[i],0,imgVoxCnt);
				break;
			case 1:
			case 2:
				System.arraycopy(aimShort, cPos, stack[i], 0, imgVoxCnt);
				break;
			case 3:
				System.arraycopy(aimFloat, cPos, stack[i], 0, imgVoxCnt);
				break;
			default:
				System.out
						.println("Hats dir im gring gschisse? So s'typ hans ned "
								+ imageType);
			}
			cPos += imgVoxCnt;
		}
		inSync = true;
		isLoaded = true;

	}

	/** create a copy of the desired file and return the path */
	protected String localLoadingRead(final String inpath) {
		if (!scratchLoading)
			return inpath;
		try {
			final File f = File.createTempFile("virtAIM-", "", new File(
					scratchDirectory));
			scratchFilename = scratchDirectory + "/" + f.getName();
			try {
				TIPLGlobal.copyFile(new File(inpath), f);
			} catch (final Exception e) {
				e.printStackTrace();
				scratchFilename = "";
				System.out.println("Could not write local file :"
						+ scratchFilename + ", proceeding normally with:"
						+ inpath);
				return inpath;
			}
		} catch (final Exception e) {
			e.printStackTrace();
			scratchFilename = "";
			System.out
					.println("Could not create local int :" + scratchDirectory
							+ ", proceeding normally with:" + inpath);
			return inpath;
		}
		System.out.println("Created Local File : " + scratchFilename);
		TIPLGlobal.DeleteTempAtFinish(scratchFilename);

		return scratchFilename;
	}

	/** how many cores does the plugin want (-1 = as many as possible) */
	public int neededCores() {
		final int readerCount = TIPLGlobal.getMaximumReaders();
		int cCore = readerCount;
		if ((5 * cCore) > (dim.z))
			cCore = (int) Math.ceil(dim.z / 5);
		if (cCore >readerCount)
			cCore = readerCount; // can't really make use of much
											// more than 4 processors due to
											// file system limitations
		if (cCore < 1)
			cCore = 1;
		// cCore=1; // no multithreading
		return cCore;
	}

	/** Fills in the important values from the first slice and using defaults */
	public void ParseAimHeader() {
		isConnected = true;
		ischGuet = true;

		if (fullAimLoaded) {
			// Aim is already fully loaded don't do anything
		} else {

			dataName = "Int";

			try {
				loadslice(0); // Load the first slice to have an idea about the
								// data.
			} catch (final Exception e) {
				System.out.println("Error Reading First Slice!!!");
				e.printStackTrace();
			}

			if (dataName.compareTo("Char") == 0)
				imageType = 0;
			if (dataName.compareTo("Short") == 0)
				imageType = 1;
			if (dataName.compareTo("Spec") == 0)
				imageType = 2;
			if (dataName.compareTo("Float") == 0)
				imageType = 3;
			if (dataName.compareTo("Mask") == 0)
				imageType = 10;

		}

		System.out.println("VirtualAim [" + dataName + "] v" + kVer
				+ " : Sample: " + sampleName + " Aim has been selected");

		// ImageJ code
		width = dim.x;
		height = dim.y;
		appendProcLog("Dimensions: " + dim);
		appendProcLog("Position: " + pos);
		appendProcLog("Element Size: " + elSize);
		appendProcLog("Image Type: " + imageType);

		getSliceCalls = 0;

	}

	/** Read in a tif-stack and return the decoder */
	public ImageDecoder parseTifStack(final boolean parseHeader) {
		try {
			final SeekableStream s = new FileSeekableStream(imglist[0]);

			final TIFFDecodeParam param = null;
			if (parseHeader) {
				try {
					final TIFFDirectory tifdir = new TIFFDirectory(s, 0);
					final TIFFField[] allfields = tifdir.getFields();
					
					TImgHeader inHeaders =TImgHeader.ReadHeaderFromTIFF(allfields);
					TImgTools.mirrorImage(inHeaders, this);
					
					tifStackHeaderRead = true;
				} catch (final Exception e) {
					System.out.println("ERROR LoadAim: TiffDirectory failed");
					e.printStackTrace();
					imglist = null;
				}
			}
			
			final ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s,
					param);

			if (parseHeader)
				System.out.println("Number of images in this TIFF: "
						+ dec.getNumPages());
			return dec;

		} catch (final Exception e) {
			System.out.println("ERROR LoadAim: Layered " + imglist[0]
					+ " cannot be opened? Does it exist");
			e.printStackTrace();
			imglist = null;
			return null;
		}
	}

	/** Show a preview of a given slice */
	public ImagePlus previewSlice() {
		return previewSlice(Math.round(dim.z / 2 + 1));
	}

	@Deprecated
	/** Show a preview of a given slice */
	public ImagePlus previewSlice(final int n) {
		if (previewDisabled)
			return null; // X windows can lick my ball and stop hard crashing
							// fuckin eh
		try {
			final ImagePlus curImPlus = TImgToImagePlus.MakeImagePlus(this);

			curImPlus.show();
			curImPlus.getProcessor().setMinAndMax(0, 255);
			return curImPlus;
		} catch (final Exception e) {
			System.out.println("Preview failed..." + sampleName + " - " + this);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Provide access to private slice matrices, needed for faster operation of
	 * some plugins but dangerous, please do not use
	 */
	@Deprecated
	public Object PrivateSliceAccess(final int slNum) throws Exception {
		return loadslice(slNum);
	}

	/** Reads in the given path and loads the first slice */
	public boolean ReadAim(final String inpath, final boolean onlyHeader) {
		int zlen = 0;
		// Initialize the Fields here so they can be filled
		pos = new D3int();
		offset = new D3int();
		elSize = new D3float(0.0014, 0.0014, 0.0014);

		ShortScaleFactor = 1.0f;
		isSigned = false;

		String spath = inpath.toUpperCase();
		String localpath = inpath;
		// Get rid of any semicolons stupid ass vms
		if (spath.lastIndexOf(";") > 0) {

			spath = spath.substring(0, (spath.lastIndexOf(";")));
		}

		if (inpath.length() < 1) {
			System.out.println("Name is empty, cannot be loaded");
			ischGuet = false;
			return false;
		}
		if (spath.endsWith(".TIF")) { // Read tiffdirectory -> one file
			layertiff = true;
			imglist = new File[1];
			// For tifdirectory files we can copy them locally first
			localpath = localLoadingRead(inpath);
			imglist[0] = new File(localpath);
			final ImageDecoder dec = parseTifStack(true);
			try {
				appendProcLog("Reading in Layered Tiff: " + inpath
						+ ", locally as " + localpath + ", layers:"
						+ dec.getNumPages());
				zlen = dec.getNumPages();
			} catch (final Exception e) {
				System.out.println("ERROR LoadAim: TIFS: " + inpath
						+ ", locally as " + localpath
						+ ", cannot be opened? Does it exist");
				e.printStackTrace();
				imglist = null;
				ischGuet = false;
				return false;
			}
			dim = new D3int(-1, -1, zlen);
		} else if (spath.endsWith("ISQ")) {
			// For isq files we can copy them locally first
			localpath = localLoadingRead(inpath);
			System.out.println("Reading in ISQ File: " + inpath
					+ ", locally as " + localpath);
			final ISQReader myISQ = new ISQReader();

			myISQ.SetupReader(localpath);
			myISQ.ReadHeader();

			appendProcLog("Reading in ISQ File: " + inpath + ", locally as "
					+ localpath + ", dimensions:" + myISQ.getDim());
			WrapTImg(myISQ.getImage());

			fullAimLoaded = false;
			imageType = 1;
			ischGuet = true;
			// myISQ=null;
			isSigned = false;
			TIPLGlobal.runGC();

			return true;
		} else {
			layertiff = false;
			final TImg cImg = DirectoryReader.ChooseBest(inpath).getImage();
			WrapTImg(cImg);
			return true;
		}

		if (zlen > 0) {
			aimPath = inpath;
			sampleName = inpath;
			ParseAimHeader();
			ischGuet = true;
			return true;
		} else {
			System.out.println("VirtualAim : ERROR Aim " + inpath
					+ ", locally as " + localpath + ", could not be read!");
			ischGuet = false;
			return false;
		}

	}

	public float readShortScaleFactor() {
		return (float) getScaleFactorFromProc();
	}

	/** multithreaded code for loading slices */
	public void runSliceLoader(final int nOperation, final int asType) {
		jStartTime = System.currentTimeMillis();
		// Call the other threads
		final Hashtable<sliceLoader, int[]> threadList = new Hashtable<sliceLoader, int[]>(
				neededCores());
		int[] mySlices;
		sliceLoader bgThread;
		for (int i = 0; i < neededCores(); i++) { // setup the background
													// threads
			mySlices = divideSlices(i);
			bgThread = new sliceLoader(this, asType, mySlices[0], mySlices[1]);
			threadList.put(bgThread, mySlices);
			bgThread.start();
		}
		// bgThread.join(); // run on current thread

		// Now check the others
		while (threadList.size() > 0) { // for all other threads:
			final sliceLoader theThread = threadList.keys().nextElement();
			try {
				theThread.join(); // wait until thread has finished
			} catch (final InterruptedException e) {
				System.out.println("ERROR-Thread : " + theThread
						+ " was interrupted, proceed carefully!");

			}
			threadList.remove(theThread); // and remove it from the list.
		}
		System.out.println("VA-MC Job Ran in "
				+ StrRatio(System.currentTimeMillis() - jStartTime, 1000)
				+ " seconds on " + neededCores() + " threads");
	}

	@Override
	public void setCompression(final boolean inData) {
		useCompression = inData;
	}

	/** The size of the image */
	@Override
	public void setDim(final D3int inData) {
		dim = inData;
	}

	/** The element size (in mm) of a voxel */
	@Override
	public void setElSize(final D3float inData) {
		elSize = inData;
	}

	/**
	 * The aim type of the image (0=char, 1=short, 2=int, 3=float, 10=bool, -1
	 * same as input)
	 */
	@Override
	public void setImageType(final int inData) {
		imageType = inData;
	}

	/**
	 * The size of the border around the image which does not contain valid
	 * voxel data
	 */
	@Override
	public void setOffset(final D3int inData) {
		offset = inData;
	}

	/**
	 * The position of the bottom leftmost voxel in the image in real space,
	 * only needed for ROIs
	 */
	@Override
	public void setPos(final D3int inData) {
		pos = inData;
	}

	public boolean setShortArray(final int n, final short[] gs) {
		if ((n < dim.z) && (imageType == 2) && (fullAimLoaded)) {
			final int sliceSize = dim.x * dim.y;
			final int outPos = n * sliceSize;
			ishortCopy(gs, outPos, sliceSize);
			return true;

		} else {
			System.out
					.println("Slice:" + n + " of VA:" + this + " type:"
							+ imageType
							+ " is not the appropriate usage for setShort!");
			return false;
		}

	}

	@Override
	public void setShortScaleFactor(final float ssf) {
		ShortScaleFactor = ssf;
	}

	@Override
	public void setSigned(final boolean inData) {
		isSigned = inData;
	}


	/** Show aim as a stack */
	public ImagePlus show() {
		final ImagePlus curImPlus = TImgToImagePlus.MakeImagePlus(this);
		ImageJ ijcore=new ImageJ();
		System.out.println("Show Aim... " + getDim().z);
		if (getDim().z > 0) {

			System.out.println("Created...");
			switch (imageType) {
			case 0:
				curImPlus.getProcessor().setMinAndMax(Byte.MIN_VALUE,
						Byte.MAX_VALUE);
				break;
			case 1:
				curImPlus.getProcessor().setMinAndMax(Short.MIN_VALUE,
						Short.MAX_VALUE);
			case 2:
				curImPlus.getProcessor().setMinAndMax(Integer.MIN_VALUE,
						Integer.MAX_VALUE);
				break;
			case 3:
				curImPlus.getProcessor().setMinAndMax(-Double.MAX_VALUE,
						Double.MAX_VALUE);
				break;
			case 10:
				curImPlus.getProcessor().setMinAndMax(0, 1);
				break;

			}

			// if (imp2.getType()==ImagePlus.GRAY16 ||
			// imp2.getType()==ImagePlus.GRAY32)

			// imp2.getProcessor().setMinAndMax(min, max);

			// imp2.setFileInfo(fi); // saves FileInfo of the first image
			// if (imp2.getStackSize()==1 && info1!=null)
			// imp2.setProperty("Info", info1);
			curImPlus.show();
			// curHistWind=new
			// HistogramWindow("Slice Histogram:",curImPlus,255,-Double.MAX_VALUE,Double.MAX_VALUE);
		}
		ijcore.show();
		TImgToImagePlus.waitForFrameClose(ijcore);
		return curImPlus;
	}

	/**
	 * Deletes the stack information from memory and marks the stack as
	 * unloaded, good for saving memory
	 */
	public void unloadStack() {
		stack = null;
		isLoaded = false;
		TIPLGlobal.runGC();
	}

	/**
	 * Generic code for wrapping a TImg
	 * 
	 * @param inTImg
	 */
	public void WrapTImg(final TImgRO inTImg) {
		loadAimfromTImg(inTImg);
		// This is sort of a hack, but it needs to be done, VirtualAims should
		// be copied the same way everything else is
		TImgTools.mirrorImage(inTImg, this);
		GenerateHeader(inTImg.getDim(), inTImg.getOffset(), inTImg.getPos(),
				inTImg.getElSize());
	}

	/**
	 * Write the output using the default settings for the given data-type
	 */
	public void WriteAim(final String outpath) {
		WriteAim(outpath, -1);
	}

	/**
	 * A bit confusing here since there are two different type integers being
	 * thrown around
	 * 
	 * @param outpath
	 *            The path of the file to be saved (.tif indicates one tif file
	 *            instead of a directory containing many tiff files)
	 * @param outType
	 *            represents the VirtualAim notion of type (0=char, 1=short,
	 *            2=int, 3=float, 10=bool, -1 same as input) <li>The internal
	 *            cType is the java image libraries type notation described
	 *            fully in the BufferedImage documentation
	 */
	public void WriteAim(final String outpath, final int outType) {
		int biType;
		TWriter outWriter;
		if (outType == -1)
			biType = imageType; // -1 means use input type
		else
			biType = outType;

		if (showPreview)
			previewSlice();

		if (outpath.length() < 1) {
			System.out
					.println("Output name is too short, Aim will not be written!!!");
			return;
		}

		int cType = BufferedImage.TYPE_CUSTOM;

		if (biType == 0)
			cType = BufferedImage.TYPE_BYTE_GRAY;
		if (biType == 1)
			cType = BufferedImage.TYPE_USHORT_GRAY;
		if (biType == 2)
			cType = BufferedImage.TYPE_USHORT_GRAY;
		if (biType == 3)
			cType = BufferedImage.TYPE_CUSTOM; // Since we cant write 32bit
												// floats, lets fake it
		if (biType == 10)
			cType = BufferedImage.TYPE_BYTE_GRAY;
		// if (biType==10) cType=BufferedImage.TYPE_BYTE_BINARY; // not
		// compatible with imagej output
		String plPath;
		if (outpath.endsWith(".raw") || outpath.endsWith(".RAW")) {
			System.out.println("Writing raw: " + outpath);
			outWriter = new RAWWriter(biType);
			outWriter.SetupWriter(this, outpath);
			outWriter.Write();
			plPath = outpath + ".pl.txt";
		} else if (outpath.endsWith(".tif") || outpath.endsWith(".TIF")) {
			// Layered Tiff
			try {
				System.out.println("Writing layered-tiff: " + outpath);
				final OutputStream os = new FileOutputStream(outpath);
				final TIFFEncodeParam tparam = new TIFFEncodeParam();
				// To Disable compression completely
				// useCompression=false;
				// useHighCompression=false;

				if (useHighCompression)
					tparam.setCompression(TIFFEncodeParam.COMPRESSION_DEFLATE);
				else if (useCompression)
					tparam.setCompression(TIFFEncodeParam.COMPRESSION_PACKBITS);

				final ImageEncoder encoder = ImageCodec.createImageEncoder(
						"tiff", os, tparam);
				final List<BufferedImage> imageList = new ArrayList<BufferedImage>();
				for (int n = 1; n < dim.z; n++) {
					imageList.add(aimSlice(n, cType));
				}
				tparam.setExtraImages(imageList.iterator());
				TIPLGlobal.runGC();
				
				
				// Assign TiffHeaders
				TImgHeader.ReadHeadersFromTImg(this).writeToTIFF(tparam);
				// Write image
				encoder.encode(aimSlice(0, cType));
				os.close();
			} catch (final Exception e) {
				System.out.println("Cannot write layered-tiff " + outpath);
				e.printStackTrace();
			}

			plPath = outpath + ".pl.txt";
		} else { // Tiff Stack
			final boolean makeFolder = (new File(outpath)).mkdir();
			if (makeFolder) {
				System.out.println("Directory: " + outpath + " created");
			}

			for (int n = 0; n < dim.z; n++) {
				final String coutName = outpath + "/"
						+ String.format("%04d", n) + ".tif";
				if (n == 0)
					System.out.println("Writing: " + coutName);
				try {
					// File imageFile = new File(coutName);

					final OutputStream os = new FileOutputStream(coutName);

					final TIFFEncodeParam tparam = new TIFFEncodeParam();
					/**
					 * No compression on individual slices , yet! if
					 * (useHighCompression)
					 * tparam.setCompression(TIFFEncodeParam
					 * .COMPRESSION_DEFLATE); else if (useCompression)
					 * tparam.setCompression
					 * (TIFFEncodeParam.COMPRESSION_PACKBITS);
					 */

					// Assign TiffHeaders
					TImgHeader.ReadHeadersFromTImg(this).writeToTIFF(tparam);

					final ImageEncoder encoder = ImageCodec.createImageEncoder(
							"tiff", os, tparam);

					encoder.encode(aimSlice(n, cType));
					os.close();

					/*
					 * Old Version if (false) { ImageIO.write(aimSlice(n,cType),
					 * "tif", imageFile); imageFile=null; }
					 */
				} catch (final Exception e) {
					System.out.println("Cannot write slice " + n);
					e.printStackTrace();
				}
				TIPLGlobal.runGC();
			}
			plPath = outpath + "/procLog.txt";
		}
		try {
			// Write the procedure log to a text file
			final FileWriter fstream = new FileWriter(plPath);
			final BufferedWriter out = new BufferedWriter(fstream);
			out.write(getProcLog());
			// Close the output stream
			out.close();
		} catch (final Exception e) {// Catch exception if any
			System.out.println("Error: " + e.getMessage());
			System.out.println(getProcLog());
		}
	}
	public void WriteAim(final String outpath, final int outType,
			final float scaleVal) {
		ShortScaleFactor = scaleVal;
		WriteAim(outpath, outType);
	}

	/**
	 * A bit confusing here since there are two different type integers being
	 * thrown around
	 * 
	 * @param outpath
	 *            The path of the file to be saved (.tif indicates one tif file
	 *            instead of a directory containing many tiff files)
	 * @param outType
	 *            represents the VirtualAim notion of type (0=char, 1=short,
	 *            2=int, 3=float, 10=bool, -1 same as input) <li>The internal
	 *            cType is the java image libraries type notation described
	 *            fully in the BufferedImage documentation
	 * @param scaleVal
	 *            is the value used to scale float images into short / int /
	 *            char images
	 * @param IisSigned
	 *            indicates whether or not the data is representing a signed
	 *            quantity
	 */
	public void WriteAim(final String outpath, final int outType,
			final float scaleVal, final boolean IisSigned) {
		ShortScaleFactor = scaleVal;
		isSigned = IisSigned;
		WriteAim(outpath, outType);
	}

}
