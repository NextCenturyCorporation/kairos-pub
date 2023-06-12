package com.ncc.kairos.moirai.clotho.utilities;

import com.ncc.kairos.moirai.clotho.resources.GraphConstants;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.util.*;

public final class GremlinConversion {

    private GremlinConversion() {
        throw new IllegalStateException("Utility class, not to be instanced.");
    }

    /**
     * Convert an iterator over Map< Obj, Obj > to a List of Map< String, String >.
     * Used by several methods in GremlinDriver, including getEdges() and getVertices().
     * NOTE: Determine if this can be replaced by "convertVertexToMap" function; Cast objects to Vertex type.
     */
    public static List<Map<String, String>> convertObjectMapsToStringMaps(Iterator<Map<Object, Object>> objMapsIter) {
        // Convert the iterator over vertices(Map<Obj,Obj>) to a list of Map<String,String> for the controller to process
        List<Map<String, String>> verticesListToReturn = new ArrayList<>();
        Map<Object, Object> curMapOfObjects;

        // See KAIR-805: implement this for edges as well. Need to include FROM and TO vertexes

        // Iterate through each vertex (Map<Object,Object) and create the corresponding Map<String,String>
        while (objMapsIter.hasNext()) {
            Map<String, String> curMapOfStrings = new HashMap<>();
            curMapOfObjects = objMapsIter.next();

            // Convert each entry from <Object, Object> => <String, String>
            for (Map.Entry<Object, Object> curMapEntry : curMapOfObjects.entrySet()) {
                curMapOfStrings.put(curMapEntry.getKey().toString(), curMapEntry.getValue().toString());
            }
            // Add converted map to list to be returned to controller
            verticesListToReturn.add(curMapOfStrings);
        }
        return verticesListToReturn;
    }

    /**
     * Convert the path-elements in each of the paths into Map(String, String).
     *
     */
    public static List<List<Map<String, String>>> getListsOfMapsListFromPaths(List<Path> paths) {
        List<List<Map<String, String>>> listsOfVertexMapsToReturn = new ArrayList<>();

        // Iterate through the paths list and convert each object in each path into Maps respectively.
        for (Path curPath : paths) {
            List<Map<String, String>> curListOfMaps = getMapsFromPath(curPath);
            listsOfVertexMapsToReturn.add(curListOfMaps);
        }
        return listsOfVertexMapsToReturn;
    }

    /**
     * Return a list of vertex maps (Map(String, String)) in a given Path.
     * Used as a helper method by getVertexMapsFromPaths
     */
    public static List<Map<String, String>> getMapsFromPath(Path path) {
        List<Map<String, String>> mapsListToReturn = new ArrayList<>();

        try {
            Iterator<Object> pathElementIter = path.iterator();
            // Iterate through the vertices contained in the path and convert to Maps
            // **Assume that each path starts with a vertex object and then alternates edge-vertex until the last element.
            String curElement = GraphConstants.VERTEX;
            while (pathElementIter.hasNext()) {
                if (curElement.equals(GraphConstants.VERTEX)) {
                    mapsListToReturn.add(convertVertexToMap((Vertex) pathElementIter.next()));
                    curElement = GraphConstants.EDGE;
                } else {
                    mapsListToReturn.add(convertEdgeToMap((Edge) pathElementIter.next()));
                    curElement = GraphConstants.VERTEX;
                }
            }
            return mapsListToReturn;
        } catch (Exception e) {
            // NOTE: Create uniform exception handling
            List<Map<String, String>> errorList = new ArrayList<>();
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put(GraphConstants.ERROR, e.getMessage());
            errorMap.put(GraphConstants.ID, GraphConstants.ERROR_DB_ID);
            errorList.add(errorMap);
            return errorList;
        }
    }

    /**
     * Convert a Gremlin-Vertex object into a Map(String,String) for processing purposes.
     *
     */
    public static Map<String, String> convertVertexToMap(Vertex vertex) {
        try {
            Map<String, String> vertexMapToReturn = new HashMap<>();
            // Populate vertex-unique properties
            vertexMapToReturn.put(GraphConstants.GRAPH_ELEMENT_TYPE, GraphConstants.VERTEX);
            vertexMapToReturn.put(GraphConstants.ID, vertex.id().toString());
            vertexMapToReturn.put(GraphConstants.LABEL, vertex.label());

            // Iterate through the Vertex properties and add to the Map
            Iterator<VertexProperty<String>> vertexPropertyIter = vertex.properties();
            while (vertexPropertyIter.hasNext()) {
                VertexProperty<String> curProperty = vertexPropertyIter.next();
                vertexMapToReturn.put(curProperty.key(), curProperty.value());
            }
            return vertexMapToReturn;
        } catch (Exception ex) {
            // NOTE: create uniform exception-handling
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put(GraphConstants.ERROR, ex.getMessage());
            errorMap.put(GraphConstants.ID, GraphConstants.ERROR_DB_ID);
            return errorMap;
        }
    }

    public static Map<String, String> convertEdgeToMap(Edge edge) {
        try {
            Map<String, String> edgeMapToReturn = new HashMap<>();
            // Populate edge-unique properties
            edgeMapToReturn.put(GraphConstants.GRAPH_ELEMENT_TYPE, GraphConstants.EDGE);
            edgeMapToReturn.put(GraphConstants.ID, edge.id().toString());
            edgeMapToReturn.put(GraphConstants.LABEL, edge.label());
            edgeMapToReturn.put(GraphConstants.FROM_VERTEX_ID, edge.outVertex().id().toString());
            edgeMapToReturn.put(GraphConstants.TO_VERTEX_ID, edge.inVertex().id().toString());

            // Iterate through the Vertex properties and add to the Map
            Iterator<Property<String>> edgePropertyIter = edge.properties();
            while (edgePropertyIter.hasNext()) {
                Property<String> curProperty = edgePropertyIter.next();
                edgeMapToReturn.put(curProperty.key(), curProperty.value());
            }

            return edgeMapToReturn;
        } catch (Exception ex) {
            // NOTE: create uniform exception-handling
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put(GraphConstants.ERROR, ex.getMessage());
            errorMap.put(GraphConstants.ID, GraphConstants.ERROR_DB_ID);
            return errorMap;
        }
    }



}
