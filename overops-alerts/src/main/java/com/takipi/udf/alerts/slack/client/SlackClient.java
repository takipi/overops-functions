package com.takipi.udf.alerts.slack.client;

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

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.takipi.udf.alerts.slack.SlackConsts;
import com.takipi.udf.alerts.slack.message.Attachment;
import com.takipi.udf.alerts.slack.message.Message;
import com.takipi.udf.alerts.slack.message.NamingStrategy;

public class SlackClient {
	private static final Logger logger = LoggerFactory.getLogger(SlackClient.class);

	private static final Gson GSON = new GsonBuilder().setFieldNamingStrategy(NamingStrategy.instance).create();

	private static final String OK_RESPONSE_STRING = "ok";

	private static final String CONN_TEST_MESSAGE_TEXT = "This message was sent by OverOps to verify your Slack integration. "
			+ "The integration has been successfully added. There's no need for any further action.";

	private final WebTarget inhookTarget;

	SlackClient(WebTarget inhookTarget) {
		this.inhookTarget = inhookTarget;
	}

	public boolean testConnection() {
		try {
			return postMessage(getConnectionTestMessage()).ok;
		} catch (Exception e) {
			logger.warn("Slack connectivity test fail", e);
			return false;
		}
	}

	private static Message getConnectionTestMessage() {
		return Message.newBuilder().setUsername(SlackConsts.USERNAME).setIconUrl(SlackConsts.ICON_URL)
				.addAttachment(Attachment.newBuilder().setFallback(CONN_TEST_MESSAGE_TEXT)
						.setText(CONN_TEST_MESSAGE_TEXT).setColor("#01BF15").build())
				.build();
	}

	public SlackResponse postMessage(Message message) {
		String dataJson = GSON.toJson(message);
		Entity<String> dataEntity = Entity.entity(dataJson, MediaType.APPLICATION_JSON_TYPE);

		String responseStr = inhookTarget.request(MediaType.APPLICATION_JSON_TYPE).post(dataEntity, String.class);

		return toResponse(responseStr);
	}

	private SlackResponse toResponse(String responseStr) {
		if (OK_RESPONSE_STRING.equals(responseStr)) {
			return SlackResponse.OK;
		}

		return SlackResponse.of(false, responseStr);
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String inhookUrl;
		private String proxyUri;
		private String proxyUsername;
		private String proxyPassword;

		Builder() {

		}

		public Builder setInhookUrl(String val) {
			this.inhookUrl = val;

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

		public SlackClient build() {
			ClientConfig config = new ClientConfig();
			config.connectorProvider(new ApacheConnectorProvider());

			if (!Strings.isNullOrEmpty(proxyUri)) {
				config.property(ClientProperties.PROXY_URI, proxyUri);

				if (!Strings.isNullOrEmpty(proxyUsername)) {
					config.property(ClientProperties.PROXY_USERNAME, proxyUsername);
				}

				if (!Strings.isNullOrEmpty(proxyPassword)) {
					config.property(ClientProperties.PROXY_PASSWORD, proxyPassword);
				}
			}

			Client restClient = ClientBuilder.newClient(config);

			URI hostUri = UriBuilder.fromUri(inhookUrl).build().normalize();

			WebTarget inhookTarget = restClient.target(hostUri);

			return new SlackClient(inhookTarget);
		}
	}
}
