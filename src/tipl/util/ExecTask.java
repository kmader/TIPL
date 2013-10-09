package tipl.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Date;

public class ExecTask {
	public ExecTask(final String cmds) {
		RunTask(cmds, true, "EvalService_KMNT.com");
	}

	public ExecTask(final String cmds, final String batchFileName,
			final boolean useQueue) {
		if (useQueue) {
			RunTask("$ DEF DK1 'P1'\n$ CD 'P2'\n" + cmds, false, batchFileName);
			RunTask("$ SUBMIT/PARAMETERS=('F$TRNLNM(\"DK1\")','F$ENVIRONMENT(\"DEFAULT\")')/NOPRINT/QUE=SYS$EVAL/LOG=SYS$SCRATCH: "
					+ batchFileName + "\n", true, "EvalService_KMNT.com");
		} else {
			// "EvalService_KMNT_Batch.com"
			RunTask(cmds, true, batchFileName);
		}
	}

	public void RunTask(final String cmds, final boolean dRun,
			final String submitterFilename) {
		final File submitterFile = new File(submitterFilename);
		FileOutputStream fos;
		String submitCommand = "$! Submitted file created by a java tool\n";
		submitCommand += "$! " + new Date() + "\n";
		submitCommand += "$! =============================================================================\n";
		submitCommand += "$! \n";
		submitCommand += "$! \n";
		submitCommand += "$! Execute commands from file.\n";
		submitCommand += "$! --------------------\n";
		submitCommand += cmds;
		submitCommand += "$! \n";
		submitCommand += "$! \n";
		submitCommand += "$! Delete this procedure.\n";
		submitCommand += "$! ----------------------\n";
		submitCommand += "$ PROC = F$ENVIRONMENT(\"PROCEDURE\")\n";
		submitCommand += "$ PROC = F$SEARCH(PROC)\n";
		submitCommand += "$ DELETE \'PROC\'\n";
		submitCommand += "$! \n";
		submitCommand += "$! \n";
		submitCommand += "$ EXIT";

		try {
			fos = new FileOutputStream(submitterFile);
			fos.write(submitCommand.getBytes());
			fos.flush();
			fos.close();
			String line = "";
			final Runtime rt = Runtime.getRuntime();
			if (dRun) {
				final Process p = rt.exec(submitterFilename);
				final BufferedReader stdInput = new BufferedReader(
						new InputStreamReader(p.getInputStream()));
				final BufferedReader stdError = new BufferedReader(
						new InputStreamReader(p.getErrorStream()));
				while ((line = stdInput.readLine()) != null) {
					System.out.println(line);
				}
				stdInput.close();
				while ((line = stdError.readLine()) != null) {
					System.err.println(line);
				}
				stdError.close();
				p.exitValue();
			}
		} catch (final Exception e) {
			System.out.println("Error Executing Task");
			e.printStackTrace();
		}
	}
}