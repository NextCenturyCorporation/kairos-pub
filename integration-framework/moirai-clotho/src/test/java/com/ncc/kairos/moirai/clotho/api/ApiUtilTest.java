package com.ncc.kairos.moirai.clotho.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncc.kairos.moirai.clotho.model.*;
import com.ncc.kairos.moirai.clotho.resources.ApplicationConstants;
import com.ncc.kairos.moirai.clotho.resources.ClothoTestConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SpringBootTest
@TestPropertySource(locations = "classpath:test.properties")
class ApiUtilTest {

    private Map<String, Object> requestBody;

    @BeforeEach
    void setUp() throws Exception {
        String jsonFileText = Files.readString(Path.of(this.getClass().getResource(ClothoTestConstants.LEGIONNAIRES_DISEASE_TA2_FILE).toURI()));
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        requestBody = mapper.reader().forType(Map.class).readValue(jsonFileText);
    }

    @AfterEach
    void tearDown() {
        requestBody = null;
    }

    @Test
    void testAssertCorrectContext() {
        try {
            Object context = requestBody.get(JSON_LD_CONTEXT);
            assertNotNull(context);
            ApiUtil.assertCorrectContext(context);
        } catch (Exception ex) {
            fail("Exception was not expected: " + ex.getMessage() + ".");
        }
    }

    @Test
    void testCheckUnknownProperties() {
        // 1. Test that the default example has no unknown or misplaced properties.
        assertNull(ApiUtil.checkUnknownProperties(requestBody));

        // 2. Test that introducing an unknown property is flagged.
        requestBody.put("unknownProperty", "foobar");
        assertNotNull(ApiUtil.checkUnknownProperties(requestBody));
        requestBody.remove("unknownProperty");

        // 3. Test that introducing a known property in an unexpected place is flagged.
        requestBody.put(MIN_DURATION, "foobar");
        assertNotNull(ApiUtil.checkUnknownProperties(requestBody));
    }

    @Test
    void testDeserializeJsonLdRepresentation() {
        JsonLdRepresentation deserializedKsfRequest = ApiUtil.deserializeCompactJsonLd(requestBody);
        if (deserializedKsfRequest.getParsingErrors() != null) {
            fail(deserializedKsfRequest.getParsingErrors().get(0));
        }

        List<Instance> instances = deserializedKsfRequest.getInstances();
        assertEquals(2, instances.size());
        Instance instance = instances.get(0);
        // Hack to find the right instance, as they are unordered
        if (instance.getConfidence().get(0) < 0.8) {
            instance = instances.get(1);
        }
        List<SchemaEvent> events = instances.get(0).getEvents();
        assertEquals(32, events.size());
        List<Participant> participants = events.get(4).getParticipants();
        assertEquals(2, participants.size());
        assertEquals("A1-Patient", participants.get(1).getRoleName());
        List<SchemaEntity> entities = instance.getEntities();
        assertEquals(25, entities.size());
        List<Relation> relations = instance.getRelations();
        assertEquals(6, relations.size());
        List<Provenance> provenanceData = deserializedKsfRequest.getProvenanceData();
        assertEquals(7, provenanceData.size());
        assertEquals(CACI_TA2_PREFIX + "Submissions/TA2/12345", deserializedKsfRequest.getAtId());
        assertEquals(ApplicationConstants.SDF_VERSION_VALUE, deserializedKsfRequest.getSdfVersion());
        assertEquals(false, deserializedKsfRequest.getTask2());
        assertEquals("ce1002", deserializedKsfRequest.getCeID());
    }

}
