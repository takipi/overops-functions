package com.takipi.udf.app;

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
import com.takipi.udf.util.TestUtil;

public class AppRoutingFunction {
	private static final boolean SHARED = true;
	private static final boolean IMMUTABLE_VIEWS = true;

	public static String validateInput(String rawInput) {
		return parseAppRoutingInput(rawInput).toString();
	}

	static AppRoutingInput parseAppRoutingInput(String rawInput) {
		System.out.println("validateInput rawInput: " + rawInput);

		if (Strings.isNullOrEmpty(rawInput)) {
			throw new IllegalArgumentException("Input is empty");
		}

		AppRoutingInput input;

		try {
			input = AppRoutingInput.of(rawInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		if (input.category_name == null) {
			throw new IllegalArgumentException("'category_name' must exist");
		}

		if (input.max_views <= 0) {
			throw new IllegalArgumentException("'max_views' must be greater than zero");
		}

		return input;
	}

	public static void execute(String rawContextArgs, String rawInput) {
		System.out.println("execute: " + rawContextArgs);

		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		if (!args.viewValidate()) {
			throw new IllegalArgumentException("Invalid context args - " + rawContextArgs);
		}

		AppRoutingInput input = parseAppRoutingInput(rawInput);

		buildAppRoutingViews(args, input);
	}

	private static void buildAppRoutingViews(ContextArgs args, AppRoutingInput input) {
		ApiClient apiClient = args.apiClient();

		String serviceId = args.serviceId;

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

		if (appsWithViewsSize >= input.max_views) {
			System.out.println("Found " + appsWithViewsSize + " app views, exceeding max for " + serviceId);
			return;
		}

		List<ViewInfo> viewInfos = Lists.newArrayList();

		for (String app : appsWithoutViews) {
			ViewInfo viewInfo = new ViewInfo();

			viewInfo.name = app;
			viewInfo.filters = new ViewFilters();
			viewInfo.filters.apps = Collections.singletonList(app);
			viewInfo.shared = SHARED;
			viewInfo.immutable = IMMUTABLE_VIEWS;

			viewInfos.add(viewInfo);
		}

		ViewUtil.createFilteredViews(apiClient, serviceId, viewInfos, views, categoryId);
	}

	private static String createAppCategory(ApiClient apiClient, String serviceId, AppRoutingInput input) {
		Category category = CategoryUtil.getServiceCategoryByName(apiClient, serviceId, input.category_name);

		String result;

		if (category == null) {
			result = CategoryUtil.createCategory(input.category_name, serviceId, apiClient, SHARED);

			System.out.println("Created category " + result + " for " + input.category_name);
		} else {
			result = category.id;
		}

		return result;
	}

	static class AppRoutingInput extends Input {
		public String category_name; // The category in which to place views
		public int max_views; // Max number of views to create in the category

		private AppRoutingInput(String raw) {
			super(raw);
		}

		static AppRoutingInput of(String raw) {
			return new AppRoutingInput(raw);
		}

		@Override
		public String toString() {
			return "AppRouting: " + category_name + " " + max_views;
		}
	}

	// A sample program on how to programmatically activate AppRoutingFunction
	public static void main(String[] args) {
		String rawContextArgs = TestUtil.getViewContextArgs(args, "All Events");

		// some test values
		String[] sampleValues = new String[] { "category_name=Apps", "max_views=50" };

		AppRoutingFunction.execute(rawContextArgs, String.join("\n", sampleValues));
	}
}
