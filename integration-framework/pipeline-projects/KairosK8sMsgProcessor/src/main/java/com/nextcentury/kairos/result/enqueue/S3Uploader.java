package com.nextcentury.kairos.result.enqueue;

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class S3Uploader {
    private static final Logger logger = LogManager.getLogger(S3Uploader.class);

    public static final String DASH = "-";
    private static AmazonS3 s3Client = null;
    private static Regions clientRegion = null;

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(Inclusion.NON_NULL);
        mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
    }

    static {
        clientRegion = Regions.US_EAST_1;
        s3Client = AmazonS3ClientBuilder.standard().withRegion(clientRegion).build();
    }

    private String submissionMessage;
    private String kafkaBrokers;
    private String bucket;

    String runId = null;
    String ceId = null;
    String experimenttype = null;
    String ta1name = null;
    String performername = null;
    String data = null;
    String fileName = null;

    private void getSubmissionMetadata() {
        try {
            Map<String, String> map = mapper.readValue(submissionMessage, Map.class);
            runId = map.get("runId");
            ceId = map.get("ceid");
            experimenttype = map.get("experimenttype");
            fileName = map.get("filename");
            ta1name = map.get("ta1name");
            performername = map.get("performername");
            data = (map.get("data") instanceof String) ? map.get("data") : mapper.writeValueAsString(map.get("data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void upload(String experiment, String startTimeId, FileItem fileItem, Map<String, String> parameters) {
        this.bucket = "kairos-experiment-output";

        if (s3Client == null) {
            logger.info("Error initializing s3 client");
        } else {
            try {
                String key = new StringBuilder(experiment)
                        .append("/").append(startTimeId)
                        .append("/").append(parameters.get("performername"))
                        .append("/hrf/").append(parameters.get("runId"))
                        .toString();
                        
                String filename = new StringBuilder(parameters.get("ta1name")).append(DASH)
                    .append(parameters.get("performername")).append(DASH)
                    .append(parameters.get("experimenttype")).append(DASH)
                    .append(parameters.get("ceid")).append("-hrf.")
                    .append(FilenameUtils.getExtension(fileItem.getName())).toString();

                String fqKey = new StringBuilder(key).append("/").append(filename).toString();

                File file = new File(filename);
                fileItem.write(file);

                if (s3Client.doesObjectExist(bucket, fqKey)) {
                    logger.info("Removing existing file: {}", fqKey);
                    s3Client.deleteObject(bucket, fqKey);
                }

                logger.info(String.format("Uploading submission to bucket %s, key %s", bucket, fqKey));
                s3Client.putObject(bucket, fqKey, file); // overwrite if exists

                // Cleanup
                file.delete();

            } catch (SdkClientException e) {
                e.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void upload(String evaluator, String experiment, String submissionMessage, String kafkaBrokers,
            String startTimeId) {
        this.bucket = "kairos-experiment-output";
        this.submissionMessage = submissionMessage;
        this.kafkaBrokers = kafkaBrokers;
        getSubmissionMetadata();

        if (s3Client == null) {
            logger.info("Error initializing s3 client");
        } else {
            if (runId == null || runId.isEmpty()) {
                logger.info("Error retrieving runid from message");
                return;
            }

            String key = new StringBuilder(experiment)
                    .append("/").append(startTimeId)
                    .append("/").append(performername)
                    .append("/submission-results/").append(runId)
                    .toString();

            String filename = new StringBuilder(ta1name).append(DASH)
                    .append(performername).append(DASH)
                    .append(experimenttype).append(DASH)
                    .append(ceId).append(".json").toString();
            String fqKey = new StringBuilder(key).append("/").append(filename).toString();
            logger.info(String.format("Uploading submission to bucket %s, key %s", bucket, fqKey));
            try {
                PutObjectResult result = s3Client.putObject(bucket, fqKey, data);

                // upload is ok, now post the submission notification
                postNotification(evaluator, experiment, kafkaBrokers, fqKey);
            } catch (SdkClientException e) {
                e.printStackTrace();
            }
        }
    }

    private void postNotification(String evaluator, String experiment, String kafkaBrokers, String fqKey) {
        String outputtopic = new StringBuilder(evaluator).append(DASH).append(experiment).append(DASH)
                .append("output-topic").toString().toLowerCase();

        SubmissionNotification submissionNotification = new SubmissionNotification();
        submissionNotification.setCeid(ceId);
        submissionNotification.setExperimentType(experimenttype);
        submissionNotification.setPerformername(performername);
        submissionNotification.setRunId(runId);
        submissionNotification.setTa1name(ta1name);
        submissionNotification.setS3Location("s3://" + bucket + "/" + fqKey);
        String submissionNotificationString = null;
        try {
            submissionNotificationString = new String(
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(submissionNotification));

            new OutputTopicPoster().postSubmission(kafkaBrokers, outputtopic, fqKey, submissionNotificationString);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
