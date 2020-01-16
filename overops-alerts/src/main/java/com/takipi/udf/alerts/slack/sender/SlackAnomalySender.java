package com.takipi.udf.alerts.slack.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.slack.SlackFunction.SlackInput;
import com.takipi.udf.alerts.template.model.Model;
import com.takipi.udf.alerts.template.model.Models;
import com.takipi.udf.alerts.template.token.Tokenizer;
import com.takipi.udf.alerts.util.SourceConstants;

public class SlackAnomalySender extends SlackTimeframeSender {
	private static final Logger logger = LoggerFactory.getLogger(SlackAnomalySender.class);

	private static final String MESSAGE_TEXT_PLAIN_FORMAT = "UDF: Anomaly detected in view %s by %s in %s in %s (alert added by %s)";
	private static final String MESSAGE_TEXT_RICH_FORMAT = "UDF: Anomaly detected in view *%s* by *%s* in %s in *%s* (alert added by %s)";

	private final String anomalyReason;
	private final String anomalyTimeframe;

	private SlackAnomalySender(SlackInput input, ContextArgs contextArgs, Model model, Tokenizer tokenizer,
			String addedByUser, String viewName, long fromTimestamp, long toTimestamp, String anomalyReason,
			String anomalyTimeframe) {
		super(input, contextArgs, model, tokenizer, addedByUser, viewName, fromTimestamp, toTimestamp);

		this.anomalyReason = anomalyReason;
		this.anomalyTimeframe = anomalyTimeframe;
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
		return String.format(format, viewName, anomalyReason, anomalyTimeframe, contextArgs.serviceName, addedByUser);
	}

	@Override
	protected int getTaleSource() {
		// TODO - dedicated source.
		//
		return SourceConstants.SOURCE_SLACK_THRESHOLD_MESSAGE;
	}

	@Override
	protected String getInternalDescription() {
		return "View havnig anomaly: " + viewName;
	}

	public static SlackSender create(SlackInput input, ContextArgs contextArgs) {
		Model model = Models.anomaly();

		if (model == null) {
			// Logging happens inside anomaly.
			//
			return null;
		}

		if (!contextArgs.viewValidate()) {
			return null;
		}

		String anomalyReason = contextArgs.data("anomaly_reason");
		String anomalyTimeframe = contextArgs.data("anomaly_timeframe");
		String viewName = contextArgs.data("view_name");

		if ((Strings.isNullOrEmpty(anomalyReason)) || (Strings.isNullOrEmpty(anomalyTimeframe))
				|| (Strings.isNullOrEmpty(viewName))) {
			logger.error("Can't build sender with incomplete data");
			return null;
		}

		String addedByUser = contextArgs.data("added_by_user", "Unknown");
		long fromTimestamp = contextArgs.longData("from_timestamp");
		long toTimestamp = contextArgs.longData("to_timestamp");

		return new SlackAnomalySender(input, contextArgs, model, Tokenizer.from(contextArgs), addedByUser, viewName,
				fromTimestamp, toTimestamp, anomalyReason, anomalyTimeframe);
	}
}
