package com.takipi.udf.volume;

import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.event.Action;
import com.takipi.api.client.request.alert.Anomaly;
import com.takipi.api.client.request.alert.AnomalyAlertRequest;
import com.takipi.api.client.request.event.BatchForceSnapshotsRequest;
import com.takipi.api.client.request.event.EventActionsRequest;
import com.takipi.api.client.request.label.BatchModifyLabelsRequest;
import com.takipi.api.client.request.label.CreateLabelRequest;
import com.takipi.api.client.result.EmptyResult;
import com.takipi.api.client.result.GenericResult;
import com.takipi.api.client.result.event.EventActionsResult;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.CollectionUtil;
import com.takipi.udf.input.TimeInterval;

public class AnomalyUtil {
	private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZoneUTC();

	private static final int MAX_ANOMALY_CONTRIBUTORS = 10;

	private static final String LABEL_ADD = "ADD_LABEL";
	private static final String LABEL_TYPE = "LABEL";

	public static List<EventResult> getContributors(Collection<EventResult> events, ApiClient apiClient,
			String serviceId, TimeInterval interval, String label) {

		if (CollectionUtil.safeIsEmpty(events)) {
			return Collections.emptyList();
		}

		List<EventResult> result = Lists.newArrayListWithCapacity(MAX_ANOMALY_CONTRIBUTORS);
		boolean checkAlertAllowed = (interval.isPositive()) && (!Strings.isNullOrEmpty(label));

		int count = 0;

		for (EventResult event : events) {

			if (count >= MAX_ANOMALY_CONTRIBUTORS) {
				break;
			}

			count++;

			if (ThresholdUtil.getEventHits(event) == 0) {
				continue;
			}

			if ((checkAlertAllowed) && (!isAlertAllowed(apiClient, serviceId, event, label, interval))) {
				continue;
			}

			result.add(event);
		}

		return result;
	}

	private static boolean isAlertAllowed(ApiClient apiClient, String serviceId, EventResult event, String label,
			TimeInterval interval) {

		EventActionsRequest request = EventActionsRequest.newBuilder().setServiceId(serviceId).setEventId(event.id)
				.build();

		Response<EventActionsResult> response = apiClient.get(request);

		if (response.isBadResponse()) {
			// in case of API failure we should not prevent an alert
			System.err.println("Could not get event actions for " + event + " code: " + response.responseCode);
			return true;
		}

		if ((response.data == null) || (CollectionUtil.safeIsEmpty(response.data.event_actions))) {
			return true;
		}

		for (Action action : response.data.event_actions) {

			if (!(LABEL_ADD.equals(action.action)) || (!LABEL_TYPE.equals(action.type))
					|| (!label.equals(action.data))) {
				continue;
			}

			DateTime actionTime = fmt.parseDateTime(action.timestamp);

			long delta = DateTime.now().minus(actionTime.getMillis()).getMillis();
			long actionMinutesInterval = TimeUnit.MILLISECONDS.toMinutes(delta);

			if (actionMinutesInterval < interval.asMinutes()) {
				return false;
			}
		}

		return true;
	}

	public static void reportAnomaly(ApiClient apiClient, String serviceId, String viewId,
			Collection<EventResult> contributors, String anomalyLabel, DateTime from, DateTime to,
			String anomalyMessage) {

		System.out.println(
				"Alerting on " + contributors.size() + " anomalies: " + StringUtils.join(contributors.toArray(), ','));

		applyAnomalyLabel(apiClient, serviceId, anomalyLabel, contributors);

		resetContributorSnapshots(apiClient, serviceId, contributors);

		send(apiClient, serviceId, viewId, contributors, from, to, anomalyMessage);
	}

	private static void applyAnomalyLabel(ApiClient apiClient, String serviceId, String label,
			Collection<EventResult> contributors) {

		if ((Strings.isNullOrEmpty(label)) || (CollectionUtil.safeIsEmpty(contributors))) {
			return;
		}

		CreateLabelRequest createLabel = CreateLabelRequest.newBuilder().setServiceId(serviceId).setName(label).build();

		Response<EmptyResult> createResult = apiClient.post(createLabel);

		if ((createResult.isBadResponse()) && (createResult.responseCode != HttpURLConnection.HTTP_CONFLICT)) {
			throw new IllegalStateException("Cannot create label " + label);
		}

		BatchModifyLabelsRequest.Builder builder = BatchModifyLabelsRequest.newBuilder().setServiceId(serviceId)
				.setForceHistory(true);

		for (EventResult contributor : contributors) {
			builder.addLabelModifications(contributor.id, Collections.singleton(label), Collections.emptyList());
		}

		Response<EmptyResult> addResult = apiClient.post(builder.build());

		if (addResult.isBadResponse()) {
			throw new IllegalStateException("Can't apply label " + label + " to contributors");
		}
	}

	private static void resetContributorSnapshots(ApiClient apiClient, String serviceId,
			Collection<EventResult> contributors) {

		if (CollectionUtil.safeIsEmpty(contributors)) {
			return;
		}

		BatchForceSnapshotsRequest.Builder builder = BatchForceSnapshotsRequest.newBuilder().setServiceId(serviceId);

		for (EventResult contributor : contributors) {
			builder.addEventId(contributor.id);
		}

		Response<EmptyResult> reponse = apiClient.post(builder.build());

		if (reponse.isBadResponse()) {
			System.err.println("Cannot reset snapshots, code: " + reponse.responseCode);
		}
	}

	private static void send(ApiClient apiClient, String serviceId, String viewId, Collection<EventResult> events,
			DateTime from, DateTime to, String desc) {
		// Send anomaly message to integrations

		Anomaly anomaly = Anomaly.create();

		anomaly.addAnomalyPeriod(viewId, from.getMillis(), to.getMillis());

		for (EventResult event : events) {
			if ((event.stats != null) && (event.stats.hits > 0)) {
				anomaly.addContributor(Integer.parseInt(event.id), event.stats.hits);
			}
		}

		AnomalyAlertRequest anomalyAlertRequest = AnomalyAlertRequest.newBuilder().setServiceId(serviceId)
				.setViewId(viewId).setFrom(from.toString()).setTo(to.toString()).setDesc(desc).setAnomaly(anomaly)
				.build();

		Response<GenericResult> anomalyAlertResponse = apiClient.post(anomalyAlertRequest);

		if (anomalyAlertResponse.isBadResponse()) {
			throw new IllegalStateException("Failed alerting on anomaly for view - " + viewId);
		}

		GenericResult alertResult = anomalyAlertResponse.data;

		if (alertResult == null) {
			throw new IllegalStateException("Failed getting anomaly alert result on view - " + viewId);
		}

		if (!alertResult.result) {
			throw new IllegalStateException(
					"Anomaly alert on view - " + viewId + " failed with - " + alertResult.message);
		}
	}
}
