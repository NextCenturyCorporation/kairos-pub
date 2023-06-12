package com.ncc.kairos.moirai.clotho.api;

import static com.ncc.kairos.moirai.clotho.resources.GraphConstants.ERROR_DB_ID;
import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.JSON_LD_CONTEXT;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdEmbed;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.JsonLdVersion;
import com.apicatalog.jsonld.api.CompactionApi;
import com.apicatalog.jsonld.api.ExpansionApi;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.http.media.MediaType;
import com.ncc.kairos.moirai.clotho.exceptions.ExampleResponseException;
import com.ncc.kairos.moirai.clotho.exceptions.ValidationException;
import com.ncc.kairos.moirai.clotho.model.JsonLdRepresentation;
import com.ncc.kairos.moirai.clotho.model.Schema;
import com.ncc.kairos.moirai.clotho.resources.ApplicationConstants;
import com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfModelValidation;
import com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfProvenanceData;
import com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfValidator;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.NativeWebRequest;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
/**
 * ApiUtil class auto generated from swagger code gen.
 * For this project we only generate the api and model and ignore everything else.
 * This file falls under 'everything else' so we copy it over here so we don't need to generate it again.
 *
 * @author Swagger SpringCodegen
 */
public final class ApiUtil {

    /**
     * Marking the class as private as this is not meant to be instantiated.
     */
    private ApiUtil() {
        super();
    }

    public static void setExampleResponse(NativeWebRequest req, String contentType, String example) {
        try {
            HttpServletResponse res = req.getNativeResponse(HttpServletResponse.class);
            if (res != null) {
                res.setCharacterEncoding("UTF-8");
                res.addHeader("Content-Type", contentType);
                res.getWriter().print(example);
            }
        } catch (IOException e) {
            throw new ExampleResponseException("An IO error occurred while writing a response.", e);
        }
    }

    public static HttpStatus getResponseFromId(String id) {
        if (!id.equals(ERROR_DB_ID)) {
            return HttpStatus.CREATED;
        } else {
            return HttpStatus.NOT_MODIFIED;
        }
    }

    public static <T> HttpStatus getResponseFromList(List<T> genericList) {
        HttpStatus statusToReturn;
        if (!genericList.isEmpty()) {
            statusToReturn = HttpStatus.OK;
        } else {
            statusToReturn = HttpStatus.NO_CONTENT;
        }
        return statusToReturn;
    }

    public static void safePut(Map<String, Object> map, String key, Object val) {
        if (val != null) {
            map.put(key, val);
        }
    }

    // JSON-LD-related processing ======================================================================================

    // Process the context object directly received from API-request.
    // If required, create/retrieve the context(s) that will be passed to Json-Ld Framework for expansion
    // See KAIR-629 for thoughts on dynamic context deserialization
    // For now, this method merely throws an exception if there is an apparent context version mismatch
    public static void assertCorrectContext(Object contextToProcess) throws ValidationException {
        // Verify that the context contains appropriate link to KAIROS-context on S3
        // See KAIR-1149 for ideas to make this more robust
        String contextStr = contextToProcess.toString();
        if (!contextStr.matches(ApplicationConstants.KAIROS_CONTEXT_S3_URL_REGEX)) {
            throw new ValidationException("The provided context does not include a valid KAIROS-context S3-URL");
        } else {
            if (!contextStr.matches(ApplicationConstants.KAIROS_CONTEXT_S3_URL)) {
                throw new ValidationException(String.format("The validator currently only accepts context v%s",
                        ApplicationConstants.CONTEXT_VERSION));
            }
        }
    }

    // Apply Expansion-algorithm on the json-ld provided in request
    public static String expandJsonLd(Map<String, Object> jsonLdObject) throws JsonLdError {
        // Use titanium framework to run expansion-algorithm
        JsonObject json = Json.createObjectBuilder(jsonLdObject).build();
        JsonDocument document = JsonDocument.of(MediaType.JSON_LD, json);
        ExpansionApi expand = JsonLd.expand(document);
        JsonLdOptions options = new JsonLdOptions();
        options.setExplicit(true);
        expand.options(options);
        JsonArray array = expand.get();
        return (array.size() == 1) ? array.get(0).toString() : array.toString();
    }

    public static JsonObject compactJsonLdMapToJsonObject(Map<String, Object> jsonLdObject) throws JsonLdError {
        // Use titanium framework to run expansion-algorithm
        JsonObject docJson = Json.createObjectBuilder(jsonLdObject).build();
        JsonDocument document = JsonDocument.of(MediaType.JSON_LD, docJson);
        InputStreamReader reader = new InputStreamReader(ApiUtil.class.getResourceAsStream(ApplicationConstants.VALIDATION_FILE));
        JsonDocument context = JsonDocument.of(reader);
        JsonLdOptions options = new JsonLdOptions();
        options.setCompactArrays(true);
        options.setProcessingMode(JsonLdVersion.V1_1);
        options.setCompactToRelative(false);
        options.setUseNativeTypes(true);
        options.setEmbed(JsonLdEmbed.ALWAYS);
        CompactionApi compact = JsonLd.compact(document, context);
        compact.options(options);
        return compact.get();
    }

    public static String checkUnknownProperties(Map<String, Object> parsableJsonLd) {
        String unknownPropertyMsg = null;

        try {
            ObjectMapper mapper = JsonMapper.builder().addModule(new JSONPModule())
                    .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    .build();
            JsonObject rawJson = Json.createObjectBuilder(parsableJsonLd).build();
            mapper.convertValue(rawJson, JsonLdRepresentation.class); // check for unknown properties/keywords
        } catch (Exception ex) {
            unknownPropertyMsg = extractPropertyMsg(ex.getMessage());
        }
        return unknownPropertyMsg;
    }

    private static String extractPropertyMsg(String fullMessage) {
        String propertyMsg;
        try {
            StringBuilder propertyMsgSB = new StringBuilder();
            int fieldIndex = fullMessage.indexOf("(class");
            propertyMsgSB.append(fullMessage, 0, fieldIndex).append("in ");
            fieldIndex += "(class com.ncc.kairos.moirai.clotho.model.".length();
            String keywordName = fullMessage.substring(fieldIndex, fullMessage.indexOf(")", fieldIndex));

            // Convert certain keywords to commonly used keyword names
            if (keywordName.equals("Filler")) {
                keywordName = "Values";
            } else if (keywordName.equals("JsonLdRepresentation")) {
                keywordName = "Document";
            } else {
                keywordName = keywordName.replace("Schema", "");
            }
            propertyMsgSB.append(keywordName).append(" object.  Validation aborted.  Set failOnUnknown to false to disable this error.");
            propertyMsg = propertyMsgSB.toString().replace("\"", "'").replace("Unrecognized field", "Unexpected keyword");
        } catch (Throwable t) { // If parsing fails, return original message
            propertyMsg = fullMessage;
        }
        return propertyMsg;
    }

    public static JsonLdRepresentation deserializeCompactJsonLd(Map<String, Object> parsableJsonLd) {
        JsonLdRepresentation correctObject;
        try {
            // Check the CONTEXT object and validate that it includes the correct kairos context
            assertCorrectContext(parsableJsonLd.get(JSON_LD_CONTEXT));

            //  Use json-ld library to EXPAND the json-ld provided in the request-body
            JsonObject value = compactJsonLdMapToJsonObject(parsableJsonLd);
            ObjectMapper mapper = JsonMapper.builder().addModule(new JSONPModule())
                    .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();
            correctObject = mapper.convertValue(value, JsonLdRepresentation.class);
        } catch (Exception ex) {
            correctObject = new JsonLdRepresentation();
            correctObject.addParsingErrorsItem(String.format("SDF parsing error: %s", ex.getMessage()));
        }
        return correctObject;
    }

    /**
     * Create the schema-model objects from the JsonLdRepresentation. Currently unimplemented.
     * @param jlo the JSON-LD representation
     * @return a list of Schema objects
     * @deprecated because we are no longer converting JSON-LD to Clotho model objects
     */
    @Deprecated(since = "2.3", forRemoval = false)
    public static List<Schema> convertCompactedJsonLdToSchemas(JsonLdRepresentation jlo) { // NOSONAR
        try {
            // Create Schema objects based on JsonLdRepresentation.
            return new ArrayList<>();
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    // Validate the JsonLdRepresentation
    public static List<String> validateKsfRequest(JsonLdRepresentation jlo, boolean warnOnProvenanceMismatch) {
        KsfValidator validator = new KsfModelValidation(jlo);
        KsfProvenanceData.setPassThroughMode(!warnOnProvenanceMismatch);
        if (validator.isTA2()) {
            // Load KsfProvenanceData for provenanceData checking
            KsfProvenanceData.loadProvenanceData();
        }
        return validator.validate();
    }
}
