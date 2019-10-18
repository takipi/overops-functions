package com.takipi.udf;

import com.google.common.base.Strings;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.core.url.UrlClient.LogLevel;

import java.net.HttpURLConnection;

public class ContextArgs {
	public String appHost;
	public String apiHost;
	public String grafanaHost;
	public String serviceId;
	public String libraryId;
	public String functionId;
	public String eventId;
	public String viewId;
	public String apiKey;
	public String resurface;

	public ContextArgs() {}

	public ContextArgs(String appHost, String apiHost, String serviceId, String eventId, String viewId, String apiKey, String resurface) {
		this.appHost = appHost;
		this.apiHost = apiHost;
		this.serviceId = serviceId;
		this.eventId = eventId;
		this.viewId = viewId;
		this.apiKey = apiKey;
		this.resurface = resurface;
	}

	public boolean validate() {
		return ((!Strings.isNullOrEmpty(serviceId)) && (!Strings.isNullOrEmpty(apiKey)));
	}

	public boolean eventValidate() {
		return ((validate()) && (!Strings.isNullOrEmpty(eventId)));
	}

	public boolean viewValidate() {
		return ((validate()) && (!Strings.isNullOrEmpty(viewId)));
	}

	public ApiClient apiClient() {
		return RemoteApiClient.newBuilder().setHostname(apiHost).setApiKey(apiKey).setDefaultLogLevel(LogLevel.WARN)
				.setResponseLogLevel(HttpURLConnection.HTTP_CONFLICT, LogLevel.INFO).build();
	}

	public static ContextArgsBuilder newBuilder() {
		return new ContextArgsBuilder();
	}

	public static class ContextArgsBuilder {
		private String appHost;
		private String apiHost;
		private String serviceId;
		private String eventId;
		private String viewId;
		private String apiKey;
		private String resurface;

		public ContextArgsBuilder setAppHost(String appHost) {
			this.appHost = appHost;
			return this;
		}

		public ContextArgsBuilder setApiHost(String apiHost) {
			this.apiHost = apiHost;
			return this;
		}

		public ContextArgsBuilder setServiceId(String serviceId) {
			this.serviceId = serviceId;
			return this;
		}

		public ContextArgsBuilder setEventId(String eventId) {
			this.eventId = eventId;
			return this;
		}

		public ContextArgsBuilder setViewId(String viewId) {
			this.viewId = viewId;
			return this;
		}

		public ContextArgsBuilder setApiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public ContextArgsBuilder setResurface(String resurface) {
			this.resurface = resurface;
			return this;
		}

		public ContextArgs build() {
			return new ContextArgs(appHost, apiHost, serviceId, eventId, viewId, apiKey, resurface);
		}
	}
}
