package com.nextcentury.kairos.performer.submission;

import org.codehaus.jackson.annotate.JsonProperty;

public class SubmissionMessage {
	@JsonProperty("runId")
	private String runId;

	public String getCeid() {
		return ceid;
	}

	public void setCeid(String ceid) {
		this.ceid = ceid;
	}

	@JsonProperty("ceid")
	private String ceid;

	@JsonProperty("ta1name")
	private TA1NameType ta1name;

	@JsonProperty("experimenttype")
	private ExperimentType experimentType;

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

	public String getRunId() {
		return runId;
	}

	public void setRunId(String runId) {
		this.runId = runId;
	}


	public TA1NameType getTa1name() {
		return ta1name;
	}

	public void setTa1name(TA1NameType ta1name) {
		this.ta1name = ta1name;
	}

	public ExperimentType getExperimentType() {
		return experimentType;
	}

	public void setExperimentType(ExperimentType experimentType) {
		this.experimentType = experimentType;
	}

	@Override
	public String toString() {
		return "SubmissionMessage [runId=" + runId + ", ceId=" + ceid + ", ta1name=" + ta1name + ", experimentType="
				+ experimentType + ", data=" + data + ", performername=" + performername + "]";
	}

}
