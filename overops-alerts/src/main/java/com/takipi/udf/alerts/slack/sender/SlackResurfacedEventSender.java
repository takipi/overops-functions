package com.takipi.udf.alerts.slack.sender;

import com.takipi.api.client.result.event.EventResult;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.slack.SlackFunction.SlackInput;
import com.takipi.udf.alerts.template.model.Model;
import com.takipi.udf.alerts.template.model.Models;
import com.takipi.udf.alerts.template.token.EventTokenizer;
import com.takipi.udf.alerts.util.AlertUtil;

public class SlackResurfacedEventSender extends SlackEventSender {
	private SlackResurfacedEventSender(SlackInput input, ContextArgs contextArgs, Model model, EventTokenizer tokenizer,
			EventResult event) {
		super(input, contextArgs, model, tokenizer, event);
	}

	@Override
	protected String getInternalDescription() {
		return "Resurfaced event: " + event.id;
	}

	public static SlackSender create(SlackInput input, ContextArgs contextArgs) {
		Model model = Models.resurfacedError();

		if (model == null) {
			// Logging happens inside resurfacedError.
			//
			return null;
		}

		EventResult event = AlertUtil.getEvent(contextArgs);

		if (event == null) {
			// Logging happens inside getEvent if needed.
			//
			return null;
		}

		return new SlackResurfacedEventSender(input, contextArgs, model, EventTokenizer.from(contextArgs, event),
				event);
	}
}
