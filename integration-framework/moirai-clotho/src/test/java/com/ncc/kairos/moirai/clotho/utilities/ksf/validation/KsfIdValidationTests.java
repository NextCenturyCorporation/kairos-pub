package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import com.ncc.kairos.moirai.clotho.model.*;
import com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfValidator.FATAL;

/**
 * Set of tests to show that duplicate and malformed IDs and ta1refs cause appropriate failures.
 * @author Darren Gemoets
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KsfIdValidationTests {
    // Modify these flags to control how tests output their models/reports and if so, how they output them
    // When DUMP_ALWAYS is false, the model is only dumped when the result is unexpected (and if invalid, the report is also dumped)
    // When DUMP_ALWAYS is true, the model is always dumped, and the report is always dumped if invalid
    private static final boolean DUMP_ALWAYS = true;
    // When DUMP_TO_FILE is false, if a model or report is dumped, it goes to stdout
    // When DUMP_TO_FILE is true, if a model or report is dumped, it goes to a file in target/test-dump-output
    private static final boolean DUMP_TO_FILE = false;

    private JsonLdRepresentation model;
    private static final String FOOBAR = "foobar";
    private static final String KE_TYPE_VIOLATION = "object type";
    private static final String DIGIT_VIOLATION = "5-digit int";
    private static final List<String> VIOLATION_TYPES = List.of(KE_TYPE_VIOLATION, DIGIT_VIOLATION);
    private static final List<String> ILLEGAL_URI_CHARS = // See https://datatracker.ietf.org/doc/html/rfc3987#section-3.1
            List.of(" ", "%", "\\", "\"", "^", "[", "]", "{", "}", "|", "<", ">", "`");

    @Test
    void testTA1duplicateIds() {
        TA1TestUtils utils = new TA1TestUtils(DUMP_ALWAYS, DUMP_TO_FILE);
        model = utils.startNewTest();
        testDuplicateIds(utils);
    }

    @Test
    void testTA2Task1duplicateIds() {
        TA2TestUtils utils = new TA2TestUtils(DUMP_ALWAYS, DUMP_TO_FILE, true);
        model = utils.startNewTest();
        testDuplicateIds(utils);
        testDuplicateTA2Ids(utils);
        testDuplicateProvenanceIds(utils);
    }

    @Test
    void testTA2Task2duplicateIds() {
        TA2TestUtils utils = new TA2TestUtils(DUMP_ALWAYS, DUMP_TO_FILE, false);
        model = utils.startNewTest();
        testDuplicateIds(utils);
        testDuplicateTA2Ids(utils);
    }

    @Test
    void testTA1malformedIds() {
        TA1TestUtils utils = new TA1TestUtils(DUMP_ALWAYS, DUMP_TO_FILE);
        VIOLATION_TYPES.forEach(violationType -> testMalformedIds(utils, violationType));
        ILLEGAL_URI_CHARS.forEach(illegalChar -> testIllegalIdURIs(utils, illegalChar));
        ILLEGAL_URI_CHARS.forEach(illegalChar -> testIllegalTA1URIs(utils, illegalChar));
    }

    @Test
    void testTA2Task1malformedIds() {
        TA2TestUtils utils = new TA2TestUtils(DUMP_ALWAYS, DUMP_TO_FILE, true);
        VIOLATION_TYPES.forEach(violationType -> testMalformedIds(utils, violationType));
        VIOLATION_TYPES.forEach(violationType -> testMalformedTA2Ids(utils, violationType));
        VIOLATION_TYPES.forEach(violationType -> testMalformedProvenanceIds(utils, violationType));
        ILLEGAL_URI_CHARS.forEach(illegalChar -> testIllegalIdURIs(utils, illegalChar));
        ILLEGAL_URI_CHARS.forEach(illegalChar -> testIllegalTA1URIs(utils, illegalChar));
        ILLEGAL_URI_CHARS.forEach(illegalChar -> testIllegalTA2URIs(utils, illegalChar, false));
    }

    @Test
    void testTA2Task2malformedIds() {
        TA2TestUtils utils = new TA2TestUtils(DUMP_ALWAYS, DUMP_TO_FILE, false);
        VIOLATION_TYPES.forEach(violationType -> testMalformedIds(utils, violationType));
        VIOLATION_TYPES.forEach(violationType -> testMalformedTA2Ids(utils, violationType));
        ILLEGAL_URI_CHARS.forEach(illegalChar -> testIllegalIdURIs(utils, illegalChar));
        ILLEGAL_URI_CHARS.forEach(illegalChar -> testIllegalTA1URIs(utils, illegalChar));
        ILLEGAL_URI_CHARS.forEach(illegalChar -> testIllegalTA2URIs(utils, illegalChar, true));
    }

    private void testIllegalIdURIs(TestUtils utils, String illegalChar) {
        model = utils.startNewTest();
        SchemaEvent event = model.getTa2() ? utils.makeLeafEvent() : utils.getEvents().get(0); // TA1 event created by startNewTest()
        SchemaEntity entity = utils.makeEntity();
        Participant participant = utils.addParticipant(event);
        Relation relation = utils.makeRelation(event.getAtId(), entity.getAtId(), "Q105123647");
        String badEventId = event.getAtId().replace(getDigits(event.getAtId()), getDigits(event.getAtId()).concat(illegalChar));
        String badEntityId = entity.getAtId().replace(getDigits(entity.getAtId()), getDigits(entity.getAtId()).concat(illegalChar));
        String badParticipantId = participant.getAtId().replace(getDigits(participant.getAtId()), getDigits(participant.getAtId()).concat(illegalChar));
        String badRelationId = relation.getAtId().replace(getDigits(relation.getAtId()), getDigits(relation.getAtId()).concat(illegalChar));
        if (model.getTa2()) { // keep hierarchy as consistent as possible
            SchemaEvent rootEvent = utils.getEvents().get(0);
            rootEvent.getSubgroupEvents().remove(event.getAtId());
        }
        event.setAtId(badEventId);
        event.setParent(badEventId);
        event.setPredictionProvenance(List.of(badEventId));
        entity.setAtId(badEntityId);
        participant.setAtId(badParticipantId);
        relation.setAtId(badRelationId);
        relation.setRelationSubject(badEventId);
        relation.setRelationObject(List.of(badEntityId));
        List<String> errorList = new ArrayList<>();
        errorList.add(KsfValidationError.constructInvalidURIMessage(EVENTS, event.getAtId(), JSON_LD_ID, badEventId));
        errorList.add(KsfValidationError.constructInvalidURIMessage(EVENT, event.getAtId(), PARENT, event.getParent()));
        errorList.add(KsfValidationError.constructInvalidURIMessage(ENTITIES, entity.getAtId(), JSON_LD_ID, badEntityId));
        errorList.add(KsfValidationError.constructInvalidURIMessage(PARTICIPANTS, participant.getAtId(), JSON_LD_ID, badParticipantId));
        errorList.add(KsfValidationError.constructInvalidURIMessage(RELATIONS, relation.getAtId(), JSON_LD_ID, badRelationId));
        if (model.getTa2()) {
            Filler filler = ((TA2TestUtils) utils).instantiateParticipant(participant);
            String badValuesId = filler.getAtId().replace(getDigits(filler.getAtId()), getDigits(filler.getAtId()).concat(illegalChar));
            filler.setAtId(badValuesId);
            errorList.add(KsfValidationError.constructInvalidURIMessage(VALUES, filler.getAtId(), JSON_LD_ID, badValuesId));
            utils.expectFatals(
                    KsfValidationError.constructOrphanedChildMessage(event.getAtId(), event.getParent())
            );
        }
        utils.expectErrors(errorList);
        utils.testInvalid((model.getTa2() ? "TA2" : "TA1") + " malformed event, entity, participant, filler, and relation @id URIs");
    }

    private void testIllegalTA1URIs(TestUtils utils, String illegalChar) {
        String badUri = FOOBAR.replace(FOOBAR.charAt(0), illegalChar.charAt(0));
        List<String> badUris = Arrays.asList(badUri);

        model = utils.startNewTest();
        SchemaEvent event = utils.getEvents().get(model.getTa2() ? 1 : 0); // created by startNewTest()
        SchemaEntity entity = utils.makeEntity();
        Relation relation = utils.makeRelation(event.getAtId(), entity.getAtId(), badUri);
        Participant participant = utils.addParticipant(event);
        entity.setWdNode(badUris);
        event.setWdNode(badUris);
        participant.setReference(badUris);
        participant.setWdNode(badUris);
        participant.setWdLabel(badUris);
        participant.setWdDescription(badUris);
        event.setReference(badUris);
        entity.setReference(badUris);
        relation.setReference(badUris);
        utils.expectErrors(
                KsfValidationError.constructInvalidURIMessage(EVENT, event.getAtId(), WD_NODE, badUri),
                KsfValidationError.constructInvalidURIMessage(ENTITY, entity.getAtId(), WD_NODE, badUri),
                KsfValidationError.constructInvalidURIMessage(EVENT, event.getAtId(), REFERENCE, badUri),
                KsfValidationError.constructInvalidURIMessage(ENTITY, entity.getAtId(), REFERENCE, badUri),
                KsfValidationError.constructInvalidURIMessage(RELATION, relation.getAtId(), REFERENCE, badUri),
                KsfValidationError.constructInvalidURIMessage(PARTICIPANT, participant.getAtId(), REFERENCE, badUri),
                KsfValidationError.constructInvalidURIMessage(PARTICIPANT, participant.getAtId(), WD_NODE, badUri),
                KsfValidationError.constructInvalidURIMessage(RELATION, relation.getAtId(), KairosSchemaFormatConstants.WD_NODE, badUri)
        );
        utils.testInvalid("TA1 malformed URIs");
    }

    private void testIllegalTA2URIs(TA2TestUtils utils, String illegalChar, boolean isTask2) {
        String badUri = FOOBAR.replace(FOOBAR.charAt(0), illegalChar.charAt(0));
        List<String> badUris = Arrays.asList(badUri);

        model = utils.startNewTest();
        SchemaEvent event = utils.getEvents().get(1); // created by startNewTest()
        SchemaEntity entity = utils.makeEntity();
        Participant participant = utils.addParticipant(event, entity.getAtId());
        Relation relation = utils.makeRelation(event.getAtId(), entity.getAtId(), "Q105123647");
        utils.instantiateEvent(event);
        Filler filler = utils.instantiateParticipant(participant);
        utils.instantiateRelation(relation);
        Temporal temporal = utils.makeTemporal(event);
        event.setTa2wdNode(badUris);
        event.setTa2wdLabel(badUris); // shouldn't produce an error
        SchemaEntity ta2entity = utils.getEntities().get(2); // no clean way to get the created TA2 entity
        ta2entity.setTa2wdNode(badUris);
        if (isTask2) {
            event.setProvenance(badUris);
            filler.setProvenance(badUris);
            temporal.setProvenance(badUris);
            event.setTemporal(Arrays.asList(temporal));
            relation.setRelationProvenance(badUris);
            relation.setRelationSubjectProv(badUris);
            relation.setRelationObjectProv(badUris);
            utils.expectErrors(
                    KsfValidationError.constructInvalidURIMessage(TEMPORAL, String.format("'%s'.%s[%d]", event.getAtId(), TEMPORAL, 0), PROVENANCE, badUri),
                    KsfValidationError.constructInvalidURIMessage(EVENT, event.getAtId(), TA2WD_NODE, badUri),
                    KsfValidationError.constructInvalidURIMessage(ENTITY, ta2entity.getAtId(), TA2WD_NODE, badUri),
                    KsfValidationError.constructInvalidURIMessage(EVENT, event.getAtId(), PROVENANCE, badUri),
                    KsfValidationError.constructInvalidURIMessage(VALUES, filler.getAtId(), PROVENANCE, badUri),
                    KsfValidationError.constructInvalidURIMessage(RELATION, relation.getAtId(), RELATION_PROVENANCE, badUri),
                    KsfValidationError.constructInvalidURIMessage(RELATION, relation.getAtId(), RELATION_SUBJECT_PROV, badUri),
                    KsfValidationError.constructInvalidURIMessage(RELATION, relation.getAtId(), RELATION_OBJECT_PROV, badUri)
            );
        } else {
            // Task 1 doesn't need to test provenance because they are already checked as part of validating that they
            // are valid references to other IDs
            utils.expectErrors(
                    KsfValidationError.constructInvalidURIMessage(EVENT, event.getAtId(), TA2WD_NODE, badUri),
                    KsfValidationError.constructInvalidURIMessage(ENTITY, ta2entity.getAtId(), TA2WD_NODE, badUri)
            );
        }
        utils.testInvalid((isTask2 ? "TA2 Task 2" : "TA2 Task 1") + " malformed URIs");
    }

    private void testMalformedIds(TestUtils utils, String violationType) {
        model = utils.startNewTest();
        SchemaEvent event = model.getTa2() ? utils.makeLeafEvent() : utils.getEvents().get(0); // TA1 event created by startNewTest()
        SchemaEntity entity = utils.makeEntity();
        Participant participant = utils.addParticipant(event);
        Relation relation = utils.makeRelation(event.getAtId(), entity.getAtId(), "Q105123647");
        String badDocumentId;
        String badEventId;
        String badEntityId;
        String badParticipantId;
        String badRelationId;
        switch (violationType) {
            case KE_TYPE_VIOLATION:
                badDocumentId = FOOBAR;
                badEventId = FOOBAR;
                badEntityId = FOOBAR;
                badParticipantId = FOOBAR;
                badRelationId = FOOBAR;
                break;
            case DIGIT_VIOLATION:
                badDocumentId = FOOBAR;
                badEventId = event.getAtId().replace(getDigits(event.getAtId()), getDigits(event.getAtId()).concat("Q"));
                badEntityId = entity.getAtId().replace(getDigits(entity.getAtId()), getDigits(entity.getAtId()).concat("Q"));
                badParticipantId = participant.getAtId().replace(getDigits(participant.getAtId()), getDigits(participant.getAtId()).concat("Q"));
                badRelationId = relation.getAtId().replace(getDigits(relation.getAtId()), getDigits(relation.getAtId()).concat("Q"));
                break;
            default: throw new IllegalArgumentException(violationType);
        }
        model.setAtId(badDocumentId);
        if (model.getTa2()) { // keep hierarchy as consistent as possible
            SchemaEvent rootEvent = utils.getEvents().get(0);
            rootEvent.getSubgroupEvents().remove(event.getAtId());
        }
        event.setAtId(badEventId);
        event.setPredictionProvenance(List.of(badEventId));
        entity.setAtId(badEntityId);
        participant.setAtId(badParticipantId);
        relation.setAtId(badRelationId);
        relation.setRelationSubject(badEventId);
        relation.setRelationObject(List.of(badEntityId));
        String instanceId = model.getTa2() ? model.getInstances().get(0).getAtId() : model.getAtId();
        if (model.getTa2()) {
            utils.expectWarnings(
                    KsfValidationError.constructPrimitivesAtRootMessage(instanceId, 2)
            );
            utils.expectFatals(
                    KsfValidationError.constructOrphanedChildMessage(event.getAtId(), event.getParent())
            );
        }
        utils.expectErrors(
                KsfValidationError.constructNamingViolationMessage(SUBMISSIONS, badDocumentId, JSON_LD_ID, badDocumentId, KE_TYPE_VIOLATION),
                KsfValidationError.constructNamingViolationMessage(EVENTS, instanceId, JSON_LD_ID, badEventId, violationType),
                KsfValidationError.constructNamingViolationMessage(ENTITIES, instanceId, JSON_LD_ID, badEntityId, violationType),
                KsfValidationError.constructNamingViolationMessage(PARTICIPANTS, instanceId, JSON_LD_ID, badParticipantId, violationType),
                KsfValidationError.constructNamingViolationMessage(RELATIONS, instanceId, JSON_LD_ID, badRelationId, violationType)
        );
        utils.testInvalid((model.getTa2() ? "TA2" : "TA1") + " malformed event, entity, participant, and relation IDs");
    }

    private void testMalformedTA2Ids(TA2TestUtils utils, String violationType) {
        model = utils.startNewTest();
        Instance instance = model.getInstances().get(0); // created by startNewTest
        SchemaEvent event = utils.instantiateEvent(utils.getEvents().get(1)); // created by startNewTest()
        Relation relation = utils.makeRelation(event.getAtId(), event.getAtId(), "Q105123647");
        Filler filler = utils.instantiateParticipant(utils.addParticipant(event));
        String badInstanceId;
        String badInstanceTa1ref;
        String badEventTa1ref;
        String badRelationTa1ref;
        String badValuesId;
        switch (violationType) {
            case KE_TYPE_VIOLATION:
                badInstanceId = FOOBAR;
                badInstanceTa1ref = FOOBAR;
                badEventTa1ref = FOOBAR;
                badRelationTa1ref = FOOBAR;
                badValuesId = FOOBAR;
                break;
            case DIGIT_VIOLATION:
                badInstanceId = instance.getAtId().replace(getDigits(instance.getAtId()), getDigits(instance.getAtId()).concat("Q"));
                badInstanceTa1ref = instance.getTa1ref().replace("SC", "SCQ");
                badEventTa1ref = event.getTa1ref().replace(getDigits(event.getTa1ref()), ("Q" + getDigits(event.getAtId())));
                badRelationTa1ref = relation.getTa1ref().replace(getDigits(relation.getTa1ref()), ("Q" + getDigits(relation.getAtId())));
                badValuesId = filler.getAtId().replace(getDigits(filler.getAtId()), getDigits(filler.getAtId()).concat("Q"));
                break;
            default: throw new IllegalArgumentException(violationType);
        }
        instance.setAtId(badInstanceId);
        instance.setTa1ref(badInstanceTa1ref);
        event.setTa1ref(badEventTa1ref);
        relation.setTa1ref(badRelationTa1ref);
        filler.setAtId(badValuesId);
        utils.expectFatals(
                KsfValidationError.constructNamingViolationMessage(FATAL, INSTANCES, instance.getAtId(), TA1_REF, badInstanceTa1ref, violationType)
                );
        utils.expectErrors(
                KsfValidationError.constructNamingViolationMessage(INSTANCES, model.getAtId(), JSON_LD_ID, badInstanceId, violationType),
                KsfValidationError.constructNamingViolationMessage(EVENTS, event.getAtId(), TA1_REF, badEventTa1ref, violationType),
                KsfValidationError.constructNamingViolationMessage(RELATIONS, relation.getAtId(), TA1_REF, badRelationTa1ref, violationType),
                KsfValidationError.constructNamingViolationMessage(VALUES, instance.getAtId(), JSON_LD_ID, badValuesId, violationType)
                );
        utils.testInvalid("Malformed TA2 @ids (and ta1refs)");
    }

    private void testMalformedProvenanceIds(TA2TestUtils utils, String violationType) {
        model = utils.startNewTest();
        SchemaEvent event = utils.instantiateEvent(utils.getEvents().get(1)); // created by startNewTest()
        Provenance provenance = model.getProvenanceData().get(1); // get(0) was created by startNewTest()
        String badProvenanceId;
        switch (violationType) {
            case KE_TYPE_VIOLATION:
                badProvenanceId = FOOBAR;
                break;
            case DIGIT_VIOLATION:
                badProvenanceId = provenance.getProvenanceID().replace(getDigits(provenance.getProvenanceID()),
                        getDigits(provenance.getProvenanceID()).concat("Q"));
                break;
            default: throw new IllegalArgumentException(violationType);
        }
        provenance.setProvenanceID(badProvenanceId);
        event.setProvenance(List.of(badProvenanceId));
        utils.expectErrors(
                KsfValidationError.constructNamingViolationMessage(PROVENANCE, model.getAtId(), PROVENANCE_ID, badProvenanceId, violationType
                )
        );
        utils.testInvalid("Malformed provenance IDs");
    }

    private void testDuplicateIds(TestUtils utils) {
        SchemaEvent event = utils.getEvents().get(model.getTa2() ? 1 : 0); // created by startNewTest()
        SchemaEvent event2 = utils.makeLeafEvent();
        if (model.getTa2()) {
            SchemaEvent rootEvent = utils.getEvents().get(0);
            rootEvent.getSubgroupEvents().remove(event2.getAtId());
        }
        event2.setAtId(event.getAtId());
        event2.setPredictionProvenance(List.of(event2.getAtId()));
        SchemaEntity entity = utils.makeEntity();
        SchemaEntity entity2 = utils.makeEntity();
        entity2.setAtId(entity.getAtId());
        Participant participant = utils.addParticipant(event);
        Participant participant2 = utils.addParticipant(event2);
        participant2.setAtId(participant.getAtId());
        Relation relation = utils.makeRelation(event.getAtId(), entity.getAtId(), "Q105123647");
        Relation relation2 = utils.makeRelation(event2.getAtId(), entity2.getAtId(), "Q105123647");
        relation2.setAtId(relation.getAtId());
        String instanceType = model.getTa2() ? INSTANCES : DOCUMENT;
        String instantiationId = model.getTa2() ? model.getInstances().get(0).getAtId() : model.getAtId();
        utils.expectFatals(
                KsfValidationError.constructDuplicateIdsMessage(ENTITY, instantiationId, instanceType, Set.of(getDigits(entity.getAtId()))),
                KsfValidationError.constructDuplicateIdsMessage(EVENTS, instantiationId, instanceType, Set.of(getDigits(event.getAtId()))),
                KsfValidationError.constructDuplicateIdsMessage(PARTICIPANTS, instantiationId, instanceType, Set.of(getDigits(participant.getAtId()))),
                KsfValidationError.constructDuplicateIdsMessage(RELATIONS, instantiationId, instanceType, Set.of(getDigits(relation.getAtId())))
        );
        utils.testInvalid((model.getTa2() ? "TA2" : "TA1") + " duplicate event, entity, participant, and relation IDs");
    }

    private void testDuplicateTA2Ids(TA2TestUtils utils) {
        Instance instance = model.getInstances().get(0);
        Instance instance2 = utils.makeInstance();
        instance2.setAtId(instance.getAtId());
        SchemaEvent event = utils.instantiateEvent(utils.getEvents().get(1)); // created by startNewTest()
        SchemaEvent event2 = utils.makeParentEvent("OR", event);
        Filler filler = utils.instantiateParticipant(utils.addParticipant(event));
        Filler filler2 = utils.instantiateParticipant(utils.addParticipant(event2));
        filler2.setAtId(filler.getAtId());
        utils.expectFatals(
                KsfValidationError.constructDuplicateIdsMessage(INSTANCES, model.getAtId(), DOCUMENT, Set.of(getDigits(instance.getAtId()))),
                KsfValidationError.constructDuplicateIdsMessage(VALUES, instance.getAtId(), INSTANCES, Set.of(getDigits(filler.getAtId())))
        );
        utils.testInvalid("Duplicate TA2 IDs");
    }

    private void testDuplicateProvenanceIds(TA2TestUtils utils) {
        Provenance prov1 = utils.makeTextProvenance();
        Provenance prov2 = utils.makeTextProvenance();
        prov2.setProvenanceID(prov1.getProvenanceID());
        utils.expectFatals(
                KsfValidationError.constructDuplicateIdsMessage(PROVENANCE_DATA, model.getAtId(), DOCUMENT,
                        Set.of(getDigits(prov1.getProvenanceID())))
        );
        utils.testInvalid("Duplicate provenance IDs");
    }

    // Note that this won't work on all performer IDs, but do work on all IDs generated by the TestUtils.
    private String getDigits(String id) {
        int lastSlashIndex = id.lastIndexOf('/');
        return id.substring(lastSlashIndex - UNIQUE_ID_NUMDIGITS, lastSlashIndex);
    }

} // KsfIdValidationTests
