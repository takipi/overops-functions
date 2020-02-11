package com.takipi.udf.alerts.util;

import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.takipi.api.client.ApiClient;
import com.takipi.api.client.request.event.EventRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.StringUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.util.StringPrettification;
import com.takipi.udf.util.url.DashboardUrlBuilder;
import com.takipi.udf.util.url.DashboardUrlBuilder.TimeframeType;
import com.takipi.udf.util.url.UrlBuilder;
import com.takipi.udf.util.url.UrlUtil;

public class AlertUtil {
	protected static final Logger logger = LoggerFactory.getLogger(AlertUtil.class);

	public static boolean isException(EventResult event) {
		return isException(event.type);
	}

	public static boolean isException(String type) {
		return (("Swallowed Exception".equals(type)) || ("Caught Exception".equals(type))
				|| ("Uncaught Exception".equals(type)));
	}

	public static boolean isLogEvent(EventResult event) {
		return isLogEvent(event.type);
	}

	public static boolean isLogEvent(String type) {
		return (("Logged Error".equals(type)) || ("Logged Warning".equals(type)));
	}

	public static String createEventTitle(EventResult event) {
		return createEventTitle(event, 0);
	}

	public static String createEventTitle(EventResult event, int maxLength) {
		String fullTitle;

		if (AlertUtil.isLogEvent(event)) {
			fullTitle = event.type;

			if (!StringUtil.isNullOrEmpty(event.message)) {
				fullTitle += ": \"" + event.message + "\"";
			}
		} else {
			fullTitle = event.name;
		}

		fullTitle = fullTitle.replace("\r", "").replace("\n", "").replace("\t", "");

		if (maxLength <= 0) {
			return fullTitle;
		}

		return StringPrettification.ellipsize(fullTitle, maxLength);
	}

	public static String generateMailAutoArchiveActionLink(String appHost, String serviceId, String exceptionName) {
		try {
			UrlBuilder builder = DashboardUrlBuilder.create(appHost).withServiceId(serviceId).withNav("archivemailitem")
					.toUrlBuilder();

			return builder.withParam("exception_class", UrlUtil.encodeUrl(exceptionName)).buildUrl();
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public static String buildLinkForView(String appHost, String serviceId, String viewName) {
		return buildLinkForView(appHost, serviceId, viewName, 0, 0);
	}

	public static String buildLinkForView(String appHost, String serviceId, String viewName, long fromTimestamp,
			long toTimestamp) {
		DashboardUrlBuilder builder = DashboardUrlBuilder.create(appHost).withServiceId(serviceId)
				.withViewName(viewName);

		if ((fromTimestamp != 0) && (toTimestamp != 0)) {
			builder.withTimeframe(TimeframeType.CUSTOM).withFrom(fromTimestamp).withTo(toTimestamp);
		}

		return builder.buildUrl();
	}

	public static String buildLinkForSettings(String appHost, String serviceId) {
		return DashboardUrlBuilder.create(appHost).withServiceId(serviceId).withNav("alertset").buildUrl();
	}

	public static String formatCountable(long count, String countable) {
		return formatCountable(count, countable, true);
	}

	public static String formatCountable(long count, String countable, boolean showNumberInResult) {
		StringBuilder sb = new StringBuilder();

		if (showNumberInResult) {
			sb.append(formatNumberWithCommas(count));
			sb.append(' ');
		}

		sb.append(countable);

		if (count != 1) {
			sb.append('s');
		}

		return sb.toString();
	}

	public static String formatNumberWithCommas(long number) {
		try {
			return NumberFormat.getNumberInstance(Locale.US).format(number);
		} catch (Exception e) {
			return Long.toString(number);
		}
	}

	public static String formatEventData(String eventData, int maxChars) {
		int charsFromEnd = ((eventData.endsWith("\"")) ? 1 : 0); // to account for the '"' at the end
		String shortString = StringPrettification.minimizeString(eventData, maxChars, charsFromEnd);
		String result = formatText(shortString);

		return result;
	}

	public static String formatText(String text) {
		String s = text.replace('*', '-');

		return s.replaceAll("`|_|~|\n", "-");
	}

	public static EventResult getEvent(ContextArgs args) {
		if (!args.eventValidate()) {
			return null;
		}

		ApiClient apiClient = args.apiClient();

		EventRequest eventRequest = EventRequest.newBuilder().setServiceId(args.serviceId).setEventId(args.eventId)
				.setIncludeStacktrace(true).build();

		Response<EventResult> eventResult = apiClient.get(eventRequest);

		if ((eventResult.isBadResponse()) || (eventResult.data == null)) {
			logger.error("Can't get event {}/{}", args.serviceId, args.eventId);
			return null;
		}

		return eventResult.data;
	}
}
