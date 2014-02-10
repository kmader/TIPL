package tipl.formats;


/** Interface for writing TImg files to a data source */
public interface TWriter {
	/**
	 * Can the writer be run in parallel (multiple threads processing different
	 * slices
	 */
	public boolean isParallel();

	/** The command to initialize the writer */
	public void SetupWriter(TImg inputImage, String outputPath);

	/** Write everything (header and all slices) */
	public void Write();

	/** write just the header */
	public void WriteHeader();

	/** The name of the writer, used for menus and logging */
	public String writerName();

	/**
	 * write the given slice
	 * @param outSlicePosition the position to write it too
	 */
	public void WriteSlice(int outSlicePosition);
}