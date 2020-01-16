package com.takipi.udf.alerts.slack.format;

import com.takipi.udf.alerts.template.model.Options;
import com.takipi.udf.alerts.template.token.TokenizerUtil.Formatter;

public class SlackFormatter implements Formatter {
	private final Options options;

	private SlackFormatter(Options options) {
		this.options = options;
	}

	@Override
	public String format(String val) {
		if ((options == null) || (!options.boldFields)) {
			return val;
		}

		return "*" + val + "*";
	}

	public static Formatter of(Options options) {
		return new SlackFormatter(options);
	}
}
