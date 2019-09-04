package com.takipi.udf.microsoftteams;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.request.team.TeamMembersRequest;
import com.takipi.api.client.request.view.ViewRequest;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.result.team.TeamMembersResult;
import com.takipi.api.client.result.view.ViewResult;
import com.takipi.api.client.util.validation.ValidationUtil;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient;
import com.takipi.udf.ContextArgs;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.takipi.udf.microsoftteams.MicrosoftTeamsChannelFunction.getContextArgs;
import static com.takipi.udf.microsoftteams.MicrosoftTeamsChannelFunction.getEnvironmentName;
import static com.takipi.udf.microsoftteams.MicrosoftTeamsUtil.getEventData;
import static com.takipi.udf.microsoftteams.MicrosoftTeamsUtil.getTimeSlot;

public class MicrosoftTeamsAnomalyFunction {
    public static String validateInput(String rawInput) {
        return getInput(rawInput).toString();
    }

    public static void execute(String rawContextArgs, String rawInput) {
        MicrosoftTeamsAnomalyInput input = getInput(rawInput);

        ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

        System.out.println("execute context: " + rawContextArgs);

        if (!args.validate())
            throw new IllegalArgumentException("Bad context args: " + rawContextArgs);

        if (!args.viewValidate())
            return;

        ApiClient apiClient = args.apiClient();

        MicrosoftTeamsUtil.TimeSlot timeSlot = getTimeSlot(input.timespan);

        DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZoneUTC();

        EventsVolumeRequest eventsVolumeRequest = EventsVolumeRequest.newBuilder().setServiceId(args.serviceId)
                .setViewId(args.viewId).setFrom(timeSlot.from.toString(fmt)).setTo(timeSlot.to.toString(fmt))
                .setVolumeType(ValidationUtil.VolumeType.all).build();
        UrlClient.Response<EventsResult> volumeResponse = apiClient.get(eventsVolumeRequest);

        MicrosoftTeamsUtil.EventData eventData = getEventData(apiClient, args);

        String viewName = getViewName(args, apiClient);

        String viewErrorsLink = getViewLink(args, viewName);

        long hitSum = getHitSum(volumeResponse);
        if (input.threshold != 0 && input.threshold > hitSum) {
            return;
        }

        MicrosoftTeamsAnomalyRequest microsoftTeamsAnomalyRequest = MicrosoftTeamsAnomalyRequest.newBuilder()
                .setUrl(input.url)
                .setEnvironmentsName(getEnvironmentName(eventData.environments, args.serviceId))
                .setViewErrorsLink(viewErrorsLink)
                .setManageSettingsLink("https://app.overops.com/index.html?nav=mailset")
                .setReportedByName(getReporterMail(args, apiClient))
                .setViewErrorsLink(viewErrorsLink)
                .setThresholdCount(String.valueOf(input.threshold))
                .setEventsVolume(volumeResponse.data.events, apiClient, args, timeSlot, eventData)
                .setTimeSlot(timeSlot.toString())
                .setTotalEventsOccurred(String.valueOf(hitSum))
                .setViewName(viewName)
                .build();

        UrlClient.Response<String> post = SimpleUrlClient.newBuilder().build().post(microsoftTeamsAnomalyRequest);
        if (post.isBadResponse())
            throw new IllegalStateException("Can't send anomaly card to " + input.url);
    }

    private static String getViewLink(ContextArgs args, String viewName){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("https://app.overops.com/index.html?key=").append(args.serviceId).append("&timeframe=last-24-hours");
        try {
            stringBuilder.append(URLEncoder.encode(viewName, StandardCharsets.UTF_8.toString()));
        } catch (Exception e) {System.out.println("Couldn't encode " + viewName);}
        return  stringBuilder.toString();
    }

    private static String getReporterMail(ContextArgs args, ApiClient apiClient) {
        String reporterMail = "";
        TeamMembersRequest teamMembersRequest = TeamMembersRequest.newBuilder().setServiceId(args.serviceId).build();
        UrlClient.Response<TeamMembersResult> teamMembersResultResponse = apiClient.get(teamMembersRequest);
        if (teamMembersResultResponse.isOK() && teamMembersResultResponse.data.team_members != null && teamMembersResultResponse.data.team_members.size() > 0) {
            reporterMail = teamMembersResultResponse.data.team_members.get(0).email;
        }

        return reporterMail;
    }

    private static long getHitSum(UrlClient.Response<EventsResult> volumeResponse) {
        long hitSum = 0;

        if (volumeResponse.data != null && volumeResponse.data.events != null) {
            hitSum = volumeResponse.data.events.stream().map(v -> v.stats.hits).reduce(0L, (sum, c) -> sum + c);
        }

        return hitSum;
    }

    private static String getViewName(ContextArgs args, ApiClient apiClient) {
        String viewName = "";

        ViewRequest viewRequest = ViewRequest.newBuilder().setServiceId(args.serviceId).setViewId(args.viewId).build();
        UrlClient.Response<ViewResult> viewResultResponse = apiClient.get(viewRequest);
        if (viewResultResponse.isOK()) viewName = viewResultResponse.data.name;

        return viewName;
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

        if (input.timespan <= 0)
            throw new IllegalArgumentException("'timespan' must be positive");

        if (input.threshold < 0) {
            throw new IllegalArgumentException("'threshold' must be positive or zero");
        }

        input.checkUrl();

        return input;
    }

    // A sample program on how to programmatically activate
    public static void main(String[] args) {
        ContextArgs contextArgs = getContextArgs(args);

        SummarizedView view = ViewUtil.getServiceViewByName(contextArgs.apiClient(), contextArgs.serviceId,
                "All Exceptions (1)");

        contextArgs.viewId = view.id;

        String rawContextArgs = new Gson().toJson(contextArgs);
        String rawInput = "url=https://outlook.office.com/webhook/..." +
                "\n" +
                "timespan=5" + "\n" +
                "threshold = 1";
        MicrosoftTeamsAnomalyFunction.execute(rawContextArgs, rawInput);
    }
}
