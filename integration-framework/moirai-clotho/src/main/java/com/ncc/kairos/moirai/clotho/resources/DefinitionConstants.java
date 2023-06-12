package com.ncc.kairos.moirai.clotho.resources;

@SuppressWarnings("checkstyle:JavadocVariable")
public class DefinitionConstants extends GraphConstants {
    public static final String ENTITY = "entity";
    public static final String EVENT = "event";
    public static final String EVENT_ID = "eventID";
    public static final String RELATION = "relation";
    public static final String SCHEMA = "schema";
    public static final String SUBTYPE_OF = "subtypeOf";
    public static final String ROLE = "role";
    public static final String STEP = "step";
    public static final String SLOT = "slot";
    public static final String PARTICIPANT = "participant";
    public static final String SLOT_VALUES = "values";

    // Edge Labels
    public static final String ROLE_EDGE_LABEL = "CONTAINS_ROLE";
    public static final String STEP_EDGE_LABEL = "CONTAINS_STEP";
    public static final String SLOT_EDGE_LABEL = "CONTAINS_SLOT";
    public static final String TEMPORAL_EDGE_LABEL = "CONTAINS_TEMPORAL";
    public static final String SLOT_VALUES_EDGE_LABEL = "CONTAINS_VALUES";
    public static final String STEP_PRECEDES = "PRECEDES";
    public static final String SUBTYPE_OF_EDGE_LABEL = "SUBTYPE_OF";
    public static final String REFERENCES = "REFERENCES";
    public static final String STEP_CONTAINS = "CONTAINS";
    public static final String STEP_OVERLAPS = "OVERLAPS";

    // Exception Names
    public static final String PRE_SCHEMA_INSERTION_ERROR = "Pre-SCHEMA-INSERTION-ERROR";
    public static final String PRE_EVENT_INSERTION_ERROR = "Pre-EVENT-INSERTION-ERROR";
    public static final String PRE_ENTITY_INSERTION_ERROR = "Pre-ENTITY-INSERTION-ERROR";
    public static final String STEP_INSERTION_ERROR = "STEP-INSERTION-ERROR";

    // Model properties; Subject to change based on KSF-updates
    public static final String STATUS = "Status";
    public static final String REPEATS = "Repeats";
    public static final String OPTIONAL = "Optional";
    public static final String STEPS_ORDER = "StepsOrder";
    public static final String LIKELINESS = "Likeliness";
    public static final String PATTERN = "Pattern";
    public static final String INDEX = "Index";
    public static final String FILLED_BY = "filled-by";
    public static final String REQUIRES_EVENT  = "RequiresEvent";
    public static final String ACHIEVES_EVENT = "AchievesEvent";
    public static final String REMOVES_EVENT = "RemovedEvent";
}
