package tipl.formats;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;

import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * Reads raw 8-bit, 16-bit or 32-bit (float or RGB) images from a stream or URL.
 * 
 * <pre>
 * B. Koller, SCANCO Medical AG, April 2005 ImageJ plugin
 *   This plugin implements the Acquire/ISQ command.
 * Newer version to find parameters ( Michael Gerber, 05-03-2003)
 */
public class ISQReader implements TReader {
	public static class ISQSliceReader extends SliceReader {
		private final RandomAccessFile in;
		final long startPos;
		final ISQReader curReader;

		public ISQSliceReader(final RandomAccessFile infile,
				final int sliceNumber, final ISQReader icurReader)
				throws IOException {
			in = infile;
			curReader = icurReader;
			dim = curReader.getDim();
			pos = curReader.getPos();
			elSize = curReader.getElSize();
			long nPos = ((long) dim.x) * dim.y * bytesPerPixel;
			nPos *= sliceNumber;
			nPos += curReader.fileOffset;
			//System.out.println("Read Slice " + sliceNumber + " : " + nPos+ ", Currently :" + in.getFilePointer());
			in.seek(nPos);
			startPos = in.getFilePointer();
		}

		@Override
		public Object polyReadImage(final int asType) throws IOException {
			if (in.getFilePointer() != startPos)
				in.seek(startPos);

			int pixelsRead;
			final byte[] buffer = new byte[curReader.bufferSize];
			long totalRead = 0L;
			int base = 0; // file position nPixels*iSlice;
			int count;
			int bufferCount;
			final short[] cSlice = new short[curReader.nPixels];
			int bufferSize = curReader.bufferSize;

			while (totalRead < curReader.byteCount) {
				if ((totalRead + bufferSize) > curReader.byteCount)
					bufferSize = (int) (curReader.byteCount - totalRead);
				bufferCount = 0;
				while (bufferCount < bufferSize) { // fill the buffer
					count = in.read(buffer, bufferCount, bufferSize
							- bufferCount);
					if (count == -1) {
						if (bufferCount > 0)
							for (int i = bufferCount; i < bufferSize; i++)
								buffer[i] = 0;
						totalRead = curReader.byteCount;
						curReader.eofError();
						break;
					}
					bufferCount += count;
				}
				totalRead += bufferSize;
				pixelsRead = bufferSize / bytesPerPixel;

				for (int i = base, j = 0; i < (base + pixelsRead); i++, j += 2) {

					cSlice[i] = ((short) (((buffer[j + 1] & 0xff) << 8) | (buffer[j] & 0xff)));

					if (cSlice[i] > curReader.max)
						curReader.max = cSlice[i];
					if (cSlice[i] < curReader.min)
						curReader.min = cSlice[i];
					curReader.mean += cSlice[i];

				}
				base += pixelsRead;
			}
			return TImgTools.convertArrayType(cSlice,TImgTools.IMAGETYPE_SHORT, asType, true, 1, maxVal);
		}

	}

	int record = 0;
	int recCount = 0;
	public int i, fileOffset, fileOffset1, fileOffset2, fileOffset3,
			xdimension, ydimension, zdimension;

	int tmpInt;
	public float el_size_mm_x, el_size_mm_y, el_size_mm_z;
	float tmp_float;
	File iFile;
	// TImg iImg;
	private int width, height;
	protected long skipCount;
	protected int bufferSize, nPixels;
	protected long byteCount;

	public double min, max, mean; // readRGB48() calculates min/max pixel values
	public boolean headerRead = false;
	protected D3int dim = new D3int(0);
	private final D3int pos = new D3int(0);
	private D3int off = new D3int(0);
	private D3float elSize = new D3float(0.0f, 0.0f, 0.0f);
	/** Each thread gets its own RandomAccessFile */
	protected Hashtable<Thread, RandomAccessFile> fileForThread = new Hashtable<Thread, RandomAccessFile>(
			5);
	public final static int bytesPerPixel = 2;

	public ISQReader() {
	}

	void eofError() {
	}

	@Override
	public D3int getDim() {
		return dim;
	}

	@Override
	public D3float getElSize() {
		return elSize;
	}

	@Override
	public TImg getImage() {
		return new TReader.TReaderImg(this);
	}

	@Override
	public int getImageType() {
		return 1;
	} // only short aims are supported right now

	@Override
	public D3int getPos() {
		return pos;
	}

	@Override
	public String getProcLog() {
		return readerName() + ": Opened: " + iFile.getPath() + "\n";
	}

	protected RandomAccessFile getRAFile() throws IOException {
		final Thread myThread = Thread.currentThread();
		if (fileForThread != null && fileForThread.containsKey(myThread)) { // RAIo
																			// already
																			// exists
			return fileForThread.get(myThread);
		} else {
			System.out.println("New File for " + myThread
					+ " has been opened...");
			final RandomAccessFile curFile = new RandomAccessFile(iFile, "r");
			fileForThread.put(myThread, curFile);
			return curFile;
		}

	}

	@Override
	public float getShortScaleFactor() {
		return 1.0f;
	}

	@Override
	public boolean getSigned() {
		return false;
	}

	@Override
	public boolean isParallel() {
		return true;
	}

	@Override
	public String readerName() {
		return "ISQ_Reader";
	}

	@Override
	public void ReadHeader() {
		try {
			final FileInputStream p = new FileInputStream(iFile);

			p.skip(44);
			xdimension = p.read() + p.read() * 256 + p.read() * 65536;
			width = xdimension;
			p.skip(1);
			ydimension = p.read() + p.read() * 256 + p.read() * 65536;
			height = ydimension;
			p.skip(1);
			zdimension = p.read() + p.read() * 256 + p.read() * 65536;
			p.skip(1);

			tmpInt = (p.read() + p.read() * 256 + p.read() * 65536 + p.read() * 256 * 65536);
			el_size_mm_x = ((float) tmpInt) / ((float) xdimension);
			tmpInt = (p.read() + p.read() * 256 + p.read() * 65536 + p.read() * 256 * 65536);
			el_size_mm_y = ((float) tmpInt) / ((float) ydimension);
			tmpInt = (p.read() + p.read() * 256 + p.read() * 65536 + p.read() * 256 * 65536);
			el_size_mm_z = ((float) tmpInt) / ((float) zdimension);

			el_size_mm_x = el_size_mm_x / 1000;
			el_size_mm_y = el_size_mm_y / 1000;
			el_size_mm_z = el_size_mm_z / 1000;

			p.skip(440);

			fileOffset = (p.read() + p.read() * 256 + p.read() * 65536 + 1) * 512;

			p.close();
			System.out.println("el_size x (in mm): " + el_size_mm_x
					+ "\nel_size y (in mm): " + el_size_mm_y
					+ "\nel_size z (in mm): " + el_size_mm_z);
			System.out.println("fileOffset: " + fileOffset + "\nxdimension: "
					+ xdimension + "\nydimension: " + ydimension
					+ "\nzdimension: " + zdimension);
			dim = new D3int(xdimension, ydimension, zdimension);
			elSize = new D3float(el_size_mm_x, el_size_mm_y, el_size_mm_z);
			off = new D3int(0);

			nPixels = xdimension * ydimension;
			byteCount = ((long) width) * height * bytesPerPixel;
			bufferSize = (int) (byteCount / 25L);
			if (bufferSize < 8192)
				bufferSize = 8192;
			else
				bufferSize = (bufferSize / 8192) * 8192;
			headerRead = true;

		} catch (final Exception e) {
			System.err.println("ISQ is not valid...");
			e.printStackTrace();
		}

	}

	@Override
	public TSliceReader ReadSlice(final int iSlice) { // implicit random
		try {
			return new ISQSliceReader(getRAFile(), iSlice, this);
		} catch (final Exception e) {
			e.printStackTrace();
			throw new IllegalStateException("Invalid Slice to Read from ISQ "
					+ iSlice);
		}
	}

	@Override
	public void SetupReader(final String inPath) {
		try {
			iFile = new File(inPath);
		} catch (final Exception e) {
			System.err.println("Given path:" + inPath + " is not valid...");
			e.printStackTrace();
		}

	}

}