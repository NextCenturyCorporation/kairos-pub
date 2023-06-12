package com.ncc.kairos.moirai.clotho.resources;

import com.ncc.kairos.moirai.clotho.utilities.PropertiesExtractor;

@SuppressWarnings("checkstyle:JavadocVariable")
public class ApplicationConstants {
    static {
        SDF_VERSION_VALUE = PropertiesExtractor.getProperty("kairos.sdf.sdf-version");
        CONTEXT_VERSION = PropertiesExtractor.getProperty("kairos.sdf.context-version");
    }

    public static final String CONTEXT_VERSION;
    public static final String SDF_VERSION_VALUE;
    public static final String DATABASE_URI_ENVIRONMENT_VARIABLE = "DB_URI";
    public static final String DATABASE_TYPE_ENVIRONMENT_VARIABLE = "DB_TYPE";

    // JanusGraph-Cassandra specific environment variables
    public static final String CASSANDRA_BACKEND_ENVIRONMENT_VARIABLE = "CASSANDRA_BACKEND_CQL";
    public static final String CASSANDRA_HOSTNAME_ENVIRONMENT_VARIABLE = "CASSANDRA_HOST_NAME";
    public static final String CASSANDRA_CQL_KEYSPACE_ENVIRONMENT_VARIABLE = "CASSANDRA_CQL_KEYSPACE";

    // Ontology upload files
    public static final String RELATIONS_FILE = "/ontology/relations.json";
    public static final String VALIDATION_FILE = "/ontology/kairos-context.jsonld";

    // Kairos-Context Url : note that java RegEx assumes beginning and end of string, so must explicitly indicate that characters can appear before or after.
    public static final String KAIROS_CONTEXT_S3_URL_REGEX = "(.*)https:\\/\\/kairos-sdf\\.s3\\.amazonaws\\.com\\/context\\/kairos-v\\S+\\.jsonld(.*)";
    public static final String KAIROS_CONTEXT_S3_URL = String.format("(.*)https:\\/\\/kairos-sdf\\.s3\\.amazonaws\\.com\\/context\\/kairos-v%s\\.jsonld(.*)", CONTEXT_VERSION);
    public static final String KAIROS_CONTEXT_STRING = String.format("https://kairos-sdf.s3.amazonaws.com/context/kairos-v%s.jsonld\"", CONTEXT_VERSION);
}
