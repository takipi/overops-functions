package com.takipi.udf.volume;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.transaction.Transaction;
import com.takipi.api.client.request.event.EventModifyLabelsRequest;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.request.label.CreateLabelRequest;
import com.takipi.api.client.request.transaction.TransactionsVolumeRequest;
import com.takipi.api.client.result.EmptyResult;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsVolumeResult;
import com.takipi.api.client.result.transaction.TransactionsVolumeResult;
import com.takipi.api.client.util.validation.ValidationUtil.VolumeType;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.input.Input;

public class ThresholdFunction {
	
	private static final int DEFAULT_TIME_WINDOW = 60;
	
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

		if (input.threshold < 0l) {
			throw new IllegalArgumentException("'threshold' must be positive");
		}

		if ((input.relative_to == Mode.Method_Calls) && (input.rate <= 0.0)) {
			throw new IllegalArgumentException("'rate' must be positive");
		}

		if ((input.relative_to == Mode.Thread_Calls) && (input.rate <= 0.0)) {
			throw new IllegalArgumentException("'rate' must be positive");
		}

		return input;
	}

	static void execute(String rawContextArgs, ThresholdInput input) {
		System.out.println("execute:" + rawContextArgs);

		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		if (!args.viewValidate()) {
			throw new IllegalArgumentException("Bad context args - " + rawContextArgs);
		}

		ApiClient apiClient = args.apiClient();

		VolumeType volumeType = ((input.relative_to == null) || (input.relative_to == Mode.Method_Calls) 
			? VolumeType.all : VolumeType.hits);

		int timespan;
		
		if (input.timespan != 0) {
			timespan = input.timespan;
		} else {
			timespan = DEFAULT_TIME_WINDOW;
		}
		
		DateTime to = DateTime.now();
		DateTime from = to.minusMinutes(timespan);

		DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZoneUTC();

		EventsVolumeRequest eventsVolumeRequest = EventsVolumeRequest.newBuilder().setServiceId(args.serviceId)
				.setViewId(args.viewId).setFrom(from.toString(fmt)).setTo(to.toString(fmt)).setVolumeType(volumeType)
				.build();

		Response<EventsVolumeResult> eventsVolumeResponse = apiClient.get(eventsVolumeRequest);

		if (eventsVolumeResponse.isBadResponse()) {
			throw new IllegalStateException("Can't create events volume.");
		}

		EventsVolumeResult eventsVolumeResult = eventsVolumeResponse.data;

		if (eventsVolumeResult == null) {
			throw new IllegalStateException("Missing events volume result.");
		}

		if (eventsVolumeResult.events == null) {
			return;
		}

		long hitCount = 0l;

		for (EventResult event : eventsVolumeResult.events) {
			if (event.stats != null) {
				hitCount += event.stats.hits;
			}
		}

		if (hitCount <= input.threshold) {
			return;
		}

		boolean thresholdExceeded = false;

		Mode mode;
		
		if (input.relative_to != null) {
			mode = input.relative_to;
		} else {
			mode = Mode.Method_Calls;
		}
		
		switch (mode) {
		case Absolute: {
			thresholdExceeded = true;
		}
			break;

		case Method_Calls: {
			long invocationsCount = 0l;

			Collections.sort(eventsVolumeResult.events, new Comparator<EventResult>() {
				@Override
				public int compare(EventResult o1, EventResult o2) {
					int i1 = Integer.parseInt(o1.id);
					int i2 = Integer.parseInt(o2.id);
					return i1 - i2;
				}
			});

			for (EventResult event : eventsVolumeResult.events) {
				if (event.stats != null) {
					System.out.println(event.id + ": " + event.summary + " - hits: " + event.stats.hits + " - inv: "
							+ event.stats.invocations);

					invocationsCount += Math.max(event.stats.invocations, event.stats.hits);
				}
			}

			invocationsCount = Math.max(invocationsCount, hitCount);

			double failRate = (hitCount / (double) invocationsCount) * 100.0;

			thresholdExceeded = (failRate >= input.rate);
		}
			break;

		case Thread_Calls: {
			TransactionsVolumeRequest transactionsVolumeRequest = TransactionsVolumeRequest.newBuilder()
					.setServiceId(args.serviceId).setViewId(args.viewId)
					.setFrom(DateTime.now().minusMinutes(input.timespan).toString()).setTo(DateTime.now().toString())
					.build();

			Response<TransactionsVolumeResult> transactionsVolumeResponse = apiClient.get(transactionsVolumeRequest);

			if (transactionsVolumeResponse.isBadResponse()) {
				throw new IllegalStateException("Can't create transactions volume.");
			}

			TransactionsVolumeResult transactionsVolumeResult = transactionsVolumeResponse.data;

			if (transactionsVolumeResult == null) {
				throw new IllegalStateException("Missing events volume result.");
			}

			long transactionInvocationsCount = 0l;

			for (Transaction transaction : transactionsVolumeResult.transactions) {
				if (transaction.stats != null) {
					transactionInvocationsCount += transaction.stats.invocations;
				}
			}

			if (transactionInvocationsCount > 0l) {
				double failRate = (hitCount / (double) transactionInvocationsCount) * 100.0;

				thresholdExceeded = (failRate >= input.rate);
			}
		}
			break;
		}

		System.out.println("Threshold response: " + thresholdExceeded);

		if (!thresholdExceeded) {
			return;
		}

		List<EventResult> contributors = Lists.newArrayList();
		
		for (EventResult event : eventsVolumeResult.events) {
			if ((event.stats != null) && (event.stats.hits > 0)) {
				contributors.add(event);
			}
		}
		
		AnomalyUtil.send(apiClient, args.serviceId, args.viewId, 
			contributors, from, to, input.toString());

		// Mark all contributors as Alert label
		if (!StringUtils.isEmpty(input.label)) {
			CreateLabelRequest createLabel = CreateLabelRequest.newBuilder().setServiceId(args.serviceId)
					.setName(input.label).build();

			Response<EmptyResult> createResult = apiClient.post(createLabel);

			if ((createResult.isBadResponse()) && (createResult.responseCode != HttpURLConnection.HTTP_CONFLICT)) {
				throw new IllegalStateException("Can't create label " + input);
			}

			for (EventResult contributor : contributors) {
				EventModifyLabelsRequest addLabel = EventModifyLabelsRequest.newBuilder().setServiceId(args.serviceId)
						.setEventId(String.valueOf(contributor.id)).addLabel(input.label).build();

				Response<EmptyResult> addResult = apiClient.post(addLabel);

				if (addResult.isBadResponse()) {
					throw new IllegalStateException("Can't apply label " + input.label + " to event " + args.eventId);
				}
			}
		}
	}

	static class ThresholdInput extends Input {
		public Mode relative_to;
		public long threshold;
		public double rate;
		public int timespan; // minutes
		public String label;

		private ThresholdInput(String raw) {
			super(raw);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();

			builder.append("Threshold(");

			Mode mode;
			
			if (relative_to != null ) {
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
