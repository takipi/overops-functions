package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.output.ReliabilityReport.ReliabilityReportItem;
import com.takipi.api.client.util.grafana.GrafanaDashboard;
import com.takipi.udf.quality.QualityGateType;

public class NewEventsGate extends QualityGate {
	private static final String ALL_FORMAT = "There are %d new events, %d of which are critical.";
	private static final String CRITICAL_FORMAT = "There are %d new critical events.";

	private final boolean criticalOnly;

	public NewEventsGate(boolean criticalOnly) {
		this.criticalOnly = criticalOnly;
	}

	@Override
	public QualityGateType getType() {
		return QualityGateType.NEW_EVENTS;
	}

	@Override
	public String getDesc() {
		return "New Events(" + (criticalOnly ? "critical" : "all") + ")";
	}

	@Override
	public GrafanaDashboard getGrafanaDashboard() {
		return GrafanaDashboard.NEW_ERRORS;
	}

	@Override
	public String isBreached(ReliabilityReportItem report) {
		int newCriticalEventsCount = report.getNewErrors(false, true).size();

		if (criticalOnly) {
			if (newCriticalEventsCount == 0) {
				return null;
			}

			return String.format(CRITICAL_FORMAT, newCriticalEventsCount);
		} else {
			int newEventsCount = report.getNewErrors(true, true).size();

			if (newEventsCount == 0) {
				return null;
			}

			return String.format(ALL_FORMAT, newEventsCount, newCriticalEventsCount);
		}
	}
}
