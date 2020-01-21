package com.takipi.udf.alerts.template.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.takipi.udf.ContextArgs;

public class Tokenizer {
	public static enum TokenType {
		ServiceName, ViewName, AlertOwner
	}

	protected static final Logger logger = LoggerFactory.getLogger(Tokenizer.class);

	protected final ContextArgs contextArgs;

	Tokenizer(ContextArgs contextArgs) {
		this.contextArgs = contextArgs;
	}

	public String get(String token) {
		return get(token, null);
	}

	public String get(String token, String defaultValue) {
		TokenType type = safeEnum(TokenType.class, token, false);

		if (type == null) {
			return defaultValue;
		}

		String value = getToken(type);

		return (Strings.isNullOrEmpty(value) ? defaultValue : value);
	}

	public String getToken(TokenType token) {
		switch (token) {
		case ServiceName:
			return contextArgs.serviceName;
		case ViewName:
			return contextArgs.data("view_name");
		case AlertOwner:
			return contextArgs.data("added_by_user", "Unknown");
		}

		return null;
	}

	protected static <E extends Enum<E>> E safeEnum(Class<E> clazz, String s, boolean silent) {
		try {
			return Enum.valueOf(clazz, s);
		} catch (Exception e) {
			if (!silent) {
				logger.error("Unknown token type {}.", s);
			}
			return null;
		}
	}

	public static Tokenizer from(ContextArgs contextArgs) {
		return new Tokenizer(contextArgs);
	}
}
