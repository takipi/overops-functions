package com.takipi.udf.alerts.servicenow.sender;

import com.takipi.common.util.StringUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.servicenow.ServiceNowFunction.ServiceNowInput;
import com.takipi.udf.alerts.util.SourceConstants;

public class ServiceNowAnomalySender extends ServiceNowTimeframeSender {

	private static final String SHORT_DESCRIPTION_TEMPLATE = "UDF: OverOps - Anomaly detected in view '%s' by '%s' in %s in '%s'";

	private final String anomalyReason;
	private final String anomalyTimeframe;

	protected ServiceNowAnomalySender(ServiceNowInput input, ContextArgs contextArgs, String addedByUser,
			String viewName, long fromTimestamp, long toTimestamp, String anomalyReason, String anomalyTimeframe) {
		super(input, contextArgs, addedByUser, viewName, fromTimestamp, toTimestamp);

		this.anomalyReason = anomalyReason;
		this.anomalyTimeframe = anomalyTimeframe;
	}

	@Override
	protected String getShortDescription() {
		return String.format(SHORT_DESCRIPTION_TEMPLATE, viewName, anomalyReason, anomalyTimeframe,
				contextArgs.serviceName);
	}

	@Override
	protected int getTaleSource() {
		// TODO - dedicated source.
		//
		return SourceConstants.SOURCE_SERVICENOW_THRESHOLD_MESSAGE;
	}

	@Override
	protected String getInternalDescription() {
		return "Preset havnig anomaly: " + viewName;
	}

	public static ServiceNowSender create(ServiceNowInput input, ContextArgs contextArgs) {
		if (!contextArgs.viewValidate()) {
			return null;
		}

		String anomalyReason = contextArgs.data("anomaly_reason");
		String anomalyTimeframe = contextArgs.data("anomaly_timeframe");
		String viewName = contextArgs.data("view_name");

		if ((StringUtil.isNullOrEmpty(anomalyReason)) || (StringUtil.isNullOrEmpty(anomalyTimeframe))
				|| (StringUtil.isNullOrEmpty(viewName))) {
			logger.error("Can't build sender with incomplete data");
			return null;
		}

		String addedByUser = contextArgs.data("added_by_user", "Unknown");
		long fromTimestamp = contextArgs.longData("from_timestamp");
		long toTimestamp = contextArgs.longData("to_timestamp");

		return new ServiceNowAnomalySender(input, contextArgs, addedByUser, viewName, fromTimestamp, toTimestamp,
				anomalyReason, anomalyTimeframe);
	}
}
