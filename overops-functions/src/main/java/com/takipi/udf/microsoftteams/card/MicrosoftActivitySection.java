package com.takipi.udf.microsoftteams.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MicrosoftActivitySection implements MicrosoftSection {
	public final String activityTitle;
	public final String activitySubtitle;
	public final String activityImage;
	public final boolean markdown;
	public final List<MicrosoftFact> facts;
	public final String text;

	public MicrosoftActivitySection(String activityTitle, String activitySubtitle, String activityImage,
			boolean markdown, List<MicrosoftFact> facts, String text) {
		this.activityTitle = activityTitle;
		this.activitySubtitle = activitySubtitle;
		this.activityImage = activityImage;
		this.markdown = markdown;
		this.facts = facts;
		this.text = text;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private String activityTitle;
		private String activitySubtitle;
		private String activityImage;
		private boolean markdown;
		private List<MicrosoftFact> facts;
		public String text = "";

		public Builder setActivityTitle(String activityTitle) {
			this.activityTitle = activityTitle;
			return this;
		}

		public Builder setActivitySubtitle(String activitySubtitle) {
			this.activitySubtitle = activitySubtitle;
			return this;
		}

		public Builder setActivityImage(String activityImage) {
			this.activityImage = activityImage;
			return this;
		}

		public Builder setText(String text) {
			this.text = text;
			return this;
		}

		public Builder setMarkdown(boolean markdown) {
			this.markdown = markdown;
			return this;
		}

		public Builder setFacts(List<MicrosoftFact> facts) {
			this.facts = facts;
			return this;
		}

		public Builder addFacts(MicrosoftFact... facts) {
			if (this.facts == null) {
				this.facts = new ArrayList<>();
			}
			this.facts.addAll(Arrays.asList(facts));

			return this;
		}

		public MicrosoftActivitySection build() {
			return new MicrosoftActivitySection(activityTitle, activitySubtitle, activityImage, markdown, facts, text);
		}
	}
}
