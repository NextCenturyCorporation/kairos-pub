package com.nextcentury.kairos;

public class KafkaConfig {
	private String kafkaBrokers;
	private String inputKafkaTopic;
	private String outputKafkaTopic;
	private String errorKafkaTopic;

	public KafkaConfig(String kafkaBrokers, String inputKafkaTopic, String outputKafkaTopic, String errorKafkaTopic) {
		this.kafkaBrokers = kafkaBrokers;
		this.inputKafkaTopic = inputKafkaTopic;
		this.outputKafkaTopic = outputKafkaTopic;
		this.errorKafkaTopic = errorKafkaTopic;
	}

	public String getKafkaBrokers() {
		return kafkaBrokers;
	}

	public void setKafkaBrokers(String kafkaBrokers) {
		this.kafkaBrokers = kafkaBrokers;
	}

	public String getInputKafkaTopic() {
		return inputKafkaTopic;
	}

	public void setInputKafkaTopic(String inputKafkaTopic) {
		this.inputKafkaTopic = inputKafkaTopic;
	}

	public String getOutputKafkaTopic() {
		return outputKafkaTopic;
	}

	public void setOutputKafkaTopic(String outputKafkaTopic) {
		this.outputKafkaTopic = outputKafkaTopic;
	}

	public String getErrorKafkaTopic() {
		return errorKafkaTopic;
	}

	public void setErrorKafkaTopic(String errorKafkaTopic) {
		this.errorKafkaTopic = errorKafkaTopic;
	}

	@Override
	public String toString() {
		return "KafkaConfig [kafkaBrokers=" + kafkaBrokers + ", inputKafkaTopic=" + inputKafkaTopic
				+ ", outputKafkaTopic=" + outputKafkaTopic + ", errorKafkaTopic=" + errorKafkaTopic + "]";
	}
}
