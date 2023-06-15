package com.nextcentury.kairos.experimentstatus;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.nextcentury.kairos.experimentstatus.tuple.ExperimentStatus;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentStatusResponse {
	@JsonProperty("experiments")
	private List<ExperimentStatus> statusList;

	public List<ExperimentStatus> getStatusList() {
		return statusList;
	}

	public void setStatusList(List<ExperimentStatus> statusList) {
		this.statusList = statusList;
	}
}
