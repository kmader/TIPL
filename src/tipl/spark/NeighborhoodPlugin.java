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
 * @param <U> input image format
 * @param <V> output image format
 */
abstract public interface NeighborhoodPlugin<U extends Cloneable,V extends Cloneable> extends Serializable {
	/**
	 * A function which combines blocks containing the same position (and different offsets)
	 * A good example is to get surrounding values for calculating a filter
	 * @param inBlocks
	 * @return
	 */
	public Tuple2<D3int,TImgBlock<V>> GatherBlocks(Tuple2<D3int,List<TImgBlock<U>>> inBlocks);
	
	
	@SuppressWarnings("serial")
	public abstract class GatherBasedFilter<U extends Cloneable,V extends Cloneable> implements NeighborhoodPlugin<U,V> {
		public GatherBasedFilter() {
		}
		abstract public BaseTIPLPluginIn.filterKernel getKernel();
		abstract public BaseTIPLPluginIn.morphKernel getMKernel();
		abstract public D3int getNeighborSize();
	}
	abstract public class FloatFilter extends GatherBasedFilter<float[],float[]> {

		@Override
		public Tuple2<D3int,TImgBlock<float[]>> GatherBlocks(Tuple2<D3int,List<TImgBlock<float[]>>> inTuple) {
			final D3int ns=getNeighborSize();
			List<TImgBlock<float[]>> inBlocks=inTuple._2();
			final TImgBlock<float[]> templateBlock=inBlocks.get(0);
			final D3int blockSize=templateBlock.getDim();
			float[] outData=new float[templateBlock.get().length];
			BaseTIPLPluginIn.morphKernel mKernel=getMKernel();
			for(int zp=0;zp<templateBlock.getDim().z;zp++) {
				for(int yp=0;yp<templateBlock.getDim().y;yp++) {
					for(int xp=0;xp<templateBlock.getDim().x;xp++) {
						int off = ((zp) * blockSize.y + (yp)) * blockSize.x+(xp);
						BaseTIPLPluginIn.filterKernel curKernel=getKernel();
						for(TImgBlock<float[]> cBlock: inBlocks) {
							final float[] curBlock=cBlock.get();
							// the offset of the current block
							final int ix=cBlock.getOffset().x;
							final int iy=cBlock.getOffset().y;
							final int iz=cBlock.getOffset().z;
							// need to recalculate the bounds
							final int start_x=Math.max(-ns.x+ix,0);
							final int end_x=Math.min(ns.x+ix,blockSize.x);

							final int start_y=Math.max(-ns.y+iy,0);
							final int end_y=Math.min(ns.y+iy,blockSize.y);

							final int start_z=Math.max(-ns.z+iz,0);
							final int end_z=Math.min(ns.z+iz,blockSize.z);
							// ox,oy,oz are the coordinates inside the second block
							for(int oz=start_z;oz<end_z;oz++) {
								for(int oy=start_y;oy<end_y;oy++) {
									for(int ox=start_x;ox<end_x;ox++) {
										int off2 = ((oz) * blockSize.y + (oy)) * blockSize.x+(ox);
										if (mKernel.inside(off, off2, xp, xp+ox, yp, yp+oy, zp, zp+oz)) {
											curKernel.addpt(xp, xp+ox, yp, yp+oy, zp, zp+oz, curBlock[off2]);
										}
									}
								}
							}
						}
						outData[off]=(float) curKernel.value();
					}
				}
			}
			return new Tuple2<D3int,TImgBlock<float[]>>(inTuple._1(),templateBlock);
		}

	}
}
