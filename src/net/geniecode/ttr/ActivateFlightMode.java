package net.geniecode.ttr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;

public class ActivateFlightMode extends Activity {
	
	public static final String PREFS_NAME = "TTRPrefs";
	public static final String WIFI_STATE = "WiFiState";

	private WifiManager mWifiManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Set Holo theme for Android 3+
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			setTheme(R.style.Theme_Holo_Transparent);
		}
		
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

	@SuppressWarnings("deprecation")
	protected void activateAPMode() {
		if (android.os.Build.VERSION.SDK_INT < 17) {
	    	boolean isEnabled = Settings.System.getInt(
					getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON, 0) == 1;
			
			mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			
			Settings.System.putInt(getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);
			
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
			ScheduleIntentService.launchService(getBaseContext());
		}
	}
}
