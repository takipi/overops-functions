package com.takipi.udf.alerts.template.model;

import java.util.List;

public class Body {
	public static enum PartType {
		STRING, TABLE, STACKTRACE, ACTION, TOP_CONTRIBUTORS
	}

	public static enum ActionType {
		VIEW_EVENT, DONT_ALERT_ON_EVENT
	}
	
	public static class Part {
		public PartType type;
		public Options options;

		// STRING
		public String text;

		// TABLE
		public List<Row> rows;
		
		// ACTION
		public ActionType actionType;
	}

	public Headline headline;
	public List<Part> parts;
	public Options options;
}
