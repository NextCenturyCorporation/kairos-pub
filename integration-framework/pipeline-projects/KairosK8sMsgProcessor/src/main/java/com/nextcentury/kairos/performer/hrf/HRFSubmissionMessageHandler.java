package com.nextcentury.kairos.performer.hrf;

import com.nextcentury.kairos.ProcessorConfig;
import com.nextcentury.kairos.result.enqueue.MsgEnqueuer;
import com.nextcentury.kairos.result.enqueue.S3Uploader;
import com.nextcentury.kairos.utils.ExceptionHelper;
import com.sun.net.httpserver.HttpExchange;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.type.TypeReference;
import java.util.Map;


import java.io.IOException;
import org.apache.commons.fileupload.FileItem;

public class HRFSubmissionMessageHandler extends AHandler {
    private static final Logger logger = LogManager.getLogger(HRFSubmissionMessageHandler.class);

    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "Content-Type";

    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(Inclusion.NON_NULL);
        mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
    }

    private MsgEnqueuer msgEnqueuer = null;

    private String resultQName;
    private String errorQName;

    private String evaluator;
    private String experiment;

    private String kafkaBrokers;

    private String startTimeId;
    private String performerName;

    public HRFSubmissionMessageHandler(MsgEnqueuer msgEnqueuer, ProcessorConfig config) {
        super();
        this.kafkaBrokers = config.getKafkaBrokers();
        this.msgEnqueuer = msgEnqueuer;
        this.resultQName = config.getResultQName();
        this.errorQName = config.getErrorQName();
        this.evaluator = config.getEvaluatorName();
        this.experiment = config.getExperimentName();
        this.startTimeId = config.getStartTimeId();
        this.performerName = config.getPerformerName();
    }

    public void handle(HttpExchange httpExchange, FileItem file, Map<String, String> parameters) throws IOException {
        new S3Uploader().upload(experiment, startTimeId, file, parameters);
    }

}
