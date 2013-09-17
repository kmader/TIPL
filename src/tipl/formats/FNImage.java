package tipl.formats;

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
				public void add(Double[] ipos, double v) {
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
				public void add(Double[] ipos, double v) {
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
				public void add(Double[] ipos, double v) {
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

		public PhaseImage(double[] iphaseVals) {
			phaseVals = iphaseVals;
		}

		@Override
		public VoxelFunctionN get() {
			return new VoxelFunctionN() {
				double oValue = 0.0;
				int imgCount = 0;

				@Override
				public void add(Double[] ipos, double v) {
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

	public static interface VFNGenerator {
		public VoxelFunctionN get();
	}

	/**
	 * An interface for making function to combining N images into a single
	 * image
	 **/
	public static interface VoxelFunctionN {
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

	protected TImg[] templateDataArray;
	protected VFNGenerator vfg;
	protected VoxelFunctionN vfn;
	protected final int sliceSize;

	/**
	 * Fimage simply returns data from the template file whenever any resource
	 * except slice data is requested
	 */
	public FNImage(TImg[] dummyDataset, int iimageType, VFNGenerator ivfg) {
		super(false);
		templateDataArray = dummyDataset;
		templateData = templateDataArray[0];
		imageType = iimageType;
		sliceSize = ((boolean[]) templateData.getPolyImage(0, 10)).length; // read
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
	public FNImage(TImg[] dummyDataset, int iimageType, VFNGenerator ivfg,
			boolean useFloatInput) {
		super(useFloatInput);
		templateDataArray = dummyDataset;
		templateData = templateDataArray[0];
		imageType = iimageType;
		sliceSize = ((boolean[]) templateData.getPolyImage(0, 10)).length; // read
																			// slice
																			// 0
																			// this
																			// must
																			// be
																			// present
		VFGSetup(iimageType, ivfg, useFloatInput);
	}

	@Override
	public String appendProcLog(String inData) {
		final String oName = templateData.getProcLog() + inData;
		for (final TImg cImg : templateDataArray) {
			cImg.appendProcLog(inData);
		}
		return oName;
	}

	@Override
	public boolean CheckSizes(TImg otherTImg) {
		return TImgTools.CheckSizes2(this, otherTImg);
	}

	public boolean[] getBoolArray(int isliceNumber) {
		final VoxelFunctionN[] cvf = VFNSpawn();
		final boolean[] maskSlice = new boolean[sliceSize];
		VFNIterate(cvf, isliceNumber);
		for (int i = 0; i < sliceSize; i++)
			maskSlice[i] = cvf[i].get() > 0.5;

		return maskSlice;
	}

	public char[] getByteArray(int isliceNumber) {
		final VoxelFunctionN[] cvf = VFNSpawn();
		final char[] byteSlice = new char[sliceSize];
		VFNIterate(cvf, isliceNumber);
		for (int i = 0; i < sliceSize; i++)
			byteSlice[i] = (char) cvf[i].get();
		return byteSlice;
	}

	public float[] getFloatArray(int isliceNumber) {
		final VoxelFunctionN[] cvf = VFNSpawn();
		final float[] floatSlice = new float[sliceSize];
		VFNIterate(cvf, isliceNumber);
		for (int i = 0; i < sliceSize; i++)
			floatSlice[i] = (float) cvf[i].get();
		return floatSlice;
	}

	public int[] getIntArray(int isliceNumber) {
		final VoxelFunctionN[] cvf = VFNSpawn();
		final int[] intSlice = new int[sliceSize];
		VFNIterate(cvf, isliceNumber);
		for (int i = 0; i < sliceSize; i++)
			intSlice[i] = (int) cvf[i].get();
		return intSlice;
	}

	@Override
	public String getProcLog() {
		return templateData.getProcLog() + "\n" + "FNImage Applied\n"
				+ getSampleName() + "\n";
	}

	@Override
	public String getSampleName() {
		String oName = vf.name() + " @ (";
		for (final TImg cImg : templateDataArray) {
			oName += cImg.getSampleName() + ", ";
		}
		return oName + ")";
	}

	public short[] getShortArray(int isliceNumber) {
		final VoxelFunctionN[] cvf = VFNSpawn();
		final short[] shortSlice = new short[sliceSize];
		VFNIterate(cvf, isliceNumber);
		for (int i = 0; i < sliceSize; i++)
			shortSlice[i] = (short) cvf[i].get();
		return shortSlice;
	}

	public Double[] getXYZVec(int cIndex, int sliceNumber) {
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

	protected final void VFGSetup(int iimageType, VFNGenerator ivfg,
			boolean useFloatInput) {

		vfg = ivfg;
		vfn = vfg.get();
		final VFNGenerator tvfg = ivfg;
		// to preserve compatibility with the old / other functions repackage
		// vfN as VF
		vf = new VoxelFunction() {
			@Override
			public double get(Double[] ipos, double v) {
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

	protected void VFNFloatIterate(VoxelFunctionN[] cvf, int isliceNumber) {

		for (final TImg cImg : templateDataArray) {
			final TImg.TImgFull fullCImg = new TImg.TImgFull(cImg);
			final float[] fSlice = fullCImg.getFloatArray(isliceNumber);
			for (int i = 0; i < sliceSize; i++) {
				cvf[i].add(getXYZVec(i, isliceNumber), fSlice[i]);
			}
		}
	}

	protected void VFNIntIterate(VoxelFunctionN[] cvf, int isliceNumber) {
		for (final TImg cImg : templateDataArray) {
			final TImg.TImgFull fullCImg = new TImg.TImgFull(cImg);
			final int[] tSlice = fullCImg.getIntArray(isliceNumber);
			for (int i = 0; i < sliceSize; i++) {
				cvf[i].add(getXYZVec(i, isliceNumber), tSlice[i]);
			}
		}
	}

	protected void VFNIterate(VoxelFunctionN[] cvf, int isliceNumber) {
		if (useFloat)
			VFNFloatIterate(cvf, isliceNumber);
		else
			VFNIntIterate(cvf, isliceNumber);
	}

	protected VoxelFunctionN[] VFNSpawn() {
		final VoxelFunctionN[] outVFN = new VoxelFunctionN[sliceSize];
		for (int i = 0; i < sliceSize; i++)
			outVFN[i] = vfg.get();
		return outVFN;
	}

}