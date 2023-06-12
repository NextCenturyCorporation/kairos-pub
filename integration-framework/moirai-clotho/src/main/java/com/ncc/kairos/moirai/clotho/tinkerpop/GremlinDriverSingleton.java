package com.ncc.kairos.moirai.clotho.tinkerpop;

import com.ncc.kairos.moirai.clotho.interfaces.IGremlinDriver;
import com.ncc.kairos.moirai.clotho.services.DefinitionService;
import com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfOntology;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.NotImplementedException;

import java.io.InputStreamReader;
import java.io.Reader;

import java.util.List;
import java.util.Map;

import com.ncc.kairos.moirai.clotho.resources.ApplicationConstants;

import static com.ncc.kairos.moirai.clotho.resources.ApplicationConstants.*;

public final class GremlinDriverSingleton {

    private static IGremlinDriver driver;

    private GremlinDriverSingleton() {
        throw new IllegalStateException("Utility class, not to be instantiated.");
    }

    public static void createGremlinDriver(GraphDatabases graphType, Map<String, String> environmentVars) throws ConfigurationException {
        try {

            String dbUri = environmentVars.get(DATABASE_URI_ENVIRONMENT_VARIABLE);

            if (driver == null) {
                switch (graphType) {
                    case JANUSGRAPH:
                        driver = new JanusGraphGremlinDriver(dbUri);
                        break;
                    case JANUSGRAPH_CASSANDRA:
                        driver = new JanusGraphCassandraGremlinDriver(
                            environmentVars.get(CASSANDRA_BACKEND_ENVIRONMENT_VARIABLE),
                            environmentVars.get(CASSANDRA_HOSTNAME_ENVIRONMENT_VARIABLE),
                            environmentVars.get(CASSANDRA_CQL_KEYSPACE_ENVIRONMENT_VARIABLE));
                        break;
                    case NEO4J:
                        driver = new Neo4JGremlinDriver(dbUri);
                        break;
                    case ORIENTDB:
                        driver = new OrientDBGremlinDriver(dbUri);
                        break;
                    case VALIDATION:
                        driver = new InMemoryDriver();
                        break;
                    case NEPTUNE:
                        driver = new NeptuneDriver(dbUri);
                        break;
                    default:
                        throw new NotImplementedException("The following graph type is not yet supported:  " + graphType);
                }
            }
        } catch (Exception ex) {
            throw new ConfigurationException(ex.getMessage());
        } finally {
            if (driver != null) {
                driver.close();
            }
        }
    }

    public static IGremlinDriver getDriver() {
        return driver;
    }

    public static void loadOntology() throws ConfigurationException {
        DefinitionService defnServ = new DefinitionService();
        try {
            defnServ.open();
            loadRelations(defnServ);

            // Load into static maps on KSFOntology class.
            List<String> ontologyUploadErrors = KsfOntology.loadOntology(defnServ);
            if (!ontologyUploadErrors.isEmpty()) {
                throw new ConfigurationException(ontologyUploadErrors.toString());
            }
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to load ontology during Clotho start-up: " + ex.getMessage());
        } finally {
            defnServ.close();
        }
    }

    private static void loadRelations(DefinitionService defServ) { // NOSONAR
        try (Reader relationsJsonReader =
                new InputStreamReader(GremlinDriverSingleton.class.getResourceAsStream(ApplicationConstants.RELATIONS_FILE))) {
            // Currently disabled
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
