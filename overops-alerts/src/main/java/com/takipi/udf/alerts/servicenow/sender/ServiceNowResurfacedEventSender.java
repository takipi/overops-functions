package com.takipi.udf.alerts.servicenow.sender;

import com.takipi.api.client.result.event.EventResult;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.servicenow.ServiceNowFunction.ServiceNowInput;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.alerts.util.SourceConstants;

public class ServiceNowResurfacedEventSender extends ServiceNowEventSender {
	private static final String SHORT_DESCRIPTION_TEMPLATE = "UDF: OverOps - A resolved error just reappeared in '%s': '%s'";

	private ServiceNowResurfacedEventSender(ServiceNowInput input, ContextArgs contextArgs, EventResult event) {
		super(input, contextArgs, event);
	}

	@Override
	protected int getTaleUrlSource() {
		return SourceConstants.SOURCE_SERVICENOW_RESURFACE_MESSAGE;
	}

	@Override
	protected String getShortDescription() {
		return String.format(SHORT_DESCRIPTION_TEMPLATE, contextArgs.serviceName, event.summary);
	}

	@Override
	protected String createHtmlAlertAddedBy() {
		return "";
	}

	@Override
	protected String getInternalDescription() {
		return "Resurfaced request: " + event.id;
	}

	public static ServiceNowSender create(ServiceNowInput input, ContextArgs contextArgs) {
		EventResult event = AlertUtil.getEvent(contextArgs);

		if (event == null) {
			// Logging happens inside getEvent if needed.
			//
			return null;
		}

		return new ServiceNowResurfacedEventSender(input, contextArgs, event);
	}
}
