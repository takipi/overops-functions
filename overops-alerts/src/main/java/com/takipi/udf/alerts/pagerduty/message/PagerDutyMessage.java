package com.takipi.udf.alerts.pagerduty.message;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.takipi.udf.alerts.pagerduty.PagerDutyConsts;

public class PagerDutyMessage {
	public final String serviceKey;
	public final String eventType;
	public final String description;
	public final String incidentKey;
	public final String client;
	public final String clientUrl;
	public final String settingsUrl;
	public final Details details;
	public final List<Context> contexts;

	PagerDutyMessage(String serviceKey, String eventType, String description, String incidentKey, String client,
			String clientUrl, String settingsUrl, Details details, List<Context> contexts) {
		this.serviceKey = serviceKey;
		this.eventType = eventType;
		this.description = description;
		this.incidentKey = incidentKey;
		this.client = client;
		this.clientUrl = clientUrl;
		this.settingsUrl = settingsUrl;
		this.details = details;
		this.contexts = contexts;
	}

	public String toJson() {
		return PagerDutyConsts.GSON.toJson(this);
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String serviceKey;
		private String eventType;
		private String description;
		private String incidentKey;
		private String client;
		private String clientUrl;
		private String settingsUrl;
		private Details details;
		private final List<Context> contexts;

		Builder() {
			this.contexts = Lists.newArrayList();
		}

		public Builder setServiceKey(String val) {
			this.serviceKey = val;

			return this;
		}

		public Builder setEventType(String val) {
			this.eventType = val;

			return this;
		}

		public Builder setDescription(String val) {
			this.description = val;

			return this;
		}

		public Builder setIncidentKey(String val) {
			this.incidentKey = val;

			return this;
		}

		public Builder setClient(String val) {
			this.client = val;

			return this;
		}

		public Builder setClientUrl(String val) {
			this.clientUrl = val;

			return this;
		}

		public Builder setSettingsUrl(String val) {
			this.settingsUrl = val;

			return this;
		}

		public Builder setDetails(Details val) {
			this.details = val;

			return this;
		}

		public Builder addContext(Context val) {
			this.contexts.add(val);

			return this;
		}

		public Builder addAllContexts(Collection<Context> val) {
			this.contexts.addAll(val);

			return this;
		}

		public PagerDutyMessage build() {
			return new PagerDutyMessage(serviceKey, eventType, description, incidentKey, client, clientUrl, settingsUrl,
					details, contexts);
		}
	}
}
