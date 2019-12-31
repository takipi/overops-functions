package com.takipi.udf.util.url;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

import com.google.common.base.Strings;
import com.takipi.api.core.consts.ApiConstants;

public class UrlUtil {
	public static final String INDEX = "index.html";
	public static final String TALE = "tale.html";

	public static String getPageUrl(String appHost, String page) {
		if (appHost.endsWith("/")) {
			return appHost + page;
		}

		return appHost + '/' + page;
	}

	public static String getDashboardUrl(String appHost) {
		return getPageUrl(appHost, INDEX);
	}

	public static String getTaleUrl(String appHost) {
		return getPageUrl(appHost, TALE);
	}

	public static String encodeSnapshotParam(String serviceId, String eventId, int snapshotId)
			throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();

		builder.append(serviceId);
		builder.append('#');
		builder.append(eventId);
		builder.append('#');
		builder.append(snapshotId);

		return encodeUrl(builder.toString());
	}

	public static String encodeUrl(String str) throws UnsupportedEncodingException {
		byte[] strBytes = str.getBytes(ApiConstants.UTF8_ENCODING);

		String base64Str = Base64.encodeBase64String(strBytes);
		base64Str = base64Str.replace('/', '.').replace('+', '_').replace('=', '-');

		return base64Str;
	}

	public static String buildTaleUrl(String appHost, String serviceId, String eventId, int snapshotId, int source) {
		return buildTaleUrl(appHost, serviceId, eventId, snapshotId, source, false, null, 0);
	}

	public static String buildTaleUrl(String appHost, String serviceId, String eventId, int snapshotId, int source,
			int secondsAgo) {
		return buildTaleUrl(appHost, serviceId, eventId, snapshotId, source, false, null, secondsAgo);
	}

	public static String buildTaleUrl(String appHost, String serviceId, String eventId, int snapshotId, int source,
			boolean allowFallback) {
		return buildTaleUrl(appHost, serviceId, eventId, snapshotId, source, allowFallback, null, 0);
	}

	public static String buildTaleUrl(String appHost, String serviceId, String eventId, int snapshotId, int source,
			String navigation) {
		return buildTaleUrl(appHost, serviceId, eventId, snapshotId, source, false, navigation, 0);
	}

	public static String buildTaleUrl(String appHost, String serviceId, String eventId, int snapshotId, int source,
			boolean allowFallback, String navigation, int secondsAgo) {
		try {
			UrlBuilder builder = UrlBuilder.create(getTaleUrl(appHost))
					.withParam("event", encodeSnapshotParam(serviceId, eventId, snapshotId))
					.withParam("source", String.valueOf(source));

			if (!Strings.isNullOrEmpty(navigation)) {
				builder.withParam("nav", navigation);
			}

			if (allowFallback) {
				builder.withParam("fb", "true");
			}

			if (secondsAgo > 0) {
				builder.withParam("sa", String.valueOf(secondsAgo));
			}

			return builder.buildUrl();
		} catch (Exception e) {
			UrlBuilder builder = UrlBuilder.create(getTaleUrl(appHost)).withParam("serviceId", serviceId)
					.withParam("requestId", eventId).withParam("hitId", String.valueOf(snapshotId))
					.withParam("source", String.valueOf(source));

			return builder.buildUrl();
		}
	}
}
