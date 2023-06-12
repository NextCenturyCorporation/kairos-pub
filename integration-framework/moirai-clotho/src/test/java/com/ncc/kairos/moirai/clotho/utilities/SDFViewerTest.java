package com.ncc.kairos.moirai.clotho.utilities;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.utils.JsonUtils;
import com.ncc.kairos.moirai.clotho.api.ApiUtil;
import com.ncc.kairos.moirai.clotho.model.JsonLdRepresentation;
import com.ncc.kairos.moirai.clotho.resources.ClothoTestConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SDFViewerTest {

    private static final Logger log = LoggerFactory.getLogger(SDFViewerTest.class);
    private String viewerTemplateText = null;
    private JsonLdRepresentation jlo;

    // Initialize validation objects based on JSON-LD in supplied filename; fail if any errors
    void initTest(String inputJsonFile, String outputTxtFile) {
        try {
            String jsonFileText = JsonUtils.toString(JsonUtils.fromInputStream(this.getClass().getResourceAsStream(inputJsonFile)));
            // This relies on ApiUtil class to prepare the test objects properly
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
            Map<String, Object> jsonLdObject = mapper.reader().forType(Map.class).readValue(jsonFileText);
            jlo = ApiUtil.deserializeCompactJsonLd(jsonLdObject);

            // Convert target filename to a String
            viewerTemplateText = Files.readString(Path.of(this.getClass().getResource(outputTxtFile).toURI()));
        } catch (Exception e) {
            Assertions.fail("Initialization", e);
        }
    }

    private boolean templateMismatch(boolean verbose) {
        return templateMismatch(verbose, true, true);        
    }

    private boolean templateMismatch(boolean verbose, boolean useJGraphT, boolean useAllTemporalRelations) {
        SDFViewer sdfViewer = new SDFViewer(jlo, verbose, useJGraphT, useAllTemporalRelations);
        String elCandidateStr = sdfViewer.getOutput();

        assertNotNull(viewerTemplateText);
        assertNotNull(elCandidateStr);

        if (elCandidateStr.equals(viewerTemplateText)) {
            return false;
        } else {
            log.info(String.format("-> Complete output:%n%s", elCandidateStr));
            return true;
        }
    }

    @Test
    void testSDFta1SDFViewer() {
        initTest(ClothoTestConstants.DISEASE_OUTBREAK_TA1_FILE, ClothoTestConstants.DISEASE_OUTBREAK_SDFVIEWER_OUTPUT);
        if (templateMismatch(true)) {
            Assertions.fail("Generated TA1 SDF Viewer output doesn't match template.  See logging.");
        }
    }

    @Test
    void testSDFta2SDFViewer_brief() {
        initTest(ClothoTestConstants.LEGIONNAIRES_DISEASE_TA2_FILE, ClothoTestConstants.LEGIONNAIRES_DISEASE_SDFVIEWER_BRIEF_OUTPUT);
        if (templateMismatch(false)) {
            Assertions.fail("Generated TA2 SDF Viewer brief output doesn't match template.  See logging.");
        }
    }

    @Test
    void testSDFta2SDFViewer_verbose() {
        initTest(ClothoTestConstants.LEGIONNAIRES_DISEASE_TA2_FILE, ClothoTestConstants.LEGIONNAIRES_DISEASE_SDFVIEWER_OUTPUT);
        if (templateMismatch(true)) {
            Assertions.fail("Generated TA2 SDF Viewer verbose output doesn't match template.  See logging.");
        }
    }

    @Test
    void testSDFta2SDFViewer_IED_brief() {
        initTest(ClothoTestConstants.IED_TA2_FILE, ClothoTestConstants.IED_SDFVIEWER_BRIEF_OUTPUT);
        if (templateMismatch(false)) {
            Assertions.fail("Generated TA2 SDF Viewer IED brief output doesn't match template.  See logging.");
        }
    }

    @Test
    void testSDFta2SDFViewer_IED_verbose() {
        initTest(ClothoTestConstants.IED_TA2_FILE, ClothoTestConstants.IED_SDFVIEWER_OUTPUT);
        if (templateMismatch(true)) {
            Assertions.fail("Generated TA2 SDF Viewer IED verbose output doesn't match template.  See logging.");
        }
    }

    @Test
    void testSDFta2SDFViewer_noGraphT() {
        initTest(ClothoTestConstants.IED_TA2_FILE, ClothoTestConstants.IED_SDFVIEWER_CUSTOM_OUTLINK_ONLY_OUTPUT);
        if (templateMismatch(true, false, false)) {
            Assertions.fail("Generated TA2 SDF Viewer without JGraphT doesn't match template.  See logging.");
        }
    }

    @Test
    void testSDFta2SDFViewer_noGraphT_allRelations() {
        initTest(ClothoTestConstants.IED_TA2_FILE, ClothoTestConstants.IED_SDFVIEWER_CUSTOM_ALL_TEMPORALS_OUTPUT);
        if (templateMismatch(true, false, true)) {
            Assertions.fail("Generated TA2 SDF Viewer without JGraphT but with all temporal relations doesn't match template.  See logging.");
        }
    }

}
