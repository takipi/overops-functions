package com.takipi.udf.input;

import com.google.common.base.Strings;

public class TimeInterval {
	private static final int M_TO_H = 60;
	private static final int M_TO_D = 60 * 24;

	private final int minutes;

	private TimeInterval(int minutes) {
		this.minutes = minutes;
	}

	public int asMinutes() {
		return minutes;
	}

	public int asHours() {
		return (minutes / M_TO_H);
	}

	public int asDays() {
		return (minutes / (M_TO_D));
	}

	public boolean isPositive() {
		return (minutes > 0);
	}

	public boolean isZero() {
		return (minutes == 0);
	}

	public boolean isNegative() {
		return (minutes < 0);
	}

	public static TimeInterval of(int minutes) {
		return new TimeInterval(minutes);
	}

	public static TimeInterval parse(String s) {
		if (Strings.isNullOrEmpty(s)) {
			return of(0);
		}

		char timeUnit = s.charAt(s.length() - 1);
		String timeWindow = s.substring(0, s.length() - 1);

		switch (timeUnit) {
		case 'd':
			return of(Integer.valueOf(timeWindow) * M_TO_D);
		case 'h':
			return of(Integer.valueOf(timeWindow) * M_TO_H);
		case 'm':
			return of(Integer.valueOf(timeWindow));
		}

		return of(Integer.valueOf(s));
	}
}
