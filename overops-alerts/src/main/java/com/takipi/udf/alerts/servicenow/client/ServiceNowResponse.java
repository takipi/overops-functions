package com.takipi.udf.alerts.servicenow.client;

public class ServiceNowResponse {
	public final boolean success;
	public final int statusCode;
	public final String rawJson;

	ServiceNowResponse(boolean success, int statusCode, String rawJson) {
		this.success = success;
		this.statusCode = statusCode;
		this.rawJson = rawJson;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private boolean success;
		private int statusCode;
		private String rawJson;

		public Builder setSuccess(boolean success) {
			this.success = success;

			return this;
		}

		public Builder setStatusCode(int statusCode) {
			this.statusCode = statusCode;

			return this;
		}

		public Builder setRawJson(String rawJson) {
			this.rawJson = rawJson;

			return this;
		}

		public ServiceNowResponse build() {
			return new ServiceNowResponse(success, statusCode, rawJson);
		}
	}
}
