package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.input.RegressionsInput;
import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.RegressionRow;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.functions.output.SeriesRow;
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
	protected String getRelevantSeriesType() {
		return ReliabilityReportInput.REGRESSION_SERIES;
	}

	@Override
	protected String isBreached(Series series) {
		int newEventsCount = 0;
		int newCriticalEventsCount = 0;

		for (SeriesRow row : series) {
			RegressionRow regressionRow = (RegressionRow) row;

			if (RegressionsInput.SEVERE_NEW_ISSUE_REGRESSIONS.equals(regressionRow.regression_type)) {
				newEventsCount++;
				newCriticalEventsCount++;
			} else if (RegressionsInput.NEW_ISSUE_REGRESSIONS.equals(regressionRow.regression_type)) {
				newEventsCount++;
			}
		}

		if (criticalOnly) {
			if (newCriticalEventsCount == 0) {
				return null;
			}

			return String.format(CRITICAL_FORMAT, newCriticalEventsCount);
		} else {
			if (newEventsCount == 0) {
				return null;
			}

			return String.format(ALL_FORMAT, newEventsCount, newCriticalEventsCount);
		}
	}
}
