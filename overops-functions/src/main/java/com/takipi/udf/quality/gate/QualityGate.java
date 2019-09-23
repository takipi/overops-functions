package com.takipi.udf.quality.gate;

import java.util.Collection;

import com.takipi.api.client.functions.output.Series;
import com.takipi.udf.quality.QualityGateType;

public abstract class QualityGate {
	public abstract QualityGateType getType();

	public abstract String getDesc();

	protected abstract String getRelevantSeriesType();

	protected abstract boolean isBreached(Series series);

	public boolean isBreached(Collection<Series> allSeries) {
		for (Series series : allSeries) {
			if (getRelevantSeriesType().equals(series.type)) {
				return isBreached(series);
			}
		}

		return false;
	}
}
