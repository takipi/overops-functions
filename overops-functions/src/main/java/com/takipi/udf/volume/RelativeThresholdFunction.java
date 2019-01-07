package com.takipi.udf.volume;

import com.takipi.udf.util.TestUtil;

public class RelativeThresholdFunction extends ThresholdFunction {
	public static String validateInput(String rawInput) {
		return getThresholdInput(rawInput).toString();
	}

	public static void execute(String rawContextArgs, String rawInput) {
		execute(rawContextArgs, getThresholdInput(rawInput));
	}

	// A sample program on how to programmatically activate
	// RelativeThresholdFunction

	public static void main(String[] args) {
		String rawContextArgs = TestUtil.getViewContextArgs(args, "All Events");

		// example values
		String[] sampleValues = new String[] { "relative_to=Method_Calls", "threshold=100", "rate=0.01",
				"label=Anomaly", "min_interval=24h", };

		RelativeThresholdFunction.execute(rawContextArgs, String.join("\n", sampleValues));
	}
}
