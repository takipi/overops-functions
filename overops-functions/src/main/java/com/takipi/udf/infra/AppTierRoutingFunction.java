package com.takipi.udf.infra;

import com.takipi.udf.util.TestUtil;

public class AppTierRoutingFunction extends RoutingFunction {
	private static String adjustInput(String rawInput) {
		return rawInput + "\nrouting_type=app";
	}

	public static String validateInput(String rawInput) {
		getRoutingInput(adjustInput(rawInput));

		return "App Tiers";
	}

	public static void install(String rawContextArgs, String rawInput) {
		install(rawContextArgs, getRoutingInput(adjustInput(rawInput)));
	}

	public static void execute(String rawContextArgs, String rawInput) {
		execute(rawContextArgs, getRoutingInput(adjustInput(rawInput)));
	}

	// A sample program on how to programmatically activate
	// InfrastructureRoutingFunction
	public static void main(String[] args) {
		String rawContextArgs = TestUtil.getEventContextArgs(args);

		// some test values
		String[] sampleValues = new String[] { "category_name=Apps", "namespaces=org.comp=Comp" };

		AppTierRoutingFunction.execute(rawContextArgs, String.join("\n", sampleValues));
	}
}
