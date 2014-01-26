package tipl.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import scala.Tuple2;
import tipl.tools.BaseTIPLPluginIn;
import tipl.util.D3int;
import tipl.util.TImgBlock;
import tipl.formats.TImgRO;
import tipl.spark.NeighborhoodPlugin.FloatFilter;

public class FloatFilterTest {
	static public class FloatFilterTestImpl extends FloatFilter {
		@Override
		public BaseTIPLPluginIn.filterKernel getKernel() {
			return BaseTIPLPluginIn.gaussFilter(0.5);
		}

		@Override
		public BaseTIPLPluginIn.morphKernel getMKernel() {
			return BaseTIPLPluginIn.fullKernel;
		}
		
		@Override
		public D3int getNeighborSize() {
			return new D3int(1,1,1);
		}
	}
	private FloatFilter ff = new FloatFilterTestImpl();;
	static final TImgRO lineImg = TestPosFunctions.wrapItAs(4,
			new TestPosFunctions.LinesFunction(),3);
	protected static List<TImgBlock<float[]>> makeSomeSlices(TImgRO testImg,int startZ) {
		List<TImgBlock<float[]>> imList=new ArrayList<TImgBlock<float[]>>();
		for(int i=0;i<testImg.getDim().z;i++) {
			D3int pos=new D3int(0,0,startZ);
			D3int offset=new D3int(0,0,startZ-i);
			D3int dim=new D3int(testImg.getDim().x,testImg.getDim().y,1);
			imList.add(new TImgBlock<float[]>((float[]) testImg.getPolyImage(1,3),pos,dim,offset));
		}
		return imList;
	}
	@Test
	public void testKernel() {
		// everything should be inside this kernel
		assert(ff.getMKernel().inside(-1, -1, 0, 50, 0, -30, 0, 10));
	}
	
	@Test
	public void testGatherBlocks() {
		int startSlice=2;
		float[] inSlice=(float[]) lineImg.getPolyImage(5, 3);
		TImgBlock<float[]> outSlice=ff.GatherBlocks(new Tuple2<D3int,List<TImgBlock<float[]>>>(new D3int(0,0,startSlice),
				makeSomeSlices(lineImg,startSlice)))._2();
		System.out.println(String.format("i\tIn\tOut"));
		for(int i=0;i<inSlice.length;i++) {
			System.out.println(String.format("%d\t%3.2f\t%3.2f",i,inSlice[i],outSlice.get()[i]));
		}
		fail("Not yet implemented"); // TODO
	}

}
