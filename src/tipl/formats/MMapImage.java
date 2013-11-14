/**
 * 
 */
package tipl.formats;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.ITIPLStorage;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

/**
 * A class to write image data to a memory map and read it from there as well
 * 
 * @author maderk
 * 
 */
public class MMapImage implements TImg, TReader, TWriter {
	private static class ByteBufferMaker implements Callable<MappedByteBuffer> {
		final long offset, size;
		final boolean fillSlice;
		final FileChannel fc;
		final TImgRO image;
		final int slice;
		final int type;

		public ByteBufferMaker(final FileChannel inFc, final long cOffset,
				final long sliceSize) {
			offset = cOffset;
			size = sliceSize;
			fillSlice = false;
			fc = inFc;
			slice = -1;
			type = -1;
			image = null;
		}

		public ByteBufferMaker(final FileChannel inFc, final long cOffset,
				final long sliceSize, final TImgRO readImage,
				final int sliceNumber) {
			offset = cOffset;
			size = sliceSize;
			fillSlice = true;
			fc = inFc;
			type = readImage.getImageType();
			image = readImage;
			slice = sliceNumber;
		}

		@Override
		public MappedByteBuffer call() {
			try {
				final MappedByteBuffer cSlice = fc.map(
						FileChannel.MapMode.READ_WRITE, offset, size);
				if (fillSlice)
					putSlice(cSlice, image.getPolyImage(slice, type), type);
				return cSlice;
			} catch (final IOException ioe) {
				ioe.printStackTrace(); // Convert an IO exception to a illegal
										// argument (execution would be more
										// elagant but doesn't work)
				throw new IllegalArgumentException("IO Error on Slice" + offset
						/ size);
			}
		}
	}

	public static MMapImage EmptyMap(final String fileName, final int x,
			final int y, final int z, final int asType) throws IOException {
		assert TImgTools.isValidType(asType);
		final MMapImage mp = new MMapImage(fileName, new D3int(x, y, z), asType);
		return mp;
	}

	private static void init(final String filename) {
		TIPLGlobal.DeleteTempAtFinish(filename);
	}

	/**
	 * Factory method to create a MMapImage from a TImg
	 * 
	 * @param fileName
	 * @param inImage
	 * @return the MMap image
	 * @throws IOException
	 */
	public static MMapImage MMapFromTImg(final String fileName,
			final TImgRO inImage) throws IOException {
		final MMapImage mp = new MMapImage(fileName, inImage);
		mp.setPos(inImage.getPos());
		mp.setOffset(inImage.getOffset());
		mp.setElSize(inImage.getElSize());
		return mp;
	}

	protected static MappedByteBuffer putSlice(final MappedByteBuffer curMap,
			final Object iSlice, final int asType) {
		switch (asType) {
		case TImgTools.IMAGETYPE_BOOL:
			for (final boolean cVal : (boolean[]) iSlice)
				curMap.putChar((cVal ? (char) 127 : (char) 0));
			break;
		case TImgTools.IMAGETYPE_CHAR:
			for (final char cVal : (char[]) iSlice)
				curMap.putChar(cVal);
			break;
		case TImgTools.IMAGETYPE_SHORT:
			for (final short sVal : (short[]) iSlice)
				curMap.putShort(sVal);
			break;
		case TImgTools.IMAGETYPE_INT:
			for (final int iVal : (int[]) iSlice)
				curMap.putInt(iVal);
			break;
		case TImgTools.IMAGETYPE_FLOAT:
			for (final float fVal : (float[]) iSlice)
				curMap.putFloat(fVal);
			break;

		}
		return curMap;
	}

	// private Object getRawSlice(final int iSliceNumber) {
	private final RandomAccessFile raf;
	private final List<Future<MappedByteBuffer>> mappings = new ArrayList<Future<MappedByteBuffer>>();
	private final D3int dim;
	private D3float elSize = new D3float(1.0f, 1.0f, 1.0f);
	private D3int pos = new D3int(0);
	private D3int offset = new D3int(0);
	private final int type;
	private final long typeSize;

	private final int sliceElements;

	private MMapImage(final String filename, final D3int inDim, final int inType)
			throws IOException {
		this.raf = new RandomAccessFile(filename, "rw");
		init(filename);
		dim = inDim;
		type = inType;
		typeSize = TImgTools.typeSize(inType);
		sliceElements = dim.x * dim.y;
		final long sliceSize = typeSize * sliceElements;
		makeSlices(sliceSize);
	}

	private MMapImage(final String filename, final TImgRO inImage)
			throws IOException {
		this.raf = new RandomAccessFile(filename, "rw");
		dim = inImage.getDim();
		type = inImage.getImageType();
		typeSize = TImgTools.typeSize(type);
		sliceElements = dim.x * dim.y;
		final long sliceSize = typeSize * sliceElements;
		fillSlices(sliceSize, inImage);
		init(filename);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#appendProcLog(java.lang.String)
	 */
	@Override
	public String appendProcLog(final String inData) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean checkpos(final int x, final int y, final int z) {
		return (x >= 0 && x < dim.x) && (y >= 0 && y < dim.y)
				&& (z >= 0 && z < dim.z);
	}

	public void close() throws IOException {
		// A TIPLGlobal.runGC() is required to remove the memory mappings.
		mappings.clear();
		raf.close();
	}

	private void fillSlices(final long sliceSize, final TImgRO inImage) {
		long offset = 0;
		final ExecutorService es = TIPLGlobal.getIOExecutor();
		for (int z = 0; z < dim.z; z++, offset += sliceSize)
			mappings.add(es.submit(new ByteBufferMaker(raf.getChannel(),
					offset, sliceSize, inImage, z)));
		es.shutdown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getCompression()
	 */
	@Override
	public boolean getCompression() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getDim()
	 */
	@Override
	public D3int getDim() {
		// TODO Auto-generated method stub
		return dim;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getElSize()
	 */
	@Override
	public D3float getElSize() {
		// TODO Auto-generated method stub
		return elSize;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TReader#getImage()
	 */
	@Override
	public TImg getImage() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getImageType()
	 */
	@Override
	public int getImageType() {
		return type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getOffset()
	 */
	@Override
	public D3int getOffset() {
		// TODO Auto-generated method stub
		return offset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getPath()
	 */
	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getPolyImage(int, int)
	 */
	@Override
	public Object getPolyImage(final int iSliceNumber, final int asType) {
		assert checkpos(0, 0, iSliceNumber);
		assert TImgTools.isValidType(asType);
		final MappedByteBuffer curMap = getSliceFromFuture(iSliceNumber);

		switch (type) {
		case TImgTools.IMAGETYPE_CHAR:
		case TImgTools.IMAGETYPE_BOOL:
			final char[] cb = new char[sliceElements];
			curMap.asCharBuffer().get(cb, 0, sliceElements);
			return TImgTools.convertArrayType(cb, 0, asType, false, 1.0f, 127);
		case TImgTools.IMAGETYPE_SHORT:
			final short[] sb = new short[sliceElements];
			curMap.asShortBuffer().get(sb, 0, sliceElements);
			return TImgTools.convertArrayType(sb, type, asType, false, 1.0f,
					127);
		case TImgTools.IMAGETYPE_INT:
			final int[] ib = new int[sliceElements];
			curMap.asIntBuffer().get(ib, 0, sliceElements);
			return TImgTools.convertArrayType(ib, type, asType, false, 1.0f,
					127);
		case TImgTools.IMAGETYPE_FLOAT:
			final float[] fb = new float[sliceElements];
			curMap.asFloatBuffer().get(fb, 0, sliceElements);
			return TImgTools.convertArrayType(fb, type, asType, false, 1.0f,
					127);
		default:
			throw new IllegalArgumentException("This type: " + type
					+ " does not exist, what on earth happened!");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getPos()
	 */
	@Override
	public D3int getPos() {
		// TODO Auto-generated method stub
		return pos;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getProcLog()
	 */
	@Override
	public String getProcLog() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getSampleName()
	 */
	@Override
	public String getSampleName() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getShortScaleFactor()
	 */
	@Override
	public float getShortScaleFactor() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getSigned()
	 */
	@Override
	public boolean getSigned() {
		// TODO Auto-generated method stub
		return false;
	}

	private MappedByteBuffer getSliceFromFuture(final int iSliceNumber) {
		try {
			final MappedByteBuffer curMap = mappings.get(iSliceNumber).get();
			curMap.rewind();
			return curMap;
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalArgumentException("Task has been interrupted:"
					+ this + " getSlice:" + iSliceNumber);
		} catch (final ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalArgumentException(
					"Task has not executed successfuly:" + this + " getSlice:"
							+ iSliceNumber);
		}
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#InitializeImage(tipl.util.D3int, tipl.util.D3int,
	 * tipl.util.D3int, tipl.util.D3float, int)
	 */
	@Override
	public boolean InitializeImage(final D3int dPos, final D3int cDim,
			final D3int dOffset, final D3float elSize, final int imageType) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * I hate these methods but there is sadly no other way, without
	 * encapsulating everything in objects and making a big damn mess
	 */
	/*
	 * private float getFloat(int x, int y,int z) { assert checkpos(x,y,z); long
	 * p = position(x, y, z); int mapN = (int) (p / MAPPING_SIZE); int offN =
	 * (int) (p % MAPPING_SIZE); return mappings.get(mapN).getFloat(offN); }
	 * private short getShort(int x, int y,int z) { assert checkpos(x,y,z); long
	 * p = position(x, y, z); int mapN = (int) (p / MAPPING_SIZE); int offN =
	 * (int) (p % MAPPING_SIZE); return mappings.get(mapN).getShort(offN); }
	 * private int getInt(int x, int y,int z) { assert checkpos(x,y,z); long p =
	 * position(x, y, z); int mapN = (int) (p / MAPPING_SIZE); int offN = (int)
	 * (p % MAPPING_SIZE); return mappings.get(mapN).getInt(offN); } private
	 * char getChar(int x, int y,int z) { assert checkpos(x,y,z); long p =
	 * position(x, y, z); int mapN = (int) (p / MAPPING_SIZE); int offN = (int)
	 * (p % MAPPING_SIZE); return mappings.get(mapN).getChar(offN); } private
	 * boolean getBool(int x, int y,int z) { assert checkpos(x,y,z); long p =
	 * position(x, y, z); int mapN = (int) (p / MAPPING_SIZE); int offN = (int)
	 * (p % MAPPING_SIZE); return mappings.get(mapN).getChar(offN)>0; }
	 * 
	 * private void setFloat(int x, int y,int z, float d) { assert
	 * checkpos(x,y,z); long p = position(x, y, z) ; int mapN = (int) (p /
	 * MAPPING_SIZE); int offN = (int) (p % MAPPING_SIZE);
	 * mappings.get(mapN).putFloat(offN, d); } private void setChar(int x, int
	 * y,int z, char d) { assert checkpos(x,y,z); long p = position(x, y, z) ;
	 * int mapN = (int) (p / MAPPING_SIZE); int offN = (int) (p % MAPPING_SIZE);
	 * mappings.get(mapN).putChar(offN, d); } private void setInt(int x, int
	 * y,int z, int d) { assert checkpos(x,y,z); long p = position(x, y, z) ;
	 * int mapN = (int) (p / MAPPING_SIZE); int offN = (int) (p % MAPPING_SIZE);
	 * mappings.get(mapN).putInt(offN, d); } private void setShort(int x, int
	 * y,int z, short d) { assert checkpos(x,y,z); long p = position(x, y, z) ;
	 * int mapN = (int) (p / MAPPING_SIZE); int offN = (int) (p % MAPPING_SIZE);
	 * mappings.get(mapN).putShort(offN, d); } private void setBool(int x, int
	 * y,int z, boolean d) { assert checkpos(x,y,z); long p = position(x, y, z)
	 * ; int mapN = (int) (p / MAPPING_SIZE); int offN = (int) (p %
	 * MAPPING_SIZE); mappings.get(mapN).putChar(offN, (d ? (char) 127 : (char)
	 * 0)); }
	 */
	@Override
	public int isFast() {
		return ITIPLStorage.FAST_MEMORY_MAP_BASED;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TReader#isParallel()
	 */
	@Override
	public boolean isParallel() {
		// TODO Auto-generated method stub
		return false;
	}

	private void makeSlices(final long sliceSize) {
		long offset = 0;
		final ExecutorService es = TIPLGlobal.getIOExecutor();
		for (int z = 0; z < dim.z; z++, offset += sliceSize)
			mappings.add(es.submit(new ByteBufferMaker(raf.getChannel(),
					offset, sliceSize)));
		es.shutdown();
	}

	/**
	 * position within a slice
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	protected long position(final long x, final long y) {
		return (y * dim.x + x) * typeSize;
	}

	protected long position(final long x, final long y, final long z) {
		return ((z * dim.y + y) * dim.x + x) * typeSize;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TReader#readerName()
	 */
	@Override
	public String readerName() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TReader#ReadHeader()
	 */
	@Override
	public void ReadHeader() throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TReader#ReadSlice(int)
	 */
	@Override
	public TSliceReader ReadSlice(final int n) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setCompression(boolean)
	 */
	@Override
	public void setCompression(final boolean inData) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setDim(tipl.util.D3int)
	 */
	@Override
	public void setDim(final D3int inData) {
		throw new IllegalArgumentException(
				"Cannot set image type of a mmap after creation");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setElSize(tipl.util.D3float)
	 */
	@Override
	public void setElSize(final D3float inData) {
		// TODO Auto-generated method stub
		elSize = inData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setImageType(int)
	 */
	@Override
	public void setImageType(final int inData) {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException(
				"Cannot set image type of a mmap after creation");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setOffset(tipl.util.D3int)
	 */
	@Override
	public void setOffset(final D3int inData) {
		// TODO Auto-generated method stub
		offset = inData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setPos(tipl.util.D3int)
	 */
	@Override
	public void setPos(final D3int inData) {
		// TODO Auto-generated method stub
		pos = inData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setShortScaleFactor(float)
	 */
	@Override
	public void setShortScaleFactor(final float ssf) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setSigned(boolean)
	 */
	@Override
	public void setSigned(final boolean inData) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TReader#SetupReader(java.lang.String)
	 */
	@Override
	public void SetupReader(final String inPath) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TWriter#SetupWriter(tipl.formats.TImg,
	 * java.lang.String)
	 */
	@Override
	public void SetupWriter(final TImg inputImage, final String outputPath) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TWriter#Write()
	 */
	@Override
	public void Write() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TWriter#WriteHeader()
	 */
	@Override
	public void WriteHeader() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TWriter#writerName()
	 */
	@Override
	public String writerName() {
		// TODO Auto-generated method stu
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TWriter#WriteSlice(int)
	 */
	@Override
	public void WriteSlice(final int n) {
		// TODO Auto-generated method stub

	}

}
