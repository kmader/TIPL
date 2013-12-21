package tipl.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedList;


/** Simple generic tool for reading and parsing CSV files 
 * */
public class CSVFile {
	public static class ColumnData {
		LinkedList<String> colEntries=new LinkedList<String>();
		public void add(String item) { colEntries.add(item);}
	}
	public Hashtable<String,ColumnData> ReadColumns(int numberOfLines) {
		Hashtable<String,ColumnData> out=new Hashtable<String,ColumnData>();
		if(numberOfLines==0) return out;
		int rowNo=0;
		boolean stillRunning=true;
		while(stillRunning) {
			Hashtable<String,String> curRow=lineAsDictionary();
			for(String cKey : curRow.keySet()) {
				String cVal=curRow.get(cKey);
				if(!out.containsKey(cKey)) out.put(cKey, new ColumnData());
				out.get(cKey).add(cVal);
			}
			rowNo++;
			if((numberOfLines>0) && (rowNo>=numberOfLines)) stillRunning=false;
			if(fileDone) stillRunning=false;
		}
		return out;
	}
	/**
	 * joining an array of strings with a delimiter
	 * @param delimiter string to use to combine them
	 * @param elements array of strings
	 * @return a single string with the delimiter
	 */
	public static String join(final CharSequence delimiter,
			final String[] elements) {
		final StringBuilder sb = new StringBuilder(4096); // 16 is most certainly
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

	private int row;
	private int headerlines = 1;
	private final CanReadLines bufRdr;
	/**
	 * header is the formatted list of column names that is the last line of the rawHeader (headerlines)
	 */
	private String[] header;
	public String[] getHeader() {return header;}
	/**
	 * rawHeader are the lines leading up to the start of the data including the header
	 * 
	 */
	private ParseableLine[] rawHeader;
	public String getRawHeader(int i) {return rawHeader[i].getLine();}
	public boolean isStrict; // All lines must be same length or read until one
								// is too short
	public boolean fileDone = true;
	public String delimiter=",";
	/**
	 * This interface and the subsequent classes make testing and running with artificial data easier
	 * @author mader
	 *
	 */
	protected static interface CanReadLines {
		public String readLine() throws IOException;	
	}
	private final static class BufReadWrap implements CanReadLines,Closeable {
		private final BufferedReader myBuf;
		public BufReadWrap(BufferedReader inBuf) {
			myBuf=inBuf;
		}
		public String readLine() throws IOException { return myBuf.readLine();}
		public void close() throws IOException { myBuf.close();}
	}
	private final static class StrReader implements CanReadLines {
		private final String[] myString;
		private int curRow=0;
		public StrReader(String[] inString) {
			myString=inString;
		}
		public String readLine() throws IOException {
			if(curRow<myString.length) {
				curRow++;
				return myString[curRow-1];
			} else throw new IOException("You have read past my "+this+" last line ("+myString.length+")");
		}
	}
	/**
	 * Read in the csv file at path _filename with _headerlines number of lines
	 * of header, header will automatically be read upon creation
	 * @param _filename file to read
	 * @param _headerlines number of header lines
	 */
	public static CSVFile FromPath(final String _filename, final int _headerlines) {
		String filename = _filename;
		try {
			File file = new File(_filename);
			return new CSVFile(new BufReadWrap(new BufferedReader(new FileReader(file))),_headerlines);
		} catch (final Exception e) {
			System.out.println("File cannot be opened? Does it exist");
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
		
	}
	public static CSVFile FromString(final String[] inString,final int _headerlines) {
		return new CSVFile(new StrReader(inString),_headerlines);
	}
	private CSVFile(final CanReadLines readableObj,final int _headerlines) {
		bufRdr=readableObj;
		headerlines = _headerlines;
		readHeader();
		isStrict = false;
		fileDone = false;
	}

	public void close() {
		try {
			if (bufRdr instanceof Closeable) ((Closeable) bufRdr).close();
		} catch (final Exception e) {
			System.out.println("File cannot be closed? Already closed perhaps");
			e.printStackTrace();
		}

	}

	/**
	 * The main function of this tool is once a file has been opened, read each
	 * line and store the values for each line in a hash table mapped to their
	 * appropriate entries in the header
	 *
	 * @return
	 */
	public Hashtable<String, String> lineAsDictionary() {
		ParseableLine cline=readLine();
		return ZipArrays(getHeader(),cline.getSplitLine(),isStrict);
	}
	/**
	 * Zips two string arrays together appending columns or values if necessary
	 * @param headerElements
	 * @param inline
	 * @param isStrict
	 * @return
	 */
	public static Hashtable<String, String> ZipArrays(String[] headerElements,String[] inline,boolean isStrict) {
		final Hashtable<String, String> out = new Hashtable<String, String>();
		if (isStrict)
			if (inline.length != headerElements.length)
				return out;
		final int runLength;
		if (inline.length>=headerElements.length) runLength=inline.length;
		else runLength=headerElements.length;
		
		for (int i = 0; i<runLength; i++) {
			final String curHead;
			final String curVal;
			if(i>=headerElements.length) curHead="Extra Column:"+(i-headerElements.length);
			else curHead=headerElements[i];
			if(i>=inline.length) curVal="Empty Value:"+(i-inline.length);
			else curVal=inline[i];
			out.put(curHead, curVal);
		}
		return out;
		
	}

	protected void readHeader() {
		rawHeader = new ParseableLine[headerlines];
		for (int i = 0; i < headerlines; i++) {
			rawHeader[i] = readLine();
		}
	}
	
	/**
	 * class to store a single line and parse it if needed also caches the results
	 * @author mader
	 *
	 */
	public static class ParseableLine {
		private static String[] lineSplit(final String rawstring,final String delim) {
			return join("", rawstring.split("//")).split(delim);
		}
		private final String line;
		private final String delim;
		public ParseableLine(String inline,String indelim) {
			line=inline;
			delim=indelim;
		}
		public String getLine() {return line;}
		private String[] splitLine=null;
		public String[] getSplitLine() {
			if (splitLine==null) splitLine=lineSplit(line,delim);
			return splitLine;
		}
		private String[] cleanedLine=null;
		public String[] getCleanedLine() {
			if (cleanedLine==null) {
				cleanedLine=getSplitLine();
				for (int i = 0; i < cleanedLine.length; i++) {
					cleanedLine[i] = cleanedLine[i].trim().toLowerCase(); // make everything
																// lowercase in the
																// header
				}
			}
			return cleanedLine;
			
		}
	}
	public static final ParseableLine emptyList=new ParseableLine("",",");
	
	public ParseableLine readLine() {
		String rline;
		try {
			if ((rline = bufRdr.readLine()) != null) {
				row++;
				return new ParseableLine(rline,delimiter);
			} else {
				fileDone = true;
				return emptyList;
			}
		} catch (final Exception e) {
			System.out.println("File cannot be Read? Does it exist");
			e.printStackTrace();
			fileDone = true;
			return emptyList;
		}
	}
}