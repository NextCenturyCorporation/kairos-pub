package com.ncc.kairos.moirai.clotho.services;

import com.ncc.kairos.moirai.clotho.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;

public final class DefinitionServiceUtils {

    private DefinitionServiceUtils() {
        throw new IllegalStateException("Utility class, not to be instanced.");
    }

    // Used to process step-orders
    public static Map<String, String> getStepAtIdToVertexIdMap(List<Step> steps) {
        Map<String, String> atIdToVertexIdMap = new HashMap<>();
        for (Step curStep: steps) {
            if (!curStep.getAtId().equals(ERROR_DB_ID)) {
                atIdToVertexIdMap.put(curStep.getAtId(), curStep.getAtId());
            }
        }
        return  atIdToVertexIdMap;
    }

    // Used to process entity-relations
    public static Map<String, String> getParticipantIdToVertexIdMap(List<Slot> slots) {
        Map<String, String> atIdToVertexIdMap = new HashMap<>();
        for (Slot curSlot: slots) {
            if (curSlot.getAtId() != null && !curSlot.getAtId().equals(ERROR_DB_ID)) {
                // NOTE: should probably be an exception/warning if id is null/empty.
                atIdToVertexIdMap.put(curSlot.getAtId(), curSlot.getAtId());
            }
        }
        return  atIdToVertexIdMap;
    }

    // Used to retrieve all slots across all steps of a given schema.
    public static List<Slot> getSlotsFromSteps(List<Step> steps) {
        List<Slot> slotsToReturn = new ArrayList<>();
        for (Step curStep: steps) {
            slotsToReturn.addAll(curStep.getParticipants());
        }
        return slotsToReturn;
    }

    // Used by groupPathsByNextVertex function to add new paths without overriding existing entries.
    public static void safeAddPathEntry(Map<String, List<Path>> pathMap, String key, Path pathToAdd) {
        if (pathMap != null) {
            if (pathMap.containsKey(key)) {
                List<Path> existingPathList = pathMap.get(key);
                if (existingPathList == null) {
                    existingPathList = new ArrayList<>();
                    existingPathList.add(pathToAdd);
                    pathMap.put(key, existingPathList);
                } else {
                    existingPathList.add(pathToAdd);
                }
            } else {
                List<Path> newPathList = new ArrayList<>();
                newPathList.add(pathToAdd);
                pathMap.put(key, newPathList);
            }
        }
    }

    // Ad-hoc function for removing duplicate step-step/slot-slot paths from a group of paths.
    // NOTE: will remove after updating getOutgoingPaths query
    public static Map<String, List<Path>> removeDuplicatePaths(Map<String, List<Path>> groupedPaths, String nextVertexLabel) {
        Map<String, List<Path>> groupedPathsToReturn = new HashMap<>();
        List<String> encounteredStepEdgeIds = new ArrayList<>();

        // Assume that paths have a step vertex as the root
        if (groupedPaths == null) {
            return groupedPathsToReturn;
        }

        for (Map.Entry<String, List<Path>> curEntry: groupedPaths.entrySet()) {
            List<Path> curPathListToAdd = new ArrayList<>();

            for (Path curPath: curEntry.getValue()) {
                List<GraphElementType> curPathList = curPath.getPath();
                // Ignore paths that have less than 3 elements
                if (curPath.getPath().size() >= 3) {
                    // First determine if the edge leads to a Step vertex
                    String curNextEdgeId = curPathList.get(1).getId();
                    String curNextStepLabel = curPathList.get(2).getLabel();
                    if (!curNextStepLabel.equals(nextVertexLabel) || !encounteredStepEdgeIds.contains(curNextEdgeId)) {
                        encounteredStepEdgeIds.add(curNextEdgeId);
                        curPathListToAdd.add(curPath);
                    } // if the next vertex is a step/slot and the edge has already been added, then ignore current path
                }
            }
            groupedPathsToReturn.put(curEntry.getKey(), curPathListToAdd);
        }

        return groupedPathsToReturn;
    }


    public static Map<String, List<Path>> groupPathsByNextEdge(List<Path> paths) {
        Map<String, List<Path>> groupedPathsToReturn = new HashMap<>();
        // Assume: 1st-element: root vertex, 2nd-element: edge, 3rd-element is "the Next Vertex".
        // Retrieve the id of the 2nd element/ "next-edge"
        for (Path curPath: paths) {
            List<GraphElementType> curPathList = curPath.getPath();
            // Each Path MUST HAVE AT LEAST 3 elements: root-vertex -> edge -> next-vertex
            if (curPathList.size() >= 3) {
                String curEdgeId = curPathList.get(1).getId();
                DefinitionServiceUtils.safeAddPathEntry(groupedPathsToReturn, curEdgeId, curPath);
            } // NOTE: should probably be an exception/warning if path-size is less than 3
        }
        return groupedPathsToReturn;
    }

    // Used to represent traversing a path and arriving at a new root vertex.
    // Beginning elements are removed from the path based on specified number of steps to take.
    public static Path traversePath(Path path, int numSteps) {
        Path pathToReturn = new Path().path(new ArrayList<>());
        List<GraphElementType> pathToTraverse = path.getPath();
        // Verify that the path has at least 1 element
        // Verify that the number of size is >= 2*steps + 1. Otherwise the path is not long enough.
        if (pathToTraverse != null && pathToTraverse.size() > 0 && pathToTraverse.size() >= (2 * numSteps + 1)) {
            pathToReturn.setPath(pathToTraverse.subList(2 * numSteps, pathToTraverse.size()));
        }
        return pathToReturn;
    }

    public static List<Path> traversePaths(List<Path> paths, int numSteps) {
        List<Path> pathsToReturn = new ArrayList<>();
        if (paths != null) {
            for (Path curPath : paths) {
                pathsToReturn.add(traversePath(curPath, numSteps));
            }
        }
        return pathsToReturn;
    }

    public static Map<String, List<Path>> traversePaths(Map<String, List<Path>> groupedPaths, int numSteps) {
        Map<String, List<Path>> groupedPathsToReturn = new HashMap<>();
        if (groupedPaths != null) {
            for (Map.Entry<String, List<Path>> curEntry: groupedPaths.entrySet()) {
                List<Path> curTraversedPath = traversePaths(curEntry.getValue(), numSteps);
                groupedPathsToReturn.put(curEntry.getKey(), curTraversedPath);
            }
        }
        return groupedPathsToReturn;
    }

    public static String getNextEdgeLabel(Path path) {
        if (path != null && path.getPath().size() > 2) {
            return path.getPath().get(1).getLabel();
        } else {
            return "";
        }
    }

    public static String getNextEdgeLabel(List<Path> paths) {
        if (paths != null && paths.size() > 0) {
            return getNextEdgeLabel(paths.get(0));
        } else {
            return "";
        }
    }

    public static Edge getNextEdge(Path path) {
        if (path != null && path.getPath().size() > 2) {
            return (Edge) path.getPath().get(1);
        } else {
            return null;
        }
    }

    public static Edge getNextEdge(List<Path> paths) {
        if (paths != null && paths.size() > 0) {
            return getNextEdge(paths.get(0));
        } else {
            return null;
        }
    }

    public static Vertex getNextVertex(Path path, int degreesFromRoot) {
        if (path != null && path.getPath() != null) {
            List<GraphElementType> pathElements = path.getPath();
            if (pathElements.size() >= 2 * degreesFromRoot + 1) {
                return (Vertex) path.getPath().get(2 * degreesFromRoot);
            }
        }
        return null;
    }

    public static Vertex getNextVertex(List<Path> paths, int degreesFromRoot) {
        if (paths != null && paths.size() > 0) {
            return getNextVertex(paths.get(0), degreesFromRoot);
        }
        return null;
    }

}
