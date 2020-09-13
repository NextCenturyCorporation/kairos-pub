package com.nextcentury.kairos.aida.performer.tuple;

import org.codehaus.jackson.annotate.JsonProperty;

public class ValueTuple {

	@JsonProperty("value")
	private String value;
	
	public ValueTuple() {
		super();
		// TODO Auto-generated constructor stub
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
