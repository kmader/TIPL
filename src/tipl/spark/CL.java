/**
 * 
 */
package tipl.spark;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;
import tipl.formats.TImgRO;
import tipl.spark.NeighborhoodPlugin.GatherBasedPlugin;
import tipl.tests.TestPosFunctions;
import tipl.tools.BaseTIPLPluginIn;
import tipl.tools.BaseTIPLPluginIn.filterKernel;
import tipl.tools.BaseTIPLPluginIn.morphKernel;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.D4int;
import tipl.util.ITIPLPluginIn;
import tipl.util.TImgBlock;
import tipl.util.TImgTools;

/**
 * CL performs iterative component labeling using a very simple label and merge algorithm
 * @author mader
 *
 */
public class CL extends GatherBasedPlugin<boolean[],int[]> {
	protected static void checkVals(final ITIPLPluginIn CL, final int maxLabel,
			final double avgCount) {
		/** System.out.println("Maximum Label of CL Image:" + getMax(CL)
				+ ", average count:" + getAvg(CL));
		assertEquals(getMax(CL), maxLabel);
		assertEquals(getAvg(CL), avgCount, 0.1);
		 **/
	}
	public static void main(String[] args) {
		
		System.out.println("Testing runVoxelsIntInt");
		ArgumentParser p=SparkGlobal.activeParser(args);
		final TImgRO testImg = TestPosFunctions.wrapIt(100,
				new TestPosFunctions.DiagonalPlaneFunction());
		
		
		final DTImg<boolean[]> testDImg=DTImg.<boolean[]>ConvertTImg(SparkGlobal.getContext(), testImg, TImgTools.IMAGETYPE_BOOL);
		CL curPlugin=new CL(testDImg);
		
		curPlugin.execute();
		
		
		//checkVals(curPlugin, 1, 10);

	}
	public CL(final DTImg<boolean[]> inImage) {
		maskImg=inImage;
	}
	final protected DTImg<boolean[]> maskImg;
	protected DTImg<long[]> labelImg;
	@Override
	public Tuple2<D3int, TImgBlock<int[]>> GatherBlocks(
			Tuple2<D3int, List<TImgBlock<boolean[]>>> inBlocks) {
		// TODO Auto-generated method stub
		return null;
	}
	/** 
	 * Get a summary of the groups and the count in each
	 * @param inLabelImg
	 * @return
	 */
	protected static Map<Long,Long> groupCount(DTImg<long[]> inLabelImg) {
		final D3int wholeSize=inLabelImg.getDim();
		final Map<Long,Long> grpCount= inLabelImg.getBaseImg().values().map(
				new Function<TImgBlock<long[]>,Map<Long,Long>>() {

					@Override
					public Map<Long,Long> call(
							TImgBlock<long[]> inBlock) throws Exception {
						final long[] cSlice=inBlock.get();
						Map<Long,Long> outMapping=new HashMap<Long,Long>();
						final Long one=new Long(1);
						for(long ival : cSlice) {
							if (ival>0) {
								final Long cKey=new Long(ival);
								if (outMapping.containsKey(cKey)) outMapping.put(cKey, outMapping.get(cKey)+one);
								else outMapping.put(cKey, one);
							}
						}
						return outMapping;
					}

				}).reduce(new Function2<Map<Long,Long>,Map<Long,Long>,Map<Long,Long>>() {

					@Override
					public Map<Long, Long> call(Map<Long, Long> arg0,
							Map<Long, Long> arg1) throws Exception {
						
						return joinMap(arg0,arg1,LongAdder);
					}
					
				});
		
		return grpCount;
				
	}
	/**
	 * A simple interface for joining two elements together 
	 * @author mader
	 *
	 * @param <Si>
	 */
	public static interface canJoin <Si> extends Serializable {
		public Si join(Si a, Si b);
	}
	protected static canJoin<Long> LongAdder = new canJoin<Long>() {
		@Override
		public Long join(Long a, Long b) {
			return a+b;
		}
	};
	/**
	 * A utility function for joining together two maps where values from the same key are added
	 * @param mapA
	 * @param mapB
	 * @return a joined map where the overlapping elements have been joined using the canJoin interface
	 */
	protected static <Sf,Sc> Map<Sf,Sc>  joinMap(Map<Sf,Sc> mapA, Map<Sf,Sc> mapB,canJoin<Sc> joinTool) {
		Map<Sf,Sc> joinMap=new HashMap<Sf,Sc>(mapA.size()+mapB.size());
		joinMap.putAll(mapA);
		for(Entry<Sf,Sc> cElement : mapB.entrySet()) {
			Sf cKey=cElement.getKey();
			if (joinMap.containsKey(cKey)) joinMap.put(cKey, 
					joinTool.join(cElement.getValue(), joinMap.get(cKey) ));
			else joinMap.put(cKey, cElement.getValue());
		}
		return joinMap;
	}
	/**
	 * Generates a label image using the index of each voxel (easy to keep concurrent)
	 * @param inMaskImg the binary input image
	 * @return labeled image as long[]
	 */
	protected static DTImg<long[]> makeLabelImage(DTImg<boolean[]> inMaskImg) {
		final D3int wholeSize=inMaskImg.getDim();
		// give every voxel a unique label as a long
		return inMaskImg.map(
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
									if (cSlice[off]) {
										// the initial label is just the index of the voxel in the whole image
										long label=((z+spos.z)*wholeSize.y+(y+spos.y))*wholeSize.x+x+spos.x;
										oSlice[off]=label;
									}
								}
							}
						}
						return  new Tuple2<D3int, TImgBlock<long[]>>(arg0._1,
								new TImgBlock<long[]>(oSlice,inBlock.getPos(),inBlock.getDim()));
					}

				}, TImgTools.IMAGETYPE_LONG);
	}
	/**
	 * Effectively a map but it uses only primitives instead of objects so it doesn't really implement the interface
	 * @author mader
	 *
	 */
	protected static class OmnidirectionalMap implements Serializable {
		protected Set<long[]> mapElements;
		public static final List<Long> emptyList=new ArrayList<Long>(0);
		public OmnidirectionalMap(int guessLength) {
			mapElements=new HashSet<long[]>(guessLength);
		}
		/** for presorted elements
		 * @param ele
		 */
		protected void add(long[] ele) {
			if (!mapElements.contains(ele)) mapElements.add(ele);
		}
		public void put(long valA,long valB) {
			long[] curEle;
			if (valA>valB) curEle=new long[] {valB,valA};
			else curEle=new long[] {valA,valB};
			add(curEle);
		}
		public int size() {
			return mapElements.size();
		}
		public boolean isEmpty() {
			return (this.size()==0);
		}
		public boolean containsKey(long key) {
			for(long[] curKey : mapElements)  {
				if (key==curKey[0]) return true;
				else if (key==curKey[1]) return true;
			}
			return false;
		}
		/**
		 * get all of the keys in the list
		 * @return
		 */
		public Set<Long> getKeys() {
			Set<Long> outList=new HashSet<Long>();
			for(long[] curKey : mapElements)  {
				outList.add(curKey[0]);
				outList.add(curKey[1]);
			}
			return outList;
		}
		public Set<Long> get(long key) {
			Set<Long> outList=new HashSet<Long>();
			for(long[] curKey : mapElements)  {
				if (key==curKey[0]) outList.add(curKey[1]);
				else if (key==curKey[1]) outList.add(curKey[0]);
			}
			return outList;
		}
		/**
		 * A recursive get command which gets all the neighbors of the neighbors ... until the list stops growing
		 * @param key
		 * @return
		 */
		public Set<Long> rget(long key) {
			final Set<Long> firstSet=get(key); // this should not be modified
			Set<Long> outSet=new HashSet<Long>(firstSet.size());
			
			outSet.addAll(get(key));
			int lastlen=0;
			while(outSet.size()>lastlen) {
				lastlen=outSet.size();
				// need to create a temp set since you cant add items to a set which is being iterated over
				final Set<Long> outSetTemp=new HashSet<Long>(outSet.size());
				outSetTemp.addAll(outSet);
				for(Long e : outSet) outSetTemp.addAll(get(e));
				outSet=outSetTemp;
			}
			outSet.add(key);
			return outSet;
		}
		
		protected Set<long[]> getAsSet() {return mapElements;}
		/**
		 * merge two sets together
		 * @param map2
		 */
		public void coalesce(OmnidirectionalMap map2) {
			for(long[] curEle : map2.getAsSet()) this.add(curEle);
		}

	}
	protected static Map<Long,Long> scanForNewGroups(DTImg<long[]> labeledImage,D3int neighborSize,BaseTIPLPluginIn.morphKernel mKernel) {
		JavaRDD<OmnidirectionalMap> connectedGroups=labeledImage.spreadSlices(neighborSize.z).
				groupByKey().partitionBy(SparkGlobal.getPartitioner(labeledImage.getDim())).
				map(new GetConnectedComponents(mKernel,neighborSize));
		OmnidirectionalMap groupList=connectedGroups.reduce(new Function2<OmnidirectionalMap,OmnidirectionalMap,OmnidirectionalMap>() {

			@Override
			public OmnidirectionalMap call(final OmnidirectionalMap arg0,
					final OmnidirectionalMap arg1) throws Exception {
				arg0.coalesce(arg1);
				return arg0;
			}
			
		});
		// ugly coalescence and boilerplate code
		final Set<Long> groups=groupList.getKeys();
		final Set<Set<Long>> groupGroups=new HashSet<Set<Long>>(groups.size());
		for(Long curKey : groups) {
			// check all the old sets first
			for(Set<Long> oldSet : groupGroups) if (oldSet.contains(curKey)) break;
			Set<Long> cList=groupList.rget(curKey);
			groupGroups.add(cList);
		}
		// now reduce the results to long[] since they are smaller
		
		final Map<Long,Long> mergeCommands=new PassthroughHashMap(groupGroups.size()*2);
		for(final Set<Long> curSet : groupGroups) {
			final Long mapToVal=Collections.min(curSet);
			for (final Long cKey : curSet) {
				mergeCommands.put(cKey,mapToVal);
			}
		}
		return mergeCommands;
	}
	/**
	 * a hashmap which returns the input value when it is missing from the map and 0 when 0 is given as an input
	 * @author mader
	 *
	 */
	protected static class PassthroughHashMap extends HashMap<Long,Long> implements Serializable {
		public PassthroughHashMap(int guessSize) {
			super(guessSize);
		}
		protected static final Long zero=new Long(0);
		@Override
		public Long get(Object objVal) {
			final Long eVal=(Long) objVal;
			if (eVal==zero) return zero;
			if (this.containsKey(eVal)) return super.get(objVal);
			else return eVal;
		}
	}
	
	protected static DTImg<long[]> mergeGroups(DTImg<long[]> labeledImage,final Map<Long,Long> mergeCommands) {
		SparkGlobal.getContext().broadcast(mergeCommands);
		DTImg<long[]> newlabeledImage=labeledImage.map(new PairFunction<Tuple2<D3int,TImgBlock<long[]>>,D3int,TImgBlock<long[]>>() {

			@Override
			public Tuple2<D3int, TImgBlock<long[]>> call(
					Tuple2<D3int, TImgBlock<long[]>> arg0) throws Exception {
				final long[] curSlice=arg0._2.get();
				final long[] outSlice=new long[curSlice.length];
				for(int i=0;i<curSlice.length;i++) {
					outSlice[i]=mergeCommands.get(curSlice[i]);
				}
				return new Tuple2<D3int,TImgBlock<long[]>>(arg0._1,new TImgBlock<long[]>(outSlice,arg0._2));
			}
			
		}, TImgTools.IMAGETYPE_LONG);
		
		return newlabeledImage;
	}
	

	static public class GetConnectedComponents extends Function<Tuple2<D3int, List<TImgBlock<long[]>>>,OmnidirectionalMap> {
		final protected BaseTIPLPluginIn.morphKernel mKernel;
		final protected D3int ns;
		public GetConnectedComponents(final BaseTIPLPluginIn.morphKernel mKernel,final D3int ns) {
			this.mKernel=mKernel;
			this.ns=ns;
		}
		
		public D3int getNeighborSize() {return ns;}
		public BaseTIPLPluginIn.morphKernel getMKernel() {return mKernel;}
		@Override
		public OmnidirectionalMap call(Tuple2<D3int, List<TImgBlock<long[]>>> inTuple) {
			final D3int ns = getNeighborSize();
			
			final List<TImgBlock<long[]>> inBlocks = inTuple._2();
			final TImgBlock<long[]> templateBlock = inBlocks.get(0);
			final D3int blockSize = templateBlock.getDim();
			final BaseTIPLPluginIn.morphKernel mKernel = getMKernel();
			final int eleCount = (int) templateBlock.getDim().prod();
			
			// for every item in the offset==0 block, calculate a list of touching components
			final Set<Long>[] neighborList = new Set[eleCount];
			for(int i=0;i<eleCount;i++) neighborList[i]=new HashSet<Long>();
			// the output image
			for (final TImgBlock<long[]> cBlock : inBlocks) {
				final long[] curBlock = cBlock.get();
				for (int zp = 0; zp < templateBlock.getDim().z; zp++) {
					for (int yp = 0; yp < templateBlock.getDim().y; yp++) {
						for (int xp = 0; xp < templateBlock.getDim().x; xp++) {
							final int off = ((zp) * blockSize.y + (yp))
									* blockSize.x + (xp);
							
							for(D4int cPos : getScanPositions(mKernel,new D3int(xp,yp,zp),cBlock.getOffset(),off, blockSize,ns)) {
								final long cval=curBlock[cPos.offset];
								if (cval>0) neighborList[off].add(new Long(cval));
							}
						}
					}
				}
			}
			OmnidirectionalMap pairs=new OmnidirectionalMap(eleCount);
			final Long[] emptyLongArray=new Long[1];
			List<Long[]> pairList=new ArrayList<Long[]>();
			for(int i=0;i<eleCount;i++) {
				if (neighborList[i].size()>1) {
					for(Long valA : neighborList[i]) {
						for(Long valB : neighborList[i]) {
							if (valA!=valB) pairs.put(valA, valB);
						}
					}
				}
			}
			return pairs;
		}
	}
	
	
	@Override
	public boolean execute() {
		labelImg=makeLabelImage(this.maskImg);
		
		boolean stillMerges=true;
		int i=0;
		while(stillMerges) {
			String curGrpSummary="";
			Map<Long,Long> cGrp=groupCount(labelImg);
			for(Entry<Long,Long> cEntry : cGrp.entrySet()) {
				curGrpSummary+=cEntry.getKey()+"\t"+cEntry.getValue()+"\n";
				if (curGrpSummary.length()>50) break;
			}
			Map<Long,Long> curMap=scanForNewGroups(labelImg, getNeighborSize(), getMKernel());
			String curMapSummary="";
			for(Entry<Long,Long> cEntry : curMap.entrySet()) {
				curMapSummary+=cEntry.getKey()+"=>"+cEntry.getValue()+",";
				if (curMapSummary.length()>50) break;
			}
			System.out.println("Iter: "+i+"\n"+curGrpSummary+"\tMerges "+curMap.size()+":\n\t"+curMapSummary);
			
			if (curMap.size()>0) {
				labelImg=mergeGroups(labelImg, curMap);
			} else {
				stillMerges=false;
			}
			
			i++;
			
		}
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
		//return BaseTIPLPluginIn.gaussFilter(1.0);
		throw new IllegalArgumentException(this+" does not have a kernel to get");
	}

	@Override
	public morphKernel getMKernel() {
		// TODO Auto-generated method stub
		return BaseTIPLPluginIn.fullKernel;
	}

	@Override
	public D3int getNeighborSize() {
		// TODO Auto-generated method stub
		return new D3int(1,1,1);
	}

	@Override
	public String getPluginName() {
		// TODO Auto-generated method stub
		return null;
	}

}
