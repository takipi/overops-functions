package com.takipi.udf.alerts.template.model;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.takipi.common.util.IOUtil;

public class Model {
	private static final Gson GSON = new GsonBuilder()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

	public Subject subject;
	public Headline headline;
	public Body body;

	public static Model from(String resource) {
		return IOUtil.readFromResource(resource, Model.class, GSON);
	}
}
