package com.takipi.udf.alerts.servicenow.client;

import java.net.URI;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.takipi.common.util.Pair;
import com.takipi.udf.alerts.servicenow.ServiceNowConsts;
import com.takipi.udf.alerts.servicenow.message.Message;
import com.takipi.udf.util.url.UrlUtil;

public class ServiceNowClient {
	protected static final Logger logger = LoggerFactory.getLogger(ServiceNowClient.class);

	private final WebTarget webTarget;

	ServiceNowClient(WebTarget webTarget) {
		this.webTarget = webTarget;
	}

	public ServiceNowResponse testConnection() {
		try {
			Response rawResponse = webTarget.request(MediaType.APPLICATION_JSON_TYPE).get();

			return toResponse(rawResponse);
		} catch (Exception e) {
			logger.error("Unable to GET from {}", webTarget.getUri());

			return null;
		}
	}

	public ServiceNowResponse postMessage(Message message) {
		try {
			Entity<String> dataEntity = Entity.entity(message.toJson(), MediaType.APPLICATION_JSON_TYPE);

			Response rawResponse = webTarget.request(MediaType.APPLICATION_JSON_TYPE).post(dataEntity);

			return toResponse(rawResponse);
		} catch (Exception e) {
			logger.error("Unable to POST to {}", webTarget.getUri(), e);

			return null;
		}
	}

	private ServiceNowResponse toResponse(Response rawResponse) {
		boolean success = rawResponse.getStatusInfo().getFamily().equals(Status.Family.SUCCESSFUL);

		int statusCode = rawResponse.getStatusInfo().getStatusCode();

		String responseJson;

		try {
			responseJson = rawResponse.readEntity(String.class);
		} catch (Exception e) {
			logger.warn("Error reading response json from {}, success={}, statusCode={} ", webTarget.getUri(), success,
					statusCode, e);

			success = false;
			responseJson = "{}";
		}

		return ServiceNowResponse.newBuilder().setSuccess(success).setStatusCode(statusCode).setRawJson(responseJson)
				.build();
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String baseAddress;
		private String apiVersion;
		private String table;
		private List<Pair<String, String>> queryFieldValuePairs;
		private String userId;
		private String password;

		public Builder() {
			queryFieldValuePairs = Lists.newArrayList();
		}

		public Builder setBaseAddress(String baseAddress) {
			this.baseAddress = baseAddress;

			return this;
		}

		public Builder setApiVersion(String apiVersion) {
			this.apiVersion = apiVersion;

			return this;
		}

		public Builder setTable(String table) {
			this.table = table;

			return this;
		}

		public Builder addQueryField(String field, String value) {
			if ((!Strings.isNullOrEmpty(field)) && (!Strings.isNullOrEmpty(value))) {
				queryFieldValuePairs.add(Pair.of(field, value));
			}

			return this;
		}

		public Builder setCredentials(String userId, String password) {
			this.userId = userId;
			this.password = password;

			return this;
		}

		public ServiceNowClient build() {
			if ((Strings.isNullOrEmpty(baseAddress)) || (Strings.isNullOrEmpty(table))) {
				logger.warn("Missing baseAddress or table name, can't build ServiceNowRestClient");

				return null;
			}

			baseAddress = UrlUtil.getCanonicalHostname(baseAddress, false);

			if (Strings.isNullOrEmpty(apiVersion)) {
				apiVersion = ServiceNowConsts.API_VERSION;
			}

			String path = String.format(ServiceNowConsts.API_PATH_TEMPLATE, apiVersion, table);

			StringBuilder querySb = new StringBuilder();

			for (int i = 0; i < queryFieldValuePairs.size(); i++) {
				Pair<String, String> pair = queryFieldValuePairs.get(i);

				if (i > 0) {
					querySb.append("&");
				}

				querySb.append(pair.getFirst());
				querySb.append("=");
				querySb.append(pair.getSecond());
			}

			StringBuilder fullUrlSb = new StringBuilder().append(baseAddress).append(path);

			if (querySb.length() > 0) {
				fullUrlSb.append("?").append(querySb.toString());
			}

			URI hostUri = UriBuilder.fromUri(fullUrlSb.toString()).build().normalize();

			Client restClient = ClientBuilder.newClient();
			WebTarget webTarget = restClient.target(hostUri);

			if (!Strings.isNullOrEmpty(userId)) {
				if (Strings.isNullOrEmpty(password)) {
					webTarget.register(HttpAuthenticationFeature.basic(userId, ""));
				} else {
					webTarget.register(HttpAuthenticationFeature.basic(userId, password));
				}
			}

			return new ServiceNowClient(webTarget);
		}
	}
}
