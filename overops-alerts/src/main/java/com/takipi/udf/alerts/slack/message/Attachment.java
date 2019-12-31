package com.takipi.udf.alerts.slack.message;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

public class Attachment {
	public final String fallback;
	public final String color;
	public final String pretext;
	public final String authorName;
	public final String authorLink;
	public final String authorIcon;
	public final String title;
	public final String titleLink;
	public final String text;
	public final List<AttachmentField> fields;
	public final String imageUrl;
	public final String thumbUrl;
	public final List<String> mrkdwnIn;
	public final String footer;

	Attachment(String fallback, String color, String pretext, String authorName, String authorLink, String authorIcon,
			String title, String titleLink, String text, List<AttachmentField> fields, String imageUrl, String thumbUrl,
			List<String> mrkdwnIn, String footer) {
		this.fallback = fallback;
		this.color = color;
		this.pretext = pretext;
		this.authorName = authorName;
		this.authorLink = authorLink;
		this.authorIcon = authorIcon;
		this.title = title;
		this.titleLink = titleLink;
		this.text = text;
		this.fields = fields;
		this.imageUrl = imageUrl;
		this.thumbUrl = thumbUrl;
		this.mrkdwnIn = mrkdwnIn;
		this.footer = footer;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String fallback;
		private String color;
		private String pretext;
		private String authorName;
		private String authorLink;
		private String authorIcon;
		private String title;
		private String titleLink;
		private String text;
		private List<AttachmentField> fields;
		private String imageUrl;
		private String thumbUrl;
		private List<String> mrkdwnIn;
		private String footer;

		Builder() {
			this.fields = Lists.newArrayList();
			this.mrkdwnIn = Lists.newArrayList();
		}

		public Builder setFallback(String val) {
			this.fallback = val;
			return this;
		}

		public Builder setColor(String val) {
			this.color = val;
			return this;
		}

		public Builder setPretext(String val) {
			this.pretext = val;
			return this;
		}

		public Builder setAuthorName(String val) {
			this.authorName = val;
			return this;
		}

		public Builder setAuthorLink(String val) {
			this.authorLink = val;
			return this;
		}

		public Builder setAuthorIcon(String val) {
			this.authorIcon = val;
			return this;
		}

		public Builder setTitle(String val) {
			this.title = val;
			return this;
		}

		public Builder setTitleLink(String val) {
			this.titleLink = val;
			return this;
		}

		public Builder setText(String val) {
			this.text = val;
			return this;
		}

		public Builder addField(String title, String value, boolean isShort) {
			this.fields.add(AttachmentField.of(title, value, isShort));
			return this;
		}

		public Builder addField(AttachmentField val) {
			this.fields.add(val);
			return this;
		}

		public Builder addAllFields(Collection<AttachmentField> values) {
			this.fields.addAll(values);
			return this;
		}

		public Builder setImageUrl(String val) {
			this.imageUrl = val;
			return this;
		}

		public Builder setThumbUrl(String val) {
			this.thumbUrl = val;
			return this;
		}

		public Builder addMrkdwn(String val) {
			this.mrkdwnIn.add(val);
			return this;
		}

		public Builder setFooter(String val) {
			this.footer = val;
			return this;
		}

		public Attachment build() {
			return new Attachment(fallback, color, pretext, authorName, authorLink, authorIcon, title, titleLink, text,
					fields, imageUrl, thumbUrl, mrkdwnIn, footer);
		}
	}
}
