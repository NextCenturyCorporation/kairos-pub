package com.ncc.kairos.moirai.clotho.services;

import static org.junit.jupiter.api.Assertions.*;

import static com.ncc.kairos.moirai.clotho.resources.GremlinTestConstants.*;
import static org.mockito.Mockito.*;

import java.util.*;

import com.ncc.kairos.moirai.clotho.exceptions.GraphException;
import com.ncc.kairos.moirai.clotho.interfaces.IGremlinDriver;
import com.ncc.kairos.moirai.clotho.model.Edge;
import com.ncc.kairos.moirai.clotho.model.Vertex;

import org.junit.jupiter.api.*;

class GraphServiceTest extends GraphService {

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
    void setUp() {
        this.open();
    }

    @AfterEach
    void tearDown() {
        this.close();
    }

    @Test
    void testAcceptChanges() {
        addedVertexIds.add("Vertex");
        addedEdgeIds.add("Edge");
        errors.add("ERROR");

        acceptChanges();

        assertEquals(0, addedVertexIds.size());
        assertEquals(0, addedEdgeIds.size());
        assertEquals(0, errors.size());
    }

    @Test
    void testRollbackChanges() {
        addedVertexIds.add("Vertex");
        addedEdgeIds.add("Edge");

        // Define the mock implementations for relevant gremlin driver instance functions
        doNothing().when(driver).deleteEdge("Edge");
        doNothing().when(driver).deleteVertex("Vertex");

        rollbackChanges();

        assertEquals(0, addedVertexIds.size());
        assertEquals(0, addedEdgeIds.size());
    }

    @Test
    void testGetVertices() {

        List<Map<String, String>> searchCriteriaMapList = new ArrayList<>();
        Map<String, String> searchCriteriaMap = new HashMap<>();
        searchCriteriaMap.put(LABEL, PERSON_LABEL);
        searchCriteriaMap.put("a", "1");
        searchCriteriaMapList.add(searchCriteriaMap);

        // Define the mock implementations for relevant gremlin driver instance functions
        when(driver.getVertices(PERSON_LABEL, new HashMap<>())).thenReturn(searchCriteriaMapList);

        // Execute test
        List<Vertex> resultsList = getVertices(PERSON_LABEL, new HashMap<>());
        Vertex resultsVertex = resultsList.get(0);
        assertEquals(1, resultsList.size());
        assertEquals(PERSON_LABEL, resultsVertex.getLabel());
        assertEquals("1", resultsVertex.getPropertiesMap().get("a"));
    }

    @Test
    void testGetVerticesByEdgeLabel() {
        // Initialize test data

        List<Map<String, String>> resultsList = new ArrayList<>();
        Map<String, String> resultVertexMap = new HashMap<>();
        resultVertexMap.put(LABEL, PROJECT_LABEL);
        resultVertexMap.put(NAME, "KAIROS");
        resultsList.add(resultVertexMap);

        // Define the mock implementations for relevant gremlin driver instance functions
        when(driver.getVerticesByEdgeLabel(PERSON_LABEL, WORKS_ON_LABEL)).thenReturn(resultsList);

        // Execute test
        List<Vertex> resultsVertexList = getVerticesByEdgeLabel(PERSON_LABEL, WORKS_ON_LABEL);
        Vertex resultVertex = resultsVertexList.get(0);
        assertEquals(1, resultsVertexList.size());
        assertEquals("KAIROS", resultVertex.getPropertiesMap().get(NAME));
    }

    @Test
    void testGetEdges() {
        // Initialize test data

        List<Map<String, String>> resultsList = new ArrayList<>();
        Map<String, String> resultEdgeMap = new HashMap<>();
        resultEdgeMap.put(LABEL, WORKS_ON_LABEL);
        resultsList.add(resultEdgeMap);

        // Define the mock implementations for relevant gremlin driver instance functions
        when(driver.getEdges(WORKS_ON_LABEL, new HashMap<>())).thenReturn(resultsList);

        // Execute test
        List<Edge> resultsEdgeList = getEdges(WORKS_ON_LABEL, new HashMap<>());
        Edge resultEdge = resultsEdgeList.get(0);
        assertEquals(1, resultsEdgeList.size());
        assertEquals(WORKS_ON_LABEL, resultEdge.getLabel());
    }

    @Test
    void testAddVertex() {
        // Initialize test data
        Map<String, String> resultVertexMap = new HashMap<>();
        resultVertexMap.put(LABEL, PERSON_LABEL);
        resultVertexMap.put(ID, "1");
        Vertex vertexToAdd = new Vertex();
        vertexToAdd.setLabel(PERSON_LABEL);
        vertexToAdd.setPropertiesMap(new HashMap<>());

        // Define the mock implementations for relevant gremlin driver instance functions
        when(driver.addVertex(PERSON_LABEL, new HashMap<>())).thenReturn(resultVertexMap);

        // Execute test
        Vertex resultVertex = addVertex(vertexToAdd);
        assertEquals(PERSON_LABEL, resultVertex.getLabel());
        assertTrue(addedVertexIds.contains("1"));
    }

    @Test
    void testAddEdge() {
        // Initialize test data
        Map<String, String> resultEdgeMap = new HashMap<>();
        resultEdgeMap.put(LABEL, WORKS_ON_LABEL);
        resultEdgeMap.put(FROM_VERTEX_ID, "1");
        resultEdgeMap.put(TO_VERTEX_ID, "2");
        resultEdgeMap.put(ID, "1");
        Edge edgeToAdd = new Edge();
        edgeToAdd.setLabel(WORKS_ON_LABEL);
        edgeToAdd.setFromVertexID("1");
        edgeToAdd.setToVertexID("2");
        edgeToAdd.setPropertiesMap(new HashMap<>());

        // Define the mock implementations for relevant gremlin driver instance functions
        when(driver.addEdgeToVertex(WORKS_ON_LABEL, "1", "2", new HashMap<>()))
                .thenReturn(resultEdgeMap);

        // Execute test
        Edge resultEdge = addEdge(edgeToAdd);
        assertEquals(WORKS_ON_LABEL, resultEdge.getLabel());
        assertEquals("1", resultEdge.getFromVertexID());
        assertEquals("2", resultEdge.getToVertexID());
        assertTrue(addedEdgeIds.contains("1"));
    }

    @Test
    void testDeleteVertex() {
        // Define the mock implementations for relevant gremlin driver instance functions
        doNothing().when(driver).deleteVertex("A");
        doNothing().when(driver).deleteVertex("B");

        List<String> idsToDelete = new ArrayList<>();
        idsToDelete.add("A");
        idsToDelete.add("B");

        deleteVertices(idsToDelete);

        verify(driver, times(1)).deleteVertex("A");
        verify(driver, times(1)).deleteVertex("B");
    }

    @Test
    void testDeleteEdge() {
        // Define the mock implementations for relevant gremlin driver instance functions
        doNothing().when(driver).deleteEdge("A");
        doNothing().when(driver).deleteEdge("B");

        List<String> idsToDelete = new ArrayList<>();
        idsToDelete.add("A");
        idsToDelete.add("B");

        deleteEdges(idsToDelete);

        verify(driver, times(1)).deleteEdge("A");
        verify(driver, times(1)).deleteEdge("B");
    }
}
