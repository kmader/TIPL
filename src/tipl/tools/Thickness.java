package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * Thickness is an abstract class used for calculating Thickness images. It
 * provides the basic infrastructure for accepting a distance map and exporting
 * a thickness map but provides no implementations, see subclasses for details
 * 
 * @author mader
 * 
 */
public abstract class Thickness extends BaseTIPLPluginIO {
	public int[] inAim;
	public int[] outAim;
	// double neighborOccupancy=1.0; // delete if less than 100% of neighbors
	// are occupied
	static final int MAXDIST = 4000;

	static final int MAXDISTVAL = 32765;
	final int OUTERSHELL = 0;
	public float MINTHICKNESS = 1;
	public static final double distScalar = (MAXDIST + 0.0)
			/ (MAXDISTVAL + 0.0);
	/**
	 * Should the input files be left intact. Since this function is used quite
	 * often we shall keep the default value at true
	 **/
	public static boolean doPreserveInput = true;
	public int killVox = 0;
	public int curSphere = 0;

	public Thickness() {
		// Just Sit and wait
	}

	// Multithreading functions
	@Override
	public TImg ExportAim(TImgRO.CanExport templateAim) {
		if (isInitialized) {
			if (runCount > 0) {
				final TImg outAimData = templateAim.inheritedAim(outAim, dim,
						offset);
				outAimData.appendProcLog(procLog);
				return outAimData;
			} else {
				System.err
						.println("The plug-in : "
								+ getPluginName()
								+ ", has not yet been run, exported does not exactly make sense, original data will be sent.");
				return templateAim.inheritedAim(inAim, dim, offset);
			}
		} else {
			throw new IllegalArgumentException(
					"The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exported does not make any sense");
		}
	}

	protected void fillBubble(int x, int y, int z, double cMaxVal,
			final boolean useSync) {
		// Fill in the bubble based on the distance map

		final int tlowx = max(x - cMaxVal, lowx);
		final int tlowy = max(y - cMaxVal, lowy);
		final int tlowz = max(z - cMaxVal, lowz);
		final int tuppx = min(x + cMaxVal, uppx);
		final int tuppy = min(y + cMaxVal, uppy);
		final int tuppz = min(z + cMaxVal, uppz);
		final int myDist = (int) Math.round(cMaxVal / distScalar);
		for (int z2 = tlowz; z2 < tuppz; z2++) {

			// safe mode so to say, but uses at most 2.5 cores useSync=true;
			final double zd = (z - z2) * (z - z2);
			for (int y2 = tlowy; y2 < tuppy; y2++) {
				final double yd = (y - y2) * (y - y2);
				int off2 = (z2 * dim.y + y2) * dim.x + lowx;
				off2 += (tlowx - lowx);// since loop in next line starts at
										// tlowx instead of lowx
				for (int x2 = tlowx; x2 < tuppx; x2++, off2++) {
					final double ballCentDist = Math.sqrt((x - x2) * (x - x2)
							+ yd + zd);

					if (ballCentDist < cMaxVal) {
						if ((outAim[off2] < myDist) && (outAim[off2] > 0)) {
							final int outAimVal = myDist;
							final double redDist = cMaxVal - (ballCentDist); // Reduced
																				// distance
																				// is
																				// the
																				// distance
																				// from
																				// the
																				// current
																				// point
																				// to
																				// the
																				// edge
																				// of
																				// the
																				// sphere
																				// at
																				// (x,y,z)
							final double actDist = inAim[off2] * distScalar - 1; // actual
																					// distance
																					// is
																					// the
																					// distance
																					// of
																					// the
																					// current
																					// point
																					// to
																					// the
																					// nearest
																					// edge
							if (actDist <= redDist) {
								// if the distance here is the same as what
								// you'd predict from the sphere or less than
								// turn it off
								if (useSync)
									synWriteIn(off2, 0);
								else
									inAim[off2] = 0;
							}

							if (useSync)
								synWriteOut(off2, outAimVal);
							else
								outAim[off2] = outAimVal;

						}
					}
				}
			}
		}
		runCount++;
		// System.out.println("Filling Bubble :: "+x+", "+y+", "+z+" : "+cMaxVal+" -> "+myDist+"-K:"+ckill+", V:"+cbub+"/"+cball);
	}

	@Override
	public abstract String getPluginName();

	protected void ImportData(TImgRO inImg) {
		final int[] inputmap = TImgTools.makeTImgFullReadable(inImg)
				.getIntAim();
		aimLength = inputmap.length;
		if (Thickness.doPreserveInput) {
			inAim = new int[aimLength];
			System.arraycopy(inputmap, 0, inAim, 0, aimLength);
		} else {
			inAim = inputmap;
		}
		InitLabels(inImg.getDim(), inImg.getOffset());
	}

	protected abstract void InitLabels(D3int idim, D3int ioffset);

	/**
	 * Load the distance map
	 * 
	 */
	@Override
	public void LoadImages(TImgRO[] inImages) {
		// TODO Auto-generated method stub
		if (inImages.length < 1)
			throw new IllegalArgumentException(
					"Too few arguments for LoadImages in:" + getPluginName());
		final TImgRO inImg = inImages[0];
		ImportData(inImg);
	}

	@Override
	public ArgumentParser setParameter(ArgumentParser p, String prefix) {
		MINTHICKNESS = p.getOptionInt(prefix + "minthickness", OUTERSHELL,
				"Minimum thickness to calculate");

		return p;
	}

	/**
	 * synchronized input writing function
	 * 
	 * @param off2
	 * @param val
	 */
	protected synchronized void synWriteIn(int off2, int val) {
		inAim[off2] = val;
	}

	/**
	 * synchronized output image writing function
	 * 
	 * @param off2
	 * @param val
	 */
	protected synchronized void synWriteOut(int off2, int val) {
		outAim[off2] = val;
	}
}
