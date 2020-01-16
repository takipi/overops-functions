package com.takipi.udf.alerts.slack.sender;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.slack.SlackFunction.SlackInput;
import com.takipi.udf.alerts.slack.message.AttachmentField;
import com.takipi.udf.alerts.template.model.Model;
import com.takipi.udf.alerts.template.model.Models;
import com.takipi.udf.alerts.template.token.Tokenizer;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.alerts.util.SourceConstants;

public class SlackThresholdSender extends SlackTimeframeSender {
	private static final Logger logger = LoggerFactory.getLogger(SlackThresholdSender.class);

	private static final String MESSAGE_TEXT_PLAIN_FORMAT = "UDF: Events in view %s have occurred more than %s %s in the past %d minutes in %s (alert added by %s)";
	private static final String MESSAGE_TEXT_RICH_FORMAT = "UDF: Events in view *%s* have occurred more than *%s* %s in the past %d minutes in *%s* (alert added by %s)";

	private final long threshold;
	private final int thresholdTimeframe;
	private final long hitCount;

	private SlackThresholdSender(SlackInput input, ContextArgs contextArgs, Model model, Tokenizer tokenizer,
			String addedByUser, String viewName, long fromTimestamp, long toTimestamp, long threshold,
			int thresholdTimeframe, long hitCount) {
		super(input, contextArgs, model, tokenizer, addedByUser, viewName, fromTimestamp, toTimestamp);

		this.threshold = threshold;
		this.thresholdTimeframe = thresholdTimeframe;
		this.hitCount = hitCount;
	}

	@Override
	protected String createPlainMessageText() {
		return createMessageText(MESSAGE_TEXT_PLAIN_FORMAT);
	}

	@Override
	protected String createText() {
		return createMessageText(MESSAGE_TEXT_RICH_FORMAT);
	}

	private String createMessageText(String format) {
		String formattedTimesString = AlertUtil.formatCountable(threshold, "time", false);

		return String.format(format, viewName, AlertUtil.formatNumberWithCommas(threshold), formattedTimesString,
				thresholdTimeframe, contextArgs.serviceName, addedByUser);
	}

	@Override
	protected Collection<AttachmentField> createAttachmentFields() {
		Collection<AttachmentField> result = super.createAttachmentFields();

		result.add(createAttachmentField("Threshold", AlertUtil.formatNumberWithCommas(threshold)));
		result.add(createAttachmentField("Times", AlertUtil.formatNumberWithCommas(hitCount)));

		return result;
	}

	@Override
	protected int getTaleSource() {
		return SourceConstants.SOURCE_SLACK_THRESHOLD_MESSAGE;
	}

	@Override
	protected String getInternalDescription() {
		return "View passing threshold: " + viewName;
	}

	public static SlackSender create(SlackInput input, ContextArgs contextArgs) {
		Model model = Models.threshold();

		if (model == null) {
			// Logging happens inside threshold.
			//
			return null;
		}

		if (!contextArgs.viewValidate()) {
			return null;
		}

		long threshold = contextArgs.longData("threshold");
		int thresholdTimeframe = contextArgs.intData("threshold_timeframe");
		long hitCount = contextArgs.longData("hit_count");
		String viewName = contextArgs.data("view_name");

		if ((threshold < 0) || (thresholdTimeframe < 0) || (hitCount < 0) || (Strings.isNullOrEmpty(viewName))) {
			logger.error("Can't build sender with incomplete data");
			return null;
		}

		String addedByUser = contextArgs.data("added_by_user", "Unknown");
		long fromTimestamp = contextArgs.longData("from_timestamp");
		long toTimestamp = contextArgs.longData("to_timestamp");

		return new SlackThresholdSender(input, contextArgs, model, Tokenizer.from(contextArgs), addedByUser, viewName,
				fromTimestamp, toTimestamp, threshold, thresholdTimeframe, hitCount);
	}
}
