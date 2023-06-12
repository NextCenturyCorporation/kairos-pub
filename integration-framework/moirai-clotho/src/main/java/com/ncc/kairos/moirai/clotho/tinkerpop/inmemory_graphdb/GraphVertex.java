package com.ncc.kairos.moirai.clotho.tinkerpop.inmemory_graphdb;

import java.util.*;

public class GraphVertex extends GraphComponent {
    final List<GraphEdge> outgoing;
    final List<GraphEdge> incoming;

    public GraphVertex(Map<String, String> properties) {
        super(properties);

        this.outgoing = new ArrayList<>();
        this.incoming = new ArrayList<>();
    }

    public void addOutgoing(GraphEdge newEdge) {
        this.outgoing.add(newEdge);
    }

    public void addIncoming(GraphEdge newEdge) {
        this.incoming.add(newEdge);
    }

    public void removeOutgoing(GraphEdge edgeToRemove) {
        this.outgoing.remove(edgeToRemove);
    }

    public void removeIncoming(GraphEdge edgeToRemove) {
        this.incoming.remove(edgeToRemove);
    }

    public List<GraphEdge> getOutgoing() {
        return outgoing;
    }

    public List<GraphEdge> getIncoming() {
        return incoming;
    }

    public boolean isConnected() {
        return incoming.size() > 0 || outgoing.size() > 0;
    }
}
