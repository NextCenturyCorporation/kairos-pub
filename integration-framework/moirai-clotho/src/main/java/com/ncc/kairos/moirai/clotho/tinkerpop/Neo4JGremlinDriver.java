package com.ncc.kairos.moirai.clotho.tinkerpop;

import com.ncc.kairos.moirai.clotho.exceptions.DriverException;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JElementIdProvider;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph;
import com.steelbridgelabs.oss.neo4j.structure.providers.Neo4JNativeElementIdProvider;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.sparql.process.traversal.dsl.sparql.SparqlTraversalSource;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

public class Neo4JGremlinDriver extends GremlinDriver {

    private final GraphTraversalSource g;
    private final Neo4JGraph graph;

    Neo4JGremlinDriver(String connectionUri) {
        Driver driver = GraphDatabase.driver(connectionUri);
        Neo4JElementIdProvider<Long> vertexIdProvider = new Neo4JNativeElementIdProvider();
        Neo4JElementIdProvider<Long> edgeIdProvider = new Neo4JNativeElementIdProvider();
        graph = new Neo4JGraph(driver, vertexIdProvider, edgeIdProvider);
        g = graph.traversal();
    }

    @Override
    public void open() throws DriverException {
        // Do nothing because nothing is needed here.
    }

    @Override
    public GraphTraversalSource getGTS() {
        return g;
    }

    @Override
    public SparqlTraversalSource getSTS() {
        return graph.traversal(SparqlTraversalSource.class);
    }

}
