package com.nextcentury.kairos.result.enqueue;

import org.codehaus.jackson.annotate.JsonProperty;

public class SubmissionNotification {
    @JsonProperty("runId")
    private String runId;

    @JsonProperty("ceid")
    private String ceid;

    @JsonProperty("ta1name")
    private String ta1name;

    @JsonProperty("experimenttype")
    private String experimentType;

    @JsonProperty("s3Location")
    private String s3Location;

    @JsonProperty("performername")
    private String performername;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getCeid() {
        return ceid;
    }

    public void setCeid(String ceid) {
        this.ceid = ceid;
    }

    public String getTa1name() {
        return ta1name;
    }

    public void setTa1name(String ta1name) {
        this.ta1name = ta1name;
    }

    public String getExperimentType() {
        return experimentType;
    }

    public void setExperimentType(String experimentType) {
        this.experimentType = experimentType;
    }

    public String getS3Location() {
        return s3Location;
    }

    public void setS3Location(String s3Location) {
        this.s3Location = s3Location;
    }

    public String getPerformername() {
        return performername;
    }

    public void setPerformername(String performername) {
        this.performername = performername;
    }
}
