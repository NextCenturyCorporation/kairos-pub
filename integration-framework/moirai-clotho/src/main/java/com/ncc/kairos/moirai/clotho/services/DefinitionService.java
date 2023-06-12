package com.ncc.kairos.moirai.clotho.services;

import com.ncc.kairos.moirai.clotho.exceptions.SchemaDefinitionException;
import com.ncc.kairos.moirai.clotho.interfaces.IDefinitionService;
import com.ncc.kairos.moirai.clotho.model.*;
import com.ncc.kairos.moirai.clotho.utilities.DefinitionConversion;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;

/**
 * Support the insertion of schemas into the database
 * This class is in an experimental, pre-release state.  There are many TODOs, that are indicated in NOTEs in the code.
 *
 *  @author Daniel Nguyen
 */
@Service
public class DefinitionService extends GraphService implements IDefinitionService {

    /** String format for element already exists error messages. */
    public static final String FMT_ALREADY_EXISTS = "%s with %s:%s already exists in the database.";
    /** String format for entity insertion error messages. */
    public static final String FMT_PRE_ENTITY_INSERTION = "%s: %s %s:%s.";
    /** String format for schema insertion error messages. */
    public static final String FMT_PRE_SCHEMA_INSERTION = "%s: %s %s:%s.";

    @Override
    public Entity addEntity(Entity entity) throws SchemaDefinitionException {
        List<String> systemErrors = new ArrayList<>();
        try {
            // 1) Determine if an event with the @id already exists in the database
            if (vertexExistsByProperty(ENTITY, JSON_LD_ID, entity.getAtId())) {
                systemErrors.add(String.format(FMT_ALREADY_EXISTS, ENTITY, JSON_LD_ID, entity.getAtId()));
            }

            // 2) NOTE: verify parent entity exists if one is specified and create edge

            // 3) Insert new Entity vertex
            if (systemErrors.isEmpty()) {
                Vertex entityVert = addVertex(DefinitionConversion.convertEntityToVertex(entity));
                if (GraphServiceUtils.isValidVertex(entityVert)) {
                    entity.setAtId(entityVert.getId());
                } else {
                    entity.setAtId(ERROR_DB_ID);
                    systemErrors.add(entityVert.getError());
                }
            } else {
                systemErrors.add(0, String.format(FMT_PRE_ENTITY_INSERTION,
                        PRE_ENTITY_INSERTION_ERROR, ENTITY, JSON_LD_ID, entity.getAtId()));
            }
        } catch (Exception ex) {
            systemErrors.add(ex.getMessage());
        }
        entity.setSystemError(systemErrors);
        // NOTE: return the stored copy reflecting what is actually in the db
        //  Currently returns the parameter entity object where the id is the vertex id of the inserted schema.
        return entity;
    }

    @Override
    public List<Entity> getEntities(Map<String, String> searchCriteria) throws SchemaDefinitionException {
        List<Vertex> vertexList = getVertices(ENTITY, searchCriteria);
        return DefinitionConversion.convertVerticesToEntityList(vertexList);
    }

    @Override
    // NOTE: Need to implement exception-handling here similar to Entities-endpoint
    public Event addEvent(Event event) throws SchemaDefinitionException {
        List<String> systemErrors = new ArrayList<>();
        try {
            // 1) Determine if an event with the @id already exists in the database
            if (vertexExistsByProperty(EVENT, JSON_LD_ID, event.getAtId())) {
                systemErrors.add(String.format(FMT_ALREADY_EXISTS, EVENT, JSON_LD_ID, event.getAtId()));
            }

            // 2) Process parent Event if any. Assume ONLY 1 parent max.
            if (event.getSupers() != null && event.getSupers().size() > 0) {
                // NOTE: Create an edge between this performer-primitive and its program-level parent
                List<Vertex> parentVertices = getVerticesByProperty(EVENT, JSON_LD_ID, event.getSupers());
                systemErrors.addAll(GraphServiceUtils.getErrorsFromVertices(parentVertices));
            }

            // 3) Insert new Event vertex
            if (systemErrors.isEmpty()) {
                Vertex eventVert = addVertex(DefinitionConversion.convertEventToVertex(event));
                if (GraphServiceUtils.isValidVertex(eventVert)) {
                    event.setAtId(eventVert.getId());

                    // Add the slots
                    List<Slot> insertedSlots = addSlots(event.getSlots(), eventVert);
                    event.setSlots(insertedSlots);

                } else {
                    event.setAtId(ERROR_DB_ID);
                    systemErrors.add(eventVert.getError());
                }
            } else {
                systemErrors.add(0, String.format(FMT_PRE_ENTITY_INSERTION,
                        PRE_EVENT_INSERTION_ERROR, EVENT, JSON_LD_ID, event.getAtId()));
            }
        } catch (Exception ex) {
            systemErrors.add(ex.getMessage());
        }
        event.setSystemError(systemErrors);
        // NOTE: return the stored copy reflecting what is actually in the db
        //  Currently returns the parameter event object where the id is the vertex id of the inserted schema.
        return event;
    }

    /**
     * Retrieve event objects which includes 1 or more roles vertex/vertices.
     */
    @Override
    public List<Event> getEvents(Map<String, String> searchCriteria) throws SchemaDefinitionException {
        // Retrieve the "Event" vertex/vertices  matching the search-criteria
        List<Vertex> eventVerticesList = getVertices(EVENT, searchCriteria);

        //Construct a Model-Event objects from each event-vertexes retrieved
        return constructModelEvents(eventVerticesList);
    }

    protected List<Event> constructModelEvents(List<Vertex> eventVerticesList) throws SchemaDefinitionException {
        List<Event> eventsListToReturn = new ArrayList<>();

        // Iterate through the vertices and retrieve the role(s) associated with each Event
        for (Vertex curEventVertex : eventVerticesList) {
            eventsListToReturn.add(constructModelEvent(curEventVertex));
        }
        return eventsListToReturn;
    }

    protected Event constructModelEvent(Vertex eventVertex) throws SchemaDefinitionException {
        Event eventToReturn = DefinitionConversion.convertVertexToEvent(eventVertex);

        // Retrieve all nodes & edges corresponding to schema-vertex
        List<Path> paths = getOutgoingPaths(eventToReturn.getAtId());
        // Iterate through the list of grouped paths and populate slot objects onto event object
        for (Path curPath : paths) {
            // Retrieve the 2nd-element(index=1), which SHOULD be an edge
            String pathType = DefinitionServiceUtils.getNextEdgeLabel(curPath);
            // Check the label/type of the 2nd element to determine what to populate on schema object
            if (SLOT_EDGE_LABEL.equals(pathType)) {
                Slot slot = DefinitionConversion.convertVertexToSlot(DefinitionServiceUtils.getNextVertex(curPath, 1));
                eventToReturn.addSlotsItem(slot);
            }

            // NOTE: Should it be an exception if there is an entry that has an empty list?
        }
        return eventToReturn;
    }

    /**
     * Retrieve the schema objects which is each comprised of a schema vertex, as well as any roles and steps vertices associated.
     * A single Gremlin call is first made to retrieve all schema-vertices matching the search criteria.
     * Then a single Gremlin call is made for EACH schema-vertex in order to retrieve its data stored on other vertices.
     */
    @Override
    public List<Schema> getSchemas(Map<String, String> searchCriteria) throws SchemaDefinitionException {
        // First retrieve the Schema vertices matching the search Criteria
        List<Vertex> schemaVerticesList = getVertices(SCHEMA, searchCriteria);
        // Construct a Model-Schema object from each schema-vertex retrieved
        return constructModelSchemas(schemaVerticesList);
    }

    /**
     * Construct a Model Schema object from a Schema-Vertex retrieved from the database.
     * NOTE: Should this function be in another type of Util class?
     */
    protected List<Schema> constructModelSchemas(List<Vertex> schemaVertexList) throws SchemaDefinitionException {
        List<Schema> schemasListToReturn = new ArrayList<>();
        // Iterate through each schema-vertex and retrieve the roles and steps data respectively to fully populate
        for (Vertex curSchemaVertex : schemaVertexList) {
            schemasListToReturn.add(constructModelSchema(curSchemaVertex));
        }
        return schemasListToReturn;
    }

    /**
     * NOTEs:
     * - Once a step-limit is implemented, may leverage when calling getOutgoingPaths, so as to not retrieve excessive data.
     * - Should this function be in another type of Util class?
     * - The fillers are retrieved directly from the role-vertex for now.
     * - This can be re-implemented to traverse to each referenced-entity and retrieve fillers dynamically
     * - Or it can be implemented to traverse to referenced event/schema to return more data
     */
    protected Schema constructModelSchema(Vertex schemaVertex) throws SchemaDefinitionException {
        Schema schemaToReturn = DefinitionConversion.convertVertexToSchema(schemaVertex);
        // Retrieve all nodes & edges corresponding to schema-vertex
        List<Path> paths = getOutgoingPaths(schemaToReturn.getAtId());
        // Group the paths by the first vertex adjacent to the schema-root-vertex (3rd-path-element)
        Map<String, List<Path>> groupedPathsMap = DefinitionServiceUtils.groupPathsByNextEdge(paths);

        // Iterate through the list of grouped paths and populate the schema structure accordingly
        for (Map.Entry<String, List<Path>> curGroupOfPaths : groupedPathsMap.entrySet()) {
            List<Path> curPathList = curGroupOfPaths.getValue();
            if (curPathList.size() > 0) {
                // Retrieve the 2nd-element(index=1), which SHOULD be an edge, of the 1st path in the list.
                String pathType = DefinitionServiceUtils.getNextEdgeLabel(curPathList);
                // Check the label/type of the 2nd element to determine what to populate on schema object
                switch (pathType) {
                    case STEP_EDGE_LABEL:
                        Step step = DefinitionConversion.convertPathToStep(DefinitionServiceUtils.traversePaths(curPathList, 1));
                        schemaToReturn.addStepsItem(step);
                        break;
                    case PRIVATE_DATA:
                        Map<String, String> privateDataMap = DefinitionConversion.convertPathToPrivateDataMap(DefinitionServiceUtils.traversePath(curPathList.get(0), 1));
                        schemaToReturn.setPrivateData(privateDataMap);
                        break;
                    case SUBTYPE_OF_EDGE_LABEL:
                        // NOTE: implement retrieval of parent-schema, if any.
                        break;
                    case PROVENANCE_DATA:
                        List<Provenance> provenanceData = DefinitionConversion.convertPathsToProvenanceData(DefinitionServiceUtils.traversePaths(curPathList, 1));
                        schemaToReturn.setProvenanceData(provenanceData);
                        break;
                    case SLOT_EDGE_LABEL:
                        Slot slot = DefinitionConversion.convertPathToSlot(DefinitionServiceUtils.traversePaths(curPathList, 1));
                        schemaToReturn.addSlotsItem(slot);
                        break;
                    default:
                        // NOTE: Do nothing for now; may be exception in future.
                        break;
                }
            }
            // NOTE: Should it be an exception if there is an entry that has an empty list?
        }
        return schemaToReturn;
    }

    /**
     * Adds a schema to the graph db.
     * NOTE: implement Roles as a property on schema for Post-Phase-1;
     * explore if roles can be defaulted to empty list in swagger;
     */
    @Override
    public Schema addSchema(Schema schema) {
        List<String> systemErrors = new ArrayList<>();
        try {
            // 1) Determine if a schema with the given @id already exists in the graph db.
            if (vertexExistsByProperty(SCHEMA, JSON_LD_ID, schema.getAtId())) {
                systemErrors.add(String.format(FMT_ALREADY_EXISTS, SCHEMA, JSON_LD_ID, schema.getAtId()));
            }
            // 2) Retrieve parent/super schema, if indicated; Assume list of @id's of supers are provided
            List<Vertex> parentVertices = getVerticesByProperty(SCHEMA, JSON_LD_ID, schema.getSupers());
            systemErrors.addAll(GraphServiceUtils.getErrorsFromVertices(parentVertices));

            // 3) Insert the new schema vertex and verify the successful result from db. If NO ERRORS.
            if (systemErrors.isEmpty()) {
                Vertex schemaVert = addVertex(DefinitionConversion.convertSchemaToVertex(schema));
                if (GraphServiceUtils.isValidVertex(schemaVert)) {
                    schema.setAtId(schemaVert.getId());

                    // 4) Process parent(s)-schema, if any.
                    List<Edge> parentEdges = addEdges(SUBTYPE_OF_EDGE_LABEL, schemaVert.getId(), schema.getSupers());
                    systemErrors.addAll(GraphServiceUtils.getErrorsFromEdges(parentEdges));

                    // 5.a) Process STEPs; Returns a Map to store each step and its vertex ID (post-insert)
                    // 5.b) Process Entity-Relations in addSteps function after all steps and slots are inserted in db.
                    // This map will be used to process the step-orders (connect steps with each other based on sequence)
                    List<Step> insertedSteps = addSteps(schema.getSteps(), schemaVert, schema.getEntityRelations());
                    schema.setSteps(insertedSteps); // NOTE: Figure out how to retrieve errors resulting from processing Entity-Relations-edges

                    // 6) Process Step-Orders to create edges between steps as necessary.
                    Map<String, String> stepAtIdToVertexIdMap = DefinitionServiceUtils.getStepAtIdToVertexIdMap(insertedSteps);
                    List<String> processStepOrdersErrors = processStepOrders(schema.getStepOrders(), stepAtIdToVertexIdMap);
                    systemErrors.addAll(processStepOrdersErrors);

                    // 7) Process schema-Slots
                    List<Slot> insertedSchemaSlots = addSlots(schema.getSlots(), schemaVert);
                    schema.setSlots(insertedSchemaSlots);

                    // 8) Process ProvenanceData
                    List<Provenance> insertedProvenanceData = addProvenanceData(schema.getProvenanceData(), schemaVert);
                    schema.setProvenanceData(insertedProvenanceData);

                    // 9) Add PrivateData
                    Vertex privateDataVertex = addPrivateData(schema.getPrivateData(), schema.getAtId());
                    if (!GraphServiceUtils.isValidVertex(privateDataVertex)) { // privateData is not required
                        systemErrors.add(privateDataVertex.getError());
                    }
                } else {
                    schema.setAtId(ERROR_DB_ID);
                    systemErrors.add(schemaVert.getError());
                }
            } else {
                systemErrors.add(0, String.format(FMT_PRE_SCHEMA_INSERTION,
                        PRE_SCHEMA_INSERTION_ERROR, SCHEMA, JSON_LD_ID, schema.getAtId()));
            }
        } catch (Exception ex) {
            systemErrors.add(ex.getMessage());
        }
        schema.setSystemError(systemErrors);
        // NOTE: return the stored copy reflecting what is actually in the db
        return schema;
    }

    protected List<Step> addSteps(List<Step> stepsToInsert, Vertex schemaVertex, List<EntityRelation> entityRelations) throws SchemaDefinitionException {
        List<Step> insertedSteps = new ArrayList<>();
        for (Step stepToInsert : stepsToInsert) {
            insertedSteps.add(addStep(schemaVertex, stepToInsert));
        }

        // Retrieve All slots
        List<Slot> slots = DefinitionServiceUtils.getSlotsFromSteps(insertedSteps);

        // Create a map of Slot: @id -> VertexId and process Entity-Relations
        Map<String, String> slotAtIdToVertexIdMap = DefinitionServiceUtils.getParticipantIdToVertexIdMap(slots);

        processEntityRelations(entityRelations, slotAtIdToVertexIdMap);
        // NOTE: figure out how to return these errors to schema level.

        return insertedSteps;
    }

    protected Step addStep(Vertex schemaVertex, Step step) throws SchemaDefinitionException {
        Vertex stepVertex;
        List<String> systemErrors = new ArrayList<>();

        // 1) Ensure the primitive-event-vertex that the step references exists.
        // Use the @type property of the step to identify the primitive event it references
        Vertex existingEventOrSchemaVertex = getVertexByProperty(null, JSON_LD_ID, step.getEvent());
        if (GraphServiceUtils.isValidVertex(existingEventOrSchemaVertex)) {

            // 2) Create the Step VERTEX
            stepVertex = addVertex(DefinitionConversion.convertStepToVertex(step));
            if (GraphServiceUtils.isValidVertex(stepVertex)) {
                step.setAtId(stepVertex.getId());

                // 3) Connect the schema vertex and new step vertex with an EDGE
                Edge stepEdge = addEdge(STEP_EDGE_LABEL, schemaVertex.getId(), stepVertex.getId());
                if (!GraphServiceUtils.isValidEdge(stepEdge)) {
                    systemErrors.add(String.format("%s: failed to create an edge between schema %s:%s and step %s:%s.",
                            STEP_INSERTION_ERROR, JSON_LD_ID, schemaVertex.getPropertiesMap().get(JSON_LD_ID), JSON_LD_ID, step.getAtId()));
                }

                // 4) Connect the new step vertex and the event/schema it references with an edge
                Edge existingEventOrSchemaEdge = addEdge(REFERENCES, stepVertex.getId(), existingEventOrSchemaVertex.getId());
                if (!GraphServiceUtils.isValidEdge(existingEventOrSchemaEdge)) {
                    systemErrors.add(String.format("%s: failed to create an edge between step %s:%s and object %s:%s.",
                            STEP_INSERTION_ERROR, JSON_LD_ID, step.getAtId(), JSON_LD_ID, existingEventOrSchemaVertex.getPropertiesMap().get(JSON_LD_ID)));
                }
                // 5) Create Slot-vertexes and create an edge between each slot and the step
                List<Slot> insertedSlots = addParticipants(step.getParticipants(), stepVertex);
                step.setParticipants(insertedSlots);
                // 6) Create temporal object(s) and create an edge between each with the step.
                List<Temporal> insertedTemporals = addTemporals(step.getTemporals(), stepVertex);
                step.setTemporals(insertedTemporals);

                // 7) NOTE: Add edge to provenance object if necessary
                // 8) Add privateData
                if (step.getPrivateData() != null && step.getPrivateData().size() > 0) {
                    Vertex privateDataVertex = addPrivateData(step.getPrivateData(), step.getAtId());
                    if (!GraphServiceUtils.isValidVertex(privateDataVertex)) { // privateData is not required
                        systemErrors.add(privateDataVertex.getError());
                    }
                }

                // 9) NOTE: Add edge to MaxDuration, MinDuration objects if decide to create vertices for these.

            } else {
                step.setAtId(ERROR_DB_ID);
                systemErrors.add(String.format("%s: %s", STEP_INSERTION_ERROR, stepVertex.getError()));
            }
        } else {
            step.setAtId(ERROR_DB_ID);
            systemErrors.add(String.format("%s: referenced primitive/schema/event: %s does not exist.",
                    STEP_INSERTION_ERROR, step.getEvent()));
        }

        step.setSystemError(systemErrors);
        return step;
    }

    // Utilized by schema-steps
    protected List<Slot> addParticipants(List<Slot> slotsToInsert, Vertex stepVertex) throws SchemaDefinitionException {
        List<Slot> insertedSlots = new ArrayList<>();
        for (Slot slotToInsert : slotsToInsert) {
            insertedSlots.add(addParticipant(slotToInsert, stepVertex));
        }
        return insertedSlots;
    }

    protected Slot addParticipant(Slot slot, Vertex referencingVertex) throws SchemaDefinitionException {
        List<String> systemErrors = new ArrayList<>();

        // 1) NOTE: should there be an edge connection current participant to corresponding slot in ontology?

        // 2) Create the Slot vertex
        Vertex slotVertex = addVertex(DefinitionConversion.convertParticipantToVertex(slot));
        if (GraphServiceUtils.isValidVertex(slotVertex)) {
            slot.setAtId(slotVertex.getId());

            // 3) Connect the new slot vertex with its step-vertex
            Edge slotEdge = addEdge(SLOT_EDGE_LABEL, referencingVertex.getId(), slotVertex.getId());
            if (GraphServiceUtils.isValidEdge(slotEdge)) {

                // 4) Create slot-values-vertexes
                List<SlotValue> slotValues = addParticipantValues(slot.getValues(), slotVertex);
                slot.setValues(slotValues);

                // 5) add private-data, if any
                if (slot.getPrivateData() != null && slot.getPrivateData().size() > 0) {
                    Vertex privateDataVertex = addPrivateData(slot.getPrivateData(), slot.getAtId());
                    if (!GraphServiceUtils.isValidVertex(privateDataVertex)) {
                        systemErrors.add(privateDataVertex.getError());
                    }
                }
            } else {
                systemErrors.add(slotEdge.getError());
            }
        } else {
            slot.setAtId(ERROR_DB_ID);
            systemErrors.add(slotVertex.getError());
        }
        slot.setSystemError(systemErrors);
        return slot;
    }

    // Utilized by event-primitives and schemas
    protected List<Slot> addSlots(List<Slot> slotsToInsert, Vertex eventVertex) {
        List<Slot> insertedSlots = new ArrayList<>();
        for (Slot slotToInsert : slotsToInsert) {
            insertedSlots.add(addSlot(slotToInsert, eventVertex));
        }
        return insertedSlots;
    }

    protected Slot addSlot(Slot slot, Vertex fromVertex) {
        List<String> systemErrors = new ArrayList<>();

        // 1) Create the slot vertex
        Vertex slotVertex = addVertex(DefinitionConversion.convertSlotToVertex(slot));
        if (GraphServiceUtils.isValidVertex(slotVertex)) {
            slot.setAtId(slotVertex.getId());

            // 2) Connect the new slot vertex with its event or schema vertex
            Edge slotEdge = addEdge(SLOT_EDGE_LABEL, fromVertex.getId(), slotVertex.getId());
            if (!GraphServiceUtils.isValidEdge(slotEdge)) {
                systemErrors.add(slotEdge.getError());
            } else {
                // 3) add private-data, if any
                if (slot.getPrivateData() != null && slot.getPrivateData().size() > 0) {
                    Vertex privateDataVertex = addPrivateData(slot.getPrivateData(), slot.getAtId());
                    if (!GraphServiceUtils.isValidVertex(privateDataVertex)) {
                        systemErrors.add(privateDataVertex.getError());
                    }
                }
            }
        } else {
            slot.setAtId(ERROR_DB_ID);
            systemErrors.add(slotVertex.getError());
        }

        slot.setSystemError(systemErrors);
        return slot;
    }

    protected List<SlotValue> addParticipantValues(List<SlotValue> slotValues, Vertex referencingVertex) {
        List<SlotValue> insertedSlotValues = new ArrayList<>();
        // Values array CAN BE NULL
        if (slotValues != null) {
            for (SlotValue slotValueToInsert : slotValues) {
                insertedSlotValues.add(addParticipantValue(slotValueToInsert, referencingVertex));
            }
        }
        return insertedSlotValues;
    }

    protected SlotValue addParticipantValue(SlotValue slotValue, Vertex referencingVertex) {
        List<String> systemErrors = new ArrayList<>();

        // 1) Create value vertex
        Vertex slotValueVertex = addVertex(DefinitionConversion.convertSlotValueToVertex(slotValue));
        if (GraphServiceUtils.isValidVertex(slotValueVertex)) {
            slotValue.setId(slotValueVertex.getId());

            // 2) Create edge between value and slot vertices.
            Edge slotValueEdge = addEdge(SLOT_VALUES_EDGE_LABEL, referencingVertex.getId(), slotValueVertex.getId());
            if (GraphServiceUtils.isValidEdge(slotValueEdge)) {
                // NOTE: Add edge to provenance-object-vertex, if any.

                // add private-data, if any
                if (slotValue.getPrivateData() != null && slotValue.getPrivateData().size() > 0) {
                    Vertex privateDataVertex = addPrivateData(slotValue.getPrivateData(), slotValue.getId());
                    if (!GraphServiceUtils.isValidVertex(privateDataVertex)) {
                        systemErrors.add(privateDataVertex.getError());
                    }
                }
            } else {
                systemErrors.add(slotValueEdge.getError());
            }
        } else {
            slotValue.setId(ERROR_DB_ID);
            systemErrors.add(slotValueVertex.getError());
        }

        slotValue.setSystemError(systemErrors);
        return slotValue;
    }

    protected List<Temporal> addTemporals(List<Temporal> temporalsToInsert, Vertex referencingVertex) throws SchemaDefinitionException {
        List<Temporal> insertedTemporals = new ArrayList<>();
        for (Temporal temporalToInsert : temporalsToInsert) {
            insertedTemporals.add(addTemporal(temporalToInsert, referencingVertex));
        }
        return insertedTemporals;
    }

    protected Temporal addTemporal(Temporal temporal, Vertex referencingVertex) {
        List<String> systemErrors = new ArrayList<>();

        // 1) Create temporal vertex
        Vertex temporalVertex = addVertex(DefinitionConversion.convertTemporalToVertex(temporal));
        if (GraphServiceUtils.isValidVertex(temporalVertex)) {
            temporal.setAtId(temporalVertex.getId());

            // 2) Create edge between temporal-vertex and referencing-vertex
            Edge temporalEdge = addEdge(TEMPORAL_EDGE_LABEL, referencingVertex.getId(), temporalVertex.getId());
            if (GraphServiceUtils.isValidEdge(temporalEdge)) {

                // 3) Add Edge to provenance object
                // String provenance = temporal.getProvenance()
                // NOTE: Verify this is needed Edge; Need to retrieve provenance-vertex id somehow
            } else {
                systemErrors.add(temporalEdge.getError());
            }
        } else {
            temporal.setAtId(ERROR_DB_ID);
            systemErrors.add(temporalVertex.getError());
        }
        return temporal;
    }

    protected List<Provenance> addProvenanceData(List<Provenance> provenanceDataToAdd, Vertex schemaVertex) {
        List<Provenance> provenanceDataToReturn = new ArrayList<>();

        // 1) Create Vertex for provenanceData; This vertex is simply used as the link to each provenance-element
        Vertex provenanceDataVertex = addVertex(DefinitionConversion.getProvenanceDataVertex());

        if (GraphServiceUtils.isValidVertex(provenanceDataVertex)) {
            // 2) Create an edge between the schema vertex and new provenanceData Vertex
            Edge provenanceDataEdge = addEdge(PROVENANCE_DATA, schemaVertex.getId(), provenanceDataVertex.getId());

            if (GraphServiceUtils.isValidEdge(provenanceDataEdge)) {
                // 3) Create a Vertex for each provenance element and link to provenanceData vertex.
                for (Provenance curProvenanceToAdd : provenanceDataToAdd) {
                    provenanceDataToReturn.add(addProvenance(curProvenanceToAdd, provenanceDataVertex));
                }
            }
        }
        return provenanceDataToReturn;
    }

    protected Provenance addProvenance(Provenance provenanceToAdd, Vertex provenanceDataVertex) {
        List<String> systemErrors = new ArrayList<>();

        // 1) Create Vertex for provenance element
        Vertex provenanceVertex = addVertex(DefinitionConversion.convertProvenanceToVertex(provenanceToAdd));
        if (GraphServiceUtils.isValidVertex(provenanceVertex)) {
            provenanceToAdd.setProvenanceID(provenanceVertex.getId());
            // 2) Create Edge between provenance element and provenanceData vertex
            Edge provenanceEdge = addEdge(PROVENANCE, provenanceDataVertex, provenanceVertex);
            if (!GraphServiceUtils.isValidEdge(provenanceEdge)) {
                systemErrors.add(provenanceEdge.getError());
            }
        } else {
            provenanceToAdd.setProvenanceID(ERROR_DB_ID);
            systemErrors.add(provenanceVertex.getError());
        }

        return provenanceToAdd;
    }

    protected Vertex addPrivateData(Map<String, String> privateDataProperties, String schemaVertexId) {
        Vertex privateDataVertex;
        if (privateDataProperties != null && privateDataProperties.size() > 0) {
            privateDataVertex = addVertex(PRIVATE_DATA, privateDataProperties);
            if (GraphServiceUtils.isValidVertex(privateDataVertex)) {
                Edge edge = addEdge(PRIVATE_DATA, schemaVertexId, privateDataVertex.getId());
                if (!GraphServiceUtils.isValidEdge(edge)) {
                    privateDataVertex.setError(edge.getError());
                }
            }
        } else {
            // return empty Vertex if privateData properties map is null/empty
            privateDataVertex = new Vertex();
            privateDataVertex.setId(ERROR_DB_ID);
            privateDataVertex.setError("privateData properties map was null/empty");
        }
        return privateDataVertex;
    }

    protected List<String> processStepOrders(List<StepOrder> stepOrders, Map<String, String> stepAtIdAndVertexIdMap) {
        List<String> errors = new ArrayList<>();
        try {
            for (StepOrder curStepOrder : stepOrders) {
                Map<String, String> propsMap = new HashMap<>();

                propsMap.put(COMMENT, curStepOrder.getComment().toString());
                propsMap.put(CONFIDENCE, curStepOrder.getConfidence().toString()); // NOTE: may have to store @type for confidence
                propsMap.put(PROVENANCE, curStepOrder.getProvenance());

                // Determine which combo of field(s) are provided: before/after, container/contained, or overlaps.
                if (!curStepOrder.getBefore().isEmpty() && !curStepOrder.getAfter().isEmpty()) {
                    errors.addAll(processBeforeAfterStepOrder(curStepOrder, stepAtIdAndVertexIdMap, propsMap));
                } else if (curStepOrder.getContainer().length() > 0 && !curStepOrder.getContained().isEmpty()) {
                    errors.addAll(processContainerContainedStepOrder(curStepOrder, stepAtIdAndVertexIdMap, propsMap));
                } else if (!curStepOrder.getOverlaps().isEmpty()) {
                    errors.addAll(processOverlapsStepOrder(curStepOrder, stepAtIdAndVertexIdMap, propsMap));
                } else {
                    // The step-order object is invalid; must have values for before/after, container/contained or overlaps
                    // NOTE: Should this error be reported with more details?
                    errors.add("A step-order object must have before/after, container/contained, or overlaps field(s) populated.");
                }
            }
        } catch (Exception ex) {
            errors.add(ex.getMessage());
        }
        return errors;
    }

    protected List<String> processBeforeAfterStepOrder(StepOrder stepOrder, Map<String, String> stepAtIdAndVertexIdMap, Map<String, String> edgeProperties) {
        List<String> errors = new ArrayList<>();
        // Create "PRECEDES" edge from each before-vertex to each after-vertex
        for (String curBeforeStepAtId : stepOrder.getBefore()) {
            for (String curAfterStepAtId : stepOrder.getAfter()) {
                String curBeforeStepVertexId = stepAtIdAndVertexIdMap.get(curBeforeStepAtId);
                String curAfterStepVertexId = stepAtIdAndVertexIdMap.get(curAfterStepAtId);
                Edge curEdge = addEdge(STEP_PRECEDES, curBeforeStepVertexId, curAfterStepVertexId, edgeProperties);
                if (!GraphServiceUtils.isValidEdge(curEdge)) {
                    errors.add(curEdge.getError());
                }
            }
        }
        return errors;
    }

    protected List<String> processContainerContainedStepOrder(StepOrder stepOrder, Map<String, String> stepAtIdAndVertexIdMap, Map<String, String> edgeProperties) {
        List<String> errors = new ArrayList<>();
        String containerStepAtId = stepOrder.getContainer();
        String containerStepVertexId = stepAtIdAndVertexIdMap.get(containerStepAtId);
        // Create "CONTAINS" edge from the container-vertex to each contained-vertex
        for (String curContainedStepAtId : stepOrder.getContained()) {
            String curContainedStepVertexId = stepAtIdAndVertexIdMap.get(curContainedStepAtId);
            Edge curEdge = addEdge(STEP_CONTAINS, containerStepVertexId, curContainedStepVertexId, edgeProperties);
            if (!GraphServiceUtils.isValidEdge(curEdge)) {
                errors.add(curEdge.getError());
            }
        }
        return errors;
    }

    protected List<String> processOverlapsStepOrder(StepOrder stepOrder, Map<String, String> stepAtIdAndVertexIdMap, Map<String, String> edgeProperties) {
        List<String> errors = new ArrayList<>();
        // Create "OVERLAPS" edge from each vertex to every other vertex.
        for (String curFromVertexAtId : stepOrder.getOverlaps()) {
            for (String curToVertexAtId : stepOrder.getOverlaps()) {
                if (!curFromVertexAtId.equals(curToVertexAtId)) {
                    String curFromVertexId = stepAtIdAndVertexIdMap.get(curFromVertexAtId);
                    String curToVertexId = stepAtIdAndVertexIdMap.get(curToVertexAtId);

                    Edge curEdge = addEdge(STEP_OVERLAPS, curFromVertexId, curToVertexId, edgeProperties);
                    if (!GraphServiceUtils.isValidEdge(curEdge)) {
                        errors.add(curEdge.getError());
                    }
                }
            }
        }
        return errors;
    }

    protected List<String> processEntityRelations(List<EntityRelation> entityRelations, Map<String, String> slotAtIdToVertexIdMap) {
        List<String> errors = new ArrayList<>();

        for (EntityRelation curEntityRelation : entityRelations) {
            Map<String, String> propsMap = new HashMap<>();

            // These comments will get combined with the relations-level comments
            List<String> entityRelationsComments = curEntityRelation.getComment();
            String curSubject = curEntityRelation.getRelationSubject();

            for (Relation curRelation : curEntityRelation.getRelations()) {
                String curPredicate = curRelation.getWdNode().get(0);
                propsMap.put(CONFIDENCE, curRelation.getConfidence().toString()); // NOTE: may have to store @type for confid.
                curRelation.getComment().addAll(entityRelationsComments);
                propsMap.put(COMMENT, curRelation.getComment().toString());

                for (String curObject : curRelation.getRelationObject()) {
                    Edge curRelationEdge = addEdge(curPredicate, slotAtIdToVertexIdMap.get(curSubject), slotAtIdToVertexIdMap.get(curObject), propsMap);
                    if (!GraphServiceUtils.isValidEdge(curRelationEdge)) {
                        errors.add(curRelationEdge.getError());
                    }
                }
            }
        }
        return errors;
    }


}
