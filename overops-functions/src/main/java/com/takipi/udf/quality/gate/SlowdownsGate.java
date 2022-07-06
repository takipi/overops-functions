package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.output.ReliabilityReport.ReliabilityReportItem;
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
	public String isBreached(ReliabilityReportItem report) {
		int criticalSlowdownsCount = report.getSlowdowns(false, true).size();

		if (criticalOnly) {
			if (criticalSlowdownsCount == 0) {
				return null;
			}

			return String.format(CRITICAL_FORMAT, criticalSlowdownsCount);
		} else {
			int slowdownsCount = report.getSlowdowns(true, true).size();

			if (slowdownsCount == 0) {
				return null;
			}

			return String.format(ALL_FORMAT, slowdownsCount, criticalSlowdownsCount);
		}
	}
}
