package com.takipi.udf.alerts.slack.sender;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Strings;
import com.takipi.api.client.data.event.Location;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.common.util.CollectionUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.slack.SlackFunction.SlackInput;
import com.takipi.udf.alerts.slack.SlackUtil;
import com.takipi.udf.alerts.slack.format.SlackFormatter;
import com.takipi.udf.alerts.slack.message.Attachment;
import com.takipi.udf.alerts.slack.message.AttachmentField;
import com.takipi.udf.alerts.template.model.Body.ActionType;
import com.takipi.udf.alerts.template.model.Body.Part;
import com.takipi.udf.alerts.template.model.Model;
import com.takipi.udf.alerts.template.token.EventTokenizer;
import com.takipi.udf.alerts.template.token.TokenizerUtil;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.alerts.util.SourceConstants;
import com.takipi.udf.util.StringUtil;
import com.takipi.udf.util.url.UrlUtil;

public abstract class SlackEventSender extends SlackSender {
	private static final String CODE_BLOCK_MARKER = "```";

	private static final String STACK_FRAME_FORMAT = " at %s";

	private static final String COLOR_EXCEPTION = "#d00000";
	private static final String COLOR_LOG_ERROR = "#ff7000";
	private static final String COLOR_LOG_WARN = "#ffc000";
	private static final String COLOR_HTTP = "#a000f7";
	private static final String COLOR_DEFAULT = "#b40000";

	private static final String VIEW_EVENT = "*View Event*";
	private static final String ARCHIVE_FUTURE_ALERTS_FORMAT = "_Do not alert on new %ss_";

	protected final EventResult event;

	protected SlackEventSender(SlackInput input, ContextArgs contextArgs, Model model, EventTokenizer tokenizer,
			EventResult event) {
		super(input, contextArgs, model, tokenizer);

		this.event = event;
	}

	@Override
	protected List<Attachment> createAttachments() {
		Attachment.Builder builder = Attachment.newBuilder().addMrkdwn(MARKDOWN_IN_PRETEXT).addMrkdwn(MARKDOWN_IN_TEXT)
				.addMrkdwn(MARKDOWN_IN_FIELDS);

		builder.setFallback(createFallback());

		String title = getTitle();
		builder.setTitle(title);

		String titleLink = createTaleLink();
		builder.setTitleLink(titleLink);
		builder.setColor(createColor());

		if (!CollectionUtil.safeIsEmpty(model.body.parts)) {
			StringBuilder textBuilder = new StringBuilder();

			for (Part part : model.body.parts) {
				switch (part.type) {
				case STACKTRACE:
					fillTextBuilder(textBuilder, createStackTrace());
					break;
				case STRING:
					String partText = TokenizerUtil.work(tokenizer, part.text, SlackFormatter.of(part.options));
					fillTextBuilder(textBuilder, partText);
					break;
				case TABLE:
					builder.addAllFields(createTableFields(part));
					break;
				case ACTION:
					AttachmentField attachmentField = getActionField(part.actionType);

					if (attachmentField != null) {
						builder.addField(attachmentField);
					}

					break;
				case TOP_CONTRIBUTORS:
					break;
				}
			}

			String text = textBuilder.toString();

			if (!text.isEmpty()) {
				builder.setText(text);
			}
		}

		return Collections.singletonList(builder.build());
	}

	private void fillTextBuilder(StringBuilder builder, String text) {
		if (Strings.isNullOrEmpty(text)) {
			return;
		}

		if (builder.length() > 0) {
			builder.append('\n');
		}

		builder.append(text);
	}

	private AttachmentField getActionField(ActionType actionType) {
		switch (actionType) {
		case DONT_ALERT_ON_EVENT:
			return getDontAlertActionField();
		case VIEW_EVENT:
			return createAttachmentField("", SlackUtil.formatLink(VIEW_EVENT, createTaleLink()), false);
		}

		return null;
	}

	private AttachmentField getDontAlertActionField() {
		if (!AlertUtil.isException(event)) {
			return null;
		}

		String title = getTitle();
		String archiveTitle = getArchiveFutureTitle(title);
		String url = AlertUtil.generateMailAutoArchiveActionLink(contextArgs.appHost, contextArgs.serviceId,
				event.name);

		if (Strings.isNullOrEmpty(url)) {
			return null;
		}

		return createAttachmentField("", SlackUtil.formatLink(archiveTitle, url), false);
	}

	private String getTitle() {
		return TokenizerUtil.work(tokenizer, model.title.text);
	}

	private String createColor() {
		String type = event.type;

		if (AlertUtil.isException(type)) {
			return COLOR_EXCEPTION;
		}

		if ("Logged Error".equals(type)) {
			return COLOR_LOG_ERROR;
		}

		if ("Logged Warning".equals(type)) {
			return COLOR_LOG_WARN;
		}

		if ("HTTP Error".equals(type)) {
			return COLOR_HTTP;
		}

		return COLOR_DEFAULT;
	}

	private String createStackTrace() {
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

	private String getArchiveFutureTitle(String title) {
		String requestHeader = StringUtil.ellipsize(title, 40);

		return String.format(ARCHIVE_FUTURE_ALERTS_FORMAT, requestHeader);
	}
}
