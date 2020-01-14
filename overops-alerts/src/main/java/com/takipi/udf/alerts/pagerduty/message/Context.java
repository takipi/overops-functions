package com.takipi.udf.alerts.pagerduty.message;

public class Context {
	public final String type;
	public final String href;
	public final String text;
	public final String src;
	public final String alt;

	Context(String type, String href, String text, String src, String alt) {
		this.type = type;
		this.href = href;
		this.text = text;
		this.src = src;
		this.alt = alt;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String type;
		private String href;
		private String text;
		private String src;
		private String alt;

		Builder() {

		}

		public Builder setType(String val) {
			this.type = val;

			return this;
		}

		public Builder setHref(String val) {
			this.href = val;

			return this;
		}

		public Builder setText(String val) {
			this.text = val;

			return this;
		}

		public Builder setSrc(String val) {
			this.src = val;

			return this;
		}

		public Builder setAlt(String val) {
			this.alt = val;

			return this;
		}

		public Context build() {
			return new Context(type, href, text, src, alt);
		}
	}
}
