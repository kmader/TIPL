package tipl.tests;

import tipl.formats.PureFImage;
import tipl.formats.TImgRO;
import tipl.formats.PureFImage.PositionFunction;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * A collection of simple bw synthetic images used for unit-testing in TIPL
 * 
 * @author mader
 * 
 */
public abstract class TestFImages implements PureFImage.PositionFunction {
	/**
	 * diagonal line
	 * 
	 * @author mader
	 * 
	 */
	public static class DiagonalLineFunction extends TestFImages {
		@Override
		public boolean tget(long x, long y, long z) {
			return (z == y & y == x);
		}
	}
	
	public static class DiagonalPlaneAndDotsFunction extends TestFImages {
		private final TestFImages Plane=new DiagonalPlaneFunction();
		private final TestFImages Dots=new DotsFunction();
		@Override
		public boolean tget(long x, long y, long z) {
			return Plane.tget(x, y, z) | Dots.tget(x, y, z);
		}
	}
	/**
	 * diagonal plane
	 * 
	 * @author mader
	 * 
	 */
	public static class DiagonalPlaneFunction extends TestFImages {
		@Override
		public boolean tget(long x, long y, long z) {
			return (Math.abs(z - (x + y)) < 0.5) & ((x + y) % 2 == (z % 2));
		}
	}

	/**
	 * dot pattern
	 * 
	 * @author mader
	 * 
	 */
	public static class DotsFunction extends TestFImages {
		@Override
		public boolean tget(long x, long y, long z) {
			return ((x + y + z) % 2 == 1);
		}
	}

	/**
	 * lines
	 * 
	 * @author mader
	 * 
	 */
	public static class LinesFunction extends TestFImages {
		@Override
		public boolean tget(long x, long y, long z) {
			return ((x + y) % 2 == 1);
		}
	}

	/**
	 * progresive x image
	 * 
	 * @author mader
	 * 
	 */
	public static class ProgXImage extends TestFImages {
		@Override
		public double[] getRange() {
			return new double[] { 0, 2000 };
		}

		@Override
		public double rget(long x, long y, long z) {
			return x;
		}

	}

	/**
	 * progresive z image
	 * 
	 * @author mader
	 * 
	 */
	public static class ProgZImage extends TestFImages {
		@Override
		public double[] getRange() {
			return new double[] { 0, 2000 };
		}

		@Override
		public double rget(long x, long y, long z) {
			return z;
		}

	}

	/**
	 * simple sheets seperated by 1 voxel
	 * 
	 * @author mader
	 * 
	 */
	public static class SheetImageFunction extends TestFImages {
		@Override
		public boolean tget(long x, long y, long z) {
			return (x % 2 == 1); // sheets
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
				// TODO Auto-generated method stub
				return null;
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
	public static TImgRO wrapIt(int sizeX, PureFImage.PositionFunction pf) {
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
	public static TImgRO wrapItAs(int sizeX, PureFImage.PositionFunction pf,
			int imType) {
		return new PureFImage(justDims(new D3int(sizeX, sizeX, sizeX)), imType,
				pf);
	}

	@Override
	public double get(Double[] ipos) {
		return rget(Math.round(ipos[0]), Math.round(ipos[1]),
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
	 * function to get the number using x,y,z instead of the silly array
	 */
	public double rget(long x, long y, long z) {
		return tget(x, y, z) ? 1.0 : 0;
	}

	/**
	 * function to override for just binary images
	 */
	public boolean tget(long x, long y, long z) {
		return false;
	}

}