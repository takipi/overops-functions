package com.takipi.udf.volume;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.joda.time.DateTime;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.util.regression.RateRegression;
import com.takipi.api.client.util.regression.RegressionInput;
import com.takipi.api.client.util.regression.RegressionResult;
import com.takipi.api.client.util.regression.RegressionUtil;
import com.takipi.common.util.CollectionUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.input.Input;
import com.takipi.udf.input.TimeInterval;
import com.takipi.udf.util.TestUtil;

public class RegressionFunction {
	public static String validateInput(String rawInput) {
		return parseRegressionInput(rawInput).toString();
	}

	static RegressionFunctionInput parseRegressionInput(String rawInput) {
		System.out.println("validateInput rawInput:" + rawInput);

		if (Strings.isNullOrEmpty(rawInput)) {
			throw new IllegalArgumentException("Input is empty");
		}

		RegressionFunctionInput input;

		try {
			input = RegressionFunctionInput.of(rawInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		if (input.activeTimespan == null) {
			throw new IllegalArgumentException("'activeTimespan' must not be empty");
		} else if (input.activeTimespan.isNegative()) {
			throw new IllegalArgumentException("'activeTimespan' can't be negative time");
		}

		if (input.baseTimespan == null) {
			throw new IllegalArgumentException("'baseTimespan' must not be empty");
		} else if (input.baseTimespan.isNegative()) {
			throw new IllegalArgumentException("'baseTimespan' can't be negative time");
		}

		if (input.min_interval == null) {
			input.min_interval = TimeInterval.parse("2d");
		} else if (input.min_interval.isNegative()) {
			throw new IllegalArgumentException("'min_interval' can't be negative time");
		}

		if (input.max_interval == null) {
			input.max_interval = TimeInterval.parse("3d");
		} else if (input.max_interval.isNegative()) {
			throw new IllegalArgumentException("'max_interval' can't be negative time");
		}

		if (input.max_interval.asMinutes() <= input.min_interval.asMinutes()) {
			throw new IllegalArgumentException("'max_interval' must be greater than 'min_interval'");
		}

		if (input.regressionDelta <= 0) {
			throw new IllegalArgumentException("'regressionDelta' must be positive");
		}

		return input;
	}

	public static void execute(String rawContextArgs, String rawInput) {
		System.out.println("execute:" + rawContextArgs);

		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		ApiClient apiClient = args.apiClient();

		if (!args.viewValidate()) {
			throw new IllegalArgumentException("Invalid context args - " + rawContextArgs);
		}

		RegressionFunctionInput input = parseRegressionInput(rawInput);

		System.out.println("Calculating regressions\n");

		RegressionInput regressionInput = new RegressionInput();

		regressionInput.serviceId = args.serviceId;
		regressionInput.viewId = args.viewId;
		regressionInput.activeTimespan = input.activeTimespan.asMinutes();
		regressionInput.baselineTimespan = input.baseTimespan.asMinutes();
		regressionInput.minVolumeThreshold = input.minVolumeThreshold;
		regressionInput.minErrorRateThreshold = input.minErrorRateThreshold / 100;
		regressionInput.regressionDelta = input.regressionDelta / 100;
		regressionInput.applySeasonality = true;

		if (input.appName != null) {
			regressionInput.applictations = Arrays.asList(input.appName.split(","));
		}

		RateRegression rateRegression = RegressionUtil.calculateRateRegressions(apiClient, regressionInput, System.out,
				false);

		AnomalyUtil.removeAnomalyLabel(rateRegression.getNonRegressions(), apiClient, args.serviceId,
				input.max_interval, input.label);

		Collection<RegressionResult> activeRegressions = rateRegression.getAllRegressions().values();

		if (activeRegressions.size() == 0) {
			System.out.println("No anomalies found");
			return;
		}

		List<EventResult> candidates = Lists.newArrayList();

		for (RegressionResult regressionResult : activeRegressions) {
			candidates.add(regressionResult.getEvent());
		}

		Collection<EventResult> contributors = AnomalyUtil.filterAnomalyEvents(candidates, apiClient, args.serviceId,
				input.min_interval, input.label, AnomalyUtil.MAX_ANOMALY_CONTRIBUTORS);

		if (CollectionUtil.safeIsEmpty(contributors)) {
			return;
		}

		AnomalyUtil.reportAnomaly(apiClient, args.serviceId, args.viewId, contributors, input.label,
				rateRegression.getActiveWndowStart(), DateTime.now(), input.toString());
	}

	static class RegressionFunctionInput extends Input {

		public String appName;

		public TimeInterval activeTimespan;
		public TimeInterval baseTimespan;

		public double regressionDelta;

		public double minErrorRateThreshold;
		public int minVolumeThreshold;

		public String label;
		public TimeInterval min_interval;
		public TimeInterval max_interval;

		private RegressionFunctionInput(String raw) {
			super(raw);
		}

		static RegressionFunctionInput of(String raw) {
			return new RegressionFunctionInput(raw);
		}

		@Override
		public String toString() {
			String result = String.format("Anomaly(last %s vs. prev %s, %s > %.0f%%)", activeTimespan, baseTimespan,
					Character.toString((char) 916), regressionDelta);

			return result;
		}
	}

	// A sample program on how to programmatically activate RegressionFunction
	//
	public static void main(String[] args) {
		String rawContextArgs = TestUtil.getViewContextArgs(args, "All Events");

		// example values
		//
		String[] sampleValues = new String[] { "activeTimespan=1d", "baseTimespan=14d", "regressionDelta=100",
				"minErrorRateThreshold=1", "minVolumeThreshold=100", "label=Anomaly", "minInterval=1d" };

		RegressionFunction.execute(rawContextArgs, String.join("\n", sampleValues));
	}
}
