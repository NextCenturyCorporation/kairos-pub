package com.ncc.kairos.moirai.clotho.api;

import com.ncc.kairos.moirai.clotho.interfaces.IDefinitionService;
import com.ncc.kairos.moirai.clotho.model.Entity;
import com.ncc.kairos.moirai.clotho.model.Event;
import com.ncc.kairos.moirai.clotho.model.Schema;
import com.ncc.kairos.moirai.clotho.utilities.DefinitionConversion;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ncc.kairos.moirai.clotho.resources.DefinitionConstants.NAME;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2019-09-20T06:20:37.043-04:00[America/New_York]")

@RestController
public class SchemaApiController implements SchemaApi {

    private final NativeWebRequest request;

    @Autowired
    private IDefinitionService definitionService;

    @org.springframework.beans.factory.annotation.Autowired
    public SchemaApiController(NativeWebRequest request) {
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    @Override
    public ResponseEntity<String> insertEvents(@ApiParam(value = "event data to add") @Valid @RequestBody List<Event> events) {
        try {
            definitionService.open();
            HttpStatus statusToReturn = HttpStatus.CREATED;
            for (Event event: events) {
                Event insertedEvent = definitionService.addEvent(event);
                HttpStatus currentStatus = ApiUtil.getResponseFromId(insertedEvent.getAtId());
                if (currentStatus != HttpStatus.CREATED) {
                    statusToReturn = currentStatus;
                }
            }
            definitionService.acceptChanges();
            return new ResponseEntity<>(statusToReturn);
        } catch (Exception e) {
            e.printStackTrace();
            definitionService.rollbackChanges();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            definitionService.close();
        }
    }

    @Override
    public ResponseEntity<String> insertEntities(@ApiParam(value = "Entity items to add") @Valid @RequestBody List<Entity> entities) {
        try {
            definitionService.open();
            HttpStatus statusToReturn = HttpStatus.CREATED;
            for (Entity entity : entities) {
                Entity insertedEntity = definitionService.addEntity(entity);
                HttpStatus currentStatus = ApiUtil.getResponseFromId(insertedEntity.getAtId());
                if (currentStatus != HttpStatus.CREATED) {
                    statusToReturn = currentStatus;
                }
            }
            definitionService.acceptChanges();
            return new ResponseEntity<>(statusToReturn);
        } catch (Exception e) {
            e.printStackTrace();
            definitionService.rollbackChanges();
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            definitionService.close();
        }
    }

    @Override
    public ResponseEntity<String> insertSchema(@ApiParam(value = "Schema item to add") @Valid @RequestBody Schema schema) {
        try {
            definitionService.open();
            Schema insertedSchema = definitionService.addSchema(schema);
            if (!insertedSchema.getSystemError().isEmpty()) {
                throw new Exception(insertedSchema.getSystemError().toString());
            }
            HttpStatus statusToReturn = ApiUtil.getResponseFromId(insertedSchema.getAtId());
            definitionService.acceptChanges();
            return new ResponseEntity<>(statusToReturn);
        } catch (Exception e) {
            e.printStackTrace();
            definitionService.rollbackChanges();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            definitionService.close();
        }
    }

    @Override
    public ResponseEntity<List<Entity>> getEntities(@ApiParam(value = "name value for edge") @Valid @RequestParam(value = "name", required = false) String name, @ApiParam(value = "a map of key-val pairs to use as search criteria on edges")
    @Valid @RequestParam(value = "searchCriteria", required = false) String searchCriteria) {
        Map<String, String> searchCriteriaMap = DefinitionConversion.searchCriteriaToMap(searchCriteria);
        DefinitionConversion.addIfNotEmpty(searchCriteriaMap, NAME, name);

        try {
            definitionService.open();
            List<Entity> entities = definitionService.getEntities(searchCriteriaMap);
            definitionService.acceptChanges();
            return new ResponseEntity<>(entities, ApiUtil.getResponseFromList(entities));
        } catch (Exception e) {
            e.printStackTrace();
            definitionService.rollbackChanges();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            definitionService.close();
        }
    }

    @Override
    public ResponseEntity<List<Event>> getEvents(@ApiParam(value = "map of key-val pairs to use as search-criteria")
                                          @Valid @RequestParam(value = "searchCriteria", required = false)
                                                  String searchCriteria) {
        try {
            definitionService.open();

            Map<String, String> searchCriteriaMap = DefinitionConversion.searchCriteriaToMap(searchCriteria);
            List<Event> events = definitionService.getEvents(searchCriteriaMap);
            definitionService.acceptChanges();
            return new ResponseEntity<>(events, ApiUtil.getResponseFromList(events));
        } catch (Exception e) {
            e.printStackTrace();
            definitionService.rollbackChanges();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            definitionService.close();
        }

    }

    @Override
    public ResponseEntity<Event> getEventByName(@ApiParam(value = "unique name of the event to retrieve", required = true) @PathVariable("name") String name) {

        try {
            definitionService.open();

            Map<String, String> searchCriteriaMap = new HashMap<String, String>();
            DefinitionConversion.addIfNotEmpty(searchCriteriaMap, NAME, name);

            List<Event> events = definitionService.getEvents(searchCriteriaMap);
            if (events.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NO_CONTENT, String.format("Expected one matching event with name %s, found %s", name, events.size()));
            } else if (events.size() > 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("Expected one matching event with name %s, found %s", name, events.size()));
            }
            definitionService.acceptChanges();
            return new ResponseEntity<Event>(events.get(0), ApiUtil.getResponseFromList(events));
        } catch (Exception e) {
            e.printStackTrace();
            definitionService.rollbackChanges();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            definitionService.close();
        }

    }

    @Override
    public ResponseEntity<List<Schema>> getSchemata(@ApiParam(value = "map of key-val pairs to use as search criteria for schema") @Valid @RequestParam(value = "searchCriteria", required = false) String searchCriteria) {
        try {
            definitionService.open();

            // Parse the search-criteria string
            Map<String, String> searchCriteriaMap = DefinitionConversion.searchCriteriaToMap(searchCriteria);
            // Pass the search-criteria map to the definition service
            List<Schema> schemaList = definitionService.getSchemas(searchCriteriaMap);

            return new ResponseEntity<List<Schema>>(schemaList, ApiUtil.getResponseFromList(schemaList));
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
