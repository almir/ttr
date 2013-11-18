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

import java.io.File;

import net.geniecode.ttr.ExecShell.SHELL_CMD;

public class CheckRoot {

	public boolean isDeviceRooted() {
		if (checkRootMethod1()) {
			return true;
		}
		if (checkRootMethod2()) {
			return true;
		}
		if (checkRootMethod3()) {
			return true;
		}
		return false;
	}

	public boolean checkRootMethod1() {
		String buildTags = android.os.Build.TAGS;

		if (buildTags != null && buildTags.contains("test-keys")) {
			return true;
		}
		return false;
	}

	public boolean checkRootMethod2() {
		try {
			File file = new File("/system/app/Superuser.apk");
			if (file.exists()) {
				return true;
			}
		} catch (Exception e) {
		}
		return false;
	}

	public boolean checkRootMethod3() {
		if (new ExecShell().executeCommand(SHELL_CMD.check_su_binary) != null) {
			return true;
		} else {
			return false;
		}
	}
}
