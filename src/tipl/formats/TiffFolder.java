/**
 * 
 */
package tipl.formats;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.media.jai.PlanarImage;

import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

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
		public TSliceReader ReadFile(final File infile) throws IOException {
			return new TIFSliceReader(infile);
		}
	}

	public static class TIFSliceReader extends SliceReader {
		public Raster activeRaster;

		public TIFSliceReader(final File infile) throws IOException {
			final FileInputStream in = new FileInputStream(infile);
			final FileChannel channel = in.getChannel();
			final ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
			channel.read(buffer);
			in.close();
			final SeekableStream stream = new ByteArraySeekableStream(
					buffer.array());
			final String[] names = ImageCodec.getDecoderNames(stream);
			final ImageDecoder dec = ImageCodec.createImageDecoder(names[0],
					stream, null);
			final RenderedImage im = dec.decodeAsRenderedImage();
			
			SetupFromRenderImage(im);
		}

		public TIFSliceReader(final RenderedImage im) throws IOException {
			SetupFromRenderImage(im);
		}

		@Override
		public Object polyReadImage(final int asType) throws IOException {
			
			switch (imageType) {
			case TImgTools.IMAGETYPE_CHAR: // Char use the interface for short with a different max val
			case TImgTools.IMAGETYPE_INT: // Int
			case TImgTools.IMAGETYPE_BOOL: // binary also uses the same reader
				
				int[]  gi =  new int[sliceSize];
				if (readAsByte) {
					// how to handle a 24 bit color image, just take the red channel
					byte[] gb=( (DataBufferByte) activeRaster.getDataBuffer()).getData();
					for(int i=0;i<sliceSize;i++) gi[i]=gb[3*i];
				} else {
					gi = activeRaster.getPixels(0, 0, activeRaster.getWidth(),
						activeRaster.getHeight(), gi);
				}
				
				// System.out.println("Getting pixels:"+dataType+", converting to:"+asType+", status:"+gi);
				return TImgTools.convertIntArray(gi, asType, useSignedConversion, 1, maxVal);
			case TImgTools.IMAGETYPE_FLOAT: // Float
				float[] gf = new float[sliceSize];
				gf = activeRaster.getPixels(0, 0, activeRaster.getWidth(),
						activeRaster.getHeight(), gf);
				// System.out.println("Getting pixels:"+dataType+", converting to:"+asType+", status:"+gf);
				return TImgTools.convertFloatArray(gf, asType, useSignedConversion, 1);
			default:
				throw new IOException("Input file format is not known!!!!");
			}
		}
		/**
		 * should the conversions be signed
		 */
		protected boolean useSignedConversion=false;
		protected boolean readAsByte=false;
		private void SetupFromRenderImage(final RenderedImage im)
				throws IOException {
			readAsByte=false;
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
				
			case 24: // sometimes ok for jpeg images
				imageType=TImgTools.IMAGETYPE_CHAR;
				maxVal=255;
				readAsByte=true;
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

			activeRaster = bim.getData();
			dim = new D3int(activeRaster.getWidth(), activeRaster.getHeight(),
					1);

			// number of pixels in a slice
			sliceSize = activeRaster.getWidth() * activeRaster.getHeight();
		}

	}

	final static String version = "26-02-2014";
	final static public FileFilter tifFilter = new FileFilter() {
		@Override
		public boolean accept(final File file) {
			if (file.getAbsolutePath().endsWith(".tif"))
				return true;
			if (file.getAbsolutePath().endsWith(".TIF"))
				return true;
			if (file.getAbsolutePath().endsWith(".tiff"))
				return true;
			if (file.getAbsolutePath().endsWith(".TIFF"))
				return true;
			return false;
		}
	};

	@DirectoryReader.DReader(name = "tiff")
	final public static DRFactory readerFactory = new DRFactory() {
		@Override
		public DirectoryReader get(final String path) {
			try {
				return new TiffFolder(path,tifFilter,"tiff");
			} catch (final Exception e) {
				System.out.println("Error converting or reading slice");
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public FileFilter getFilter() {
			return TiffFolder.tifFilter;
		}
	};
	
	final static public FileFilter jpegFilter = new FileFilter() {
		@Override
		public boolean accept(final File file) {
			if (file.getAbsolutePath().endsWith(".jpg"))
				return true;
			if (file.getAbsolutePath().endsWith(".JPG"))
				return true;
			if (file.getAbsolutePath().endsWith(".jpeg"))
				return true;
			if (file.getAbsolutePath().endsWith(".JPEG"))
				return true;
			return false;
		}
	};
	@DirectoryReader.DReader(name = "jpeg")
	final public static DRFactory jpreaderFactory = new DRFactory() {
		@Override
		public DirectoryReader get(final String path) {
			try {
				return new TiffFolder(path,jpegFilter,"jpeg");
			} catch (final Exception e) {
				System.out.println("Error converting or reading slice");
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public FileFilter getFilter() {
			return TiffFolder.jpegFilter;
		}
	};
	
	public static void main(final ArgumentParser p) {
		System.out.println("TiffFolder Tool v" + VirtualAim.kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final String inputFile = p.getOptionString("input", "",
				"Aim File to Convert");
		final String outputFile = p.getOptionString("output", "test.tif",
				"Aim File to Convert");
		try {
			final TiffFolder inputAim = new TiffFolder(inputFile,tifFilter,"main");
			final VirtualAim bob = new VirtualAim(inputAim.getImage());
			bob.WriteAim(outputFile);
		} catch (final Exception e) {
			System.out.println("Error converting or reading slice");
			e.printStackTrace();
		}

	}

	public static void main(final String[] args) {
		main(TIPLGlobal.activeParser(args));
	}
	/** 
	 * operating as a tiff or jpeg folder
	 */
	protected final String pluginMode;
	public TiffFolder(final String path,final FileFilter ffilter,final String mode) throws IOException {
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

}
