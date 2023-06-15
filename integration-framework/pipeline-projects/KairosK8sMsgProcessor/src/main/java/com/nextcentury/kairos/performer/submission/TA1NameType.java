package com.nextcentury.kairos.performer.submission;

public enum TA1NameType {
	sbu("sbu"), ibm("ibm"), isi("isi"), cmu("cmu"), jhu("jhu"), resin("resin"),
	SBU("SBU"), IBM("IBM"), ISI("ISI"), CMU("CMU"), JHU("JHU"), RESIN("RESIN");

	private String name;

	TA1NameType(String type) {
		this.name = type;
	}

	public String getName() {
		return name.toLowerCase();
	}
	
	public String toString() {
		return name.toLowerCase();
	}
}
