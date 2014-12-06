/**
 * 
 */
package tipl.formats;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
/*
 * Special imports to allow for these annotations of file format loaders
 */
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

import org.scijava.annotations.Index;
import org.scijava.annotations.IndexItem;
import org.scijava.annotations.Indexable;

import tipl.util.ArgumentParser;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;
import tipl.util.TypedPath;
import tipl.util.TypedPath.PathFilter;

// Logging
/**
 * Abstract Class Designed for reading a folder full of images
 * 
 * @author maderk
 * 
 */
public abstract class DirectoryReader implements TReader {
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.SOURCE)
	@Indexable
	@Deprecated
	public static @interface DReader {
		String name();
		String desc() default "";
	}



	public static abstract class DRFactory implements ImgFactory {

		abstract public DirectoryReader get(TypedPath path);

		abstract public TReader.TSliceReader getSliceReader(TypedPath slice);

		/**
		 *
		 * @return a list of all the files in the given directory which match the critera
		 */
		abstract public TypedPath.PathFilter getFilter();

		/**
		 *
		 * @param path the directory to check
		 * @return true if a single image is kept
		 */
		@Override
		public boolean matchesPath(TypedPath path) {
			final TypedPath.PathFilter cPF = getFilter();
			if (path.listFiles(cPF).length>0) return true;
			else return false;
		}
	}

	
	public static HashMap<PathFilter, DRFactory> getAllFactories()
			throws InstantiationException {
		final HashMap<PathFilter, DRFactory> current = new HashMap<PathFilter, DRFactory>();

        for (Iterator<IndexItem<DReader>> cIter = Index.load(DReader.class).iterator(); cIter.hasNext(); ) {
            final IndexItem<DReader> item = cIter.next();

            final DReader bName = item.annotation();

            try {

                final DRFactory dBlock = (DRFactory) Class.forName(item.className()).newInstance();
                final TypedPath.PathFilter f = dBlock.getFilter();
                System.out.println(bName + " loaded as: " + dBlock);
                current.put(f, dBlock);
                System.out.println(item.annotation().name() + " loaded as: " + dBlock);
            } catch (InstantiationException e) {
                System.err.println(DirectoryReader.class.getSimpleName()+": " + bName.name() + " could not be loaded or instantiated by plugin manager!\t" + e);
                if (TIPLGlobal.getDebug()) e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.err.println(DirectoryReader.class.getSimpleName()+": " + bName.name()+ " could not be found by plugin manager!\t" + e);
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                System.err.println(DirectoryReader.class.getSimpleName()+": " + bName.name() + " was accessed illegally by plugin manager!\t" + e);
                e.printStackTrace();
            }
        }
		return current;
	}

	final static String version = "04-12-2014";


	/**
	 * ChooseBest chooses the directory reader plugin which has the highest
	 * number of matches in the given directory using the PathFilter
	 * 
	 * @param path
	 *            folder path name
	 * @return best suited directory reader
	 */
	public static DirectoryReader ChooseBest(final TypedPath path) {
		HashMap<PathFilter, DRFactory> allFacts;
		try {
			allFacts = getAllFactories();
		} catch (final InstantiationException e) {
			e.printStackTrace();
			throw new IllegalStateException(
					"No Appropriate Plugins Have Been Loaded for DirectoryReader");

		}
		System.out.println("Loaded DirectoryReader Plugins:");
		PathFilter bestFilter = null;
		int bestLen = 0;
		for (final PathFilter cFilter : allFacts.keySet()) {
			final int zlen = FilterCount(path, cFilter);
			if (zlen > bestLen) {
				bestFilter = cFilter;
				bestLen = zlen;
			}
		}
		if (bestLen < 1)
			throw new IllegalStateException(
					"No Appropriate Plugins Have Been Loaded for DirectoryReader");
		return allFacts.get(bestFilter).get(path);
	}

	/**
	 * Evaluates each directory reader plugin on a given directory and reports
	 * the number of matches for each filter. The filter with the most matches
	 * is then returned or an exception is thrown
	 **/
	public static int FilterCount(final TypedPath path, final PathFilter cFilter) {
		final TypedPath[] imglist = path.listFiles(cFilter);
		final int zlen = imglist.length;
		String tempName = "None";
		if (zlen > 0)
			tempName = imglist[0].getPath();
		System.out.println("Filter:" + cFilter + "\t FileCount:" + zlen
				+ "\t FirstFile:" + tempName);
		return zlen;
	}



	public static void main(final ArgumentParser p) {
		System.out.println("DirectoryReader Tool v" + version);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final TypedPath inputFile = p.getOptionPath("input", "",
				"Directory to Convert");
		final TypedPath outputFile = p.getOptionPath("output", "test.tif",
				"Output File");
		try {
			final DirectoryReader cdirReader = ChooseBest(inputFile);
			TImgTools.WriteTImg(cdirReader.getImage(), outputFile);
		} catch (final Exception e) {
			System.out.println("Error converting or reading slice");
			e.printStackTrace();
		}

	}

	public static void main(final String[] args) {
		main(TIPLGlobal.activeParser(args));
	}

	protected TypedPath[] imglist;
	protected D3int dim;
	protected D3int pos = new D3int(0, 0, 0);
	protected D3float elSize = new D3float(1, 1, 1);
	protected D3int offset = new D3int(0, 0, 0);
	protected float ShortScaleFactor = 1.0f;
	private final String procLog = "";
	final private TypedPath dirPath;

	final private TSliceFactory tsf;
	private final boolean signedValue = true;

	private int imageType=-1;


	public DirectoryReader(final TypedPath path, final TypedPath.PathFilter filter,
			final TSliceFactory itsf) throws IOException {
		dirPath = path;

		imglist = path.listFiles(filter);
		final int zlen = imglist.length;
		dim = new D3int(-1, -1, zlen);
		// Sort the list of filenames because some operating systems do not
		// handle this automatically
		Arrays.sort(imglist, new Comparator<TypedPath>() {
			@Override
			public int compare(final TypedPath f1, final TypedPath f2) {
				return f1.getPath().compareTo(f2.getPath());
			}
		});

		if (zlen < 1)
			throw new IOException("Directory has no Files : " + path);
		tsf = itsf;
		ReadHeader();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getDim()
	 */
	@Override
	public D3int getDim() {
		return dim;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getElSize()
	 */
	@Override
	public D3float getElSize() {
		return elSize;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TReader#getImage()
	 */
	@Override
	public TImg getImage() {
		return new TReaderImg(this);
	}

	@Override
	public int getImageType() {
		return imageType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getImageType()
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getPath()
	 */
	public TypedPath getPath() {
		return dirPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getPos()
	 */
	@Override
	public D3int getPos() {
		return pos;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getProcLog()
	 */
	@Override
	public String getProcLog() {
		return procLog;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getShortScaleFactor()
	 */
	@Override
	public float getShortScaleFactor() {
		return ShortScaleFactor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#getSigned()
	 */
	@Override
	public boolean getSigned() {
		return signedValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TReader#isParallel()
	 */
	@Override
	public boolean isParallel() {
		// TODO Auto-generated method stub
		return true;
	}

	abstract public void ParseFirstHeader();

	@Override
	abstract public String readerName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TReader#ReadHeader()
	 */
	@Override
	public void ReadHeader() {
		ParseFirstHeader();
		TypedPath firstFile=imglist[0];
		if (!firstFile.exists()) throw new IllegalArgumentException(this+":First file is not " +
				"even found!!");
		try {
			final TSliceReader tsr = tsf.ReadFile(firstFile);
			final D3int cDim = tsr.getDim();
			cDim.z = imglist.length;
			setDim(cDim);
			setElSize(tsr.getElSize());
			setOffset(tsr.getOffset());
			setPos(tsr.getPos());
			setImageType(tsr.getImageType());

			System.out.println("DirectoryReader [" + readerName()
					+ "] : Sample: " + getPath() + " Timg has been selected");
		} catch (final Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Error Reading header from " + tsf + " of "
					+ imglist[0]);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TReader#ReadSlice(int) Reads a given slice using the
	 * factory function to read the filename
	 */
	@Override
	public TReader.TSliceReader ReadSlice(final int slice) throws IOException {
		if (slice >= imglist.length) {
			throw new IOException("Exceeds bound!!!" + slice + " of "
					+ imglist.length);
		}
		return tsf.ReadFile(imglist[slice]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setDim()
	 */
	protected void setDim(final D3int inDim) {
		dim = inDim;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setElSize()
	 */
	protected void setElSize(final D3float inDim) {
		elSize = inDim;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setOffset()
	 */
	protected void setOffset(final D3int inDim) {
		offset = inDim;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setPos()
	 */
	protected void setPos(final D3int inDim) {
		pos = inDim;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TImg#setImageType()
	 */
	protected void setImageType(final int inIT) {
		imageType = inIT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tipl.formats.TReader#SetupReader(java.lang.String)
	 */
	@Override
	abstract public void SetupReader(final TypedPath inPath);

}
