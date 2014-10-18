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

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.provider.Settings;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;

public class ScheduleIntentService extends IntentService {

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
			CommandCapture command = new CommandCapture(0,
					mEnableCommand + "1", mBroadcastCommand + "true");
			try {
				RootTools.getShell(true).add(command);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			} catch (RootDeniedException e) {
				e.printStackTrace();
			}
		} else {
			CommandCapture command = new CommandCapture(0,
					mEnableCommand + "0", mBroadcastCommand + "false");
			try {
				RootTools.getShell(true).add(command);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			} catch (RootDeniedException e) {
				e.printStackTrace();
			}
		}
		
		// Release the wake lock provided by the WakefulBroadcastReceiver
		WakefulBroadcastReceiver.completeWakefulIntent(intent);
	}
}
