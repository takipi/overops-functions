package com.takipi.udf.alerts.servicenow.sender;

import com.google.common.base.Strings;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.servicenow.ServiceNowFunction.ServiceNowInput;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.alerts.util.SourceConstants;

public class ServiceNowThresholdSender extends ServiceNowTimeframeSender {
	private static final String SHORT_DESCRIPTION_TEMPLATE = "UDF: OverOps - Events in view '%s' have occurred more than %s %s in the past %d minutes in '%s'";

	private final long threshold;
	private final int thresholdTimeframe;
	private final long hitCount;

	private ServiceNowThresholdSender(ServiceNowInput input, ContextArgs contextArgs, String addedByUser,
			String viewName, long fromTimestamp, long toTimestamp, long threshold, int thresholdTimeframe,
			long hitCount) {
		super(input, contextArgs, addedByUser, viewName, fromTimestamp, toTimestamp);

		this.threshold = threshold;
		this.thresholdTimeframe = thresholdTimeframe;
		this.hitCount = hitCount;
	}

	@Override
	protected String getShortDescription() {
		String formattedTimesString = AlertUtil.formatCountable(threshold, "time", false);

		return String.format(SHORT_DESCRIPTION_TEMPLATE, viewName, AlertUtil.formatNumberWithCommas(threshold),
				formattedTimesString, thresholdTimeframe, contextArgs.serviceName);
	}

	@Override
	protected String htmlTableContent() {
		StringBuilder sb = new StringBuilder(super.htmlTableContent());

		sb.append(nonEmptyHtmlTableRow("Threshold", AlertUtil.formatNumberWithCommas(threshold)));
		sb.append(nonEmptyHtmlTableRow("Times", AlertUtil.formatNumberWithCommas(hitCount)));

		return sb.toString();
	}

	@Override
	protected int getTaleSource() {
		return SourceConstants.SOURCE_SERVICENOW_THRESHOLD_MESSAGE;
	}

	@Override
	protected String getInternalDescription() {
		return "Preset passing threshold: " + viewName;
	}

	public static ServiceNowSender create(ServiceNowInput input, ContextArgs contextArgs) {
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

		return new ServiceNowThresholdSender(input, contextArgs, addedByUser, viewName, fromTimestamp, toTimestamp,
				threshold, thresholdTimeframe, hitCount);
	}
}
