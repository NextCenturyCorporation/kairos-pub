package com.ncc.kairos.moirai.clotho.tinkerpop;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Placeholder tests for NeptuneDriver, just to exercise some NeptuneDriver code.  No connection to Neptune is made. 
 * @author Darren Gemoets
 *
 */
class NeptuneDriverTest extends NeptuneDriver {
    private static final String NAMED_GRAPH = "https://neptune.kairos.nextcentury.com/resin/encoding/test";
    private static final String EVENT_NAME = "Attack";
    private static final String USER_NAME = "Frank";
    private static final String CONSTRUCT_ERROR = "400 BAD_REQUEST \"Error handling CONSTRUCT query.\"";
    private static final String SVERTEX_QUERY_RESULT = "SELECT ?g ?s ?p ?o\n"
            + "FROM <" + NAMED_GRAPH + ">\n"
            + "WHERE { ?s ?p ?o, ?g . }\n"
            + "LIMIT 100\n";

    @Test
    void testGetSGraph() {
        ResponseStatusException thrown = assertThrows(
                ResponseStatusException.class,
                () -> getSGraph(NAMED_GRAPH),
                "Expected getSGraph() to throw, but it didn't."
                );

        assertTrue(thrown.getMessage().contentEquals(CONSTRUCT_ERROR));
    }

    @Test
    void testGetOffsetGraph() {
        ResponseStatusException thrown = assertThrows(
                ResponseStatusException.class,
                () -> getOffsetGraph(NAMED_GRAPH, 1000, 0),
                "Expected getOffsetGraph() to throw, but it didn't."
                );

        assertTrue(thrown.getMessage().contentEquals(CONSTRUCT_ERROR));
    }

    @Test
    void testGetEventList() {
        assertEquals("Failed to execute query", getEventList(NAMED_GRAPH));
    }

    @Test
    void testGetEventTree() {
        ResponseStatusException thrown = assertThrows(
                ResponseStatusException.class,
                () -> getEventTree("foobar", NAMED_GRAPH),
                "Expected getEventTree() to throw, but it didn't."
                );

        assertTrue(thrown.getMessage().contentEquals(CONSTRUCT_ERROR));
    }

    @Test
    void testDeleteNamedGraph() {
        assertEquals("Failed to delete graph", deleteNamedGraph(NAMED_GRAPH));
    }

    @Test
    void testGetNamedGraphs() {
        assertEquals("Failed to execute query", getNamedGraphs());
    }

    @Test
    void testSaveOrUpdate() {
        assertEquals("Failed", saveOrUpdate(NAMED_GRAPH, EVENT_NAME, "foobar", USER_NAME));
    }

    @Test
    void testGetSVertex() {
        assertEquals(SVERTEX_QUERY_RESULT, getSVertex(NAMED_GRAPH, "foobar"));
    }

}
