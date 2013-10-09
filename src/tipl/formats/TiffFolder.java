/**
 * 
 */
package tipl.formats;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.media.jai.PlanarImage;

import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TImgTools;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;

/**
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
		private Raster activeRaster;

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
			case 0: // Char use the interface for short with a different max val
			case 2: // Int
			case 10: // binary also uses the same reader
				int[] gi = new int[sliceSize];
				gi = activeRaster.getPixels(0, 0, activeRaster.getWidth(),
						activeRaster.getHeight(), gi);
				// System.out.println("Getting pixels:"+dataType+", converting to:"+asType+", status:"+gi);
				return TImgTools.convertIntArray(gi, asType, true, 1, maxVal);
			case 3: // Float
				float[] gf = new float[sliceSize];
				gf = activeRaster.getPixels(0, 0, activeRaster.getWidth(),
						activeRaster.getHeight(), gf);
				// System.out.println("Getting pixels:"+dataType+", converting to:"+asType+", status:"+gf);
				return TImgTools.convertFloatArray(gf, asType, true, 1);
			default:
				throw new IOException("Input file format is not known!!!!");
			}
		}

		private void SetupFromRenderImage(final RenderedImage im)
				throws IOException {
			switch (im.getColorModel().getPixelSize()) {
			case 1: // Boolean
				imageType = 10;
				maxVal = 1;
				break;
			case 8: // Char
				imageType = 0;
				maxVal = 255;
				break;
			case 16: // Integer
				imageType = 2;
				maxVal = 65536;
				break;
			case 32: // Float
				imageType = 3;
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

	final static String version = "08-03-2013";
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
	final public static DRFactory myFactory = new DRFactory() {
		@Override
		public DirectoryReader get(final String path) {
			try {
				return new TiffFolder(path);
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
	static {
		DirectoryReader.Register(tifFilter, myFactory);

	}

	public static void main(final ArgumentParser p) {
		System.out.println("TiffFolder Tool v" + VirtualAim.kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final String inputFile = p.getOptionString("input", "",
				"Aim File to Convert");
		final String outputFile = p.getOptionString("output", "test.tif",
				"Aim File to Convert");
		try {
			final TiffFolder inputAim = new TiffFolder(inputFile);
			final VirtualAim bob = new VirtualAim(inputAim.getImage());
			bob.WriteAim(outputFile);
		} catch (final Exception e) {
			System.out.println("Error converting or reading slice");
			e.printStackTrace();
		}

	}

	public static void main(final String[] args) {
		main(new ArgumentParser(args));
	}

	public TiffFolder(final String path) throws IOException {
		super(path, tifFilter, new TiffSliceFactory());

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
		return "Tiff-Folder-Reader " + version;
	}

}
