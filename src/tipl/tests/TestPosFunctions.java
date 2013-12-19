package tipl.tests;

import java.util.Random;

import tipl.formats.PureFImage;
import tipl.formats.TImgRO;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * A collection of simple bw synthetic images used for unit-testing in TIPL
 * 
 * @author mader
 * 
 */
public abstract class TestPosFunctions implements PureFImage.PositionFunction {
	/**
	 * diagonal line
	 * 
	 * @author mader
	 * 
	 */
	public static class DiagonalLineFunction extends TestPosFunctions {
		@Override
		public boolean tget(final long x, final long y, final long z) {
			return (z == y & y == x);
		}
	}

	public static class DiagonalPlaneAndDotsFunction extends TestPosFunctions {
		private final TestPosFunctions Plane = new DiagonalPlaneFunction();
		private final TestPosFunctions Dots = new DotsFunction();

		@Override
		public boolean tget(final long x, final long y, final long z) {
			return Plane.tget(x, y, z) | Dots.tget(x, y, z);
		}
	}

	/**
	 * diagonal plane
	 * 
	 * @author mader
	 * 
	 */
	public static class DiagonalPlaneFunction extends TestPosFunctions {
		@Override
		public boolean tget(final long x, final long y, final long z) {
			return (Math.abs(z - (x + y)) < 0.5) & ((x + y) % 2 == (z % 2));
		}
	}

	/**
	 * dot pattern
	 * 
	 * @author mader
	 * 
	 */
	public static class DotsFunction extends TestPosFunctions {
		@Override
		public boolean tget(final long x, final long y, final long z) {
			return ((x + y + z) % 2 == 1);
		}
	}
	/**
	 * background image, plus a bw image with a given phase
	 * 
	 * @author mader
	 * 
	 */
	public static class BGPlusPhase extends TestPosFunctions {
		protected final TestPosFunctions bgFun;
		protected final TestPosFunctions phFun;
		protected final int phase;
		protected final double[] range;
		public BGPlusPhase(final TestPosFunctions ibgf, final TestPosFunctions iphf, final int iphase) {
			bgFun=ibgf;
			double minVal=Math.min(ibgf.getRange()[0], iphase);
			double maxVal=Math.max(ibgf.getRange()[1], iphase);
			range=new double[] {minVal,maxVal};
			phFun=iphf;
			phase=iphase;
			
		}
		@Override
		public double[] getRange() {
			return range;
		}
		@Override
		public double rget(final long ix, final long iy, final long iz) {
			return phFun.nget(ix,iy,iz)>0 ? phase : (bgFun.nget(ix,iy,iz));
		}
	}
	
	/**
	 * random flipping between phases
	 * 
	 * @author mader
	 * 
	 */
	public static class PhaseNoise extends TestPosFunctions {
		protected final TestPosFunctions phFun;
		protected final int[] possiblephases;
		protected final double noiselevel;
		protected final double[] range;
		protected Random rn = new Random();
		/**
		 * Creates a noisy version of an object
		 * @param iphf starting phase function
		 * @param possiblephases the list of possible phases
		 * @param noiselevel the probability of a flip at a given point
		 */
		public PhaseNoise(final TestPosFunctions iphf, final int[] ipossiblephases,final double inoiselevel) {
			phFun=iphf;
			range=iphf.getRange();
			possiblephases=ipossiblephases;
			noiselevel=inoiselevel;	
		}
		@Override
		public double[] getRange() {
			return range;
		}
		@Override
		public double rget(final long ix, final long iy, final long iz) {
			if (rn.nextDouble()>noiselevel)
				return possiblephases[rn.nextInt(possiblephases.length)];
			else 
				return phFun.nget(ix,iy,iz);
		}
	}
	
	/**
	 * single ellipsoid at (5,5,5) with a radius 5 or whatever is given in the
	 * constructor
	 * 
	 * @author mader
	 * 
	 */
	public static class EllipsoidFunction extends TestPosFunctions {
		protected final int x, y, z;
		protected final float rx, ry, rz;

		public EllipsoidFunction() {
			x = 5;
			y = 5;
			z = 5;
			rx = 5.0f;
			ry = 5.0f;
			rz = 5.0f;
		}

		public EllipsoidFunction(final int ix, final int iy, final int iz,
				final float ir) {
			x = ix;
			y = iy;
			z = iz;
			rx = ir;
			ry = ir;
			rz = ir;
		}

		public EllipsoidFunction(final int ix, final int iy, final int iz,
				final float irx, final float iry, final float irz) {
			x = ix;
			y = iy;
			z = iz;
			rx = irx;
			ry = iry;
			rz = irz;
		}

		@Override
		public boolean tget(final long ix, final long iy, final long iz) {
			return (Math.pow(ix - x, 2) / Math.pow(rx, 2) + Math.pow(iy - y, 2)
					/ Math.pow(ry, 2) + Math.pow(iz - z, 2) / Math.pow(rz, 2)) <= 1;
		}
	}

	/**
	 * fixed value image
	 * 
	 * @author mader
	 * 
	 */
	public static class FixedValueImage extends TestPosFunctions {
		private final int fixedValue;

		public FixedValueImage(final int value) {
			fixedValue = value;
		}

		@Override
		public double[] getRange() {
			return new double[] { 0, fixedValue };
		}

		@Override
		public double rget(final long x, final long y, final long z) {
			return fixedValue;
		}

	}

	/**
	 * lines
	 * 
	 * @author mader
	 * 
	 */
	public static class LinesFunction extends TestPosFunctions {
		@Override
		public boolean tget(final long x, final long y, final long z) {
			return ((x + y) % 2 == 1);
		}
	}
	
	
	/**
	 * layered material (xy slices stacked in z)
	 * 
	 * @author mader
	 * 
	 */
	public static class LayeredImage extends TestPosFunctions {
		protected final float xwidth,ywidth,zwidth;
		protected final int phase1, phase2;
		public LayeredImage( final int iphase1,final int iphase2, final float ixwidth) {
			phase1=iphase1;
			phase2=iphase2;
			xwidth=ixwidth;
			ywidth=0;
			zwidth=0;
		}
		public LayeredImage( final int iphase1,final int iphase2, final float ixwidth, final float iywidth, final float izwidth) {
			phase1=iphase1;
			phase2=iphase2;
			xwidth=ixwidth;
			ywidth=iywidth;
			zwidth=izwidth;
		}
		@Override
		public double[] getRange() {
			return new double[] {  Math.min(phase1, phase2), Math.max(phase1, phase2) };
		}
		
		@Override
		public double rget(final long x, final long y, final long z) {
			int phase=0;
			if (xwidth>0) phase+=Math.round(x/xwidth)%2;
			if (ywidth>0) phase+=Math.round(y/ywidth)%2;
			if (zwidth>0) phase+=Math.round(z/zwidth)%2;
			return ((phase%2)>0) ? phase1 : phase2;
		}

	}
	/**
	 * Radially (cylinrical) layers centered at a predetermined point
	 * @author mader
	 *
	 */
	public static class PolarLayeredImage extends LayeredImage {
		protected final float xc,yc,zc;
		/**
		 * Create a new function for a radially layered polar image (just in r)
		 * @param xcent center position
		 * @param ycent
		 * @param zcent
		 * @param iphase1
		 * @param iphase2
		 * @param irwidth width of radial rings
		 */
		public PolarLayeredImage( final int xcent, final int ycent, final int zcent, final int iphase1,final int iphase2, final float irwidth) {
			super(iphase1,iphase2,irwidth);
			xc=xcent;
			yc=ycent;
			zc=zcent;
		}
		/**
		 * 
		 * @param xcent
		 * @param ycent
		 * @param zcent
		 * @param iphase1
		 * @param iphase2
		 * @param irwidth width of radial rings
		 * @param ithwidth spread of sectors in theta
		 * @param izwidth height of rings in z
		 */
		public PolarLayeredImage( final int xcent, final int ycent, final int zcent, final int iphase1,final int iphase2, final float irwidth, final float ithwidth, final float izwidth) {
			super(iphase1,iphase2,irwidth,ithwidth,izwidth);
			xc=xcent;
			yc=ycent;
			zc=zcent;
		}
		@Override
		public double rget(final long x, final long y, final long z) {
			int phase=0;
			double r=Math.sqrt(Math.pow(x-xc, 2)+Math.pow(y-yc, 2));
			double th=Math.atan2(y-yc, x-xc)*180/Math.PI;
			if (xwidth>0) phase+=Math.round(r/xwidth)%2;
			if (ywidth>0) phase+=Math.round(th/ywidth)%2;
			if (zwidth>0) phase+=Math.round(z/zwidth)%2;
			return ((phase%2)>0) ? phase1 : phase2;
		}
	}
	/**
	 * Spherical layers centered at a predetermined point
	 * @author mader
	 *
	 */
	public static class SphericalLayeredImage extends LayeredImage {
		protected final float xc,yc,zc;
		/**
		 * Create a new function for a radially layered polar image (just in r)
		 * @param xcent center position
		 * @param ycent
		 * @param zcent
		 * @param iphase1
		 * @param iphase2
		 * @param irwidth width of radial shells
		 */
		public SphericalLayeredImage( final int xcent, final int ycent, final int zcent, final int iphase1,final int iphase2, final float irwidth) {
			super(iphase1,iphase2,irwidth);
			xc=xcent;
			yc=ycent;
			zc=zcent;
		}
		/**
		 * 
		 * @param xcent
		 * @param ycent
		 * @param zcent
		 * @param iphase1
		 * @param iphase2
		 * @param irwidth width of radial shells
		 * @param ithwidth spread of sectors in phi (XY)
		 * @param izwidth spread of the sectors in theta (XZ)
		 */
		public SphericalLayeredImage( final int xcent, final int ycent, final int zcent, final int iphase1,final int iphase2, final float irwidth, final float ithwidth, final float izwidth) {
			super(iphase1,iphase2,irwidth,ithwidth,izwidth);
			xc=xcent;
			yc=ycent;
			zc=zcent;
		}
		@Override
		public double rget(final long x, final long y, final long z) {
			int phase=0;
			double r=Math.sqrt(Math.pow(x-xc, 2)+Math.pow(y-yc, 2)+Math.pow(z-zc,2));
			double phi=Math.atan2(y-yc, x-xc)*180/Math.PI;
			
			double theta=Math.atan2(z-yc, r)*180/Math.PI;
			if (xwidth>0) phase+=Math.round(r/xwidth)%2;
			if (ywidth>0) phase+=Math.round(phi/ywidth)%2;
			if (zwidth>0) phase+=Math.round(theta/zwidth)%2;
			return ((phase%2)>0) ? phase1 : phase2;
		}
	}
	/**
	 * progressive x image
	 * 
	 * @author mader
	 * 
	 */
	public static class ProgXImage extends TestPosFunctions {
		@Override
		public double[] getRange() {
			return new double[] { 0, 2000 };
		}

		@Override
		public double rget(final long x, final long y, final long z) {
			return x;
		}

	}

	/**
	 * progressive y image
	 * 
	 * @author mader
	 * 
	 */
	public static class ProgYImage extends TestPosFunctions {
		@Override
		public double[] getRange() {
			return new double[] { 0, 2000 };
		}

		@Override
		public double rget(final long x, final long y, final long z) {
			return y;
		}

	}

	/**
	 * progressive z image
	 * 
	 * @author mader
	 * 
	 */
	public static class ProgZImage extends TestPosFunctions {
		
		@Override
		public double[] getRange() {
			return new double[] { 0, 2000 };
		}

		@Override
		public double rget(final long x, final long y, final long z) {
			return z;
		}

	}

	/**
	 * simple sheets separated by 1 voxel
	 * 
	 * @author mader
	 * 
	 */
	public static class SheetImageFunction extends TestPosFunctions {
		@Override
		public boolean tget(final long x, final long y, final long z) {
			return (x % 2 == 1); // sheets
		}
	}

	/**
	 * single point at (5,5,5)
	 * 
	 * @author mader
	 * 
	 */
	public static class SinglePointFunction extends TestPosFunctions {
		protected final int x, y, z;

		public SinglePointFunction() {
			x = 5;
			y = 5;
			z = 5;
		}

		public SinglePointFunction(final int ix, final int iy, final int iz) {
			x = ix;
			y = iy;
			z = iz;
		}

		@Override
		public boolean tget(final long ix, final long iy, final long iz) {
			return (ix == x) & (iy == y) & (iz == z);
		}
	}

	public static TImgTools.HasDimensions justDims(final D3int inDim) {
		return new TImgTools.HasDimensions() {

			@Override
			public D3int getDim() {
				// TODO Auto-generated method stub
				return inDim;
			}

			@Override
			public D3float getElSize() {
				// TODO Auto-generated method stub
				return new D3float(1, 1, 1);
			}

			@Override
			public D3int getOffset() {
				// TODO Auto-generated method stub
				return new D3int(0);
			}

			@Override
			public D3int getPos() {
				// TODO Auto-generated method stub
				return new D3int(0);
			}

			@Override
			public String getProcLog() {
				// just return nothing
				return "";
			}

			@Override
			public float getShortScaleFactor() {
				// this value is fixed in these images
				return 1.0f;
			}

		};
	}

	/**
	 * wraps a boolean position function in an image
	 * 
	 * @param sizeX
	 *            size of cube side
	 * @param pf
	 *            position function
	 * @return an image
	 */
	public static TImgRO wrapIt(final int sizeX,
			final PureFImage.PositionFunction pf) {
		return wrapItAs(sizeX, pf, 10);
	}

	/**
	 * wraps a boolean position function in an image
	 * 
	 * @param sizeX
	 *            size of cube side
	 * @param pf
	 *            position function
	 * @return an image
	 */
	public static TImgRO wrapItAs(final int sizeX,
			final PureFImage.PositionFunction pf, final int imType) {
		return new PureFImage(justDims(new D3int(sizeX, sizeX, sizeX)), imType,
				pf);
	}

	@Override
	public double get(final Double[] ipos) {
		return nget(Math.round(ipos[0]), Math.round(ipos[1]),
				Math.round(ipos[2]));
	}

	@Override
	public double[] getRange() {
		return new double[] { 0, 1 };
	}

	@Override
	public String name() {
		return "TestPositionFunction!";
	}
	/**
	 * enables rotation and sets the values  to theta and phi in degrees (converted to radians)
	 * @param otheta (xz) rotation
	 * @param iphi  (xy) rotation
	 */
	public void setRotation(double otheta, double iphi,double icx, double icy, double icz) { 
		theta=otheta/180.0*Math.PI;
		phi=iphi/180.0*Math.PI;
		cx=icx;
		cy=icy;
		cz=icz;
		rotated=true;
	}
	public void disableRotation() {
		rotated=false;
	}
	
	/**
	 * theta and phi to rotate by
	 */
	protected double theta=0,phi=0;
	/**
	 * center position for rotation (subtracted and added again)
	 */
	protected double cx=0,cy=0,cz=0;
	/** should the rotation function even be used **/
	protected boolean rotated=false;
	/**
	 * set the maximum value of the noise signal 
	 * @param inoisemax maximum value of the noise (negative means phase noise and absolute value is the probability of switching)
	 */
	public void setNoise(final double inoisemax) { noisemax=inoisemax;}
	/**
	 * set the possible phases for phase-based noise (phase jumping)
	 * @param iphases list of possible phase values
	 */
	public void setNoisePhases(final double[] iphases) {phases=iphases;}
	/** amount of noise in the image **/
	protected double noisemax=0.0;
	protected double[] phases={0.0};
	protected Random rn = new Random();
	
	final public double nget(final long x, final long y, final long z) {
		double noise=0.0;
		if (noisemax>0) noise=noisemax*2*(rn.nextDouble()-0.5);
		if (noisemax<0) if (rn.nextDouble()<(-1*noisemax)) return phases[rn.nextInt(phases.length)];
		if (rotated) return rot_rget(x,y,z)+noise;
		else return rget(x,y,z)+noise;
		
	}
	/**
	 * rotates the coordinates according to theta (xz) and phi (xy)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public double rot_rget(final long x, final long y, final long z) {
		double rx1=Math.cos(phi)*(x-cx)-Math.sin(phi)*(y-cy);
		double ry1=Math.sin(phi)*(x-cx)+Math.cos(phi)*(y-cy);
		//z stays put
		double rx2=Math.cos(theta)*rx1-Math.sin(theta)*(z-cz);
		//y2 stays put
		double rz2=Math.sin(theta)*rx1+Math.cos(theta)*(z-cz);
		
		return rget(Math.round(rx2+cx),Math.round(ry1+cy),Math.round(rz2+cz));
	}
	/**
	 * function to get the number using x,y,z instead of the silly array
	 */
	public double rget(final long x, final long y, final long z) {
		return tget(x, y, z) ? 1.0 : 0;
	}

	/**
	 * function to override for just binary images
	 */
	public boolean tget(final long x, final long y, final long z) {
		return false;
	}

}