package com.ncc.kairos.moirai.clotho.tinkerpop.inmemory_graphdb;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class GraphComponent {
    final String id;
    final Map<String, String> properties;

    GraphComponent(Map<String, String> properties) {
        this.id = UUID.randomUUID().toString();

        this.properties = new HashMap<>();
        this.properties.putAll(properties);
        this.properties.put("id", id);
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getProperty(String prop) {
        return properties.get(prop);
    }
}
