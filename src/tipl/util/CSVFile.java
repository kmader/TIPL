package tipl.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Hashtable;

/** Simple generic tool for reading and parsing csv files */
public class CSVFile {
	private static String join(final CharSequence delimiter,
			final String[] elements) {
		final StringBuilder sb = new StringBuilder(4096); // 16 is most certanly
															// not enough
		boolean first = true;
		for (final String s : elements) {
			if (first) {
				first = false;
			} else {
				sb.append(delimiter);
			}
			sb.append(s);
		}
		return sb.toString();
	}

	public File file;
	public int row;
	public String rline = "";
	public String line = "";
	public String[] cline;
	public String filename;
	public int headerlines = 1;
	public BufferedReader bufRdr;
	public String[] header;
	public String[] rawHeader;
	public boolean isStrict; // All lines must be same length or read until one
								// is too short
	public boolean fileDone = true;

	/**
	 * Read in the csv file at path _filename with _headerlines number of lines
	 * of header, header will automatically be read upon creation
	 */
	public CSVFile(final String _filename, final int _headerlines) {
		filename = _filename;
		try {
			file = new File(_filename);
			bufRdr = new BufferedReader(new FileReader(file));
		} catch (final Exception e) {
			System.out.println("File cannot be opened? Does it exist");
			e.printStackTrace();
		}

		headerlines = _headerlines;
		readheader();
		isStrict = false;
		fileDone = false;
	}

	public void close() {
		try {
			bufRdr.close();
		} catch (final Exception e) {
			System.out.println("File cannot be closed? Already closed perhaps");
			e.printStackTrace();
		}

	}

	/**
	 * The main function of this tool is once a file has been opened, read each
	 * line and store the values for each line in a hash table mapped to their
	 * appropriate entries in the header
	 */
	public Hashtable<String, String> parseline() {
		final Hashtable<String, String> out = new Hashtable<String, String>();
		if (readline()) {
			if (isStrict)
				if (cline.length != header.length)
					return out;
			for (int i = 0; (i < cline.length) && (i < header.length); i++) {
				out.put(header[i], cline[i]);
			}
		}
		return out;
	}

	public void readheader() {
		rawHeader = new String[headerlines];
		for (int i = 0; i < headerlines; i++) {
			readline();
			rawHeader[i] = rline;
		}
		header = cline;
		for (int i = 0; i < header.length; i++) {
			header[i] = header[i].trim().toLowerCase(); // make everything
														// lowercase in the
														// header
		}
	}

	private boolean readline() {
		try {
			if ((rline = bufRdr.readLine()) != null) {
				line = join("", rline.split("//"));
				cline = line.split(",");
				row++;
				return true;
			} else {
				System.out.println("File finished");
				fileDone = true;
				return false;
			}
		} catch (final Exception e) {
			System.out.println("File cannot be Read? Does it exist");

			e.printStackTrace();
			fileDone = true;
			return false;

		}
	}
}