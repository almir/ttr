package net.geniecode.ttr;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Parcel;
import android.provider.Settings;

public class ScheduleReceiver extends BroadcastReceiver {

	public static final String PREFS_NAME = "TTRPrefs";
	public static final String WIFI_STATE = "WiFiState";

	private WifiManager wifiManager;
	
	/** If the alarm is older than STALE_WINDOW, ignore.  It
    is probably the result of a time or timezone change */
	private final static int STALE_WINDOW = 30 * 60 * 1000;
	
	@SuppressWarnings("deprecation")
	@SuppressLint({ "Recycle", "NewApi", "InlinedApi" })
	@Override
    public void onReceive(Context context, Intent intent) {
		if (!Schedules.SCHEDULE_ACTION.equals(intent.getAction())) {
            // Unknown intent, bail.
            return;
        }
		
		Schedule schedule = null;
        // Grab the schedule from the intent. Since the remote AlarmManagerService
        // fills in the Intent to add some extra data, it must unparcel the
        // Schedule object. It throws a ClassNotFoundException when unparcelling.
        // To avoid this, do the marshalling ourselves.
        final byte[] data = intent.getByteArrayExtra(Schedules.SCHEDULE_RAW_DATA);
        if (data != null) {
            Parcel in = Parcel.obtain();
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
            schedule = Schedule.CREATOR.createFromParcel(in);
        }

        if (schedule == null) {
        	// Make sure we set the next schedule if needed.
            Schedules.setNextSchedule(context);
            return;
        }

        // Disable this alarm if it does not repeat.
        if (!schedule.daysOfWeek.isRepeatSet()) {
            Schedules.enableSchedule(context, schedule.id, false);
        } else {
            // Enable the next schedule if there is one. The above call to
            // enableSchedule will call setNextSchedule so avoid calling it twice.
            Schedules.setNextSchedule(context);
        }
        
        long now = System.currentTimeMillis();

        if (now > schedule.time + STALE_WINDOW) {
            return;
        }
        
        if (android.os.Build.VERSION.SDK_INT < 17) {
			boolean isEnabled = Settings.System.getInt(
					context.getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON, 0) == 1;
			
			wifiManager = (WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);
			
			if ((schedule.aponoff) && (!isEnabled)) {
				Settings.System.putInt(context.getContentResolver(),
						Settings.System.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);
				
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
			}
			else if ((!schedule.aponoff) && (isEnabled)) {
				Settings.System.putInt(context.getContentResolver(),
						Settings.System.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);
				
				SharedPreferences settings = context.getSharedPreferences(
						PREFS_NAME, 0);
				Boolean WiFiState = settings.getBoolean(WIFI_STATE, true);

				if (WiFiState) {
					wifiManager.setWifiEnabled(true);
				}
			}

			// Post an intent to reload
			Intent relintent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
			relintent.putExtra("state", !isEnabled);
			context.sendBroadcast(relintent);			
		} else {
			String result = Settings.Global.getString(
					context.getContentResolver(),
					Settings.Global.AIRPLANE_MODE_ON);
			
			if ((schedule.aponoff) && (result.equals("0"))) {
				ScheduleWakeLock.acquireCpuWakeLock(context);
				ScheduleIntentService.launchService(context);
				ScheduleWakeLock.releaseCpuLock();
			}
			else if ((!schedule.aponoff) && (result.equals("1"))) {
				ScheduleWakeLock.acquireCpuWakeLock(context);
				ScheduleIntentService.launchService(context);
				ScheduleWakeLock.releaseCpuLock();
			}
		}
    }
}
