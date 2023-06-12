package com.ncc.kairos.moirai.clotho.tinkerpop.inmemory_graphdb;

import java.util.Map;

public class GraphEdge  extends GraphComponent {
    final GraphVertex to;
    final GraphVertex from;

    GraphEdge(GraphVertex from, GraphVertex to, Map<String, String> properties) {
        super(properties);

        this.to = to;
        this.from = from;

        //update vertices
        from.addOutgoing(this);
        to.addIncoming(this);
    }

    public GraphVertex getFrom() {
        return from;
    }

    public GraphVertex getTo() {
        return to;
    }

    public void cleanReferences() {
        this.to.removeIncoming(this);
        this.from.removeOutgoing(this);
    }
}
