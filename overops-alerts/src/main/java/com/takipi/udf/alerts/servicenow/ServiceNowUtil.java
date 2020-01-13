package com.takipi.udf.alerts.servicenow;

import com.takipi.udf.alerts.servicenow.client.ServiceNowClient;
import com.takipi.udf.alerts.servicenow.client.ServiceNowResponse;

public class ServiceNowUtil {
	public static boolean testConnection(String url, String userId, String password, String table) {
		ServiceNowClient client = ServiceNowClient.newBuilder().setBaseAddress(url).setTable(table)
				.setCredentials(userId, password).addQueryField(ServiceNowConsts.LIMIT_RESULTS_PARAM, "1").build();

		ServiceNowResponse response = client.testConnection();

		if (response == null) {
			return false;
		}

		return response.success;
	}
}
