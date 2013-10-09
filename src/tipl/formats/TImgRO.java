package tipl.formats;

import java.util.ArrayList;

import ij.ImageStack;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * A Read-only TImg (the full TImg is a subinterface of this) useful for
 * concurrent tasks where writing should be impossible
 */
public interface TImgRO extends TImgTools.HasDimensions {
	/**
	 * CanExport means the TImg is capable of imparting itself on other images.
	 * 
	 * @author mader
	 * 
	 */
	public static interface CanExport extends TImgRO {
		@Deprecated
		public TImg inheritedAim(boolean[] imgArray, D3int dim, D3int offset);

		@Deprecated
		public TImg inheritedAim(char[] imgArray, D3int dim, D3int offset);

		@Deprecated
		public TImg inheritedAim(float[] imgArray, D3int dim, D3int offset);

		public TImg inheritedAim(ImageStack iStack);

		@Deprecated
		public TImg inheritedAim(int[] imgArray, D3int dim, D3int offset);

		@Deprecated
		public TImg inheritedAim(short[] imgArray, D3int dim, D3int offset);

		public TImg inheritedAim(TImgRO inAim);
	}

	/**
	 * FullReadable means the entire image can be read out into a single linear
	 * array sized x*y*z The standard processing format used in IPL and limits
	 * array to 2^31 elements ~ 1300^3 This really should not be used in future
	 * plugins if at all possible
	 * 
	 * @author mader
	 * 
	 */
	@Deprecated
	public static interface FullReadable extends TImgRO {
		public boolean[] getBoolAim();

		public char[] getByteAim();

		public float[] getFloatAim();

		public int[] getIntAim();

		public short[] getShortAim();
	}

	/**
	 * an object that can output its entire stack
	 * 
	 * @author mader
	 * 
	 */
	public static interface HasStack extends TImgRO {
		/**
		 * Returns a stack of images as an array of linear arrays
		 * 
		 * @param asType
		 * @return
		 */
		public Object[] getPolyStack(int asType);
	}

	public static class TImgFull implements TImgOld {
		protected final TImgRO myImg;

		public TImgFull(final TImgRO inImg) {
			myImg = inImg;
		}

		@Override
		public boolean[] getBoolArray(int sliceNumber) {
			return (boolean[]) myImg.getPolyImage(sliceNumber, 10);
		}

		@Override
		public char[] getByteArray(int sliceNumber) {
			return (char[]) myImg.getPolyImage(sliceNumber, 0);
		}

		@Override
		public float[] getFloatArray(int sliceNumber) {
			return (float[]) myImg.getPolyImage(sliceNumber, 3);
		}

		@Override
		public int[] getIntArray(int sliceNumber) {
			return (int[]) myImg.getPolyImage(sliceNumber, 2);
		}

		@Override
		public short[] getShortArray(int sliceNumber) {
			return (short[]) myImg.getPolyImage(sliceNumber, 1);
		}

		/**
		 * return the underlying TImgRO object
		 * 
		 * @return the TImgRO object
		 */
		public TImgRO gT() {
			return myImg;
		}
	}

	public static interface TImgOld {
		public boolean[] getBoolArray(int sliceNumber);

		public char[] getByteArray(int sliceNumber);

		public float[] getFloatArray(int sliceNumber);

		public int[] getIntArray(int sliceNumber);

		public short[] getShortArray(int sliceNumber);
	}

	public abstract static class TImgStack implements HasStack {
		@Override
		public Object[] getPolyStack(int asType) {
			final ArrayList<Object> cStack = new ArrayList<Object>();
			for (int i = 0; i <= getDim().z; i++)
				cStack.add(getPolyImage(i, asType));
			return cStack.toArray();
		}
	}

	public String appendProcLog(String inData);

	/**
	 * Whether or not basic compression (compatible almost everywhere) should be
	 * used when writing data
	 */
	public boolean getCompression();

	/**
	 * The aim type of the image (0=char, 1=short, 2=int, 3=float, 10=bool, -1
	 * same as input)
	 */
	public int getImageType();

	/** The path of the data (whatever it might be) */
	public String getPath();

	public Object getPolyImage(int sliceNumber, int asType);

	/** The name of the data */
	public String getSampleName();

	/**
	 * The factor to scale bool/short/int/char values by when converting to/from
	 * float (for distance maps is (1000.0/32767.0))
	 */
	@Override
	public float getShortScaleFactor();

	/**
	 * Is the image signed (should an offset be added / subtracted when the data
	 * is loaded to preserve the sign)
	 */
	public boolean getSigned();

	/**
	 * Check to see if the image is cached or otherwise fast access (used for
	 * caching methods to avoid double caching), 0 is encoded disk-based, 1 is
	 * memory map, 2 is in memory + computation, 3 is memory based, in general
	 * each computational layer (FImage) subtracts 1 from the isFast
	 */
	public int isFast();

	/** Is the data in good shape */
	public boolean isGood();

	/**
	 * save TImg to a file
	 * 
	 * @param path
	 */
	public void WriteAim(String path);

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
	public void WriteAim(String outpath, int outType, float scaleVal,
			boolean IisSigned);

}
