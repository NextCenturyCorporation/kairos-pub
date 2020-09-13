package com.nextcentury.kairos.aida.performer.tuple;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReturnValueTuple {

	@JsonProperty("statusCode")
	private int statusCode;
	
	@JsonProperty("value")
	private String value;
	
	@JsonProperty("input")
	private String input;

	public ReturnValueTuple() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	@Override
	public String toString() {
		return "ReturnValueTuple [statusCode=" + statusCode + ", value=" + value + ", input=" + input + "]";
	}

}
