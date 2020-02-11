package com.takipi.udf.alerts.slack.sender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.takipi.api.client.ApiClient;
import com.takipi.api.client.request.event.EventRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.CollectionUtil;
import com.takipi.common.util.StringUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.ContextArgs.Contributor;
import com.takipi.udf.alerts.slack.SlackFunction.SlackInput;
import com.takipi.udf.alerts.slack.SlackUtil;
import com.takipi.udf.alerts.slack.message.Attachment;
import com.takipi.udf.alerts.template.model.Body.Part;
import com.takipi.udf.alerts.template.model.Model;
import com.takipi.udf.alerts.template.token.Tokenizer;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.util.url.UrlUtil;

public abstract class SlackTimeframeSender extends SlackSender {
	private static final String VIEW_ERRORS_LINK = "View errors";

	private static final String MESSAGE_COLOR = "#303940";
	private static final String[] REQUEST_ROW_COLORS = { "#c80000", "#c82323", "#c84646", "#c86969", "#c88c8c" };

	private static final int MAX_CONTRIBUTER_LENGTH = 40;

	private final ApiClient apiClient;
	protected final String viewName;
	protected final long fromTimestamp;
	protected final long toTimestamp;

	protected SlackTimeframeSender(SlackInput input, ContextArgs contextArgs, Model model, Tokenizer tokenizer,
			String viewName, long fromTimestamp, long toTimestamp) {
		super(input, contextArgs, model, tokenizer);

		this.apiClient = contextArgs.apiClient();

		this.viewName = viewName;
		this.fromTimestamp = fromTimestamp;
		this.toTimestamp = toTimestamp;
	}

	@Override
	protected List<Attachment> createAttachments() {
		List<Attachment> attachments = new ArrayList<>();

		String viewLink = AlertUtil.buildLinkForView(contextArgs.appHost, contextArgs.serviceId, viewName,
				fromTimestamp, toTimestamp);

		String settingsLink = AlertUtil.buildLinkForSettings(contextArgs.appHost, contextArgs.serviceId);

		Attachment.Builder mainAttachmentBuilder = Attachment.newBuilder().addMrkdwn(MARKDOWN_IN_PRETEXT)
				.addMrkdwn(MARKDOWN_IN_TEXT).setColor(MESSAGE_COLOR).setFallback(createFallback())
				.setTitle(VIEW_ERRORS_LINK).setTitleLink(viewLink)
				.setText(SlackUtil.formatLink("Manage settings", settingsLink)).setThumbUrl(thumbUrl());

		if (!CollectionUtil.safeIsEmpty(model.body.parts)) {
			for (Part part : model.body.parts) {
				switch (part.type) {
				case TABLE:
					mainAttachmentBuilder.addAllFields(createTableFields(part));
					break;
				case TOP_CONTRIBUTORS:
					attachments.addAll(createContributorAttachments());
					break;
				case ACTION:
				case STACKTRACE:
				case STRING:
					break;
				}
			}
		}

		attachments.add(0, mainAttachmentBuilder.build());

		return attachments;
	}

	private Collection<Attachment> createContributorAttachments() {
		if (CollectionUtil.safeIsEmpty(contextArgs.contributors)) {
			return Collections.emptyList();
		}

		Collection<Attachment> result = new ArrayList<>();

		int colorIndex = 0;

		for (Contributor contributor : contextArgs.contributors) {
			String desc = buildContributorDescription(contributor);

			if (StringUtil.isNullOrEmpty(desc)) {
				continue;
			}

			Attachment.Builder contributorBuilder = Attachment.newBuilder();

			contributorBuilder.setColor(REQUEST_ROW_COLORS[colorIndex]).addMrkdwn(MARKDOWN_IN_TEXT).setText(desc);

			result.add(contributorBuilder.build());

			if (colorIndex < REQUEST_ROW_COLORS.length - 1) {
				colorIndex++;
			}
		}

		return result;
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

		if (!StringUtil.isNullOrEmpty(eventLocation)) {
			sb.append(" at " + AlertUtil.formatEventData(eventLocation, MAX_CONTRIBUTER_LENGTH));
		}

		String hitsCount = AlertUtil.formatCountable(contributor.hits, "time");
		sb.append(" | *" + hitsCount + "*");

		return sb.toString();
	}

	protected String thumbUrl() {
		return "https://s3.amazonaws.com/www.takipi.com/email/v5/threshold-thumb.png";
	}

	protected abstract int getTaleSource();
}
