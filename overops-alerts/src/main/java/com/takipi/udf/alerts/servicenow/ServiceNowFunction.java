package com.takipi.udf.alerts.servicenow;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.takipi.common.util.StringUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.servicenow.client.ServiceNowClient;
import com.takipi.udf.alerts.servicenow.sender.ServiceNowAnomalySender;
import com.takipi.udf.alerts.servicenow.sender.ServiceNowNewEventSender;
import com.takipi.udf.alerts.servicenow.sender.ServiceNowResurfacedEventSender;
import com.takipi.udf.alerts.servicenow.sender.ServiceNowSender;
import com.takipi.udf.alerts.servicenow.sender.ServiceNowThresholdSender;
import com.takipi.udf.input.Input;

public class ServiceNowFunction {
	public static String validateInput(String rawInput) {
		ServiceNowInput input = getServiceNowInput(rawInput);

		if (!ServiceNowUtil.testConnection(input.url, input.user_id, input.password, input.table)) {
			throw new IllegalArgumentException("Failed connection test");
		}

		return "ServiceNow Channel(" + input.url + ")";
	}

	static ServiceNowInput getServiceNowInput(String rawInput) {
		System.out.println("validateInput rawInput:" + rawInput);

		if (StringUtil.isNullOrEmpty(rawInput)) {
			throw new IllegalArgumentException("Input is empty");
		}

		ServiceNowInput input;

		try {
			input = ServiceNowInput.of(rawInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		if (StringUtils.isEmpty(input.url)) {
			throw new IllegalArgumentException("url can't be empty");
		}

		if (StringUtils.isEmpty(input.user_id)) {
			throw new IllegalArgumentException("user_id can't be empty");
		}

		if (StringUtils.isEmpty(input.password)) {
			throw new IllegalArgumentException("password can't be empty");
		}

		if (StringUtils.isEmpty(input.table)) {
			throw new IllegalArgumentException("table can't be empty");
		}

		return input;
	}

	public static void execute(String rawContextArgs, String rawInput) {
		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		System.out.println("execute context: " + rawContextArgs);

		if (!args.validate()) {
			throw new IllegalArgumentException("Bad context args - " + rawContextArgs);
		}

		ServiceNowInput input = getServiceNowInput(rawInput);

		ServiceNowSender sender = null;

		switch (args.executionContext) {
		case NEW_EVENT:
			sender = ServiceNowNewEventSender.create(input, args);
			break;
		case RESURFACED_EVENT:
			sender = ServiceNowResurfacedEventSender.create(input, args);
			break;
		case THRESHOLD:
			sender = ServiceNowThresholdSender.create(input, args);
			break;
		case ANOMALY:
			sender = ServiceNowAnomalySender.create(input, args);
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
						+ "service now message for " + args.serviceId);
			}
		} catch (Exception e) {
			throw new IllegalStateException(
					"Failed sending " + args.executionContext.toString() + "service now message for " + args.serviceId,
					e);
		}
	}

	public static class ServiceNowInput extends Input {
		public String url;
		public String user_id;
		public String password;
		public String table;

		private ServiceNowInput(String raw) {
			super(raw);
		}

		public ServiceNowClient client() {
			return ServiceNowClient.newBuilder().setBaseAddress(url).setCredentials(user_id, password).setTable(table)
					.build();
		}

		static ServiceNowInput of(String raw) {
			return new ServiceNowInput(raw);
		}
	}
}
