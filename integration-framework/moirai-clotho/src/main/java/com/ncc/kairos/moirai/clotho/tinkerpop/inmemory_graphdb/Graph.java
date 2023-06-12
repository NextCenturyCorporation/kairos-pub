package com.ncc.kairos.moirai.clotho.tinkerpop.inmemory_graphdb;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Graph {
    final Map<String, GraphVertex> vertices;
    final Map<String, GraphEdge> edges;

    public Graph() {
        vertices = new HashMap<>();
        edges = new HashMap<>();
    }

    public void reset() {
        edges.values().forEach(GraphEdge::cleanReferences);
        edges.clear();
        vertices.clear();
    }

    public GraphVertex addVertex(Map<String, String> properties) {
        GraphVertex newVertex = new GraphVertex(properties);
        vertices.put(newVertex.getId(), newVertex);
        return newVertex;
    }

    public void deleteVertex(String id) {
        GraphVertex vertexToDelete = getVertex(id);
        if (vertexToDelete.isConnected()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can't not delete vertex with edges" + id);
        }
        vertices.remove(id);
    }

    public GraphVertex getVertex(String id) {
        if (!vertices.containsKey(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Can not find vertex with id " + id);
        }
        return vertices.get(id);
    }

    public List<GraphVertex> getVertices(Map<String, String> searchCriteria) {
        return vertices.values().stream()
                .filter(vertex -> {
                    for (String key : searchCriteria.keySet()) {
                        if (!vertex.getProperty(key).equals(searchCriteria.get(key))) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public GraphEdge addEdge(String fromVertexId, String toVertexId, Map<String, String> properties) {
        GraphVertex fromVertex = getVertex(fromVertexId);
        GraphVertex toVertex = getVertex(toVertexId);
        return addEdge(fromVertex, toVertex, properties);
    }

    public GraphEdge addEdge(GraphVertex fromVertex, GraphVertex toVertex, Map<String, String> properties) {
        GraphEdge newE = new GraphEdge(fromVertex, toVertex, properties);
        edges.put(newE.getId(), newE);
        return newE;
    }

    public void deleteEdge(String id) {
        GraphEdge edgeToDelete = getEdge(id);
        edgeToDelete.cleanReferences();
        edges.remove(id);
    }

    public GraphEdge getEdge(String id) {
        if (!edges.containsKey(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Can not find edge with id " + id);
        }
        return edges.get(id);
    }

    public List<GraphEdge> getEdges(Map<String, String> searchCriteria) {
        return edges.values().stream()
                .filter(edge -> {
                    for (Map.Entry<String, String> entry : searchCriteria.entrySet()) {
                        if (!edge.getProperty(entry.getKey()).equals(entry.getValue())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
}
