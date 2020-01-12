package com.takipi.udf.alerts.pagerduty.message;

public class Details {
	public final String server;
	public final String jvm;
	public final String deployment;
	public final String error;
	public final String presetName;
	public final String timeframe;
	public final String threshold;
	public final String occurences;
	public final String textMessage;
	public final String contributors;

	Details(String server, String jvm, String deployment, String error, String presetName, String timeframe,
			String threshold, String occurences, String textMessage, String contributors) {
		this.server = server;
		this.jvm = jvm;
		this.deployment = deployment;
		this.error = error;
		this.presetName = presetName;
		this.timeframe = timeframe;
		this.threshold = threshold;
		this.occurences = occurences;
		this.textMessage = textMessage;
		this.contributors = contributors;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String server;
		private String jvm;
		private String deployment;
		private String error;
		private String presetName;
		private String timeframe;
		private String threshold;
		private String occurences;
		private String textMessage;
		private String contributors;

		Builder() {

		}

		public Builder setServer(String val) {
			this.server = val;

			return this;
		}

		public Builder setJvm(String val) {
			this.jvm = val;

			return this;
		}

		public Builder setDeployment(String val) {
			this.deployment = val;

			return this;
		}

		public Builder setError(String val) {
			this.error = val;

			return this;
		}

		public Builder setPresetName(String val) {
			this.presetName = val;

			return this;
		}

		public Builder setTimeframe(String val) {
			this.timeframe = val;

			return this;
		}

		public Builder setThreshold(String val) {
			this.threshold = val;

			return this;
		}

		public Builder setOccurences(String val) {
			this.occurences = val;

			return this;
		}

		public Builder setTextMessage(String val) {
			this.textMessage = val;

			return this;
		}

		public Builder setContributors(String val) {
			this.contributors = val;

			return this;
		}

		public Details build() {
			return new Details(server, jvm, deployment, error, presetName, timeframe, threshold, occurences,
					textMessage, contributors);
		}
	}
}
