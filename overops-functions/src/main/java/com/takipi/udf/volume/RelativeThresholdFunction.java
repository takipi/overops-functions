package com.takipi.udf.volume;

import com.google.gson.Gson;
import com.takipi.api.client.ApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.udf.ContextArgs;

public class RelativeThresholdFunction extends ThresholdFunction {
	public static String validateInput(String rawInput) {
		return getThresholdInput(rawInput).toString();
	}
	
	public static void execute(String rawContextArgs, String rawInput) {
		execute(rawContextArgs, getThresholdInput(rawInput));
	}
	
	// A sample program on how to programmatically activate RelativeThresholdFunction
		
	public static void main(String[] args) {

		if ((args == null) || (args.length < 3)) {
			throw new IllegalArgumentException("args");
		}

		ContextArgs contextArgs = new ContextArgs();

		contextArgs.apiHost = args[0];
		contextArgs.apiKey = args[1];
		contextArgs.serviceId = args[2];
		
		ApiClient apiClient = contextArgs.apiClient();
		
		SummarizedView view = ViewUtil.getServiceViewByName(apiClient, contextArgs.serviceId, "All Events");

		contextArgs.viewId = view.id;

		// example values
			
		String[] sampleValues = new String[] { "relative_to=Method_Calls", 
			"threshold=100", "rate=0.01",
			"label=Anomaly", "minInterval=24h", };

		String rawContextArgs = new Gson().toJson(contextArgs);
		RelativeThresholdFunction.execute(rawContextArgs, String.join("\n", sampleValues));
	}
}
