package com.takipi.udf.microsoftteams;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.metrics.Graph;
import com.takipi.api.client.data.service.SummarizedService;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventRequest;
import com.takipi.api.client.request.event.EventsRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.event.EventUtil;
import com.takipi.api.client.util.validation.ValidationUtil;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient;
import com.takipi.common.util.CollectionUtil;
import com.takipi.udf.ContextArgs;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Base64;
import java.util.List;

import static com.takipi.udf.microsoftteams.MicrosoftTeamsUtil.getEventData;
import static com.takipi.udf.microsoftteams.MicrosoftTeamsUtil.getTimeSlot;

public class MicrosoftTeamsChannelFunction {

    public static final int MINUTES_TIME_SPAN = 5;

    public static String validateInput(String rawInput) {
        return getInput(rawInput).toString();
    }

    private static MicrosoftTeamsInput getInput(String rawInput) {
        if (Strings.isNullOrEmpty(rawInput))
            throw new IllegalArgumentException("Input is empty");

        MicrosoftTeamsInput input;

        try {
            input = MicrosoftTeamsInput.of(rawInput);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        input.checkUrl();

        return input;
    }

    public static void execute(String rawContextArgs, String rawInput) {
        MicrosoftTeamsInput input = getInput(rawInput);

        ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

        if (!args.validate())
            throw new IllegalArgumentException("Bad context args: " + rawContextArgs);

        if (!args.eventValidate())
            return;

        ApiClient apiClient = args.apiClient();

        UrlClient.Response<EventResult> eventResultResponse = getEventResultResponse(args, apiClient);
        MicrosoftTeamsUtil.EventData eventData = getEventData(apiClient, args);

        MicrosoftTeamsUtil.TimeSlot timeSlot = getTimeSlot(MINUTES_TIME_SPAN);

        Graph eventsGraph = ViewUtil.getEventsGraph(apiClient, args.serviceId, args.viewId,
                1, ValidationUtil.VolumeType.all, timeSlot.from, timeSlot.to);
        String exceptionLinkToOverOps = EventUtil.getEventRecentLink(apiClient, args.serviceId, args.eventId,
                timeSlot.from, timeSlot.to, eventData.applications,
                eventData.servers, eventData.deployments);

        MicrosoftTeamsChannelRequest microsoftTeamsChannelRequest = MicrosoftTeamsChannelRequest.newBuilder()
                .setUrl(input.url)
                .setEventResult(eventResultResponse.data)
                .setExceptionLink(exceptionLinkToOverOps)
                .setServer(eventsGraph.machine_name)
                .setApplication(eventsGraph.application_name)
                .setEnvironmentName(getEnvironmentName(eventData.environments, args.serviceId))
                .setDeployment(eventsGraph.deployment_name)
                .setDoNotAlertLink(getDoNotAlertLink(args, eventResultResponse.data.type))
                .build();

        UrlClient.Response<String> post = SimpleUrlClient.newBuilder().build().post(microsoftTeamsChannelRequest);

        if (post.isBadResponse())
            throw new IllegalStateException("Can't send card to " + input.url);
    }

    private static String getDoNotAlertLink(ContextArgs args, String exceptionType) {
        return args.appHost + "/index.html?key=" + args.serviceId +
                "&nav=alertset&nav=archivemailitem&exception_class=" + Base64.getEncoder().encodeToString(exceptionType.getBytes());
    }

    private static UrlClient.Response<EventResult> getEventResultResponse(ContextArgs args, ApiClient apiClient) {
        EventRequest eventRequest = EventRequest.newBuilder()
                .setEventId(args.eventId)
                .setServiceId(args.serviceId)
                .setIncludeStacktrace(true)
                .build();

        UrlClient.Response<EventResult> eventResultResponse = apiClient.get(eventRequest);

        if (!eventResultResponse.isOK() || eventResultResponse.data == null)
            throw new IllegalStateException("Event is not present or api issue. eventId = " + args.eventId
                    + " , serviceId = " + args.serviceId);

        return eventResultResponse;
    }

    public static String getEnvironmentName(List<SummarizedService> environments, String serviceId) {
        return environments.stream()
                .filter(v -> v.id.equals(serviceId))
                .findFirst()
                .map(v -> v.name).orElse("");
    }

    // A sample program on how to programmatically activate
    public static void main(String[] args) {
        ContextArgs contextArgs = getContextArgs(args);

        // get "All Events" view
        SummarizedView view = ViewUtil.getServiceViewByName(contextArgs.apiClient(), contextArgs.serviceId,
                "view 2");

        contextArgs.viewId = view.id;

        MicrosoftTeamsUtil.TimeSlot timeSlot = getTimeSlot(MINUTES_TIME_SPAN);

        // date parameter must be properly formatted
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZoneUTC();

        // get all events within the date range
        EventsRequest eventsRequest = EventsRequest.newBuilder().setServiceId(contextArgs.serviceId)
                .setViewId(contextArgs.viewId).setFrom(timeSlot.from.toString(fmt)).setTo(timeSlot.to.toString(fmt)).build();

        // create a new API Client
        ApiClient apiClient = contextArgs.apiClient();

        // execute API GET request
        UrlClient.Response<EventsResult> eventsResponse = apiClient.get(eventsRequest);

        // check for a bad API response
        if (eventsResponse.isBadResponse())
            throw new IllegalStateException("Failed getting events.");

        // retrieve event data from the result
        EventsResult eventsResult = eventsResponse.data;

        // exit if there are no events - increase date range if this occurs
        if (CollectionUtil.safeIsEmpty(eventsResult.events)) {
            System.out.println("NO EVENTS");
            return;
        }

        // retrieve a list of events from the result
        List<EventResult> events = eventsResult.events;

        // get the first event
        contextArgs.eventId = events.get(0).id;

        // set url similar to "url=https://outlook.office.com/webhook/..."
        String rawInput = "url=https://outlook.office.com/webhook/...";

        // convert context args to a JSON string
        String rawContextArgs = new Gson().toJson(contextArgs);

        // execute the UDF
        MicrosoftTeamsChannelFunction.execute(rawContextArgs, rawInput);
    }

    public static ContextArgs getContextArgs(String[] args) {
        ContextArgs contextArgs = new ContextArgs();

        if (args.length == 0) {
            contextArgs.apiHost = "https://api.overops.com";
            contextArgs.apiKey = "api_token";
            contextArgs.serviceId = "SXXXXX";
        } else  {
            // pass API Host, Key, and Service ID as command line arguments
            if ((args == null) || (args.length < 3))
                throw new IllegalArgumentException("java MicrosoftTeamsFunction");

            contextArgs.apiHost = args[0];
            contextArgs.apiKey = args[1];
            contextArgs.serviceId = args[2];
        }

        return contextArgs;
    }
}
