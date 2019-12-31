package com.takipi.udf.alerts.slack.message;

import java.lang.reflect.Field;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;

public class NamingStrategy implements FieldNamingStrategy {
	public static FieldNamingStrategy instance = new NamingStrategy();

	@Override
	public String translateName(Field f) {
		String name = FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.translateName(f);

		// We couldn't name the field short because it's reserved in java.
		//
		return (name.equals("is_short") ? "short" : name);
	}
}
