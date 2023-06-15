package com.nextcentury.kairos.performer.submission;

public enum ExperimentType {

	task1("task1"), task2("task2"),TASK1("TASK1"), TASK2("TASK2");

	private String name;

	ExperimentType(String type) {
		this.name = type;
	}

	public String getExperimentName() {
		return name.toLowerCase();
	}
	
	public String toString() {
		return name.toLowerCase();
	}
};
