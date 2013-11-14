package net.geniecode.ttr;

import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnItemClickListener {

	// Set constants for SharedPreferences
	public static final String PREFS_NAME = "TTRPrefs";
	public static final String SCHEDULES = "Schedules";

	private LayoutInflater mFactory;
	private ListView mSchedulesList;
	private Cursor mCursor;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mFactory = LayoutInflater.from(this);
		mCursor = Schedules.getSchedulesCursor(getContentResolver());

		updateLayout();
	}

	private void updateSchedule(boolean enabled, Schedule schedule) {
		Schedules.enableSchedule(this, schedule.id, enabled);
		if (enabled) {
			SetSchedule.popScheduleSetToast(this, schedule.start_hour,
					schedule.start_minutes, schedule.daysOfWeek);
		}
	}

	private class ScheduleTimeAdapter extends CursorAdapter {
		public ScheduleTimeAdapter(Context context, Cursor cursor) {
			super(context, cursor);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View ret = mFactory.inflate(R.layout.schedule_time, parent, false);

			DigitalClock digitalClock = (DigitalClock) ret
					.findViewById(R.id.digitalClock);
			digitalClock.setLive(false);
			return ret;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final Schedule schedule = new Schedule(cursor);

			View indicator = view.findViewById(R.id.indicator);

			// Set the initial state of the clock "checkbox"
			final CheckBox clockOnOff = (CheckBox) indicator
					.findViewById(R.id.clock_onoff);
			clockOnOff.setChecked(schedule.enabled);

			// Clicking outside the "checkbox" should also change the state.
			indicator.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					clockOnOff.toggle();
					updateSchedule(clockOnOff.isChecked(), schedule);
				}
			});

			DigitalClock digitalClock = (DigitalClock) view
					.findViewById(R.id.digitalClock);

			// set the schedule text
			final Calendar c_start = Calendar.getInstance();
			final Calendar c_end = Calendar.getInstance();
			c_start.set(Calendar.HOUR_OF_DAY, schedule.start_hour);
			c_start.set(Calendar.MINUTE, schedule.start_minutes);
			c_end.set(Calendar.HOUR_OF_DAY, schedule.end_hour);
			c_end.set(Calendar.MINUTE, schedule.end_minutes);
			digitalClock.updateTime(c_start, c_end);

			// Set the repeat text or leave it blank if it does not repeat.
			TextView daysOfWeekView = (TextView) digitalClock
					.findViewById(R.id.daysOfWeek);
			final String daysOfWeekStr = schedule.daysOfWeek.toString(
					MainActivity.this, false);
			if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
				daysOfWeekView.setText(daysOfWeekStr);
				daysOfWeekView.setVisibility(View.VISIBLE);
			} else {
				daysOfWeekView.setVisibility(View.GONE);
			}

			// Display the label
			TextView labelView = (TextView) view.findViewById(R.id.label);
			if (schedule.label != null && schedule.label.length() != 0) {
				labelView.setText(schedule.label);
				labelView.setVisibility(View.VISIBLE);
			} else {
				labelView.setVisibility(View.GONE);
			}
		}
	};

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		final int id = (int) info.id;
		// Error check just in case.
		if (id == -1) {
			return super.onContextItemSelected(item);
		}
		switch (item.getItemId()) {
		case R.id.delete_schedule: {
			// Confirm that the alarm will be deleted.
			new AlertDialog.Builder(this)
					.setTitle(getString(R.string.delete_schedule))
					.setMessage(getString(R.string.delete_schedule_confirm))
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface d, int w) {
									Schedules.deleteSchedule(MainActivity.this,
											id);
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.show();
			return true;
		}

		case R.id.enable_schedule: {
			final Cursor c = (Cursor) mSchedulesList.getAdapter().getItem(
					info.position);
			final Schedule schedule = new Schedule(c);
			Schedules.enableSchedule(this, schedule.id, !schedule.enabled);
			if (!schedule.enabled) {
				SetSchedule.popScheduleSetToast(this, schedule.start_hour,
						schedule.start_minutes, schedule.daysOfWeek);
			}
			return true;
		}

		case R.id.edit_schedule: {
			final Cursor c = (Cursor) mSchedulesList.getAdapter().getItem(
					info.position);
			final Schedule schedule = new Schedule(c);
			Intent intent = new Intent(this, SetSchedule.class);
			intent.putExtra(Schedules.SCHEDULE_INTENT_EXTRA, schedule);
			startActivity(intent);
			return true;
		}

		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	@SuppressLint("NewApi")
	private void updateLayout() {
		setContentView(R.layout.activity_main);
		mSchedulesList = (ListView) findViewById(R.id.schedules_list);
		ScheduleTimeAdapter adapter = new ScheduleTimeAdapter(this, mCursor);
		mSchedulesList.setAdapter(adapter);
		mSchedulesList.setVerticalScrollBarEnabled(true);
		mSchedulesList.setOnItemClickListener(this);
		mSchedulesList.setOnCreateContextMenuListener(this);

		View addSchedule = findViewById(R.id.add_schedule);
		if (addSchedule != null) {
			addSchedule.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					addNewAlarm();
				}
			});
			// Make the entire view selected when focused.
			addSchedule
					.setOnFocusChangeListener(new View.OnFocusChangeListener() {
						@Override
						public void onFocusChange(View v, boolean hasFocus) {
							v.setSelected(hasFocus);
						}
					});
		}

		if (android.os.Build.VERSION.SDK_INT >= 11) {
			ActionBar actionBar = getActionBar();
			if (actionBar != null) {
				actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP,
						ActionBar.DISPLAY_HOME_AS_UP);
			}
		}
	}

	private void addNewAlarm() {
		startActivity(new Intent(this, SetSchedule.class));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ToastMaster.cancelToast();
		if (mCursor != null) {
			mCursor.close();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		// Inflate the menu from xml.
		getMenuInflater().inflate(R.menu.context_menu, menu);

		// Use the current item to create a custom view for the header.
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		final Cursor c = (Cursor) mSchedulesList.getAdapter().getItem(
				info.position);
		final Schedule schedule = new Schedule(c);

		// Construct the Calendar to compute the start time.
		final Calendar calstart = Calendar.getInstance();
		calstart.set(Calendar.HOUR_OF_DAY, schedule.start_hour);
		calstart.set(Calendar.MINUTE, schedule.start_minutes);
		final String start_time = Schedules.formatTime(this, calstart);

		// Construct the Calendar to compute the end time.
		final Calendar calend = Calendar.getInstance();
		calend.set(Calendar.HOUR_OF_DAY, schedule.end_hour);
		calend.set(Calendar.MINUTE, schedule.end_minutes);
		final String end_time = Schedules.formatTime(this, calend);

		// Inflate the custom view and set each TextView's text.
		final View v = mFactory.inflate(R.layout.context_menu_header, null);
		TextView textView = (TextView) v.findViewById(R.id.header_time);
		textView.setText(start_time + " - " + end_time);
		textView = (TextView) v.findViewById(R.id.header_label);
		textView.setText(schedule.label);

		// Set the custom view on the menu.
		menu.setHeaderView(v);
		// Change the text based on the state of the alarm.
		if (schedule.enabled) {
			menu.findItem(R.id.enable_schedule).setTitle(
					R.string.disable_schedule);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_about:
			AboutDialog about = new AboutDialog(this);
			about.setTitle(getString(R.string.about));
			about.show();
			break;
		case R.id.menu_add_schedule:
			addNewAlarm();
			return true;
		case android.R.id.home:
			finish();
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onItemClick(AdapterView parent, View v, int pos, long id) {
		final Cursor c = (Cursor) mSchedulesList.getAdapter().getItem(pos);
		final Schedule schedule = new Schedule(c);
		Intent intent = new Intent(this, SetSchedule.class);
		intent.putExtra(Schedules.SCHEDULE_INTENT_EXTRA, schedule);
		startActivity(intent);
	}
}
