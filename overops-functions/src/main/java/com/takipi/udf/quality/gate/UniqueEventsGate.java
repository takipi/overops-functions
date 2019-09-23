package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.Series;
import com.takipi.udf.quality.QualityGateType;

public class UniqueEventsGate extends QualityGate {

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
	protected String getRelevantSeriesType() {
		return ReliabilityReportInput.ERRORS_SERIES;
	}

	@Override
	protected boolean isBreached(Series series) {
		return (series.size() >= amount);
	}
}
