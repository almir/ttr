package net.geniecode.ttr;

import java.io.DataOutputStream;
import java.io.IOException;

public class CommonServices {

	// RunAsRoot function
	public static void RunAsRoot(String command) {
		RunAsRoot(command, false);
	}

	public static void RunAsRoot(String command, boolean state) {
		try {
			Process localProcess = Runtime.getRuntime().exec("su");
			DataOutputStream mDataOutputStream = new DataOutputStream(
					localProcess.getOutputStream());
			mDataOutputStream.writeBytes(command);
			mDataOutputStream.writeBytes("\n");
			mDataOutputStream.writeBytes("exit");
			mDataOutputStream.writeBytes("\n");
			mDataOutputStream.flush();
			mDataOutputStream.close();
			if (state) {
				try {
					localProcess.waitFor();
					return;
				} catch (InterruptedException localInterruptedException) {

				}
			}
		} catch (IOException localIOException) {

		}
	}
}
