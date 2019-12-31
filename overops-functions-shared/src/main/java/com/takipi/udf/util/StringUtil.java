package com.takipi.udf.util;

public class StringUtil {
	public static String ellipsize(String str, int targetLength) {
		return ellipsize(str, targetLength, 1.0);
	}

	public static String ellipsize(String str, int targetLength, double location) {
		return ellipsize(str, targetLength, location, "...");
	}

	public static String ellipsize(String str, int targetLength, double location, String placeholder) {
		if (str.length() <= targetLength) {
			return str;
		}

		int netLength = targetLength - placeholder.length();

		int leftLength = (int) Math.round(netLength * location);
		int rightLength = netLength - leftLength;

		String leftStr = str.substring(0, leftLength);
		String rightStr = str.substring(str.length() - rightLength);

		return (leftStr + placeholder + rightStr);
	}

	public static String minimizeString(String input, int maxChars) {
		return minimizeString(input, maxChars, 0);
	}

	public static String minimizeString(String input, int maxChars, int charsFromEnd) {
		if (input.length() <= maxChars) {
			return input;
		} else if (maxChars == 2) {
			return "..";
		} else if (maxChars == 1) {
			return ".";
		} else if (maxChars == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		int leftPos = maxChars - charsFromEnd - 2;

		if (leftPos > 0) {
			sb.append(input.substring(0, leftPos));
		}

		sb.append("..");

		if (charsFromEnd > 0) {
			int rightPos = input.length() - charsFromEnd;

			if (rightPos >= 0) {
				sb.append(input.substring(rightPos));
			}
		}

		return sb.toString();
	}
}
