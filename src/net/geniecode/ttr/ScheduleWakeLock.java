package net.geniecode.ttr;

import android.content.Context;
import android.os.PowerManager;

class ScheduleWakeLock {

	private static PowerManager.WakeLock sCpuWakeLock;
	
	@SuppressWarnings("deprecation")
	static void acquireCpuWakeLock(Context context) {
		if (sCpuWakeLock != null) {
			return;
		}

		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);

		sCpuWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.ON_AFTER_RELEASE, "Released.");
		sCpuWakeLock.acquire();
	}

	static void releaseCpuLock() {
		if (sCpuWakeLock != null) {
			sCpuWakeLock.release();
			sCpuWakeLock = null;
		}
	}
}
