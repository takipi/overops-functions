package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.EventRow;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.functions.output.SeriesRow;
import com.takipi.udf.quality.QualityGateType;

public class CriticalVolumeGate extends QualityGate {

	private final long volume;

	public CriticalVolumeGate(long volume) {
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
	protected String getRelevantSeriesType() {
		return ReliabilityReportInput.FAILURES_SERIES;
	}

	@Override
	protected boolean isBreached(Series series) {
		long totalVolume = 0;

		for (SeriesRow row : series) {
			EventRow eventRow = (EventRow) row;

			totalVolume += eventRow.hits;
		}

		return (totalVolume >= volume);
	}
}
