package com.ncc.kairos.moirai.zeus.services;

import com.ncc.kairos.moirai.zeus.resources.EnvironmentTier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service layer for storing and accessing user and admin services.
 * @author rscott
 * @version 0.1
 */
@Service
public class PropertiesService {

    @Value("${environmentTier}")
    private String environmentProp;

    @Value("${jwt.token.secret}")
    private String other;

    private String experimentDataBucket = "kairos-experiment-data";

    @Value("${kairos.s3.performerdata.bucket}")
    private String performerDataBucket;

    @Value("${kairos.s3.performerdata.basekey}")
    private String performerDataBaseKey;

    @Value("${kairos.s3.performerdata.history}")
    private boolean createHistory;

    @Value("${awsRegion}")
    private String awsRegion;

    @Value("${kairos.retry.attempts}")
    private String retryAttempts;

    public EnvironmentTier whichEnvironment() {
        switch (environmentProp.toLowerCase().strip()) {
            case "dev":
            case "development":
                return EnvironmentTier.DEVELOPMENT;
            case "test":
            case "testing":
                return EnvironmentTier.TESTING;
            case "staging":
            case "stage":
                return EnvironmentTier.STAGING;
            case "prod":
            case "production":
                return EnvironmentTier.PRODUCTION;
            default:
                return EnvironmentTier.PRODUCTION;
                //TODO default needs to be replaced with error but first we need to update infrastructure
                //to set this property so its not failing
        }
    }

    public String getPerformerDataBucket() {
        return performerDataBucket;
    }

    public String getExperimentDataBucket() {
        return experimentDataBucket;
    }

    public String getPerformerBaseKey() {
        return performerDataBaseKey;
    }

    public boolean getCreateHistory() {
        return createHistory;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public int getRetryAttempts() {
        return Integer.parseInt(retryAttempts);
    }
}
