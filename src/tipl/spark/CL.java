/**
 * 
 */
package tipl.spark;

import java.io.Serializable;
import java.util.List;

import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;
import tipl.spark.NeighborhoodPlugin.GatherBasedPlugin;
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

	@Override
	public Tuple2<D3int, TImgBlock<int[]>> GatherBlocks(
			Tuple2<D3int, List<TImgBlock<boolean[]>>> inBlocks) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean execute() {
		final D3int wholeSize=this.maskImg.getDim();
		// give every voxel a unique label as a long
		DTImg<long[]> labelImg=this.maskImg.map(
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
									long label=((z+spos.z)*wholeSize.y+(y+spos.y))*wholeSize.x+x+spos.x;
									oSlice[off]=label;
								}
							}
						}
						return  new Tuple2<D3int, TImgBlock<long[]>>(arg0._1,
								new TImgBlock<long[]>(oSlice,inBlock.getPos(),inBlock.getDim()));
					}

				}, TImgTools.IMAGETYPE_LONG);
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
