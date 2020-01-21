package com.takipi.udf.alerts.slack.sender;

import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.slack.SlackFunction.SlackInput;
import com.takipi.udf.alerts.template.model.Model;
import com.takipi.udf.alerts.template.model.Models;
import com.takipi.udf.alerts.template.token.ThresholdTokenizer;
import com.takipi.udf.alerts.template.token.Tokenizer;
import com.takipi.udf.alerts.util.SourceConstants;

public class SlackThresholdSender extends SlackTimeframeSender {
	private SlackThresholdSender(SlackInput input, ContextArgs contextArgs, Model model, Tokenizer tokenizer,
			String viewName, long fromTimestamp, long toTimestamp) {
		super(input, contextArgs, model, tokenizer, viewName, fromTimestamp, toTimestamp);
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

		String viewName = contextArgs.data("view_name");
		long fromTimestamp = contextArgs.longData("from_timestamp");
		long toTimestamp = contextArgs.longData("to_timestamp");

		return new SlackThresholdSender(input, contextArgs, model, ThresholdTokenizer.from(contextArgs), viewName,
				fromTimestamp, toTimestamp);
	}
}
