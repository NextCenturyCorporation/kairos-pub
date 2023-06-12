package com.ncc.kairos.moirai.clotho.resources;

@SuppressWarnings("checkstyle:JavadocVariable")
public class QueryConstants {
    public static final String GRAPHNAME = "GRAPHNAME";
    public static final String EVENT_KEY = "EVENT_KEY";
    public static final String PREDICATE_OBJECT = "PREDICATE_OBJECT";
    public static final String INSERTED_OBJECT = "INSERTED_OBJECT";

    public static final String FROM = " FROM <" + GRAPHNAME + "> ";

    public static final String SELECT_S_P_O = " select ?s ?p ?o ";

    public static final String SELECT_S_O = " select ?s ?o ";

    public static final String CONSTRUCT_S_P_O = " construct { ?s ?p ?o } ";

    // Construct
    public static final String WHERE_EVENT_TREE = " WHERE { " +
        " <CEID> (<>|!<>)* ?o . ?s ?p ?o .  }";

    // Select
    public static final String WHERE_EVENT_LIST = " WHERE {  ?sub <https://kairos-sdf.s3.amazonaws.com/context/kairos/instances> ?s . " +
        "OPTIONAL { " +
            "?s <https://kairos-sdf.s3.amazonaws.com/context/kairos/confidence> ?blank .  " +
            "?blank ?whocares ?o .  " +
            "FILTER (?o != <http://www.w3.org/1999/02/22-rdf-syntax-ns#nil>)  " +
        "} " +
        "OPTIONAL { " +
            "?s <http://schema.org/name> ?p . " +
            "FILTER (?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#nil>) " +
          "} " +
    "} ";

    
    public static final String DELETE_RDF_TRIPLE =
    "delete { " + 
        "GRAPH <" + GRAPHNAME + "> { " +
            "<" + EVENT_KEY + "> <" + PREDICATE_OBJECT + "> ?o . " +
        "}" +
    "} WHERE { " +
    "?s <" + PREDICATE_OBJECT + "> ?o . " +
    "FILTER(?s = <" + EVENT_KEY + ">)} ";


    public static final String INSERT_RDF_TRIPLE =
        " insert " +
        " { " +
        " GRAPH <" + GRAPHNAME + "> { " +
         " ?s <" + PREDICATE_OBJECT + "> '" + INSERTED_OBJECT + "' . " +
        "} " +
    "} where " +
    "{ " +
        " GRAPH <" + GRAPHNAME + "> { " +
          " ?s ?p ?o . " +
          " FILTER(?s = <" + EVENT_KEY + ">) " +
        " } " +
    " } ";

    public static final String ADDITIONAL_NOTES_PREDICATE = "https://kairos-sdf.s3.amazonaws.com/context/kairos/additionalNotes";

    public static final String USER_EDIT_PREDICATE = "https://kairos-sdf.s3.amazonaws.com/context/kairos/userEdit";
}
