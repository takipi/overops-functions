package com.takipi.udf.microsoftteams;

import com.google.common.base.Strings;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.service.SummarizedService;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.util.client.ClientUtil;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.udf.ContextArgs;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

public class MicrosoftTeamsUtil {

    public static EventData getEventData(ApiClient apiClient, ContextArgs args) {
        List<String> servers = ClientUtil.getServers(apiClient, args.serviceId);
        List<String> applications = ClientUtil.getApplications(apiClient, args.serviceId);
        List<SummarizedService> environments = ClientUtil.getEnvironments(apiClient);
        List<String> deployments = ClientUtil.getDeployments(apiClient, args.serviceId, false);

        return new EventData(servers, applications, environments, deployments);
    }

    public static TimeSlot getTimeSlot(int minusMinutesFromNow) {
        DateTime now = DateTime.now();
        return new TimeSlot(now, now.minusMinutes(minusMinutesFromNow));
    }

    public static SummarizedView getSummarizedView(String rawContextArgs, ContextArgs args, ApiClient apiClient) {
        SummarizedView view = ViewUtil.getServiceViewByName(apiClient, args.serviceId, "All Events");
        if (view == null || Strings.isNullOrEmpty(view.id))
            throw new IllegalArgumentException("Couldn't get viewId " + rawContextArgs);
        return view;
    }

    public static void logUDFInput(String rawContextArgs, String rawInput) {
        System.out.println("rawContextArgs");
        System.out.println(rawContextArgs);
        System.out.println("rawInput");
        System.out.println(rawInput);
    }

    public static class TimeSlot {
        public DateTime to;
        public DateTime from;

        public TimeSlot(DateTime to, DateTime from) {
            this.to = to;
            this.from = from;
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            DateTimeFormatter formatter = DateTimeFormat.forPattern("hh:mm a").withZone(DateTimeZone.UTC);
            return stringBuilder.append(from.toString(formatter)).append(" - ").append(to.toString(formatter))
                    .append(" (UTC)")
                    .toString();
        }
    }

    public static class EventData {
        public List<String> servers;
        public List<String> applications;
        public List<SummarizedService> environments;
        public List<String> deployments;

        public EventData(List<String> servers, List<String> applications, List<SummarizedService> environments, List<String> deployments) {
            this.servers = servers;
            this.applications = applications;
            this.environments = environments;
            this.deployments = deployments;
        }
    }
}
