package com.nextcentury.kairos.tuple;

import org.codehaus.jackson.annotate.JsonProperty;

public class PerformerStatusCheckTuple {
	
	public enum PerformerStatusType {
		INITIALIZED("Initialized"), PROCESSING("Processing"), DONE_PROCESSING("Done Processing"), PROCESSING_ERROR("Processing Error"),
		PROCESSING_WARNING("Processing Warning"), WAITING_ON_DATA("Waiting on Data");

		private String status;

		PerformerStatusType(String type) {
			this.status = type;
		}

		public String getStatus() {
			return status;
		}
	};
	
	@JsonProperty("performerstatus")
	private PerformerStatusType performerStatus;
	
	@JsonProperty("payload")
	private String payload;
	
	public PerformerStatusCheckTuple() {
		super();
	}

	public PerformerStatusType getPerformerStatus() {
		return performerStatus;
	}

	public void setPerformerStatus(PerformerStatusType performerStatus) {
		this.performerStatus = performerStatus;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	@Override
	public String toString() {
		return "PerformerStatusCheckTuple [performerStatus=" + performerStatus + ", payload=" + payload + "]";
	}
}
