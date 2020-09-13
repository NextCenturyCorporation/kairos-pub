package com.nextcentury.kairos.aida.performer.tuple;

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