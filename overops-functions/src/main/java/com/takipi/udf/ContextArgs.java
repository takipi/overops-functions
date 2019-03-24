package com.takipi.udf;

import com.google.common.base.Strings;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.RemoteApiClient;

public class ContextArgs {
	public String apiHost;
	public String serviceId;
	public String eventId;
	public String viewId;
	public String apiKey;
	public String resurface;

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
		return RemoteApiClient.newBuilder().setHostname(apiHost).setApiKey(apiKey).build();
	}
}
