package com.ncc.kairos.moirai.clotho.tinkerpop;

import com.ncc.kairos.moirai.clotho.exceptions.DriverException;
import com.ncc.kairos.moirai.clotho.tinkerpop.inmemory_graphdb.Graph;
import com.ncc.kairos.moirai.clotho.tinkerpop.inmemory_graphdb.GraphComponent;
import com.ncc.kairos.moirai.clotho.tinkerpop.inmemory_graphdb.GraphEdge;
import com.ncc.kairos.moirai.clotho.tinkerpop.inmemory_graphdb.GraphVertex;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryDriver extends AbstractGremlinDriver {

    private static final String LABEL_KEY = "label";
    Graph graph;

    @Override
    public void open() throws DriverException {
        graph = new Graph();
    }

    @Override
    public List<Map<String, String>> getVertices(String label, Map<String, String> searchCriteria) {
        Map<String, String> combinedCriteria = new HashMap<>(searchCriteria);
        combinedCriteria.put(LABEL_KEY, label);
        return graph.getVertices(combinedCriteria).stream()
                .map(GraphComponent::getProperties)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> getEdges(String label, Map<String, String> searchCriteria) {
        Map<String, String> combinedCriteria = new HashMap<>(searchCriteria);
        combinedCriteria.put(LABEL_KEY, label);
        return graph.getEdges(combinedCriteria).stream()
                .map(GraphComponent::getProperties)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> addVertices(String label, List<Map<String, String>> propertiesMapList) {
        return propertiesMapList.stream()
                .map(propertiesMap -> addVertex(label, propertiesMap))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> addVertex(String label, Map<String, String> properties) {
        Map<String, String> combinedProperties = new HashMap<>(properties);
        combinedProperties.put(LABEL_KEY, label);
        return graph.addVertex(combinedProperties).getProperties();
    }

    @Override
    public void deleteVertex(String id) {
        graph.deleteVertex(id);
    }

    @Override
    public Map<String, String> addEdgeToVertex(String label, String fromVertexId, String toVertexId, Map<String, String> properties) {
        Map<String, String> combinedProperties = new HashMap<>(properties);
        combinedProperties.put(LABEL_KEY, label);
        return graph.addEdge(fromVertexId, toVertexId, combinedProperties).getProperties();
    }

    @Override
    public void deleteEdge(String id) {
        graph.deleteEdge(id);
    }


    @Override
    public List<Map<String, String>> getVerticesByEdgeLabel(String vertexID, String edgeLabel) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Null Driver doesn't implement getVerticesByEdgeLabel");
    }

    @Override
    public List<List<Map<String, String>>> getOutgoingPaths(String sourceVertexID, int numSteps) {
        GraphVertex sourceVertex = graph.getVertex(sourceVertexID);
        List<GraphComponent> path = new ArrayList<>();
        path.add(sourceVertex);
        return getOutgoingPaths(sourceVertex, path).stream()
                .map(listOfGraphComponents ->
                        listOfGraphComponents.stream()
                                .map(GraphComponent::getProperties)
                                .collect(Collectors.toList())
                ).collect(Collectors.toList());
    }

    public List<List<GraphComponent>> getOutgoingPaths(GraphVertex currentVertex, List<GraphComponent> parentChain) {
        List<List<GraphComponent>> paths = new ArrayList<>();
        if (currentVertex.getOutgoing().size() == 0) {
            paths.add(parentChain);
        } else {
            for (GraphEdge edge : currentVertex.getOutgoing()) {
                List<GraphComponent> subchain = new ArrayList<>(parentChain);
                subchain.add(edge);
                subchain.add(edge.getTo());
                paths.addAll(getOutgoingPaths(edge.getTo(), subchain));
            }
        }
        return paths;
    }

    @Override
    public String runSparqlQuery(String filter) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "InMemoryDriver doesn't implement runSparqlQuery");
    }

    @Override
    public void close() throws DriverException {
        if (graph != null) {
            graph.reset();
            graph = null;
        }
    }

}
