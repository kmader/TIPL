package tipl.util;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Takes an Aim file and assembles components in a given list together
 * 
 * @author mader
 * 
 */
public class CollectCanals {
	public static void main(final String[] args) {
		final String[] canArgs = new String[args.length - 2];
		System.arraycopy(args, 2, canArgs, 0, canArgs.length);
		// for (int i=0;i<canArgs.length;i++)
		// System.out.println("Arg "+i+"="+canArgs[i]);

		final CollectCanals ccans = new CollectCanals();
		ccans.useQueue = true;
		ccans.RunCollection(args[0], args[1], canArgs, "");// ,false);
	}

	boolean useQueue = false;

	public CollectCanals() {
	}

	public CollectCanals(final String inFileRaw, final String outFileRaw,
			final String[] canArgs) { // ,boolean
		// runInQueueA)
		// {
		// TODO Auto-generated method stub
		// Arguments CANLB or BK file, Values in csv list, color, ...
		RunCollection(inFileRaw, outFileRaw, canArgs, "");

	}

	public CollectCanals(final String inFileRaw, final String outFileRaw,
			final String[] canArgs, final String ruleList) { // ,boolean
																// runInQueueA)
																// {
		// TODO Auto-generated method stub
		// Arguments CANLB or BK file, Values in csv list, color, ...
		RunCollection(inFileRaw, outFileRaw, canArgs, ruleList);

	}

	public void RunCollection(final String inFileRaw, final String outFileRaw,
			final String[] canArgs, final String ruleList) {
		final String inFile = inFileRaw.trim().toUpperCase();
		final String outFile = outFileRaw.trim().toUpperCase();
		FileOutputStream fos;
		final File f = new File(inFile);
		boolean areRules = false;
		if (ruleList.length() > 0)
			areRules = true;
		if (f.exists()) {
			System.out.println(inFile + " is present, script will continue");

			String execLine = "$ @DISK1:[KSM.JPL]JPL_INIT.COM \n";

			String iplCode = "$ JAVA_IPL_COMMAND \n\n";
			String cleanUpCode = "";
			execLine += "$ JAVA_IPL_COMMAND == \"$UM:XIPL_SCANCO_IBT_OPT.EXE\" \n$! Mainly Noninteractive IPL (canbe changed)\n";
			String batchName = outFile.trim().toUpperCase();
			String tempName;
			int loc = batchName.lastIndexOf(".");
			batchName = batchName.substring(0, loc);
			tempName = batchName;
			loc = batchName.lastIndexOf("]");
			batchName = batchName.substring(loc + 1);
			loc = batchName.lastIndexOf("/");
			batchName = batchName.substring(loc + 1);
			loc = batchName.lastIndexOf(")");
			batchName = batchName.substring(loc + 1);
			for (int i = 0; i < canArgs.length; i += 2) {
				final String colorVal = canArgs[i + 1];
				final String[] cans = canArgs[i].split(",");
				String curCLT = "";
				execLine += "$!\n$ CLCOLLECT " + inFile + " " + tempName + "_J"
						+ i + ".Aim " + tempName + "_J" + i + "_CLT.TXT";
				if (areRules) {
					execLine += " " + tempName + "_J" + i + "_RLS.TXT";
				}
				execLine += "\n$!\n";
				for (int j = 0; j < cans.length; j++) {
					final String canVal = cans[j];

					curCLT += canVal;
					if (j < cans.length - 1)
						curCLT += " ";
					else
						curCLT += "\n";
				}
				try {
					fos = new FileOutputStream(tempName + "_J" + i + "_CLT.TXT");
					fos.write(curCLT.getBytes());
					fos.flush();
					fos.close();
				} catch (final Exception e) {
					System.out.println("File :" + tempName + "_J" + i
							+ " could not be written\n");
				}
				if (areRules) {
					try {
						fos = new FileOutputStream(tempName + "_J" + i
								+ "_RLS.TXT");
						fos.write(ruleList.getBytes());
						fos.flush();
						fos.close();
					} catch (final Exception e) {
						System.out.println("Rules File :" + tempName + "_J" + i
								+ " could not be written\n");
					}
				}
				if (i == 0) {
					iplCode += "/read ina " + tempName + "_J" + i + ".Aim\n";
					iplCode += "/set ina " + colorVal + " 0\n";
				} else {
					iplCode += "/read inb " + tempName + "_J" + i + ".Aim\n";
					iplCode += "/set inb " + colorVal + " 0\n";
				}
				if (i > 0)
					iplCode += "/add ina inb out\n/rename out ina\n";
				cleanUpCode += "$ DELETE " + tempName + "_J" + i + ".Aim;*\n";
				cleanUpCode += "$ DELETE " + tempName + "_J" + i
						+ "_CLT.TXT;*\n";
				if (areRules)
					cleanUpCode += "$ DELETE " + tempName + "_J" + i
							+ "_RLS.TXT;*\n";
			}
			iplCode += "/write ina " + outFile + "\n\n/rename ina out\n\n..\n";
			execLine += iplCode;
			execLine += cleanUpCode;
			System.out.println(execLine);
			new ExecTask(execLine, "Jeval_" + batchName + ".com", useQueue);
		} else {
			System.out.println(inFile + " is missing, script will abort");
		}
	}

}
