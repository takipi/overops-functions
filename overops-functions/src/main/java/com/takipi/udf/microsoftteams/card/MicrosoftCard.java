package com.takipi.udf.microsoftteams.card;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MicrosoftCard {
    @SerializedName("@type")
    public final String type;
    @SerializedName("@context")
    public final String context;
    public final String themeColor;
    public final String summary;
    public final String text;
    public final List<MicrosoftSection> sections;
    public final List<MicrosoftPotentialAction> potentialAction;

    public MicrosoftCard(String type, String context, String themeColor, String summary, String text, List<MicrosoftSection> sections, List<MicrosoftPotentialAction> potentialAction) {
        this.type = type;
        this.context = context;
        this.themeColor = themeColor;
        this.summary = summary;
        this.text = text;
        this.sections = sections;
        this.potentialAction = potentialAction;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        public String type = "MessageCard";
        public String context = "http://schema.org/extensions";
        public String themeColor = "0076D7";
        public String summary = "Summary";
        public String text = "";
        public List<MicrosoftSection> sections = new ArrayList<>();
        public List<MicrosoftPotentialAction> potentialAction = new ArrayList<>();

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setContext(String context) {
            this.context = context;
            return this;
        }

        public Builder setThemeColor(String themeColor) {
            this.themeColor = themeColor;
            return this;
        }

        public Builder setSummary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder setText(String text) {
            this.text = text;
            return this;
        }

        public Builder setSections(List<MicrosoftSection> sections) {
            this.sections = sections;
            return this;
        }

        public Builder addSections(MicrosoftSection... sections) {
            if (this.sections == null) {
                this.sections = new ArrayList<>();
            }
            this.sections.addAll(Arrays.asList(sections));

            return this;
        }

        public Builder addSections(List<MicrosoftTextSection> eventSections) {
            if (this.sections == null) {
                this.sections = new ArrayList<>();
            }
            this.sections.addAll(eventSections);

            return this;
        }

        public Builder setPotentialAction(List<MicrosoftPotentialAction> potentialAction) {
            this.potentialAction = potentialAction;
            return this;
        }

        public Builder addPotentialActions(MicrosoftPotentialAction... potentialActions) {
            if (this.potentialAction == null) {
                this.potentialAction = new ArrayList<>();
            }
            this.potentialAction.addAll(Arrays.asList(potentialActions));

            return this;
        }

        public MicrosoftCard build() {
            return new MicrosoftCard(type, context, themeColor, summary, text, sections, potentialAction);
        }
    }
}
