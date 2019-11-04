package com.takipi.udf.quality.gate;

import com.takipi.api.client.functions.output.ReliabilityReport.ReliabilityReportItem;
import com.takipi.api.client.util.grafana.GrafanaDashboard;
import com.takipi.udf.quality.QualityGateType;

public abstract class QualityGate {
	public abstract QualityGateType getType();

	public abstract String getDesc();

	public abstract GrafanaDashboard getGrafanaDashboard();

	public abstract String isBreached(ReliabilityReportItem report);
}
