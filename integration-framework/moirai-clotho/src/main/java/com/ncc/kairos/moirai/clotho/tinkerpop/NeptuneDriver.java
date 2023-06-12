package com.ncc.kairos.moirai.clotho.tinkerpop;


import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.neptune.auth.NeptuneSigV4SignerException;
import com.amazonaws.regions.Regions;
import com.ncc.kairos.moirai.clotho.exceptions.DriverException;
import com.ncc.kairos.moirai.clotho.utilities.PropertiesExtractor;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.SigV4WebSocketChannelizer;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.sparql.process.traversal.dsl.sparql.SparqlTraversalSource;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.jsonld.JSONLDWriter;
import org.eclipse.rdf4j.sparqlbuilder.core.From;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ncc.kairos.moirai.clotho.resources.QueryConstants.*;

public class NeptuneDriver extends GremlinDriver {

    private static final Logger LOGGER = Logger.getLogger(NeptuneDriver.class.getName());
    
    private final GraphTraversalSource gts;
    private NeptuneSparqlRepository neptuneSparqlRepo;
    private final Client client;
    private final Cluster neptuneCluster;
    Variable s = SparqlBuilder.var("s");
    Variable p = SparqlBuilder.var("p");
    Variable o = SparqlBuilder.var("o");
    Variable g = SparqlBuilder.var("g");

    NeptuneDriver() {
        this.gts = null;
        this.client = null;
        this.neptuneCluster = null;
    }
    

    NeptuneDriver(String dbUri) throws NeptuneSigV4SignerException {
        LOGGER.log(Level.INFO, () -> "Opening connection: " + dbUri);
        Cluster.Builder builder = Cluster.build()
        .addContactPoint(dbUri)
        .port(getPort())
        .enableSsl(true)
        // .keyStore(path)
        .maxConnectionPoolSize(15)
        .maxSimultaneousUsagePerConnection(15)
        .serializer(Serializers.GRAPHBINARY_V1D0)
        .channelizer(SigV4WebSocketChannelizer.class)
        .reconnectInterval(2000);
        
        neptuneCluster = builder.create();
        this.client = neptuneCluster.connect();
        gts = AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using(client));
        
    }

    @Override
    protected GraphTraversalSource getGTS() {
        return gts;
    }

    @Override
    protected SparqlTraversalSource getSTS() {
        return null;
    }

    @Override
    public String runSparqlQuery(String query) {
        getConnection();
        LOGGER.info(query);
        return query.contains("CONSTRUCT") ? executeConstruct(query) : executeSelect(query);
    }

    private void getConnection() {
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        try {
            neptuneSparqlRepo = new NeptuneSparqlRepository(getUri(), awsCredentialsProvider, Regions.US_EAST_1.getName());
            neptuneSparqlRepo.init();
        } catch (NeptuneSigV4SignerException e) {
            LOGGER.severe("NeptuneSigV4SignerException: " + e.toString());
        } catch (Exception e) {
            LOGGER.severe(e.toString());
        }
    }

    @Override // NOTE: could look into depth limiting
    public String getSGraph(String namedGraph) {
        int limit = Integer.parseInt(PropertiesExtractor.getProperty("kairos.neptune.queryoffset"));
        getConnection();
        From from = SparqlBuilder.from(Rdf.iri(namedGraph));
        StringBuilder resultSetSB = new StringBuilder();
        int offset = 0; // offset for query
        while (true) {
            ConstructQuery query = Queries.CONSTRUCT()
            .from(from)
            .limit(limit)
            .offset(offset)
            .where(g.has(s, p, o));
            String result = executeConstruct(query.getQueryString());
            // empty results stop the loop.
            if (result.equals("[ ]")) {
                break;
            }

            if (offset != 0) {
                result = resultSetSB.substring(0, resultSetSB.length() - 1);
            } else {
                // removed the last ] from the string before appending
                if (resultSetSB.length() == 0) {
                    resultSetSB.setLength(resultSetSB.length() - 1);
                }
            }
            resultSetSB.append(result);
            offset += limit;
        }
        return resultSetSB.append(']').toString();
    }

    @Override
    public String getOffsetGraph(String namedGraph, Integer limit, Integer offset) {
        getConnection();
        From from = SparqlBuilder.from(Rdf.iri(namedGraph));
        ConstructQuery query = Queries.CONSTRUCT()
            .from(from)
            .limit(limit)
            .offset(offset)
            .where(g.has(s, p, o));
        return executeConstruct(query.getQueryString());
    }

    @Override
    public String getEventList(String namedGraph) {
        getConnection();
        String query = "";
        query += SELECT_S_P_O;
        query += FROM.replace(GRAPHNAME, namedGraph);
        query += WHERE_EVENT_LIST;
        return executeSelect(query);
    }

    // New ep to get schema tree given schema ID and named graph
    @Override
    public String getEventTree(String eventName, String namedGraph) {
        getConnection();
        String query = "";
        query += CONSTRUCT_S_P_O;
        query += FROM.replace(GRAPHNAME, namedGraph);
        query += WHERE_EVENT_TREE.replace("CEID", eventName);
        return executeConstruct(query);
    }

    @Override
    public String deleteNamedGraph(String namedGraph) {
        getConnection();
        IRI graph = SimpleValueFactory.getInstance().createIRI(namedGraph); 
        try (RepositoryConnection conn = neptuneSparqlRepo.getConnection()) {
            conn.clear(graph);
        } catch (Exception e) {
            return "Failed to delete graph";
        }
        return "Graph delete successful.";
    }

    @Override
    public String getNamedGraphs() {
        getConnection();
        SelectQuery query = Queries.SELECT()
        .select(g)
        .where(GraphPatterns.and(s.has(p, o)).from(g))
        .groupBy(g);
        return executeSelect(query.getQueryString());
    }

    @Override
    public String saveOrUpdate(String namedGraph, String event, String details, String userName) {
        LocalDateTime now = LocalDateTime.now();
        if (deleteTriple(namedGraph, event, ADDITIONAL_NOTES_PREDICATE)) {
            insertTripleAttachedToEvent(namedGraph, event, details, ADDITIONAL_NOTES_PREDICATE);
            insertTripleAttachedToEvent(namedGraph, event, userName + " " + now.toString(), USER_EDIT_PREDICATE);
        } else {
            return "Failed";
        }
        return "Success";
    }

    private boolean deleteTriple(String namedGraph, String event, String predicate) {
        getConnection();
        try (RepositoryConnection conn = neptuneSparqlRepo.getConnection()) {
            String deleteQueryString = DELETE_RDF_TRIPLE
                .replaceAll(GRAPHNAME, namedGraph)
                .replaceAll(PREDICATE_OBJECT, predicate)
                .replaceAll(EVENT_KEY, event);
            
            Update deleteQuery = conn.prepareUpdate(deleteQueryString);

            deleteQuery.execute();
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            return false;
        }
        return true;
    }

    private void insertTripleAttachedToEvent(String namedGraph, String event, String details, String predicate) {
        getConnection();
        try (RepositoryConnection conn = neptuneSparqlRepo.getConnection()) {
            String insertQuertString = INSERT_RDF_TRIPLE
                .replaceAll(GRAPHNAME, namedGraph)
                .replaceAll(EVENT_KEY, event)
                .replaceAll(PREDICATE_OBJECT, predicate)
                .replaceAll(INSERTED_OBJECT, details);

            Update insertQuery = conn.prepareUpdate(insertQuertString);

            insertQuery.execute();
            LOGGER.log(Level.INFO, () -> "Updated: " + event);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    public String getSVertex(String namedGraph, String key) {
        From from = SparqlBuilder.from(Rdf.iri(namedGraph));
		SelectQuery query = Queries.SELECT()
        .from(from)
        .where(s.has(p, o, g))
        .select(g, s, p, o)
        .limit(100);
        
        return query.getQueryString();
    }

    @Override
    public void open() throws DriverException { 
        // Nothing needed, formerly "neptuneCluster.open();"
    }

    public void close() {
        // Nothing needed, formerly "neptuneCluster.close();"
    }

    /**
     * Runs a select sparql query.
     * @param query a select sparql query.
     * @return Query Result
     */
    private String executeSelect(String query) {
        String results = "";
        LOGGER.log(Level.INFO, () -> "Executing Query: " + query);
        try (RepositoryConnection conn = neptuneSparqlRepo.getConnection()) {
            OutputStream output = new ByteArrayOutputStream();
            TupleQuery tupleQuery = conn.prepareTupleQuery(query);

            TupleQueryResultHandler jsonWriter = new SPARQLResultsJSONWriter(output);
            tupleQuery.evaluate(jsonWriter);
            results = output.toString();
        
			output.flush();
		} catch (IOException e) {
			LOGGER.severe(e.toString());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error handling SELECT query.");
		} catch (Exception e) {
		    LOGGER.severe(e.toString());
		    return "Failed to execute query";
		}
        return results;
    }

    private String executeConstruct(String query) {
        OutputStream output = new ByteArrayOutputStream();
        LOGGER.log(Level.INFO, () -> "Executing Query: " + query);
        String results = "";
        try (RepositoryConnection conn = neptuneSparqlRepo.getConnection()) {
            // Get Data from Neptune
            GraphQuery graphQuery = conn.prepareGraphQuery(query);
            RDFHandler handler = new JSONLDWriter(output);
            graphQuery.evaluate(handler);
            results = output.toString();
            output.flush();
        } catch (Exception e) {
            LOGGER.severe(e.toString());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error handling CONSTRUCT query.");
        }
        return results;
    }

    private String getUri() {
        return "https://" + getEndEndpoint() + ":" + getPort();
    }

    private String getEndEndpoint() {
        String endpoint = System.getenv()
        .get("DB_URI");
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalArgumentException("Access Key ID is null");
        }
       return endpoint;
    }

    private int getPort() {
       return Integer.parseInt(System.getenv().get("DB_PORT"));
    }

}
