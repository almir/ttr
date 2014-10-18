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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;

public class ActivateFlightMode extends Activity {
	
	public static final String PREFS_NAME = "TTRPrefs";
	public static final String WIFI_STATE = "WiFiState";

	private WifiManager mWifiManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		showActivateDialog();
	}
	
	private void showActivateDialog() {
 		new AlertDialog.Builder(this)
		.setTitle(getString(R.string.title_activate))
		.setMessage(getString(R.string.message_activate))
		.setCancelable(false)
		.setPositiveButton(R.string.yes,
		new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface d, int w) {
				activateAPMode();
			}
		})
		.setNegativeButton(R.string.no,
		new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface d, int w) {
				finish();
			}
		}).show();
 	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	protected void activateAPMode() {
		if (android.os.Build.VERSION.SDK_INT < 17) {
	    	boolean isEnabled = Settings.System.getInt(
					getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON, 0) == 1;
			
			mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			
			// Enable flight mode
			Settings.System.putInt(getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);
			
			// Get Wi-Fi state and disable that one too, just in case
			// (On some devices it doesn't get disabled when the flight mode is
			// turned on, so we do it here)
			boolean isWifiEnabled = mWifiManager.isWifiEnabled();

			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

			if (isWifiEnabled) {
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(WIFI_STATE, isWifiEnabled);
				editor.commit();
				mWifiManager.setWifiEnabled(false);
			} else {
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(WIFI_STATE, isWifiEnabled);
				editor.commit();
			}
			
			// Post an intent to reload
			Intent relintent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
			relintent.putExtra("state", !isEnabled);
			sendBroadcast(relintent);
		} else {
			// Enable flight mode on Android 4.2 and newer devices
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
		}
	}
}
