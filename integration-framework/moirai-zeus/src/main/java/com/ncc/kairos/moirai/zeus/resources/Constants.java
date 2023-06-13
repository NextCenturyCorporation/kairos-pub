package com.ncc.kairos.moirai.zeus.resources;

import java.util.regex.Pattern;

/**
 * Constants class to hold all constants.
 * @author Lion Tamer
 */
@SuppressWarnings("checkstyle:JavadocVariable")
public class Constants {
    /**
     * Active @Service.
     */
    public static final String SERVICE_STATUS_ACTIVE = "Active";
    /**
     * Pending @Service.
     */
    public static final String SERVICE_STATUS_PENDING = "Pending";
    /**
     * Failed to partition @Service.
     */
    public static final String SERVICE_STATUS_FAILED = "Failed";
    /**
     * An @Service that was torn down manually.
     */
    public static final String SERVICE_STATUS_TERMINATED = "Terminated";

    // AWS Specific
    /**
     * AWS Prefix to Service tag names.
     */
    public static final String AWS_TAG_PREFIX = "clotho_";
    /**
     * Key name for the Services clotho endpoint.
     */
    public static final String AWS_CLOTHO_ENDPOINT_TAG = "ClothoEndpoint";
    /**
     * Key name for the Services graph database endpoint.
     */
    public static final String AWS_GRAPHDB_ENDPOINT_TAG = "DatabaseEndpoint";

    public static final String OPEN_ACCESS_KEYWORD = "Public";

    public static final String CLOTHO_TYPE = "clotho";
    public static final String PRIVATE_ACCESS_KEYWORD = "Private";
    public static final String IN_PROGRESS_STATUS = "In Progress";

    public static final Pattern HREF_PATTERN = Pattern.compile("<a href=\".+?\">.+?</a>");


    /**
     * DOCKER REGISTRY CONSTANTS.
     */
    // S3 Bucket Name
    public static final String DOCKER_REGISTRY_BUCKET_NAME = "kairos-docker-registries";
    // Registry Cname
    public static final String DOCKER_REGISTRY_CNAME = "docker-registry.kairos.nextcentury.com";
    // Flask Service CName 
    public static final String DOCKER_REGISTRY_FLASK_CNAME = "http://docker-registry.kairos.nextcentury.com:8008/registry";
    // Create Registry Rest Endpoint
    public static final String DOCKER_REGISTRY_CREATE_REGISTRY_EP = "/create";
    // Create Registry Rest Endpoint
    public static final String DOCKER_REGISTRY_DELETE_REGISTRY_EP = "/delete";
    // Password Reset Endpoint
    public static final String DOCKER_REGISTRY_PASSWORD_RESET_EP = "/password/reset";

    //Pieces for creating uris
    public static final String REPO_FOLDER = "repo/";
    public static final String DOCKER_REGISTRY_V2 = "/docker/registry/v2/";
    public static final String DOCKER_REGISTRY_V2_REPOSITORIES = "/docker/registry/v2/repositories/";
    public static final String MANIFEST_TAGS = "/_manifests/tags/";

    // Uri Patterns
    public static final Pattern DOCKER_REGISTRY_PATTERN = Pattern.compile("(?<=repo/)(.*)(?=/docker)");
    public static final Pattern DOCKER_IMAGE_PATTERN = Pattern.compile("(?<=repositories/)(.*)(?=/_layers)");
    public static final Pattern DOCKER_REPO_PATTERN = Pattern.compile("(?<=repositories/)(.*)(?=/_manifests)");
    public static final Pattern DOCKER_TAG_PATTERN = Pattern.compile("(?<=tags/)(.*)(?=/current)");
    public static final Pattern DOCKER_SHA256_PATTERN = Pattern.compile("(?<=sha256/)(.*)(?=/link)");

    /**
     * Local Paths.
     */
    public static final String MOIRAI_INFRASTRUCURE = "/usr/src/moirai-infrastructure/";
    public static final String TERRAFORM_FOLDER_LOCATION = MOIRAI_INFRASTRUCURE + "terraform/";
    public static final String ANSIBLE_FOLDER_LOCATION = MOIRAI_INFRASTRUCURE + "ansible/";
    public static final String PEM_FILE = MOIRAI_INFRASTRUCURE + "key-pairs/moirai-machine.pem";

    /**
     * Role Constants.
     */
    public static final String CREATE_SUB_TEAM_ROLE_NAME = "create-sub-team";
    public static final String CREATE_SUB_TEAM_PERMISSION_NAME = "CREATE-SUB-ACCOUNT";

    /**
     * Key Name for Request Attribute JwtUser.
     */
    public static final String ATTRIBUTE_JWTUSER = "jwtUser";

    /**
     * FAQ Statuses.
     */
    public static final String FAQ_DELETED = "Deleted";
    public static final String FAQ_ADDED = "Added";
    public static final String FAQ_MODIFIED = "Modified";

    /**
     * Error Messages.
     */
    public static final String RDS_PERSISTANCE_ERROR_MSG = "Unable to persist Service to RDS, Service: ";

    /**
     * Zeus setting constants.
     */
    public static final String ZS_REGISTRATION_ALLOWED = "registrationAllowed";
    public static final String ZS_MAIL_USERNAME = "mail.username";
    public static final String ZS_MAIL_PASSWORD = "mail.password";
    public static final String ZS_VALIDATION_ENDPOINT = "validation.endpoint";
    public static final String ZS_VALIDATION_SHOULDRUN = "validation.shouldrun";

    /**
     * Experiment Constants.
     */
    public static final String EXPERIMENT_K8S_ENV_ACTIVE = "Active";
    public static final String EXPERIMENT_K8S_ENV_TERMINATED = "Terminated";
    public static final String EXPERIMENT_K8S_ENV_PENDING = "Pending";
    public static final String EXPERIMENT_STATUS_URL = "http://status.hippodrome.kairos.nextcentury.com/kairos/experimentstatus";
}
