package com.nextcentury.kairos;

public class ProcessorConfig {
    private static final String DASH = "-";
    private static final String DEFAULT_PORT = "80";

    public String getKafkaBrokers() {
        return kafkaBrokers;
    }

    public void setKafkaBrokers(String kafkaBrokers) {
        this.kafkaBrokers = kafkaBrokers;
    }

    private String kafkaBrokers;
    private String evaluatorName;
    private String experimentName;
    private String performerName;
    private String resultQName;
    private String errorQName;
    private String serviceName;
    private String port;
    private String entryPointpathSpec;
    private String esHost;
    private String fqPerformerSericeUrl;

    private String inputRedisTopic;
    private String startTimeId;

    public String getInputRedisTopic() {
        inputRedisTopic = new StringBuffer(evaluatorName).append(DASH).append(experimentName).append(DASH)
                .append("input-redis-topic").toString().toLowerCase();
        return inputRedisTopic;
    }

    public String getEvaluatorName() {
        return evaluatorName;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public String getPerformerName() {
        return performerName;
    }

    public String getResultQName() {
        resultQName = new StringBuffer(evaluatorName).append(DASH).append(experimentName).append(DASH)
                .append("output-redis-queue").toString().toLowerCase();
        return resultQName;
    }

    public String getErrorQName() {
        resultQName = new StringBuffer(evaluatorName).append(DASH).append(experimentName).append(DASH)
                .append("error-redis-queue").toString().toLowerCase();
        return resultQName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getStartTimeId() {
        return startTimeId;
    }

    public String getPort() {
        return port;
    }

    public String getEntryPointpathSpec() {
        return entryPointpathSpec;
    }

    public void setEntryPointpathSpec(String entryPointpathSpec) {
        this.entryPointpathSpec = entryPointpathSpec;
    }

    public String getEsHost() {
        return esHost;
    }

    public void setEsHost(String esHost) {
        this.esHost = esHost;
    }

    public String getFqPerformerSericeUrl() {
        return fqPerformerSericeUrl;
    }

    public void setFqPerformerSericeUrl(String fqPerformerSericeUrl) {
        this.fqPerformerSericeUrl = fqPerformerSericeUrl;
    }

    public static String getDash() {
        return DASH;
    }

    public static String getDefaultPort() {
        return DEFAULT_PORT;
    }

    public void setEvaluatorName(String evaluatorName) {
        this.evaluatorName = evaluatorName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public void setPerformerName(String performerName) {
        this.performerName = performerName;
    }

    public void setResultQName(String resultQName) {
        this.resultQName = resultQName;
    }

    public void setErrorQName(String errorQName) {
        this.errorQName = errorQName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setStartTimeId(String startTimeId) {
        this.startTimeId = startTimeId;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setPathSpec(String entryPointpathSpec) {
        this.entryPointpathSpec = entryPointpathSpec;
    }

    public void setInputRedisTopic(String inputRedisTopic) {
        this.inputRedisTopic = inputRedisTopic;
    }

    private ProcessorConfig() {
    }

    public static class Builder {
        private String kafkaBrokers;
        private String evaluatorName;
        private String experimentName;
        private String performerName;
        private String serviceName;
        private String startTimeId;
        private String port = DEFAULT_PORT;
        private String entryPointpathSpec;
        private String esHost;
        private String fqPerformerSericeUrl;

        public Builder kafkaBrokers(String kafkaBrokers) {
            this.kafkaBrokers = kafkaBrokers;
            return this;
        }

        public Builder evaluator(String evaluator) {
            this.evaluatorName = evaluator;
            return this;
        }

        public Builder experiment(String experiment) {
            this.experimentName = experiment;
            return this;
        }

        public Builder performer(String performer) {
            this.performerName = performer;
            return this;
        }

        public Builder service(String service) {
            this.serviceName = service;
            return this;
        }

        public Builder startTimeId(String startTimeId) {
            this.startTimeId = startTimeId;
            return this;
        }

        public Builder port(String port) {
            this.port = port;
            return this;
        }

        public Builder entryPointpathSpec(String pathSpec) {
            this.entryPointpathSpec = pathSpec;
            return this;
        }

        public Builder fqPerformerSericeUrl(String fqPerformerSericeUrl) {
            this.fqPerformerSericeUrl = fqPerformerSericeUrl;
            return this;
        }

        public Builder esHost(String esHost) {
            this.esHost = esHost;
            return this;
        }

        public ProcessorConfig build() {
            ProcessorConfig cfg = new ProcessorConfig();
            cfg.kafkaBrokers = kafkaBrokers;
            cfg.evaluatorName = evaluatorName;
            cfg.experimentName = experimentName;
            cfg.performerName = performerName;
            cfg.serviceName = serviceName;
            cfg.startTimeId = startTimeId;
            cfg.port = port;
            cfg.entryPointpathSpec = entryPointpathSpec;
            cfg.esHost = esHost;
            cfg.fqPerformerSericeUrl = fqPerformerSericeUrl;

            return cfg;
        }
    }
}
