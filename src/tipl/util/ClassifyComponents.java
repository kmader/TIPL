package tipl.util;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;

class CCRule {
	public String lhs = "";
	public double rhs = 0.0;
	public int opr = -1;
	public boolean valid = false;
	private final String oprList[] = { "eq", "neq", "gt", "geq", "lt", "leq" };

	public CCRule(final String cond) {
		valid = false;
		final String[] curRule = cond.split(",");
		if (curRule.length == 3) {
			lhs = curRule[0];
			opr = parseOpr(curRule[1]);
			rhs = (new Float(curRule[2])).doubleValue();
			if (opr > 0)
				valid = true;
		}
	}

	public boolean Apply(final Hashtable<String, String> values) {
		if (valid) {
			if (values.containsKey(lhs)) {
				final double curVal = (new Float(values.get(lhs)))
						.doubleValue();
				if (opr == 0) {
					return (curVal == rhs);
				} else if (opr == 1) {
					return (curVal != rhs);
				} else if (opr == 2) {
					return (curVal > rhs);
				} else if (opr == 3) {
					return (curVal >= rhs);
				} else if (opr == 4) {
					return (curVal < rhs);
				} else if (opr == 5) {
					return (curVal <= rhs);
				} else {
					System.out.println("wtf mate?, that operation is too high "
							+ opr);
				}
			}
		}
		return false;
	}

	private int parseOpr(final String cOpr) {
		for (int i = 0; i < oprList.length; i++) {
			if (cOpr.compareToIgnoreCase(oprList[i]) == 0)
				return i;
		}
		return -1;
	}

	@Override
	public String toString() {
		return lhs + " is " + oprList[opr] + " than " + rhs;
	}
}

public class ClassifyComponents {

	/**
	 * Takes an Aim file and assembles components in a given list together
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		// TODO Auto-generated method stub
		// Arguments Component Labeled File (LACUN,CANLB,CANBK,..), CSV file,
		// Rules, ...
		final String kVer = "091022_012";

		System.out
				.println(" Classify Components based on Morphological Analysis v"
						+ kVer);
		final ArgumentParser p = new ArgumentParser(args);

		boolean debugMode = true;
		boolean useor = false;
		String mapName = ""; // Map is a needed parameter
		String csvName = ""; // CSV is a needed parameter
		String outName = "";
		String icolName = "";

		try {
			debugMode = p.hasOption("debug");
			useor = p.hasOption("useor");
			if (useor) {
				if (p.getOptionAsString("useor").trim().toLowerCase()
						.compareTo("false") == 0)
					useor = false;
				if (p.getOptionAsString("useor").trim().toLowerCase()
						.compareTo("0") == 0)
					useor = false;
			}
			p.hasOption("inter");
			mapName = p.getOptionAsString("map"); // Map is a needed parameter
			csvName = p.getOptionAsString("csv"); // CSV is a needed parameter
			outName = p.getOptionAsString("out"); // Map is a needed parameter
			icolName = p.getOptionAsString("index").trim().toLowerCase(); // Index
																			// Column
																			// is
																			// a
																			// needed
																			// parameter
			if (p.hasOption("?"))
				throw new Exception();
		} catch (final Exception e) {
			System.out.println(" ClassifyComponents Help");
			System.out
					.println(" Takes FullLacuna output and generates new images based on rules");
			System.out.println(" Arguments::");
			System.out.println("	-map   = Map aim with labels");
			System.out.println("	-csv   = CSV filename from analysis");
			System.out.println("	-out   = Output segmented image");
			System.out.println("	-index   = Index Column Name (lacuna_number)");

			System.out.println("	rules ...");
			System.out
					.println("	Rules are formatted as COLUMN,OPERATOR,VALUE and are separated by spaces");
			System.out
					.println("	COLUMN is a column from the CSV file produced by Lacuna shape analysis");
			System.out
					.println("	OPERATOR is a EQ (equals), NEQ (not equals), LT, LEQ, GT, GEQ (greater or equal)");
			System.out.println("	VALUE is a float value to compare column to");
			System.out.println("	");
			System.out
					.println("	The rules are by default ANDed together to only rows which pass all conditions");
			System.out.println("	");
			System.out.println("	");
			System.out.println("	-debug = debug mode enable");
			System.out.println("	-useor = use OR to join rules instead of AND");
			System.out.println("	-inter = run interactively");
			e.printStackTrace();
			System.exit(0);
		}

		System.out.println("Map Aim: " + mapName);
		System.out.println("Input CSV: " + csvName);
		System.out.println("Out Aim: " + outName);
		System.out.println("Index Col: " + icolName);
		System.out.println("Rules: " + p.ParamCount());
		System.out.println("Debug: " + debugMode);
		System.out.println("Use OR: " + useor);
		final Vector<CCRule> rules = new Vector<CCRule>();
		p.ParamZero();
		for (int i = 0; i < p.ParamCount(); i++) {
			final String cParam = p.nextParam().trim().toLowerCase();

			rules.addElement(new CCRule(cParam));
		}
		final File f = new File(csvName);

		if (f.exists()) {
			System.out.println(csvName + " is present, script will continue");
			final CSVFile cData = CSVFile.FromPath(csvName, 2);
			if (rules.size() < 1) {

				System.out.println("No Rules Given, Printing Header Elements:"
						+ cData.getHeader().length);
				for (final String s : cData.getHeader()) {
					System.out.println(s);
				}

				System.out.println("First Line:" + cData.lineAsDictionary());
			} else {
				for (final CCRule cRule : rules) {
					System.out.println("Rule : " + cRule);
				}
				String outIndex = "";
				int savingDex = 0;
				int totalDex = 0;

				while (!cData.fileDone) {
					final Hashtable<String, String> cLine = cData.lineAsDictionary();
					boolean result = true;
					if (useor)
						result = false;
					for (final CCRule cRule : rules) {
						final boolean curRes = cRule.Apply(cLine);
						if (useor)
							result = result || curRes;
						else
							result = result && curRes;
					}
					if (result) {
						if (debugMode)
							System.out.println("Debug :" + cLine);
						final String sString = cLine.get(icolName);
						final int sIndex = (new Float(sString)).intValue();
						if (savingDex == 0)
							outIndex = "" + sIndex;
						else
							outIndex += "," + sIndex;
						savingDex++;
					}
					totalDex++;
				}
				System.out.println("Using Indices : " + outIndex);
				System.out.println("Using " + savingDex + " of " + totalDex);
				final String[] canArgs = { outIndex, "127" };
				if (savingDex > 0) {
					String outLog = "";
					for (final CCRule cRule : rules) {
						outLog += cRule + "\n";
					}
					new CollectCanals(mapName, outName, canArgs, outLog);
					System.out.println("Checking for File...");
					final File fOut = new File(outName);
					if ((fOut.exists())) {
						System.out.println("File exists!");

					} else {
						System.out.println("ERROR : Output Aim is missing  -- "
								+ outName + " !!!");
					}

				} else {
					System.out.println("No Valid Results!");
				}
			}
		} else {
			System.out.println(csvName + " is missing, script will abort");
		}

	}

}
