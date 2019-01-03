package com.takipi.udf.volume;

import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.event.Action;
import com.takipi.api.client.data.transaction.Transaction;
import com.takipi.api.client.request.event.BatchForceSnapshotsRequest;
import com.takipi.api.client.request.event.EventActionsRequest;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.request.label.BatchModifyLabelsRequest;
import com.takipi.api.client.request.label.CreateLabelRequest;
import com.takipi.api.client.request.transaction.TransactionsVolumeRequest;
import com.takipi.api.client.result.EmptyResult;
import com.takipi.api.client.result.event.EventActionsResult;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsVolumeResult;
import com.takipi.api.client.result.transaction.TransactionsVolumeResult;
import com.takipi.api.client.util.validation.ValidationUtil.VolumeType;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.CollectionUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.input.Input;
import com.takipi.udf.input.TimeInterval;

public class ThresholdFunction {

	private static final int DEFAULT_TIME_WINDOW = 60;

	private static final String LABEL_ADD = "ADD_LABEL";
	private static final String LABEL_TYPE = "LABEL";

	private static final int MAX_CONTRIBUTORS = 10;

	private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZoneUTC();

	static ThresholdInput getThresholdInput(String rawInput) {
		System.out.println("validateInput rawInput:" + rawInput);

		if (Strings.isNullOrEmpty(rawInput)) {
			throw new IllegalArgumentException("Input is empty");
		}

		ThresholdInput input;

		try {
			input = ThresholdInput.of(rawInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		if ((input.timespan != null) && (!input.timespan.isPositive())) {
			throw new IllegalArgumentException("'timespan' must be positive");
		}

		if ((input.threshold <= 0l) && (input.relative_to == Mode.Absolute)) {
			throw new IllegalArgumentException("'threshold' must be positive");
		}

		if (input.threshold < 0l) {
			throw new IllegalArgumentException("'threshold' must be positive");
		}

		if ((input.relative_to == Mode.Method_Calls) && (input.rate <= 0.0)) {
			throw new IllegalArgumentException("'rate' must be positive");
		}

		if ((input.relative_to == Mode.Thread_Calls) && (input.rate <= 0.0)) {
			throw new IllegalArgumentException("'rate' must be positive");
		}

		if (input.minInterval == null) {
			input.minInterval = TimeInterval.of(0);
		} else if (input.minInterval.isNegative()) {
			throw new IllegalArgumentException("'minInterval' can't be negative time");
		}

		return input;
	}

	private static List<EventResult> getEventVolume(ApiClient apiClient, String serviceId, String viewId, DateTime from,
			DateTime to, VolumeType volumeType) {

		EventsVolumeRequest eventsVolumeRequest = EventsVolumeRequest.newBuilder().setServiceId(serviceId)
				.setViewId(viewId).setFrom(from.toString(fmt)).setTo(to.toString(fmt)).setVolumeType(volumeType)
				.build();

		Response<EventsVolumeResult> eventsVolumeResponse = apiClient.get(eventsVolumeRequest);

		if (eventsVolumeResponse.isBadResponse()) {
			throw new IllegalStateException("Can't create events volume.");
		}

		EventsVolumeResult eventsVolumeResult = eventsVolumeResponse.data;

		if (eventsVolumeResult == null) {
			throw new IllegalStateException("Missing events volume result.");
		}

		return eventsVolumeResult.events;
	}

	private static long getTransactionVolume(ApiClient apiClient, String serviceId, String viewId, DateTime from,
			DateTime to) {

		TransactionsVolumeRequest transactionsVolumeRequest = TransactionsVolumeRequest.newBuilder()
				.setServiceId(serviceId).setViewId(viewId).setFrom(from.toString(fmt)).setTo(to.toString(fmt)).build();

		Response<TransactionsVolumeResult> transactionsVolumeResponse = apiClient.get(transactionsVolumeRequest);

		if (transactionsVolumeResponse.isBadResponse()) {
			throw new IllegalStateException("Can't create transactions volume.");
		}

		TransactionsVolumeResult transactionsVolumeResult = transactionsVolumeResponse.data;

		if (transactionsVolumeResult == null) {
			throw new IllegalStateException("Missing events volume result.");
		}

		if (transactionsVolumeResult.transactions == null) {
			return 0;
		}

		long result = 0;

		for (Transaction transaction : transactionsVolumeResult.transactions) {
			if (transaction.stats != null) {
				result += transaction.stats.invocations;
			}
		}

		return result;
	}

	public static void applyAnomalyLabels(ApiClient apiClient, String serviceId, String label,
			Collection<EventResult> contributors) {

		if (StringUtils.isEmpty(label)) {
			return;
		}

		CreateLabelRequest createLabel = CreateLabelRequest.newBuilder().setServiceId(serviceId).setName(label).build();

		Response<EmptyResult> createResult = apiClient.post(createLabel);

		if ((createResult.isBadResponse()) && (createResult.responseCode != HttpURLConnection.HTTP_CONFLICT)) {
			throw new IllegalStateException("Cannot create label " + label);
		}

		BatchModifyLabelsRequest.Builder builder = BatchModifyLabelsRequest.newBuilder().setServiceId(serviceId)
				.setForceHistory(true);

		for (EventResult contributor : contributors) {

			builder.addLabelModifications(contributor.id, Collections.singleton(label), Collections.emptyList());
		}

		Response<EmptyResult> addResult = apiClient.post(builder.build());

		if (addResult.isBadResponse()) {
			throw new IllegalStateException("Can't apply label " + label + " to contribs");
		}
	}

	private static boolean isAlertAllowed(ApiClient apiClient, String serviceId, EventResult event, String label,
			TimeInterval interval) {

		EventActionsRequest request = EventActionsRequest.newBuilder().setServiceId(serviceId).setEventId(event.id)
				.build();

		Response<EventActionsResult> response = apiClient.get(request);

		if (response.isBadResponse()) {
			// in case of API failure we should not prevent an alert
			System.err.println("Could not get event actions for " + event + " code: " + response.responseCode);
			return true;
		}

		if (response.data == null) {
			return true;
		}

		if (response.data.event_actions == null) {
			return true;
		}

		for (Action action : response.data.event_actions) {

			if (!(LABEL_ADD.equals(action.action)) || (!LABEL_TYPE.equals(action.type))
					|| (!label.equals(action.data))) {
				continue;
			}

			DateTime actionTime = fmt.parseDateTime(action.timestamp);

			long delta = DateTime.now().minus(actionTime.getMillis()).getMillis();
			long actionMinutesInterval = TimeUnit.MILLISECONDS.toMinutes(delta);

			if (actionMinutesInterval < interval.asMinutes()) {
				return false;
			}
		}

		return true;
	}

	public static Collection<EventResult> getContributors(Collection<EventResult> events, ApiClient apiClient,
			String serviceId, TimeInterval interval, String label) {

		List<EventResult> result = Lists.newArrayList();
		boolean checkAlertAllowed = (interval.isPositive()) && (!Strings.isNullOrEmpty(label));

		int index = 0;

		for (EventResult event : events) {

			if (index >= MAX_CONTRIBUTORS) {
				break;
			}

			index++;

			if (ThresholdUtil.getEventHits(event) == 0) {
				continue;
			}

			if ((checkAlertAllowed) && (!isAlertAllowed(apiClient, serviceId, event, label, interval))) {
				continue;
			}

			result.add(event);
		}

		return result;
	}

	static void execute(String rawContextArgs, ThresholdInput input) {

		System.out.println("execute:" + rawContextArgs);

		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		if (!args.viewValidate()) {
			throw new IllegalArgumentException("Invalid context args - " + rawContextArgs);
		}

		ApiClient apiClient = args.apiClient();

		VolumeType volumeType = ((input.relative_to == null) || (input.relative_to == Mode.Method_Calls)
				? VolumeType.all
				: VolumeType.hits);

		int timespan = (input.timespan != null) ? input.timespan.asMinutes() : DEFAULT_TIME_WINDOW;

		DateTime to = DateTime.now();
		DateTime from = to.minusMinutes(timespan);

		List<EventResult> events = getEventVolume(apiClient, args.serviceId, args.viewId, from, to, volumeType);

		if (events == null) {
			return;
		}

		long hitCount = ThresholdUtil.getEventsHits(events);

		if ((input.threshold > 0) && (hitCount <= input.threshold)) {
			return;
		}

		boolean thresholdExceeded = false;

		Mode mode = (input.relative_to != null) ? input.relative_to : Mode.Method_Calls;

		ThresholdUtil.sortEventsByHitsDesc(events);

		switch (mode) {

		case Absolute: {
			thresholdExceeded = true;
		}
			break;

		case Method_Calls: {

			long invocationsCount = ThresholdUtil.getEventsInvocations(events, hitCount);
			double failRate = (hitCount / (double) invocationsCount) * 100.0;

			thresholdExceeded = (failRate >= input.rate);
		}
			break;

		case Thread_Calls: {

			long transactionInvocationsCount = getTransactionVolume(apiClient, args.serviceId, args.viewId, from, to);

			if (transactionInvocationsCount > 0l) {
				double failRate = (hitCount / (double) transactionInvocationsCount) * 100.0;
				thresholdExceeded = (failRate >= input.rate);
			}

			break;
		}
		}

		System.out.println("Threshold response: " + thresholdExceeded);

		if (!thresholdExceeded) {
			return;
		}

		Collection<EventResult> contributors = getContributors(events, apiClient, args.serviceId, input.minInterval,
				input.label);

		if (CollectionUtil.safeIsEmpty(contributors)) {
			return;
		}

		System.out.println(
				"Alerting on " + contributors.size() + " anomalies: " + StringUtils.join(contributors.toArray(), ','));

		applyAnomalyLabels(apiClient, args.serviceId, input.label, contributors);

		resetEventsSnapshots(apiClient, args.serviceId, contributors);

		AnomalyUtil.send(apiClient, args.serviceId, args.viewId, contributors, from, to, input.toString());
	}

	public static void resetEventsSnapshots(ApiClient apiClient, String serviceId, Collection<EventResult> events) {

		BatchForceSnapshotsRequest.Builder builder = BatchForceSnapshotsRequest.newBuilder().setServiceId(serviceId);

		for (EventResult event : events) {
			builder.addEventId(event.id);
		}

		Response<EmptyResult> reponse = apiClient.post(builder.build());

		if (reponse.isBadResponse()) {
			System.err.println("Cannot reset snapshots, code: " + reponse.responseCode);
		}
	}

	static class ThresholdInput extends Input {

		public Mode relative_to;

		public long threshold;
		public double rate;

		public TimeInterval timespan;

		public String label;
		public TimeInterval minInterval;

		private ThresholdInput(String raw) {
			super(raw);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();

			builder.append("Threshold(");

			Mode mode;

			if (relative_to != null) {
				mode = relative_to;
			} else {
				mode = Mode.Method_Calls;
			}

			switch (mode) {
			case Absolute:
				builder.append(threshold);
				break;

			case Method_Calls:
			case Thread_Calls: {
				builder.append(String.format("%.2f", rate));
				builder.append('%');
				builder.append(", ");
				builder.append(threshold);
				if (relative_to == Mode.Thread_Calls) {
					builder.append(" of ");
					builder.append(relative_to);
				}
			}
				break;
			}

			builder.append(")");

			return builder.toString();
		}

		static ThresholdInput of(String raw) {
			return new ThresholdInput(raw);
		}
	}

	public enum Mode {
		Absolute, Method_Calls, Thread_Calls
	}
}
