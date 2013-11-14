package net.geniecode.ttr;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Displays the time
 */
public class DigitalClock extends RelativeLayout {

	private final static String M12 = "h:mm";

	private Calendar mCalendarStart;
	private Calendar mCalendarEnd;
	private String mFormat;
	private AndroidClockTextView mStartTimeDisplay;
	private AmPm mStartAmPm;
	private AndroidClockTextView mEndTimeDisplay;
	private AmPm mEndAmPm;
	private ContentObserver mFormatChangeObserver;
	private boolean mLive = true;
	private boolean mAttached;

	/* called by system on minute ticks */
	private final Handler mHandler = new Handler();
	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (mLive
					&& intent.getAction()
							.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				mCalendarStart = Calendar.getInstance();
				mCalendarEnd = Calendar.getInstance();
			}
			// Post a runnable to avoid blocking the broadcast.
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					updateTime();
				}
			});
		}
	};

	static class AmPm {
		private AndroidClockTextView mAmPm;
		private String mAmString, mPmString;

		AmPm(View parent, int id) {
			mAmPm = (AndroidClockTextView) parent.findViewById(id);

			String[] ampm = new DateFormatSymbols().getAmPmStrings();
			mAmString = ampm[0];
			mPmString = ampm[1];
		}

		void setShowAmPm(boolean show) {
			mAmPm.setVisibility(show ? View.VISIBLE : View.GONE);
		}

		void setIsMorning(boolean isMorning) {
			mAmPm.setText(isMorning ? mAmString : mPmString);
		}
	}

	private class FormatChangeObserver extends ContentObserver {
		public FormatChangeObserver() {
			super(new Handler());
		}

		@Override
		public void onChange(boolean selfChange) {
			setDateFormat();
			updateTime();
		}
	}

	public DigitalClock(Context context) {
		this(context, null);
	}

	public DigitalClock(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		mStartTimeDisplay = (AndroidClockTextView) findViewById(R.id.startTimeDisplay);
		mEndTimeDisplay = (AndroidClockTextView) findViewById(R.id.endTimeDisplay);
		mStartAmPm = new AmPm(this, R.id.start_am_pm);
		mEndAmPm = new AmPm(this, R.id.end_am_pm);
		mCalendarStart = Calendar.getInstance();
		mCalendarEnd = Calendar.getInstance();

		setDateFormat();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if (mAttached)
			return;
		mAttached = true;

		if (mLive) {
			/* monitor time ticks, time changed, timezone */
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_TIME_TICK);
			filter.addAction(Intent.ACTION_TIME_CHANGED);
			filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
			getContext().registerReceiver(mIntentReceiver, filter);
		}

		/* monitor 12/24-hour display preference */
		mFormatChangeObserver = new FormatChangeObserver();
		getContext().getContentResolver().registerContentObserver(
				Settings.System.CONTENT_URI, true, mFormatChangeObserver);

		updateTime();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		if (!mAttached)
			return;
		mAttached = false;

		if (mLive) {
			getContext().unregisterReceiver(mIntentReceiver);
		}
		getContext().getContentResolver().unregisterContentObserver(
				mFormatChangeObserver);
	}

	void updateTime(Calendar c_start, Calendar c_end) {
		mCalendarStart = c_start;
		mCalendarEnd = c_end;
		updateTime();
	}

	private void updateTime() {
		if (mLive) {
			mCalendarStart.setTimeInMillis(System.currentTimeMillis());
			mCalendarEnd.setTimeInMillis(System.currentTimeMillis());
		}

		CharSequence newStartTime = DateFormat.format(mFormat, mCalendarStart);
		CharSequence newEndTime = DateFormat.format(mFormat, mCalendarEnd);
		mStartTimeDisplay.setText(newStartTime);
		mEndTimeDisplay.setText(newEndTime);
		mStartAmPm.setIsMorning(mCalendarStart.get(Calendar.AM_PM) == 0);
		mEndAmPm.setIsMorning(mCalendarEnd.get(Calendar.AM_PM) == 0);
	}

	private void setDateFormat() {
		mFormat = Schedules.get24HourMode(getContext()) ? Schedules.M24 : M12;
		mStartAmPm.setShowAmPm(mFormat == M12);
		mEndAmPm.setShowAmPm(mFormat == M12);
	}

	void setLive(boolean live) {
		mLive = live;
	}
}
