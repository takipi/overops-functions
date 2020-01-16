package com.takipi.udf.alerts.template.model;

import java.util.List;

public class Row {
	public static enum RowType {
		KV;
	}

	public RowType type;
	public List<String> items;
	public Options options;
}
