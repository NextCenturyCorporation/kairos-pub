package com.ncc.kairos.moirai.clotho.tinkerpop;

import com.ncc.kairos.moirai.clotho.exceptions.DriverException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.sparql.process.traversal.dsl.sparql.SparqlTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class JanusGraphGremlinDriver extends GremlinDriver {

    private GraphTraversalSource g;
    private final String remotePropertiesFile;

    JanusGraphGremlinDriver(String connectionUri) {
        remotePropertiesFile = connectionUri;
    }

    /**
     * - JanusGraph leverages/requires Gremlin-Server which is configured via a .properties and a .yml file.
     * - In contrast to Neo4j and OrientDB, the JanusGraph connection uri points to a .properties file
     */
    @Override
    public void open() throws DriverException {
        try {
            // Retrieve the remote-janusgraph-connection-yaml file and create a Cluster object
            Configuration conf = new PropertiesConfiguration(getClass().getResource("/" + remotePropertiesFile));
            Cluster cluster = Cluster.open(conf);
    
            // Initialize a DriverRemoteConnection object using the cluster object
            DriverRemoteConnection drc = DriverRemoteConnection.using(cluster, "g");
    
            // Initialize the GraphTraversalSource using the DriverRemoteConnection object
            // The graph traversal source is instantiated directly without the need of a "graph" object
            g = AnonymousTraversalSource.traversal().withRemote(drc);
        } catch (Exception e) {
            throw new DriverException("JanusGraph/Cassandra gremlin driver failed to connect.", e);
        }
    }

    @Override
    public GraphTraversalSource getGTS() {
        return g;
    }

    @Override
    public SparqlTraversalSource getSTS() {
        return null;
    }

    @Override
    public String addPropertyToVertex(Vertex v, String key, String value) {
        String errorMsg = "";
        try {
            g.V(v).property(key, value).next();
        } catch (Exception ex) {
            errorMsg = String.format("An error occurred while attempting to add the key %s and value %s to the vertex: %s.", key, value, ex.getMessage());
        }
        return errorMsg;
    }

    @Override
    protected void commitTransaction() {
        // do nothing
        // Gremlin-Server-hosted graphs do not explicitly manage transactions since each call
        //         to the GraphTraversalSource is automatically committed as a transaction.
    }

    @Override
    protected void closeTransaction() {
        // do nothing
    }
}
