package net.geniecode.ttr;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

public final class Schedule implements Parcelable {

	// ////////////////////////////
	// Parcelable apis
	// ////////////////////////////
	public static final Parcelable.Creator<Schedule> CREATOR = new Parcelable.Creator<Schedule>() {
		@Override
		public Schedule createFromParcel(Parcel p) {
			return new Schedule(p);
		}

		@Override
		public Schedule[] newArray(int size) {
			return new Schedule[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel p, int flags) {
		p.writeInt(id);
		p.writeInt(enabled ? 1 : 0);
		p.writeInt(start_hour);
		p.writeInt(start_minutes);
		p.writeInt(end_hour);
		p.writeInt(end_minutes);
		p.writeInt(daysOfWeek.getCoded());
		p.writeLong(start_time);
		p.writeLong(end_time);
		p.writeString(label);
	}

	// ////////////////////////////
	// end Parcelable apis
	// ////////////////////////////

	// ////////////////////////////
	// Column definitions
	// ////////////////////////////
	public static class Columns implements BaseColumns {
		/**
		 * The content:// style URL for this table
		 */
		public static final Uri CONTENT_URI = Uri
				.parse("content://net.geniecode.ttr/schedule");

		/**
		 * Hour in 24-hour localtime 0 - 23.
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String STARTHOUR = "start_hour";

		/**
		 * Minutes in localtime 0 - 59
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String STARTMINUTES = "start_minutes";

		/**
		 * Hour in 24-hour localtime 0 - 23.
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String ENDHOUR = "end_hour";

		/**
		 * Minutes in localtime 0 - 59
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String ENDMINUTES = "end_minutes";

		/**
		 * Days of week coded as integer
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String DAYS_OF_WEEK = "daysofweek";

		/**
		 * Schedule start time in UTC milliseconds from the epoch.
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String SCHEDULE_START_TIME = "schedulestarttime";

		/**
		 * Schedule end time in UTC milliseconds from the epoch.
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String SCHEDULE_END_TIME = "scheduleendtime";

		/**
		 * True if schedule is active
		 * <P>
		 * Type: BOOLEAN
		 * </P>
		 */
		public static final String ENABLED = "enabled";

		/**
		 * Message to show when schedule triggers Note: not currently used
		 * <P>
		 * Type: STRING
		 * </P>
		 */
		public static final String MESSAGE = "message";

		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = STARTHOUR + ", "
				+ STARTMINUTES + " ASC";

		// Used when filtering enabled schedules.
		public static final String WHERE_ENABLED = ENABLED + "=1";

		static final String[] SCHEDULE_QUERY_COLUMNS = { _ID, STARTHOUR,
				STARTMINUTES, ENDHOUR, ENDMINUTES, DAYS_OF_WEEK,
				SCHEDULE_START_TIME, SCHEDULE_END_TIME, ENABLED, MESSAGE };

		/**
		 * These save calls to cursor.getColumnIndexOrThrow() THEY MUST BE KEPT
		 * IN SYNC WITH ABOVE QUERY COLUMNS
		 */
		public static final int SCHEDULE_ID_INDEX = 0;
		public static final int SCHEDULE_STARTHOUR_INDEX = 1;
		public static final int SCHEDULE_STARTMINUTES_INDEX = 2;
		public static final int SCHEDULE_ENDHOUR_INDEX = 3;
		public static final int SCHEDULE_ENDMINUTES_INDEX = 4;
		public static final int SCHEDULE_DAYS_OF_WEEK_INDEX = 5;
		public static final int SCHEDULE_START_TIME_INDEX = 6;
		public static final int SCHEDULE_END_TIME_INDEX = 7;
		public static final int SCHEDULE_ENABLED_INDEX = 8;
		public static final int SCHEDULE_MESSAGE_INDEX = 9;
	}

	// ////////////////////////////
	// End column definitions
	// ////////////////////////////

	// Public fields
	public int id;
	public boolean enabled;
	public int start_hour;
	public int start_minutes;
	public int end_hour;
	public int end_minutes;
	public DaysOfWeek daysOfWeek;
	public long start_time;
	public long end_time;
	public String label;

	public Schedule(Cursor c) {
		id = c.getInt(Columns.SCHEDULE_ID_INDEX);
		enabled = c.getInt(Columns.SCHEDULE_ENABLED_INDEX) == 1;
		start_hour = c.getInt(Columns.SCHEDULE_STARTHOUR_INDEX);
		start_minutes = c.getInt(Columns.SCHEDULE_STARTMINUTES_INDEX);
		end_hour = c.getInt(Columns.SCHEDULE_ENDHOUR_INDEX);
		end_minutes = c.getInt(Columns.SCHEDULE_ENDMINUTES_INDEX);
		daysOfWeek = new DaysOfWeek(
				c.getInt(Columns.SCHEDULE_DAYS_OF_WEEK_INDEX));
		start_time = c.getLong(Columns.SCHEDULE_START_TIME_INDEX);
		end_time = c.getLong(Columns.SCHEDULE_END_TIME_INDEX);
		label = c.getString(Columns.SCHEDULE_MESSAGE_INDEX);
	}

	public Schedule(Parcel p) {
		id = p.readInt();
		enabled = p.readInt() == 1;
		start_hour = p.readInt();
		start_minutes = p.readInt();
		end_hour = p.readInt();
		end_minutes = p.readInt();
		daysOfWeek = new DaysOfWeek(p.readInt());
		start_time = p.readLong();
		end_time = p.readLong();
		label = p.readString();
	}

	// Creates a default schedule at the current time.
	public Schedule() {
		id = -1;
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		start_hour = c.get(Calendar.HOUR_OF_DAY);
		start_minutes = c.get(Calendar.MINUTE);
		end_hour = c.get(Calendar.HOUR_OF_DAY);
		end_minutes = c.get(Calendar.MINUTE);
		daysOfWeek = new DaysOfWeek(0);
	}

	public String getLabelOrDefault(Context context) {
		if (label == null || label.length() == 0) {
			return context.getString(R.string.default_label);
		}
		return label;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Schedule))
			return false;
		final Schedule other = (Schedule) o;
		return id == other.id;
	}

	/*
	 * Days of week code as a single int. 0x00: no day 0x01: Monday 0x02:
	 * Tuesday 0x04: Wednesday 0x08: Thursday 0x10: Friday 0x20: Saturday 0x40:
	 * Sunday
	 */
	static final class DaysOfWeek {

		private static int[] DAY_MAP = new int[] { Calendar.MONDAY,
				Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
				Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY, };

		// Bitmask of all repeating days
		private int mDays;

		DaysOfWeek(int days) {
			mDays = days;
		}

		public String toString(Context context, boolean showNever) {
			StringBuilder ret = new StringBuilder();

			// no days
			if (mDays == 0) {
				return showNever ? context.getText(R.string.never).toString()
						: "";
			}

			// every day
			if (mDays == 0x7f) {
				return context.getText(R.string.every_day).toString();
			}

			// count selected days
			int dayCount = 0, days = mDays;
			while (days > 0) {
				if ((days & 1) == 1)
					dayCount++;
				days >>= 1;
			}

			// short or long form?
			DateFormatSymbols dfs = new DateFormatSymbols();
			String[] dayList = (dayCount > 1) ? dfs.getShortWeekdays() : dfs
					.getWeekdays();

			// selected days
			for (int i = 0; i < 7; i++) {
				if ((mDays & (1 << i)) != 0) {
					ret.append(dayList[DAY_MAP[i]]);
					dayCount -= 1;
					if (dayCount > 0)
						ret.append(context.getText(R.string.day_concat));
				}
			}
			return ret.toString();
		}

		private boolean isSet(int day) {
			return ((mDays & (1 << day)) > 0);
		}

		public void set(int day, boolean set) {
			if (set) {
				mDays |= (1 << day);
			} else {
				mDays &= ~(1 << day);
			}
		}

		public void set(DaysOfWeek dow) {
			mDays = dow.mDays;
		}

		public int getCoded() {
			return mDays;
		}

		// Returns days of week encoded in an array of booleans.
		public boolean[] getBooleanArray() {
			boolean[] ret = new boolean[7];
			for (int i = 0; i < 7; i++) {
				ret[i] = isSet(i);
			}
			return ret;
		}

		public boolean isRepeatSet() {
			return mDays != 0;
		}

		/**
		 * returns number of days from today until next schedule
		 * 
		 * @param c
		 *            must be set to today
		 */
		public int getNextSchedule(Calendar c) {
			if (mDays == 0) {
				return -1;
			}

			int today = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;

			int day = 0;
			int dayCount = 0;
			for (; dayCount < 7; dayCount++) {
				day = (today + dayCount) % 7;
				if (isSet(day)) {
					break;
				}
			}
			return dayCount;
		}
	}
}
