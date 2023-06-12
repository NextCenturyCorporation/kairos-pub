package com.ncc.kairos.moirai.clotho.tinkerpop;

import com.ncc.kairos.moirai.clotho.exceptions.DriverException;
import com.ncc.kairos.moirai.clotho.exceptions.GraphException;
import com.ncc.kairos.moirai.clotho.resources.GraphConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions;
import org.apache.tinkerpop.gremlin.sparql.process.traversal.dsl.sparql.SparqlTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.ncc.kairos.moirai.clotho.utilities.GremlinConversion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class populates the graph database.
 * This class is in an experimental, pre-release state.  There are many TODOs, that are indicated in NOTEs in the code.
 *
 * @author Daniel Nguyen
 */
public abstract class GremlinDriver extends AbstractGremlinDriver {

    private static final Logger log = LoggerFactory.getLogger(GremlinDriver.class);

    protected abstract GraphTraversalSource getGTS();

    protected abstract SparqlTraversalSource getSTS();

    /**
     * Creates one or more vertices based on the passed-in array of properties-maps and label.
     *
     * @return a list of Maps corresponding to the vertices that were inserted. For failures: "ID=-1", "Error" property.
     */
    @Override
    public List<Map<String, String>> addVertices(String label, List<Map<String, String>> propertiesMapArray) {
        List<Map<String, String>> postInsertVerticesList = new ArrayList<>();
        // NOTE: add null check on propertiesMap
        for (Map<String, String> curPropsMap : propertiesMapArray) {
            // Attempt to insert the current vertex
            Map<String, String> curInsertResult = addVertex(getGTS(), label, curPropsMap);
            // Insert the result into the list to be returned
            postInsertVerticesList.add(curInsertResult);
        }
        return postInsertVerticesList;
    }

    /**
     * Method for creating a vertex in the given graph with properties.
     *
     * @return a Map< String, String > representing the insertion of the new vertex (Success or Failed)
     * The map will contain a key entry for "id" which will be -1 if failed
     * If the insertion fails, an entry for the key ERROR will also be created.
     */
    public Map<String, String> addVertex(String label, Map<String, String> properties) {
        return addVertex(getGTS(), label, properties);
    }

    // NOTEs:
    // - The returned map is populated with values assumed to populated in the database
    // - Update so that map is reflective of what is actually stored in database.
    protected Map<String, String> addVertex(GraphTraversalSource g, String label, Map<String, String> properties) {
        String id;
        String errorMsg = "";
        Map<String, String> vertexMapToReturn = new HashMap<>();
        try {
            // Initialize and instantiate new vertex object using gremlin-language
            Vertex v = g.addV(label).next();
            // Add the label as the first entry of the map
            vertexMapToReturn.put(GraphConstants.LABEL, label);

            // Populate properties of new vertex and add to map for return to API controller
            // NOTE: add null check on propertiesMap
            for (Map.Entry<String, String> curProp : properties.entrySet()) {
                String curPropKey = curProp.getKey();
                String curPropVal = curProp.getValue();

                // add the current property to the new vertex; syntax can differ based on graph-type
                String curPropErrMsg = addPropertyToVertex(v, curPropKey, curPropVal);

                // add property to map to be returned
                vertexMapToReturn.put(curPropKey, curPropVal);

                if (curPropErrMsg.length() > 0) {
                    throw new GraphException(curPropErrMsg);
                }
            }
            // commit the transaction to the db
            commitTransaction();

            // assign the id variable with the id of the new vertex object
            id = v.id().toString();

        } catch (Exception ex) {
            // If an exception occurs during insertion, create an error message and set ID => -1
            errorMsg = String.format("%s: %s", GraphConstants.GREMLIN_ADD_VERTEX_EXCEPTION, ex.getMessage());
            id = GraphConstants.ERROR_DB_ID;
        }

        vertexMapToReturn.put(GraphConstants.ID, id);
        if (errorMsg.length() > 0) {
            vertexMapToReturn.put(GraphConstants.ERROR, errorMsg);
        }
        return vertexMapToReturn;
    }

    @Override
    public void deleteVertex(String id) {
        deleteVertex(getGTS(), id);
    }

    protected void deleteVertex(GraphTraversalSource g, String id) {
        g.V(id).drop().iterate();
        log.debug("Deleted vertex {}", id);
        commitTransaction();
    }

    @Override
    public void deleteEdge(String id) {
        deleteEdge(getGTS(), id);
    }

    protected void deleteEdge(GraphTraversalSource g, String id) {
        Edge e = g.E(id).next();
        log.debug("Deleting edge {}", e);
        e.remove();
        commitTransaction();
    }

    protected String addPropertyToVertex(Vertex v, String key, String value) {
        String errorMsg = "";

        try {
            v.property(key, value);

        } catch (Exception ex) {
            errorMsg = String.format("An error occurred while attempting to add the key %s and value %s to the vertex: %s.", key, value, ex.getMessage());
        }
        return errorMsg;
    }

    /**
     * Adds parent edge to new vertex if parent vertex exists.
     */
    @Override
    public Map<String, String> addEdgeToVertex(String label, String fromVertex, String toVertex, Map<String, String> propertiesMap) {
        return addEdgeToVertex(getGTS(), label, fromVertex, toVertex, propertiesMap);
    }

    /**
     * Adds parent edge to new vertex if parent vertex exists.
     */
    protected Map<String, String> addEdgeToVertex(GraphTraversalSource g, String label, String fromVertex, String toVertex, Map<String, String> propertiesMap) {
        Map<String, String> edgeMapToReturn = new HashMap<>();

        try {
            // Verify that the parent and child vertices exist and retrieve them based on provided ID's
            Vertex childVert = g.V(toVertex).next();
            Vertex parentVert = g.V(fromVertex).next();

            // Create the edge between the two vertices
            Edge edge = g.addE(label).from(parentVert).to(childVert).next();
            /* NOTE: use these values to populate the edgeMapToReturn
            String inVertexID = edge.inVertex().id().toString(); // "TO" vertex
            String outVertexID = edge.outVertex().id().toString(); // "FROM" vertex
            */

            // Add label, parent-vertex(outgoing) ID and child-vertex(incoming) ID to map
            // NOTE: Return an edge map that is reflective of what is ACTUALLY in graph.
            edgeMapToReturn.put(GraphConstants.FROM_VERTEX_ID, fromVertex);
            edgeMapToReturn.put(GraphConstants.TO_VERTEX_ID, toVertex);
            edgeMapToReturn.put(GraphConstants.LABEL, label);

            // Iterate through properties map and add each entry to map to return
            // NOTE: add null check on propertiesMap
            for (Map.Entry<String, String> curProp : propertiesMap.entrySet()) {
                String curPropKey = curProp.getKey();
                String curPropVal = curProp.getValue();
                // Add property entry to edge
                edge.property(curPropKey, curPropVal);
                // Add property entry to map to be returned
                edgeMapToReturn.put(curPropKey, curPropVal);
            }

            // Commit the new edge to the database
            commitTransaction();

            // Retrieve id after transaction is committed to db.
            edgeMapToReturn.put(GraphConstants.ID, edge.id().toString());

        } catch (Exception ex) {
            edgeMapToReturn.put(GraphConstants.ERROR, String.format("%s: %s", GraphConstants.GREMLIN_ADD_EDGE_EXCEPTION, ex.getMessage()));
            edgeMapToReturn.put(GraphConstants.ID, GraphConstants.ERROR_DB_ID);
        } finally {
            closeTransaction();
        }
        return edgeMapToReturn;
    }

    /**
     * Retrieve all vertices belonging to the label and satisfying the search criteria.
     * NOTE: What should be returned when an error occurs?
     */
    public List<Map<String, String>> getVertices(String label, Map<String, String> searchCriteria) {
        try {
            // Retrieve an iterator over the vertices that match the search criteria
            return getVertices(getGTS(), label, searchCriteria);
        } catch (Exception ex) {
            Map<String, String> errorMap = new HashMap<>();
            List<Map<String, String>> errorList = new ArrayList<>();
            errorMap.put(GraphConstants.ERROR, ex.getMessage());
            errorList.add(errorMap);
            return errorList;
        }
    }

    /**
     * Retrieve all vertices along with their respective properties and label.
     */
    protected List<Map<String, String>> getVertices(GraphTraversalSource g, String label, Map<String, String> searchCriteria) {
        // Use gremlin-language to retrieve all vertices and their respective properties in a list
        GraphTraversal<Vertex, Vertex> graphTraversal = g.V();
        if (!StringUtils.isBlank(label)) {
            graphTraversal = graphTraversal.hasLabel(label);
        }

        if (searchCriteria == null) {
            searchCriteria = new HashMap<>();
        }

        for (Map.Entry<String, String> criteria : searchCriteria.entrySet()) {
            graphTraversal = graphTraversal.has(criteria.getKey(), criteria.getValue());
        }

        Iterator<Map<Object, Object>> vIt = graphTraversal.valueMap().with(WithOptions.tokens).by(__.unfold());

        return GremlinConversion.convertObjectMapsToStringMaps(vIt);
    }

    /**
     * Retrieve all vertices connected to a given vertex by the provided list of edge labels.
     */
    public List<Map<String, String>> getVerticesByEdgeLabel(String vertexID, String edgeLabel) {
        return getVerticesByEdgeLabel(getGTS(), vertexID, edgeLabel);
    }

    protected List<Map<String, String>> getVerticesByEdgeLabel(GraphTraversalSource g, String vertexID, String edgeLabel) {
        try {
            List<Map<String, String>> vertexMapsToReturn;

            // Retrieve the vertices connected via edges of the provided label
            Iterator<Map<Object, Object>> vertexIt = g.V(vertexID).out(edgeLabel).valueMap().with(WithOptions.tokens).by(__.unfold());

            // Add all vertices returned for the edge label to the list to return
            vertexMapsToReturn = GremlinConversion.convertObjectMapsToStringMaps(vertexIt);

            return vertexMapsToReturn;

        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            List<Map<String, String>> errorList = new ArrayList<>();
            errorMap.put(GraphConstants.ERROR, e.getMessage());
            errorList.add(errorMap);

            return errorList;
        }
    }

    /**
     * Given a source vertex, retrieve all outgoing paths(outgoing-traversals).
     * A path is a list of vertices in the order of their node-distance from the source vertex.
     * NOTE: possible enhancement to allow caller to specify which edges to traverse; currently traverses all outgoing edges.
     */
    @Override
    public List<List<Map<String, String>>> getOutgoingPaths(String sourceVertexID, int depth) {
        return getOutgoingPaths(getGTS(), sourceVertexID);
    }

    // NOTEs:
    // - Incorporate a depth parameter into the implementation of this function
    // - Create uniform exception-handling
    // - Optimize g.V() query to exclude any paths that contain step-step-post-info
    protected List<List<Map<String, String>>> getOutgoingPaths(GraphTraversalSource g, String sourceVertexID) {
        List<List<Map<String, String>>> pathsToReturn;

        try {
            // List<Path> paths = g.V(sourceVertexID).repeat(__.out()).until(__.not(__.outE())).path().toList()
            List<Path> paths = g.V(sourceVertexID).repeat(__.outE().inV().simplePath()).until(__.not(__.outE())).path().toList();
            pathsToReturn = GremlinConversion.getListsOfMapsListFromPaths(paths);

            return pathsToReturn;
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            List<Map<String, String>> errorList = new ArrayList<>();
            List<List<Map<String, String>>> errorLists = new ArrayList<>();
            errorMap.put(GraphConstants.ERROR, e.getMessage());
            errorList.add(errorMap);
            errorLists.add(errorList);
            return errorLists;
        }
    }

    /**
     * Given a graph traversal source instance, retrieve all vertices and their respective properties and labels.
     */
    public List<Map<String, String>> getEdges(String label, Map<String, String> searchCriteria) {
        try {
            return getEdges(getGTS(), label, searchCriteria);
        } catch (Exception ex) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put(GraphConstants.ERROR, "error is second layer");
            List<Map<String, String>> errorList = new ArrayList<>();
            errorList.add(errorMap);
            return errorList;
        }

    }

    // See KAIR-805: parse fromVertexId and toVertexId separately as part of graph-traversal-search
    protected List<Map<String, String>> getEdges(GraphTraversalSource g, String label, Map<String, String> searchCriteria) {
        // Use gremlin-language to retrieve all edges and their respective properties in a list
        GraphTraversal<Edge, Edge> graphTraversal = g.E();

        if (label != null && label.length() > 0) {
            graphTraversal = graphTraversal.hasLabel(label);
        }
        if (searchCriteria == null) {
            searchCriteria = new HashMap<>();
        }
        // NOTE: do special check if inVertex or outVertex IDs are specified: g.E().inV() or g.E().outV()
        for (Map.Entry<String, String> criteria : searchCriteria.entrySet()) {
            graphTraversal = graphTraversal.has(criteria.getKey(), criteria.getValue());
        }
        Iterator<Map<Object, Object>> eIt = graphTraversal.valueMap().with(WithOptions.tokens).by(__.unfold());
        return GremlinConversion.convertObjectMapsToStringMaps(eIt);
    }

    /**
     * Commit current transaction to database.
     * For Gremlin-Server-hosted dbs (JanusGraph), this will do nothing because
     * each call to the GraphTraversalSource is its own transaction and
     * commit does not need to be called explicitly
     */
    protected void commitTransaction() {
        getGTS().tx().commit();
    }

    protected void closeTransaction() {
        getGTS().tx().close();
    }

    /**
     * Close the connection to the db
     * NOTE: Verify db connections and transactions are managed correctly. May need a "refresh" method.
     * @throws DriverException if closing the connection fails
     */
    @Override
    public void close() throws DriverException {
        try {
            getGTS().close();
        } catch (Exception e) {
            throw new DriverException("Failed to close our gremlin driver", e);
        }
    }

    @Override
    public String runSparqlQuery(String filter) {
        SparqlTraversalSource g = getSTS();
        Iterator<?> r = g.sparql(filter);
        List<Map<?, ?>> results = new ArrayList<>();

        while (r.hasNext()) {
            results.add((Map<?, ?>) r.next());
        }
        return results.toString();
    }

}
