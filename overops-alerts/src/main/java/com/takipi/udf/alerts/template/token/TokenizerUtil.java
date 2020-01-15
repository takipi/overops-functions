package com.takipi.udf.alerts.template.token;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenizerUtil {
	public static interface Formatter {
		public String format(String val);
	}

	private static final Formatter identity = new Formatter() {
		@Override
		public String format(String val) {
			return val;
		}
	};

	private static final Pattern TOKEN_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

	public static String work(Tokenizer tokenizer, String value) {
		return work(tokenizer, value, identity);
	}

	public static String work(Tokenizer tokenizer, String s, Formatter formatter) {
		String result = s;
		Matcher matcher = TOKEN_PATTERN.matcher(result);

		while (matcher.find()) {
			String token = matcher.group(1);
			String value = tokenizer.get(token);

			value = ((value != null) ? formatter.format(value) : "");

			result = result.substring(0, matcher.start()) + value + result.substring(matcher.end());
			matcher = TOKEN_PATTERN.matcher(result);
		}

		return result;
	}
}
