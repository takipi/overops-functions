package com.takipi.udf.microsoftteams;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.result.EmptyResult;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.util.event.EventUtil;
import com.takipi.api.core.consts.ApiConstants;
import com.takipi.api.core.request.intf.ApiPostRequest;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.microsoftteams.card.MicrosoftActivitySection;
import com.takipi.udf.microsoftteams.card.MicrosoftCard;
import com.takipi.udf.microsoftteams.card.MicrosoftFact;
import com.takipi.udf.microsoftteams.card.MicrosoftTextBuilder;
import com.takipi.udf.microsoftteams.card.MicrosoftTextSection;

public class MicrosoftTeamsAnomalyRequest implements ApiPostRequest<EmptyResult> {

	public static final String SUMMARY_IMAGE = "https://s3.amazonaws.com/www.takipi.com/email/v5/threshold-thumb.png";

	private final String url;
	private final String viewErrorsLink;
	private final String viewName;
	private final String thresholdCount;
	private final String totalEventsOccurred;
	private final String timeSlot;
	private final String environmentsName;
	private final String reportedByName;
	private final String manageSettingsLink;
	private final List<MicrosoftTextSection> eventSections;

	public MicrosoftTeamsAnomalyRequest(String url, String viewErrorsLink, String viewName, String thresholdCount,
			String totalEventsOccurred, String timeSlot, String environmentsName, String reportedByName,
			String manageSettingsLink, List<MicrosoftTextSection> eventSections) {
		this.url = url;
		this.viewErrorsLink = viewErrorsLink;
		this.viewName = viewName;
		this.thresholdCount = thresholdCount;
		this.totalEventsOccurred = totalEventsOccurred;
		this.timeSlot = timeSlot;
		this.environmentsName = environmentsName;
		this.reportedByName = reportedByName;
		this.manageSettingsLink = manageSettingsLink;
		this.eventSections = eventSections;
	}

	@Override
	public String contentType() {
		return ApiConstants.CONTENT_TYPE_JSON;
	}

	@Override
	public String urlPath() {
		return url;
	}

	@Override
	public String[] queryParams() throws UnsupportedEncodingException {
		return new String[0];
	}

	@Override
	public String postData() {
		MicrosoftCard microsoftCard = MicrosoftCard.newBuilder().setThemeColor("c80000").setText(getTopText())
				.addSections(getSummarySection()).addSections(eventSections).build();

		return new Gson().toJson(microsoftCard);
	}

	private MicrosoftActivitySection getSummarySection() {
		return MicrosoftActivitySection.newBuilder().setActivityImage(SUMMARY_IMAGE)
				.setActivityTitle(new MicrosoftTextBuilder().addBoldLink(viewErrorsLink, "View errors").build())
				.setActivitySubtitle(new MicrosoftTextBuilder().addLink(manageSettingsLink, "Manage settings").build())
				.addFacts(new MicrosoftFact("View", viewName), new MicrosoftFact("Threshold", thresholdCount),
						new MicrosoftFact("Between", timeSlot), new MicrosoftFact("Times", totalEventsOccurred))
				.build();
	}

	private String getTopText() {
		return new MicrosoftTextBuilder().add("Events in view ").addBold(viewName).add(" have occurred more than ")
				.addBold(thresholdCount).add(" has been detected in ").addBold(environmentsName)
				.add(StringUtils.isNotEmpty(reportedByName)
						? new MicrosoftTextBuilder().add(" (alert added by ").addBold(reportedByName).add(")").build()
						: "")
				.build();
	}

	@Override
	public Class<EmptyResult> resultClass() {
		return EmptyResult.class;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private String url = "";
		private String viewErrorsLink = "";
		private String viewName = "";
		private String thresholdCount = "";
		private String totalEventsOccurred = "";
		private String timeSlot = "";
		private String environmentsName = "";
		private String reportedByName = "";
		private String manageSettingsLink = "";
		private List<MicrosoftTextSection> eventSections = new ArrayList<>();

		public Builder setUrl(String url) {
			this.url = url;
			return this;
		}

		public Builder setViewErrorsLink(String viewErrorsLink) {
			this.viewErrorsLink = viewErrorsLink;
			return this;
		}

		public Builder setViewName(String viewName) {
			this.viewName = viewName;
			return this;
		}

		public Builder setThresholdCount(String thresholdCount) {
			this.thresholdCount = thresholdCount;
			return this;
		}

		public Builder setTotalEventsOccurred(String totalEventsOccurred) {
			this.totalEventsOccurred = totalEventsOccurred;
			return this;
		}

		public Builder setTimeSlot(String timeSlot) {
			this.timeSlot = timeSlot;
			return this;
		}

		public Builder setEnvironmentsName(String environmentsName) {
			this.environmentsName = environmentsName;
			return this;
		}

		public Builder setReportedByName(String reportedByName) {
			this.reportedByName = reportedByName;
			return this;
		}

		public Builder setManageSettingsLink(String manageSettingsLink) {
			this.manageSettingsLink = manageSettingsLink;
			return this;
		}

		public MicrosoftTeamsAnomalyRequest build() {
			return new MicrosoftTeamsAnomalyRequest(url, viewErrorsLink, viewName, thresholdCount, totalEventsOccurred,
					timeSlot, environmentsName, reportedByName, manageSettingsLink, eventSections);
		}

		public Builder setEventsVolume(List<EventResult> events, ApiClient apiClient, ContextArgs args,
				MicrosoftTeamsUtil.TimeSlot timeSlot) {
			eventSections = new ArrayList<>();

			events.forEach(evt -> {
				String link = "";
				try {
					link = EventUtil.getEventRecentLink(apiClient, args.serviceId, evt.id, timeSlot.from, timeSlot.to,
							null, null, null);
				} catch (Exception exception) {
					System.out.println(exception.getMessage());
				}
				MicrosoftTextSection microsoftTextSection = new MicrosoftTextSection(new MicrosoftTextBuilder()
						.addBoldLink(link, evt.name).add(" at ").add(evt.error_location.prettified_name).add("| ")
						.addBold(String.valueOf(evt.stats.hits)).addBold(evt.stats.hits > 1 ? " times" : " time")
						.build());
				eventSections.add(microsoftTextSection);
			});

			return this;
		}
	}
}
