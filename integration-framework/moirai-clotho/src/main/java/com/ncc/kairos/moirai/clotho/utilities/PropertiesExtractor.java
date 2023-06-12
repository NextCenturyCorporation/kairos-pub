package com.ncc.kairos.moirai.clotho.utilities;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public final class PropertiesExtractor {
    private static final Properties properties;
    private static final Logger LOGGER = Logger.getLogger(PropertiesExtractor.class.getName());

    static {
        properties = new Properties();
        try {
            properties.load(PropertiesExtractor.class.getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            LOGGER.severe(e.toString());
        }
    }

    private PropertiesExtractor() {
        throw new IllegalStateException("Utility class, not to be instantiated.");
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
