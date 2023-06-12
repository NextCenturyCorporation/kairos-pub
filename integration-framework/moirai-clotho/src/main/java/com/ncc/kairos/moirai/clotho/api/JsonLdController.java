package com.ncc.kairos.moirai.clotho.api;

import com.ncc.kairos.moirai.clotho.exceptions.EmptyContextElementException;
import com.ncc.kairos.moirai.clotho.interfaces.IDefinitionService;
import com.ncc.kairos.moirai.clotho.model.*;

import com.ncc.kairos.moirai.clotho.utilities.SDFViewer;
import com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfValidator;
import io.swagger.annotations.ApiParam;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.JSON_LD_CONTEXT;

@RestController
public class JsonLdController implements JsonLdApi {

    private static final String EMPTY_CONTEXT_ELEMENT_MSG = "The context element cannot be empty/null.";
    @Autowired
    private IDefinitionService definitionService;

    /**
     * Processes JSON-LD for insertion.
     *
     * @param requestBody the json schema to insert represented as a map
     * @param validateCycles boolean for if the validator should check for cycles in the graph.
     * @return the REST HTTP response to the insertion
     * @deprecated because we are no longer inserting JSON-LD into Clotho this way.
     */
    @Deprecated(since = "2.3", forRemoval = false)
    public ResponseEntity<String> processJsonLd(@ApiParam(value = "json-ld", required = true)
                                                @Valid @RequestBody Map<String, Object> requestBody) {
        try {
            definitionService.open();

            // Parse json-ld
            JsonLdRepresentation jlo = ApiUtil.deserializeCompactJsonLd(requestBody);

            // 2) Validate the incoming Json-LD before inserting into db.
            List<String> errorsArr = new ArrayList<>();
            List<String> warningsArr = new ArrayList<>();
            List<String> errorsAndWarningsList = new ArrayList<>(); // formerly ApiUtil.validateKsfRequest(jlo)
            for (String curMsg : errorsAndWarningsList) {
                if (curMsg.startsWith(KsfValidator.ERROR)) {
                    errorsArr.add(curMsg);
                } else {
                    warningsArr.add(curMsg);
                }
            }

            // NOTE: What to do if any errors/warnings occurred? Will proceed with insertion anyways for now.
            errorsAndWarningsList.clear();

            // 3) Convert the Json-LD representation into Api-Model-form
            List<Schema> resultSchemas = ApiUtil.convertCompactedJsonLdToSchemas(jlo);

            // 4) Insert the schema(s) into the graph database
            for (Schema schema : resultSchemas) {
                Schema insertedSchema = definitionService.addSchema(schema);
                errorsAndWarningsList.addAll(insertedSchema.getSystemError());
                // NOTE: Ensure that system-errors at ALL levels of the schema object are retrieved. i.e. slots, values
                // NOTE: Determine what types of errors should result in rollback; Throw exception in those cases.
            }

            definitionService.acceptChanges();
            return new ResponseEntity<>(errorsAndWarningsList.toString(), HttpStatus.CREATED);
        } catch (Exception ex) {
            definitionService.rollbackChanges();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            definitionService.close();
        }
    }

    /**
     * Validate JSON-LD against the SDF.
     * @param requestBody the json schema to insert represented as a map
     * @return the REST HTTP response to the validation
     */
    @Override
    public ResponseEntity<ErrorLog> validateKsfRequest(@ApiParam(value = "SDF submission to validate", required = true)
                                                       @Valid @RequestBody Map<String, Object> requestBody,
                                                       @ApiParam(value = "Fail on unknown keywords/properties, or known properties in unexpected places")
                                                       @Valid @RequestParam(value = "failOnUnknown", required = false) Boolean failOnUnknown,
                                                       @ApiParam(value = "Warn if childID is not from the current corpus, or if mediaType doesn't match childID")
                                                       @Valid @RequestParam(value = "warnOnProvenanceMismatch", required = false) Boolean warnOnProvenanceMismatch) {
        if (failOnUnknown == null) {
            failOnUnknown = Boolean.TRUE;
        }
        if (warnOnProvenanceMismatch == null) {
            warnOnProvenanceMismatch = Boolean.FALSE;
        }
        HttpStatus statusCodeToReturn = HttpStatus.OK;
        ErrorLog errorLogToReturn = new ErrorLog();
        try {
            List<String> fatalArr = new ArrayList<>();
            List<String> errorsArr = new ArrayList<>();
            List<String> warningsArr = new ArrayList<>();

            // 1) Check that a context has been set.
            Object context = requestBody.get(JSON_LD_CONTEXT);
            if (context != null && context.equals("")) {
                fatalArr.add(EMPTY_CONTEXT_ELEMENT_MSG);
                throw new EmptyContextElementException(EMPTY_CONTEXT_ELEMENT_MSG);
            }

            // 2) Fail if there are any unknown properties, or known properties in unexpected places.
            if (failOnUnknown) {
                String errorMsg = ApiUtil.checkUnknownProperties(requestBody);
                if (errorMsg != null) {
                    fatalArr.add(errorMsg);
                    statusCodeToReturn = HttpStatus.BAD_REQUEST;
                }
            }

            if (statusCodeToReturn != HttpStatus.BAD_REQUEST) {
                // 3) Deserialize compact JSON and return parsing errors.
                JsonLdRepresentation jlo = ApiUtil.deserializeCompactJsonLd(requestBody);
                if (jlo.getParsingErrors() != null && !jlo.getParsingErrors().isEmpty()) {
                    fatalArr.addAll(jlo.getParsingErrors());
                    statusCodeToReturn = HttpStatus.BAD_REQUEST;
                } else {
                    // 4) Validate the deserialized KSF submission.
                    List<String> errorsAndWarningsList = ApiUtil.validateKsfRequest(jlo, warnOnProvenanceMismatch);
                    for (String curMsg : errorsAndWarningsList) {
                        if (curMsg.startsWith(KsfValidator.FATAL)) {
                            fatalArr.add(curMsg);
                        } else if (curMsg.startsWith(KsfValidator.ERROR)) {
                            errorsArr.add(curMsg);
                        } else {
                            warningsArr.add(curMsg);
                        }
                    }
                }
            }
            errorLogToReturn.setFatalList(fatalArr);
            errorLogToReturn.setErrorsList(errorsArr);
            errorLogToReturn.setWarningsList(warningsArr);
        } catch (Exception ex) {
            errorLogToReturn.addFatalListItem("An exception occurred on the server: " + Arrays.toString(ex.getStackTrace()));
            statusCodeToReturn = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(errorLogToReturn, statusCodeToReturn);
    }

    /**
     * Generates a list of events from the json schema.
     * @param requestBody The json schema to insert represented as a map.
     * @return The event list.
     */
    @Override
    public ResponseEntity<String> generateSDFView(
            @ApiParam(value = "SDF submission", required = true)
            @Valid @RequestBody Map<String, Object> requestBody,
            @ApiParam(value = "Show full output, including un-instantiated events/relations")
            @Valid @RequestParam(value = "verbose", required = false) Boolean verbose) {

        if (verbose == null) {
            verbose = Boolean.TRUE;
        }
        HttpStatus statusCodeToReturn = HttpStatus.OK;
        StringBuilder sdfViewOutput = new StringBuilder();

        try {
            // 1) Deserialize SDF/Json-LD formatted request
            JsonLdRepresentation jlo = ApiUtil.deserializeCompactJsonLd(requestBody);

            if (jlo.getParsingErrors() != null && !jlo.getParsingErrors().isEmpty()) {
                statusCodeToReturn = HttpStatus.BAD_REQUEST;
                sdfViewOutput.append(jlo.getParsingErrors().toString());
            } else {
                // 2) Get output from SDF viewer
                SDFViewer sdfViewer = new SDFViewer(jlo, verbose);
                sdfViewOutput.append(sdfViewer.getOutput());
            }
        } catch (Exception ex) {
            sdfViewOutput.append(ex.toString());
            statusCodeToReturn = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(sdfViewOutput.toString(), statusCodeToReturn);
    }

}
