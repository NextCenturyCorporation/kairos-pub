package com.nextcentury.kairos.performer.algorithm.status.io;

public enum PerformerStatusResponseType {

	INITIALIZING("INITIALIZING"), PROCESSING("PROCESSING"), ERROR("ERROR"), WARNING("WARNING"), WAITING("WAITING");

	private String status;

	PerformerStatusResponseType(String type) {
		this.status = type;
	}

	public String getStatus() {
		return status;
	}
}
