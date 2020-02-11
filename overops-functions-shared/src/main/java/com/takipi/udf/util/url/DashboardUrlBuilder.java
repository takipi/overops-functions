package com.takipi.udf.util.url;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.takipi.common.util.StringUtil;

public class DashboardUrlBuilder {

	public enum TimeframeType
	{
		LAST_HOUR("Last-hour"),
		LAST_DAY("Last-day"),
		LAST_WEEK("Last-week"),
		LAST_MONTH("Last-month"),
		LAST_3_HOURS("Last-3-hours"),
		LAST_6_HOURS("Last-6-hours"),
		LAST_12_HOURS("Last-12-hours"),
		LAST_24_HOURS("Last-24-hours"),
		LAST_48_HOURS("Last-48-hours"),
		LAST_7_DAYS("Last-7-days"),
		LAST_14_DAYS("Last-14-days"),
		LAST_30_DAYS("Last-30-days"),
		ONBOARDING("Onboarding"),
		CUSTOM("Custom");
		
		public final String stringValue;
		
		private TimeframeType(String stringValue)
		{
			this.stringValue = stringValue;
		}
	}

	private static final String KEY_PARAMETER = "key";
	private static final String VIEW_PARAMETER = "view";
	private static final String TIMEFRAME_PARAMETER = "timeframe";
	private static final String FROM_PARAMETER = "from";
	private static final String TO_PARAMETER = "to";
	private static final String NAV_PARAMETER = "nav";

	private String serviceId;
	private String viewName;
	private TimeframeType timeframe;
	private long from;
	private long to;
	private String nav;

	private final String appHost;

	private DashboardUrlBuilder(String appHost) {
		this.appHost = appHost;
	}

	public DashboardUrlBuilder withServiceId(String serviceId) {
		this.serviceId = serviceId;

		return this;
	}

	public DashboardUrlBuilder withViewName(String viewName) {
		this.viewName = viewName;

		return this;
	}

	public DashboardUrlBuilder withTimeframe(TimeframeType timeframe) {
		this.timeframe = timeframe;

		return this;
	}

	public DashboardUrlBuilder withFrom(long from) {
		this.from = from;

		return this;
	}

	public DashboardUrlBuilder withTo(long to) {
		this.to = to;

		return this;
	}

	public DashboardUrlBuilder withNav(String nav) {
		this.nav = nav;

		return this;
	}

	private boolean validTimeframe() {
		if (timeframe == null) {
			return false;
		}

		if (timeframe != TimeframeType.CUSTOM) {
			return true;
		}

		return ((from != 0) && (to != 0) && (from < to));
	}

	public UrlBuilder toUrlBuilder() {
		UrlBuilder builder = UrlBuilder.create(UrlUtil.getDashboardUrl(appHost));

		if (!StringUtil.isNullOrEmpty(serviceId)) {
			builder.withParam(KEY_PARAMETER, serviceId);
		}

		if (!StringUtil.isNullOrEmpty(viewName)) {
			builder.withParam(VIEW_PARAMETER, viewName);
		}

		if (validTimeframe()) {
			builder.withParam(TIMEFRAME_PARAMETER, timeframe.stringValue);

			if (timeframe == TimeframeType.CUSTOM) {
				builder.withParam(FROM_PARAMETER, formatDate(from));
				builder.withParam(TO_PARAMETER, formatDate(to));
			}
		}

		if (!StringUtil.isNullOrEmpty(nav)) {
			builder.withParam(NAV_PARAMETER, nav);
		}

		return builder;
	}

	public String buildUrl() {
		return toUrlBuilder().buildUrl();
	}

	private static String formatDate(long timestamp) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MMMMM-yy-HH-mmZ");

		Date date = new Date(timestamp);
		String formattedDate = formatter.format(date);

		return formattedDate.toUpperCase();
	}

	public static DashboardUrlBuilder create(String appHost) {
		return new DashboardUrlBuilder(appHost);
	}
}
