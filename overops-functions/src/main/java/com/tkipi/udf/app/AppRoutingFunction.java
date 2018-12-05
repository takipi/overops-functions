package com.tkipi.udf.app;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.category.Category;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.data.view.ViewFilters;
import com.takipi.api.client.data.view.ViewInfo;
import com.takipi.api.client.util.category.CategoryUtil;
import com.takipi.api.client.util.client.ClientUtil;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.input.Input;

public class AppRoutingFunction {

	private static final boolean SHARED = false;

	public static String validateInput(String rawInput) {
		return parseAppRoutingInput(rawInput).toString();
	}

	static AppRoutingInput parseAppRoutingInput(String rawInput) {
		System.out.println("validateInput rawInput:" + rawInput);

		if (Strings.isNullOrEmpty(rawInput)) {
			throw new IllegalArgumentException("Input is empty");
		}

		AppRoutingInput input;

		try {
			input = AppRoutingInput.of(rawInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		if (input.category == null) {
			throw new IllegalArgumentException("'category' must exist");
		}

		if (input.maxViews <= 0) {
			throw new IllegalArgumentException("'maxViews' must be greater than zero");
		}

		return input;
	}

	public static void execute(String rawContextArgs, String rawInput) {

		System.out.println("execute:" + rawContextArgs);

		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		if (!args.viewValidate()) {
			throw new IllegalArgumentException("Invalid context args - " + rawContextArgs);
		}

		AppRoutingInput input = parseAppRoutingInput(rawInput);

		buildAppRoutingViews(args, input);
	}

	private static void buildAppRoutingViews(ContextArgs args, AppRoutingInput input) {

		String serviceId = args.serviceId;
		ApiClient apiClient = args.apiClient();

		String categoryId = createAppCategory(apiClient, serviceId, input);

		List<String> apps = ClientUtil.getApplications(apiClient, serviceId, true);

		if (apps == null) {
			System.out.println("Could not acquire apps for service " + serviceId);
			return;
		}

		Map<String, SummarizedView> views = ViewUtil.getServiceViewsByName(apiClient, serviceId);

		if (views == null) {
			System.out.println("Could not acquire view for service " + serviceId);
			return;
		}

		int appsWithViewsSize = 0;
		Set<String> appsWithoutViews = Sets.newHashSet();

		for (String app : apps) {
			if (!views.containsKey(app)) {
				appsWithoutViews.add(app);
			} else {
				appsWithViewsSize++;
			}
		}

		if (appsWithViewsSize >= input.maxViews) {
			System.out.println("Found " + appsWithViewsSize + " app views, excedding max for " + serviceId);
			return;
		}

		List<ViewInfo> viewInfos = Lists.newArrayList();

		for (String app : appsWithoutViews) {

			ViewInfo viewInfo = new ViewInfo();

			viewInfo.name = app;
			viewInfo.filters = new ViewFilters();
			viewInfo.filters.apps = Collections.singletonList(app);
			viewInfo.shared = SHARED;

			viewInfos.add(viewInfo);
		}

		ViewUtil.createFilteredViews(apiClient, serviceId, viewInfos, views, categoryId);
	}

	private static String createAppCategory(ApiClient apiClient, String serviceId, AppRoutingInput input) {

		String result;
		Category category = CategoryUtil.getServiceCategoryByName(apiClient, serviceId, input.category);

		if (category == null) {
			result = CategoryUtil.createCategory(input.category, serviceId, apiClient, SHARED);

			System.out.println("Created category " + result + " for " + input.category);
		} else {
			result = category.id;
		}

		return result;
	}

	static class AppRoutingInput extends Input {

		public String category; // The category in which to place views
		public int maxViews; // Max number of views to create in the category

		private AppRoutingInput(String raw) {
			super(raw);
		}

		static AppRoutingInput of(String raw) {
			return new AppRoutingInput(raw);
		}

		@Override
		public String toString() {
			return "AppRouting: " + category + " " + maxViews;
		}
	}

	// A sample program on how to programmatically activate AppRoutingFunction
	public static void main(String[] args) {

		if ((args == null) || (args.length < 3)) {
			throw new IllegalArgumentException("args");
		}

		ContextArgs contextArgs = new ContextArgs();

		contextArgs.apiHost = args[0];
		contextArgs.apiKey = args[1];
		contextArgs.serviceId = args[2];

		SummarizedView view = ViewUtil.getServiceViewByName(contextArgs.apiClient(), contextArgs.serviceId,
				"All Events");
		contextArgs.viewId = view.id;

		// some test values
		String[] sampleValues = new String[] { "category=Apps", "maxViews=50" };

		String rawContextArgs = new Gson().toJson(contextArgs);
		AppRoutingFunction.execute(rawContextArgs, String.join("\n", sampleValues));
	}
}
