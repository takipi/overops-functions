package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.output.ReliabilityReport.ReliabilityReportItem;
import com.takipi.api.client.util.grafana.GrafanaDashboard;
import com.takipi.udf.quality.QualityGateType;

public class IncreasingErrorsGate extends QualityGate {
	private static final String ALL_FORMAT = "There's an increase in %d events, %d of which are critical.";
	private static final String CRITICAL_FORMAT = "There's an increase in %d critical events.";

	private final boolean criticalOnly;

	public IncreasingErrorsGate(boolean criticalOnly) {
		this.criticalOnly = criticalOnly;
	}

	@Override
	public QualityGateType getType() {
		return QualityGateType.INCREASING_EVENTS;
	}

	@Override
	public String getDesc() {
		return "Increasing Errors(" + (criticalOnly ? "critical" : "all") + ")";
	}

	@Override
	public GrafanaDashboard getGrafanaDashboard() {
		return GrafanaDashboard.INCREASING_ERRORS;
	}

	@Override
	public String isBreached(ReliabilityReportItem report) {
		int increasingCriticalEventsCount = report.geIncErrors(false, true).size();

		if (criticalOnly) {
			if (increasingCriticalEventsCount == 0) {
				return null;
			}

			return String.format(CRITICAL_FORMAT, increasingCriticalEventsCount);
		} else {
			int incresingEventsCount = report.geIncErrors(true, true).size();

			if (incresingEventsCount == 0) {
				return null;
			}

			return String.format(ALL_FORMAT, incresingEventsCount, increasingCriticalEventsCount);
		}
	}
}
