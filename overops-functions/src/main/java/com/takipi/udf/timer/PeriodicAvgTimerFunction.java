package com.takipi.udf.timer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.timer.Timer;
import com.takipi.api.client.data.transaction.Stats;
import com.takipi.api.client.data.transaction.TransactionGraph;
import com.takipi.api.client.request.event.BatchForceSnapshotsRequest;
import com.takipi.api.client.request.event.EventsRequest;
import com.takipi.api.client.request.label.BatchModifyLabelsRequest;
import com.takipi.api.client.request.redaction.CodeRedactionExcludeRequest;
import com.takipi.api.client.request.transactiontimer.CreateTransactionTimerRequest;
import com.takipi.api.client.request.transactiontimer.DeleteTransactionTimerRequest;
import com.takipi.api.client.request.transactiontimer.EditTransactionTimerRequest;
import com.takipi.api.client.request.transactiontimer.TransactionTimersRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.result.redaction.CodeRedactionElements;
import com.takipi.api.client.result.transactiontimer.TransactionTimersResult;
import com.takipi.api.client.util.performance.PerformanceUtil;
import com.takipi.api.client.util.performance.calc.PerformanceScore;
import com.takipi.api.client.util.performance.calc.PerformanceState;
import com.takipi.api.client.util.performance.transaction.GraphPerformanceCalculator;
import com.takipi.api.client.util.transaction.TransactionUtil;
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

		if (input.active_timespan_minutes <= 0) {
			throw new IllegalArgumentException("'active_timespan_minutes' must be positive");
		}

		if (input.baseline_timespan_minutes <= 0) {
			throw new IllegalArgumentException("'baseline_timespan_minutes' must be positive");
		}

		if (input.baseline_timespan_minutes <= input.active_timespan_minutes) {
			throw new IllegalArgumentException(
					"'baseline_timespan_minutes' must be larger than 'active_timespan_minutes'");
		}

		if (input.active_timespan_point_res <= 0) {
			throw new IllegalArgumentException("'active_timespan_point_res' must be positive");
		}

		if (input.baseline_timespan_minutes <= 0) {
			throw new IllegalArgumentException("'baseline_timespan_minutes' must be positive");
		}

		if (input.active_invocations_threshold <= 0) {
			throw new IllegalArgumentException("'active_invocations_threshold' must be positive");
		}

		if (input.baseline_invocations_threshold <= 0) {
			throw new IllegalArgumentException("'baseline_invocations_threshold' must be positive");
		}

		if (input.min_delta_threshold < 0) {
			throw new IllegalArgumentException("'min_delta_threshold' can't be negative");
		}

		if (input.over_avg_slowing_percentage <= 0.0) {
			throw new IllegalArgumentException("'over_avg_slowing_percentage' must be positive");
		}

		if (input.over_avg_slowing_percentage > 1.0) {
			throw new IllegalArgumentException("'over_avg_slowing_percentage' can't be greater than 1.0");
		}

		if (input.over_avg_critical_percentage <= 0.0) {
			throw new IllegalArgumentException("'over_avg_critical_percentage' must be positive");
		}

		if (input.over_avg_critical_percentage > 1.0) {
			throw new IllegalArgumentException("'over_avg_critical_percentage' can't be greater than 1.0");
		}

		if (input.over_avg_critical_percentage <= input.over_avg_slowing_percentage) {
			throw new IllegalArgumentException(
					"'over_avg_critical_percentage' must be larger than 'over_avg_slowing_percentage'");
		}

		if (input.std_dev_factor <= 0.0) {
			throw new IllegalArgumentException("'std_dev_factor' must be positive");
		}

		if (input.min_timer_threshold <= 0) {
			throw new IllegalArgumentException("'min_timer_threshold' must be positive");
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
		DateTime activeFrom = to.minusMinutes(input.active_timespan_minutes);

		Map<String, TransactionGraph> activeTransactions = TransactionUtil.getTransactionGraphs(apiClient,
				args.serviceId, args.viewId, activeFrom, to, input.active_timespan_point_res);

		if (CollectionUtil.safeIsEmpty(activeTransactions)) {
			return;
		}

		DateTime baselineFrom = to.minusMinutes(input.baseline_timespan_minutes);

		Map<String, TransactionGraph> baselineTransactions = TransactionUtil.getTransactionGraphs(apiClient,
				args.serviceId, args.viewId, baselineFrom, to, input.baseline_timespan_point_res);

		GraphPerformanceCalculator calculator = GraphPerformanceCalculator.of(input.active_invocations_threshold,
				input.baseline_invocations_threshold, input.min_delta_threshold, input.over_avg_slowing_percentage,
				input.over_avg_critical_percentage, input.std_dev_factor);

		Map<TransactionGraph, PerformanceScore> performance = PerformanceUtil.getPerformanceStates(activeTransactions,
				baselineTransactions, calculator);

		DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZoneUTC();

		EventsRequest eventsRequest = EventsRequest.newBuilder().setServiceId(args.serviceId).setViewId(args.viewId)
				.setFrom(baselineFrom.toString(fmt)).setTo(to.toString(fmt)).build();

		Response<EventsResult> eventsResponse = apiClient.get(eventsRequest);

		if (eventsResponse.isBadResponse()) {
			throw new IllegalStateException("Failed getting view events.");
		}

		EventsResult eventsResult = eventsResponse.data;

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

		Map<TransactionGraph, List<EventResult>> eventsMap = buildTransactionEvents(activeTransactions.values(),
				eventsResult.events);

		boolean labelsUpdateNeeded = false;
		BatchModifyLabelsRequest.Builder labelsRequestBuilder = BatchModifyLabelsRequest.newBuilder()
				.setServiceId(args.serviceId).setHandleSimilarEvents(false);

		Map<TransactionName, Long> newTimers = Maps.newHashMap();
		Map<String, Long> updatedTimers = Maps.newHashMap();
		List<String> removedTimers = Lists.newArrayList();
		Set<String> existingLabels = Sets.newHashSet();
		Set<String> eventsToForceSnapshot = Sets.newHashSet();

		for (Map.Entry<TransactionGraph, PerformanceScore> entry : performance.entrySet()) {
			TransactionGraph transaction = entry.getKey();
			List<EventResult> transactionEvents = eventsMap.get(transaction);

			// Can happen if the transaction didn't have any events in the timeframe.
			//
			if (transactionEvents == null) {
				transactionEvents = Collections.emptyList();
			}

			TransactionName transactionName = getTransactionName(transaction, transactionEvents);

			if (transactionName == null) {
				continue;
			}

			List<EventResult> transactionTimerEvents = getTimerEvents(transactionEvents);

			Timer timer = getExistingTransactionTimer(transactionName,
					transactionTimersResponse.data.transaction_timers);

			boolean excludedTransaction = isExcludedTransaction(transaction, excludeResponse.data);

			PerformanceScore score = entry.getValue();
			PerformanceState state = (excludedTransaction ? PerformanceState.NO_DATA : score.state);

			if (addLabelModifications(transactionTimerEvents, state, labelsRequestBuilder, existingLabels,
					args.serviceId, apiClient)) {
				labelsUpdateNeeded = true;
			}

			if ((state == PerformanceState.NO_DATA) || (state == PerformanceState.OK)) {
				if (timer != null) {
					removedTimers.add(timer.id);
				}

				continue;
			}

			Stats stats = TransactionUtil.aggregateGraph(transaction);

			long timerThreshold = (long) (stats.avg_time + stats.avg_time_std_deviation);

			if (timerThreshold < input.min_timer_threshold) {
				continue;
			}

			if (timer == null) {
				newTimers.put(transactionName, timerThreshold);
			} else {
				updatedTimers.put(timer.id, timerThreshold);
			}

			for (EventResult event : transactionTimerEvents) {
				eventsToForceSnapshot.add(event.id);
			}
		}

		for (Map.Entry<TransactionName, Long> entry : newTimers.entrySet()) {
			TransactionName transactionName = entry.getKey();
			long threshold = entry.getValue();

			CreateTransactionTimerRequest createTransactionTimerRequest = CreateTransactionTimerRequest.newBuilder()
					.setServiceId(args.serviceId).setClassName(transactionName.className)
					.setMethodName(transactionName.methodName).setThreshold(threshold).build();

			apiClient.post(createTransactionTimerRequest);
		}

		for (Map.Entry<String, Long> entry : updatedTimers.entrySet()) {
			String timerId = entry.getKey();
			long threshold = entry.getValue();

			EditTransactionTimerRequest editTransactionTimerRequest = EditTransactionTimerRequest.newBuilder()
					.setServiceId(args.serviceId).setTimerId(Integer.parseInt(timerId)).setThreshold(threshold).build();

			apiClient.post(editTransactionTimerRequest);
		}

		for (String timerId : removedTimers) {
			DeleteTransactionTimerRequest deleteTransactionTimerRequest = DeleteTransactionTimerRequest.newBuilder()
					.setServiceId(args.serviceId).setTimerId(Integer.parseInt(timerId)).build();

			apiClient.delete(deleteTransactionTimerRequest);
		}

		if (labelsUpdateNeeded) {
			apiClient.post(labelsRequestBuilder.build());
		}

		if (!eventsToForceSnapshot.isEmpty()) {
			BatchForceSnapshotsRequest forceSnapshotsRequest = BatchForceSnapshotsRequest.newBuilder()
					.setServiceId(args.serviceId).addEventIds(eventsToForceSnapshot).build();

			apiClient.post(forceSnapshotsRequest);
		}
	}

	private static List<EventResult> getTimerEvents(List<EventResult> events) {
		if (CollectionUtil.safeIsEmpty(events)) {
			return Collections.emptyList();
		}

		List<EventResult> result = Lists.newArrayList();

		for (EventResult event : events) {
			if ("timer".equalsIgnoreCase(event.type)) {
				result.add(event);
			}
		}

		return result;
	}

	private static boolean addLabelModifications(List<EventResult> events, PerformanceState state,
			BatchModifyLabelsRequest.Builder labelsRequestBuilder, Set<String> existingLabels, String serviceId,
			ApiClient apiClient) {

		boolean hasModifications = false;

		for (EventResult event : events) {
			Pair<Collection<String>, Collection<String>> modifications = PerformanceUtil.categorizeEvent(event,
					serviceId, state, existingLabels, apiClient, false);

			Collection<String> labelsToAdd = modifications.getFirst();
			Collection<String> labelsToRemove = modifications.getSecond();

			if ((labelsToAdd.isEmpty()) && (labelsToRemove.isEmpty())) {
				continue;
			}

			labelsRequestBuilder.addLabelModifications(event.id, labelsToAdd, labelsToRemove);

			hasModifications = true;
		}

		return hasModifications;
	}

	private static Map<TransactionGraph, List<EventResult>> buildTransactionEvents(
			Collection<TransactionGraph> transactions, List<EventResult> events) {

		if (CollectionUtil.safeIsEmpty(events)) {
			return Collections.emptyMap();
		}

		Map<TransactionGraph, List<EventResult>> result = Maps.newHashMap();

		for (EventResult event : events) {
			if ((event.entry_point == null) || (Strings.isNullOrEmpty(event.entry_point.class_name))
					|| (Strings.isNullOrEmpty(event.entry_point.method_name))) {
				continue;
			}

			for (TransactionGraph transaction : transactions) {
				if (!JavaUtil.toInternalName(event.entry_point.class_name).equals(transaction.class_name)) {
					continue;
				}

				if ((!Strings.isNullOrEmpty(transaction.method_name))
						&& (!event.entry_point.method_name.equals(transaction.method_name))) {
					continue;
				}

				List<EventResult> transactionEvents = result.get(transaction);

				if (transactionEvents == null) {
					transactionEvents = Lists.newArrayList();
					result.put(transaction, transactionEvents);
				}

				transactionEvents.add(event);
			}
		}

		return result;
	}

	private static boolean isExcludedTransaction(TransactionGraph transaction,
			CodeRedactionElements redactionElements) {

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
			String simpleTransactionName = JavaUtil.toSimpleClassName(transaction.class_name);

			for (String className : redactionElements.classes) {
				String simpleClassName = JavaUtil.toSimpleClassName(className);

				if (simpleTransactionName.equals(simpleClassName)) {
					return true;
				}
			}
		}

		return false;
	}

	private static TransactionName getTransactionName(TransactionGraph transaction, List<EventResult> events) {
		String internalName = JavaUtil.toInternalName(transaction.class_name);

		if (!Strings.isNullOrEmpty(transaction.method_name)) {
			return TransactionName.of(internalName, transaction.method_name);
		}

		if (events.isEmpty()) {
			return null;
		}

		for (EventResult event : events) {
			if (!Strings.isNullOrEmpty(event.entry_point.method_name)) {
				return TransactionName.of(internalName, event.entry_point.method_name);
			}
		}

		return null;
	}

	private static Timer getExistingTransactionTimer(TransactionName transactionName, List<Timer> timers) {
		if (CollectionUtil.safeIsEmpty(timers)) {
			return null;
		}

		for (Timer timer : timers) {
			if ((transactionName.className.equals(JavaUtil.toInternalName(timer.class_name)))
					&& (transactionName.methodName.equals(timer.method_name))) {
				return timer;
			}
		}

		return null;
	}

	static class TransactionName {
		public final String className;
		public final String methodName;

		private TransactionName(String className, String methodName) {
			this.className = className;
			this.methodName = methodName;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}

			if ((o == null) || (!(o instanceof TransactionName))) {
				return false;
			}

			TransactionName other = (TransactionName) o;

			return ((this.className.equals(other.className)) && (this.methodName.equals(other.methodName)));
		}

		@Override
		public int hashCode() {
			return className.hashCode();
		}

		static TransactionName of(String className, String methodName) {
			return new TransactionName(className, methodName);
		}
	}

	static class PeriodicAvgTimerInput extends Input {
		public int active_timespan_minutes;
		public int active_timespan_point_res;
		public int baseline_timespan_minutes;
		public int baseline_timespan_point_res;

		public long active_invocations_threshold;
		public long baseline_invocations_threshold;
		public int min_delta_threshold;
		public double over_avg_slowing_percentage;
		public double over_avg_critical_percentage;
		public double std_dev_factor;

		public long min_timer_threshold;

		private PeriodicAvgTimerInput(String raw) {
			super(raw);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();

			builder.append("PeriodicAvgTimer(");
			builder.append(active_timespan_minutes);
			builder.append(")");

			return builder.toString();
		}

		static PeriodicAvgTimerInput of(String raw) {
			return new PeriodicAvgTimerInput(raw);
		}
	}
}
