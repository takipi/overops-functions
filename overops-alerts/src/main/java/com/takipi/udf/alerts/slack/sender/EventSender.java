package com.takipi.udf.alerts.slack.sender;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Strings;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.event.Location;
import com.takipi.api.client.request.event.EventRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.CollectionUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.slack.SlackFunction.SlackInput;
import com.takipi.udf.alerts.slack.SlackUtil;
import com.takipi.udf.alerts.slack.message.Attachment;
import com.takipi.udf.alerts.slack.message.AttachmentField;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.alerts.util.SourceConstants;
import com.takipi.udf.util.StringUtil;
import com.takipi.udf.util.url.UrlUtil;

public abstract class EventSender extends Sender {
	private static final String CODE_BLOCK_MARKER = "```";

	private static final String STACK_FRAME_FORMAT = " at %s";

	private static final String COLOR_EXCEPTION = "#d00000";
	private static final String COLOR_XMEN_ERROR = "#ff7000";
	private static final String COLOR_XMEN_WARN = "#ffc000";
	private static final String COLOR_ROCKY = "#a000f7";
	private static final String COLOR_DEFAULT = "#b40000";

	private static final String VIEW_EVENT = "*View Event*";
	private static final String ARCHIVE_FUTURE_ALERTS_FORMAT = "_Do not alert on new %ss_";

	private static final int MAX_TITLE_LENGTH = 120;

	private static final String UNNAMED_DEPLOYMENT = "Unnamed Deployment";
	private static final String DEPLOYMENT_NAMING_LINK = "https://doc.overops.com/docs/naming-your-application-server-deployment#section-naming-the-deployment";

	protected final EventResult event;
	protected final ContextArgs contextArgs;

	protected EventSender(SlackInput input, EventResult event, ContextArgs contextArgs) {
		super(input);

		this.event = event;
		this.contextArgs = contextArgs;
	}

	protected abstract String getSubheadingPlainFormat();

	protected abstract String getSubheadingRichFormat();

	protected abstract String createSubheading(String serviceName, String format);

	@Override
	protected String createText() {
		return null;
	}

	@Override
	protected List<Attachment> createAttachments() {
		Attachment.Builder builder = Attachment.newBuilder().addMrkdwn(MARKDOWN_IN_PRETEXT).addMrkdwn(MARKDOWN_IN_TEXT)
				.addMrkdwn(MARKDOWN_IN_FIELDS);

		builder.setFallback(createPlainSubheading(contextArgs.serviceName));

		String title = AlertUtil.createEventTitle(event, MAX_TITLE_LENGTH);
		builder.setTitle(title);

		String titleLink = createTaleLink();
		builder.setTitleLink(titleLink);
		builder.setColor(createColor());
		builder.setPretext(createRichSubheading(contextArgs.serviceName));

		String attachmentText = createAttachmentText();

		if (attachmentText != null) {
			builder.setText(attachmentText);
		}

		AttachmentField serverNameField = createServerNameField();
		AttachmentField jvmNameField = createJvmNameField();
		AttachmentField deploymentNameField = createDeploymentNameField();

		if (serverNameField != null) {
			builder.addField(serverNameField);
		}

		if (jvmNameField != null) {
			builder.addField(jvmNameField);
		}

		if (deploymentNameField != null) {
			builder.addField(deploymentNameField);
		}

		AttachmentField viewEventField = createAttachmentField("", SlackUtil.formatLink(VIEW_EVENT, titleLink), false);
		builder.addField(viewEventField);

		if (AlertUtil.isException(event)) {
			String archiveTitle = getArchiveFutureTitle(title);
			String url = AlertUtil.generateMailAutoArchiveActionLink(contextArgs.appHost, contextArgs.serviceId,
					event.name);

			if (!Strings.isNullOrEmpty(url)) {
				AttachmentField archiveField = createAttachmentField("", SlackUtil.formatLink(archiveTitle, url),
						false);

				builder.addField(archiveField);
			}
		}

		return Collections.singletonList(builder.build());
	}

	private String createColor() {
		String type = event.type;

		if (AlertUtil.isException(type)) {
			return COLOR_EXCEPTION;
		}

		if ("Logged Error".equals(type)) {
			return COLOR_XMEN_ERROR;
		}

		if ("Logged Warning".equals(type)) {
			return COLOR_XMEN_WARN;
		}

		if ("HTTP Error".equals(type)) {
			return COLOR_ROCKY;
		}

		return COLOR_DEFAULT;
	}

	private AttachmentField createServerNameField() {
		String serverName = event.introduced_by_server;

		if (Strings.isNullOrEmpty(serverName)) {
			return null;
		}

		return createAttachmentField("Server", serverName);
	}

	private AttachmentField createJvmNameField() {
		String jvmName = event.introduced_by_application;

		if (Strings.isNullOrEmpty(jvmName)) {
			return null;
		}

		return createAttachmentField("Application", jvmName);
	}

	private AttachmentField createDeploymentNameField() {
		String deploymentName = event.introduced_by;

		if (Strings.isNullOrEmpty(deploymentName)) {
			return null;
		} else if (deploymentName.equals(UNNAMED_DEPLOYMENT)) {
			return createAttachmentField("Deployment", deploymentName, DEPLOYMENT_NAMING_LINK);
		} else {
			return createAttachmentField("Deployment", deploymentName);
		}
	}

	private String createAttachmentText() {
		if (CollectionUtil.safeIsEmpty(event.stack_frames)) {
			return null;
		}

		StringBuilder textBuilder = new StringBuilder();

		textBuilder.append(CODE_BLOCK_MARKER);

		for (Location frame : event.stack_frames) {
			if ((!frame.in_filter) || (Strings.isNullOrEmpty(frame.prettified_name))) {
				continue;
			}

			textBuilder.append(String.format(STACK_FRAME_FORMAT, frame.prettified_name)).append('\n');
		}

		textBuilder.append(CODE_BLOCK_MARKER);

		return textBuilder.toString();
	}

	private String createTaleLink() {
		return UrlUtil.buildTaleUrl(contextArgs.appHost, contextArgs.serviceId, contextArgs.eventId, 1,
				SourceConstants.SOURCE_SLACK_FIRST_HIT_MESSAGE, true, null, 0);
	}

	private String createPlainSubheading(String serviceName) {
		return createSubheading(serviceName, getSubheadingPlainFormat());
	}

	private String createRichSubheading(String serviceName) {
		return createSubheading(serviceName, getSubheadingRichFormat());
	}

	private String getArchiveFutureTitle(String title) {
		String requestHeader = StringUtil.ellipsize(title, 40);

		return String.format(ARCHIVE_FUTURE_ALERTS_FORMAT, requestHeader);
	}

	protected static EventResult getEvent(ContextArgs args) {
		if (!args.eventValidate()) {
			return null;
		}

		ApiClient apiClient = args.apiClient();

		EventRequest eventRequest = EventRequest.newBuilder().setServiceId(args.serviceId).setEventId(args.eventId)
				.setIncludeStacktrace(true).build();

		Response<EventResult> eventResult = apiClient.get(eventRequest);

		if ((eventResult.isBadResponse()) || (eventResult.data == null)) {
			logger.error("Can't get event {}/{}", args.serviceId, args.eventId);
			return null;
		}

		return eventResult.data;
	}
}
