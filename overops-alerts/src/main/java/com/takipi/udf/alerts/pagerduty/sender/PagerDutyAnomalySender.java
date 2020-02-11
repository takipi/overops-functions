package com.takipi.udf.alerts.pagerduty.sender;

import com.takipi.common.util.StringUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.pagerduty.PagerDutyFunction.PagerDutyInput;
import com.takipi.udf.alerts.util.SourceConstants;

public class PagerDutyAnomalySender extends PagerDutyTimeframeSender {

	private static final String EVENT_DESC_FORMAT = "Anomaly detected in view %s by %s in %s in %s (alert added by %s)";

	private final String anomalyReason;
	private final String anomalyTimeframe;

	private PagerDutyAnomalySender(PagerDutyInput input, ContextArgs contextArgs, String addedByUser, String viewName,
			long fromTimestamp, long toTimestamp, String anomalyReason, String anomalyTimeframe) {
		super(input, contextArgs, addedByUser, viewName, fromTimestamp, toTimestamp);

		this.anomalyReason = anomalyReason;
		this.anomalyTimeframe = anomalyTimeframe;
	}

	@Override
	protected String createDescription() {
		return String.format(EVENT_DESC_FORMAT, viewName, anomalyReason, anomalyTimeframe, contextArgs.serviceName,
				addedByUser);
	}

	@Override
	protected int getTaleSource() {
		// TODO - dedicated source.
		//
		return SourceConstants.SOURCE_PAGERDUTY_THRESHOLD_MESSAGE;
	}

	@Override
	protected String getInternalDescription() {
		return "New custom anomaly alert for " + viewName;
	}

	public static PagerDutySender create(PagerDutyInput input, ContextArgs contextArgs) {
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

		return new PagerDutyAnomalySender(input, contextArgs, addedByUser, viewName, fromTimestamp, toTimestamp,
				anomalyReason, anomalyTimeframe);
	}
}
