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

		ContextArgs contextArgs = new ContextArgs();

		contextArgs.apiHost = args[0];
		contextArgs.apiKey = args[1];
		contextArgs.serviceId = args[2];

		SummarizedView view = ViewUtil.getServiceViewByName(contextArgs.apiClient(), contextArgs.serviceId, viewName);

		contextArgs.viewId = view.id;

		return new Gson().toJson(contextArgs);
	}

	public static String getEventContextArgs(String[] args) {
		if ((args == null) || (args.length < 4)) {
			throw new IllegalArgumentException("args");
		}

		ContextArgs contextArgs = new ContextArgs();

		contextArgs.apiHost = args[0];
		contextArgs.apiKey = args[1];
		contextArgs.serviceId = args[2];
		contextArgs.eventId = args[3];

		return new Gson().toJson(contextArgs);
	}
}
