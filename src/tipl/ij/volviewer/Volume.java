package tipl.ij.volviewer;

/*
 * Volume Viewer 2.01
 * 01.12.2012
 * 
 * (C) Kai Uwe Barthel
 */

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import tipl.formats.TImgRO;
import tipl.util.D3int;


public abstract class Volume {

	int widthV;		// size of the volume
	int heightV;
	int depthV;

	float xOffa;   	// center of the volume				
	float yOffa;  	 	   
	float zOffa; 

	final byte[][][][] data3D;

	final byte[][][] grad3D;

	final byte[][][] mean3D;
	final byte[][][] diff3D;

	final byte[][][] col_3D;

	final byte [][][] aPaint_3D;  	
	final byte[][][] aPaint_3D2;  	// -254 .. 254 

	final byte[][][] nx_3D;
	final byte[][][] ny_3D;
	final byte[][][] nz_3D;

	protected double a = 0, b = 1;
	protected double min, max;

	boolean firstTime = true;

	int[][] histValGrad = new int[256][128]; // lum, grad
	int[][] histMeanDiff = new int[256][128]; 
	int[] histVal = new int[256];			// lum

	private ImageProcessor ip;
	protected Control control;

	private Volume_Viewer vv; 

	public double[] getRange() { return new double[] {min,max};}

	public static Volume create(Control control, Volume_Viewer vv,ImagePlus imp) {
		Volume outV=new StackVolume(control, vv,imp);
		outV.getMinMax();
		outV.readVolumeData();
		return outV;
	}
	public static Volume create(Control control, Volume_Viewer vv,ImagePlus imp,double imin,double imax) {
		Volume outV=new StackVolume(control, vv,imp);
		outV.rescaleImage(imin, imax);
		return outV;
	}
	public static Volume create(Control control, Volume_Viewer vv,TImgRO inImg) {
		Volume outV=new TImgVolume(control, vv,inImg);
		outV.getMinMax();
		outV.readVolumeData();
		return outV;
	}
	public static Volume create(Control control, Volume_Viewer vv,TImgRO inImg,double imin,double imax) {
		Volume outV=new TImgVolume(control, vv,inImg);
		outV.rescaleImage(imin, imax);
		return outV;
	}


	protected Volume(Control control, Volume_Viewer vv,D3int dim) {
		this.control = control;
		this.vv = vv;

		widthV = dim.x;
		heightV = dim.y;
		depthV = dim.z;

		// the volume data
		if (control.isRGB )
			data3D = new byte[4][depthV+4][heightV+4][widthV+4]; //z y x
		else
			data3D = new byte[1][depthV+4][heightV+4][widthV+4]; //z y x

		// these arrays could be shared 
		grad3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x

		// or
		mean3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x
		diff3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x

		// or
		aPaint_3D2 = new byte[depthV+4][heightV+4][widthV+4]; //z y x
		aPaint_3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x
		col_3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x


		// for the illumination		
		nx_3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x
		ny_3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x
		nz_3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x

		xOffa  = widthV/2.f;   				
		yOffa  = heightV/2.f;  	 	   
		zOffa  = depthV/2.f;
	}


	protected abstract void getMinMax();

	public void resetImage() {
		getMinMax();
		readVolumeData();
	}
	public void rescaleImage(double imin,double imax) {
		min=imin;
		max=imax;
		readVolumeData();
	}

	protected void readVolumeData() {
		if (control.LOG) System.out.println("Read data:"+this);
		customReadVolume();
		finishReadingVolume();
	}	
	protected abstract void customReadVolume();

	protected void finishReadingVolume() {
		// calculate variance etc. and fill histograms
		int[] va = new int[7];
		int[] vb = new int[7];

		for(int z=2; z < depthV+2; z++) {
			IJ.showStatus("Analyzing stack, slice: " + (z-1) + "/" + depthV);
			IJ.showProgress(0.6+0.4*z/depthV);
			for (int y = 2; y < heightV+2; y++) {
				for (int x = 2; x < widthV+2; x++) {
					int val = 0xff & data3D[0][z][y][x];
					va[0] = 0xff & data3D[0][z-1][y  ][x  ];
					vb[0] = 0xff & data3D[0][z+1][y  ][x  ];
					va[1] = 0xff & data3D[0][z  ][y-1][x  ];
					vb[1] = 0xff & data3D[0][z  ][y+1][x  ];
					va[2] = 0xff & data3D[0][z  ][y  ][x-1];
					vb[2] = 0xff & data3D[0][z  ][y  ][x+1];
					va[3] = 0xff & data3D[0][z-1][y-1][x-1];
					vb[3] = 0xff & data3D[0][z+1][y+1][x+1];
					va[4] = 0xff & data3D[0][z-1][y+1][x-1];
					vb[4] = 0xff & data3D[0][z+1][y-1][x+1];
					va[5] = 0xff & data3D[0][z-1][y-1][x+1];
					vb[5] = 0xff & data3D[0][z+1][y+1][x-1];
					va[6] = 0xff & data3D[0][z-1][y+1][x+1];
					vb[6] = 0xff & data3D[0][z+1][y-1][x-1];

					int grad = 0, d, dMax = 0, iMax = 0;
					for (int i = 0; i < vb.length; i++) {
						grad += d = Math.abs(va[i] - vb[i]);
						if (d > dMax) {
							dMax = d;
							iMax = i;
						}
					}

					int low, high;
					if(va[iMax] < vb[iMax]) {
						low = va[iMax]; high = vb[iMax];
					}
					else {
						low = vb[iMax]; high = va[iMax];
					}							
					grad /= 7;
					if (grad > 127)
						grad = 127;

					grad3D[z][y][x] = (byte)grad;
					histValGrad[val][(int)grad]++;
					histVal[val]++;					// luminance
					int mean = (int) (Math.max(0, Math.min(255,(low+high)*0.5)));
					int diff = (int) (Math.max(0, Math.min(127,(high-low)*0.5)));
					mean3D[z][y][x] = (byte) mean;
					diff3D[z][y][x] = (byte) diff;
					histMeanDiff[mean][diff]++;
				}
			}
		}

		IJ.showProgress(1.0);
		IJ.showStatus("");

	}


	void calculateGradients() {
		control.alphaWasChanged = false;

		byte[][][] alpha_3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x 
		byte[][][] alpha_3D_smooth = new byte[depthV+4][heightV+4][widthV+4]; //z y x

		long start = 0;
		if (control.LOG) { 
			IJ.log("Calculate Gradients ");
			start = System.currentTimeMillis();
		}

		if (control.alphaMode == Control.ALPHA1) {
			for(int z = 2; z < depthV+2; z++) {
				for (int y = 2; y < heightV+2; y++) {
					for (int x = 2; x < widthV+2; x++) {
						int val = data3D[0][z][y][x] & 0xFF;
						alpha_3D[z][y][x] = (byte) (vv.a1_R[val]*255);
					}
				}
			}			
		}
		else if (control.alphaMode == Control.ALPHA2) {
			for(int z = 2; z < depthV+2; z++) {
				for (int y = 2; y < heightV+2; y++) {
					for (int x = 2; x < widthV+2; x++) {
						int val = data3D[0][z][y][x] & 0xFF;
						int grad = grad3D[z][y][x] & 0xFF;
						alpha_3D[z][y][x] = (byte) (vv.a2_R[val][grad]*255);
					}
				}
			}
		}
		else if (control.alphaMode == Control.ALPHA3) {
			for(int z = 2; z < depthV+2; z++) {
				for (int y = 2; y < heightV+2; y++) {
					for (int x = 2; x < widthV+2; x++) {
						int val = mean3D[z][y][x] & 0xFF;
						int diff = diff3D[z][y][x] & 0xFF;
						alpha_3D[z][y][x] = (byte) (vv.a3_R[val][diff]*255);
					}
				}
			}
		}
		else if (control.alphaMode == Control.ALPHA4) {
			for(int z = 2; z < depthV+2; z++) {
				for (int y = 2; y < heightV+2; y++) {
					for (int x = 2; x < widthV+2; x++) {
						alpha_3D[z][y][x] = aPaint_3D[z][y][x];
					}
				}
			}
		}

		// filter alpha
		for(int z=1; z < depthV+3; z++) {
			for (int y = 1; y < heightV+3; y++) {
				int a000, a010, a020;
				int a001 = 0xff & alpha_3D[z-1][y-1][0];
				int a011 = 0xff & alpha_3D[z-1][y  ][0];
				int a021 = 0xff & alpha_3D[z-1][y+1][0];
				int a002 = 0xff & alpha_3D[z-1][y-1][1];
				int a012 = 0xff & alpha_3D[z-1][y  ][1];
				int a022 = 0xff & alpha_3D[z-1][y+1][1];
				int a100, a110, a120;
				int a101 = 0xff & alpha_3D[z][y-1][0];
				int a111 = 0xff & alpha_3D[z][y  ][0];
				int a121 = 0xff & alpha_3D[z][y+1][0];
				int a102 = 0xff & alpha_3D[z][y-1][1];
				int a112 = 0xff & alpha_3D[z][y  ][1];
				int a122 = 0xff & alpha_3D[z][y+1][1];
				int a200, a210, a220;
				int a201 = 0xff & alpha_3D[z+1][y-1][0];
				int a211 = 0xff & alpha_3D[z+1][y  ][0];
				int a221 = 0xff & alpha_3D[z+1][y+1][0];
				int a202 = 0xff & alpha_3D[z+1][y-1][1];
				int a212 = 0xff & alpha_3D[z+1][y  ][1];
				int a222 = 0xff & alpha_3D[z+1][y+1][1];

				for (int x = 1; x < widthV+3; x++) {
					a000 = a001; a010 = a011; a020 = a021;
					a001 = a002; a011 = a012; a021 = a022;
					a002 = 0xff & alpha_3D[z-1][y-1][x+1];
					a012 = 0xff & alpha_3D[z-1][y  ][x+1];
					a022 = 0xff & alpha_3D[z-1][y+1][x+1];

					a100 = a101; a110 = a111; a120 = a121;
					a101 = a102; a111 = a112; a121 = a122;
					a102 = 0xff & alpha_3D[z][y-1][x+1];
					a112 = 0xff & alpha_3D[z][y  ][x+1];
					a122 = 0xff & alpha_3D[z][y+1][x+1];

					a200 = a201; a210 = a211; a220 = a221;
					a201 = a202; a211 = a212; a221 = a222;
					a202 = 0xff & alpha_3D[z+1][y-1][x+1];
					a212 = 0xff & alpha_3D[z+1][y  ][x+1];
					a222 = 0xff & alpha_3D[z+1][y+1][x+1];

					int a = (a000 + a001 + a002 + a010 + a011 + a012 + a020 + a021 + a022 +
							a100 + a101 + a102 + a110 + a111 + a112 + a120 + a121 + a122 +
							a200 + a201 + a202 + a210 + a211 + a212 + a220 + a221 + a222) >> 5;

				alpha_3D_smooth[z][y][x] = (byte)a;
				}
			}
		}

		//alpha_3D_smooth = alpha_3D;

		// gradient
		for(int z=1; z < depthV+3; z++) {
			for (int y = 1; y < heightV+3; y++) {			
				int a000, a010, a020;
				int a001 = 0xff & alpha_3D_smooth[z-1][y-1][0];
				int a011 = 0xff & alpha_3D_smooth[z-1][y  ][0];
				int a021 = 0xff & alpha_3D_smooth[z-1][y+1][0];
				int a002 = 0xff & alpha_3D_smooth[z-1][y-1][1];
				int a012 = 0xff & alpha_3D_smooth[z-1][y  ][1];
				int a022 = 0xff & alpha_3D_smooth[z-1][y+1][1];
				int a100, a110, a120;
				int a101 = 0xff & alpha_3D_smooth[z][y-1][0];
				int a111 = 0xff & alpha_3D_smooth[z][y  ][0];
				int a121 = 0xff & alpha_3D_smooth[z][y+1][0];
				int a102 = 0xff & alpha_3D_smooth[z][y-1][1];
				int a112 = 0xff & alpha_3D_smooth[z][y  ][1];
				int a122 = 0xff & alpha_3D_smooth[z][y+1][1];
				int a200, a210, a220;
				int a201 = 0xff & alpha_3D_smooth[z+1][y-1][0];
				int a211 = 0xff & alpha_3D_smooth[z+1][y  ][0];
				int a221 = 0xff & alpha_3D_smooth[z+1][y+1][0];
				int a202 = 0xff & alpha_3D_smooth[z+1][y-1][1];
				int a212 = 0xff & alpha_3D_smooth[z+1][y  ][1];
				int a222 = 0xff & alpha_3D_smooth[z+1][y+1][1];

				for (int x = 1; x < widthV+3; x++) {
					a000 = a001; a010 = a011; a020 = a021;
					a001 = a002; a011 = a012; a021 = a022;
					a002 = 0xff & alpha_3D_smooth[z-1][y-1][x+1];
					a012 = 0xff & alpha_3D_smooth[z-1][y  ][x+1];
					a022 = 0xff & alpha_3D_smooth[z-1][y+1][x+1];

					a100 = a101; a110 = a111; a120 = a121;
					a101 = a102; a111 = a112; a121 = a122;
					a102 = 0xff & alpha_3D_smooth[z][y-1][x+1];
					a112 = 0xff & alpha_3D_smooth[z][y  ][x+1];
					a122 = 0xff & alpha_3D_smooth[z][y+1][x+1];

					a200 = a201; a210 = a211; a220 = a221;
					a201 = a202; a211 = a212; a221 = a222;
					a202 = 0xff & alpha_3D_smooth[z+1][y-1][x+1];
					a212 = 0xff & alpha_3D_smooth[z+1][y  ][x+1];
					a222 = 0xff & alpha_3D_smooth[z+1][y+1][x+1];

					int dx = ((a002 + a012 + a022 + a102 + a112 + a122 + a202 + a212 + a222) >> 2) - 
							((a000 + a010 + a020 + a100 + a110 + a120 + a200 + a210 + a220) >> 2);

					int dy = ((a020 + a021 + a022 + a120 + a121 + a122 + a220 + a221 + a222) >> 2) -
							((a000 + a001 + a002 + a100 + a101 + a102 + a200 + a201 + a202) >> 2);

					int dz = ((a200 + a201 + a202 + a210 + a211 + a212 + a220 + a221 + a222) >> 2) -
							((a000 + a001 + a002 + a010 + a011 + a012 + a020 + a021 + a022) >> 2);

					//					int dx = (a102 + 2*a112 + a122 - a100 - 2*a110 - a120) / 4;
					//					int dy = (a021 + 2*a121 + a221 - a001 - 2*a101 - a201) / 4;
					//					int dz = (a210 + 2*a211 + a212 - a010 - 2*a011 - a012) / 4;

					nx_3D[z][y][x] = (byte)(Math.max(-127, Math.min(127,dx))+128);
					ny_3D[z][y][x] = (byte)(Math.max(-127, Math.min(127,dy))+128);
					nz_3D[z][y][x] = (byte)(Math.max(-127, Math.min(127,dz))+128);
				}
			}
		}

		alpha_3D = null;
		alpha_3D_smooth = null;

		if (control.LOG) {
			long end = System.currentTimeMillis();
			System.out.println("  Execution time "+(end-start)+" ms.");
		}
	}

	private static final byte YES = 1;

	void findAndSetSimilarInVolume(int lum, int alpha, int z0, int y0, int x0) {

		int width  = vv.vol.widthV+4;
		int height = vv.vol.heightV+4;
		int depth  = vv.vol.depthV+4;
		byte[][][] data = vv.vol.data3D[0];

		control.alphaWasChanged = true;

		int regionSize = 1;
		int pointsInQueue = 0;
		int queueArrayLength = 40000;
		int[] queue = new int[queueArrayLength];

		byte[] hasBeenProcessed = new byte[depth * width * height];
		setAlphaAndColorInVolume(alpha, z0, y0, x0); 
		int i = width * (z0 * height + y0) + x0;
		hasBeenProcessed[i] = YES;
		queue[pointsInQueue++] = i;

		while (pointsInQueue > 0) {

			int nextIndex = queue[--pointsInQueue];
			int pz = nextIndex / (width * height);
			int currentSliceIndex = nextIndex % (width * height);
			int py = currentSliceIndex / width;
			int px = currentSliceIndex % width;

			//int actualValue = data[pz][py][px] & 0xff;

			for (int k = 0; k < 6; k++) {

				int x = px;
				int y = py;
				int z = pz;

				if      (k == 0)  x = Math.max(0, x-1); 
				else if (k == 1)  x = Math.min(width-1, x+1); 
				else if (k == 2)  y = Math.max(0, y-1); 
				else if (k == 3)  y = Math.min(height-1, y+1);
				else if (k == 4)  z = Math.max(0, z-1); 
				else if (k == 5)  z = Math.min(depth-1, z+1);

				col_3D[z][y][x] = (byte)control.indexPaint; // set the color of neigbors

				int newPointStateIndex = width * (z * height + y) + x;
				if (hasBeenProcessed[newPointStateIndex] == YES) 
					continue;

				int neighbourValue = data[z][y][x] & 0xff; 
				int diff = Math.abs(neighbourValue-lum);
				//int diff = Math.abs(neighbourValue-actualValue);

				if (diff > control.lumTolerance)
					continue;

				int grad = vv.vol.grad3D[z][y][x] & 0xff; 
				if (grad > control.gradTolerance)
					continue;

				regionSize++;
				setAlphaAndColorInVolume(alpha, z, y, x); 

				hasBeenProcessed[newPointStateIndex] = YES;
				if (pointsInQueue == queueArrayLength) {
					int newArrayLength = (int) (queueArrayLength * 2);
					int[] newArray = new int[newArrayLength];
					System.arraycopy(queue, 0, newArray, 0, pointsInQueue);
					queue = newArray;
					queueArrayLength = newArrayLength;
				}
				queue[pointsInQueue++] = newPointStateIndex;
			}
		}

		if (regionSize < 100)
			IJ.error("Found only " + regionSize + " connected voxel(s).\n Try changing the tolerance values.");
	}


	private void setAlphaAndColorInVolume(int alpha, int z, int y, int x) {
		vv.vol.aPaint_3D[z][y][x] = (byte)alpha;
		if (alpha > 0) {
			int a = alpha - vv.tf_a4.getAlphaOffset();
			a = Math.min(254, Math.max(-254, a));
			aPaint_3D2[z][y][x] = (byte) (a/2);
		}
		else
			aPaint_3D2[z][y][x] = 0;

		col_3D[z][y][x] = (byte)control.indexPaint;
	}

}