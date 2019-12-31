package com.takipi.udf.alerts.slack.sender;

import com.takipi.api.client.result.event.EventResult;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.slack.SlackFunction.SlackInput;

public class ResurfacedEventSender extends EventSender {
	private static final String SUBHEADING_PLAIN_FORMAT = "A resolved error just reappeared in %s: %s";
	private static final String SUBHEADING_RICH_FORMAT = "A resolved error just reappeared in *%s*: *%s*";

	private ResurfacedEventSender(SlackInput input, EventResult event, ContextArgs contextArgs) {
		super(input, event, contextArgs);
	}

	@Override
	protected String getSubheadingPlainFormat() {
		return SUBHEADING_PLAIN_FORMAT;
	}

	@Override
	protected String getSubheadingRichFormat() {
		return SUBHEADING_RICH_FORMAT;
	}

	@Override
	protected String getInternalDescription() {
		return "New request: " + event.id;
	}

	@Override
	protected String createSubheading(String serviceName, String format) {
		return String.format(format, serviceName, event.summary);
	}

	public static Sender create(SlackInput input, ContextArgs contextArgs) {
		EventResult event = getEvent(contextArgs);

		if (event == null) {
			// Logging happens inside getEvent if needed.
			//
			return null;
		}

		return new ResurfacedEventSender(input, event, contextArgs);
	}
}
