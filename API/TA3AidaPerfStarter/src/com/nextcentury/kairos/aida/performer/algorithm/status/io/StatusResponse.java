package com.nextcentury.kairos.aida.performer.algorithm.status.io;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusResponse {
	@JsonProperty("status")
	private PerformerStatusResponseType status;

	@JsonProperty("message")
	private String message;

	public PerformerStatusResponseType getStatus() {
		return status;
	}

	public void setStatus(PerformerStatusResponseType status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "StatusResponse [status=" + status + ", message=" + message + "]";
	}
}