package com.takipi.udf.alerts.pagerduty.client;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.takipi.common.util.StringUtil;
import com.takipi.udf.alerts.pagerduty.PagerDutyConsts;
import com.takipi.udf.alerts.pagerduty.message.PagerDutyMessage;

public class PagerDutyClient {
	private static final Logger logger = LoggerFactory.getLogger(PagerDutyClient.class);

	private static final String PAGER_DUTY_API_URL = "https://events.pagerduty.com/generic/2010-04-15/create_event.json";

	private static final String RESPONSE_MESSAGE_NAME = "message";
	private static final String RESPONSE_STATUS_SUCCESS = "success";

	private static final String TEST_INCIDENT_TYPE = PagerDutyConsts.TRIGGER_EVENT_TYPE;
	private static final String TEST_INCIDENT_DESC = "OverOps test incident";
	private static final String TEST_INCIDENT_ID = "1";

	private final WebTarget apiTarget;
	private final String serviceIntegrationKey;

	PagerDutyClient(WebTarget apiTarget, String serviceIntegrationKey) {
		this.apiTarget = apiTarget;
		this.serviceIntegrationKey = serviceIntegrationKey;
	}

	public boolean testConnection() {
		try {
			return postMessage(getConnectionTestMessage()).success;
		} catch (Exception e) {
			logger.warn("PagerDuty connectivity test fail", e);
			return false;
		}
	}

	private PagerDutyMessage getConnectionTestMessage() {
		return PagerDutyMessage.newBuilder().setServiceKey(serviceIntegrationKey).setEventType(TEST_INCIDENT_TYPE)
				.setDescription(TEST_INCIDENT_DESC).setIncidentKey(TEST_INCIDENT_ID).build();
	}

	public PagerDutyResponse postMessage(PagerDutyMessage message) {
		String dataJson = message.toJson();
		Entity<String> dataEntity = Entity.entity(dataJson, MediaType.APPLICATION_JSON_TYPE);

		String responseStr = apiTarget.request(MediaType.APPLICATION_JSON_TYPE).post(dataEntity, String.class);

		return toResponse(responseStr);
	}

	private PagerDutyResponse toResponse(String responseStr) {
		PagerDutyResponse response = PagerDutyConsts.GSON.fromJson(responseStr, PagerDutyResponse.class);

		if (response == null) {
			logger.error("Failed to parse PagerDuty json response for {}. Response: {}.", serviceIntegrationKey,
					responseStr);

			return PagerDutyResponse.newBuilder().setSuccess(false).build();
		}

		boolean isSuccess = response.status.equals(RESPONSE_STATUS_SUCCESS);

		PagerDutyResponse.Builder builder = response.toBuilder().setSuccess(isSuccess);

		try {
			JsonParser parser = new JsonParser();
			JsonObject jsonObject = parser.parse(responseStr).getAsJsonObject();
			String message = jsonObject.get(RESPONSE_MESSAGE_NAME).getAsString();

			builder.setResponseMessage(message);
		} catch (Exception ex) {
			logger.info("Failed to parse PagerDuty json response message for {}. Response: {}.", serviceIntegrationKey,
					responseStr, ex);
		}

		return builder.build();
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String serviceIntegrationKey;
		private String proxyUri;
		private String proxyUsername;
		private String proxyPassword;

		Builder() {

		}

		public Builder setServiceIntegrationKey(String val) {
			this.serviceIntegrationKey = val;

			return this;
		}

		public Builder setProxyUri(String val) {
			this.proxyUri = val;

			return this;
		}

		public Builder setProxyUsername(String val) {
			this.proxyUsername = val;

			return this;
		}

		public Builder setProxyPassword(String val) {
			this.proxyPassword = val;

			return this;
		}

		public PagerDutyClient build() {
			ClientConfig config = new ClientConfig();
			config.connectorProvider(new ApacheConnectorProvider());

			if (!StringUtil.isNullOrEmpty(proxyUri)) {
				config.property(ClientProperties.PROXY_URI, proxyUri);

				if (!StringUtil.isNullOrEmpty(proxyUsername)) {
					config.property(ClientProperties.PROXY_USERNAME, proxyUsername);
				}

				if (!StringUtil.isNullOrEmpty(proxyPassword)) {
					config.property(ClientProperties.PROXY_PASSWORD, proxyPassword);
				}
			}

			Client restClient = ClientBuilder.newClient(config);

			URI hostUri = UriBuilder.fromUri(PAGER_DUTY_API_URL).build().normalize();

			WebTarget inhookTarget = restClient.target(hostUri);

			return new PagerDutyClient(inhookTarget, serviceIntegrationKey);
		}
	}
}
