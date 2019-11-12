package com.takipi.udf.util;

import com.google.gson.Gson;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.udf.ContextArgs;

public class TestUtil {
	public static String getViewContextArgs(String[] args, String viewName) {
		if ((args == null) || (args.length < 3)) {
			throw new IllegalArgumentException("args");
		}

		ContextArgs contextArgs = ContextArgs.newBuilder().setApiHost(args[0]).setApiKey(args[1]).setServiceId(args[2])
				.build();

		SummarizedView view = ViewUtil.getServiceViewByName(contextArgs.apiClient(), contextArgs.serviceId, viewName);

		contextArgs.viewId = view.id;

		return new Gson().toJson(contextArgs);
	}

	public static String getEventContextArgs(String[] args) {
		if ((args == null) || (args.length < 4)) {
			throw new IllegalArgumentException("args");
		}

		ContextArgs contextArgs = ContextArgs.newBuilder().setApiHost(args[0]).setApiKey(args[1]).setServiceId(args[2])
				.setEventId(args[3]).build();

		return new Gson().toJson(contextArgs);
	}

	public static ContextArgs.Builder getDefaultContextArgsBuilder() {
		return ContextArgs.newBuilder().setAppHost("https://app.overops.com").setApiHost("https://api.overops.com")
				.setGrafanaHost("https://app.overops.com/grafana");
	}
}
