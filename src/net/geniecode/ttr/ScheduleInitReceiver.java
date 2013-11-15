package net.geniecode.ttr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScheduleInitReceiver extends BroadcastReceiver {

	/**
	 * Sets schedule on ACTION_BOOT_COMPLETED. Resets schedule on TIME_SET,
	 * TIMEZONE_CHANGED
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		// Remove the expired alarm after a boot.
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Schedules.disableExpiredSchedules(context);
        }

        Schedules.setNextSchedule(context);
	}
}
