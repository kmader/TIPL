package tipl.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TomcatDB {
	public static void main(String[] args) {
		final TomcatDB myDB = new TomcatDB();
		System.out.println(" Kevin's SLS DUO Database Interface v" + myDB.kVer);

		// double r=5.6;
		// System.out.println(" Kevin's SLS Tomcat Database Interface "+((int)
		// r%2));

		if (args[0].compareTo("test") == 0) {
			myDB.UpdateRecord("TEST", "ME", "H");
		} else {
			if (args.length < 4) {
				myDB.Help();
				return;
			}
			String prior = "N";
			String nName = "";
			final int nLgbook = Integer.parseInt(args[3]);

			System.out.println("username: " + args[1]);
			System.out.println("password: " + args[2]);
			System.out.println("log book id: " + nLgbook);

			if (args[1].compareTo("peteypablo") != 0) {
				myDB.username = args[1];
				myDB.password = args[2];
				myDB.logbookId = nLgbook;
			}

			if (args[0].compareTo("SEARCH") == 0) {
				System.out.println(myDB.Search(args[4], false, true));
			} else if (args[0].compareTo("VIEW") == 0) {
				myDB.ShowResult(Integer.parseInt(args[4]));
			} else if (args[0].compareTo("CREATE") == 0) {
				nName = myDB.VMSCleanName(args[4]);
				System.out.println("name: " + nName);
				if (args.length == 6)
					prior = args[6];
				myDB.UpdateRecord(args[4], args[5], false, prior);
			} else if (args[0].compareTo("UPDATE") == 0) {
				nName = myDB.VMSCleanName(args[4]);
				System.out.println("name: " + nName);
				if (args.length == 6)
					prior = args[6];
				myDB.UpdateRecord(args[4], args[5], true, prior);
			}
		}
	}

	String addr;
	String username;
	String password;
	String combAddr;
	int logbookId;
	public String kVer = "001_090906";

	public TomcatDB() {
		addr = "http://duo.psi.ch/public/logbook_ext.php";
		username = "AAVTH9066B13409799AAD7F39107FB262D226";
		password = "";
		combAddr = "";
		logbookId = 57;

	}

	public int CheckResults(int[] LogIds) {
		int output = 0;
		for (final int cResult : LogIds)
			if (cResult > -1)
				output++;
		return output;
	}

	public void CreateEntry(String title, String text) {
		CreateEntry(title, text, "N");
	}

	public void CreateEntry(String title, String text, String priority) {
		try {
			final SimpleDateFormat tomcatdate = new SimpleDateFormat(
					"dd/MM/yyyy H:m");
			final String myQuery = "ADD&LBID=" + logbookId + "&DATE="
					+ URLEncoder.encode(tomcatdate.format(new Date()), "UTF-8")
					+ "&PRIORITY=" + priority + "&TITLE="
					+ URLEncoder.encode(title, "UTF-8") + "&LOGENTRY="
					+ URLEncoder.encode(text, "UTF-8");
			ExecCommand("", myQuery);
		} catch (final Exception e) {
			System.out.println("!!!Create Entry Failed");
			e.printStackTrace();
		}
	}

	public void DeleteEntry(int LogId) {
		final String[] bloed = ExecCommand("DEL&LOGID=" + LogId, "");
		for (final String mud : bloed)
			System.out.println(mud);
	}

	public void EditEntry(int LogId, String text) {
		EditEntry(LogId, text, "N");
	}

	public void EditEntry(int LogId, String text, String priority) {
		final String[] probStr = ExecCommand("VIEW&LOGID=" + LogId, "");
		DeleteEntry(LogId);
		CreateEntry(probStr[4], probStr[5] + text, priority);
	}

	public String[] ExecCommand(String action, String poschti) {
		String output = "";
		String postContent = poschti;
		new URLDecoder();
		try {
			if (action.length() == 0) {
				combAddr = addr;
				postContent = "USER=" + username + "&PASSWORD=" + password
						+ "&ACTION=" + poschti;
			} else {
				combAddr = addr + "?USER=" + username + "&PASSWORD=" + password
						+ "&ACTION=" + action;
			}
			final URL url = new URL(combAddr);
			final HttpURLConnection conn = (HttpURLConnection) url
					.openConnection();

			if (postContent.length() > 0) {
				conn.setDoOutput(true);
				final OutputStreamWriter out = new OutputStreamWriter(
						conn.getOutputStream());
				out.write(postContent);
				out.close();
			} else {
				conn.connect();
			}
			final InputStream in = conn.getInputStream();
			final BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));
			String text;

			while ((text = reader.readLine()) != null) {
				// System.out.println(text);
				output += "&@&" + URLDecoder.decode(text);
			}
			conn.disconnect();
		} catch (final Exception ex) {
			ex.printStackTrace();
			System.out.println("DUO Query Not Executed");
		}
		final String[] outputarr = output.split("&@&");
		return outputarr;
	}

	public void Help() {
		System.out.println("Usage : ");
		System.out
				.println("TomcatDB cmd, username, password, logbookid, [name/number], [info], [priority] ");
		System.out.println("Command Choices (cmd) :");
		System.out.println("\tSEARCH\t Search for an entry given name");
		System.out.println("\tVIEW\t View an entry given a number/record-id");
		System.out.println("\tCREATE\t Create a new entry with the given name");
		System.out.println("\tUPDATE\t Update a record with the given name");
	}

	public int[] Search(String searchstr) {
		return Search(searchstr, false);
	}

	public int[] Search(String searchstr, boolean isStrict) {
		return Search(searchstr, isStrict, false);
	}

	public int[] Search(String searchstr, boolean isStrict, boolean verbose) {
		// URLEncoder bob=new URLEncoder();
		String myStr;
		try {
			myStr = URLEncoder.encode(searchstr, "UTF-8");
		} catch (final Exception e) {
			System.out.println("String Encoding Failed for Search");
			myStr = searchstr;
		}
		final String[] probStr = ExecCommand("SEARCH&SEARCH=" + myStr
				+ "&LBID=" + logbookId, "");
		String[] curEleSplit;

		int curEleDex = 0;
		final int[] output = new int[probStr.length];
		for (final String curEle : probStr) {
			if (verbose)
				System.out.println("-------");
			if (curEle.length() > 0) {
				curEleSplit = curEle.split(",");
				if (verbose)
					for (final String curEle2 : curEleSplit)
						System.out.println("\t" + curEle2);

				if (isStrict) {
					if (myStr.compareTo(curEleSplit[4]) == 0) {
						// Exact Match
						output[curEleDex] = Integer.parseInt(curEleSplit[0]);
					} else {
						output[curEleDex] = -3;
					}
				} else {
					output[curEleDex] = Integer.parseInt(curEleSplit[0]);
				}
			} else {
				output[curEleDex] = -1;
			}
			curEleDex++;
		}
		return output;
	}

	public String[] ShowResult(int LogId) {
		return ShowResult(LogId, true);
	}

	public String[] ShowResult(int LogId, boolean printAlles) {
		if (LogId > 0) {
			final String[] probStr = ExecCommand("VIEW&LOGID=" + LogId, "");
			int cDex = 0;
			for (final String curEle : probStr) {
				if (printAlles)
					System.out.println("[" + cDex + "]=" + curEle);
				cDex++;
			}
			return probStr;
		} else {
			System.out.println("Invalid Log - Id");
			return null;
		}
	}

	public void UpdateRecord(String searchName, String newText) {
		UpdateRecord(searchName, newText, true);
	}

	public void UpdateRecord(String searchName, String newText,
			boolean preserveOld) {
		UpdateRecord(searchName, newText, preserveOld, "N");
	}

	public void UpdateRecord(String searchName, String newText,
			boolean preserveOld, String priority) {
		try {
			final int[] sresults = Search(searchName, true);
			final int nresults = CheckResults(sresults);
			System.out.println("Number of Records Found : " + nresults);
			String output[];
			boolean haveEdited = false;
			for (final int cResult : sresults) {
				if (cResult > 0) {
					// System.out.println(cResult);
					output = ShowResult(cResult, false);
					// System.out.println(searchName.compareTo(output[4]));
					if (searchName.compareTo(output[4]) == 0) {
						if (haveEdited) {
							DeleteEntry(cResult);
						} else {
							if (preserveOld) {
								EditEntry(cResult, newText, priority);
								haveEdited = true;
							} else {
								DeleteEntry(cResult);
							}
						}
					}
				}

			}
			if (haveEdited == false)
				CreateEntry(searchName, searchName + "\n" + newText, priority);
		} catch (final Exception e) {
			System.out.println("!!! Record not successfully updated");
			e.printStackTrace();
		}
	}

	// public void AddFile(int LogId,String FileName,String
	public void UpdateRecord(String searchName, String newText, String priority) {
		UpdateRecord(searchName, newText, true, priority);
	}

	public String VMSCleanName(String filename) {
		// Cleans out the garbage in the filename
		String outname = filename.toUpperCase();
		int startVal = outname.lastIndexOf('/');
		if (startVal > 0)
			outname = outname.substring(startVal + 1);
		final int endVal = outname.lastIndexOf('.');
		if (endVal > 0)
			outname = outname.substring(0, endVal);

		startVal = outname.lastIndexOf(']');
		if (startVal > 0)
			outname = outname.substring(startVal + 1);
		outname = outname.replaceAll("_STATS_J", "");
		outname = outname.replaceAll("_STATS_", "");
		outname = outname.replaceAll("_STATS", "");
		outname = outname.replaceAll("STATS_", "");
		outname = outname.replaceAll("_LACPA_", "");
		outname = outname.replaceAll("_LACPA", "");
		outname = outname.replaceAll("LACPA_", "");
		outname = outname.replaceAll("_LACSH_", "");
		outname = outname.replaceAll("_LACSH", "");
		outname = outname.replaceAll("LACSH_", "");
		return outname;
	}

}