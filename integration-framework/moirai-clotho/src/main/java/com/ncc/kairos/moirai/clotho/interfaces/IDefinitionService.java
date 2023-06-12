package com.ncc.kairos.moirai.clotho.interfaces;

import com.ncc.kairos.moirai.clotho.exceptions.SchemaDefinitionException;
import com.ncc.kairos.moirai.clotho.model.Entity;
import com.ncc.kairos.moirai.clotho.model.Event;
import com.ncc.kairos.moirai.clotho.model.Schema;

import java.util.List;
import java.util.Map;

public interface IDefinitionService extends IGraphService {
    Schema addSchema(Schema schema) throws SchemaDefinitionException;

    Entity addEntity(Entity entity) throws SchemaDefinitionException;

    Event addEvent(Event event) throws SchemaDefinitionException;

    List<Event> getEvents(Map<String, String> searchCriteria) throws SchemaDefinitionException;

    List<Entity> getEntities(Map<String, String> searchCriteria) throws SchemaDefinitionException;

    List<Schema> getSchemas(Map<String, String> searchCriteria) throws SchemaDefinitionException;
}
