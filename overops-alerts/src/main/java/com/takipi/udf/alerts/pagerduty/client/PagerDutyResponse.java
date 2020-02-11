package com.takipi.udf.alerts.pagerduty.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.takipi.common.util.CollectionUtil;

public class PagerDutyResponse {
	public boolean success;
	public String status;
	public String responseMessage;
	public String incidentKey;
	public List<String> errors;

	// To be used by GSON.
	//
	public PagerDutyResponse() {

	}

	PagerDutyResponse(boolean success, String status, String responseMessage, String incidentKey, List<String> errors) {
		this.success = success;
		this.status = status;
		this.responseMessage = responseMessage;
		this.incidentKey = incidentKey;
		this.errors = errors;
	}

	public Builder toBuilder() {
		Builder builder = newBuilder().setSuccess(success).setStatus(status).setResponseMessage(responseMessage)
				.setIncidentKey(incidentKey);

		if (!CollectionUtil.safeIsEmpty(errors)) {
			builder.addAllErrors(errors);
		}

		return builder;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private boolean success;
		private String status;
		private String responseMessage;
		private String incidentKey;
		private final List<String> errors;

		Builder() {
			this.errors = new ArrayList<>();
		}

		public Builder setSuccess(boolean val) {
			this.success = val;

			return this;
		}

		public Builder setStatus(String val) {
			this.status = val;

			return this;
		}

		public Builder setResponseMessage(String val) {
			this.responseMessage = val;

			return this;
		}

		public Builder setIncidentKey(String val) {
			this.incidentKey = val;

			return this;
		}

		public Builder addError(String val) {
			this.errors.add(val);

			return this;
		}

		public Builder addAllErrors(Collection<String> val) {
			this.errors.addAll(val);

			return this;
		}

		public PagerDutyResponse build() {
			return new PagerDutyResponse(success, status, responseMessage, incidentKey, errors);
		}
	}
}
