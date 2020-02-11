package com.takipi.udf.util.url;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;

import com.takipi.api.core.consts.ApiConstants;
import com.takipi.common.util.CollectionUtil;
import com.takipi.common.util.StringUtil;

public class UrlBuilder {
	private final String baseUrl;
	private final Map<String, String> urlParams;
	private final Map<String, Collection<String>> urlListParams;

	private UrlBuilder(String baseUrl) {
		this.baseUrl = baseUrl;
		this.urlParams = new HashMap<>();
		this.urlListParams = new HashMap<>();
	}

	public UrlBuilder withParam(String param, String value) {
		if (StringUtil.isNullOrEmpty(value)) {
			urlParams.remove(param);

			return this;
		}

		urlParams.put(param, value);

		return this;
	}

	public UrlBuilder withParam(String param, DateTime value) {
		if (value == null) {
			urlParams.remove(param);

			return this;
		}

		urlParams.put(param, String.valueOf(value.getMillis()));

		return this;
	}

	public UrlBuilder withListParam(String param, String value) {
		if (StringUtil.isNullOrEmpty(value)) {
			return this;
		}

		Collection<String> listParams = urlListParams.get(param);

		if (listParams == null) {
			listParams = new HashSet<>();
			urlListParams.put(param, listParams);
		}

		listParams.add(value);

		return this;
	}

	public UrlBuilder withListParams(String param, Collection<String> values) {
		if (CollectionUtil.safeIsEmpty(values)) {
			return this;
		}

		for (String value : values) {
			withListParam(param, value);
		}

		return this;
	}

	public UrlBuilder withoutListParam(String param, String value) {
		if (StringUtil.isNullOrEmpty(value)) {
			return this;
		}

		Collection<String> listParams = urlListParams.get(param);

		if (listParams == null) {
			return this;
		}

		listParams.remove(value);

		return this;
	}

	public String buildUrl() {
		StringBuilder builder = new StringBuilder();

		builder.append(baseUrl);

		boolean addedQ = (baseUrl.indexOf('?') >= 0);

		for (Map.Entry<String, String> entry : urlParams.entrySet()) {
			String param = entry.getKey();
			String value = safeUrlEncode(entry.getValue());

			if (!addedQ) {
				builder.append('?');
				addedQ = true;
			} else {
				builder.append('&');
			}

			builder.append(param);

			if (!StringUtil.isNullOrEmpty(value)) {
				builder.append('=');
				builder.append(value);
			}
		}

		for (Entry<String, Collection<String>> entry : urlListParams.entrySet()) {
			String param = entry.getKey();
			Collection<String> values = entry.getValue();

			for (String rawValue : values) {
				String value = safeUrlEncode(rawValue);

				if (!StringUtil.isNullOrEmpty(value)) {
					if (!addedQ) {
						builder.append('?');
						addedQ = true;
					} else {
						builder.append('&');
					}

					builder.append(param);
					builder.append('=');
					builder.append(value);
				}
			}
		}

		return builder.toString();
	}

	private static String safeUrlEncode(String value) {
		if (StringUtil.isNullOrEmpty(value)) {
			return value;
		}

		try {
			return URLEncoder.encode(value, ApiConstants.UTF8_ENCODING);
		} catch (UnsupportedEncodingException e) {
			return value;
		}
	}

	public static UrlBuilder create(String baseUrl) {
		return new UrlBuilder(baseUrl);
	}
}
