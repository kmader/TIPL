package tipl.spark;

import java.io.Serializable;
import java.util.List;

import scala.Tuple2;
import tipl.tools.BaseTIPLPlugin;
import tipl.tools.BaseTIPLPluginIO;
import tipl.tools.BaseTIPLPluginIn;
import tipl.util.D3int;
import tipl.util.ITIPLPlugin;
import tipl.util.TImgBlock;

/**
 * 
 * A generic interface for neighborhood operations and filters
 * @author mader
 * 
 * @param <U>
 *            input image format
 * @param <V>
 *            output image format
 */
abstract public interface NeighborhoodPlugin<U extends Cloneable, V extends Cloneable>
		extends Serializable,ITIPLPlugin {
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

	@SuppressWarnings("serial")
	public abstract class GatherBasedPlugin<U extends Cloneable, V extends Cloneable>
	   extends BaseTIPLPlugin implements NeighborhoodPlugin<U, V>  {
		
		public GatherBasedPlugin() {
		}

		abstract public BaseTIPLPluginIn.filterKernel getKernel();

		abstract public BaseTIPLPluginIn.morphKernel getMKernel();

		abstract public D3int getNeighborSize();
		
	}
	/**
	 * A very generic class for filtering with abstract methods for getting and setting elements inside the generic
	 * types since that is not by default support since they aren't arrays (boo java)
	 * @author mader
	 *
	 * @param <U>
	 * @param <V>
	 */
	abstract public class GenericFilter<U extends Cloneable,V extends Cloneable> extends GatherBasedPlugin<U,V> {
		abstract protected double getEle(U obj,int index);
		abstract protected void setEle(V obj,int index, double val);
		abstract protected V createObj(int size);
		@Override
		public Tuple2<D3int, TImgBlock<V>> GatherBlocks(
				Tuple2<D3int, List<TImgBlock<U>>> inTuple) {
			final D3int ns = getNeighborSize();
			
			final List<TImgBlock<U>> inBlocks = inTuple._2();
			final TImgBlock<U> templateBlock = inBlocks.get(0);
			final D3int blockSize = templateBlock.getDim();
			final BaseTIPLPluginIn.morphKernel mKernel = getMKernel();
			final int eleCount=(int) templateBlock.getDim().prod();
			// the output image
			final V outData = createObj(eleCount);
			
			for (int zp = 0; zp < templateBlock.getDim().z; zp++) {
				for (int yp = 0; yp < templateBlock.getDim().y; yp++) {
					for (int xp = 0; xp < templateBlock.getDim().x; xp++) {
						final int off = ((zp) * blockSize.y + (yp))
								* blockSize.x + (xp);
						BaseTIPLPluginIn.filterKernel curKernel = getKernel(); // curKernels[off];
	
						for (TImgBlock<U> cBlock : inBlocks) {
							final U curBlock = cBlock.get();
							// the offset of the current block
							final int offx = cBlock.getOffset().x;
							final int offy = cBlock.getOffset().y;
							final int offz = cBlock.getOffset().z;
							// the offset position
							final int ix = xp + offx;
							final int iy = yp + offy;
							final int iz = zp + offz;
							// need to recalculate the bounds

							final int start_x = Math.max(ix - ns.x, 0);
							final int end_x = Math.min(ix + ns.x, blockSize.x);

							final int start_y = Math.max(iy - ns.y, 0);
							final int end_y = Math.min(iy + ns.y, blockSize.y);

							final int start_z = Math.max(iz - ns.z, 0);
							final int end_z = Math.min(iz + ns.z, blockSize.z);

							// ox,oy,oz are the coordinates inside the second
							// block
							for (int oz = start_z; oz < end_z; oz++) {
								for (int oy = start_y; oy < end_y; oy++) {
									for (int ox = start_x; ox < end_x; ox++) {
										int off2 = ((oz) * blockSize.y + (oy))
												* blockSize.x + (ox);
										if (mKernel.inside(off, off2, xp, ox
												- offx, yp, oy - offy, zp, oz
												- offz)) {
											curKernel.addpt(xp, ox - offx, yp,
													oy - offy, zp, oz - offz,
													getEle(curBlock,off2));
										}
									}
								}
							}
						}
						setEle(outData,off,curKernel.value());
					}
				}
			}

			return new Tuple2<D3int, TImgBlock<V>>(inTuple._1(),
					new TImgBlock<V>(outData, templateBlock.getPos(),
							templateBlock.getDim()));
		}
	}
	/**
	 * THe simplist implementation of the float filter
	 * @author mader
	 *
	 */
	abstract public class FloatFilter extends
			GenericFilter<float[], float[]> {
		final public static boolean show_debug = false;
		protected double getEle(float[] obj,int index) {return obj[index];}
		protected void setEle(float[] obj,int index, double val) {obj[index]=(float) val;}
		protected float[] createObj(int size) {return new float[size];}	
	}

	abstract public class FloatFilterSlice extends FloatFilter {
		final public static boolean show_debug = false;

		@Override
		public Tuple2<D3int, TImgBlock<float[]>> GatherBlocks(
				Tuple2<D3int, List<TImgBlock<float[]>>> inTuple) {
			final D3int ns = getNeighborSize();
			List<TImgBlock<float[]>> inBlocks = inTuple._2();
			final TImgBlock<float[]> templateBlock = inBlocks.get(0);
			final D3int blockSize = templateBlock.getDim();
			final BaseTIPLPluginIn.morphKernel mKernel = getMKernel();
			// the output image
			float[] outData = new float[templateBlock.get().length];
			// Make the output image first as kernels, then add the respective
			// points to it
			final BaseTIPLPluginIn.filterKernel[] curKernels = new BaseTIPLPluginIn.filterKernel[(int) blockSize
					.prod()];
			
			for (int ci = 0; ci < curKernels.length; ci++)
				curKernels[ci] = getKernel();

			for (TImgBlock<float[]> cBlock : inBlocks) {
				final float[] curBlock = cBlock.get();
				
				// the offset of the current block
				final int offx = 0;
				final int offy = 0;
				final int offz = cBlock.getOffset().z;
				final int zp = 0;
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
									int off2 = (oy) * blockSize.x + (ox);
									if (mKernel.inside(off, off2, xp,
											ox, yp, oy, 0, offz)) {
										curKernel
												.addpt(xp, ox, yp, oy
														, 0, offz,
														curBlock[off2]);
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
