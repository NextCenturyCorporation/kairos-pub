package com.nextcentury.kairos.performer.submission;

import org.codehaus.jackson.annotate.JsonProperty;

public class SubmissionMessage {
	@JsonProperty("data")
	private String data;

	@JsonProperty("performername")
	private String performername;

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getPerformername() {
		return performername;
	}

	public void setPerformername(String performername) {
		this.performername = performername;
	}
}
