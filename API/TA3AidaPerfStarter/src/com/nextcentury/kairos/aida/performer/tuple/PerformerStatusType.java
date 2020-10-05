package com.nextcentury.kairos.aida.performer.tuple;

public enum PerformerStatusType {
	INITIALIZED("INITIALIZED"), PROCESSING("PROCESSING"), DONE_PROCESSING("DONE_PROCESSING"), PROCESSING_ERROR("PROCESSING_ERROR"),
	PROCESSING_WARNING("PROCESSING_WARNING"), WAITING_ON_DATA("WAITING_ON_DATA");

	private String status;

	PerformerStatusType(String type) {
		this.status = type;
	}

	public String getStatus() {
		return status;
	}
};