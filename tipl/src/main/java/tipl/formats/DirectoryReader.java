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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import net.java.sezpoz.Index;
import net.java.sezpoz.IndexItem;
import net.java.sezpoz.Indexable;
import tipl.util.ArgumentParser;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;
import tipl.util.TypedPath;

// Logging
/**
 * Abstract Class Designed for reading a folder full of images
 * 
 * @author maderk
 * 
 */
public abstract class DirectoryReader implements TReader {
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.SOURCE)
	@Indexable(type = DRFactory.class)
	public static @interface DReader {
		String name();
	}

	public static abstract interface DRFactory {
		public DirectoryReader get(TypedPath path);

		public FileFilter getFilter();
	}
	
	public static HashMap<FileFilter, DRFactory> getAllFactories()
			throws InstantiationException {
		final HashMap<FileFilter, DRFactory> current = new HashMap<FileFilter, DRFactory>();

		for (final IndexItem<DReader, DRFactory> item : Index.load(
				DReader.class, DRFactory.class)) {
			final FileFilter f = item.instance().getFilter();
			final DRFactory d = item.instance();
			System.out.println(item.annotation().name() + " loaded as: " + d);
			current.put(f, d);
		}
		return current;
	}

	final static String version = "28-08-2014";


	/**
	 * ChooseBest chooses the directory reader plugin which has the highest
	 * number of matches in the given directory using the FileFilter
	 * 
	 * @param path
	 *            folder path name
	 * @return best suited directory reader
	 */
	public static DirectoryReader ChooseBest(final TypedPath path) {
		HashMap<FileFilter, DRFactory> allFacts;
		try {
			allFacts = getAllFactories();
		} catch (final InstantiationException e) {
			e.printStackTrace();
			throw new IllegalStateException(
					"No Appropriate Plugins Have Been Loaded for DirectoryReader");

		}
		System.out.println("Loaded DirectoryReader Plugins:");
		FileFilter bestFilter = null;
		int bestLen = 0;
		for (final FileFilter cFilter : allFacts.keySet()) {
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
	public static int FilterCount(final TypedPath path, final FileFilter cFilter) {
		final File dir = new File(path.getPath());
		final File[] imglist = dir.listFiles(cFilter);
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

	protected File[] imglist;
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


	public DirectoryReader(final TypedPath path, final FileFilter filter,
			final TSliceFactory itsf) throws IOException {
		dirPath = path;
		final File dir = new File(path.getPath());
		imglist = dir.listFiles(filter);
		final int zlen = imglist.length;
		dim = new D3int(-1, -1, zlen);
		// Sort the list of filenames because some operating systems do not
		// handle this automatically
		Arrays.sort(imglist, new Comparator<File>() {
			@Override
			public int compare(final File f1, final File f2) {
				return f1.getAbsolutePath().compareTo(f2.getAbsolutePath());
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
		// TODO Auto-generated method stub
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
		File firstFile=imglist[0];
		if (!firstFile.exists()) throw new IllegalArgumentException(this+":First file is not even found!!");
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
	public void SetupReader(final TypedPath inPath) {
		// TODO Auto-generated method stub

	}

}
