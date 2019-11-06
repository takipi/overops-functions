package com.takipi.udf.microsoftteams;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.validation.ValidationUtil;
import com.takipi.api.core.url.UrlClient;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.microsoftteams.MicrosoftTeamsUtil.TimeSlot;
import com.takipi.udf.util.TestUtil;

public class MicrosoftTeamsAnomalyFunction {

	public static String validateInput(String rawInput) {
		return getInput(rawInput).toString();
	}

	public static void execute(String rawContextArgs, String rawInput) {
		try {
			executeImplementation(rawContextArgs, rawInput);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void executeImplementation(String rawContextArgs, String rawInput) {
		MicrosoftTeamsUtil.logUDFInput(rawContextArgs, rawInput);
		MicrosoftTeamsAnomalyInput input = getInput(rawInput);

		ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

		if (!args.validate()) {
			throw new IllegalArgumentException("Bad context args: " + rawContextArgs);
		}

		ApiClient apiClient = args.apiClient();
		TimeSlot timeSlot = MicrosoftTeamsUtil.getTimeSlot(input.timespan);

		DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZoneUTC();

		SummarizedView view = MicrosoftTeamsUtil.getSummarizedView(rawContextArgs, args, apiClient);
		System.out.println("Got viewId");

		EventsVolumeRequest eventsVolumeRequest = EventsVolumeRequest.newBuilder().setServiceId(args.serviceId)
				.setViewId(view.id).setFrom(timeSlot.from.toString(fmt)).setTo(timeSlot.to.toString(fmt))
				.setVolumeType(ValidationUtil.VolumeType.all).build();

		UrlClient.Response<EventsResult> volumeResponse = apiClient.get(eventsVolumeRequest);

		System.out.println("Got events volume data ");
		String viewErrorsLink = getViewLink(args, view.name);
		System.out.println("Got View link");

		long hitSum = getHitSum(volumeResponse);

		if ((input.threshold != 0) && (input.threshold > hitSum)) {
			return;
		}

		MicrosoftTeamsAnomalyRequest microsoftTeamsAnomalyRequest = MicrosoftTeamsAnomalyRequest.newBuilder()
				.setUrl(input.url).setEnvironmentsName(MicrosoftTeamsUtil.getEnvironmentName(apiClient, args.serviceId))
				.setManageSettingsLink(getManageSettingsLink(args)).setViewErrorsLink(viewErrorsLink)
				.setThresholdCount(String.valueOf(input.threshold))
				.setEventsVolume(volumeResponse.data.events, apiClient, args, timeSlot).setTimeSlot(timeSlot.toString())
				.setTotalEventsOccurred(String.valueOf(hitSum)).setViewName(view.name).build();

		UrlClient.Response<String> post = SimpleUrlClient.newBuilder().build().post(microsoftTeamsAnomalyRequest);

		System.out.println("Post Microsoft Teams Webhook Anomaly request");

		if (post.isBadResponse()) {
			throw new IllegalStateException("Can't send anomaly card to " + input.url);
		}
	}

	private static String getManageSettingsLink(ContextArgs args) {
		return args.appHost + "/index.html?key=" + args.serviceId + "&nav=alertset";
	}

	private static String getViewLink(ContextArgs args, String viewName) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append(args.appHost).append("/index.html?key=").append(args.serviceId)
				.append("&timeframe=last-24-hours");

		try {
			stringBuilder.append("&view=").append(URLEncoder.encode(viewName, StandardCharsets.UTF_8.toString()));
		} catch (Exception e) {
			System.out.println("Couldn't encode " + viewName);
		}

		return stringBuilder.toString();
	}

	private static long getHitSum(UrlClient.Response<EventsResult> volumeResponse) {
		long hitSum = 0;

		if ((volumeResponse.data) != null && (volumeResponse.data.events != null)) {
			hitSum = volumeResponse.data.events.stream().map(v -> v.stats.hits).reduce(0L, (sum, c) -> sum + c);
		}

		return hitSum;
	}

	private static MicrosoftTeamsAnomalyInput getInput(String rawInput) {
		System.out.println("rawInput:" + rawInput);

		if (Strings.isNullOrEmpty(rawInput))
			throw new IllegalArgumentException("Input is empty");

		MicrosoftTeamsAnomalyInput input;

		try {
			input = MicrosoftTeamsAnomalyInput.of(rawInput);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		if (input.timespan <= 0) {
			throw new IllegalArgumentException("'timespan' must be positive");
		}

		if (input.threshold < 0) {
			throw new IllegalArgumentException("'threshold' must be positive or zero");
		}

		input.checkUrl();

		return input;
	}

	// A sample program on how to programmatically activate
	public static void main(String[] args) {
		ContextArgs contextArgs = TestUtil.getDefaultContextArgsBuilder().setApiKey("api_token").setServiceId("SXXXXX")
				.build();

		String rawContextArgs = new Gson().toJson(contextArgs);
		String rawInput = "url=https://outlook.office.com/webhook/..." + "\n" + "timespan=5" + "\n" + "threshold = 1";

		MicrosoftTeamsAnomalyFunction.execute(rawContextArgs, rawInput);
	}
}
