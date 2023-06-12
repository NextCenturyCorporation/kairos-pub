package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import com.ncc.kairos.moirai.clotho.model.*;

import java.util.ArrayList;
import java.util.List;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.BEFORE_QID;
import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.CACI_TA1_PREFIX;

/**
 * Utilities for testing KAIROS Clotho functionality and/or creating TA1 examples.
 * Call {@link #startNewTest()} before each test to ensure a clean model.
 * @author Darren Gemoets, initially adapted from AIDA source code
 */
class TA1TestUtils extends TestUtils {

    /**
     * Construct TA1 test utilities.
     * @param dumpAlways whether or not always to dump the model and validation report
     * @param dumpToFile whether or not to dump to a file
     */
    TA1TestUtils(boolean dumpAlways, boolean dumpToFile) {
        super(dumpAlways, dumpToFile);
    }

    /**
     * Get an appropriate performer prefix.
     * @return a TA1 performer prefix
     */
    @Override
    String getPrefix() {
        return CACI_TA1_PREFIX;
    }

    /**
     * Get the list of events from the model.
     * @return a {@link List} of {@link SchemaEvent}s
     */
    @Override
    List<SchemaEvent> getEvents() {
        return model.getEvents();
    }

    /**
     * Get the list of entities from the model.
     * @return a {@link List} of {@link SchemaEntity}s
     */
    @Override
    List<SchemaEntity> getEntities() {
        return model.getEntities();
    }

    /**
     * Get the list of top-level relations from the model.
     * @return a {@link List} of {@link Relation}s
     */
    @Override
    List<Relation> getTopLevelRelations() {
        return model.getRelations();
    }

    /**
     * Call before each TA1 test.  Returns a new, valid model with standard TA-specific SDF metadata.
     * It includes a single {@link SchemaEvent}.
     * @return a new model with which to start a test
     */
    @Override
    JsonLdRepresentation startNewTest() {
        super.startNewTest();
        model.setTa2(false);
        model.setEntities(new ArrayList<>());
        model.setEvents(new ArrayList<>());
        model.setRelations(new ArrayList<>());
        makeLeafEvent();
        return model;
    }

    @Override
    void attachChildrenToParent(SchemaEvent parent, List<SchemaEvent> children) {
        List<Child> childList = new ArrayList<>();
        for (SchemaEvent child : children) {
            Child childObj = new Child();
            childObj.setChild(child.getAtId());
            childList.add(childObj);
        }
        // Create an outlink relation from child[n] to child[n+1] in the parent event
        for (int childIndex = 1; childIndex < childList.size(); childIndex++) {
            makeRelation(childList.get(childIndex - 1).getChild(),
                    children.get(childIndex).getAtId(), BEFORE_QID, parent);
        }
        parent.setChildren(childList);
    }

}
