package tipl.formats;

import tipl.util.ArgumentList.TypedPath;
import tipl.util.ArgumentList;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

import java.io.Serializable;

/**
 * PureFImage is simply an image which is a transform (VoxelFunction) of another
 * image, this is currently used to create Zimages, Rimages and the like
 * <p/>
 * <pre> v2 Fixed a bug in initialization order for PureFImage constructor. The mirrorImage command runs getProcLog which requires the function to be defined.
 * <p/>
 * <pre> v1
 */
public class PureFImage implements TImgRO, TImgTools.ChangesDimensions {
	public static final double[] intRange = {0, 2 ^ 31 - 1};
	public static final double[] byteRange = {0, 2 ^ 7 - 1};
	public static final double[] shortRange = {0, 2 ^ 15 - 1};
	public static final double[] floatRange = {0, 1};
	public static final double[] boolRange = {0, 1};
	protected final int imageType;
	protected final PositionFunction pf;
	protected D3int myPos, myDim, myOffset;
	protected D3float mySize;
	protected String procLog = "";
	/**
	 * allows for a purefimage to have a short scaling factor (mainly used for
	 * testing but can have other purposes)
	 */
	protected float shortScaleFactor;

	/**
	 * Creates a PureFImage tool with the given settings
	 *
	 * @param dummyDataset used as the basis for the whole analysis
	 * @param iimageType   type of image to be made
	 * @param ipf          function to use to calculate values
	 */
	public PureFImage(final TImgTools.HasDimensions dummyDataset,
			final int iimageType, final PositionFunction ipf) {
		imageType = iimageType;
		pf = ipf;
		shortScaleFactor = 1.0f;
		TImgTools.mirrorImage(dummyDataset, this);
	}

	/**
	 * Creates a PureFImage tool with the given settings
	 *
	 * @param dummyDataset       used as the basis for the whole analysis
	 * @param iimageType         type of image to be made
	 * @param ipf                function to use to calculate values
	 * @param inShortScaleFactor a scaling factor between integer and floats (used when reading
	 *                           floats)
	 */
	public PureFImage(final TImgTools.HasDimensions dummyDataset,
			final int iimageType, final PositionFunction ipf,
			final float inShortScaleFactor) {
		imageType = iimageType;
		pf = ipf;
		shortScaleFactor = inShortScaleFactor;
		TImgTools.mirrorImage(dummyDataset, this);
	}

	public static double[] typeRange(final int cType) {
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

	// New functions just tackily implemented
	@Override
	public String appendProcLog(final String inData) {
		procLog += inData;
		return getProcLog();
	}

	public boolean CheckSizes(final String otherPath) {
		System.out
		.println("The CheckSizes function has not yet been implemented in PureFImage for arbitrary image sizes");
		return false;
	}

	public boolean CheckSizes(final TImgTools.HasDimensions otherTImg) {
		return TImgTools.CheckSizes2(otherTImg, this);
	}

	@Override
	public boolean getCompression() {
		return false;
	}

	/**
	 * The size of the image
	 */
	@Override
	public D3int getDim() {
		return myDim;
	}

	@Override
	public void setDim(D3int inData) {
		myDim = inData;
	}

	@Override
	public D3float getElSize() {
		return mySize;
	}

	@Override
	public void setElSize(D3float inData) {
		mySize = inData;
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
	public void setOffset(D3int inData) {
		myOffset = inData;
	}

	@Override
	public TypedPath getPath() {
		return ArgumentList.TypedPath.virtualPath(pf.name() + " @ PureFImage");
	}

	@Override
	public Object getPolyImage(final int isliceNumber, final int asType) {
		final int sliceLength = getDim().x * getDim().y;
		final double[] dSlice = new double[sliceLength];
		for (int i = 0; i < sliceLength; i++) dSlice[i]=pf.get(getXYZVec(i, isliceNumber));
		return TImgTools.convertArrayType(dSlice, TImgTools.IMAGETYPE_DOUBLE, asType, this.getSigned(), this.getShortScaleFactor());
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
	public void setPos(D3int inData) {
		myPos = inData;

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

	@Override
	public void setShortScaleFactor(float ssf) {
		shortScaleFactor = ssf;

	}

	/**
	 * Is the image signed (should an offset be added / subtracted when the data
	 * is loaded to preserve the sign)
	 */
	@Override
	public boolean getSigned() {
		return false;
	}

	public Double[] getXYZVec(final int cIndex, final int sliceNumber) {
		return TImgTools.getXYZVecFromVec(myPos, myDim, cIndex, sliceNumber);
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

	/**
	 * An interface for making function to apply to voxels in an image *
	 */
	public static interface PositionFunction extends Serializable {
		/**
		 * gray value to return for a voxel at position ipos[] with value v *
		 */
		public double get(Double[] ipos);

		/**
		 * function returning the estimated range of the image *
		 */
		public double[] getRange();

		/**
		 * name of the function being applied *
		 */
		public String name();
	}
	/**
	 * A simple utility function to convert pure position functions into standard voxel functions
	 * @param testFun
	 * @return
	 */
	public static FImage.VoxelFunction fromPositionFunction(final PositionFunction testFun) {
		return new FImage.VoxelFunction() {
			final PositionFunction pf = testFun;
			@Override
			public double get(Double[] ipos, double v) {
				return testFun.get(ipos);
			}

			@Override
			public double[] getRange() {
				return testFun.getRange();
			}

			@Override
			public String name() {
				return testFun.name();
			}
			
		};
	}

	/**
	 * RImageCyl is simply an image where the intensity values = the radial
	 * distance in cylindrical coordinates
	 */
	public static class CylR implements PureFImage.PositionFunction {
		int x, y;
		double minr = 0, maxr = 4000;

		public CylR(final TImgTools.HasDimensions dummyDataset) {
			final Double[] sPos = TImgTools
					.getXYZVecFromVec(dummyDataset, 0, 0);
			final Double[] fPos = TImgTools.getXYZVecFromVec(dummyDataset,
					dummyDataset.getDim().x * dummyDataset.getDim().y - 1, 0);
			x = (int) (fPos[0] / 2 + sPos[0] / 2);
			y = (int) (fPos[1] / 2 + sPos[1] / 2);
			maxr = Math.sqrt(2) * get(fPos);
		}

		@Override
		public double get(final Double[] ipos) {
			final double xVal = (ipos[0].floatValue() - x);
			final double yVal = (ipos[1].floatValue() - y);
			return (float) Math.sqrt(xVal * xVal + yVal * yVal);
		}

		@Override
		public double[] getRange() {
			return new double[]{minr, maxr};
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

		public CylTheta(final TImgTools.HasDimensions dummyDataset) {
			final Double[] sPos = TImgTools
					.getXYZVecFromVec(dummyDataset, 0, 0);
			final Double[] fPos = TImgTools.getXYZVecFromVec(dummyDataset,
					dummyDataset.getDim().x * dummyDataset.getDim().y - 1,
					dummyDataset.getDim().z);
			x = (int) (fPos[0] / 2 + sPos[0] / 2);
			y = (int) (fPos[1] / 2 + sPos[1] / 2);
		}

		@Override
		public double get(final Double[] ipos) {
			final double xVal = (ipos[0] - x);
			final double yVal = (ipos[1] - y);
			return (float) Math.atan2(yVal, xVal);
		}

		@Override
		public double[] getRange() {
			return new double[]{-Math.PI, Math.PI};
		}

		@Override
		public String name() {
			return "CylTheta-Image:(" + x + "," + y + ")";
		}
	}

	/**
	 * Returns a constant value
	 */
	public static class ConstantValue implements PureFImage.PositionFunction {
		double retValue;

		public ConstantValue(double retValue) {
			this.retValue = retValue;
		}

		@Override
		public double get(final Double[] ipos) {
			return retValue;
		}

		@Override
		public double[] getRange() {
			return new double[]{retValue, retValue};
		}

		@Override
		public String name() {
			return "Constant-Value:" + retValue;
		}
	}

	public static class PhiImageSph extends PureFImage {
		protected TImgTools.HasDimensions templateData;
		protected int imageType;

		/**
		 * ThetaImageCyl simply returns the theta position in the current slice
		 */
		public PhiImageSph(final TImgTools.HasDimensions dummyDataset, final int iimageType) {
			super(dummyDataset, iimageType, new SphPhi(dummyDataset));
		}
	}

	/**
	 * RImage is simply an image where the intensity values = the radial
	 * distance
	 */
	public static class RImage extends PureFImage {
		protected TImgTools.HasDimensions templateData;
		protected int imageType;

		/**
		 * Rimage simply returns the r position based on the estimated center of
		 * volume
		 */
		public RImage(final TImgTools.HasDimensions dummyDataset, final int iimageType) {
			// templateData=dummyDataset;
			// imageType=iimageType;
			super(dummyDataset, iimageType, new SphR(dummyDataset));
		}

		/**
		 * use a fixed center of volume *
		 */
		public RImage(final TImgTools.HasDimensions dummyDataset, final int iimageType,
				final int x, final int y, final int z) {
			// templateData=dummyDataset;
			// imageType=iimageType;
			super(dummyDataset, iimageType, new SphR(x, y, z));
		}
	}

	public static class RImageCyl extends PureFImage {
		protected TImgTools.HasDimensions templateData;
		protected int imageType;

		/**
		 * Rimage simply returns the r position in the current slice
		 */
		public RImageCyl(final TImgTools.HasDimensions dummyDataset, final int iimageType) {
			super(dummyDataset, iimageType, new CylR(dummyDataset));
		}
	}

	/**
	 * SphPhi is simply an image where the intensity values = the phi in
	 * spherical coordinates (the angle to the XY plane)
	 */
	public static class SphPhi implements PureFImage.PositionFunction {
		int x, y, z;

		public SphPhi(final TImgTools.HasDimensions dummyDataset) {
			final Double[] sPos = TImgTools
					.getXYZVecFromVec(dummyDataset, 0, 0);
			final Double[] fPos = TImgTools.getXYZVecFromVec(dummyDataset,
					dummyDataset.getDim().x * dummyDataset.getDim().y - 1,
					dummyDataset.getDim().z);
			x = (int) (fPos[0] / 2 + sPos[0] / 2);
			y = (int) (fPos[1] / 2 + sPos[1] / 2);
			z = (int) (fPos[2] / 2 + sPos[2] / 2);
		}

		@Override
		public double get(final Double[] ipos) {
			final double xVal = (ipos[0] - x);
			final double yVal = (ipos[1] - y);
			final double zVal = (ipos[2] - z);
			return (float) Math.atan2(zVal,
					Math.sqrt(xVal * xVal + yVal * yVal));
		}

		@Override
		public double[] getRange() {
			return new double[]{-Math.PI, Math.PI};
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

		/**
		 * use a fixed center of volume for r calculations *
		 */
		public SphR(final int ix, final int iy, final int iz) {
			x = ix;
			y = iy;
			z = iz;

		}

		/**
		 * estimate the center of volume based on the dimensions of the dataset *
		 */
		public SphR(final TImgTools.HasDimensions dummyDataset) {
			estCOV(dummyDataset);
		}

		protected void estCOV(final TImgTools.HasDimensions dummyDataset) {
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
		public double get(final Double[] ipos) {
			final double xVal = (ipos[0].floatValue() - x);
			final double yVal = (ipos[1].floatValue() - y);
			final double zVal = (ipos[2].floatValue() - z);
			return (float) Math.sqrt(xVal * xVal + yVal * yVal + zVal * zVal);
		}

		@Override
		public double[] getRange() {
			return new double[]{minr, maxr};
		}

		@Override
		public String name() {
			return "SphR-Image:(" + x + "," + y + "," + z + ")";
		}
	}

	public static class ThetaImageCyl extends PureFImage {
		protected TImgTools.HasDimensions templateData;
		protected int imageType;

		/**
		 * ThetaImageCyl simply returns the theta position in the current slice
		 */
		public ThetaImageCyl(final TImgTools.HasDimensions dummyDataset, final int iimageType) {
			super(dummyDataset, iimageType, new CylTheta(dummyDataset));
		}
	}

	/**
	 * ZFunc is the voxel function to produce z valued slices
	 */
	public static class ZFunc implements PureFImage.PositionFunction {
		Double[] sPos, fPos;

		public ZFunc(final TImgTools.HasDimensions dummyDataset) {
			sPos = TImgTools.getXYZVecFromVec(dummyDataset, 0, 0);
			fPos = TImgTools.getXYZVecFromVec(dummyDataset,
					dummyDataset.getDim().x * dummyDataset.getDim().y - 1,
					dummyDataset.getDim().z);

		}

		@Override
		public double get(final Double[] ipos) {
			return ipos[2];
		}

		@Override
		public double[] getRange() {
			return new double[]{sPos[2], fPos[2]};
		}

		@Override
		public String name() {
			return "Z-Image";
		}
	}

	/**
	 * ZImage is simply an image where the intensity values = the slice number
	 */
	public static class ZImage extends PureFImage {
		protected TImgTools.HasDimensions templateData;
		protected int imageType;

		/**
		 * Zimage simply returns data from the template file whenever any
		 * resource except slice data is requested
		 */
		public ZImage(final TImgTools.HasDimensions dummyDataset, final int iimageType) {
			// templateData=dummyDataset;
			// imageType=iimageType;
			super(dummyDataset, iimageType, new ZFunc(dummyDataset));
		}
	}

}