/**
 * 
 */
package tipl.ij.volviewer;

import ij.IJ;
import tipl.formats.TImgRO;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * Created a new subclass for handling TImg data since it was far too messy to try to have frankenstein code for both
 * @author mader
 *
 */
public class TImgVolume extends Volume {
	protected final TImgRO inTImg;
	/**
	 * @param control
	 * @param vv
	 * @param dim
	 */
	protected TImgVolume(Control control, Volume_Viewer vv, TImgRO iTImg) {
		super(control, vv, iTImg.getDim());
		inTImg=iTImg;
		control.zAspect=(float) (inTImg.getElSize().z/(0.5*(inTImg.getElSize().x+inTImg.getElSize().y)));
		// TODO Auto-generated constructor stub
	}
	@Override
	protected void getMinMax() {
		min=TImgTools.identifyTypeRange(inTImg.getImageType())[0];
		max=TImgTools.identifyTypeRange(inTImg.getImageType())[1];
		
	}
	@Override
	protected void customReadVolume() {
		final int getImageType;
		if (inTImg.getImageType()==10) getImageType=0;
		else getImageType=inTImg.getImageType();
		
			float scale = (float) (255f/(max-min));

			for (int z=1;z<=depthV;z++) {
				IJ.showStatus("Reading stack, slice: " + z + "/" + depthV);
				IJ.showProgress(0.6*z/depthV);

				byte[] bytePixels = null;
				short[] shortPixels = null;
				float[] floatPixels = null;
				int[] intPixels = null;
				
				if (getImageType==TImgTools.IMAGETYPE_CHAR) {
					char[] tempChar=(char[]) (inTImg.getPolyImage(z-1, getImageType));
					bytePixels=new byte[tempChar.length];
					for(int ii=0;ii<tempChar.length;ii++) bytePixels[ii]=(byte) tempChar[ii];
				} else if (getImageType==TImgTools.IMAGETYPE_SHORT) 
					shortPixels = (short[]) (inTImg.getPolyImage(z-1, getImageType));
				else if ( getImageType==TImgTools.IMAGETYPE_INT)
					intPixels=(int[]) (inTImg.getPolyImage(z-1, getImageType));
				else if ( getImageType==TImgTools.IMAGETYPE_FLOAT)
					floatPixels =  (float[]) (inTImg.getPolyImage(z-1, getImageType));
				

				int pos = 0;
				for (int y = 2; y < heightV+2; y++) {
					for (int x = 2; x < widthV+2; x++) {
						int val;

						if (getImageType==TImgTools.IMAGETYPE_FLOAT) {
							float value = (float) (floatPixels[pos++] - min);
							val = (int)(value*scale);
						}
						else if (getImageType==TImgTools.IMAGETYPE_INT) {
							double value = (intPixels[pos++] - min);
							val = (int) Math.round(value*scale);
						}
						else if (getImageType==TImgTools.IMAGETYPE_SHORT) {
							val = (int) ((int)(0xFFFF & shortPixels[pos++])*b + a - min);
							val = (int)(val*scale);
						}
						else { // 8 bits 
							val = 0xff & bytePixels[pos++];
							val = (int)((val-min)*scale);
						}
						if (val<0f) val = 0;
						if (val>255) val = 255;

						data3D[0][z+1][y][x] = (byte)val; 
					}
					data3D[0][z+1][y][0] = data3D[0][z+1][y][1] = data3D[0][z+1][y][2]; 						// duplicate first 2 pixels
					data3D[0][z+1][y][widthV+3] = data3D[0][z+1][y][widthV+2] = data3D[0][z+1][y][widthV+1]; 	// duplicate last 2 pixels
				}
				for (int x = 0; x < widthV+4; x++) {
					data3D[0][z+1][0][x] = data3D[0][z+1][1][x] = data3D[0][z+1][2][x]; 						// duplicate first 2 rows
					data3D[0][z+1][heightV+3][x] = data3D[0][z+1][heightV+2][x] = data3D[0][z+1][heightV+1][x];	// duplicate last 2 rows
				}
			}
			for (int y = 0; y < heightV+4; y++) 
				for (int x = 0; x < widthV+4; x++) {
					data3D[0][depthV+3][y][x] = data3D[0][depthV+2][y][x] = data3D[0][depthV+1][y][x];	// duplicate last 2 layers
					data3D[0][0][y][x] = data3D[0][1][y][x] = data3D[0][2][y][x];						// duplicate first 2 layers
				}
		// no 24 bit TImg files yet
		
	}
}
