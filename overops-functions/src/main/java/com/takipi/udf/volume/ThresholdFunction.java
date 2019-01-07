package com.takipi.udf.volume;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.transaction.Transaction;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsVolumeResult;
import com.takipi.api.client.util.transaction.TransactionUtil;
import com.takipi.api.client.util.validation.ValidationUtil.VolumeType;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.CollectionUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.input.Input;
import com.takipi.udf.input.TimeInterval;

public class ThresholdFunction {

	private static final int DEFAULT_TIME_WINDOW = 60;

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

		if (input.min_interval == null) {
			input.min_interval = TimeInterval.of(0);
		} else if (input.min_interval.isNegative()) {
			throw new IllegalArgumentException("'min_interval' can't be negative time");
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

		Map<String, Transaction> transactions = TransactionUtil.getTransactions(apiClient, serviceId, viewId, from, to);

		long result = 0;

		for (Transaction transaction : transactions.values()) {
			if (transaction.stats != null) {
				result += transaction.stats.invocations;
			}
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

		Collection<EventResult> contributors = AnomalyUtil.getContributors(events, apiClient, args.serviceId,
				input.min_interval, input.label);

		if (CollectionUtil.safeIsEmpty(contributors)) {
			return;
		}

		AnomalyUtil.reportAnomaly(apiClient, args.serviceId, args.viewId, contributors, input.label, from, to, input.toString());
	}

	static class ThresholdInput extends Input {

		public Mode relative_to;

		public long threshold;
		public double rate;

		public TimeInterval timespan;

		public String label;
		public TimeInterval min_interval;

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
