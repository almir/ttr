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

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.text.format.DateFormat;

/**
 * The Schedules provider supplies info about Time To Rest's settings
 */
public class Schedules {

	// This action triggers the ScheduleReceiver.
	// It is a public action used in the manifest for receiving Schedule broadcasts
	// from the alarm manager.
	public static final String SCHEDULE_ACTION = "net.geniecode.ttr.SCHEDULE";

	// This string is used when passing a Schedule object through an intent.
	public static final String SCHEDULE_INTENT_EXTRA = "intent.extra.schedule";

	// This extra is the raw Schedule object data. It is used in the
	// AlarmManagerService to avoid a ClassNotFoundException when filling in
	// the Intent extras.
	public static final String SCHEDULE_RAW_DATA = "intent.extra.schedule_raw";

	private final static String M12 = "h:mm aa";
	// Shared with DigitalClock
	final static String M24 = "kk:mm";

	final static int INVALID_SCHEDULE_ID = -1;

	/**
	 * Creates a new Schedule and fills in the given schedule's id.
	 */
	public static long addSchedule(Context context, Schedule schedule) {
		ContentValues values = createContentValues(schedule);
		Uri uri = context.getContentResolver().insert(
				Schedule.Columns.CONTENT_URI, values);
		schedule.id = (int) ContentUris.parseId(uri);

		long timeInMillis = calculateSchedule(schedule);
		setNextSchedule(context);
		return timeInMillis;
	}

	/**
	 * Removes an existing Schedule. Sets next alert.
	 */
	public static void deleteSchedule(Context context, int alarmId) {
		if (alarmId == INVALID_SCHEDULE_ID)
			return;

		ContentResolver contentResolver = context.getContentResolver();

		Uri uri = ContentUris.withAppendedId(Schedule.Columns.CONTENT_URI,
				alarmId);
		contentResolver.delete(uri, "", null);

		setNextSchedule(context);
	}

	/**
	 * Queries all schedules
	 * 
	 * @return cursor over all schedules
	 */
	public static Cursor getSchedulesCursor(ContentResolver contentResolver) {
		return contentResolver.query(Schedule.Columns.CONTENT_URI,
				Schedule.Columns.SCHEDULE_QUERY_COLUMNS, null, null,
				Schedule.Columns.DEFAULT_SORT_ORDER);
	}

	// Private method to get a more limited set of schedules from the database.
	private static Cursor getFilteredSchedulesCursor(
			ContentResolver contentResolver) {
		return contentResolver.query(Schedule.Columns.CONTENT_URI,
				Schedule.Columns.SCHEDULE_QUERY_COLUMNS,
				Schedule.Columns.WHERE_ENABLED, null, null);
	}

	private static ContentValues createContentValues(Schedule schedule) {
		ContentValues values = new ContentValues(9);
		// Set the schedule_time value if this schedule does not repeat. This
		// will be used later to disable expire schedules.
		long time = 0;
		if (!schedule.daysOfWeek.isRepeatSet()) {
			time = calculateSchedule(schedule);
		}

		values.put(Schedule.Columns.ENABLED, schedule.enabled ? 1 : 0);
		values.put(Schedule.Columns.HOUR, schedule.hour);
		values.put(Schedule.Columns.MINUTES, schedule.minutes);
		values.put(Schedule.Columns.SCHEDULE_TIME, time);
		values.put(Schedule.Columns.DAYS_OF_WEEK,
				schedule.daysOfWeek.getCoded());
		values.put(Schedule.Columns.MODE, schedule.mode);
		values.put(Schedule.Columns.APONOFF, schedule.aponoff);
		values.put(Schedule.Columns.SILENTONOFF, schedule.silentonoff);
		values.put(Schedule.Columns.MESSAGE, schedule.label);

		return values;
	}

	/**
	 * Return a Schedule object representing the schedule id in the database.
	 * Returns null if no schedule exists.
	 */
	public static Schedule getSchedule(ContentResolver contentResolver,
			int scheduleId) {
		Cursor cursor = contentResolver.query(ContentUris.withAppendedId(
				Schedule.Columns.CONTENT_URI, scheduleId),
				Schedule.Columns.SCHEDULE_QUERY_COLUMNS, null, null, null);
		Schedule schedule = null;
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				schedule = new Schedule(cursor);
			}
			cursor.close();
		}
		return schedule;
	}

	/**
	 * A convenience method to set a schedule in the Schedules content provider.
	 * 
	 * @return Time when the schedule will be executed.
	 */
	public static long setSchedule(Context context, Schedule schedule) {
		ContentValues values = createContentValues(schedule);
		ContentResolver resolver = context.getContentResolver();
		resolver.update(ContentUris.withAppendedId(
				Schedule.Columns.CONTENT_URI, schedule.id), values, null, null);

		long timeInMillis = calculateSchedule(schedule);

		setNextSchedule(context);

		return timeInMillis;
	}

	/**
	 * A convenience method to enable or disable a schedule.
	 * 
	 * @param id
	 *            corresponds to the _id column
	 * @param enabled
	 *            corresponds to the ENABLED column
	 */

	public static void enableSchedule(final Context context, final int id,
			boolean enabled) {
		enableScheduleInternal(context, id, enabled);
		setNextSchedule(context);
	}

	private static void enableScheduleInternal(final Context context,
			final int id, boolean enabled) {
		enableScheduleInternal(context,
				getSchedule(context.getContentResolver(), id), enabled);
	}

	private static void enableScheduleInternal(final Context context,
			final Schedule schedule, boolean enabled) {
		if (schedule == null) {
			return;
		}
		ContentResolver resolver = context.getContentResolver();

		ContentValues values = new ContentValues(2);
		values.put(Schedule.Columns.ENABLED, enabled ? 1 : 0);

		// If we are enabling the schedule, calculate schedule time since the
		// time value in Schedule may be old.
		if (enabled) {
			long time = 0;
			if (!schedule.daysOfWeek.isRepeatSet()) {
				time = calculateSchedule(schedule);
			}
			values.put(Schedule.Columns.SCHEDULE_TIME, time);
		}

		resolver.update(ContentUris.withAppendedId(
				Schedule.Columns.CONTENT_URI, schedule.id), values, null, null);
	}
	
	private static Schedule calculateNextSchedule(final Context context) {
        long minTime = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        //final SharedPreferences prefs = context.getSharedPreferences(AlarmClock.PREFERENCES, 0);

        Set<Schedule> schedules = new HashSet<Schedule>();

        // We need to to build the list of schedules from the scheduled list.
        // For a non-repeating schedule, when it goes of, it becomes disabled.

        // Now add the scheduled schedules
        final Cursor cursor = getFilteredSchedulesCursor(context.getContentResolver());
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        final Schedule a = new Schedule(cursor);
                        schedules.add(a);
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }

        Schedule schedule = null;

        for (Schedule s : schedules) {
            // A time of 0 indicates this is a repeating schedule, so
            // calculate the time to get the next alert.
            if (s.time == 0) {
                s.time = calculateSchedule(s);
            }

            if (s.time < now) {
                // Expired schedule, disable it and move along.
                enableScheduleInternal(context, s, false);
                continue;
            }
            if (s.time < minTime) {
                minTime = s.time;
                schedule = s;
            }
        }

        return schedule;
    }

	/**
	 * Disables non-repeating schedules that have passed. Called at boot.
	 */
	public static void disableExpiredSchedules(final Context context) {
		Cursor cur = getFilteredSchedulesCursor(context.getContentResolver());
		long now = System.currentTimeMillis();

		try {
			if (cur.moveToFirst()) {
				do {
					Schedule schedule = new Schedule(cur);
					// A time of 0 means this schedule repeats. If the time is
					// non-zero, check if the time is before now.
					if (schedule.time != 0 && schedule.time < now) {
						enableScheduleInternal(context, schedule, false);
					}
				} while (cur.moveToNext());
			}
		} finally {
			cur.close();
		}
	}

	/**
	 * Called at system startup, on time/timezone change, and whenever the user
	 * changes schedule settings. Loads all alarms, activates next schedule.
	 */
	public static void setNextSchedule(final Context context) {
		final Schedule schedule = calculateNextSchedule(context);
		if (schedule != null) {
			enableSchedule(context, schedule, schedule.time);
		} else {
			disableSchedule(context);
		}
	}

	/**
	 * Sets schedule start in AlarmManger. This is what will actually launch the
	 * schedule when the schedule triggers.
	 * 
	 * @param schedule
	 *            Schedule.
	 * @param atTimeInMillis
	 *            milliseconds since epoch
	 */
	@SuppressLint("Recycle")
	private static void enableSchedule(Context context,
			final Schedule schedule, final long atTimeInMillis) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(SCHEDULE_ACTION);

		// XXX: This is a slight hack to avoid an exception in the remote
		// AlarmManagerService process. The AlarmManager adds extra data to
		// this Intent which causes it to inflate. Since the remote process
		// does not know about the Schedule class, it throws a
		// ClassNotFoundException.
		//
		// To avoid this, we marshall the data ourselves and then parcel a plain
		// byte[] array. The ScheduleReceiver class knows to build the Schedule
		// object from the byte[] array.
		Parcel out = Parcel.obtain();
		schedule.writeToParcel(out, 0);
		out.setDataPosition(0);
		intent.putExtra(SCHEDULE_RAW_DATA, out.marshall());

		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);

		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(atTimeInMillis);
	}

	/**
	 * Disables schedule in AlarmManger.
	 * 
	 * @param id
	 *            Schedule ID.
	 */
	static void disableSchedule(Context context) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0,
				new Intent(SCHEDULE_ACTION),
				PendingIntent.FLAG_CANCEL_CURRENT);
		am.cancel(sender);
	}

	private static long calculateSchedule(Schedule schedule) {
		return calculateSchedule(schedule.hour, schedule.minutes,
				schedule.daysOfWeek).getTimeInMillis();
	}

	/**
	 * Given a schedule in hours and minutes, return a time suitable for setting
	 * in AlarmManager.
	 */
	static Calendar calculateSchedule(int hour, int minute,
			Schedule.DaysOfWeek daysOfWeek) {

		// start with now
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());

		int nowHour = c.get(Calendar.HOUR_OF_DAY);
		int nowMinute = c.get(Calendar.MINUTE);

		// if alarm is behind current time, advance one day
		if (hour < nowHour || hour == nowHour && minute <= nowMinute) {
			c.add(Calendar.DAY_OF_YEAR, 1);
		}
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minute);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		int addDays = daysOfWeek.getNextSchedule(c);
		if (addDays > 0)
			c.add(Calendar.DAY_OF_WEEK, addDays);
		return c;
	}

	static String formatTime(final Context context, int hour, int minute,
			Schedule.DaysOfWeek daysOfWeek) {
		Calendar c = calculateSchedule(hour, minute, daysOfWeek);
		return formatTime(context, c);
	}

	/* used by AlarmAlert */
	static String formatTime(final Context context, Calendar c) {
		String format = get24HourMode(context) ? M24 : M12;
		return (c == null) ? "" : (String) DateFormat.format(format, c);
	}

	/**
	 * @return true if clock is set to 24-hour mode
	 */
	static boolean get24HourMode(final Context context) {
		return android.text.format.DateFormat.is24HourFormat(context);
	}
}
