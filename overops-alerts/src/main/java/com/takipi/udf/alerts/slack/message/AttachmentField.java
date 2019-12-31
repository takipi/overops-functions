package com.takipi.udf.alerts.slack.message;

public class AttachmentField {
	public final String title;
	public final String value;
	public final boolean isShort;

	private AttachmentField(String title, String value, boolean isShort) {
		this.title = title;
		this.value = value;
		this.isShort = isShort;
	}

	public static AttachmentField of(String title, String value, boolean isShort) {
		return new AttachmentField(title, value, isShort);
	}
}
