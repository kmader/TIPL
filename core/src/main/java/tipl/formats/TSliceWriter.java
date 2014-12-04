package tipl.formats;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Iterator;

import org.scijava.annotations.Index;
import org.scijava.annotations.IndexItem;
import org.scijava.annotations.Indexable;

import tipl.util.*;

/**
 * Interface for writing TImg files to a data source on a slice by slice basis
 * An important decision has been taken here to not force the type using generics
 * This should make it easier to save files as they are. 
 * TODO Ultimately there is probably a better solution, but I can't think of it at the moment
 * @author mader
 *
 */
import tipl.util.TImgTools.*;

public interface TSliceWriter extends Serializable {
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.SOURCE)
	@Indexable
	public static @interface DWriter {
		String name();
		int[] supportedTypes();
		String type() default "";
	}

	public static abstract interface DWFactory {
		/**
		 * Get the slicewriter
		 * @param imageData the image data (just need the header information)
		 * @param path the output folder to write to 
		 * @param imgType the type of image to write (default -1)
		 * @return the instance of the writer, after setup has been run and header written
		 */
		public TSliceWriter get(TImgRO imageData,TypedPath path,int imgType);
	}
	/**
	 * Since TSliceWriter is an interface and since this isnt java8 I need a subclass to have static functions
	 * @author mader
	 */
	abstract public static class Writers {
		public static HashMap<String, DWFactory> getAllFactories()
				throws InstantiationException {
			final HashMap<String, DWFactory> current = new HashMap<String, DWFactory>();


            for (Iterator<IndexItem<DWriter>> cIter = Index.load(DWriter.class).iterator(); cIter.hasNext(); ) {
                final IndexItem<DWriter> item = cIter.next();

                final DWriter bName = item.annotation();

                try {

                    final DWFactory dBlock = (DWFactory) Class.forName(item.className()).newInstance();

                    System.out.println(bName + " loaded as: " + dBlock);
                    current.put(item.annotation().type(), dBlock);
                    System.out.println(item.annotation().name() + " loaded as: " + dBlock);
                } catch (InstantiationException e) {
                    System.err.println(TSliceWriter.class.getSimpleName()+": " + bName.name() + " could not be loaded or instantiated by plugin manager!\t" + e);
                    if (TIPLGlobal.getDebug()) e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    System.err.println(TSliceWriter.class.getSimpleName()+": " + bName.name()+ " could not be found by plugin manager!\t" + e);
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    System.err.println(TSliceWriter.class.getSimpleName()+": " + bName.name() + " was accessed illegally by plugin manager!\t" + e);
                    e.printStackTrace();
                }
            }
            return current;
		}
		/**
		 * ChooseBest chooses the directory reader plugin which has the highest
		 * number of matches in the given directory using the PathFilter
		 * 
		 * @param path
		 *            folder path name
		 * @return best suited directory reader
		 */
		public static TSliceWriter ChooseBest(final TImgRO outImage,final TypedPath path,int imgType) {
			HashMap<String, DWFactory> allFacts;
			try {
				allFacts = getAllFactories();
			} catch (final InstantiationException e) {
				e.printStackTrace();
				throw new IllegalStateException(
						"No Appropriate Plugins Have Been Loaded for "+TSliceWriter.class.getName());

			}
			System.out.println("Loaded "+TSliceWriter.class.getName()+" Plugins:");
			for(String cFilter: allFacts.keySet()) return allFacts.get(cFilter).get(outImage,path,imgType);
			throw new IllegalArgumentException("No matching filters found:"+path);
		}

		/**
		 * A simple function to write
		 * @param tsw the initialized writer tool
		 * @param inImage the image to be written
		 */
		public static void SimpleWrite(final TSliceWriter tsw,final TImgRO inImage, final int
				imageType) {
			for(int sliceNo=0;sliceNo<inImage.getDim().z;sliceNo++) {
				final D3int cpos = new D3int(inImage.getPos().x,inImage.getPos().y,inImage.getPos
						().z+sliceNo);
				tsw.WriteSlice(

						new TImgSlice.TImgSliceFromImage(inImage,sliceNo,imageType,cpos,inImage
								.getDim(),D3int.zero)
						,sliceNo);
			}

		}
	}

	/** The command to initialize the writer */
	public void SetupWriter(TImgRO imgToSave, TypedPath outputPath, int outType);

	/** write just the header */
	public void WriteHeader();

	/** The name of the writer, used for menus and logging */
	public String writerName();

	/**
	 * write the given slice data to a specific slice
	 * @param outSlice the data to write
	 * @param outSlicePosition the position to write it too
	 */
	public void WriteSlice(TImgSlice outSlice,int outSlicePosition);
}