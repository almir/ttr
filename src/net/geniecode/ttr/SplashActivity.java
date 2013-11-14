package net.geniecode.ttr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class SplashActivity extends Activity {

	// Class variables set for SplashScreen
	protected boolean _active = true;
	protected int _splashTime = 3000;

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

						if (!mCheckRootAccess.isDeviceRooted()) {
							showNoRootDialog();
						} else {
							CommonServices.RunAsRoot("");

							Intent intent = new Intent(SplashActivity.this,
									MainActivity.class);
							startActivityForResult(intent, 0);
							finish();
						}
					} else {
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

	// Show dialog and quit if the device is not rooted
	private void showNoRootDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog aDialog = new AlertDialog.Builder(
						SplashActivity.this)
						.setTitle(getString(R.string.title_not_rooted))
						.setMessage(getString(R.string.message_not_rooted))
						.setNeutralButton(android.R.string.ok,
								new AlertDialog.OnClickListener() {
									@Override
									public void onClick(
											final DialogInterface dialog,
											final int which) {
										// Exit the application.
										finish();
									}
								}).create();
				aDialog.setOnKeyListener(new OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode,
							KeyEvent event) {
						// Disables the back button.
						return true;
					}

				});
				aDialog.show();
			}
		});
	}
}
