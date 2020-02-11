package com.takipi.udf.alerts.servicenow.sender;

import com.takipi.api.client.data.event.Location;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.common.util.CollectionUtil;
import com.takipi.common.util.StringUtil;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.servicenow.ServiceNowConsts;
import com.takipi.udf.alerts.servicenow.ServiceNowFunction.ServiceNowInput;
import com.takipi.udf.alerts.servicenow.message.Message;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.alerts.util.HTMLUtil;
import com.takipi.udf.util.url.UrlUtil;

public abstract class ServiceNowEventSender extends ServiceNowSender {
	private static final int MAX_TITLE_LENGTH = 120;

	private static final String UNNAMED_APPLICATION = "Unnamed Application";
	private static final String APPLICATION_NAMING_LINK = "https://doc.overops.com/docs/naming-your-application-server-deployment#section-naming-the-application";

	private static final String UNNAMED_DEPLOYMENT = "Unnamed Deployment";
	private static final String DEPLOYMENT_NAMING_LINK = "https://doc.overops.com/docs/naming-your-application-server-deployment#section-naming-the-deployment";

	private static final String STACK_FRAME_FORMAT = "&nbsp;&nbsp;at %s<br>";

	protected final EventResult event;

	protected ServiceNowEventSender(ServiceNowInput input, ContextArgs contextArgs, EventResult event) {
		super(input, contextArgs);

		this.event = event;
	}

	@Override
	protected Message buildMessage() {
		return Message.newBuilder().setShortDescription(getShortDescription())
				.setComments("[code]" + HTMLUtil.minimizeHtml(getHtmlMessageBody()) + "[/code]").build();
	}

	protected String getHtmlMessageBody() {
		StringBuilder bodyBuilder = new StringBuilder();

		String title = AlertUtil.createEventTitle(event, MAX_TITLE_LENGTH);
		String taleLink = UrlUtil.buildTaleUrl(contextArgs.appHost, contextArgs.serviceId, contextArgs.eventId, 1,
				getTaleUrlSource(), true, null, 0);

		String applicationName = event.introduced_by_application;

		if (StringUtil.isNullOrEmpty(applicationName)) {
			applicationName = HTMLUtil.htmlLink(UNNAMED_APPLICATION, APPLICATION_NAMING_LINK, true);
		}

		String deploymentName = event.introduced_by;

		if (deploymentName.equals(UNNAMED_DEPLOYMENT)) {
			deploymentName = HTMLUtil.htmlLink(deploymentName, DEPLOYMENT_NAMING_LINK, true);
		}

		bodyBuilder.append(createHtmlAlertAddedBy());

		bodyBuilder.append(HTMLUtil.htmlParagraph(HTMLUtil.htmlLink(HTMLUtil.htmlBold(title), taleLink, true)
				+ HTMLUtil.htmlLineBreak() + createHtmlStack()));

		bodyBuilder.append(HTMLUtil.htmlParagraph(
				HTMLUtil.htmlTable(nonEmptyHtmlTableRow(HTMLUtil.htmlBold("Server:"), event.introduced_by_server)
						+ nonEmptyHtmlTableRow(HTMLUtil.htmlBold("Application:"), applicationName)
						+ nonEmptyHtmlTableRow(HTMLUtil.htmlBold("Deployment:"), deploymentName))));

		bodyBuilder.append(HTMLUtil.htmlParagraph(HTMLUtil.htmlLink(HTMLUtil.htmlBold("View Event"), taleLink, true)));

		bodyBuilder
				.append(HTMLUtil.htmlParagraph(HTMLUtil.htmlLink(HTMLUtil.htmlImage(ServiceNowConsts.OVEROPS_LOGO_URL),
						UrlUtil.getDashboardUrl(contextArgs.appHost), true)));

		return bodyBuilder.toString();
	}

	private String createHtmlStack() {
		if (CollectionUtil.safeIsEmpty(event.stack_frames)) {
			return "";
		}

		StringBuilder stringBuilder = new StringBuilder();

		for (Location frame : event.stack_frames) {
			if ((!frame.in_filter) || (StringUtil.isNullOrEmpty(frame.prettified_name))) {
				continue;
			}

			stringBuilder.append(String.format(STACK_FRAME_FORMAT, frame.prettified_name));
		}

		return stringBuilder.toString();
	}

	protected abstract int getTaleUrlSource();

	protected abstract String getShortDescription();

	protected abstract String createHtmlAlertAddedBy();
}
