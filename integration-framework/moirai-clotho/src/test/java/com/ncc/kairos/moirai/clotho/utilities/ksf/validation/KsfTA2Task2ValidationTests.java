package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import com.ncc.kairos.moirai.clotho.model.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.ncc.kairos.moirai.clotho.resources.DefinitionConstants.EVENT;
import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfValidator.FATAL;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfValidator.ERROR;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfValidator.WARNING;

/**
 * Set of tests to show that TA2 Task 2 operations pass and fail appropriately.
 * NOTE: For now, we largely duplicate the Task 1 valid tests, and make any Task 2-specific changes.
 * @author Darren Gemoets
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KsfTA2Task2ValidationTests {
    // Modify these flags to control how tests output their models/reports and if so, how they output them
    // When DUMP_ALWAYS is false, the model is only dumped when the result is unexpected (and if invalid, the report is also dumped)
    // When DUMP_ALWAYS is true, the model is always dumped, and the report is always dumped if invalid
    private static final boolean DUMP_ALWAYS = true;
    // When DUMP_TO_FILE is false, if a model or report is dumped, it goes to stdout
    // When DUMP_TO_FILE is true, if a model or report is dumped, it goes to a file in target/test-dump-output
    private static final boolean DUMP_TO_FILE = false;

    private static TA2TestUtils utils;
    private JsonLdRepresentation model;
    private static final String FOOBAR = "foobar";
    private static final List<String> FOOBAR_LIST = Arrays.asList(FOOBAR);

    private Instance instance;
    private SchemaEntity entity;
    private SchemaEvent rootEvent;
    private String rootId;
    private SchemaEvent event;
    private Relation relation;

    @BeforeAll
    static void initTest() {
        utils = new TA2TestUtils(DUMP_ALWAYS, DUMP_TO_FILE, false);
    }

    @BeforeEach
    void setup() {
        model = utils.startNewTest();
        // Grab handles to the objects created by startNewTest
        instance = model.getInstances().get(0);
        rootEvent = utils.getEvents().get(0);
        rootId = rootEvent.getAtId();
        event = utils.getEvents().get(1);
        // Create a couple more objects for the various tests
        entity = utils.makeEntity();
        relation = utils.makeRelation(event.getAtId(), entity.getAtId(), "Q105123647");
    }

    @Nested
    class DocumentTests {
        String documentId;

        @BeforeEach
        void setup() {
            documentId = model.getAtId();
        }

        @Test
        void invalidMissingFields() {
            model.setSdfVersion("");
            model.setVersion("");
            model.setCeID(null);
            model.setInstances(null);
            utils.expectFatals(
                    KsfValidationError.constructMissingValueMessage(FATAL, DOCUMENT, documentId, SDF_VERSION),
                    KsfValidationError.constructMissingValueMessage(FATAL, DOCUMENT, documentId, CE_ID),
                    KsfValidationError.constructMissingValueMessage(FATAL, DOCUMENT, documentId, INSTANCES)
            );
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(DOCUMENT, documentId, VERSION)
            );
            utils.testInvalid("TA2 Task 2 Document-level missing fields tests");
        }

        @Test
        void invalidValues() {
            model.setAtId("");
            documentId = "<no @id>";
            model.setSdfVersion(FOOBAR);
            model.setEvents(instance.getEvents());
            model.setEntities(instance.getEntities());
            model.setRelations(instance.getRelations());
            utils.expectFatals(
                    KsfValidationError.constructInvalidValueMessage(FATAL, DOCUMENT, documentId, SDF_VERSION, model.getSdfVersion())
            );
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(DOCUMENT, documentId, JSON_LD_ID),
                    KsfValidationError.constructUnsupportedKeywordMessage(DOCUMENT, documentId, EVENTS),
                    KsfValidationError.constructUnsupportedKeywordMessage(DOCUMENT, documentId, ENTITIES),
                    KsfValidationError.constructUnsupportedKeywordMessage(DOCUMENT, documentId, RELATIONS)
            );
            utils.testInvalid("TA2 Task 2 Document-level invalid value tests");
        }

        @Test
        void invalidTooManyInstances() {
            utils.makeInstance();
            utils.makeInstance();
            utils.expectErrors(
                    KsfValidationError.constructTooManyValuesMessage(ERROR, DOCUMENT, documentId, INSTANCES, MAX_INSTANCES)
            );
            utils.testInvalid("TA2 Task 2 too many instances");
        }

        @Test
        void valid() {
            // May contain comment.
            model.setComment(List.of("document comment"));
            // Ensure ta2 is optional
            model.setTa2(null);
            utils.testValid("TA2 Task 2 document-level valid test");
        }
    } // DocumentTests

    @Nested
    class InstanceTests {
        String instantiationId;

        @BeforeEach
        void setup() {
            instantiationId = instance.getAtId();
        }

        @Test
        void invalidMissingFields() {
            instance.setName("");
            instance.setTa1ref(null);
            instance.setConfidence(null);
            instance.setEntities(null);
            instance.setEvents(null);
            instance.setRelations(null);
            utils.expectFatals(
                    KsfValidationError.constructMissingValueMessage(FATAL, INSTANCES, instantiationId, TA1_REF),
                    KsfValidationError.constructBothKeywordsMissingMessage(FATAL, INSTANCES, instantiationId, EVENTS, ENTITIES)
            );
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(INSTANCES, instantiationId, NAME),
                    KsfValidationError.constructMissingValueMessage(INSTANCES, instantiationId, CONFIDENCE)
            );
            utils.testInvalid("TA2 Task 2 instance-level missing fields test");
        }

        @Test
        void invalidMissingId() {
            instantiationId = "instance[0]";
            utils.getTopLevelRelations().clear();
            instance.setAtId("");
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(INSTANCES, instantiationId, JSON_LD_ID)
            );
            utils.testInvalid("Error if any required elements are missing: @id");
        }

        @Test
        void invalidNullTa1ref() {
            instance.setTa1ref(NONE);
            utils.expectFatals(
                    KsfValidationError.constructInvalidValueMessage(FATAL, INSTANCES, instantiationId, TA1_REF, instance.getTa1ref())
            );
            utils.testInvalid("Error if ta1ref is none");
        }

        @Test
        void invalidConfidence() {
            instance.setConfidence(List.of(5f));
            utils.expectErrors(
                    KsfValidationError.constructInvalidValueMessage(INSTANCES, instantiationId, CONFIDENCE, instance.getConfidence().get(0))
            );
            utils.testInvalid("Error if confidence is not a float between 0 and 1.0.");
        }

        @Test
        void valid() {
            // May contain comment and description.
            instance.setComment(List.of("instance comment"));
            instance.setDescription("Description of instance " + instance.getAtId());
            instance = utils.makeInstance(); // Test multiple instances
            utils.makeEntity();
            utils.testValid("TA2 Task 2 instance-level valid test");
        }
    } // InstanceTests

    @Nested
    class EntityTests {
        String entityIdentifier;

        @BeforeEach
        void setup() {
            entityIdentifier = entity.getAtId();
        }

        @Test
        void invalid() {
            entity.setName("");
            entity.setWdNode(null);
            entity.setWdLabel(null);
            entity.setWdDescription(null);
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(ENTITY, entityIdentifier, NAME),
                    KsfValidationError.constructBothKeywordsMissingMessage(ENTITY, entityIdentifier, WD_NODE, TA2WD_NODE),
                    KsfValidationError.constructBothKeywordsMissingMessage(ENTITY, entityIdentifier, WD_LABEL, TA2WD_LABEL),
                    KsfValidationError.constructBothKeywordsMissingMessage(ENTITY, entityIdentifier, WD_DESCRIPTION, TA2WD_DESCRIPTION)
            );
            utils.testInvalid("TA2 Task 2 entity-level invalid tests");
        }

        @Test
        void invalidMissingId() {
            entityIdentifier = String.format("'%s'.entities[1]", instance.getAtId());
            utils.getTopLevelRelations().clear();
            entity.setAtId("");
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(ENTITY, entityIdentifier, JSON_LD_ID)
            );
            utils.testInvalid("Error if any required elements are missing: @id");
        }

        @Test
        void invalidBothWd_nodeAndTA2wd_nodeArePresent() {
            entity.setTa2wdNode(entity.getWdNode());
            entity.setTa2wdLabel(entity.getWdLabel());
            entity.setTa2wdDescription(entity.getWdDescription());
            utils.expectErrors(
                    KsfValidationError.constructBothKeywordsPresentMessage(ENTITY, WD_NODE, TA2WD_NODE, entityIdentifier)
            );
            utils.testInvalid("Error if both wd_node and ta2wd_node are present");
        }

        @Test
        void invalidMismatchedWd_values() {
            entity.setTa2wdNode(entity.getWdNode());
            entity.setWdNode(null);
            utils.expectErrors(
                    KsfValidationError.constructMismatchedKeywordCountMessage(ENTITY, entityIdentifier, TA2WD_NODE, TA2WD_LABEL, TA2WD_DESCRIPTION),
                    KsfValidationError.constructMismatchedKeywordCountMessage(ENTITY, entityIdentifier, WD_NODE, WD_LABEL, WD_DESCRIPTION)
            );
            utils.testInvalid("Error if there is a mismatch between the number of (ta2)wd_nodes, (ta2)wd_labels, and (ta2)wd_description values");
        }

        @Test
        void valid() {
            // May contain aka, centrality, comment, origName, and reference.
            entity.setCentrality(5f);
            entity.setComment(List.of("entity comment"));
            entity.setAka(List.of("aka value"));
            entity.setReference(List.of("wiki:1234"));
            utils.makeTA2Entity();
            utils.testValid("TA2 Task 2 entity-level valid test");
        }
    } // EntityTests

    @Nested
    class EventTests {
        String eventIdentifier;

        @BeforeEach
        void setup() {
            eventIdentifier = event.getAtId();
        }

        @Test
        void invalid() {
            event.setName(" ");
            event.setDescription("");
            event.setWdNode(null);
            event.setTa1ref(null);
            utils.instantiateEvent(event);
            event.setParticipants(null);
            rootEvent.setIsTopLevel(false);
            rootEvent.setParent(null);
            event.setModality(FOOBAR_LIST);
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, NAME),
                    KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, DESCRIPTION),
                    KsfValidationError.constructMissingValueMessage(EVENT, rootId, PARENT),
                    KsfValidationError.constructBothKeywordsMissingMessage(EVENT, eventIdentifier, WD_NODE, SUBGROUP_EVENTS),
                    KsfValidationError.constructMismatchedKeywordCountMessage(EVENT, eventIdentifier, WD_NODE, WD_LABEL, WD_DESCRIPTION),
                    KsfValidationError.constructMismatchedKeywordCountMessage(EVENT, eventIdentifier, TA2WD_NODE, TA2WD_LABEL, TA2WD_DESCRIPTION),
                    KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, TA1_REF),
                    KsfValidationError.constructBothKeywordsMissingMessage(EVENT, eventIdentifier, PARTICIPANTS, SUBGROUP_EVENTS),
                    KsfValidationError.constructExactlyOneTopLevelEventsMessage(instance.getAtId()),
                    KsfValidationError.constructInvalidModalityValuesMessage(EVENT, eventIdentifier, FOOBAR_LIST)
            );
            utils.testInvalid("TA2 Task 2 event-level invalid tests");
        }

        @Test
        void invalidTask2() {
            event.setConfidence(null);
            utils.expectErrors(
                    KsfValidationError.constructHasKeywordButNotOtherKeywordMessage(EVENT,
                            eventIdentifier, PREDICTION_PROVENANCE, CONFIDENCE)
            );
            utils.testInvalid("TA2 Task 2-specific event-level invalid tests");
        }

        @Test
        void invalidKairosNullParent() {
            event.setParent(KAIROS_NULL_UNEXPANDED);
            utils.expectFatals(
                    KsfValidationError.constructRunawayChildMessage(rootId, eventIdentifier)
            );
            utils.expectErrors(
                    KsfValidationError.constructInvalidIdMessage(EVENT, eventIdentifier, PARENT, KAIROS_NULL_UNEXPANDED)
            );
            utils.testInvalid("TA2 Task 2 kairos:NULL parent invalid test");
        }

        @Test
        void invalidIds() {
            eventIdentifier = event.getName();
            utils.getTopLevelRelations().clear();
            String oldEventId = event.getAtId();
            event.setAtId("");
            event.setParent(FOOBAR);
            event.setOutlinks(FOOBAR_LIST);
            event.setSubgroupEvents(FOOBAR_LIST);
            event.setChildrenGate(AND_GATE);
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, JSON_LD_ID),
                    KsfValidationError.constructInvalidIdMessage(EVENT, eventIdentifier, PARENT, FOOBAR),
                    KsfValidationError.constructInvalidIdMessage(EVENT, eventIdentifier, OUTLINKS, FOOBAR),
                    KsfValidationError.constructInvalidIdMessage(EVENT, eventIdentifier, SUBGROUP_EVENTS, FOOBAR),
                    KsfValidationError.constructInvalidIdMessage(EVENT, eventIdentifier, PREDICTION_PROVENANCE, oldEventId),
                    KsfValidationError.constructInvalidIdMessage(EVENT, rootId, SUBGROUP_EVENTS, oldEventId)
            );
            utils.testInvalid("Task 2 Bad or missing IDs");
        }

        @Test
        void invalidNoProvenance() {
            event.setPredictionProvenance(null);
            utils.expectErrors(
                    KsfValidationError.constructExactlyOneRequiredMessage("primitive " + EVENT, eventIdentifier, PROVENANCE, PREDICTION_PROVENANCE)
            );
            utils.testInvalid("Error if both provenance and predictionProvenance are missing");
        }

        @Test
        void invalidTa1refNull() {
            event.setTa1ref(NONE);
            utils.expectErrors(
                    KsfValidationError.constructBothKeywordsMissingMessage("source-only event", eventIdentifier, TA2WD_NODE, SUBGROUP_EVENTS),
                    KsfValidationError.constructProvenanceTa1refMessage(EVENT, eventIdentifier, PROVENANCE)
            );
            utils.testInvalid("TA2 Task 2 tests when ta1ref=none");
        }

        @Test
        void invalidChildren() {
            rootEvent.setChildrenGate(FOOBAR);
            Child childObj = new Child();
            childObj.setChild(eventIdentifier);
            event.setChildren(List.of(childObj));
            SchemaEvent malformedParent = utils.makeParentEvent(AND_GATE, utils.makeLeafEvent(), utils.makeLeafEvent());
            malformedParent.setChildrenGate(null);
            utils.expectErrors(
                    KsfValidationError.constructHasKeywordButNotOtherKeywordMessage(EVENT, malformedParent.getAtId(), SUBGROUP_EVENTS, CHILDREN_GATE),
                    KsfValidationError.constructUnsupportedKeywordMessage(EVENT, eventIdentifier, CHILDREN),
                    KsfValidationError.constructInvalidValueMessage(EVENT, rootId, CHILDREN_GATE, rootEvent.getChildrenGate())
            );
            utils.testInvalid("TA2 Task 2 invalid children_gate and children keyword");
        }

        @Test
        void invalidTopLevelIssues() {
            SchemaEvent parentEvent = utils.makeParentEvent(AND_GATE, event, utils.makeLeafEvent());
            parentEvent.setIsTopLevel(true);
            utils.expectErrors(
                    KsfValidationError.constructExactlyOneTopLevelEventsMessage(instance.getAtId()),
                    KsfValidationError.constructChildIsTopLevelMessage(rootId, parentEvent.getAtId())
            );
            utils.testInvalid("TA2 Task 2 invalid topLevel issues");
        }

        @Test
        void invalidHierarchyIssues() {
            SchemaEvent parentEvent = utils.makeParentEvent(AND_GATE, event, utils.makeLeafEvent());
            SchemaEvent parentEvent2 = utils.makeParentEvent(AND_GATE, utils.makeLeafEvent(), utils.makeLeafEvent());
            SchemaEvent grandParentEvent = utils.makeParentEvent(OR_GATE, parentEvent, parentEvent2, event);
            event.setOutlinks(null);
            event.setParent(parentEvent2.getAtId());
            parentEvent.setOutlinks(List.of(eventIdentifier));
            SchemaEvent cyclicEvent = utils.makeLeafEvent();
            String cyclicID = cyclicEvent.getAtId();
            cyclicEvent.setChildrenGate(AND_GATE);
            cyclicEvent.setSubgroupEvents(List.of(cyclicID));
            cyclicEvent.setParent(cyclicID);
            rootEvent.getSubgroupEvents().remove(cyclicID);
            utils.expectFatals(
                    KsfValidationError.constructSameValueMessage(FATAL, EVENT, cyclicID, PARENT, JSON_LD_ID),
                    KsfValidationError.constructOrphanedChildMessage(eventIdentifier, parentEvent2.getAtId()),
                    KsfValidationError.constructRunawayChildMessage(grandParentEvent.getAtId(), eventIdentifier)
            );
            utils.expectErrors(
                    KsfValidationError.constructNonSiblingOutlinkMessage(parentEvent.getAtId(), eventIdentifier),
                    KsfValidationError.constructNonSiblingOutlinkMessage(parentEvent2.getAtId(), eventIdentifier)
            );
            utils.testInvalid("TA2 Task 2 invalid hierarchy issues");
        }

        @Test
        void invalidTooManyChildren() {
            for (int i = 0; i < MAX_CHILDREN; i++) {
                utils.makeLeafEvent(); // automatically placed as direct child of root
            }
            utils.expectWarnings(
                    KsfValidationError.constructPrimitivesAtRootMessage(instance.getAtId(), MAX_CHILDREN + 1),
                    KsfValidationError.constructTooManyValuesMessage(WARNING, EVENT, rootId, SUBGROUP_EVENTS, MAX_CHILDREN)
            );
            utils.testInvalid("TA2 Task 2 too many children");
        }

        @Test
        void invalidMultipleXORInstantiation() {
            utils.instantiateEvent(event);
            SchemaEvent parentEvent = utils.makeParentEvent(XOR_GATE, event, utils.instantiateEvent(utils.makeLeafEvent()));
            utils.expectWarnings(
                    KsfValidationError.constructMultipleXorProvenanceMessage(parentEvent.getAtId())
            );
            utils.expectErrors(
                    KsfValidationError.constructUnsupportedXOROutlinksMessage(event.getAtId(), parentEvent.getAtId())
            );
            utils.testInvalid("Task 2 XOR tests (contains outlinks and provenance for multiple children)");
        }

        @Test
        void invalidSimpleHierarchyCycle() {
            SchemaEvent newEvent = utils.makeLeafEvent();
            event.setOutlinks(List.of(newEvent.getAtId()));
            newEvent.setOutlinks(List.of(eventIdentifier));
            utils.expectWarnings(
                    KsfValidationError.constructPrimitivesAtRootMessage(instance.getAtId(), 2)
            );
            utils.expectErrors(
                    KsfValidationError.constructCircularOutlinksMessage(instance.getAtId())
            );
            utils.testInvalid("TA2 Task 2 simple hierarchy cycle");
        }

        @Test
        void invalidUnreducedGraph() {
            SchemaEvent newEvent1 = utils.instantiateEvent(utils.makeLeafEvent());
            SchemaEvent newEvent2 = utils.instantiateEvent(utils.makeLeafEvent());
            event.setOutlinks(List.of(newEvent1.getAtId(), newEvent2.getAtId()));
            newEvent1.setOutlinks(List.of(newEvent2.getAtId()));
            utils.expectWarnings(
                    KsfValidationError.constructPrimitivesAtRootMessage(instance.getAtId(), 3)
            );
            utils.expectErrors(
                    KsfValidationError.constructNotTransitiveReductionMessage(instance.getAtId())
            );
            utils.testInvalid("TA2 Task 2 unreduced outlink graph");
        }

        // May contain aka, comment, goal, instanceOf, maxDuration, minDuration, modality, origName, origDescription,
        // reference, relations, and repeatable
        private void populateOptionalFields(SchemaEvent event) {
            event.setAka(List.of("event aka value"));
            event.setComment(List.of("event comment"));
            event.setRepeatable(true);
            event.setGoal("event goal value");
            event.setInstanceOf(FOOBAR); // any value should be fine in TA2
            event.setMaxDuration("P3D");
            event.setMinDuration("P4D");
            event.setModality(List.of(GENERIC, HEDGED, IRREALIS, NEGATED));
            event.setReference(List.of("wiki:1234"));
            relation = utils.makeRelation(event.getAtId(), entity.getAtId(), "Q105123647", event);
        }

        @Test
        void valid() {
            populateOptionalFields(event);
            utils.testValid("Valid leaf event");
        }

        @Test
        void validInstantiatedEvent() {
            populateOptionalFields(event);
            event = utils.instantiateEvent(event);
            utils.testValid("Valid instantiated event");
        }

        @Test
        void validInstantiatedHierarchy() {
            utils.instantiateEvent(event);
            SchemaEvent newEvent1 = utils.instantiateEvent(utils.makeLeafEvent());
            SchemaEvent newEvent2 = utils.instantiateEvent(utils.makeLeafEvent());
            populateOptionalFields(event);
            populateOptionalFields(newEvent1);
            populateOptionalFields(newEvent2);
            utils.instantiateParticipant(utils.addParticipant(event, entity.getAtId()));
            utils.instantiateParticipant(utils.addParticipant(event));
            utils.instantiateParticipant(utils.addParticipant(newEvent1, entity.getAtId()));
            utils.instantiateParticipant(utils.addParticipant(newEvent1));
            utils.instantiateParticipant(utils.addParticipant(newEvent2, entity.getAtId()));
            utils.instantiateParticipant(utils.addParticipant(newEvent2));
            SchemaEvent parentEvent = utils.makeParentEvent(OR_GATE, newEvent1, newEvent2);
            SchemaEvent grandparentEvent = utils.makeParentEvent(AND_GATE, event, parentEvent);
            utils.instantiateEvent(parentEvent);
            utils.instantiateEvent(grandparentEvent);
            utils.instantiateParticipant(utils.addParticipant(parentEvent, entity.getAtId()));
            utils.instantiateParticipant(utils.addParticipant(parentEvent));
            utils.testValid("Valid instantiated hierarchy");
        }
    } // EventTests

    @Nested
    class RelationTests {
        String relationIdentifier;

        @BeforeEach
        void setup() {
            relationIdentifier = relation.getAtId();
        }

        @Test
        void invalid() {
            relation.setName("\t");
            relation.setRelationObject(new ArrayList<>());
            relation.setRelationSubject("");
            relation.setWdNode(new ArrayList<>());
            relation.setWdLabel(null);
            relation.setWdDescription(null);
            relation.setTa1ref(null);
            relation.setRelationProvenance(FOOBAR_LIST);
            relation.setModality(FOOBAR_LIST);
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, RELATION_SUBJECT),
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, RELATION_OBJECT),
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, WD_NODE),
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, WD_LABEL),
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, WD_DESCRIPTION),
                    KsfValidationError.constructAllRequiredMessage(RELATION, relationIdentifier, Arrays.asList(RELATION_PROVENANCE,
                            RELATION_OBJECT_PROV, RELATION_SUBJECT_PROV)),
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, TA1_REF),
                    KsfValidationError.constructInvalidModalityValuesMessage(RELATION, relationIdentifier, FOOBAR_LIST)
            );
            utils.expectWarnings(
                    KsfValidationError.constructMissingValueMessage(WARNING, RELATIONS, relationIdentifier, NAME)
            );
            utils.testInvalid("TA2 Task 2 relation-level invalid tests");
        }

        @Test
        void moreInvalid() {
            relation.setTa1ref(NONE);
            relation.getRelationObject().add(FOOBAR);
            relation.getWdNode().add(FOOBAR);
            relation.getWdLabel().add("");
            relation.setRelationSubject(FOOBAR);
            relation.setRelationProvenance(FOOBAR_LIST);
            utils.expectWarnings(
                    KsfValidationError.constructEmptyStringIgnoredWarning(RELATIONS, relationIdentifier, WD_LABEL)
            );
            utils.expectErrors(
                    KsfValidationError.constructAllRequiredMessage(RELATION, relationIdentifier, Arrays.asList(RELATION_PROVENANCE,
                            RELATION_OBJECT_PROV, RELATION_SUBJECT_PROV)),
                    KsfValidationError.constructInvalidIdMessage(RELATION, relationIdentifier, RELATION_OBJECT, FOOBAR),
                    KsfValidationError.constructInvalidIdMessage(RELATION, relationIdentifier, RELATION_SUBJECT, FOOBAR),
                    KsfValidationError.constructTooManyValuesMessage(RELATION, relationIdentifier, RELATION_OBJECT),
                    KsfValidationError.constructMismatchedKeywordCountMessage(RELATIONS, relationIdentifier, WD_NODE, WD_LABEL, WD_DESCRIPTION),
                    KsfValidationError.constructTooManyValuesMessage(RELATION, relationIdentifier, WD_NODE),
                    KsfValidationError.constructProvenanceTa1refMessage(RELATION, relationIdentifier, RELATION_SUBJECT_PROV),
                    KsfValidationError.constructProvenanceTa1refMessage(RELATION, relationIdentifier, RELATION_OBJECT_PROV)
            );
            utils.testInvalid("More TA2 relation-level invalid tests");
        }

        @Test
        void invalidMissingId() {
            relationIdentifier = String.format("'%s'.relation[0]", instance.getAtId());
            relation.setAtId("");
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, JSON_LD_ID)
            );
            utils.testInvalid("Error if any required elements are missing: @id");
        }

        @Test
        void invalidBeforeRelation() {
            String beforeWdNode = WIKI_EVENT_PREFIX + BEFORE_QID;
            relation.setWdNode(List.of(beforeWdNode));
            Relation relation2 = utils.makeRelation(event.getAtId(), entity.getAtId(), BEFORE_QID);
            utils.expectErrors(
                    KsfValidationError.constructInvalidValueMessage(RELATION, relationIdentifier, WD_NODE, beforeWdNode),
                    KsfValidationError.constructInvalidValueMessage(RELATION, relation2.getAtId(), WD_NODE, BEFORE_QID)
            );
            utils.testInvalid("Error if wd_node is the 'before' relation.");
        }

        void invalidCircularRelation() {
            relation = utils.makeRelation(event.getAtId(), event.getAtId(), TEMPORAL_SUBSET_IDS.get(0));
            relationIdentifier = relation.getAtId();
            utils.expectErrors(
                    KsfValidationError.constructCircularTemporalRelationMessage(relationIdentifier)
            );
            utils.testInvalid("TA2 Task 2 circular relation");
        }

        // May contain centrality, comment, modality, name, and reference.
        private void populateOptionalFields(Relation relation) {
            relation.setCentrality(5f);
            relation.setComment(List.of("relation comment"));
            relation.setModality(List.of(HEDGED, NEGATED));
            relation.setName("relation name");
            relation.setReference(List.of("wiki:1234"));
        }

        @Test
        void valid() {
            populateOptionalFields(relation);
            utils.testValid("Valid un-instantiated relation");
        }

        @Test
        void validTemporalRelation() {
            relation = utils.makeRelation(event.getAtId(), rootId, TEMPORAL_SUBSET_IDS.get(0));
            populateOptionalFields(relation);
            utils.testValid("Valid temporal relation");
        }

        @Test
        void validInstantiatedRelation() {
            populateOptionalFields(relation);
            relation = utils.instantiateRelation(relation);
            utils.testValid("Valid instantiated relation");
        }
    } // RelationTests

    @Nested
    class ParticipantTests {
        String participantIdentifier;
        Participant participant;

        @BeforeEach
        void setup() {
            participant = utils.addParticipant(event, entity.getAtId());
            participantIdentifier = participant.getAtId();
        }

        @Test
        void invalidMissingFields() {
            participant.setEntity("");
            participant.setRoleName("");
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(PARTICIPANTS, participantIdentifier, ENTITY),
                    KsfValidationError.constructMissingValueMessage(PARTICIPANTS, participantIdentifier, ROLE_NAME)
            );
            utils.testInvalid("TA2 Task 2 participant-level missing fields test");
        }

        @Test
        void invalidMissingId() {
            participantIdentifier = String.format("'%s'.participant[1]", event.getAtId());
            participant.setAtId("");
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(PARTICIPANTS, participantIdentifier, JSON_LD_ID)
            );
            utils.testInvalid("Error if any required elements are missing: @id");
        }

        @Test
        void invalidEntity() {
            participant.setEntity(FOOBAR);
            Participant participant2 = utils.addParticipant(event, utils.makeTA2Entity().getAtId()); // won't have wd_node
            utils.expectErrors(
                    KsfValidationError.constructInvalidIdMessage(PARTICIPANT, participantIdentifier, ENTITY, participant.getEntity()),
                    KsfValidationError.constructIdMissingKeywordMessage(PARTICIPANT, participantIdentifier, ENTITY, participant.getEntity(), WD_NODE),
                    KsfValidationError.constructIdMissingKeywordMessage(PARTICIPANT, participant2.getAtId(), ENTITY, participant2.getEntity(), WD_NODE)
            );
            utils.testInvalid("Errors with participant's entity keyword");
        }

        @Test
        void invalidMultipleParticipants() {
            Participant participant2 = utils.addParticipant(event, participant.getEntity());
            utils.instantiateParticipant(participant);
            participant2.setRoleName(participant.getRoleName()); // add participant with same entity and same roleName
            utils.instantiateParticipant(participant2);
            utils.addParticipant(event, participant.getEntity()); // add participant with same entity but different roleName
            utils.expectErrors(
                    KsfValidationError.constructDuplicateParticipantMessage(event.getAtId(), participant.getRoleName(), participant.getEntity())
            );
            utils.testInvalid("TA2 Task 2 invalid multiple participant test");
        }

        @Test
        void invalidMismatchedWd_values() {
            if (participant.getWdLabel() == null) {
                ArrayList<String> wd_labels = new ArrayList<>();
                wd_labels.add("");
                wd_labels.add(FOOBAR);
                participant.setWdLabel(wd_labels);
            } else {
                participant.getWdLabel().add("");
                participant.getWdLabel().add(FOOBAR);
            }
            utils.expectWarnings(
                    KsfValidationError.constructEmptyStringIgnoredWarning(PARTICIPANT, participantIdentifier, WD_LABEL)
            );
            utils.expectErrors(
                    KsfValidationError.constructMismatchedKeywordCountMessage(PARTICIPANT, participantIdentifier, WD_NODE, WD_LABEL, WD_DESCRIPTION)
            );
            utils.testInvalid("TA2 Task 2 participant wd_node/wd_label/wd_description issues");
        }

        @Test
        void valid() {
            // May contain comment, reference, values (TA2).
            participant.setComment(List.of("temporal comment"));
            participant.setReference(List.of("wiki:1234"));
            utils.addParticipant(event).setEntity(KAIROS_NULL_UNEXPANDED); // Should be valid in TA2
            utils.testValid("TA2 Task 2 participant-level valid test");
        }
    } // ParticipantTests

    @Nested
    class FillerTests {
        Participant participant;
        Filler filler;
        String fillerIdentifier;

        @BeforeEach
        void setup() {
            participant = utils.addParticipant(event);
            filler = utils.instantiateParticipant(participant);
            fillerIdentifier = filler.getAtId();
        }

        @Test
        void invalidMissingFields() {
            filler.setProvenance(new ArrayList<>());
            filler.setTa2entity("");
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(VALUES, fillerIdentifier, PROVENANCE),
                    KsfValidationError.constructMissingValueMessage(VALUES, fillerIdentifier, TA2ENTITY)
            );
            utils.testInvalid("TA2 Task 2 filler-level missing fields test");
        }

        @Test
        void invalidValues() {
            filler.setConfidence(List.of(5f));
            filler.setTa2entity(FOOBAR);
            filler.setProvenance(FOOBAR_LIST);
            filler.setModality(FOOBAR_LIST);
            utils.expectErrors(
                    KsfValidationError.constructInvalidValueMessage(VALUES, fillerIdentifier, CONFIDENCE, filler.getConfidence().get(0)),
                    KsfValidationError.constructInvalidIdMessage(VALUES, fillerIdentifier, TA2ENTITY, filler.getTa2entity()),
                    KsfValidationError.constructInvalidModalityValuesMessage(VALUES, fillerIdentifier, filler.getModality())
            );
            utils.testInvalid("TA2 Task 2 filler-level invalid values test");
        }

        @Test
        void mismatchedValues() {
            filler.setTa2entity(entity.getAtId()); // won't have ta2wd_node
            filler.setConfidence(List.of(1f, 1f)); // shouldn't cause an error in Task 2
            utils.expectErrors(
                    KsfValidationError.constructIdMissingKeywordMessage(VALUES, fillerIdentifier,
                            TA2ENTITY, filler.getTa2entity(), TA2WD_NODE)
            );
            utils.testInvalid("TA2 Task 2 filler-level mismatched values test");
        }

        @Test
        void valid() {
            // May contain comment and modality.
            filler.setComment(List.of("temporal comment"));
            event.setModality(List.of(GENERIC, HEDGED, NEGATED));
            utils.testValid("TA2 Task 2 filler-level valid test");
        }
    } // FillerTests

    @Nested
    class TemporalTests {
        String temporalIdentifier;
        Temporal temporal;

        @BeforeEach
        void setup() {
            temporalIdentifier = String.format("'%s'.%s[0]", event.getAtId(), TEMPORAL);
            temporal = utils.makeTemporal(event);
        }

        @Test
        void invalid() {
            temporal.setEarliestStartTime("");
            Temporal temporal2 = utils.makeTemporal(event);
            temporal2.setEarliestEndTime(String.valueOf(TA2TestUtils.DEFAULT_TEMPORAL_YEAR - 4) + "-04-12T13:20:00");
            utils.makeTemporal(event);
            utils.expectErrors(
                    KsfValidationError.constructEmptyTemporalMessage(temporalIdentifier),
                    KsfValidationError.constructDuplicateTemporalMessage(event.getAtId(), EARLIEST_START_TIME)
            );
            utils.expectWarnings(
                    KsfValidationError.constructContradictoryTemporalMessage(event.getAtId(), EARLIEST_START_TIME, EARLIEST_END_TIME)
            );
            utils.testInvalid("TA2 Task 2 temporal-level invalid tests");
        }

        @Test
        void invalidValues() {
            temporal.setEarliestStartTime(FOOBAR);
            temporal.setEarliestEndTime(FOOBAR);
            temporal.setLatestStartTime(FOOBAR);
            temporal.setLatestEndTime(FOOBAR);
            temporal.setAbsoluteTime(FOOBAR);
            utils.expectErrors(
                    KsfValidationError.constructMalformedTemporalMessage(event.getAtId(), EARLIEST_START_TIME),
                    KsfValidationError.constructMalformedTemporalMessage(event.getAtId(), EARLIEST_END_TIME),
                    KsfValidationError.constructMalformedTemporalMessage(event.getAtId(), LATEST_START_TIME),
                    KsfValidationError.constructMalformedTemporalMessage(event.getAtId(), LATEST_END_TIME),
                    KsfValidationError.constructMalformedTemporalMessage(event.getAtId(), ABSOLUTE_TIME)
            );
            utils.testInvalid("TA2 Task 2 temporal-level invalid values tests");
        }

        @Test
        void invalidOutlinkTemporalMismatch() {
            SchemaEvent newEvent1 = utils.instantiateEvent(utils.makeLeafEvent());
            Temporal temporal1 = utils.makeTemporal(newEvent1, String.valueOf(TA2TestUtils.DEFAULT_TEMPORAL_YEAR - 4));
            temporal1.setLatestEndTime(temporal1.getEarliestStartTime());

            SchemaEvent newEvent2 = utils.instantiateEvent(utils.makeLeafEvent());
            Temporal temporal2 = utils.makeTemporal(newEvent2, String.valueOf(TA2TestUtils.DEFAULT_TEMPORAL_YEAR - 8));
            temporal2.setLatestEndTime(temporal2.getEarliestStartTime());

            temporal.setAbsoluteTime(temporal.getEarliestStartTime());
            utils.makeParentEvent(OR_GATE, event, newEvent1, newEvent2);

            utils.expectWarnings(
                    KsfValidationError.constructOutlinkTemporalMismatchMessage(event.getAtId(), EARLIEST_START_TIME, LATEST_END_TIME),
                    KsfValidationError.constructOutlinkTemporalMismatchMessage(event.getAtId(), ABSOLUTE_TIME, LATEST_END_TIME),
                    KsfValidationError.constructOutlinkTemporalMismatchMessage(newEvent1.getAtId(), EARLIEST_START_TIME, LATEST_END_TIME)
            );
            utils.testInvalid("TA2 Task 2 outlink-temporal mismatch tests");
        }

        @Test
        void valid() {
            // May contain comment.
            temporal.setComment(List.of("temporal comment"));
            utils.testValid("TA2 Task 2 temporal-level valid test");
        }
    } // TemporalTests

} // KsfTA2Task2ValidationTests
