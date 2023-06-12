package com.ncc.kairos.moirai.clotho.tinkerpop;

import com.ncc.kairos.moirai.clotho.exceptions.DriverException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * This is an abstract base class for driver classes that populate the graph database.
 *
 * @author Darren Gemoets
 */
public abstract class AbstractGremlinDriver implements com.ncc.kairos.moirai.clotho.interfaces.IGremlinDriver {

    /**
     * Creates one or more vertices based on the passed-in array of properties-maps and label.
     *
     * @return a list of Maps corresponding to the vertices that were inserted. For failures: "ID=-1", "Error" property.
     */
    @Override
    public abstract List<Map<String, String>> addVertices(String label, List<Map<String, String>> propertiesMapArray);

    /**
     * Method for creating a vertex in the given graph with properties.
     *
     * @return a Map< String, String > representing the insertion of the new vertex (Success or Failed)
     * The map will contain a key entry for "id" which will be -1 if failed
     * If the insertion fails, an entry for the key ERROR will also be created.
     */
    public abstract Map<String, String> addVertex(String label, Map<String, String> properties);

    /**
     * Delete a single vertex from the graph. All outgoing AND incoming edges will be removed as well.
     */
    @Override
    public abstract void deleteVertex(String id);

    /**
     * Remove a single edge from the graph. Connected vertices will persist.
     */
    @Override
    public abstract void deleteEdge(String id);

    /**
     * Adds an edge between two existing vertices.
     */
    @Override
    public abstract Map<String, String> addEdgeToVertex(String label, String fromVertex, String toVertex, Map<String, String> propertiesMap);

    /**
     * Retrieve all vertices belonging to the label and satisfying the search criteria.
     * NOTE: What should be returned when an error occurs?
     */
    @Override
    public abstract List<Map<String, String>> getVertices(String label, Map<String, String> searchCriteria);

    /**
     * Retrieve all vertices connected to a given vertex by the provided list of edge labels.
     */
    @Override
    public abstract List<Map<String, String>> getVerticesByEdgeLabel(String vertexID, String edgeLabel);

    /**
     * Given a source vertex, retrieve all outgoing paths(outgoing-traversals).
     * A path is a list of vertices in the order of their node-distance from the source vertex.
     * NOTE: possible enhancement to allow caller to specify which edges to traverse; currently traverses all outgoing edges.
     */
    @Override
    public abstract List<List<Map<String, String>>> getOutgoingPaths(String sourceVertexID, int depth);

    /**
     * Given a graph traversal source instance, retrieve all vertices and their respective properties and labels.
     */
    @Override
    public abstract List<Map<String, String>> getEdges(String label, Map<String, String> searchCriteria);

    /**
     * Close the connection to the db
     * NOTE: Verify db connections and transactions are managed correctly. May need a "refresh" method.
     * @throws DriverException if closing the connection fails
     */
    @Override
    public abstract void close() throws DriverException;

    /**
     * Runs raw sparql queries passed into the /query/sparql endpoint.
     */
    @Override
    public abstract String runSparqlQuery(String filter);

    @Override
    public String getSGraph(String namedGraph) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, this.getClass().getName() + " doesn't implement getSGraph");
    }

    @Override
    public String getEventList(String namedGraph) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, this.getClass().getName() + " doesn't implement getEventList");
    }

    @Override
    public String getEventTree(String eventName, String namedGraph) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, this.getClass().getName() + " doesn't implement getEventTree");
    }

    @Override
    public String getNamedGraphs() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, this.getClass().getName() + " doesn't implement getNamedGraph");
    }

    @Override
    public String getOffsetGraph(String namedGraph, Integer limit, Integer offset) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, this.getClass().getName() + " doesn't implement getOffsetGraph");
    }

    @Override
    public String deleteNamedGraph(String namedGraph) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, this.getClass().getName() + " doesn't implement deleteNamedGraph");
    }

    @Override
    public String saveOrUpdate(String namedGraph, String jsobObject, String info, String userName) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, this.getClass().getName() + " doesn't implement saveOrUpdate");
    }

}
