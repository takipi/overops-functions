package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.input.BaseEventVolumeInput;
import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.functions.output.SeriesRow;
import com.takipi.api.client.functions.output.TransactionRow;
import com.takipi.api.client.util.grafana.GrafanaDashboard;
import com.takipi.udf.quality.QualityGateType;

public class SlowdownsGate extends QualityGate {
	private static final String ALL_FORMAT = "There are %d slowdowns, %d of which are critical.";
	private static final String CRITICAL_FORMAT = "There are %d critical slowdowns.";

	private final boolean criticalOnly;

	public SlowdownsGate(boolean criticalOnly) {
		this.criticalOnly = criticalOnly;
	}

	@Override
	public QualityGateType getType() {
		return QualityGateType.SLOWDOWNS;
	}

	@Override
	public String getDesc() {
		return "Slowdowns(" + (criticalOnly ? "critical" : "all") + ")";
	}

	@Override
	public GrafanaDashboard getGrafanaDashboard() {
		return GrafanaDashboard.SLOWDOWNS;
	}

	@Override
	protected String getRelevantSeriesType() {
		return ReliabilityReportInput.SLOWDOWN_SERIES;
	}

	@Override
	protected String isBreached(Series<SeriesRow> series) {
		int slowdownsCount = 0;
		int criticalSlowdownsCount = 0;

		for (SeriesRow row : series) {
			TransactionRow transactionRow = (TransactionRow) row;

			if (BaseEventVolumeInput.CRITICAL_ORDINAL == transactionRow.slow_state) {
				slowdownsCount++;
				criticalSlowdownsCount++;
			} else if (BaseEventVolumeInput.SLOWING_ORDINAL == transactionRow.slow_state) {
				slowdownsCount++;
			}
		}

		if (criticalOnly) {
			if (criticalSlowdownsCount == 0) {
				return null;
			}

			return String.format(CRITICAL_FORMAT, criticalSlowdownsCount);
		} else {
			if (slowdownsCount == 0) {
				return null;
			}

			return String.format(ALL_FORMAT, slowdownsCount, criticalSlowdownsCount);
		}
	}
}
