package com.takipi.udf.alerts.slack.message;

import java.util.List;

import com.google.common.collect.Lists;

public class Message {
	public final String text;
	public final String username;
	public final String iconUrl;
	public final String iconEmoji;
	public final String channel;
	public final List<Attachment> attachments;

	Message(String text, String username, String iconUrl, String iconEmoji, String channel,
			List<Attachment> attachments) {
		this.text = text;
		this.username = username;
		this.iconUrl = iconUrl;
		this.iconEmoji = iconEmoji;
		this.channel = channel;
		this.attachments = attachments;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String text;
		private String username;
		private String iconUrl;
		private String iconEmoji;
		private String channel;
		private List<Attachment> attachments;

		Builder() {
			this.attachments = Lists.newArrayList();
		}

		public Builder setText(String val) {
			this.text = val;
			return this;
		}

		public Builder setUsername(String val) {
			this.username = val;
			return this;
		}

		public Builder setIconUrl(String val) {
			this.iconUrl = val;
			return this;
		}

		public Builder setIconEmoji(String val) {
			this.iconEmoji = val;
			return this;
		}

		public Builder setChannel(String val) {
			this.channel = val;
			return this;
		}

		public Builder addAttachment(Attachment val) {
			this.attachments.add(val);
			return this;
		}

		public Message build() {
			return new Message(text, username, iconUrl, iconEmoji, channel, attachments);
		}
	}
}
