package com.takipi.udf.microsoftteams;

public class MicrosoftTeamsAnomalyInput extends MicrosoftTeamsInput {
	public int timespan;
	public int threshold;

	private MicrosoftTeamsAnomalyInput(String raw) {
		super(raw);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("Microsoft Teams Digest (").append(timespan).append(" min, url = ").append(url)
				.append(" , threshold = ").append(threshold).append(" )");

		return builder.toString();
	}

	static MicrosoftTeamsAnomalyInput of(String raw) {
		return new MicrosoftTeamsAnomalyInput(raw);
	}
}
