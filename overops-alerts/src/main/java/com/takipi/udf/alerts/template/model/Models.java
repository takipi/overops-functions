package com.takipi.udf.alerts.template.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Models {
	protected static final Logger logger = LoggerFactory.getLogger(Models.class);

	private static final String BASE_PATH = "/models/";
	private static final String NEW_EVENT = "new-event.json";
	private static final String RESURFACED_EVENT = "resurfaced-event.json";

	public static Model newEvent() {
		return load(NEW_EVENT);
	}

	public static Model resurfacedError() {
		return load(RESURFACED_EVENT);
	}

	// TODO - actual model load when used.
	//
	public static Model threshold() {
		return new Model();
	}

	// TODO - actual model load when used.
	//
	public static Model anomaly() {
		return new Model();
	}

	private static Model load(String path) {
		try {
			return Model.from(BASE_PATH + path);
		} catch (Exception e) {
			logger.error("Failed loading model {}.", path, e);
			return null;
		}
	}
}
