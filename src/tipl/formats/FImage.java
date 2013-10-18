package tipl.formats;

import tipl.util.TImgTools;

/**
 * FImage is simply an image which is a transform (VoxelFunction) of another
 * image, this is currently used to create Zimages, Rimages and the like
 */
public class FImage extends FuncImage {
	public static class MaskablePFImage extends FImage {
		protected TImgRO templateData;
		protected int imageType;

		public MaskablePFImage(final TImgRO dummyDataset, final int iimageType,
				final PureFImage.PositionFunction ipf) {

			super(dummyDataset, iimageType, new FImage.VoxelFunction() {

				@Override
				public double get(final Double[] ipos, final double voxval) {
					return ipf.get(ipos);
				}

				@Override
				public double[] getRange() {
					return ipf.getRange();
				}

				@Override
				public String name() {
					return "Masked:" + ipf.name();
				}

				@Override
				public String toString() {
					return "Masked:" + ipf;
				}
			}, true);
		}

		public MaskablePFImage(final TImgRO dummyDataset,
				final PureFImage iPFImage) {

			super(dummyDataset, iPFImage.imageType, new FImage.VoxelFunction() {

				@Override
				public double get(final Double[] ipos, final double voxval) {
					return iPFImage.pf.get(ipos);
				}

				@Override
				public double[] getRange() {
					return iPFImage.pf.getRange();
				}

				@Override
				public String name() {
					return "Masked:" + iPFImage.pf.name();
				}

				@Override
				public String toString() {
					return "Masked:" + iPFImage.pf;
				}
			}, true);
		}

		@Override
		public String toString() {
			return "Masked-PFImage:" + vf;
		}
	}

	/** An interface for making function to apply to voxels in an image **/
	public static interface VoxelFunction {
		/** gray value to return for a voxel at position ipos[] with value v **/
		public double get(Double[] ipos, double v);

		/** function returning the estimated range of the image **/
		public double[] getRange();

		/** name of the function being applied **/
		public String name();
	}

	protected VoxelFunction vf;

	protected FImage(final boolean useFloatInput) {
		super(useFloatInput);
	}

	/**
	 * Fimage simply returns data from the template file whenever any resource
	 * except slice data is requested
	 */
	public FImage(final TImgRO dummyDataset, final int iimageType,
			final VoxelFunction ivf) {
		super(dummyDataset, iimageType, false);
		vf = ivf;

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
	public FImage(final TImgRO dummyDataset, final int iimageType,
			final VoxelFunction ivf, final boolean useFloatInput) {
		super(dummyDataset, iimageType, useFloatInput);
		vf = ivf;
	}

	@Override
	public String getPath() {
		return vf.name() + " @ " + templateData.getPath();
	}

	@Override
	public String getProcLog() {
		return templateData.getProcLog() + "\n" + vf.name() + "\n";
	}

	@Override
	public double[] getRange() {
		return vf.getRange();
	}

	@Override
	public String getSampleName() {
		return vf.name() + " @ " + templateData.getSampleName();
	}

	/**
	 * Pulls value from VoxelFunction
	 * 
	 * @param xyzPos
	 * @param v
	 * @return value
	 */
	@Override
	public double getVFvalue(final int cIndex, final int sliceNumber,
			final double v) {
		final Double[] xyzPos = TImgTools.getXYZVecFromVec(this, cIndex,
				sliceNumber);
		return vf.get(xyzPos, v);
	}

}