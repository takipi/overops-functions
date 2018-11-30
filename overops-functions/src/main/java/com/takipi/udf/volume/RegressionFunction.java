package com.takipi.udf.volume;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
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
import com.takipi.udf.ContextArgs;
import com.takipi.udf.input.Input;

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

		if (input.activeTimespan <= 0) {
			throw new IllegalArgumentException("'activeTimespan' must be positive'");
		}

		if (input.baseTimespan <= 0) {
			throw new IllegalArgumentException("'timespan' must be positive");
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
		regressionInput.activeTimespan = input.activeTimespan;
		regressionInput.baselineTimespan = input.baseTimespan;
		regressionInput.minVolumeThreshold = input.minVolumeThreshold;
		regressionInput.minErrorRateThreshold = input.minErrorRateThreshold / 100;
		regressionInput.regressionDelta = input.regressionDelta / 100;
		regressionInput.applySeasonality = true;

		if (input.appName != null) {
			regressionInput.applictations = Arrays.asList(input.appName.split(","));
		}

		RateRegression rateRegression = RegressionUtil.calculateRateRegressions(apiClient, regressionInput, System.out,
				false);

		Collection<RegressionResult> activeRegressions = rateRegression.getAllRegressions().values();

		if (activeRegressions.size() == 0) {
			System.out.println("No anomalies found");
			return;
		}

		List<EventResult> contributors = Lists.newArrayList();

		for (RegressionResult regressionResult : activeRegressions) {
			contributors.add(regressionResult.getEvent());
		}

		System.out.println("Alerting on " + activeRegressions.size() + " anomalies: "
				+ StringUtils.join(contributors.toArray(), ','));

		AnomalyUtil.send(apiClient, args.serviceId, args.viewId, contributors, rateRegression.getActiveWndowStart(),
				DateTime.now(), input.toString());
	}

	static class RegressionFunctionInput extends Input {
		public int activeTimespan;
		public int baseTimespan;
		public double regressionDelta;
		public double minErrorRateThreshold;
		public int minVolumeThreshold;
		public String appName;

		private RegressionFunctionInput(String raw) {
			super(raw);
		}

		static RegressionFunctionInput of(String raw) {
			return new RegressionFunctionInput(raw);
		}

		private String prettify(int value) {
			if (TimeUnit.MINUTES.toHours(value) > 24) {
				return TimeUnit.MINUTES.toDays(value) + " days";
			}

			if (TimeUnit.MINUTES.toHours(value) > 1) {
				return TimeUnit.MINUTES.toDays(value) + " hours";
			}

			return value + " minutes";
		}

		@Override
		public String toString() {
			String result = String.format("Anomaly(last %s vs. prev %s, change > %.2f%%)", prettify(activeTimespan),
					prettify(baseTimespan), regressionDelta * 100);

			return result;
		}
	}
}
