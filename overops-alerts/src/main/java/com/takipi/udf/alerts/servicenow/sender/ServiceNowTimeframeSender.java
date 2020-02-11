package com.takipi.udf.alerts.servicenow.sender;

import java.util.Date;

import com.takipi.api.client.ApiClient;
import com.takipi.api.client.request.event.EventRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.CollectionUtil;
import com.takipi.common.util.StringUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.ContextArgs.Contributor;
import com.takipi.udf.alerts.servicenow.ServiceNowConsts;
import com.takipi.udf.alerts.servicenow.ServiceNowFunction.ServiceNowInput;
import com.takipi.udf.alerts.servicenow.message.Message;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.alerts.util.HTMLUtil;
import com.takipi.udf.util.DateUtil;
import com.takipi.udf.util.url.UrlUtil;

public abstract class ServiceNowTimeframeSender extends ServiceNowSender {
	private final ApiClient apiClient;

	protected final String addedByUser;
	protected final String viewName;
	protected final long fromTimestamp;
	protected final long toTimestamp;

	protected ServiceNowTimeframeSender(ServiceNowInput input, ContextArgs contextArgs, String addedByUser,
			String viewName, long fromTimestamp, long toTimestamp) {
		super(input, contextArgs);

		this.apiClient = contextArgs.apiClient();

		this.addedByUser = addedByUser;
		this.viewName = viewName;
		this.fromTimestamp = fromTimestamp;
		this.toTimestamp = toTimestamp;
	}

	@Override
	protected Message buildMessage() {
		return Message.newBuilder().setShortDescription(getShortDescription())
				.setComments("[code]" + HTMLUtil.minimizeHtml(getHtmlMessageBody()) + "[/code]").build();
	}

	private String getHtmlMessageBody() {
		StringBuilder bodyBuilder = new StringBuilder();

		String viewErrorsLink = AlertUtil.buildLinkForView(contextArgs.appHost, contextArgs.serviceId, viewName,
				fromTimestamp, toTimestamp);

		bodyBuilder.append(createHtmlAlertAddedBy(addedByUser));

		bodyBuilder.append(
				HTMLUtil.htmlParagraph(HTMLUtil.htmlLink(HTMLUtil.htmlBold("View errors"), viewErrorsLink, true)));

		bodyBuilder.append(HTMLUtil.htmlParagraph(HTMLUtil.htmlTable(htmlTableContent())));

		bodyBuilder.append(HTMLUtil.htmlParagraph(createHtmlTopContributorsDetails()));

		bodyBuilder
				.append(HTMLUtil.htmlParagraph(HTMLUtil.htmlLink(HTMLUtil.htmlImage(ServiceNowConsts.OVEROPS_LOGO_URL),
						UrlUtil.getDashboardUrl(contextArgs.appHost), true)));

		return bodyBuilder.toString();
	}

	protected String htmlTableContent() {
		Date fromDate = DateUtil.fromMillis(fromTimestamp);
		Date toDate = DateUtil.fromMillis(toTimestamp);

		String timeframe = DateUtil.toTimeStringAmPmNoSeconds(fromDate) + " - "
				+ DateUtil.toTimeStringAmPmNoSeconds(toDate) + " (GMT)";

		StringBuilder sb = new StringBuilder();

		sb.append(nonEmptyHtmlTableRow("View", viewName));
		sb.append(nonEmptyHtmlTableRow("Between", timeframe));

		return sb.toString();
	}

	private String createHtmlAlertAddedBy(String username) {
		return HTMLUtil.htmlParagraph("(alert added by " + HTMLUtil.htmlBold(username) + ")");
	}

	private String createHtmlTopContributorsDetails() {
		StringBuilder builder = new StringBuilder();

		if (!CollectionUtil.safeIsEmpty(contextArgs.contributors)) {
			for (Contributor contributor : contextArgs.contributors) {
				String desc = buildContributorDescription(contributor);

				if (StringUtil.isNullOrEmpty(desc)) {
					continue;
				}

				builder.append(desc);
				builder.append(HTMLUtil.htmlLineBreak());
			}
		}

		return builder.toString();
	}

	private String buildContributorDescription(Contributor contributor) {
		EventRequest eventRequest = EventRequest.newBuilder().setServiceId(contextArgs.serviceId)
				.setEventId(contributor.id).setIncludeStacktrace(true).build();

		Response<EventResult> eventResult = apiClient.get(eventRequest);

		if ((eventResult.isBadResponse()) || (eventResult.data == null)) {
			logger.error("Can't get event {}/{}.", contextArgs.serviceId, contributor.id);
			return null;
		}

		EventResult event = eventResult.data;
		StringBuilder sb = new StringBuilder();

		String fullRequestHeader = AlertUtil.createEventTitle(event, 0);
		String eventName = AlertUtil.formatEventData(fullRequestHeader, 40);

		String url = UrlUtil.buildTaleUrl(contextArgs.appHost, contextArgs.serviceId, contributor.id,
				contributor.lastSnapshotId, getTaleSource());

		sb.append(HTMLUtil.htmlLink(HTMLUtil.htmlBold(eventName), url, true));

		String eventLocation = ((event.error_location != null) ? event.error_location.prettified_name : null);

		if (!StringUtil.isNullOrEmpty(eventLocation)) {
			sb.append(" at " + eventLocation);
		}

		String hitsCount = AlertUtil.formatCountable(contributor.hits, "time");
		sb.append(" | " + HTMLUtil.htmlBold(hitsCount));

		return sb.toString();
	}

	protected abstract String getShortDescription();

	protected abstract int getTaleSource();
}
