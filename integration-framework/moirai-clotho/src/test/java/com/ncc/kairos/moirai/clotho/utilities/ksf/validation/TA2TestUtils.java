package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import com.ncc.kairos.moirai.clotho.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.ValidationUtils.isNullOrEmptyList;

/**
 * Utilities for testing KAIROS Clotho functionality and/or creating TA2 examples.
 * Call {@link #startNewTest()} before each test to ensure a clean model.
 * Calls apply to the {@link Instance} selected via {@link #selectInstance(int)}.
 * @author Darren Gemoets, initially adapted from AIDA source code
 */
class TA2TestUtils extends TestUtils {
    private int instanceCount;
    private int valuesCount;
    private int provenanceCount;
    private int selectedInstance;
    private final boolean isTask1;
    private ArrayList<SchemaEvent> rootEvents = new ArrayList<SchemaEvent>();

    protected static final int DEFAULT_TEMPORAL_YEAR = 1976;

    /**
     * Construct TA2 test utilities.
     * @param dumpAlways whether or not always to dump the model and validation report
     * @param dumpToFile whether or not to dump to a file
     * @param isTask1 whether to request TA2 Task 1 or Task 2 utilities
     */
    TA2TestUtils(boolean dumpAlways, boolean dumpToFile, boolean isTask1) {
        super(dumpAlways, dumpToFile);
        this.isTask1 = isTask1;
    }

    /**
     * Get an appropriate performer prefix.
     * @return a TA2 performer prefix
     */
    @Override
    String getPrefix() {
        return CACI_TA2_PREFIX;
    }

    /**
     * Get the list of events from the selected {@link Instance} from model.
     * @return a {@link List} of {@link SchemaEvent}s
     */
    @Override
    List<SchemaEvent> getEvents() {
        return model.getInstances().get(selectedInstance).getEvents();
    }

    /**
     * Get the list of entities from the selected {@link Instance} from model.
     * @return a {@link List} of {@link SchemaEntity}s
     */
    @Override
    List<SchemaEntity> getEntities() {
        return model.getInstances().get(selectedInstance).getEntities();
    }

    /**
     * Get the list of top-level relations from the selected {@link Instance} from model.
     * @return a {@link List} of {@link Relation}s
     */
    @Override
    List<Relation> getTopLevelRelations() {
        return model.getInstances().get(selectedInstance).getRelations();
    }

    /**
     * Select which {@link Instance} further object creation calls will apply to.
     * @param index an index into the model's list of {@link Instance}s.
     * @throws IllegalArgumentException if the specified index cannot be mapped to an {@link Instance} in the model
     */
    void selectInstance(int index) throws IllegalArgumentException {
        if (index >= 0 && index < model.getInstances().size()) {
            selectedInstance = index;
        } else {
            throw new IllegalArgumentException(String.format("Invalid index '%d': there is/are only %d instance(s).",
                    index, model.getInstances().size()));
        }
    }

    /**
     * Returns a unique prefixed String @id for use with instances.
     */
    private String getInstanceId() {
        return getId(INSTANCES, ++instanceCount);
    }

    /**
     * Returns a unique prefixed String @id for use with instances.
     */
    private String getValuesId() {
        return getId(VALUES, ++valuesCount);
    }

    /**
     * Returns a unique prefixed String @id for use with provenance.
     */
    private String getProvenanceId() {
        return isTask1 ? getId(PROVENANCE, ++provenanceCount) : "EE2013.000740";
    }

    /**
     * Create a TA2 event with no children, adding it to the model as a child of the root event.
     * @return a valid {@link SchemaEvent} with no child events
     */
    @Override
    SchemaEvent makeLeafEvent() {
        return makeLeafEvent(rootEvents.get(selectedInstance));
    }

    /**
     * Create a TA2 event with no children, adding it to the model as a child of the specified event.
     * @return a valid {@link SchemaEvent} with no child events
     */
    SchemaEvent makeLeafEvent(SchemaEvent parent) {
        SchemaEvent event = super.makeLeafEvent();
        event.setConfidence(Arrays.asList(rand.nextFloat()));
        event.setTa1ref("EV1000" + rand.nextInt(9) + "/");
        event.setPredictionProvenance(Arrays.asList(event.getAtId()));
        event.setParent(parent.getAtId());
        List<String> children = parent.getSubgroupEvents();
        children.add(event.getAtId());
        return event;
    }

    /**
     * Create a parent event with the specified children and children_gate, adding it to the model.
     * @param gateType the logic gate to use with children_gate
     * @param children the child events to associate with the created parent event
     * @return a valid {@link SchemaEvent} with the specified children and children_gate
     */
    @Override
    SchemaEvent makeParentEvent(String gateType, SchemaEvent... children) {
        return makeParentEvent(gateType, List.of(children));
    }

    /**
     * Create a TA2 parent event with the specified children and children_gate, adding it to the model as a child of the root event.
     * @param gateType the logic gate to use with children_gate
     * @param children the child events to associate with the created parent event
     * @return a valid {@link SchemaEvent} with the specified children and children_gate
     */
    @Override
    SchemaEvent makeParentEvent(String gateType, List<SchemaEvent> children) {
        SchemaEvent event = super.makeParentEvent(gateType, children);
        if (rootEvents.size() > selectedInstance) { // chicken & egg when creating root event
            event.setParent(rootEvents.get(selectedInstance).getAtId());
            rootEvents.get(selectedInstance).getSubgroupEvents().add(event.getAtId());
        }
        event.setConfidence(Arrays.asList(rand.nextFloat()));
        event.setTa1ref("EV2000" + rand.nextInt(9) + "/");
        event.setPredictionProvenance(Arrays.asList(event.getAtId()));
        return event;
    }

    @Override
    void attachChildrenToParent(SchemaEvent parent, List<SchemaEvent> children) {
        List<String> subgroupEvents = parent.getSubgroupEvents();
        if (subgroupEvents == null) {
            subgroupEvents = new ArrayList<String>();
            parent.setSubgroupEvents(subgroupEvents);
        }
        for (int childIndex = 0; childIndex < children.size(); childIndex++) {
            SchemaEvent child = children.get(childIndex);

            // Add child to specified parent
            subgroupEvents.add(child.getAtId());

            // Detach child from original parent and set to new parent
            SchemaEvent origParent = ValidationUtils.getEventById(getEvents(), child.getParent());
            if (origParent != null) {
                origParent.getSubgroupEvents().remove(child.getAtId());
            }
            child.setParent(parent.getAtId());

            // Create an outlink from child[n] to child[n+1] in the parent event
            if (childIndex < children.size() - 1) {
                child.setOutlinks(List.of(children.get(childIndex + 1).getAtId()));
            }
        }
    }

    /**
     * Create a TA2 entity, adding it to the model.
     * @return a valid {@link SchemaEntity}
     */
    SchemaEntity makeTA2Entity() {
        SchemaEntity entity = makeEntity();
        entity.setTa2wdNode(entity.getWdNode());
        entity.setTa2wdLabel(entity.getWdLabel());
        entity.setTa2wdDescription(entity.getWdDescription());
        entity.setWdNode(null);
        entity.setWdLabel(null);
        entity.setWdDescription(null);
        return entity;
    }

    /**
     * Instantiate a TA2 {@link SchemaEvent} in a Task-specific manner.
     * @param event a {@link SchemaEvent} to instantiate
     * @return the same event, instantiated with Task-specific sample provenance
     */
    SchemaEvent instantiateEvent(SchemaEvent event) {
        event.setPredictionProvenance(null);
        event.setTa2wdNode(event.getWdNode());
        event.setTa2wdLabel(event.getWdLabel());
        event.setTa2wdDescription(event.getWdDescription());
        if (isTask1) {
            Provenance newProvenance = makeTextProvenance();
            event.setProvenance(Arrays.asList(newProvenance.getProvenanceID()));
        } else {
            event.setProvenance(Arrays.asList(this.getProvenanceId()));
            event.setConfidence(null);
        }
        List<Participant> participants = event.getParticipants();
        if (!isNullOrEmptyList(participants)) {
            instantiateParticipant(participants.get(0));
        }
        return event;
    }

    /**
     * Create a relation between two objects, adding it to the model.
     * @param subjectId a {@link String} id for the relationSubject
     * @param objectId a {@link String} id for the relationObject
     * @param relationPredicate a {@link String} qnode for the relationPredicate
     * @return a valid {@link Relation} between subject and object
     */
    @Override
    Relation makeRelation(String subjectId, String objectId, String relationPredicate) {
        Relation relation = super.makeRelation(subjectId, objectId, relationPredicate);
        relation.setTa1ref("RE1000" + rand.nextInt(9) + "/");
        return relation;
    }

    /**
     * Create a relation between two objects, adding it to the specified {@link SchemaEvent}.
     * @param subjectId a {@link String} id for the relationSubject
     * @param objectId a {@link String} id for the relationObject
     * @param relationPredicate a {@link String} qnode for the relationPredicate
     * @param event a {@link SchemaEvent} to which to add the relation
     * @return a valid {@link Relation} between subject and object, added to event
     */
    @Override
    Relation makeRelation(String subjectId, String objectId, String relationPredicate, SchemaEvent event) {
        Relation relation = super.makeRelation(subjectId, objectId, relationPredicate, event);
        relation.setTa1ref("RE2000" + rand.nextInt(9) + "/");
        return relation;
    }

    /**
     * Instantiate a TA2 {@link Relation} in a Task-specific manner.
     * @param relation a {@link Relation} to instantiate
     * @return the same relation, instantiated with confidence (Task 1) and Task-specific sample provenance
     */
    Relation instantiateRelation(Relation relation) {
        if (isTask1) {
            Provenance newProvenance = makeTextProvenance();
            relation.setRelationProvenance(Arrays.asList(newProvenance.getProvenanceID()));
            relation.setRelationObjectProv(Arrays.asList(newProvenance.getProvenanceID()));
            relation.setRelationSubjectProv(Arrays.asList(newProvenance.getProvenanceID()));
            relation.setConfidence(Arrays.asList(rand.nextFloat()));
        } else {
            relation.setRelationProvenance(Arrays.asList(this.getProvenanceId()));
            relation.setRelationObjectProv(Arrays.asList(this.getProvenanceId()));
            relation.setRelationSubjectProv(Arrays.asList(this.getProvenanceId()));
        }
        return relation;
    }

    /**
     * Instantiate a TA2 {@link Participant} in a Task-specific manner.
     * @param participant a {@link Participant} to instantiate
     * @return the same relation, instantiated with a Task-specific argument {@link Filler}
     */
    Filler instantiateParticipant(Participant participant) {
        Filler filler = new Filler();
        filler.setAtId(getValuesId());
        if (isTask1) {
            filler.setConfidence(Arrays.asList(rand.nextFloat()));
            Provenance newProvenance = makeTextProvenance();
            filler.setProvenance(Arrays.asList(newProvenance.getProvenanceID()));
        } else {
            filler.setProvenance(Arrays.asList(this.getProvenanceId()));
        }
        filler.setTa2entity(makeTA2Entity().getAtId());
        List<Filler> fillerList = participant.getValues();
        if (fillerList == null) {
            fillerList = new ArrayList<>();
            participant.setValues(fillerList);
        }
        fillerList.add(filler);
        return filler;
    }

    /**
     * Create sample temporal data, adding it to the specified {@link SchemaEvent}.
     * @param event a {@link SchemaEvent} to which to add the temporal
     * @return a valid {@link Temporal} object
     */
    Temporal makeTemporal(SchemaEvent event, String year) {
        Temporal temporal = makeTemporal(year);
        List<Temporal> temporals = event.getTemporal();
        if (isNullOrEmptyList(temporals)) {
            temporals = new ArrayList<Temporal>();
        }
        temporals.add(temporal);
        event.setTemporal(temporals);
        return temporal;
    }

    /**
     * Create sample temporal data, adding it to the specified {@link SchemaEvent}.
     * @param event a {@link SchemaEvent} to which to add the temporal
     * @return a valid {@link Temporal} object
     */
    Temporal makeTemporal(SchemaEvent event) {
        return makeTemporal(event, String.valueOf(DEFAULT_TEMPORAL_YEAR));
    }

    /**
     * Create sample temporal data.
     * @return a valid {@link Temporal} object
     */
    private Temporal makeTemporal(String year) {
        Temporal temporal = new Temporal();
        if (isTask1) {
            temporal.setConfidence(Arrays.asList(rand.nextFloat()));
        }
        temporal.setEarliestStartTime(year + "-07-21T00:00:00");
        return temporal;
    }

    // Used by all of the makeXYZProvenance methods
    private Provenance makeGenericProvenance(String mediaType) {
        if (!isTask1) {
            throw new UnsupportedOperationException();
        }
        Provenance provenance = new Provenance();
        provenance.setProvenanceID(getProvenanceId());
        provenance.setMediaType(mediaType);
        provenance.setChildID("L0C04GM35");
        ArrayList<String> parentIDs = new ArrayList<>();
        parentIDs.add("L0C04FMC9");
        provenance.setParentIDs(parentIDs);
        return provenance;
    }

    /**
     * Make a new text provenance entry, adding it to the model.
     * @return the newly created {@link Provenance}
     */
    Provenance makeTextProvenance() {
        Provenance provenance = makeGenericProvenance(KsfProvenanceData.TEXT_MEDIA_TYPES.get(0));
        provenance.setSourceURL("https://en.wikipedia.org/wiki/Plain_text");
        provenance.setOffset(0);
        provenance.setLength(76);
        model.getProvenanceData().add(provenance);
        return provenance;
    }

    /**
     * Make a new image provenance entry, adding it to the model.
     * @return the newly created {@link Provenance}
     */
    Provenance makeImageProvenance() {
        Provenance provenance = makeGenericProvenance(KsfProvenanceData.IMAGE_MEDIA_TYPES.get(0));
        provenance.setSourceURL("https://en.wikipedia.org/wiki/Image");
        ArrayList<Integer> bbList = new ArrayList<>(List.of(100, 101, 200, 201));
        provenance.setBoundingBox(bbList);
        model.getProvenanceData().add(provenance);
        return provenance;
    }

    /**
     * Make a new audio provenance entry, adding it to the model.
     * @return the newly created {@link Provenance}
     */
    Provenance makeAudioProvenance() {
        Provenance provenance = makeGenericProvenance(KsfProvenanceData.AUDIO_MEDIA_TYPES.get(0));
        provenance.setSourceURL("https://en.wikipedia.org/wiki/Audio_file_format");
        provenance.setStartTime(2.56f);
        provenance.setEndTime(5.12f);
        model.getProvenanceData().add(provenance);
        return provenance;
    }

    /**
     * Make a new time range video provenance entry, adding it to the model.
     * @return the newly created {@link Provenance}
     */
    Provenance makeTimeRangeVideoProvenance() {
        Provenance provenance = makeKeyFrameVideoProvenance();
        provenance.setSourceURL("https://en.wikipedia.org/wiki/Video_file_format");
        provenance.setKeyframes(null);
        return provenance;
    }

    /**
     * Make a new keyframe video provenance entry, adding it to the model.
     * @return the newly created {@link Provenance}
     */
    Provenance makeKeyFrameVideoProvenance() {
        Provenance provenance = makeGenericProvenance(KsfProvenanceData.VIDEO_MEDIA_TYPES.get(0));
        provenance.setSourceURL("https://en.wikipedia.org/wiki/Key_frame");
        ArrayList<Integer> intList = new ArrayList<>(List.of(100, 101, 200, 201));
        provenance.setBoundingBox(intList);
        intList = new ArrayList<>(List.of(60, 61, 62));
        provenance.setKeyframes(intList);
        provenance.setStartTime(2.56f);
        provenance.setEndTime(5.12f);
        model.getProvenanceData().add(provenance);
        return provenance;
    }

    /**
     * Make a new, valid {@link Instance}, with a single root and leaf {@link SchemaEvent}, adding it to the model.
     * This also selects the instance via {@link #selectInstance(int)}.
     * @return a new {@link Instance}, with a single {@link SchemaEvent}.
     */
    Instance makeInstance() {
        Instance instance = new Instance();
        instance.setAtId(getInstanceId());
        instance.setConfidence(Arrays.asList(rand.nextFloat()));
        instance.setName("instance-" + instanceCount);
        instance.setTa1ref("SC12345/");
        instance.setEvents(new ArrayList<>());
        instance.setEntities(new ArrayList<>());
        instance.setRelations(new ArrayList<>());
        model.getInstances().add(instance);
        selectInstance(model.getInstances().size() - 1);
        SchemaEvent rootEvent = makeParentEvent(AND_GATE, new ArrayList<SchemaEvent>());
        rootEvent.setParent(KAIROS_NULL_UNEXPANDED);
        rootEvent.setIsTopLevel(true);
        rootEvents.add(rootEvent);
        makeLeafEvent(rootEvent);
        return instance;
    }

    /**
     * Call before each TA2 test.  Returns a new, valid model with standard TA-specific SDF metadata.
     * It includes a single {@link SchemaEvent} and {@link Provenance} (Task 1 only).
     * @return a new model with which to start a test
     */
    @Override
    JsonLdRepresentation startNewTest() {
        super.startNewTest();
        model.setTa2(true);
        model.setTask2(!isTask1);
        model.setCeID("ce2013");
        instanceCount = 0;
        provenanceCount = 0;
        model.setInstances(new ArrayList<>());
        rootEvents.clear();
        makeInstance();
        if (isTask1) {
            model.setProvenanceData(new ArrayList<>());
            makeTextProvenance();
            KsfProvenanceData.setPassThroughMode(true);
        }
        return model;
    }

}
