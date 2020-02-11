package com.takipi.udf.alerts.template.token;

import com.takipi.common.util.StringUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.util.AlertUtil;

public class ThresholdTokenizer extends TimeframeTokenizer {

	public static enum ThresholdTokenType {
		Threshold, ThresholdTime, HitCount
	}

	long threshold = contextArgs.longData("threshold");
	int thresholdTimeframe = contextArgs.intData("threshold_timeframe");
	long hitCount = contextArgs.longData("hit_count");

	private ThresholdTokenizer(ContextArgs contextArgs) {
		super(contextArgs);
	}

	@Override
	public String get(String token, String defaultValue) {
		ThresholdTokenType type = safeEnum(ThresholdTokenType.class, token, true);

		if (type == null) {
			return super.get(token, defaultValue);
		}

		String value = getThresholdToken(type);

		return (StringUtil.isNullOrEmpty(value) ? defaultValue : value);
	}

	public String getThresholdToken(ThresholdTokenType token) {
		switch (token) {
		case HitCount:
			long hitCount = contextArgs.longData("hit_count");
			return AlertUtil.formatNumberWithCommas(hitCount);
		case Threshold:
			long threshold = contextArgs.longData("threshold");
			return AlertUtil.formatNumberWithCommas(threshold);
		case ThresholdTime:
			return Integer.toString(contextArgs.intData("threshold_timeframe"));
		}

		return null;
	}

	public static ThresholdTokenizer from(ContextArgs contextArgs) {
		return new ThresholdTokenizer(contextArgs);
	}
}
