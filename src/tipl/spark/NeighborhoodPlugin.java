package tipl.spark;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import scala.Tuple2;
import tipl.tools.BaseTIPLPlugin;
import tipl.tools.BaseTIPLPluginIn;
import tipl.util.D3int;
import tipl.util.D4int;
import tipl.util.ITIPLPlugin;
import tipl.util.TImgBlock;

/**
 * 
 * A generic interface for neighborhood operations and filters
 * 
 * @author mader
 * 
 * @param <U>
 *            input image format
 * @param <V>
 *            output image format
 */
abstract public interface NeighborhoodPlugin<U extends Cloneable, V extends Cloneable>
extends Serializable, ITIPLPlugin {
	/**
	 * THe simplist implementation of the float filter
	 * 
	 * @author mader
	 * 
	 */
	abstract public class FloatFilter extends GenericFilter<float[], float[]> {
		final public static boolean show_debug = false;

		@Override
		protected float[] createObj(int size) {
			return new float[size];
		}

		@Override
		protected double getEle(float[] obj, int index) {
			return obj[index];
		}

		@Override
		protected void setEle(float[] obj, int index, double val) {
			obj[index] = (float) val;
		}
	}



	@SuppressWarnings("serial")
	public abstract class GatherBasedPlugin<U extends Cloneable, V extends Cloneable>
	extends BaseTIPLPlugin implements NeighborhoodPlugin<U, V> {

		public GatherBasedPlugin() {
		}

		abstract public BaseTIPLPluginIn.filterKernel getKernel();

		abstract public BaseTIPLPluginIn.morphKernel getMKernel();

		abstract public D3int getNeighborSize();
		
		/**
		 * Generates all of the scan positions within the neighborhood given the current conditions
		 * @param start the position of the voxel being investigated
		 * @param offset position offset of the image
		 * @param off integer offset in the image of the starting point
		 * @param blockSize image size
		 * @param ns neighborhood
		 * @return
		 */
		public static List<D4int> getScanPositions(final D3int start,final D3int offset,final int off, final D3int blockSize, final D3int ns) {
			final int neighborCount=(2*ns.x+1)*(2*ns.y+1)*(2*ns.z+1);
			List<D4int> out=new ArrayList<D4int>(neighborCount);
			// correct for the offset
			final int ix = start.x + offset.x;
			final int iy = start.y + offset.y;
			final int iz = start.z + offset.z;
			
			// calculate the range using the neighborhood and the bounds
			final int start_x = Math.max(ix - ns.x, 0);
			final int end_x = Math.min(ix + ns.x, blockSize.x-1);

			final int start_y = Math.max(iy - ns.y, 0);
			final int end_y = Math.min(iy + ns.y, blockSize.y-1);

			final int start_z = Math.max(iz - ns.z, 0);
			final int end_z = Math.min(iz + ns.z, blockSize.z-1);
			// ox,oy,oz are the coordinates inside the second
			// block
			for (int oz = start_z; oz <= end_z; oz++) {
				for (int oy = start_y; oy <= end_y; oy++) {
					for (int ox = start_x; ox <= end_x; ox++) {
						final int off2 = ((oz) * blockSize.y + (oy))
								* blockSize.x + (ox);
						out.add(new D4int(ox-offset.x,oy-offset.y,oz-offset.z,off2));
					}
				}
			}
			return out;
		}

	}

	/**
	 * A very generic class for filtering with abstract methods for getting and
	 * setting elements inside the generic types since that is not by default
	 * support since they aren't arrays (boo java)
	 * 
	 * @author mader
	 * 
	 * @param <U>
	 * @param <V>
	 */
	abstract public class GenericFilter<U extends Cloneable, V extends Cloneable>
	extends GatherBasedPlugin<U, V> {
		abstract protected V createObj(int size);


		@Override
		public Tuple2<D3int, TImgBlock<V>> GatherBlocks(
				Tuple2<D3int, List<TImgBlock<U>>> inTuple) {
			final D3int ns = getNeighborSize();

			final List<TImgBlock<U>> inBlocks = inTuple._2();
			final TImgBlock<U> templateBlock = inBlocks.get(0);
			final D3int blockSize = templateBlock.getDim();
			final BaseTIPLPluginIn.morphKernel mKernel = getMKernel();
			final int eleCount = (int) templateBlock.getDim().prod();
			// the output image
			final V outData = createObj(eleCount);
			final BaseTIPLPluginIn.filterKernel[] kernelList = new BaseTIPLPluginIn.filterKernel[eleCount];
			for(int i=0;i<eleCount;i++) kernelList[i]=getKernel();
			for (final TImgBlock<U> cBlock : inBlocks) {
				final U curBlock = cBlock.get();
				for (int zp = 0; zp < templateBlock.getDim().z; zp++) {
					for (int yp = 0; yp < templateBlock.getDim().y; yp++) {
						for (int xp = 0; xp < templateBlock.getDim().x; xp++) {
							final int off = ((zp) * blockSize.y + (yp))
									* blockSize.x + (xp);
							final BaseTIPLPluginIn.filterKernel curKernel=kernelList[off];	
							for(D4int cPos : getScanPositions(new D3int(xp,yp,zp),cBlock.getOffset(),off, blockSize,ns)) {
								if (mKernel.inside(off, cPos.offset, xp, cPos.x, yp, cPos.y, zp, cPos.z)) {
									curKernel.addpt(xp, cPos.x, yp, cPos.y, zp, cPos.z,
											getEle(curBlock, cPos.offset));
								}
							}
						}
					}
				}
			}
			for(int i=0;i<eleCount;i++) setEle(outData, i, kernelList[i].value());
			return new Tuple2<D3int, TImgBlock<V>>(inTuple._1(),
					new TImgBlock<V>(outData, templateBlock.getPos(),
							templateBlock.getDim()));
		}

		abstract protected double getEle(U obj, int index);

		abstract protected void setEle(V obj, int index, double val);
	}

	/**
	 * A function which combines blocks containing the same position (and
	 * different offsets) A good example is to get surrounding values for
	 * calculating a filter
	 * 
	 * @param inBlocks
	 * @return
	 */
	public Tuple2<D3int, TImgBlock<V>> GatherBlocks(
			Tuple2<D3int, List<TImgBlock<U>>> inBlocks);


	/** 
	 * A version of the float filter optimized for operating on slices
	 * @author mader
	 *
	 */
	abstract public class FloatFilterSlice extends FloatFilter {
		final public static boolean show_debug = false;

		@Override
		public Tuple2<D3int, TImgBlock<float[]>> GatherBlocks(
				Tuple2<D3int, List<TImgBlock<float[]>>> inTuple) {
			final D3int ns = getNeighborSize();
			final List<TImgBlock<float[]>> inBlocks = inTuple._2();
			final TImgBlock<float[]> templateBlock = inBlocks.get(0);
			final D3int blockSize = templateBlock.getDim();
			final BaseTIPLPluginIn.morphKernel mKernel = getMKernel();
			// the output image
			final float[] outData = new float[templateBlock.get().length];
			// Make the output image first as kernels, then add the respective
			// points to it
			final BaseTIPLPluginIn.filterKernel[] curKernels = new BaseTIPLPluginIn.filterKernel[(int) blockSize
			                                                                                     .prod()];

			for (int ci = 0; ci < curKernels.length; ci++)
				curKernels[ci] = getKernel();

			for (final TImgBlock<float[]> cBlock : inBlocks) {
				final float[] curBlock = cBlock.get();

				// the offset of the current block
				final int offx = 0;
				final int offy = 0;
				final int offz = cBlock.getOffset().z;
				if (Math.abs(offz) <= ns.z) {
					for (int yp = 0; yp < templateBlock.getDim().y; yp++) {
						for (int xp = 0; xp < templateBlock.getDim().x; xp++) {
							final int off = (yp) * blockSize.x + (xp);
							final BaseTIPLPluginIn.filterKernel curKernel = curKernels[off];
							// the offset position
							final int ix = xp + offx;
							final int iy = yp + offy;

							// need to recalculate the bounds

							final int start_x = Math.max(ix - ns.x, 0);
							final int end_x = Math.min(ix + ns.x, blockSize.x);

							final int start_y = Math.max(iy - ns.y, 0);
							final int end_y = Math.min(iy + ns.y, blockSize.y);

							// ox,oy,oz are the coordinates inside the second
							// block
							for (int oy = start_y; oy < end_y; oy++) {
								for (int ox = start_x; ox < end_x; ox++) {
									final int off2 = (oy) * blockSize.x + (ox);
									if (mKernel.inside(off, off2, xp, ox, yp,
											oy, 0, offz)) {
										curKernel.addpt(xp, ox, yp, oy, 0,
												offz, curBlock[off2]);
									}
								}
							}
						}
					}
				}
			}
			for (int i = 0; i < outData.length; i++)
				outData[i] = (int) curKernels[i].value();
			return new Tuple2<D3int, TImgBlock<float[]>>(inTuple._1(),
					new TImgBlock<float[]>(outData, templateBlock.getPos(),
							templateBlock.getDim()));
		}

	}
}
