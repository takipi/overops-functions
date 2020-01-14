package com.takipi.udf.alerts.pagerduty.sender;

import com.takipi.api.client.result.event.EventResult;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.pagerduty.PagerDutyFunction.PagerDutyInput;
import com.takipi.udf.alerts.util.AlertUtil;

public class PagerDutyResurfacedEventSender extends PagerDutyEventSender {

	private static final String EVENT_DESC_FORMAT = "UDF: A resolved error just reappeared in %s: %s";

	private PagerDutyResurfacedEventSender(PagerDutyInput input, EventResult event, ContextArgs contextArgs) {
		super(input, event, contextArgs);
	}

	@Override
	protected String getInternalDescription() {
		return "Resurfaced request: " + event.id;
	}

	@Override
	protected String createDescription() {
		return String.format(EVENT_DESC_FORMAT, contextArgs.serviceName, event.summary);
	}

	public static PagerDutySender create(PagerDutyInput input, ContextArgs contextArgs) {
		EventResult event = AlertUtil.getEvent(contextArgs);

		if (event == null) {
			// Logging happens inside getEvent if needed.
			//
			return null;
		}

		return new PagerDutyResurfacedEventSender(input, event, contextArgs);
	}
}
