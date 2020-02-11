package com.takipi.udf.alerts.pagerduty.sender;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.takipi.udf.alerts.pagerduty.PagerDutyConsts;
import com.takipi.udf.alerts.pagerduty.PagerDutyFunction.PagerDutyInput;
import com.takipi.udf.alerts.pagerduty.client.PagerDutyClient;
import com.takipi.udf.alerts.pagerduty.client.PagerDutyResponse;
import com.takipi.udf.alerts.pagerduty.message.PagerDutyMessage;

public abstract class PagerDutySender {
	protected static final Logger logger = LoggerFactory.getLogger(PagerDutySender.class);

	protected final PagerDutyInput input;

	protected PagerDutySender(PagerDutyInput input) {
		this.input = input;
	}

	public boolean sendMessage() {
		String internalDescription = getInternalDescription();

		logger.info("About to send a PagerDuty message ({}) to {}.", internalDescription,
				input.service_integration_key);

		try {
			PagerDutyResponse response = postMessage();

			if (!response.success) {
				logger.error("PagerDuty message for {} returned an error: \"{}\" (key: {}).", internalDescription,
						response.responseMessage, input.service_integration_key);

				return false;
			}

			logger.info("PagerDuty message for {} successfully sent (key: {}).", internalDescription,
					input.service_integration_key);

			return true;
		} catch (Exception e) {
			logger.error("Unagle to send PagerDuty message for {} successfully sent (key: {}).", internalDescription,
					input.service_integration_key, e);

			return false;
		}
	}

	private PagerDutyResponse postMessage() {
		try {
			PagerDutyClient client = input.client();

			PagerDutyMessage message = createMessage(createBaseMessage());
			PagerDutyResponse response = client.postMessage(message);

			return response;
		} catch (Exception e) {
			logger.error("Error posting message to {}.", input.service_integration_key, e);

			return PagerDutyResponse.newBuilder().setSuccess(false).build();
		}
	}

	protected PagerDutyMessage.Builder createBaseMessage() {
		PagerDutyMessage.Builder builder = PagerDutyMessage.newBuilder().setServiceKey(input.service_integration_key)
				.setEventType(PagerDutyConsts.TRIGGER_EVENT_TYPE).setClient(PagerDutyConsts.EVENT_CLIENT);

		return builder;
	}

	protected String incidentKey(Object... objects) {
		int hashValue = Arrays.hashCode(objects);

		return String.valueOf(hashValue);
	}

	protected abstract String getInternalDescription();

	protected abstract PagerDutyMessage createMessage(PagerDutyMessage.Builder builder);
}
