package com.takipi.udf.alerts.pagerduty;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PagerDutyConsts {
	public static final Gson GSON = new GsonBuilder()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	
	public static final String TRIGGER_EVENT_TYPE = "trigger";
	public static final String EVENT_CLIENT = "OverOps";
}
