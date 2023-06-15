package com.nextcentury.kairos;

public class ServiceInvocationConfig {
	private boolean aidaPerformer;
	private boolean kairosPerformer;
	private String mountPathInput;
	private String inputRedisTopic;

	public ServiceInvocationConfig(boolean aidaPerformer, boolean kairosPerformer, String mountPathInput,String inputRedisTopic) {
		super();
		this.aidaPerformer = aidaPerformer;
		this.kairosPerformer = kairosPerformer;
		this.mountPathInput = mountPathInput;
		this.inputRedisTopic = inputRedisTopic;
	}

	public boolean isAidaPerformer() {
		return aidaPerformer;
	}

	public boolean isKairosPerformer() {
		return kairosPerformer;
	}

	public String getMountPathInput() {
		return mountPathInput;
	}

	public String getInputRedisTopic() {
		return inputRedisTopic;
	}

}
