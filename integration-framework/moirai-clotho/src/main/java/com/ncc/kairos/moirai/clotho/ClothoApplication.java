package com.ncc.kairos.moirai.clotho;

import com.ncc.kairos.moirai.clotho.resources.ApplicationConstants;
import com.ncc.kairos.moirai.clotho.tinkerpop.GraphDatabases;
import com.ncc.kairos.moirai.clotho.tinkerpop.GremlinDriverSingleton;
import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * This class is the entry point to the Clotho Springboot application.
 *
 * @author ryan scott
 * @version 0.1
 */

@SpringBootApplication
@EntityScan(basePackages = {"com.ncc.kairos.moirai.clotho"})
public class ClothoApplication extends SpringBootServletInitializer {

    private static final Logger log = LoggerFactory.getLogger(ClothoApplication.class);

    /**
     * Main method to start the clotho springboot application.
     *
     * @param args Command line arguments, currently none are expected.
     */
    public static void main(String[] args) {
        SpringApplication.run(ClothoApplication.class, args);
    }

    @Bean
    @ConditionalOnProperty(name = "kairos.cors.enabled")
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Value("${kairos.cors.api.allowed-origins}")
            private String apiAllowedOrigins;

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(this.apiAllowedOrigins.split(","))
                        .allowedMethods("PUT", "DELETE", "GET", "POST", "OPTIONS", "HEAD")
                        .allowedHeaders("*");
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry
                        .addResourceHandler("/static/**")
                        .addResourceLocations("/static/");
            }
        };
    }

    /**
     * Method that runs when the application starts and handles getting environment variables to setup the application.
     * Expects DB_URI to point to the bolt connection of an existing Neo4J repo. bolt://localhost:7687 is the default.
     *
     * @return The closure that handles environment variables.
     */
    @Bean
    @ConditionalOnProperty(
        name = "kairos.clotho.check-env",
        havingValue = "true",
        matchIfMissing = true)
    public CommandLineRunner getEnv() {
        return args -> {
            Map<String, String> env = System.getenv();

            String databaseType = env.containsKey(ApplicationConstants.DATABASE_TYPE_ENVIRONMENT_VARIABLE) ?
                    env.get(ApplicationConstants.DATABASE_TYPE_ENVIRONMENT_VARIABLE) :
                    null;

            // Verify the db_type env-var is set
            if (databaseType == null) {
                log.error("DB_TYPE has not been specified.");
                throw new NullArgumentException("DB_TYPE has not been specified.");
            }

            GraphDatabases database = GraphDatabases.getEnumFromString(databaseType);

            // If we are in validation we don't expect a uri else Verify the db_uri env-var is set
            if (database == GraphDatabases.VALIDATION) {
                env = Collections.singletonMap("DB_URI", "NONE");

            } else if (!env.containsKey(ApplicationConstants.DATABASE_URI_ENVIRONMENT_VARIABLE)) {
                log.error("DB_URI has not been specified.");
                throw new NullArgumentException("DB_URI has not been specified.");
            }

            // Parse the db_type env-var into the corresponding enumerated value

            // If database type is JanusGraph_Cassandra, ensure ALL required environment variables are set
            if ((database == GraphDatabases.JANUSGRAPH_CASSANDRA) &&
                (!(env.containsKey(ApplicationConstants.CASSANDRA_BACKEND_ENVIRONMENT_VARIABLE)
                    && env.containsKey(ApplicationConstants.CASSANDRA_HOSTNAME_ENVIRONMENT_VARIABLE)
                    && env.containsKey(ApplicationConstants.CASSANDRA_CQL_KEYSPACE_ENVIRONMENT_VARIABLE)))) {
                throw new NullArgumentException(String.format("ALL of the following environment variables MUST be set when connecting Clotho to a JanusGraph-Cassandra instance: %s, %s, %s.",
                    ApplicationConstants.CASSANDRA_BACKEND_ENVIRONMENT_VARIABLE, ApplicationConstants.CASSANDRA_HOSTNAME_ENVIRONMENT_VARIABLE, ApplicationConstants.CASSANDRA_CQL_KEYSPACE_ENVIRONMENT_VARIABLE));
            }

            // Initialize the appropriate gremlin-driver singleton based on the specified graph db type
            GremlinDriverSingleton.createGremlinDriver(database, env);

            // Load ontology after gremlin-driver initialization. Unless It is to Neptune
            if (database != GraphDatabases.NEPTUNE) {
                GremlinDriverSingleton.loadOntology();
            }

        };
    }

    /**
     * Grabs the info from version.properties and displays it.
     *
     * @return The closure that handles display.
     */
    @Bean
    public CommandLineRunner getVersionProperties() {
        return args -> {
            try {
                Properties props = new Properties();
                props.load(getClass().getResourceAsStream("/BOOT-INF/classes/version.properties"));
                log.info("Build Version : {}", props.getProperty("version"));
                log.info("Build Time : {}", props.getProperty("build_time"));
            } catch (Exception e) {
                log.info("\nCannot find version.properties file: If this is not being run in development node contact a KAIROS admin.\n");
            }
        };
    }
}
