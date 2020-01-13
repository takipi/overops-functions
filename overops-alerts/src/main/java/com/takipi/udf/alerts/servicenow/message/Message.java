package com.takipi.udf.alerts.servicenow.message;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Message {
	private static final Gson GSON = new GsonBuilder()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

	public final String shortDescription;
	public final String comments;

	Message(String shortDescription, String comments) {
		this.shortDescription = shortDescription;
		this.comments = comments;
	}

	public String toJson() {
		return GSON.toJson(this);
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String shortDescription;
		private String comments;

		public Builder setShortDescription(String shortDescription) {
			this.shortDescription = shortDescription;

			return this;
		}

		public Builder setComments(String comments) {
			this.comments = comments;

			return this;
		}

		public Message build() {
			return new Message(shortDescription, comments);
		}
	}
}
