package com.takipi.udf.alerts.pagerduty.sender;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.takipi.api.client.data.event.Location;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.pagerduty.PagerDutyFunction.PagerDutyInput;
import com.takipi.udf.alerts.pagerduty.message.Details;
import com.takipi.udf.alerts.pagerduty.message.PagerDutyMessage;
import com.takipi.udf.alerts.util.AlertUtil;
import com.takipi.udf.alerts.util.SourceConstants;
import com.takipi.udf.util.url.UrlUtil;

public abstract class PagerDutyEventSender extends PagerDutySender {
	private static final String STACK_FRAME_FORMAT = "\tat %s\n";

	protected final EventResult event;
	protected final ContextArgs contextArgs;

	protected PagerDutyEventSender(PagerDutyInput input, EventResult event, ContextArgs contextArgs) {
		super(input);

		this.event = event;
		this.contextArgs = contextArgs;
	}

	protected abstract String createDescription();

	@Override
	protected PagerDutyMessage createMessage(PagerDutyMessage.Builder builder) {

		builder.setDescription(createDescription()).setIncidentKey(createIncidentKey()).setClientUrl(createTaleLink())
				.setDetails(createDetails());

		return builder.build();
	}

	private String createIncidentKey() {
		int hashValue = Objects.hashCode(contextArgs.serviceId, event.class_group);

		return String.valueOf(hashValue);
	}

	private String createTaleLink() {
		return UrlUtil.buildTaleUrl(contextArgs.appHost, contextArgs.serviceId, contextArgs.eventId, 1,
				SourceConstants.SOURCE_PAGERDUTY_FIRST_HIT_MESSAGE, true, null, 0);
	}

	private Details createDetails() {
		Details.Builder builder = Details.newBuilder();

		String serverName = createServerName();
		String jvmName = createJvmName();
		String deploymentName = createDeploymentName();
		String errorText = createErrorText();

		if (!Strings.isNullOrEmpty(serverName)) {
			builder.setServer(serverName);
		}

		if (!Strings.isNullOrEmpty(jvmName)) {
			builder.setJvm(jvmName);
		}

		if (!Strings.isNullOrEmpty(deploymentName)) {
			builder.setDeployment(deploymentName);
		}

		if (!Strings.isNullOrEmpty(errorText)) {
			builder.setError(errorText);
		}

		return builder.build();
	}

	private String createServerName() {
		return event.introduced_by_server;
	}

	private String createJvmName() {
		return event.introduced_by_application;
	}

	private String createDeploymentName() {
		return event.introduced_by;
	}

	private String createErrorText() {
		String errorTitle = AlertUtil.createEventTitle(event);
		String stackText = createStackText();

		StringBuilder builder = new StringBuilder();

		builder.append(errorTitle);

		if (!Strings.isNullOrEmpty(stackText)) {
			builder.append("\n");
			builder.append(stackText);
		}

		return builder.toString();
	}

	private String createStackText() {
		StringBuilder builder = new StringBuilder();

		for (Location frame : event.stack_frames) {
			if ((!frame.in_filter) || (Strings.isNullOrEmpty(frame.prettified_name))) {
				continue;
			}

			builder.append(String.format(STACK_FRAME_FORMAT, frame.prettified_name));
		}

		return builder.toString();
	}
}
