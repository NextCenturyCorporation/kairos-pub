package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncc.kairos.moirai.clotho.ClothoApplication;
import com.ncc.kairos.moirai.clotho.api.ApiUtil;
import com.ncc.kairos.moirai.clotho.model.JsonLdRepresentation;
import com.ncc.kairos.moirai.clotho.resources.ClothoTestConstants;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = ClothoApplication.class)
@TestPropertySource(locations = "classpath:test.properties")
class KsfExampleValidationTests {

    private static final String SNIPPET_DIRECTORY = "/test-snippets";
    private static final String SNIPPET_ERROR_SENTINEL = "expected errors: ";
    private static final String SNIPPET_WARNING_SENTINEL = "expected warnings: ";
    private static KsfValidator validator;

    void initTest(String filename) {
        try {
            initTest(Path.of(Objects.requireNonNull(this.getClass().getResource(filename)).toURI()));
        } catch (Exception e) {
            Assertions.fail("Initialization", e);
        }
    }

    // Initialize validation objects based on JSON-LD in supplied filename; fail if any errors
    private ImmutablePair<Integer, Integer> initTest(Path filePath) {
        JsonLdRepresentation jlo = null;
        try {
            String jsonFileText = Files.readString(filePath);
            // This relies on ApiUtil class to properly prepare the test objects
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Map<String, Object> requestObject = mapper.reader().forType(Map.class).readValue(jsonFileText);
            jlo = ApiUtil.deserializeCompactJsonLd(requestObject);
            validator.setModel(jlo);
            KsfProvenanceData.setPassThroughMode(true); // don't check to see if provenance is valid for the dataset
        } catch (Exception e) {
            Assertions.fail("Initialization", e);
        }
        return getInfractions(jlo);
    }

    private ImmutablePair<Integer, Integer> getInfractions(JsonLdRepresentation jlo) {
        int expectedWarnings = 0;
        int expectedErrors = 0;
        List<String> comments = ValidationUtils.safeGetStrings(jlo.getComment());
        for (String comment: comments) {
            int index = comment.toLowerCase().indexOf(SNIPPET_ERROR_SENTINEL);
            if (index > -1) {
                expectedErrors = Integer.parseUnsignedInt(comment, index + SNIPPET_ERROR_SENTINEL.length(), comment.length(), 10);
            }
            index = comment.toLowerCase().indexOf(SNIPPET_WARNING_SENTINEL);
            if (index > -1) {
                expectedWarnings = Integer.parseUnsignedInt(comment, index + SNIPPET_WARNING_SENTINEL.length(), comment.length(), 10);
            }
        }
        return new ImmutablePair<>(expectedErrors, expectedWarnings);
    }

    @BeforeAll
    static void initTests() {
        validator = new KsfModelValidation();
    }

    @Test
    void testValidateTA1KsfRequest() {
        initTest(ClothoTestConstants.DISEASE_OUTBREAK_TA1_FILE);
        assertAndDump("testValidateTA1KsfRequest", 0, 1);
    }

    @Test
    void testValidateTA2KsfRequest() {
        initTest(ClothoTestConstants.LEGIONNAIRES_DISEASE_TA2_FILE);
        assertAndDump("testValidateTA2KsfRequest", 0, 3);
    }

    @Test
    void testValidateTA2KsfRequest2() {
        initTest(ClothoTestConstants.IED_TA2_FILE);
        assertAndDump("testValidateTA2KsfRequest2", 0, 1);
    }

    @Test
    @Disabled("We don't have a current example of Task 2 TA2 output to test.")
    void testValidateTask2TA2KsfRequest() {
        initTest(ClothoTestConstants.TASK2_TA2_FILE);
        assertAndDump("testValidateTask2TA2KsfRequest", 0, 0);
    }

    @Test
    void testValidateGraphGKsfRequest() {
        initTest(ClothoTestConstants.TASK2_GRAPH_G_FILE);
        assertAndDump("testValidateGraphGKsfRequest", 0, 2);
    }

    @Test
    void testSnippets() {
        // Validate all JSON files in the test-snippets subdirectory
        try {
            DirectoryStream.Filter<Path> filter = file -> file.toString().endsWith(".json");
            var dirPath = Path.of(Objects.requireNonNull(this.getClass().getResource(SNIPPET_DIRECTORY)).toURI());
            var paths = Files.newDirectoryStream(dirPath, filter);
            paths.forEach(file -> {
                ImmutablePair<Integer, Integer> expectedInfractions = initTest(file);
                List<String> errorsAndWarningsList = validator.validate();
                testInfractions(file.toString(), errorsAndWarningsList, expectedInfractions.getLeft(), KsfValidator.ERROR);
                testInfractions(file.toString(), errorsAndWarningsList, expectedInfractions.getRight(), KsfValidator.WARNING);
            });
        } catch (Exception e) {
            Assertions.fail("testSnippets execution", e);
        }
    }

    private void assertAndDump(String testDescription, int expectedErrors, int expectedWarnings) {
        List<String> errorsAndWarningsList = validator.validate();
        int numFatals = getInfractionCount(errorsAndWarningsList, KsfValidator.FATAL);
        int numErrors = getInfractionCount(errorsAndWarningsList, KsfValidator.ERROR);
        int numWarnings = getInfractionCount(errorsAndWarningsList, KsfValidator.WARNING);
        if (numFatals > 0 || numErrors != expectedErrors || numWarnings != expectedWarnings) {
            System.out.println("*** Failed test: " + testDescription);
            errorsAndWarningsList.forEach(System.out::println);
        }
        assertEquals(0, numFatals);
        assertEquals(expectedErrors, numErrors);
        assertEquals(expectedWarnings, numWarnings);
    }

    private void testInfractions(String filename, List<String> infractionList, int expectedInfractions, String infractionType) {
        int infractionCount = getInfractionCount(infractionList, infractionType);
        if (infractionCount != expectedInfractions) {
            infractionList.forEach(System.out::println);
            Assertions.fail(String.format("%s count %d does not match expected count %d in snippet '%s'.",
                    infractionType, infractionCount, expectedInfractions, filename));
        }
    }

    // Return number of specified infraction type
    private int getInfractionCount(List<String> errorsAndWarningsList, String infractionType) {
        return (int) errorsAndWarningsList.stream().filter(msg -> msg.startsWith(infractionType)).count();
    }

}
