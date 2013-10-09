package tipl.util;

import java.io.File;

/**
 * CheckFile is a tool used to see if a file is present and if it is missing
 * abort the current script. It is currently only implemented on the OpenVMS
 * operating system.
 **/
public class CheckFile {

	/**
	 * main is the only function since it is intended to be called from the
	 * command-line
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		// TODO Auto-generated method stub
		final File f = new File(args[0]);
		if (f.exists()) {
			System.out.println(args[0] + " is present, script will continue");
		} else {
			System.out.println(args[0] + " is missing, script will abort");
			String execLine = "";
			if (args.length < 2) {
				execLine = "$ DELETE/ENTRY='JOB_NR'  \n";
			} else {
				// execLine="sue:kill_task.com "+args[2];
				execLine = "$ DELETE/ENTRY=" + args[1] + "  \n";
			}
			for (int i = 2; i < args.length; i++)
				execLine += execLine = "$ DELETE " + args[1] + ";*  \n";
			new ExecTask(execLine);
		}

	}

}
