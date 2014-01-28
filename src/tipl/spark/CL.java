/**
 * 
 */
package tipl.spark;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;
import tipl.spark.NeighborhoodPlugin.GatherBasedPlugin;
import tipl.tools.BaseTIPLPluginIn;
import tipl.tools.BaseTIPLPluginIn.filterKernel;
import tipl.tools.BaseTIPLPluginIn.morphKernel;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.ITIPLPlugin;
import tipl.util.TImgBlock;
import tipl.util.TImgTools;

/**
 * @author mader
 *
 */
public class CL extends GatherBasedPlugin<boolean[],int[]> {
	protected DTImg<boolean[]> maskImg;
	protected DTImg<long[]> labelImg;
	@Override
	public Tuple2<D3int, TImgBlock<int[]>> GatherBlocks(
			Tuple2<D3int, List<TImgBlock<boolean[]>>> inBlocks) {
		// TODO Auto-generated method stub
		return null;
	}
	protected void makeLabelImage() {
		final D3int wholeSize=this.maskImg.getDim();
		// give every voxel a unique label as a long
		labelImg=this.maskImg.map(
				new PairFunction<Tuple2<D3int, TImgBlock<boolean[]>>,D3int,TImgBlock<long[]>>() {

					@Override
					public Tuple2<D3int, TImgBlock<long[]>> call(
							Tuple2<D3int, TImgBlock<boolean[]>> arg0) throws Exception {
						final TImgBlock<boolean[]> inBlock=arg0._2;
						final boolean[] cSlice=inBlock.get();
						final D3int spos=inBlock.getPos();
						final D3int sdim=inBlock.getDim();
						final long[] oSlice=new long[cSlice.length];
						for(int z=0;z<sdim.z;z++) {
							for(int y=0;y<sdim.y;y++) {
								for(int x=0;x<sdim.x;x++) {
									int off=(int) (z*sdim.y+y)*sdim.x+x;
									// the initial label is just the index of the voxel in the whole image
									long label=((z+spos.z)*wholeSize.y+(y+spos.y))*wholeSize.x+x+spos.x;
									oSlice[off]=label;
								}
							}
						}
						return  new Tuple2<D3int, TImgBlock<long[]>>(arg0._1,
								new TImgBlock<long[]>(oSlice,inBlock.getPos(),inBlock.getDim()));
					}

				}, TImgTools.IMAGETYPE_LONG);
	}
	public void scanForNewGroups() {
		JavaRDD<Iterable<long[]>> connectedGroups=labelImg.spreadSlices(getNeighborSize().z).groupByKey().map(new FindConnectedGroups(getMKernel(),getNeighborSize()));
	}
	/** 
	 * a static class to perform the find connected groups in single slices / blocks
	 * @author mader
	 *
	 */
	protected static class FindConnectedGroups extends FlatMapFunction<Tuple2<D3int, List<TImgBlock<long[]>>>,long[]> {
		final protected BaseTIPLPluginIn.morphKernel mKernel;
		final protected D3int ns;
		public FindConnectedGroups(final BaseTIPLPluginIn.morphKernel mKernel,final D3int ns) {
			this.mKernel=mKernel;
			this.ns=ns;
		}
		@Override
		public List<long[]> call(
				Tuple2<D3int, List<TImgBlock<long[]>>> inTuple) throws Exception {

			final List<TImgBlock<long[]>> inBlocks = inTuple._2();
			final TImgBlock<long[]> templateBlock = inBlocks.get(0);
			final D3int blockSize = templateBlock.getDim();
			final int eleCount = (int) templateBlock.getDim().prod();
			// output list (overinitialize) since adding elements is expensive
			// using long[] to avoid creating too many objects
			List<long[]> groupsToMerge=new ArrayList<long[]>(eleCount);
			for (int zp = 0; zp < templateBlock.getDim().z; zp++) {
				for (int yp = 0; yp < templateBlock.getDim().y; yp++) {
					for (int xp = 0; xp < templateBlock.getDim().x; xp++) {
						/** position **/
						final int off = ((zp) * blockSize.y + (yp))
								* blockSize.x + (xp);
						
						for (final TImgBlock<long[]> cBlock : inBlocks) {
							final long[] curBlock = cBlock.get();
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
										final int off2 = ((oz) * blockSize.y + (oy))
												* blockSize.x + (ox);
										if (mKernel.inside(off, off2, xp, ox
												- offx, yp, oy - offy, zp, oz
												- offz)) {
											
											// curKernel.addpt(xp, ox - offx, yp, oy - offy, zp, oz - offz,
											// curBlock[off2]  current position
										}
									}
								}
							}
						}
						//setEle(outData, off, curKernel.value());
					}
				}
			}
			return groupsToMerge;
		}
		
	}
	@Override
	public boolean execute() {
		makeLabelImage();
		
		// now iteratively merge these groups until no more merges remain
		return true;
	}
	

	@Override
	public ArgumentParser setParameter(ArgumentParser p, String prefix) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public filterKernel getKernel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public morphKernel getMKernel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public D3int getNeighborSize() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPluginName() {
		// TODO Auto-generated method stub
		return null;
	}

}
