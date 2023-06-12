package com.ncc.kairos.moirai.clotho.services;

import com.ncc.kairos.moirai.clotho.exceptions.GraphException;
import com.ncc.kairos.moirai.clotho.interfaces.IGremlinDriver;
import com.ncc.kairos.moirai.clotho.model.*;
import com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants;
import com.ncc.kairos.moirai.clotho.resources.DefinitionConstants;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.ncc.kairos.moirai.clotho.resources.GremlinTestConstants.*;
import static org.mockito.Mockito.*;

@Disabled
class DefinitionServiceTest extends DefinitionService {

    @Override
    public void open() throws GraphException {
        // Mock the Gremlin Driver instance
        driver = mock(IGremlinDriver.class);

        // Initialize arrays to be used for
        addedVertexIds = new ArrayList<>();
        addedEdgeIds = new ArrayList<>();
        errors = new ArrayList<>();
    }

    @BeforeEach
    void setUp() throws Exception {
        this.open();
    }

    @AfterEach
    void tearDown() {
        this.close();
    }

    @Test void testAddEntity() throws Exception {
        // Input for Mock getVertices()
        Map<String, String> perSearch = new HashMap<>();
        perSearch.put(NAME, "PER");
        Map<String, String> policeSearch = new HashMap<>();
        policeSearch.put(NAME, "PER:Police");

        Map<String, String> policeProps = new HashMap<>();
        policeProps.put(NAME, "PER:Police");
        policeProps.put(SUBTYPE_OF, "PER");
        policeProps.put(GENERIC_MAP_KEY, GENERIC_MAP_VAL);

        // Output for Mock getVertices
        Map<String, String> perResultMap = new HashMap<>();
        perResultMap.put(ID, "10");
        perResultMap.put(LABEL, ENTITY);
        perResultMap.put(NAME, "PER");
        List<Map<String, String>> resultList = new ArrayList<>();
        resultList.add(perResultMap);

        Map<String, String> policeResult = new HashMap<>();
        policeResult.put(ID, "20");
        policeResult.put(LABEL, ENTITY);
        policeResult.put(NAME, "PER:Police");
        policeResult.put(SUBTYPE_OF, "PER");
        policeResult.put(GENERIC_MAP_KEY, GENERIC_MAP_VAL);

        Map<String, String> edgeResult = new HashMap<>();
        edgeResult.put(ID, "99");
        edgeResult.put(LABEL, SUBTYPE_OF_EDGE_LABEL);
        edgeResult.put(FROM_VERTEX_ID, "20");
        edgeResult.put(TO_VERTEX_ID, "10");

        // Define mock implementation for driver
        when(driver.getVertices(ENTITY, perSearch)).thenReturn(resultList);
        when(driver.getVertices(ENTITY, policeSearch)).thenReturn(new ArrayList<>());

        when(driver.addVertex(ENTITY, policeProps)).thenReturn(policeResult);
        when(driver.addEdgeToVertex(SUBTYPE_OF_EDGE_LABEL, "20", "10", new HashMap<>())).
                thenReturn(edgeResult);

        Entity entityToAdd = new Entity();
        entityToAdd.setName("PER:Police");

        Entity resultEntity = addEntity(entityToAdd);
        assertEquals("PER:Police", resultEntity.getName());
    }

    @Test void testGetEntities() throws Exception {
        // Input for Mock getVertices()
        Map<String, String> perSearch = new HashMap<>();
        perSearch.put(NAME, "PER");

        // Output for Mock getVertices
        Map<String, String> perResultMap = new HashMap<>();
        perResultMap.put(ID, "10");
        perResultMap.put(LABEL, ENTITY);
        perResultMap.put(NAME, "PER");
        List<Map<String, String>> resultList = new ArrayList<>();
        resultList.add(perResultMap);

        // Define mock implementation for driver.getVertices
        when(driver.getVertices(ENTITY, perSearch)).thenReturn(resultList);

        List<Entity> resultEntities = getEntities(perSearch);
        assertEquals(1, resultEntities.size());
        assertEquals("PER", resultEntities.get(0).getName());
        assertEquals("10", resultEntities.get(0).getAtId());
    }

    @Test void testAddEvent() throws Exception {
        // Input for Mock getVertices()
        Map<String, String> attackSearch = new HashMap<>();
        attackSearch.put(NAME, "attack");
        Map<String, String> bombingSearch = new HashMap<>();
        bombingSearch.put(NAME, "bombing");
        Map<String, String> perFillerSearch = new HashMap<>();
        perFillerSearch.put(NAME, "PER");

        // Input for event vertex to insert
        Map<String, String> bombingProps = new HashMap<>();
        bombingProps.put(NAME, "bombing");
        bombingProps.put(KairosSchemaFormatConstants.KAIROS_CONTEXT_IRI + NAME, "bombing");
        bombingProps.put(SUBTYPE_OF, "attack");
        bombingProps.put(LIKELINESS, "very-likely");
        bombingProps.put(GENERIC_MAP_KEY, GENERIC_MAP_VAL);

        // Input for role vertex to insert
        Map<String, String> roleProps = new HashMap<>();
        roleProps.put(PATTERN, "?p");
        roleProps.put(NAME, "bomber");
        roleProps.put(FILLED_BY, "[PER]");

        // Output for Mock getVertices
        Map<String, String> attackResultMap = new HashMap<>();
        attackResultMap.put(ID, "10");
        attackResultMap.put(LABEL, EVENT);
        attackResultMap.put(NAME, "attack");
        List<Map<String, String>> resultList = new ArrayList<>();
        resultList.add(attackResultMap);

        Map<String, String> bombingResult = new HashMap<>();
        bombingResult.put(ID, "20");
        bombingResult.put(LABEL, EVENT);
        bombingResult.put(NAME, "bombing");
        bombingResult.put(SUBTYPE_OF, "attack");
        bombingResult.put(LIKELINESS, "very-likely");
        bombingResult.put(GENERIC_MAP_KEY, GENERIC_MAP_VAL);

        Map<String, String> perFillerResult = new HashMap<>();
        perFillerResult.put(ID, "5");
        perFillerResult.put(LABEL, ENTITY);
        perFillerResult.put(NAME, "PER");
        List<Map<String, String>> perFillerResultList = new ArrayList<>();
        perFillerResultList.add(perFillerResult);

        Map<String, String> roleResult = new HashMap<>();
        roleResult.put(ID, "30");
        roleResult.put(PATTERN, "?p");
        roleResult.put(NAME, "bomber");
        roleResult.put(FILLED_BY, "[PER]");

        // Output for Mock addEdge
        Map<String, String> parentEventEdge = new HashMap<>();
        parentEventEdge.put(ID, "99");
        parentEventEdge.put(LABEL, SUBTYPE_OF_EDGE_LABEL);
        parentEventEdge.put(FROM_VERTEX_ID, "20");
        parentEventEdge.put(TO_VERTEX_ID, "10");

        Map<String, String> roleEdge = new HashMap<>();
        roleEdge.put(ID, "101");
        roleEdge.put(LABEL, ROLE_EDGE_LABEL);
        roleEdge.put(FROM_VERTEX_ID, "20");
        roleEdge.put(TO_VERTEX_ID, "30");

        Map<String, String> referenceEntityEdge = new HashMap<>();
        referenceEntityEdge.put(ID, "105");
        referenceEntityEdge.put(LABEL, REFERENCES);
        referenceEntityEdge.put(FROM_VERTEX_ID, "30");
        referenceEntityEdge.put(TO_VERTEX_ID, "5");

        // Define mock implementations for driver
        when(driver.getVertices(EVENT, attackSearch)).thenReturn(resultList);
        when(driver.getVertices(EVENT, bombingSearch)).thenReturn(new ArrayList<>());
        when(driver.getVertices(null, perFillerSearch)).thenReturn(perFillerResultList);

        when(driver.addVertex(EVENT, bombingProps)).thenReturn(bombingResult);
        when(driver.addVertex(ROLE, roleProps)).thenReturn(roleResult);
        when(driver.addEdgeToVertex(SUBTYPE_OF_EDGE_LABEL, "20", "10", new HashMap<>())).
                thenReturn(parentEventEdge);
        when(driver.addEdgeToVertex(ROLE_EDGE_LABEL, "20", "30", new HashMap<>())).
                thenReturn(roleEdge);
        when(driver.addEdgeToVertex(REFERENCES, "30", "5", new HashMap<>())).
                thenReturn(referenceEntityEdge);

        Event eventToAdd = new Event();
        eventToAdd.setName("bombing");

        Event resultEvent = addEvent(eventToAdd);
        assertEquals("20", resultEvent.getAtId());
        assertEquals("bombing", resultEvent.getName());
    }

    @Test void testGetEvents() throws Exception {
        // Input for getVertices()
        Map<String, String> eventSearch = new HashMap<>();
        eventSearch.put(NAME, "bombing");

        // Output for Mock getVertices
        Map<String, String> perResultMap = new HashMap<>();
        perResultMap.put(ID, "10");
        perResultMap.put(LABEL, ENTITY);
        perResultMap.put(NAME, "PER");

        Map<String, String> eventResultMap = new HashMap<>();
        eventResultMap.put(ID, "100");
        eventResultMap.put(LABEL, EVENT);
        eventResultMap.put(NAME, "bombing");
        eventResultMap.put(SUBTYPE_OF, "attack");
        List<Map<String, String>> eventResultList = new ArrayList<>();
        eventResultList.add(eventResultMap);

        // Output for Mock getOutgoingPaths
        Map<String, String> roleResultMap = new HashMap<>();
        roleResultMap.put(ID, "300");
        roleResultMap.put(LABEL, ROLE);
        roleResultMap.put(NAME, "bomber");
        roleResultMap.put(PATTERN, "?p");
        roleResultMap.put(FILLED_BY, "[PER]");

        List<Map<String, String>> pathOne = new ArrayList<>();
        pathOne.add(eventResultMap);
        pathOne.add(roleResultMap);

        List<List<Map<String, String>>> pathsList = new ArrayList<>();
        pathsList.add(pathOne);

        when(driver.getVertices(EVENT, eventSearch)).thenReturn(eventResultList);
        when(driver.getOutgoingPaths("100", -1)).thenReturn(pathsList);

        List<Event> eventsResultList = getEvents(eventSearch);
        assertEquals(1, eventResultList.size());

        Event retrievedEvent = eventsResultList.get(0);
        assertEquals("100", retrievedEvent.getAtId());
        //assertEquals(retrievedEvent.getSubtypeOfName(), "attack");
        //assertEquals(retrievedEvent.getRoles().size(), 1);
    }

    @Test void testAddSchema() throws Exception {
        // Input for Mock getVertices()
        Map<String, String> raidSearch = new HashMap<>();
        raidSearch.put(NAME, "raid");
        Map<String, String> bombRaidSearch = new HashMap<>();
        bombRaidSearch.put(NAME, "bomb-raid");
        Map<String, String> perFillerSearch = new HashMap<>();
        perFillerSearch.put(NAME, "PER");
        Map<String, String> bombingSearch = new HashMap<>();
        bombingSearch.put(NAME, "bombing");
        Map<String, String> policeRaidSearch = new HashMap<>();
        policeRaidSearch.put(NAME, "police-raid");

        // Input for schema vertex to insert
        Map<String, String> bombRaidProps = new HashMap<>();
        //bombRaidProps.put(NAME, "bomb-raid");
        bombRaidProps.put(SUBTYPE_OF, "raid");
        bombRaidProps.put(STEPS_ORDER, "in-sequence");
        bombRaidProps.put(GENERIC_MAP_KEY, GENERIC_MAP_VAL);
        bombRaidProps.put(KairosSchemaFormatConstants.KAIROS_CONTEXT_IRI + DefinitionConstants.NAME, "bomb-raid");

        // Input for role vertex to insert
        Map<String, String> roleProps = new HashMap<>();
        roleProps.put(PATTERN, "?p");
        roleProps.put(NAME, "bomber");
        roleProps.put(FILLED_BY, "[PER]");

        // Input for step vertices to insert
        Map<String, String> stepOneProps = new HashMap<>();
        stepOneProps.put(EVENT, "bombing");
        stepOneProps.put(OPTIONAL, "false");
        stepOneProps.put(REPEATS, "1+");

        Map<String, String> stepTwoProps = new HashMap<>();
        stepTwoProps.put(EVENT, "police-raid");
        stepTwoProps.put(OPTIONAL, "false");
        stepTwoProps.put(REPEATS, "1+");

        // Output for Mock getVertices
        Map<String, String> raidSearchResult = new HashMap<>();
        raidSearchResult.put(ID, "10");
        raidSearchResult.put(LABEL, SCHEMA);
        raidSearchResult.put(NAME, "raid");
        List<Map<String, String>> raidSearchResultList = new ArrayList<>();
        raidSearchResultList.add(raidSearchResult);

        // Output for addRole and addSteps; verify referenced items exist
        Map<String, String> perFillerResult = new HashMap<>();
        perFillerResult.put(ID, "5");
        perFillerResult.put(LABEL, ENTITY);
        perFillerResult.put(NAME, "PER");
        List<Map<String, String>> perFillerResultList = new ArrayList<>();
        perFillerResultList.add(perFillerResult);

        Map<String, String> bombingResult = new HashMap<>();
        bombingResult.put(ID, "44");
        bombingResult.put(LABEL, EVENT);
        bombingResult.put(NAME, "bombing");
        List<Map<String, String>> bombingResultList = new ArrayList<>();
        bombingResultList.add(bombingResult);

        Map<String, String> policeRaidResult = new HashMap<>();
        policeRaidResult.put(ID, "45");
        policeRaidResult.put(LABEL, EVENT);
        policeRaidResult.put(NAME, "police-raid");
        List<Map<String, String>> policeRaidResultList = new ArrayList<>();
        policeRaidResultList.add(policeRaidResult);

        // Output for Mock addVertex
        Map<String, String> bombRaidResult = new HashMap<>();
        bombRaidResult.put(ID, "20");
        bombRaidResult.put(LABEL, SCHEMA);
        bombRaidResult.put(NAME, "bomb-raid");
        bombRaidResult.put(SUBTYPE_OF, "raid");
        bombRaidResult.put(STEPS_ORDER, "in-sequence");
        bombRaidResult.put(GENERIC_MAP_KEY, GENERIC_MAP_VAL);

        Map<String, String> roleResult = new HashMap<>();
        roleResult.put(ID, "30");
        roleResult.put(PATTERN, "?p");
        roleResult.put(NAME, "bomber");
        roleResult.put(FILLED_BY, "[PER]");

        Map<String, String> stepOneResult = new HashMap<>();
        stepOneResult.put(ID, "66");
        stepOneResult.put(EVENT, "bombing");
        stepOneResult.put(OPTIONAL, "false");
        stepOneResult.put(REPEATS, "1+");

        Map<String, String> stepTwoResult = new HashMap<>();
        stepTwoResult.put(ID, "67");
        stepTwoResult.put(EVENT, "police-raid");
        stepTwoResult.put(OPTIONAL, "false");
        stepTwoResult.put(REPEATS, "1+");

        // Output for Mock addEdge
        Map<String, String> parentSchemaEdge = new HashMap<>();
        parentSchemaEdge.put(ID, "99");
        parentSchemaEdge.put(LABEL, SUBTYPE_OF_EDGE_LABEL);
        parentSchemaEdge.put(FROM_VERTEX_ID, "20");
        parentSchemaEdge.put(TO_VERTEX_ID, "10");

        Map<String, String> roleEdge = new HashMap<>();
        roleEdge.put(ID, "101");
        roleEdge.put(LABEL, ROLE_EDGE_LABEL);
        roleEdge.put(FROM_VERTEX_ID, "20");
        roleEdge.put(TO_VERTEX_ID, "30");

        Map<String, String> referenceEntityEdge = new HashMap<>();
        referenceEntityEdge.put(ID, "105");
        referenceEntityEdge.put(LABEL, REFERENCES);
        referenceEntityEdge.put(FROM_VERTEX_ID, "30");
        referenceEntityEdge.put(TO_VERTEX_ID, "5");

        Map<String, String> schemaToStepOneEdge = new HashMap<>();
        schemaToStepOneEdge.put(ID, "201");
        schemaToStepOneEdge.put(LABEL, STEP_EDGE_LABEL);
        schemaToStepOneEdge.put(FROM_VERTEX_ID, "20");
        schemaToStepOneEdge.put(TO_VERTEX_ID, "66");

        Map<String, String> schemaToStepTwoEdge = new HashMap<>();
        schemaToStepTwoEdge.put(ID, "202");
        schemaToStepTwoEdge.put(LABEL, STEP_EDGE_LABEL);
        schemaToStepTwoEdge.put(FROM_VERTEX_ID, "20");
        schemaToStepTwoEdge.put(TO_VERTEX_ID, "67");

        Map<String, String> stepOneToTwoEdge = new HashMap<>();
        stepOneToTwoEdge.put(ID, "203");
        stepOneToTwoEdge.put(LABEL, STEP_PRECEDES);
        stepOneToTwoEdge.put(FROM_VERTEX_ID, "66");
        stepOneToTwoEdge.put(TO_VERTEX_ID, "67");

        Map<String, String> stepOneToEventEdge = new HashMap<>();
        stepOneToEventEdge.put(ID, "51");
        stepOneToEventEdge.put(LABEL, REFERENCES);
        stepOneToEventEdge.put(FROM_VERTEX_ID, "66");
        stepOneToEventEdge.put(TO_VERTEX_ID, "44");

        Map<String, String> stepTwoToEventEdge = new HashMap<>();
        stepTwoToEventEdge.put(ID, "52");
        stepTwoToEventEdge.put(LABEL, REFERENCES);
        stepTwoToEventEdge.put(FROM_VERTEX_ID, "67");
        stepTwoToEventEdge.put(TO_VERTEX_ID, "45");

        // Define mock implementations for driver
        when(driver.getVertices(SCHEMA, raidSearch)).thenReturn(raidSearchResultList);
        when(driver.getVertices(SCHEMA, bombRaidSearch)).thenReturn(new ArrayList<>());
        when(driver.getVertices(null, perFillerSearch)).thenReturn(perFillerResultList);
        when(driver.getVertices(null, bombingSearch)).thenReturn(bombingResultList);
        when(driver.getVertices(null, policeRaidSearch)).thenReturn(policeRaidResultList);

        when(driver.addVertex(SCHEMA, bombRaidProps)).thenReturn(bombRaidResult);
        when(driver.addVertex(ROLE, roleProps)).thenReturn(roleResult);
        when(driver.addVertex(STEP, stepOneProps)).thenReturn(stepOneResult);
        when(driver.addVertex(STEP, stepTwoProps)).thenReturn(stepTwoResult);
        when(driver.addEdgeToVertex(SUBTYPE_OF_EDGE_LABEL, "20", "10", new HashMap<>())).
                thenReturn(parentSchemaEdge);
        when(driver.addEdgeToVertex(ROLE_EDGE_LABEL, "20", "30", new HashMap<>())).
                thenReturn(roleEdge);
        when(driver.addEdgeToVertex(REFERENCES, "30", "5", new HashMap<>())).
                thenReturn(referenceEntityEdge);
        when(driver.addEdgeToVertex(STEP_EDGE_LABEL, "20", "66", new HashMap<>())).
                thenReturn(schemaToStepOneEdge);
        when(driver.addEdgeToVertex(STEP_EDGE_LABEL, "20", "67", new HashMap<>())).
                thenReturn(schemaToStepTwoEdge);
        when(driver.addEdgeToVertex(STEP_PRECEDES, "66", "67", new HashMap<>())).
                thenReturn(stepOneToTwoEdge);
        when(driver.addEdgeToVertex(REFERENCES, "66", "44", new HashMap<>())).
                thenReturn(stepOneToEventEdge);
        when(driver.addEdgeToVertex(REFERENCES, "67", "45", new HashMap<>())).
                thenReturn(stepTwoToEventEdge);

        Schema schemaToAdd = new Schema();
        schemaToAdd.setName("bomb-raid");
        //schemaToAdd.setSubtypeOfName("raid");
        schemaToAdd.putPrivateDataItem(GENERIC_MAP_KEY, GENERIC_MAP_VAL);

        Step stepOne = new Step();
        stepOne.setEvent("bombing");

        schemaToAdd.addStepsItem(stepOne);

        Schema resultSchema = addSchema(schemaToAdd);

        assertEquals("20", resultSchema.getAtId());
        assertEquals("bomb-raid", resultSchema.getName());
        //assertEquals(resultSchema.getSubtypeOfName(), "raid");

        assertEquals(2, resultSchema.getSteps().size());
    }

    @Test void testGetSchemas() throws Exception {
        // Input for getVertices()
        Map<String, String> schemaSearch = new HashMap<>();
        schemaSearch.put(NAME, "bomb-raid");

        // Output for Mock getVertices
        Map<String, String> perResultMap = new HashMap<>();
        perResultMap.put(ID, "10");
        perResultMap.put(LABEL, ENTITY);
        perResultMap.put(NAME, "PER");

        Map<String, String> schemaResultMap = new HashMap<>();
        schemaResultMap.put(ID, "100");
        schemaResultMap.put(LABEL, SCHEMA);
        schemaResultMap.put(NAME, "bomb-raid");
        schemaResultMap.put(SUBTYPE_OF, "raid");
        List<Map<String, String>> eventResultList = new ArrayList<>();
        eventResultList.add(schemaResultMap);

        // Output for Mock getOutgoingPaths
        Map<String, String> roleResultMap = new HashMap<>();
        roleResultMap.put(ID, "300");
        roleResultMap.put(LABEL, ROLE);
        roleResultMap.put(NAME, "bomber");
        roleResultMap.put(PATTERN, "?p");
        roleResultMap.put(FILLED_BY, "[PER]");

        Map<String, String> stepOneResultMap = new HashMap<>();
        stepOneResultMap.put(ID, "400");
        stepOneResultMap.put(LABEL, STEP);
        stepOneResultMap.put(EVENT, "bombing");
        stepOneResultMap.put(OPTIONAL, "false");
        stepOneResultMap.put(REPEATS, "1+");

        Map<String, String> stepTwoResultMap = new HashMap<>();
        stepTwoResultMap.put(ID, "500");
        stepTwoResultMap.put(LABEL, STEP);
        stepTwoResultMap.put(EVENT, "police-raid");
        stepTwoResultMap.put(OPTIONAL, "?p");
        stepTwoResultMap.put(REPEATS, "1+");

        List<Map<String, String>> pathOne = new ArrayList<>();
        pathOne.add(schemaResultMap);
        pathOne.add(roleResultMap);

        List<Map<String, String>> pathTwo = new ArrayList<>();
        pathTwo.add(schemaResultMap);
        pathTwo.add(stepOneResultMap);
        pathTwo.add(stepTwoResultMap);

        List<List<Map<String, String>>> pathsList = new ArrayList<>();
        pathsList.add(pathOne);
        pathsList.add(pathTwo);

        when(driver.getVertices(SCHEMA, schemaSearch)).thenReturn(eventResultList);
        when(driver.getOutgoingPaths("100", -1)).thenReturn(pathsList);

        List<Schema> schemasResultList = getSchemas(schemaSearch);
        assertEquals(1, eventResultList.size());

        Schema retrievedSchema = schemasResultList.get(0);
        assertEquals("100", retrievedSchema.getAtId());
    }
}


