package com.takipi.udf.alerts.slack.sender;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.request.event.EventRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.CollectionUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.ContextArgs.Contributor;
import com.takipi.udf.alerts.slack.SlackFunction.SlackInput;
import com.takipi.udf.alerts.slack.SlackUtil;
import com.takipi.udf.alerts.slack.message.Attachment;
import com.takipi.udf.alerts.slack.message.AttachmentField;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.util.DateUtil;
import com.takipi.udf.util.url.UrlUtil;

public abstract class SlackTimeframeSender extends SlackSender {
	private static final Logger logger = LoggerFactory.getLogger(SlackTimeframeSender.class);

	private static final String VIEW_ERRORS_LINK = "View errors";

	private static final String MESSAGE_COLOR = "#303940";
	private static final String[] REQUEST_ROW_COLORS = { "#c80000", "#c82323", "#c84646", "#c86969", "#c88c8c" };

	private static final int MAX_CONTRIBUTER_LENGTH = 40;

	protected final ContextArgs contextArgs;
	private final ApiClient apiClient;

	protected final String addedByUser;
	protected final String viewName;
	protected final long fromTimestamp;
	protected final long toTimestamp;

	protected SlackTimeframeSender(SlackInput input, ContextArgs contextArgs, String addedByUser, String viewName,
			long fromTimestamp, long toTimestamp) {
		super(input);

		this.contextArgs = contextArgs;
		this.apiClient = contextArgs.apiClient();

		this.addedByUser = addedByUser;
		this.viewName = viewName;
		this.fromTimestamp = fromTimestamp;
		this.toTimestamp = toTimestamp;
	}

	protected Collection<AttachmentField> createAttachmentFields() {
		Collection<AttachmentField> result = Lists.newArrayList();

		Date fromDate = DateUtil.fromMillis(fromTimestamp);
		Date toDate = DateUtil.fromMillis(toTimestamp);

		String timeframe = DateUtil.toTimeStringAmPmNoSeconds(fromDate) + " - "
				+ DateUtil.toTimeStringAmPmNoSeconds(toDate) + " (GMT)";

		result.add(createAttachmentField("View", viewName));
		result.add(createAttachmentField("Between", timeframe));

		return result;
	}

	@Override
	protected List<Attachment> createAttachments() {
		List<Attachment> attachments = Lists.newArrayList();

		String viewLink = AlertUtil.buildLinkForView(contextArgs.appHost, contextArgs.serviceId, viewName,
				fromTimestamp, toTimestamp);

		String settingsLink = AlertUtil.buildLinkForSettings(contextArgs.appHost, contextArgs.serviceId);

		Attachment.Builder builder = Attachment.newBuilder().addMrkdwn(MARKDOWN_IN_PRETEXT).addMrkdwn(MARKDOWN_IN_TEXT)
				.setColor(MESSAGE_COLOR).setFallback(createPlainMessageText()).setTitle(VIEW_ERRORS_LINK)
				.setTitleLink(viewLink).setText(SlackUtil.formatLink("Manage settings", settingsLink))
				.setThumbUrl(thumbUrl()).addAllFields(createAttachmentFields());

		attachments.add(builder.build());

		if (!CollectionUtil.safeIsEmpty(contextArgs.contributors)) {
			int colorIndex = 0;

			for (Contributor contributor : contextArgs.contributors) {
				String desc = buildContributorDescription(contributor);

				if (Strings.isNullOrEmpty(desc)) {
					continue;
				}

				Attachment.Builder contributorBuilder = Attachment.newBuilder();

				contributorBuilder.setColor(REQUEST_ROW_COLORS[colorIndex]).addMrkdwn(MARKDOWN_IN_TEXT).setText(desc);

				attachments.add(contributorBuilder.build());

				if (colorIndex < REQUEST_ROW_COLORS.length - 1) {
					colorIndex++;
				}
			}
		}

		return attachments;
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
		String eventName = AlertUtil.formatEventData(fullRequestHeader, MAX_CONTRIBUTER_LENGTH);

		String url = UrlUtil.buildTaleUrl(contextArgs.appHost, contextArgs.serviceId, contributor.id,
				contributor.lastSnapshotId, getTaleSource());

		sb.append("*" + SlackUtil.formatLink(eventName, url) + "*");

		String eventLocation = ((event.error_location != null) ? event.error_location.prettified_name : null);

		if (!Strings.isNullOrEmpty(eventLocation)) {
			sb.append(" at " + AlertUtil.formatEventData(eventLocation, MAX_CONTRIBUTER_LENGTH));
		}

		String hitsCount = AlertUtil.formatCountable(contributor.hits, "time");
		sb.append(" | *" + hitsCount + "*");

		return sb.toString();
	}

	protected String thumbUrl() {
		return "https://s3.amazonaws.com/www.takipi.com/email/v5/threshold-thumb.png";
	}

	protected abstract String createPlainMessageText();

	protected abstract int getTaleSource();
}
