package net.geniecode.ttr;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

/**
 * Manages each schedule
 */
public class SetSchedule extends PreferenceActivity implements
		Preference.OnPreferenceChangeListener,
		TimePickerDialog.OnTimeSetListener, OnCancelListener {

	private static final String KEY_CURRENT_SCHEDULE = "currentSchedule";
	private static final String KEY_ORIGINAL_SCHEDULE = "originalSchedule";
	private static final String KEY_START_TIME_PICKER_BUNDLE = "startTimePickerBundle";
	private static final String KEY_END_TIME_PICKER_BUNDLE = "endTimePickerBundle";

	private EditText mLabel;
	private CheckBoxPreference mEnabledPref;
	private Preference mStartTimePref;
	private Preference mEndTimePref;
	private RepeatPreference mRepeatPref;

	private int mId;
	private int mStartHour;
	private int mStartMinute;
	private int mEndHour;
	private int mEndMinute;
	private int StartOrEnd = 0;
	private TimePickerDialog mStartTimePickerDialog;
	private TimePickerDialog mEndTimePickerDialog;
	private Schedule mOriginalSchedule;

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Override the default content view.
		setContentView(R.layout.set_schedule);

		EditText label = (EditText) getLayoutInflater().inflate(
				R.layout.schedule_label, null);
		ListView list = (ListView) findViewById(android.R.id.list);
		list.addFooterView(label);

		// TODO Stop using preferences for this view. Save on done, not after
		// each change.
		addPreferencesFromResource(R.xml.schedule_prefs);

		// Get each preference so we can retrieve the value later.
		mLabel = label;
		mEnabledPref = (CheckBoxPreference) findPreference("enabled");
		mEnabledPref.setOnPreferenceChangeListener(this);
		mStartTimePref = findPreference("start_time");
		mEndTimePref = findPreference("end_time");
		mRepeatPref = (RepeatPreference) findPreference("setRepeat");
		mRepeatPref.setOnPreferenceChangeListener(this);

		Intent i = getIntent();
		Schedule schedule = i
				.getParcelableExtra(Schedules.SCHEDULE_INTENT_EXTRA);

		if (schedule == null) {
			// No schedules means create a new schedule.
			schedule = new Schedule();
		}
		mOriginalSchedule = schedule;

		// Populate the prefs with the original schedule data. updatePrefs also
		// sets mId so it must be called before checking mId below.
		updatePrefs(mOriginalSchedule);

		// We have to do this to get the save/cancel buttons to highlight on
		// their own.
		getListView().setItemsCanFocus(true);

		if (android.os.Build.VERSION.SDK_INT >= 11) {
			ActionBar actionBar = getActionBar();
			if (actionBar != null) {
				actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_SHOW_TITLE);
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View customActionBarView = inflater.inflate(
						R.layout.set_schedule_action_bar, null);
				actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
						ActionBar.DISPLAY_SHOW_CUSTOM
								| ActionBar.DISPLAY_SHOW_HOME
								| ActionBar.DISPLAY_SHOW_TITLE);
				actionBar.setCustomView(customActionBarView);
				View saveMenuItem = customActionBarView
						.findViewById(R.id.save_menu_item);
				saveMenuItem.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						saveAndExit();
					}
				});
			}
		}

		// Attach actions to each button.
		Button b = (Button) findViewById(R.id.schedule_save);
		if (b != null) {
			if (android.os.Build.VERSION.SDK_INT >= 11) {
				b.setEnabled(false);
				b.setVisibility(View.GONE);
			} else {
				b.setVisibility(View.VISIBLE);
				b.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						saveAndExit();
					}
				});
			}
		}
		b = (Button) findViewById(R.id.schedule_delete);
		if (b != null) {
			if ((mId == -1) || (android.os.Build.VERSION.SDK_INT >= 11)) {
				b.setEnabled(false);
				b.setVisibility(View.GONE);
			} else {
				b.setVisibility(View.VISIBLE);
				b.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						deleteAlarm();
					}
				});
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_delete) {
			deleteAlarm();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if ((android.os.Build.VERSION.SDK_INT >= 11) && (mId != -1)) {
			getMenuInflater().inflate(R.menu.set_schedule_context, menu);
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(KEY_ORIGINAL_SCHEDULE, mOriginalSchedule);
		outState.putParcelable(KEY_CURRENT_SCHEDULE, buildScheduleFromUi());
		if (mStartTimePickerDialog != null) {
			if (mStartTimePickerDialog.isShowing()) {
				outState.putParcelable(KEY_START_TIME_PICKER_BUNDLE,
						mStartTimePickerDialog.onSaveInstanceState());
				mStartTimePickerDialog.dismiss();
			}
			mStartTimePickerDialog = null;
		}
		if (mEndTimePickerDialog != null) {
			if (mEndTimePickerDialog.isShowing()) {
				outState.putParcelable(KEY_END_TIME_PICKER_BUNDLE,
						mEndTimePickerDialog.onSaveInstanceState());
				mEndTimePickerDialog.dismiss();
			}
			mEndTimePickerDialog = null;
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);

		Schedule scheduleFromBundle = state
				.getParcelable(KEY_ORIGINAL_SCHEDULE);
		if (scheduleFromBundle != null) {
			mOriginalSchedule = scheduleFromBundle;
		}

		scheduleFromBundle = state.getParcelable(KEY_CURRENT_SCHEDULE);
		if (scheduleFromBundle != null) {
			updatePrefs(scheduleFromBundle);
		}

		Bundle bStart = state.getParcelable(KEY_START_TIME_PICKER_BUNDLE);
		if (bStart != null) {
			showStartTimePicker();
			mStartTimePickerDialog.onRestoreInstanceState(bStart);
		}

		Bundle bEnd = state.getParcelable(KEY_END_TIME_PICKER_BUNDLE);
		if (bEnd != null) {
			showEndTimePicker();
			mEndTimePickerDialog.onRestoreInstanceState(bEnd);
		}
	}

	// Used to post runnables asynchronously.
	private static final Handler sHandler = new Handler();

	@Override
	public boolean onPreferenceChange(final Preference p, Object newValue) {
		// Asynchronously save the schedule since this method is called _before_
		// the value of the preference has changed.
		sHandler.post(new Runnable() {
			@Override
			public void run() {
				// Editing any preference (except enable) enables the schedule.
				if (p != mEnabledPref) {
					mEnabledPref.setChecked(true);
				}
				saveSchedule(null);
			}
		});
		return true;
	}

	private void updatePrefs(Schedule schedule) {
		mId = schedule.id;
		mEnabledPref.setChecked(schedule.enabled);
		mLabel.setText(schedule.label);
		mStartHour = schedule.start_hour;
		mStartMinute = schedule.start_minutes;
		mEndHour = schedule.end_hour;
		mEndMinute = schedule.end_minutes;
		mRepeatPref.setDaysOfWeek(schedule.daysOfWeek);
		updateTime();
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference == mStartTimePref) {
			StartOrEnd = 1;
			showStartTimePicker();
		} else if (preference == mEndTimePref) {
			StartOrEnd = 2;
			showEndTimePicker();
		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public void onBackPressed() {
		if (mId == -1) {
			showSaveDialog();
		} else {
			revert();
			finish();
		}
	}

	private void showStartTimePicker() {
		if (mStartTimePickerDialog != null) {
			mStartTimePickerDialog = null;
		}

		mStartTimePickerDialog = new TimePickerDialog(this, this, mStartHour,
				mStartMinute, DateFormat.is24HourFormat(this));
		mStartTimePickerDialog.setOnCancelListener(this);
		mStartTimePickerDialog.show();
	}

	private void showEndTimePicker() {
		if (mEndTimePickerDialog != null) {
			mEndTimePickerDialog = null;
		}

		mEndTimePickerDialog = new TimePickerDialog(this, this, mEndHour,
				mEndMinute, DateFormat.is24HourFormat(this));
		mEndTimePickerDialog.setOnCancelListener(this);
		mEndTimePickerDialog.show();
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		// onTimeSet is called when the user clicks "Set"
		if (StartOrEnd == 1) {
			mStartTimePickerDialog = null;
			mStartHour = hourOfDay;
			mStartMinute = minute;
		} else if (StartOrEnd == 2) {
			mEndTimePickerDialog = null;
			mEndHour = hourOfDay;
			mEndMinute = minute;
		}

		updateTime();
		// If the time has been changed, enable the alarm.
		mEnabledPref.setChecked(true);
		saveSchedule(null);
		StartOrEnd = 0;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		// mTimePickerDialog = null;
		if (mStartTimePickerDialog != null) {
			mStartTimePickerDialog = null;
		}
		if (mEndTimePickerDialog != null) {
			mEndTimePickerDialog = null;
		}
	}

	private void updateTime() {
		// mTimePref.setSummary(Schedules.formatTime(this, mStartHour,
		// mStartMinute,
		// mRepeatPref.getDaysOfWeek()));
		if (StartOrEnd == 1)
			mStartTimePref.setSummary(Schedules.formatTime(this, mStartHour,
					mStartMinute, mRepeatPref.getDaysOfWeek()));

		else if (StartOrEnd == 2) {
			mEndTimePref.setSummary(Schedules.formatTime(this, mEndHour,
					mEndMinute, mRepeatPref.getDaysOfWeek()));
		}

		else if (StartOrEnd == 0) {
			mStartTimePref.setSummary(Schedules.formatTime(this, mStartHour,
					mStartMinute, mRepeatPref.getDaysOfWeek()));
			mEndTimePref.setSummary(Schedules.formatTime(this, mEndHour,
					mEndMinute, mRepeatPref.getDaysOfWeek()));
		}
	}

	private long saveSchedule(Schedule schedule) {
		if (schedule == null) {
			schedule = buildScheduleFromUi();
		}

		long time;
		if (schedule.id == -1) {
			time = Schedules.addSchedule(this, schedule);
			// addSchedule populates the schedule with the new id. Update mId so
			// that
			// changes to other preferences update the new schedule.
			mId = schedule.id;
		} else {
			time = Schedules.setSchedule(this, schedule);
		}
		return time;
	}

	private Schedule buildScheduleFromUi() {
		Schedule schedule = new Schedule();
		schedule.id = mId;
		schedule.enabled = mEnabledPref.isChecked();
		schedule.start_hour = mStartHour;
		schedule.start_minutes = mStartMinute;
		schedule.end_hour = mEndHour;
		schedule.end_minutes = mEndMinute;
		schedule.daysOfWeek = mRepeatPref.getDaysOfWeek();
		schedule.label = mLabel.getText().toString();
		return schedule;
	}

	private void deleteAlarm() {
		if (mId == -1) {
			// Unedited, newly created alarms don't require confirmation
			finish();
		} else {
			new AlertDialog.Builder(this)
					.setTitle(getString(R.string.delete_schedule))
					.setMessage(getString(R.string.delete_schedule_confirm))
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface d, int w) {
									Schedules.deleteSchedule(SetSchedule.this,
											mId);
									finish();
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.show();
		}
	}

	private void revert() {
		int newId = mId;
		// "Revert" on a newly created schedule should delete it.
		if (mOriginalSchedule.id == -1) {
			Schedules.deleteSchedule(SetSchedule.this, newId);
		} else {
			saveSchedule(mOriginalSchedule);
		}
	}

	/**
	 * Store any changes to the schedule and exit the activity. Show a toast if
	 * the schedule is enabled with the time remaining until schedule
	 */
	private void saveAndExit() {
		long time = saveSchedule(null);
		if (mEnabledPref.isChecked()) {
			popScheduleSetToast(SetSchedule.this, time);
		}
		finish();
	}

	/**
	 * Display a toast that tells the user how long until the schedule goes off.
	 * This helps prevent "am/pm" mistakes.
	 */
	static void popScheduleSetToast(Context context, int hour, int minute,
			Schedule.DaysOfWeek daysOfWeek) {
		popScheduleSetToast(context,
				Schedules.calculateSchedule(hour, minute, daysOfWeek)
						.getTimeInMillis());
	}

	static void popScheduleSetToast(Context context, long timeInMillis) {
		String toastText = formatToast(context, timeInMillis);
		Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
		ToastMaster.setToast(toast);
		toast.show();
	}

	/**
	 * format "Schedule set for 2 days 7 hours and 53 minutes from now"
	 */
	static String formatToast(Context context, long timeInMillis) {
		long delta = timeInMillis - System.currentTimeMillis();
		long hours = delta / (1000 * 60 * 60);
		long minutes = delta / (1000 * 60) % 60;
		long days = hours / 24;
		hours = hours % 24;

		String daySeq = (days == 0) ? "" : (days == 1) ? context
				.getString(R.string.day) : context.getString(R.string.days,
				Long.toString(days));

		String minSeq = (minutes == 0) ? "" : (minutes == 1) ? context
				.getString(R.string.minute) : context.getString(
				R.string.minutes, Long.toString(minutes));

		String hourSeq = (hours == 0) ? "" : (hours == 1) ? context
				.getString(R.string.hour) : context.getString(R.string.hours,
				Long.toString(hours));

		boolean dispDays = days > 0;
		boolean dispHour = hours > 0;
		boolean dispMinute = minutes > 0;

		int index = (dispDays ? 1 : 0) | (dispHour ? 2 : 0)
				| (dispMinute ? 4 : 0);

		String[] formats = context.getResources().getStringArray(
				R.array.schedule_set);
		return String.format(formats[index], daySeq, hourSeq, minSeq);
	}
	
	// Show dialog when back button is pressed
	private void showSaveDialog() {
 		AlertDialog aDialog = new AlertDialog.Builder(this)
		.setTitle(getString(R.string.title_saveorcancel))
		.setMessage(getString(R.string.message_saveorcancel))
		.setPositiveButton(R.string.yes,
		new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface d, int w) {
				saveAndExit();
			}
		})
		.setNegativeButton(R.string.no,
		new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface d, int w) {
				revert();
				finish();
			}
		}).create();
 		aDialog.setOnKeyListener(new OnKeyListener() {
 			@Override
 			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
 				// Disables the back button.
 				return true;
 			}
 		});
 		aDialog.show();
 	}
}
