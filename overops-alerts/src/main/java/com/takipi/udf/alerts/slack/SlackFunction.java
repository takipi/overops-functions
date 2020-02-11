package com.takipi.udf.alerts.slack;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.takipi.common.util.StringUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.slack.client.SlackClient;
import com.takipi.udf.alerts.slack.sender.SlackAnomalySender;
import com.takipi.udf.alerts.slack.sender.SlackNewEventSender;
import com.takipi.udf.alerts.slack.sender.SlackResurfacedEventSender;
import com.takipi.udf.alerts.slack.sender.SlackSender;
import com.takipi.udf.alerts.slack.sender.SlackThresholdSender;
import com.takipi.udf.input.Input;

public class SlackFunction {
	public static String validateInput(String rawInput) {
		SlackInput input = getSlackInput(rawInput);
		SlackClient client = input.client();

		if (!client.testConnection()) {
			throw new IllegalArgumentException("Failed connection test");
		}

		return "Slack Channel(" + input.inhook_url + ")";
	}

	static SlackInput getSlackInput(String rawInput) {
		System.out.println("validateInput rawInput:" + rawInput);

		if (StringUtil.isNullOrEmpty(rawInput)) {
			throw new IllegalArgumentException("Input is empty");
		}

		SlackInput input;

		try {
			input = SlackInput.of(rawInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		if (StringUtils.isEmpty(input.inhook_url)) {
			throw new IllegalArgumentException("Inhook URL can't be empty");
		}

		return input;
	}

	public static void execute(String rawContextArgs, String rawInput) {
		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		System.out.println("execute context: " + rawContextArgs);

		if (!args.validate()) {
			throw new IllegalArgumentException("Bad context args - " + rawContextArgs);
		}

		SlackInput input = getSlackInput(rawInput);

		SlackSender sender = null;

		switch (args.executionContext) {
		case NEW_EVENT:
			sender = SlackNewEventSender.create(input, args);
			break;
		case RESURFACED_EVENT:
			sender = SlackResurfacedEventSender.create(input, args);
			break;
		case THRESHOLD:
			sender = SlackThresholdSender.create(input, args);
			break;
		case ANOMALY:
			sender = SlackAnomalySender.create(input, args);
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
				throw new IllegalStateException(
						"Failed sending " + args.executionContext.toString() + "slack message for " + args.serviceId);
			}
		} catch (Exception e) {
			throw new IllegalStateException(
					"Failed sending " + args.executionContext.toString() + "slack message for " + args.serviceId, e);
		}
	}

	public static class SlackInput extends Input {
		public String inhook_url;
		public String inhook_channel;
		public String proxy_uri;
		public String proxy_username;
		public String proxy_password;

		private SlackInput(String raw) {
			super(raw);
		}

		public SlackClient client() {
			return SlackClient.newBuilder().setInhookUrl(inhook_url).setProxyUri(proxy_uri)
					.setProxyUsername(proxy_username).setProxyPassword(proxy_password).build();
		}

		static SlackInput of(String raw) {
			return new SlackInput(raw);
		}
	}
}
