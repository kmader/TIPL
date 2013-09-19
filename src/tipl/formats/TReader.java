package tipl.formats;

import ij.ImageStack;

import java.io.File;
import java.io.IOException;

import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

/** Interface for reading TImg files from a data source */
public interface TReader {
	public static abstract class SliceReader implements TSliceReader {
		protected D3int offset = new D3int(0, 0, 0);
		protected D3int pos = new D3int(0, 0, 0);
		protected D3float elSize = new D3float(1, 1, 1);
		protected String path = "";
		final protected String procLog = "";
		protected int imageType;
		protected int maxVal;
		protected int sliceSize;

		protected float ShortScaleFactor = 1.0f;
		protected D3int dim;

		@Override
		public boolean CheckSizes(String otherPath) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void checkSliceDim(D3int globalDim) throws IOException {
			if (getDim().x != globalDim.x)
				throw new IOException(
						"Aim Sizes, Suddenly No Longer Match Input Images, x:"
								+ getDim() + ", " + globalDim);
			if (getDim().y != globalDim.y)
				throw new IOException(
						"Aim Sizes, Suddenly No Longer Match Input Images, y:"
								+ getDim() + ", " + globalDim);
		}

		@Override
		public D3int getDim() {
			return dim;
		}

		@Override
		public D3float getElSize() {
			// TODO Auto-generated method stub
			return elSize;
		}

		@Override
		public int getImageType() {
			return imageType;
		}

		@Override
		public D3int getOffset() {
			return offset;
		}

		@Override
		public String getPath() {
			// TODO Auto-generated method stub
			return path;
		}

		@Override
		public D3int getPos() {
			return pos;
		}

		@Override
		public String getProcLog() {
			// TODO Auto-generated method stub
			return procLog;
		}

		@Override
		public String getSampleName() {
			// TODO Auto-generated method stub
			return path;
		}

		@Override
		public float getShortScaleFactor() {
			return ShortScaleFactor;
		}

		@Override
		public boolean getSigned() {
			return true;
		}

		@Override
		public boolean isGood() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setShortScaleFactor(float ssf) {
			ShortScaleFactor = ssf;
		}

	}

	public static class TReaderImg implements TImg {
		/* Implement TImg functions */
		protected TReader cReader;
		protected String procLog = "";

		public TReaderImg(TReader inReader) {
			cReader = inReader;
		}

		@Override
		public String appendProcLog(String inData) {
			return getProcLog() + inData;
		}

		public boolean CheckSizes(String otherPath) {
			return true;
		}

		public boolean CheckSizes(TImg otherTImg) {
			return true;
		}

		@Override
		public boolean getCompression() {
			return false;
		}

		/** The size of the image */
		@Override
		public D3int getDim() {
			return cReader.getDim();
		}

		@Override
		public D3float getElSize() {
			return cReader.getElSize();
		}

		@Override
		public int getImageType() {
			return cReader.getImageType();
		}

		/**
		 * The size of the border around the image which does not contain valid
		 * voxel data
		 */
		@Override
		public D3int getOffset() {
			return new D3int(0);
		}

		@Override
		public String getPath() {
			return cReader.readerName();
		}

		@Override
		public Object getPolyImage(int slice, int asType) {
			try {
				final TSliceReader curSlice = cReader.ReadSlice(slice);
				curSlice.checkSliceDim(getDim());
				return curSlice.polyReadImage(asType);
			} catch (final Exception e) {
				System.out.println("Error Reading or Converting Slice from "
						+ cReader + " @ " + slice);
				e.printStackTrace();
				return null;
			}

		}

		/**
		 * The position of the bottom leftmost voxel in the image in real space,
		 * only needed for ROIs
		 */
		@Override
		public D3int getPos() {
			return cReader.getPos();
		}

		@Override
		public String getProcLog() {
			return procLog;
		}

		@Override
		public String getSampleName() {
			return cReader.readerName();
		}

		@Override
		public float getShortScaleFactor() {
			return 1;
		}

		/**
		 * Is the image signed (should an offset be added / subtracted when the
		 * data is loaded to preserve the sign)
		 */
		@Override
		public boolean getSigned() {
			return true;
		}



		@Override
		public TImg inheritedAim(boolean[] imgArray, D3int dim, D3int offset) {
			return TImgTools.makeTImgExportable(this).inheritedAim(imgArray,
					dim, offset);
		}

		@Override
		public TImg inheritedAim(char[] imgArray, D3int dim, D3int offset) {
			return TImgTools.makeTImgExportable(this).inheritedAim(imgArray,
					dim, offset);
		}

		@Override
		public TImg inheritedAim(float[] imgArray, D3int dim, D3int offset) {
			return TImgTools.makeTImgExportable(this).inheritedAim(imgArray,
					dim, offset);
		}

		@Override
		public TImg inheritedAim(ImageStack iStack) {
			return TImgTools.makeTImgExportable(this).inheritedAim(iStack);
		}

		@Override
		public TImg inheritedAim(int[] imgArray, D3int dim, D3int offset) {
			return TImgTools.makeTImgExportable(this).inheritedAim(imgArray,
					dim, offset);
		}

		@Override
		public TImg inheritedAim(short[] imgArray, D3int dim, D3int offset) {
			return TImgTools.makeTImgExportable(this).inheritedAim(imgArray,
					dim, offset);
		}

		// Temporary solution,
		@Override
		public TImg inheritedAim(TImgRO inAim) {
			return TImgTools.makeTImgExportable(this).inheritedAim(inAim);
		}

		@Override
		public boolean InitializeImage(D3int iPos, D3int iDim, D3int iOff,
				D3float iSize, int iType) {
			return false;
		}

		@Override
		public int isFast() {
			return TImgTools.FAST_TIFF_BASED;
		}

		@Override
		public boolean isGood() {
			return true;
		}

		public float readShortScaleFactor() {
			return 1;
		}

		@Override
		public void setCompression(boolean inData) {
		}

		/** The size of the image */
		@Override
		public void setDim(D3int inData) {
		}

		@Override
		public void setElSize(D3float inData) {
		}

		/**
		 * The aim type of the image (0=char, 1=short, 2=int, 3=float, 10=bool,
		 * -1 same as input)
		 */
		@Override
		public void setImageType(int inData) {
			System.out
					.println(" Input type setting is not permitted for reader types...");
		}

		/**
		 * The size of the border around the image which does not contain valid
		 * voxel data
		 */
		@Override
		public void setOffset(D3int inData) {
		}

		/**
		 * The position of the bottom leftmost voxel in the image in real space,
		 * only needed for ROIs
		 */
		@Override
		public void setPos(D3int inData) {
		}

		@Override
		public void setShortScaleFactor(float ssf) {
		}

		@Override
		public void setSigned(boolean inData) {
		}

		@Override
		public void WriteAim(String path) {
			TImgTools.WriteTImg(this, path);
		}

		@Override
		public void WriteAim(String outpath, int outType, float scaleVal,
				boolean IisSigned) {
			TImgTools.WriteTImg(this, outpath, outType, scaleVal, IisSigned);
		}

	}

	public interface TSliceFactory {
		public TSliceReader ReadFile(File curfile) throws IOException;
	}

	public static interface TSliceReader {
		public boolean CheckSizes(String otherPath);

		public void checkSliceDim(D3int globalDim) throws IOException;

		/** The size of the image */
		public D3int getDim();

		/** The element size (in mm) of a voxel */
		public D3float getElSize();

		/**
		 * The aim type of the image (0=char, 1=short, 2=int, 3=float, 10=bool,
		 * -1 same as input)
		 */
		public int getImageType();

		/**
		 * The size of the border around the image which does not contain valid
		 * voxel data
		 */
		public D3int getOffset();

		/** The path of the data (whatever it might be) */
		public String getPath();

		/**
		 * The position of the bottom leftmost voxel in the image in real space,
		 * only needed for ROIs
		 */
		public D3int getPos();

		/**
		 * Procedure Log, string containing past operations and information on
		 * the aim-file
		 */
		public String getProcLog();

		/** The name of the data */
		public String getSampleName();

		/**
		 * The factor to scale bool/short/int/char values by when converting
		 * to/from float (for distance maps is (1000.0/32767.0))
		 */
		public float getShortScaleFactor();

		/**
		 * Is the image signed (should an offset be added / subtracted when the
		 * data is loaded to preserve the sign)
		 */
		public boolean getSigned();

		/** Is the data in good shape */
		public boolean isGood();

		public Object polyReadImage(int asType) throws IOException;

		public void setShortScaleFactor(float ssf);
	}

	public D3int getDim();

	public D3float getElSize();

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getSigned()
	 */

	/** get a TImg from the file **/
	public TImg getImage();

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getImageType()
	 */
	public int getImageType();

	public D3int getPos();

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getProcLog()
	 */
	public String getProcLog();

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getShortScaleFactor()
	 */
	public float getShortScaleFactor();

	public boolean getSigned();

	/**
	 * Can the writer be run in parallel (multiple threads processing different
	 * slices
	 */
	public boolean isParallel();

	/** The name of the writer, used for menus and logging */
	public String readerName();

	/** Write everything (header and all slices) */
	// public void Read();
	/** write just the header */
	public void ReadHeader() throws IOException;

	/**
	 * write just a given slice
	 * 
	 * @throws IOException
	 */
	public TSliceReader ReadSlice(int n) throws IOException;

	/** The command to initialize the writer */
	public void SetupReader(String inPath);
}
