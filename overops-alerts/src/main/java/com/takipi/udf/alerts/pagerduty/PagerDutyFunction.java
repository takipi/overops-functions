package com.takipi.udf.alerts.pagerduty;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.takipi.common.util.StringUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.pagerduty.client.PagerDutyClient;
import com.takipi.udf.alerts.pagerduty.sender.PagerDutyAnomalySender;
import com.takipi.udf.alerts.pagerduty.sender.PagerDutyNewEventSender;
import com.takipi.udf.alerts.pagerduty.sender.PagerDutyResurfacedEventSender;
import com.takipi.udf.alerts.pagerduty.sender.PagerDutySender;
import com.takipi.udf.alerts.pagerduty.sender.PagerDutyThresholdSender;
import com.takipi.udf.input.Input;

public class PagerDutyFunction {
	public static String validateInput(String rawInput) {
		PagerDutyInput input = getPagerDutyInput(rawInput);
		PagerDutyClient client = input.client();

		if (!client.testConnection()) {
			throw new IllegalArgumentException("Failed connection test");
		}

		return "PagerDuty Channel(" + input.service_integration_key + ")";
	}

	static PagerDutyInput getPagerDutyInput(String rawInput) {
		System.out.println("validateInput rawInput:" + rawInput);

		if (StringUtil.isNullOrEmpty(rawInput)) {
			throw new IllegalArgumentException("Input is empty");
		}

		PagerDutyInput input;

		try {
			input = PagerDutyInput.of(rawInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		if (StringUtils.isEmpty(input.service_integration_key)) {
			throw new IllegalArgumentException("Service Integration Key can't be empty");
		}

		return input;
	}

	public static void execute(String rawContextArgs, String rawInput) {
		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		System.out.println("execute context: " + rawContextArgs);

		if (!args.validate()) {
			throw new IllegalArgumentException("Bad context args - " + rawContextArgs);
		}

		PagerDutyInput input = getPagerDutyInput(rawInput);

		PagerDutySender sender = null;

		switch (args.executionContext) {
		case NEW_EVENT:
			sender = PagerDutyNewEventSender.create(input, args);
			break;
		case RESURFACED_EVENT:
			sender = PagerDutyResurfacedEventSender.create(input, args);
			break;
		case THRESHOLD:
			sender = PagerDutyThresholdSender.create(input, args);
			break;
		case ANOMALY:
			sender = PagerDutyAnomalySender.create(input, args);
			break;

		case CUSTOM_ALERT:
		case PERIODIC:
			return;
		}

		if (sender == null) {
			return;
		}

		try {
			if (!sender.sendMessage()) {
				throw new IllegalStateException("Failed sending " + args.executionContext.toString()
						+ "pager duty message for " + args.serviceId);
			}
		} catch (Exception e) {
			throw new IllegalStateException(
					"Failed sending " + args.executionContext.toString() + "pager duty message for " + args.serviceId,
					e);
		}
	}

	public static class PagerDutyInput extends Input {
		public String service_integration_key;
		public String proxy_uri;
		public String proxy_username;
		public String proxy_password;

		private PagerDutyInput(String raw) {
			super(raw);
		}

		public PagerDutyClient client() {
			return PagerDutyClient.newBuilder().setServiceIntegrationKey(service_integration_key).setProxyUri(proxy_uri)
					.setProxyUsername(proxy_username).setProxyPassword(proxy_password).build();
		}

		static PagerDutyInput of(String raw) {
			return new PagerDutyInput(raw);
		}
	}
}
