package com.nextcentury.kairos.aida.performer.algorithm.entrypoint.io;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntrypointResponse {
	@JsonProperty("requestId")
	private String requestId;

	@JsonProperty("content")
	private String content;

	public EntrypointResponse() {
		super();
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return "EntrypointResponse [requestId=" + requestId + ", content=" + content + "]";
	}
}
