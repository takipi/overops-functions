package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.ReliabilityReportRow;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.util.grafana.GrafanaDashboard;
import com.takipi.udf.quality.QualityGateType;

public class VolumeGate extends QualityGate {
	private static final String FORMAT = "There have been a total of %d events (defined threshold is %d).";

	private final long volume;

	public VolumeGate(long volume) {
		this.volume = volume;
	}

	@Override
	public QualityGateType getType() {
		return QualityGateType.TOTAL_VOLUME;
	}

	@Override
	public String getDesc() {
		return "Volume(" + volume + ")";
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

		if (report.errorVolume < volume) {
			return null;
		}

		return String.format(FORMAT, report.errorVolume, volume);
	}
}
