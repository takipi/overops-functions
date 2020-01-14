package com.takipi.udf.alerts.pagerduty.sender;

import java.util.Date;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.request.event.EventRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.CollectionUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.ContextArgs.Contributor;
import com.takipi.udf.alerts.pagerduty.PagerDutyFunction.PagerDutyInput;
import com.takipi.udf.alerts.pagerduty.message.Details;
import com.takipi.udf.alerts.pagerduty.message.PagerDutyMessage;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.util.DateUtil;
import com.takipi.udf.util.url.UrlUtil;

public abstract class PagerDutyTimeframeSender extends PagerDutySender {
	private static final int MAX_CONTRIBUTER_LENGTH = 40;

	protected final ContextArgs contextArgs;
	private final ApiClient apiClient;

	protected final String addedByUser;
	protected final String viewName;
	protected final long fromTimestamp;
	protected final long toTimestamp;

	protected PagerDutyTimeframeSender(PagerDutyInput input, ContextArgs contextArgs, String addedByUser,
			String viewName, long fromTimestamp, long toTimestamp) {
		super(input);

		this.contextArgs = contextArgs;
		this.apiClient = contextArgs.apiClient();

		this.addedByUser = addedByUser;
		this.viewName = viewName;
		this.fromTimestamp = fromTimestamp;
		this.toTimestamp = toTimestamp;
	}

	@Override
	protected PagerDutyMessage createMessage(PagerDutyMessage.Builder builder) {
		String viewLink = AlertUtil.buildLinkForView(contextArgs.appHost, contextArgs.serviceId, viewName,
				fromTimestamp, toTimestamp);

		String settingsLink = AlertUtil.buildLinkForSettings(contextArgs.appHost, contextArgs.serviceId);

		builder.setDescription(createDescription()).setIncidentKey(createIncidentKey()).setClientUrl(viewLink)
				.setSettingsUrl(settingsLink).setDetails(getDetailsBuilder().build());

		return builder.build();
	}

	private String createIncidentKey() {
		int hashValue = Objects.hashCode(contextArgs.serviceId, viewName, fromTimestamp, toTimestamp);

		return String.valueOf(hashValue);
	}

	protected Details.Builder getDetailsBuilder() {
		Details.Builder detailsBuilder = Details.newBuilder().setPresetName(viewName)
				.setTimeframe(formatTimeframe(fromTimestamp, toTimestamp));

		String contributors = buildContributors();

		if (contributors != null) {
			detailsBuilder.setContributors(contributors);
		}

		return detailsBuilder;
	}

	protected String buildContributors() {
		if (CollectionUtil.safeIsEmpty(contextArgs.contributors)) {
			return null;
		}

		StringBuilder sb = new StringBuilder();

		for (Contributor contributor : contextArgs.contributors) {
			String desc = buildContributor(contributor);

			if (Strings.isNullOrEmpty(desc)) {
				continue;
			}

			sb.append(contributor);
			sb.append('\n');
		}

		return sb.toString();
	}

	private String buildContributor(Contributor contributor) {
		EventRequest eventRequest = EventRequest.newBuilder().setServiceId(contextArgs.serviceId)
				.setEventId(contributor.id).setIncludeStacktrace(true).build();

		Response<EventResult> eventResult = apiClient.get(eventRequest);

		if ((eventResult.isBadResponse()) || (eventResult.data == null)) {
			logger.error("Can't get event {}/{}.", contextArgs.serviceId, contributor.id);
			return null;
		}

		EventResult event = eventResult.data;

		String fullRequestHeader = AlertUtil.createEventTitle(event, 0);
		String eventName = AlertUtil.formatEventData(fullRequestHeader, MAX_CONTRIBUTER_LENGTH);

		StringBuilder sb = new StringBuilder();
		sb.append("- " + eventName);

		String url = UrlUtil.buildTaleUrl(contextArgs.appHost, contextArgs.serviceId, contributor.id,
				contributor.lastSnapshotId, getTaleSource());

		sb.append(" | URL: " + url);

		String eventLocation = ((event.error_location != null) ? event.error_location.prettified_name : null);

		if (eventLocation != null) {
			sb.append(" | at " + eventLocation);
		}

		sb.append(" | " + contributor.hits + " times");

		return sb.toString();
	}

	private static String formatTimeframe(long from, long to) {
		Date fromDate = DateUtil.fromMillis(from);
		Date toDate = DateUtil.fromMillis(to);

		return DateUtil.toTimeString(fromDate) + " - " + DateUtil.toTimeString(toDate) + " (GMT)";
	}

	protected abstract String createDescription();

	protected abstract int getTaleSource();
}
