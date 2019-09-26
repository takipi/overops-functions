package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.ReliabilityReportRow;
import com.takipi.api.client.functions.output.Series;
import com.takipi.udf.quality.QualityGateType;

public class CriticalVolumeGate extends QualityGate {

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
	protected String getRelevantSeriesType() {
		return ReliabilityReportInput.RELIABITY_REPORT_SERIES;
	}

	@Override
	protected boolean isBreached(Series series) {
		if (series.size() == 0) {
			return false;
		}

		ReliabilityReportRow report = (ReliabilityReportRow) series.iterator().next();

		return (report.failureVolume >= volume);
	}
}
