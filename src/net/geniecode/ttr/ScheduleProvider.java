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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class ScheduleProvider extends ContentProvider {
	private SQLiteOpenHelper mOpenHelper;

	private static final int SCHEDULES = 1;
	private static final int SCHEDULES_ID = 2;
	private static final UriMatcher sURLMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);

	static {
		sURLMatcher.addURI("net.geniecode.ttr", "schedule", SCHEDULES);
		sURLMatcher.addURI("net.geniecode.ttr", "schedule/#", SCHEDULES_ID);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "apscheduler.db";
		private static final int DATABASE_VERSION = 8;

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE schedules (" + "_id INTEGER PRIMARY KEY,"
					+ "hour INTEGER, " + "minutes INTEGER, "
					+ "daysofweek INTEGER, " + "scheduletime INTEGER, "
					+ "enabled INTEGER, " + "mode TEXT, " + "aponoff INTEGER, "
					+ "silentonoff INTEGER, " + "message TEXT);");

			/**
			 * Numeric values for days of week: Sunday = 64 Saturday = 32 Friday
			 * = 16 Thursday = 8 Wednesday = 4 Tuesday = 2 Monday = 1;
			 */

			// insert default schedules
			String insertMe = "INSERT INTO schedules "
					+ "(hour, minutes, daysofweek, "
					+ "scheduletime, enabled, mode, "
					+ "aponoff, silentonoff, message) "
					+ "VALUES ";
			db.execSQL(insertMe + "(23, 0, 127, 0, 0, '2', 0, 1, 'Go to sleep');");
			db.execSQL(insertMe + "(6, 0, 127, 0, 0, '2', 0, 0, 'Wake up');");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion,
				int currentVersion) {
			db.execSQL("DROP TABLE IF EXISTS schedules");
			onCreate(db);
		}
	}

	public ScheduleProvider() {
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri url, String[] projectionIn, String selection,
			String[] selectionArgs, String sort) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		// Generate the body of the query
		int match = sURLMatcher.match(url);
		switch (match) {
		case SCHEDULES:
			qb.setTables("schedules");
			break;
		case SCHEDULES_ID:
			qb.setTables("schedules");
			qb.appendWhere("_id=");
			qb.appendWhere(url.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor ret = qb.query(db, projectionIn, selection, selectionArgs, null,
				null, sort);

		if (ret == null) {
			System.out.println("Schedules.query: failed");
		} else {
			ret.setNotificationUri(getContext().getContentResolver(), url);
		}

		return ret;
	}

	@Override
	public String getType(Uri url) {
		int match = sURLMatcher.match(url);
		switch (match) {
		case SCHEDULES:
			return "vnd.android.cursor.dir/schedules";
		case SCHEDULES_ID:
			return "vnd.android.cursor.item/schedules";
		default:
			throw new IllegalArgumentException("Unknown URL");
		}
	}

	@Override
	public int update(Uri url, ContentValues values, String where,
			String[] whereArgs) {
		int count;
		long rowId = 0;
		int match = sURLMatcher.match(url);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		switch (match) {
		case SCHEDULES_ID: {
			String segment = url.getPathSegments().get(1);
			rowId = Long.parseLong(segment);
			count = db.update("schedules", values, "_id=" + rowId, null);
			break;
		}
		default: {
			throw new UnsupportedOperationException("Cannot update URL: " + url);
		}
		}
		getContext().getContentResolver().notifyChange(url, null);
		return count;
	}

	@Override
	public Uri insert(Uri url, ContentValues initialValues) {
		if (sURLMatcher.match(url) != SCHEDULES) {
			throw new IllegalArgumentException("Cannot insert into URL: " + url);
		}

		ContentValues values;
		if (initialValues != null)
			values = new ContentValues(initialValues);
		else
			values = new ContentValues();

		if (!values.containsKey(Schedule.Columns.HOUR))
			values.put(Schedule.Columns.HOUR, 0);

		if (!values.containsKey(Schedule.Columns.MINUTES))
			values.put(Schedule.Columns.MINUTES, 0);

		if (!values.containsKey(Schedule.Columns.DAYS_OF_WEEK))
			values.put(Schedule.Columns.DAYS_OF_WEEK, 0);

		if (!values.containsKey(Schedule.Columns.SCHEDULE_TIME))
			values.put(Schedule.Columns.SCHEDULE_TIME, 0);

		if (!values.containsKey(Schedule.Columns.ENABLED))
			values.put(Schedule.Columns.ENABLED, 0);
		
		if (!values.containsKey(Schedule.Columns.MODE))
			values.put(Schedule.Columns.MODE, "");
		
		if (!values.containsKey(Schedule.Columns.APONOFF))
			values.put(Schedule.Columns.APONOFF, 0);
		
		if (!values.containsKey(Schedule.Columns.SILENTONOFF))
			values.put(Schedule.Columns.SILENTONOFF, 0);

		if (!values.containsKey(Schedule.Columns.MESSAGE))
			values.put(Schedule.Columns.MESSAGE, "");

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId = db.insert("schedules", Schedule.Columns.MESSAGE, values);
		if (rowId < 0) {
			throw new SQLException("Failed to insert row into " + url);
		}

		Uri newUrl = ContentUris.withAppendedId(Schedule.Columns.CONTENT_URI,
				rowId);
		getContext().getContentResolver().notifyChange(newUrl, null);
		return newUrl;
	}

	@Override
	public int delete(Uri url, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		@SuppressWarnings("unused")
		long rowId = 0;
		switch (sURLMatcher.match(url)) {
		case SCHEDULES:
			count = db.delete("schedules", where, whereArgs);
			break;
		case SCHEDULES_ID:
			String segment = url.getPathSegments().get(1);
			rowId = Long.parseLong(segment);
			if (TextUtils.isEmpty(where)) {
				where = "_id=" + segment;
			} else {
				where = "_id=" + segment + " AND (" + where + ")";
			}
			count = db.delete("schedules", where, whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Cannot delete from URL: " + url);
		}

		getContext().getContentResolver().notifyChange(url, null);
		return count;
	}
}
