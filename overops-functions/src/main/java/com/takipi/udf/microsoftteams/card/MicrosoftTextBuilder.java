package com.takipi.udf.microsoftteams.card;

import java.util.List;

import com.takipi.api.client.data.event.Location;

public class MicrosoftTextBuilder {
	private StringBuilder stringBuilder = new StringBuilder();

	public MicrosoftTextBuilder add(String text) {
		stringBuilder.append(text);

		return this;
	}

	public MicrosoftTextBuilder addBold(String text) {
		stringBuilder.append("<strong>").append(text).append("</strong>");

		return this;
	}

	public MicrosoftTextBuilder addQuote(String text) {
		stringBuilder.append("<blockquote>").append(text).append("</blockquote>");

		return this;
	}

	public MicrosoftTextBuilder addHighlighted(String text) {
		stringBuilder.append("<pre>").append(text).append("</pre>");

		return this;
	}

	public MicrosoftTextBuilder addHighlightedQuote(String text) {
		stringBuilder.append("<blockquote><pre>").append(text).append("</pre></blockquote>");

		return this;
	}

	public MicrosoftTextBuilder addLink(String link, String nameOfLink) {
		stringBuilder.append("<a href=\"").append(link).append("\">").append(nameOfLink).append("</a>");

		return this;
	}

	public MicrosoftTextBuilder addBoldLink(String link, String nameOfLink) {
		stringBuilder.append("<a href=\"").append(link).append("\">").append("<strong>").append(nameOfLink)
				.append("</strong>").append("</a>");

		return this;
	}

	public MicrosoftTextBuilder addEnter() {
		stringBuilder.append("\n\n");

		return this;
	}

	public MicrosoftTextBuilder addArray(List<Location> stackFrames, String prefix) {
		for (Location stack_frame : stackFrames) {
			stringBuilder.append(prefix).append(stack_frame.prettified_name).append("\n");
		}

		return this;
	}

	public String build() {
		return stringBuilder.toString();
	}
}
