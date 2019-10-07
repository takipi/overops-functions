package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.ReliabilityReportRow;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.util.grafana.GrafanaDashboard;
import com.takipi.udf.quality.QualityGateType;

public class CriticalVolumeGate extends QualityGate {
	private static final String FORMAT = "There have been a total of %d critical events (defined threshold is %d).";

	private final long volume;

	public CriticalVolumeGate(long volume) {
		this.volume = volume;
	}

	@Override
	public QualityGateType getType() {
		return QualityGateType.CRITICAL_EVENTS_VOLUME;
	}

	@Override
	public String getDesc() {
		return "Critical Volume(" + volume + ")";
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

		if (report.failureVolume < volume) {
			return null;
		}

		return String.format(FORMAT, report.failureVolume, volume);
	}
}
