package com.ncc.kairos.moirai.clotho.services;

import com.ncc.kairos.moirai.clotho.exceptions.GraphException;
import com.ncc.kairos.moirai.clotho.interfaces.IGraphService;
import com.ncc.kairos.moirai.clotho.interfaces.IGremlinDriver;
import com.ncc.kairos.moirai.clotho.model.Edge;
import com.ncc.kairos.moirai.clotho.model.Vertex;
import com.ncc.kairos.moirai.clotho.model.Path;
import com.ncc.kairos.moirai.clotho.tinkerpop.GremlinDriverSingleton;
import com.ncc.kairos.moirai.clotho.utilities.GraphConversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import static com.ncc.kairos.moirai.clotho.resources.GraphConstants.*;

@Service
public class GraphService implements IGraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);
    protected IGremlinDriver driver;

    List<String> addedVertexIds;
    List<String> addedEdgeIds;
    List<String> errors;

    @Override
    public void open() throws GraphException {
        driver = GremlinDriverSingleton.getDriver();
        addedVertexIds = new ArrayList<>();
        addedEdgeIds = new ArrayList<>();
        errors = new ArrayList<>();
        try {
            driver.open();
        } catch (Exception e) {
            throw new GraphException("Failed to open the gremlin driver.", e);
        }
    }

    @Override
    public void close() throws GraphException {
        try {
            driver.close();
        } catch (Exception e) {
            throw new GraphException("Failed to close the gremlin driver.", e);
        }
    }

    @Override
    public void acceptChanges() {
        addedVertexIds.clear();
        addedEdgeIds.clear();
        errors.clear();
    }

    @Override
    public void rollbackChanges() {
        for (String edgeId : addedEdgeIds) {
            driver.deleteEdge(edgeId);
        }
        for (String vertexId : addedVertexIds) {
            driver.deleteVertex(vertexId);
        }
        addedEdgeIds.clear();
        addedVertexIds.clear();
    }

    @Override
    public void displayChanges() {
        log.info("Changes:\n  Vertices: {}\n  Edges: {}", addedVertexIds, addedEdgeIds);
    }

    @Override
    public List<Vertex> getVertices(String label, Map<String, String> searchCriteria) throws GraphException {
        return GraphConversion.convertMapsToVertices(driver.getVertices(label, searchCriteria));
    }

    @Override
    public List<Vertex> getVertices() throws GraphException {
        return getVertices(null, null);
    }

    @Override
    public List<Vertex> getVerticesByProperty(String label, String propertyKey, String propertyValue) throws GraphException {
        Map<String, String> searchCriterion = new HashMap<>();
        searchCriterion.put(propertyKey, propertyValue);
        return getVertices(label, searchCriterion);
    }

    @Override
    public List<Vertex> getVerticesByProperty(String label, String propertyKey, List<String> propertyValues) throws GraphException {
        List<Vertex> verticesToReturn = new ArrayList<>();
        for (String curVal: propertyValues) {
            try {
                verticesToReturn.add(getVertexByProperty(label, propertyKey, curVal));
            } catch (Exception e) {
                throw new GraphException("Failed to get vertex by property " + propertyKey, e);
            }
        }
        return verticesToReturn;
    }

    @Override
    public Vertex getVertexByProperty(String label, String propertyKey, String propertyValue) throws  GraphException {
        List<Vertex> vertices = getVerticesByProperty(label, propertyKey, propertyValue);
        Vertex vertexToReturn;
        if (!vertices.isEmpty()) {
            // if multiple vertices are retrieved, return the "1st" vertex in the list.
            vertexToReturn = vertices.get(0);
        } else {
            vertexToReturn = (Vertex) new Vertex().id(ERROR_DB_ID).putPropertiesMapItem(propertyKey, propertyValue).error(
                    String.format("No vertex found with label:'%s' and property: '%s':'%s' was found in the database.",
                            label, propertyKey, propertyValue));
        }
        return vertexToReturn;
    }

    @Override
    public Vertex getUniqueVertexByProperty(String label, String propertyKey, String propertyValue) throws GraphException {
        List<Vertex> vertices = getVerticesByProperty(label, propertyKey, propertyValue);
        Vertex vertexToReturn;
        if (vertices.size() == 1) {
            vertexToReturn = vertices.get(0);
        } else {
            vertexToReturn = (Vertex) new Vertex().id(ERROR_DB_ID).error(
                    String.format("One vertex with label:'%s' and property: '%s':'%s' should exist in the database but %d were found.",
                            label, propertyKey, propertyValue, vertices.size()));
        }
        return vertexToReturn;
    }

    @Override
    public boolean vertexExists(String label, Map<String, String> searchCriteria) throws GraphException {
        return (!getVertices(label, searchCriteria).isEmpty());
    }

    @Override
    public boolean vertexExistsByProperty(String label, String propertyKey, String propertyValue) throws GraphException {
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put(propertyKey, propertyValue);
        return vertexExists(label, searchCriteria);
    }

    @Override
    public List<Vertex> getVerticesByEdgeLabel(String vertexID, String edgeLabel) throws GraphException {
        return GraphConversion
            .convertMapsToVertices(driver.getVerticesByEdgeLabel(vertexID, edgeLabel));
    }

    @Override
    public List<Path> getOutgoingPaths(String vertexID, int depth) throws GraphException {
        List<List<Map<String, String>>> pathsList = driver.getOutgoingPaths(vertexID, depth);
        return GraphConversion.convertMapsListsToPaths(pathsList);
    }

    @Override
    public List<Path> getOutgoingPaths(String vertexID) throws GraphException {
        return getOutgoingPaths(vertexID, -1);
    }

    @Override
    public List<Edge> getEdges(String label, Map<String, String> searchCriteria) throws GraphException {
        return GraphConversion.convertMapsToEdges(driver.getEdges(label, searchCriteria));
    }

    @Override
    public Vertex addVertex(Vertex vertexToAdd) {
        return addVertex(vertexToAdd.getLabel(), vertexToAdd.getPropertiesMap());
    }

    @Override
    public List<Edge> getEdges() throws GraphException {
        return getEdges(null, null);
    }

    @Override
    public Vertex addVertex(String label, Map<String, String> properties) {
        Vertex vertexToReturn = GraphConversion.convertMapToVertex(driver.addVertex(label, properties));
        if (GraphServiceUtils.isValidVertex(vertexToReturn)) {
            addedVertexIds.add(vertexToReturn.getId());
        } else {
            String errorMessage = String.format("Failed to insert vertex(label:'%s', properties: %s): %s",
                    label, properties, vertexToReturn.getError());
            vertexToReturn.setError(errorMessage);
        }
        return vertexToReturn;
    }

    @Override
    public Edge addEdge(Edge edge) {
        return addEdge(edge.getLabel(), edge.getFromVertexID(), edge.getToVertexID(), edge.getPropertiesMap());
    }

    @Override
    public Edge addEdge(String label, Vertex fromVertex, Vertex toVertex) {
        return addEdge(label, fromVertex.getId(), toVertex.getId(), new HashMap<>());
    }

    @Override
    public Edge addEdge(String label, Vertex fromVertex, Vertex toVertex, Map<String, String> propertiesMap) {
        return addEdge(label, fromVertex.getId(), toVertex.getId(), propertiesMap);
    }

    @Override
    public Edge addEdge(String label, String fromVertexId, String toVertexId) {
        return addEdge(label, fromVertexId, toVertexId, new HashMap<>());
    }

    @Override
    public Edge addEdge(String label, String fromVertexId, String toVertexId, Map<String, String> propertiesMap) {
        Edge newEdge = GraphConversion.convertMapToEdge(driver.addEdgeToVertex(label,
                fromVertexId, toVertexId, propertiesMap));
        if (GraphServiceUtils.isValidEdge(newEdge)) {
            addedEdgeIds.add(newEdge.getId());
        } else {
            String errorMsg = String.format("Failed to insert edge with label:'%s', between vertices with IDs: '%s' and '%s'.",
                label, fromVertexId, toVertexId);
            newEdge.setError(errorMsg);
        }
        return newEdge;
    }

    @Override
    public List<Edge> addEdges(String label, String fromVertexId, List<String> toVertexIds) {
        List<Edge> edgesToReturn = new ArrayList<>();
        for (String curToVertexId: toVertexIds) {
            Edge curEdge = addEdge(label, fromVertexId, curToVertexId);
            edgesToReturn.add(curEdge);
        }
        return edgesToReturn;
    }



    @Override
    public void deleteVertices(@NotNull @Valid List<String> ids) {
        if (!ids.isEmpty()) {
            for (String id : ids) {
                this.driver.deleteVertex(id);
            }
        }
    }

    @Override
    public void deleteEdges(@NotNull @Valid List<String> ids) {
        if (!ids.isEmpty()) {
            for (String id : ids) {
                this.driver.deleteEdge(id);
            }
        }
    }

    @Override
    public void deleteGraph() throws GraphException {
        for (Edge edge: this.getEdges()) {
            this.driver.deleteEdge(edge.getId());
        }
        for (Vertex vertex: this.getVertices()) {
            this.driver.deleteVertex(vertex.getId());
        }
    }
}
