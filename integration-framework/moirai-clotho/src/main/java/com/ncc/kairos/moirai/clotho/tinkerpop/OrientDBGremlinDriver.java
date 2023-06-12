package com.ncc.kairos.moirai.clotho.tinkerpop;

import com.ncc.kairos.moirai.clotho.exceptions.DriverException;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.sparql.process.traversal.dsl.sparql.SparqlTraversalSource;

public class OrientDBGremlinDriver extends GremlinDriver {
    private OrientGraph graph;
    private final String connectionUri;

    OrientDBGremlinDriver(String connectionUri) {
        this.connectionUri = connectionUri;
    }

    @Override
    public void open() throws DriverException {
        graph = OrientGraph.open(connectionUri);
    }

    @Override
    public GraphTraversalSource getGTS() {
        return graph.traversal();
    }

    @Override
    public SparqlTraversalSource getSTS() {
        return graph.traversal(SparqlTraversalSource.class);
    }

}
