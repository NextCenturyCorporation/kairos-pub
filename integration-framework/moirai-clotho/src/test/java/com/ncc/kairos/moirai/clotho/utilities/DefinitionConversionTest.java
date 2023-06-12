package com.ncc.kairos.moirai.clotho.utilities;

import com.ncc.kairos.moirai.clotho.model.*;
import com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static com.ncc.kairos.moirai.clotho.resources.GremlinTestConstants.*;

@Disabled
class DefinitionConversionTest extends DefinitionConversion {

    @AfterEach
    void tearDown() {

    }

    @Test
    void testConvertEntityToVertex() {
        Entity entityParent = new Entity();
        Map<String, String> parentProps = new HashMap<>();
        parentProps.put("Parent Key", "Parent Value");
        entityParent.setName("PER");
        entityParent.setDescription("PERSON");

        Vertex resultVertex = convertEntityToVertex(entityParent);
        Map<String, String> vertexProperties = resultVertex.getPropertiesMap();

        assertEquals(ENTITY, resultVertex.getLabel());
        assertEquals("PER", vertexProperties.get(NAME));
        // assertEquals(vertexProperties.get(), PERSON_LABEL);
        assertEquals("Parent Value", vertexProperties.get("Parent Key"));
    }

    @Test
    void testConvertVerticesToEntityList() {
        // This test also covers convertVertexToEntityList
        Vertex vertexOne = new Vertex();
        vertexOne.setId("100");
        vertexOne.setLabel(ENTITY);
        Map<String, String> vertexOneProps = new HashMap<>();
        vertexOneProps.put(NAME, PERSON_LABEL);
        // vertexOneProps.put(DESCRIPTION, PERSON_LABEL);
        vertexOneProps.put("KeyOne", "ValOne");
        vertexOne.setPropertiesMap(vertexOneProps);

        Vertex vertexTwo = new Vertex();
        vertexTwo.setId("200");
        vertexTwo.setLabel(ENTITY);
        Map<String, String> vertexTwoProps = new HashMap<>();
        vertexTwoProps.put(NAME, "Police");
        vertexTwoProps.put(SUBTYPE_OF, PERSON_LABEL);
        vertexTwoProps.put("KeyTwo", "ValTwo");
        vertexTwo.setPropertiesMap(vertexTwoProps);

        List<Vertex> verticesList = new ArrayList<>();
        verticesList.add(vertexOne);
        verticesList.add(vertexTwo);

        List<Entity> resultEntitiesList = convertVerticesToEntityList(verticesList);

        assertEquals(2, resultEntitiesList.size());

        assertNotNull(resultEntitiesList.get(0));
        assertNotNull(resultEntitiesList.get(1));
    }

    @Test
    void testConvertEventToVertex() {
        Event event = new Event();
        Map<String, String> eventProps = new HashMap<>();
        eventProps.put("propertyKey", "propertyVal");

        Vertex resultVertex = convertEventToVertex(event);
        Map<String, String> resultVertexProps = resultVertex.getPropertiesMap();

        assertEquals(EVENT, resultVertex.getLabel(), EVENT);
        assertEquals("bombing", resultVertexProps.get(NAME));
        assertEquals("very-likely", resultVertexProps.get(LIKELINESS));
        assertEquals("bomb-prep", resultVertexProps.get(ACHIEVES_EVENT));
        assertEquals("propertyVal", resultVertexProps.get("propertyKey"));
    }

    @Test
    void testVertexToEvent() {
        // This test also covers convertVertexToEntityList
        Vertex vertexOne = new Vertex();
        vertexOne.setId("100");
        vertexOne.setLabel(EVENT);
        Map<String, String> vertexOneProps = new HashMap<>();
        vertexOneProps.put(NAME, "bombing");
        // vertexOneProps.put(DESCRIPTION, "bombing event");
        vertexOneProps.put(LIKELINESS, "very-likely");
        vertexOneProps.put("KeyOne", "ValOne");
        vertexOne.setPropertiesMap(vertexOneProps);

        Event resultEvent = convertVertexToEvent(vertexOne);

        assertEquals("100", resultEvent.getAtId());
        assertEquals("bombing", resultEvent.getName());

    }

    @Test
    void testConvertSchemaToVertex() {
        Schema schema = new Schema();
        schema.setName("bomb-raid");
        // schema.setSubtypeOfName("raid");
        schema.setAtId("1000");
        Map<String, String> props = new HashMap<>();
        props.put("key", "val");
        schema.setPrivateData(props);

        Vertex resultVertex = convertSchemaToVertex(schema);
        Map<String, String> resultProps = resultVertex.getPropertiesMap();

        assertEquals(SCHEMA, resultVertex.getLabel());
        assertEquals("1000", resultVertex.getId());
        assertEquals("bomb-raid", resultProps.get(KairosSchemaFormatConstants.KAIROS_CONTEXT_IRI + NAME));
        assertEquals("raid", resultProps.get(SUBTYPE_OF));
        assertEquals("val", resultProps.get("key"));
    }

    @Test
    void testConvertVertexToSchema() {
        Vertex vertex = new Vertex();
        vertex.setId("5");
        vertex.setLabel(SCHEMA);
        Map<String, String> props = new HashMap<>();
        props.put(NAME, "bombing");
        props.put(STEPS_ORDER, "in-sequence");
        props.put(SUBTYPE_OF, "explosive");
        vertex.setPropertiesMap(props);

        List<Step> steps = new ArrayList<>();
        Step stepOne = new Step();
        stepOne.setAtId("999");
        Step stepTwo = new Step();
        stepTwo.setEvent("raid");
        steps.add(stepOne);
        steps.add(stepTwo);

        Schema schema = convertVertexToSchema(vertex);

        assertEquals("5", schema.getAtId());
        // assertEquals(schema.getSubtypeOfName(), "explosive");

        assertEquals(2, schema.getSteps().size());
    }

    @Test
    void testConvertStepToVertex() {
        Step step = new Step();

        Vertex resultVert = convertStepToVertex(step);
        Map<String, String> props = resultVert.getPropertiesMap();

        assertEquals("999", resultVert.getId());
        assertEquals(STEP, resultVert.getLabel());
        assertEquals("1+", props.get(REPEATS));
        assertEquals("very-unlikely", props.get(LIKELINESS));
        assertEquals("bombing", props.get(EVENT));
    }

    @Test
    void testConvertVerticesToSteps() {
        // Also covers convert VertexToStep

        Vertex vertexOne = new Vertex();
        vertexOne.setId("100");
        vertexOne.setLabel(STEP);
        Map<String, String> vertexOneProps = new HashMap<>();
        vertexOneProps.put(REPEATS, "1+");
        vertexOneProps.put(LIKELINESS, "very-likely");
        vertexOneProps.put(OPTIONAL, "false");
        vertexOne.setPropertiesMap(vertexOneProps);

        Vertex vertexTwo = new Vertex();
        vertexTwo.setId("200");
        vertexTwo.setLabel(STEP);
        Map<String, String> vertexTwoProps = new HashMap<>();
        vertexTwoProps.put(REPEATS, "2+");
        vertexTwoProps.put(LIKELINESS, "very-likely");
        vertexTwoProps.put(OPTIONAL, "true");
        vertexTwo.setPropertiesMap(vertexTwoProps);

        List<Vertex> verticesList = new ArrayList<>();
        verticesList.add(vertexOne);
        verticesList.add(vertexTwo);

        List<Step> stepsList = convertVerticesToSteps(verticesList);

        assertEquals(2, stepsList.size());
        assertNotNull(stepsList.get(0));
    }
}
