package com.ncc.kairos.moirai.clotho.interfaces;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.ncc.kairos.moirai.clotho.exceptions.DriverException;

public interface IGremlinDriver {
    /**
     * Establish connection to the particular graph database and instantiate graph traversal object in memory.
     *
     * @throws Exception Exception to throw when the graph database cannot open the DB driver
     */
    void open() throws DriverException;

    /**
     * Retrieve all vertices along with their respective properties and label.
     */
    List<Map<String, String>> getVertices(String label, Map<String, String> searchCriteria);

    /**
     * Given a graph traversal source instance, retrieve all vertices and their respective properties and labels.
     */
    List<Map<String, String>> getEdges(String filter, Map<String, String> searchCriteria);

    /**
     *  Given a source vertex, retrieve all connected vertices via outgoing edges with the provided edge label.
     */
    List<Map<String, String>> getVerticesByEdgeLabel(String vertexID, String edgeLabel);

    /**
     * Given a source vertex, retrieve all vertices connected via outgoing edges.
     * The number of steps traversed / degrees of separation can be specified.
     * By default, traversal will reach all leaves (vertices with no outgoing edges)
     */
    List<List<Map<String, String>>> getOutgoingPaths(String sourceVertexID, int numSteps);

    /**
     * Remove a single edge from the graph. Connected vertices will persist.
     *
     */
    void deleteEdge(String id);

    /**
     * Method for creating 1 or more vertices in the graph with properties.
     */
    List<Map<String, String>> addVertices(String label, List<Map<String, String>> propertiesMapArray);

    /**
     * Method for creating a vertex in the given graph with properties.
     *
     * @return an errorMessage string populated with any errors that occurred. Any empty string implies a successful transaction.
     */
    Map<String, String> addVertex(String label, Map<String, String> properties);

    /**
     * Delete a single vertex from the graph. All outgoing AND incoming edges will be removed as well.
     */
    void deleteVertex(String id);

    /**
     * Adds an edge between two existing vertices.
     *
     * @return a map containg the properties of the edge that is created.
     */
    Map<String, String> addEdgeToVertex(String label, String fromVertex, String toVertex, Map<String, String> propertiesMap);

    /**
     * Runs raw sparql queries passed into the /query/sparql endpoint.
     */
    String runSparqlQuery(String filter);

    /**
     * Verify db connections and transactions are managed correctly. May need a "refresh" method.
     */
    void close() throws DriverException;

    /**
     * Get limited Graph content from named graph.
     * @param namedGraph name of graph.
     * @return Returns the graph as a string.
     */
    String getSGraph(@NotNull @Valid String namedGraph);

    /**
     * Get Event List from namedGraph.
     * @param namedGraph name of graph.
     * @param schema schema name.
     * @return The event list as a string.
     */
    String getEventList(@NotNull @Valid String namedGraph);

    /**
     * Get Event tree.
     * @param eventName name of Event.
     * @param namedGraph name of graph.
     * @return The event tree
     */
    String getEventTree(@NotNull @Valid String eventName, @NotNull @Valid String namedGraph);

    /**
     * Gets all namedGraphs.
     * @return json String of named graphs.
     */
    String getNamedGraphs();

    /**
     * Gets the limited data from graph using offset.
     * @param namedGraph name of graph.
     * @param offset offset of results.
     * @return The section of the graph specified by the offset.
     */
    String getOffsetGraph(@Valid String namedGraph, @NotNull @Valid Integer limit, @NotNull @Valid Integer offset);

    /**
     * Deletes Named graph fiven name.
     * @param namedGraph graph to delete.
     * @return Success status of the graph deletion action.
     */
    String deleteNamedGraph(@NotNull @Valid String namedGraph);

    /**
     * Saves or updates an object to Neptune.
     * @param namedGraph name of Graph for action.
     * @param jsonObject Object that is persisted.
     * @return Success status of the saveOrUpdate action.
     */
    String saveOrUpdate(@NotNull @Valid String namedGraph, @NotNull @Valid String eventKey, @NotNull @Valid String additionalInfo, @NotNull @Valid String userName);
}
