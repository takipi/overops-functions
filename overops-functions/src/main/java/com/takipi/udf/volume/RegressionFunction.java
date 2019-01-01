package com.takipi.udf.volume;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.util.regression.RateRegression;
import com.takipi.api.client.util.regression.RegressionInput;
import com.takipi.api.client.util.regression.RegressionResult;
import com.takipi.api.client.util.regression.RegressionUtil;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.common.util.CollectionUtil;
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

		if ((input.activeTimespan == null) || (input.activeTimespan.length() == 0)) {
			throw new IllegalArgumentException("'activeTimespan' must not be empty");
		} else {
			ThresholdFunction.parseInterval(input.activeTimespan);
		}

		if ((input.baseTimespan == null) || (input.baseTimespan.length() == 0)) {
			throw new IllegalArgumentException("'base timespan' must not be empty");
		} else {
			ThresholdFunction.parseInterval(input.baseTimespan);
		}
		
		try {
			ThresholdFunction.parseInterval(input.minInterval);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("invalid anomaly interval " + input.minInterval);
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
		regressionInput.activeTimespan = ThresholdFunction.parseInterval(input.activeTimespan);
		regressionInput.baselineTimespan = ThresholdFunction.parseInterval(input.baseTimespan);
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

		List<EventResult> candidates = Lists.newArrayList();

		for (RegressionResult regressionResult : activeRegressions) {
			candidates.add(regressionResult.getEvent());
		}

		int interval = ThresholdFunction.parseInterval(input.minInterval);
		
		Collection<EventResult> contributors = ThresholdFunction.getContributors(candidates, apiClient, 
			args.serviceId, interval, input.label);
		
		if (CollectionUtil.safeIsEmpty(contributors)) {
			return;
		}
		
		System.out.println("Alerting on " + contributors.size() + " anomalies: "
				+ StringUtils.join(contributors.toArray(), ','));

		ThresholdFunction.applyAnomalyLabels(apiClient, args.serviceId,
				input.label, contributors);
		
		AnomalyUtil.send(apiClient, args.serviceId, args.viewId, contributors, 
			rateRegression.getActiveWndowStart(), DateTime.now(), input.toString());
	}

	static class RegressionFunctionInput extends Input {
		
		public String appName;
		
		public String activeTimespan;
		public String baseTimespan;

		public double regressionDelta;
		
		public double minErrorRateThreshold;
		public int minVolumeThreshold;
		
		public String label;
		public String minInterval;

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

		if ((args == null) || (args.length < 3)) {
			throw new IllegalArgumentException("args");
		}

		ContextArgs contextArgs = new ContextArgs();

		contextArgs.apiHost = args[0];
		contextArgs.apiKey = args[1];
		contextArgs.serviceId = args[2];

		SummarizedView view = ViewUtil.getServiceViewByName(contextArgs.apiClient(), contextArgs.serviceId,
				"All Events");

		contextArgs.viewId = view.id;

		// example values
		//
		String[] sampleValues = new String[] { "activeTimespan=1d", "baseTimespan=14d", "regressionDelta=100",
				"minErrorRateThreshold=1", "minVolumeThreshold=100", 
				"label=Anomaly", "minInterval=1d"};

		String rawContextArgs = new Gson().toJson(contextArgs);
		RegressionFunction.execute(rawContextArgs, String.join("\n", sampleValues));
	}
}
