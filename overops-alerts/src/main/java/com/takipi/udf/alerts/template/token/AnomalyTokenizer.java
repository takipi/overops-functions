package com.takipi.udf.alerts.template.token;

import com.google.common.base.Strings;
import com.takipi.udf.ContextArgs;

public class AnomalyTokenizer extends TimeframeTokenizer {

	public static enum AnomalyTokenType {
		AnomalyReason, AnomalyTimeframe
	}

	private AnomalyTokenizer(ContextArgs contextArgs) {
		super(contextArgs);
	}

	@Override
	public String get(String token, String defaultValue) {
		AnomalyTokenType type = safeEnum(AnomalyTokenType.class, token, true);

		if (type == null) {
			return super.get(token, defaultValue);
		}

		String value = getAnomalyToken(type);

		return (Strings.isNullOrEmpty(value) ? defaultValue : value);
	}

	public String getAnomalyToken(AnomalyTokenType token) {
		switch (token) {
		case AnomalyReason:
			return contextArgs.data("anomaly_reason");
		case AnomalyTimeframe:
			return contextArgs.data("anomaly_timeframe");
		}

		return null;
	}

	public static AnomalyTokenizer from(ContextArgs contextArgs) {
		return new AnomalyTokenizer(contextArgs);
	}
}
