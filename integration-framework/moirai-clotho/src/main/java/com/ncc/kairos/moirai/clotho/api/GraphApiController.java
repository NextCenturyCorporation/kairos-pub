package com.ncc.kairos.moirai.clotho.api;

import com.ncc.kairos.moirai.clotho.interfaces.IGraphService;
import com.ncc.kairos.moirai.clotho.model.Edge;
import com.ncc.kairos.moirai.clotho.model.Graph;
import com.ncc.kairos.moirai.clotho.model.Vertex;
import com.ncc.kairos.moirai.clotho.utilities.GraphConversion;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
public class GraphApiController implements GraphApi {

    private static final Logger log = LoggerFactory.getLogger(GraphApiController.class);

    @Autowired
    private IGraphService graphService;

    @Override
    public ResponseEntity<List<Vertex>> getVertices(
            @NotNull @ApiParam(value = "label value for vertex", required = true) @Valid @RequestParam(value = "label", required = true) String label,
            @ApiParam(value = "a map of key-val pairs to use as search criteria on vertices") @Valid @RequestParam(value = "searchCriteria", required = false) String searchCriteria) {
        try {
            graphService.open();
            Map<String, String> searchCriteriaMap = GraphConversion.searchCriteriaToMap(searchCriteria);
            List<Vertex> searchResultVertices = graphService.getVertices(label, searchCriteriaMap);
            HttpStatus statusToReturn = ApiUtil.getResponseFromList(searchResultVertices);
            return new ResponseEntity<>(searchResultVertices, statusToReturn);
        } catch (Exception e) {
            log.error("Failed to fetch vertices", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            graphService.close();
        }
    }

    @Override
    public ResponseEntity<Vertex> insertVertex(
            @NotNull @ApiParam(value = "label value for vertex", required = true) @Valid @RequestParam(value = "label", required = true) String label,
            @ApiParam(value = "json representing the key-val properties of the new vertex to add") @Valid @RequestBody Map<String, String> requestBody) {
        try {
            graphService.open();
            Vertex vertexToInsert = (Vertex) new Vertex().label(label).propertiesMap(requestBody);
            Vertex vertexToReturn = graphService.addVertex(vertexToInsert);
            HttpStatus statusToReturn = ApiUtil.getResponseFromId(vertexToReturn.getId());
            graphService.acceptChanges();
            return new ResponseEntity<>(vertexToReturn, statusToReturn);
        } catch (Exception e) {
            log.error("Failed to insert a vertex", e);
            graphService.rollbackChanges();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            graphService.close();
        }
    }

    @Override
    public ResponseEntity<List<Edge>> getEdges(@NotNull @ApiParam(value = "label value for vertex", required = true)
                                               @Valid @RequestParam(value = "label", required = true) String label,
                                               @ApiParam(value = "a map of key-val pairs to use as search criteria on vertices")
                                               @Valid @RequestParam(value = "searchCriteria", required = false) String searchCriteria) {
        try {
            graphService.open();
            Map<String, String> searchCriteriaMap = GraphConversion.searchCriteriaToMap(searchCriteria);
            List<Edge> edges = graphService.getEdges(label, searchCriteriaMap);
            HttpStatus statusToReturn = ApiUtil.getResponseFromList(edges);
            return new ResponseEntity<>(edges, statusToReturn);
        } catch (Exception e) {
            log.error("Failed to fetch edges", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            graphService.close();
        }
    }

    @Override
    public ResponseEntity<Edge> insertEdge(@NotNull @ApiParam(value = "label/relationship-type", required = true)
                                           @Valid @RequestParam(value = "label", required = true) String label,
                                           @NotNull @ApiParam(value = "ID of parent-vertex (outgoing edge)", required = true)
                                           @Valid @RequestParam(value = "fromVertexID", required = true) String fromVertexID,
                                           @NotNull @ApiParam(value = "ID of child-vertex (incoming edge)", required = true)
                                           @Valid @RequestParam(value = "toVertexID", required = true) String toVertexID,
                                           @ApiParam(value = "json representing a map of key-val pairs to add as properties to the Edge")
                                           @Valid @RequestBody Map<String, String> requestBody) {
        try {
            graphService.open();
            Edge edgeToInsert = (Edge) new Edge().label(label).propertiesMap(requestBody);
            edgeToInsert = edgeToInsert.fromVertexID(fromVertexID).toVertexID(toVertexID);
            Edge edge = graphService.addEdge(edgeToInsert);
            HttpStatus statusToReturn = ApiUtil.getResponseFromId(edge.getId());
            graphService.acceptChanges();
            return new ResponseEntity<>(edge, statusToReturn);
        } catch (Exception e) {
            log.error("Failed to insert edge.", e);
            graphService.rollbackChanges();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            graphService.close();
        }
    }

    @Override
    public ResponseEntity<String> deleteEdges(@NotNull @ApiParam(value = "id value for edge", required = true, defaultValue = "") 
                                            @Valid @RequestParam(value = "ids", required = true, defaultValue = "") List<String> ids) {
        try {
            this.graphService.open();
            this.graphService.deleteEdges(ids);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to delete an edge", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            this.graphService.close();
        }
    }

    @Override
    public ResponseEntity<String> deleteVertices(@NotNull @ApiParam(value = "id value for vertex", required = true, defaultValue = "") 
                                            @Valid @RequestParam(value = "ids", required = true, defaultValue = "") List<String> ids) {
        try {
            this.graphService.open();
            this.graphService.deleteVertices(ids);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NoSuchElementException nsee) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to delete vertices", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            this.graphService.close();
        }
    }

    @Override
    public ResponseEntity<Graph> getGraph() {
        try {
            graphService.open();
            Graph graph = new Graph();
            Map<String, String> idmap = new HashMap<>();

            List<Vertex> vertices = graphService.getVertices();
            for (Vertex v : vertices) {
                idmap.put(v.getId(), v.getPropertiesMap().get("@id"));
                v.setId(v.getPropertiesMap().get("@id"));
                v.getPropertiesMap().remove("@id");
            }
            graph.setVertexArray(vertices);

            List<Edge> edges = graphService.getEdges();
            for (Edge edge : edges) {
                edge.setFromVertexID(idmap.get(edge.getFromVertexID()));
                edge.setToVertexID(idmap.get(edge.getToVertexID()));
            }
            graph.setEdgeArray(edges);

            HttpStatus statusToReturn = ApiUtil.getResponseFromList(graph.getVertexArray());
            return new ResponseEntity<>(graph, statusToReturn);
        } catch (Exception e) {
            log.error("Failed to delete fetch a graph", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to retrieve Graph");
        } finally {
            graphService.close();
        }
    }

    @Override
    public ResponseEntity<Void> insertGraph(
            @ApiParam(value = "delete existing data?") @Valid @RequestParam(value = "overwrite", required = false) Boolean overwrite,
            @ApiParam(value = "json representing a map of key-val pairs to insert as properties to the Edge")  @Valid @RequestBody Graph graph) {
        try {
            graphService.open();
            if (Boolean.TRUE.equals(overwrite)) {
                graphService.deleteGraph();
            }

            Map<String, String> idmap = new HashMap<>();

            for (Vertex vertex : graph.getVertexArray()) {
                vertex.getPropertiesMap().put("@id", vertex.getId());
                idmap.put(vertex.getId(), graphService.addVertex(vertex).getId());
            }
            for (Edge edge : graph.getEdgeArray()) {
                edge.setFromVertexID(idmap.get(edge.getFromVertexID()));
                edge.setToVertexID(idmap.get(edge.getToVertexID()));
                graphService.addEdge(edge);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to insert a graph", e);
            graphService.rollbackChanges();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to insert Graph");
        } finally {
            graphService.close();
        }
    }
}


