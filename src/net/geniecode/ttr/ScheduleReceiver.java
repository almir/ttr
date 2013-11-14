package net.geniecode.ttr;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.provider.Settings;

public class ScheduleReceiver extends BroadcastReceiver {

	public static final String PREFS_NAME = "TTRPrefs";
	public static final String WIFI_STATE = "WiFiState";

	private WifiManager wifiManager;

	@SuppressLint("NewApi")
	@Override
	public void onReceive(Context context, Intent intent) {
		if (android.os.Build.VERSION.SDK_INT < 17) {
			boolean isEnabled = Settings.System.getInt(
					context.getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON, 0) == 1;

			if (!isEnabled) {
				Settings.System.putInt(context.getContentResolver(),
						Settings.System.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);
			}

			// Post an intent to reload
			Intent relintent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
			relintent.putExtra("state", !isEnabled);
			context.sendBroadcast(relintent);

			wifiManager = (WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);
			boolean isWifiEnabled = wifiManager.isWifiEnabled();

			SharedPreferences settings = context.getSharedPreferences(
					PREFS_NAME, 0);

			if (isWifiEnabled) {
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(WIFI_STATE, isWifiEnabled);
				editor.commit();
				wifiManager.setWifiEnabled(false);
			} else {
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(WIFI_STATE, isWifiEnabled);
				editor.commit();
			}
		} else {
			String result = Settings.Global.getString(
					context.getContentResolver(),
					Settings.Global.AIRPLANE_MODE_ON);

			if (result.equals("0")) {
				ScheduleWakeLock.acquireCpuWakeLock(context);
				ScheduleIntentService.launchService(context);
				ScheduleWakeLock.releaseCpuLock();
			}
		}
	}
}
