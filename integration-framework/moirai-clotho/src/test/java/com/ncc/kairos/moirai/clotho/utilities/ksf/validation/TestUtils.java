package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import ch.qos.logback.classic.Logger;
import com.ncc.kairos.moirai.clotho.model.*;
import com.ncc.kairos.moirai.clotho.resources.ApplicationConstants;
import com.ncc.kairos.moirai.clotho.utilities.SDFViewer;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Utilities for testing KAIROS Clotho functionality and/or creating examples.
 * Features:
 * <p><ul>
 * <li>Contains methods for creating a valid SDF entity, event, relation, and provenance objects.;</li>
 * <li>IDs are numbered in a standard, sequential, simple way;</li>
 * <li>Contains methods for asserting test model validity and dumping models and validation reports.</li>
 * </ul></p>
 * Call {@link #startNewTest()} before each test to ensure a clean model.
 * @author Darren Gemoets, initially adapted from AIDA source code
 */
abstract class TestUtils {
    protected Logger logger;

    private final KsfModelValidation validator;
    private final boolean dumpAlways;
    private final boolean dumpToFile;

    private static final String DUMP_DIRECTORY = "test-dump-output";
    protected final Random rand = new Random();

    // Counters for the various elements tracked by the TestUtils
    private int entityCount;
    private int eventCount;
    private int relationCount;
    private int participantCount;

    // Data created by each test
    protected JsonLdRepresentation model;
    private final ArrayList<String> expectedFatals;
    private final ArrayList<String> expectedErrors;
    private final ArrayList<String> expectedWarnings;

    /**
     * Construct the abstract base class.
     * @param dumpAlways whether or not always to dump the model and validation report
     * @param dumpToFile whether or not to dump to a file
     */
    protected TestUtils(boolean dumpAlways, boolean dumpToFile) {
        this.validator = new KsfModelValidation();
        this.dumpAlways = dumpAlways;
        this.dumpToFile = dumpToFile;
        this.logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        this.expectedFatals = new ArrayList<>();
        this.expectedErrors = new ArrayList<>();
        this.expectedWarnings = new ArrayList<>();
    }

    /**
     * Call before each test.  Returns a new, valid model with standard TA-specific SDF metadata.
     * @return a new model with which to start a test
     */
    JsonLdRepresentation startNewTest() {
        model = new JsonLdRepresentation();
        model.setAtContext(ApplicationConstants.KAIROS_CONTEXT_STRING);
        model.setAtId(getPrefix() + SUBMISSIONS + "/");
        model.setSdfVersion(ApplicationConstants.SDF_VERSION_VALUE);
        model.setVersion("caci-v" + ApplicationConstants.SDF_VERSION_VALUE);
        entityCount = 0;
        eventCount = 0;
        relationCount = 0;
        participantCount = 0;
        expectedFatals.clear();
        expectedErrors.clear();
        expectedWarnings.clear();
        return model;
    }

    //
    // Utilities for creating IDs and returning lists of model objects in a TA-independent manner.
    //

    /**
     * Get an appropriate performer prefix.
     * @return a performer prefix
     */
    abstract String getPrefix();

    /**
     * Get the list of events from the model.
     * @return a {@link List} of {@link SchemaEvent}s
     */
    abstract List<SchemaEvent> getEvents();

    /**
     * Get the list of entities from the model.
     * @return a {@link List} of {@link SchemaEntity}s
     */
    abstract List<SchemaEntity> getEntities();

    /**
     * Get the list of top-level relations from the model.
     * @return a {@link List} of {@link Relation}s
     */
    abstract List<Relation> getTopLevelRelations();

    protected String getId(String KE_type, int count) {
        return getPrefix() + KE_type + "/" + (10000 + count) + "/";
    }

    /**
     * Returns a unique prefixed String @id for use with entities.
     */
    String getEntityId() {
        return getId(ENTITIES, ++entityCount);
    }

    /**
     * Returns a unique prefixed String @id for use with events.
     */
    String getEventId() {
        return getId(EVENTS, ++eventCount);
    }

    /**
     * Returns a unique prefixed String @id for use with relations.
     */
    String getRelationId() {
        return getId(RELATIONS, ++relationCount);
    }

    /**
     * Returns a unique prefixed String @id for use with participants.
     */
    String getParticipantId() {
        return getId(PARTICIPANTS, ++participantCount);
    }

    //
    // Utilities for creating model object for use in tests.
    //

    /**
     * Create a TA1 entity, adding it to the model.
     * @return a valid {@link SchemaEntity}
     */
    SchemaEntity makeEntity() {
        SchemaEntity entity = new SchemaEntity();
        entity.setAtId(getEntityId());
        entity.setName("entity name");
        ArrayList<String> wd_nodes = new ArrayList<>();
        wd_nodes.add("Q181600");
        entity.setWdNode(wd_nodes);
        ArrayList<String> wd_labels = new ArrayList<>();
        wd_labels.add("patient");
        entity.setWdLabel(wd_labels);
        ArrayList<String> wd_descriptions = new ArrayList<>();
        wd_descriptions.add("person who takes a medical treatment or is subject of a case study");
        entity.setWdDescription(wd_descriptions);
        this.getEntities().add(entity);
        return entity;
    }

    /**
     * Create a relation between two objects, adding it to the model.
     * @param subjectId a {@link String} id for the relationSubject
     * @param objectId a {@link String} id for the relationObject
     * @param relationPredicate a {@link String} qnode for the relationPredicate
     * @return a valid {@link Relation} between subject and object
     */
    Relation makeRelation(String subjectId, String objectId, String relationPredicate) {
        return makeRelation(subjectId, objectId, relationPredicate, getTopLevelRelations());
    }

    /**
     * Create a relation between two objects, adding it to the specified {@link SchemaEvent}.
     * @param subjectId a {@link String} id for the relationSubject
     * @param objectId a {@link String} id for the relationObject
     * @param relationPredicate a {@link String} qnode for the relationPredicate
     * @param event a {@link SchemaEvent} to which to add the relation
     * @return a valid {@link Relation} between subject and object, added to event
     */
    Relation makeRelation(String subjectId, String objectId, String relationPredicate, SchemaEvent event) {
        if (event.getRelations() == null) {
            event.setRelations(new ArrayList<>());
        }
        return makeRelation(subjectId, objectId, relationPredicate, event.getRelations());
    }

    // Creates the actual relation, adding it to the specified list of Relations
    private Relation makeRelation(String subjectId, String objectId, String relationPredicate, List<Relation> relationList) {
        Relation relation = new Relation();
        relation.setAtId(getRelationId());
        relation.setName("Relation name");
        relation.setRelationSubject(subjectId);
        ArrayList<String> objects = new ArrayList<>();
        objects.add(objectId);
        relation.setRelationObject(objects);
        ArrayList<String> wd_nodes = new ArrayList<>();
        wd_nodes.add(relationPredicate);
        relation.setWdNode(wd_nodes);
        ArrayList<String> wd_labels = new ArrayList<>();
        wd_labels.add("relation label");
        relation.setWdLabel(wd_labels);
        ArrayList<String> wd_descriptions = new ArrayList<>();
        wd_descriptions.add("relation description");
        relation.setWdDescription(wd_descriptions);
        relationList.add(relation);
        return relation;
    }

    /**
     * Create a parent event with the specified children and children_gate, adding it to the model.
     * @param gateType the logic gate to use with children_gate
     * @param children the child events to associate with the created parent event
     * @return a valid {@link SchemaEvent} with the specified children and children_gate
     */
    SchemaEvent makeParentEvent(String gateType, SchemaEvent... children) {
        return makeParentEvent(gateType, List.of(children));
    }

    /**
     * Create a parent event with the specified children and children_gate, adding it to the model.
     * @param gateType the logic gate to use with children_gate
     * @param children the child events to associate with the created parent event
     * @return a valid {@link SchemaEvent} with the specified children and children_gate
     */
    SchemaEvent makeParentEvent(String gateType, List<SchemaEvent> children) {
        SchemaEvent parent = new SchemaEvent();
        parent.setAtId(getEventId());
        parent.setName("parent event name");
        parent.setDescription("parent event description");
        parent.setChildrenGate(gateType.toLowerCase());
        this.getEvents().add(parent);
        attachChildrenToParent(parent, children); // TA-specific
        return parent;
    }

    abstract void attachChildrenToParent(SchemaEvent parent, List<SchemaEvent> children);

    /**
     * Create an event with no children, adding it to the model.
     * @return a valid {@link SchemaEvent} with no child events
     */
    SchemaEvent makeLeafEvent() {
        SchemaEvent event = new SchemaEvent();
        event.setAtId(getEventId());
        event.setName("event name");
        event.setDescription("event description");
        ArrayList<String> qnodes = new ArrayList<>();
        qnodes.add("Q60528603");
        event.setWdNode(qnodes);
        ArrayList<String> qlabels = new ArrayList<>();
        qlabels.add("contamination");
        event.setWdLabel(qlabels);
        ArrayList<String> qDescriptions = new ArrayList<>();
        qDescriptions.add("presence of an unwanted constituent, harmful substance or impurity in a material, physical body, or environment");
        event.setWdDescription(qDescriptions);
        addParticipant(event);
        this.getEvents().add(event);
        return event;
    }

    /**
     * Add a participant to the specified {@link SchemaEvent}, with the specified {@link String} entityId.
     * @param event the {@link SchemaEvent} to which to add the {@link Participant}
     * @param entityId the {@link String} entityId with which to associate the {@link Participant}
     * @return the {@link Participant} added to the specified {@link SchemaEvent}
     */
    Participant addParticipant(SchemaEvent event, String entityId) {
        Participant participant = new Participant();
        participant.setAtId(getParticipantId());
        int numParticipants = event.getParticipants() == null ? 0 : event.getParticipants().size();
        switch (numParticipants) {
            case 0:
                participant.setRoleName("A0-Agent");
                break;
            case 1:
                participant.setRoleName("A1-Recipient");
                break;
            case 2:
                participant.setRoleName("A2-Location");
                break;
            default:
                participant.setRoleName(String.format("A%d-Theme", numParticipants - 1));
        }
        participant.setEntity(entityId);
        if (rand.nextBoolean()) { // wd_nodes are optional in participants
            ArrayList<String> wd_nodes = new ArrayList<>();
            wd_nodes.add("Q24229398");
            participant.setWdNode(wd_nodes);
            ArrayList<String> wd_labels = new ArrayList<>();
            wd_labels.add("agent");
            participant.setWdLabel(wd_labels);
            ArrayList<String> wd_descriptions = new ArrayList<>();
            wd_descriptions.add("individual and identifiable entity capable of performing actions");
            participant.setWdDescription(wd_descriptions);
        }
        List<Participant> participantList = event.getParticipants();
        if (participantList == null) {
            participantList = new ArrayList<>();
            event.setParticipants(participantList);
        }
        participantList.add(participant);
        return participant;
    }

    /**
     * Add a participant to the specified {@link SchemaEvent}, creating an associated TA1 {@link SchemaEntity}.
     * @param event the {@link SchemaEvent} to which to add the {@link Participant}
     * @return the {@link Participant} added to the specified {@link SchemaEvent}
     */
    Participant addParticipant(SchemaEvent event) {
        SchemaEntity entity = makeEntity();
        return addParticipant(event, entity.getAtId());
    }

    //
    // Utilities for executing tests and collecting and reporting on results.
    //

    /**
     * Expect that the specified fatal errors will be returned by the next call to {@link #testValid(String)}.
     * @param fatalMsg a variable number of {@link String} fatal error messages
     */
    void expectFatals(String... fatalMsg) {
        expectedFatals.addAll(Arrays.asList(fatalMsg));
    }

    /**
     * Expect that the specified errors will be returned by the next call to {@link #testValid(String)}.
     * @param errorMsg a variable number of {@link String} error messages
     */
    void expectErrors(String... errorMsg) {
        expectedErrors.addAll(Arrays.asList(errorMsg));
    }

    /**
     * Expect that the specified errors will be returned by the next call to {@link #testValid(String)}.
     * @param errorMsgs a {@link List} of {@link String} error messages
     */
    void expectErrors(List<String> errorMsgs) {
        expectedErrors.addAll(errorMsgs);
    }

    /**
     * Expect that the specified warnings will be returned by the next call to {@link #testValid(String)}.
     * @param warnMsg a variable number of {@link String} warning messages
     */
    void expectWarnings(String... warnMsg) {
        expectedWarnings.addAll(Arrays.asList(warnMsg));
    }

    /**
     * Assert that the test with the specified description is valid based on the current model and validator.
     * @param testDescription a {@link String} description of the test
     */
    void testValid(String testDescription) {
        model.setComment(Collections.singletonList("Test name: " + testDescription));
        assertAndDump(testDescription, true);
    }

    /**
     * Assert that the test with the specified description is invalid based on the current model and validator,
     * with the specified number of expected warnings and errors.
     * @param testDescription a {@link String} description of the test
     * @param expectedWarnings the number of expected validation warnings
     * @param expectedErrors the number of expected validation errors
     */
    void testInvalid(String testDescription, int expectedWarnings, int expectedErrors) {
        List<String> actualViolations = assertAndDump(testDescription, false);
        // fail if number of expected warnings and errors doesn't match report
        int warningCount = getViolationCount(actualViolations, KsfValidator.WARNING);
        if (expectedWarnings != warningCount) {
            fail(String.format("Validation was expected to have %d warnings but had %d instead.", expectedWarnings, warningCount));
        }
        int errorCount = getViolationCount(actualViolations, KsfValidator.ERROR);
        if (expectedErrors != errorCount) {
            fail(String.format("Validation was expected to have %d errors but had %d instead.", expectedErrors, errorCount));
        }
    }

    // Note: This has the side effect of removing expected violations from list of actual violations.
    private void testViolations(List<String> expectedViolations, List<String> actualViolations) {
        HashSet<String> duplicateViolations = new HashSet<>();
        // Remove expected violations from report, and fail if any violations don't exist in report
        for (String violation : expectedViolations) {
            if (!actualViolations.contains(violation)) {
                if (duplicateViolations.contains(violation)) {
                    fail("Unable to find all cases of violation: [" + violation + "]");
                } else {
                    fail("Unable to find violation: [" + violation + "]");
                }
            } else { // in case a given violation is supposed to occur more than once
                actualViolations.remove(violation);
                duplicateViolations.add(violation);
            }
        }
    }

    /**
     * Assert that the test with the specified description is invalid based on the current model and validator,
     * with the expected warnings and errors defined via {@link #expectErrors(String...)}()} and
     * {@link #expectWarnings(String...)}()}.
     * @param testDescription a {@link String} description of the test
     */
    void testInvalid(String testDescription) {
        List<String> actualViolations = assertAndDump(testDescription, false);
        testViolations(expectedFatals, actualViolations);
        testViolations(expectedErrors, actualViolations);
        testViolations(expectedWarnings, actualViolations);

        // Then fail if there are any violations left
        if (!actualViolations.isEmpty()) {
            fail("Encountered unexpected violation [" + actualViolations.get(0) + "]");
        }
    }

    // Return the number of violations of the specified type
    private int getViolationCount(List<String> actualViolationsList, String violationType) {
        int retVal = 0;
        for (String curMsg : actualViolationsList) {
            if (curMsg.startsWith(violationType)) {
                retVal++;
            }
        }
        return retVal;
    }

    /**
     * Return calling method name from test class.
     * Looks at the calling stack for the calling test.
     * Converts class/method from something like:
     * <code>com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfTA1ValidationTests$ValidExamples</code> / <code>testPlaceholder</code>
     * <code>com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfTA2ValidationTests$Task2Examples$ValidExamples</code> / <code>testPlaceholder</code>
     * to:
     * KsfTA1ValidationTests_ValidExamples_testPlaceholder
     * KsfTA2ValidationTests_Task2Examples_ValidExamples_testPlaceholder
     */
    private String getCallingMethodName() {
        int steIndex = 0;
        StackTraceElement ste = Thread.currentThread().getStackTrace()[steIndex];

        // Traverse stack from the bottom until we get to methods in this class
        while (!ste.getClassName().contains("TestUtils")) {
            steIndex++;
            ste = Thread.currentThread().getStackTrace()[steIndex];
        }

        // Traverse stack until we get to the first method external to this class that calls a method in this class
        // This is done because this has been refactored several times and I don't want to assume which methods from this class call it
        while (ste.getClassName().contains("TestUtils")) {
            steIndex++;
            ste = Thread.currentThread().getStackTrace()[steIndex];
        }

        String[] pathList = ste.getClassName().split("\\.");
        StringBuilder classNameSB = new StringBuilder();
        if (pathList.length > 0) {
            // We don't want any of the package names in the final string, so just take the part of the string after the last "."
            // Inner classes are concatenated by "$", but it's better to use "_" rather than "$" in filenames.
            String[] nestedClassList = pathList[pathList.length - 1].split("\\$");
            for (int classIndex = 0; classIndex < nestedClassList.length; classIndex++) {
                classNameSB.append(nestedClassList[classIndex]);
                if (classIndex < nestedClassList.length - 1) {
                    classNameSB.append('_');
                }
            }
        }
        classNameSB.append('_').append(ste.getMethodName());
        return classNameSB.toString();
    }

    /**
     * Return path to file in dump directory. First check if directory exists, and create it if it doesn't
     * @param filename a {@link String} filename specification
     */
    private Path createDirectoryForPath(String filename) throws IOException {
        Path directory = Paths.get("build", "reports", DUMP_DIRECTORY);
        Files.createDirectories(directory);
        return Paths.get("build", "reports", DUMP_DIRECTORY, filename);
    }

    /**
     * This method writes the specified model to a file ({@link #getCallingMethodName()}.
     * if {@link #dumpToFile} is true, otherwise writes to System.out
     * @param testDescription {@link String} containing a description of the test
     */
    private void dumpModel(String testDescription) {
        String report = new SDFViewer(model, true).getOutput();

        if (dumpToFile) {
            String outputFilename = getCallingMethodName() + ".txt";
            try {
                Path path = createDirectoryForPath(outputFilename);
                logger.info("Dump to " + path);
                // Dump model to file impl
                Files.writeString(path, report);
            } catch (IOException ioe) {
                logger.error("---> Could not dump model to " + outputFilename);
            }
        } else {
            System.out.println("\n----------------------------------------------\n" + testDescription + "\n\nJSON Model:");
            System.out.println(report);
        }
    }

    /**
     * This method dumps the validation report either to stdout or to a file.
     * @param report {@link List} of validation errors and warnings
     */
    private void dumpReport(List<String> report) {
        if (dumpToFile) {
            String outputFilename = getCallingMethodName() + ".txt";
            try {
                Path path = createDirectoryForPath(outputFilename);
                logger.info("Dump to " + path);
                // Dump report to file impl
                Files.writeString(path, report.toString());
            } catch (IOException ioe) {
                logger.error("---> Could not dump model to " + outputFilename);
            }
        } else {
            System.out.println("\n" + "Failure:");
            report.forEach(System.out::println);
        }
    }

    /**
     * This method will validate the model using the provided validator and will dump the model if
     * either the validation result is unexpected or if the model is valid and forceDump is true
     * Thus, forceDump can be used to write all the valid examples to console.
     * @param testDescription {@link String} containing the description of the test
     * @param expected true if validation is expected to pass, false otherwise
     * @return a {@link List} containing error and warning messages from the validation
     */
    private List<String> assertAndDump(String testDescription, boolean expected) {
        validator.setModel(model);
        final List<String> actualViolations = validator.validate();
        final boolean valid = actualViolations.isEmpty();
        final boolean unexpected = valid != expected;

        // dump model if result is unexpected or if forced
        if (dumpAlways || unexpected) {
            dumpModel(testDescription);
            // dump report if it should dump AND report is invalid
            if (!valid) {
                dumpReport(actualViolations);
            }
        }

        // fail if result is unexpected
        if (unexpected) {
            fail("Validation was expected to " + (expected ? "pass" : "fail") + " but did not");
        }

        return actualViolations;
    }
}
