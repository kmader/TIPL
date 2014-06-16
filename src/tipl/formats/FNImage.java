package tipl.formats;

import java.io.Serializable;

import tipl.util.TImgTools;

/**
 * FNImage is simply an image which is a transform (VoxelFunction) of another
 * image, this is currently used to create Zimages, Rimages and the like
 */
public class FNImage extends FImage {

	/** Class for the generator to add N images together **/
	public static class AddImages implements VFNGenerator {
		@Override
		public VoxelFunctionN get() {
			return new VoxelFunctionN() {
				double cValue = 0.0;

				@Override
				public void add(final Double[] ipos, final double v) {
					cValue += v;
				}

				@Override
				public double get() {
					return cValue;
				}

				@Override
				public double[] getRange() {
					return floatRange;
				}

				@Override
				public String name() {
					return "Add Images";
				}
			};
		}
	}

	/** Class for the generator to average N images together **/
	public static class AvgImages implements VFNGenerator {
		@Override
		public VoxelFunctionN get() {
			return new VoxelFunctionN() {
				double cValue = 0.0;
				long cCount = 0;

				@Override
				public void add(final Double[] ipos, final double v) {
					cValue += v;
					cCount++;
				}

				@Override
				public double get() {
					return (cValue / cCount);
				}

				@Override
				public double[] getRange() {
					return floatRange;
				}

				@Override
				public String name() {
					return "Average Images";
				}
			};
		}
	}

	/** Class for the generator to multiply N images together **/
	public static class MultiplyImages implements VFNGenerator {
		@Override
		public VoxelFunctionN get() {
			return new VoxelFunctionN() {
				double cValue = 1.0;

				@Override
				public void add(final Double[] ipos, final double v) {
					cValue *= v;
				}

				@Override
				public double get() {
					return cValue;
				}

				@Override
				public double[] getRange() {
					return floatRange;
				}

				@Override
				public String name() {
					return "Multiply Images";
				}
			};
		}
	}

	/** Class for the generator to turn binary images into colored phase images **/
	public static class PhaseImage implements VFNGenerator {
		protected final double[] phaseVals;

		public PhaseImage(final double[] iphaseVals) {
			phaseVals = iphaseVals;
		}

		@Override
		public VoxelFunctionN get() {
			return new VoxelFunctionN() {
				double oValue = 0.0;
				int imgCount = 0;

				@Override
				public void add(final Double[] ipos, final double v) {
					if ((v > 0) && (imgCount < phaseVals.length))
						oValue = phaseVals[imgCount];
					imgCount++;
				}

				@Override
				public double get() {
					return oValue;
				}

				@Override
				public double[] getRange() {
					double minval = phaseVals[0];
					double maxval = phaseVals[0];
					for (final double cPhase : phaseVals) {
						if (minval < cPhase)
							minval = cPhase;
						if (maxval > cPhase)
							maxval = cPhase;
					}
					return new double[] { minval, maxval };
				}

				@Override
				public String name() {
					String oString = "PhaseImage(" + phaseVals.length + "):";
					for (final double cPhase : phaseVals)
						oString += cPhase + ", ";
					return oString;
				}
			};
		}
	}
	/**
	 * Interface for creating the voxel functions (since they need to be created new for every voxel)
	 * @author mader
	 *
	 */
	public static interface VFNGenerator extends Serializable  {
		public VoxelFunctionN get();
	}

	/**
	 * An interface for making function to combining N images into a single
	 * image
	 **/
	public static interface VoxelFunctionN extends Serializable {
		/** add a voxel at position ipos[] with value v **/
		public void add(Double[] ipos, double v);

		/**
		 * get the value based on all the voxels which have been added to this
		 * point (either spatially variant or different images
		 **/
		public double get();

		/** function returning the estimated range of the image **/
		public double[] getRange();

		/** name of the function being applied **/
		public String name();
	}

	protected TImgRO[] templateDataArray;
	protected VFNGenerator vfg;
	protected VoxelFunctionN vfn;
	protected final int sliceSize;

	/**
	 * Fimage simply returns data from the template file whenever any resource
	 * except slice data is requested
	 */
	public FNImage(final TImgRO[] inputDataset, final int iimageType,
			final VFNGenerator ivfg) {
		super(false);
		templateDataArray = inputDataset;
		templateData = templateDataArray[0];
		imageType = iimageType;
		sliceSize = ((boolean[]) templateData.getPolyImage(0, TImgTools.IMAGETYPE_BOOL)).length; // read
		// slice
		// 0
		// this
		// must
		// be
		// present
		VFGSetup(iimageType, ivfg, false);

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
	public FNImage(final TImg[] inputDataset, final int iimageType,
			final VFNGenerator ivfg, final boolean useFloatInput) {
		super(useFloatInput);
		templateDataArray = inputDataset;
		templateData = templateDataArray[0];
		imageType = iimageType;
		sliceSize = ((boolean[]) templateData.getPolyImage(0, TImgTools.IMAGETYPE_BOOL)).length; // read
		// slice
		// 0
		// this
		// must
		// be
		// present
		VFGSetup(iimageType, ivfg, useFloatInput);
	}

	@Override
	public String appendProcLog(final String inData) {
		final String oName = templateData.getProcLog() + inData;
		for (final TImgRO cImg : templateDataArray) {
			cImg.appendProcLog(inData);
		}
		return oName;
	}

	@Override
	public boolean CheckSizes(final TImgRO otherTImg) {
		return TImgTools.CheckSizes2(this, otherTImg);
	}
	
	@Override
	public Object getPolyImage(final int isliceNumber, final int asType) {
		final VoxelFunctionN[] cvf = VFNSpawn();
		VFNIterate(cvf, isliceNumber);
		switch (asType) {
		case TImgTools.IMAGETYPE_BOOL:
			final boolean[] maskSlice = new boolean[sliceSize];
			for (int i = 0; i < sliceSize; i++)
				maskSlice[i] = cvf[i].get() > 0.5;

				return maskSlice;

		case TImgTools.IMAGETYPE_CHAR:
			final char[] byteSlice = new char[sliceSize];
			for (int i = 0; i < sliceSize; i++)
				byteSlice[i] = (char) cvf[i].get();
			return byteSlice;
		case TImgTools.IMAGETYPE_SHORT:
			final short[] sSlice = new short[sliceSize];
			for (int i = 0; i < sliceSize; i++)
				sSlice[i] = (short) cvf[i].get();
			return sSlice;
		case TImgTools.IMAGETYPE_INT:
			final int[] intSlice = new int[sliceSize];
			for (int i = 0; i < sliceSize; i++)
				intSlice[i] = (int) cvf[i].get();
			return intSlice;
		case TImgTools.IMAGETYPE_FLOAT:
			final float[] floatSlice = new float[sliceSize];
			for (int i = 0; i < sliceSize; i++)
				floatSlice[i] = (float) cvf[i].get();
			return floatSlice;
		default:
			throw new IllegalArgumentException("Type must be valid :"
					+ asType);

		}
	} 


	@Override
	public String getProcLog() {
		return templateData.getProcLog() + "\n" + "FNImage Applied\n"
				+ getSampleName() + "\n";
	}

	@Override
	public String getSampleName() {
		String oName = vf.name() + " @ (";
		for (final TImgRO cImg : templateDataArray) {
			oName += cImg.getSampleName() + ", ";
		}
		return oName + ")";
	}

	public short[] getShortArray(final int isliceNumber) {
		final VoxelFunctionN[] cvf = VFNSpawn();
		final short[] shortSlice = new short[sliceSize];
		VFNIterate(cvf, isliceNumber);
		for (int i = 0; i < sliceSize; i++)
			shortSlice[i] = (short) cvf[i].get();
		return shortSlice;
	}

	public Double[] getXYZVec(final int cIndex, final int sliceNumber) {
		return TImgTools.getXYZVecFromVec(this, cIndex, sliceNumber);
	}

	@Override
	public int isFast() {
		return 0;
	}

	@Override
	public boolean isGood() {
		return templateData.isGood();
	}

	protected final void VFGSetup(final int iimageType,
			final VFNGenerator ivfg, final boolean useFloatInput) {

		vfg = ivfg;
		vfn = vfg.get();
		final VFNGenerator tvfg = ivfg;
		// to preserve compatibility with the old / other functions repackage
		// vfN as VF
		vf = new VoxelFunction() {
			@Override
			public double get(final Double[] ipos, final double v) {
				final VoxelFunctionN vfon = tvfg.get();
				vfon.add(ipos, v);
				return vfon.get();
			}

			@Override
			public double[] getRange() {
				return tvfg.get().getRange();
			}

			@Override
			public String name() {
				return tvfg.get().name();
			}
		};
	}

	protected void VFNFloatIterate(final VoxelFunctionN[] cvf,
			final int isliceNumber) {
		for (final TImgRO cImg : templateDataArray) {
			final TImg.TImgFull fullCImg = new TImg.TImgFull(cImg);
			final float[] fSlice = fullCImg.getFloatArray(isliceNumber);
			for (int i = 0; i < sliceSize; i++) {
				cvf[i].add(getXYZVec(i, isliceNumber), fSlice[i]);
			}
		}
	}

	/**
	 * The function reads the given slices from each of the input images and uses it to populate the VoxelFunction array
	 * with integer values. 
	 * @param cvf
	 * @param isliceNumber
	 */
	protected void VFNIntIterate(final VoxelFunctionN[] cvf,
			final int isliceNumber) {
		for (final TImgRO cImg : templateDataArray) {
			final TImg.TImgFull fullCImg = new TImg.TImgFull(cImg);
			final int[] tSlice = fullCImg.getIntArray(isliceNumber);
			for (int i = 0; i < sliceSize; i++) {
				cvf[i].add(getXYZVec(i, isliceNumber), tSlice[i]);
			}
		}
	}
	/**
	 * VFN iterate determines based on the class if the float or integer based iterate command makes sense
	 * @param cvf function array to populate with values
	 * @param isliceNumber slice number to extract from all images
	 */
	protected void VFNIterate(final VoxelFunctionN[] cvf, final int isliceNumber) {
		if (useFloat) {
			VFNFloatIterate(cvf, isliceNumber);
		} else {
			VFNIntIterate(cvf, isliceNumber);
		}
	}

	protected VoxelFunctionN[] VFNSpawn() {
		final VoxelFunctionN[] outVFN = new VoxelFunctionN[sliceSize];
		for (int i = 0; i < sliceSize; i++) // create a new function for every point in the slice
			outVFN[i] = vfg.get();
		return outVFN;
	}

}