package com.takipi.udf.alerts.slack.client;

public class SlackResponse {
	public static final SlackResponse OK = of(true);

	public final boolean ok;
	public final String error;

	private SlackResponse(boolean ok, String error) {
		this.ok = ok;
		this.error = error;
	}

	public static SlackResponse of(boolean ok) {
		return of(ok, null);
	}

	public static SlackResponse of(boolean ok, String error) {
		return new SlackResponse(ok, error);
	}
}
