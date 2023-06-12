package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import com.ncc.kairos.moirai.clotho.interfaces.IDefinitionService;

import java.util.ArrayList;
import java.util.List;

public final class KsfOntology {

    private KsfOntology() {
        throw new IllegalStateException("Utility class, not to be instantiated.");
    }

    public static List<String> loadOntology(IDefinitionService definitionService) {
        List<String> errorMsgs = new ArrayList<>();

        try {
            definitionService.close();
            errorMsgs = new ArrayList<>(); // Currently there is no ontology to load.
        } catch (Exception ex) {
            errorMsgs.add("Failed to load ontology: " + ex.getMessage());
        }

        return errorMsgs;
    }

}
