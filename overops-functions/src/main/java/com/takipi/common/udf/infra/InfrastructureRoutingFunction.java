package com.takipi.common.udf.infra;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.takipi.common.api.ApiClient;
import com.takipi.common.api.request.event.EventsRequest;
import com.takipi.common.api.request.label.BatchModifyLabelsRequest;
import com.takipi.common.api.result.EmptyResult;
import com.takipi.common.api.result.event.EventResult;
import com.takipi.common.api.result.event.EventsResult;
import com.takipi.common.api.url.UrlClient.Response;
import com.takipi.common.api.util.CollectionUtil;
import com.takipi.common.api.util.Pair;
import com.takipi.common.udf.ContextArgs;
import com.takipi.common.udf.input.Input;

public class InfrastructureRoutingFunction {
	public static void main(String[] args) {

	}

	public static String validateInput(String rawInput) {
		getInfrastructureInput(rawInput);

		return "Infrastructure";
	}

	private static InfrastructureInput getInfrastructureInput(String rawInput) {
		System.out.println("validateInput rawInput:" + rawInput);

		if (Strings.isNullOrEmpty(rawInput)) {
			throw new IllegalArgumentException("Input is empty");
		}

		InfrastructureInput input;

		try {
			input = InfrastructureInput.of(rawInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		if (StringUtils.isEmpty(input.category_name)) {
			throw new IllegalArgumentException("'category_name' can't be empty");
		}

		return input;
	}

	public static void install(String rawContextArgs, String rawInput) {
		InfrastructureInput input = getInfrastructureInput(rawInput);

		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		System.out.println("install context: " + rawContextArgs);

		if (!args.validate()) {
			throw new IllegalArgumentException("Bad context args - " + rawContextArgs);
		}

		if (!args.viewValidate()) {
			return;
		}

		ApiClient apiClient = args.apiClient();

		DateTime to = DateTime.now();
		DateTime from = to.minusDays(30);

		DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZoneUTC();

		EventsRequest eventsRequest = EventsRequest.newBuilder().setServiceId(args.serviceId).setViewId(args.viewId)
				.setFrom(from.toString(fmt)).setTo(to.toString(fmt)).build();

		Response<EventsResult> eventsResponse = apiClient.get(eventsRequest);

		if (eventsResponse.isBadResponse()) {
			throw new IllegalStateException("Failed getting view events.");
		}

		EventsResult eventsResult = eventsResponse.data;

		if (CollectionUtil.safeIsEmpty(eventsResult.events)) {
			return;
		}

		String categoryId = InfraUtil.createCategory(input.category_name, args.serviceId, apiClient);
		Categories categories = input.getCategories();
		Set<String> createdLabels = Sets.newHashSet();

		boolean hasModifications = false;
		BatchModifyLabelsRequest.Builder builder = BatchModifyLabelsRequest.newBuilder().setServiceId(args.serviceId);

		for (EventResult event : eventsResult.events) {
			Pair<Collection<String>, Collection<String>> eventCategories = InfraUtil.categorizeEvent(event,
					args.serviceId, categoryId, categories, createdLabels, apiClient, false);

			Collection<String> labelsToAdd = eventCategories.getFirst();
			Collection<String> labelsToRemove = eventCategories.getSecond();

			if ((!labelsToAdd.isEmpty()) || (!labelsToRemove.isEmpty())) {
				builder.addLabelModifications(event.id, labelsToAdd, labelsToRemove);
				hasModifications = true;
			}
		}

		if (!hasModifications) {
			return;
		}

		Response<EmptyResult> response = apiClient.post(builder.build());

		if (response.isBadResponse()) {
			throw new IllegalStateException("Failed batch apply of labels.");
		}
	}

	public static void execute(String rawContextArgs, String rawInput) {
		InfrastructureInput input = getInfrastructureInput(rawInput);

		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		System.out.println("execute context: " + rawContextArgs);

		if (!args.validate()) {
			throw new IllegalArgumentException("Bad context args - " + rawContextArgs);
		}

		if (!args.eventValidate()) {
			return;
		}

		ApiClient apiClient = args.apiClient();

		String categoryId = InfraUtil.createCategory(input.category_name, args.serviceId, apiClient);

		InfraUtil.categorizeEvent(args.eventId, args.serviceId, categoryId, input.getCategories(), Sets.newHashSet(),
				apiClient, true);
	}

	static class InfrastructureInput extends Input {
		public List<String> namespaces;
		public String template_view;
		public String category_name;

		private final List<Pair<String, String>> namespaceToLabel = Lists.newArrayList();

		InfrastructureInput(String raw) {
			super(raw);

			processNamespaces();
		}

		private void processNamespaces() {
			if (CollectionUtil.safeIsEmpty(namespaces)) {
				return;
			}

			for (String namespace : namespaces) {
				int index = namespace.indexOf('=');

				if ((index <= 0) || (index > namespace.length() - 1)) {
					throw new IllegalArgumentException("Invalid namespaces");
				}

				String key = StringUtils.trim(namespace.substring(0, index));
				String value = StringUtils.trim(namespace.substring(index + 1, namespace.length()));

				if ((key.isEmpty()) || (value.isEmpty())) {
					throw new IllegalArgumentException("Invalid namespaces");
				}

				namespaceToLabel.add(Pair.of(key, value));
			}
		}

		public Categories getCategories() {
			if (CollectionUtil.safeIsEmpty(namespaceToLabel)) {
				return Categories.defaultCategories();
			}

			return Categories.from(namespaceToLabel);
		}

		static InfrastructureInput of(String raw) {
			return new InfrastructureInput(raw);
		}
	}
}
