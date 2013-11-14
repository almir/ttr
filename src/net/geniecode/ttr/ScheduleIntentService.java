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
