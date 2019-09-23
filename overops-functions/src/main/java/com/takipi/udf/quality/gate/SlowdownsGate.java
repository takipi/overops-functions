package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.input.BaseEventVolumeInput;
import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.functions.output.SeriesRow;
import com.takipi.api.client.functions.output.TransactionRow;
import com.takipi.udf.quality.QualityGateType;

public class SlowdownsGate extends QualityGate {

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
	protected String getRelevantSeriesType() {
		return ReliabilityReportInput.SLOWDOWN_SERIES;
	}

	@Override
	protected boolean isBreached(Series series) {
		for (SeriesRow row : series) {
			TransactionRow transactionRow = (TransactionRow) row;

			if (isSlowTransaction(transactionRow)) {
				return true;
			}
		}

		return false;
	}

	private boolean isSlowTransaction(TransactionRow transactionRow) {
		return ((transactionRow.slow_state == BaseEventVolumeInput.CRITICAL_ORDINAL)
				|| ((!criticalOnly) && (transactionRow.slow_state == BaseEventVolumeInput.SLOWING_ORDINAL)));
	}
}
