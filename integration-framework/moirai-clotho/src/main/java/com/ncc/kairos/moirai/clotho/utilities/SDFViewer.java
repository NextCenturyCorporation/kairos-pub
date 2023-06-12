package com.ncc.kairos.moirai.clotho.utilities;

import com.ncc.kairos.moirai.clotho.model.*;
import com.ncc.kairos.moirai.clotho.utilities.ksf.validation.JavaDataStructureUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.ValidationUtils.isNonEmptyList;

public class SDFViewer {
    private static final String FILE_HEADER = "SDF Event Viewer";
    private static final String EVENT_HIERARCHY_HEADER = System.lineSeparator() + "Event hierarchy (order does not consider outlinks):" + System.lineSeparator();
    private static final String EVENT_LIST_HEADER_TA1 = System.lineSeparator() + "Event List:" + System.lineSeparator();
    private static final String EVENT_LIST_HEADER_TA2 = System.lineSeparator() + "Event List (topologically sorted):" + System.lineSeparator();
    private static final String TA1_ENTITY_LIST_HEADER = System.lineSeparator() + "TA1 Entities (variables):" + System.lineSeparator();
    private static final String TA2_ENTITY_LIST_HEADER = System.lineSeparator() + "TA2 Entities (extracted/predicted):" + System.lineSeparator();
    private static final String TOP_LEVEL_RELATIONS_HEADER = System.lineSeparator() + "Top-level relations:" + System.lineSeparator();
    private static final String INSTANCE_SEPARATOR = "==============" + System.lineSeparator();

    private static final String QUOTED_STRING_WITH_ID_FORMAT = "\"%s\" [%s]";
    private final boolean verbose;
    private final boolean useJGraphT; // use JGraphT for topological sort
    private final boolean useAllTemporalRelations; // use all temporal relations for topological sort, not just outlinks; ignored if useJGraphT is true

    private String instanceName;
    private boolean isTA2 = false;
    private boolean isTask2 = false;
    private String instanceCeID;
    private List<Instance> instances;
    private List<SchemaEvent> rootEvents;
    private List<SchemaEvent> events;
    private List<SchemaEntity> entities;
    private List<Relation> relations;
    private List<Provenance> provenanceData;

    private HashMap<String, String> numericIdCache; // cache numeric IDs as we determine them
    private HashMap<String, SchemaEntity> entityIdCache;
    private HashMap<String, SchemaEvent> eventIdCache;
    private HashMap<String, List<SchemaEvent>> parentMap; // parent(s) of the given event
    private HashMap<SchemaEntity, List<SchemaEvent>> entityReferenceMap; // maps entities to list of events that refer to them
    private HashMap<SchemaEntity, List<SchemaEvent>> entityPredictionMap; // maps entities to list of events that predict them

    public SDFViewer(JsonLdRepresentation jRep, boolean verboseFlag) {
        verbose = verboseFlag;
        useJGraphT = true;
        useAllTemporalRelations = false;
        initViewer(jRep);
    }

    public SDFViewer(JsonLdRepresentation jRep, boolean verboseFlag, boolean useJGraphT, boolean useAllTemporalRelations) {
        verbose = verboseFlag;
        this.useJGraphT = useJGraphT;
        this.useAllTemporalRelations = useAllTemporalRelations;
        initViewer(jRep);
    }

    private void initViewer(JsonLdRepresentation jRep) {
        if (jRep == null) {
            instances = null;
        } else if (jRep.getInstances() != null) {
            isTA2 = true;
            instances = jRep.getInstances(); // TA2
            instanceCeID = jRep.getCeID();
            isTask2 = jRep.getTask2() != null && jRep.getTask2();
            provenanceData = jRep.getProvenanceData();
        } else { // TA2 error condition, or TA1
            Instance instance = new Instance();
            instance.events(jRep.getEvents());
            instance.entities(jRep.getEntities());
            instance.relations(jRep.getRelations());
            instance.name(jRep.getAtId() == null ? "[Unnamed TA1 output]" : jRep.getAtId());
            instances = new ArrayList<>();
            instances.add(instance);
        }
    }

    private void loadInstance(Instance instance) {
        events = instance.getEvents() != null ? instance.getEvents() : new ArrayList<>();
        entities = instance.getEntities() != null ? instance.getEntities() : new ArrayList<>();
        relations = instance.getRelations() != null ? instance.getRelations() : new ArrayList<>();
        numericIdCache = new HashMap<>();
        entityIdCache = new HashMap<>();
        eventIdCache = new HashMap<>();
        rootEvents = getRootEvents(events, isTA2);
        instanceName = instance.getName();
        parentMap = new HashMap<>();
        entityReferenceMap = new HashMap<>();
        entityPredictionMap = new HashMap<>();
    }

    /**
     * Return human-readable, formatted output of the underlying SDF JSON-LD.
     *
     * @return the String output
     */
    public String getOutput() {
        StringBuilder outputStr = new StringBuilder(FILE_HEADER);
        outputStr.append(verbose ? " (verbose output)" : " (non-verbose output)").append(System.lineSeparator());
        if (!useJGraphT) {
            outputStr.append("--> NOTE: using custom event topological sort instead of third-party implementation.").append(System.lineSeparator());
            outputStr.append("    This custom sort used ");
            outputStr.append(useAllTemporalRelations ? "outlinks and explicit temporal relations." : "outlinks only.").append(System.lineSeparator());
        }

        if (instances != null) {
            int instanceCount = 0;
            for (Instance instance : instances) {
                loadInstance(instance);
                instanceCount++;
                if (instances.size() > 1) {
                    outputStr.append(String.format("Instance #%d: ", instanceCount));
                }
                outputStr.append(getInstanceOutput());
                if (instanceCount < instances.size()) {
                    outputStr.append(String.format("%n%n%s%n", INSTANCE_SEPARATOR));
                }
            }
        } else {
            outputStr.append(String.format("No valid input.%n"));
        }
        return outputStr.toString();
    }

    private String getInstanceOutput() {
        StringBuilder outputStr = new StringBuilder();
        outputStr.append(String.format("%s%n", instanceName));
        if (isTA2) {
            outputStr.append(String.format("ceID: %s%n", instanceCeID));
            outputStr.append(String.format("Confidence: %s%n", safeGetString(instances.get(0).getConfidence())));
        }
        outputStr.append(EVENT_HIERARCHY_HEADER);
        outputStr.append(displayEntireHierarchy());
        outputStr.append(isTA2 ? EVENT_LIST_HEADER_TA2 : EVENT_LIST_HEADER_TA1);

        // Maps eventID to a list of outlinks
        HashMap<String, List<String>> outlinksMap = new HashMap<>();
        HashMap<String, List<String>> hierarchicalOutlinksMap = new HashMap<>(); // outlinks derived from hierarchy

        List<SchemaEvent> sortedEvents = events;
        if (isTA2) {
            HashMap<String, SchemaEvent> validIDMap = new HashMap<>();
            initOutlinksMaps(events, outlinksMap, hierarchicalOutlinksMap, validIDMap);
            if (useJGraphT) {
                sortedEvents = GraphTraversal.getSortedEvents(events, validIDMap);
            } else {
                // Convent outlinks and/or temporal relations to a list of StepOrders for ease of code reuse.
                List<StepOrder> stepOrders;
                if (useAllTemporalRelations) {
                    // Maps eventID to a map of temporal relations to event names
                    HashMap<String, HashMap<String, List<String>>> temporalMap = new HashMap<>();
                    initTemporalMap(events, relations, outlinksMap, hierarchicalOutlinksMap, temporalMap);
                    stepOrders = convertTemporalMapToStepOrders(temporalMap);
                } else {
                    stepOrders = convertOutlinksMapToStepOrders(outlinksMap);
                    stepOrders.addAll(convertOutlinksMapToStepOrders(hierarchicalOutlinksMap));
                }
                sortedEvents = sortEvents(events, stepOrders);
            }
        } // TA2

        int orderNum = 0;
        for (SchemaEvent sortedEvent : sortedEvents) {
            sortedEvent.setOrderNumber(String.valueOf(orderNum++));
            outputStr.append(displayEventDetails(sortedEvent, outlinksMap));
        }
        outputStr.append(displayTA1entities());
        if (isTA2) {
            outputStr.append(displayTA2entities());
        }
        outputStr.append(displayTopLevelRelations());
        return outputStr.toString();
    }

    private void initOutlinksMaps(List<SchemaEvent> events, HashMap<String, List<String>> outlinksMap,
            HashMap<String, List<String>> hierarchicalOutlinksMap, HashMap<String, SchemaEvent> validIdMap) {
        for (SchemaEvent event : events) {
            String eventId = event.getAtId();
            if (isNullOrEmptyString(eventId)) {
                continue;
            }
            validIdMap.put(eventId, event);

            // Map event to its outlinks.
            List<String> outlinks = event.getOutlinks();
            addOutlinks(outlinksMap, outlinks, eventId);

            // Map event to its children, as parents can be considered "before" their children temporally.
            List<String> children = event.getSubgroupEvents();
            addOutlinks(hierarchicalOutlinksMap, children, eventId);
        } // for event
    }

    private void addOutlinks(HashMap<String, List<String>> outlinksMap, List<String> outlinks, String eventId) {
        if (isNonEmptyList(outlinks)) {
            for (String outlink : outlinks) {
                SchemaEvent afterEvent = findEventById(outlink);
                if (afterEvent != null) { // make sure it's a real event
                    List<String> outlinkIDs = outlinksMap.get(eventId);
                    if (isNullOrEmptyList(outlinkIDs)) {
                        outlinkIDs = new ArrayList<>();
                    }
                    outlinkIDs.add(getNumericId(outlink));
                    outlinksMap.put(eventId, outlinkIDs);
                }
            } // for outlink
        }
    }

    // Populate the temporal map
    private void initTemporalMap(List<SchemaEvent> events, List<Relation> relations, HashMap<String, List<String>> outlinksMap,
            HashMap<String, List<String>> hierarchicalOutlinksMap, HashMap<String, HashMap<String, List<String>>> temporalMap) {
        // Process temporal relations
        for (Relation relation: relations) {
            String relationPredicate = safeGetString(relation.getWdNode());
            if (TEMPORAL_SUBSET_IDS.contains(relationPredicate)) {
                addTemporalRelation(temporalMap, relation);
            }
        }
        for (SchemaEvent event: events) {
            List<Relation> eventRelations = event.getRelations();
            if (eventRelations != null) {
                for (Relation relation: eventRelations) {
                    String relationPredicate = safeGetString(relation.getWdNode());
                    if (TEMPORAL_SUBSET_IDS.contains(relationPredicate)) {
                        addTemporalRelation(temporalMap, relation);
                    }
                }
            }
        }

        // Create before relations in temporalMap from outlinks and event hierarchy
        addOutlinksToTemporalMap(temporalMap, outlinksMap);
        addOutlinksToTemporalMap(temporalMap, hierarchicalOutlinksMap);
    }

    private void addOutlinksToTemporalMap(HashMap<String, HashMap<String, List<String>>> temporalMap,
            HashMap<String, List<String>> outlinksMap) {

        Set<String> outlinkIds = outlinksMap.keySet();
        for (String beforeEventId : outlinkIds) {
            SchemaEvent beforeEvent = findEventById(beforeEventId);
            if (beforeEvent == null) { // bad events should have been culled out by initOutlinksMap
                throw new IllegalStateException("'beforeEvent' is null.");
            }
            HashMap<String, List<String>> eventMap = temporalMap.get(beforeEventId);
            if (eventMap == null) {
                eventMap = new HashMap<>();
            }
            final String relationPredicate = WIKI_EVENT_PREFIX + BEFORE_QID;
            List<String> eventIds = eventMap.get(relationPredicate);
            if (eventIds == null) {
                eventIds = new ArrayList<>();
            }
            List<String> outlinks = outlinksMap.get(beforeEventId);
            for (String afterEventId : outlinks) {
                if (!eventIds.contains(afterEventId)) {
                    eventIds.add(getNumericId(afterEventId)); //
                    eventMap.put(relationPredicate, eventIds);
                    temporalMap.put(beforeEvent.getAtId(), eventMap);
                }
            }
        }
    }

    private void addTemporalRelation(HashMap<String, HashMap<String, List<String>>> temporalMap, Relation relation) {
        SchemaEvent beforeEvent = findEventById(relation.getRelationSubject());
        if (beforeEvent == null || StringUtils.isEmpty(beforeEvent.getAtId()) ||
                isNullOrEmptyList(relation.getRelationObject())) {
            return; // ignore bad data
        }

        String afterEventId = relation.getRelationObject().get(0);
        HashMap<String, List<String>> eventMap = temporalMap.get(beforeEvent.getAtId());
        if (eventMap == null) {
            eventMap = new HashMap<>();
        }
        String relationPredicate = safeGetString(relation.getWdNode());
        List<String> eventIds = eventMap.get(relationPredicate);
        if (eventIds == null) {
            eventIds = new ArrayList<>();
        }
        if (!eventIds.contains(afterEventId)) {
            eventIds.add(getNumericId(afterEventId)); //
            eventMap.put(relationPredicate, eventIds);
            temporalMap.put(beforeEvent.getAtId(), eventMap);
        }
    }

    // See KAIR-1411: sort event list based on TA2 temporal relations
    private List<SchemaEvent> sortEvents(List<SchemaEvent> events, List<StepOrder> orders) {

        if (isNullOrEmptyList(events)) {
            return events;
        }

        // Set initial (default) event order based on the order they appeared in the output.
        for (int eventNum = 0; eventNum < events.size(); eventNum++) {
            events.get(eventNum).setOrderNumber(Integer.toString(eventNum));
        }

        // Generate additional order relations based on extracted temporal values.
        List<StepOrder> temporalOrders = generateOrdersFromTemporal(events, orders);
        orders.addAll(temporalOrders);

        // Use order relations (StepOrders) to reassign order number to events.
        return assignOrderNumberToEventsTemporally(events, orders);
    }

    private StringBuilder displayEntireHierarchy() {
        StringBuilder outputStr = new StringBuilder();
        List<String> addedChildren = new ArrayList<>();
        for (SchemaEvent rootEvent : rootEvents) {
            outputStr.append(isTA2 ?
                    displayEventInTA2Hierarchy(rootEvent, addedChildren) : displayEventInTA1Hierarchy(rootEvent, addedChildren));
            addedChildren.clear();
        }
        return outputStr;
    }

    private StringBuilder displayEventInTA1Hierarchy(SchemaEvent event, List<String> expandedEvents) {
        return displayEventInTA1Hierarchy(event, expandedEvents, 0, null, null, null);
    }

    private StringBuilder displayEventInTA1Hierarchy(SchemaEvent event, List<String> expandedEvents,
                                                  int tabLevel, Float importance, Boolean optional, Boolean repeatable) {
        StringBuilder outputStr = displayBasicEventDetails(event, tabLevel);

        // importance, optional, repeatable
        if (importance != null) {
            outputStr.append(", importance ").append(importance);
        }
        if (optional == Boolean.TRUE) {
            outputStr.append(", optional");
        }
        if (repeatable == Boolean.TRUE) {
            outputStr.append(", repeatable");
        }
        outputStr.append(System.lineSeparator());

        // Process container nodes
        String eventId = event.getAtId();
        if (isNonEmptyList(event.getChildren()) && !expandedEvents.contains(eventId)) {
            expandedEvents.add(eventId); // Only recurse down a given child once
            outputStr.append(String.format("%s%s%n", "\t".repeat(++tabLevel), safeGetString(event.getChildrenGate()).toUpperCase()));
            List<Child> orderedChildren = event.getChildren();
            for (Child child : orderedChildren) {
                // Save parent reference
                addToReferenceMap(parentMap, child.getChild(), event);
                SchemaEvent childEvent = findEventById(child.getChild());
                if (childEvent != null) {
                    outputStr.append(displayEventInTA1Hierarchy(childEvent, expandedEvents, tabLevel, child.getImportance(),
                            child.getOptional(), child.getRepeatable()));
                }
            }
        }

        return outputStr;
    }

    private StringBuilder displayBasicEventDetails(SchemaEvent event, int tabLevel) {
        StringBuilder outputStr = new StringBuilder();

        // <name> [<5-digit ID>], <instantiated/predicted flag>, importance/optional/repeatable -> <non-child outlinks>
        outputStr.append("\t".repeat(tabLevel)); // indent
        outputStr.append(event.getName());
        outputStr.append(String.format(" [%s]", getNumericId(event.getAtId())));
        String flag = "";
        if (event.getPredictionProvenance() != null) {
            flag = ", predicted";
        } else if (isNonEmptyList(event.getProvenance())) {
            flag = ", instantiated";
        }
        outputStr.append(flag);

        return outputStr;
    }

    private StringBuilder displayEventInTA2Hierarchy(SchemaEvent event, List<String> expandedEvents) {
        return displayEventInTA2Hierarchy(event, expandedEvents, -1);
    }

    private StringBuilder displayEventInTA2Hierarchy(SchemaEvent event, List<String> expandedEvents, int tabLevel) {
        StringBuilder outputStr = new StringBuilder();

        if (event.getIsTopLevel() == null || !event.getIsTopLevel()) {
            outputStr = displayBasicEventDetails(event, tabLevel);

            if (event.getRepeatable() != null && event.getRepeatable() == Boolean.TRUE) {
                outputStr.append(", repeatable");
            }
            outputStr.append(System.lineSeparator());
        }

        // Process hierarchy nodes
        List<String> subgroupEventIds = event.getSubgroupEvents();
        if (isNonEmptyList(subgroupEventIds)) {
            expandedEvents.add(event.getAtId()); // Only recurse down a given child once
            outputStr.append(String.format("%s%s%n", "\t".repeat(++tabLevel), safeGetString(event.getChildrenGate()).toUpperCase()));
            // See KAIR-1412: Recursively sort event hierarchy based on TA2 outlinks via getTA2PartialOrder(subgroupEventIds)
            for (String subeventId : subgroupEventIds) {
                if (!subeventId.equals(event.getAtId())) {
                    SchemaEvent childEvent = findEventById(subeventId);
                    addToReferenceMap(parentMap, subeventId, event);
                    if (childEvent != null) {
                        outputStr.append(displayEventInTA2Hierarchy(childEvent, expandedEvents, tabLevel));
                    }
                }
            }
        }

        return outputStr;
    }

    private StringBuilder displayTA1entities() {
        StringBuilder outputStr = new StringBuilder(TA1_ENTITY_LIST_HEADER);

        boolean foundEntity = false;
        for (SchemaEntity entity : entities) {
            if (isNullOrEmptyList(entity.getWdNode())) {
                continue; // TA2 entity
            }
            List<SchemaEvent> eventList = entityReferenceMap.get(entity);

            // Only show referred to entities in brief output
            if (verbose || eventList != null) {
                outputStr.append('[').append(getNumericId(entity.getAtId())).append("] \"");
                outputStr.append(entity.getName());
                outputStr.append("\", qnode ").append(entity.getWdNode());
                outputStr.append(", qlabel: ").append(entity.getWdLabel());
                outputStr.append(", qdescription: ").append(entity.getWdDescription());
                outputStr.append(System.lineSeparator());
                // Add "referred to by"
                if (eventList != null) {
                    outputStr.append("\treferred to by ")
                            .append(displayEventList(eventList))
                            .append(System.lineSeparator());
                }
            }
            foundEntity = true;
        }
        if (!foundEntity) {
            outputStr.append("None").append(System.lineSeparator());
        }
        return outputStr;
    }

    private StringBuilder displayTA2entities() {
        StringBuilder outputStr = new StringBuilder(TA2_ENTITY_LIST_HEADER);

        boolean foundEntity = false;
        for (SchemaEntity entity : entities) {
            if (isNullOrEmptyList(entity.getTa2wdNode())) {
                continue; // TA1 entity
            }
            List<SchemaEvent> extractedList = entityReferenceMap.get(entity);
            List<SchemaEvent> predictedList = entityPredictionMap.get(entity);

            // Only show extracted/predicted entities in brief output
            if (verbose || extractedList != null || predictedList != null) {
            outputStr.append('[').append(getNumericId(entity.getAtId())).append("] \"");
                outputStr.append(entity.getName()).append("\", qnode ");
                outputStr.append(entity.getTa2wdNode());
                outputStr.append(", qlabel: ").append(entity.getTa2wdLabel());
                outputStr.append(", qdescription: ").append(entity.getTa2wdDescription());
                outputStr.append(System.lineSeparator());
                // Add "extracted in"
                if (extractedList != null) {
                    outputStr.append("\textracted in ")
                            .append(displayEventList(extractedList))
                            .append(System.lineSeparator());
                }
                // Add "predicted in"
                if (predictedList != null) {
                    outputStr.append("\tpredicted in ")
                            .append(displayEventList(predictedList))
                            .append(System.lineSeparator());
                }
                foundEntity = true;
            }
        }
        if (!foundEntity) {
            outputStr.append("None").append(System.lineSeparator());
        }
        return outputStr;
    }


    private StringBuilder displayEventList(List<SchemaEvent> eventList) {
        StringBuilder entityRefOutput = new StringBuilder();
        if (isNullOrEmptyList(eventList)) {
            return entityRefOutput;
        }

        for (int eventCtr = 0; eventCtr < eventList.size(); eventCtr++) {
            SchemaEvent event = eventList.get(eventCtr);
            if (eventCtr > 0) {
                entityRefOutput.append(", ");
            }
            entityRefOutput.append(String.format(QUOTED_STRING_WITH_ID_FORMAT, event.getName(), getNumericId(event.getAtId())));
        }
        return entityRefOutput;
    }

    private StringBuilder displayTopLevelRelations() {
        StringBuilder outputStr = new StringBuilder(TOP_LEVEL_RELATIONS_HEADER);

        int numRelations = 0;
        for (Relation relation : relations) {
            if (!verbose && isTA2) {
                String relationPredicate = safeGetString(relation.getWdNode());
                if (TEMPORAL_SUBSET_IDS.contains(relationPredicate) || // skip TA2 order relations in non-verbose TA2 output
                        isNullOrEmptyList(relation.getRelationProvenance())) {  // skip un-instantiated relations in non-verbose TA2 output
                    continue;
                }
            }
            numRelations++;
            outputStr.append('[').append(getNumericId(relation.getAtId())).append("] \"");
            outputStr.append(relation.getName() != null ? relation.getName() : "<Unnamed>").append("\"");
            if (isTA2 && relation.getTa1ref() != null) {
                outputStr.append(relation.getTa1ref().equals(NONE) ? " (from docs)" : " (from schema)");
            }
            outputStr.append(System.lineSeparator()).append("\t");
            outputStr.append(displayBasicRelationInfo(relation));
        }
        if (numRelations == 0) {
            outputStr.append("None").append(System.lineSeparator());
        }
        return outputStr;
    }


    private String getArgName(String argId) {
        String name = "";
        SchemaEntity entityArg = findEntityById(argId);
        if (entityArg == null) {
            SchemaEvent eventArg = findEventById(argId);
            if (eventArg != null) {
                name = eventArg.getName();
            }
        } else {
            name = entityArg.getName();
        }
        return name;
    }


    private StringBuilder displayEventDetails(SchemaEvent event, HashMap<String, List<String>> outlinksMap) {
        StringBuilder eventStr = new StringBuilder();
        if (event == null || event.getIsTopLevel() != null && event.getIsTopLevel()) {
            return eventStr;
        }

        // Display event order number
        if (isTA2) {
            eventStr.append(event.getOrderNumber()).append(". ");
        }
        // Display event ID and name
        String eventId = event.getAtId();
        eventStr.append('[').append(getNumericId(eventId)).append("] ");
        eventStr.append(String.format("\"%s\"", event.getName()));

        // Display event source and instantiation status
        eventStr.append(displaySourceInfo(event));

        // Qnodes
        eventStr.append("\tQnodes: ");
        if (isNonEmptyList(event.getWdNode())) {
            eventStr.append(String.format("%s, Qlabels: %s, Qdescriptions: %s%n", event.getWdNode(), event.getWdLabel(), event.getWdDescription()));
        } else if (isNonEmptyList(event.getTa2wdNode())) {
            eventStr.append(String.format("%s, Qlabels: %s, Qdescriptions: %s%n", event.getTa2wdNode(), event.getTa2wdLabel(), event.getTa2wdDescription()));
        } else {
            eventStr.append("[None]").append(System.lineSeparator());
        }

        // Parent event(s)
        if (isTA2) {
            if (event.getParent() != null) {
                SchemaEvent parent = findEventById(event.getParent());
                eventStr.append(String.format("\tParent: %s%n", parent != null ?
                        displayEventList(List.of(parent)) : "[" + event.getParent() + "]"));
            }
        } else {
            List<SchemaEvent> parents = parentMap.get(eventId);
            if (parents != null) {
                eventStr.append(String.format("\tParents: %s%n", displayEventList(parents)));
            }
        }

        // Children
        if (isNonEmptyList(event.getChildren())) {
            eventStr.append(displayChildren(event.getChildren(), event.getChildrenGate()));
        }
        if (isNonEmptyList(event.getSubgroupEvents())) {
            eventStr.append(displaySubgroupEvents(event.getSubgroupEvents(), event.getChildrenGate()));
        }

        // ta1Explanation
        if (!isTA2 && event.getTa1explanation() != null) {
            eventStr.append("\tTA1 explanation: ").append(event.getTa1explanation()).append(System.lineSeparator());
        }

        // Roles/argument fillers
        if (isNonEmptyList(event.getParticipants())) {
            eventStr.append("\tRoles:").append(System.lineSeparator());
            for (Participant participant: event.getParticipants()) {
                eventStr.append("\t\t");
                eventStr.append(displayArgument(participant, event));
            }
        }

        // sourceURL
        if (isTA2) {
            eventStr.append(displayEventSourceURL(event.getProvenance()));
        }

        // Relations
        if (isNonEmptyList(event.getRelations())) {
            eventStr.append("\tRelations:").append(System.lineSeparator());
            for (Relation relation: event.getRelations()) {
                String relationPredicate = safeGetString(relation.getWdNode());
                if (verbose || !isTA2 || !TEMPORAL_SUBSET_IDS.contains(relationPredicate)) {
                    eventStr.append("\t\t");
                    eventStr.append(displayBasicRelationInfo(relation));
                }
            }
        }

        // Temporals
        if (isNonEmptyList(event.getTemporal())) {
            eventStr.append(displayTemporals(event));
        }

        // TA2 Order
        List<String> outlinks = outlinksMap.get(eventId);
        if (isNonEmptyList(outlinks)) {
            eventStr.append("\tOrder:").append(System.lineSeparator());
            for (String outlink : outlinks) {
                eventStr.append(String.format("\t\t%s: %s%n", BEFORE, outlink));
            }
        }

        // Prediction
        eventStr.append(displayPrediction(event));

        // Modality
        if (isNonEmptyList(event.getModality())) {
            eventStr.append("\tModality: ").append(event.getModality()).append(System.lineSeparator());
        }
        return eventStr;
    }

    // Return first available sourceURL in the specified event's provenance, otherwise the empty string.
    private String displayEventSourceURL(List<String> provenanceList) {
        if (!isNonEmptyList(provenanceList) || !isNonEmptyList(provenanceData)) {
            return "";
        }
        for (String provenance : provenanceList) {
            for (Provenance provData : provenanceData) {
                if (!isNullOrEmptyString(provenance) && !isNullOrEmptyString(provData.getSourceURL()) &&
                        provenance.equals(provData.getProvenanceID())) {
                    return "\tURL (first): " + provData.getSourceURL() + System.lineSeparator();
                }
            }
        }
        return "";
    }

    private StringBuilder displaySourceInfo(SchemaEvent event) {
        StringBuilder sourceStr = new StringBuilder();
        if (isTA2) {
            if (event.getPredictionProvenance() != null) {
                sourceStr.append(" (predicted ");
            } else if (isNonEmptyList(event.getProvenance())) {
                sourceStr.append(" (instantiated ");
            } else {
                sourceStr.append(" (");
            }
            if (event.getTa1ref() != null) {
                sourceStr.append(event.getTa1ref().equals(NONE) ?
                        "from docs" : "from schema");
            }
            if (!isTask2) {
                sourceStr.append(" with confidence ").append(safeGetString(event.getConfidence()));
            }
            sourceStr.append(")").append(System.lineSeparator());
        } else {
            sourceStr.append(System.lineSeparator());
        }
        return sourceStr;
    }

    private StringBuilder displayPrediction(SchemaEvent event) {
        StringBuilder predictionStr = new StringBuilder();
        if (isNonEmptyList(event.getPredictionProvenance())) {
            predictionStr.append("\tPredicted: confidence of ")
            .append(safeGetString(event.getConfidence()))
            .append(", provenance of ");
            List<String> predictionProvenance = event.getPredictionProvenance();
            for (int provCtr = 0; provCtr < predictionProvenance.size(); provCtr++) {
                String prov = predictionProvenance.get(provCtr);
                SchemaEvent provEvent = findEventById(prov);
                if (provCtr > 0) {
                    predictionStr.append(", ");
                }
                if (provEvent != null) {
                    predictionStr.append(String.format("\"%s\" [Event %s]", provEvent.getName(), getNumericId(prov)));
                } else {
                    SchemaEntity provEntity = findEntityById(prov);
                    if (provEntity != null) {
                        predictionStr.append(String.format("\"%s\" [Entity %s]", provEntity.getName(), getNumericId(prov)));
                    } else {
                        predictionStr.append(String.format("Relation [%s]", getNumericId(prov)));
                    }
                }
            }
            predictionStr.append(System.lineSeparator());
        }
        return predictionStr;
    }

    private StringBuilder displayTemporals(SchemaEvent event) {
        StringBuilder temporalStr = new StringBuilder();
        temporalStr.append("\tTemporals:").append(System.lineSeparator());
        for (Temporal temporal: event.getTemporal()) {
            if (temporal.getEarliestStartTime() != null && !temporal.getEarliestStartTime().isEmpty()) {
                temporalStr.append("\t\tEarliest start time: ").append(temporal.getEarliestStartTime()).append(System.lineSeparator());
            }
            if (temporal.getLatestStartTime() != null && !temporal.getLatestStartTime().isEmpty()) {
                temporalStr.append("\t\tLatest start time: ").append(temporal.getLatestStartTime()).append(System.lineSeparator());
            }
            if (temporal.getEarliestEndTime() != null && !temporal.getEarliestEndTime().isEmpty()) {
                temporalStr.append("\t\tEarliest end time: ").append(temporal.getEarliestEndTime()).append(System.lineSeparator());
            }
            if (temporal.getLatestEndTime() != null && !temporal.getLatestEndTime().isEmpty()) {
                temporalStr.append("\t\tLatest end time: ").append(temporal.getLatestEndTime()).append(System.lineSeparator());
            }
            if (temporal.getDuration() != null && !temporal.getDuration().isEmpty()) {
                temporalStr.append("\t\tDuration: ").append(temporal.getDuration()).append(System.lineSeparator());
            }
            if (temporal.getAbsoluteTime() != null && !temporal.getAbsoluteTime().isEmpty()) {
                temporalStr.append("\t\tAbsolute time: ").append(temporal.getAbsoluteTime()).append(System.lineSeparator());
            }
        }
        return temporalStr;
    }

    private StringBuilder displayBasicRelationInfo(Relation relation) {
        StringBuilder relationStr = new StringBuilder();
        relationStr.append("\"").append(getArgName(relation.getRelationSubject())).append("\" ")
                .append(relation.getWdLabel()).append(" ").append(relation.getWdNode()).append(" \"")
                .append(getArgName(safeGetString(relation.getRelationObject())))
                .append("\"");
        if (isNonEmptyList(relation.getModality())) {
            relationStr.append(", with modality ").append(relation.getModality());
        }

        if (isNonEmptyList(relation.getRelationProvenance())) {
            if (isTask2) {
                relationStr.append(", instantiated");
            } else {
                relationStr.append(", instantiated with confidence ").append(safeGetString(relation.getConfidence()));
            }
        }

        relationStr.append(System.lineSeparator());
        return relationStr;
    }

    private StringBuilder displayArgument(Participant participant, SchemaEvent parentEvent) {
        // Collect necessary info
        SchemaEntity entity = findEntityById(participant.getEntity());
        String entityName = "";
        boolean hasName = false;
        if (entity != null && entity.getName() != null) {
            entityName = entity.getName();
            hasName = !entityName.isEmpty();
        }
        StringBuilder fillerNames = new StringBuilder();
        boolean hasTA2name = getFillerInfo(participant.getValues(), fillerNames, parentEvent);

        // Display entity name and/or ta2entity's filler name(s)
        StringBuilder argStr = new StringBuilder(participant.getRoleName() == null ? "<no role>" : participant.getRoleName());
        if (hasName || hasTA2name) {
            argStr.append(" (");
        }
        if (hasName) {
            argStr.append(entityName).append(" [").append(getNumericId(entity.getAtId())).append("]");
            addToReferenceMap(entityReferenceMap, entity, parentEvent);
        }
        if (hasTA2name) {
            if (hasName) {
                argStr.append(parentEvent.getPredictionProvenance() == null ? " instantiated as " : " predicted as ");
            }
            argStr.append(fillerNames);
        }
        if (hasName || hasTA2name) {
            argStr.append(")");
        }

        return argStr.append(System.lineSeparator());
    }

    private boolean getFillerInfo(List<Filler> fillers, StringBuilder fillerNames, SchemaEvent parentEvent) {
        if (isNullOrEmptyList(fillers)) {
            return false;
        }

        boolean hasTA2name = false;
        for (Filler filler : fillers) {
            SchemaEntity ta2entity = findEntityById(filler.getTa2entity());
            String ta2Name = (ta2entity != null && ta2entity.getName() != null) ? ta2entity.getName() : "";  // valid SDF will have a ta2name
            if (ta2Name.isEmpty()) {
                continue;
            }
            if (hasTA2name) {
                fillerNames.append(", ");
            } else {
                hasTA2name = true;
            }
            if (isTask2) {
                fillerNames.append(String.format(QUOTED_STRING_WITH_ID_FORMAT, ta2Name,
                        getNumericId(ta2entity != null ? ta2entity.getAtId() : "UNKNOWN")));
                if (isNonEmptyList(filler.getModality())) {
                    fillerNames.append(" with modality ").append(filler.getModality());
                }
            } else {
                fillerNames.append(String.format("\"%s\" [%s] with confidence %s", ta2Name,
                        getNumericId(ta2entity != null ? ta2entity.getAtId() : "UNKNOWN"), safeGetString(filler.getConfidence())));
                if (isNonEmptyList(filler.getModality())) {
                    fillerNames.append(" and modality ").append(filler.getModality());
                }
            }
            if (parentEvent.getPredictionProvenance() == null) {
                addToReferenceMap(entityReferenceMap, ta2entity, parentEvent);
            } else {
                addToReferenceMap(entityPredictionMap, ta2entity, parentEvent);
            }
        }

        return hasTA2name;
    }

    private void addToReferenceMap(HashMap<SchemaEntity, List<SchemaEvent>> referenceMap,
                                   SchemaEntity entity, SchemaEvent event) {
        List<SchemaEvent> eventList = referenceMap.get(entity);
        if (eventList == null) {
            eventList = new ArrayList<>();
        }
        eventList.add(event);
        referenceMap.put(entity, eventList);
    }

    private void addToReferenceMap(HashMap<String, List<SchemaEvent>> referenceMap,
                                   String id, SchemaEvent event) {
        List<SchemaEvent> eventList = referenceMap.get(id);
        if (eventList == null) {
            eventList = new ArrayList<>();
        }
        eventList.add(event);
        referenceMap.put(id, eventList);
    }

    private StringBuilder displayChildren(List<Child> children, String childrenGate) {
        StringBuilder childrenStr = new StringBuilder();
        if (children == null || children.isEmpty()) {
            return childrenStr;
        }

        // Display child names
        childrenStr.append(String.format("\tChildren (%s): ", displayChildrenGate(childrenGate)));
        for (int childCtr = 0; childCtr < children.size(); childCtr++) {
            if (childCtr > 0) {
                childrenStr.append(", ");
            }
            Child child = children.get(childCtr);
            SchemaEvent childEvent = findEventById(child.getChild());
            if (childEvent != null) {
                childrenStr.append(String.format(QUOTED_STRING_WITH_ID_FORMAT, childEvent.getName(), getNumericId(childEvent.getAtId())));
            }
        }
        childrenStr.append(System.lineSeparator());

        return childrenStr;
    }

    private StringBuilder displaySubgroupEvents(List<String> children, String childrenGate) {
        StringBuilder childrenStr = new StringBuilder();
        if (children == null || children.isEmpty()) {
            return childrenStr;
        }

        // Display child names
        childrenStr.append(String.format("\tChildren (%s): ", displayChildrenGate(childrenGate)));
        for (int childCtr = 0; childCtr < children.size(); childCtr++) {
            if (childCtr > 0) {
                childrenStr.append(", ");
            }
            SchemaEvent childEvent = findEventById(children.get(childCtr));
            if (childEvent != null) {
                childrenStr.append(String.format(QUOTED_STRING_WITH_ID_FORMAT, childEvent.getName(), getNumericId(childEvent.getAtId())));
            }
        }
        childrenStr.append(System.lineSeparator());

        return childrenStr;
    }

    private String displayChildrenGate(String childrenGate) {
        if (childrenGate == null || childrenGate.isEmpty()) {
            return AND_GATE;
        } else {
            return childrenGate.toUpperCase();
        }
    }

    private <T> String safeGetString(T object) {
        return object == null ? "null" : object.toString();
    }

    private <T> String safeGetString(List<T> list) {
        return list == null || list.isEmpty() ? "null" : list.get(0).toString();
    }

    private String getNumericId(String id) {
        String numericId = numericIdCache.get(id);
        if (numericId != null) {
            return numericId;
        }

        numericId = "<UNKNOWN>";
        if (id == null) {
            return numericId;
        }

        String[] idParts = id.split("/");
        for (String part: idParts) {
            if (part.matches(String.format("\\d{%d}", UNIQUE_ID_NUMDIGITS))) {
                numericId = part;
                numericIdCache.put(id, numericId);
                break;
            }
        }
        return numericId;
    }

    private SchemaEntity findEntityById(String entityId) {
        if (entities == null || entityId == null) {
            return null;
        }

        SchemaEntity cachedEntity = entityIdCache.get(entityId);
        if (cachedEntity != null) {
            return cachedEntity;
        }

        for (SchemaEntity entity : entities) {
            if (entityId.equals(entity.getAtId())) {
                entityIdCache.put(entityId, entity);
                return entity;
            }
        }
        return null;
    }

    private SchemaEvent findEventById(String eventId) {
        if (events == null || eventId == null || eventId.isBlank()) {
            return null;
        }

        SchemaEvent cachedEvent = eventIdCache.get(eventId);
        if (cachedEvent != null) {
            return cachedEvent;
        }

        for (SchemaEvent event : events) {
            if (eventId.equals(event.getAtId())) {
                eventIdCache.put(eventId, event);
                return event;
            }
        }
        return null;
    }

    static List<SchemaEvent> getRootEvents(List<SchemaEvent> events, boolean isTA2) {
        ArrayList<SchemaEvent> rootEvents = new ArrayList<>();
        if (isNullOrEmptyList(events)) {
            return rootEvents;
        }

        if (isTA2) {
            for (SchemaEvent event : events) {
                if (event.getIsTopLevel() != null && event.getIsTopLevel()) {
                    rootEvents.add(event);
                }
            }
            return rootEvents;
        }

        // TA1 code
        HashMap<String, List<String>> eventParentMap = new HashMap<>();
        for (SchemaEvent event : events) {
            if (isNonEmptyList(event.getChildren())) {
                for (Child child : event.getChildren()) {
                    List<String> parents;
                    if (eventParentMap.containsKey(child.getChild())) {
                        parents = eventParentMap.get(child.getChild());
                    } else {
                        parents = new ArrayList<>();
                    }
                    parents.add(event.getAtId());
                    eventParentMap.put(child.getChild(), parents);
                }
            }
        }

        for (SchemaEvent event : events) {
            if (!eventParentMap.containsKey(event.getAtId())) {
                rootEvents.add(event);
            }
        }
        return rootEvents;
    }

    // Convert outlinksMap (containing only before-after via outlinks) to list of StepOrders so that Phase I code can be reused.
    private List<StepOrder> convertOutlinksMapToStepOrders(HashMap<String, List<String>> outlinksMap) {
        List<StepOrder> stepOrdersToReturn = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : outlinksMap.entrySet()) {
            String eventID = entry.getKey();
            List<String> outlinks = entry.getValue();
            StepOrder stepOrder;
            if (isNonEmptyList(outlinks)) {
                stepOrder = new StepOrder();
                stepOrder.setBefore(List.of(getNumericId(eventID)));
                stepOrder.setAfter(outlinks);
                stepOrder.container("").contained(new ArrayList<>());
                stepOrdersToReturn.add(stepOrder);
            }
        }

        return stepOrdersToReturn;
    }

    // Convert temporalMap (containing all temporal relations) to list of StepOrders so that Phase I code can be reused.
    private List<StepOrder> convertTemporalMapToStepOrders(HashMap<String, HashMap<String, List<String>>> temporalMap) {
        List<StepOrder> stepOrdersToReturn = new ArrayList<>();
        for (Map.Entry<String, HashMap<String, List<String>>> entry : temporalMap.entrySet()) {
            String eventID = entry.getKey();
            HashMap<String, List<String>> eventMap = entry.getValue();

            // Convert before relations into before/after StepOrder
            List<String> eventIds = eventMap.get(WIKI_EVENT_PREFIX + BEFORE_QID);
            StepOrder stepOrder;
            if (isNonEmptyList(eventIds)) {
                stepOrder = new StepOrder();
                stepOrder.setBefore(List.of(getNumericId(eventID)));
                stepOrder.setAfter(eventIds);
                stepOrder.container("").contained(new ArrayList<>());
                stepOrdersToReturn.add(stepOrder);
            }

            // Convert contains relations into container/contained StepOrder
            eventIds = eventMap.get(WIKI_RELATION_PREFIX + CONTAINS_QID);
            if (isNonEmptyList(eventIds)) {
                stepOrder = new StepOrder();
                stepOrder.setContainer(getNumericId(eventID));
                stepOrder.setContained(eventIds);
                stepOrder.before(new ArrayList<>()).after(new ArrayList<>());
                stepOrdersToReturn.add(stepOrder);
            }
        }
        return stepOrdersToReturn;
    }

    //
    // The rest of this is restored from Phase I AnnotatorReadableFormConversion class with almost no modification
    // except for renaming lots of variables (step -> event) and fixes to generateOrdersFromTemporal().
    // Some refactoring was later performed.
    //

    private static boolean isNullOrEmptyString(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static <T> boolean isNullOrEmptyList(List<T> list) {
        return list == null || list.isEmpty() || list.get(0) == null;
    }

    private List<SchemaEvent> assignOrderNumberToEventsTemporally(List<SchemaEvent> eventsToProcess, List<StepOrder> orders) {
        Map<String, String> eventOrderAssignment = getEventOrderAssignments(eventsToProcess, orders);

        for (SchemaEvent event : eventsToProcess) {
            String orderAssignment = eventOrderAssignment.get(getNumericId(event.getAtId()));
            event.setOrderNumber(orderAssignment);
        }

        eventsToProcess.sort((eventA, eventB) -> {
            // Determine if each event's order assignment is a single number or a range
            // If range, use the lower-limit as the comparison criterion
            String eventAOrderStr = eventA.getOrderNumber().contains("-") ? eventA.getOrderNumber().split("-")[0] : eventA.getOrderNumber();
            String eventBOrderStr = eventB.getOrderNumber().contains("-") ? eventB.getOrderNumber().split("-")[0] : eventB.getOrderNumber();

            Integer eventAOrderNo = Integer.parseInt(eventAOrderStr);
            Integer eventBOrderNo = Integer.parseInt(eventBOrderStr);

            return eventAOrderNo.compareTo(eventBOrderNo);
        });
        return eventsToProcess;
    }

    /*
     * Determine order-assignments for each event based on topological sort.
     * Subgraph-size and original index from events array are used as tie-breakers.
     * Event-consolidates are done using order information
     */
    private Map<String, String> getEventOrderAssignments(List<SchemaEvent> events, List<StepOrder> orders) {
        // Retrieve original index of each event
        Map<String, Integer> eventsOriginalIndexMap = new HashMap<>();
        for (SchemaEvent event: events) {
            eventsOriginalIndexMap.put(getNumericId(event.getAtId()), Integer.parseInt(event.getOrderNumber()));
        }

        // Generate adjacency map to represent the StepOrder graph (outgoing & incoming)
        Map<String, List<String>> outgoingAdjacenciesMap = getEventOrderGraph(orders, eventsOriginalIndexMap.keySet(), true);
        Map<String, List<String>> incomingAdjacenciesMap = getEventOrderGraph(orders, eventsOriginalIndexMap.keySet(), false);

        // Calculate subgraph-size for each event
        Map<String, Integer> subGraphSizeMap = GraphTraversal.getSubGraphSizeForAllNodes(outgoingAdjacenciesMap);

        // Sort the events and assign numbering
        return GraphTraversal.getTopologicalOrderAssignments(outgoingAdjacenciesMap, incomingAdjacenciesMap, subGraphSizeMap, eventsOriginalIndexMap);
    }

    // Generate new StepOrders, if possible, based on existing temporal relations.
    private List<StepOrder> generateOrdersFromTemporal(List<SchemaEvent> events, List<StepOrder> orders) {
        List<StepOrder> newOrders = new ArrayList<>();
        if (isNullOrEmptyList(events)) {
            return newOrders;
        }

        List<StepOrder> ordersToCheck = isNullOrEmptyList(orders) ? new ArrayList<>() : orders;
        for (int stepB = 0; stepB < events.size() - 1; stepB++) {
            for (int stepA = stepB + 1; stepA < events.size(); stepA++) {
                String stepAId = getNumericId(events.get(stepA).getAtId());
                String stepBId = getNumericId(events.get(stepB).getAtId());

                // Find stepOrders between StepA (inner loop event) and StepB (outer loop event)
                List<StepOrder> existingOrders = ordersToCheck.stream()
                        .filter(order -> hasOrderBetween(stepAId, stepBId, order))
                        .collect(Collectors.toList());

                // If we have an existing order relationship between the two events, then we don't add new ones (but see KAIR-1439).
                if (!existingOrders.isEmpty()) {
                    continue;
                }

                // Aggregate temporal info for each step (event)
                TemporalAggregate stepaTemporalInfo = new TemporalAggregate();
                stepaTemporalInfo.addTemporalInfo(events.get(stepA).getTemporal());
                TemporalAggregate stepbTemporalInfo = new TemporalAggregate();
                stepbTemporalInfo.addTemporalInfo(events.get(stepB).getTemporal());

                // If there are temporals to consider, try to generate a new StepOrder based on defined rules (see KAIR-1053).
                if (!stepaTemporalInfo.isEmpty() || !stepbTemporalInfo.isEmpty()) {
                    StepOrder newOrder = generateNewOrder(stepAId, stepaTemporalInfo, stepBId, stepbTemporalInfo);
                    if (newOrder != null) {
                        newOrders.add(newOrder);
                    }
                }
            }
        }
        return newOrders;
    }

    /*
     * Generate a new StepOrder, if possible, based on existing temporal relation(s).
     * Comparison rules defined in the comments of KAIR-1053
     */
    private StepOrder generateNewOrder(String stepAId, TemporalAggregate stepaInfo, String stepBId, TemporalAggregate stepbInfo) {
        String stepaEST = stepaInfo.getEarliestStartTime();
        String stepaLST = stepaInfo.getLatestStartTime();
        String stepaLET = stepaInfo.getLatestEndTime();
        String stepaAbsT = stepaInfo.getAbsoluteTime();
        String stepbEST = stepbInfo.getEarliestStartTime();
        String stepbLST = stepbInfo.getLatestStartTime();
        String stepbLET = stepbInfo.getLatestEndTime();
        String stepbAbsT = stepbInfo.getAbsoluteTime();
        StepOrder newOrder = null;

        if (DateComparison.before(stepaAbsT, stepbAbsT)) { // Rule 1
            newOrder = new StepOrder().addBeforeItem(stepAId).addAfterItem(stepBId);
        } else if (DateComparison.before(stepbAbsT, stepaAbsT)) { // Rule 1 reversal
            newOrder = new StepOrder().addBeforeItem(stepBId).addAfterItem(stepAId);
        } else if (DateComparison.before(stepaAbsT, stepbEST)) { // Rule 2
            newOrder = new StepOrder().addBeforeItem(stepAId).addAfterItem(stepBId);
        } else if (DateComparison.before(stepbAbsT, stepaEST)) { // Rule 2 reversal
            newOrder = new StepOrder().addBeforeItem(stepBId).addAfterItem(stepAId);
        } else if (DateComparison.beforeEqual(stepaLET, stepbEST)) { // Rule 3
            newOrder = new StepOrder().addBeforeItem(stepAId).addAfterItem(stepBId);
        } else if (DateComparison.beforeEqual(stepbLET, stepaEST)) { // Rule 3 reversal
            newOrder = new StepOrder().addBeforeItem(stepBId).addAfterItem(stepAId);
        } else if (DateComparison.beforeEqual(stepaLST, stepbEST)) { // Rule 4
            newOrder = new StepOrder().addBeforeItem(stepAId).addAfterItem(stepBId);
        } else if (DateComparison.beforeEqual(stepbLST, stepaEST)) { // Rule 4 reversal
            newOrder = new StepOrder().addBeforeItem(stepBId).addAfterItem(stepAId);
        } else if (DateComparison.beforeEqual(stepaEST, stepbEST) && DateComparison.before(stepaLST, stepbLST)) { // Rule 5
            newOrder = new StepOrder().addBeforeItem(stepAId).addAfterItem(stepBId);
        } else if (DateComparison.beforeEqual(stepbEST, stepaEST) && DateComparison.before(stepbLST, stepaLST)) { // Rule 5 reversal
            newOrder = new StepOrder().addBeforeItem(stepBId).addAfterItem(stepAId);
        } else if (DateComparison.before(stepaEST, stepbEST) && DateComparison.beforeEqual(stepaLST, stepbLST)) { // Rule 6
            newOrder = new StepOrder().addBeforeItem(stepAId).addAfterItem(stepBId);
        } else if (DateComparison.before(stepbEST, stepaEST) && DateComparison.beforeEqual(stepbLST, stepaLST)) { // Rule 6 reversal
            newOrder = new StepOrder().addBeforeItem(stepBId).addAfterItem(stepAId);
        } else if (DateComparison.before(stepaEST, stepbEST)) { // Rule 7
            newOrder = new StepOrder().addBeforeItem(stepAId).addAfterItem(stepBId);
        } else if (DateComparison.before(stepbEST, stepaEST)) { // Rule 7 reversal
            newOrder = new StepOrder().addBeforeItem(stepBId).addAfterItem(stepAId);
        } else if (DateComparison.beforeEqual(stepaLST, stepbLST) && DateComparison.beforeEqual(stepbEST, stepaEST)) { // Rule 8
            newOrder = new StepOrder().addBeforeItem(stepAId).addAfterItem(stepBId);
        } else if (DateComparison.beforeEqual(stepbLST, stepaLST) && DateComparison.beforeEqual(stepaEST, stepbEST)) { // Rule 8 reversal
            newOrder = new StepOrder().addBeforeItem(stepBId).addAfterItem(stepAId);
        }

        return newOrder;
    }

    // Return whether or not two steps have a defined order between them.
    private boolean hasOrderBetween(String stepAId, String stepBId, StepOrder order) {
        // We don't bother comparing an event against itself so double check ids
        if (!stepAId.equals(stepBId)) {
            return false;
        }

        String container = order.getContainer();
        List<String> contained = order.getContained();
        List<String> before = order.getBefore();
        List<String> after = order.getAfter();

        return (container.equals(stepBId) && contained.contains(stepAId)) ||
                (contained.contains(stepBId) && container.equals(stepAId)) ||
                (before.contains(stepBId) && after.contains(stepAId)) ||
                (after.contains(stepBId) && before.contains(stepAId));
    }

    private Map<String, List<String>> getEventOrderGraph(List<StepOrder> orders, Set<String> eventIds, boolean isOutgoing) {
        Map<String, List<String>> orderAdjacencyMap = new HashMap<>();

        // Iterate through EACH StepOrder record
        for (StepOrder curOrder : orders) {
            List<String> curFromEventIds;
            List<String> curToEventIds;

            // Only process before/after and container/contained
            if (isNonEmptyList(curOrder.getBefore()) && isNonEmptyList(curOrder.getAfter())) {
                curFromEventIds = curOrder.getBefore();
                curToEventIds = curOrder.getAfter();
            } else if (!isNullOrEmptyString(curOrder.getContainer()) && isNonEmptyList(curOrder.getContained())) {
                curFromEventIds = new ArrayList<>();
                curFromEventIds.add(curOrder.getContainer());
                curToEventIds = curOrder.getContained();
            } else {
                // Only process before and contains relations for purposes of event ordering
                curFromEventIds = new ArrayList<>();
                curToEventIds = new ArrayList<>();
            }

            // Populate the adjacent-map based on current event order; EACH fromEvent -> EACH toEvent
            // For incoming edge map; EACH toEvent -> EACH fromEvent
            if (isOutgoing) {
                JavaDataStructureUtils.safePutMapKeysVals(orderAdjacencyMap, curFromEventIds, curToEventIds);
            } else {
                JavaDataStructureUtils.safePutMapKeysVals(orderAdjacencyMap, curToEventIds, curFromEventIds);
            }
        }

        // Add any "isolated-events" to the map as well.
        JavaDataStructureUtils.safePutMapKeysIfEmpty(orderAdjacencyMap, eventIds);
        return orderAdjacencyMap;
    }

    private class TemporalAggregate extends Temporal {

        public void addTemporalInfo(List<Temporal> temporals) {
            if (temporals == null) {
                return;
            }
            for (Temporal temporal : temporals) {
                if (!isNullOrEmptyString(temporal.getEarliestStartTime())) {
                    setEarliestStartTime(temporal.getEarliestStartTime());
                }
                if (!isNullOrEmptyString(temporal.getLatestStartTime())) {
                    setLatestStartTime(temporal.getLatestStartTime());
                }
                if (!isNullOrEmptyString(temporal.getEarliestEndTime())) {
                    setEarliestEndTime(temporal.getEarliestEndTime());
                }
                if (!isNullOrEmptyString(temporal.getLatestEndTime())) {
                    setLatestEndTime(temporal.getLatestEndTime());
                }
                if (!isNullOrEmptyString(temporal.getAbsoluteTime())) {
                    setAbsoluteTime(temporal.getAbsoluteTime());
                }
            }
        }

        public boolean isEmpty() {
            return getEarliestStartTime() == null && getLatestStartTime() == null &&
                    getEarliestEndTime() == null && getLatestEndTime() == null;
        }

    }
}
