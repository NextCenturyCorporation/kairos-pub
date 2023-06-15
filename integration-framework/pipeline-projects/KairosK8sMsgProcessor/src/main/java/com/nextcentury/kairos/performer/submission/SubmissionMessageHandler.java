package com.nextcentury.kairos.performer.submission;

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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class SubmissionMessageHandler {
    private static final Logger logger = LogManager.getLogger(SubmissionMessageHandler.class);

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

    public SubmissionMessageHandler(MsgEnqueuer msgEnqueuer, ProcessorConfig config) {
        super();
        this.kafkaBrokers = config.getKafkaBrokers();
        this.msgEnqueuer = msgEnqueuer;
        this.resultQName = config.getResultQName();
        this.errorQName = config.getErrorQName();
        this.evaluator = config.getEvaluatorName();
        this.experiment = config.getExperimentName();
        this.startTimeId = config.getStartTimeId();
    }

    public void handleSubmissionMessage(HttpExchange exchange) {
        logger.info("URI ---> " + exchange.getRequestURI().toString());

        OutputStream responseBody = null;
        BufferedReader reader = null;
        InputStreamReader isReader = null;

        try {
            StringBuffer buff = new StringBuffer();
            String line = null;
            isReader = new InputStreamReader(exchange.getRequestBody());
            reader = new BufferedReader(isReader);
            while ((line = reader.readLine()) != null) {
                buff.append(line);
            }
            // SubmissionMessage inputObject = mapper.readValue(buff.toString(), new
            // TypeReference<SubmissionMessage>() {
            // });

            // do something with the submission message
            forwardSubmissionMessage(buff.toString());

            exchange.sendResponseHeaders(HttpStatus.SC_OK, 0);
            // write the response back
            exchange.getResponseHeaders().set(CONTENT_TYPE, APPLICATION_JSON);
            responseBody = exchange.getResponseBody();
            responseBody.write("submitted".getBytes());
        } catch (Throwable e) {
            System.err.println(ExceptionHelper.getExceptionTrace(e));
        } finally {
            if (isReader != null) {
                try {
                    isReader.close();
                } catch (Throwable e) {
                    System.err.println(ExceptionHelper.getExceptionTrace(e));
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable e) {
                    System.err.println(ExceptionHelper.getExceptionTrace(e));
                }
            }
            if (responseBody != null) {
                try {
                    responseBody.close();
                } catch (Throwable e) {
                    System.err.println(ExceptionHelper.getExceptionTrace(e));
                }
            }
        }
    }

    private void forwardSubmissionMessage(String submissionMessage) {
        logger.info("Submission payload received");
        // do what we are doing right now
        // write result to result queue and to es
        if (submissionMessage != null && !submissionMessage.isEmpty()) {
            // if (validSubmission(submissionMessage)) {
            // msgEnqueuer.push(resultQName, submissionMessage);

            // es6
            // esPersister.persist(submissionMessage);

            // s3 uploader and kafka output topic poster
            new S3Uploader().upload(evaluator, experiment, submissionMessage, kafkaBrokers, startTimeId);

            // }
        } else {
            System.err.println("No result - nothing to put to queue");
        }
    }

    private boolean validSubmission(String submissionMessage) {
        SubmissionMessage inputObject = null;
        boolean returnFlag = false;
        try {
            inputObject = mapper.readValue(submissionMessage, new TypeReference<SubmissionMessage>() {
            });
            returnFlag = inputObject.getRunId() != null && !inputObject.getRunId().isEmpty();
            if (returnFlag == false) {
                System.err.println("Invalid submission - no runId");
                System.err.println(inputObject);
            }
        } catch (Throwable e) {
            System.err.println(ExceptionHelper.getExceptionTrace(e));
        }

        return returnFlag;
    }
}
