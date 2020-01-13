package com.takipi.udf.alerts.servicenow.sender;

import com.takipi.api.client.result.event.EventResult;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.servicenow.ServiceNowFunction.ServiceNowInput;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.alerts.util.HTMLUtil;
import com.takipi.udf.alerts.util.SourceConstants;

public class ServiceNowNewEventSender extends ServiceNowEventSender {
	private static final String SHORT_DESCRIPTION_TEMPLATE = "UDF: OverOps - A new '%s' has been detected in '%s'";

	private ServiceNowNewEventSender(ServiceNowInput input, ContextArgs contextArgs, EventResult event) {
		super(input, contextArgs, event);
	}

	@Override
	protected int getTaleUrlSource() {
		return SourceConstants.SOURCE_SERVICENOW_FIRST_HIT_MESSAGE;
	}

	@Override
	protected String getShortDescription() {
		return String.format(SHORT_DESCRIPTION_TEMPLATE, event.summary, contextArgs.serviceName);
	}

	@Override
	protected String createHtmlAlertAddedBy() {
		return HTMLUtil.htmlParagraph(
				"(alert added by " + HTMLUtil.htmlBold(contextArgs.data("added_by_user", "Unknown")) + ")");
	}

	@Override
	protected String getInternalDescription() {
		return "New request: " + event.id;
	}

	public static ServiceNowSender create(ServiceNowInput input, ContextArgs contextArgs) {
		EventResult event = AlertUtil.getEvent(contextArgs);

		if (event == null) {
			// Logging happens inside getEvent if needed.
			//
			return null;
		}

		return new ServiceNowNewEventSender(input, contextArgs, event);
	}
}
