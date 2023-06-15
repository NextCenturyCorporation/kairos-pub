package com.nextcentury.kairos.experimentstatus.tuple;

public enum PerformerStatusType {
	INITIALIZING("INITIALIZING"), PROCESSING("PROCESSING"), ERROR("ERROR"), WARNING("WARNING"), WAITING("WAITING");

	private String status;

	PerformerStatusType(String type) {
		this.status = type;
	}

	public String getStatus() {
		return status;
	}
};