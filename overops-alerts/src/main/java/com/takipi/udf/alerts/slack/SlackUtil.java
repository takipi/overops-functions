package com.takipi.udf.alerts.slack;

public class SlackUtil {
	public static String formatLink(String title, String url) {
		return "<" + url + "|" + escapeSlackHtml(title) + ">";
	}

	public static String boldText(String text) {
		return "*" + text + "*";
	}

	public static String formatText(String text) {
		String s = text.replace('*', '-');

		return s.replaceAll("`|_|~|\n", "-");
	}

	public static String escapeSlackHtml(String str) {
		return str.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
	}
}
