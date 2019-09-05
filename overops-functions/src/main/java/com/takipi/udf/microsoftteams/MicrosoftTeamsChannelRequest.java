package com.takipi.udf.microsoftteams;

import com.google.gson.Gson;
import com.takipi.api.client.data.event.Location;
import com.takipi.api.client.result.EmptyResult;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.core.consts.ApiConstants;
import com.takipi.api.core.request.intf.ApiPostRequest;
import com.takipi.udf.microsoftteams.card.*;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MicrosoftTeamsChannelRequest implements ApiPostRequest<EmptyResult> {

    public static final String THEME_COLOR = "ffc000";
    private String url;
    private String exceptionLinkToOverOps = "";
    private String exceptionClassName = "";
    private String exceptionLocationPath = "";
    private List<Location> stack_frames = new ArrayList<>();
    private String deployment = "";
    private String application = "";
    private String server = "";
    private String environmentsName = "";
    private String reportedBy = "";
    private String doNotAlertLink = "";

    public MicrosoftTeamsChannelRequest(String url,
                                        String exceptionLinkToOverOps,
                                        String exceptionClassName,
                                        String exceptionLocationPath,
                                        List<Location> stack_frames,
                                        String deployment,
                                        String application,
                                        String server,
                                        String environmentsName,
                                        String reportedBy,
                                        String doNotAlertLink) {
        this.url = url;
        this.exceptionLinkToOverOps = exceptionLinkToOverOps;
        this.exceptionClassName = exceptionClassName;
        this.exceptionLocationPath = exceptionLocationPath;
        this.stack_frames = stack_frames;
        this.deployment = deployment;
        this.application = application;
        this.server = server;
        this.environmentsName = environmentsName;
        this.reportedBy = reportedBy;
        this.doNotAlertLink = doNotAlertLink;
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
        MicrosoftCard microsoftCard = MicrosoftCard.newBuilder()
                .setThemeColor(THEME_COLOR)
                .setText(new MicrosoftTextBuilder()
                        .add("A new ").addBold(exceptionClassName).add(" in ")
                        .addBold(exceptionLocationPath)
                        .add(" has been detected in ").addBold(environmentsName)
                        .add(StringUtils.isNotEmpty(reportedBy) ?
                                new MicrosoftTextBuilder().add(" (alert added by ").addBold(reportedBy).add(")").build() : "" )
                        .build())
                .addSections(new MicrosoftTextSection(new MicrosoftTextBuilder()
                                .addLink(exceptionLinkToOverOps, exceptionClassName)
                                .addEnter()
                                .addHighlighted(new MicrosoftTextBuilder().addArray(stack_frames, " at ").build())
                                .build()),
                        new MicrosoftActivitySection.Builder()
                                .addFacts(new MicrosoftFact("Server", server),
                                        new MicrosoftFact("Application", application),
                                        new MicrosoftFact("Deployment", deployment)).build())
                .addPotentialActions(MicrosoftPotentialAction.newBuilder()
                                .setName("View Event")
                                .addTargets(new MicrosoftTarget(exceptionLinkToOverOps))
                                .build(),
                        MicrosoftPotentialAction.newBuilder()
                                .setName("Do not alert on new " + exceptionClassName)
                                .addTargets(new MicrosoftTarget(doNotAlertLink))
                                .build())
                .build();

        return new Gson().toJson(microsoftCard);
    }

    @Override
    public Class<EmptyResult> resultClass() {
        return EmptyResult.class;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String exceptionClassName = "";
        private String exceptionLocationPath = "";
        private List<Location> stack_frames = new ArrayList<>();
        private String deployment = "";
        private String application = "";
        private String server = "";
        private String environmentsName = "";
        private String url;
        private String exceptionLink = "";
        private String reportedBy = "";
        private String doNotAlertLink = "";

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setEventResult(EventResult eventResult) {
            if (eventResult != null) {
                exceptionClassName = eventResult.name;
                exceptionLocationPath = eventResult.error_location.prettified_name;
                stack_frames = eventResult.stack_frames;
                deployment = eventResult.introduced_by;
            }

            return this;
        }

        public Builder setExceptionLink(String exceptionLink) {
            this.exceptionLink = exceptionLink;
            return this;
        }

        public Builder setServer(String server) {
            this.server = server;

            return this;
        }

        public Builder setApplication(String application) {
            this.application = application;

            return this;
        }

        public Builder setEnvironmentName(String environmentsName) {
            this.environmentsName = environmentsName;

            return this;
        }

        public Builder setDeployment(String deployment) {
            this.deployment = deployment;
            return this;
        }

        public Builder setReportedBy(String reportedBy) {
            this.reportedBy = reportedBy;
            return this;
        }

        public Builder setDoNotAlertLink(String doNotAlertLink) {
            this.doNotAlertLink = doNotAlertLink;
            return this;
        }

        public MicrosoftTeamsChannelRequest build() {
            return new MicrosoftTeamsChannelRequest(url, exceptionLink, exceptionClassName,
                    exceptionLocationPath, stack_frames, deployment,
                    application, server, environmentsName,
                    reportedBy, doNotAlertLink);
        }
    }
}
