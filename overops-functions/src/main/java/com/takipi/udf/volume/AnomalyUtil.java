package com.takipi.udf.volume;

import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.event.Action;
import com.takipi.api.client.request.event.BatchForceSnapshotsRequest;
import com.takipi.api.client.request.event.EventActionsRequest;
import com.takipi.api.client.request.label.BatchModifyLabelsRequest;
import com.takipi.api.client.request.label.CreateLabelRequest;
import com.takipi.api.client.result.EmptyResult;
import com.takipi.api.client.result.event.EventActionsResult;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.util.alert.AlertUtil;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.CollectionUtil;
import com.takipi.udf.input.TimeInterval;

public class AnomalyUtil {
	private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZoneUTC();

	private static final String LABEL_ADD = "ADD_LABEL";
	private static final String LABEL_TYPE = "LABEL";

	public static final int MAX_ANOMALY_CONTRIBUTORS = 10;

	public static void removeAnomalyLabel(Collection<EventResult> events, ApiClient apiClient, String serviceId,
			TimeInterval maxInterval, String label) {

		if ((CollectionUtil.safeIsEmpty(events)) || (!maxInterval.isPositive()) || (Strings.isNullOrEmpty(label))) {
			return;
		}

		DateTime now = DateTime.now();
		boolean labelsUpdateNeeded = false;
		BatchModifyLabelsRequest.Builder labelsRequest = BatchModifyLabelsRequest.newBuilder().setServiceId(serviceId);

		for (EventResult event : events) {
			if (!CollectionUtil.safeContains(event.labels, label)) {
				continue;
			}

			DateTime lastestLabeling = getLatestLabelingTime(apiClient, serviceId, event, label);

			if (lastestLabeling == null) {
				continue;
			}

			if (lastestLabeling.plusMinutes(maxInterval.asMinutes()).isAfter(now)) {
				continue;
			}

			labelsUpdateNeeded = true;

			labelsRequest.addLabelModifications(event.id, Collections.emptyList(), Collections.singletonList(label));

			// By removing the label from the event we save up on redundant querying when
			// later checking on anomalies.
			//
			event.labels.remove(label);
		}

		if (labelsUpdateNeeded) {
			Response<EmptyResult> response = apiClient.post(labelsRequest.build());

			if (response.isBadResponse()) {
				System.err.println("Could not remove label from events. Code: " + response.responseCode);
			}
		}
	}

	public static List<EventResult> filterAnomalyEvents(Collection<EventResult> events, ApiClient apiClient,
			String serviceId, TimeInterval minInterval, String label, int maxEvents) {

		if (CollectionUtil.safeIsEmpty(events)) {
			return Collections.emptyList();
		}

		List<EventResult> result = Lists.newArrayList();

		DateTime now = DateTime.now();

		boolean labelFilteringNeeded = ((!Strings.isNullOrEmpty(label)) && (minInterval.isPositive()));

		for (EventResult event : events) {

			if ((maxEvents > 0) && (result.size() >= maxEvents)) {
				continue;
			}

			if (ThresholdUtil.getEventHits(event) == 0) {
				continue;
			}

			if ((labelFilteringNeeded) && (CollectionUtil.safeContains(event.labels, label))) {

				DateTime lastestLabeling = getLatestLabelingTime(apiClient, serviceId, event, label);

				if ((lastestLabeling != null) && (lastestLabeling.plusMinutes(minInterval.asMinutes()).isAfter(now))) {
					continue;
				}
			}

			result.add(event);
		}

		return result;
	}

	private static DateTime getLatestLabelingTime(ApiClient apiClient, String serviceId, EventResult event,
			String label) {

		EventActionsRequest request = EventActionsRequest.newBuilder().setServiceId(serviceId).setEventId(event.id)
				.build();

		Response<EventActionsResult> response = apiClient.get(request);

		if (response.isBadResponse()) {
			System.err.println("Could not get event actions for " + event + " code: " + response.responseCode);

			return null;
		}

		if ((response.data == null) || (CollectionUtil.safeIsEmpty(response.data.event_actions))) {
			return null;
		}

		DateTime result = null;

		for (Action action : response.data.event_actions) {

			if (!(LABEL_ADD.equals(action.action)) || (!LABEL_TYPE.equals(action.type))
					|| (!label.equals(action.data))) {
				continue;
			}

			DateTime actionTime = fmt.parseDateTime(action.timestamp);

			if ((result == null) || (actionTime.isAfter(result))) {
				result = actionTime;
			}
		}

		return result;
	}

	public static void reportAnomaly(ApiClient apiClient, String serviceId, String viewId,
			Collection<EventResult> contributors, String anomalyLabel, DateTime from, DateTime to,
			String anomalyMessage) {

		System.out.println(
				"Alerting on " + contributors.size() + " anomalies: " + StringUtils.join(contributors.toArray(), ','));

		applyAnomalyLabel(apiClient, serviceId, anomalyLabel, contributors);

		resetContributorSnapshots(apiClient, serviceId, contributors);

		AlertUtil.reportAnomaly(apiClient, serviceId, viewId, contributors, from, to, anomalyMessage);
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
}
