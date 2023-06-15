package com.nextcentury.kairos.experimentstatus.tuple;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnclavePodStatus {

	@JsonProperty("podname")
	protected String podname;

	@JsonProperty("podstatus")
	protected String podstatus;

	public EnclavePodStatus() {
		super();
	}

	public String getPodname() {
		return podname;
	}

	public void setPodname(String podname) {
		this.podname = podname;
	}

	public String getPodstatus() {
		return podstatus;
	}

	public void setPodstatus(String podstatus) {
		this.podstatus = podstatus;
	}

}
