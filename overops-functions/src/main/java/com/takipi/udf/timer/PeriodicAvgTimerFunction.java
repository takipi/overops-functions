package com.takipi.udf.timer;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.timer.Timer;
import com.takipi.api.client.data.transaction.Transaction;
import com.takipi.api.client.request.event.EventsRequest;
import com.takipi.api.client.request.redaction.CodeRedactionExcludeRequest;
import com.takipi.api.client.request.transaction.TransactionsVolumeRequest;
import com.takipi.api.client.request.transactiontimer.CreateTransactionTimerRequest;
import com.takipi.api.client.request.transactiontimer.EditTransactionTimerRequest;
import com.takipi.api.client.request.transactiontimer.TransactionTimersRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.result.redaction.CodeRedactionElements;
import com.takipi.api.client.result.transaction.TransactionsVolumeResult;
import com.takipi.api.client.result.transactiontimer.TransactionTimersResult;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.CollectionUtil;
import com.takipi.common.util.Pair;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.input.Input;
import com.takipi.udf.util.JavaUtil;

public class PeriodicAvgTimerFunction {
	public static String validateInput(String rawInput) {
		return getPeriodicAvgTimerInput(rawInput).toString();
	}

	static PeriodicAvgTimerInput getPeriodicAvgTimerInput(String rawInput) {
		System.out.println("validateInput rawInput:" + rawInput);

		if (Strings.isNullOrEmpty(rawInput)) {
			throw new IllegalArgumentException("Input is empty");
		}

		PeriodicAvgTimerInput input;

		try {
			input = PeriodicAvgTimerInput.of(rawInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		if (input.timespan <= 0) {
			throw new IllegalArgumentException("'timespan' must be positive");
		}

		if (input.std_dev <= 0) {
			throw new IllegalArgumentException("'std_dev' must be positive");
		}

		if (input.minimum_absolute_threshold <= 0) {
			throw new IllegalArgumentException("'minimum_absolute_threshold' must be positive");
		}

		if (input.minimum_threshold_delta <= 0) {
			throw new IllegalArgumentException("'minimum_threshold_delta' must be positive");
		}

		return input;
	}

	public static void execute(String rawContextArgs, String rawInput) {
		PeriodicAvgTimerInput input = getPeriodicAvgTimerInput(rawInput);

		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		System.out.println("execute context: " + rawContextArgs);

		if (!args.validate()) {
			throw new IllegalArgumentException("Bad context args - " + rawContextArgs);
		}

		if (!args.viewValidate()) {
			return;
		}

		ApiClient apiClient = args.apiClient();

		DateTime to = DateTime.now();
		DateTime from = to.minusHours(input.timespan);

		DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZoneUTC();

		TransactionsVolumeRequest transactionsRequest = TransactionsVolumeRequest.newBuilder()
				.setServiceId(args.serviceId).setViewId(args.viewId).setFrom(from.toString(fmt)).setTo(to.toString(fmt))
				.build();

		Response<TransactionsVolumeResult> transactionsResponse = apiClient.get(transactionsRequest);

		if (transactionsResponse.isBadResponse()) {
			throw new IllegalStateException("Failed getting view transactions.");
		}

		TransactionsVolumeResult transactionsResult = transactionsResponse.data;

		if (CollectionUtil.safeIsEmpty(transactionsResult.transactions)) {
			return;
		}

		EventsRequest eventsRequest = EventsRequest.newBuilder().setServiceId(args.serviceId).setViewId(args.viewId)
				.setFrom(from.toString(fmt)).setTo(to.toString(fmt)).build();

		Response<EventsResult> eventsResponse = apiClient.get(eventsRequest);

		if (eventsResponse.isBadResponse()) {
			throw new IllegalStateException("Failed getting view events.");
		}

		EventsResult eventsResult = eventsResponse.data;

		if (CollectionUtil.safeIsEmpty(eventsResult.events)) {
			throw new IllegalStateException("Missing events");
		}

		TransactionTimersRequest transactionTimersRequest = TransactionTimersRequest.newBuilder()
				.setServiceId(args.serviceId).build();

		Response<TransactionTimersResult> transactionTimersResponse = apiClient.get(transactionTimersRequest);

		if (transactionTimersResponse.isBadResponse()) {
			throw new IllegalStateException("Failed getting timers.");
		}

		CodeRedactionExcludeRequest excludeRequest = CodeRedactionExcludeRequest.newBuilder()
				.setServiceId(args.serviceId).build();

		Response<CodeRedactionElements> excludeResponse = apiClient.get(excludeRequest);

		if (excludeResponse.isBadResponse()) {
			throw new IllegalStateException("Failed exclude filters.");
		}

		Map<Pair<String, String>, Long> newTimers = Maps.newHashMap();
		Map<String, Long> updatedTimers = Maps.newHashMap();

		for (Transaction transaction : transactionsResult.transactions) {
			if ((Strings.isNullOrEmpty(transaction.class_name)) || (transaction.stats == null)) {
				continue;
			}

			if (isExcludedTransaction(transaction, excludeResponse.data)) {
				continue;
			}

			long baseThreshold = (long) Math.floor(transaction.stats.avg_time);

			if (baseThreshold < 1L) {
				continue;
			}

			long adjustedThreshold = baseThreshold
					+ Math.round(input.std_dev * transaction.stats.avg_time_std_deviation);

			long timerThreshold = Math.max(adjustedThreshold, input.minimum_absolute_threshold);

			Pair<String, String> fullName = getFullTransactionName(transaction, eventsResult.events);

			if (fullName == null) {
				continue;
			}

			Timer timer = getExistingTransactionTimer(fullName, transactionTimersResponse.data.transaction_timers);

			if (timer == null) {
				newTimers.put(fullName, timerThreshold);
			} else {
				long thresholdDelta = Math.abs(timer.threshold - timerThreshold);

				if (thresholdDelta >= input.minimum_threshold_delta) {
					updatedTimers.put(timer.id, timerThreshold);
				}
			}
		}

		for (Map.Entry<Pair<String, String>, Long> entry : newTimers.entrySet()) {
			Pair<String, String> fullName = entry.getKey();
			long threshold = entry.getValue();

			CreateTransactionTimerRequest createTransactionTimerRequest = CreateTransactionTimerRequest.newBuilder()
					.setServiceId(args.serviceId).setClassName(fullName.getFirst()).setMethodName(fullName.getSecond())
					.setThreshold(threshold).build();

			apiClient.post(createTransactionTimerRequest);
		}

		for (Map.Entry<String, Long> entry : updatedTimers.entrySet()) {
			String timerId = entry.getKey();
			long threshold = entry.getValue();

			EditTransactionTimerRequest editTransactionTimerRequest = EditTransactionTimerRequest.newBuilder()
					.setServiceId(args.serviceId).setTimerId(Integer.parseInt(timerId)).setThreshold(threshold).build();

			apiClient.post(editTransactionTimerRequest);
		}
	}

	private static boolean isExcludedTransaction(Transaction transaction, CodeRedactionElements redactionElements) {
		if (redactionElements == null) {
			return false;
		}

		if (!CollectionUtil.safeIsEmpty(redactionElements.packages)) {
			for (String packageName : redactionElements.packages) {
				if (transaction.class_name.startsWith(packageName)) {
					return true;
				}
			}
		}

		if (!CollectionUtil.safeIsEmpty(redactionElements.classes)) {
			String transactionName = JavaUtil.toSimpleClassName(transaction.class_name);

			for (String className : redactionElements.classes) {
				String simpleClassName = JavaUtil.toSimpleClassName(className);

				if (transactionName.equals(simpleClassName)) {
					return true;
				}
			}
		}

		return false;
	}

	private static Pair<String, String> getFullTransactionName(Transaction transaction, List<EventResult> events) {
		String internalName = JavaUtil.toInternalName(transaction.class_name);
		
		if (!Strings.isNullOrEmpty(transaction.method_name)) {
			return Pair.of(internalName, transaction.method_name);
		}

		for (EventResult event : events) {
			if ((event.entry_point == null) || (Strings.isNullOrEmpty(event.entry_point.class_name))
					|| (Strings.isNullOrEmpty(event.entry_point.method_name))) {
				continue;
			}

			if (internalName.equals(JavaUtil.toInternalName(event.entry_point.class_name))) {
				return Pair.of(internalName, event.entry_point.method_name);
			}
		}

		return null;
	}

	private static Timer getExistingTransactionTimer(Pair<String, String> fullName, List<Timer> timers) {
		if (CollectionUtil.safeIsEmpty(timers)) {
			return null;
		}

		for (Timer timer : timers) {
			if ((fullName.getFirst().equals(JavaUtil.toInternalName(timer.class_name)))
					&& (fullName.getSecond().equals(timer.method_name))) {
				return timer;
			}
		}

		return null;
	}

	static class PeriodicAvgTimerInput extends Input {
		public int timespan; // hours
		public double std_dev; // number of std deviations from the mean time to place threshold
		public long minimum_absolute_threshold;
		public long minimum_threshold_delta;

		private PeriodicAvgTimerInput(String raw) {
			super(raw);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();

			builder.append("PeriodicAvgTimer(");
			builder.append(std_dev);
			builder.append(")");

			return builder.toString();
		}

		static PeriodicAvgTimerInput of(String raw) {
			return new PeriodicAvgTimerInput(raw);
		}
	}
}
