package com.ncc.kairos.moirai.clotho.interfaces;

import com.ncc.kairos.moirai.clotho.exceptions.GraphException;
import com.ncc.kairos.moirai.clotho.model.Edge;
import com.ncc.kairos.moirai.clotho.model.Vertex;
import com.ncc.kairos.moirai.clotho.model.Path;

import java.util.List;
import java.util.Map;

public interface IGraphService {
    void open() throws GraphException;

    void close() throws GraphException;

    void acceptChanges();

    void rollbackChanges();

    void displayChanges();

    /**
     * Retrieve all @Vertex along with their respective properties and label.
     */
    List<Vertex> getVertices(String label, Map<String, String> searchCriteria) throws GraphException;

    List<Vertex> getVerticesByProperty(String label, String propertyKey, String propertyValue) throws GraphException;

    List<Vertex> getVerticesByProperty(String label, String propertyKey, List<String> propertyValues) throws GraphException;

    Vertex getVertexByProperty(String label, String propertyKey, String propertyValue) throws GraphException;

    Vertex getUniqueVertexByProperty(String label, String propertyKey, String propertyValue) throws GraphException;

    boolean vertexExists(String label, Map<String, String> searchCriteria) throws GraphException;

    boolean vertexExistsByProperty(String label, String propertyKey, String propertyValue) throws GraphException;

    /**
     * Retrieve all @Vertex along with their respective properties and label.
     */
    List<Vertex> getVertices() throws GraphException;

    /**
     * Given a source vertex id, return all vertices connected to the source via the edge labels also provided.
     */
    List<Vertex> getVerticesByEdgeLabel(String vertexID, String edgeLabel) throws GraphException;

    /**
     * Retrieve all vertices connected to a given source-vertex via outgoing edges.
     */
    List<Path> getOutgoingPaths(String vertexID, int numSteps) throws GraphException;

    List<Path> getOutgoingPaths(String vertexID) throws GraphException;

    /**
     * Retrieve all @Edge and their respective properties and labels.
     */
    List<Edge> getEdges(String label, Map<String, String> searchCriteria) throws GraphException;
    /**
     * Retrieve all @Edge and their respective properties and labels.
     */
    List<Edge> getEdges() throws GraphException;
    /**
     * Method for creating a @Vertex in the given graph with properties.
     *
     * @return an errorMessage string populated with any errors that occurred. Any empty string implies a successful transaction.
     */
    Vertex addVertex(Vertex newVertex) throws GraphException;

    Vertex addVertex(String label, Map<String, String> properties) throws GraphException;

    /**
     * Adds an @Edge between two existing vertices.
     *
     * @return the edge created in the graph
     */
    Edge addEdge(Edge newEdge) throws GraphException;

    Edge addEdge(String label, String fromVertexID, String toVertexID) throws GraphException;

    Edge addEdge(String label, String fromVertexId, String toVertexId, Map<String, String> propertiesMap) throws GraphException;
    /**
     * Adds an @Edge between two existing vertices.
     */
    Edge addEdge(String label, Vertex fromVertex, Vertex toVertex) throws GraphException;

    /**
     * Adds an @Edge between two existing vertices.
     *
     * @return the edge created in the graph
     */
    Edge addEdge(String label, Vertex fromVertex, Vertex toVertex, Map<String, String> propertiesMap) throws GraphException;

    List<Edge> addEdges(String label, String fromVertexId, List<String> toVertexIds);

	void deleteVertices(List<String> ids);

	void deleteEdges(List<String> ids);

    void deleteGraph() throws GraphException;
}
