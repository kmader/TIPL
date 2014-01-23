package tipl.spark;

import java.util.List;

import tipl.util.TImgBlock;

abstract public class NeighborhoodPlugin<U extends Cloneable,V extends Cloneable> {
	
	abstract public TImgBlock<V> GatherBlocks(List<TImgBlock<U>> inBlocks);
	
	public class FloatFilter extends NeighborhoodPlugin<float[],float[]> {

		@Override
		public TImgBlock<float[]> GatherBlocks(List<TImgBlock<float[]>> inBlocks) {
			return inBlocks.get(0);
		}
		
	}
}
