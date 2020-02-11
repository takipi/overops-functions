package com.takipi.udf;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import com.takipi.api.client.ApiClient;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.core.url.UrlClient.LogLevel;
import com.takipi.common.util.CollectionUtil;
import com.takipi.common.util.StringUtil;

public class ContextArgs {
	public static class Contributor {
		public String id;
		public int lastSnapshotId;
		public long hits;
	}

	public static enum ExecutionContext {
		NEW_EVENT, RESURFACED_EVENT, THRESHOLD, ANOMALY, CUSTOM_ALERT, PERIODIC
	}

	public String appHost;
	public String apiHost;
	public String grafanaHost;
	public String serviceId;
	public String serviceName;
	public String libraryId;
	public String functionId;
	public String eventId;
	public String viewId;
	public String apiKey;
	public String resurface;
	public String callerLibraryId;
	public String callerFunctionId;
	public List<Contributor> contributors;
	public ExecutionContext executionContext;
	public Map<String, String> contextData;

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

	public boolean validate() {
		return ((!StringUtil.isNullOrEmpty(serviceId)) && (!StringUtil.isNullOrEmpty(apiKey)));
	}

	public boolean eventValidate() {
		return ((validate()) && (!StringUtil.isNullOrEmpty(eventId)));
	}

	public boolean viewValidate() {
		return ((validate()) && (!StringUtil.isNullOrEmpty(viewId)));
	}

	public ApiClient apiClient() {
		return RemoteApiClient.newBuilder().setHostname(apiHost).setApiKey(apiKey).setDefaultLogLevel(LogLevel.WARN)
				.setResponseLogLevel(HttpURLConnection.HTTP_CONFLICT, LogLevel.INFO).build();
	}

	public long longData(String name) {
		return longData(name, -1);
	}

	public long longData(String name, long defaultValue) {
		if (CollectionUtil.safeIsEmpty(contextData)) {
			return defaultValue;
		}

		String value = contextData.get(name);

		if (StringUtil.isNullOrEmpty(value)) {
			return defaultValue;
		}

		try {
			return Long.valueOf(value);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public int intData(String name) {
		return intData(name, -1);
	}

	public int intData(String name, int defaultValue) {
		if (CollectionUtil.safeIsEmpty(contextData)) {
			return defaultValue;
		}

		String value = contextData.get(name);

		if (StringUtil.isNullOrEmpty(value)) {
			return defaultValue;
		}

		try {
			return Integer.valueOf(value);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public String data(String name) {
		return data(name, "");
	}

	public String data(String name, String defaultValue) {
		if (CollectionUtil.safeIsEmpty(contextData)) {
			return defaultValue;
		}

		String value = contextData.get(name);

		if (StringUtil.isNullOrEmpty(value)) {
			return defaultValue;
		}

		return value;
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
