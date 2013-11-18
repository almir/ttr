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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Parcel;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class ScheduleReceiver extends BroadcastReceiver {

	public static final String PREFS_NAME = "TTRPrefs";
	public static final String WIFI_STATE = "WiFiState";

	private WifiManager mWifiManager;
	private TelephonyManager mTelephonyManager;
	private AudioManager mAudioManager;
	private NotificationManager mNotificationManager;
	private String ScrollingText;
	private String NotificationText;
	
	// Notification constant
	public static final int NOTIFICATION_ID = 0;
	
	/** If the schedule is older than STALE_WINDOW, ignore.  It
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

        // Disable this schedule if it does not repeat.
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
        
        // Get telephony service
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        
        // Execute only for devices with versions of Android less than 4.2
        if (android.os.Build.VERSION.SDK_INT < 17) {
        	// Get flight mode state
			boolean isEnabled = Settings.System.getInt(
					context.getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON, 0) == 1;
			
			// Get Wi-Fi service
			mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			
			if ((schedule.aponoff) && (!isEnabled) && (schedule.mode.equals("1")) &&
					(mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE)) {
				// Enable flight mode
				Settings.System.putInt(context.getContentResolver(),
						Settings.System.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);
				
				// Get Wi-Fi state and disable that one too, just in case
				// (On some devices it doesn't get disabled when the flight mode is
				// turned on, so we do it here)
				boolean isWifiEnabled = mWifiManager.isWifiEnabled();

				SharedPreferences settings = context.getSharedPreferences(
						PREFS_NAME, 0);

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
				context.sendBroadcast(relintent);
			}
			else if ((!schedule.aponoff) && (isEnabled) && (schedule.mode.equals("1"))) {
				// Disable flight mode
				Settings.System.putInt(context.getContentResolver(),
						Settings.System.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);
				
				// Restore previously remembered Wi-Fi state
				SharedPreferences settings = context.getSharedPreferences(
						PREFS_NAME, 0);
				Boolean WiFiState = settings.getBoolean(WIFI_STATE, true);

				if (WiFiState) {
					mWifiManager.setWifiEnabled(true);
				}
				
				// Post an intent to reload
				Intent relintent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
				relintent.putExtra("state", !isEnabled);
				context.sendBroadcast(relintent);
			}
			// Check whether there are ongoing phone calls, and if so
			// show notification instead of just enabling the flight mode
			else if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
				setNotification(context);
			}
		// Execute for devices with Android 4.2	or higher
		} else {
			// Get flight mode state
			String result = Settings.Global.getString(
					context.getContentResolver(),
					Settings.Global.AIRPLANE_MODE_ON);
			
			if ((schedule.aponoff) && (result.equals("0")) && (schedule.mode.equals("1")) &&
					(mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE)) {
				// Acquire full device wake (needed for the phone state to be refreshed
				// correctly) and enable flight mode
				ScheduleWakeLock.acquireCpuWakeLock(context);
				ScheduleIntentService.launchService(context);
				ScheduleWakeLock.releaseCpuLock();
			}
			else if ((!schedule.aponoff) && (result.equals("1")) && (schedule.mode.equals("1"))) {
				// Acquire full device wake (needed for the phone state to be refreshed
				// correctly) and disable flight mode
				ScheduleWakeLock.acquireCpuWakeLock(context);
				ScheduleIntentService.launchService(context);
				ScheduleWakeLock.releaseCpuLock();
			}
			else if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
				setNotification(context);
			}
		}
        
        // Get audio service
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        // Get current ringer mode and set silent or normal mode accordingly
        switch (mAudioManager.getRingerMode()) {
        case AudioManager.RINGER_MODE_SILENT:
        	if ((!schedule.silentonoff) && (schedule.mode.equals("2"))) {
        		mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }
        	break;
        case AudioManager.RINGER_MODE_NORMAL:
        	if ((schedule.silentonoff) && (schedule.mode.equals("2"))) {
        		mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        	break;
        }
    }

	// Show notification if the phone call has been detected
	@SuppressLint("NewApi")
	private void setNotification(Context context) {
		ScrollingText = context.getString(R.string.schedule_postponed_scroll);
    	NotificationText = context.getString(R.string.schedule_postponed_notify);
    	
		// Trigger a notification that, when clicked, will activate airplane mode
		mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
        		new Intent(context, ActivateFlightMode.class), PendingIntent.FLAG_CANCEL_CURRENT);
		
		Notification.Builder builder = new Notification.Builder(context);
		builder.setContentIntent(contentIntent)
			.setSmallIcon(R.drawable.ic_launcher)
			.setTicker(ScrollingText)
			.setWhen(System.currentTimeMillis())
			.setAutoCancel(true)
			.setOnlyAlertOnce(true)
			.setDefaults(Notification.DEFAULT_LIGHTS)
			.setContentTitle(context.getText(R.string.app_name))
			.setContentText(NotificationText);
		Notification notification = builder.build();
		 
		 mNotificationManager.notify(NOTIFICATION_ID, notification);
	}
}
