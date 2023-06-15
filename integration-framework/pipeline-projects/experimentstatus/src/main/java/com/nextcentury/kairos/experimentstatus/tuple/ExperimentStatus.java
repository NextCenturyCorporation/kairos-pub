package com.nextcentury.kairos.experimentstatus.tuple;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentStatus {
	@JsonProperty("age")
	private String age;

	@JsonProperty("experiment")
	private String experiment;

	@JsonProperty("frameworkPods")
	private List<EnclavePodStatus> frameworkPods = new ArrayList<>();

	@JsonProperty("namespace")
	private String namespace;

	@JsonProperty("performerPods")
	private List<EnclavePodStatus> performerPods = new ArrayList<>();

	@JsonProperty("pods")
	private Map<String, List<String>> pods = new HashMap<>();

	@JsonProperty("status")
	private String status;

	@JsonProperty("services")
	private List<EnclaveServiceStatus> services = new ArrayList<>();

	public ExperimentStatus() {
		super();
	}

	public String getAge() {
		return age;
	}

	public void setAge(String age) {
		this.age = age;
	}

	public String getExperiment() {
		return experiment;
	}

	public void setExperiment(String experiment) {
		this.experiment = experiment;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public List<EnclavePodStatus> getFrameworkPods() {
		return frameworkPods;
	}

	public void setFrameworkPods(List<EnclavePodStatus> frameworkPods) {
		this.frameworkPods = frameworkPods;
	}

	public Map<String,List<String>> getPods() {
		return pods;
	}

	public void setPods(Map<String,List<String>> pods) {
		this.pods = pods;
	}

	public List<EnclavePodStatus> getPerformerPods() {
		return performerPods;
	}

	public void setPerformerPods(List<EnclavePodStatus> performerPods) {
		this.performerPods = performerPods;
	}

	public List<EnclaveServiceStatus> getServices() {
		return services;
	}

	public void setServices(List<EnclaveServiceStatus> services) {
		this.services = services;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
