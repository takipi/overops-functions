package com.takipi.udf.microsoftteams.card;

public class MicrosoftTextSection implements MicrosoftSection {
	@SuppressWarnings("unused")
	private final String text;

	public MicrosoftTextSection(String text) {
		this.text = text;
	}
}
