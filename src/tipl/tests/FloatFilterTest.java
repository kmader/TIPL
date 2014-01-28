package tipl.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import scala.Tuple2;
import tipl.tools.BaseTIPLPluginIn;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.D4int;
import tipl.util.TImgBlock;
import tipl.formats.TImgRO;
import tipl.spark.NeighborhoodPlugin.FloatFilter;
import tipl.spark.NeighborhoodPlugin.FloatFilterSlice;
import tipl.spark.NeighborhoodPlugin.GatherBasedPlugin;

public class FloatFilterTest {
	
	static public class FloatFilterTestImpl extends FloatFilter {
		@Override
		public BaseTIPLPluginIn.filterKernel getKernel() {
			return BaseTIPLPluginIn.gaussFilter(0.4);
		}

		@Override
		public BaseTIPLPluginIn.morphKernel getMKernel() {
			return new BaseTIPLPluginIn.stationaryKernel(BaseTIPLPluginIn.fullKernel);	
		}
		
		@Override
		public D3int getNeighborSize() {
			return new D3int(2,2,1);
		}

		@Override
		public boolean execute() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public ArgumentParser setParameter(ArgumentParser p, String prefix) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getPluginName() {
			// TODO Auto-generated method stub
			return null;
		}
	}
	static public class FloatFilterTestImpl2 extends FloatFilterSlice {
		@Override
		public BaseTIPLPluginIn.filterKernel getKernel() {
			return BaseTIPLPluginIn.gaussFilter(0.4);
		}

		@Override
		public BaseTIPLPluginIn.morphKernel getMKernel() {
			return new BaseTIPLPluginIn.stationaryKernel(BaseTIPLPluginIn.fullKernel);
		}
		
		@Override
		public D3int getNeighborSize() {
			return new D3int(1,1,1);
		}

		@Override
		public boolean execute() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public ArgumentParser setParameter(ArgumentParser p, String prefix) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getPluginName() {
			// TODO Auto-generated method stub
			return null;
		}
	}
	private FloatFilter ff = new FloatFilterTestImpl();
	private FloatFilter ffSlice = new FloatFilterTestImpl2();
	static final TImgRO lineImg = TestPosFunctions.wrapItAs(400,
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
	public void testListPerformance() {
		D3int start=new D3int(1);
		D3int ns=new D3int(1);
		D3int blockSize=new D3int(10);
		D3int offset=new D3int(0);
		for(int i=0;i<10000;i++) {
			List<D4int> scanpos=GatherBasedPlugin.getScanPositions(start, offset, 0, blockSize, ns);
			long offsum=0;
			for (D4int scanpoint: scanpos) offsum+=scanpoint.offset;
		}
	}
	@Test
	public void testListGeneration() {
		D3int start=new D3int(1);
		D3int ns=new D3int(1);
		D3int blockSize=new D3int(10);
		D3int offset=new D3int(0);
		List<D4int> scanpos=GatherBasedPlugin.getScanPositions(start, offset, 0, blockSize, ns);
		assertEquals(scanpos.size(),27);
		//test the bottom corner
		start=new D3int(0);
		scanpos=GatherBasedPlugin.getScanPositions(start, offset, 0, blockSize, ns);
		assertEquals(scanpos.size(),8);
		//test the top corner
		start=new D3int(9);
		scanpos=GatherBasedPlugin.getScanPositions(start, offset, 0, blockSize, ns);
		assertEquals(scanpos.size(),8);
		
		//test the bottomish corner
		start=new D3int(0,1,1);
		scanpos=GatherBasedPlugin.getScanPositions(start, offset, 0, blockSize, ns);
		assertEquals(scanpos.size(),18);
		
		//test the bottomish corner
		start=new D3int(1,0,0);
		scanpos=GatherBasedPlugin.getScanPositions(start, offset, 0, blockSize, ns);
		assertEquals(scanpos.size(),12);
		
		//test a big window
		start=new D3int(4,4,4);
		ns=new D3int(4,4,4);
		scanpos=GatherBasedPlugin.getScanPositions(start, offset, 0, blockSize, ns);
		assertEquals(scanpos.size(),9*9*9);
	}
	@Test
	public void testKernel() {
		// everything should be inside this kernel
		assert(ff.getMKernel().inside(-1, -1, 0, 50, 0, -30, 0, 10));
	}
	protected static float[] inSlice=(float[]) lineImg.getPolyImage(5, 3);
	protected static List<TImgBlock<float[]>> someSlices=makeSomeSlices(lineImg,2);
	@Test
	public void testGatherBlocks() {
		int startSlice=2;
		TImgBlock<float[]> outSlice=ff.GatherBlocks(new Tuple2<D3int,List<TImgBlock<float[]>>>(new D3int(0,0,startSlice),
				someSlices))._2();
		System.out.println(String.format("i\tIn\tOut"));
		for(int i=0;i<inSlice.length;i++) {
			if(i%1000==0) System.out.println(String.format("%d\t%3.2f\t%3.2f",i,inSlice[i],outSlice.get()[i]));
			assertEquals(inSlice[i],outSlice.get()[i],0.1f);
		}
	}

}
