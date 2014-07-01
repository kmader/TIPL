/**
 *
 */
package tipl.spark;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

import java.io.Serializable;
import java.util.List;

/**
 * KVImg is a key value pair image consisting of a key (position formatted as D3int) and a value of type T (extension of number)
 * <strong>Note since neither Character or Boolean are subclasses of number they have both been replaced with Byte</strong>
 *
 * @author mader
 */
public class KVImg<T extends Number> extends TImg.ATImg implements TImg, Serializable {
    final protected JavaPairRDD<D3int, T> baseImg;
    final int partitionCount;
    public KVImg(TImgTools.HasDimensions tempImg, final int iimageType, JavaRDD<Tuple2<D3int, T>> ibImg) {
        super(tempImg, iimageType);
        partitionCount = SparkGlobal.calculatePartitions(tempImg.getDim().z);
        baseImg = ibImg.mapToPair(new TupleToPair()).repartition(partitionCount);
    }

    public KVImg(D3int idim, D3int ipos, D3float ielSize, final int iimageType, JavaPairRDD<D3int, T> ibImg) {
        super(idim, ipos, ielSize, iimageType);
        partitionCount = SparkGlobal.calculatePartitions(idim.z);
        baseImg = ibImg.repartition(partitionCount);
    }

    /**
     * a function to make passing images from scala / spark easier
     *
     * @param inImg
     * @param imageType
     * @param baseImg
     * @return
     */
    public static <R extends Number> KVImg<R> FromRDD(TImgTools.HasDimensions inImg, final int imageType, JavaRDD<Tuple2<D3int, Double>> baseImg) {
        return
                new KVImg<R>(inImg.getDim(), inImg.getPos(), inImg.getElSize(), imageType,
                        baseImg.mapToPair(new pairDoubleConversion<R>(imageType)));
    }

    static public KVImg ConvertTImg(JavaSparkContext jsc, TImgRO inImage, int imageType) {
        return DTImg.ConvertTImg(jsc, inImage, imageType).asKV();
    }

    public JavaPairRDD<D3int, T> getBaseImg() {
        return baseImg;
    }

    /**
     * Force the KVImg to be a long no matter what it is now (used for shape analysis and other applications)
     */
    @SuppressWarnings("serial")
    public KVImg<Long> toKVLong() {
        if (imageType == TImgTools.IMAGETYPE_LONG) return (KVImg<Long>) this;
        return new KVImg<Long>(dim, pos, elSize, TImgTools.IMAGETYPE_LONG,
                baseImg.mapValues(new Function<T, Long>() {
                    @Override
                    public Long call(T arg0) throws Exception {
                        return arg0.longValue();
                    }
                }));
    }
    public KVImg<Long> toKVSLong() {
        if (imageType == TImgTools.IMAGETYPE_LONG) return (KVImg<Long>) this;
        return new KVImg<Long>(dim, pos, elSize, TImgTools.IMAGETYPE_LONG,
                baseImg.mapValues(new Function<T, Long>() {
                    @Override
                    public Long call(T arg0) throws Exception {
                        return arg0.longValue();
                    }
                }));
    }

    /**
     * Force the KVImg to be a long no matter what it is now (used for shape analysis and other applications)
     */
    @SuppressWarnings("serial")
    public KVImg<Float> toKVFloat() {
        if (imageType == TImgTools.IMAGETYPE_FLOAT) return (KVImg<Float>) this;
        return new KVImg<Float>(dim, pos, elSize, TImgTools.IMAGETYPE_FLOAT,
                baseImg.mapValues(new Function<T, Float>() {
                    @Override
                    public Float call(T arg0) throws Exception {
                        return arg0.floatValue();
                    }
                }));
    }

    /* The function to collect all the key value pairs and return it as the appropriate array for a given slice
     * @see tipl.formats.TImgRO#getPolyImage(int, int)
     */
    @Override
    public Object getPolyImage(final int sliceNumber, int asType) {
        assert (TImgTools.isValidType(asType));
        final D3int cDim = getDim();
        final int sliceSize = cDim.x * cDim.y;
        final int cImageType = imageType;

        List<Tuple2<Integer, T>> sliceAsList = baseImg.filter(new Function<Tuple2<D3int, T>, Boolean>() {

            @Override
            public Boolean call(Tuple2<D3int, T> arg0) throws Exception {
                if (arg0._1.z == sliceNumber) return true;
                else return false;
            }

        }).mapToPair(new posToIndex<T>(cDim, getPos())).sortByKey().collect();

        // first create an array of the current type
        Object curSlice = null;
        switch (cImageType) {
            case TImgTools.IMAGETYPE_BOOL:
                curSlice = new boolean[sliceSize];
                break;
            case TImgTools.IMAGETYPE_CHAR:
                curSlice = new char[sliceSize];
                break;
            case TImgTools.IMAGETYPE_INT:
                curSlice = new int[sliceSize];
                break;
            case TImgTools.IMAGETYPE_FLOAT:
                curSlice = new float[sliceSize];
                break;
            case TImgTools.IMAGETYPE_DOUBLE:
                curSlice = new double[sliceSize];
                break;
            case TImgTools.IMAGETYPE_LONG:
                curSlice = new long[sliceSize];
                break;
            default:
                throw new IllegalArgumentException("Image type :" + TImgTools.getImageTypeName(imageType) + " is not yet supported");


        }
        // now copy the list into this array
        for (Tuple2<Integer, T> curElement : sliceAsList) {
            final int index = curElement._1.intValue();
            switch (cImageType) {
                case TImgTools.IMAGETYPE_BOOL:
                    ((boolean[]) curSlice)[index] = (Byte) curElement._2 > 0;
                    break;
                case TImgTools.IMAGETYPE_CHAR:
                    ((char[]) curSlice)[index] = (char) ((Byte) curElement._2).byteValue();
                    break;
                case TImgTools.IMAGETYPE_INT:
                    ((int[]) curSlice)[index] = (Integer) curElement._2;
                    break;
                case TImgTools.IMAGETYPE_FLOAT:
                    ((float[]) curSlice)[index] = (Float) curElement._2;
                    break;
                case TImgTools.IMAGETYPE_DOUBLE:
                    ((double[]) curSlice)[index] = (Double) curElement._2;
                    break;
                case TImgTools.IMAGETYPE_LONG:
                    ((long[]) curSlice)[index] = (Long) curElement._2;
                    break;
                default:
                    throw new IllegalArgumentException("Image type :" + TImgTools.getImageTypeName(imageType) + " is not yet supported");

            }
        }
        // convert this array into the proper output format
        return TImgTools.convertArrayType(curSlice, cImageType, asType, getSigned(), getShortScaleFactor());
    }

    /* (non-Javadoc)
     * @see tipl.formats.TImgRO#getSampleName()
     */
    @Override
    public String getSampleName() {
        return baseImg.name();
    }

    @Override
    public TImg inheritedAim(TImgRO inAim) {
        TImg outImage = ConvertTImg(SparkGlobal.getContext("" + this), inAim, inAim.getImageType());
        outImage.appendProcLog("Merged with:" + getSampleName() + ":" + this + "\n" + getProcLog());
        return outImage;
    }

    static public class TupleToPair<T extends Number> implements PairFunction<Tuple2<D3int, T>, D3int, T> {

        @Override
        public Tuple2<D3int, T> call(Tuple2<D3int, T> arg0) throws Exception {
            return arg0;
        }

    }

    /**
     * a class to convert the double output of many functions (FImage, PureFImage, etc) to the proper type
     *
     * @param <U>
     * @author mader
     */
    static public class pairDoubleConversion<U extends Number> implements PairFunction<Tuple2<D3int, Double>, D3int, U> {
        /**
         * the output image type (always from double)
         */
        final int imageType;

        public pairDoubleConversion(int iit) {
            assert (TImgTools.isValidType(iit));
            imageType = iit;
        }

        @Override
        public Tuple2<D3int, U> call(Tuple2<D3int, Double> arg0) throws Exception {
            double cVal = (arg0._2).doubleValue();
            Object outVal;
            switch (imageType) {
                case TImgTools.IMAGETYPE_BOOL:
                    outVal = cVal > 0;
                    break;
                case TImgTools.IMAGETYPE_CHAR:
                    outVal = (char) cVal;
                    break;
                case TImgTools.IMAGETYPE_SHORT:
                    outVal = (short) cVal;
                    break;
                case TImgTools.IMAGETYPE_FLOAT:
                    outVal = (float) cVal;
                    break;
                case TImgTools.IMAGETYPE_INT:
                    outVal = (int) cVal;
                    break;
                case TImgTools.IMAGETYPE_LONG:
                    outVal = (long) cVal;
                    break;
                case TImgTools.IMAGETYPE_DOUBLE:
                    outVal = cVal;
                    break;
                default:
                    throw new IllegalArgumentException("Type not officially supported:" + imageType + " = " + TImgTools.getImageTypeName(imageType));
            }

            return new Tuple2<D3int, U>(arg0._1, (U) outVal);
        }

    }

    /**
     * Turns a KV image listed by position (as D3int) to one listed by index
     *
     * @param <U>
     * @author mader
     */
    static public class posToIndex<U> implements PairFunction<Tuple2<D3int, U>, Integer, U> {
        protected final D3int cDim, cPos;

        public posToIndex(D3int inDim, D3int inPos) {
            cDim = inDim;
            cPos = inPos;
        }

        @Override
        public Tuple2<Integer, U> call(Tuple2<D3int, U> curElement)
                throws Exception {

            return new Tuple2<Integer, U>((curElement._1.y - cPos.y) * cDim.x + curElement._1.x - cPos.x, curElement._2);
        }
    }


}
