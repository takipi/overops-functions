package com.takipi.udf.alerts.template.token;

import com.google.common.base.Strings;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.util.AlertUtil;

public class EventTokenizer extends Tokenizer {

	public static enum EventTokenType {
		EventType, EventFullMessage, Application, Deployment, Server, Location, FirstSeen, ResolvedTime,
	}

	private final EventResult event;

	EventTokenizer(ContextArgs contextArgs, EventResult event) {
		super(contextArgs);

		this.event = event;
	}

	@Override
	public String get(String token, String defaultValue) {
		EventTokenType type = safeEnum(EventTokenType.class, token, true);

		if (type == null) {
			return super.get(token, defaultValue);
		}

		String value = getEventToken(type);

		return (Strings.isNullOrEmpty(value) ? defaultValue : value);
	}

	public String getEventToken(EventTokenType token) {
		switch (token) {
		case Application:
			return event.introduced_by_application;
		case Deployment:
			return event.introduced_by;
		case EventFullMessage:
			return AlertUtil.createEventTitle(event);
		case EventType:
			return event.type;
		case FirstSeen:
			return event.first_seen;
		case Location:
			return event.summary.replaceFirst(event.name + " in ", ""); // TODO - return location in api directly.
		case Server:
			return event.introduced_by_server;
		case ResolvedTime:
			return "RESOLVED_TIME";	// TODO - have this available
		}

		return null;
	}

	public static EventTokenizer from(ContextArgs contextArgs, EventResult event) {
		return new EventTokenizer(contextArgs, event);
	}
}
