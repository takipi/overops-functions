package com.takipi.udf.alerts.slack.sender;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.slack.SlackConsts;
import com.takipi.udf.alerts.slack.SlackFunction.SlackInput;
import com.takipi.udf.alerts.slack.SlackUtil;
import com.takipi.udf.alerts.slack.client.SlackClient;
import com.takipi.udf.alerts.slack.client.SlackResponse;
import com.takipi.udf.alerts.slack.message.Attachment;
import com.takipi.udf.alerts.slack.message.AttachmentField;
import com.takipi.udf.alerts.slack.message.Message;
import com.takipi.udf.alerts.template.model.Model;
import com.takipi.udf.alerts.template.token.Tokenizer;

public abstract class SlackSender {
	protected static final Logger logger = LoggerFactory.getLogger(SlackSender.class);

	protected static final String MARKDOWN_IN_PRETEXT = "pretext";
	protected static final String MARKDOWN_IN_TEXT = "text";
	protected static final String MARKDOWN_IN_FIELDS = "fields";

	protected final SlackInput input;
	protected final ContextArgs contextArgs;
	protected final Model model;
	protected final Tokenizer tokenizer;

	protected SlackSender(SlackInput input, ContextArgs contextArgs, Model model, Tokenizer tokenizer) {
		this.input = input;
		this.contextArgs = contextArgs;
		this.model = model;
		this.tokenizer = tokenizer;
	}

	public boolean sendMessage() {
		String internalDescription = getInternalDescription();

		logger.info("About to send a Slack message for {} ({}) to {}.", contextArgs.serviceId, internalDescription,
				input.inhook_url);

		try {
			return doSendMessage(internalDescription);
		} catch (Exception e) {
			logger.error("Unable to send Slack message for {} ({}) to {}.", contextArgs.serviceId, internalDescription,
					input.inhook_url, e);

			return false;
		}
	}

	private boolean doSendMessage(String internalDescription) {
		String text = createText();

		List<Attachment> attachments = Lists.newArrayList();
		attachments.addAll(createAttachments());

		SlackResponse response = postMessage(text, attachments);

		if (!response.ok) {
			logger.error("Slack message ({}) returned an error: \"{}\" (url: {}).", internalDescription, response.error,
					input.inhook_url);

			return false;
		}

		logger.info("Slack message ({}) successfully sent to {}.", internalDescription, input.inhook_url);

		return true;
	}

	private SlackResponse postMessage(String text, List<Attachment> attachments) {
		try {
			Message message = createMessage(text, attachments);
			SlackClient client = input.client();
			SlackResponse response = client.postMessage(message);

			return response;
		} catch (Exception e) {
			logger.error("Error posting message to {}.", input.inhook_url);

			return SlackResponse.of(false, e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private Message createMessage(String text, List<Attachment> attachments) {
		Message.Builder messageBuilder = Message.newBuilder().setUsername(SlackConsts.USERNAME)
				.setIconUrl(SlackConsts.ICON_URL);

		if (!Strings.isNullOrEmpty(input.inhook_channel)) {
			messageBuilder.setChannel(input.inhook_channel);
		}

		if (!Strings.isNullOrEmpty(text)) {
			messageBuilder.setText(text);
		}

		for (Attachment attachment : attachments) {
			messageBuilder.addAttachment(attachment);
		}

		return messageBuilder.build();
	}

	protected AttachmentField createAttachmentField(String title, String value, String link) {
		return createAttachmentField(title, value, link, true);
	}

	protected AttachmentField createAttachmentField(String title, String value) {
		return createAttachmentField(title, value, null, true);
	}

	protected AttachmentField createAttachmentField(String title, String value, boolean isShort) {
		return createAttachmentField(title, value, null, isShort);
	}

	protected AttachmentField createAttachmentField(String title, String value, String link, boolean isShort) {
		String finalValue;

		if (!Strings.isNullOrEmpty(link)) {
			finalValue = SlackUtil.formatLink(value, link);
		} else {
			finalValue = value;
		}

		return AttachmentField.of(title, finalValue, isShort);
	}

	protected abstract String getInternalDescription();

	protected abstract String createText();

	protected abstract List<Attachment> createAttachments();
}
