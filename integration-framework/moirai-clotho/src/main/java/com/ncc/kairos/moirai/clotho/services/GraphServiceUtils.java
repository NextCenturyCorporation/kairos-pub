package com.ncc.kairos.moirai.clotho.services;

import com.ncc.kairos.moirai.clotho.model.Edge;
import com.ncc.kairos.moirai.clotho.model.Vertex;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.ncc.kairos.moirai.clotho.resources.GraphConstants.ERROR_DB_ID;

public final class GraphServiceUtils {

    private GraphServiceUtils() {
        throw new IllegalStateException("Utility class, not to be instanced.");
    }

    // Used when the vertex is not required and allowed to be empty for an object; such as privateData
    public static boolean isValidVertex(Vertex vertex) {
        return vertex != null && vertex.getId() != null && !vertex.getId().equals(ERROR_DB_ID) && StringUtils.isAllEmpty(vertex.getError());
    }

    public static boolean isValidEdge(Edge edge) {
        return edge != null && !edge.getId().equals(ERROR_DB_ID) && StringUtils.isAllEmpty(edge.getError());
    }

    public static List<String> getErrorsFromVertices(List<Vertex> vertices) {
        ArrayList<String> errorsToReturn = new ArrayList<>();
        vertices.forEach(vertex -> {
            if (vertex != null && !isValidVertex(vertex) && !StringUtils.isAllEmpty(vertex.getError())) {
                errorsToReturn.add(vertex.getError());
            }
        });
        return errorsToReturn;
    }

    public static List<String> getErrorsFromEdges(List<Edge> edges) {
        ArrayList<String> errorsToReturn = new ArrayList<>();
        edges.forEach(edge -> {
            if (edge != null && !isValidEdge(edge) && !StringUtils.isAllEmpty(edge.getError())) {
                errorsToReturn.add(edge.getError());
            }
        });
        return errorsToReturn;
    }


}
