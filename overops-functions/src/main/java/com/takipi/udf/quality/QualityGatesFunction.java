package com.takipi.udf.quality;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.service.SummarizedService;
import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.ReliabilityReport;
import com.takipi.api.client.functions.output.ReliabilityReport.ReliabilityReportItem;
import com.takipi.api.client.request.alert.CustomAlertRequest;
import com.takipi.api.client.request.alert.CustomAlertRequest.AlertLink;
import com.takipi.api.client.request.functions.settings.GetFunctionSettingRequest;
import com.takipi.api.client.request.functions.settings.PutFunctionSettingRequest;
import com.takipi.api.client.result.EmptyResult;
import com.takipi.api.client.result.functions.FunctionSettingsResult;
import com.takipi.api.client.result.view.ViewResult;
import com.takipi.api.client.util.client.ClientUtil;
import com.takipi.api.client.util.grafana.GrafanaUrlBuilder;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.CollectionUtil;
import com.takipi.common.util.TimeUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.input.Input;
import com.takipi.udf.input.TimeInterval;
import com.takipi.udf.quality.gate.CriticalVolumeGate;
import com.takipi.udf.quality.gate.IncreasingErrorsGate;
import com.takipi.udf.quality.gate.NewEventsGate;
import com.takipi.udf.quality.gate.QualityGate;
import com.takipi.udf.quality.gate.SlowdownsGate;
import com.takipi.udf.quality.gate.UniqueEventsGate;
import com.takipi.udf.quality.gate.VolumeGate;

public class QualityGatesFunction {
	private static final String SETTINGS_SUFFIX = "-settings";

	private static final String ALERT_TITLE_TEMPLATE = "Quality gates in %s have been breached";
	private static final String ALERT_BODY_TEMPLATE = "The following quality gates have been breached between %s - %s (GMT)";

	public static String validateInput(String rawInput) {
		return parseQualityGatesInput(rawInput).toString();
	}

	static QualityGatesInput parseQualityGatesInput(String rawInput) {
		System.out.println("validateInput rawInput:" + rawInput);

		if (Strings.isNullOrEmpty(rawInput)) {
			throw new IllegalArgumentException("Input is empty");
		}

		QualityGatesInput input;

		try {
			input = QualityGatesInput.of(rawInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		if (input.period == null) {
			throw new IllegalArgumentException("'period' must not be empty");
		} else if (!input.period.isPositive()) {
			throw new IllegalArgumentException("'period' must be positive");
		}

		if (input.min_alert_interval == null) {
			throw new IllegalArgumentException("'min_alert_interval' must not be empty");
		} else if (!input.min_alert_interval.isPositive()) {
			throw new IllegalArgumentException("'min_alert_interval' must be positive");
		}

		if ((input.alert_new_events < 0) || (input.alert_new_events > 2)) {
			throw new IllegalArgumentException("'alert_new_events' has to be [0, 1, 2]");
		}

		if ((input.alert_increasing_events < 0) || (input.alert_increasing_events > 2)) {
			throw new IllegalArgumentException("'alert_increasing_events' has to be [0, 1, 2]");
		}

		if ((input.alert_slowdowns < 0) || (input.alert_slowdowns > 2)) {
			throw new IllegalArgumentException("'alert_slowdowns' has to be [0, 1, 2]");
		}

		if (input.min_unique_events < 0) {
			throw new IllegalArgumentException("'min_unique_events' can't be negative");
		}

		if (input.min_total_volume < 0) {
			throw new IllegalArgumentException("'min_total_volume' can't be negative");
		}

		if (input.min_critical_volume < 0) {
			throw new IllegalArgumentException("'min_critical_volume' can't be negative");
		}

		if (input.activeGates().isEmpty()) {
			throw new IllegalArgumentException("No quality gates defined");
		}

		return input;
	}

	public static void execute(String rawContextArgs, String rawInput) {
		System.out.println("execute:" + rawContextArgs);

		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		if (!args.viewValidate()) {
			throw new IllegalArgumentException("Invalid context args - " + rawContextArgs);
		}

		QualityGatesInput input = parseQualityGatesInput(rawInput);

		Collection<QualityGate> inputGates = input.activeGates();

		if (inputGates.isEmpty()) {
			return;
		}

		ApiClient apiClient = args.apiClient();

		String settingsKey = args.viewId + SETTINGS_SUFFIX;
		GetFunctionSettingRequest settingsRequest = GetFunctionSettingRequest.newBuilder().setServiceId(args.serviceId)
				.setLibraryId(args.libraryId).setFunctionId(args.functionId).setKey(settingsKey).build();

		String settingsStr = null;
		Response<FunctionSettingsResult> settingsResponse = apiClient.get(settingsRequest);

		if ((settingsResponse.isOK()) && (settingsResponse.data != null) && (settingsResponse.data.setting != null)) {
			settingsStr = settingsResponse.data.setting.value;
		}

		FunctionSettings settings = FunctionSettings.from(settingsStr);

		DateTime now = DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0);
		DateTime maxLastAlertTime = now.minusMinutes(input.min_alert_interval.asMinutes());

		Collection<QualityGate> activeGates = Lists.newArrayList();

		for (QualityGate gate : inputGates) {
			if (settings.canAlert(gate.getType(), maxLastAlertTime)) {
				activeGates.add(gate);
			}
		}

		if (activeGates.isEmpty()) {
			return;
		}

		ViewResult view = ViewUtil.getServiceView(apiClient, args.serviceId, args.viewId);

		if (view == null) {
			throw new IllegalStateException("Failed getting view " + args.viewId);
		}

		if (Strings.isNullOrEmpty(view.name)) {
			throw new IllegalStateException("Missing name for view " + args.viewId);
		}

		ReliabilityReportInput reportInput = new ReliabilityReportInput();

		reportInput.timeFilter = TimeUtil.getLastWindowTimeFilter(TimeUnit.MINUTES.toMillis(input.period.asMinutes()));
		reportInput.environments = args.serviceId;
		reportInput.view = view.name;
		reportInput.outputDrillDownSeries = true;
		reportInput.mode = ReliabilityReportInput.DEFAULT_REPORT;

		ReliabilityReport reliabilityReport = ReliabilityReport.execute(apiClient, reportInput);

		if (reliabilityReport == null) {
			throw new IllegalStateException("Failed getting report");
		}

		if (CollectionUtil.safeIsEmpty(reliabilityReport.items)) {
			throw new IllegalStateException("Missing data");
		}

		// Because we're in ReliabilityReportInput.DEFAULT_REPORT mode, we have just a
		// single report.
		//
		ReliabilityReportItem report = reliabilityReport.items.values().iterator().next();

		DateTime from = now.minusMinutes(input.period.asMinutes());
		SummarizedService env = ClientUtil.getEnvironment(apiClient, args.serviceId);
		String envName = ((env != null) ? env.name : null);

		Collection<AlertLink> links = Lists.newArrayList();

		for (QualityGate gate : activeGates) {
			String breachText = gate.isBreached(report);

			if (Strings.isNullOrEmpty(breachText)) {
				continue;
			}

			settings.setLastAlertDate(gate.getType(), now);

			GrafanaUrlBuilder urlBuilder = GrafanaUrlBuilder.create(args.grafanaHost, gate.getGrafanaDashboard())
					.withFrom(from).withTo(now);

			urlBuilder.withEnrivonment(envName, args.serviceId);

			if (view.filters != null) {
				urlBuilder.withApplications(view.filters.apps).withDeployments(view.filters.deployments)
						.withMachines(view.filters.servers);
			}

			String grafanaUrl = urlBuilder.buildUrl();

			links.add(AlertLink.create(breachText + " ", "Click here to view", null, grafanaUrl));
		}

		if (links.isEmpty()) {
			return;
		}

		DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(ISODateTimeFormat.date()).appendLiteral(' ')
				.append(ISODateTimeFormat.hourMinute()).toFormatter().withZoneUTC();

		CustomAlertRequest.Builder alertBuilder = CustomAlertRequest.newBuilder().setServiceId(args.serviceId)
				.setViewId(args.viewId)
				.setTitle(String.format(ALERT_TITLE_TEMPLATE,
						(Strings.isNullOrEmpty(envName) ? args.serviceId : envName)))
				.setBody(String.format(ALERT_BODY_TEMPLATE, from.toString(formatter), now.toString(formatter)));

		for (AlertLink link : links) {
			alertBuilder.addLink(link);
		}

		Response<EmptyResult> alertResponse = apiClient.post(alertBuilder.build());

		if (alertResponse.isBadResponse()) {
			System.err.println("Failed sending alert on quality gates");
		}

		String newSettingsStr = settings.serialize();

		PutFunctionSettingRequest putSettingsRequest = PutFunctionSettingRequest.newBuilder()
				.setServiceId(args.serviceId).setLibraryId(args.libraryId).setFunctionId(args.functionId)
				.setKey(settingsKey).setValue(newSettingsStr).build();

		Response<EmptyResult> putSettingsResponse = apiClient.put(putSettingsRequest);

		if (putSettingsResponse.isBadResponse()) {
			System.err.println("Failed saving new function settings");
		}
	}

	static class QualityGatesInput extends Input {
		public TimeInterval period; // Time period to look at.
		public TimeInterval min_alert_interval; // Minimal time between two consecutive alerts on same gate.

		public int alert_new_events; // 0 - No. 1 - Critical only. 2 - All.
		public int alert_increasing_events; // 0 - No. 1 - Critical only. 2 - All.
		public int alert_slowdowns; // 0 - No. 1 - Critical only. 2 - All.
		public int min_unique_events; // minimal unique events count to break quality gate. 0 for disabled.
		public int min_total_volume; // minimal total volume of events to break quality gate. 0 for disabled.
		public int min_critical_volume; // minimal total volume of critical events to break quality gate. 0 for
										// disabled.

		private QualityGatesInput(String raw) {
			super(raw);
		}

		@Override
		public String toString() {
			return String.format("QualityGates(Window = %s, %s)", period, activeGatesDesc());
		}

		private String activeGatesDesc() {
			Collection<QualityGate> gates = activeGates();

			if (gates.isEmpty()) {
				return "NONE";
			}

			StringBuilder sb = new StringBuilder();
			boolean first = true;

			for (QualityGate gate : gates) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}

				sb.append(gate.getDesc());
			}

			return sb.toString();
		}

		private boolean criticalOnly(int v) {
			return (v == 1);
		}

		public Collection<QualityGate> activeGates() {
			List<QualityGate> gates = Lists.newArrayList();

			if (alert_new_events > 0) {
				gates.add(new NewEventsGate(criticalOnly(alert_new_events)));
			}

			if (alert_increasing_events > 0) {
				gates.add(new IncreasingErrorsGate(criticalOnly(alert_increasing_events)));
			}

			if (alert_slowdowns > 0) {
				gates.add(new SlowdownsGate(criticalOnly(alert_slowdowns)));
			}

			if (min_unique_events > 0) {
				gates.add(new UniqueEventsGate(min_unique_events));
			}

			if (min_total_volume > 0) {
				gates.add(new VolumeGate(min_total_volume));
			}

			if (min_critical_volume > 0) {
				gates.add(new CriticalVolumeGate(min_critical_volume));
			}

			return gates;
		}

		static QualityGatesInput of(String raw) {
			return new QualityGatesInput(raw);
		}
	}

	static class FunctionSettings {
		private static final Gson GSON = new GsonBuilder().create();

		private Map<QualityGateType, String> lastAlertDates;

		public boolean canAlert(QualityGateType type, DateTime maxDate) {
			try {
				if (lastAlertDates == null) {
					return true;
				}

				String lastAlertDateStr = lastAlertDates.get(type);

				if (Strings.isNullOrEmpty(lastAlertDateStr)) {
					return true;
				}

				DateTime lastAlertDate = new DateTime(Long.valueOf(lastAlertDateStr));

				return (!maxDate.isBefore(lastAlertDate));
			} catch (Exception e) {
				System.err.println("Failed checking canAlert on " + type);
				return true;
			}
		}

		public void setLastAlertDate(QualityGateType type, DateTime date) {
			if (lastAlertDates == null) {
				lastAlertDates = Maps.newHashMap();
			}

			lastAlertDates.put(type, Long.toString(date.getMillis()));
		}

		public String serialize() {
			return GSON.toJson(this);
		}

		public static FunctionSettings from(String s) {
			try {
				if (Strings.isNullOrEmpty(s)) {
					return new FunctionSettings();
				}

				return GSON.fromJson(s, FunctionSettings.class);
			} catch (Exception e) {
				System.err.println("Failed parsing FunctionSettings");
				return new FunctionSettings();
			}
		}
	}
}
