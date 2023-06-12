package com.ncc.kairos.moirai.clotho.tinkerpop;


/**
 * This enum contains the list of available graph databases currently supported by clotho.
 *
 * @author ryan scott
 */
public enum GraphDatabases {
    /**
     * The following are the graph databases that clotho will support.
     */
    NEPTUNE, VALIDATION, NEO4J, ORIENTDB, BLAZEGRAPH, JANUSGRAPH, JANUSGRAPH_CASSANDRA;

    public static GraphDatabases getEnumFromString(String input) {
        //Implement this to return the correct database type based on different strings.
        switch (input.toLowerCase()) {
            case "neo4j":
                return NEO4J;
            case "orientdb":
                return ORIENTDB;
            case "blazegraph":
                return BLAZEGRAPH;
            case "janusgraph":
                return JANUSGRAPH;
            case "cassandra":
                return JANUSGRAPH_CASSANDRA;
            case "validation":
                return VALIDATION;
            case "neptune":
                return NEPTUNE;
            default:
                throw new IllegalArgumentException("Unrecognized DB_TYPE environment variable: " + input);
        }
    }
}

