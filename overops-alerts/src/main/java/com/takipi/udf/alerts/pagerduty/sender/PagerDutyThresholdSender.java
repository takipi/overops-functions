package com.takipi.udf.alerts.pagerduty.sender;

import com.takipi.common.util.StringUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.pagerduty.PagerDutyFunction.PagerDutyInput;
import com.takipi.udf.alerts.pagerduty.message.Details;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.alerts.util.SourceConstants;

public class PagerDutyThresholdSender extends PagerDutyTimeframeSender {
	private static final String EVENT_DESC_FORMAT = "UDF: Events in view %s have occurred more than %s in the past %d minutes in %s (alert added by %s)";

	private final long threshold;
	private final int thresholdTimeframe;
	private final long hitCount;

	private PagerDutyThresholdSender(PagerDutyInput input, ContextArgs contextArgs, String addedByUser, String viewName,
			long fromTimestamp, long toTimestamp, long threshold, int thresholdTimeframe, long hitCount) {
		super(input, contextArgs, addedByUser, viewName, fromTimestamp, toTimestamp);

		this.threshold = threshold;
		this.thresholdTimeframe = thresholdTimeframe;
		this.hitCount = hitCount;
	}

	@Override
	protected String createDescription() {
		String formattedTimesString = AlertUtil.formatCountable(threshold, "time", false);

		return String.format(EVENT_DESC_FORMAT, viewName, AlertUtil.formatNumberWithCommas(threshold),
				formattedTimesString, thresholdTimeframe, contextArgs.serviceName, addedByUser);
	}

	@Override
	protected int getTaleSource() {
		return SourceConstants.SOURCE_PAGERDUTY_THRESHOLD_MESSAGE;
	}

	@Override
	protected String getInternalDescription() {
		return "New threshold alert for " + viewName;
	}

	@Override
	protected Details.Builder getDetailsBuilder() {
		return super.getDetailsBuilder().setThreshold(Long.toString(threshold)).setOccurences(Long.toString(hitCount));
	}

	public static PagerDutySender create(PagerDutyInput input, ContextArgs contextArgs) {
		if (!contextArgs.viewValidate()) {
			return null;
		}

		long threshold = contextArgs.longData("threshold");
		int thresholdTimeframe = contextArgs.intData("threshold_timeframe");
		long hitCount = contextArgs.longData("hit_count");
		String viewName = contextArgs.data("view_name");

		if ((threshold < 0) || (thresholdTimeframe < 0) || (hitCount < 0) || (StringUtil.isNullOrEmpty(viewName))) {
			logger.error("Can't build sender with incomplete data");
			return null;
		}

		String addedByUser = contextArgs.data("added_by_user", "Unknown");
		long fromTimestamp = contextArgs.longData("from_timestamp");
		long toTimestamp = contextArgs.longData("to_timestamp");

		return new PagerDutyThresholdSender(input, contextArgs, addedByUser, viewName, fromTimestamp, toTimestamp,
				threshold, thresholdTimeframe, hitCount);
	}
}
