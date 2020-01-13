package com.takipi.udf.alerts.servicenow.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.takipi.udf.ContextArgs;
import com.takipi.udf.alerts.servicenow.ServiceNowConsts;
import com.takipi.udf.alerts.servicenow.ServiceNowFunction.ServiceNowInput;
import com.takipi.udf.alerts.servicenow.client.ServiceNowResponse;
import com.takipi.udf.alerts.servicenow.message.Message;
import com.takipi.udf.alerts.util.HTMLUtil;

public abstract class ServiceNowSender {
	protected static final Logger logger = LoggerFactory.getLogger(ServiceNowSender.class);

	protected final ServiceNowInput input;
	protected final ContextArgs contextArgs;

	protected ServiceNowSender(ServiceNowInput input, ContextArgs contextArgs) {
		this.input = input;
		this.contextArgs = contextArgs;
	}

	public boolean sendMessage() {
		String internalDescription = getInternalDescription();

		logger.info("About to send a ServiceNow message ({}) to {} (url: {}; table: {}).", internalDescription,
				contextArgs.serviceId, input.url, input.table);

		try {
			Message message = buildMessage();

			ServiceNowResponse response = input.client().postMessage(message);

			if (response == null) {
				logger.error("Error sending ServiceNow message ({}) to {} (url: {}; table: {}).", internalDescription,
						contextArgs.serviceId, input.url, input.table);

				return false;
			}

			if (!response.success) {
				logger.error("ServiceNow message ({}) to {} returned error code {} (url: {}; table: {}).",
						internalDescription, contextArgs.serviceId, response.statusCode, input.url, input.table);

				return false;
			}

			logger.info("ServiceNow message ({}) to {} sent successfully (url: {}; table: {}).", internalDescription,
					contextArgs.serviceId, input.url, input.table);

			return true;
		} catch (Exception e) {
			logger.error("Unable to send ServiceNow message ({}) to {} (url: {}; table: {}).", internalDescription,
					contextArgs.serviceId, input.url, input.table, e);

			return false;
		}
	}

	protected abstract String getInternalDescription();

	protected abstract Message buildMessage();

	protected String nonEmptyHtmlTableRow(String firstColumnContent, String secondColumnContent) {
		if ((Strings.isNullOrEmpty(firstColumnContent)) || (Strings.isNullOrEmpty(secondColumnContent))) {
			return "";
		}

		return HTMLUtil.htmlTableRow(HTMLUtil.htmlTableRowCell(firstColumnContent, ServiceNowConsts.COLUMN_WIDTH)
				+ HTMLUtil.htmlTableRowCell(secondColumnContent, null));
	}
}
