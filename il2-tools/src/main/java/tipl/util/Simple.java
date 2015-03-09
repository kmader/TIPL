package tipl.util;


import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;

import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.FloatType;

import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class Simple
{
    /**
     * Compute the partial derivative of source in a particular dimension.
     *
     * @param source
     *            source image, has to provide valid data in the interval of the
     *            gradient image plus a one pixel border in dimension.
     * @param target
     *            output image, the partial derivative of source in the
     *            specified dimension.
     * @param dimension
     *            along which dimension the partial derivatives are computed
     */
    public static < T extends NumericType< T > > void gradient(
            final RandomAccessible< T > source,
            final RandomAccessibleInterval< T > target,
            final int dimension )
    {
        // TODO: complete the gradient() method
        // 1.) Define a view that takes an interval out of source that
        //     corresponds to the target interval shifted by 1 in dimension.
        //     Hint: Use Intervals.translate() and Views.interval().
        // 2.) Make the view flatIterable() and obtain a cursor().
        // 3.) Likewise get a cursor from a view shifted by -1 in dimension.
        // 4.) Iterate both views and target simultaneously to compute the
        //     gradient at every pixel.
    }

    public static void main( final String[] args ) throws ImgIOException
    {
        final FloatType type = new FloatType();
        final ArrayImgFactory< FloatType > factory = new ArrayImgFactory< FloatType >();

        final Img< FloatType > input = new ImgOpener().openImg(
                "/Users/mader/Dropbox/WorkRelated/worm3d/bgimg.tif", factory, type);
       // ImageJFunctions.show( input );
        System.out.println("Current Image:"+input+", with factory:"+factory);
        /** new InteractiveViewer2D((int) input.dimension(0),(int) input.dimension(1),
                input,new net.imglib2.converter.RealARGBConverter(0,100));
         **/
        final Img< FloatType > dX = factory.create( input, new FloatType() );

        gradient( Views.extendBorder( input ), dX, 0 );
      //  ImageJFunctions.show( dX, "gradient X" );
        /**
        new InteractiveViewer2D((int) input.dimension(0),(int) input.dimension(1),
                dX,new net.imglib2.converter.RealARGBConverter(0,100));
         **/

        // TODO:
        // Compute and show the gradient of input in Y direction
    }

}
