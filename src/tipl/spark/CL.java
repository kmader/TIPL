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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.spark.Accumulator;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;

import scala.Tuple2;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sort;
import static ch.lambdaj.Lambda.DESCENDING;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.TImgRO.CanExport;
import tipl.spark.NeighborhoodPlugin.GatherBasedPlugin;
import tipl.tests.TestPosFunctions;
import tipl.tools.BaseTIPLPlugin;
import tipl.tools.BaseTIPLPluginIO;
import tipl.tools.BaseTIPLPluginIn;
import tipl.tools.ComponentLabel;
import tipl.tools.BaseTIPLPluginIn.filterKernel;
import tipl.tools.BaseTIPLPluginIn.morphKernel;
import tipl.tools.ComponentLabel.CLFilter;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.D4int;
import tipl.util.ITIPLPlugin;
import tipl.util.ITIPLPluginIn;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgBlock;
import tipl.util.TImgTools;

/**
 * CL performs iterative component labeling using a very simple label and merge algorithm
 * @author mader
 *
 */
@SuppressWarnings("serial")
public class CL extends BaseTIPLPluginIO {//extends GatherBasedPlugin<boolean[],int[]> {
	@TIPLPluginManager.PluginInfo(pluginType = "ComponentLabel",
			desc="Spark-based component labeling",
			sliceBased=false,
			maximumSize=-1,
			bytesPerVoxel=3,
			sparkBased=true)
	final public static TIPLPluginManager.TIPLPluginFactory myFactory = new TIPLPluginManager.TIPLPluginFactory() {
		@Override
		public ITIPLPlugin get() {
			return new CL();
		}
	};
	
	protected boolean runSliceMergesFirst=true;
	@Override
	public ArgumentParser setParameter(ArgumentParser p, String prefix) {
		runSliceMergesFirst=p.getOptionBoolean("slicemerging", runSliceMergesFirst,"Run slice merges before entire image merges, faster for very large data sets");
		return super.setParameter(p, prefix);
	}


	@Override
	public String getPluginName() {
		return "Spark-ComponentLabel";
	}
	
	protected DTImg<boolean[]> maskImg;
	protected DTImg<long[]> labelImg;
	@Override
	public void LoadImages(TImgRO[] inImages) {
		assert(inImages.length>0);
		
		TImgRO inImage=inImages[0];
		if (inImage instanceof DTImg<?> & inImage.getImageType()==TImgTools.IMAGETYPE_BOOL)
			maskImg = (DTImg<boolean[]>) inImage;
		else
			maskImg=DTImg.<boolean[]>ConvertTImg(SparkGlobal.getContext(getPluginName()+"Context"), inImage, TImgTools.IMAGETYPE_BOOL);
		
	}
	
	public static void main(String[] args) {
		
		System.out.println("Testing Component Label Code");
		ArgumentParser p=SparkGlobal.activeParser(args);
		
		testFunc(p);

	}
	
	public static void testFunc(ArgumentParser p) {
		int boxSize=p.getOptionInt("boxsize", 8, "The dimension of the image used for the analysis");
		int layerWidth=p.getOptionInt("width", boxSize/4, "The width of the layer used for the analysis");
		String writeIt=p.getOptionPath("out", "", "write image as output file");
		
		final TImgRO testImg = TestPosFunctions.wrapIt(boxSize,
				new TestPosFunctions.SphericalLayeredImage(boxSize/2, boxSize/2, boxSize/2, 0, 1, layerWidth));
			//	new TestPosFunctions.DiagonalPlaneFunction());
		if (writeIt.length()>0) TImgTools.WriteTImg(testImg,writeIt);
		
		CL curPlugin=new CL();
		
		curPlugin.setParameter(p,"");
		p.checkForInvalid();
		
		curPlugin.LoadImages(new TImgRO[] {testImg});
		
		
		curPlugin.execute();
		
	}
	public CL() {
	}
	
	protected ComponentLabel.CLFilter objFilter;
	/**
	 * run and return only the largest component
	 */
	public void runFirstComponent() {
		objFilter = new ComponentLabel.CLFilter() {
			int maxComp = -1;
			int maxVol = -1;

			@Override
			public boolean accept(final int labelNumber, final int voxCount) {
				return (labelNumber == maxComp);
			}

			@Override
			public String getProcLog() {
				return "Using largest component filter\n";
			}

			@Override
			public void prescan(final int labelNumber, final int voxCount) {
				if ((voxCount > maxVol) | (maxComp == -1)) {
					maxComp = labelNumber;
					maxVol = voxCount;
				}
			};
		};
		execute();
	}

	/** 
	 * Get a summary of the groups and the count in each
	 * @param inLabelImg
	 * @return
	 */
	protected static Map<Long,Long> groupCount(DTImg<long[]> inLabelImg) {
		final D3int wholeSize=inLabelImg.getDim();
		return inLabelImg.getBaseImg().values().
				flatMap(new FlatMapFunction<TImgBlock<long[]>,Long>() {
					
					@Override
					public Iterable<Long> call(
							TImgBlock<long[]> inBlock) throws Exception {
						final long[] cSlice=inBlock.get();
						final Long one=new Long(1);
						List<Long> outData=new LinkedList<Long>();
						for(long ival : cSlice) {
							if (ival>0) {
								final Long cKey=new Long(ival);
								outData.add(cKey);
						}
					}
					return outData;
					}

			
		}).countByValue();
				
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
	protected static DTImg<long[]> makeLabelImage(DTImg<boolean[]> inMaskImg,final D3int ns,final  BaseTIPLPluginIn.morphKernel mKernel) {
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
						final D3int gOffset=new D3int(0,0,0);
						final long[] oSlice=new long[cSlice.length];
						for(int z=0;z<sdim.z;z++) {
							for(int y=0;y<sdim.y;y++) {
								for(int x=0;x<sdim.x;x++) {
									int off=(int) (z*sdim.y+y)*sdim.x+x;
									if (cSlice[off]) {
										long label;
										// the default label is just the index of the voxel in the whole image
										if (oSlice[off]==0) label=((z+spos.z)*wholeSize.y+(y+spos.y))*wholeSize.x+x+spos.x;
										else label=oSlice[off];
										oSlice[off]=label;
										for (D4int scanPos : BaseTIPLPluginIn.getScanPositions(mKernel,new D3int(x,y,z),gOffset, off, sdim, ns)) {
											if(cSlice[scanPos.offset]) oSlice[scanPos.offset]=label;
										}
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
		/**
		 * The maximum number of iterations for the recursive get command (-1 is no-limit)
		 */
		public static int RGET_MAX_ITERS=2; 
		public OmnidirectionalMap(int guessLength) {
			
			mapElements=new LinkedHashSet<long[]>(guessLength);
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
			int rgetCount=0;
			while(outSet.size()>lastlen) {
				lastlen=outSet.size();
				// need to create a temp set since you cant add items to a set which is being iterated over
				final Set<Long> outSetTemp=new HashSet<Long>(outSet.size());
				outSetTemp.addAll(outSet);
				for(Long e : outSet) outSetTemp.addAll(get(e));
				outSet=outSetTemp;
				rgetCount++;
				if((RGET_MAX_ITERS>0) & (rgetCount>RGET_MAX_ITERS)) break;
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
	/**
	 * Looks for all the connections in an image by scanning neighbors
	 * @param labeledImage
	 * @param neighborSize
	 * @param mKernel
	 * @param fullIteration
	 * @return
	 */
	protected static JavaPairRDD<D3int,OmnidirectionalMap> slicesToConnections(DTImg<long[]> labeledImage,D3int neighborSize,BaseTIPLPluginIn.morphKernel mKernel,boolean fullIteration,final TimingObject inTO) {
		JavaPairRDD<D3int,List<TImgBlock<long[]>>> fannedImage;
		if (fullIteration) 
			fannedImage=labeledImage.spreadSlices(neighborSize.z).
			groupByKey().partitionBy(SparkGlobal.getPartitioner(labeledImage.getDim()));
		else 
			fannedImage=labeledImage.getBaseImg().groupByKey();
		
		/**
		 * the code was changed to store everything slice-wise so it could potentially be transported again that way for half-iterations
		 * this is not yet implemented
		 */
		GetConnectedComponents gccObj=new GetConnectedComponents(inTO,mKernel,neighborSize);
		JavaPairRDD<D3int,OmnidirectionalMap> outComponents=fannedImage.map(gccObj);
		return outComponents;
	}
	/**
	 * Searches for new groups in the images by scanning all neighbors
	 * @param labeledImage
	 * @param neighborSize
	 * @param mKernel
	 * @param fullIteration perform the fanning out (or not)
	 * @return
	 */
	protected static Map<Long,Long> scanForNewGroups(DTImg<long[]> labeledImage,D3int neighborSize,BaseTIPLPluginIn.morphKernel mKernel, final TimingObject inTO) {
		JavaPairRDD<D3int,OmnidirectionalMap> connectedGroups = slicesToConnections(labeledImage,neighborSize,mKernel,false,inTO);
		
		OmnidirectionalMap groupList=connectedGroups.values().reduce(new Function2<OmnidirectionalMap,OmnidirectionalMap,OmnidirectionalMap>() {

			@Override
			public OmnidirectionalMap call(final OmnidirectionalMap arg0,
					final OmnidirectionalMap arg1) throws Exception {
				arg0.coalesce(arg1);
				return arg0;
			}
		
		});
		return groupListToMerges(groupList);
		
	}
	/** 
	 * A simple command to run the slice based component labeling and connecting on each slice first
	 * @param labeledImage
	 * @param neighborSize
	 * @param mKernel
	 * @return
	 */
	protected static Tuple2<DTImg<long[]>,Long> scanAndMerge(DTImg<long[]> labeledImage,D3int neighborSize,BaseTIPLPluginIn.morphKernel mKernel,final TimingObject inTO) {
		JavaPairRDD<D3int,OmnidirectionalMap> connectedGroups = slicesToConnections(labeledImage,neighborSize,mKernel,false,inTO);
		
		JavaPairRDD<D3int,Map<Long,Long>> mergeCmds=connectedGroups.mapValues(new Function<OmnidirectionalMap,Map<Long,Long>>() {
			@Override
			public Map<Long, Long> call(OmnidirectionalMap arg0)
					throws Exception {
				final long start = System.currentTimeMillis();
				Map<Long, Long> outList=groupListToMerges(arg0);
				inTO.timeElapsed.$plus$eq(new Double(System.currentTimeMillis() - start));
				inTO.mapOperations.$plus$eq(1);
				return outList;
			}
			
		});
		
		System.out.println("Merges per slice");
		
		long totalMerges=0;
		for (Long cVal: mergeCmds.values().map(new Function<Map<Long,Long>,Long>() {

			@Override
			public Long call(Map<Long, Long> arg0) throws Exception {
				return new Long(arg0.size());
			}
			
		}).collect()) {
			System.out.println("\t"+cVal);
			totalMerges+=cVal;
		}
		
		JavaPairRDD<D3int,TImgBlock<long[]>> newlabeledImage=labeledImage.getBaseImg().
				join(mergeCmds, SparkGlobal.getPartitioner(labeledImage.getDim()))
				.mapValues(new Function<Tuple2<TImgBlock<long[]>,Map<Long,Long>>,TImgBlock<long[]>>()
						{

			@Override
			public TImgBlock<long[]> call(
					Tuple2<TImgBlock<long[]>,Map<Long,Long>> inTuple) throws Exception {
				final long start=System.currentTimeMillis();
				final TImgBlock<long[]> cBlock=inTuple._1;
				
				final long[] curSlice=cBlock.get();
				final long[] outSlice=new long[curSlice.length];
				final Map<Long,Long> mergeCommands=inTuple._2;
				
				for(int i=0;i<curSlice.length;i++) {
					if (curSlice[i]>0) outSlice[i]=mergeCommands.get(curSlice[i]);
				}
				inTO.mapOperations.$plus$eq(1);
				inTO.timeElapsed.$plus$eq(new Double(System.currentTimeMillis()-start));
				return new TImgBlock<long[]>(outSlice,cBlock);
			}
			
		});
		return 
				new Tuple2<DTImg<long[]>,Long> (
						DTImg.<long[]>WrapRDD(labeledImage,newlabeledImage, TImgTools.IMAGETYPE_LONG),
						new Long(totalMerges));
		
		
		
	}
	
	/**
	 * A rather ugly function to turn the neighbor lists into specific succinct merge commands
	 * @param groupList
	 * @return
	 */
	protected static Map<Long,Long> groupListToMerges(OmnidirectionalMap groupList) {
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
	
	protected static DTImg<long[]> mergeGroups(DTImg<long[]> labeledImage,final Map<Long,Long> mergeCommands,final TimingObject inTO) {
		final Broadcast<Map<Long,Long>> broadcastMergeCommands=SparkGlobal.getContext().broadcast(mergeCommands);
		DTImg<long[]> newlabeledImage=labeledImage.map(new PairFunction<Tuple2<D3int,TImgBlock<long[]>>,D3int,TImgBlock<long[]>>() {
			final Map<Long,Long> cMergeCommands=broadcastMergeCommands.value();
			@Override
			public Tuple2<D3int, TImgBlock<long[]>> call(
					Tuple2<D3int, TImgBlock<long[]>> arg0) throws Exception {
				final long start=System.currentTimeMillis();
				final long[] curSlice=arg0._2.get();
				final long[] outSlice=new long[curSlice.length];
				for(int i=0;i<curSlice.length;i++) {
					outSlice[i]=cMergeCommands.get(curSlice[i]);
				}
				inTO.timeElapsed.$plus$eq(new Double(System.currentTimeMillis() - start));
				inTO.mapOperations.$plus$eq(1);
				return new Tuple2<D3int,TImgBlock<long[]>>(arg0._1,new TImgBlock<long[]>(outSlice,arg0._2));
			}
			
		}, TImgTools.IMAGETYPE_LONG);
		
		return newlabeledImage;
	}
	protected static class TimingObject implements Serializable {

		public final Accumulator<Double> timeElapsed;
		public final Accumulator<Integer> mapOperations;
		public TimingObject(final JavaSparkContext jsc) {
			timeElapsed=jsc.accumulator(new Double(0));
			mapOperations=jsc.accumulator(new Integer(0));
		}
	}
	/**
	 * The function which actually finds connected components in each block and returns
	 * them as a list
	 * @author mader
	 *
	 */
	static public class GetConnectedComponents extends PairFunction<Tuple2<D3int, List<TImgBlock<long[]>>>,D3int,OmnidirectionalMap> {
		final protected BaseTIPLPluginIn.morphKernel mKernel;
		final protected D3int ns;
		public final TimingObject to;
		public GetConnectedComponents(final TimingObject inTO,final BaseTIPLPluginIn.morphKernel mKernel,final D3int ns) {
			this.mKernel=mKernel;
			this.ns=ns;
			this.to=inTO;
		}
		
		public D3int getNeighborSize() {return ns;}
		
		public BaseTIPLPluginIn.morphKernel getMKernel() {return mKernel;}
		
		@Override
		public Tuple2<D3int,OmnidirectionalMap> call(Tuple2<D3int, List<TImgBlock<long[]>>> inTuple) {
			final long start=System.currentTimeMillis();
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
							
							for(D4int cPos : BaseTIPLPluginIn.getScanPositions(mKernel,new D3int(xp,yp,zp),cBlock.getOffset(),off, blockSize,ns)) {
								final long cval=curBlock[cPos.offset];
								if (cval>0) neighborList[off].add(new Long(cval));
							}
						}
					}
				}
			}
			
			OmnidirectionalMap pairs=new OmnidirectionalMap(2*eleCount);
			final Long[] emptyLongArray=new Long[1];
			List<Long[]> pairList=new ArrayList<Long[]>();
			for(int i=0;i<eleCount;i++) {
				if (neighborList[i].size()>2) {
					Long[] iterArray=neighborList[i].toArray(new Long[neighborList[i].size()]);
					for(int ax=0;ax<(iterArray.length-1);ax++) {
						for(int bx=ax+1;bx<iterArray.length;bx++) {
							pairs.put(iterArray[ax], iterArray[bx]);
						}
					}
				}
			}
			to.timeElapsed.$plus$eq(new Double(System.currentTimeMillis()-start));
			to.mapOperations.$plus$eq(1);
			return new Tuple2<D3int,OmnidirectionalMap>(inTuple._1(),pairs);
		}
	}
	

	
	@Override
	public boolean execute() {
		final long start=System.currentTimeMillis();
		final TimingObject to=new TimingObject(this.maskImg.getContext());
		labelImg=makeLabelImage(this.maskImg,getNeighborSize(),getKernel());
		
		boolean stillMerges=runSliceMergesFirst;
		int i=0;
		/** runs for slice based iterations for awhile and then move to whole image iterations once
		 * the slice based one flatten out.
		 */
		while(stillMerges) {
			Tuple2<DTImg<long[]>,Long> smOutput=scanAndMerge(labelImg,getNeighborSize(), getKernel(),to);
			labelImg=smOutput._1;
			System.out.println("Iter: "+i+", Full:"+(false)	+"\n\tMerges "+smOutput._2);
			stillMerges=(smOutput._2>0);
			i++;
		}
		
		stillMerges=true;
		/** 
		 * round 2 process the whole image
		 * iteratively merge these groups until no more merges remain
		 */
		while(stillMerges) {
			String curGrpSummary="";
			Map<Long,Long> cGrp=groupCount(labelImg);
			for(Entry<Long,Long> cEntry : cGrp.entrySet()) {
				curGrpSummary+=cEntry.getKey()+"\t"+cEntry.getValue()+"\n";
				if (curGrpSummary.length()>50) break;
			}
			Map<Long,Long> curMap=scanForNewGroups(labelImg, getNeighborSize(), getKernel(),to);
			String curMapSummary="";
			for(Entry<Long,Long> cEntry : curMap.entrySet()) {
				curMapSummary+=cEntry.getKey()+"=>"+cEntry.getValue()+",";
				if (curMapSummary.length()>50) break;
			}
			System.out.println("Iter: "+i+", Full:"+(true)	+"\n"+curGrpSummary+"\tMerges "+curMap.size()+": Groups:"+cGrp.size()+"\n\t"+curMapSummary);
			
			if (curMap.size()>0) {
				labelImg=mergeGroups(labelImg, curMap,to);
			} else {
				stillMerges=false;
			}
			
			i++;
			
		}
		
		// now sort by voxel count
		Map<Long,Long> cGrp=groupCount(labelImg);
		String curGrpSummary="";
		
		List<Entry<Long,Long>> finalMap=sort(cGrp.entrySet(),on(Map.Entry.class).getValue(),DESCENDING);
		Map<Long,Long> reArrangement=new HashMap<Long,Long>(finalMap.size());
		int outDex=1;
		for(Entry<Long,Long> cEntry : finalMap) {
			if (curGrpSummary.length()<100) curGrpSummary+=cEntry.getKey()+"=>"+outDex+"\t"+cEntry.getValue()+" voxels\n";;
			reArrangement.put(cEntry.getKey(), new Long(outDex));
			outDex++;
		}
		System.out.println("Final List:\n"+curGrpSummary+"\n Total Elements:\t"+reArrangement.size());
		labelImg=mergeGroups(labelImg, reArrangement,to);
		
		final long runTime=System.currentTimeMillis()-start;
		double mapTime=0;
		long mapOps=0;
		mapTime=to.timeElapsed.value();
		mapOps=to.mapOperations.value();
		
		System.out.println("CSV_OUT,"+SparkGlobal.getMasterName()+","+reArrangement.size()+","+
		labelImg.getDim().x+","+labelImg.getDim().y+","+labelImg.getDim().z+","+mapTime+","+
				runTime+","+mapOps+","+SparkGlobal.maxCores+	","+SparkGlobal.getSparkPersistenceValue()+","+SparkGlobal.useCompression);
		
		return true;
	}


	@Override
	public TImg ExportAim(CanExport templateAim) {
		// TODO Auto-generated method stub
		return null;
	}
	


}
