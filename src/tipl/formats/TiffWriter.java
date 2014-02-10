package tipl.formats;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

import tipl.util.D3int;
import tipl.util.TImgBlock;
import tipl.util.TImgTools;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codecimpl.util.DataBufferFloat;
import com.sun.media.jai.codecimpl.util.RasterFactory;
/**
 * A function for writing images based on the slices inside. 
 * @author mader
 *
 */
public class TiffWriter implements TSliceWriter {
	@TSliceWriter.DWriter(name = "Tiff Folder",type="tif")
	final public static DWFactory myFactory = new DWFactory() {
		@Override
		public TSliceWriter get(final TImgRO outFile,final String path,int outType) {
			TSliceWriter outWriter=new TiffWriter();
			outWriter.SetupWriter(outFile, path, outType);
			return outWriter;
		}
	};
	protected String plPath="";

	protected String outpath="";
	protected D3int dim=new D3int(-1,-1,1);
	protected int cType=-1;
	protected TImgHeader theader;
	/** 
	 * output file format
	 */
	protected int biType=2;
	protected boolean isSigned=true;
	public static boolean writeFailureThrowsError=true;
	
	@Override
	public void SetupWriter(TImgRO imageToSave, String outputPath, int outType) {
		final boolean makeFolder = (new File(outputPath)).mkdir();
		if (makeFolder) {
			System.out.println("Directory: " + outputPath + " created");
		}
		outpath=outputPath;
		plPath = outputPath + "/procLog.txt";
		dim=imageToSave.getDim();
		
		if (outType==-1) biType=imageToSave.getImageType();
		else biType=outType;
		
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

		theader=TImgHeader.ReadHeadersFromTImg(imageToSave);
	}

	@Override
	public void WriteHeader() {
		// Write the procedure log to a text file
		final FileWriter fstream;
		final BufferedWriter out;
		try {
			fstream = new FileWriter(plPath);
			out = new BufferedWriter(fstream);
			out.write(theader.getProcLog());
			// Close the output stream
			out.close();
			fstream.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error, Header for "+plPath+" could not be written");
		}
	}

	@Override
	public String writerName() {
		return "TiffWriter";
	}

	@Override
	public void WriteSlice(TImgBlock outSlice, int outSlicePosition) {
		final String coutName = outpath + "/"
				+ String.format("%04d", outSlicePosition) + ".tif";
		if (outSlicePosition == 0)
			System.out.println("Writing: " + coutName);
		try {

			final OutputStream os = new FileOutputStream(coutName);

			final TIFFEncodeParam tparam = new TIFFEncodeParam();
			
			theader.writeToTIFF(tparam);


			final ImageEncoder encoder = ImageCodec.createImageEncoder(
					"tiff", os, tparam);
			
			encoder.encode(sliceAsImage(outSlice));
			os.close();
		} catch (final Exception e) {
			System.err.println("Cannot write slice " + outSlicePosition);
			e.printStackTrace();
			if (writeFailureThrowsError) throw new IllegalArgumentException(e+"Cant write file at "+coutName+":"+outSlicePosition);
			
		}

	}
	
	/**
	 * Creates a buffered image for the given slice which can be used to save as
	 * tiff
	 * 
	 * @param n
	 *            slice number
	 * @param cType
	 *            type of image to buffer
	 * @return a bufferedimage
	 */
	protected BufferedImage sliceAsImage(final TImgBlock in) {
		Object curSliceData=in.get();
		final int imageType=TImgTools.identifySliceType(curSliceData);
		
		int maxVal = 255;
		if (cType == BufferedImage.TYPE_BYTE_GRAY)
			maxVal = 127;
		if (cType == BufferedImage.TYPE_USHORT_GRAY)
			maxVal = 65536;
		if (cType == BufferedImage.TYPE_BYTE_BINARY)
			maxVal = 255;
		final int sliceLen = dim.x * dim.y;
		//final int outType;
		if (cType==BufferedImage.TYPE_CUSTOM) {
			final int outType=TImgTools.IMAGETYPE_FLOAT;
			float[] fpixels = (float[]) TImgTools.convertArrayType(curSliceData, imageType, outType, isSigned, theader.getShortScaleFactor(), maxVal);
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
			final WritableRaster raster = RasterFactory
					.createWritableRaster(sampleModel, dataBuffer,
							new Point(0, 0));

			return new BufferedImage(colorModel, raster, false, null);
		} else {
			final int outType=TImgTools.IMAGETYPE_INT;
			int[] pixels = (int[]) TImgTools.convertArrayType(curSliceData, imageType, outType, isSigned,  theader.getShortScaleFactor(), maxVal);
			BufferedImage image = new BufferedImage(dim.x, dim.y, cType);
			final WritableRaster raster = (WritableRaster) image.getData();
			raster.setPixels(0, 0, dim.x, dim.y, pixels);
			image.setData(raster);
			return image;
		} 	
	}

}
