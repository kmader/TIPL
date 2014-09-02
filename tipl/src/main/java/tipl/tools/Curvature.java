package tipl.tools;

/**
 * <p>Title: Principal Curvature Plugin for TIPL</p>
 *
 * <p>Description: Computes the Principal Curvatures of for 2D and 3D images except the pixels/voxels directly at the borders of the image</p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: MPI-CBG</p>
 *
 * <p>License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * @author Stephan Preibisch, Modified by Kevin Mader
 * @version 1.0
 */

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.settings.FilterSettings;
import tipl.util.*;

/**
 * Curvature computes the curvatures at every point in an image based on a
 * gaussian filter
 */
public class Curvature extends BaseTIPLPluginIO {
    @TIPLPluginManager.PluginInfo(pluginType = "Curvature",
            desc = "Full memory curvature (using VFIlterScale)",
            sliceBased = false,
            bytesPerVoxel = -1)
    final public static TIPLPluginManager.TIPLPluginFactory myFactory = new TIPLPluginManager.TIPLPluginFactory() {
        @Override
        public ITIPLPlugin get() {
            return new Curvature();
        }
    };
    protected GrayVoxels gv;
    protected GrayVoxels[] gvresult;
    double sigma = 0.5;
    float processThresh = 0.5f;
    boolean internalGaussian = false;
    private FloatArray3D data3D;
    private FloatArray3D result3D[];
    private double minV = Double.MAX_VALUE, maxV = Double.MIN_VALUE;

    /**
     * internal initializer only for the the plugin generating code *
     */
    protected Curvature() {

    }

    /**
     * This function only works with aim data
     */
    public Curvature(final TImgRO inImg, final float iprocessThresh) {
        ImportData(inImg);
        processThresh = iprocessThresh;
    }

    /**
     * This method creates a gaussian kernel
     *
     * @param sigma     Standard Derivation of the gaussian function
     * @param normalize Normalize integral of gaussian function to 1 or not...
     * @return float[] The gaussian kernel
     * @author Stephan Saalfeld
     */
    public static float[] createGaussianKernel1D(final float sigma,
                                                 final boolean normalize) {
        int size = 3;
        float[] gaussianKernel;

        if (sigma <= 0) {
            gaussianKernel = new float[3];
            gaussianKernel[1] = 1;
        } else {
            size = max(3, 2 * (int) (3 * sigma + 0.5) + 1);

            final float two_sq_sigma = 2 * sigma * sigma;
            gaussianKernel = new float[size];

            for (int x = size / 2; x >= 0; --x) {
                final float val = (float) Math.exp(-(float) (x * x)
                        / two_sq_sigma);

                gaussianKernel[size / 2 - x] = val;
                gaussianKernel[size / 2 + x] = val;
            }
        }

        if (normalize) {
            float sum = 0;

            for (float aGaussianKernel : gaussianKernel) sum += aGaussianKernel;

			/*
             * for (float value : gaussianKernel) sum += value;
			 */

            for (int i = 0; i < gaussianKernel.length; i++)
                gaussianKernel[i] /= sum;
        }

        return gaussianKernel;
    }

    public static void main(final String[] args) {
        final String kVer = "120326_002";
        System.out.println("Curvature (filter/voxel-based) v" + kVer);
        System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");

        final ArgumentParser p = TIPLGlobal.activeParser(args);
        final TypedPath inputFile = p.getOptionPath("input", "",
                "Input distance map image");

        // boolean doPeel =
        // p.getOptionBoolean("peel","Run peel afterwards (removes edges?) ");
        final int myUp = p.getOptionInt("upsample", 1, "Upsample dimensions");
        final int myDown = p.getOptionInt("downsample", 1,
                "Downsample Factor dimensions");
        final double mySigma = p.getOptionDouble("sigma", 0.5 * myUp,
                "Sigma (for filter)");
        final double myThresh = p
                .getOptionDouble(
                        "thresh",
                        0.5,
                        "Threshold for processing data (1.0 is only full points, 0.5 is all values which round to a full point))");
        final TypedPath outputFile = p.getOptionPath("output", "curvemap.tif",
                "Output thickness image");

        if (p.hasOption("?")) {
            System.out.println("Curvature v" + kVer);
            System.out.println(" Calculates curvature from filtered image v"
                    + kVer);
            System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
            System.out.println(" Arguments::");
            System.out.println(" ");
            System.out.println(p.getHelp());
            System.exit(0);
        }

        if (inputFile.length() > 0) { // Read in labels
            System.out.println("Loading " + inputFile + " ...");
            final TImg inputAim = TImgTools.ReadTImg(inputFile);
            final TImg outputAim = RunCC(inputAim, mySigma, myUp, myDown,
                    (float) myThresh);
            TImgTools.WriteTImg(outputAim, outputFile);

        }

    }

    /**
     * This static method is designed to perform the entire functionality of
     * curvature, running the filtering using filterscale and then performing
     * the curvature analysis
     *
     * @param inputAim  Input Image
     * @param isig      Sigma value for gaussian filter
     * @param upV       Upscale factor (see FilterScale)
     * @param dnV       Downscale factor
     * @param peelImage Peel image as to only keep curvature values inside the
     *                  original image (object rather than background curvature)
     * @author Kevin Mader
     */
    public static TImg RunCC(final TImg inputAim, final double isig,
                             final int upV, final int dnV, final float processThresh) {
        final TImgRO.FullReadable finputAim = TImgTools
                .makeTImgFullReadable(inputAim);

        finputAim.getBoolAim(); // make it into a boolean array before reading
        // it as a float array
        
        final ITIPLPluginIO fs = TIPLPluginManager.createBestPluginIO("Filter", new TImgRO[] {finputAim});
        fs.LoadImages( new TImgRO[] {finputAim});
        final D3int ds = new D3int(dnV,dnV,dnV);
        final D3int up = new D3int(upV,upV,upV);
        fs.setParameter("-upfactor="+up+" -downfactor="+ds+" -filtersetting="+isig+" -filter="+FilterSettings.GAUSSIAN);
        fs.execute();


        final TImg floatImg = fs.ExportImages(inputAim)[0];

        final Curvature ccPlug = new Curvature(floatImg, processThresh);
        ccPlug.sigma = isig * upV / dnV;
        ccPlug.execute();
        final TImg outAim = ccPlug.CreateOutputImage(floatImg, 0);
        // if (peelImage) outAim=(new Peel(outAim,inMask,0)).ExportAim(outAim);
        return outAim;
    }

    /**
     * This method computes the Eigenvalues of the Hessian Matrix, the
     * Eigenvalues correspond to the Principal Curvatures<br>
     * <br>
     * Note: If the Eigenvalues contain imaginary numbers, this method will
     * return null
     *
     * @param matrix The hessian Matrix
     * @return double[] The Real Parts of the Eigenvalues or null (if there were
     * imganiary parts)
     * @author Stephan Preibisch
     */
    public double[] computeEigenValues(final double[][] matrix) {
        final Matrix M = new Matrix(matrix);
        final EigenvalueDecomposition E = new EigenvalueDecomposition(M);

        final double[] result = E.getImagEigenvalues();

        boolean found = false;

        for (double aResult : result)
            if (aResult > 0)
                found = true;

        if (found)
            return null;
        else
            return E.getRealEigenvalues();
    }

    public double[][] computeEigenVectors(final double[][] matrix) {
        final Matrix M = new Matrix(matrix);
        final EigenvalueDecomposition E = new EigenvalueDecomposition(M);

        final double[][] result = E.getV().transpose().getArray();

        return result;
    }

    /**
     * This method does the gaussian filtering of an image. On the edges of the
     * image it does mirror the pixels. It also uses the seperability of the
     * gaussian convolution.
     *
     * @param input FloatProcessor which will be folded (will not be touched)
     * @param sigma Standard Derivation of the gaussian function
     * @return FloatProcessor The folded image
     * @author Stephan Preibisch
     */
    public FloatArray2D computeGaussianFastMirror(final FloatArray2D input,
                                                  final float sigma) {
        final FloatArray2D output = new FloatArray2D(input.width, input.height);

        float avg, kernelsum = 0;
        final float[] kernel = createGaussianKernel1D(sigma, true);
        final int filterSize = kernel.length;

        // get kernel sum
        /*
		 * for (double value : kernel) kernelsum += value;
		 */
        for (float aKernel : kernel) kernelsum += aKernel;

        // fold in x
        for (int x = 0; x < input.width; x++)
            for (int y = 0; y < input.height; y++) {
                avg = 0;

                if (x - filterSize / 2 >= 0 && x + filterSize / 2 < input.width)
                    for (int f = -filterSize / 2; f <= filterSize / 2; f++)
                        avg += input.get(x + f, y) * kernel[f + filterSize / 2];
                else
                    for (int f = -filterSize / 2; f <= filterSize / 2; f++)
                        avg += input.getMirror(x + f, y)
                                * kernel[f + filterSize / 2];

                output.set(avg / kernelsum, x, y);

            }

        // fold in y
        for (int x = 0; x < input.width; x++) {
            final float[] temp = new float[input.height];

            for (int y = 0; y < input.height; y++) {
                avg = 0;

                if (y - filterSize / 2 >= 0
                        && y + filterSize / 2 < input.height)
                    for (int f = -filterSize / 2; f <= filterSize / 2; f++)
                        avg += output.get(x, y + f)
                                * kernel[f + filterSize / 2];
                else
                    for (int f = -filterSize / 2; f <= filterSize / 2; f++)
                        avg += output.getMirror(x, y + f)
                                * kernel[f + filterSize / 2];

                temp[y] = avg / kernelsum;
            }

            for (int y = 0; y < input.height; y++)
                output.set(temp[y], x, y);
        }

        return output;
    }

    /**
     * This method does the gaussian filtering of an 3D image. On the edges of
     * the image it does mirror the pixels. It also uses the seperability of the
     * gaussian convolution.
     *
     * @param input FloatProcessor which will be folded (will not be touched)
     * @param sigma Standard Derivation of the gaussian function
     * @return FloatProcessor The folded image
     * @author Stephan Preibisch
     */
    public FloatArray3D computeGaussianFastMirror(final FloatArray3D input,
                                                  final float sigma) {
        final FloatArray3D output = new FloatArray3D(input.width, input.height,
                input.depth);

        float avg, kernelsum = 0;
        final float[] kernel = createGaussianKernel1D(sigma, true);
        final int filterSize = kernel.length;

        // get kernel sum
		/*
		 * for (double value : kernel) kernelsum += value;
		 */
        for (float aKernel : kernel) kernelsum += aKernel;

        // fold in x
        for (int x = 0; x < input.width; x++)
            for (int y = 0; y < input.height; y++)
                for (int z = 0; z < input.depth; z++) {
                    avg = 0;

                    if (x - filterSize / 2 >= 0
                            && x + filterSize / 2 < input.width)
                        for (int f = -filterSize / 2; f <= filterSize / 2; f++)
                            avg += input.get(x + f, y, z)
                                    * kernel[f + filterSize / 2];
                    else
                        for (int f = -filterSize / 2; f <= filterSize / 2; f++)
                            avg += input.getMirror(x + f, y, z)
                                    * kernel[f + filterSize / 2];

                    output.set(avg / kernelsum, x, y, z);

                }

        // fold in y
        for (int x = 0; x < input.width; x++)
            for (int z = 0; z < input.depth; z++) {
                final float[] temp = new float[input.height];

                for (int y = 0; y < input.height; y++) {
                    avg = 0;

                    if (y - filterSize / 2 >= 0
                            && y + filterSize / 2 < input.height)
                        for (int f = -filterSize / 2; f <= filterSize / 2; f++)
                            avg += output.get(x, y + f, z)
                                    * kernel[f + filterSize / 2];
                    else
                        for (int f = -filterSize / 2; f <= filterSize / 2; f++)
                            avg += output.getMirror(x, y + f, z)
                                    * kernel[f + filterSize / 2];

                    temp[y] = avg / kernelsum;
                }

                for (int y = 0; y < input.height; y++)
                    output.set(temp[y], x, y, z);
            }

        // fold in z
        for (int x = 0; x < input.width; x++)
            for (int y = 0; y < input.height; y++) {
                final float[] temp = new float[input.depth];

                for (int z = 0; z < input.depth; z++) {
                    avg = 0;

                    if (z - filterSize / 2 >= 0
                            && z + filterSize / 2 < input.depth)
                        for (int f = -filterSize / 2; f <= filterSize / 2; f++)
                            avg += output.get(x, y, z + f)
                                    * kernel[f + filterSize / 2];
                    else
                        for (int f = -filterSize / 2; f <= filterSize / 2; f++)
                            avg += output.getMirror(x, y, z + f)
                                    * kernel[f + filterSize / 2];

                    temp[z] = avg / kernelsum;
                }

                for (int z = 0; z < input.depth; z++)
                    output.set(temp[z], x, y, z);

            }
        return output;
    }

    /**
     * This method computes the Hessian Matrix for the 3x3 environment of a
     * certain pixel <br>
     * <br>
     * <p/>
     * The 3D Hessian Matrix:<br>
     * xx xy <br>
     * yx yy <br>
     *
     * @param img The image as FloatArray3D
     * @param x   The x-position of the voxel
     * @param y   The y-position of the voxel
     * @return double[][] The 2D - Hessian Matrix
     * @author Stephan Preibisch
     */
    public double[][] computeHessianMatrix2D(final FloatArray2D laPlace,
                                             final int x, final int y, final double sigma) {
        final double[][] hessianMatrix = new double[2][2]; // zeile, spalte

        final double temp = 2 * laPlace.get(x, y);

        // xx
        hessianMatrix[0][0] = laPlace.get(x + 1, y) - temp
                + laPlace.get(x - 1, y);

        // yy
        hessianMatrix[1][1] = laPlace.get(x, y + 1) - temp
                + laPlace.get(x, y - 1);

        // xy
        hessianMatrix[0][1] = hessianMatrix[1][0] = ((laPlace.get(x + 1, y + 1) - laPlace
                .get(x - 1, y + 1)) / 2 - (laPlace.get(x + 1, y - 1) - laPlace
                .get(x - 1, y - 1)) / 2) / 2;

        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++)
                hessianMatrix[i][j] *= (sigma * sigma);

        return hessianMatrix;
    }

    /**
     * This method computes the Hessian Matrix for the 3x3x3 environment of a
     * certain voxel <br>
     * <br>
     * <p/>
     * The 3D Hessian Matrix:<br>
     * xx xy xz <br>
     * yx yy yz <br>
     * zx zy zz <br>
     *
     * @param img The image as FloatArray3D
     * @param x   The x-position of the voxel
     * @param y   The y-position of the voxel
     * @param z   The z-position of the voxel
     * @return double[][] The 3D - Hessian Matrix
     * @author Stephan Preibisch
     */
    public double[][] computeHessianMatrix3D(final FloatArray3D img,
                                             final int x, final int y, final int z, final double sigma) {
        final double[][] hessianMatrix = new double[3][3]; // zeile, spalte

        final double temp = 2 * img.get(x, y, z);

        // xx
        hessianMatrix[0][0] = img.get(x + 1, y, z) - temp
                + img.get(x - 1, y, z);

        // yy
        hessianMatrix[1][1] = img.get(x, y + 1, z) - temp
                + img.get(x, y - 1, z);

        // zz
        hessianMatrix[2][2] = img.get(x, y, z + 1) - temp
                + img.get(x, y, z - 1);

        // xy
        hessianMatrix[0][1] = hessianMatrix[1][0] = ((img.get(x + 1, y + 1, z) - img
                .get(x - 1, y + 1, z)) / 2 - (img.get(x + 1, y - 1, z) - img
                .get(x - 1, y - 1, z)) / 2) / 2;

        // xz
        hessianMatrix[0][2] = hessianMatrix[2][0] = ((img.get(x + 1, y, z + 1) - img
                .get(x - 1, y, z + 1)) / 2 - (img.get(x + 1, y, z - 1) - img
                .get(x - 1, y, z - 1)) / 2) / 2;

        // yz
        hessianMatrix[1][2] = hessianMatrix[2][1] = ((img.get(x, y + 1, z + 1) - img
                .get(x, y - 1, z + 1)) / 2 - (img.get(x, y + 1, z - 1) - img
                .get(x, y - 1, z - 1)) / 2) / 2;

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                hessianMatrix[i][j] *= (sigma * sigma);

        return hessianMatrix;
    }

    public double[] computeTraceAndDeterminant(final double[][] matrix) {
        final Matrix M = new Matrix(matrix);
        final double[] result = new double[2];
        result[0] = M.trace();
        result[1] = M.det();
        return result;
    }

    protected void curvature(final int startSlice, final int lastSlice) {
        for (int z = startSlice; z < lastSlice; z++) {
            for (int y = 1; y < data3D.height - 1; y++)
                for (int x = 1; x < data3D.width - 1; x++) {
                    if (data3D.get(x, y, z) > processThresh) {
                        final double[][] hessianMatrix = computeHessianMatrix3D(
                                data3D, x, y, z, sigma);
                        final double[] eigenValues = computeEigenValues(hessianMatrix);

                        final double[] trcDet = computeTraceAndDeterminant(hessianMatrix);

                        // there were imaginary numbers
                        if (eigenValues == null) {
                            result3D[0].set(0, x, y, z);
                            result3D[1].set(0, x, y, z);
                            result3D[2].set(0, x, y, z);

                            if (0 < minV)
                                minV = 0;
                            if (0 > maxV)
                                maxV = 0;
                        } else {
                            // result3D[0].set((float)eigenValues[0], x, y, z);
                            result3D[0].set((float) trcDet[0], x, y, z);
                            final double[][] eigenVectors = computeEigenVectors(hessianMatrix);
                            /**
                             * if (Math.abs(eigenVectors[2][0])>Math.abs(
                             * eigenVectors[2][1])) { if
                             * (Math.abs(eigenVectors[2
                             * ][0])>Math.abs(eigenVectors[2][2])) {
                             * result3D[0].set(1f, x, y, z); } else {
                             * result3D[0].set(3f, x, y, z); } } else { if
                             * (Math.
                             * abs(eigenVectors[2][1])>Math.abs(eigenVectors
                             * [2][2])) { result3D[0].set(2f, x, y, z); } else {
                             * result3D[0].set(3f, x, y, z); } }
                             */
                            // result3D[0].set((float)
                            // Math.atan2(eigenVectors[2][1],eigenVectors[2][0]),
                            // x, y, z);
                            gvresult[0].addVox(0, 0, 0, (float) trcDet[0]);
                            result3D[1].set((float) trcDet[1], x, y, z);
                            gvresult[1].addVox(0, 0, 0, (float) trcDet[1]);
                            // result3D[1].set((float)eigenValues[1], x, y, z);
                            result3D[2].set((float) eigenValues[2], x, y, z);
                            gvresult[2].addVox(0, 0, 0, (float) eigenValues[2]);
                            gvresult[3].addVox(0, 0, 0, (float) eigenValues[1]);
                            gvresult[4].addVox(0, 0, 0, (float) eigenValues[0]);

                            gv.addVox(0, 0, 0, 1.0f);

                            gv.addCovVox(eigenVectors[2][0],
                                    eigenVectors[2][1], eigenVectors[2][2],
                                    1.0f);
                            // IJ.log(eigenVectors[2][0]+","+eigenVectors[2][1]+","+eigenVectors[2][2]);

                            if (eigenValues[2] < minV)
                                minV = eigenValues[2];
                            if (eigenValues[2] > maxV)
                                maxV = eigenValues[2];
                        }
                    }

                }

            // System.out.println(z+" of "+data3D.depth);
        }

    }

    /**
     * Object to divide the thread work into supportCores equal parts, default
     * is z-slices, customized to use the ranges specified by the fancy
     * floatarray tool
     */
    @Override
    public Object divideThreadWork(final int cThread) {
        return BaseTIPLPluginIn.sliceProcessWork(cThread, neededCores(), 1, data3D.depth - 1, 3);
    }

    @Override
    public boolean execute() {
        if (runMulticore()) {
            runCount += 1;
            procLog += "TIPL:Curvature(Sigma,Thresh):(" + sigma + ","
                    + processThresh + ")\n";
            String testLog = "Hessian Trace (Mean,Std):" + gvresult[0].mean()
                    + ", " + gvresult[0].std() + "\n";
            testLog += "Gaussian Determinant  (Mean,Std):" + gvresult[1].mean()
                    + ", " + gvresult[1].std() + "\n";
            testLog += "Principal Eigenvalue (Mean,Std):" + gvresult[2].mean()
                    + ", " + gvresult[2].std() + "\n";
            testLog += "Secondary Eigenvalue (Mean,Std):" + gvresult[3].mean()
                    + ", " + gvresult[3].std() + "\n";
            testLog += "Tertiary Eigenvalue (Mean,Std):" + gvresult[4].mean()
                    + ", " + gvresult[4].std() + "\n";
            testLog += gv.diag(true);
            procLog += testLog + "\n";
            System.out.println(testLog);
        }
        return true;
    }

    /**
     * export result to an aim structure
     *
     * @param exportType type of image to export (0 = trace / mean curvature, 1 =
     *                   determinant / gaussian curvature, 2 = principal curvature
     *                   eigenvalue 1)
     *                   *
     */
    public TImg CreateOutputImage(final TImgRO.CanExport templateAim,
                                  final int exportType) {
        if (isInitialized) {
            if (runCount > 0) {
                TImg outAimData;
                /**
                 * // // Output the data //
                 *
                 * //for (int i = 0; i < 3; i++) FloatArrayToStack(result3D[0],
                 * "Trace ", (float)min, (float)max).show();
                 * FloatArrayToStack(result3D[1], "Determinant ", (float)min,
                 * (float)max).show(); FloatArrayToStack(result3D[2],
                 * "Eigenvalues 3", (float)min, (float)max).show();
                 */
                switch (exportType) {

                    case 0: // Trace, mean curvatue
                        outAimData = templateAim.inheritedAim(result3D[0].data,
                                dim, offset);
                        procLog += "\nOutput Image: Trace";
                        break;
                    case 1: // Determinant, gaussian curvature
                        outAimData = templateAim.inheritedAim(result3D[1].data,
                                dim, offset);
                        procLog += "\nOutput Image: Determinant, Gaussian Curvature";
                        break;
                    case 2: // Trace
                        outAimData = templateAim.inheritedAim(result3D[2].data,
                                dim, offset);
                        procLog += "\nOutput Image: Principal Eigenvalue, (" + minV
                                + ", " + maxV + ")";
                        break;
                    default:
                        outAimData = null;
                        System.err.println("Invalid export image type!!!");
                }
                outAimData.appendProcLog(procLog);
                return outAimData;
            } else {
                System.err
                        .println("The plug-in : "
                                + getPluginName()
                                + ", has not yet been run, exported does not exactly make sense, original data will be sent.");
                return templateAim.inheritedAim(data3D.data, dim, offset);
            }
        } else {
            System.err
                    .println("The plug-in : "
                            + getPluginName()
                            + ", has not yet been initialized, exported does not make any sense");
            return templateAim.inheritedAim(templateAim);
        }
    }

    @Override
    public String getPluginName() {
        return "ComputeCurvature";
    }

    protected void ImportData(final TImgRO inImg) {
        final D3int idim = inImg.getDim();
        final D3int ioffset = inImg.getOffset();
        final TImgRO.FullReadable inAim = TImgTools.makeTImgFullReadable(inImg);
        final float[] inputmap = inAim.getFloatAim();
        aimLength = inputmap.length;
        data3D = new FloatArray3D(inputmap, idim.x, idim.y, idim.z);
        init();
        InitDims(idim, ioffset);
    }

    /**
     * This method will be called when initializing the PlugIn, it coordinates
     * the main process.
     *
     * @param args UNUSED
     * @author Stephan Preibisch
     */
    public void init() {
        //
        // Compute Hessian Matrix and Principal curvatures for all pixels/voxels
        //
        System.out.println("Computing Principal Curvatures");
        result3D = new FloatArray3D[3];
        result3D[0] = new FloatArray3D(data3D.width, data3D.height,
                data3D.depth);
        result3D[1] = new FloatArray3D(data3D.width, data3D.height,
                data3D.depth);
        result3D[2] = new FloatArray3D(data3D.width, data3D.height,
                data3D.depth);
        gv = new GrayVoxels(0.0);
        gvresult = new GrayVoxels[5];
        for (int ii = 0; ii < 5; ii++)
            gvresult[ii] = new GrayVoxels(0.0);
        gv.useWeights = true;

    }

    @Override
    public void LoadImages(final TImgRO[] inImages) {
        if (inImages.length < 1)
            throw new IllegalArgumentException(
                    "Too few arguments for LoadImages in:" + getPluginName());
        final TImgRO inImg = inImages[0];

        ImportData(inImg);
    }

    /**
     * prefilter This method runs the original (nonparallel) gaussian filter on
     * the data
     *
     * @author Stephan Preibisch
     */
    public void prefilter() {
        System.out.println("Computing Gauss image");
        data3D = computeGaussianFastMirror(data3D, (float) sigma);
    }

    @Override
    public void processWork(final Object currentWork) {
        final int[] range = (int[]) currentWork;
        final int bSlice = range[0];
        final int tSlice = range[1];
        curvature(bSlice, tSlice);
    }

    @Override
    public TImg[] ExportImages(TImgRO templateImage) {
        TImgRO.CanExport templateAim = TImgTools.makeTImgExportable(templateImage);
        return new TImg[]{CreateOutputImage(templateAim, 0)};
    }

    /**
     * This class is the abstract class for my FloatArrayXDs, which are a one
     * dimensional structures with methods for access in n dimensions
     *
     * @author Stephan Preibisch
     */
    public abstract class FloatArray {
        public float data[] = null;

        @Override
        public abstract FloatArray clone();
    }

    /**
     * The 2D implementation of the FloatArray
     *
     * @author Stephan Preibisch
     */
    public class FloatArray2D extends FloatArray {
        public float data[] = null;
        public int width = 0;
        public int height = 0;

        public FloatArray2D(final float[] data, final int width,
                            final int height) {
            this.data = data;
            this.width = width;
            this.height = height;
        }

        public FloatArray2D(final int width, final int height) {
            data = new float[width * height];
            this.width = width;
            this.height = height;
        }

        @Override
        public FloatArray2D clone() {
            final FloatArray2D clone = new FloatArray2D(width, height);
            System.arraycopy(this.data, 0, clone.data, 0, this.data.length);
            return clone;
        }

        public float get(final int x, final int y) {
            return data[getPos(x, y)];
        }

        public float getMirror(int x, int y) {
            if (x >= width)
                x = width - (x - width + 2);

            if (y >= height)
                y = height - (y - height + 2);

            if (x < 0) {
                int tmp = 0;
                int dir = 1;

                while (x < 0) {
                    tmp += dir;
                    if (tmp == width - 1 || tmp == 0)
                        dir *= -1;
                    x++;
                }
                x = tmp;
            }

            if (y < 0) {
                int tmp = 0;
                int dir = 1;

                while (y < 0) {
                    tmp += dir;
                    if (tmp == height - 1 || tmp == 0)
                        dir *= -1;
                    y++;
                }
                y = tmp;
            }

            return data[getPos(x, y)];
        }

        public int getPos(final int x, final int y) {
            return x + width * y;
        }

        public float getZero(final int x, final int y) {
            if (x >= width)
                return 0;

            if (y >= height)
                return 0;

            if (x < 0)
                return 0;

            if (y < 0)
                return 0;

            return data[getPos(x, y)];
        }

        public void set(final float value, final int x, final int y) {
            data[getPos(x, y)] = value;
        }
    }

    /**
     * The 3D implementation of the FloatArray
     *
     * @author Stephan Preibisch
     */
    public class FloatArray3D extends FloatArray {
        public float data[] = null;
        public int width = 0;
        public int height = 0;
        public int depth = 0;

        public FloatArray3D(final float[] data, final int width,
                            final int height, final int depth) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        public FloatArray3D(final int width, final int height, final int depth) {
            data = new float[width * height * depth];
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        @Override
        public FloatArray3D clone() {
            final FloatArray3D clone = new FloatArray3D(width, height, depth);
            System.arraycopy(this.data, 0, clone.data, 0, this.data.length);
            return clone;
        }

        public float get(final int x, final int y, final int z) {
            return data[getPos(x, y, z)];
        }

        public float getMirror(int x, int y, int z) {
            if (x >= width)
                x = width - (x - width + 2);

            if (y >= height)
                y = height - (y - height + 2);

            if (z >= depth)
                z = depth - (z - depth + 2);

            if (x < 0) {
                int tmp = 0;
                int dir = 1;

                while (x < 0) {
                    tmp += dir;
                    if (tmp == width - 1 || tmp == 0)
                        dir *= -1;
                    x++;
                }
                x = tmp;
            }

            if (y < 0) {
                int tmp = 0;
                int dir = 1;

                while (y < 0) {
                    tmp += dir;
                    if (tmp == height - 1 || tmp == 0)
                        dir *= -1;
                    y++;
                }
                y = tmp;
            }

            if (z < 0) {
                int tmp = 0;
                int dir = 1;

                while (z < 0) {
                    tmp += dir;
                    if (tmp == height - 1 || tmp == 0)
                        dir *= -1;
                    z++;
                }
                z = tmp;
            }

            return data[getPos(x, y, z)];
        }

        public int getPos(final int x, final int y, final int z) {
            return x + width * (y + z * height);
        }

        public FloatArray2D getXPlane(final int x) {
            final FloatArray2D plane = new FloatArray2D(height, depth);

            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    plane.set(this.get(x, y, z), y, z);

            return plane;
        }

        public float[][] getXPlane_float(final int x) {
            final float[][] plane = new float[height][depth];

            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    plane[y][z] = this.get(x, y, z);

            return plane;
        }

        public FloatArray2D getYPlane(final int y) {
            final FloatArray2D plane = new FloatArray2D(width, depth);

            for (int x = 0; x < width; x++)
                for (int z = 0; z < depth; z++)
                    plane.set(this.get(x, y, z), x, z);

            return plane;
        }

        public float[][] getYPlane_float(final int y) {
            final float[][] plane = new float[width][depth];

            for (int x = 0; x < width; x++)
                for (int z = 0; z < depth; z++)
                    plane[x][z] = this.get(x, y, z);

            return plane;
        }

        public FloatArray2D getZPlane(final int z) {
            final FloatArray2D plane = new FloatArray2D(width, height);

            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    plane.set(this.get(x, y, z), x, y);

            return plane;
        }

        public float[][] getZPlane_float(final int z) {
            final float[][] plane = new float[width][height];

            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    plane[x][y] = this.get(x, y, z);

            return plane;
        }

        public void set(final float value, final int x, final int y, final int z) {
            data[getPos(x, y, z)] = value;
        }

        public void setXPlane(final float[][] plane, final int x) {
            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    this.set(plane[y][z], x, y, z);
        }

        public void setXPlane(final FloatArray2D plane, final int x) {
            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    this.set(plane.get(y, z), x, y, z);
        }

        public void setYPlane(final float[][] plane, final int y) {
            for (int x = 0; x < width; x++)
                for (int z = 0; z < depth; z++)
                    this.set(plane[x][z], x, y, z);
        }

        public void setYPlane(final FloatArray2D plane, final int y) {
            for (int x = 0; x < width; x++)
                for (int z = 0; z < depth; z++)
                    this.set(plane.get(x, z), x, y, z);
        }

        public void setZPlane(final float[][] plane, final int z) {
            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    this.set(plane[x][y], x, y, z);
        }

        public void setZPlane(final FloatArray2D plane, final int z) {
            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    this.set(plane.get(x, y), x, y, z);
        }

    }


}
