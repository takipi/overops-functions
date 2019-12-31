package com.takipi.udf.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Minutes;
import org.joda.time.MutableDateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;

public class DateUtil {
	private static final String STR_SEPARATOR = " ";

	public static Date now() {
		return new Date();
	}

	public static long nowMillis() {
		return System.currentTimeMillis();
	}

	public static long nowNanos() {
		return System.nanoTime();
	}

	public static long nowMicros() {
		return (1000 * nowMillis());
	}

	public static String nowAsMillisString() {
		return Long.toString(nowMillis());
	}

	public static DateTime stringToDateTime(String value) {
		return new DateTime(Long.valueOf(value));
	}

	public static String toTimeString(Date date) {
		return DateFormat.getTimeInstance().format(date);
	}

	public static String toTimeDayString(Date date) {
		DateFormat formatter = new SimpleDateFormat("MMM d HH:mm");
		return formatter.format(date);
	}

	public static String toTimeStringAmPmNoSeconds(Date date) {
		DateFormat formatter = new SimpleDateFormat("HH:mm aa");
		return formatter.format(date);
	}

	public static String toTimeStringNoSeconds(Date date) {
		DateFormat formatter = new SimpleDateFormat("HH:mm");
		return formatter.format(date);
	}

	public static boolean isToday(long input) {
		DateTime date = new DateTime(input);

		return isToday(date);
	}

	public static boolean isToday(Date input) {
		DateTime date = new DateTime(input);

		return isToday(date);
	}

	public static String getDayOfWeek(Date input) {
		DateTime dt = new DateTime(input);
		DateTime.Property pDoW = dt.dayOfWeek();

		return pDoW.getAsText(Locale.US);
	}

	public static String toTimeOrShortDate(Date input) {
		DateTime date = new DateTime(input);

		if (isToday(date)) {
			return date.toString("h:mm a", Locale.US);
		}

		return date.toString("MMM d", Locale.US);
	}

	public static DateTime yesterday() {
		return DateTime.now().minusDays(1);
	}

	public static long yesterdayAsLong() {
		return yesterday().getMillis();
	}

	public static String yesterdayAsString() {
		Long yesterday = yesterday().getMillis();
		return Long.toString(yesterday);
	}

	public static String toShortDate(Date input) {
		return toShortDate(input, false);
	}

	public static String toShortDate(Date input, boolean withSuffix) {
		return toShortDate(new DateTime(input), withSuffix);
	}

	public static String toShortDate(DateTime input) {
		return toShortDate(input, false);
	}

	public static String toShortDate(DateTime input, boolean withSuffix) {
		StringBuilder builder = new StringBuilder(input.toString("MMM d", Locale.US));

		if (withSuffix) {
			builder.append(getDaySuffix(input));
		}

		return builder.toString();
	}

	public static String getDaySuffix(DateTime input) {
		return getDaySuffix(input.getDayOfMonth());
	}

	public static String getDaySuffix(int day) {
		switch (day) {
		case 1:
		case 21:
		case 31:
			return "st";
		case 2:
		case 22:
			return "nd";
		case 3:
		case 23:
			return "rd";
		default:
			return "th";
		}
	}

	public static String toThisDayAndSixDaysAgo(Date input) {
		DateTime date = new DateTime(input);

		return toThisDayAndSixDaysAgo(date);
	}

	public static String toThisDayAndSixDaysAgo(DateTime date) {
		DateTime sixDaysAgo = date.minusDays(6);

		StringBuilder builder = new StringBuilder();

		builder.append(toShortDate(sixDaysAgo, true));
		builder.append(" - ");
		builder.append(toShortDate(date, true));

		return builder.toString();
	}

	public static String toDateString(Date date) {
		return DateFormat.getDateInstance().format(date);
	}

	public static String toDateTimeString(long millis) {
		return toDateTimeString(fromMillis(millis));
	}

	public static String toDateTimeString(Date date) {
		return toDateString(date) + STR_SEPARATOR + toTimeString(date);
	}

	public static Date addMillis(Date date, int millis) {
		DateTime dt = new DateTime(date);
		return dt.plusMillis(millis).toDate();
	}

	public static Date addSeconds(Date date, int seconds) {
		DateTime dt = new DateTime(date);
		return dt.plusSeconds(seconds).toDate();
	}

	public static Date addMinutes(Date date, int minutes) {
		DateTime dt = new DateTime(date);
		return dt.plusMinutes(minutes).toDate();
	}

	public static Date addHours(Date date, int hours) {
		DateTime dt = new DateTime(date);
		return dt.plusHours(hours).toDate();
	}

	public static Date addDays(Date date, int days) {
		DateTime dt = new DateTime(date);
		return dt.plusDays(days).toDate();
	}

	public static Date removeDays(Date date, int days) {
		DateTime dt = new DateTime(date);
		return dt.minusDays(days).toDate();
	}

	public static Date addYears(Date date, int years) {
		DateTime dt = new DateTime(date);
		return dt.plusYears(years).toDate();
	}

	public static long toMillis(Date date) {
		return date.getTime();
	}

	public static String toMillisString(long millis) {
		return Long.toString(millis);
	}

	public static String toMillisString(Date date) {
		return toMillisString(toMillis(date));
	}

	public static String toMillisString(DateTime dt) {
		return toMillisString(dt.getMillis());
	}

	public static Date fromMillis(long millis) {
		return new Date(millis);
	}

	public static Date fromMillis(String millis) {
		if (millis == null) {
			return fromMillis(0);
		}

		return fromMillis(Long.parseLong(millis));
	}

	public static DateTime safeToDateTime(String date) {
		return ((date == null) ? null : fromMillisToDateTime(date));
	}

	public static DateTime getDateTime(int year, int month, int day, int hour, int minute, int second, int millis) {
		try {
			return new DateTime(year, month, day, hour, minute, second, millis);
		} catch (Exception ex) {
			return null;
		}
	}

	public static DateTime fromMillisToDateTime(String millis) {
		return fromMillisToDateTime(Long.parseLong(millis));
	}

	public static DateTime fromMillisToDateTime(long millis) {
		return new DateTime(millis);
	}

	public static String fromMillisToIso(String millis) {
		return fromMillisToIso(Long.parseLong(millis));
	}

	public static String fromMillisToIso(long millis) {
		DateTimeFormatter isoFormatter = ISODateTimeFormat.dateTime().withZoneUTC();

		return isoFormatter.print(millis);
	}

	public static long fromIsoToMillis(String isoDate) throws IllegalArgumentException {
		DateTime dt = new DateTime(isoDate);

		return dt.getMillis();
	}

	public static int compare(String date1, String date2) {
		if ((date1 == null) && (date2 == null))
			return 0;

		if (date1 == null)
			return -1;
		if (date2 == null)
			return 1;

		long l1 = Long.parseLong(date1);
		long l2 = Long.parseLong(date2);

		return ((l1 < l2) ? -1 : ((l1 == l2) ? 0 : 1));
	}

	public static boolean isBeforeOrEqual(String date1, String date2) {
		if (Strings.isNullOrEmpty(date1) && Strings.isNullOrEmpty(date2))
			return true;

		if (Strings.isNullOrEmpty(date1))
			return true;
		if (Strings.isNullOrEmpty(date2))
			return false;

		long l1 = Long.parseLong(date1);
		long l2 = Long.parseLong(date2);

		return (l1 <= l2);
	}

	public static long truncSeconds(long timestamp) {
		MutableDateTime dt = new MutableDateTime(timestamp);
		dt.setSecondOfMinute(0);
		dt.setMillisOfSecond(0);

		return dt.getMillis();
	}

	public static boolean isToday(DateTime dateTime) {
		DateTime startOfToday = DateTime.now().withTimeAtStartOfDay();
		DateTime startOfOtherDay = dateTime.withTimeAtStartOfDay();

		return startOfToday.equals(startOfOtherDay);
	}

	public static String toDaysAgoTimestamp(long millis, int days) {
		long dayMillis = 1000 * 60 * 60 * 24;
		long daysMillis = dayMillis * days;

		return Long.toString(millis - daysMillis);
	}

	public static long howManyHoursPassed(long dateMillis) {
		return howManyHoursPassed(dateMillis, nowMillis());
	}

	public static long howManyHoursPassed(long from, long to) {
		long hourMillis = 1000 * 60 * 60;
		long diff = to - from;

		if (diff < 0) {
			return 0;
		}

		return (diff / hourMillis);
	}

	public static long howManyHoursPassed(String dateMillis) {
		return howManyHoursPassed(Long.parseLong(dateMillis));
	}

	public static long microsToMillis(long micros) {
		return (micros / 1000);
	}

	public static long millisToSeconds(long millis) {
		return (millis / 1000);
	}

	public static long microsToSeconds(long micros) {
		return (micros / 1000000);
	}

	public static boolean isInbetween(Date date, Date beginDate, Date endDate) {
		if ((date == null) || (beginDate == null) || (endDate == null)) {
			return false;
		}

		return ((date.after(beginDate)) && (date.before(endDate)));
	}

	public static DateTime maxDateTime(String date1, String date2) {
		if ((date1 == null) && (date2 == null))
			return null;

		if (date1 == null)
			return stringToDateTime(date2);
		if (date2 == null)
			return stringToDateTime(date1);

		long l1 = Long.parseLong(date1);
		long l2 = Long.parseLong(date2);

		return new DateTime(Math.max(l1, l2));
	}

	public static DateTime minDateTime(DateTime date1, DateTime date2) {
		if (date1.isBefore(date2)) {
			return date1;
		} else {
			return date2;
		}
	}

	public static DateTime maxDateTime(DateTime date1, DateTime date2) {
		if (date1.isAfter(date2)) {
			return date1;
		} else {
			return date2;
		}
	}

	// Joda's toStandardMinutes for org.joda.time.Period can throw an exception in
	// case the period
	// contains a month or more, since the minutes can vary depending on the month.
	// This method returns the minutes of the given period based on current time.
	public static Minutes safeToStandardMinutes(Period period) {
		Duration duration = period.toDurationFrom(new DateTime());

		return duration.toStandardMinutes();
	}
}
