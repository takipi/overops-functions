package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.output.ReliabilityReport.ReliabilityReportItem;
import com.takipi.api.client.util.grafana.GrafanaDashboard;
import com.takipi.udf.quality.QualityGateType;

public class UniqueEventsGate extends QualityGate {
	private static final String FORMAT = "There have been a total of %d unique events (defined threshold is %d).";

	private final long amount;

	public UniqueEventsGate(long amount) {
		this.amount = amount;
	}

	@Override
	public QualityGateType getType() {
		return QualityGateType.UNIQUE_EVENTS;
	}

	@Override
	public String getDesc() {
		return "Unique(" + amount + ")";
	}

	@Override
	public GrafanaDashboard getGrafanaDashboard() {
		return GrafanaDashboard.UNIQUE_ERRORS;
	}

	@Override
	public String isBreached(ReliabilityReportItem report) {
		if (report.row.errorCount < amount) {
			return null;
		}

		return String.format(FORMAT, report.row.errorCount, amount);
	}
}
