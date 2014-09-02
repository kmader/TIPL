package tipl.ij.volviewer;
/*
 * Volume Viewer 2.0
 * 27.11.2012
 * 
 * (C) Kai Uwe Barthel
 */

class Transform {
	private Control control;

	private double mP[][];

	private double xRot, yRot, zRot;		// rotation vector
	private double rotAngle=0;			// rotation angle

	private double xSOff; 	  	// screen center
	private double ySOff; 
	private double xOff; 	  	// screen center
	private double yOff; 

	private double xvOff;		// volume center		
	private double yvOff;
	private double zvOff;

	private double scale = 1;
	private double zAspect = 1;

	private double a00,  a01,  a02,  a03;  // coefficients of the tramsformation
	private double a10,  a11,  a12,  a13;
	private double a20,  a21,  a22,  a23;
	private double ai00, ai01, ai02, ai03;  // coefficients of the inverse tramsformation
	private double ai10, ai11, ai12, ai13;
	private double ai20, ai21, ai22, ai23;

	private double degreeX;
	private double degreeY;
	private double degreeZ;

	void initializeTransformation() {

		if (control.LOG) System.out.println("initializeTransformation");

		double m[][] = new double[4][4];

		// translation & scale
		double s = scale;
		m[0][0] = s; 	m[0][1] = 0; 	m[0][2] = 0;				m[0][3] = -s*xvOff;
		m[1][0] = 0;	m[1][1] = s;	m[1][2] = 0;				m[1][3] = -s*yvOff; 
		m[2][0] = 0;	m[2][1] = 0;	m[2][2] = s*zAspect; 		m[2][3] = -s*zAspect*zvOff;
		m[3][0] = 0;	m[3][1] = 0;	m[3][2] = 0; 				m[3][3] = 1;

		double mR[][] = new double[4][4];

		// rotation from mouse drag
		double u = xRot;
		double v = yRot;
		double w = zRot;

		double u2=u*u;
		double v2=v*v;
		double w2=w*w;

		double cos = (double)Math.cos(rotAngle);
		double sin = (double)Math.sin(rotAngle);
		rotAngle = 0;

		mR[0][0] = u2 + (1-u2)*cos;	
		mR[0][1] = u*v*(1-cos) - w*sin;	
		mR[0][2] = u*w*(1-cos) + v*sin;			
		mR[1][0] = u*v*(1-cos) + w*sin;	
		mR[1][1] = v2 + (1-v2)*cos;	
		mR[1][2] = v*w*(1-cos) - u*sin;			
		mR[2][0] = u*w*(1-cos) - v*sin;	    
		mR[2][1] = v*w*(1-cos) + u*sin;		
		mR[2][2] = w2 + (1-w2)*cos;			
		mR[3][3] = 1;

		double m_RP[][] = new double[4][4];
		matProd(m_RP, mR, mP);
		matCopy4(mP, m_RP);	// remember the previous rotation

		double ar=0, br=0, gr=0;
		if (mP[2][0] != 1 && mP[2][0] != -1) {
			br = -Math.asin(mP[2][0]);
			double cosbr = Math.cos(br);
			ar = Math.atan2(mP[2][1]/cosbr, mP[2][2]/cosbr);
			gr = Math.atan2(mP[1][0]/cosbr, mP[0][0]/cosbr);
		}
		else {
			gr = 0;
			if (mP[2][0] == -1) {
				br = Math.PI/2;
				ar = Math.atan2(mP[0][1], mP[0][2]);
			}
			else {
				br = -Math.PI/2;
				ar = Math.atan2(-mP[0][1], -mP[0][2]);
			}
		}

		degreeX = (double) Math.toDegrees(ar);
		degreeY = (double) Math.toDegrees(br);
		degreeZ = (double) Math.toDegrees(gr);

		double mat[][] = new double[4][4];
		matProd(mat, mP, m); 

		a00 =  mat[0][0];
		a01 =  mat[0][1];
		a02 =  mat[0][2];
		a03 =  mat[0][3] + xSOff + xOff*scale;

		a10 =  mat[1][0];
		a11 =  mat[1][1];
		a12 =  mat[1][2];
		a13 =  mat[1][3] + ySOff  + yOff*scale; 

		a20 =  mat[2][0];
		a21 =  mat[2][1];
		a22 =  mat[2][2];
		a23 =  mat[2][3];

		double matInv[][] = new double[4][4];
		matInv4(matInv, mat);

		ai00 = matInv[0][0];
		ai01 = matInv[0][1];
		ai02 = matInv[0][2];
		ai03 = matInv[0][3];

		ai10 = matInv[1][0];
		ai11 = matInv[1][1];
		ai12 = matInv[1][2];
		ai13 = matInv[1][3]; 

		ai20 = matInv[2][0];
		ai21 = matInv[2][1];
		ai22 = matInv[2][2];
		ai23 = matInv[2][3];
	}

	public double getDegreeX() {
		return degreeX;
	}

	public double getDegreeY() {
		return degreeY;
	}

	public double getDegreeZ() {
		return degreeZ;
	}

	void matProd(double z[][], double u[][], double v[][]) {
		int i, j, k;
		for (i=0; i<4; i++) 
			for (j=0; j<4; j++) {
				z[i][j]=0.0f;
				for (k=0; k<4; k++) 
					z[i][j]+=u[i][k]*v[k][j];
			}
	}

	public void setView(double angleX, double angleY, double angleZ) {
		// calculate new cos and sins 
		double cosX = (double)Math.cos(angleX); 
		double sinX = (double)Math.sin(angleX);
		double cosY = (double)Math.cos(angleY); 
		double sinY = (double)Math.sin(angleY);
		double cosZ = (double)Math.cos(angleZ);
		double sinZ = (double)Math.sin(angleZ);			

		// x rotation
		double mX[][] = new double[4][4];
		mX[0][0] =  1;   	mX[0][1] =  0;		mX[0][2] =  0;			mX[0][3] =  0;
		mX[1][0] =  0;		mX[1][1] =  cosX;	mX[1][2] = -sinX;		mX[1][3] =  0; 
		mX[2][0] =  0;		mX[2][1] =  sinX;	mX[2][2] =  cosX;		mX[2][3] =  0;
		mX[3][0] =  0;		mX[3][1] =  0;		mX[3][2] =  0;			mX[3][3] =  1;

		// y rotation
		double mY[][] = new double[4][4];
		mY[0][0] =  cosY;   mY[0][1] =  0; 		mY[0][2] =  sinY;		mY[0][3] =  0;
		mY[1][0] =  0;		mY[1][1] =  1;		mY[1][2] =  0;			mY[1][3] =  0; 
		mY[2][0] = -sinY;	mY[2][1] =  0;		mY[2][2] =  cosY;		mY[2][3] =  0;
		mY[3][0] =  0;		mY[3][1] =  0;		mY[3][2] =  0;			mY[3][3] =  1;

		// z rotation
		double mZ[][] = new double[4][4];
		mZ[0][0] =  cosZ;	mZ[0][1] = -sinZ;	mZ[0][2] =  0;			mZ[0][3] =  0;
		mZ[1][0] =  sinZ;	mZ[1][1] =  cosZ;	mZ[1][2] =  0;			mZ[1][3] =  0; 
		mZ[2][0] =  0;	    mZ[2][1] =  0;		mZ[2][2] =  1;			mZ[2][3] =  0;
		mZ[3][0] =  0;		mZ[3][1] =  0;		mZ[3][2] =  0;			mZ[3][3] =  1;

		double m_XY[][] = new double[4][4];
		matProd(m_XY,  mY, mX);
		matProd(mP, mZ, m_XY);

		initializeTransformation();	
	}

	/* 4x4 matrix inverse */
	void matInv4(double z[][], double u[][]) {
		int    i, j, n, ii[] = new int[4];
		double f;
		double w[][] = new double[4][4];
		n=4;
		matCopy4(w,u);
		matUnit4(z);
		for (i=0; i<n; i++) {
			ii[i]=matge4(w,i);
			matXr4(w,i,ii[i]);
			for (j=0; j<n; j++) {
				if (i==j) continue;
				f=-w[i][j]/w[i][i];
				matAc4(w,j,j,f,i);
				matAc4(z,j,j,f,i);
			}
		}
		for (i=0; i<n; i++) 
			matMc4(z,1.0f/w[i][i],i);
		for (i=0; i<n; i++) {
			j=n-i-1; 
			matXc4(z,j,ii[j]);
		}
	}

	/* greatest element in the nth column of 4x4 matrix */
	int matge4(double p[][], int n) {
		double g, h; 
		int m;
		m=n;
		g=p[n][n];
		g=(g<0.0?-g:g);
		for (int i=n; i<4; i++) {
			h=p[i][n];
			h=(h<0.0?-h:h);
			if (h<g) continue;
			g=h; m=i;
		}
		return m;
	}

	/* copy 4x4 matrix */
	void matCopy4(double z[][], double x[][]) {
		int i, j;
		for (i=0; i<4; i++) 
			for (j=0; j<4; j++) 
				z[i][j]=x[i][j];
	}

	/* 4x4 unit matrix */
	void matUnit4(double z[][]) {
		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) 
				z[i][j]=0.0f;
			z[i][i]=1.0f;
		}
	}

	/* exchange ith and jth columns of a 4x4 matrix */
	void matXc4(double z[][], int i, int j) {
		double t;
		if (i==j) 
			return;
		for (int k=0; k<4; k++) {
			t=z[k][i]; 
			z[k][i]=z[k][j]; 
			z[k][j]=t;}
	}

	/* exchange ith and jth rows of a 4x4 matrix */
	void matXr4(double z[][], int i, int j) {
		double t;
		if (i==j) 
			return;
		for (int k=0; k<4; k++) {
			t=z[i][k]; 
			z[i][k]=z[j][k]; 
			z[j][k]=t;
		}
	}

	/* augment column of a 4x4 matrix */
	void matAc4(double z[][], int i, int j, double f, int k) {
		int l;
		for (l=0; l<4; l++) 
			z[l][i] = z[l][j] + f*z[l][k];
	}

	/* multiply ith column of 4x4 matrix by a factor */
	void matMc4(double z[][], double f, int i) {
		int j;
		for (j=0; j<4; j++) 
			z[j][i]*=f;
	}

	final double[] trVol2Screen(double[] xyzV) {
		double xV = xyzV[0];
		double yV = xyzV[1];
		double zV = xyzV[2];
		double[] xyzS = new double[3];
		xyzS[0] = a00*xV + a01*yV + a02*zV + a03;
		xyzS[1] = a10*xV + a11*yV + a12*zV + a13;
		xyzS[2] = a20*xV + a21*yV + a22*zV + a23;
		return xyzS;
	}

	final double[]  trVol2Screen(double xV, double yV, double zV) {
		double[] xyzS = new double[3];
		xyzS[0] = a00*xV + a01*yV + a02*zV + a03;
		xyzS[1] = a10*xV + a11*yV + a12*zV + a13;
		xyzS[2] = a20*xV + a21*yV + a22*zV + a23;
		return xyzS;
	}

	final double[] trScreen2Vol(double xS, double yS, double zS) {
		xS -= xSOff + xOff*scale;
		yS -= ySOff + yOff*scale;
		double[] xyzV = new double[3];
		xyzV[0] = ai00*xS + ai01*yS + ai02*zS + ai03;
		xyzV[1] = ai10*xS + ai11*yS + ai12*zS + ai13;
		xyzV[2] = ai20*xS + ai21*yS + ai22*zS + ai23;
		return xyzV;
	}

	final double[] screen2Volume(double[] xyzS) {
		double xS = xyzS[0] - xSOff - xOff*scale;
		double yS = xyzS[1] - ySOff - yOff*scale;
		double zS = xyzS[2];
		double[] xyzV = new double[3];
		xyzV[0] = ai00*xS + ai01*yS + ai02*zS + ai03;
		xyzV[1] = ai10*xS + ai11*yS + ai12*zS + ai13;
		xyzV[2] = ai20*xS + ai21*yS + ai22*zS + ai23;
		return xyzV;
	}

	public void setScale(double scale) {
		this.scale = scale;		
		initializeTransformation();
	}

	// virtual trackball
	public void setMouseMovement(int xAct, int yAct, int xStart, int yStart, double width) {
		//if (control.LOG) System.out.println("setMouseMovement");

		double size = 2*width;

		double x1 = (xStart - width/2f)/size;
		double y1 = (yStart - width/2f)/size;
		double z1 = 1-x1*x1-y1*y1;
		z1 = (z1 > 0) ? (double) Math.sqrt(z1) : 0;

		double x2 = (xAct - width/2f)/size;
		double y2 = (yAct - width/2f)/size;
		double z2 = 1-x2*x2-y2*y2;
		z2 = (z2>0) ? (double) Math.sqrt(z2) : 0;

		// Cross product
		xRot = y1 * z2 - z1 * y2;
		yRot = z1 * x2 - x1 * z2;
		zRot = -x1 * y2 + y1 * x2;
		double len = xRot*xRot+yRot*yRot+zRot*zRot;
		if (len <= 0)
			return;

		double len_ = (double) (1./Math.sqrt(xRot*xRot+yRot*yRot+zRot*zRot));
		xRot *= len_;
		yRot *= len_;
		zRot *= len_;

		double dot = x1*x2+y1*y2+z1*z2;
		if (dot > 1) dot = 1;
		else if (dot < -1) dot = -1;
		rotAngle = (double) Math.acos(dot)*10;
		//System.out.println("xRot " + xRot + " yRot " + yRot + " zRot " + zRot + " rotAngle " + rotAngle);

		initializeTransformation();	
	}

	public void setMouseMovementOffset(int dx, int dy) {
		xOff += dx/scale;
		yOff += dy/scale;

		initializeTransformation();
	}

	Transform(Control control, double width, double height, double xOffa, double yOffa, double zOffa) {
		this.control = control;
		xSOff = (double)(width/2.  + 0.5);
		ySOff = (double)(height/2. + 0.5);

		xvOff = xOffa;
		yvOff = yOffa;
		zvOff = zOffa;

		mP = new double[4][4];
		mP[0][0] = mP[1][1] = mP[2][2] = mP[3][3] = 1;
	}

	public void setZAspect(double zAspect) {
		this.zAspect = zAspect;
		// validate the zAspect
		if (Double.isNaN(this.zAspect))
	    	this.zAspect=1;
		if (this.zAspect <=0.01f)
			this.zAspect = 0.01f;
	    
	};
}