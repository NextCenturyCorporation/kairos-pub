package com.ncc.kairos.moirai.clotho.utilities;

import java.util.*;

import org.jgrapht.Graphs;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.ncc.kairos.moirai.clotho.exceptions.GraphException;
import com.ncc.kairos.moirai.clotho.model.SchemaEvent;
import com.ncc.kairos.moirai.clotho.utilities.ksf.validation.JavaDataStructureUtils;
import org.jgrapht.traverse.TopologicalOrderIterator;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.UNIQUE_ID_NUMDIGITS;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.ValidationUtils.safeGetStrings;


public final class GraphTraversal {

    // See discussion in KAIR-1087.  The "orig order strategy" was replaced by the algorithm in KAIR-1087.
    private static final boolean ORIG_ORDER_STRATEGY = false;

    private GraphTraversal() {
        throw new IllegalStateException("Utility class, not to be instantiated.");
    }

    // Return a map containing subgraph-size for all nodes
    public static Map<String, Integer> getSubGraphSizeForAllNodes(Map<String, List<String>> nodeAdjacencyMap) {
        Map<String, Integer> subtreeSizeByNode = new HashMap<>();

        if (nodeAdjacencyMap != null) {
            // Iterate through each node and calculate size
            for (String curNode : nodeAdjacencyMap.keySet()) {
                int curSize = getSubGraphSizeViaDFS(nodeAdjacencyMap, curNode);
                subtreeSizeByNode.put(curNode, curSize);
            }
        }
        return subtreeSizeByNode;
    }


    // Use DFS to determine sub-graph size for single node
    private static int getSubGraphSizeViaDFS(Map<String, List<String>> nodeAdjacencyMap, String rootNodeId) {
        List<String> visitedNodes = new ArrayList<>();
        Deque<String> nodesToVisit = new ArrayDeque<>();

        nodesToVisit.add(rootNodeId);

        while (!nodesToVisit.isEmpty()) {
            String nodeToVisit = nodesToVisit.pop();
            visitedNodes.add(nodeToVisit);

            // visit node: add neighbors to stack
            List<String> adjacentNodes = nodeAdjacencyMap.get(nodeToVisit);
            for (String curAdjacentNode : adjacentNodes) {
                if (!visitedNodes.contains(curAdjacentNode)) {
                    nodesToVisit.push(curAdjacentNode);
                }
            }
        }
        return visitedNodes.size();
    }

    public static Map<String, String> getTopologicalOrderAssignments(Map<String, List<String>> outgoingAdjacenciesMap, Map<String, List<String>> incomingAdjacenciesMap,
                                                                     Map<String, Integer> subGraphSizeMap, Map<String, Integer> stepsOriginalIndexMap) {
        List<List<String>> consolidatedNodes = new ArrayList<>();
        int numNodesProcessed = 0;

        // Process nodes iteratively by processing root nodes for each iteration.
        while (numNodesProcessed < stepsOriginalIndexMap.size()) {
            // Determine root nodes for current iteration
            List<String> curRootNodes = getRootNodes(outgoingAdjacenciesMap);

            // Sort root nodes based on subgraph-size and original-index
            sortRootNodes(curRootNodes, subGraphSizeMap, stepsOriginalIndexMap);

            if (ORIG_ORDER_STRATEGY) {
                // Determine which root nodes to consolidate; Only adjacent elements in list could potentially be consolidated
                List<List<String>> consolidatedNodesSubList = consolidateNodesWithIdenticalNeighbors(curRootNodes, outgoingAdjacenciesMap, incomingAdjacenciesMap);

                // Add consolidated root nodes sublist to the whole list.
                consolidatedNodes.addAll(consolidatedNodesSubList);

                // Remove rootNodes from Map
                for (String curRoot : curRootNodes) {
                    outgoingAdjacenciesMap.remove(curRoot);
                    // increment numNodesProcessed as cur-root nodes are removed from remaining graph.
                    numNodesProcessed++;
                }
            } else {
                // Remove top rootNode from Map
                if (curRootNodes.isEmpty()) {
                    throw new GraphException("Topological sort error: does input SDF have cycles in order relations?");
                }
                String topRoot = curRootNodes.remove(0); // remove top weighted root
                List<String> consolidatedNodesSubList = consolidateNodeWithIdenticalNeighbors(topRoot, curRootNodes, outgoingAdjacenciesMap, incomingAdjacenciesMap);
                consolidatedNodes.add(consolidatedNodesSubList);
                for (String curRoot : consolidatedNodesSubList) {
                    outgoingAdjacenciesMap.remove(curRoot);
                    // increment numNodesProcessed as cur-root nodes are removed from remaining graph.
                    numNodesProcessed++;
                }
            }
        }

        // After all nodes have been processed and added to the list in order, convert to a map designating the order number/range for each step
        return getStepOrderAssignments(consolidatedNodes);
    }

    private static Map<String, String> getStepOrderAssignments(List<List<String>> consolidatedNodesList) {
        Map<String, String> stepOrderAssignmentsToReturn = new HashMap<>();

        int curIndex = 1;

        for (List<String> curGroupOfNodes : consolidatedNodesList) {
            String orderNumberToAssign = Integer.toString(curIndex);

            // Determine if the curGroup is a single node or multiple nodes
            // IMPORTANT: All groups should have AT LEAST ONE node. Any empty nodes will be ignored and not affect count.
            if (!curGroupOfNodes.isEmpty()) {
                if (curGroupOfNodes.size() == 1) {
                    // increment by 1
                    curIndex++;
                } else {
                    // Multiple-nodes group -> order-number will be range
                    int upperLimit = curIndex + curGroupOfNodes.size() - 1;
                    orderNumberToAssign += String.format("-%d", upperLimit);
                    curIndex = upperLimit + 1;
                }
            }

            // Add the stepId -> orderAssignment to map.
            for (String curNode : curGroupOfNodes) {
                stepOrderAssignmentsToReturn.put(curNode, orderNumberToAssign);
            }
        }
        return stepOrderAssignmentsToReturn;
    }

    // Root-node ties are decided by subgraph size and original index
    private static void sortRootNodes(List<String> rootNodesToSort, Map<String, Integer> subGraphSizeMap, Map<String, Integer> stepsOriginalIndexMap) {
        rootNodesToSort.sort((nodeA, nodeB) -> {
            Integer nodeASubgraphSize = subGraphSizeMap.get(nodeA);
            Integer nodeAOriginalIndex = stepsOriginalIndexMap.get(nodeA);
            Integer nodeBSubgraphSize = subGraphSizeMap.get(nodeB);
            Integer nodeBOriginalIndex = stepsOriginalIndexMap.get(nodeB);

            if (nodeASubgraphSize.equals(nodeBSubgraphSize)) {
                return nodeAOriginalIndex.compareTo(nodeBOriginalIndex);
            } else {
                return nodeBSubgraphSize.compareTo(nodeASubgraphSize);
            }
        });
    }

    // Consolidate candidateNodes which have identical set of incoming- and outgoing-neighbors as the targetNode
    private static List<String> consolidateNodeWithIdenticalNeighbors(String targetNode, List<String> candidateNodes, Map<String, List<String>> outgoingAdjacenciesMap,
                                                                             Map<String, List<String>> incomingAdjacenciesMap) {
        List<String> targetGroup = new ArrayList<>();
        targetGroup.add(targetNode);
        for (String curNodeToAdd : candidateNodes) {
            if (!targetNode.equals(curNodeToAdd) &&
                nodeBelongsToNodeGroup(curNodeToAdd, targetGroup, outgoingAdjacenciesMap, incomingAdjacenciesMap)) {
                targetGroup.add(curNodeToAdd);
            }
        }
        return targetGroup;
    }

    // Consolidate nodes which have identical set of incoming-neighbors and outgoing-neighbors respectively
    // List<String> -> List<List<String>>
    private static List<List<String>> consolidateNodesWithIdenticalNeighbors(List<String> nodesToConsolidate, Map<String, List<String>> outgoingAdjacenciesMap,
                                                                             Map<String, List<String>> incomingAdjacenciesMap) {

        List<List<String>> consolidatedNodesSubList = new ArrayList<>();

        // Iterate through list of nodes and add to consolidated list, either add to existing list or a new list.
        for (String curNodeToAdd : nodesToConsolidate) {
            addNodeToConsolidatedList(consolidatedNodesSubList, curNodeToAdd, outgoingAdjacenciesMap, incomingAdjacenciesMap);
        }

        return consolidatedNodesSubList;
    }

    // Utility add-function to determine whether a new node should be added to an existing list with "identical"-nodes or as a new list of its own
    private static void addNodeToConsolidatedList(List<List<String>> consolidatedNodesSubList, String nodeIdToAdd,
                                                  Map<String, List<String>> outgoingAdjacenciesMap, Map<String, List<String>> incomingAdjacenciesMap) {

        if (consolidatedNodesSubList != null && !JavaDataStructureUtils.isNullOrEmptyString(nodeIdToAdd)) {
            // Iterate through existing groups, and determine if node-to-add belongs with any
            for (List<String> curGroup : consolidatedNodesSubList) {
                // Determine if the node-to-add belongs
                if (nodeBelongsToNodeGroup(nodeIdToAdd, curGroup, outgoingAdjacenciesMap, incomingAdjacenciesMap)) {
                    // Add node to curGroup; Should reflect in consolidated-list
                    curGroup.add(nodeIdToAdd);
                    return;
                }
            }

            // If this point is reached in the code, then the node-to-add did not belong to any existing group.
            // Create a new group with the node-to-add and add to consolidated-list
            List<String> newGroup = new ArrayList<>();
            newGroup.add(nodeIdToAdd);
            consolidatedNodesSubList.add(newGroup);
        }
    }

    // Returns a boolean indicating whether a node belongs to a group of nodes (identical incoming-neighbors AND identical outgoing-neighbors)
    private static boolean nodeBelongsToNodeGroup(String node, List<String> nodesGroup,
                                                  Map<String, List<String>> outgoingAdjacenciesMap, Map<String, List<String>> incomingAdjacenciesMap) {
        boolean boolToReturn = false;

        if (nodesGroup != null && !nodesGroup.isEmpty()) {
            // Retrieve first element from node group and compare
            String nodeFromGroup = nodesGroup.get(0);
            Set<String> groupOutgoingNeighbors = new HashSet<>(outgoingAdjacenciesMap.get(nodeFromGroup));
            Set<String> groupIncomingNeighbors = new HashSet<>(incomingAdjacenciesMap.get(nodeFromGroup));

            Set<String> nodeOutgoingNeighbors = new HashSet<>(outgoingAdjacenciesMap.get(node));
            Set<String> nodeIncomingNeighbors = new HashSet<>(incomingAdjacenciesMap.get(node));

            // Check neighbor-equality
            if (groupOutgoingNeighbors.equals(nodeOutgoingNeighbors) && groupIncomingNeighbors.equals(nodeIncomingNeighbors)) {
                boolToReturn = true;
            }
        }
        // Return first node from list; does not matter which since theoretically each node in list should be "identical"
        return boolToReturn;
    }

    private static List<String> getRootNodes(Map<String, List<String>> stepAdjacenciesMap) {
        List<String> rootNodesToReturn = new ArrayList<>();
        Set<String> nodesWithIncomingEdge = new HashSet<>();

        // Add all nodes with incoming edges to a set.
        for (Map.Entry<String, List<String>> entry : stepAdjacenciesMap.entrySet()) {
            nodesWithIncomingEdge.addAll(entry.getValue());
        }

        // Iterate nodes a second time, to determine roots.
        for (String curNodeId : stepAdjacenciesMap.keySet()) {
            if (!nodesWithIncomingEdge.contains(curNodeId)) {
                rootNodesToReturn.add(curNodeId);
            }
        }
        return rootNodesToReturn;
    }

    private static SimpleDirectedGraph<String, DefaultEdge> createOutlinksGraph(List<SchemaEvent> events, List<String> validIds,
            boolean addHierarchicalOutlinks) {

        SimpleDirectedGraph<String, DefaultEdge> outlinksGraph = null;
        try {
            outlinksGraph = new SimpleDirectedGraph<>(DefaultEdge.class);

            // First create nodes from events (with valid ids)
            for (SchemaEvent event : events) {
                if (validIds.contains(event.getAtId())) {
                    outlinksGraph.addVertex(event.getAtId());
                }
            }

            // Then create edges from outlinks and hierarchy; all nodes must have already been created
            for (SchemaEvent event : events) {
                if (validIds.contains(event.getAtId())) {
                    // Add outlinks
                    List<String> outlinks = safeGetStrings(event.getOutlinks());
                    for (String outlink : outlinks) {
                        if (validIds.contains(outlink)) {
                            outlinksGraph.addEdge(event.getAtId(), outlink);
                        }
                    }

                    // Add hierarchical relations
                    outlinks = safeGetStrings(event.getSubgroupEvents());
                    for (String outlink : outlinks) {
                        if (addHierarchicalOutlinks && validIds.contains(outlink)) {
                            outlinksGraph.addEdge(event.getAtId(), outlink);
                        }
                    }
                }
            }
        } catch (IllegalArgumentException iae) { // usually because of self-loops
            return null;
        }
        return outlinksGraph;
    }

    public static List<SchemaEvent> getSortedEvents(List<SchemaEvent> events, Map<String, SchemaEvent> validIdMap) {
        if (events == null || events.isEmpty() || validIdMap == null || validIdMap.isEmpty()) {
            return events;
        }

        SimpleDirectedGraph<String, DefaultEdge> outlinksGraph = createOutlinksGraph(events, List.copyOf(validIdMap.keySet()), true);
        if (outlinksGraph == null || getHasCycles(outlinksGraph)) {
            return events; // can't sort bad graphs
        }

        List<SchemaEvent> sortedEvents = new ArrayList<>();
        TopologicalOrderIterator<String, DefaultEdge> iterator = new TopologicalOrderIterator<>(outlinksGraph,
                new GraphTraversal.SortByIdDigits());
        while (iterator.hasNext()) {
            sortedEvents.add(validIdMap.get(iterator.next()));
        }

        return sortedEvents;
    }

    private static boolean getHasCycles(SimpleDirectedGraph<String, DefaultEdge> outlinksGraph) {
        // Detect cycles
        CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(outlinksGraph);
        return cycleDetector.detectCycles();
    }

    public static boolean getHasCycles(List<SchemaEvent> events, List<String> validIds) {
        if (events == null || events.isEmpty() || validIds == null || validIds.isEmpty()) {
            return false;
        }

        SimpleDirectedGraph<String, DefaultEdge> outlinksGraph = createOutlinksGraph(events, validIds, false);
        return outlinksGraph == null || getHasCycles(outlinksGraph);
    }

    public static boolean isTransitiveReduction(List<SchemaEvent> events, List<String> validIds) {
        if (events == null || events.isEmpty() || validIds == null || validIds.isEmpty()) {
            return true;
        }

        SimpleDirectedGraph<String, DefaultEdge> outlinksGraph = createOutlinksGraph(events, validIds, false);
        if (outlinksGraph == null) {
            return true;
        }

        SimpleDirectedGraph<String, DefaultEdge> reducedGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
        Graphs.addGraph(reducedGraph, outlinksGraph);
        TransitiveReduction.INSTANCE.reduce(reducedGraph);

        return reducedGraph.equals(outlinksGraph);
    }

    // Helper class implementing Comparator interface for SchemaEvent ids
    static class SortByIdDigits implements Comparator<String> {
        // Sorting by 5-digit id within event's @id
        public int compare(String s1, String s2) {
            return getNumericId(s1) - getNumericId(s2);
        }

        private int getNumericId(String id) {
            String numericId = "99999";
            String[] idParts = id.split("/");
            for (String part: idParts) {
                if (part.matches(String.format("\\d{%d}", UNIQUE_ID_NUMDIGITS))) {
                    numericId = part;
                    break;
                }
            }
            return Integer.valueOf(numericId);
        }
    }

}
