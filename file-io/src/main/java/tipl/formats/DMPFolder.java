/**
 * 
 */
package tipl.formats;

import java.io.*;

import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;
import tipl.util.TypedPath;
import tipl.util.TypedPath.PathFilter;

/**
 * @author maderk
 * 
 */
public class DMPFolder extends DirectoryReader {

	private static class DMPSliceFactory implements TSliceFactory {
		@Override
		public TSliceReader ReadFile(final TypedPath infile) throws IOException {
			return new DMPSliceReader(infile);
		}
	}

	public static class DMPSliceReader extends SliceReader {
		final int byteCount;
		final InputStream is;

		public DMPSliceReader(final TypedPath infile) throws IOException {
			is = infile.getFileObject().getInputStream();
			final byte[] buf = new byte[6]; // we're about to read the first 6
											// bytes
			is.read(buf, 0, 6);
			// one reason I hate java: there's no such thing as a uint16 data
			// type. To work around this, we have to do some gymnastics:
			int firstByte = 0;
			int secondByte = 0;
			firstByte = (0x000000FF & (buf[0]));
			secondByte = (0x000000FF & (buf[1]));
			dim = new D3int(0, 0, 1);
			dim.x = (secondByte << 8 | firstByte);
			// because I never could remember that even a single day, here
			// are some explanations:
			// first we are promoting a signed byte to an int, which
			// results in bits 8 through 31 set to 1 if the number happens
			// to be larger than 127. the bitwise and with 0x000000FF wipes
			// out all but the first 8 bits.
			// The last line deals with endianness: the binary file is
			// little endian, while java is in general big endian.
			// System.out.println(fi.width);
			firstByte = (0x000000FF & (buf[2]));
			secondByte = (0x000000FF & (buf[3]));
			dim.y = (secondByte << 8 | firstByte);
			// System.out.println(fi.height);

			imageType = 3;
			sliceSize = dim.x * dim.y;
			byteCount = (dim.x) * dim.y * 4;

		}

		@Override
		public Object polyReadImage(final int asType) throws IOException {
			final float[] gf = new float[sliceSize];
			final int bufferSize = sliceSize * 4;
			final byte[] buffer = new byte[bufferSize];

			int bufferCount = 0;
			int totalRead = 0;
			while (bufferCount < bufferSize) { // fill the buffer
				final int count = is.read(buffer, bufferCount, bufferSize
						- bufferCount);
				if (count == -1) {
					if (bufferCount > 0)
						for (int i = bufferCount; i < bufferSize; i++)
							buffer[i] = 0;
					totalRead = byteCount;
					throw new IOException("Current Slice is Corrupt: "
							+ totalRead + " of " + bufferSize);
				}
				bufferCount += count;
			}
			int j = 0;
			final int base = 0;
			for (int i = base; i < sliceSize; i++) {
				final int tmp = ((buffer[j + 3] & 0xff) << 24)
						| ((buffer[j + 2] & 0xff) << 16)
						| ((buffer[j + 1] & 0xff) << 8) | (buffer[j] & 0xff);
				gf[i] = Float.intBitsToFloat(tmp);
				j += 4;
			}
			return TImgTools.convertArrayType(gf,TImgTools.IMAGETYPE_FLOAT, asType, true, 1);
		}

	}

	final static String version = "08-29-2014";


	@DirectoryReader.DReader(name = "DMP"
			)
    final public static class dmpReaderFactory extends DRFactory  {
		@Override
		public DirectoryReader get(final TypedPath path) {
			try {
				return new DMPFolder(path);
			} catch (final Exception e) {
				System.out.println("Error converting or reading slice:"+path);
				e.printStackTrace();
				throw new IllegalArgumentException(e+" could not be read: "+path);
			}
		}

		@Override
		public TSliceReader getSliceReader(TypedPath slice) {
			try {
				return new DMPSliceReader(slice);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(e+" could not be read: "+slice);
			}
		}

		final static public TypedPath.PathFilter dmpFilter = new TypedPath.PathFilter.
				ExtBased("dmp");

		@Override
		public PathFilter getFilter() {
			return dmpFilter;
		}
	};


	public DMPFolder(final TypedPath path) throws IOException {
		super(path,dmpReaderFactory.dmpFilter, new DMPSliceFactory());

	}

	@Override
	public int getImageType() {
		return TImgTools.IMAGETYPE_FLOAT;
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
		return "DMP-Folder-Reader " + version;
	}

	@Override
	public void SetupReader(TypedPath inPath) {
		//TODO maybe add some setup code (if its needed)
	}

}
