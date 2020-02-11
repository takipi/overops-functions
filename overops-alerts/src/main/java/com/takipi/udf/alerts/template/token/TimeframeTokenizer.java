package com.takipi.udf.alerts.template.token;

import java.util.Date;

import com.takipi.common.util.StringUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.util.DateUtil;

public abstract class TimeframeTokenizer extends Tokenizer {

	public static enum TimeframeTokenType {
		FromTimeStamp, ToTimestamp, Timeframe,
	}

	TimeframeTokenizer(ContextArgs contextArgs) {
		super(contextArgs);
	}

	@Override
	public String get(String token, String defaultValue) {
		TimeframeTokenType type = safeEnum(TimeframeTokenType.class, token, true);

		if (type == null) {
			return super.get(token, defaultValue);
		}

		String value = getTimeframeToken(type);

		return (StringUtil.isNullOrEmpty(value) ? defaultValue : value);
	}

	public String getTimeframeToken(TimeframeTokenType token) {
		switch (token) {
		case FromTimeStamp:
			return Long.toString(contextArgs.longData("from_timestamp"));
		case ToTimestamp:
			return Long.toString(contextArgs.longData("to_timestamp"));
		case Timeframe:

			Date fromDate = DateUtil.fromMillis(contextArgs.longData("from_timestamp"));
			Date toDate = DateUtil.fromMillis(contextArgs.longData("to_timestamp"));

			return DateUtil.toTimeStringAmPmNoSeconds(fromDate) + " - " + DateUtil.toTimeStringAmPmNoSeconds(toDate)
					+ " (GMT)";
		}

		return null;
	}
}
