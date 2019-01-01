package com.takipi.udf.volume;

import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

		if (input.timespan < 0) {
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
		
		try {
			parseInterval(input.minInterval);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("invalid anomaly interval " + input.minInterval);
		}

		return input;
	}
	
	private static List<EventResult> getEventVolume(ApiClient apiClient, String serviceId,
		String viewId, DateTime from, DateTime to, VolumeType volumeType) {

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
	
	private static long getHits(EventResult event) {
		
		if (event.stats == null) {
			return 0l;
		}
		
		return event.stats.hits;
	}
	
	private static void sortEventsByHitsDesc(List<EventResult> events) {
		
		Collections.sort(events, new Comparator<EventResult>() {
			@Override
			public int compare(EventResult o1, EventResult o2) {
				return (int)(getHits(o2) - getHits(o1));
			}
		});
	}
	
	private static long getEventsHits(Collection<EventResult> events) {
		
		long result = 0l;

		for (EventResult event : events) {			
			result += getHits(event);
		}
		
		return result;
	}
	
	private static long getEventsInvocations(Collection<EventResult> events, long hitCount) {
		
		long invocations = 0;
		
		for (EventResult event : events) {
			
			if (event.stats != null) {
				System.out.println(event.id + ": " + event.summary + " - hits: " + event.stats.hits + " - inv: "
						+ event.stats.invocations);

				invocations += Math.max(event.stats.invocations, event.stats.hits);
			}
		}

		long result = Math.max(invocations, hitCount);
		
		return result;
	}
	
	private static long getTransactionVolume(ApiClient apiClient,
		String serviceId, String viewId, DateTime from, DateTime to) {
		
		TransactionsVolumeRequest transactionsVolumeRequest = TransactionsVolumeRequest.newBuilder()
				.setServiceId(serviceId).setViewId(viewId)
				.setFrom(from.toString(fmt)).setTo(to.toString(fmt))
				.build();

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
			
		CreateLabelRequest createLabel = CreateLabelRequest.newBuilder().setServiceId(serviceId)
			.setName(label).build();

		Response<EmptyResult> createResult = apiClient.post(createLabel);

		if ((createResult.isBadResponse()) && (createResult.responseCode != HttpURLConnection.HTTP_CONFLICT)) {
			throw new IllegalStateException("Cannot create label " + label);
		}

		BatchModifyLabelsRequest.Builder builder = BatchModifyLabelsRequest.newBuilder().
			setServiceId(serviceId).setForceHistory(true);
		
		for (EventResult contributor : contributors) {
			
			builder.addLabelModifications(contributor.id, Collections.singleton(label), 
				Collections.emptyList());
		}
		
		Response<EmptyResult> addResult = apiClient.post(builder.build());

		if (addResult.isBadResponse()) {
			throw new IllegalStateException("Can't apply label " + label + " to contribs");
		}
	}
	
	private static boolean isAlertAllowed(ApiClient apiClient, String serviceId, 
		EventResult event, String label, int interval) {
		
		EventActionsRequest request = EventActionsRequest.newBuilder().
			setServiceId(serviceId).setEventId(event.id).build();
			
		Response<EventActionsResult> response = apiClient.get(request);
			
		if (response.isBadResponse()) {
			//in case of API failure we should not prevent an alert
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
			long actionInterval = TimeUnit.MILLISECONDS.toMinutes(delta);
			
			if (actionInterval < interval) {
				return false;
			}
		}
		
		return true;
	}
	
	public static Collection<EventResult> getContributors(Collection<EventResult> events, 
		ApiClient apiClient, String serviceId, int interval, String label) {
		
		List<EventResult> result = Lists.newArrayList();
		boolean checkAlertAllowed = (interval > 0) && (label != null) && (label.length() > 0);
		
		int index = 0;
		
		for (EventResult event : events) {
		
			if (index >= MAX_CONTRIBUTORS) {
				break;
			}
			
			index++;
			
			if (getHits(event) == 0) {
				continue;
			}
			
			if ((checkAlertAllowed) && (!isAlertAllowed(apiClient, serviceId, event, label, interval))) {
				continue;
			}
			
			result.add(event);
		}
		
		return result;
	}
	
	public static int parseInterval(String timeWindowWithUnit) {

		if ((timeWindowWithUnit == null) || (timeWindowWithUnit.length() == 0)) {
			return 0;
		}
		
		String timeWindow = timeWindowWithUnit.substring(0, timeWindowWithUnit.length() - 1);
		char timeUnit = timeWindowWithUnit.charAt(timeWindowWithUnit.length() - 1);

		int delta = Integer.valueOf(timeWindow);
		if (timeUnit == 'd') {
			return delta * 24 * 60;
		} else if (timeUnit == 'h') {
			return delta * 60;
		} else if (timeUnit == 'm') {
			return delta;
		} else {
			return Integer.valueOf(timeWindowWithUnit);
		}
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

		int timespan = (input.timespan != 0) ? input.timespan : DEFAULT_TIME_WINDOW;

		DateTime to = DateTime.now();
		DateTime from = to.minusMinutes(timespan);
		
		List<EventResult> events = getEventVolume(apiClient, args.serviceId,
			args.viewId, from, to, volumeType);
		
		if (events == null) {
			return;
		}

		long hitCount = getEventsHits(events);

		if ((input.threshold > 0) && (hitCount <= input.threshold)) {
			return;
		}
		
		int interval = parseInterval(input.minInterval);

		boolean thresholdExceeded = false;

		Mode mode = (input.relative_to != null) ? input.relative_to : Mode.Method_Calls;

		sortEventsByHitsDesc(events);

		switch (mode) {
		
			case Absolute: {
				thresholdExceeded = true;
			}
				break;
	
			case Method_Calls: {
					
				long invocationsCount = getEventsInvocations(events, hitCount);		
				double failRate = (hitCount / (double) invocationsCount) * 100.0;
	
				thresholdExceeded = (failRate >= input.rate);
			}
				break;
	
			case Thread_Calls: {
				
				long transactionInvocationsCount  = getTransactionVolume(apiClient, args.serviceId, 
					args.viewId, from, to);
				
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

		Collection<EventResult> contributors = getContributors(events, apiClient, args.serviceId, interval, input.label);
		
		if (CollectionUtil.safeIsEmpty(contributors)) {
			return;
		}
		
		System.out.println("Alerting on " + contributors.size() + " anomalies: "
				+ StringUtils.join(contributors.toArray(), ','));

		applyAnomalyLabels(apiClient, args.serviceId, input.label, contributors);

		AnomalyUtil.send(apiClient, args.serviceId, args.viewId, contributors, from, to, input.toString());
	}
	
	static class ThresholdInput extends Input {
		
		public Mode relative_to;
		
		public long threshold;
		public double rate;
		
		public int timespan; // minutes
		
		public String label;
		public String minInterval;

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
