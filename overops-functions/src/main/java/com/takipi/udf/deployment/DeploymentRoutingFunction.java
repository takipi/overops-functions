package com.takipi.udf.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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

public class DeploymentRoutingFunction {
	private static final boolean SHARED = true;

	public static String validateInput(String rawInput) {
		return parseDeploymentRoutingInput(rawInput).toString();
	}

	static DeploymentRoutingInput parseDeploymentRoutingInput(String rawInput) {
		System.out.println("validateInput rawInput: " + rawInput);

		if (Strings.isNullOrEmpty(rawInput)) {
			throw new IllegalArgumentException("Input is empty");
		}

		DeploymentRoutingInput input;

		try {
			input = DeploymentRoutingInput.of(rawInput);
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

		DeploymentRoutingInput input = parseDeploymentRoutingInput(rawInput);

		buildDeploymentRoutingViews(args, input);
	}

	private static String cleanPrefix(DeploymentRoutingInput input) {
		return input.prefix.replace("'", "");
	}

	private static void buildDeploymentRoutingViews(ContextArgs args, DeploymentRoutingInput input) {
		ApiClient apiClient = args.apiClient();

		String serviceId = args.serviceId;

		String categoryId = createDepCategory(apiClient, serviceId, input);

		List<String> activeDeps = ClientUtil.getDeployments(apiClient, serviceId, true);
		List<String> allDeps = ClientUtil.getDeployments(apiClient, serviceId, false);

		if (activeDeps == null) {
			System.err.println("Could not acquire active deps for service " + serviceId);
			return;
		}

		if (allDeps == null) {
			System.err.println("Could not acquire all deps for service " + serviceId);
			return;
		}

		Map<String, SummarizedView> views = ViewUtil.getServiceViewsByName(apiClient, serviceId);

		if (views == null) {
			System.err.println("Could not acquire view for service " + serviceId);
			return;
		}

		List<SummarizedView> activeDepViews = Lists.newArrayList();
		List<SummarizedView> allDepViews = Lists.newArrayList();

		for (SummarizedView view : views.values()) {
			String viewDepName;

			if (input.prefix != null) {
				viewDepName = view.name.replace(cleanPrefix(input), "");
			} else {
				viewDepName = view.name;
			}

			if (activeDeps.contains(viewDepName)) {
				activeDepViews.add(view);
			} else if (allDeps.contains(viewDepName)) {
				allDepViews.add(view);
			}
		}

		if (activeDepViews.size() >= input.max_views) {
			System.out.println("Found " + activeDepViews.size() + " active dep views, exceeding max for " + serviceId);
			return;
		}

		int itemsToDeleteSize = 1;// activeDepViews.size() + allDepViews.size() - input.max_views;

		if (itemsToDeleteSize > 0) {
			for (int i = 0; i < Math.min(itemsToDeleteSize, allDepViews.size()); i++) {
				SummarizedView view = allDepViews.get(i);

				System.out.println("Deleting view " + view.name + " Id: " + view.id);

				ViewUtil.removeView(apiClient, serviceId, view.id);
			}
		}

		List<String> activeViewsToCreate = Lists.newArrayList();

		for (String activeDep : activeDeps) {
			String depViewName = toViewName(input, activeDep);

			if (!views.containsKey(depViewName)) {
				activeViewsToCreate.add(activeDep);
			}
		}

		int activeDepViewsToCreateSize = Math.min(activeViewsToCreate.size() + activeDepViews.size(), input.max_views);

		List<ViewInfo> viewInfos = Lists.newArrayList();

		for (int i = 0; i < activeDepViewsToCreateSize; i++) {
			String dep = activeViewsToCreate.get(i);
			ViewInfo viewInfo = new ViewInfo();

			viewInfo.name = toViewName(input, dep);
			viewInfo.filters = new ViewFilters();
			viewInfo.filters.introduced_by = Collections.singletonList(dep);
			viewInfo.shared = SHARED;

			viewInfos.add(viewInfo);
		}

		ViewUtil.createFilteredViews(apiClient, serviceId, viewInfos, views, categoryId);
	}

	private static String toViewName(DeploymentRoutingInput input, String dep) {
		String result;

		if (input.prefix != null) {
			result = cleanPrefix(input) + dep;
		} else {
			result = dep;
		}

		return result;
	}

	private static String createDepCategory(ApiClient apiClient, String serviceId, DeploymentRoutingInput input) {
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

	static class DeploymentRoutingInput extends Input {
		public String category_name; // The category in which to place views
		public int max_views; // Max number of views to create in the category
		public String prefix; // An optional prefix to add to the view name (e.g. 'New in')

		private DeploymentRoutingInput(String raw) {
			super(raw);
		}

		static DeploymentRoutingInput of(String raw) {
			return new DeploymentRoutingInput(raw);
		}

		@Override
		public String toString() {
			return "DeploymentRouting: " + category_name + " " + max_views;
		}
	}

	// A sample program on how to programmatically activate
	// DeploymentRoutingFunction
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
		String[] sampleValues = new String[] { "category_name=CI / CD", "prefix='New in '", "max_views=5" };

		String rawContextArgs = new Gson().toJson(contextArgs);
		DeploymentRoutingFunction.execute(rawContextArgs, String.join("\n", sampleValues));
	}
}
