package com.takipi.udf.severity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.category.Category;
import com.takipi.api.client.data.event.Action;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventActionsRequest;
import com.takipi.api.client.request.label.BatchModifyLabelsRequest;
import com.takipi.api.client.result.EmptyResult;
import com.takipi.api.client.result.event.EventActionsResult;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsVolumeResult;
import com.takipi.api.client.util.category.CategoryUtil;
import com.takipi.api.client.util.label.LabelUtil;
import com.takipi.api.client.util.regression.RateRegression;
import com.takipi.api.client.util.regression.RegressionInput;
import com.takipi.api.client.util.regression.RegressionResult;
import com.takipi.api.client.util.regression.RegressionUtil;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.Pair;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.input.Input;

public class SeverityFunction {
	public static String validateInput(String rawInput) {
		return parseSeverityInput(rawInput).toString();
	}

	static SeverityInput parseSeverityInput(String rawInput) {
		System.out.println("validateInput rawInput:" + rawInput);

		if (Strings.isNullOrEmpty(rawInput)) {
			throw new IllegalArgumentException("Input is empty");
		}

		SeverityInput input;

		try {
			input = SeverityInput.of(rawInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		if (input.activeTimespan <= 0) {
			throw new IllegalArgumentException("'activeTimespan' must be positive'");
		}

		if (input.baseTimespan <= 0) {
			throw new IllegalArgumentException("'timespan' must be positive");
		}

		if (input.regressionDelta <= 0) {
			throw new IllegalArgumentException("'regressionDelta' must be positive");
		}

		if (input.criticalRegressionDelta < 0) {
			throw new IllegalArgumentException("'criticalRegressionDelta' can't be negative");
		}

		if (input.newEventslabel == null) {
			throw new IllegalArgumentException("'newEventslabel' must exist");
		}

		if (input.regressedEventsLabel == null) {
			throw new IllegalArgumentException("'regressedEventsLabel' must exist");
		}

		return input;
	}

	public static void execute(String rawContextArgs, String rawInput) {
		System.out.println("execute:" + rawContextArgs);

		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		if (!args.viewValidate()) {
			throw new IllegalArgumentException("Invalid context args - " + rawContextArgs);
		}

		SeverityInput input = parseSeverityInput(rawInput);

		setupSeverityViews(args, input);

		System.out.println("Calculating regressions\n");

		RegressionInput regressionInput = new RegressionInput();

		regressionInput.serviceId = args.serviceId;
		regressionInput.viewId = args.viewId;
		regressionInput.activeTimespan = input.activeTimespan;
		regressionInput.baselineTimespan = input.baseTimespan;
		regressionInput.minVolumeThreshold = input.minVolumeThreshold;
		regressionInput.minErrorRateThreshold = input.minErrorRateThreshold;
		regressionInput.regressionDelta = input.regressionDelta;
		regressionInput.criticalRegressionDelta = input.criticalRegressionDelta;
		regressionInput.applySeasonality = input.applySeasonality;
		regressionInput.criticalExceptionTypes = input.criticalExceptionTypes;

		RateRegression rateRegression = RegressionUtil.calculateRateRegressions(args.apiClient(), regressionInput,
				System.out, false);

		Map<String, EventResult> allNewAndCritical = Maps.newHashMap();

		if (input.newEventsView != null) {
			
			allNewAndCritical.putAll(rateRegression.getExceededNewEvents());
			allNewAndCritical.putAll(rateRegression.getCriticalNewEvents());
	
			applySeverityLabels(args, input.newEventslabel, input.newEventsView, input.labelRetention,
					Lists.newArrayList(allNewAndCritical.values()));
		}

		if (input.regressedEventsView != null) {
			
			Collection<RegressionResult> activeRegressions = rateRegression.getAllRegressions().values();
			Collection<EventResult> activeRegressionEvents = Lists.newArrayListWithCapacity(activeRegressions.size());
	
			for (RegressionResult activeRegression : activeRegressions) {
				activeRegressionEvents.add(activeRegression.getEvent());
			}
			
			applySeverityLabels(args, input.regressedEventsLabel, input.regressedEventsView, input.labelRetention,
					activeRegressionEvents);
		}
	}

	private static void setupSeverityViews(ContextArgs args, SeverityInput input) {
		String categoryId = createSeverityCategory(args, input);

		LabelUtil.createLabelsIfNotExists(args.apiClient(), args.serviceId,
				new String[] { input.newEventslabel, input.regressedEventsLabel });

		Collection<Pair<String, String>> views = Lists.newArrayList();

		if (input.newEventsView != null) {
			views.add(Pair.of(input.newEventsView, input.newEventslabel));
		}
		
		if (input.regressedEventsView != null) {
			views.add(Pair.of(input.regressedEventsView, input.regressedEventsLabel));
		}

		if (views.size() > 0) {
			ViewUtil.createLabelViewsIfNotExists(args.apiClient(), args.serviceId, 
				views, categoryId);
		}
	}

	private static String createSeverityCategory(ContextArgs args, SeverityInput input) {
		String result;
		Category category = CategoryUtil.getServiceCategoryByName(args.apiClient(), args.serviceId, input.category);

		if (category == null) {
			result = ViewUtil.createCategory(input.category, args.serviceId, args.apiClient());
			System.out.println("Created category " + result + " for " + input.category);
		} else {
			result = category.id;
		}

		return result;
	}

	private static void applySeverityLabels(ContextArgs args, String label, String viewName, int labelRetention,
			Collection<EventResult> targetEvents) {
		ApiClient apiClient = args.apiClient();

		Map<String, EventResult> newlyLabeledEvents = Maps.newHashMap();

		boolean modified = false;
		BatchModifyLabelsRequest.Builder builder = BatchModifyLabelsRequest.newBuilder().setServiceId(args.serviceId)
				.setHandleSimilarEvents(false);

		for (EventResult event : targetEvents) {
			boolean hasLabel = (event.labels != null) && (event.labels.contains(label));

			if (!hasLabel) {
				modified = true;
				newlyLabeledEvents.put(event.id, event);

				builder.addLabelModifications(event.id, Collections.singleton(label), Collections.emptyList());
				System.out.println("Applying label " + label + " to " + event.id);
			} else {
				System.out.println("Event " + event.id + " already has label " + label);
			}
		}

		DateTime now = DateTime.now();
		DateTime retentionTime = now.minusMinutes(labelRetention * 2);

		SummarizedView view = ViewUtil.getServiceViewByName(apiClient, args.serviceId, viewName);

		if (view == null) {
			System.out.println("Could not get view " + viewName);
			return;
		}

		EventsVolumeResult previousEvents = ViewUtil.getEventsVolume(apiClient, args.serviceId, view.id, retentionTime,
				now);

		DateTime retentionWindow = DateTime.now().minusMinutes(labelRetention);

		if ((previousEvents != null) && (previousEvents.events != null)) {
			for (int i = 0; i < previousEvents.events.size(); i++) {
				EventResult event = previousEvents.events.get(i);

				if (targetEvents.contains(event)) {
					continue;
				}

				// if this is a new severe issue, no need to cleanup its label
				if (newlyLabeledEvents.containsKey(event.id)) {
					continue;
				}

				// if this event wasn't prev marked as severe - skip
				if ((event.labels == null) || (!event.labels.contains(label))) {
					continue;
				}

				// get the actions for this event. Let's see when was that event marked severe
				EventActionsRequest eventActionsRequest = EventActionsRequest.newBuilder().setServiceId(args.serviceId)
						.setEventId(event.id).build();

				Response<EventActionsResult> eventsActionsResponse = apiClient.get(eventActionsRequest);

				if (eventsActionsResponse.isBadResponse()) {
					System.err.println("Can't create events actions for event " + event.id);
				}

				if (eventsActionsResponse.data.event_actions == null) {
					continue;
				}

				boolean keepLabel = false;

				for (Action action : eventsActionsResponse.data.event_actions) {
					if (!label.equals(action.data)) {
						continue;
					}

					// we should add a constant for this in the Java API wrapper
					if (!"ADD_LABEL".equals(action.action.toUpperCase())) {
						continue;
					}

					DateTime labelAddTime = ISODateTimeFormat.dateTimeParser().parseDateTime(action.timestamp);

					// lets see if the label was added after the retention window, is so - keep
					if (labelAddTime.isAfter(retentionWindow)) {
						keepLabel = true;
						System.out.println("Keeping label " + label + " on " + event.id);
						break;
					}
				}

				if (!keepLabel) {
					modified = true;
					System.out.println("Removing label " + label + " from " + event.id);
					builder.addLabelModifications(event.id, Collections.emptyList(), Collections.singleton(label));
				}
			}
		}

		if (!modified) {
			return;
		}

		Response<EmptyResult> response = apiClient.post(builder.build());

		if (!response.isOK()) {
			System.out.println("Error adding /  removing labels " + response.responseCode);
		}

	}

	static class SeverityInput extends Input {
		public int activeTimespan; // the time window (min) that we compare the baseline to
		public int baseTimespan; // the time window (min) to compare the last <activeTimespan> against
		public double regressionDelta; // a change in % that would be considered a regression
		public double criticalRegressionDelta; // a change in % that would be considered a critical regression
		public boolean applySeasonality; // whether or not to apply a seasonality algorithm.
		public List<String> criticalExceptionTypes; // comma delimited list of exception types that are severe by def
		public double minErrorRateThreshold; // min ER that a regression, new + non-critical event must exceed
		public int minVolumeThreshold; // min volume that a regression, new + non-critical event must exceed
		public String category; // The category in which to place views
		public String newEventslabel; // how to label new issues
		public String regressedEventsLabel; // how to label regressions
		public String newEventsView; // view containing new issues
		public String regressedEventsView; // view containing regressions
		public int labelRetention; // how long (min) should thse labels "stick" to an event

		private SeverityInput(String raw) {
			super(raw);
		}

		static SeverityInput of(String raw) {
			return new SeverityInput(raw);
		}

		@Override
		public String toString() {
			return String.format("Severe(Window = %d, Baseline = %d, Thresh = %d, Rate = %.2f)", activeTimespan,
					baseTimespan, minVolumeThreshold, minErrorRateThreshold);
		}
	}
}
