package tipl.spark;

import java.io.Serializable;
import java.util.List;

import scala.Tuple2;
import tipl.tools.BaseTIPLPluginIO;
import tipl.tools.BaseTIPLPluginIn;
import tipl.util.D3int;
import tipl.util.TImgBlock;

/**
 * 
 * @author mader
 * 
 * @param <U>
 *            input image format
 * @param <V>
 *            output image format
 */
abstract public interface NeighborhoodPlugin<U extends Cloneable, V extends Cloneable>
		extends Serializable {
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
	public abstract class GatherBasedFilter<U extends Cloneable, V extends Cloneable>
			implements NeighborhoodPlugin<U, V> {
		public GatherBasedFilter() {
		}

		abstract public BaseTIPLPluginIn.filterKernel getKernel();

		abstract public BaseTIPLPluginIn.morphKernel getMKernel();

		abstract public D3int getNeighborSize();
	}

	abstract public class FloatFilter extends
			GatherBasedFilter<float[], float[]> {
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
			// BaseTIPLPluginIn.filterKernel[] curKernels= new
			// BaseTIPLPluginIn.filterKernel[(int) blockSize.prod()];
			// for(int ci=0;ci<curKernels.length;ci++)
			// curKernels[ci]=getKernel();
			for (int zp = 0; zp < templateBlock.getDim().z; zp++) {
				for (int yp = 0; yp < templateBlock.getDim().y; yp++) {
					for (int xp = 0; xp < templateBlock.getDim().x; xp++) {
						final int off = ((zp) * blockSize.y + (yp))
								* blockSize.x + (xp);
						BaseTIPLPluginIn.filterKernel curKernel = getKernel(); // curKernels[off];
						int vcnt = 0;
						int scnt = 0;
						for (TImgBlock<float[]> cBlock : inBlocks) {
							scnt = 0;
							final float[] curBlock = cBlock.get();
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
							if (show_debug)
								System.out.println(String.format(
										"%d: Off%d,%d,%d", off, ix, iy, iz));
							if (show_debug)
								System.out.println(String.format(
										"X: %d %d-%d,Y: %d %d-%d,Z: %d %d-%d",
										xp, start_x, end_x, yp, start_y, end_y,
										zp, start_z, end_z));
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
													curBlock[off2]);
											vcnt++;
											scnt++;
										}
									}
								}
							}
						}
						outData[off] = (float) curKernel.value();
						if (show_debug)
							System.out.println(String.format(
									"Vox : %d %d %d = %d, sc = %d", xp, yp, zp,
									vcnt, scnt));
					}
				}
			}

			return new Tuple2<D3int, TImgBlock<float[]>>(inTuple._1(),
					new TImgBlock<float[]>(outData, templateBlock.getPos(),
							templateBlock.getDim()));
		}

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
				outData[i] = (float) curKernels[i].value();
			return new Tuple2<D3int, TImgBlock<float[]>>(inTuple._1(),
					new TImgBlock<float[]>(outData, templateBlock.getPos(),
							templateBlock.getDim()));
		}

	}
}
