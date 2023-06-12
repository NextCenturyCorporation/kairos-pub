package com.ncc.kairos.moirai.clotho.utilities;

import static org.junit.jupiter.api.Assertions.*;

import static com.ncc.kairos.moirai.clotho.resources.GremlinTestConstants.*;

import com.ncc.kairos.moirai.clotho.model.Edge;
import com.ncc.kairos.moirai.clotho.model.Vertex;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GraphConversionTest extends GraphConversion {

    @AfterEach
    void tearDown() {

    }

    @Test
    void testAddIfNotEmpty() {
        Map<String, String> map = new HashMap<>();
        String key = "key";
        String val = "val";
        String emptyKey = "emptyKey";
        String emptyVal = "";

        addIfNotEmpty(map, key, val);
        assertEquals(1, map.size());

        addIfNotEmpty(map, emptyKey, emptyVal);
        assertEquals(1, map.size());
    }

    @Test
    void testConvertStringArrayToString() {
        String[] strArr = {"A", "B", "C", "D"};
        String strExpected = "[A, B, C, D]";

        String strResult = convertStringArrayToString(strArr);
        assertEquals(strExpected, strResult);
    }

    @Test
    void testConvertStringToStringArray() {
        String[] strArrExpected = {"A", "B", "C", "D"};
        String str = "[A, B, C, D]";

        String[] strArrResult = convertStringToStringArray(str);

        assertEquals(strArrExpected.length, strArrResult.length);

        for (int i = 0; i < strArrExpected.length; i++) {
            assertEquals(strArrExpected[i], strArrResult[i]);
        }
    }

    @Test
    void testConvertMapsToVertices() {
        Map<String, String> mapOne = new HashMap<>();
        mapOne.put(LABEL, PERSON_LABEL);
        mapOne.put(NAME, "Daniel");

        Map<String, String> mapTwo = new HashMap<>();
        mapTwo.put(LABEL, PROJECT_LABEL);
        mapTwo.put(NAME, "KAIROS");

        List<Map<String, String>> mapsList = new ArrayList<>();
        mapsList.add(mapOne);
        mapsList.add(mapTwo);

        // Execute function to test
        List<Vertex> resultVertices = convertMapsToVertices(mapsList);

        Vertex vertexOne = resultVertices.get(0);
        assertEquals(PERSON_LABEL, vertexOne.getLabel());
        assertEquals("Daniel", vertexOne.getPropertiesMap().get(NAME));

        Vertex vertexTwo = resultVertices.get(1);
        assertEquals(PROJECT_LABEL, vertexTwo.getLabel());
        assertEquals("KAIROS", vertexTwo.getPropertiesMap().get(NAME));
    }

    @Test
    void testConvertMapsToEdges() {
        Map<String, String> mapOne = new HashMap<>();
        mapOne.put(LABEL, WORKS_ON_LABEL);
        mapOne.put(FROM_VERTEX_ID, "A");
        mapOne.put(TO_VERTEX_ID, "B");

        Map<String, String> mapTwo = new HashMap<>();
        mapTwo.put(LABEL, RELATION);
        mapTwo.put(FROM_VERTEX_ID, "C");
        mapTwo.put(TO_VERTEX_ID, "D");

        List<Map<String, String>> mapsList = new ArrayList<>();
        mapsList.add(mapOne);
        mapsList.add(mapTwo);

        // Execute function to test
        List<Edge> resultEdges = convertMapsToEdges(mapsList);

        Edge edgeOne = resultEdges.get(0);
        assertEquals(WORKS_ON_LABEL, edgeOne.getLabel());
        assertEquals("A", edgeOne.getFromVertexID());
        assertEquals("B", edgeOne.getToVertexID());

        Edge edgeTwo = resultEdges.get(1);
        assertEquals(RELATION, edgeTwo.getLabel());
        assertEquals("C", edgeTwo.getFromVertexID());
        assertEquals("D", edgeTwo.getToVertexID());
    }

    @Test
    void testSearchCriteriaToMap() {
        String searchCriteria = "label=PERSON,name=Daniel";

        Map<String, String> searchMap = searchCriteriaToMap(searchCriteria);

        assertEquals(2, searchMap.size());
        assertEquals("PERSON", searchMap.get("label"));
        assertEquals("Daniel", searchMap.get("name"));
    }
}
