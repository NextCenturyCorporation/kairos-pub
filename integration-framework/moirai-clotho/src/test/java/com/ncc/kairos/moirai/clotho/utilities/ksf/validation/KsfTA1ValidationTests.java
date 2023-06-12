package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import com.ncc.kairos.moirai.clotho.model.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfValidator.FATAL;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfValidator.WARNING;

/**
 * Set of tests to show that TA1 operations pass and fail appropriately.
 * @author Darren Gemoets
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KsfTA1ValidationTests {
    // Modify these flags to control how tests output their models/reports and if so, how they output them
    // When DUMP_ALWAYS is false, the model is only dumped when the result is unexpected (and if invalid, the report is also dumped)
    // When DUMP_ALWAYS is true, the model is always dumped, and the report is always dumped if invalid
    private static final boolean DUMP_ALWAYS = true;
    // When DUMP_TO_FILE is false, if a model or report is dumped, it goes to stdout
    // When DUMP_TO_FILE is true, if a model or report is dumped, it goes to a file in target/test-dump-output
    private static final boolean DUMP_TO_FILE = false;

    private static TA1TestUtils utils;
    private JsonLdRepresentation model;
    private static final String FOOBAR = "foobar";
    private static final List<String> FOOBAR_LIST = Arrays.asList(FOOBAR);
    SchemaEntity entity;
    SchemaEvent event;
    Relation relation;

    @BeforeAll
    static void initTest() {
        utils = new TA1TestUtils(DUMP_ALWAYS, DUMP_TO_FILE);
    }

    @BeforeEach
    void setup() {
        model = utils.startNewTest();
        // Grab handles to the objects created by startNewTest
        event = utils.getEvents().get(0);
        // Create a couple more objects for the various tests
        entity = utils.makeEntity();
        relation = utils.makeRelation(event.getAtId(), entity.getAtId(), "Q105123647");
    }

    @Nested
    class DocumentLevelTests {
        String documentId;

        @BeforeEach
        void setup() {
            documentId = model.getAtId();
        }

        @Test
        void invalidMissingFields() {
            model.setSdfVersion("");
            model.setVersion("");
            model.setEvents(null);
            model.setEntities(null);
            model.setRelations(null);
            utils.expectFatals(
                    KsfValidationError.constructMissingValueMessage(FATAL, DOCUMENT, documentId, SDF_VERSION),
                    KsfValidationError.constructBothKeywordsMissingMessage(FATAL, DOCUMENT, documentId, EVENTS, ENTITIES)
            );
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(DOCUMENT, documentId, VERSION)
            );
            utils.testInvalid("TA1 Document-level missing fields tests");
        }

        @Test
        void invalidValues() {
            model.setAtId("");
            documentId = "<no @id>";
            model.setSdfVersion(FOOBAR);
            utils.expectFatals(
                    KsfValidationError.constructInvalidValueMessage(FATAL, DOCUMENT, documentId, SDF_VERSION, model.getSdfVersion())
            );
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(DOCUMENT, documentId, JSON_LD_ID)
            );
            utils.testInvalid("TA1 Document-level invalid value tests");
        }

        @Test
        void valid() {
            model.setComment(List.of("document comment"));
            model.setTask2(false); // demonstrates that it's valid to include the keyword, even if it's ignored
            utils.testValid("TA1 document-level valid tests.");
        }
    }

    @Nested
    class EntityTests {
        String entityIdentifier;

        @BeforeEach
        void setup() {
            entityIdentifier = entity.getAtId();
        }

        @Test
        void invalidFields() {
            entity.setName("");
            entity.setWdNode(null);
            entity.setWdLabel(null);
            entity.setWdDescription(null);
            entity.setTa2wdNode(FOOBAR_LIST);
            entity.setTa2wdLabel(FOOBAR_LIST);
            entity.setTa2wdDescription(FOOBAR_LIST);
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(ENTITY, entityIdentifier, NAME),
                    KsfValidationError.constructMissingValueMessage(ENTITY, entityIdentifier, WD_NODE),
                    KsfValidationError.constructMissingValueMessage(ENTITY, entityIdentifier, WD_LABEL),
                    KsfValidationError.constructMissingValueMessage(ENTITY, entityIdentifier, WD_DESCRIPTION),
                    KsfValidationError.constructUnsupportedKeywordMessage(ENTITY, entityIdentifier, TA2WD_NODE),
                    KsfValidationError.constructUnsupportedKeywordMessage(ENTITY, entityIdentifier, TA2WD_LABEL),
                    KsfValidationError.constructUnsupportedKeywordMessage(ENTITY, entityIdentifier, TA2WD_DESCRIPTION)
            );
            utils.testInvalid("TA1 entity-level invalid field tests");
        }

        @Test
        void invalidMissingId() {
            entityIdentifier = String.format("'%s'.entities[1]", model.getAtId());
            utils.getTopLevelRelations().clear();
            entity.setAtId("");
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(ENTITY, entityIdentifier, JSON_LD_ID)
            );
            utils.testInvalid("Error if any required elements are missing: @id");
        }

        @Test
        void invalidMismatchedWd_values() {
            entity.getWdLabel().add("");
            entity.getWdLabel().add(FOOBAR);
            utils.expectWarnings(
                    KsfValidationError.constructEmptyStringIgnoredWarning(ENTITY, entityIdentifier, WD_LABEL)
            );
            utils.expectErrors(
                    KsfValidationError.constructMismatchedKeywordCountMessage(ENTITY, entityIdentifier, WD_NODE, WD_LABEL, WD_DESCRIPTION)
            );
            utils.testInvalid("TA1 entity wd_node/wd_label/wd_description issues");
        }

        @Test
        void valid() {
            // May contain aka, centrality, comment, and reference.
            entity.setCentrality(1f);
            entity.setComment(List.of("entity comment"));
            entity.setAka(List.of("aka value"));
            entity.setReference(List.of("wiki:1234"));
            utils.testValid("TA1 entity-level valid test");
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
        void invalidMissingFields() {
            event.setName("");
            event.setDescription("");
            event.setParticipants(null);
            event.setWdNode(null);
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, NAME),
                    KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, DESCRIPTION),
                    KsfValidationError.constructBothKeywordsMissingMessage(EVENT, eventIdentifier, WD_NODE, CHILDREN),
                    KsfValidationError.constructBothKeywordsMissingMessage(EVENT, eventIdentifier, PARTICIPANTS, CHILDREN),
                    KsfValidationError.constructMismatchedKeywordCountMessage(EVENT, eventIdentifier, WD_NODE, WD_LABEL, WD_DESCRIPTION)
            );
            utils.testInvalid("TA1 event-level missing field tests");
        }

        @Test
        void invalidValues() {
            event.getWdLabel().add("");
            event.getWdLabel().add(FOOBAR);
            event.setInstanceOf(FOOBAR);
            event.setModality(FOOBAR_LIST);
            utils.expectWarnings(
                    KsfValidationError.constructEmptyStringIgnoredWarning(EVENT, eventIdentifier, WD_LABEL)
            );
            utils.expectErrors(
                    KsfValidationError.constructMismatchedKeywordCountMessage(EVENT, eventIdentifier, WD_NODE, WD_LABEL, WD_DESCRIPTION),
                    KsfValidationError.constructInvalidIdMessage(EVENT, eventIdentifier, INSTANCE_OF, FOOBAR),
                    KsfValidationError.constructInvalidModalityValuesMessage(EVENT, eventIdentifier, FOOBAR_LIST)
            );
            utils.testInvalid("TA1 event-level invalid value tests");
        }

        @Test
        void invalidMissingId() {
            eventIdentifier = event.getName();
            utils.getTopLevelRelations().clear();
            event.setAtId("");
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, JSON_LD_ID)
            );
            utils.testInvalid("Error if any required elements are missing: @id");
        }

        @Test
        void invalidChildrenGate() {
            SchemaEvent parentEvent = utils.makeParentEvent(FOOBAR, event, utils.makeLeafEvent());
            eventIdentifier = parentEvent.getAtId();
            SchemaEvent malformedParent = utils.makeParentEvent(AND_GATE, event, utils.makeLeafEvent());
            malformedParent.setChildrenGate(null);
            utils.expectErrors(
                    KsfValidationError.constructHasKeywordButNotOtherKeywordMessage(EVENT, malformedParent.getAtId(), CHILDREN, CHILDREN_GATE),
                    KsfValidationError.constructInvalidValueMessage(EVENT, eventIdentifier, CHILDREN_GATE, parentEvent.getChildrenGate())
            );
            utils.testInvalid("TA1 invalid children_gate");
        }

        // May contain aka, comment, goal, instanceOf, maxDuration, minDuration, modality,
        // reference, relations
        private void populateOptionalFields(SchemaEvent event) {
            event.setAka(List.of("event aka value"));
            event.setComment(List.of("event comment"));
            event.setGoal("event goal value");
            event.setInstanceOf(event.getAtId()); // Valid, if nonsensical
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
        void validHierarchy() {
            SchemaEvent newEvent1 = utils.makeLeafEvent();
            SchemaEvent newEvent2 = utils.makeLeafEvent();
            populateOptionalFields(event);
            populateOptionalFields(newEvent1);
            populateOptionalFields(newEvent2);
            utils.addParticipant(event, entity.getAtId());
            utils.addParticipant(event);
            utils.addParticipant(newEvent1, entity.getAtId());
            utils.addParticipant(newEvent1);
            utils.addParticipant(newEvent2, entity.getAtId());
            utils.addParticipant(newEvent2);
            SchemaEvent parentEvent = utils.makeParentEvent(OR_GATE, event, newEvent1, newEvent2);
            utils.addParticipant(parentEvent, entity.getAtId());
            utils.addParticipant(parentEvent);
            utils.testValid("Valid hierarchy");
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
            relation.setName(null);
            relation.setRelationObject(new ArrayList<>());
            relation.setRelationSubject("");
            relation.setWdNode(new ArrayList<>());
            relation.setWdLabel(null);
            relation.setWdDescription(null);
            relation.setModality(FOOBAR_LIST);
            relation.setCentrality(5f);
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, RELATION_SUBJECT),
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, RELATION_OBJECT),
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, WD_NODE),
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, WD_LABEL),
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, WD_DESCRIPTION),
                    KsfValidationError.constructInvalidModalityValuesMessage(RELATION, relationIdentifier, FOOBAR_LIST),
                    KsfValidationError.constructInvalidValueMessage(RELATION, relationIdentifier, CENTRALITY, relation.getCentrality())
            );
            utils.expectWarnings(
                    KsfValidationError.constructMissingValueMessage(WARNING, RELATIONS, relationIdentifier, NAME)
            );
            utils.testInvalid("TA1 relation-level invalid tests");
        }

        @Test
        void moreInvalid() {
            relation.getWdNode().add(FOOBAR);
            relation.getWdLabel().add("");
            relation.setRelationObject(FOOBAR_LIST);
            relation.setRelationSubject(FOOBAR);
            utils.expectWarnings(
                    KsfValidationError.constructEmptyStringIgnoredWarning(RELATIONS, relationIdentifier, WD_LABEL)
            );
            utils.expectErrors(
                    KsfValidationError.constructMismatchedKeywordCountMessage(RELATIONS, relationIdentifier, WD_NODE, WD_LABEL, WD_DESCRIPTION),
                    KsfValidationError.constructTooManyValuesMessage(RELATION, relationIdentifier, WD_NODE),
                    KsfValidationError.constructInvalidIdMessage(RELATION, relationIdentifier, RELATION_OBJECT, FOOBAR),
                    KsfValidationError.constructInvalidIdMessage(RELATION, relationIdentifier, RELATION_SUBJECT, FOOBAR)
            );
            utils.testInvalid("More TA1 relation-level invalid tests");
        }

        @Test
        void invalidMissingId() {
            relationIdentifier = String.format("'%s'.relation[0]", model.getAtId());
            relation.setAtId("");
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, JSON_LD_ID)
            );
            utils.testInvalid("Error if any required elements are missing: @id");
        }

        @Test
        void invalidCircularRelation() {
            relation = utils.makeRelation(event.getAtId(), event.getAtId(), TEMPORAL_SUBSET_IDS.get(0));
            relationIdentifier = relation.getAtId();
            utils.expectErrors(
                    KsfValidationError.constructCircularTemporalRelationMessage(relationIdentifier)
            );
            utils.testInvalid("TA1 circular relation");
        }

        // May contain centrality, comment, modality, name, and reference.
        private void populateOptionalFields(Relation relation) {
            relation.setCentrality(1f);
            relation.setComment(List.of("relation comment"));
            relation.setModality(List.of(HEDGED, NEGATED));
            relation.setName("relation name");
            relation.setReference(List.of("wiki:1234"));
        }

        @Test
        void valid() {
            populateOptionalFields(relation);
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
            utils.testInvalid("TA1 participant-level missing fields test");
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
            utils.expectErrors(
                    KsfValidationError.constructInvalidIdMessage(PARTICIPANT, participantIdentifier, ENTITY, participant.getEntity())
            );
            utils.testInvalid("TA1 participant-level invalid values test");
        }

        @Test
        void invalidMultipleParticipants() {
            Participant participant2 = utils.addParticipant(event, participant.getEntity());
            participant2.setRoleName(participant.getRoleName()); // add participant with same entity and same roleName
            utils.addParticipant(event, participant.getEntity()); // add participant with same entity but different roleName
            utils.expectErrors(
                    KsfValidationError.constructDuplicateParticipantMessage(event.getAtId(), participant.getRoleName(), participant.getEntity())
            );
            utils.testInvalid("TA1 invalid multiple participant test");
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
            utils.testInvalid("TA1 participant wd_node/wd_label/wd_description issues");
        }

        @Test
        void valid() {
            // May contain comment, reference.
            participant.setComment(List.of("temporal comment"));
            participant.setReference(List.of("wiki:1234"));
            utils.testValid("TA1 participant-level valid test");
        }
    } // ParticipantTests

    @Nested
    class ChildrenTests {
        Child child;
        String childIdentifier;
        SchemaEvent parentEvent;
        String parentId;

        @BeforeEach
        void setup() {
            parentEvent = utils.makeParentEvent(AND_GATE, event, utils.makeLeafEvent(), utils.makeLeafEvent());
            parentId = parentEvent.getAtId();
            child = parentEvent.getChildren().get(0);
            childIdentifier = String.format("%s[%d]", parentId, 0);
        }

        @Test
        void invalidMissingFields() {
            child.setChild("");
            utils.expectErrors(
                    KsfValidationError.constructMissingValueMessage(CHILD, childIdentifier, CHILD)
            );
            utils.testInvalid("TA1 child-level missing fields test");
        }

        @Test
        void invalidValues() {
            child.setChild(FOOBAR);
            child.setImportance(5f);
            utils.expectErrors(
                    KsfValidationError.constructInvalidIdMessage(EVENT, parentId, CHILD, child.getChild()),
                    KsfValidationError.constructInvalidValueMessage(CHILD, childIdentifier, IMPORTANCE, child.getImportance())
            );
            utils.testInvalid("TA1 child-level invalid values test");
        }

        @Test
        void valid() {
            // May contain comment, importance, optional, repeatable.
            child.setComment(List.of("child comment"));
            child.setImportance(1f);
            child.setOptional(true);
            child.repeatable(true);
            utils.testValid("TA1 child-level valid test");
        }
    } // ChildrenTests

} // KsfTA1ValidationTests
