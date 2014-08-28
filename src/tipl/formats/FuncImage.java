package tipl.formats;

import tipl.util.ArgumentList.TypedPath;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * FImage is simply an image which is a transform (VoxelFunction) of another
 * image, this is currently used to create Zimages, Rimages and the like
 */
public abstract class FuncImage implements TImgRO {

	protected TImgRO templateData;
	protected int imageType;

	public boolean useMask = false;
	public static final double[] intRange = { 0, (int) Math.pow(2, 31) - 1 };
	public static final double[] byteRange = { 0, (int) Math.pow(2, 7) - 1 };
	public static final double[] shortRange = { 0, (int) Math.pow(2, 15) - 1 };
	public static final double[] floatRange = { 0, 1 };
	public static final double[] boolRange = { 0, 1 };

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

	/**
	 * is the voxel function filter based on float (ture) or integer (false)
	 * based images
	 **/
	public final boolean useFloat;

	protected FuncImage(final boolean useFloatInput) {
		useFloat = useFloatInput;
	}

	/**
	 * Fimage simply returns data from the template file whenever any resource
	 * except slice data is requested
	 */
	public FuncImage(final TImgRO dummyDataset, final int iimageType) {
		templateData = dummyDataset;

		imageType = iimageType;
		useFloat = false;
	}

	/**
	 * Fimage simply returns data from the template file whenever any resource
	 * except slice data is requested
	 * 
	 * @param useFloatInput
	 *            is the value for useFloat as defined earlier and basically
	 *            asks if integers or floats are given as input to the
	 *            voxelfunction
	 **/
	public FuncImage(final TImgRO dummyDataset, final int iimageType,
			final boolean useFloatInput) {
		templateData = dummyDataset;
		imageType = iimageType;
		useFloat = useFloatInput;
	}

	@Override
	public String appendProcLog(final String inData) {
		return templateData.getProcLog() + inData;
	}

	public boolean CheckSizes(final TImgRO otherTImg) {
		return TImgTools.CheckSizes2(this, otherTImg);
	}

	@Override
	public boolean getCompression() {
		return templateData.getCompression();
	}

	/** The size of the image */
	@Override
	public D3int getDim() {
		return templateData.getDim();
	}

	@Override
	public D3float getElSize() {
		return templateData.getElSize();
	}

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
		return templateData.getOffset();
	}

	@Override
	public abstract TypedPath getPath();

	@Override
	public Object getPolyImage(final int isliceNumber, final int asType) {
		final boolean[] maskSlice = (boolean[]) templateData.getPolyImage(
				isliceNumber, 10);
		if (useFloat) {
			final float[] fSlice = (float[]) templateData.getPolyImage(
					isliceNumber, 3);
			
			final double[] dSlice = new double[fSlice.length];
			for (int i = 0; i < fSlice.length; i++) dSlice[i]=getVFvalue(i, isliceNumber, fSlice[i]);
			
			return TImgTools.convertArrayType(dSlice, TImgTools.IMAGETYPE_DOUBLE, asType, this.getSigned(), this.getShortScaleFactor());
			
			
		} else {
			final int[] tSlice = (int[]) templateData.getPolyImage(
					isliceNumber, 2);
			
			final double[] dSlice = new double[tSlice.length];
			for (int i = 0; i < tSlice.length; i++) dSlice[i]=getVFvalue(i, isliceNumber, tSlice[i]);
			
			return TImgTools.convertArrayType(dSlice, TImgTools.IMAGETYPE_DOUBLE, asType, this.getSigned(), this.getShortScaleFactor());

		}

	}

	/**
	 * The position of the bottom leftmost voxel in the image in real space,
	 * only needed for ROIs
	 */
	@Override
	public D3int getPos() {
		return templateData.getPos();
	}

	@Override
	public abstract String getProcLog();

	public abstract double[] getRange();

	@Override
	public abstract String getSampleName();

	@Override
	public float getShortScaleFactor() {
		return templateData.getShortScaleFactor();
	}

	/**
	 * Is the image signed (should an offset be added / subtracted when the data
	 * is loaded to preserve the sign)
	 */
	@Override
	public boolean getSigned() {
		return templateData.getSigned();
	}

	/**
	 * The output value for a given position and value
	 * 
	 * @param xyzPos
	 * @param v
	 * @return value
	 */
	public abstract double getVFvalue(int cIndex, int sliceNumber, double v);


	@Override
	public int isFast() {
		return templateData.isFast();
	}

	@Override
	public boolean isGood() {
		return templateData.isGood();
	}

	public boolean setBoolArray(final int iSlice, final boolean[] junk) {
		System.out.println("NOT IMPLEMENTED FOR :" + this);
		return false;
	}

	public boolean setByteArray(final int iSlice, final char[] junk) {
		System.out.println("NOT IMPLEMENTED FOR :" + this);
		return false;
	}

	public boolean setFloatArray(final int iSlice, final float[] junk) {
		System.out.println("NOT IMPLEMENTED FOR :" + this);
		return false;
	}


	public boolean setIntArray(final int iSlice, final int[] junk) {
		System.out.println("NOT IMPLEMENTED FOR :" + this);
		return false;
	}


	public boolean setShortArray(final int iSlice, final short[] junk) {
		System.out.println("NOT IMPLEMENTED FOR :" + this);
		return false;
	}

}