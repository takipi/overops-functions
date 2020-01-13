package com.takipi.udf.alerts.util;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import com.takipi.common.util.Pair;

public class HTMLUtil {
	private static final Logger logger = LoggerFactory.getLogger(HTMLUtil.class);

	public static String minimizeHtml(String html) {
		try {
			HtmlCompressor hc = new HtmlCompressor();
			String htmlCompressed = hc.compress(html);

			return htmlCompressed;
		} catch (Exception e) {
			logger.error("Problem minimizing html", e);
			return html;
		}
	}

	public static String htmlLineBreak() {
		return "<br>";
	}

	public static String htmlBold(String content) {
		return htmlTag("b", content);
	}

	public static String htmlParagraph(String content) {
		return htmlTag("p", content);
	}

	public static String htmlTable(String content) {
		return htmlTag("table", content);
	}

	public static String htmlTableRow(String content) {
		return htmlTag("tr", content);
	}

	public static String htmlTableRowCell(String content, String width) {
		List<Pair<String, String>> attributes = Lists.newArrayList();

		if (!Strings.isNullOrEmpty(width)) {
			attributes.add(Pair.of("width", width));
		}

		return htmlTag("td", content, attributes);
	}

	public static String htmlLink(String content, String url, boolean openInNew) {
		List<Pair<String, String>> attributes = Lists.newArrayList();

		if (!Strings.isNullOrEmpty(url)) {
			attributes.add(Pair.of("href", url));
		}

		if (openInNew) {
			attributes.add(Pair.of("target", "_blank"));
		}

		return htmlTag("a", content, attributes);
	}

	public static String htmlImage(String imageAddress) {
		List<Pair<String, String>> attributes = Lists.newArrayList();

		if (!Strings.isNullOrEmpty(imageAddress)) {
			attributes.add(Pair.of("src", imageAddress));
		}

		return htmlTag("img", "", attributes);
	}

	public static String htmlTag(String tagName, String content) {
		return htmlTag(tagName, content, Collections.<Pair<String, String>>emptyList());
	}

	public static String htmlTag(String tagName, String content, List<Pair<String, String>> attributes) {
		return new StringBuilder().append("<").append(tagName).append(buildAttributesString(attributes)).append(">")
				.append(content).append("</").append(tagName).append(">").toString();
	}

	private static String buildAttributesString(List<Pair<String, String>> attributes) {
		// 'attributes' list pairs: first is name, second is value
		//
		StringBuilder attributeSb = new StringBuilder();

		for (Pair<String, String> attribute : attributes) {
			attributeSb.append(" ").append(attribute.getFirst()).append("=\"").append(attribute.getSecond())
					.append("\"");
		}

		return attributeSb.toString();
	}
}
