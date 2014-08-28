/**
 *
 */
package tipl.spark;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.*;
import org.apache.spark.storage.StorageLevel;

import scala.Tuple2;
import scala.Tuple3;
import tipl.formats.FImage;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.TSliceWriter;
import tipl.formats.TImg.ArrayBackedTImg;
import tipl.util.*;
import tipl.util.ArgumentList.TypedPath;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A distributed TImg based on a JavaRDD class and several subclass types I
 * intentionally used different variables for the generics to keep them straight
 * from each other particularly with the static functions and classes within
 * this method. TODO This should probably be made into it's own RDD type, but
 * right now I think this is easier
 *
 * @author mader
 */
@SuppressWarnings("serial")
public class DTImg<T> extends TImg.ATImg implements TImg, Serializable {
	/**
	 * if future is utilized to reduce the load initially (causes problems with iterative loops)
	 */
    static protected final boolean futureTImgMigrate = false;
    /**
     *
     */
    private static final long serialVersionUID = -1824695496632428954L;
    final ArgumentList.TypedPath path;
    /**
     * should be final but sometimes it changes *
     */
    protected JavaPairRDD<D3int, TImgBlock<T>> baseImg;
    protected String procLog = "";


    /**
     * Everything is much easier with one unified constructor and then several factories which call it
     *
     * @param parent
     * @param newImage
     * @param imgType
     * @param path
     */
    protected DTImg(TImgTools.HasDimensions parent, JavaPairRDD<D3int, TImgBlock<T>> newImage,
                    int imgType, ArgumentList.TypedPath path) {
        super(parent,imgType);
    	this.baseImg = newImage;//.partitionBy(SparkGlobal.getPartitioner(getDim()));
        TImgTools.mirrorImage(parent, this);
        this.path = path;
        SparkGlobal.assertPersistance(this);
        int sliceCount = (int) baseImg.count();
        if (getDim().z != sliceCount) dim.z = sliceCount;

    }


    public static void main(String[] args) {

        System.out.println("DTImage Convertor");
        ArgumentParser p = SparkGlobal.activeParser(args);
        boolean writeAsTextFile = p.getOptionBoolean("astext", true, "Write the output as multiple text files");
        final ArgumentList.TypedPath imagePath = p.getOptionPath("input", "/Users/mader/Dropbox/TIPL/test/io_tests/rec8tiff", "Path of image (or directory) to read in");
        ArgumentList.TypedPath writeIt = p.getOptionPath("output", imagePath + "-txt", "write image as output file");
        p.checkForInvalid();

        JavaSparkContext jsc = SparkGlobal.getContext("DTImg-Tool");
        DTImg<int[]> cImg = DTImg.ReadImage(jsc, imagePath, TImgTools.IMAGETYPE_INT);
        if (writeAsTextFile) if (writeIt.length() > 0) cImg.HSave(writeIt.getPath());

    }

    /**
     * import an image from a path as a future by reading line by line, useful when many will be filtered out anyways
     *
     * @param jsc
     * @param imgName
     * @param imgType
     * @return
     */
    protected static <U> JavaPairRDD<D3int, TImgBlock<U>> ImportImage(
            final JavaSparkContext jsc, final ArgumentList.TypedPath imgName, final int imgType) {
        assert (TImgTools.isValidType(imgType));
        final TImgRO cImg = TImgTools.ReadTImg(imgName, false, true);
        final D3int imgDim = cImg.getDim();

        final List<Integer> l = new ArrayList<Integer>(imgDim.z);
        for (int i = 0; i < imgDim.z; i++) {
            l.add(i);
        }
        /**
         * performance is much better when partition count matches slice count
         * (or is at least larger than 2)
         **/
        final int partitionCount = SparkGlobal.calculatePartitions(cImg.getDim().z);
        return jsc.parallelize(l, partitionCount)
                .mapToPair(new ReadSlicePromise<U>(imgName, imgType, cImg.getPos(), cImg
                        .getDim()));
    }

    /**
     * import an image from a path by reading in chunks based on the maximum number of cores
     *
     * @param jsc
     * @param imgName
     * @param imgType
     * @return
     */
    protected static <U> JavaPairRDD<D3int, TImgBlock<U>> ImportImageSerial(
            final JavaSparkContext jsc, final ArgumentList.TypedPath imgName, final int imgType) {
        assert (TImgTools.isValidType(imgType));
        final TImgRO cImg = TImgTools.ReadTImg(imgName, false, true);
        final D3int imgDim = cImg.getDim();
        final int partitionCount = TIPLGlobal.getMaximumReaders();
        final int slicesPerReader = (int) Math.ceil(imgDim.z / partitionCount);
        final List<int[]> l = new ArrayList<int[]>(partitionCount);
        for (int i = 0; i < imgDim.z; i += slicesPerReader) {
            final int maxSlice = Math.min(i + slicesPerReader, imgDim.z);
            l.add(new int[]{i, maxSlice});
        }

        return jsc.parallelize(l, partitionCount).flatMapToPair(new PairFlatMapFunction<int[], D3int, TImgBlock<U>>() {

            @Override
            public Iterable<Tuple2<D3int, TImgBlock<U>>> call(int[] sliceRange)
                    throws Exception {
                final PairFunction<Integer, D3int, TImgBlock<U>> standardPairFcn = new ReadSlice<U>(imgName, imgType, cImg.getPos(), cImg.getDim());

                ArrayList<Tuple2<D3int, TImgBlock<U>>> outSlices = new ArrayList<Tuple2<D3int, TImgBlock<U>>>(sliceRange[1] - sliceRange[0] + 1);

                for (int s = sliceRange[0]; s < sliceRange[1]; s++) {
                    outSlices.add(standardPairFcn.call(s));
                }
                return outSlices;
            }

        });
    }

    /**
     * import an image from an existing TImgRO by reading in every slice (this
     * is no manually done and singe core..)
     *
     * @param jsc
     * @param cImg
     * @param imgType
     * @return
     */
    protected static <U> JavaPairRDD<D3int, TImgBlock<U>> MigrateImage(
            final JavaSparkContext jsc, TImgRO cImg, final int imgType) {
        assert (TImgTools.isValidType(imgType));
        final D3int imgDim = cImg.getDim();
        final D3int imgPos = cImg.getPos();
        final D3int sliceDim = new D3int(imgDim.x, imgDim.y, 1);
        final D3int zero = new D3int(0);
        final List<Tuple2<D3int, TImgBlock<U>>> inSlices = new ArrayList<Tuple2<D3int, TImgBlock<U>>>(
                imgDim.z);
        for (int i = 0; i < imgDim.z; i++) {
            final int curSlice = i;
            final D3int nPos = new D3int(imgPos.x, imgPos.y, imgPos.z + i);
            TImgBlock<U> curBlock;
            if (futureTImgMigrate)
                curBlock = new TImgBlock.TImgBlockFromImage<U>(cImg, curSlice, imgType, nPos, sliceDim, zero);
            else curBlock = new TImgBlock<U>((U) cImg.getPolyImage(curSlice, imgType), nPos, sliceDim);

            inSlices.add(new Tuple2<D3int, TImgBlock<U>>(nPos, curBlock));
        }
        final int partitionCount = SparkGlobal.calculatePartitions(cImg.getDim().z);
        return jsc.parallelizePairs(inSlices, partitionCount);
    }


    /**
     * factory for wrapping RDDs into DTImg classes
     *
     * @param parent
     * @param newImage
     * @param imgType
     * @return
     */
    static public <Fc> DTImg<Fc> WrapRDD(TImgTools.HasDimensions parent, JavaPairRDD<D3int, TImgBlock<Fc>> newImage,
                                         int imgType) {
        DTImg<Fc> outImage = new DTImg<Fc>(parent, newImage, imgType, ArgumentList.TypedPath.virtualPath(newImage.toString()));
        return outImage;
    }


    /**
     * factory create a new image from a javasparkcontext and a path and type
     *
     * @param jsc
     * @param imgName
     * @param imgType
     */
    static public <Fc> DTImg<Fc> ReadImage(JavaSparkContext jsc, final ArgumentList.TypedPath imgName, int imgType) {
        JavaPairRDD<D3int, TImgBlock<Fc>> newImage = ImportImage(jsc, imgName, imgType);
        TImgTools.HasDimensions parent = TImgTools.ReadTImg(imgName);
        DTImg<Fc> outImage = new DTImg<Fc>(parent, newImage, imgType, imgName);
        return outImage;
    }

    /**
     * Produce a new DTImg from an existing TImgRO object
     *
     * @param jsc     the JavaSparkContext
     * @param inImage the input inmage to convert
     * @param imgType the type of the image (must match Fc)
     * @return
     */
    static public <Fc> DTImg<Fc> ConvertTImg(JavaSparkContext jsc, final TImgRO inImage, int imgType) {
        JavaPairRDD<D3int, TImgBlock<Fc>> newImage = MigrateImage(jsc, inImage, imgType);
        return new DTImg<Fc>(inImage, newImage, imgType, inImage.getPath());
    }

    static public <Fc> DTImg<Fc> ReadObjectFile(JavaSparkContext jsc, final ArgumentList.TypedPath imgName, int imgType) {
        final JavaRDD<Tuple2<D3int, TImgBlock<Fc>>> newImage = jsc.objectFile(imgName.getPath());
        final Tuple2<D3int, TImgBlock<Fc>> cTuple = newImage.first();
        final TImgBlock<Fc> cBlock = cTuple._2();
        JavaPairRDD<D3int, TImgBlock<Fc>> baseImg = newImage.mapToPair(new PairFunction<Tuple2<D3int, TImgBlock<Fc>>, D3int, TImgBlock<Fc>>() {
            @Override
            public Tuple2<D3int, TImgBlock<Fc>> call(final Tuple2<D3int, TImgBlock<Fc>> arg0)
                    throws Exception {
                return arg0;
            }

        });
        //TODO this assumes slices for normal data you will need to prowl the whole thing
        return new DTImg<Fc>(new TImgTools.HasDimensions() {
            final D3int pos = cBlock.getPos();
            final D3int dim = new D3int(cBlock.getDim().x, cBlock.getDim().y, (int) newImage.count());

            @Override
            public D3int getDim() {
                return dim;
            }

            @Override
            public D3float getElSize() {
                return new D3float(1.0f);
            }

            @Override
            public D3int getOffset() {
                return new D3int(0, 0, 0);
            }

            @Override
            public D3int getPos() {
                return pos;
            }

            @Override
            public String getProcLog() {
                return "";
            }

            @Override
            public float getShortScaleFactor() {
                return 1;
            }

        }, baseImg, imgType, imgName);


    }

    /**
     * get the javasparkcontext not the scala sparkcontext
     *
     * @return
     */
    public JavaSparkContext getContext() {
        return new JavaSparkContext(getBaseImg().context());
    }

    public JavaPairRDD<D3int, TImgBlock<T>> getBaseImg() {
        return baseImg;
    }

    @Override
    public String appendProcLog(String inData) {
        procLog += "\n" + inData;
        return procLog;
    }

    /**
     * Save the image in parallel using a TSliceWriter
     *
     * @param path
     */
    public void DSave(TypedPath path) {
        final TypedPath absTP = path.makeAbsPath();
        final TSliceWriter cWriter = TSliceWriter.Writers.ChooseBest(this,
                absTP, imageType);
        baseImg.foreach(new VoidFunction<Tuple2<D3int, TImgBlock<T>>>() {

            @Override
            public void call(Tuple2<D3int, TImgBlock<T>> arg0) throws Exception {

                cWriter.WriteSlice(arg0._2(), arg0._1().z);

            }

        });
    }


    /**
     * Save the image into a series of text files without header (format x,y,z,val)
     *
     * @param path
     */
    public void HSave(String path) {
        final boolean makeFolder = (new File(path)).mkdir();
        if (makeFolder) {
            System.out.println("Directory: " + path + " created");
        }
        final String absTP =
                (new File(path)).getAbsolutePath() + "/";

        String plPath = absTP + "procLog.log";
        final boolean isSigned = this.getSigned();
        final float ssf = this.getShortScaleFactor();
        baseImg.foreach(new VoidFunction<Tuple2<D3int, TImgBlock<T>>>() {

            @Override
            public void call(final Tuple2<D3int, TImgBlock<T>> inBlock) throws Exception {
                final D3int pos = inBlock._1();
                final TImgBlock<T> startingBlock = inBlock._2();
                final OutputStreamWriter outFile = new OutputStreamWriter(new FileOutputStream(absTP + "block." + pos.x + "_" + pos.y + "_" + pos.z + ".csv"), "UTF-8");
                T curPts = startingBlock.get();
                double[] dblPts = (double[]) TImgTools.convertArrayType(curPts, TImgTools.identifySliceType(curPts),
                        TImgTools.IMAGETYPE_DOUBLE, isSigned, ssf, Integer.MAX_VALUE);
                for (int zi = 0; zi < startingBlock.getDim().z; zi++) {
                    int zpos = zi + pos.z;
                    for (int yi = 0; yi < startingBlock.getDim().y; yi++) {
                        int ypos = yi + pos.y;
                        for (int xi = 0; xi < startingBlock.getDim().x; xi++) {
                            int ind = (zi * getDim().z + yi) * getDim().y + xi;
                            int xpos = xi + pos.x;
                            outFile.write(xpos + "," + ypos + "," + zpos + "," + dblPts[ind] + "\n");
                        }
                    }
                }
                outFile.close();

            }

        });
        try {
            FileWriter fstream = new FileWriter(plPath);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(this.getProcLog() + "\n");
            out.write("POS:" + this.getPos() + "\n");
            out.write("ELESIZE:" + this.getElSize() + "\n");
            out.write("DIM:" + this.getDim() + "\n");
            // Close the output stream
            out.close();
            fstream.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error, Header for " + plPath + " could not be written");
        }


    }

    /**
     * A fairly simple operation of filtering the RDD for the correct slice and returning that slice
     */
    @Override
    public Object getPolyImage(int sliceNumber, final int asType) {
    	if ((sliceNumber<0) || (sliceNumber>=getDim().z)) throw new IllegalArgumentException(this.getSampleName()+": Slice requested ("+sliceNumber+") exceeds image dimensions "+getDim());
    	
    	final int zPos = getPos().z + sliceNumber;

    	
    	List<TImgBlock<T>> outSlices =  this.baseImg.lookup(new D3int(getPos().x,getPos().y,zPos));
    	
    	if (outSlices.size()!=1) throw 
    	new IllegalArgumentException(this.getSampleName()+", lookup failed (#"+outSlices.size()+" found):"+sliceNumber+" (z:"+zPos+"), of "+getDim()+" of #"+this.baseImg.count()+" blocks");
    	
    	T curSlice = outSlices.get(0).get();
    	
        return TImgTools.convertArrayType(curSlice, getImageType(), asType, getSigned(), getShortScaleFactor());
    }



    @Override
    public String getSampleName() {
        return path.getPath();
    }
    
    @Override
    protected TImg makeImageFromDataArray(D3int dim,D3int pos,D3float elSize,int stype,Object[] sliceData) {
    	final TImg tempConstruct = new ArrayBackedTImg(dim,pos,elSize,stype,sliceData);
    	final JavaSparkContext cJsc = SparkGlobal.getContext();
        final JavaPairRDD<D3int, TImgBlock<T>> oldImage = MigrateImage(cJsc,
                tempConstruct, stype);
        return DTImg.WrapRDD(tempConstruct, oldImage, getImageType());
    }


    @Override
    public boolean InitializeImage(D3int dPos, D3int cDim, D3int dOffset,
                                   D3float elSize, int imageType) {
        throw new IllegalArgumentException("Not Implemented");
    }


    /**
     * passes basically directly through to the JavaPair RDD but it wraps
     * everything in a DTImg class. Automatically partition
     *
     * @param mapFunc
     * @return
     */
    public <U> DTImg<U> map(
            final PairFunction<Tuple2<D3int, TImgBlock<T>>, D3int, TImgBlock<U>> mapFunc,
            final int outType) {
        return DTImg.WrapRDD(this, this.baseImg.mapToPair(mapFunc).partitionBy(SparkGlobal.getPartitioner(getDim())), outType);
    }

    /**
     * passes basically directly through to the JavaPair RDD but it wraps
     * everything in a DTImg class and automatically partitions
     *
     * @param mapFunc
     * @return
     */
    public <U> DTImg<U> mapValues(
            final Function<TImgBlock<T>, TImgBlock<U>> mapFunc,
            final int outType) {
        return DTImg.WrapRDD(this, this.baseImg.mapValues(mapFunc).partitionBy(SparkGlobal.getPartitioner(getDim())), outType);
    }

    /**
     * apply a given voxel function in parallel to every point in the image
     *
     * @param inFunction
     * @return
     */
    public DTImg<double[]> applyVoxelFunction(final FImage.VoxelFunction inFunction) {
        return mapValues(new Function<TImgBlock<T>, TImgBlock<double[]>>() {

            @Override
            public TImgBlock<double[]> call(TImgBlock<T> startingBlock) throws Exception {

                T curPts = startingBlock.get();
                D3int sPos = startingBlock.getPos();
                double[] dblPts = (double[]) TImgTools.convertArrayType(curPts, TImgTools.identifySliceType(curPts),
                        TImgTools.IMAGETYPE_DOUBLE, true, 1, Integer.MAX_VALUE);
                double[] outPts = new double[dblPts.length];
                    Double zpos = (double) (sPos.z);
                    for (int yi = 0; yi < startingBlock.getDim().y; yi++) {
                        Double ypos = (double) (yi + sPos.y);
                        for (int xi = 0; xi < startingBlock.getDim().x; xi++) {
                            int ind = yi * getDim().y + xi;
                            Double xpos = (double) (xi + sPos.x);
                            Double[] ipos = new Double[]{xpos, ypos, zpos};
                            outPts[ind] = inFunction.get(ipos, dblPts[ind]);
                        }
                    }
 
                return new TImgBlock<double[]>(outPts, startingBlock);

            }

        }, TImgTools.IMAGETYPE_DOUBLE);
    }
    
    /**
     * Convert the current image into an integer image (for labels useful)
     * @return
     */
    static public <To,Tn> DTImg<Tn> changeType(DTImg<To> inImage, final int outType) {
    	assert(TImgTools.isValidType(outType));
        return inImage.mapValues(new Function<TImgBlock<To>, TImgBlock<Tn>>() {
            @Override
            public TImgBlock<Tn> call(TImgBlock<To> startingBlock) throws Exception {
                To curPts = startingBlock.get();
                Tn ipts = (Tn) TImgTools.convertArrayType(curPts, TImgTools.identifySliceType(curPts),
                        outType, true, 1, Integer.MAX_VALUE);
                
                return new TImgBlock<Tn>(ipts, startingBlock);
            }
        }, outType);
    }
    
    /**
     * Convert the current image into an float image (for labels useful)
     * @return
     */
    public DTImg<float[]> asDTFloat() {
        return DTImg.changeType(this,TImgTools.IMAGETYPE_FLOAT);
    }
    
    /**
     * Convert the current image into an float image (for labels useful)
     * @return
     */
    public DTImg<double[]> asDTDouble() {
        return DTImg.changeType(this,TImgTools.IMAGETYPE_DOUBLE);
        
    }
    /**
     * Convert the current image into an integer image (for labels useful)
     * @return
     */
    public DTImg<int[]> asDTInt() {
        return DTImg.changeType(this,TImgTools.IMAGETYPE_INT);
    }
    
    /**
     * Convert the current image into an long image (for labels useful)
     * @return
     */
    public DTImg<long[]> asDTLong() {
        return DTImg.changeType(this,TImgTools.IMAGETYPE_LONG);
    }
    
    /**
     * Convert the current image into an boolean image (for labels useful)
     * @return
     */
    public DTImg<boolean[]> asDTBool() {
        return DTImg.changeType(this,TImgTools.IMAGETYPE_BOOL);
    }

    /**
     * Performs a subselection (a function filter) of the dataset based on the blocks
     *
     * @param filtFunc the function to filter with
     * @return a subselection of the image
     */
    public DTImg<T> subselect(
            final Function<Tuple2<D3int, TImgBlock<T>>, Boolean> filtFunc
    ) {
        final JavaPairRDD<D3int, TImgBlock<T>> subImg = this.baseImg.filter(filtFunc);
        DTImg<T> outImage = DTImg.WrapRDD(this, subImg, this.getImageType());
        //TODO Only works on slices
        int sliceCount = (int) subImg.count();
        outImage.setDim(new D3int(outImage.getDim().x, outImage.getDim().y, sliceCount));
        outImage.setPos(subImg.first()._1);
        return outImage;
    }

    /**
     * The same as subselect but takes a function which operates on just the positions instead (needs to be erased because of strange type erasure behavior)
     *
     * @param filtFunc
     * @return
     */
    public DTImg<T> subselectPos(
            final Function<D3int, Boolean> filtFunc
    ) {
        return subselect(new Function<Tuple2<D3int, TImgBlock<T>>, Boolean>() {

            @Override
            public Boolean call(Tuple2<D3int, TImgBlock<T>> arg0)
                    throws Exception {
                return filtFunc.call(arg0._1);
            }

        });
    }

    /**
     * first spreads the slices out, then runs a group by key, then applies the given mapfunction and creates a new DTImg that wraps around the object
     *
     * @param spreadWidth number of slices to spread out over
     * @param mapFunc
     * @return
     */
    public <U> DTImg<U> spreadMap(
            final int spreadWidth,
            final PairFunction<Tuple2<D3int, Iterable<TImgBlock<T>>>, D3int, TImgBlock<U>> mapFunc,
            final int outType) {

        JavaPairRDD<D3int, Iterable<TImgBlock<T>>> joinImg;
        joinImg = this.spreadSlices(spreadWidth).
                groupByKey(getPartitions()).
                partitionBy(SparkGlobal.getPartitioner(getDim()));

        return DTImg.WrapRDD(this, joinImg.
                mapToPair(mapFunc), outType);
    }

    public void showPartitions() {
        //this.baseImg.mapPartition()
        List<String> curPartitions = this.baseImg.mapPartitions(new FlatMapFunction<Iterator<Tuple2<D3int, TImgBlock<T>>>, String>() {

            @Override
            public Iterable<String> call(
                    Iterator<Tuple2<D3int, TImgBlock<T>>> arg0)
                    throws Exception {
                List<String> outList = new LinkedList<String>();
                outList.add("\nPartition:");
                while (arg0.hasNext()) outList.add("" + arg0.next()._1.z);
                return outList;
            }

        }).collect();
        String partStr = "";

        for (String cPartition : curPartitions) partStr += ", " + cPartition;
        System.out.println("Partitions=>" + partStr);
    }

    public void setRDDName(String cName) {
        this.baseImg.setName(cName);
    }

    /**
     * the number of partitions to use when breaking up data
     *
     * @return partition count
     */
    public int getPartitions() {
        return SparkGlobal.calculatePartitions(getDim().z);
    }

    /**
     * Set the persistence level of the image only if none has been set so far.
     *
     * @param setLevel the level from the storagelevel class
     */
    public void persist(StorageLevel setLevel) {
        if (this.baseImg.getStorageLevel() == StorageLevel.NONE()) this.baseImg.persist(setLevel);
    }

    // Here are the specialty functions for DTImages
    public JavaPairRDD<D3int, List<TImgBlock<T>>> spreadSlices3(int windSize) {
        JavaPairRDD<D3int, TImgBlock<T>> down1 = baseImg.mapToPair(new BlockShifter(new D3int(0, 0, -1)));
        JavaPairRDD<D3int, TImgBlock<T>> up1 = baseImg.mapToPair(new BlockShifter(new D3int(0, 0, 1)));
        JavaPairRDD<D3int, Tuple3<Iterable<TImgBlock<T>>, Iterable<TImgBlock<T>>, Iterable<TImgBlock<T>>>> joinImg = baseImg.cogroup(down1, up1, SparkGlobal.getPartitioner(getDim()));
        return joinImg.mapValues(new Function<Tuple3<Iterable<TImgBlock<T>>, Iterable<TImgBlock<T>>, Iterable<TImgBlock<T>>>, List<TImgBlock<T>>>() {

            @Override
            public List<TImgBlock<T>> call(
                    Tuple3<Iterable<TImgBlock<T>>, Iterable<TImgBlock<T>>, Iterable<TImgBlock<T>>> arg0)
                    throws Exception {
                // TODO Auto-generated method stub
                List<TImgBlock<T>> alist = org.apache.commons.collections.IteratorUtils.toList(arg0._1().iterator());
                List<TImgBlock<T>> blist = org.apache.commons.collections.IteratorUtils.toList(arg0._2().iterator());
                List<TImgBlock<T>> clist = org.apache.commons.collections.IteratorUtils.toList(arg0._3().iterator());
                List<TImgBlock<T>> outList = new ArrayList(alist.size() + blist.size() + clist.size());
                outList.addAll(alist);
                outList.addAll(blist);
                outList.addAll(clist);
                return outList;
            }

        });

    }

    /**
     * Spread blocks out over a given range
     *
     * @param blockDimension the size of the blocks to distribute
     * @param offsetList     the offsets of the starting position of the blocks
     * @return
     */
    public JavaPairRDD<D3int, TImgBlock<T>> spreadBlocks(final D3int[] offsetList) {
        return baseImg.flatMapToPair(new BlockSpreader(offsetList, getDim()));
    }

    /**
     * Spread out image over slices
     *
     * @param windowSize range above and below to spread
     * @return
     */
    protected JavaPairRDD<D3int, TImgBlock<T>> spreadSlices(final int windowSize) {
        return baseImg.flatMapToPair(BlockSpreader.<T>SpreadSlices(windowSize, getDim()));

    }

    /**
     * An overloaded function for reading slices from images
     *
     * @param <W> the type of the image as an array
     * @author mader
     */
    protected static class ReadSlice<W> implements
            PairFunction<Integer, D3int, TImgBlock<W>> {
        protected final ArgumentList.TypedPath imgPath;
        protected final int imgType;
        protected final D3int imgPos;
        protected final D3int sliceDim;

        /**
         * The function for reading slices from an image
         *
         * @param imgName the path to the image
         * @param inType  the type of image to be loaded (must match with W, convert
         *                later)
         * @param imPos   the starting position of the image
         * @param imgDim  the dimensions of the image
         */
        public ReadSlice(ArgumentList.TypedPath imgName, int inType, final D3int imPos,
                         final D3int imgDim) {
            this.imgPos = imPos;
            this.sliceDim = new D3int(imgDim.x, imgDim.y, 1);
            // this is important since spark instances do not know the current
            // working directory
            this.imgPath = imgName.makeAbsPath();
            this.imgType = inType;
        }

        @Override
        public Tuple2<D3int, TImgBlock<W>> call(Integer sliceNum) {
            if (!TIPLGlobal.waitForReader()) throw new IllegalArgumentException("Timed Out Waiting for Reader" + this);

            final W cSlice = (W) TImgTools.ReadTImgSlice(imgPath,
                    sliceNum, imgType);
            TIPLGlobal.returnReader();
            final D3int cPos = new D3int(imgPos.x, imgPos.y, imgPos.z
                    + sliceNum);
            return new Tuple2<D3int, TImgBlock<W>>(cPos, new TImgBlock<W>(
                    cSlice, cPos, sliceDim));
        }
    }

    /**
     * Another version of the read slice code where the read itself is a future rather than upon creation
     *
     * @param <W> the type of the image as an array
     * @author mader
     */
    protected static class ReadSlicePromise<W> implements
            PairFunction<Integer, D3int, TImgBlock<W>> {
        protected final ArgumentList.TypedPath imgPath;
        protected final int imgType;
        protected final D3int imgPos;
        protected final D3int sliceDim;

        /**
         * The function for reading slices from an image
         *
         * @param imgName the path to the image
         * @param inType  the type of image to be loaded (must match with W, convert
         *                later)
         * @param imPos   the starting position of the image
         * @param imgDim  the dimensions of the image
         */
        public ReadSlicePromise(ArgumentList.TypedPath imgName, int inType, final D3int imPos,
                                final D3int imgDim) {
            this.imgPos = imPos;
            this.sliceDim = new D3int(imgDim.x, imgDim.y, 1);
            // this is important since spark instances do not know the current
            // working directory
            this.imgPath = imgName.makeAbsPath();
            this.imgType = inType;
        }

        @Override
        public Tuple2<D3int, TImgBlock<W>> call(Integer sliceNum) {
            final D3int cPos = new D3int(imgPos.x, imgPos.y, imgPos.z
                    + sliceNum);
            return new Tuple2<D3int, TImgBlock<W>>(cPos, new TImgBlock.TImgBlockFile<W>(
                    imgPath, sliceNum, imgType, cPos, sliceDim, TImgBlock.zero));
        }
    }

    /**
     * a function to turn a slice into a list of points
     *
     * @param <U>
     * @author mader
     */
    public static class SliceToPoints<U> implements PairFlatMapFunction<Tuple2<D3int, TImgBlock<U>>, D3int, Number> {
        final int imageType;

        public SliceToPoints(int inImageType) {
            imageType = inImageType;
        }

        @Override
        public Iterable<Tuple2<D3int, Number>> call(
                Tuple2<D3int, TImgBlock<U>> arg0) throws Exception {
            TImgBlock<U> cBlock = arg0._2;
            final U curSlice = cBlock.get();
            final D3int dim = cBlock.getDim();
            final D3int pos = arg0._1;
            int sliceLength;
            switch (imageType) {
                case TImgTools.IMAGETYPE_BOOL:
                    sliceLength = ((boolean[]) curSlice).length;
                    break;
                case TImgTools.IMAGETYPE_CHAR:
                    sliceLength = ((char[]) curSlice).length;
                    break;
                case TImgTools.IMAGETYPE_INT:
                    sliceLength = ((int[]) curSlice).length;
                    break;
                case TImgTools.IMAGETYPE_FLOAT:
                    sliceLength = ((float[]) curSlice).length;
                    break;
                case TImgTools.IMAGETYPE_DOUBLE:
                    sliceLength = ((double[]) curSlice).length;
                    break;
                case TImgTools.IMAGETYPE_LONG:
                    sliceLength = ((long[]) curSlice).length;
                    break;
                default:
                    throw new IllegalArgumentException("Image type :" + TImgTools.getImageTypeName(imageType) + " is not yet supported");

            }
            ArrayList<Tuple2<D3int, Number>> outList = new ArrayList<Tuple2<D3int, Number>>(sliceLength);
            if (TIPLGlobal.getDebug()) System.out.println("Current Block: dim:" + dim + " @ " + pos);
            int index;
            for (int z = 0; z < dim.z; z++) {
                for (int y = 0; y < dim.y; y++) {
                    index = (z * dim.y + y) * dim.x;
                    for (int x = 0; x < dim.x; x++, index++) {
                        final D3int outPos = new D3int(pos.x + x, pos.y + y, pos.z + z);
                        if (index >= sliceLength)
                            throw new IllegalArgumentException("Current Pos:(" + x + "," + y + "," + z + ")/" + pos + " of (" + dim + ") @ " + index + " is outside of:" + sliceLength);
                        Number outValue;
                        switch (imageType) {
                            case TImgTools.IMAGETYPE_BOOL:
                                outValue = ((boolean[]) curSlice)[index] ? (byte) 0 : (byte) 127;
                                break;
                            case TImgTools.IMAGETYPE_CHAR:
                                outValue = (byte) ((char[]) curSlice)[index];
                                break;
                            case TImgTools.IMAGETYPE_INT:
                                outValue = ((int[]) curSlice)[index];
                                break;
                            case TImgTools.IMAGETYPE_FLOAT:
                                outValue = ((float[]) curSlice)[index];
                                break;
                            case TImgTools.IMAGETYPE_DOUBLE:
                                outValue = ((double[]) curSlice)[index];
                                break;
                            case TImgTools.IMAGETYPE_LONG:
                                outValue = ((long[]) curSlice)[index];
                                break;
                            default:
                                throw new IllegalArgumentException("Image type :" + TImgTools.getImageTypeName(imageType) + " is not yet supported");

                        }
                        outList.add(new Tuple2<D3int, Number>(outPos, outValue));
                    }
                }
            }
            return outList;
        }

    }

    /**
     * A simple block shifting class for testing out the join operations
     *
     * @param <T>
     * @author mader
     */
    protected static class BlockShifter<T> implements PairFunction<Tuple2<D3int, TImgBlock<T>>, D3int, TImgBlock<T>> {
        protected final D3int inOffset;

        // Since we can't have constructors here (probably should make it into a subclass)
        public BlockShifter(D3int inOffset) {
            this.inOffset = inOffset;
        }

        @Override
        public Tuple2<D3int, TImgBlock<T>> call(
                Tuple2<D3int, TImgBlock<T>> inData) {
            final TImgBlock<T> inSlice = inData._2();
            final D3int nOffset = this.inOffset;

            final D3int oPos = inData._1();
            final D3int nPos = new D3int(oPos.x + nOffset.x, oPos.y + nOffset.y, oPos.z + nOffset.z);
            return new Tuple2<D3int, TImgBlock<T>>(
                    nPos, new TImgBlock<T>(inSlice
                    .getClone(), nPos, inSlice.getDim(),
                    nOffset));

        }
    }

    /**
     * A class for spreading out blocks
     *
     * @param <T>
     * @author mader
     */
    protected static class BlockSpreader<T> implements PairFlatMapFunction<Tuple2<D3int, TImgBlock<T>>, D3int, TImgBlock<T>> {

        protected final D3int[] inOffsetList;

        protected final D3int imgDim;

        // Since we can't have constructors here (probably should make it into a subclass)
        public BlockSpreader(D3int[] inOffsetList, D3int imgDim) {
            this.inOffsetList = inOffsetList;
            this.imgDim = imgDim;
        }

        static public <Fc> BlockSpreader<Fc> SpreadSlices(final int windowSize, final D3int imgDim) {
            final D3int sliceDim = new D3int(imgDim.x, imgDim.y, 1);
            final D3int[] offsetList = new D3int[2 * windowSize + 1];
            for (int i = -windowSize; i <= windowSize; i++) offsetList[i + windowSize] = new D3int(0, 0, i);
            return new BlockSpreader<Fc>(offsetList, imgDim);

        }

        @Override
        public Iterable<Tuple2<D3int, TImgBlock<T>>> call(
                Tuple2<D3int, TImgBlock<T>> inData) {
            final TImgBlock<T> inSlice = inData._2();
            final List<Tuple2<D3int, TImgBlock<T>>> outList = new ArrayList<Tuple2<D3int, TImgBlock<T>>>(inOffsetList.length);
            for (final D3int nOffset : this.inOffsetList) {
                final D3int oPos = inData._1();
                final D3int nPos = new D3int(oPos.x + nOffset.x, oPos.y + nOffset.y, oPos.z + nOffset.z);
                /**
                 * the clone is used otherwise it loses slices when
                 * they drift between machines (I think)
                 */
                if (nPos.z >= 0 & nPos.z < imgDim.z)
                    outList.add(new Tuple2<D3int, TImgBlock<T>>(
                            nPos, new TImgBlock<T>(inSlice
                            .getClone(), nPos, inSlice.getDim(),
                            nOffset)));
            }
            return outList;
        }
    }


}
