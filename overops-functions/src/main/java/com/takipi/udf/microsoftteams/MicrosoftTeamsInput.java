package com.takipi.udf.microsoftteams;

import com.google.common.base.Strings;
import com.takipi.udf.input.Input;

class MicrosoftTeamsInput extends Input {
	public String url;

	protected MicrosoftTeamsInput(String raw) {
		super(raw);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("Microsoft Teams Alert ( url = ").append(url).append(" )");

		return builder.toString();
	}

	public void checkUrl() {
		if (Strings.isNullOrEmpty(url)) {
			throw new IllegalArgumentException("'url' isn't valid.");
		}
	}

	static MicrosoftTeamsInput of(String raw) {
		return new MicrosoftTeamsInput(raw);
	}
}
