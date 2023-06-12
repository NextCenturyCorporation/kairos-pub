package com.ncc.kairos.moirai.clotho.utilities;

import com.ncc.kairos.moirai.clotho.model.*;
import java.util.*;

import com.ncc.kairos.moirai.clotho.services.DefinitionServiceUtils;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;

/**
 * This class is in an experimental, pre-release state.  There are many TODOs, that are indicated in NOTEs in the code.
 *
 * @author Daniel Nguyen
 * @deprecated because this code was discarded in preference to validating JSON directly and storing JSON in a graph database
 */
@Deprecated(since = "2.3", forRemoval = false)
public class DefinitionConversion extends GraphConversion {

    /**
     * Converts the generated entity to a map of properties to be inserted as a vertex.
     *
     * @param entity the model to convert
     * @return the map of properties in the model.
     */
    public static Vertex convertEntityToVertex(Entity entity) {
        Vertex vertex = new Vertex();
        String id = entity.getAtId() == null ? "" : entity.getAtId();
        vertex.setId(id);
        vertex.label(ENTITY);

        Map<String, String> entityMap = new HashMap<>();
        entityMap.put(SCHEMA_CONTEXT_IRI + NAME, entity.getName());
        entityMap.put(NAME, entity.getName());
        entityMap.put(JSON_LD_ID, entity.getAtId());
        addIfNotEmpty(entityMap, SCHEMA_CONTEXT_IRI + DESCRIPTION, entity.getDescription());

        vertex.setPropertiesMap(entityMap);
        return vertex;
    }

    /**
     * Convert a list of vertices to a list of the generated Entity model.
     *
     * @param vertexList The list of vertices
     * @return The list of entities
     */
    public static List<Entity> convertVerticesToEntityList(List<Vertex> vertexList) {
        List<Entity> entityList = new ArrayList<>();
        for (Vertex vertex : vertexList) {
            entityList.add(convertVertexToEntity(vertex));
        }
        return entityList;
    }

    /**
     * Convert a vertex to an Entity model.
     *
     * @param vertex The @Vertex to convert.
     * @return The information from the @Vertex converted to an @Entity.
     */
    public static Entity convertVertexToEntity(Vertex vertex) {
        Entity entity = new Entity();
        entity.setAtId(vertex.getId());

        for (Map.Entry<String, String> curProperty : vertex.getPropertiesMap().entrySet()) {
            String curKey = curProperty.getKey();
            String curVal = curProperty.getValue();

            switch (curKey) {
                case NAME:
                case SCHEMA_CONTEXT_IRI + NAME:
                    entity.setName(curVal);
                    break;
                case JSON_LD_ID:
                    entity.setAtId(curVal);
                    break;
                case SCHEMA_CONTEXT_IRI + DESCRIPTION:
                case KAIROS_CONTEXT_IRI + DESCRIPTION:
                    entity.setDescription(curVal);
                    break;
                default:
                    break;
            }
        }
        return entity;
    }

    public static Vertex convertEventToVertex(Event event) {
        Vertex vertex = new Vertex();
        String id = event.getAtId() == null ? "" : event.getAtId();
        vertex.setId(id);
        vertex.setLabel(EVENT);

        Map<String, String> propertyMap = new HashMap<>();
        // name
        propertyMap.put(SCHEMA_CONTEXT_IRI + NAME, event.getName());
        // @id
        propertyMap.put(JSON_LD_ID, event.getAtId());
        // version
        addIfNotEmpty(propertyMap, SCHEMA_CONTEXT_IRI + VERSION, event.getVersion());
        // description
        addIfNotEmpty(propertyMap, SCHEMA_CONTEXT_IRI + DESCRIPTION, event.getDescription());
        // comments
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + COMMENT, event.getComment());
        // aka
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + AKA, event.getAka());
        // reference (TA2)
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + REFERENCE, event.getReference());
        // maxDuration
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + MAX_DURATION, event.getMaxDuration());
        // minDuration
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + MIN_DURATION, event.getMinDuration());

        vertex.setPropertiesMap(propertyMap);
        return vertex;
    }

    /**
     * Convert a @Vertex to an @Entity model.
     * NOTE: rethink the model and how requiresEventID, achievesEvent, etc... are modeled and implement below
     *
     * @param vertex The vertex to convert.
     * @return The information from the vertex converted to an entity.
     */
    public static Event convertVertexToEvent(Vertex vertex) {
        Event event = new Event().atId(vertex.getId()).slots(new ArrayList<>());

        for (Map.Entry<String, String> curProperty : vertex.getPropertiesMap().entrySet()) {
            String curKey = curProperty.getKey();
            String curVal = curProperty.getValue();

            switch (curKey) {
                case JSON_LD_ID:
                    event.setAtId(curVal);
                    break;
                case SCHEMA_CONTEXT_IRI + NAME:
                    event.setName(curVal);
                    break;
                case SCHEMA_CONTEXT_IRI + VERSION:
                    event.setVersion(curVal);
                    break;
                case SCHEMA_CONTEXT_IRI + DESCRIPTION:
                    event.setDescription(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + COMMENT:
                    event.setComment(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                case KAIROS_CONTEXT_IRI + AKA:
                    event.setAka(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                case KAIROS_CONTEXT_IRI + REFERENCE:
                    event.setReference((curVal));
                    break;
                case KAIROS_CONTEXT_IRI + MAX_DURATION:
                    event.setMaxDuration(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + MIN_DURATION:
                    event.setMinDuration(curVal);
                    break;
                default:
                    break;
            }
        }
        return event;
    }

    public static Vertex convertSchemaToVertex(Schema schema) {
        Vertex vertex = new Vertex();
        String id = schema.getAtId() == null ? "" : schema.getAtId();
        vertex.setId(id);
        vertex.setLabel(SCHEMA);

        Map<String, String> propertyMap = new HashMap<>();
        String name = schema.getName();
        // name
        propertyMap.put(SCHEMA_CONTEXT_IRI + NAME, name);
        // NOTE: this is here only for easier tracking in neo4j visualizer
        propertyMap.put(NAME, name.substring(name.lastIndexOf('/') + 1));
        // @id
        addIfNotEmpty(propertyMap, JSON_LD_ID, schema.getAtId());
        // description
        addIfNotEmpty(propertyMap, SCHEMA_CONTEXT_IRI + DESCRIPTION, schema.getDescription());
        // version
        addIfNotEmpty(propertyMap, SCHEMA_CONTEXT_IRI + VERSION, schema.getVersion());
        // comment
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + COMMENT, schema.getComment());
        // confidence
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + CONFIDENCE, schema.getConfidence().toString());

        vertex.setPropertiesMap(propertyMap);
        return vertex;
    }

    /**
     * Used when retrieving a schema from the db and converting to the model-version for passage through the REST-API.
     */
    public static Schema convertVertexToSchema(Vertex vertex) {
        Schema schema = new Schema().atId(vertex.getId()).steps(new ArrayList<>());

        // Iterate through properties map of vertex and populate the corresponding property on the Role object
        for (Map.Entry<String, String> curProperty : vertex.getPropertiesMap().entrySet()) {
            String curKey = curProperty.getKey();
            String curVal = curProperty.getValue();

            switch (curKey) {
                case SCHEMA_CONTEXT_IRI + NAME:
                    schema.setName(curVal);
                    break;
                case JSON_LD_ID:
                    schema.setAtId(curVal);
                    break;
                case SCHEMA_CONTEXT_IRI + VERSION:
                    schema.setVersion(curVal);
                    break;
                case SCHEMA_CONTEXT_IRI + DESCRIPTION:
                    schema.setDescription(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + COMMENT:
                    schema.setComment(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                case KAIROS_CONTEXT_IRI + CONFIDENCE:
                    schema.setConfidence(Float.parseFloat(curVal));
                    break;
                default:
                    break;
            }
        }
        return schema;
    }

    public static List<Step> convertVerticesToSteps(List<Vertex> vertices) {
        List<Step> stepsList = new ArrayList<>();
        for (Vertex vertex : vertices) {
            stepsList.add(convertVertexToStep(vertex));
        }
        return stepsList;
    }

    public static Step convertVertexToStep(Vertex vertex) {
        Step stepToReturn = new Step().atId(vertex.getId()).slots(new ArrayList<>());

        // Iterate through properties map of vertex and populate the corresponding property on the Role object
        for (Map.Entry<String, String> curProperty : vertex.getPropertiesMap().entrySet()) {
            String curKey = curProperty.getKey();
            String curVal = curProperty.getValue();

            switch (curKey) {
                case EVENT: // NOTE: may remove, currently used as foreign key.
                    stepToReturn.setEvent(curVal);
                    break;
                case JSON_LD_TYPE:
                    stepToReturn.setAtType(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                case JSON_LD_ID:
                    stepToReturn.setAtId(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + COMMENT:
                    stepToReturn.setComment(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                case KAIROS_CONTEXT_IRI + CONFIDENCE:
                    stepToReturn.setConfidence(Float.parseFloat(curVal));
                    break;
                case KAIROS_CONTEXT_IRI + REFERENCE:
                    stepToReturn.setReference(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + PROVENANCE:
                    stepToReturn.provenance(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + AKA:
                    stepToReturn.setAka(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                case KAIROS_CONTEXT_IRI + MAX_DURATION:
                    stepToReturn.setMaxDuration(curVal); // NOTE: need to retrieve @type
                    break;
                case KAIROS_CONTEXT_IRI + MIN_DURATION:
                    stepToReturn.setMinDuration(curVal); // NOTE: need to retrieve @type
                    break;
                default:
                    break;
            }
        }
        return stepToReturn;
    }

    public static Slot convertVertexToParticipant(Vertex vertex) {
        Slot slotToReturn = new Slot().atId(vertex.getId());

        // Iterate through properties map of vertex and populate the corresponding property on the Role object
        for (Map.Entry<String, String> curProperty : vertex.getPropertiesMap().entrySet()) {
            String curKey = curProperty.getKey();
            String curVal = curProperty.getValue();

            // NOTE: Could add PrivateData
            switch (curKey) {
                case JSON_LD_ID:
                    slotToReturn.setAtId(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + COMMENT:
                    slotToReturn.setComment(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                case KAIROS_CONTEXT_IRI + REFERENCE:
                    slotToReturn.setReference(curVal);
                    break;
                case SCHEMA_CONTEXT_IRI + NAME:
                    slotToReturn.setName(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + AKA:
                    slotToReturn.setAka(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                default:
                    break;
            }
        }
        return slotToReturn;
    }

    public static SlotValue convertVertexToSlotValue(Vertex vertex) {
        SlotValue valueToReturn = new SlotValue().id(vertex.getId());

        // Iterate through properties map of vertex and populate the corresponding property on the Role object
        for (Map.Entry<String, String> curProperty : vertex.getPropertiesMap().entrySet()) {
            String curKey = curProperty.getKey();
            String curVal = curProperty.getValue();

            // NOTE: Could add PrivateData
            switch (curKey) {
                case KAIROS_CONTEXT_IRI + COMMENT:
                    valueToReturn.setComment(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                case KAIROS_CONTEXT_IRI + CONFIDENCE:
                    valueToReturn.setConfidence(Float.parseFloat(curVal));
                    break;
                case KAIROS_CONTEXT_IRI + PROVENANCE:
                    valueToReturn.setProvenance(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                case KAIROS_CONTEXT_IRI + NAME:
                    valueToReturn.setName(curVal);
                    break;
                case SCHEMA_CONTEXT_IRI + MEDIA_TYPE:
                    valueToReturn.setMediaType(curVal);
                    break;
                default:
                    break;
            }
        }
        return valueToReturn;
    }

    public static Temporal convertVertexToTemporal(Vertex vertex) {
        Temporal temporalToReturn = new Temporal().atId(vertex.getId());

        // Iterate through properties map of vertex and populate the corresponding property on the Role object
        for (Map.Entry<String, String> curProperty : vertex.getPropertiesMap().entrySet()) {
            String curKey = curProperty.getKey();
            String curVal = curProperty.getValue();

            // NOTE: duration, start/end-time, absolute-time need to be stored as their own vertices to include @type
            switch (curKey) {
                case JSON_LD_ID:
                    temporalToReturn.setAtId(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + COMMENT:
                    temporalToReturn.setComment(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                case KAIROS_CONTEXT_IRI + CONFIDENCE:
                    temporalToReturn.setConfidence(Collections.singletonList(Float.parseFloat(curVal)));
                    break;
                case KAIROS_CONTEXT_IRI + DURATION:
                    temporalToReturn.setDuration(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + EARLIEST_START_TIME:
                    temporalToReturn.setEarliestStartTime(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + LATEST_START_TIME:
                    temporalToReturn.setLatestStartTime(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + EARLIEST_END_TIME:
                    temporalToReturn.setEarliestEndTime(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + LATEST_END_TIME:
                    temporalToReturn.setLatestEndTime(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + ABSOLUTE_TIME:
                    temporalToReturn.setAbsoluteTime(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + PROVENANCE:
                    temporalToReturn.provenance(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                default:
                    break;
            }
        }
        return temporalToReturn;
    }

    /**
     * Converts the generated @Step model to a @Vertex.
     *
     * @param step the model to convert
     * @return vertex containing the step information.
     */
    public static Vertex convertStepToVertex(Step step) {
        Vertex vertex = new Vertex();
        String id = step.getAtId() == null ? "" : step.getAtId();
        vertex.setId(id);
        vertex.setLabel(STEP);

        Map<String, String> propertyMap = new HashMap<>();
        // @event; used as foreign key to event vertex
        propertyMap.put(EVENT, step.getEvent());
        // @id
        addIfNotEmpty(propertyMap, JSON_LD_ID, step.getAtId());
        // NOTE: name is added only for easier tracking in neo4j visualizer
        addIfNotEmpty(propertyMap, NAME, step.getAtId().substring(step.getAtId().lastIndexOf('/') + 1).replace('_', '-'));
        // @type
        addIfNotEmpty(propertyMap, JSON_LD_TYPE, step.getAtType());
        // reference
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + REFERENCE,
                step.getReference());
        // provenance
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + PROVENANCE,
                step.getProvenance());
        // comment
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + COMMENT,
                step.getComment());
        // aka
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + AKA, step.getAka());

        // confidence
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + CONFIDENCE, step.getConfidence().toString());

        // maxDuration and minDuration
        // NOTE: maxDuration, minDuration should be stored separately on their own vertex(label: json-ld-value)
        // because Need to store @type also
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + MAX_DURATION, step.getMaxDuration());
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + MIN_DURATION, step.getMinDuration());

        vertex.setPropertiesMap(propertyMap);
        return vertex;
    }

    public static Vertex convertParticipantToVertex(Slot slot) {
        Vertex vertex = new Vertex();
        String id = slot.getAtId() == null ? "" : slot.getAtId();
        vertex.setId(id);
        vertex.setLabel(SLOT);

        Map<String, String> propertyMap = new HashMap<>();
        // @id
        propertyMap.put(JSON_LD_ID, slot.getAtId());
        // context-name and name
        propertyMap.put(SCHEMA_CONTEXT_IRI + NAME, slot.getName());
        propertyMap.put(NAME, slot.getName());
        // comment
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + COMMENT, slot.getComment());
        // reference
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + REFERENCE, slot.getReference());
        // aka
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + AKA, slot.getAka());

        vertex.setPropertiesMap(propertyMap);
        return vertex;
    }

    public static Vertex convertSlotToVertex(Slot slot) {
        Vertex slotVertex = new Vertex();

        String id = slot.getAtId() == null ? "" : slot.getAtId();
        slotVertex.setId(id);
        slotVertex.setLabel(SLOT);

        Map<String, String> propertyMap = new HashMap<>();
        // @id
        propertyMap.put(JSON_LD_ID, slot.getAtId());
        //argIndex: Not required for schema-slots.
        // roleName/argLabel
        propertyMap.put(KAIROS_CONTEXT_IRI + ROLE_NAME, slot.getRoleName());
        // comment
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + COMMENT, slot.getComment());
        // aka
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + AKA, slot.getAka());

        slotVertex.setPropertiesMap(propertyMap);
        return slotVertex;
    }

    public static Vertex convertProvenanceToVertex(Provenance provenance) {
        Vertex provenanceVertex = new Vertex();

        provenanceVertex.setLabel(PROVENANCE);

        Map<String, String> propertyMap = new HashMap<>();
        // childID
        propertyMap.put(KAIROS_CONTEXT_IRI + CHILD_ID, provenance.getChildID());
        // mediaType
        propertyMap.put(KAIROS_CONTEXT_IRI +  MEDIA_TYPE, provenance.getMediaType());
        // offset
        String offsetToUse = provenance.getOffset() == null ? "" : provenance.getOffset().toString();
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + OFFSET, offsetToUse);
        // length
        String lengthToUse = provenance.getLength() == null ? "" : provenance.getLength().toString();
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + LENGTH, lengthToUse);
        // parentIds
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + PARENT_IDs, provenance.getParentIDs());
        // startTime
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + START_TIME, provenance.getStartTime().toString());
        // endTime
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + END_TIME, provenance.getEndTime().toString());
        // comments
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + COMMENT, provenance.getComment());

        provenanceVertex.setPropertiesMap(propertyMap);

        return provenanceVertex;
    }

    public static Provenance convertVertexToProvenance(Vertex provenanceVertex) {
        Provenance provenanceToReturn = new Provenance().provenanceID(provenanceVertex.getId());

        for (Map.Entry<String, String> curProperty : provenanceVertex.getPropertiesMap().entrySet()) {
            String curKey = curProperty.getKey();
            String curVal = curProperty.getValue();

            switch (curKey) {
                case KAIROS_CONTEXT_IRI + CHILD_ID:
                    provenanceToReturn.setChildID(curVal);
                    break;
                case KAIROS_CONTEXT_IRI +  MEDIA_TYPE:
                    provenanceToReturn.setMediaType(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + OFFSET:
                    provenanceToReturn.setOffset(Integer.parseInt(curVal));
                    break;
                case KAIROS_CONTEXT_IRI + LENGTH:
                    provenanceToReturn.setLength(Integer.parseInt(curVal));
                    break;
                case KAIROS_CONTEXT_IRI + PARENT_IDs:
                    provenanceToReturn.setParentIDs(Collections.singletonList(curVal));
                    break;
                case KAIROS_CONTEXT_IRI + START_TIME:
                    provenanceToReturn.setStartTime(Float.parseFloat(curVal));
                    break;
                case KAIROS_CONTEXT_IRI + END_TIME:
                    provenanceToReturn.setEndTime(Float.parseFloat(curVal));
                    break;
                case KAIROS_CONTEXT_IRI + COMMENT:
                    provenanceToReturn.setComment(Arrays.asList(convertStringToStringArray(curVal)));
                    break;
                default:
                    break;
            }
        }

        return provenanceToReturn;
    }


    public static Vertex getProvenanceDataVertex() {
        Vertex provenanceDataVertex = new Vertex();

        provenanceDataVertex.setLabel(PROVENANCE_DATA);
        provenanceDataVertex.setPropertiesMap(new HashMap<>());

        return provenanceDataVertex;
    }

    public static Slot convertVertexToSlot(Vertex vertex) {
        Slot slotToReturn = new Slot().atId(vertex.getId());

        // Iterate through properties map of vertex and populate the corresponding property on the Role object
        for (Map.Entry<String, String> curProperty : vertex.getPropertiesMap().entrySet()) {
            String curKey = curProperty.getKey();
            String curVal = curProperty.getValue();
            switch (curKey) {
                case JSON_LD_ID:
                    slotToReturn.setAtId(curVal);
                    break;
                case ROLE_NAME:
                case KAIROS_CONTEXT_IRI + ROLE_NAME:
                    slotToReturn.setRoleName(curVal);
                    break;
                case KAIROS_CONTEXT_IRI + REFERENCE:
                    slotToReturn.setReference(curVal);
                    break;
                default:
                    break;
            }
        }
        return slotToReturn;
    }

    public static Vertex convertSlotValueToVertex(SlotValue slotValue) {
        Vertex vertex = new Vertex();
        String id = slotValue.getId() == null ? "" : slotValue.getId();
        vertex.setId(id);
        vertex.setLabel(SLOT_VALUES);

        Map<String, String> propertyMap = new HashMap<>();

        // comment
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + COMMENT,
                slotValue.getComment());
        // confidence
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + CONFIDENCE,
                slotValue.getConfidence().toString());
        // provenance
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + PROVENANCE,
                slotValue.getProvenance());
        // name
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + NAME,
                slotValue.getName());
        // media type
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + MEDIA_TYPE,
                slotValue.getMediaType());

        vertex.setPropertiesMap(propertyMap);
        return vertex;
    }

    /**
     * A utility primarily used for graph-insertion
     * NOTE: confidence, duration, start-time etc should be stored separately on their own vertex(label: json-ld-value)
     *       because Need to store @type also.
     * @param temporal the temporal object to convert
     * @return the specified temporal converted to a Vertex
     */
    public static Vertex convertTemporalToVertex(Temporal temporal) {
        Vertex vertex = new Vertex();
        String id = temporal.getAtId() == null ? "" : temporal.getAtId();
        vertex.setId(id);
        vertex.setLabel(TEMPORAL);

        Map<String, String> propertyMap = new HashMap<>();
        // @id
        addIfNotEmpty(propertyMap, JSON_LD_ID, temporal.getAtId());
        // comment
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + COMMENT,
                temporal.getComment());
        // confidence
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + CONFIDENCE,
                temporal.getConfidence().toString());
        // duration
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + DURATION,
                temporal.getConfidence().toString());
        // earliest-start-time
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + EARLIEST_START_TIME,
                temporal.getEarliestStartTime());
        // latest-start-time
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + LATEST_START_TIME,
                temporal.getLatestStartTime());
        // earliest-end-time
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + EARLIEST_END_TIME,
                temporal.getEarliestEndTime());
        // latest-end-time
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + LATEST_END_TIME,
                temporal.getLatestEndTime());
        // absolute-time
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + ABSOLUTE_TIME,
                temporal.getAbsoluteTime());
        // provenance
        addIfNotEmpty(propertyMap, KAIROS_CONTEXT_IRI + PROVENANCE,
                temporal.getProvenance());

        vertex.setPropertiesMap(propertyMap);
        return vertex;
    }

    public static StepOrder convertPathToStepOrder(List<Path> stepStepPaths) {
        StepOrder stepOrderToReturn;
        if (stepStepPaths != null && stepStepPaths.size() > 0 && stepStepPaths.get(0).getPath() != null && stepStepPaths.get(0).getPath().size() >= 3) {
            stepOrderToReturn = new StepOrder();
            // Should only be 1 path in the path list. If more, will ignore any beyond 1st element.
            // Currently, for before/after, only "PRECEDES" relationship edge exists.
            // The toVertex of this edge will populate the "after" field.
            String stepEdgeLabel = DefinitionServiceUtils.getNextEdgeLabel(stepStepPaths);
            String stepVertexAtId = DefinitionServiceUtils.getNextVertex(stepStepPaths, 1).getPropertiesMap().get(JSON_LD_ID);
            List<String> stepAtId = new ArrayList<>();
            stepAtId.add(stepVertexAtId);
            switch (stepEdgeLabel) {
                case STEP_PRECEDES:
                    stepOrderToReturn.setAfter(stepAtId);
                    break;
                case STEP_CONTAINS:
                    stepOrderToReturn.setContained(stepAtId);
                    break;
                case STEP_OVERLAPS:
                    stepOrderToReturn.setOverlaps(stepAtId);
                    break;
                default:
                    break;
            }

            // Grab properties from edge (2nd element in path)
            Map<String, String> edgeProperties = stepStepPaths.get(0).getPath().get(1).getPropertiesMap();
            for (Map.Entry<String, String> curEdgeProperty: edgeProperties.entrySet()) {
                String curKey = curEdgeProperty.getKey();
                String curVal = curEdgeProperty.getValue();

                switch (curKey) {
                    case COMMENT:
                        stepOrderToReturn.setComment(Arrays.asList(convertStringToStringArray(curVal)));
                        break;
                    case CONFIDENCE:
                        // NOTE: only @value is retrieved, not @type
                        stepOrderToReturn.setConfidence(Float.parseFloat(curVal));
                        break;
                    case PROVENANCE:
                        stepOrderToReturn.setProvenance(curVal);
                        break;
                    case PRIVATE_DATA:
                        // NOTE: how is privateData on stepOrder record stored in graph db?
                        break;
                    default:
                        break;
                }
            }
        } else {
            return null;
        }
        return stepOrderToReturn;
    }

    public static EntityRelation convertPathToEntityRelation(List<Path> slotSlotPaths) {
        EntityRelation entityRelationToReturn;

        // Should only be 1 path in the path list. If more, will ignore any beyond 1st element.
        if (slotSlotPaths != null && slotSlotPaths.size() > 0 && slotSlotPaths.get(0).getPath() != null && slotSlotPaths.get(0).getPath().size() >= 3) {
            entityRelationToReturn = new EntityRelation();

            // Retrieve data stored in subject vertex, relations edge and object vertex
            String subjectVertexId = DefinitionServiceUtils.getNextVertex(slotSlotPaths, 0).getPropertiesMap().get(JSON_LD_ID);
            String objectVertexAtId = DefinitionServiceUtils.getNextVertex(slotSlotPaths, 1).getPropertiesMap().get(JSON_LD_ID);
            List<String> objectAtId = new ArrayList<>();
            objectAtId.add(objectVertexAtId);

            // Populate subject on the entityRelation-level
            entityRelationToReturn.setRelationSubject(subjectVertexId);

            Edge slotEdge = DefinitionServiceUtils.getNextEdge(slotSlotPaths);

            Relation relationToReturn = new Relation();
            relationToReturn.setRelationObject(objectAtId);
            relationToReturn.setWdNode(List.of(slotEdge.getLabel()));

            // Iterate through edge properties to populate the relations object.
            for (Map.Entry<String, String> curRelationProperty: slotEdge.getPropertiesMap().entrySet()) {
                String curKey = curRelationProperty.getKey();
                String curVal = curRelationProperty.getValue();

                switch (curKey) {
                    case COMMENT:
                        relationToReturn.setComment(Arrays.asList(convertStringToStringArray(curVal)));
                        break;
                    case CONFIDENCE:
                        // NOTE: only @value is retrieved, not @type
                        relationToReturn.setConfidence(Collections.singletonList(Float.parseFloat(curVal)));
                        break;
                    default:
                        break;
                }

            }
            entityRelationToReturn.addRelationsItem(relationToReturn);
        } else {
            return null;
        }

        return entityRelationToReturn;
    }

    public static Step convertPathToStep(List<Path> schemaStepPaths) {
        // Create step object from the root vertex
        Step stepToReturn = convertVertexToStep(DefinitionServiceUtils.getNextVertex(schemaStepPaths, 0));

        // Group the paths based on the next-edge-id
        Map<String, List<Path>> groupedPathsMap = DefinitionServiceUtils.groupPathsByNextEdge(schemaStepPaths);

        // NOTE: Currently using ad-hoc implementation to filter out duplicate step-step paths.
        //  will eventually modify gremlin-query to filter these out automatically.
        groupedPathsMap = DefinitionServiceUtils.removeDuplicatePaths(groupedPathsMap, STEP);

        // Iterate through group of paths. Some groups may only have 1 path. i.e step-step edges
        for (Map.Entry<String, List<Path>> curGroupOfPaths: groupedPathsMap.entrySet()) {
            List<Path> curPathList = curGroupOfPaths.getValue();
            String pathType = DefinitionServiceUtils.getNextEdgeLabel(curPathList);
            // Any of these types could be instantiated depending on edge type
            StepOrder curStepOrder;
            Slot curSlot;

            switch (pathType) {
                case STEP_PRECEDES:
                case STEP_CONTAINS:
                case STEP_OVERLAPS:
                    curStepOrder = convertPathToStepOrder(curPathList);
                    stepToReturn.addOrderItem(curStepOrder);
                    break;
                case SLOT_EDGE_LABEL:
                    curSlot = convertPathToParticipant(DefinitionServiceUtils.traversePaths(curPathList, 1));
                    stepToReturn.addSlotsItem(curSlot);
                    break;
                case REFERENCE:
                    // NOTE: figure out what properties on the referenced primitive object need to be returned, if any.
                    //  Currently, the @type is already on the step-vertex itself, which indicates which event primitive is referenced.
                    break;
                default:
                    // If path is size=1 then edgeLabel="" and no additional processing is needed for current path.
                    break;
            }
        }
        return stepToReturn;
    }

    public static Slot convertPathToParticipant(List<Path> participantPaths) {
        // Create participant object from the root vertex
        Slot slotToReturn = convertVertexToParticipant(DefinitionServiceUtils.getNextVertex(participantPaths, 0));

        // Group the paths based on the next-edge-id
        Map<String, List<Path>> groupedPathsMap = DefinitionServiceUtils.groupPathsByNextEdge(participantPaths);

        // NOTE: Currently using ad-hoc implementation to filter out duplicate slot-slot paths.
        //  will eventually modify gremlin-query to filter these out automatically.
        groupedPathsMap = DefinitionServiceUtils.removeDuplicatePaths(groupedPathsMap, SLOT);

        // Iterate through group of paths. Some groups may only have 1 path. i.e step-step edges
        for (Map.Entry<String, List<Path>> curGroupOfPaths: groupedPathsMap.entrySet()) {
            List<Path> curPathList = curGroupOfPaths.getValue();
            String pathType = DefinitionServiceUtils.getNextEdgeLabel(curPathList);
            // Any of these types could be instantiated depending on edge type
            SlotValue curSlotValue;

            if (SLOT_VALUES_EDGE_LABEL.equals(pathType)) {
                curSlotValue = convertPathToSlotValue(DefinitionServiceUtils.traversePaths(curPathList, 1));
                slotToReturn.addValuesItem(curSlotValue);
            }
            // If path is size=1 then edgeLabel="" and no additional processing is needed for current path.
            // NOTE: Assumption is that slots only have two types of nodes coming from them: values and entityRelations.
            // Need to revisit if another type of edge can come from a participant node.
        }
        return slotToReturn;
    }

    // Used for schema and performer-primitive slots
    public static Slot convertPathToSlot(List<Path> slotPaths) {
        // Create the vertex for the slot object
        Slot slotToReturn = convertVertexToSlot(DefinitionServiceUtils.getNextVertex(slotPaths, 0));

        // Group the paths based on the next-edge-id
        Map<String, List<Path>> groupedPathsMap = DefinitionServiceUtils.groupPathsByNextEdge(slotPaths);

        // NOTE: Currently using ad-hoc implementation to filter out duplicate slot-slot paths.
        // Will eventually modify Gremlin query to filter these out automatically.
        groupedPathsMap = DefinitionServiceUtils.removeDuplicatePaths(groupedPathsMap, SLOT);

        // Iterate through group of paths. Some groups may only have 1 path. i.e step-step edges
        for (Map.Entry<String, List<Path>> curGroupOfPaths: groupedPathsMap.entrySet()) {
            List<Path> curPathList = curGroupOfPaths.getValue();
            String pathType = DefinitionServiceUtils.getNextEdgeLabel(curPathList);

            // If path is size=1 then edgeLabel="" and no additional processing is needed for current path.
            // NOTE: Assumption is that performer primitive and schema slots do not have any additional edges.
            // Need to revisit if another type of edge can come from an event or schema slot.
            if (!pathType.isEmpty()) {
                throw new UnsupportedOperationException("Illegal pathType: " + pathType);
            }
        }

        return slotToReturn;
    }

    public static SlotValue convertPathToSlotValue(List<Path> slotValuePaths) {
        return convertVertexToSlotValue(DefinitionServiceUtils.getNextVertex(slotValuePaths, 0));
    }

    public static List<Provenance> convertPathsToProvenanceData(List<Path> provenancePaths) {
        // Create the vertex for the slot object
        List<Provenance> provenanceDataToReturn = new ArrayList<>();

        // Provenance objects are leaves in the path, so do not intersect with one another.
        // Therefore no need to group nor remove duplicate paths.

        // Iterate through each path which should lead to a single provenance object respectively
        // Assume the root of each path is the provenanceData vertex.
        // The 3rd element(index=2) of each path is the provenance object vertex
        for (Path curProvenancePath: provenancePaths) {
            Vertex curProvenanceVert = DefinitionServiceUtils.getNextVertex(curProvenancePath, 1);
            Provenance curProvenance = convertVertexToProvenance(curProvenanceVert);
            provenanceDataToReturn.add(curProvenance);
        }

        return provenanceDataToReturn;
    }

    public static Map<String, String> convertPathToPrivateDataMap(Path privateDataPath) {
        // There should only be 1 path
        Vertex privateDataVertex = (Vertex) privateDataPath.getPath().get(0);
        Map<String, String> privateDataMap = new HashMap<>();
        for (Map.Entry<String, String> curEntry: privateDataVertex.getPropertiesMap().entrySet()) {
            privateDataMap.put(curEntry.getKey(), curEntry.getValue());
        }
        return privateDataMap;
    }
}
