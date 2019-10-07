package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.input.RegressionsInput;
import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.RegressionRow;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.functions.output.SeriesRow;
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
	protected String getRelevantSeriesType() {
		return ReliabilityReportInput.REGRESSION_SERIES;
	}

	@Override
	protected String isBreached(Series series) {
		int incresingEventsCount = 0;
		int increasingCriticalEventsCount = 0;

		for (SeriesRow row : series) {
			RegressionRow regressionRow = (RegressionRow) row;

			if (RegressionsInput.SEVERE_INC_ERROR_REGRESSIONS.equals(regressionRow.regression_type)) {
				incresingEventsCount++;
				increasingCriticalEventsCount++;
			} else if (RegressionsInput.INC_ERROR_REGRESSIONS.equals(regressionRow.regression_type)) {
				incresingEventsCount++;
			}
		}

		if (criticalOnly) {
			if (increasingCriticalEventsCount == 0) {
				return null;
			}

			return String.format(CRITICAL_FORMAT, increasingCriticalEventsCount);
		} else {
			if (incresingEventsCount == 0) {
				return null;
			}

			return String.format(ALL_FORMAT, incresingEventsCount, increasingCriticalEventsCount);
		}
	}
}
