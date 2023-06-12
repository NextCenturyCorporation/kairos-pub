package com.ncc.kairos.moirai.clotho.tinkerpop;

import com.ncc.kairos.moirai.clotho.exceptions.DriverException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.sparql.process.traversal.dsl.sparql.SparqlTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.janusgraph.core.JanusGraphFactory;

public class JanusGraphCassandraGremlinDriver extends GremlinDriver {

    private static final String CASSANDRA_BACKEND = "storage.backend";
    private static final String CASSANDRA_HOSTNAME = "storage.hostname";
    private static final String CASSANDRA_KEYSPACE = "storage.cql.keyspace";
    private GraphTraversalSource g;
    private Graph graph;
    private final Configuration cassandraConf;

    JanusGraphCassandraGremlinDriver(String backend, String hostName, String keySpace) {
        cassandraConf = new PropertiesConfiguration();
        cassandraConf.addProperty(CASSANDRA_BACKEND, backend);
        cassandraConf.addProperty(CASSANDRA_HOSTNAME, hostName);
        cassandraConf.addProperty(CASSANDRA_KEYSPACE, keySpace);
    }

    @Override
    public void open() throws DriverException {
        try {
            graph = JanusGraphFactory.open(cassandraConf);
            g = graph.traversal();
        } catch (Exception e) {
            throw new DriverException("Failed to open a JanusGraph connection", e);
        }
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
