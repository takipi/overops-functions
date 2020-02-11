package com.takipi.udf.input;

public class TimeInterval {
	private static final int M_TO_H = 60;
	private static final int M_TO_D = 60 * 24;
	private static final int M_TO_W = 60 * 24 * 7;

	private final String raw;
	private final int minutes;

	private TimeInterval(String raw, int minutes) {
		this.raw = raw;
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

	public int asWeeks() {
		return (minutes / (M_TO_W));
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

	@Override
	public String toString() {
		return raw;
	}

	private static TimeInterval of(String raw, int minutes) {
		return new TimeInterval(raw, minutes);
	}

	public static TimeInterval of(int minutes) {
		return new TimeInterval(minutes + "m", minutes);
	}

	public static TimeInterval parse(String s) {
		if ((s == null) || (s.isEmpty())) {
			return of(0);
		}

		char timeUnit = s.charAt(s.length() - 1);
		String timeWindow = s.substring(0, s.length() - 1);

		switch (timeUnit) {
		case 'w':
			return of(s, Integer.valueOf(timeWindow) * M_TO_W);
		case 'd':
			return of(s, Integer.valueOf(timeWindow) * M_TO_D);
		case 'h':
			return of(s, Integer.valueOf(timeWindow) * M_TO_H);
		case 'm':
			return of(s, Integer.valueOf(timeWindow));
		}

		return of(Integer.valueOf(s));
	}
}
