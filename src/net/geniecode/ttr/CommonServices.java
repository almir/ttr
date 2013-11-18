/*
 * Copyright (C) 2013 GenieCode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
