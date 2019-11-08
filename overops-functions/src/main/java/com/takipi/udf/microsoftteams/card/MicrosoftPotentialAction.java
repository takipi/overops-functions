package com.takipi.udf.microsoftteams.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MicrosoftPotentialAction {
	@SerializedName("@type")
	public final String type;
	public final String name;
	public final List<MicrosoftTarget> targets;

	public MicrosoftPotentialAction(String type, String name, List<MicrosoftTarget> targets) {
		this.type = type;
		this.name = name;
		this.targets = targets;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String type = "OpenUri";
		private String name = "";
		private List<MicrosoftTarget> targets = new ArrayList<>();

		public Builder setType(String type) {
			this.type = type;
			return this;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setTargets(List<MicrosoftTarget> targets) {
			this.targets = targets;
			return this;
		}

		public Builder addTargets(MicrosoftTarget... targets) {
			if (this.targets == null) {
				this.targets = new ArrayList<>();
			}
			this.targets.addAll(Arrays.asList(targets));

			return this;
		}

		public MicrosoftPotentialAction build() {
			return new MicrosoftPotentialAction(type, name, targets);
		}
	}
}
