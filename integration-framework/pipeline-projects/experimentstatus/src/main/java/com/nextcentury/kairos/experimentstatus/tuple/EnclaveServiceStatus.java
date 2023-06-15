package com.nextcentury.kairos.experimentstatus.tuple;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.nextcentury.kairos.performer.algorithm.status.io.StatusResponse;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnclaveServiceStatus {
	private static final String SVC_PATTERN = "<service-name>.<namespace>.svc.cluster.local:<service-port>";
    private static final String SVC_NAME = "<service-name>";
    private static final String SVC_NAMESPACE = "<namespace>";
    private static final String SVC_PORT = "<service-port>";

	@JsonProperty("uri")
	protected String uri;

	@JsonProperty("processingState")
	protected String processingState;
	
	public EnclaveServiceStatus() {
		super();
	}

    public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

    public void setUri(String namespace, String name, String port) {
		this.uri = SVC_PATTERN.replace(SVC_NAME, name).replace(SVC_PORT, port).replace(SVC_NAMESPACE, namespace);
	}

	public String getProcessingState() {
		return processingState;
	}

	public void setProcessingState(String processingState) {
		this.processingState = processingState;
	}
}
