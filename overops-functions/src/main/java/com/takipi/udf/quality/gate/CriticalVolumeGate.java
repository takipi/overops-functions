package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.output.ReliabilityReport.ReliabilityReportItem;
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
	public String isBreached(ReliabilityReportItem report) {
		if (report.row.failureVolume < volume) {
			return null;
		}

		return String.format(FORMAT, report.row.failureVolume, volume);
	}
}
