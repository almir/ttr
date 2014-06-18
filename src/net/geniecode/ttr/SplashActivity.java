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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;

public class SplashActivity extends Activity {
	
	public static final String PREFS_NAME = "TTRPrefs";
	public static final String FIRSTRUN = "FirstRun";
	public static final String ROOTED = "DeviceRooted";

	// Class variables set for SplashScreen
	protected boolean _active = true;
	protected int _splashTime = 3000;

	@SuppressLint("InlinedApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// Set Holo theme for Android 3+
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			setTheme(android.R.style.Theme_Holo_NoActionBar_Fullscreen);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		// Thread for displaying the SplashScreen
		Thread splashThread = new Thread() {
			@Override
			public void run() {
				try {
					int waited = 0;
					while (_active && (waited < _splashTime)) {
						sleep(100);
						if (_active) {
							waited += 100;
						}
					}
				} catch (InterruptedException e) {
					// do nothing
				} finally {
					// Check whether the Android 4.2 device is rooted or not
					if (android.os.Build.VERSION.SDK_INT >= 17) {
						CheckRoot mCheckRootAccess = new CheckRoot();
						mCheckRootAccess.isDeviceRooted();
						
						// Get shared preferences
	                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	                    // Get shared preferences editor for writing later
	                    SharedPreferences.Editor editor = settings.edit();
	                    
	                    // Chech whether this is our first run
	                    Boolean mFirstRun = settings.getBoolean(FIRSTRUN, true);
	                    if(mFirstRun) {
	                    	if (!mCheckRootAccess.isDeviceRooted()) {
	                    		// Write to shared peferences
	                    		editor.putBoolean(FIRSTRUN, false);
	            			    editor.putBoolean(ROOTED, false);
	            			    editor.commit();
	            			    
	            			    // Show the dialog
								showNoRootDialog();
							} else {
								// Write to shared peferences
								editor.putBoolean(FIRSTRUN, false);
	            			    editor.putBoolean(ROOTED, true);
	            			    editor.commit();
	            			    
	            			    // Execute empty command as root just to gain access
								CommonServices.RunAsRoot("");
								
								// Start the main activity
								Intent intent = new Intent(SplashActivity.this,
										MainActivity.class);
								startActivityForResult(intent, 0);
								finish();
							}
	                    } else {
	                    	// We always check for root, write true if the device has been
	                    	// rooted in the meantime, or false if it has lost root somehow
	                    	if (mCheckRootAccess.isDeviceRooted()) {
	                    		editor.putBoolean(ROOTED, true);
	            			    editor.commit();
	            			    // Execute empty command as root just to gain access
								CommonServices.RunAsRoot("");
	                    	} else {
	                    		editor.putBoolean(ROOTED, false);
	            			    editor.commit();
	                    	}
	                    	
	                    	// Start the main activity
	                    	Intent intent = new Intent(SplashActivity.this,
									MainActivity.class);
							startActivityForResult(intent, 0);
							finish();
	                    }
					} else {
						// We don't care about root for devices with Android less than 4.2
						// let's just start the main activity
						Intent intent = new Intent(SplashActivity.this,
								MainActivity.class);
						startActivityForResult(intent, 0);
						finish();
					}
				}
			}
		};
		splashThread.start();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			_active = false;
		}
		return true;
	}

	// Show the message stating that the device hasn't been rooted
	// and that the airplane mode has been disabled, and then
	// start the main activity
	private void showNoRootDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new AlertDialog.Builder(SplashActivity.this)
				.setTitle(getString(R.string.title_not_rooted))
				.setMessage(getString(R.string.message_not_rooted))
				.setCancelable(false)
				.setNeutralButton(android.R.string.ok,
				new AlertDialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface d, int w) {
						// Start the main activity
                    	Intent intent = new Intent(SplashActivity.this,
								MainActivity.class);
						startActivityForResult(intent, 0);
						finish();
					}
				}).show();
			}
		});
	}
}
