/**
 *
 */
package tipl.ij.volviewer;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import tipl.util.D3int;

/**
 * @author mader
 */
public class StackVolume extends Volume {
    private ImagePlus imp;
    private ImageProcessor ip;

    /**
     * @param control
     * @param vv
     * @param dim
     */
    public StackVolume(Control control, Volume_Viewer vv, ImagePlus imObj) {
        super(control, vv, new D3int(imObj.getWidth(), imObj.getHeight(), imObj.getStackSize()));
        this.imp = imObj;

        ip = imp.getProcessor();

    }

    @Override
    protected void getMinMax() {
        min = ip.getMin();
        max = ip.getMax();

        Calibration cal = imp.getCalibration();

        if (cal != null) {
            if (cal.calibrated()) {

                min = cal.getCValue((int) min);
                max = cal.getCValue((int) max);

                double[] coef = cal.getCoefficients();
                if (coef != null) {
                    a = coef[0];
                    b = coef[1];
                }
            }
            if (control.zAspect == 1)
                control.zAspect = (double) (cal.pixelDepth / cal.pixelWidth);
        }

        if (Double.isNaN(control.zAspect)) {
        	control.zAspect=1;
        	System.err.println("Voxel sizes are not valid and causing meaningless aspect ratios in Z: "+cal);
        }
	        
        if (control.zAspect <=0.01f) {
        	System.err.println("Voxel sizes are not valid and causing meaningless aspect ratios in Z: "+cal);
        	control.zAspect = 0.01f;
        }
    }

    @Override
    protected void customReadVolume() {
        ImageStack stack = imp.getStack();
        int bitDepth = imp.getBitDepth();

        if (bitDepth == 8 || bitDepth == 16 || bitDepth == 32) {
            double scale = (double) (255f / (max - min));

            for (int z = 1; z <= depthV; z++) {
                IJ.showStatus("Reading stack, slice: " + z + "/" + depthV);
                IJ.showProgress(0.6 * z / depthV);

                byte[] bytePixels = null;
                short[] shortPixels = null;
                double[] doublePixels = null;

                if (bitDepth == 8)
                    bytePixels = (byte[]) stack.getPixels(z);
                else if (bitDepth == 16)
                    shortPixels = (short[]) stack.getPixels(z);
                else if (bitDepth == 32)
                    doublePixels = (double[]) stack.getPixels(z);

                int pos = 0;
                for (int y = 2; y < heightV + 2; y++) {
                    for (int x = 2; x < widthV + 2; x++) {
                        int val;

                        if (bitDepth == 32) {
                            double value = (double) (doublePixels[pos++] - min);
                            val = (int) (value * scale);
                        } else if (bitDepth == 16) {
                            val = (int) ((0xFFFF & shortPixels[pos++]) * b + a - min);
                            val = (int) (val * scale);
                        } else { // 8 bits
                            val = 0xff & bytePixels[pos++];
                            val = (int) ((val - min) * scale);
                        }
                        if (val < 0f) val = 0;
                        if (val > 255) val = 255;

                        data3D[0][z + 1][y][x] = (byte) val;
                    }
                    data3D[0][z + 1][y][0] = data3D[0][z + 1][y][1] = data3D[0][z + 1][y][2];                        // duplicate first 2 pixels
                    data3D[0][z + 1][y][widthV + 3] = data3D[0][z + 1][y][widthV + 2] = data3D[0][z + 1][y][widthV + 1];    // duplicate last 2 pixels
                }
                for (int x = 0; x < widthV + 4; x++) {
                    data3D[0][z + 1][0][x] = data3D[0][z + 1][1][x] = data3D[0][z + 1][2][x];                        // duplicate first 2 rows
                    data3D[0][z + 1][heightV + 3][x] = data3D[0][z + 1][heightV + 2][x] = data3D[0][z + 1][heightV + 1][x];    // duplicate last 2 rows
                }
            }
            for (int y = 0; y < heightV + 4; y++)
                for (int x = 0; x < widthV + 4; x++) {
                    data3D[0][depthV + 3][y][x] = data3D[0][depthV + 2][y][x] = data3D[0][depthV + 1][y][x];    // duplicate last 2 layers
                    data3D[0][0][y][x] = data3D[0][1][y][x] = data3D[0][2][y][x];                        // duplicate first 2 layers
                }
        } else if (bitDepth == 24) {
            for (int z = 1; z <= depthV; z++) {
                IJ.showStatus("Reading stack, slice: " + z + "/" + depthV);
                IJ.showProgress(0.6 * z / depthV);
                int[] pixels = (int[]) stack.getPixels(z);

                int pos = 0;
                for (int y = 2; y < heightV + 2; y++) {
                    for (int x = 2; x < widthV + 2; x++) {
                        int val = pixels[pos++];
                        int r = (val >> 16) & 0xFF;
                        int g = (val >> 8) & 0xFF;
                        int b = val & 0xFF;
                        data3D[1][z + 1][y][x] = (byte) r;
                        data3D[2][z + 1][y][x] = (byte) g;
                        data3D[3][z + 1][y][x] = (byte) b;
                        data3D[0][z + 1][y][x] = (byte) ((r + 2 * g + b) >> 2);
                    }
                    data3D[0][z + 1][y][0] = data3D[0][z + 1][y][1] = data3D[0][z + 1][y][2];                        // duplicate first 2 pixels
                    data3D[1][z + 1][y][0] = data3D[1][z + 1][y][1] = data3D[1][z + 1][y][2];                        // duplicate first 2 pixels
                    data3D[2][z + 1][y][0] = data3D[2][z + 1][y][1] = data3D[2][z + 1][y][2];                        // duplicate first 2 pixels
                    data3D[3][z + 1][y][0] = data3D[3][z + 1][y][1] = data3D[3][z + 1][y][2];                        // duplicate first 2 pixels
                    data3D[0][z + 1][y][widthV + 3] = data3D[0][z + 1][y][widthV + 2] = data3D[0][z + 1][y][widthV + 1];    // duplicate last 2 pixels
                    data3D[1][z + 1][y][widthV + 3] = data3D[1][z + 1][y][widthV + 2] = data3D[1][z + 1][y][widthV + 1];    // duplicate last 2 pixels
                    data3D[2][z + 1][y][widthV + 3] = data3D[2][z + 1][y][widthV + 2] = data3D[2][z + 1][y][widthV + 1];    // duplicate last 2 pixels
                    data3D[3][z + 1][y][widthV + 3] = data3D[3][z + 1][y][widthV + 2] = data3D[3][z + 1][y][widthV + 1];    // duplicate last 2 pixels
                }
                for (int x = 0; x < widthV + 4; x++) {
                    data3D[0][z + 1][0][x] = data3D[0][z + 1][1][x] = data3D[0][z + 1][2][x];                        // duplicate first 2 rows
                    data3D[1][z + 1][0][x] = data3D[1][z + 1][1][x] = data3D[1][z + 1][2][x];                        // duplicate first 2 rows
                    data3D[2][z + 1][0][x] = data3D[2][z + 1][1][x] = data3D[2][z + 1][2][x];                        // duplicate first 2 rows
                    data3D[3][z + 1][0][x] = data3D[3][z + 1][1][x] = data3D[3][z + 1][2][x];                        // duplicate first 2 rows
                    data3D[0][z + 1][heightV + 3][x] = data3D[0][z + 1][heightV + 2][x] = data3D[0][z + 1][heightV + 1][x];    // duplicate last 2 rows
                    data3D[1][z + 1][heightV + 3][x] = data3D[1][z + 1][heightV + 2][x] = data3D[1][z + 1][heightV + 1][x];    // duplicate last 2 rows
                    data3D[2][z + 1][heightV + 3][x] = data3D[2][z + 1][heightV + 2][x] = data3D[2][z + 1][heightV + 1][x];    // duplicate last 2 rows
                    data3D[3][z + 1][heightV + 3][x] = data3D[3][z + 1][heightV + 2][x] = data3D[3][z + 1][heightV + 1][x];    // duplicate last 2 rows
                }
            }
            for (int y = 0; y < heightV + 4; y++)
                for (int x = 0; x < widthV + 4; x++) {
                    data3D[0][depthV + 3][y][x] = data3D[0][depthV + 2][y][x] = data3D[0][depthV + 1][y][x];    // duplicate last 2 layers
                    data3D[1][depthV + 3][y][x] = data3D[1][depthV + 2][y][x] = data3D[1][depthV + 1][y][x];    // duplicate last 2 layers
                    data3D[2][depthV + 3][y][x] = data3D[2][depthV + 2][y][x] = data3D[2][depthV + 1][y][x];    // duplicate last 2 layers
                    data3D[3][depthV + 3][y][x] = data3D[3][depthV + 2][y][x] = data3D[3][depthV + 1][y][x];    // duplicate last 2 layers
                    data3D[0][0][y][x] = data3D[0][1][y][x] = data3D[0][2][y][x];                        // duplicate first 2 layers
                    data3D[1][0][y][x] = data3D[1][1][y][x] = data3D[1][2][y][x];                        // duplicate first 2 layers
                    data3D[2][0][y][x] = data3D[2][1][y][x] = data3D[2][2][y][x];                        // duplicate first 2 layers
                    data3D[3][0][y][x] = data3D[3][1][y][x] = data3D[3][2][y][x];                        // duplicate first 2 layers
                }
        }

    }

}
