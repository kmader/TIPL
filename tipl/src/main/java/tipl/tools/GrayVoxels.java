package tipl.tools;

//import java.awt.*;
//import java.awt.image.*;
//import java.awt.image.ColorModel.*;
import java.io.Serializable;

import tipl.util.D3float;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Class for handling the 3D grayvalue data. Effectively used for the creation
 * of inertial tensors from position data. It handles computing averages,
 * covariances, diagonalizations and the like.
 * 
 * It must first be run adding all voxels using the the addVox command
 * for standard deviations and shape tensor it must be run again with the 
 * addCovVox command
 * 
 */
public class GrayVoxels implements Serializable {
	int voxelCount;
	double label;
	int labelType; // 0-no label, 1-integer, 2-double
	double curValSum;
	double curSqSum;
	boolean setMax = false;
	boolean diagRun = false;
	public boolean useWeights = false;
	/**
	 * No recenter skips the calculation of the center of volume and sets the
	 * value to 0. This is useful when looking at Radial Distribution Functions
	 * which should anyways be centered at zero and a recentering does not make
	 * sense
	 **/
	public boolean noRecenter = false;
	double cx = 0;
	double cx2 = 0;
	double wx = 0;
	double minX = 0;
	double maxX = 0;
	double c_meanx = 0;

	double cy = 0;
	double cy2 = 0;
	double wy = 0;
	double minY = 0;
	double maxY = 0;
	double c_meany = 0;

	double cz = 0;
	double cz2 = 0;
	double wz = 0;
	double minZ = 0;
	double maxZ = 0;
	double c_meanz = 0;

	int cached_mean = -1;

	double minP1 = 0;
	double minP2 = 0;
	double minP3 = 0;

	double maxP1 = 0;
	double maxP2 = 0;
	double maxP3 = 0;

	double cxy = 0;
	double cxz = 0;
	double cyz = 0;

	double cr = 0;
	double csr = 0;

	double minVal = 0;
	double maxVal = 0;

	// Direction Vectors
	double[][] U; // projection matrix
	double[] info; // information matrix

	public GrayVoxels() {
		voxelCount = 0;
		curValSum = 0.0;
		curSqSum = 0.0;
		label = -1;
		labelType = 0;
	}

	public GrayVoxels(final double cLabel) {
		voxelCount = 0;
		curValSum = 0.0;
		curSqSum = 0.0;
		label = cLabel;
		labelType = 2;
	}

	public GrayVoxels(final int cLabel) {
		voxelCount = 0;
		curValSum = 0.0;
		curSqSum = 0.0;
		label = cLabel;
		labelType = 1;
	}

	/**
	 * Method to add two grayvoxels elements together A+=B -> A.add(B),
	 * non-communative as properties are inheritied from A
	 */
	public void add(final GrayVoxels gvAdd) {
		cx += gvAdd.cx;
		cy += gvAdd.cy;
		cz += gvAdd.cz;

		wx += gvAdd.wx;
		wy += gvAdd.wy;
		wz += gvAdd.wz;

		curValSum += gvAdd.curValSum;
		curSqSum += gvAdd.curSqSum;
		voxelCount += voxelCount;
	}

	/**
	 * Calculate the covariances without a weight
	 * @param x the 
	 * @param y
	 * @param z
	 * @return
	 */
	public int addCovVox(final double x, final double y, final double z) {
		if (useWeights)
			return addCovVox(x, y, z, 1.0);
		calcCOV();
		// For standard deviations
		cx2 += Math.pow(x - c_meanx, 2);
		cy2 += Math.pow(y - c_meany, 2);
		cz2 += Math.pow(z - c_meanz, 2);
		// For Inertial Tensor
		cxy += (x - c_meanx) * (y - c_meany);
		cxz += (x - c_meanx) * (z - c_meanz);
		cyz += (y - c_meany) * (z - c_meanz);

		// For Radius
		cr += Math.sqrt(Math.pow(x - c_meanx, 2) + Math.pow(y - c_meany, 2)
				+ Math.pow(z - c_meanz, 2));
		csr += Math.pow(x - c_meanx, 2) + Math.pow(y - c_meany, 2)
				+ Math.pow(z - c_meanz, 2);
		return 1;
	}


	/** Function which uses the weights to calculate the covariances
	 * 
	 * @param x position
	 * @param y position
	 * @param z position
	 * @param pixVal the value of the pixel
	 * @return
	 */
	public int addCovVox(final double x, final double y, final double z,
			final double pixVal) {
		if (!useWeights)
			return addCovVox(x, y, z);

		calcCOV();
		// For standard deviations
		cx2 += pixVal * Math.pow(x - c_meanx, 2);
		cy2 += pixVal * Math.pow(y - c_meany, 2);
		cz2 += pixVal * Math.pow(z - c_meanz, 2);
		// For Inertial Tensor
		cxy += pixVal * (x - c_meanx) * (y - c_meany);
		cxz += pixVal * (x - c_meanx) * (z - c_meanz);
		cyz += pixVal * (y - c_meany) * (z - c_meanz);

		// For Radius
		cr += pixVal
				* Math.sqrt(Math.pow(x - c_meanx, 2) + Math.pow(y - c_meany, 2)
						+ Math.pow(z - c_meanz, 2));
		csr += pixVal * Math.pow(x - c_meanx, 2) + Math.pow(y - c_meany, 2)
				+ Math.pow(z - c_meanz, 2);
		return 1;
	}

	public void addVox(final double pixVal) {
		voxelCount++;
		curValSum += pixVal;
		curSqSum += Math.pow(pixVal, 2);
		if (voxelCount == 1) {
			minVal = pixVal;
			maxVal = pixVal;
		}
		if (pixVal > maxVal)
			maxVal = pixVal;
		if (pixVal < minVal)
			minVal = pixVal;
	}

	public void addVox(final double x, final double y, final double z,
			final double pixVal) {
		cx += x;
		cy += y;
		cz += z;

		wx += x * pixVal;
		wy += y * pixVal;
		wz += z * pixVal;

		if (noRecenter)
			addCovVox(x, y, z, pixVal);

		addVox(pixVal);
	}

	public double angVec(final double v1, final double v2, final double v3) {
		// Correct the distance map orientation factor
		// Now using correct factor which is : 2 w_x (w_x+1) /

		final double wx = (rangex() + 1) / 2.0;
		final double wy = (rangey() + 1) / 2.0;
		final double wz = (rangez() + 1) / 2.0;
		final double cx = gradx() / (2 * wx * (wx + 1));
		final double cy = grady() / (2 * wy * (wy + 1));
		final double cz = gradz() / (2 * wz * (wz + 1));

		double sVal1 = v1 * cx + v2 * cy + v3 * cz;
		sVal1 /= (Math.sqrt(v1 * v1 + v2 * v2 + v3 * v3) * Math.sqrt(cx * cx
				+ cy * cy + cz * cz));

		return Math.toDegrees(Math.min(Math.acos(sVal1), Math.acos(-sVal1)));
	}

	public double angVec(final int compNum) {
		if (diagRun) {
			return angVec(getComp(compNum)[0], getComp(compNum)[1],
					getComp(compNum)[2]);
		} else {
			return -1;
		}
	}

	public void calcCOV() {
		if (noRecenter) {
			c_meanx = 0;
			c_meany = 0;
			c_meanz = 0;
			cached_mean = voxelCount;
		} else if (cached_mean < voxelCount) {
			if (useWeights) {
				c_meanx = wx / curValSum;
				c_meany = wy / curValSum;
				c_meanz = wz / curValSum;
				cached_mean = voxelCount;
			} else {
				c_meanx = cx / voxelCount;
				c_meany = cy / voxelCount;
				c_meanz = cz / voxelCount;
				cached_mean = voxelCount;
			}
		}
	}

	public double calculateBoxDist(final double minX, final double minY,
			final double minZ, final double maxX, final double maxY,
			final double maxZ) {
		// Calculate the distance away from the edge of the region of interest
		// (much faster than distance transform when object occupies the
		// entire field of view)
		double boxDist = 0;
		if (Math.abs(meanx() - minX) < Math.abs(maxX - meanx()))
			boxDist += Math.pow(meanx() - minX, 2);
		else
			boxDist += Math.pow(maxX - meanx(), 2);

		if (Math.abs(meany() - minY) < Math.abs(maxY - meany()))
			boxDist += Math.pow(meany() - minY, 2);
		else
			boxDist += Math.pow(maxY - meany(), 2);

		if (Math.abs(meanz() - minZ) < Math.abs(maxZ - meanz()))
			boxDist += Math.pow(meanz() - minZ, 2);
		else
			boxDist += Math.pow(maxZ - meanz(), 2);

		boxDist = Math.sqrt(boxDist);

		curValSum = boxDist * voxelCount;
		curSqSum = Math.pow(boxDist, 2) * voxelCount;
		minVal = boxDist;
		maxVal = boxDist;
		wx = meanx() * curValSum;
		wy = meany() * curValSum;
		wz = meanz() * curValSum;
		
		return boxDist;

	}

	public double count() {
		return voxelCount;
	}

	public String diag() {
		return diag(false);
	}

	/**
	 * Perform the inertial tensor calculations and diagnolization and calculate
	 * eigenvectors and values
	 **/
	public String diag(final boolean printResults) {

		// Create Inertial Tensor
		final double[][] IT = getTensor(); // inertial tensor
		if (IT == null)
			return "Failed Horrible! Returning!";
		// Calculate Eigentransform

		final EigenvalueDecomposition e = new EigenvalueDecomposition(
				new Matrix(IT));
		U = e.getV().transpose().getArray();
		info = e.getRealEigenvalues(); // covariance matrix is symetric, so only
										// real eigenvalues...
		final double[] newU = U[0];
		final double tInfo = info[0];
		// Swap last and first so first is most significant
		U[0] = U[2];
		U[2] = newU;
		info[0] = info[2];
		info[2] = tInfo;

		for (int cI = 0; cI < 3; cI++)
			info[cI] = Math.sqrt(info[cI]);// take the sqrt of the eigenvalues
		String curLine = "Inertial Tensor\t\tPCA\t Value\n";
		diagRun = true;
		if (printResults) {
			for (int cI = 0; cI < 3; cI++) {
				curLine += "[";
				for (int cJ = 0; cJ < 3; cJ++)
					curLine += String.format("%.2f", IT[cI][cJ]) + ",";
				curLine += "];\t[";
				for (int cJ = 0; cJ < 3; cJ++)
					curLine += String.format("%.2f", getComp(cI)[cJ]) + ",";
				curLine += "]\t\t P:" + getScore(cI);
				curLine += "\n";
			}
			curLine += "Anisotropy:"
					+ String.format("%.2f", (getScore(0) - getScore(2))
							/ getScore(0) * 100)
					+ "%,\tOblateness:"
					+ String.format("%.2f", (getScore(0) - getScore(1))
							/ (getScore(0) - getScore(2)) * 2 - 1) + "\n";
		}
		return curLine;

	}

	public double[] getComp(final int compNum) {
		return U[compNum];
	}

	public double getLabel() {
		return label;
	}

	public double getScore(final int compNum) {
		return info[compNum];
	}

	public double[][] getTensor() {
		double[][] IT; // inertial tensor
		double divFactor;
		if (useWeights)
			divFactor = curValSum;
		else
			divFactor = voxelCount;
		IT = new double[3][3];

		IT[0][0] = cx2 / divFactor;
		IT[0][1] = cxy / divFactor;
		IT[0][2] = cxz / divFactor;

		IT[1][0] = cxy / divFactor;
		IT[1][1] = cy2 / divFactor;
		IT[1][2] = cyz / divFactor;

		IT[2][0] = cxz / divFactor;
		IT[2][1] = cyz / divFactor;
		IT[2][2] = cz2 / divFactor;
		if (divFactor == 0) {
			System.out.println("Failed Horrible! Divide by zero" + divFactor);
			return null;
		}
		return IT;

	}

	public String getTensorString() {
		return getTensorString(", ");
	}

	public String getTensorString(final String dlmChar) {
		final double[][] IT = getTensor();
		String outString = "";
		if (IT == null)
			for (int iti = 0; iti < 3; iti++)
				for (int itj = 0; itj < 3; itj++)
					outString += dlmChar;
		else
			for (int iti = 0; iti < 3; iti++)
				for (int itj = 0; itj < 3; itj++)
					outString += dlmChar + IT[iti][itj];
		return outString;
	}

	public double gradx() {
		if (curValSum > 0)
			return wmeanx() - meanx();
		else
			return 0;
	}

	public double grady() {
		if (curValSum > 0)
			return wmeany() - meany();
		else
			return 0;
	}

	public double gradz() {
		if (curValSum > 0)
			return wmeanz() - meanz();
		else
			return 0;
	}

	public double max() {
		return maxVal;
	}

	public double mean() {
		if (voxelCount > 0)
			return curValSum / voxelCount;
		else
			return 0;
	}

	public double meanx() {
		calcCOV();
		return c_meanx;
	}

	public double meany() {
		calcCOV();
		return c_meany;
	}

	public double meanz() {
		calcCOV();
		return c_meanz;
	}

	public double min() {
		return minVal;
	}

	public double radius() {
		if (voxelCount > 0) {
			if (useWeights)
				return cr / curValSum;// -Math.pow(meanx(),2));
			else
				return cr / voxelCount;// -Math.pow(meanx(),2));
		} else
			return -5;
	}

	public double rangep1() {
		return maxP1 - minP1;
	}

	public double rangep2() {
		return maxP2 - minP2;
	}

	public double rangep3() {
		return maxP3 - minP3;
	}

	public double rangex() {
		return maxX - minX;
	}

	public double rangey() {
		return maxY - minY;
	}

	public double rangez() {
		return maxZ - minZ;
	}

	public void setExtentsVoxel(final double x, final double y, final double z) {

		double p1 = 0;
		double p2 = 0;
		double p3 = 0;
		if (diagRun) {
			p1 = U[0][0] * x + U[0][1] * y + U[0][2] * z;
			p2 = U[1][0] * x + U[1][1] * y + U[1][2] * z;
			p3 = U[2][0] * x + U[2][1] * y + U[2][2] * z;
		}
		if (setMax) {
			if (x < minX)
				minX = x;
			if (y < minY)
				minY = y;
			if (z < minZ)
				minZ = z;

			if (p1 < minP1)
				minP1 = p1;
			if (p2 < minP2)
				minP2 = p2;
			if (p3 < minP3)
				minP3 = p3;

			if (x > maxX)
				maxX = x;
			if (y > maxY)
				maxY = y;
			if (z > maxZ)
				maxZ = z;

			if (p1 > maxP1)
				maxP1 = p1;
			if (p2 > maxP2)
				maxP2 = p2;
			if (p3 > maxP3)
				maxP3 = p3;

		} else {
			minX = x;
			maxX = x;

			minY = y;
			maxY = y;

			minZ = z;
			maxZ = z;

			minP1 = p1;
			maxP1 = p1;

			minP2 = p2;
			maxP2 = p2;

			minP3 = p3;
			maxP3 = p3;
			setMax = true;
		}
	}

	public double std() {
		if (voxelCount > 0)
			return Math.sqrt(curSqSum / voxelCount - Math.pow(mean(), 2));
		else
			return -1;
	}

	public double stdr() {
		if (voxelCount > 0) {
			if (useWeights)
				return Math.sqrt(csr / curValSum - Math.pow(radius(), 2));
			else
				return Math.sqrt(csr / voxelCount - Math.pow(radius(), 2));
		} else
			return -5;
	}

	public double stdx() {
		if (voxelCount > 0) {
			if (useWeights)
				return Math.sqrt(cx2 / curValSum);// -Math.pow(meanx(),2));
			else
				return Math.sqrt(cx2 / voxelCount);// -Math.pow(meanx(),2));
		} else
			return -5;
	}

	public double stdy() {
		if (voxelCount > 0) {
			if (useWeights)
				return Math.sqrt(cy2 / curValSum);// -Math.pow(meany(),2));
			else
				return Math.sqrt(cy2 / voxelCount);// -Math.pow(meany(),2));
		} else
			return -5;
	}

	public double stdz() {
		if (voxelCount > 0) {
			if (useWeights)
				return Math.sqrt(cz2 / curValSum);// -Math.pow(meanz(),2));
			else
				return Math.sqrt(cz2 / voxelCount);// -Math.pow(meanz(),2));
		} else
			return -5;
	}
	public static String getHeaderString(String analysisName,boolean addDist,boolean includeShapeTensor) {
		String headerStr = "//LACUNA_NUMBER, TRIANGLES, SCALE_X, SCALE_Y, SCALE_Z, POS_X, POS_Y, POS_Z , STD_X, STD_Y, STD_Z,"+
				"PROJ_X, PROJ_Y, PROJ_Z, PCA1_X, PCA1_Y, PCA1_Z, PCA1_S, PCA2_X, PCA2_Y, PCA2_Z, PCA2_S, PCA3_X, PCA3_Y, PCA3_Z, PCA3_S,"+
				"PROJ_PCA1, PROJ_PCA2, PROJ_PCA3,"+
				"OBJ_RADIUS, OBJ_RADIUS_STD, VOLUME, VOLUME_BOX";
		if (addDist) {
            headerStr += ", " + analysisName + "_Grad_X,"
                    + analysisName + "_Grad_Y," + analysisName
                    + "_Grad_Z," + analysisName + "_Angle";
            headerStr += ", " + analysisName
                    + "_Distance_Mean," + analysisName
                    + "_Distance_COV, " + analysisName
                    + "_Distance_STD";
        }
        if (includeShapeTensor) {
            for (final char cX : "XYZ".toCharArray()) {
                for (final char cY : "XYZ".toCharArray()) {
                    headerStr += ", SHAPET_" + cX + "" + cY;
                }
            }
        }
        return headerStr;
	}
	public String mkString(final D3float elSize,boolean addDist,boolean includeShapeTensor) {
		
		// voxels
        String lacString = "";
        lacString = getLabel() + ", 0 ," + elSize.x
                + "," + elSize.y + ","
                + elSize.z;
        // Position
        lacString += "," + meanx() + ","
                + meany() + ","
                + meanz();
        // STD
        lacString += "," + stdx() + ","
                + stdy() + ","
                + stdz();
        // Projection XYZ
        lacString += "," + rangex() + ","
                + rangey() + ","
                + rangez();
        // PCA Components
        for (int cpca = 0; cpca < 3; cpca++) {
            lacString += ","
                    + getComp(cpca)[0]
                    + ","
                    + getComp(cpca)[1]
                    + ","
                    + getComp(cpca)[2]
                    + "," + getScore(cpca);
        }
        lacString += "," + rangep1() + ","
                + rangep2() + ","
                + rangep3();
        // Radius
        lacString += "," + radius() + ","
                + stdr();
        // Volume
        lacString += ","
                + count()
                + ","
                + (rangep1() * rangep2() * rangep3());
        if (addDist) {
            // Grad X,y,z, angle
            lacString += ", " + gradx()
                    + ", " + grady() + ","
                    + gradz() + ","
                    + angVec(0);
            // Distance mean, cov, std
            lacString += ", " +mean()
                    + "," + mean() + ","
                    + std();
        }
        if (includeShapeTensor) lacString += getTensorString();
        return lacString;
	}

	@Override
	public String toString() {
		return toString(", ");
	}

	public String toString(final String curDLM) {
		String outString = "";

		if (labelType > 0) {
			if (labelType == 1)
				outString += ((int) label);
			else
				outString += label;
			outString += curDLM;
		}
		outString += voxelCount;
		return outString;

	}

	public double wmeanx() {
		if (curValSum > 0)
			return wx / curValSum;
		else
			return 0;
	}

	public double wmeany() {
		if (curValSum > 0)
			return wy / curValSum;
		else
			return 0;
	}

	public double wmeanz() {
		if (curValSum > 0)
			return wz / curValSum;
		else
			return 0;
	}
}
