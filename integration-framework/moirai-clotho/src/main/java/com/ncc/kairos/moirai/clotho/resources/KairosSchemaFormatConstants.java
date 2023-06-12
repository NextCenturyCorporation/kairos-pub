package com.ncc.kairos.moirai.clotho.resources;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("checkstyle:JavadocVariable")
public class KairosSchemaFormatConstants extends DefinitionConstants {

    // json-ld ------------------------------------------------------------------------------------

    // Reserved Json-LD keywords
    public static final String JSON_LD_CONTEXT = "@context";
    public static final String JSON_LD_ID = "@id";
    public static final String JSON_LD_TYPE = "@type";
    public static final String JSON_LD_VALUE = "@value";
    public static final String JSON_LD_LIST = "@list";

    // Validation constants
    public static final int UNIQUE_ID_NUMDIGITS = 5;
    public static final int MAX_INSTANCES = 2;
    public static final int MAX_TOP_LEVEL_PRIMITIVES = 2;
    public static final int MAX_CHILDREN = 7;
    public static final int PROVENANCE_MAX_DURATION = 15;

    // Task-Designation
    public static final String TASK_2 = "task2";

    public static final String NONE = "none";
    public static final String KAIROS_NULL = "https://kairos-sdf.s3.amazonaws.com/context/kairos/NULL";
    public static final String KAIROS_NULL_UNEXPANDED = "kairos:NULL";

    public static final String OR_GATE = "OR";
    public static final String AND_GATE = "AND";
    public static final String XOR_GATE = "XOR";

    // Contexts and prefixes (expanded and unexpanded)
    public static final String KAIROS_CONTEXT_IRI = "https://kairos-sdf.s3.amazonaws.com/context/kairos/";
    public static final String KAIROS = "kairos:";
    public static final String CACI_TA1_PREFIX = "https://caci.com/kairos/ta1/";
    public static final String CACI_TA2_PREFIX = "https://caci.com/kairos/ta2/";
    public static final String SCHEMA_CONTEXT_IRI = "http://schema.org/";
    public static final String WIKI_EVENT_IRI = "https://www.wikidata.org/entity/";
    public static final String WIKI_EVENT_PREFIX = "wd:";
    public static final String WIKI_RELATION_IRI = "https://www.wikidata.org/prop/direct/";
    public static final String WIKI_RELATION_PREFIX = "wdt:";
    public static final String DWD_PREFIX = "dwd:";

    // KSF-Group defined terms
    public static final String DOCUMENT = "document";
    public static final String SUBMISSIONS = "submissions";
    public static final String CE_ID = "ceID";
    public static final String INSTANCES = "instances";
    public static final String EVENTS = "events";
    public static final String COMMENT = "comment";
    public static final String CONFIDENCE = "confidence";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String RELATIONS = "relations";
    public static final String RELATION_SUBJECT = "relationSubject";
    public static final String RELATION_SUBJECT_PROV = "relationSubject_prov";
    public static final String RELATION_OBJECT = "relationObject";
    public static final String RELATION_OBJECT_PROV = "relationObject_prov";
    public static final String SDF_VERSION = "sdfVersion";
    public static final String VERSION = "version";
    public static final String ACHIEVES = "achieves";
    public static final String REQUIRES = "requires";
    public static final String TEMPORAL = "temporal";
    public static final String MAX_DURATION = "maxDuration";
    public static final String MIN_DURATION = "minDuration";
    public static final String DURATION = "duration";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    public static final String EARLIEST_START_TIME = "earliestStartTime";
    public static final String EARLIEST_END_TIME = "earliestEndTime";
    public static final String LATEST_START_TIME = "latestStartTime";
    public static final String LATEST_END_TIME = "latestEndTime";
    public static final String ABSOLUTE_TIME = "absoluteTime";
    public static final String PRIVATE_DATA = "privateData";
    public static final String PARTICIPANTS = "participants";
    public static final String REPEATABLE = "repeatable";
    public static final String CENTRALITY = "centrality";
    public static final String TA1_REF = "ta1ref";
    public static final String IS_TOP_LEVEL = "isTopLevel";
    public static final String SUBGROUP_EVENTS = "subgroup_events";
    public static final String PARENT = "parent";
    public static final String OUTLINKS = "outlinks";
    public static final String CHILDREN = "children";
    public static final String CHILD = "child";
    public static final String ENTITY = "entity";
    public static final String GOAL = "goal";
    public static final String CHILDREN_GATE = "children_gate";
    public static final String WD_NODE = "wd_node";
    public static final String WD_LABEL = "wd_label";
    public static final String WD_DESCRIPTION = "wd_description";
    public static final String TA2WD_NODE = "ta2qnode";
    public static final String TA2WD_LABEL = "ta2qlabel";
    public static final String TA2WD_DESCRIPTION = "ta2wd_description";
    public static final String TA2ENTITY = "ta2entity";
    public static final String OPTIONAL = "optional";
    public static final String IMPORTANCE = "importance";
    public static final String TA1_EXPLANATION = "ta1explanation";
    public static final String REFERENCE = "reference";
    public static final String PROVENANCE_DATA = "provenanceData";
    public static final String PROVENANCE = "provenance";
    public static final String PROVENANCE_ID = "provenanceID";
    public static final String INSTANCE_OF = "instanceOf";
    public static final String PREDICTION_PROVENANCE = "predictionProvenance";
    public static final String RELATION_PROVENANCE = "relationProvenance";
    public static final String CHILD_ID = "childID";
    public static final String LENGTH = "length";
    public static final String OFFSET = "offset";
    public static final String PARENT_IDs = "parentIDs";
    public static final String BOUNDING_BOX = "boundingBox";
    public static final String KEYFRAMES = "keyframes";
    public static final String AKA = "aka";
    public static final String ROLE_NAME = "roleName";
    public static final String VALUES = "values";
    public static final String ENTITIES = "entities";
    public static final String MEDIA_TYPE = "mediaType";
    public static final String MODALITY = "modality";
    public static final String GENERIC = "generic";
    public static final String HEDGED = "hedged";
    public static final String IRREALIS = "irrealis";
    public static final String NEGATED = "negated";

    // Media type categories
    public static final String TEXTUAL = "Textual";
    public static final String IMAGE = "Image";
    public static final String AUDIO = "Audio";
    public static final String VIDEO = "Video";

    // Temporal relations
    public static final String BEFORE = "Before";
    public static final String BEFORE_QID = "Q79030196";
    public static final String OVERLAPS = "Overlaps";
    public static final String OVERLAPS_QID = "Q65560376";
    public static final String CONTAINS = "Contains";
    public static final String CONTAINS_QID = "P4330";

    public static final List<String> TEMPORAL_SUBSET_IDS = Arrays.asList(WIKI_EVENT_PREFIX + OVERLAPS_QID,
            WIKI_RELATION_PREFIX + CONTAINS_QID);
    public static final List<String> TEMPORAL_SUBSET_LABELS = Arrays.asList(OVERLAPS, CONTAINS);
    public static final HashMap<String, String> TEMPORAL_SUBSET_LABEL_MAP = new HashMap<>();

    static {
        for (int index = 0; index < TEMPORAL_SUBSET_IDS.size(); index++) {
            TEMPORAL_SUBSET_LABEL_MAP.put(TEMPORAL_SUBSET_IDS.get(index), TEMPORAL_SUBSET_LABELS.get(index));
        }
    }

}
