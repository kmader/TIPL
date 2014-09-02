package tipl.ij.volviewer;
/*
 * Volume Viewer 2.0
 * 28.11.2012
 * 
 * (C) Kai Uwe Barthel
 */

public class Interpolation {

	double[][] pw = new double[256][4]; 
	double[][] sw = new double[256][4];
	private Control control; 

	public Interpolation(Control control) {
		this.control = control;
		initializeCubicPolynomialWeights();
		initializeCubicSplineWeights();
	}

//	double [] getCubicSplineWeights(double dx) {
//		double[] wx = new double[4];
//
//		double dx2 = dx*dx;
//		double dx_ = 1-dx;
//		double dx_2 = dx_*dx_;
//
//		// cubic spline
//		wx[0] = dx_2*dx_/6;
//		wx[1] = 2/3f - 0.5f*dx2*(2-dx);
//		wx[2] = 2/3f - 0.5f*dx_2*(1+dx);
//		wx[3] = dx2*dx/6;
//
//		return wx;
//	}

//	double [] getCubicPolynomialWeights(double dx) {
//		double[] wx = new double[4];
//		
//		double dx2 = dx*dx;
//		double dx3 = dx*dx2;
//		
//		wx[0] = (  -dx3 + 2*dx2 - dx)/2;
//		wx[1] = ( 3*dx3 - 5*dx2 + 2 )/2;
//		wx[2] = (-3*dx3 + 4*dx2 + dx)/2;
//		wx[3] = (   dx3 -   dx2)/2;
//		
//		return wx;
//	}

	void initializeCubicPolynomialWeights() {
		for (int i = 0; i < pw.length; i++) {
			double dx = i/256f;

			double dx2 = dx*dx;
			double dx3 = dx*dx2;

			pw[i][0] = (  -dx3 + 2*dx2 - dx)/2;
			pw[i][1] = ( 3*dx3 - 5*dx2 + 2 )/2;
			pw[i][2] = (-3*dx3 + 4*dx2 + dx)/2;
			pw[i][3] = (   dx3 -   dx2)/2;

			//			double a = -3f;
			//			pw[i][0] =  a*dx3 - 2*a*dx2 + a*dx;
			//			pw[i][1] = (a+2)*dx3 -(a+3)*dx2 + 1;
			//			pw[i][2] = -(a+2)*dx3 + (2*a+3)*dx2 - a*dx;
			//			pw[i][3] = -a*dx3 + a*dx2;
		}
	}

	void initializeCubicSplineWeights() {
		for (int i = 0; i < pw.length; i++) {
			double dx = i/256f;

			double dx2 = dx*dx;
			double dx_ = 1-dx;
			double dx_2 = dx_*dx_;

			sw[i][0] = dx_2*dx_/6;
			sw[i][1] = 2/3f - 0.5f*dx2*(2-dx);
			sw[i][2] = 2/3f - 0.5f*dx_2*(1+dx);
			sw[i][3] = dx2*dx/6;

			// equal to
			//			sw[i][0] = (  -dx3 + 3*dx2 -3*dx + 1)/6;
			//			sw[i][1] = ( 3*dx3 - 6*dx2 +4)/6;
			//			sw[i][2] = (-3*dx3 + 3*dx2 + 3*dx +1)/6;
			//			sw[i][3] = dx3/6;
		}
	}


	int get(byte[][][] data3D, double z, double y, double x) {

		x += 0.5; 
		y += 0.5;
		z += 0.5;

		int z0 = (int)z;
		double dz = z - z0;
		int y0 = (int)y;
		double dy = y - y0;
		int x0 = (int)x;
		double dx = x - x0;			

		if (control.interpolationMode == Control.TRICUBIC_POLYNOMIAL) {
			double[] wx = pw[(int) (dx*256)]; // getCubicPolynomialWeights(dx);
			double[] wy = pw[(int) (dy*256)]; // getCubicPolynomialWeights(dy);
			double[] wz = pw[(int) (dz*256)]; // getCubicPolynomialWeights(dz);

			double vz = 0;
			for (int zi = 0; zi < 4; zi++) {
				byte[][] vDataZ = data3D[z0+zi];
				double vy = 0; 
				for (int yi = 0; yi < 4; yi++) {
					byte[] vDataZY = vDataZ[y0+yi];
					double vx = wx[0]*(0xFF & vDataZY[x0]) +
							wx[1]*(0xFF & vDataZY[x0+1]) +
							wx[2]*(0xFF & vDataZY[x0+2]) +
							wx[3]*(0xFF & vDataZY[x0+3]);

					vy += wy[yi]*vx;
				}
				vz += wz[zi]*vy;
			}
			return (int)Math.min(255, Math.max(0, vz));
		}
		else if (control.interpolationMode == Control.TRICUBIC_SPLINE) {
			double[] wx = sw[(int) (dx*256)]; // getCubicSplineWeights(dx);
			double[] wy = sw[(int) (dy*256)]; // getCubicSplineWeights(dy);
			double[] wz = sw[(int) (dz*256)]; // getCubicSplineWeights(dz);

			double vz = 0;
			for (int zi = 0; zi < 4; zi++) {
				double vy = 0;
				byte[][] vDataZ = data3D[z0+zi];
				for (int yi = 0; yi < 4; yi++) {
					byte[] vDataZY = vDataZ[y0+yi];
					double vx = wx[0]*(0xFF & vDataZY[x0]) +
							wx[1]*(0xFF & vDataZY[x0+1]) +
							wx[2]*(0xFF & vDataZY[x0+2]) +
							wx[3]*(0xFF & vDataZY[x0+3]);

					vy += wy[yi]*vx;
				}
				vz += wz[zi]*vy;
			}
			//return (int)Math.min(255, Math.max(0, vz));
			return (int)vz;
		}	
		else if (control.interpolationMode == Control.TRILINEAR) {
			x0++;
			y0++;
			z0++;
			int x1 = x0+1;
			int y1 = y0+1;
			double dx_ = 1-dx;
			byte[][] data3D_z0 = data3D[z0]; 
			byte[][] data3D_z1 = data3D[z0+1]; 

			double ab = (0xff & data3D_z0[y0][x0])*dx_ + dx*(0xff & data3D_z0[y0][x1]);
			double ef = (0xff & data3D_z1[y0][x0])*dx_ + dx*(0xff & data3D_z1[y0][x1]);
			double cd = (0xff & data3D_z0[y1][x0])*dx_ + dx*(0xff & data3D_z0[y1][x1]);
			double gh = (0xff & data3D_z1[y1][x0])*dx_ + dx*(0xff & data3D_z1[y1][x1]);

			double dy_ = 1-dy;
			ab = ab*dy_ + dy*cd;
			ef = ef*dy_ + dy*gh;

			return (int) (ab + dz*(ef-ab));
		}
		else {// NN
			x += 1.5; 
			y += 1.5;
			z += 1.5;
			return 0xff & data3D[(int)z][(int)y][(int)x];	
		}
	}
}
