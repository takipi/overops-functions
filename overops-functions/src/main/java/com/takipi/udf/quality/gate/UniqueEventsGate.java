package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.ReliabilityReportRow;
import com.takipi.api.client.functions.output.Series;
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
	protected String getRelevantSeriesType() {
		return ReliabilityReportInput.RELIABITY_REPORT_SERIES;
	}

	@Override
	protected String isBreached(Series series) {
		if (series.size() == 0) {
			return null;
		}

		ReliabilityReportRow report = (ReliabilityReportRow) series.iterator().next();

		if (report.errorCount < amount) {
			return null;
		}

		return String.format(FORMAT, report.errorCount, amount);
	}
}
