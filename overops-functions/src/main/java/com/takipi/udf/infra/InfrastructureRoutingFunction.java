package com.takipi.udf.infra;

import com.takipi.udf.util.TestUtil;

public class InfrastructureRoutingFunction extends RoutingFunction {
	private static String adjustInput(String rawInput) {
		return rawInput + "\nrouting_type=infra";
	}

	public static String validateInput(String rawInput) {
		getRoutingInput(adjustInput(rawInput));

		return "Infrastructure";
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
		String[] sampleValues = new String[] { "category_name=tiers", "namespaces=org.comp=Comp" };

		InfrastructureRoutingFunction.execute(rawContextArgs, String.join("\n", sampleValues));
	}
}
