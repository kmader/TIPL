package tipl.formats;

import ij.ImageStack;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * PureFImage is simply an image which is a transform (VoxelFunction) of another
 * image, this is currently used to create Zimages, Rimages and the like
 * 
 * <pre> v2 Fixed a bug in initialization order for PureFImage constructor. The mirrorImage command runs getProcLog which requires the function to be defined.
 * 
 * <pre> v1
 * */
public class PureFImage implements TImg {
	/**
	 * RImageCyl is simply an image where the intensity values = the radial
	 * distance in cylindrical coordinates
	 */
	public static class CylR implements PureFImage.PositionFunction {
		int x, y;
		double minr = 0, maxr = 4000;

		public CylR(TImg dummyDataset) {
			final Double[] sPos = TImgTools
					.getXYZVecFromVec(dummyDataset, 0, 0);
			final Double[] fPos = TImgTools.getXYZVecFromVec(dummyDataset,
					dummyDataset.getDim().x * dummyDataset.getDim().y - 1, 0);
			x = (int) (fPos[0].doubleValue() / 2 + sPos[0].doubleValue() / 2);
			y = (int) (fPos[1].doubleValue() / 2 + sPos[1].doubleValue() / 2);
			maxr = Math.sqrt(2) * get(fPos);
		}

		@Override
		public double get(Double[] ipos) {
			final double xVal = (ipos[0].floatValue() - x);
			final double yVal = (ipos[1].floatValue() - y);
			return (float) Math.sqrt(xVal * xVal + yVal * yVal);
		}

		@Override
		public double[] getRange() {
			return new double[] { minr, maxr };
		}

		@Override
		public String name() {
			return "CYLR-Image:(" + x + "," + y + ")";
		}
	}

	/**
	 * CylTheta is simply an image where the intensity values = the theta in
	 * cylindrical coordinates
	 */
	public static class CylTheta implements PureFImage.PositionFunction {
		int x, y;

		public CylTheta(TImg dummyDataset) {
			final Double[] sPos = TImgTools
					.getXYZVecFromVec(dummyDataset, 0, 0);
			final Double[] fPos = TImgTools.getXYZVecFromVec(dummyDataset,
					dummyDataset.getDim().x * dummyDataset.getDim().y - 1,
					dummyDataset.getDim().z);
			x = (int) (fPos[0].doubleValue() / 2 + sPos[0].doubleValue() / 2);
			y = (int) (fPos[1].doubleValue() / 2 + sPos[1].doubleValue() / 2);
		}

		@Override
		public double get(Double[] ipos) {
			final double xVal = (ipos[0].doubleValue() - x);
			final double yVal = (ipos[1].doubleValue() - y);
			return (float) Math.atan2(yVal, xVal);
		}

		@Override
		public double[] getRange() {
			return new double[] { -Math.PI, Math.PI };
		}

		@Override
		public String name() {
			return "CylTheta-Image:(" + x + "," + y + ")";
		}
	}

	public static class PhiImageSph extends PureFImage {
		protected TImg templateData;
		protected int imageType;

		/** ThetaImageCyl simply returns the theta position in the current slice */
		public PhiImageSph(TImg dummyDataset, int iimageType) {
			super(dummyDataset, iimageType, new SphPhi(dummyDataset));
		}
	}

	/** An interface for making function to apply to voxels in an image **/
	public static interface PositionFunction {
		/** gray value to return for a voxel at position ipos[] with value v **/
		public double get(Double[] ipos);

		/** function returning the estimated range of the image **/
		public double[] getRange();

		/** name of the function being applied **/
		public String name();
	}

	/**
	 * RImage is simply an image where the intensity values = the radial
	 * distance
	 */
	public static class RImage extends PureFImage {
		protected TImg templateData;
		protected int imageType;

		/**
		 * Rimage simply returns the r position based on the estimated center of
		 * volume
		 */
		public RImage(TImg dummyDataset, int iimageType) {
			// templateData=dummyDataset;
			// imageType=iimageType;
			super(dummyDataset, iimageType, new SphR(dummyDataset));
		}

		/** use a fixed center of volume **/
		public RImage(TImg dummyDataset, int iimageType, int x, int y, int z) {
			// templateData=dummyDataset;
			// imageType=iimageType;
			super(dummyDataset, iimageType, new SphR(x, y, z));
		}
	}

	public static class RImageCyl extends PureFImage {
		protected TImg templateData;
		protected int imageType;

		/** Rimage simply returns the r position in the current slice */
		public RImageCyl(TImg dummyDataset, int iimageType) {
			super(dummyDataset, iimageType, new CylR(dummyDataset));
		}
	}

	/**
	 * SphPhi is simply an image where the intensity values = the phi in
	 * spherical coordinates (the angle to the XY plane)
	 */
	public static class SphPhi implements PureFImage.PositionFunction {
		int x, y, z;

		public SphPhi(TImg dummyDataset) {
			final Double[] sPos = TImgTools
					.getXYZVecFromVec(dummyDataset, 0, 0);
			final Double[] fPos = TImgTools.getXYZVecFromVec(dummyDataset,
					dummyDataset.getDim().x * dummyDataset.getDim().y - 1,
					dummyDataset.getDim().z);
			x = (int) (fPos[0].doubleValue() / 2 + sPos[0].doubleValue() / 2);
			y = (int) (fPos[1].doubleValue() / 2 + sPos[1].doubleValue() / 2);
			z = (int) (fPos[2].doubleValue() / 2 + sPos[2].doubleValue() / 2);
		}

		@Override
		public double get(Double[] ipos) {
			final double xVal = (ipos[0].doubleValue() - x);
			final double yVal = (ipos[1].doubleValue() - y);
			final double zVal = (ipos[2].doubleValue() - z);
			return (float) Math.atan2(zVal,
					Math.sqrt(xVal * xVal + yVal * yVal));
		}

		@Override
		public double[] getRange() {
			return new double[] { -Math.PI, Math.PI };
		}

		@Override
		public String name() {
			return "SphPhi-Image:(" + x + "," + y + "," + z + ")";
		}
	}

	/**
	 * RImageCyl is simply an image where the intensity values = the radial
	 * distance in cylindrical coordinates
	 */
	public static class SphR implements PureFImage.PositionFunction {
		int x, y, z;
		double minr = 0, maxr = 4000;

		/** use a fixed center of volume for r calculations **/
		public SphR(int ix, int iy, int iz) {
			x = ix;
			y = iy;
			z = iz;

		}

		/** estimate the center of volume based on the dimensions of the dataset **/
		public SphR(TImg dummyDataset) {
			estCOV(dummyDataset);
		}

		protected void estCOV(TImg dummyDataset) {
			final Double[] sPos = TImgTools
					.getXYZVecFromVec(dummyDataset, 0, 0);
			final Double[] fPos = TImgTools.getXYZVecFromVec(dummyDataset,
					dummyDataset.getDim().x * dummyDataset.getDim().y - 1,
					dummyDataset.getDim().z);
			x = (int) (fPos[0].floatValue() / 2 + sPos[0].floatValue() / 2);
			y = (int) (fPos[1].floatValue() / 2 + sPos[1].floatValue() / 2);
			z = (int) (fPos[2].floatValue() / 2 + sPos[2].floatValue() / 2);
			minr = 0;
			maxr = Math.sqrt(2) * get(fPos);
		}

		@Override
		public double get(Double[] ipos) {
			final double xVal = (ipos[0].floatValue() - x);
			final double yVal = (ipos[1].floatValue() - y);
			final double zVal = (ipos[2].floatValue() - z);
			return (float) Math.sqrt(xVal * xVal + yVal * yVal + zVal * zVal);
		}

		@Override
		public double[] getRange() {
			return new double[] { minr, maxr };
		}

		@Override
		public String name() {
			return "SphR-Image:(" + x + "," + y + "," + z + ")";
		}
	}

	public static class ThetaImageCyl extends PureFImage {
		protected TImg templateData;
		protected int imageType;

		/** ThetaImageCyl simply returns the theta position in the current slice */
		public ThetaImageCyl(TImg dummyDataset, int iimageType) {
			super(dummyDataset, iimageType, new CylTheta(dummyDataset));
		}
	}

	/** ZFunc is the voxel function to produce z valued slices */
	public static class ZFunc implements PureFImage.PositionFunction {
		Double[] sPos, fPos;

		public ZFunc(TImg dummyDataset) {
			sPos = TImgTools.getXYZVecFromVec(dummyDataset, 0, 0);
			fPos = TImgTools.getXYZVecFromVec(dummyDataset,
					dummyDataset.getDim().x * dummyDataset.getDim().y - 1,
					dummyDataset.getDim().z);

		}

		@Override
		public double get(Double[] ipos) {
			return ipos[2].doubleValue();
		}

		@Override
		public double[] getRange() {
			return new double[] { sPos[2], fPos[2] };
		}

		@Override
		public String name() {
			return "Z-Image";
		}
	}

	/** ZImage is simply an image where the intensity values = the slice number */
	public static class ZImage extends PureFImage {
		protected TImg templateData;
		protected int imageType;

		/**
		 * Zimage simply returns data from the template file whenever any
		 * resource except slice data is requested
		 */
		public ZImage(TImg dummyDataset, int iimageType) {
			// templateData=dummyDataset;
			// imageType=iimageType;
			super(dummyDataset, iimageType, new ZFunc(dummyDataset));
		}
	}

	protected final int imageType;
	protected final PositionFunction pf;

	protected D3int myPos, myDim, myOffset;

	protected D3float mySize;
	protected String procLog = "";

	protected int sliceLength;
	public static final double[] intRange = { 0, 2 ^ 31 - 1 };
	public static final double[] byteRange = { 0, 2 ^ 7 - 1 };
	public static final double[] shortRange = { 0, 2 ^ 15 - 1 };
	public static final double[] floatRange = { 0, 1 };
	public static final double[] boolRange = { 0, 1 };

	public static double[] typeRange(int cType) {
		switch (cType) {
		case 0:
			return byteRange;
		case 1:
			return shortRange;
		case 2:
			return intRange;
		case 3:
			return floatRange;
		case 10:
			return boolRange;
		default:
			System.out.println("What sort of type should this be??" + cType);
			return null;
		}
	}

	/**
	 * allows for a purefimage to have a short scaling factor (mainly used for
	 * testing but can have other purposes)
	 */
	public final float shortScaleFactor;

	/**
	 * Creates a PureFImage tool with the given settings
	 * 
	 * @param dummyDataset
	 *            used as the basis for the whole analysis
	 * @param iimageType
	 *            type of image to be made
	 * @param ipf
	 *            function to use to calculate values
	 */
	public PureFImage(TImgTools.HasDimensions dummyDataset, int iimageType,
			PositionFunction ipf) {
		imageType = iimageType;
		pf = ipf;
		shortScaleFactor = 1.0f;
		TImgTools.mirrorImage(dummyDataset, this);
	}

	/**
	 * Creates a PureFImage tool with the given settings
	 * 
	 * @param dummyDataset
	 *            used as the basis for the whole analysis
	 * @param iimageType
	 *            type of image to be made
	 * @param ipf
	 *            function to use to calculate values
	 * @param inShortScaleFactor
	 *            a scaling factor between integer and floats (used when reading
	 *            floats)
	 */
	public PureFImage(TImgTools.HasDimensions dummyDataset, int iimageType,
			PositionFunction ipf, float inShortScaleFactor) {
		imageType = iimageType;
		pf = ipf;
		shortScaleFactor = inShortScaleFactor;
		TImgTools.mirrorImage(dummyDataset, this);
	}

	// New functions just tackily implemented
	@Override
	public String appendProcLog(String inData) {
		procLog += inData;
		return getProcLog();
	}

	public boolean CheckSizes(String otherPath) {
		System.out
				.println("The CheckSizes function has not yet been implemented in PureFImage for arbitrary image sizes");
		return false;
	}

	public boolean CheckSizes(TImg otherTImg) {
		return TImgTools.CheckSizes2(otherTImg, this);
	}

	@Override
	public boolean getCompression() {
		return false;
	}

	/** The size of the image */
	@Override
	public D3int getDim() {
		return myDim;
	}

	@Override
	public D3float getElSize() {
		return mySize;
	}

	/**
	 * The aim type of the image (0=char, 1=short, 2=int, 3=float, 10=bool, -1
	 * same as input)
	 */
	@Override
	public int getImageType() {
		return imageType;
	}

	/**
	 * The size of the border around the image which does not contain valid
	 * voxel data
	 */
	@Override
	public D3int getOffset() {
		return myOffset;
	}

	@Override
	public String getPath() {
		return pf.name() + " @ PureFImage";
	}

	@Override
	public Object getPolyImage(int isliceNumber, int asType) {
		switch (asType) {
		case 10:

			final boolean[] maskSlice = new boolean[sliceLength];
			for (int i = 0; i < maskSlice.length; i++) {
				maskSlice[i] = pf.get(getXYZVec(i, isliceNumber)) > 0.5f;
			}
			return maskSlice;
		case 0:
			final char[] charSlice = new char[sliceLength];
			for (int i = 0; i < charSlice.length; i++) {
				charSlice[i] = (char) pf.get(getXYZVec(i, isliceNumber));
			}
			return charSlice;
		case 1:
			final short[] shortSlice = new short[sliceLength];
			for (int i = 0; i < shortSlice.length; i++) {
				shortSlice[i] = (short) pf.get(getXYZVec(i, isliceNumber));
			}
			return shortSlice;
		case 2:
			final int[] intSlice = new int[sliceLength];
			for (int i = 0; i < intSlice.length; i++) {
				intSlice[i] = (int) pf.get(getXYZVec(i, isliceNumber));
			}
			return intSlice;
		case 3:
			final float[] floatSlice = new float[sliceLength];
			for (int i = 0; i < floatSlice.length; i++) {
				floatSlice[i] = (float) pf.get(getXYZVec(i, isliceNumber));
			}
			return floatSlice;
		default:
			throw new IllegalStateException("Type for getPolyImge not known: "
					+ asType);
		}
	}

	/**
	 * The position of the bottom leftmost voxel in the image in real space,
	 * only needed for ROIs
	 */
	@Override
	public D3int getPos() {
		return myPos;
	}

	@Override
	public String getProcLog() {
		return procLog + "\n" + pf.name() + "\n";
	}

	public double[] getRange() {
		return pf.getRange();
	}

	@Override
	public String getSampleName() {
		return pf.name() + " @ POS:" + myPos + "," + myDim;
	}

	@Override
	public float getShortScaleFactor() {
		return shortScaleFactor;
	}

	/**
	 * Is the image signed (should an offset be added / subtracted when the data
	 * is loaded to preserve the sign)
	 */
	@Override
	public boolean getSigned() {
		return false;
	}

	public Double[] getXYZVec(int cIndex, int sliceNumber) {
		return TImgTools.getXYZVecFromVec(myPos, myDim, cIndex, sliceNumber);
	}

	@Override
	public TImg inheritedAim(boolean[] imgArray, D3int dim, D3int offset) {
		return TImgTools.makeTImgExportable(this).inheritedAim(imgArray, dim,
				offset);
	}

	@Override
	public TImg inheritedAim(char[] imgArray, D3int dim, D3int offset) {
		return TImgTools.makeTImgExportable(this).inheritedAim(imgArray, dim,
				offset);
	}

	@Override
	public TImg inheritedAim(float[] imgArray, D3int dim, D3int offset) {
		return TImgTools.makeTImgExportable(this).inheritedAim(imgArray, dim,
				offset);
	}

	@Override
	public TImg inheritedAim(ImageStack iStack) {
		return TImgTools.makeTImgExportable(this).inheritedAim(iStack);
	}

	@Override
	public TImg inheritedAim(int[] imgArray, D3int dim, D3int offset) {
		return TImgTools.makeTImgExportable(this).inheritedAim(imgArray, dim,
				offset);
	}

	@Override
	public TImg inheritedAim(short[] imgArray, D3int dim, D3int offset) {
		return TImgTools.makeTImgExportable(this).inheritedAim(imgArray, dim,
				offset);
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

	/**
	 * a functional image is faster than disk-based encoded but slower than in
	 * memory
	 */
	@Override
	public int isFast() {
		return 2;
	}

	@Override
	public boolean isGood() {
		return true;
	}

	public float readShortScaleFactor() {
		return 1;
	}

	public boolean setBoolArray(int iSlice, boolean[] junk) {
		System.out.println("NOT IMPLEMENTED FOR :" + this);
		return false;
	}

	public boolean setByteArray(int iSlice, char[] junk) {
		System.out.println("NOT IMPLEMENTED FOR :" + this);
		return false;
	}

	@Override
	public void setCompression(boolean inData) {
	}

	/** The size of the image */
	@Override
	public void setDim(D3int inData) {
		myDim = inData;
		sliceLength = myDim.x * myDim.y;
	}

	@Override
	public void setElSize(D3float inData) {
		mySize = inData;
	}

	public boolean setFloatArray(int iSlice, float[] junk) {
		System.out.println("NOT IMPLEMENTED FOR :" + this);
		return false;
	}

	/**
	 * The aim type of the image (0=char, 1=short, 2=int, 3=float, 10=bool, -1
	 * same as input)
	 */
	@Override
	public void setImageType(int inData) {
	}

	public boolean setIntArray(int iSlice, int[] junk) {
		System.out.println("NOT IMPLEMENTED FOR :" + this);
		return false;
	}

	/**
	 * The size of the border around the image which does not contain valid
	 * voxel data
	 */
	@Override
	public void setOffset(D3int inData) {
		myOffset = inData;
	}

	/**
	 * The position of the bottom leftmost voxel in the image in real space,
	 * only needed for ROIs
	 */
	@Override
	public void setPos(D3int inData) {
		myPos = inData;
	}

	public boolean setShortArray(int iSlice, short[] junk) {
		System.out.println("NOT IMPLEMENTED FOR :" + this);
		return false;
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