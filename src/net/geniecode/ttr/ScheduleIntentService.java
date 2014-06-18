/*
 * Copyright (C) 2013-2014 GenieCode
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

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class ScheduleIntentService extends IntentService {

	public static void launchService(Context context) {
		if (context == null)
			return;
		context.startService(new Intent(context, ScheduleIntentService.class));
	}

	public ScheduleIntentService() {
		super("ScheduleIntentService");
	}

	@SuppressLint("NewApi")
	@Override
	protected void onHandleIntent(Intent intent) {
		// This code will be executed in a background thread
		String result = Settings.Global.getString(getContentResolver(),
				Settings.Global.AIRPLANE_MODE_ON);

		StringBuilder mEnableCommand = new StringBuilder()
				.append("settings put global airplane_mode_on ");

		StringBuilder mBroadcastCommand = new StringBuilder()
				.append("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state ");

		if (result.equals("0")) {
			CommonServices.RunAsRoot(mEnableCommand + "1", true);
			CommonServices.RunAsRoot(mBroadcastCommand + "true", true);
		} else {
			CommonServices.RunAsRoot(mEnableCommand + "0", true);
			CommonServices.RunAsRoot(mBroadcastCommand + "false", true);
		}
	}
}
