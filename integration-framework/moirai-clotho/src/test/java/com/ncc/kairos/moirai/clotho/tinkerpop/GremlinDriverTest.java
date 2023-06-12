package com.ncc.kairos.moirai.clotho.tinkerpop;

import static org.junit.jupiter.api.Assertions.*;
import static com.ncc.kairos.moirai.clotho.resources.GremlinTestConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;

import org.apache.commons.io.IOUtils;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.sparql.process.traversal.dsl.sparql.SparqlTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import com.ncc.kairos.moirai.clotho.exceptions.DriverException;
import com.ncc.kairos.moirai.clotho.utilities.GremlinConversion;

class GremlinDriverTest extends GremlinDriver {

    protected GraphTraversalSource g;
    protected Graph graph;
    protected String connectionUri;

    @Override
    public void open() throws DriverException {
        BaseConfiguration conf = new BaseConfiguration();
        conf.setProperty("gremlin.tinkergraph.vertexIdManager", "LONG");
        conf.setProperty("gremlin.tinkergraph.edgeIdManager", "LONG");
        graph = TinkerGraph.open(conf);
        g = graph.traversal();
        connectionUri = "Tinker-Graph";
    }

    @Override
    public GraphTraversalSource getGTS() {
        return g;
    }

    @Override
    protected SparqlTraversalSource getSTS() {
        return graph.traversal(SparqlTraversalSource.class);
    }

    @Override
    protected void commitTransaction() {
        // do nothing since Tinker Graph does not support transactions
    }

    @Override
    protected void closeTransaction() {
        // do nothing
    }

    @BeforeEach
    void setUp() throws DriverException {
        this.open();
    }

    @AfterEach
    void tearDown() throws DriverException {
        graph = null;
        g = null;
        connectionUri = "";
        //this.close();
    }

    void loadVertexData() throws IOException {
        InputStream inputData = getClass().getResourceAsStream(VERTEX_DATA_FILE);
        List<String> vertexDataEntries = IOUtils.readLines(inputData, StandardCharsets.UTF_8);
        List<String> fieldMappings = new ArrayList<>();

        for (String curLine : vertexDataEntries) {
            if (!StringUtils.isAllEmpty(curLine)) {
                if (curLine.startsWith("***")) { // Check if header-line
                    fieldMappings = Arrays.asList(curLine.substring(3).trim().split("\\s*,\\s*"));
                } else { // otherwise is entry line
                    List<String> curEntryVals = Arrays.asList(curLine.trim().split("\\s*,\\s*"));
                    // Assume the 1st element is the Label
                    String curLabel = curEntryVals.get(0);
                    Map<String, String> curPropertiesMap = new HashMap<>();
                    int mapIndex = 1;

                    for (String curVal : curEntryVals.subList(1, curEntryVals.size())) {
                        String curPropertyKey = fieldMappings.get(mapIndex++);
                        curPropertiesMap.put(curPropertyKey, curVal);
                    }
                    // Create vertex
                    addVertex(curLabel, curPropertiesMap);
                }
            }
        }
    }

    void loadEdgeData() throws IOException {
        loadVertexData();
        InputStream inputData = getClass().getResourceAsStream(EDGE_DATA_FILE);
        List<String> edgeDataEntries = IOUtils.readLines(inputData, StandardCharsets.UTF_8);

        for (String curLine : edgeDataEntries) {
            if (!StringUtils.isAllEmpty(curLine)) {
                List<String> curEntryVals = Arrays.asList(curLine.trim().split("\\s*,\\s*"));
                // Assume format is correct: 1st = label, 2nd = name, 3rd = relation, 4th = label, 5th = name
                String fromLabel = curEntryVals.get(0);
                String fromName = curEntryVals.get(1);
                String relation = curEntryVals.get(2);
                String toLabel = curEntryVals.get(3);
                String toName = curEntryVals.get(4);

                Map<String, String> fromVertexSearchCriteria = new HashMap<>();
                fromVertexSearchCriteria.put(NAME, fromName);
                Map<String, String> toVertexSearchCriteria = new HashMap<>();
                toVertexSearchCriteria.put(NAME, toName);

                // Retrieve the "From" and "To" vertices respectively based on their NAME property

                // NOTE: will assume the test edge data corresponds with test vertex data, so this should return 1 vert.
                Map<String, String> fromVertex = getVertices(fromLabel, fromVertexSearchCriteria).get(0);
                Map<String, String> toVertex = getVertices(toLabel, toVertexSearchCriteria).get(0);

                // Attempt to create an edge between these vertices
                addEdgeToVertex(relation, fromVertex.get(ID), toVertex.get(ID), new HashMap<>());
            }
        }
    }

    @Test
    void testAddVertex() {
        String personName = "Daniel";
        String personLocation = "VA";
        String locationLabel = "location";

        Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put(NAME, personName);
        propertiesMap.put(locationLabel, personLocation);

        Map<String, String> addVertexResult = addVertex(PERSON_LABEL, propertiesMap);

        assertEquals(addVertexResult.get(NAME), personName);
        assertEquals(addVertexResult.get("location"), personLocation);
        assertNotEquals("-1", addVertexResult.get(ID));
        assertFalse(addVertexResult.get(ID).length() < 1);
    }

    @Test
    void testAddVertices() {
        String personOneName = "Daniel";
        String personTwoName = "Mitchell";
        String personOneLocation = "VA";
        String personTwoLocation = "MD";
        String locationLabel = "location";

        Map<String, String> personOneProperties = new HashMap<>();
        personOneProperties.put(NAME, personOneName);
        personOneProperties.put(locationLabel, personOneLocation);

        Map<String, String> personTwoProperties = new HashMap<>();
        personTwoProperties.put(NAME, personTwoName);
        personTwoProperties.put(locationLabel, personTwoLocation);

        List<Map<String, String>> vertexMaps = new ArrayList<>();

        vertexMaps.add(personOneProperties);
        vertexMaps.add(personTwoProperties);

        addVertices(PERSON_LABEL, vertexMaps);

        List<Map<String, String>> getInsertedVertices = getVertices(PERSON_LABEL, new HashMap<>());

        assertEquals(2, getInsertedVertices.size());
    }

    @Test
    void testGetVertices() throws IOException {
        loadVertexData();

        // Test variables
        String nameToSearch = "Daniel";
        String projectToSearch = "KAIROS";

        Map<String, String> getAllPersonsSearch = new HashMap<>();
        Map<String, String> getSinglePersonSearch = new HashMap<>();
        getSinglePersonSearch.put(NAME, nameToSearch);

        Map<String, String> getAllProjectsSearch = new HashMap<>();
        Map<String, String> getSingleProjectSearch = new HashMap<>();
        getSingleProjectSearch.put(NAME, projectToSearch);

        List<Map<String, String>> getAllPersonsResults = getVertices(PERSON_LABEL, getAllPersonsSearch);
        List<Map<String, String>> getSinglePersonResults = getVertices(PERSON_LABEL, getSinglePersonSearch);
        List<Map<String, String>> getAllProjectsResults = getVertices(PROJECT_LABEL, getAllProjectsSearch);
        List<Map<String, String>> getSingleProjectResults = getVertices(PROJECT_LABEL, getSingleProjectSearch);

        assertEquals(7, getAllPersonsResults.size());
        assertEquals(1, getSinglePersonResults.size());
        assertEquals(2, getAllProjectsResults.size());
        assertEquals(1, getSingleProjectResults.size());
    }

    @Test
    void testAddEdgeToVertex() throws IOException {
        loadVertexData();

        String fromVertexName = "Daniel";
        String toVertexName = "KAIROS";

        Map<String, String> fromVertexSearchCriteria = new HashMap<>();
        fromVertexSearchCriteria.put(NAME, fromVertexName);
        Map<String, String> toVertexSearchCriteria = new HashMap<>();
        toVertexSearchCriteria.put(NAME, toVertexName);

        Map<String, String> edgePropertiesMap = new HashMap<>();
        edgePropertiesMap.put("Full-Time", "true");

        // Load the to and from vertices to be connected with an edge
        String personVertexID = getVertices(PERSON_LABEL, fromVertexSearchCriteria).get(0).get(ID);
        String projectVertexID = getVertices(PROJECT_LABEL, toVertexSearchCriteria).get(0).get(ID);

        Map<String, String> addEdgeResult =
                addEdgeToVertex(WORKS_ON_LABEL, personVertexID, projectVertexID, edgePropertiesMap);

        assertEquals(addEdgeResult.get(FROM_VERTEX_ID), personVertexID);
        assertEquals(addEdgeResult.get(TO_VERTEX_ID), projectVertexID);
        assertNotEquals("-1", addEdgeResult.get(ID));
        assertFalse(addEdgeResult.get(ID).length() < 1);
    }

    @Test
    void getEdges() throws IOException {
        loadVertexData();
        loadEdgeData();

        List<Map<String, String>> getEdgeResults = getEdges(WORKS_ON_LABEL, new HashMap<>());

        assertEquals(6, getEdgeResults.size());
    }

    @Test
    void testGetVerticesByEdgeLabel() throws IOException {
        loadVertexData();
        loadEdgeData();

        // Test variables
        String nameToSearch = "Darren";

        Map<String, String> personSearch = new HashMap<>();
        personSearch.put(NAME, nameToSearch);

        String sourceVertexID = getVertices(PERSON_LABEL, personSearch).get(0).get(ID);

        List<Map<String, String>> vertexResults = getVerticesByEdgeLabel(sourceVertexID, WORKS_ON_LABEL);

        assertEquals(2, vertexResults.size());
    }

    @Test
    void testDeleteEdge() throws IOException {
        loadVertexData();
        loadEdgeData();

        List<Map<String, String>> getEdgeResults = getEdges(WORKS_ON_LABEL, new HashMap<>());

        assertEquals(6, getEdgeResults.size());

        String edgeID = getEdgeResults.get(0).get(ID);

        deleteEdge(edgeID);

        List<Map<String, String>> edgeResults = getEdges(WORKS_ON_LABEL, new HashMap<>());

        assertEquals(5, edgeResults.size());
    }

    @Test
    void testDeleteVertex() throws IOException {
        loadVertexData();

        // Test variables
        String personName = "Daniel";
        String projectName = "KAIROS";
        Map<String, String> personSearch = new HashMap<>();
        personSearch.put(NAME, personName);
        Map<String, String> projectSearch = new HashMap<>();
        projectSearch.put(NAME, projectName);

        List<Map<String, String>> getPersonVertexResult = getVertices(PERSON_LABEL, personSearch);
        List<Map<String, String>> getProjectVertexResult = getVertices(PROJECT_LABEL, projectSearch);

        String personVertexID = getPersonVertexResult.get(0).get(ID);
        String projectVertexID = getProjectVertexResult.get(0).get(ID);

        // Verify the vertices exist prior to deletion
        assertEquals(1, getPersonVertexResult.size());
        assertEquals(1, getProjectVertexResult.size());

        // Delete person and project vertices respectively
        deleteVertex(personVertexID);
        deleteVertex(projectVertexID);

        // Attempt to retrieve vertices post-deletion
        List<Map<String, String>> getPersonResult = getVertices(PERSON_LABEL, personSearch);
        List<Map<String, String>> getProjectResult = getVertices(PROJECT_LABEL, projectSearch);

        assertEquals(0, getPersonResult.size());
        assertEquals(0, getProjectResult.size());
    }

    @Test
    void testGetOutgoingPaths() throws IOException {
        loadVertexData();
        loadEdgeData();

        Map<String, String> personOne = new HashMap<>();
        personOne.put(NAME, "Kevin");
        Map<String, String> personTwo = new HashMap<>();
        personTwo.put(NAME, "Darren");

        String sourceVertexOneID = getVertices(PERSON_LABEL, personOne).get(0).get(ID);
        String sourceVertexTwoID = getVertices(PERSON_LABEL, personTwo).get(0).get(ID);

        List<List<Map<String, String>>> outgoingPathsOne = getOutgoingPaths(sourceVertexOneID, 0);
        List<List<Map<String, String>>> outgoingPathsTwo = getOutgoingPaths(sourceVertexTwoID, 0);

        assertEquals(1, outgoingPathsOne.size());
        assertEquals(2, outgoingPathsTwo.size());
    }

    @Test
    void testConvertObjectMapsToStringMaps() {
        // Instantiate Iterator(Map(Object, Object)) to be used as input
        List<Map<Object, Object>> originalList = new ArrayList<>();
        Map<Object, Object> mapA = new HashMap<>();
        mapA.put("keyA", "valA");
        Map<Object, Object> mapB = new HashMap<>();
        mapB.put("keyB", "valB");
        Map<Object, Object> mapC = new HashMap<>();
        mapC.put("keyC", "valC");
        originalList.add(mapA);
        originalList.add(mapB);
        originalList.add(mapC);
        Iterator<Map<Object, Object>> mapIterator = originalList.iterator();

        // Execute function call and evaluate output
        List<Map<String, String>> outputList = GremlinConversion.convertObjectMapsToStringMaps(mapIterator);

        int curIndex = 0;
        // Iterate through output List and compare to original input list
        for (Map<String, String> curOutputMap : outputList) {
            Map<Object, Object> curInputMap = originalList.get(curIndex);
            for (Map.Entry<String, String> curOutputEntry : curOutputMap.entrySet()) {
                String curOutputEntryKey = curOutputEntry.getKey();
                String curOutputEntryVal = curOutputEntry.getValue();
                String curInputEntryVal = curInputMap.get(curOutputEntryKey).toString();

                assertEquals(curOutputEntryVal, curInputEntryVal);
            }
            curIndex++;
        }
    }
}
