package com.nextcentury.kairos.tuple;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Kairos Message data type
 * 
 * @author kdeshpande
 * 
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KairosMessage {
	@JsonProperty("createdAt")
	private String createdAt;

	@JsonProperty("id")
	private String id;

	@JsonProperty("enclave")
	private String enclave;

	@JsonProperty("processedBy")
	private String processedBy;

	@JsonProperty("inputData")
	private ValueTuple inputData;

	@JsonProperty("processResult")
	private ReturnValueTuple processResult;

	private List<String> errors = new ArrayList<String>();

	public KairosMessage() {
		super();
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getProcessedBy() {
		return processedBy;
	}

	public void setProcessedBy(String processedBy) {
		this.processedBy = processedBy;
	}

	public String getEnclave() {
		return enclave;
	}

	public void setEnclave(String enclave) {
		this.enclave = enclave;
	}

	/*
	 * public String getS3content() { return s3content; }
	 * 
	 * public void setS3content(String s3content) { this.s3content = s3content; }
	 */

	public ValueTuple getInputData() {
		return inputData;
	}

	public void setInputData(ValueTuple inputData) {
		this.inputData = inputData;
	}

	public ReturnValueTuple getProcessResult() {
		return processResult;
	}

	public void setProcessResult(ReturnValueTuple processResult) {
		this.processResult = processResult;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}

	@Override
	public String toString() {
		return "AidaDataTuple [createdAt=" + createdAt + ", id=" + id + ", enclave=" + enclave + ", processedBy="
				+ processedBy + ", inputData=" + inputData + ", processResult=" + processResult + ", errors=" + errors
				+ "]";
	}

}
