package com.ncc.kairos.moirai.clotho.utilities;

import com.ncc.kairos.moirai.clotho.model.Edge;
import com.ncc.kairos.moirai.clotho.model.Vertex;
import com.ncc.kairos.moirai.clotho.model.Path;
import com.ncc.kairos.moirai.clotho.resources.GraphConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class GraphConversion {

    protected GraphConversion() {
        // Nothing to do here.
    }

    public static void addIfNotEmpty(Map<String, String> map, String key, String value) {
        if (map != null && !StringUtils.isAllEmpty(key) && !StringUtils.isAllEmpty(value)) {
            map.put(key, value);
        }
    }

    public static void addIfNotEmpty(Map<String, String> map, String key, List<String> values) {
        if (values != null) {
            String[] strArr = new String[values.size()];
            addIfNotEmpty(map, key, convertStringArrayToString(values.toArray(strArr)));
        }
    }

    /**
     * Convert an array of strings into a single string for storage.
     * Current format: [stringA, stringB, stringC...], note the space after each comma
     * Certain fields that have cardinality 1+ will leverage this utility for storage in db.
     */
    public static String convertStringArrayToString(String[] strArr) {
        // NOTE: Will Use the ArrayList.toString() for now, but subject to change
        return Arrays.asList(strArr).toString();
    }

    /**
     * Convert a string in the format of ["string1", "string2"...] into a string array
     * NOTE: The format of how string arrays as string may change and so the parsing logic may change also.
     *  Current format: delimiter is comma
     */
    public static String[] convertStringToStringArray(String str) {
        if (StringUtils.isAllEmpty(str)) {
            return new String[0];
        }
        // Parse the string, assuming Array.toString() format
        // NOTE: "return str.trim().replaceAll("[\\[\\]]", "").split("\\s*,\\s*");" works, but can lead to a DoS attack per Sonarqube.
        String str2 = str.trim().replaceAll("[\\[\\]]", "");
        String[] returnVal = str2.split(",");
        for (int i = 0; i < returnVal.length; i++) {
            returnVal[i] = returnVal[i].trim();
        }
        return returnVal;
    }

    /**
     * Given a list of maps, return a list of the @Vertex(REST-API-layer) along with their respective properties.
     */
    public static List<Vertex> convertMapsToVertices(List<Map<String, String>> mapsList) {
        ArrayList<Vertex> vertexArrL = new ArrayList<>();

        // Iterate through list of vertices and add to array list to be returned.
        for (Map<String, String> curMap : mapsList) {
            vertexArrL.add(convertMapToVertex(curMap));
        }
        return vertexArrL;
    }

    /**
     * Given a single map, return a @Vertex(REST-API-layer) object.
     */
    public static Vertex convertMapToVertex(Map<String, String> vertexMap) {
        // Initialize and populate a new Vertex(REST-Layer) object
        Vertex curVert = new Vertex();

        // Iterate map entry and add as property to vertex
        for (Map.Entry<String, String> curProperty : vertexMap.entrySet()) {
            // Add the current property to the properties map
            String curKey = curProperty.getKey();
            String curVal = curProperty.getValue();

            // id and label are special properties on the Vertex(REST-layer) object
            switch (curKey) {
                case GraphConstants.ID:
                    curVert.setId(curVal);
                    break;
                case GraphConstants.LABEL:
                    curVert.setLabel(curVal);
                    break;
                case GraphConstants.ERROR:
                    curVert.setError(curVal);
                    break;
                default:
                    curVert.putPropertiesMapItem(curKey, curVal);
                    break;
            }
        }
        return curVert;
    }

    /**
     * returns a list of edges given a list of edge maps.
     *
     * @param listOfEdgeMaps List of @Edge in a map format
     * @return List of @Edge
     */
    public static List<Edge> convertMapsToEdges(List<Map<String, String>> listOfEdgeMaps) {
        List<Edge> edgeList = new ArrayList<>();
        for (Map<String, String> edge : listOfEdgeMaps) {
            edgeList.add(convertMapToEdge(edge));
        }
        return edgeList;
    }

    /**
     * Given a generic map, return an @Edge(REST-API-layer) object.
     */
    public static Edge convertMapToEdge(Map<String, String> edgeMap) {
        // Initialize and populate a new Edge(REST-layer) object
        Edge curEdge = new Edge();

        for (Map.Entry<String, String> curProperty : edgeMap.entrySet()) {
            String curKey = curProperty.getKey();
            String curVal = curProperty.getValue();

            // id, fromVertexID, toVertexID and label are unique properties on an Edge
            switch (curKey) {
                case GraphConstants.ID:
                    curEdge.setId(curVal);
                    break;
                case GraphConstants.LABEL:
                    curEdge.setLabel(curVal);
                    break;
                case GraphConstants.FROM_VERTEX_ID:
                    curEdge.setFromVertexID(curVal);
                    break;
                case GraphConstants.TO_VERTEX_ID:
                    curEdge.setToVertexID(curVal);
                    break;
                default:
                    curEdge.putPropertiesMapItem(curKey, curVal);
                    break;
            }
        }
        return curEdge;
    }

    /**
     * Parses search criteria in a map format.
     *
     * @param searchCriteria comma delimited list string of Key Value pairs
     * @return Map of delimited string
     */
    public static Map<String, String> searchCriteriaToMap(String searchCriteria) {
        Map<String, String> searchCriteriaMap = new HashMap<>();
        if (searchCriteria != null && searchCriteria.length() > 0) {
            String[] splitCriteria = searchCriteria.split(",");

            for (String unprocessedCriteria : splitCriteria) {
                String criteriaKey = unprocessedCriteria.split("=")[0];
                String criteriaValue = unprocessedCriteria.split("=")[1];

                if (searchCriteriaMap.containsKey(criteriaKey)) {
                    String newValue = searchCriteriaMap.get(criteriaKey) + "," + criteriaValue;
                    searchCriteriaMap.put(criteriaKey, newValue);
                } else {
                    searchCriteriaMap.put(criteriaKey, criteriaValue);
                }
            }
        }
        return searchCriteriaMap;
    }

    public static List<Path> convertMapsListsToPaths(List<List<Map<String, String>>> listOfMapsLists) {
        List<Path> pathsToReturn = new ArrayList<>();
        // Iterate thru each maps-list and convert each element into either a vertex or edge model.
        for (List<Map<String, String>> curMapsList: listOfMapsLists) {
            String curElementType = GraphConstants.VERTEX;
            Path curPath = new Path();
            for (Map<String, String> curMap: curMapsList) {
                if (curElementType.equals((GraphConstants.VERTEX))) {
                    Vertex curVert = convertMapToVertex(curMap);
                    curPath.addPathItem(curVert);
                    curElementType = GraphConstants.EDGE;
                } else {
                    Edge curEdge = convertMapToEdge(curMap);
                    curPath.addPathItem(curEdge);
                    curElementType = GraphConstants.VERTEX;
                }
            }
            pathsToReturn.add(curPath);
        }
        return pathsToReturn;
    }

}
