package tipl.spark;

import org.apache.commons.collections.IteratorUtils;
import scala.Tuple2;
import tipl.formats.TImgRO;
import tipl.tools.BaseTIPLPluginIn;
import tipl.util.D3int;
import tipl.util.D4int;
import tipl.util.ITIPLPlugin;
import tipl.util.TImgBlock;

import java.io.Serializable;
import java.util.List;

/**
 * A generic interface for neighborhood operations and filters and the basis for making more
 * complicated algorithms for image processing using the fan-out/shuffle, group, and map operations.
 *
 * @param <U> input image format
 * @param <V> output image format
 * @author mader
 */
abstract public interface NeighborhoodPlugin<U extends Cloneable, V extends Cloneable>
        extends Serializable, ITIPLPlugin {
    /**
     * A function which combines blocks containing the same position (and
     * different offsets) A good example is to get surrounding values for
     * calculating a filter
     *
     * @param inBlocks
     * @return
     */
    public Tuple2<D3int, TImgBlock<V>> GatherBlocks(
            Tuple2<D3int, Iterable<TImgBlock<U>>> inBlocks);

    /**
     * THe simplist implementation of the float filter
     *
     * @author mader
     */
    abstract public class FloatFilter extends GenericFilter<float[], float[]> {
        final public static boolean show_debug = false;

        @Override
        protected float[] createObj(int size) {
            return new float[size];
        }

        @Override
        protected double getEle(float[] obj, int index) {
            return obj[index];
        }

        @Override
        protected void setEle(float[] obj, int index, double val) {
            obj[index] = (float) val;
        }
    }


    @SuppressWarnings("serial")
    public abstract class GatherBasedPlugin<U extends Cloneable, V extends Cloneable>
            extends BaseTIPLPluginIn implements NeighborhoodPlugin<U, V> {

        public GatherBasedPlugin() {
        }

        abstract public BaseTIPLPluginIn.filterKernel getImageKernel();

    }


    /**
     * A very generic class for filtering with abstract methods for getting and
     * setting elements inside the generic types since that is not by default
     * support since they aren't arrays (boo java)
     *
     * @param <U>
     * @param <V>
     * @author mader
     */
    abstract public class GenericFilter<U extends Cloneable, V extends Cloneable>
            extends GatherBasedPlugin<U, V> {
        DTImg<U> basisImage;

        abstract protected V createObj(int size);

        @Override
        public Tuple2<D3int, TImgBlock<V>> GatherBlocks(
                Tuple2<D3int, Iterable<TImgBlock<U>>> inTuple) {
            final D3int ns = getNeighborSize();

            final List<TImgBlock<U>> inBlocks = IteratorUtils.toList(inTuple._2().iterator());
            final TImgBlock<U> templateBlock = inBlocks.get(0);
            final D3int blockSize = templateBlock.getDim();
            final BaseTIPLPluginIn.morphKernel mKernel = getKernel();
            final int eleCount = (int) templateBlock.getDim().prod();
            // the output image
            final V outData = createObj(eleCount);
            final BaseTIPLPluginIn.filterKernel[] kernelList = new BaseTIPLPluginIn
                    .filterKernel[eleCount];
            for (int i = 0; i < eleCount; i++) kernelList[i] = getImageKernel();
            for (final TImgBlock<U> cBlock : inBlocks) {
                final U curBlock = cBlock.get();
                for (int zp = 0; zp < templateBlock.getDim().z; zp++) {
                    for (int yp = 0; yp < templateBlock.getDim().y; yp++) {
                        for (int xp = 0; xp < templateBlock.getDim().x; xp++) {
                            final int off = ((zp) * blockSize.y + (yp))
                                    * blockSize.x + (xp);
                            final BaseTIPLPluginIn.filterKernel curKernel = kernelList[off];
                            for (D4int cPos : BaseTIPLPluginIn.getScanPositions(mKernel,
                                    new D3int(xp, yp, zp), cBlock.getOffset(), off, blockSize,
                                    ns)) {
                                //if (mKernel.inside(off, cPos.offset, xp, cPos.x, yp, cPos.y,
                                // zp, cPos.z)) {
                                curKernel.addpt(xp, cPos.x, yp, cPos.y, zp, cPos.z,
                                        getEle(curBlock, cPos.offset));
                                //}
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < eleCount; i++) setEle(outData, i, kernelList[i].value());
            return new Tuple2<D3int, TImgBlock<V>>(inTuple._1(),
                    new TImgBlock<V>(outData, templateBlock.getPos(),
                            templateBlock.getDim()));
        }

        abstract protected double getEle(U obj, int index);

        abstract protected void setEle(V obj, int index, double val);

        public void LoadImages(TImgRO[] inputImages) {
            if (!(basisImage instanceof DTImg))
                throw new IllegalArgumentException("This only works with DTImg");
            basisImage = (DTImg<U>) inputImages[0];
        }
    }


    /**
     * A version of the float filter optimized for operating on slices
     *
     * @author mader
     */
    abstract public class FloatFilterSlice extends FloatFilter {
        final public static boolean show_debug = false;

        @Override
        public Tuple2<D3int, TImgBlock<float[]>> GatherBlocks(
                Tuple2<D3int, Iterable<TImgBlock<float[]>>> inTuple) {
            final D3int ns = getNeighborSize();
            final List<TImgBlock<float[]>> inBlocks = IteratorUtils.toList(inTuple._2().iterator());
            final TImgBlock<float[]> templateBlock = inBlocks.get(0);
            final D3int blockSize = templateBlock.getDim();
            final BaseTIPLPluginIn.morphKernel mKernel = getKernel();
            // the output image
            final float[] outData = new float[templateBlock.get().length];
            // Make the output image first as kernels, then add the respective
            // points to it
            final BaseTIPLPluginIn.filterKernel[] curKernels = new BaseTIPLPluginIn.filterKernel[
                    (int) blockSize
                    .prod()];

            for (int ci = 0; ci < curKernels.length; ci++)
                curKernels[ci] = getImageKernel();

            for (final TImgBlock<float[]> cBlock : inBlocks) {
                final float[] curBlock = cBlock.get();

                // the offset of the current block
                final int offx = 0;
                final int offy = 0;
                final int offz = cBlock.getOffset().z;
                if (Math.abs(offz) <= ns.z) {
                    for (int yp = 0; yp < templateBlock.getDim().y; yp++) {
                        for (int xp = 0; xp < templateBlock.getDim().x; xp++) {
                            final int off = (yp) * blockSize.x + (xp);
                            final BaseTIPLPluginIn.filterKernel curKernel = curKernels[off];
                            // the offset position
                            final int ix = xp + offx;
                            final int iy = yp + offy;

                            // need to recalculate the bounds

                            final int start_x = Math.max(ix - ns.x, 0);
                            final int end_x = Math.min(ix + ns.x, blockSize.x);

                            final int start_y = Math.max(iy - ns.y, 0);
                            final int end_y = Math.min(iy + ns.y, blockSize.y);

                            // ox,oy,oz are the coordinates inside the second
                            // block
                            for (int oy = start_y; oy < end_y; oy++) {
                                for (int ox = start_x; ox < end_x; ox++) {
                                    final int off2 = (oy) * blockSize.x + (ox);
                                    if (mKernel.inside(off, off2, xp, ox, yp,
                                            oy, 0, offz)) {
                                        curKernel.addpt(xp, ox, yp, oy, 0,
                                                offz, curBlock[off2]);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < outData.length; i++)
                outData[i] = (int) curKernels[i].value();
            return new Tuple2<D3int, TImgBlock<float[]>>(inTuple._1(),
                    new TImgBlock<float[]>(outData, templateBlock.getPos(),
                            templateBlock.getDim()));
        }

    }
}
