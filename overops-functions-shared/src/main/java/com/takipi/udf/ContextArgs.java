package com.takipi.udf;

import java.net.HttpURLConnection;

import com.takipi.api.client.ApiClient;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.core.url.UrlClient.LogLevel;

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

	// This is used for Gson parsing.
	//
	public ContextArgs() {

	}

	// Should only be called from the builder.
	//
	ContextArgs(String appHost, String apiHost, String grafanaHost, String serviceId, String libraryId,
			String functionId, String eventId, String viewId, String apiKey, String resurface) {
		this.appHost = appHost;
		this.apiHost = apiHost;
		this.grafanaHost = grafanaHost;
		this.serviceId = serviceId;
		this.libraryId = libraryId;
		this.functionId = functionId;
		this.eventId = eventId;
		this.viewId = viewId;
		this.apiKey = apiKey;
		this.resurface = resurface;
	}

	private static boolean isNullOrEmpty(String s) {
		return ((s == null) || (s.isEmpty()));
	}
	
	public boolean validate() {
		return ((!isNullOrEmpty(serviceId)) && (!isNullOrEmpty(apiKey)));
	}

	public boolean eventValidate() {
		return ((validate()) && (!isNullOrEmpty(eventId)));
	}

	public boolean viewValidate() {
		return ((validate()) && (!isNullOrEmpty(viewId)));
	}

	public ApiClient apiClient() {
		return RemoteApiClient.newBuilder().setHostname(apiHost).setApiKey(apiKey).setDefaultLogLevel(LogLevel.WARN)
				.setResponseLogLevel(HttpURLConnection.HTTP_CONFLICT, LogLevel.INFO).build();
	}

	// This is used for testing purposes and easier context args building.
	//
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String appHost;
		private String apiHost;
		private String grafanaHost;
		private String serviceId;
		private String libraryId;
		private String functionId;
		private String eventId;
		private String viewId;
		private String apiKey;
		private String resurface;

		Builder() {

		}

		public Builder setAppHost(String appHost) {
			this.appHost = appHost;
			return this;
		}

		public Builder setApiHost(String apiHost) {
			this.apiHost = apiHost;
			return this;
		}

		public Builder setGrafanaHost(String grafanaHost) {
			this.grafanaHost = grafanaHost;
			return this;
		}

		public Builder setServiceId(String serviceId) {
			this.serviceId = serviceId;
			return this;
		}

		public Builder setLibraryId(String libraryId) {
			this.libraryId = libraryId;
			return this;
		}

		public Builder setFunctionId(String functionId) {
			this.functionId = functionId;
			return this;
		}

		public Builder setEventId(String eventId) {
			this.eventId = eventId;
			return this;
		}

		public Builder setViewId(String viewId) {
			this.viewId = viewId;
			return this;
		}

		public Builder setApiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public Builder setResurface(String resurface) {
			this.resurface = resurface;
			return this;
		}

		public ContextArgs build() {
			return new ContextArgs(appHost, apiHost, grafanaHost, serviceId, libraryId, functionId, eventId, viewId,
					apiKey, resurface);
		}
	}
}
